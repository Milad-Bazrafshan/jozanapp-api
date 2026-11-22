/*
package ir.orix.school.finance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.people.domain.StudentProfile;

class FinanceLifecycleTest {
    @Test
    void chargeSettlementMovesFromOpenToPaidAndRejectsOverAllocation() {
        Charge charge = charge(new BigDecimal("100.00"), new BigDecimal("10.00"));
        assertEquals(new BigDecimal("90.00"), charge.netAmount());
        charge.applySettlement(new BigDecimal("40.00"));
        assertEquals(Charge.Status.PARTIALLY_PAID, charge.getStatus());
        charge.applySettlement(new BigDecimal("90.00"));
        assertEquals(Charge.Status.PAID, charge.getStatus());
        assertThrows(IllegalStateException.class, () -> charge.applySettlement(new BigDecimal("90.01")));
    }

    @Test
    void openChargeCanBeVoidedButSettledChargeCannot() {
        Charge open = charge(new BigDecimal("50.00"), BigDecimal.ZERO);
        open.voidCharge();
        assertEquals(Charge.Status.VOID, open.getStatus());
        Charge partial = charge(new BigDecimal("50.00"), BigDecimal.ZERO);
        partial.applySettlement(new BigDecimal("10.00"));
        assertThrows(IllegalStateException.class, partial::voidCharge);
    }

    @Test
    void allocationMustShareStudentAndSucceededPayment() {
        UUID studentId = UUID.randomUUID();
        StudentProfile student = mock(StudentProfile.class);
        when(student.getId()).thenReturn(studentId);
        Tenant tenant = mock(Tenant.class);
        Payment payment = new Payment(tenant, student, "REC-1", new BigDecimal("20.00"), Payment.Method.CASH,
                Payment.Provider.INTERNAL, null, Instant.parse("2026-09-18T10:00:00Z"), null, null);
        Charge other = charge(new BigDecimal("20.00"), BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, () -> new PaymentAllocation(tenant, payment, other, new BigDecimal("10.00")));
        Charge same = new Charge(tenant, student, null, null, "CHG-2", Charge.Type.TUITION, "Tuition",
                new BigDecimal("20.00"), BigDecimal.ZERO, LocalDate.parse("2026-10-01"), null);
        PaymentAllocation allocation = new PaymentAllocation(tenant, payment, same, new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), allocation.getAmount());
        Payment walletPay = new Payment(tenant, student, "REC-W", new BigDecimal("10.00"),
                Payment.Method.WALLET, Payment.Provider.INTERNAL, "ext-wallet", Instant.parse("2026-09-18T10:00:00Z"), null, null);
        assertEquals(Payment.Method.WALLET, walletPay.getMethod());
        assertThrows(IllegalStateException.class, () -> new Payment(tenant, student, "REC-OW", new BigDecimal("10.00"),
                Payment.Method.WALLET, Payment.Provider.ORIX_WALLET, "ext-1", Instant.parse("2026-09-18T10:00:00Z"), null, null));
    }

    private static Charge charge(BigDecimal amount, BigDecimal discount) {
        StudentProfile student = mock(StudentProfile.class);
        when(student.getId()).thenReturn(UUID.randomUUID());
        return new Charge(mock(Tenant.class), student, null, null, "CHG-1", Charge.Type.TUITION, "Tuition",
                amount, discount, LocalDate.parse("2026-10-01"), null);
    }
}
*/
