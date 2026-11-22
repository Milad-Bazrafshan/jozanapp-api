package ir.orix.school.identity.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ir.orix.school.identity.domain.TwoFactorSmsChallenge;

public interface TwoFactorSmsChallengeRepository extends JpaRepository<TwoFactorSmsChallenge, Long> {

    Optional<TwoFactorSmsChallenge> findByIdAndConsumedFalse(Long id);
}
