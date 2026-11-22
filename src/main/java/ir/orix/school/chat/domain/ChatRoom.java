package ir.orix.school.chat.domain;

import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity
@Table(name = "chat_room", schema = "school")
public class ChatRoom extends AuditedEntity {
    public enum RoomType {INSTRUCTOR_GUARDIAN, COUNSELOR_STUDENT}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = true, length = 40)
    private RoomType roomType = RoomType.INSTRUCTOR_GUARDIAN;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private StudentProfile student;
    @Column(name = "guardian_membership_id")
    private Long guardianMembershipId;
    @Column(name = "instructor_membership_id")
    private Long instructorMembershipId;
    @Column(name = "counselor_membership_id")
    private Long counselorMembershipId;
    @Column(name = "student_membership_id")
    private Long studentMembershipId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id")
    private ClassSection classSection;

    protected ChatRoom() {
    }

    public ChatRoom(Tenant tenant, StudentProfile student, Long guardianMembershipId, Long instructorMembershipId, ClassSection classSection) {
        this.tenant = tenant;
        this.roomType = RoomType.INSTRUCTOR_GUARDIAN;
        this.student = student;
        this.guardianMembershipId = guardianMembershipId;
        this.instructorMembershipId = instructorMembershipId;
        this.classSection = classSection;
    }

    public static ChatRoom counselorStudent(Tenant tenant, StudentProfile student, Long studentMembershipId, Long counselorMembershipId) {
        ChatRoom room = new ChatRoom();
        room.tenant = tenant;
        room.roomType = RoomType.COUNSELOR_STUDENT;
        room.student = student;
        room.studentMembershipId = studentMembershipId;
        room.counselorMembershipId = counselorMembershipId;
        return room;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public Long getGuardianMembershipId() {
        return guardianMembershipId;
    }

    public Long getInstructorMembershipId() {
        return instructorMembershipId;
    }

    public Long getCounselorMembershipId() {
        return counselorMembershipId;
    }

    public Long getStudentMembershipId() {
        return studentMembershipId;
    }

    public ClassSection getClassSection() {
        return classSection;
    }
}
