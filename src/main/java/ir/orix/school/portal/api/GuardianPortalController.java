package ir.orix.school.portal.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.assessment.application.AssessmentExecutionService;
import ir.orix.school.assessment.application.AssessmentService;
import ir.orix.school.attendance.application.AttendanceService;
import ir.orix.school.enrollment.application.EnrollmentService;
import ir.orix.school.finance.application.FinanceService;
import ir.orix.school.finance.application.WalletService;
import ir.orix.school.people.application.PeopleService;
import ir.orix.school.scheduling.application.SchedulingService;
import ir.orix.school.shared.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/guardian/children/{studentId}")
public class GuardianPortalController {
    private final PeopleService people;
    private final EnrollmentService enrollments;
    private final SchedulingService scheduling;
    private final AttendanceService attendance;
    private final AssessmentService assessments;
    private final AssessmentExecutionService execution;
    private final FinanceService finance;
    private final WalletService wallets;

    public GuardianPortalController(PeopleService people, EnrollmentService enrollments, SchedulingService scheduling,
            AttendanceService attendance, AssessmentService assessments, AssessmentExecutionService execution,
            FinanceService finance, WalletService wallets) {
        this.people = people;
        this.enrollments = enrollments;
        this.scheduling = scheduling;
        this.attendance = attendance;
        this.assessments = assessments;
        this.execution = execution;
        this.finance = finance;
        this.wallets = wallets;
    }

    @GetMapping("/enrollments")
    public List<EnrollmentService.View> enrollments(@PathVariable Long studentId) {
        authorize(studentId);
        return enrollments.forStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/sessions")
    public List<SchedulingService.SessionView> sessions(@PathVariable Long studentId) {
        authorize(studentId);
        return scheduling.forStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/attendance")
    public List<AttendanceService.MyAttendanceView> attendance(@PathVariable Long studentId) {
        authorize(studentId);
        return attendance.forStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/assessments")
    public List<AssessmentService.MyAssessmentView> assessments(@PathVariable Long studentId) {
        authorize(studentId);
        return assessments.forStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/results")
    public List<AssessmentExecutionService.ResultView> results(@PathVariable Long studentId) {
        authorize(studentId);
        return execution.resultsForStudent(CurrentUser.tenantId(), studentId);
    }

    @GetMapping("/results/{resultId}")
    public AssessmentExecutionService.ResultView result(@PathVariable Long studentId, @PathVariable Long resultId) {
        authorize(studentId);
        return execution.studentResult(CurrentUser.tenantId(), studentId, resultId);
    }

    @GetMapping("/finance")
    public FinanceService.Statement finance(@PathVariable Long studentId) {
        people.requireChildFinancialAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        return finance.statement(CurrentUser.tenantId(), studentId);
    }

    @GetMapping(value = "/finance/invoices/{invoiceId}/print", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public String printInvoice(@PathVariable Long studentId, @PathVariable Long invoiceId) {
        people.requireChildFinancialAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        FinanceService.InvoiceView invoice = finance.invoice(CurrentUser.tenantId(), invoiceId);
        if (!invoice.studentId().equals(studentId)) {
            throw new ir.orix.school.shared.web.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "INVOICE_ACCESS_DENIED", "Invoice does not belong to this student.");
        }
        return finance.printInvoice(CurrentUser.tenantId(), invoiceId);
    }

    @GetMapping(value = "/finance/payments/{paymentId}/receipt", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public String printReceipt(@PathVariable Long studentId, @PathVariable Long paymentId) {
        people.requireChildFinancialAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        FinanceService.PaymentView payment = finance.payment(CurrentUser.tenantId(), paymentId);
        if (!payment.studentId().equals(studentId)) {
            throw new ir.orix.school.shared.web.ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", "Payment does not belong to this student.");
        }
        return finance.printReceipt(CurrentUser.tenantId(), paymentId);
    }

    @PostMapping("/finance/pay")
    public WalletService.PayResult pay(@PathVariable Long studentId, @Valid @RequestBody(required = false) PayRequest request) {
        people.requireChildFinancialAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
        List<Long> chargeIds = request == null || request.chargeIds() == null ? List.of() : request.chargeIds();
        boolean coverAvailable = request != null && Boolean.TRUE.equals(request.coverAvailable());
        return wallets.pay(CurrentUser.tenantId(), CurrentUser.membershipId(), CurrentUser.accountId(), studentId,
                chargeIds, coverAvailable);
    }

    public record PayRequest(List<Long> chargeIds, Boolean coverAvailable) {}

    private void authorize(Long studentId) {
        people.requireChildAcademicAccess(CurrentUser.tenantId(), CurrentUser.membershipId(), studentId);
    }
}
