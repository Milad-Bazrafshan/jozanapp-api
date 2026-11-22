package ir.orix.school.finance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.finance.domain.Payment;
import ir.orix.school.finance.domain.Wallet;
import ir.orix.school.finance.domain.WalletTransaction;
import ir.orix.school.finance.persistence.PaymentRepository;
import ir.orix.school.finance.persistence.WalletRepository;
import ir.orix.school.finance.persistence.WalletTransactionRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.GuardianProfile;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.domain.StudentGuardian;
import ir.orix.school.people.persistence.GuardianProfileRepository;
import ir.orix.school.people.persistence.StudentGuardianRepository;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.shared.web.ApiException;

@Service
public class WalletService {
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final GuardianProfileRepository guardians;
    private final StudentGuardianRepository studentGuardians;
    private final StudentProfileRepository students;
    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    private final PaymentRepository payments;
    private final FinanceService finance;
    private final AuditService audit;

    public WalletService(TenantRepository tenants, TenantMembershipRepository memberships, GuardianProfileRepository guardians,
            StudentGuardianRepository studentGuardians, StudentProfileRepository students, WalletRepository wallets,
            WalletTransactionRepository transactions, PaymentRepository payments, FinanceService finance, AuditService audit) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.guardians = guardians;
        this.studentGuardians = studentGuardians;
        this.students = students;
        this.wallets = wallets;
        this.transactions = transactions;
        this.payments = payments;
        this.finance = finance;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public WalletView mine(Long tenantId, Long membershipId) {
        GuardianProfile guardian = guardianFromMembership(tenantId, membershipId);
        return wallets.findByTenantIdAndGuardianId(tenantId, guardian.getId())
                .map(wallet -> view(tenantId, wallet))
                .orElseGet(() -> new WalletView(null, BigDecimal.ZERO.setScale(2), List.of()));
    }

    @Transactional
    public WalletView topUp(Long tenantId, Long membershipId, Long actorId, BigDecimal amount, String notes) {
        GuardianProfile guardian = guardianFromMembership(tenantId, membershipId);
        return credit(tenantId, actorId, guardian, scale(amount), notes);
    }

    @Transactional
    public void ensureDemoCredit(Long tenantId, Long actorId, Long guardianId, BigDecimal amount) {
        GuardianProfile guardian = guardians.findByIdAndTenantId(guardianId, tenantId).orElse(null);
        if (guardian == null) return;
        Wallet wallet = lockedOrCreate(tenantId, guardian);
        boolean already = transactions.findAllByTenantIdAndWalletIdOrderByCreatedAtDesc(tenantId, wallet.getId()).stream()
                .anyMatch(item -> item.getNotes() != null && item.getNotes().contains("DEMO-WALLET"));
        if (already) return;
        applyCredit(tenantId, actorId, wallet, scale(amount), "شارژ اولیه کیف پول دمو · DEMO-WALLET");
    }

    @Transactional
    public PayResult pay(Long tenantId, Long membershipId, Long actorId, Long studentId, List<Long> chargeIds, boolean coverAvailable) {
        GuardianProfile guardian = guardianFromMembership(tenantId, membershipId);
        Wallet wallet = lockedOrCreate(tenantId, guardian);
        StudentProfile student = students.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "STUDENT_NOT_FOUND", "The requested student was not found."));
        List<FinanceService.ChargeView> selected = openCharges(finance.statement(tenantId, studentId).charges(), chargeIds);
        BigDecimal outstanding = selected.stream().map(FinanceService.ChargeView::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstanding.signum() <= 0) {
            throw conflict("NOTHING_TO_PAY", "There is no remaining amount to pay.");
        }
        BigDecimal amount = coverAvailable ? wallet.getBalance().min(outstanding) : outstanding;
        if (amount.signum() <= 0 || wallet.getBalance().compareTo(amount) < 0) {
            throw conflict("INSUFFICIENT_WALLET", "Wallet balance is not enough for this payment.");
        }
        List<FinanceService.AllocationCommand> allocations = fifo(selected, amount);
        BigDecimal after;
        try {
            after = wallet.debit(amount);
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "Wallet balance is not enough for this payment.");
        }
        FinanceService.PaymentView payment = finance.recordPayment(tenantId, actorId, new FinanceService.PaymentCommand(
                studentId, amount, Payment.Method.WALLET, Payment.Provider.INTERNAL, "WALLET-" + UUID.randomUUID(),
                Instant.now(), "پرداخت از کیف پول ولی", null, allocations));
        Payment recorded = payments.findByIdAndTenantId(payment.id(), tenantId).orElse(null);
        transactions.save(new WalletTransaction(tenant(tenantId), wallet, WalletTransaction.Type.PAYMENT, amount, after,
                student, recorded, "پرداخت شهریه " + student.getPerson().getFullName()));
        audit.record(tenantId, actorId, "WALLET_PAYMENT", "WALLET", wallet.getId(),
                java.util.Map.of("studentId", studentId, "amount", amount, "receipt", payment.receiptNumber()));
        return new PayResult(view(tenantId, wallet), payment, finance.statement(tenantId, studentId).balance());
    }

    private WalletView credit(Long tenantId, Long actorId, GuardianProfile guardian, BigDecimal amount, String notes) {
        Wallet wallet = lockedOrCreate(tenantId, guardian);
        applyCredit(tenantId, actorId, wallet, amount, notes);
        return view(tenantId, wallet);
    }

    private void applyCredit(Long tenantId, Long actorId, Wallet wallet, BigDecimal amount, String notes) {
        BigDecimal after = wallet.credit(amount);
        transactions.save(new WalletTransaction(tenant(tenantId), wallet, WalletTransaction.Type.TOP_UP, amount, after, null, null, notes));
        audit.record(tenantId, actorId, "WALLET_TOP_UP", "WALLET", wallet.getId(), java.util.Map.of("amount", amount));
    }

    private Wallet lockedOrCreate(Long tenantId, GuardianProfile guardian) {
        return wallets.findLocked(tenantId, guardian.getId()).orElseGet(() -> {
            try {
                wallets.saveAndFlush(new Wallet(tenant(tenantId), guardian));
            } catch (DataIntegrityViolationException ignored) {
                // another request created the wallet
            }
            return wallets.findLocked(tenantId, guardian.getId()).orElseThrow(() -> conflict("WALLET_CREATE_FAILED", "Wallet could not be created."));
        });
    }

    private List<FinanceService.ChargeView> openCharges(List<FinanceService.ChargeView> charges, List<Long> chargeIds) {
        List<FinanceService.ChargeView> open = charges.stream()
                .filter(charge -> charge.remaining().signum() > 0 && !"VOID".equals(charge.status()) && !"PAID".equals(charge.status()))
                .sorted(Comparator.comparing(FinanceService.ChargeView::dueOn).thenComparing(FinanceService.ChargeView::code))
                .toList();
        if (chargeIds == null || chargeIds.isEmpty()) return open;
        List<FinanceService.ChargeView> selected = new ArrayList<>();
        for (Long chargeId : chargeIds) {
            FinanceService.ChargeView match = open.stream().filter(item -> item.id().equals(chargeId)).findFirst()
                    .orElseThrow(() -> conflict("CHARGE_NOT_PAYABLE", "This charge cannot be paid from the wallet."));
            selected.add(match);
        }
        selected.sort(Comparator.comparing(FinanceService.ChargeView::dueOn).thenComparing(FinanceService.ChargeView::code));
        return selected;
    }

    private static List<FinanceService.AllocationCommand> fifo(List<FinanceService.ChargeView> charges, BigDecimal amount) {
        BigDecimal left = amount;
        List<FinanceService.AllocationCommand> allocations = new ArrayList<>();
        for (FinanceService.ChargeView charge : charges) {
            if (left.signum() <= 0) break;
            BigDecimal take = charge.remaining().min(left);
            if (take.signum() > 0) {
                allocations.add(new FinanceService.AllocationCommand(charge.id(), take));
                left = left.subtract(take);
            }
        }
        return allocations;
    }

    private WalletView view(Long tenantId, Wallet wallet) {
        List<TxnView> items = transactions.findAllByTenantIdAndWalletIdOrderByCreatedAtDesc(tenantId, wallet.getId()).stream()
                .map(WalletService::txnView)
                .toList();
        return new WalletView(wallet.getId(), wallet.getBalance(), items);
    }

    private static TxnView txnView(WalletTransaction txn) {
        StudentProfile student = txn.getStudent();
        return new TxnView(txn.getId(), txn.getType().name(), txn.getAmount(), txn.getBalanceAfter(),
                student == null ? null : student.getId(),
                student == null ? null : student.getPerson().getFullName(),
                txn.getPayment() == null ? null : txn.getPayment().getId(),
                txn.getNotes(), txn.getCreatedAt());
    }

    private GuardianProfile guardianFromMembership(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMBERSHIP_NOT_FOUND", "Membership was not found."));
        return guardians.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "GUARDIAN_PROFILE_REQUIRED", "A guardian profile is required."));
    }

    private Tenant tenant(Long id) {
        return tenants.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Tenant was not found."));
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw conflict("INVALID_WALLET_AMOUNT", "Wallet amount must be greater than zero.");
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static ApiException conflict(String code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }

    public record WalletView(Long id, BigDecimal balance, List<TxnView> transactions) {}
    public record TxnView(Long id, String type, BigDecimal amount, BigDecimal balanceAfter, Long studentId, String studentName,
            Long paymentId, String notes, Instant createdAt) {}
    public record PayResult(WalletView wallet, FinanceService.PaymentView payment, FinanceService.BalanceView balance) {}
}
