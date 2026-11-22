package ir.orix.school.meeting.domain;

import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.scheduling.domain.ClassSession;
import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name = "meeting_room", schema = "school")
public class MeetingRoom extends AuditedEntity {
    public enum Provider { BIGBLUEBUTTON, ADOBE_CONNECT, GENERIC_LINK }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id") private Tenant tenant;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "class_session_id") private ClassSession classSession;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Provider provider;
    @Column(name = "external_id", length = 160) private String externalId;
    @Column(name = "instructor_join_url", nullable = false, length = 500) private String instructorJoinUrl;
    @Column(name = "student_join_url", nullable = false, length = 500) private String studentJoinUrl;
    protected MeetingRoom() {}
    public MeetingRoom(Tenant tenant, ClassSession session, Provider provider, String externalId, String instructorJoinUrl, String studentJoinUrl) {
        this.tenant = tenant; this.classSession = session; this.provider = provider; this.externalId = externalId;
        this.instructorJoinUrl = instructorJoinUrl; this.studentJoinUrl = studentJoinUrl;
    }
    public ClassSession getClassSession() { return classSession; } public Provider getProvider() { return provider; }
    public String getExternalId() { return externalId; } public String getInstructorJoinUrl() { return instructorJoinUrl; }
    public String getStudentJoinUrl() { return studentJoinUrl; }
}
