package ir.orix.school.kyc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

final class ZohalNationalIdentityDtos {

    private ZohalNationalIdentityDtos() {
    }

    record NationalIdentityInquiryRequest(
            @JsonProperty("birth_date") String birthDate,
            @JsonProperty("national_code") String nationalCode,
            Boolean gender) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ZohalEnvelope(
            @JsonProperty("response_body") ResponseBody responseBody,
            Integer result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseBody(
            IdentityData data,
            String message,
            @JsonProperty("error_code") String errorCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record IdentityData(
            Boolean matched,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("national_code") String nationalCode,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("father_name") String fatherName,
            Boolean alive,
            @JsonProperty("is_dead") Boolean isDead) {
    }
}