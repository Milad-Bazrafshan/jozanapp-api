package ir.orix.school.academic.application;

import java.time.LocalDate;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.academic.domain.*;
import ir.orix.school.academic.persistence.*;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class AcademicService {
    private final TenantRepository tenants;
    private final AcademicPeriodRepository periods;
    private final AcademicProgramRepository programs;
    private final CourseRepository courses;
    private final ProgramCourseRepository programCourses;
    private final AuditService audit;

    public AcademicService(TenantRepository tenants, AcademicPeriodRepository periods, AcademicProgramRepository programs, CourseRepository courses, ProgramCourseRepository programCourses, AuditService audit) {
        this.tenants = tenants;
        this.periods = periods;
        this.programs = programs;
        this.courses = courses;
        this.programCourses = programCourses;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Catalog catalog(Long tenantId) {
        return new Catalog(periods.findAllByTenantIdOrderByStartsOnDesc(tenantId).stream().map(AcademicService::periodView).toList(), programs.findAllByTenantIdOrderByName(tenantId).stream().map(AcademicService::programView).toList(), courses.findAllByTenantIdOrderByTitle(tenantId).stream().map(AcademicService::courseView).toList(), programCourses.findAllByTenantIdOrderByProgramNameAscSequenceNumberAsc(tenantId).stream().map(AcademicService::linkView).toList());
    }

    @Transactional
    public PeriodView createPeriod(Long tenantId, Long actorId, PeriodCommand c) {
        if (c.endsOn().isBefore(c.startsOn()))
            throw invalid("INVALID_PERIOD_DATES", "End date must be on or after start date.");
        if (periods.existsByTenantIdAndCodeIgnoreCase(tenantId, c.code())) throw exists("ACADEMIC_PERIOD_CODE_EXISTS");
        AcademicPeriod value = periods.save(new AcademicPeriod(tenant(tenantId), c.code(), c.name(), c.type(), c.startsOn(), c.endsOn()));
        audit.record(tenantId, actorId, "ACADEMIC_PERIOD_CREATED", "ACADEMIC_PERIOD", value.getId(), Map.of("code", value.getCode()));
        return periodView(value);
    }

    @Transactional
    public ProgramView createProgram(Long tenantId, Long actorId, ProgramCommand c) {
        if (programs.existsByTenantIdAndCodeIgnoreCase(tenantId, c.code()))
            throw exists("ACADEMIC_PROGRAM_CODE_EXISTS");
        AcademicProgram value = programs.save(new AcademicProgram(tenant(tenantId), c.code(), c.name(), c.type(), c.description()));
        audit.record(tenantId, actorId, "ACADEMIC_PROGRAM_CREATED", "ACADEMIC_PROGRAM", value.getId(), Map.of("code", value.getCode()));
        return programView(value);
    }

    @Transactional
    public CourseView createCourse(Long tenantId, Long actorId, CourseCommand c) {
        if (c.durationMinutes() != null && c.durationMinutes() <= 0)
            throw invalid("INVALID_COURSE_DURATION", "Duration must be positive.");
        if (courses.existsByTenantIdAndCodeIgnoreCase(tenantId, c.code())) throw exists("COURSE_CODE_EXISTS");
        Course value = courses.save(new Course(tenant(tenantId), c.code(), c.title(), c.description(), c.durationMinutes()));
        audit.record(tenantId, actorId, "COURSE_CREATED", "COURSE", value.getId(), Map.of("code", value.getCode()));
        return courseView(value);
    }

    @Transactional
    public ProgramCourseView attachCourse(Long tenantId, Long actorId, AttachCommand c) {
        AcademicProgram program = programs.findByIdAndTenantId(c.programId(), tenantId).orElseThrow(notFound("ACADEMIC_PROGRAM_NOT_FOUND"));
        Course course = courses.findByIdAndTenantId(c.courseId(), tenantId).orElseThrow(notFound("COURSE_NOT_FOUND"));
        if (programCourses.existsByProgramIdAndCourseId(program.getId(), course.getId()))
            throw exists("PROGRAM_COURSE_EXISTS");
        ProgramCourse link = programCourses.save(new ProgramCourse(tenant(tenantId), program, course, c.sequenceNumber(), c.required()));
        audit.record(tenantId, actorId, "COURSE_ATTACHED_TO_PROGRAM", "ACADEMIC_PROGRAM", program.getId(), Map.of("courseId", course.getId().toString()));
        return linkView(link);
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND"));
    }

    private static ApiException exists(String code) {
        return new ApiException(HttpStatus.CONFLICT, code, "The value already exists in this tenant.");
    }

    private static ApiException invalid(String code, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, detail);
    }

    private static java.util.function.Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested academic item was not found.");
    }

    private static PeriodView periodView(AcademicPeriod p) {
        return new PeriodView(p.getId(), p.getCode(), p.getName(), p.getType().name(), p.getStartsOn(), p.getEndsOn(), p.getStatus().name());
    }

    private static ProgramView programView(AcademicProgram p) {
        return new ProgramView(p.getId(), p.getCode(), p.getName(), p.getType().name(), p.getDescription(), p.getStatus().name());
    }

    private static CourseView courseView(Course c) {
        return new CourseView(c.getId(), c.getCode(), c.getTitle(), c.getDescription(), c.getDurationMinutes(), c.getStatus().name());
    }

    private static ProgramCourseView linkView(ProgramCourse p) {
        return new ProgramCourseView(p.getId(), p.getProgram().getId(), p.getProgram().getName(), p.getCourse().getId(), p.getCourse().getTitle(), p.getSequenceNumber(), p.isRequired());
    }

    public record PeriodCommand(String code, String name, AcademicPeriod.Type type, LocalDate startsOn,
                                LocalDate endsOn) {
    }

    public record ProgramCommand(String code, String name, AcademicProgram.Type type, String description) {
    }

    public record CourseCommand(String code, String title, String description, Integer durationMinutes) {
    }

    public record AttachCommand(Long programId, Long courseId, Integer sequenceNumber, boolean required) {
    }

    public record PeriodView(Long id, String code, String name, String type, LocalDate startsOn, LocalDate endsOn,
                             String status) {
    }

    public record ProgramView(Long id, String code, String name, String type, String description, String status) {
    }

    public record CourseView(Long id, String code, String title, String description, Integer durationMinutes,
                             String status) {
    }

    public record ProgramCourseView(Long id, Long programId, String programName, Long courseId, String courseTitle,
                                    Integer sequenceNumber, boolean required) {
    }

    public record Catalog(List<PeriodView> periods, List<ProgramView> programs, List<CourseView> courses,
                          List<ProgramCourseView> programCourses) {
    }
}
