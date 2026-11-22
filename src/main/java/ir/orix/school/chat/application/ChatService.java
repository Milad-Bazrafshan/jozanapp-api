package ir.orix.school.chat.application;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.chat.domain.ChatMessage;
import ir.orix.school.chat.domain.ChatRoom;
import ir.orix.school.chat.persistence.ChatMessageRepository;
import ir.orix.school.chat.persistence.ChatRoomRepository;
import ir.orix.school.classroom.domain.ClassInstructor;
import ir.orix.school.classroom.persistence.ClassInstructorRepository;
import ir.orix.school.counseling.application.CounselingAccess;
import ir.orix.school.counseling.domain.CounselorAssignment;
import ir.orix.school.counseling.persistence.CounselorAssignmentRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.files.domain.StoredFile;
import ir.orix.school.files.persistence.StoredFileRepository;
import ir.orix.school.notification.application.NotificationService;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.people.domain.InstructorProfile;
import ir.orix.school.people.domain.StudentGuardian;
import ir.orix.school.people.persistence.InstructorProfileRepository;
import ir.orix.school.people.persistence.StudentGuardianRepository;
import ir.orix.school.shared.security.ScopeAuthorizer;
import ir.orix.school.shared.web.ApiException;

@Service
public class ChatService {
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final ChatRoomRepository rooms;
    private final ChatMessageRepository messages;
    private final EnrollmentRepository enrollments;
    private final ClassInstructorRepository classInstructors;
    private final InstructorProfileRepository instructors;
    private final StudentGuardianRepository guardians;
    private final CounselorAssignmentRepository counselorAssignments;
    private final CounselingAccess counselingAccess;
    private final StoredFileRepository files;
    private final PeopleService people;
    private final NotificationService notifications;
    private final ScopeAuthorizer authorizer;
    private final SimpMessagingTemplate broker;
    private final Clock clock;

    public ChatService(TenantRepository tenants, TenantMembershipRepository memberships, ChatRoomRepository rooms,
            ChatMessageRepository messages, EnrollmentRepository enrollments, ClassInstructorRepository classInstructors,
            InstructorProfileRepository instructors, StudentGuardianRepository guardians,
            CounselorAssignmentRepository counselorAssignments, CounselingAccess counselingAccess,
            StoredFileRepository files, PeopleService people, NotificationService notifications,
            ScopeAuthorizer authorizer, SimpMessagingTemplate broker, Clock clock) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.rooms = rooms;
        this.messages = messages;
        this.enrollments = enrollments;
        this.classInstructors = classInstructors;
        this.instructors = instructors;
        this.guardians = guardians;
        this.counselorAssignments = counselorAssignments;
        this.counselingAccess = counselingAccess;
        this.files = files;
        this.people = people;
        this.notifications = notifications;
        this.authorizer = authorizer;
        this.broker = broker;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RoomView> rooms(Long tenantId, Long membershipId) {
        boolean moderate = authorizer.hasTenantPermission("CHAT_MODERATE");
        return rooms.findMine(tenantId, membershipId).stream()
                .filter(room -> moderate || canAccessRoom(room, membershipId))
                .map(this::roomView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PartnerView> partners(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        LinkedHashMap<String, PartnerView> unique = new LinkedHashMap<>();
        InstructorProfile instructor = instructors.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId()).orElse(null);
        if (instructor != null) {
            for (ClassInstructor assignment : classInstructors.findAllByTenantIdAndInstructorIdAndStatus(tenantId, instructor.getId(), ClassInstructor.Status.ACTIVE)) {
                for (Enrollment enrollment : enrollments.findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(tenantId, assignment.getClassSection().getId())) {
                    if (enrollment.getStatus() != Enrollment.Status.ACTIVE) continue;
                    addGuardianPartners(tenantId, membershipId, instructor.getPerson().getFullName(), assignment.getClassSection().getName(), enrollment, unique);
                }
            }
        }
        if (counselingAccess.counselorTier(membershipId).isPresent()) {
            addCounselorStudentPartners(tenantId, membershipId, membership.getPerson().getFullName(), unique);
        }
        PeopleService.MyProfiles profiles = people.myProfiles(tenantId, membershipId);
        String selfName = membership.getPerson().getFullName();
        if (profiles.student() != null) {
            addInstructorPartners(tenantId, membershipId, selfName, profiles.student().id(), unique);
            addStudentCounselorPartner(tenantId, profiles.student().id(), membershipId, unique);
        }
        for (PeopleService.StudentView child : profiles.children()) {
            people.requireChildAcademicAccess(tenantId, membershipId, child.id());
            addInstructorPartners(tenantId, membershipId, selfName, child.id(), unique);
        }
        return List.copyOf(unique.values());
    }

    @Transactional
    public RoomView open(Long tenantId, Long membershipId, Long studentId, Long instructorMembershipId, Long guardianMembershipId) {
        Long guardianId = guardianMembershipId != null ? guardianMembershipId : membershipId;
        Long instructorId = instructorMembershipId != null ? instructorMembershipId : membershipId;
        if (!membershipId.equals(guardianId) && !membershipId.equals(instructorId) && !authorizer.hasTenantPermission("CHAT_MODERATE")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED", "You cannot open this conversation.");
        }
        people.requireChildAcademicAccess(tenantId, guardianId, studentId);
        TenantMembership instructorMembership = memberships.findByIdAndTenantId(instructorId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Instructor membership was not found."));
        InstructorProfile instructor = instructors.findByTenantIdAndPersonId(tenantId, instructorMembership.getPerson().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INSTRUCTOR_NOT_FOUND", "Instructor was not found."));
        Enrollment enrollment = enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .filter(item -> item.getStatus() == Enrollment.Status.ACTIVE)
                .filter(item -> classInstructors.existsByClassSectionIdAndInstructorIdAndStatus(item.getClassSection().getId(), instructor.getId(), ClassInstructor.Status.ACTIVE))
                .findFirst().orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "CHAT_NOT_ALLOWED", "No shared class between guardian and instructor."));
        return roomView(rooms.findByTenantIdAndGuardianMembershipIdAndInstructorMembershipIdAndStudentId(tenantId, guardianId, instructorId, studentId)
                .orElseGet(() -> rooms.save(new ChatRoom(tenants.findById(tenantId).orElseThrow(), enrollment.getStudent(), guardianId, instructorId, enrollment.getClassSection()))));
    }

    @Transactional
    public RoomView openCounselor(Long tenantId, Long membershipId, Long studentId, Long counselorMembershipId) {
        Long counselorId = counselorMembershipId != null ? counselorMembershipId : membershipId;
        if (!counselingAccess.isActiveStudentCounselor(tenantId, counselorId, studentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COUNSELOR_CHAT_DENIED", "No counseling assignment for this conversation.");
        }
        Long studentMembershipId = resolveStudentMembershipId(tenantId, studentId);
        boolean isStudent = membershipId.equals(studentMembershipId);
        boolean isCounselor = membershipId.equals(counselorId);
        if (!isStudent && !isCounselor && !authorizer.hasTenantPermission("CHAT_MODERATE")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED", "You cannot open this conversation.");
        }
        var student = enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .findFirst()
                .map(Enrollment::getStudent)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "Student was not found."));
        return roomView(rooms.findByTenantIdAndStudentIdAndCounselorMembershipIdAndRoomType(
                tenantId, studentId, counselorId, ChatRoom.RoomType.COUNSELOR_STUDENT)
                .orElseGet(() -> rooms.save(ChatRoom.counselorStudent(tenants.findById(tenantId).orElseThrow(), student, studentMembershipId, counselorId))));
    }

    @Transactional(readOnly = true)
    public List<MessageView> history(Long tenantId, Long membershipId, Long roomId) {
        ChatRoom room = requireRoom(tenantId, membershipId, roomId);
        return messages.findAllByRoomIdOrderByCreatedAtAsc(room.getId()).stream().map(this::messageView).toList();
    }

    @Transactional
    public MessageView send(Long tenantId, Long membershipId, Long roomId, String body, Long fileId) {
        ChatRoom room = requireRoom(tenantId, membershipId, roomId);
        StoredFile file = fileId == null ? null : files.findByIdAndTenantId(fileId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "File was not found."));
        ChatMessage message = messages.save(new ChatMessage(room.getTenant(), room, membershipId, body.strip(), file));
        MessageView view = messageView(message);
        broker.convertAndSend("/topic/chat." + room.getId(), view);
        Long other = otherParticipant(room, membershipId);
        if (other != null) {
            notifications.notifyMembership(tenantId, other, "پیام جدید", body.length() > 80 ? body.substring(0, 80) : body);
        }
        return view;
    }

    @Transactional
    public void markRead(Long tenantId, Long membershipId, Long roomId) {
        ChatRoom room = requireRoom(tenantId, membershipId, roomId);
        Instant now = Instant.now(clock);
        messages.findAllByRoomIdOrderByCreatedAtAsc(room.getId()).stream()
                .filter(item -> !item.getSenderMembershipId().equals(membershipId))
                .forEach(item -> item.markRead(now));
    }

    private void addCounselorStudentPartners(Long tenantId, Long counselorMembershipId, String counselorName,
            LinkedHashMap<String, PartnerView> unique) {
        for (CounselorAssignment assignment : counselorAssignments.findAllByTenantIdAndCounselorMembershipIdAndStatusOrderByCreatedAtDesc(
                tenantId, counselorMembershipId, CounselorAssignment.Status.ACTIVE)) {
            if (assignment.getAssignmentType() != CounselorAssignment.AssignmentType.STUDENT || assignment.getStudent() == null) {
                continue;
            }
            var student = assignment.getStudent();
            unique.put("counselor:" + student.getId(), new PartnerView(student.getId(), student.getPerson().getFullName(),
                    "مشاوره", null, null, null, null, "COUNSELOR", counselorMembershipId, counselorName));
        }
    }

    private void addStudentCounselorPartner(Long tenantId, Long studentId, Long studentMembershipId,
            LinkedHashMap<String, PartnerView> unique) {
        counselorAssignments.findFirstByTenantIdAndStudentIdAndAssignmentTypeAndStatusAndEffectiveToIsNullOrderByEffectiveFromDesc(
                tenantId, studentId, CounselorAssignment.AssignmentType.STUDENT, CounselorAssignment.Status.ACTIVE)
                .ifPresent(assignment -> {
                    TenantMembership counselor = assignment.getCounselorMembership();
                    unique.put("student-counselor:" + counselor.getId(), new PartnerView(studentId,
                            assignment.getStudent().getPerson().getFullName(), "مشاوره", studentMembershipId, null,
                            counselor.getId(), counselor.getPerson().getFullName(), "COUNSELOR", counselor.getId(),
                            counselor.getPerson().getFullName()));
                });
    }

    private Long resolveStudentMembershipId(Long tenantId, Long studentId) {
        return enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId).stream()
                .findFirst()
                .flatMap(enrollment -> memberships.findByTenantIdAndPersonId(tenantId, enrollment.getStudent().getPerson().getId()))
                .map(TenantMembership::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_MEMBERSHIP_NOT_FOUND", "Student login was not found."));
    }

    private void addGuardianPartners(Long tenantId, Long instructorMembershipId, String instructorName, String className,
            Enrollment enrollment, LinkedHashMap<String, PartnerView> unique) {
        for (StudentGuardian link : guardians.findAllByTenantIdAndStudentId(tenantId, enrollment.getStudent().getId())) {
            if (!link.hasAcademicAccess()) continue;
            TenantMembership guardianMembership = memberships.findByTenantIdAndPersonId(tenantId, link.getGuardian().getPerson().getId()).orElse(null);
            if (guardianMembership == null) continue;
            put(unique, new PartnerView(enrollment.getStudent().getId(), enrollment.getStudent().getPerson().getFullName(),
                    className, guardianMembership.getId(), link.getGuardian().getPerson().getFullName(), instructorMembershipId,
                    instructorName, "INSTRUCTOR", null, null));
        }
    }

    private void addInstructorPartners(Long tenantId, Long guardianMembershipId, String guardianName, Long studentId,
            LinkedHashMap<String, PartnerView> unique) {
        for (Enrollment enrollment : enrollments.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId)) {
            if (enrollment.getStatus() != Enrollment.Status.ACTIVE) continue;
            for (ClassInstructor assignment : classInstructors.findAllByTenantIdOrderByCreatedAtDesc(tenantId)) {
                if (assignment.getStatus() != ClassInstructor.Status.ACTIVE) continue;
                if (!assignment.getClassSection().getId().equals(enrollment.getClassSection().getId())) continue;
                TenantMembership instructorMembership = memberships.findByTenantIdAndPersonId(tenantId, assignment.getInstructor().getPerson().getId()).orElse(null);
                if (instructorMembership == null) continue;
                put(unique, new PartnerView(enrollment.getStudent().getId(), enrollment.getStudent().getPerson().getFullName(),
                        enrollment.getClassSection().getName(), guardianMembershipId, guardianName,
                        instructorMembership.getId(), assignment.getInstructor().getPerson().getFullName(), "INSTRUCTOR", null, null));
            }
        }
    }

    private static void put(LinkedHashMap<String, PartnerView> unique, PartnerView view) {
        unique.put(view.studentId() + ":" + Objects.toString(view.guardianMembershipId(), "") + ":" + Objects.toString(view.instructorMembershipId(), ""), view);
    }

    private boolean canAccessRoom(ChatRoom room, Long membershipId) {
        if (room.getRoomType() == ChatRoom.RoomType.COUNSELOR_STUDENT) {
            return membershipId.equals(room.getCounselorMembershipId()) || membershipId.equals(room.getStudentMembershipId());
        }
        return membershipId.equals(room.getGuardianMembershipId()) || membershipId.equals(room.getInstructorMembershipId());
    }

    private Long otherParticipant(ChatRoom room, Long membershipId) {
        if (room.getRoomType() == ChatRoom.RoomType.COUNSELOR_STUDENT) {
            return membershipId.equals(room.getCounselorMembershipId()) ? room.getStudentMembershipId() : room.getCounselorMembershipId();
        }
        return membershipId.equals(room.getGuardianMembershipId()) ? room.getInstructorMembershipId() : room.getGuardianMembershipId();
    }

    private ChatRoom requireRoom(Long tenantId, Long membershipId, Long roomId) {
        ChatRoom room = rooms.findByIdAndTenantId(roomId, tenantId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "Chat room was not found."));
        if (authorizer.hasTenantPermission("CHAT_MODERATE") || canAccessRoom(room, membershipId)) {
            return room;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED", "You cannot access this conversation.");
    }

    private RoomView roomView(ChatRoom room) {
        String className = room.getClassSection() == null ? "مشاوره" : room.getClassSection().getName();
        return new RoomView(room.getId(), room.getStudent().getId(), room.getStudent().getPerson().getFullName(), className,
                room.getGuardianMembershipId(), room.getInstructorMembershipId(), room.getRoomType().name(),
                room.getCounselorMembershipId(), room.getStudentMembershipId());
    }

    private MessageView messageView(ChatMessage message) {
        return new MessageView(message.getId(), message.getRoom().getId(), message.getSenderMembershipId(), message.getBody(),
                message.getFile() == null ? null : message.getFile().getId(), message.getReadAt(), message.getCreatedAt());
    }

    public record RoomView(Long id, Long studentId, String studentName, String className, Long guardianMembershipId,
            Long instructorMembershipId, String roomType, Long counselorMembershipId, Long studentMembershipId) {}

    public record PartnerView(Long studentId, String studentName, String className, Long guardianMembershipId, String guardianName,
            Long instructorMembershipId, String instructorName, String partnerType, Long counselorMembershipId, String counselorName) {}

    public record MessageView(Long id, Long roomId, Long senderMembershipId, String body, Long fileId, Instant readAt, Instant createdAt) {}
}
