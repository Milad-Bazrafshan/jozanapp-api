package ir.orix.school.enrollment.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import ir.orix.school.enrollment.domain.Enrollment;
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long>{
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","student","student.person"}) List<Enrollment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","student","student.person"}) Optional<Enrollment> findByIdAndTenantId(Long id,Long tenantId);
 @EntityGraph(attributePaths={"classSection","classSection.offering","classSection.offering.course","classSection.offering.branch","student","student.person"}) List<Enrollment> findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(Long tenantId,Long studentId);
 @EntityGraph(attributePaths={"classSection","student","student.person"}) List<Enrollment> findAllByTenantIdAndClassSectionIdOrderByStudentPersonLastNameAsc(Long tenantId,Long classSectionId);
 @Query("select count(e) from Enrollment e where e.classSection.id=:sectionId and e.status='ACTIVE'") long countActive(@Param("sectionId") Long sectionId);
 boolean existsByStudentIdAndClassSectionIdAndStatusIn(Long studentId,Long sectionId,Collection<Enrollment.Status> statuses);
}
