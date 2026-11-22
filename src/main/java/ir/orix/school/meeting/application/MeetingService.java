package ir.orix.school.meeting.application;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.instructor.application.InstructorPortalService;
import ir.orix.school.meeting.domain.MeetingRoom;
import ir.orix.school.meeting.persistence.MeetingRoomRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.scheduling.domain.ClassSession;
import ir.orix.school.scheduling.persistence.ClassSessionRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class MeetingService {
    private final TenantRepository tenants;
    private final ClassSessionRepository sessions;
    private final MeetingRoomRepository rooms;
    private final List<MeetingProvider> providers;
    private final EnrollmentRepository enrollments;
    private final InstructorPortalService instructorPortal;
    private final PeopleService people;

    public MeetingService(TenantRepository tenants, ClassSessionRepository sessions, MeetingRoomRepository rooms,
            List<MeetingProvider> providers, EnrollmentRepository enrollments, InstructorPortalService instructorPortal,
            PeopleService people) {
        this.tenants = tenants; this.sessions = sessions; this.rooms = rooms; this.providers = providers;
        this.enrollments = enrollments; this.instructorPortal = instructorPortal; this.people = people;
    }

    @Transactional
    public JoinView join(Long tenantId, Long membershipId, Long sessionId, Role role) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        if (!tenant.isVirtualClassEnabled()) throw new ApiException(HttpStatus.NOT_FOUND, "VIRTUAL_CLASS_DISABLED", "Virtual class is disabled.");
        ClassSession session = sessions.findByIdAndTenantId(sessionId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session was not found."));
        if (role == Role.INSTRUCTOR) instructorPortal.requireSection(tenantId, membershipId, session.getClassSection().getId());
        else ensureStudent(tenantId, membershipId, session);
        MeetingRoom room = rooms.findByClassSessionId(sessionId).orElseGet(() -> create(tenant, session));
        return new JoinView(room.getId(), room.getProvider().name(), role == Role.INSTRUCTOR ? room.getInstructorJoinUrl() : room.getStudentJoinUrl());
    }

    private MeetingRoom create(Tenant tenant, ClassSession session) {
        String code = tenant.getMeetingProvider() == null ? "GENERIC_LINK" : tenant.getMeetingProvider();
        MeetingProvider provider = providers.stream().filter(item -> item.code().equalsIgnoreCase(code)).findFirst()
                .orElseGet(() -> providers.stream().filter(item -> "GENERIC_LINK".equals(item.code())).findFirst().orElseThrow());
        MeetingProvider.Created created = provider.create(tenant.getMeetingBaseUrl(), tenant.getMeetingSecret(),
                session.getClassSection().getName(), session.getId().toString());
        session.attachMeeting(created.externalId());
        return rooms.save(new MeetingRoom(tenant, session, MeetingRoom.Provider.valueOf(provider.code()), created.externalId(),
                created.instructorJoinUrl(), created.studentJoinUrl()));
    }

    private void ensureStudent(Long tenantId, Long membershipId, ClassSession session) {
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        Long studentId = profiles.student() == null ? null : profiles.student().id();
        if (studentId == null && !profiles.children().isEmpty()) studentId = profiles.children().get(0).id();
        if (studentId == null) throw new ApiException(HttpStatus.FORBIDDEN, "STUDENT_REQUIRED", "Student access is required.");
        if (profiles.student() == null) people.requireChildAcademicAccess(tenantId, membershipId, studentId);
        Long finalStudentId = studentId;
        boolean enrolled = enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, finalStudentId).stream()
                .anyMatch(item -> item.getClassSection().getId().equals(session.getClassSection().getId()) && item.getStatus() == Enrollment.Status.ACTIVE);
        if (!enrolled) throw new ApiException(HttpStatus.FORBIDDEN, "MEETING_ACCESS_DENIED", "Student is not enrolled in this class.");
    }

    public enum Role { INSTRUCTOR, STUDENT }
    public record JoinView(Long meetingId, String provider, String joinUrl) {}
}
