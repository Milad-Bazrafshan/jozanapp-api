package ir.orix.school.finance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.GuardianProfile;

class WalletLifecycleTest {
    @Test
    void creditThenDebitUpdatesBalanceAndRejectsOverdraft() {
        Wallet wallet = new Wallet(mock(Tenant.class), mock(GuardianProfile.class));
        assertEquals(new BigDecimal("0.00"), wallet.getBalance());
        assertEquals(new BigDecimal("10000000.00"), wallet.credit(new BigDecimal("10000000.00")));
        assertEquals(new BigDecimal("4400000.00"), wallet.debit(new BigDecimal("5600000.00")));
        assertThrows(IllegalStateException.class, () -> wallet.debit(new BigDecimal("4400000.01")));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(BigDecimal.ZERO));
    }
}
