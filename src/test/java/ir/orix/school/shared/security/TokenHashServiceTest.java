/*
package ir.orix.school.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TokenHashServiceTest {
    private final TokenHashService service = new TokenHashService();

    @Test
    void createsOpaqueUniqueTokensAndStableHashes() {
        String first = service.newToken();
        String second = service.newToken();
        assertThat(first).isNotBlank().isNotEqualTo(second);
        assertThat(service.hash(first)).hasSize(64).isEqualTo(service.hash(first));
        assertThat(service.hash(first)).isNotEqualTo(service.hash(second));
    }
}
*/
