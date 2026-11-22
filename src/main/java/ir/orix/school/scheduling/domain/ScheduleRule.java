package ir.orix.school.scheduling.domain;

import java.time.*; import java.util.*; import jakarta.persistence.*;
import ir.orix.school.classroom.domain.ClassSection; import ir.orix.school.organization.domain.Tenant; import ir.orix.school.people.domain.InstructorProfile; import ir.orix.school.shared.persistence.AuditedEntity;

@Entity @Table(name="schedule_rule",schema="school")
public class ScheduleRule extends AuditedEntity {
 public enum RecurrenceType{ONCE,WEEKLY} public enum Status{ACTIVE,INACTIVE}
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="class_section_id") private ClassSection classSection;
 @Column(nullable=false,length=60) private String code; @Enumerated(EnumType.STRING) @Column(name="recurrence_type",nullable=false,length=20) private RecurrenceType recurrenceType;
 @Column(name="starts_on",nullable=false) private LocalDate startsOn; @Column(name="ends_on",nullable=false) private LocalDate endsOn;
 @Enumerated(EnumType.STRING) @Column(name="day_of_week",length=12) private DayOfWeek dayOfWeek;
 @Column(name="starts_at",nullable=false) private LocalTime startsAt; @Column(name="ends_at",nullable=false) private LocalTime endsAt;
 @Column(name="time_zone",nullable=false,length=60) private String timeZone; @Column(name="interval_weeks",nullable=false) private int intervalWeeks;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="location_id") private AcademicLocation location;
 @ManyToMany @JoinTable(name="schedule_rule_instructor",schema="school",joinColumns=@JoinColumn(name="schedule_rule_id"),inverseJoinColumns=@JoinColumn(name="instructor_id")) private Set<InstructorProfile> instructors=new LinkedHashSet<>();
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Status status;
 protected ScheduleRule(){}
 public ScheduleRule(Tenant tenant,ClassSection section,String code,RecurrenceType recurrence,LocalDate from,LocalDate to,DayOfWeek day,LocalTime start,LocalTime end,String zone,int interval,AcademicLocation location,Collection<InstructorProfile> instructors){this.tenant=tenant;this.classSection=section;this.code=code.strip().toUpperCase();this.recurrenceType=recurrence;this.startsOn=from;this.endsOn=to;this.dayOfWeek=day;this.startsAt=start;this.endsAt=end;this.timeZone=zone;this.intervalWeeks=interval;this.location=location;this.instructors.addAll(instructors);this.status=Status.ACTIVE;}
 public ClassSection getClassSection(){return classSection;} public String getCode(){return code;} public RecurrenceType getRecurrenceType(){return recurrenceType;} public LocalDate getStartsOn(){return startsOn;} public LocalDate getEndsOn(){return endsOn;} public DayOfWeek getDayOfWeek(){return dayOfWeek;} public LocalTime getStartsAt(){return startsAt;} public LocalTime getEndsAt(){return endsAt;} public String getTimeZone(){return timeZone;} public int getIntervalWeeks(){return intervalWeeks;} public AcademicLocation getLocation(){return location;} public Set<InstructorProfile> getInstructors(){return Collections.unmodifiableSet(instructors);} public Status getStatus(){return status;}
}
