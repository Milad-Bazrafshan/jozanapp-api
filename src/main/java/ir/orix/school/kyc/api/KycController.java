package ir.orix.school.kyc.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ir.orix.school.kyc.NationalIdentityResult;
import ir.orix.school.kyc.ZohalThirdService;
import ir.orix.school.shared.security.CurrentUser;
import ir.orix.school.audit.application.AuditService;
import java.util.Map;

/**
 * Identity inquiry ahead of person / student registration (calls Zohal civil registry).
 */
@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final ZohalThirdService zohal;
    private final AuditService audit;

    public KycController(ZohalThirdService zohal, AuditService audit) {
        this.zohal = zohal;
        this.audit = audit;
    }

    @PostMapping("/national-identity")
    @PreAuthorize("@scopeAuthorizer.hasTenantPermission('PEOPLE_WRITE') or @scopeAuthorizer.hasTenantPermission('USER_WRITE')")
    public NationalIdentityResult nationalIdentity(@Valid @RequestBody NationalIdentityRequest request) {
        NationalIdentityResult result = zohal.nationalIdentityInquiry(request.nationalCode(), request.birthDate(), request.gender());
        audit.record(CurrentUser.tenantId(), CurrentUser.accountId(), "NATIONAL_IDENTITY_INQUIRY", "KYC",
                null, Map.of("nationalCode", maskNationalCode(result.nationalCode()), "matched", result.matched()));
        return result;
    }

    private static String maskNationalCode(String code) {
        if (code == null || code.length() < 4) return "****";
        return "******" + code.substring(code.length() - 4);
    }

    public record NationalIdentityRequest(
            @NotBlank String nationalCode,
            @NotBlank String birthDate,
            Boolean gender) {
    }
}