package ir.orix.school.classroom.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.classroom.domain.ClassInstructor;
public interface ClassInstructorRepository extends JpaRepository<ClassInstructor, Long>{
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","classSection.offering.academicPeriod","instructor.person"}) List<ClassInstructor> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","classSection.offering.academicPeriod","instructor.person"}) List<ClassInstructor> findAllByTenantIdAndInstructorIdAndStatus(Long tenantId,Long instructorId,ClassInstructor.Status status);
 boolean existsByClassSectionIdAndInstructorIdAndStatus(Long sectionId,Long instructorId,ClassInstructor.Status status);
}
