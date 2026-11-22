package ir.orix.school.kyc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.zohal")
public record ZohalProperties(
        boolean enabled,
        String baseUrl,
        String token,
        String nationalIdentityInquiryPath,
        int connectTimeoutMs,
        int readTimeoutMs) {

    public ZohalProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://service.zohal.io";
        }
        if (nationalIdentityInquiryPath == null || nationalIdentityInquiryPath.isBlank()) {
            nationalIdentityInquiryPath = "/api/v0/services/inquiry/national_identity_inquiry";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 8_000;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 25_000;
        }
    }

    public String nationalIdentityInquiryUrl() {
        return baseUrl.replaceAll("/$", "") + nationalIdentityInquiryPath;
    }
}