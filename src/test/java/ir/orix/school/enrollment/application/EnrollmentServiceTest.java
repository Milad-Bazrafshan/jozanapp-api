/*
package ir.orix.school.enrollment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import java.time.Clock; import java.time.Instant; import java.time.ZoneOffset; import java.util.Optional; import java.util.UUID;
import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import ir.orix.school.audit.application.AuditService; import ir.orix.school.classroom.domain.*; import ir.orix.school.classroom.persistence.ClassSectionRepository; import ir.orix.school.enrollment.persistence.*; import ir.orix.school.identity.persistence.UserAccountRepository; import ir.orix.school.organization.persistence.*; import ir.orix.school.people.domain.StudentProfile; import ir.orix.school.people.persistence.StudentProfileRepository; import ir.orix.school.shared.web.ApiException;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
 @Mock TenantRepository tenants; @Mock TenantMembershipRepository memberships; @Mock StudentProfileRepository students; @Mock ClassSectionRepository sections; @Mock EnrollmentRepository enrollments; @Mock EnrollmentStatusHistoryRepository history; @Mock UserAccountRepository accounts; @Mock AuditService audit;
 private EnrollmentService service;
 @BeforeEach void setUp(){service=new EnrollmentService(tenants,memberships,students,sections,enrollments,history,accounts,audit,Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"),ZoneOffset.UTC));}
 @Test void rejectsEnrollmentWhenCapacityIsReachedAndWaitlistIsDisabled(){
  UUID tenantId=UUID.randomUUID(),studentId=UUID.randomUUID(),sectionId=UUID.randomUUID(); ClassSection section=org.mockito.Mockito.mock(ClassSection.class); CourseOffering offering=org.mockito.Mockito.mock(CourseOffering.class); StudentProfile student=org.mockito.Mockito.mock(StudentProfile.class);
  when(sections.findLocked(sectionId,tenantId)).thenReturn(Optional.of(section)); when(students.findByIdAndTenantId(studentId,tenantId)).thenReturn(Optional.of(student)); when(student.getId()).thenReturn(studentId); when(section.getId()).thenReturn(sectionId); when(section.getStatus()).thenReturn(ClassSection.Status.ACTIVE); when(section.getOffering()).thenReturn(offering); when(offering.getStatus()).thenReturn(CourseOffering.Status.OPEN); when(section.getCapacity()).thenReturn(20); when(enrollments.countActive(sectionId)).thenReturn(20L);
  EnrollmentService.Command command=new EnrollmentService.Command(studentId,sectionId,null,false,null);
  assertThatThrownBy(()->service.enroll(tenantId,UUID.randomUUID(),command)).isInstanceOf(ApiException.class).extracting("code").isEqualTo("CLASS_CAPACITY_REACHED");
 }
}
*/
