package ir.orix.school.attendance.application;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.attendance.domain.*;
import ir.orix.school.attendance.persistence.*;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.*;
import ir.orix.school.organization.persistence.*;
import ir.orix.school.people.domain.*;
import ir.orix.school.people.persistence.*;
import ir.orix.school.scheduling.domain.ClassSession;
import ir.orix.school.scheduling.persistence.ClassSessionRepository;
import ir.orix.school.shared.security.ScopeAuthorizer;
import ir.orix.school.shared.web.ApiException;

@Service
public class AttendanceService {
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final InstructorProfileRepository instructors;
    private final StudentProfileRepository students;
    private final UserAccountRepository accounts;
    private final ClassSessionRepository sessions;
    private final EnrollmentRepository enrollments;
    private final AttendanceSheetRepository sheets;
    private final AttendanceRecordRepository records;
    private final AttendanceRecordHistoryRepository history;
    private final ScopeAuthorizer authorizer;
    private final AuditService audit;
    private final Clock clock;

    public AttendanceService(TenantRepository tenants, TenantMembershipRepository memberships, InstructorProfileRepository instructors, StudentProfileRepository students, UserAccountRepository accounts, ClassSessionRepository sessions, EnrollmentRepository enrollments, AttendanceSheetRepository sheets, AttendanceRecordRepository records, AttendanceRecordHistoryRepository history, ScopeAuthorizer authorizer, AuditService audit, Clock clock) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.instructors = instructors;
        this.students = students;
        this.accounts = accounts;
        this.sessions = sessions;
        this.enrollments = enrollments;
        this.sheets = sheets;
        this.records = records;
        this.history = history;
        this.authorizer = authorizer;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SummaryView> list(Long tenantId, Long membershipId) {
        Map<Long, AttendanceSheet> bySession = sheets.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().collect(Collectors.toMap(s -> s.getClassSession().getId(), s -> s));
        return sessions.findAllByTenantIdOrderByPlannedStartAtAsc(tenantId).stream().distinct().filter(s -> canAccess(tenantId, membershipId, s, "ATTENDANCE_READ")).map(s -> summary(s, bySession.get(s.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public SheetView sheet(Long tenantId, Long membershipId, Long sessionId) {
        ClassSession session = session(tenantId, sessionId);
        ensureAccess(tenantId, membershipId, session, "ATTENDANCE_READ");
        AttendanceSheet sheet = sheets.findByClassSessionIdAndTenantId(sessionId, tenantId).orElse(null);
        List<Enrollment> roster = eligibleEnrollments(tenantId, session);
        Map<Long, AttendanceRecord> existing = sheet == null ? Map.of() : records.findAllBySheetIdOrderByStudentPersonLastNameAsc(sheet.getId()).stream().collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));
        return sheetView(session, sheet, roster, existing);
    }

    @Transactional
    public SheetView save(Long tenantId, Long membershipId, Long actorId, Long sessionId, SaveCommand command) {
        ClassSession session = session(tenantId, sessionId);
        ensureAccess(tenantId, membershipId, session, "ATTENDANCE_WRITE");
        if (Set.of(ClassSession.Status.CANCELLED, ClassSession.Status.POSTPONED, ClassSession.Status.RESCHEDULED).contains(session.getStatus()))
            throw conflict("SESSION_NOT_ATTENDABLE", "Attendance cannot be recorded for this session.");
        /*if (session.getSessionDate().isAfter(LocalDate.now(clock)))
            throw conflict("FUTURE_SESSION_ATTENDANCE", "Attendance cannot be recorded before the session date.");*/
        List<Enrollment> roster = eligibleEnrollments(tenantId, session);
        Map<Long, Enrollment> eligible = roster.stream().collect(Collectors.toMap(e -> e.getStudent().getId(), e -> e));
        if (command.records().stream().map(RecordCommand::studentId).distinct().count() != command.records().size())
            throw invalid("DUPLICATE_STUDENT", "A student can only occur once in an attendance submission.");
        AttendanceSheet sheet = sheets.findByClassSessionIdAndTenantId(sessionId, tenantId).orElseGet(() -> sheets.save(new AttendanceSheet(tenant(tenantId), session)));
        if (sheet.getStatus() == AttendanceSheet.Status.FINALIZED && (command.correctionReason() == null || command.correctionReason().isBlank()))
            throw conflict("CORRECTION_REASON_REQUIRED", "A reason is required to edit finalized attendance.");
        UserAccount actor = account(actorId);
        Instant now = Instant.now(clock);
        for (RecordCommand item : command.records()) {
            Enrollment enrollment = eligible.get(item.studentId());
            if (enrollment == null)
                throw invalid("STUDENT_NOT_ELIGIBLE", "Student is not enrolled for this session date.");
            validate(item);
            AttendanceRecord record = records.findBySheetIdAndStudentId(sheet.getId(), item.studentId()).orElse(null);
            AttendanceRecord.PresenceStatus previous;
            if (record == null) {
                record = records.save(new AttendanceRecord(tenant(tenantId), sheet, enrollment, item.status(), item.deliveryMode(), item.lateMinutes(), item.note(), actor, now));
                previous = null;
            } else
                previous = record.change(item.status(), item.deliveryMode(), item.lateMinutes(), item.note(), actor, now);
            history.save(new AttendanceRecordHistory(tenant(tenantId), record, previous, item.status(), item.deliveryMode(), item.status() == AttendanceRecord.PresenceStatus.LATE ? item.lateMinutes() : null, item.note(), sheet.getStatus() == AttendanceSheet.Status.FINALIZED ? command.correctionReason() : null, actor, now));
        }
        if (command.finalizeSheet()) {
            long marked = records.findAllBySheetIdOrderByStudentPersonLastNameAsc(sheet.getId()).stream().map(r -> r.getStudent().getId()).filter(eligible::containsKey).distinct().count();
            if (marked != eligible.size())
                throw conflict("ATTENDANCE_INCOMPLETE", "Every eligible student must be marked before finalization.");
            sheet.finalizeBy(actor, now);
            session.complete();
        }
        audit.record(tenantId, actorId, command.finalizeSheet() ? "ATTENDANCE_FINALIZED" : "ATTENDANCE_SAVED", "CLASS_SESSION", sessionId, Map.of("recordCount", command.records().size()));
        Map<Long, AttendanceRecord> current = records.findAllBySheetIdOrderByStudentPersonLastNameAsc(sheet.getId()).stream().collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));
        return sheetView(session, sheet, roster, current);
    }

    @Transactional(readOnly = true)
    public List<MyAttendanceView> mine(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        StudentProfile student = students.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId()).orElseThrow(notFound("STUDENT_PROFILE_NOT_FOUND"));
        return forStudent(tenantId, student.getId());
    }

    @Transactional(readOnly = true)
    public List<MyAttendanceView> forStudent(Long tenantId, Long studentId) {
        students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        return records.findAllByTenantIdAndStudentIdOrderByMarkedAtDesc(tenantId, studentId).stream().map(AttendanceService::myView).toList();
    }

    private List<Enrollment> eligibleEnrollments(Long tenantId, ClassSession session) {
        LocalDate date = session.getSessionDate();
        return enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, session.getClassSection().getId()).stream().filter(e -> !e.getStartedOn().isAfter(date) && (e.getEndedOn() == null || !e.getEndedOn().isBefore(date))).toList();
    }

    private boolean canAccess(Long tenantId, Long membershipId, ClassSession session, String permission) {
        if (authorizer.isTenantAdmin()) return true;
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        Optional<InstructorProfile> instructor = instructors.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId());
        if (instructor.isPresent())
            return session.getInstructors().stream().anyMatch(i -> i.getId().equals(instructor.get().getId()));
        return authorizer.hasPermissionForBranch(permission, session.getClassSection().getOffering().getBranch().getId());
    }

    private void ensureAccess(Long tenantId, Long membershipId, ClassSession session, String permission) {
        if (!canAccess(tenantId, membershipId, session, permission))
            throw new ApiException(HttpStatus.FORBIDDEN, "SESSION_ACCESS_DENIED", "You are not assigned or authorized for this session.");
    }

    private static void validate(RecordCommand item) {
        if (item.status() == AttendanceRecord.PresenceStatus.LATE && (item.lateMinutes() == null || item.lateMinutes() < 1))
            throw invalid("LATE_MINUTES_REQUIRED", "Late attendance requires positive late minutes.");
        if (item.lateMinutes() != null && item.lateMinutes() < 0)
            throw invalid("INVALID_LATE_MINUTES", "Late minutes cannot be negative.");
    }

    private SummaryView summary(ClassSession session, AttendanceSheet sheet) {
        List<AttendanceRecord> values = sheet == null ? List.of() : records.findAllBySheetIdOrderByStudentPersonLastNameAsc(sheet.getId());
        return new SummaryView(session.getId(), session.getClassSection().getName(), session.getClassSection().getOffering().getCourse().getTitle(), session.getSessionDate(), session.getPlannedStartAt(), sheet == null ? "NOT_STARTED" : sheet.getStatus().name(), values.size(), (int) values.stream().filter(r -> r.getPresenceStatus() == AttendanceRecord.PresenceStatus.ABSENT).count());
    }

    private static SheetView sheetView(ClassSession session, AttendanceSheet sheet, List<Enrollment> roster, Map<Long, AttendanceRecord> existing) {
        List<RecordView> values = roster.stream().map(e -> {
            AttendanceRecord r = existing.get(e.getStudent().getId());
            return new RecordView(r == null ? null : r.getId(), e.getStudent().getId(), e.getStudent().getStudentCode(), e.getStudent().getPerson().getFullName(), r == null ? null : r.getPresenceStatus().name(), r == null ? session.getClassSection().getDeliveryMode().name() : r.getDeliveryMode().name(), r == null ? null : r.getLateMinutes(), r == null ? null : r.getNote(), r == null ? null : r.getMarkedAt());
        }).toList();
        return new SheetView(sheet == null ? null : sheet.getId(), session.getId(), session.getClassSection().getName(), session.getClassSection().getOffering().getCourse().getTitle(), session.getSessionDate(), session.getPlannedStartAt(), sheet == null ? "NOT_STARTED" : sheet.getStatus().name(), sheet == null ? null : sheet.getFinalizedAt(), values);
    }

    private static MyAttendanceView myView(AttendanceRecord r) {
        ClassSession s = r.getSheet().getClassSession();
        return new MyAttendanceView(r.getId(), s.getId(), s.getSessionDate(), s.getClassSection().getName(), s.getClassSection().getOffering().getCourse().getTitle(), r.getPresenceStatus().name(), r.getDeliveryMode().name(), r.getLateMinutes(), r.getNote(), r.getSheet().getStatus().name());
    }

    private ClassSession session(Long tenantId, Long id) {
        return sessions.findByIdAndTenantId(id, tenantId).orElseThrow(notFound("SESSION_NOT_FOUND"));
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND"));
    }

    private UserAccount account(Long id) {
        return accounts.findById(id).orElseThrow(notFound("ACTOR_NOT_FOUND"));
    }

    private static Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested attendance item was not found.");
    }

    private static ApiException invalid(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }

    private static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }

    public record RecordCommand(Long studentId, AttendanceRecord.PresenceStatus status,
                                ClassSection.DeliveryMode deliveryMode, Integer lateMinutes, String note) {
    }

    public record SaveCommand(boolean finalizeSheet, String correctionReason, List<RecordCommand> records) {
    }

    public record SummaryView(Long sessionId, String className, String courseTitle, LocalDate sessionDate,
                              Instant plannedStartAt, String sheetStatus, int markedCount, int absentCount) {
    }

    public record RecordView(Long id, Long studentId, String studentCode, String studentName, String status,
                             String deliveryMode, Integer lateMinutes, String note, Instant markedAt) {
    }

    public record SheetView(Long id, Long sessionId, String className, String courseTitle, LocalDate sessionDate,
                            Instant plannedStartAt, String status, Instant finalizedAt, List<RecordView> records) {
    }

    public record MyAttendanceView(Long id, Long sessionId, LocalDate sessionDate, String className, String courseTitle,
                                   String status, String deliveryMode, Integer lateMinutes, String note,
                                   String sheetStatus) {
    }
}
