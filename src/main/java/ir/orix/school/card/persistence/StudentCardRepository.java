package ir.orix.school.card.persistence;
import java.util.*; import org.springframework.data.jpa.repository.*; import ir.orix.school.card.domain.StudentCard;
public interface StudentCardRepository extends JpaRepository<StudentCard, Long> {
    @EntityGraph(attributePaths = {"student", "student.person", "classSection", "photo"})
    List<StudentCard> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
    @EntityGraph(attributePaths = {"student", "student.person", "classSection", "photo"})
    List<StudentCard> findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(Long tenantId, Long studentId);
    @EntityGraph(attributePaths = {"student", "student.person", "classSection", "tenant"})
    Optional<StudentCard> findByVerificationToken(String token);
}
