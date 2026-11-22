package ir.orix.school.academic.domain;

import java.time.Instant;
import jakarta.persistence.*;
import ir.orix.school.organization.domain.Tenant;

@Entity @Table(name="program_course",schema="school")
public class ProgramCourse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tenant_id") private Tenant tenant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="program_id") private AcademicProgram program;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id") private Course course;
    @Column(name="sequence_number") private Integer sequenceNumber; @Column(nullable=false) private boolean required;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected ProgramCourse(){}
    public ProgramCourse(Tenant tenant,AcademicProgram program,Course course,Integer sequenceNumber,boolean required){this();this.tenant=tenant;this.program=program;this.course=course;this.sequenceNumber=sequenceNumber;this.required=required;}
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public AcademicProgram getProgram(){return program;} public Course getCourse(){return course;} public Integer getSequenceNumber(){return sequenceNumber;} public boolean isRequired(){return required;}
}
