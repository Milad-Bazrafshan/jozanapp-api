package ir.orix.school.kyc;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ir.orix.school.kyc.ZohalNationalIdentityDtos.IdentityData;
import ir.orix.school.kyc.ZohalNationalIdentityDtos.NationalIdentityInquiryRequest;
import ir.orix.school.kyc.ZohalNationalIdentityDtos.ZohalEnvelope;
import ir.orix.school.shared.web.ApiException;

/**
 * HTTP client for Zohal (زحل) identity and inquiry services.
 * <p>
 * Primary use: {@link #nationalIdentityInquiry(String, String, Boolean)} with national code and
 * birth date (Jalali, e.g. {@code 1370/01/01} or {@code 13700101}) before registering a person.
 */
@Service
public class ZohalThirdService {

    private static final Logger log = LoggerFactory.getLogger(ZohalThirdService.class);

    private final ZohalProperties properties;
    private final RestClient client;

    public ZohalThirdService(ZohalProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
    }

    /**
     * Civil registry identity lookup (هویت ملی).
     *
     * @param nationalCode 10-digit national ID (normalized, digits only)
     * @param birthDate    birth date as required by Zohal (typically Jalali {@code YYYY/MM/DD})
     * @param gender       optional; when null, {@code false} is sent per Zohal API
     */
    public NationalIdentityResult nationalIdentityInquiry(String nationalCode, String birthDate, Boolean gender) {
        ensureConfigured();
        String code = normalizeNationalCode(nationalCode);
        String dob = normalizeBirthDate(birthDate);
        NationalIdentityInquiryRequest body = new NationalIdentityInquiryRequest(
                dob,
                code,
                gender != null && gender);

        try {
            ZohalEnvelope envelope = client.post()
                    .uri(properties.nationalIdentityInquiryUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ZohalEnvelope.class);

            if (envelope == null) {
                throw inquiryFailed("ZOHAL_EMPTY_RESPONSE", "Zohal returned an empty response.");
            }
            if (envelope.result() == null || envelope.result() != 1) {
                String message = envelope.responseBody() != null ? envelope.responseBody().message() : null;
                throw inquiryFailed("ZOHAL_INQUIRY_FAILED", message != null ? message : "Zohal inquiry was not successful.");
            }
            IdentityData data = envelope.responseBody() != null ? envelope.responseBody().data() : null;
            if (data == null) {
                throw inquiryFailed("ZOHAL_NO_IDENTITY_DATA", "Zohal did not return identity data.");
            }
            if (!Boolean.TRUE.equals(data.matched())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NATIONAL_IDENTITY_NOT_MATCHED",
                        "National code and birth date do not match civil registry records.");
            }
            if (Boolean.TRUE.equals(data.isDead()) || !Boolean.TRUE.equals(data.alive())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NATIONAL_IDENTITY_DECEASED",
                        "Civil registry indicates this person is not active.");
            }
            return new NationalIdentityResult(
                    true,
                    data.nationalCode() != null ? data.nationalCode() : code,
                    safeName(data.firstName()),
                    safeName(data.lastName()),
                    safeName(data.fatherName()),
                    true);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("Zohal HTTP error status={}", ex.getStatusCode().value());
            throw inquiryFailed("ZOHAL_HTTP_ERROR", "Zohal service is temporarily unavailable.");
        } catch (Exception ex) {
            log.error("Zohal inquiry failed: {}", ex.getMessage());
            throw inquiryFailed("ZOHAL_INQUIRY_ERROR", "Zohal inquiry could not be completed.");
        }
    }

    /**
     * Convenience: inquiry then build {@link ir.orix.school.people.domain.Person} fields for registration.
     */
    public NationalIdentityResult requireVerifiedIdentity(String nationalCode, String birthDate, Boolean gender) {
        return nationalIdentityInquiry(nationalCode, birthDate, gender);
    }

    public boolean isEnabled() {
        return properties.enabled() && properties.token() != null && !properties.token().isBlank();
    }

    public static String normalizeBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BIRTH_DATE_REQUIRED", "Birth date is required for identity inquiry.");
        }
        String value = birthDate.strip();
        if (value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-') {
            try {
                LocalDate iso = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                return String.format("%04d/%02d/%02d", iso.getYear(), iso.getMonthValue(), iso.getDayOfMonth());
            } catch (Exception ignored) {
                return value;
            }
        }
        return value;
    }

    public static String normalizeNationalCode(String nationalCode) {
        if (nationalCode == null || nationalCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NATIONAL_CODE_REQUIRED", "National code is required.");
        }
        String digits = nationalCode.replaceAll("\\D", "");
        if (digits.length() != 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NATIONAL_CODE_INVALID", "National code must be 10 digits.");
        }
        return digits;
    }

    private void ensureConfigured() {
        if (!properties.enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ZOHAL_DISABLED", "Zohal identity inquiry is disabled.");
        }
        if (properties.token() == null || properties.token().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ZOHAL_TOKEN_MISSING", "Zohal API token is not configured.");
        }
    }

    private static String safeName(String value) {
        return value == null || value.isBlank() ? "" : value.strip();
    }

    private static ApiException inquiryFailed(String code, String detail) {
        return new ApiException(HttpStatus.BAD_GATEWAY, code, detail);
    }
}