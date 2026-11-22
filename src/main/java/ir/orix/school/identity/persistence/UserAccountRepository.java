package ir.orix.school.identity.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.identity.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
