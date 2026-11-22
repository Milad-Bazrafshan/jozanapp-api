package ir.orix.school.people.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.people.domain.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndNationalId(Long tenantId, String nationalId);
}
