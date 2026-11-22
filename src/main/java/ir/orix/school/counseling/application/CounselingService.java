package ir.orix.school.counseling.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.counseling.application.CounselingAccess.CounselorTier;
import ir.orix.school.counseling.domain.*;
import ir.orix.school.counseling.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.people.domain.GuardianProfile;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.GuardianProfileRepository;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class CounselingService {
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final StudentProfileRepository students;
    private final GuardianProfileRepository guardians;
    private final CounselorAssignmentRepository assignments;
    private final CounselorSupervisionRepository supervisions;
    private final StudentDailyLogRepository dailyLogs;
    private final CounselorAdminReportRepository adminReports;
    private final CounselorSeniorReportRepository seniorReports;
    private final StudyPlanRepository studyPlans;
    private final StudyPlanItemRepository planItems;
    private final StudyPlanProgressRepository planProgress;
    private final PeopleService people;
    private final CounselingAccess access;
    private final CounselingAiService ai;
    private final AuditService audit;
    private final Clock clock;

    public CounselingService(TenantRepository tenants, TenantMembershipRepository memberships, StudentProfileRepository students,
            GuardianProfileRepository guardians, CounselorAssignmentRepository assignments, CounselorSupervisionRepository supervisions,
            StudentDailyLogRepository dailyLogs, CounselorAdminReportRepository adminReports, CounselorSeniorReportRepository seniorReports,
            StudyPlanRepository studyPlans, StudyPlanItemRepository planItems, StudyPlanProgressRepository planProgress,
            PeopleService people, CounselingAccess access, CounselingAiService ai, AuditService audit, Clock clock) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.students = students;
        this.guardians = guardians;
        this.assignments = assignments;
        this.supervisions = supervisions;
        this.dailyLogs = dailyLogs;
        this.adminReports = adminReports;
        this.seniorReports = seniorReports;
        this.studyPlans = studyPlans;
        this.planItems = planItems;
        this.planProgress = planProgress;
        this.people = people;
        this.access = access;
        this.ai = ai;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> listAssignmentsForCounselor(Long tenantId, Long counselorMembershipId) {
        return assignments.findAllByTenantIdAndCounselorMembershipIdAndStatusOrderByCreatedAtDesc(
                tenantId, counselorMembershipId, CounselorAssignment.Status.ACTIVE).stream()
                .map(this::assignmentView).toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> listAssignments(Long tenantId) {
        return assignments.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::assignmentView).toList();
    }

    @Transactional
    public AssignmentView createAssignment(Long tenantId, Long actorId, AssignmentCommand command) {
        Tenant tenant = tenant(tenantId);
        TenantMembership counselor = membership(tenantId, command.counselorMembershipId());
        CounselorAssignment.AssignmentType type = CounselorAssignment.AssignmentType.valueOf(command.assignmentType());
        validateAssignmentTier(counselor.getId(), type);
        StudentProfile student = null;
        GuardianProfile guardian = null;
        if (type == CounselorAssignment.AssignmentType.STUDENT) {
            student = students.findByIdAndTenantId(command.studentId(), tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
            if (assignments.existsByTenantIdAndCounselorMembershipIdAndStudentIdAndAssignmentTypeAndStatusAndEffectiveToIsNull(
                    tenantId, counselor.getId(), student.getId(), type, CounselorAssignment.Status.ACTIVE)) {
                throw conflict("COUNSELOR_ALREADY_ASSIGNED");
            }
        } else {
            guardian = guardians.findByIdAndTenantId(command.guardianId(), tenantId).orElseThrow(notFound("GUARDIAN_NOT_FOUND"));
        }
        CounselorAssignment value = assignments.save(new CounselorAssignment(tenant, counselor, type, student, guardian,
                command.effectiveFrom() == null ? LocalDate.now(clock) : command.effectiveFrom(), command.note()));
        audit.record(tenantId, actorId, "COUNSELOR_ASSIGNED", "COUNSELOR_ASSIGNMENT", value.getId(), Map.of("type", type.name()));
        return assignmentView(value);
    }

    @Transactional
    public AssignmentView endAssignment(Long tenantId, Long actorId, Long assignmentId) {
        CounselorAssignment value = assignments.findByIdAndTenantId(assignmentId, tenantId).orElseThrow(notFound("ASSIGNMENT_NOT_FOUND"));
        value.end(LocalDate.now(clock));
        audit.record(tenantId, actorId, "COUNSELOR_ASSIGNMENT_ENDED", "COUNSELOR_ASSIGNMENT", value.getId(), Map.of());
        return assignmentView(value);
    }

    @Transactional
    public SupervisionView createSupervision(Long tenantId, Long actorId, SupervisionCommand command) {
        Tenant tenant = tenant(tenantId);
        TenantMembership senior = membership(tenantId, command.seniorMembershipId());
        TenantMembership counselor = membership(tenantId, command.counselorMembershipId());
        access.requireCounselorTier(senior.getId());
        if (access.counselorTier(senior.getId()).orElseThrow() != CounselorTier.SENIOR) {
            throw CounselingAccess.forbidden("SENIOR_COUNSELOR_REQUIRED");
        }
        CounselorSupervision value = supervisions.save(new CounselorSupervision(tenant, senior, counselor,
                command.effectiveFrom() == null ? LocalDate.now(clock) : command.effectiveFrom()));
        audit.record(tenantId, actorId, "COUNSELOR_SUPERVISION_CREATED", "COUNSELOR_SUPERVISION", value.getId(), Map.of());
        return supervisionView(value);
    }

    @Transactional(readOnly = true)
    public List<DailyLogView> listDailyLogsForStudent(Long tenantId, Long membershipId, Long studentId) {
        resolveStudentAccess(tenantId, membershipId, studentId);
        return dailyLogs.findAllByTenantIdAndStudentIdOrderByLogDateDesc(tenantId, studentId).stream().map(this::dailyLogView).toList();
    }

    @Transactional
    public DailyLogView upsertDailyLog(Long tenantId, Long membershipId, Long studentId, DailyLogCommand command) {
        StudentProfile student = resolveStudentAccess(tenantId, membershipId, studentId);
        LocalDate logDate = command.logDate() == null ? LocalDate.now(clock) : command.logDate();
        StudentDailyLog log = dailyLogs.findByTenantIdAndStudentIdAndLogDate(tenantId, student.getId(), logDate)
                .orElseGet(() -> new StudentDailyLog(tenant(tenantId), student, logDate));
        if (log.getStatus() == StudentDailyLog.Status.REVIEWED) {
            throw badRequest("DAILY_LOG_LOCKED");
        }
        log.setDaySummary(command.daySummary());
        log.replaceEntries(command.entries().stream()
                .map(entry -> new StudentDailyLogEntry(tenant(tenantId), entry.activityTitle(), entry.subjectName(),
                        entry.durationMinutes(), entry.notes()))
                .toList());
        return dailyLogView(dailyLogs.save(log));
    }

    @Transactional
    public DailyLogView submitDailyLog(Long tenantId, Long membershipId, Long studentId, LocalDate logDate) {
        StudentProfile student = resolveStudentAccess(tenantId, membershipId, studentId);
        LocalDate date = logDate == null ? LocalDate.now(clock) : logDate;
        StudentDailyLog log = dailyLogs.findByTenantIdAndStudentIdAndLogDate(tenantId, student.getId(), date)
                .orElseThrow(notFound("DAILY_LOG_NOT_FOUND"));
        log.submit();
        return dailyLogView(dailyLogs.save(log));
    }

    @Transactional(readOnly = true)
    public List<DailyLogView> counselorDailyInbox(Long tenantId, Long counselorMembershipId) {
        access.requireCounselorTier(counselorMembershipId);
        List<Long> studentIds = assignments.findAllByTenantIdAndCounselorMembershipIdAndStatusOrderByCreatedAtDesc(
                tenantId, counselorMembershipId, CounselorAssignment.Status.ACTIVE).stream()
                .filter(value -> value.getAssignmentType() == CounselorAssignment.AssignmentType.STUDENT)
                .filter(value -> value.getStudent() != null)
                .map(value -> value.getStudent().getId())
                .distinct()
                .toList();
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return dailyLogs.findAllByTenantIdAndStudentIdInAndStatusOrderByLogDateDesc(tenantId, studentIds, StudentDailyLog.Status.SUBMITTED)
                .stream().map(this::dailyLogView).toList();
    }

    @Transactional
    public DailyLogView reviewDailyLog(Long tenantId, Long counselorMembershipId, Long logId, String feedback) {
        StudentDailyLog log = dailyLogs.findByIdAndTenantId(logId, tenantId).orElseThrow(notFound("DAILY_LOG_NOT_FOUND"));
        access.requireStudentCounselorAccess(tenantId, counselorMembershipId, log.getStudent().getId());
        log.review(feedback, counselorMembershipId, clock.instant());
        return dailyLogView(dailyLogs.save(log));
    }

    @Transactional
    public StudyPlanView createStudyPlan(Long tenantId, Long counselorMembershipId, Long actorId, StudyPlanCommand command) {
        StudentProfile student = students.findByIdAndTenantId(command.studentId(), tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        access.requireCounselingWrite(tenantId, counselorMembershipId, student.getId());
        TenantMembership counselor = membership(tenantId, counselorMembershipId);
        StudyPlan plan = new StudyPlan(tenant(tenantId), student, counselor, command.title(), command.description(),
                command.startDate(), command.endDate());
        plan.replaceItems(command.items().stream()
                .map(item -> new StudyPlanItem(tenant(tenantId), item.subjectName(), item.activityTitle(), item.targetMinutes(),
                        item.plannedDate(), item.notes()))
                .toList());
        StudyPlan saved = studyPlans.save(plan);
        audit.record(tenantId, actorId, "STUDY_PLAN_CREATED", "STUDY_PLAN", saved.getId(), Map.of("student", student.getStudentCode()));
        return studyPlanView(saved);
    }

    @Transactional
    public void deleteStudyPlanItem(Long tenantId, Long counselorMembershipId, Long actorId, Long itemId) {
        StudyPlanItem item = planItems.findByIdAndTenantId(itemId, tenantId).orElseThrow(notFound("PLAN_ITEM_NOT_FOUND"));
        StudyPlan plan = studyPlans.findByIdAndTenantId(item.getStudyPlan().getId(), tenantId).orElseThrow(notFound("STUDY_PLAN_NOT_FOUND"));
        access.requireCounselingWrite(tenantId, counselorMembershipId, plan.getStudent().getId());
        if (!plan.getCounselorMembership().getId().equals(counselorMembershipId) && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("PLAN_OWNER_MISMATCH");
        }
        StudyPlanItem linked = plan.getItems().stream().filter(row -> row.getId().equals(itemId)).findFirst()
                .orElseThrow(notFound("PLAN_ITEM_NOT_FOUND"));
        planProgress.findByTenantIdAndStudyPlanItemId(tenantId, itemId).ifPresent(planProgress::delete);
        Long planId = plan.getId();
        plan.removeItem(linked);
        if (plan.getItems().isEmpty()) {
            studyPlans.delete(plan);
            audit.record(tenantId, actorId, "STUDY_PLAN_DELETED", "STUDY_PLAN", planId, Map.of("student", plan.getStudent().getStudentCode()));
        } else {
            studyPlans.save(plan);
            audit.record(tenantId, actorId, "STUDY_PLAN_ITEM_DELETED", "STUDY_PLAN_ITEM", itemId, Map.of("plan", planId.toString()));
        }
    }

    @Transactional(readOnly = true)
    public List<StudyPlanView> studyPlansForStudent(Long tenantId, Long membershipId, Long studentId) {
        resolveStudentAccess(tenantId, membershipId, studentId);
        return studyPlans.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream().map(this::studyPlanView).toList();
    }

    @Transactional(readOnly = true)
    public List<StudyPlanView> studyPlansForCounselor(Long tenantId, Long counselorMembershipId) {
        access.requireCounselorTier(counselorMembershipId);
        return studyPlans.findAllByTenantIdAndCounselorMembershipIdOrderByCreatedAtDesc(tenantId, counselorMembershipId)
                .stream().map(this::studyPlanView).toList();
    }

    @Transactional
    public StudyPlanView recordStudyProgress(Long tenantId, Long membershipId, Long studentId, Long itemId, ProgressCommand command) {
        StudentProfile student = resolveStudentAccess(tenantId, membershipId, studentId);
        StudyPlanItem item = planItems.findByIdAndTenantId(itemId, tenantId).orElseThrow(notFound("PLAN_ITEM_NOT_FOUND"));
        if (!item.getStudyPlan().getStudent().getId().equals(student.getId())) {
            throw CounselingAccess.forbidden("PLAN_ITEM_STUDENT_MISMATCH");
        }
        StudyPlanProgress progress = planProgress.findByTenantIdAndStudyPlanItemId(tenantId, itemId)
                .orElseGet(() -> new StudyPlanProgress(tenant(tenantId), item, student, command.completedMinutes(), clock.instant(), command.note()));
        progress.update(command.completedMinutes(), clock.instant(), command.note());
        planProgress.save(progress);
        return studyPlanView(studyPlans.findByIdAndTenantId(item.getStudyPlan().getId(), tenantId).orElseThrow());
    }

    @Transactional
    public AdminReportView createAdminReport(Long tenantId, Long counselorMembershipId, AdminReportCommand command) {
        access.requireCounselorTier(counselorMembershipId);
        CounselorAdminReport report = adminReports.save(new CounselorAdminReport(tenant(tenantId), membership(tenantId, counselorMembershipId),
                command.title(), command.body(), command.periodLabel()));
        return adminReportView(report);
    }

    @Transactional
    public AdminReportView sendAdminReport(Long tenantId, Long counselorMembershipId, Long reportId) {
        CounselorAdminReport report = adminReports.findByIdAndTenantId(reportId, tenantId).orElseThrow(notFound("REPORT_NOT_FOUND"));
        if (!report.getCounselorMembership().getId().equals(counselorMembershipId) && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("REPORT_OWNER_MISMATCH");
        }
        report.send(clock.instant());
        return adminReportView(adminReports.save(report));
    }

    @Transactional(readOnly = true)
    public List<AdminReportView> listAdminReports(Long tenantId) {
        return adminReports.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .filter(report -> report.getStatus() == CounselorAdminReport.Status.SENT)
                .map(this::adminReportView).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminReportView> listCounselorAdminReports(Long tenantId, Long counselorMembershipId) {
        return adminReports.findAllByTenantIdAndCounselorMembershipIdOrderByCreatedAtDesc(tenantId, counselorMembershipId)
                .stream().map(this::adminReportView).toList();
    }

    @Transactional(readOnly = true)
    public List<SupervisionView> listSupervisions(Long tenantId) {
        return supervisions.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(this::supervisionView).toList();
    }

    @Transactional(readOnly = true)
    public List<SeniorReportView> listSeniorReports(Long tenantId, Long seniorMembershipId) {
        if (access.counselorTier(seniorMembershipId).orElse(null) != CounselorTier.SENIOR && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("SENIOR_COUNSELOR_REQUIRED");
        }
        return seniorReports.findAllByTenantIdAndSeniorMembershipIdOrderByReportYearDescReportMonthDesc(tenantId, seniorMembershipId)
                .stream().map(this::seniorReportView).toList();
    }

    @Transactional
    public SeniorReportView draftMonthlySeniorReportForCounselor(Long tenantId, Long seniorMembershipId, Long counselorMembershipId,
            int year, int month) {
        if (access.counselorTier(seniorMembershipId).orElse(null) != CounselorTier.SENIOR && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("SENIOR_COUNSELOR_REQUIRED");
        }
        return systemDraftMonthlySeniorReport(tenantId, seniorMembershipId, counselorMembershipId, year, month);
    }

    @Transactional
    public SeniorReportView systemDraftMonthlySeniorReport(Long tenantId, Long seniorMembershipId, Long counselorMembershipId,
            int year, int month) {
        var existing = seniorReports.findByTenantIdAndCounselorMembershipIdAndReportYearAndReportMonth(
                tenantId, counselorMembershipId, year, month);
        if (existing.isPresent()) {
            return seniorReportView(existing.get());
        }
        TenantMembership senior = membership(tenantId, seniorMembershipId);
        TenantMembership counselor = membership(tenantId, counselorMembershipId);
        String title = "گزارش ماهانه " + year + "/" + month;
        String body = buildMonthlyCounselorSummary(tenantId, counselorMembershipId, year, month);
        CounselorSeniorReport report = new CounselorSeniorReport(tenant(tenantId), counselor, senior, year, month, title, body);
        ai.analyzeCounselorMonthlyReport(body).ifPresent(report::applyAiAnalysis);
        return seniorReportView(seniorReports.save(report));
    }

    @Transactional
    public SeniorReportView regenerateAiAnalysis(Long tenantId, Long seniorMembershipId, Long reportId) {
        CounselorSeniorReport report = seniorReports.findByIdAndTenantId(reportId, tenantId).orElseThrow(notFound("REPORT_NOT_FOUND"));
        if (!report.getSeniorMembership().getId().equals(seniorMembershipId) && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("SENIOR_REPORT_ACCESS_DENIED");
        }
        ai.analyzeCounselorMonthlyReport(report.getBody()).ifPresent(report::applyAiAnalysis);
        return seniorReportView(seniorReports.save(report));
    }

    @Transactional
    public SeniorReportView applyAiAnalysis(Long tenantId, Long seniorMembershipId, Long reportId, String analysisText) {
        CounselorSeniorReport report = seniorReports.findByIdAndTenantId(reportId, tenantId).orElseThrow(notFound("REPORT_NOT_FOUND"));
        if (!report.getSeniorMembership().getId().equals(seniorMembershipId) && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("SENIOR_REPORT_ACCESS_DENIED");
        }
        report.applyAiAnalysis(analysisText);
        return seniorReportView(seniorReports.save(report));
    }

    @Transactional
    public SeniorReportView finalizeSeniorReport(Long tenantId, Long seniorMembershipId, Long reportId) {
        CounselorSeniorReport report = seniorReports.findByIdAndTenantId(reportId, tenantId).orElseThrow(notFound("REPORT_NOT_FOUND"));
        if (!report.getSeniorMembership().getId().equals(seniorMembershipId) && !access.isTenantAdmin()) {
            throw CounselingAccess.forbidden("SENIOR_REPORT_ACCESS_DENIED");
        }
        report.finalizeReport(clock.instant());
        return seniorReportView(seniorReports.save(report));
    }

    private String buildMonthlyCounselorSummary(Long tenantId, Long counselorMembershipId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<Long> studentIds = assignments.findAllByTenantIdAndCounselorMembershipIdAndStatusOrderByCreatedAtDesc(
                tenantId, counselorMembershipId, CounselorAssignment.Status.ACTIVE).stream()
                .map(value -> value.getStudent() == null ? null : value.getStudent().getId())
                .filter(java.util.Objects::nonNull)
                .toList();
        int submittedLogs = 0;
        for (Long studentId : studentIds) {
            submittedLogs += (int) dailyLogs.findAllByTenantIdAndStudentIdOrderByLogDateDesc(tenantId, studentId).stream()
                    .filter(log -> !log.getLogDate().isBefore(from) && !log.getLogDate().isAfter(to))
                    .filter(log -> log.getStatus() != StudentDailyLog.Status.DRAFT)
                    .count();
        }
        return "بازه: " + from + " تا " + to + "\nتعداد دانش‌آموز تحت پوشش: " + studentIds.size()
                + "\nگزارش‌های کار ارسال‌شده: " + submittedLogs;
    }

    private StudentProfile resolveStudentAccess(Long tenantId, Long membershipId, Long studentId) {
        Long resolved = studentId != null ? studentId : resolveStudentId(tenantId, membershipId, null);
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        if (profiles.student() != null && profiles.student().id().equals(resolved)) {
            return students.findByIdAndTenantId(resolved, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        }
        people.requireChildAcademicAccess(tenantId, membershipId, resolved);
        return students.findByIdAndTenantId(resolved, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
    }

    private Long resolveStudentId(Long tenantId, Long membershipId, Long optionalChildId) {
        if (optionalChildId != null) {
            people.requireChildAcademicAccess(tenantId, membershipId, optionalChildId);
            return optionalChildId;
        }
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        if (profiles.student() == null) {
            throw CounselingAccess.forbidden("STUDENT_PROFILE_REQUIRED");
        }
        return profiles.student().id();
    }

    public StudentProfile resolveStudentForPortal(Long tenantId, Long membershipId, Long optionalChildId) {
        Long studentId = resolveStudentId(tenantId, membershipId, optionalChildId);
        return students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
    }

    private void validateAssignmentTier(Long counselorMembershipId, CounselorAssignment.AssignmentType type) {
        CounselorTier tier = access.requireCounselorTier(counselorMembershipId);
        if (type == CounselorAssignment.AssignmentType.STUDENT && tier == CounselorTier.MID) {
            throw badRequest("MID_COUNSELOR_CANNOT_ASSIGN_STUDENT");
        }
        if (type == CounselorAssignment.AssignmentType.GUARDIAN && tier == CounselorTier.JUNIOR) {
            throw badRequest("JUNIOR_COUNSELOR_CANNOT_ASSIGN_GUARDIAN");
        }
    }

    private Tenant tenant(Long id) { return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND")); }
    private TenantMembership membership(Long tenantId, Long id) {
        return memberships.findByIdAndTenantId(id, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
    }

    private AssignmentView assignmentView(CounselorAssignment value) {
        String counselorName = value.getCounselorMembership().getPerson().getFullName();
        String targetName = value.getStudent() != null ? value.getStudent().getPerson().getFullName()
                : value.getGuardian() != null ? value.getGuardian().getPerson().getFullName() : null;
        Long targetId = value.getStudent() != null ? value.getStudent().getId()
                : value.getGuardian() != null ? value.getGuardian().getId() : null;
        return new AssignmentView(value.getId(), value.getCounselorMembership().getId(), counselorName, value.getAssignmentType().name(),
                targetId, targetName, value.getEffectiveFrom(), value.getEffectiveTo(), value.getStatus().name(), value.getNote());
    }

    private SupervisionView supervisionView(CounselorSupervision value) {
        return new SupervisionView(value.getId(), value.getSeniorMembership().getId(), value.getSeniorMembership().getPerson().getFullName(),
                value.getCounselorMembership().getId(), value.getCounselorMembership().getPerson().getFullName(),
                value.getEffectiveFrom(), value.getEffectiveTo(), value.getStatus().name());
    }

    private DailyLogView dailyLogView(StudentDailyLog log) {
        List<DailyLogEntryView> entries = log.getEntries().stream()
                .map(entry -> new DailyLogEntryView(entry.getId(), entry.getActivityTitle(), entry.getSubjectName(),
                        entry.getDurationMinutes(), entry.getNotes()))
                .toList();
        return new DailyLogView(log.getId(), log.getStudent().getId(), log.getStudent().getPerson().getFullName(), log.getLogDate(),
                log.getStatus().name(), log.getDaySummary(), log.getCounselorFeedback(), log.getReviewedAt(), entries);
    }

    private StudyPlanView studyPlanView(StudyPlan plan) {
        List<StudyPlanItemView> items = new ArrayList<>();
        for (StudyPlanItem item : plan.getItems()) {
            StudyPlanProgress progress = planProgress.findByTenantIdAndStudyPlanItemId(plan.getTenant().getId(), item.getId()).orElse(null);
            items.add(new StudyPlanItemView(item.getId(), item.getSubjectName(), item.getActivityTitle(), item.getTargetMinutes(),
                    item.getPlannedDate(), item.getNotes(),
                    progress == null ? null : progress.getCompletedMinutes(),
                    progress == null ? null : progress.getCompletedAt(),
                    progress == null ? null : progress.getNote()));
        }
        return new StudyPlanView(plan.getId(), plan.getStudent().getId(), plan.getStudent().getPerson().getFullName(), plan.getTitle(),
                plan.getDescription(), plan.getStartDate(), plan.getEndDate(), plan.getStatus().name(), items);
    }

    private AdminReportView adminReportView(CounselorAdminReport report) {
        return new AdminReportView(report.getId(), report.getCounselorMembership().getId(),
                report.getCounselorMembership().getPerson().getFullName(), report.getTitle(), report.getBody(), report.getPeriodLabel(),
                report.getStatus().name(), report.getSentAt(), report.getCreatedAt());
    }

    private SeniorReportView seniorReportView(CounselorSeniorReport report) {
        return new SeniorReportView(report.getId(), report.getCounselorMembership().getId(),
                report.getCounselorMembership().getPerson().getFullName(), report.getReportYear(), report.getReportMonth(),
                report.getTitle(), report.getBody(), report.getAiAnalysis(), report.getStatus().name(), report.getFinalizedAt());
    }

    private static Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "Resource was not found.");
    }
    private static ApiException conflict(String code) { return new ApiException(HttpStatus.CONFLICT, code, "Conflict."); }
    private static ApiException badRequest(String code) { return new ApiException(HttpStatus.BAD_REQUEST, code, "Invalid request."); }

    public record AssignmentCommand(Long counselorMembershipId, String assignmentType, Long studentId, Long guardianId,
            LocalDate effectiveFrom, String note) {}
    public record SupervisionCommand(Long seniorMembershipId, Long counselorMembershipId, LocalDate effectiveFrom) {}
    public record DailyLogEntryCommand(String activityTitle, String subjectName, int durationMinutes, String notes) {}
    public record DailyLogCommand(LocalDate logDate, String daySummary, List<DailyLogEntryCommand> entries) {}
    public record StudyPlanItemCommand(String subjectName, String activityTitle, int targetMinutes, LocalDate plannedDate, String notes) {}
    public record StudyPlanCommand(Long studentId, String title, String description, LocalDate startDate, LocalDate endDate,
            List<StudyPlanItemCommand> items) {}
    public record ProgressCommand(int completedMinutes, String note) {}
    public record AdminReportCommand(String title, String body, String periodLabel) {}

    public record AssignmentView(Long id, Long counselorMembershipId, String counselorName, String assignmentType, Long targetId,
            String targetName, LocalDate effectiveFrom, LocalDate effectiveTo, String status, String note) {}
    public record SupervisionView(Long id, Long seniorMembershipId, String seniorName, Long counselorMembershipId, String counselorName,
            LocalDate effectiveFrom, LocalDate effectiveTo, String status) {}
    public record DailyLogEntryView(Long id, String activityTitle, String subjectName, int durationMinutes, String notes) {}
    public record DailyLogView(Long id, Long studentId, String studentName, LocalDate logDate, String status, String daySummary,
            String counselorFeedback, Instant reviewedAt, List<DailyLogEntryView> entries) {}
    public record StudyPlanItemView(Long id, String subjectName, String activityTitle, int targetMinutes, LocalDate plannedDate, String notes,
            Integer completedMinutes, Instant completedAt, String progressNote) {}
    public record StudyPlanView(Long id, Long studentId, String studentName, String title, String description, LocalDate startDate,
            LocalDate endDate, String status, List<StudyPlanItemView> items) {}
    public record AdminReportView(Long id, Long counselorMembershipId, String counselorName, String title, String body, String periodLabel,
            String status, Instant sentAt, Instant createdAt) {}
    public record SeniorReportView(Long id, Long counselorMembershipId, String counselorName, int reportYear, int reportMonth, String title,
            String body, String aiAnalysis, String status, Instant finalizedAt) {}
}
