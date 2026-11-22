package ir.orix.school.scheduling.domain;

import java.time.*; import java.util.*; import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.people.domain.InstructorProfile; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="class_session",schema="school")
public class ClassSession extends AuditedEntity {
 public enum Status{PLANNED,CONFIRMED,IN_PROGRESS,COMPLETED,CANCELLED,POSTPONED,RESCHEDULED}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_section_id") private ClassSection classSection;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="schedule_rule_id") private ScheduleRule scheduleRule;
 @Column(name="session_date",nullable=false) private LocalDate sessionDate; @Column(name="planned_start_at",nullable=false) private Instant plannedStartAt; @Column(name="planned_end_at",nullable=false) private Instant plannedEndAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="location_id") private AcademicLocation location;
 @ManyToMany @JoinTable(name="class_session_instructor",schema="school",joinColumns=@JoinColumn(name="class_session_id"),inverseJoinColumns=@JoinColumn(name="instructor_id")) private Set<InstructorProfile> instructors=new LinkedHashSet<>();
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status; @Column(name="manually_overridden",nullable=false) private boolean manuallyOverridden; @Column(name="change_reason",length=500) private String changeReason;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="replacement_session_id") private ClassSession replacementSession;
 @Column(name="meeting_id",length=120) private String meetingId;
 protected ClassSession(){}
 public ClassSession(Tenant tenant,ClassSection section,ScheduleRule rule,LocalDate date,Instant start,Instant end,AcademicLocation location,Collection<InstructorProfile> instructors){this.tenant=tenant;this.classSection=section;this.scheduleRule=rule;this.sessionDate=date;this.plannedStartAt=start;this.plannedEndAt=end;this.location=location;this.instructors.addAll(instructors);this.status=Status.PLANNED;}
 public void cancel(String reason){this.status=Status.CANCELLED;this.changeReason=reason;this.manuallyOverridden=true;}
 public void replacedBy(ClassSession replacement,String reason){this.status=Status.RESCHEDULED;this.replacementSession=replacement;this.changeReason=reason;this.manuallyOverridden=true;}
 public void complete(){if(status==Status.PLANNED||status==Status.CONFIRMED||status==Status.IN_PROGRESS)this.status=Status.COMPLETED;}
 public void attachMeeting(String meetingId){this.meetingId=meetingId;}
 public ClassSection getClassSection(){return classSection;} public ScheduleRule getScheduleRule(){return scheduleRule;} public LocalDate getSessionDate(){return sessionDate;} public Instant getPlannedStartAt(){return plannedStartAt;} public Instant getPlannedEndAt(){return plannedEndAt;} public AcademicLocation getLocation(){return location;} public Set<InstructorProfile> getInstructors(){return Collections.unmodifiableSet(instructors);} public Status getStatus(){return status;} public boolean isManuallyOverridden(){return manuallyOverridden;} public String getChangeReason(){return changeReason;} public ClassSession getReplacementSession(){return replacementSession;} public String getMeetingId(){return meetingId;}
}
