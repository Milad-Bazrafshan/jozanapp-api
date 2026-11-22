package ir.orix.school.classroom.persistence;
import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import ir.orix.school.classroom.domain.ClassSection;
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long>{
 @EntityGraph(attributePaths={"offering","offering.course","offering.branch","offering.academicPeriod"}) List<ClassSection> findAllByTenantIdOrderByName(Long tenantId);
 @EntityGraph(attributePaths={"offering","offering.course","offering.branch","offering.academicPeriod"}) Optional<ClassSection> findByIdAndTenantId(Long id,Long tenantId);
 boolean existsByOfferingIdAndCodeIgnoreCase(Long offeringId,String code);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select section from ClassSection section where section.id=:id and section.tenant.id=:tenantId") Optional<ClassSection> findLocked(@Param("id") Long id,@Param("tenantId") Long tenantId);
}
