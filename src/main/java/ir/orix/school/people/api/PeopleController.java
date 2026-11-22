package ir.orix.school.people.api;

import java.time.LocalDate;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
import ir.orix.school.people.application.PeopleService; import ir.orix.school.people.domain.StudentGuardian; import ir.orix.school.shared.security.CurrentUser;

@RestController @RequestMapping("/api/v1/people")
public class PeopleController {
 private final PeopleService service; public PeopleController(PeopleService service){this.service=service;}
 @GetMapping @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_READ')") public PeopleService.Directory directory(){return service.directory(CurrentUser.tenantId());}
 @GetMapping("/me") public PeopleService.MyProfiles me(){return service.myProfiles(CurrentUser.tenantId(),CurrentUser.membershipId());}
 @PostMapping("/students") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.StudentView student(@Valid @RequestBody StudentRequest r){return service.createStudent(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.CreateStudent(r.code(),r.admissionDate(),r.person().toInput()));}
 @PostMapping("/students/from-national-registry") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.StudentView studentFromRegistry(@Valid @RequestBody StudentFromRegistryRequest r){return service.createStudentFromNationalRegistry(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.CreateStudentFromRegistry(r.code(),r.admissionDate(),r.nationalCode(),r.birthDate(),r.gender(),r.mobile(),r.email(),r.initialPassword()));}
 @PostMapping("/instructors") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.InstructorView instructor(@Valid @RequestBody InstructorRequest r){return service.createInstructor(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.CreateInstructor(r.code(),r.title(),r.specialization(),r.person().toInput()));}
 @PostMapping("/instructors/from-national-registry") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.InstructorView instructorFromRegistry(@Valid @RequestBody InstructorFromRegistryRequest r){return service.createInstructorFromNationalRegistry(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.CreateInstructorFromRegistry(r.code(),r.title(),r.specialization(),r.nationalCode(),r.birthDate(),r.gender(),r.mobile(),r.email(),r.initialPassword()));}
 @PostMapping("/guardians") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.GuardianView guardian(@Valid @RequestBody GuardianRequest r){return service.createGuardian(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.CreateGuardian(r.occupation(),r.person().toInput()));}
 @PostMapping("/guardian-links") @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE')") public PeopleService.RelationshipView link(@Valid @RequestBody LinkRequest r){return service.linkGuardian(CurrentUser.tenantId(),CurrentUser.accountId(),new PeopleService.LinkGuardian(r.studentId(),r.guardianId(),r.relationshipType(),r.primary(),r.academicAccess(),r.financialAccess(),r.pickupPermission(),r.receivesNotifications(),r.effectiveFrom()));}
 public record PersonRequest(@NotBlank @Size(max=120) String firstName,@NotBlank @Size(max=120) String lastName,@Size(max=30) String nationalId,@Size(max=30) String mobile,@Email String email,String initialPassword){PeopleService.PersonInput toInput(){return new PeopleService.PersonInput(firstName,lastName,nationalId,mobile,email,initialPassword);}}
 public record StudentRequest(@NotBlank @Size(max=50) String code,LocalDate admissionDate,@NotNull @Valid PersonRequest person){}
 public record StudentFromRegistryRequest(@NotBlank @Size(max=50) String code,LocalDate admissionDate,@NotBlank @Size(min=10,max=10) String nationalCode,@NotBlank String birthDate,Boolean gender,@Size(max=30) String mobile,@Email String email,String initialPassword){}
 public record InstructorRequest(@NotBlank @Size(max=50) String code,@Size(max=120) String title,@Size(max=300) String specialization,@NotNull @Valid PersonRequest person){}
 public record InstructorFromRegistryRequest(@NotBlank @Size(max=50) String code,@Size(max=120) String title,@Size(max=300) String specialization,@NotBlank @Size(min=10,max=10) String nationalCode,@NotBlank String birthDate,Boolean gender,@Size(max=30) String mobile,@Email String email,String initialPassword){}
 public record GuardianRequest(@Size(max=150) String occupation,@NotNull @Valid PersonRequest person){}
 public record LinkRequest(@NotNull Long studentId,@NotNull Long guardianId,@NotNull StudentGuardian.RelationshipType relationshipType,boolean primary,boolean academicAccess,boolean financialAccess,boolean pickupPermission,boolean receivesNotifications,@NotNull LocalDate effectiveFrom){}
}
