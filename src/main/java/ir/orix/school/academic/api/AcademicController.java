package ir.orix.school.academic.api;

import java.time.LocalDate; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import ir.orix.school.academic.application.AcademicService; import ir.orix.school.academic.domain.*; import ir.orix.school.shared.security.CurrentUser;

@RestController @RequestMapping("/api/v1/academic")
public class AcademicController {
 private final AcademicService service; public AcademicController(AcademicService service){this.service=service;}
 @GetMapping @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ACADEMIC_READ')") public AcademicService.Catalog catalog(){return service.catalog(CurrentUser.tenantId());}
 @PostMapping("/periods") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ACADEMIC_WRITE')") public AcademicService.PeriodView period(@Valid @RequestBody PeriodRequest r){return service.createPeriod(CurrentUser.tenantId(),CurrentUser.accountId(),new AcademicService.PeriodCommand(r.code(),r.name(),r.type(),r.startsOn(),r.endsOn()));}
 @PostMapping("/programs") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ACADEMIC_WRITE')") public AcademicService.ProgramView program(@Valid @RequestBody ProgramRequest r){return service.createProgram(CurrentUser.tenantId(),CurrentUser.accountId(),new AcademicService.ProgramCommand(r.code(),r.name(),r.type(),r.description()));}
 @PostMapping("/courses") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ACADEMIC_WRITE')") public AcademicService.CourseView course(@Valid @RequestBody CourseRequest r){return service.createCourse(CurrentUser.tenantId(),CurrentUser.accountId(),new AcademicService.CourseCommand(r.code(),r.title(),r.description(),r.durationMinutes()));}
 @PostMapping("/program-courses") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('ACADEMIC_WRITE')") public AcademicService.ProgramCourseView attach(@Valid @RequestBody AttachRequest r){return service.attachCourse(CurrentUser.tenantId(),CurrentUser.accountId(),new AcademicService.AttachCommand(r.programId(),r.courseId(),r.sequenceNumber(),r.required()));}
 public record PeriodRequest(@NotBlank @Size(max=50) String code,@NotBlank @Size(max=150) String name,@NotNull AcademicPeriod.Type type,@NotNull LocalDate startsOn,@NotNull LocalDate endsOn){}
 public record ProgramRequest(@NotBlank @Size(max=50) String code,@NotBlank @Size(max=180) String name,@NotNull AcademicProgram.Type type,@Size(max=700) String description){}
 public record CourseRequest(@NotBlank @Size(max=50) String code,@NotBlank @Size(max=200) String title,@Size(max=1000) String description,@Positive Integer durationMinutes){}
 public record AttachRequest(@NotNull Long programId,@NotNull Long courseId,@Positive Integer sequenceNumber,boolean required){}
}
