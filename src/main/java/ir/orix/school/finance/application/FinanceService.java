package ir.orix.school.finance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ir.orix.school.audit.application.AuditService;
import ir.orix.school.classroom.domain.ClassSection;
import ir.orix.school.classroom.persistence.ClassSectionRepository;
import ir.orix.school.enrollment.domain.Enrollment;
import ir.orix.school.enrollment.persistence.EnrollmentRepository;
import ir.orix.school.finance.domain.Charge;
import ir.orix.school.finance.domain.Invoice;
import ir.orix.school.finance.domain.Payment;
import ir.orix.school.finance.domain.PaymentAllocation;
import ir.orix.school.finance.persistence.ChargeRepository;
import ir.orix.school.finance.persistence.InvoiceItemRepository;
import ir.orix.school.finance.persistence.InvoiceRepository;
import ir.orix.school.finance.persistence.PaymentAllocationRepository;
import ir.orix.school.finance.persistence.PaymentRepository;
import ir.orix.school.identity.domain.UserAccount;
import ir.orix.school.identity.persistence.UserAccountRepository;
import ir.orix.school.organization.domain.Tenant;
import ir.orix.school.organization.domain.TenantMembership;
import ir.orix.school.organization.persistence.TenantMembershipRepository;
import ir.orix.school.organization.persistence.TenantRepository;
import ir.orix.school.people.domain.StudentProfile;
import ir.orix.school.people.persistence.StudentProfileRepository;
import ir.orix.school.finance.ledger.application.LedgerService;
import ir.orix.school.shared.web.ApiException;
import ir.orix.school.shared.web.PrintDocuments;

@Service
public class FinanceService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final StudentProfileRepository students;
    private final ClassSectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final ChargeRepository charges;
    private final InvoiceRepository invoices;
    private final InvoiceItemRepository invoiceItems;
    private final PaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final UserAccountRepository accounts;
    private final AuditService audit;
    private final LedgerService ledger;
    private final Clock clock;

    public FinanceService(TenantRepository tenants, TenantMembershipRepository memberships, StudentProfileRepository students,
            ClassSectionRepository sections, EnrollmentRepository enrollments, ChargeRepository charges, InvoiceRepository invoices,
            InvoiceItemRepository invoiceItems, PaymentRepository payments, PaymentAllocationRepository allocations,
            UserAccountRepository accounts, AuditService audit, LedgerService ledger, Clock clock) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.students = students;
        this.sections = sections;
        this.enrollments = enrollments;
        this.charges = charges;
        this.invoices = invoices;
        this.invoiceItems = invoiceItems;
        this.payments = payments;
        this.allocations = allocations;
        this.accounts = accounts;
        this.audit = audit;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Overview overview(Long tenantId) {
        Ledger ledger = ledger(tenantId, null);
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        int overdue = 0;
        LocalDate today = LocalDate.now(clock);
        for (Charge charge : ledger.charges) {
            if (charge.getStatus() == Charge.Status.VOID) continue;
            BigDecimal remaining = remaining(charge, ledger.allocatedByCharge);
            outstanding = outstanding.add(remaining);
            if (remaining.signum() > 0 && charge.getDueOn().isBefore(today)) overdue++;
        }
        for (Payment payment : ledger.payments) {
            if (payment.getStatus() == Payment.Status.SUCCEEDED) collected = collected.add(payment.getAmount());
        }
        return new Overview(outstanding, collected, overdue, balancesFrom(ledger));
    }

    @Transactional(readOnly = true)
    public List<ChargeView> charges(Long tenantId) {
        Ledger ledger = ledger(tenantId, null);
        return ledger.charges.stream().map(charge -> chargeView(charge, ledger)).toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceView> invoices(Long tenantId) {
        Ledger ledger = ledger(tenantId, null);
        return ledger.invoices.stream().map(invoice -> invoiceView(invoice, ledger)).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments(Long tenantId) {
        Ledger ledger = ledger(tenantId, null);
        return ledger.payments.stream().map(payment -> paymentView(payment, ledger)).toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceView> balances(Long tenantId) {
        return balancesFrom(ledger(tenantId, null));
    }

    @Transactional(readOnly = true)
    public Statement statement(Long tenantId, Long studentId) {
        students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
        Ledger ledger = ledger(tenantId, studentId);
        BalanceView balance = balancesFrom(ledger).stream().findFirst()
                .orElseGet(() -> emptyBalance(ledger.charges.isEmpty() ? students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND")) : ledger.charges.get(0).getStudent()));
        return new Statement(
                balance,
                ledger.charges.stream().map(charge -> chargeView(charge, ledger)).toList(),
                ledger.invoices.stream().map(invoice -> invoiceView(invoice, ledger)).toList(),
                ledger.payments.stream().map(payment -> paymentView(payment, ledger)).toList());
    }

    @Transactional(readOnly = true)
    public Statement mine(Long tenantId, Long membershipId) {
        return statement(tenantId, studentFromMembership(tenantId, membershipId).getId());
    }

    @Transactional
    public ChargeView createCharge(Long tenantId, Long actorId, ChargeCommand command) {
        StudentProfile student = student(tenantId, command.studentId());
        ClassSection section = command.classSectionId() == null ? null : sections.findByIdAndTenantId(command.classSectionId(), tenantId).orElseThrow(notFound("CLASS_SECTION_NOT_FOUND"));
        Enrollment enrollment = command.enrollmentId() == null ? null : enrollments.findByIdAndTenantId(command.enrollmentId(), tenantId).orElseThrow(notFound("ENROLLMENT_NOT_FOUND"));
        if (enrollment != null && !enrollment.getStudent().getId().equals(student.getId())) throw conflict("ENROLLMENT_STUDENT_MISMATCH", "Enrollment does not belong to this student.");
        String code = uniqueCode(command.code(), "CHG", tenantId, charges::existsByTenantIdAndCodeIgnoreCase);
        Charge charge = charges.save(new Charge(tenant(tenantId), student, section, enrollment, code, command.type(),
                command.title(), scale(command.amount()), scale(command.discount()), command.dueOn(), command.notes()));
        audit.record(tenantId, actorId, "CHARGE_CREATED", "CHARGE", charge.getId(), Map.of("code", charge.getCode()));
        return chargeView(charge, ledger(tenantId, student.getId()));
    }

    @Transactional
    public List<ChargeView> createPlan(Long tenantId, Long actorId, PlanCommand command) {
        if (command.installmentCount() < 2 || command.installmentCount() > 24) {
            throw conflict("INVALID_INSTALLMENT_COUNT", "Installment count must be between 2 and 24.");
        }
        if (command.intervalDays() < 1) throw conflict("INVALID_INTERVAL", "Installment interval must be at least one day.");
        BigDecimal net = scale(command.amount()).subtract(scale(command.discount()));
        if (net.signum() <= 0) throw conflict("INVALID_CHARGE_AMOUNT", "Net amount must be greater than zero.");
        BigDecimal count = BigDecimal.valueOf(command.installmentCount());
        BigDecimal base = net.divide(count, 2, RoundingMode.DOWN);
        List<ChargeView> created = new ArrayList<>();
        for (int index = 0; index < command.installmentCount(); index++) {
            BigDecimal part = index == command.installmentCount() - 1 ? net.subtract(base.multiply(BigDecimal.valueOf(command.installmentCount() - 1))) : base;
            LocalDate dueOn = command.firstDueOn().plusDays((long) command.intervalDays() * index);
            String title = command.title().strip() + " (" + (index + 1) + "/" + command.installmentCount() + ")";
            created.add(createCharge(tenantId, actorId, new ChargeCommand(command.studentId(), command.classSectionId(),
                    command.enrollmentId(), command.type(), title, part, BigDecimal.ZERO, dueOn, command.notes(), null)));
        }
        return created;
    }

    @Transactional
    public ChargeView voidCharge(Long tenantId, Long actorId, Long chargeId) {
        Charge charge = charges.findByIdAndTenantId(chargeId, tenantId).orElseThrow(notFound("CHARGE_NOT_FOUND"));
        try {
            charge.voidCharge();
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "This charge cannot be voided.");
        }
        audit.record(tenantId, actorId, "CHARGE_VOIDED", "CHARGE", charge.getId(), Map.of());
        return chargeView(charge, ledger(tenantId, charge.getStudent().getId()));
    }

    @Transactional
    public InvoiceView createInvoice(Long tenantId, Long actorId, InvoiceCommand command) {
        if (command.chargeIds() == null || command.chargeIds().isEmpty()) {
            throw conflict("INVOICE_EMPTY", "An invoice must include at least one charge.");
        }
        StudentProfile student = student(tenantId, command.studentId());
        Invoice invoice = new Invoice(tenant(tenantId), student, uniqueCode(command.code(), "INV", tenantId, invoices::existsByTenantIdAndCodeIgnoreCase),
                command.dueOn(), command.notes());
        java.util.LinkedHashSet<Long> uniqueCharges = new java.util.LinkedHashSet<>(command.chargeIds());
        if (uniqueCharges.size() != command.chargeIds().size()) throw conflict("DUPLICATE_INVOICE_CHARGE", "A charge can appear on an invoice only once.");
        for (Long chargeId : uniqueCharges) {
            Charge charge = charges.findByIdAndTenantId(chargeId, tenantId).orElseThrow(notFound("CHARGE_NOT_FOUND"));
            if (invoiceItems.existsByChargeId(charge.getId())) throw conflict("CHARGE_ALREADY_INVOICED", "Charge is already on an invoice.");
            try {
                invoice.addCharge(charge);
            } catch (IllegalStateException ex) {
                throw conflict(ex.getMessage(), "Charge cannot be added to this invoice.");
            }
        }
        invoice = invoices.save(invoice);
        audit.record(tenantId, actorId, "INVOICE_CREATED", "INVOICE", invoice.getId(), Map.of("code", invoice.getCode()));
        return invoiceView(invoice, ledger(tenantId, student.getId()));
    }

    @Transactional
    public InvoiceView issueInvoice(Long tenantId, Long actorId, Long invoiceId) {
        Invoice invoice = invoices.findByIdAndTenantId(invoiceId, tenantId).orElseThrow(notFound("INVOICE_NOT_FOUND"));
        try {
            invoice.issue(LocalDate.now(clock));
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "This invoice cannot be issued.");
        }
        audit.record(tenantId, actorId, "INVOICE_ISSUED", "INVOICE", invoice.getId(), Map.of());
        ledger.postInvoice(tenantId, invoice);
        return invoiceView(invoice, ledger(tenantId, invoice.getStudent().getId()));
    }

    @Transactional
    public InvoiceView voidInvoice(Long tenantId, Long actorId, Long invoiceId) {
        Invoice invoice = invoices.findByIdAndTenantId(invoiceId, tenantId).orElseThrow(notFound("INVOICE_NOT_FOUND"));
        try {
            invoice.voidInvoice();
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "This invoice cannot be voided.");
        }
        audit.record(tenantId, actorId, "INVOICE_VOIDED", "INVOICE", invoice.getId(), Map.of());
        return invoiceView(invoice, ledger(tenantId, invoice.getStudent().getId()));
    }

    @Transactional
    public PaymentView recordPayment(Long tenantId, Long actorId, PaymentCommand command) {
        StudentProfile student = student(tenantId, command.studentId());
        if (command.externalReference() != null && !command.externalReference().isBlank()
                && payments.existsByTenantIdAndProviderAndExternalReferenceIgnoreCase(tenantId, command.provider() == null ? Payment.Provider.INTERNAL : command.provider(), command.externalReference().strip())) {
            throw conflict("PAYMENT_REFERENCE_EXISTS", "External payment reference already exists for this provider.");
        }
        Payment payment;
        try {
            payment = payments.save(new Payment(tenant(tenantId), student,
                    uniqueCode(command.receiptNumber(), "REC", tenantId, payments::existsByTenantIdAndReceiptNumberIgnoreCase),
                    scale(command.amount()), command.method(), command.provider() == null ? Payment.Provider.INTERNAL : command.provider(),
                    command.externalReference(), command.paidAt() == null ? Instant.now(clock) : command.paidAt(),
                    command.notes(), actor(actorId)));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw conflict(ex.getMessage(), "Payment could not be recorded.");
        }
        if (command.allocations() != null) {
            for (AllocationCommand item : command.allocations()) {
                applyAllocation(tenantId, payment, item.chargeId(), item.amount());
            }
        }
        audit.record(tenantId, actorId, "PAYMENT_RECORDED", "PAYMENT", payment.getId(), Map.of("receipt", payment.getReceiptNumber()));
        ledger.postPayment(tenantId, payment);
        return paymentView(payment, ledger(tenantId, student.getId()));
    }

    @Transactional
    public PaymentView allocate(Long tenantId, Long actorId, Long paymentId, List<AllocationCommand> items) {
        Payment payment = payments.findByIdAndTenantId(paymentId, tenantId).orElseThrow(notFound("PAYMENT_NOT_FOUND"));
        if (items == null || items.isEmpty()) throw conflict("ALLOCATION_REQUIRED", "At least one allocation is required.");
        for (AllocationCommand item : items) {
            applyAllocation(tenantId, payment, item.chargeId(), item.amount());
        }
        audit.record(tenantId, actorId, "PAYMENT_ALLOCATED", "PAYMENT", payment.getId(), Map.of());
        return paymentView(payment, ledger(tenantId, payment.getStudent().getId()));
    }

    @Transactional(readOnly = true)
    public InvoiceView invoice(Long tenantId, Long id) {
        Invoice invoice = invoices.findByIdAndTenantId(id, tenantId).orElseThrow(notFound("INVOICE_NOT_FOUND"));
        return invoiceView(invoice, ledger(tenantId, invoice.getStudent().getId()));
    }

    @Transactional(readOnly = true)
    public PaymentView payment(Long tenantId, Long id) {
        Payment payment = payments.findByIdAndTenantId(id, tenantId).orElseThrow(notFound("PAYMENT_NOT_FOUND"));
        return paymentView(payment, ledger(tenantId, payment.getStudent().getId()));
    }

    @Transactional
    public PaymentView refund(Long tenantId, Long actorId, Long paymentId, String reason) {
        Payment payment = payments.findByIdAndTenantId(paymentId, tenantId).orElseThrow(notFound("PAYMENT_NOT_FOUND"));
        List<PaymentAllocation> rows = allocations.findAllByPaymentId(payment.getId());
        try {
            payment.markRefunded(reason);
        } catch (IllegalStateException ex) {
            throw conflict(ex.getMessage(), "This payment cannot be refunded.");
        }
        for (PaymentAllocation row : rows) {
            Ledger snap = ledger(tenantId, payment.getStudent().getId());
            BigDecimal remainingAllocated = allocated(row.getCharge(), snap.allocatedByCharge).subtract(row.getAmount());
            row.getCharge().applySettlement(remainingAllocated.max(BigDecimal.ZERO));
            allocations.delete(row);
        }
        ledger.postRefund(tenantId, payment);
        audit.record(tenantId, actorId, "PAYMENT_REFUNDED", "PAYMENT", payment.getId(), Map.of("reason", reason == null ? "" : reason));
        return paymentView(payment, ledger(tenantId, payment.getStudent().getId()));
    }

    @Transactional(readOnly = true)
    public String printInvoice(Long tenantId, Long id) {
        InvoiceView invoice = invoice(tenantId, id);
        if (!"ISSUED".equals(invoice.status())) throw conflict("INVOICE_NOT_ISSUED", "Only issued invoices can be printed.");
        StringBuilder body = new StringBuilder("<h1>فاکتور ").append(PrintDocuments.escape(invoice.code())).append("</h1>");
        body.append("<p>").append(PrintDocuments.escape(invoice.studentName())).append(" — ").append(PrintDocuments.escape(invoice.studentCode())).append("</p>");
        body.append("<p class='muted'>سررسید ").append(PrintDocuments.day(invoice.dueOn())).append(" · صدور ").append(PrintDocuments.day(invoice.issuedOn())).append("</p>");
        body.append("<table><thead><tr><th>شرح</th><th>مبلغ</th><th>مانده</th></tr></thead><tbody>");
        for (InvoiceItemView item : invoice.items()) {
            body.append("<tr><td>").append(PrintDocuments.escape(item.charge().title())).append("</td><td>")
                    .append(PrintDocuments.money(item.charge().netAmount())).append("</td><td>")
                    .append(PrintDocuments.money(item.charge().remaining())).append("</td></tr>");
        }
        body.append("</tbody></table><p>جمع: <b>").append(PrintDocuments.money(invoice.total())).append("</b> · مانده ")
                .append(PrintDocuments.money(invoice.remaining())).append("</p>");
        return PrintDocuments.page("فاکتور " + invoice.code(), body.toString(), true);
    }

    @Transactional(readOnly = true)
    public String printReceipt(Long tenantId, Long id) {
        PaymentView payment = payment(tenantId, id);
        if (!"SUCCEEDED".equals(payment.status())) throw conflict("RECEIPT_NOT_AVAILABLE", "Only succeeded payments have receipts.");
        StringBuilder body = new StringBuilder("<h1>رسید پرداخت ").append(PrintDocuments.escape(payment.receiptNumber())).append("</h1>");
        body.append("<p>").append(PrintDocuments.escape(payment.studentName())).append("</p>");
        body.append("<p>مبلغ: <b>").append(PrintDocuments.money(payment.amount())).append("</b></p>");
        body.append("<p class='muted'>").append(payment.method()).append(" · ").append(payment.paidAt()).append("</p>");
        if (payment.externalReference() != null) body.append("<p>پیگیری: ").append(PrintDocuments.escape(payment.externalReference())).append("</p>");
        body.append("<table><thead><tr><th>تخصیص</th><th>مبلغ</th></tr></thead><tbody>");
        for (AllocationView line : payment.allocations()) {
            body.append("<tr><td>").append(PrintDocuments.escape(line.chargeTitle())).append("</td><td>").append(PrintDocuments.money(line.amount())).append("</td></tr>");
        }
        body.append("</tbody></table>");
        return PrintDocuments.page("رسید " + payment.receiptNumber(), body.toString(), true);
    }

    @Transactional(readOnly = true)
    public OpsReport opsReport(Long tenantId) {
        Overview overview = overview(tenantId);
        List<BalanceView> debtors = overview.balances().stream().filter(item -> item.outstanding().signum() > 0).toList();
        List<BalanceView> overdue = overview.balances().stream().filter(item -> item.overdue().signum() > 0).toList();
        return new OpsReport(overview.collected(), overview.outstanding(), overview.overdueCount(), debtors, overdue);
    }

    private void applyAllocation(Long tenantId, Payment payment, Long chargeId, BigDecimal amount) {
        Charge charge = charges.findByIdAndTenantId(chargeId, tenantId).orElseThrow(notFound("CHARGE_NOT_FOUND"));
        if (allocations.existsByPaymentIdAndChargeId(payment.getId(), charge.getId())) {
            throw conflict("ALLOCATION_EXISTS", "This payment is already allocated to that charge.");
        }
        Ledger ledger = ledger(tenantId, payment.getStudent().getId());
        BigDecimal chargeRemaining = remaining(charge, ledger.allocatedByCharge);
        BigDecimal paymentRemaining = unallocated(payment, ledger.allocatedByPayment);
        BigDecimal value = scale(amount);
        if (value.compareTo(chargeRemaining) > 0) throw conflict("ALLOCATION_EXCEEDS_CHARGE", "Allocation exceeds the remaining charge amount.");
        if (value.compareTo(paymentRemaining) > 0) throw conflict("ALLOCATION_EXCEEDS_PAYMENT", "Allocation exceeds the remaining payment amount.");
        try {
            allocations.save(new PaymentAllocation(tenant(tenantId), payment, charge, value));
            charge.applySettlement(allocated(charge, ledger.allocatedByCharge).add(value));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw conflict(ex.getMessage(), "Allocation could not be applied.");
        }
    }

    private Ledger ledger(Long tenantId, Long studentId) {
        List<Charge> chargeRows = studentId == null
                ? charges.findAllByTenantIdOrderByDueOnAscCreatedAtDesc(tenantId)
                : charges.findAllByTenantIdAndStudentIdOrderByDueOnAscCreatedAtDesc(tenantId, studentId);
        List<Invoice> invoiceRows = studentId == null
                ? invoices.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                : invoices.findAllByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId);
        List<Payment> paymentRows = studentId == null
                ? payments.findAllByTenantIdOrderByPaidAtDesc(tenantId)
                : payments.findAllByTenantIdAndStudentIdOrderByPaidAtDesc(tenantId, studentId);
        List<PaymentAllocation> allocationRows = studentId == null
                ? allocations.findAllByTenantId(tenantId)
                : allocations.findAllByTenantIdAndChargeStudentId(tenantId, studentId);
        Map<Long, BigDecimal> byCharge = new HashMap<>();
        Map<Long, BigDecimal> byPayment = new HashMap<>();
        Map<Long, List<AllocationView>> paymentLines = new HashMap<>();
        Map<Long, Long> invoiceByCharge = new HashMap<>();
        Map<Long, String> invoiceCodeByCharge = new HashMap<>();
        for (PaymentAllocation allocation : allocationRows) {
            byCharge.merge(allocation.getCharge().getId(), allocation.getAmount(), BigDecimal::add);
            byPayment.merge(allocation.getPayment().getId(), allocation.getAmount(), BigDecimal::add);
            paymentLines.computeIfAbsent(allocation.getPayment().getId(), key -> new ArrayList<>())
                    .add(new AllocationView(allocation.getId(), allocation.getCharge().getId(), allocation.getCharge().getTitle(), allocation.getAmount()));
        }
        for (Invoice invoice : invoiceRows) {
            invoice.getItems().forEach(item -> {
                invoiceByCharge.put(item.getCharge().getId(), invoice.getId());
                invoiceCodeByCharge.put(item.getCharge().getId(), invoice.getCode());
            });
        }
        return new Ledger(chargeRows, invoiceRows, paymentRows, byCharge, byPayment, paymentLines, invoiceByCharge, invoiceCodeByCharge);
    }

    private List<BalanceView> balancesFrom(Ledger ledger) {
        Map<Long, BalanceAcc> acc = new HashMap<>();
        LocalDate today = LocalDate.now(clock);
        for (Charge charge : ledger.charges) {
            BalanceAcc row = acc.computeIfAbsent(charge.getStudent().getId(), key -> new BalanceAcc(charge.getStudent()));
            if (charge.getStatus() == Charge.Status.VOID) continue;
            BigDecimal remaining = remaining(charge, ledger.allocatedByCharge);
            row.charged = row.charged.add(charge.netAmount());
            row.outstanding = row.outstanding.add(remaining);
            if (remaining.signum() > 0 && charge.getDueOn().isBefore(today)) row.overdue = row.overdue.add(remaining);
        }
        for (Payment payment : ledger.payments) {
            if (payment.getStatus() != Payment.Status.SUCCEEDED) continue;
            BalanceAcc row = acc.computeIfAbsent(payment.getStudent().getId(), key -> new BalanceAcc(payment.getStudent()));
            row.paid = row.paid.add(payment.getAmount());
            row.credit = row.credit.add(unallocated(payment, ledger.allocatedByPayment));
        }
        return acc.values().stream()
                .map(row -> new BalanceView(row.student.getId(), row.student.getStudentCode(), row.student.getPerson().getFullName(),
                        row.charged, row.paid, row.outstanding, row.credit, row.overdue))
                .sorted((left, right) -> left.studentName().compareToIgnoreCase(right.studentName()))
                .toList();
    }

    private ChargeView chargeView(Charge charge, Ledger ledger) {
        BigDecimal allocated = allocated(charge, ledger.allocatedByCharge);
        return new ChargeView(charge.getId(), charge.getStudent().getId(), charge.getStudent().getStudentCode(),
                charge.getStudent().getPerson().getFullName(), charge.getClassSection() == null ? null : charge.getClassSection().getId(),
                charge.getClassSection() == null ? null : charge.getClassSection().getName(), charge.getEnrollment() == null ? null : charge.getEnrollment().getId(),
                charge.getCode(), charge.getType().name(), charge.getTitle(), charge.getAmount(), charge.getDiscount(), charge.netAmount(),
                allocated, remaining(charge, ledger.allocatedByCharge), charge.getDueOn(), charge.getStatus().name(), charge.getNotes(),
                ledger.invoiceByCharge.get(charge.getId()), ledger.invoiceCodeByCharge.get(charge.getId()));
    }

    private InvoiceView invoiceView(Invoice invoice, Ledger ledger) {
        List<InvoiceItemView> items = invoice.getItems().stream()
                .map(item -> new InvoiceItemView(item.getId(), item.getLineNo(), chargeView(item.getCharge(), ledger)))
                .toList();
        BigDecimal total = items.stream().map(item -> item.charge().netAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = items.stream().map(item -> item.charge().remaining()).reduce(BigDecimal.ZERO, BigDecimal::add);
        String settlement = invoice.getStatus() == Invoice.Status.VOID ? "VOID"
                : remaining.signum() == 0 ? "PAID"
                : remaining.compareTo(total) < 0 ? "PARTIALLY_PAID" : "OPEN";
        return new InvoiceView(invoice.getId(), invoice.getStudent().getId(), invoice.getStudent().getStudentCode(),
                invoice.getStudent().getPerson().getFullName(), invoice.getCode(), invoice.getDueOn(), invoice.getIssuedOn(),
                invoice.getStatus().name(), settlement, total, remaining, invoice.getNotes(), items);
    }

    private PaymentView paymentView(Payment payment, Ledger ledger) {
        List<AllocationView> lines = ledger.paymentLines.getOrDefault(payment.getId(), List.of());
        return new PaymentView(payment.getId(), payment.getStudent().getId(), payment.getStudent().getStudentCode(),
                payment.getStudent().getPerson().getFullName(), payment.getReceiptNumber(), payment.getAmount(),
                allocated(payment, ledger.allocatedByPayment), unallocated(payment, ledger.allocatedByPayment),
                payment.getMethod().name(), payment.getProvider().name(), payment.getExternalReference(), payment.getStatus().name(),
                payment.getPaidAt(), payment.getNotes(), lines);
    }

    private static BigDecimal allocated(Charge charge, Map<Long, BigDecimal> byCharge) {
        return byCharge.getOrDefault(charge.getId(), BigDecimal.ZERO);
    }

    private static BigDecimal allocated(Payment payment, Map<Long, BigDecimal> byPayment) {
        return byPayment.getOrDefault(payment.getId(), BigDecimal.ZERO);
    }

    private static BigDecimal remaining(Charge charge, Map<Long, BigDecimal> byCharge) {
        if (charge.getStatus() == Charge.Status.VOID) return BigDecimal.ZERO;
        return charge.netAmount().subtract(allocated(charge, byCharge));
    }

    private static BigDecimal unallocated(Payment payment, Map<Long, BigDecimal> byPayment) {
        if (payment.getStatus() != Payment.Status.SUCCEEDED) return BigDecimal.ZERO;
        return payment.getAmount().subtract(allocated(payment, byPayment));
    }

    private BalanceView emptyBalance(StudentProfile student) {
        return new BalanceView(student.getId(), student.getStudentCode(), student.getPerson().getFullName(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private String uniqueCode(String requested, String prefix, Long tenantId, CodeExists exists) {
        if (requested != null && !requested.isBlank()) {
            String code = requested.strip().toUpperCase();
            if (exists.test(tenantId, code)) throw conflict("CODE_EXISTS", "This code already exists in the tenant.");
            return code;
        }
        LocalDate today = LocalDate.now(clock);
        for (int attempt = 0; attempt < 8; attempt++) {
            String code = prefix + "-" + today.format(DAY) + "-" + Integer.toHexString((UUID.randomUUID().hashCode() & 0xffff) | 0x1000).substring(1).toUpperCase();
            if (!exists.test(tenantId, code)) return code;
        }
        throw conflict("CODE_EXISTS", "Could not generate a unique finance code.");
    }

    private StudentProfile studentFromMembership(Long tenantId, Long membershipId) {
        TenantMembership membership = memberships.findByIdAndTenantId(membershipId, tenantId).orElseThrow(notFound("MEMBERSHIP_NOT_FOUND"));
        return students.findByTenantIdAndPersonId(tenantId, membership.getPerson().getId()).orElseThrow(notFound("STUDENT_PROFILE_NOT_FOUND"));
    }

    private StudentProfile student(Long tenantId, Long studentId) {
        return students.findByIdAndTenantId(studentId, tenantId).orElseThrow(notFound("STUDENT_NOT_FOUND"));
    }

    private Tenant tenant(Long id) { return tenants.findById(id).orElseThrow(notFound("TENANT_NOT_FOUND")); }
    private UserAccount actor(Long id) { return accounts.findById(id).orElseThrow(notFound("ACTOR_NOT_FOUND")); }
    private static BigDecimal scale(BigDecimal value) { return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private static ApiException conflict(String code, String detail) { return new ApiException(HttpStatus.CONFLICT, code, detail); }
    private static Supplier<ApiException> notFound(String code) {
        return () -> new ApiException(HttpStatus.NOT_FOUND, code, "The requested finance item was not found.");
    }

    private record Ledger(List<Charge> charges, List<Invoice> invoices, List<Payment> payments,
            Map<Long, BigDecimal> allocatedByCharge, Map<Long, BigDecimal> allocatedByPayment,
            Map<Long, List<AllocationView>> paymentLines, Map<Long, Long> invoiceByCharge, Map<Long, String> invoiceCodeByCharge) {}

    private static final class BalanceAcc {
        private final StudentProfile student;
        private BigDecimal charged = BigDecimal.ZERO;
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal outstanding = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
        private BigDecimal overdue = BigDecimal.ZERO;
        private BalanceAcc(StudentProfile student) { this.student = student; }
    }

    @FunctionalInterface
    private interface CodeExists { boolean test(Long tenantId, String code); }

    public record ChargeCommand(Long studentId, Long classSectionId, Long enrollmentId, Charge.Type type, String title,
            BigDecimal amount, BigDecimal discount, LocalDate dueOn, String notes, String code) {}
    public record PlanCommand(Long studentId, Long classSectionId, Long enrollmentId, Charge.Type type, String title,
            BigDecimal amount, BigDecimal discount, LocalDate firstDueOn, int installmentCount, int intervalDays, String notes) {}
    public record InvoiceCommand(Long studentId, LocalDate dueOn, String notes, List<Long> chargeIds, String code) {}
    public record PaymentCommand(Long studentId, BigDecimal amount, Payment.Method method, Payment.Provider provider,
            String externalReference, Instant paidAt, String notes, String receiptNumber, List<AllocationCommand> allocations) {}
    public record AllocationCommand(Long chargeId, BigDecimal amount) {}
    public record ChargeView(Long id, Long studentId, String studentCode, String studentName, Long classSectionId, String className,
            Long enrollmentId, String code, String type, String title, BigDecimal amount, BigDecimal discount, BigDecimal netAmount,
            BigDecimal allocated, BigDecimal remaining, LocalDate dueOn, String status, String notes, Long invoiceId, String invoiceCode) {}
    public record InvoiceItemView(Long id, int lineNo, ChargeView charge) {}
    public record InvoiceView(Long id, Long studentId, String studentCode, String studentName, String code, LocalDate dueOn,
            LocalDate issuedOn, String status, String settlementStatus, BigDecimal total, BigDecimal remaining, String notes,
            List<InvoiceItemView> items) {}
    public record AllocationView(Long id, Long chargeId, String chargeTitle, BigDecimal amount) {}
    public record PaymentView(Long id, Long studentId, String studentCode, String studentName, String receiptNumber, BigDecimal amount,
            BigDecimal allocated, BigDecimal unallocated, String method, String provider, String externalReference, String status,
            Instant paidAt, String notes, List<AllocationView> allocations) {}
    public record BalanceView(Long studentId, String studentCode, String studentName, BigDecimal charged, BigDecimal paid,
            BigDecimal outstanding, BigDecimal credit, BigDecimal overdue) {}
    public record Statement(BalanceView balance, List<ChargeView> charges, List<InvoiceView> invoices, List<PaymentView> payments) {}
    public record Overview(BigDecimal outstanding, BigDecimal collected, int overdueCount, List<BalanceView> balances) {}
    public record OpsReport(BigDecimal collected, BigDecimal outstanding, int overdueCount, List<BalanceView> debtors, List<BalanceView> overdue) {}
}
