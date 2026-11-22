package ir.orix.school.kyc;

/**
 * Normalized outcome of Zohal national (civil registry) identity inquiry.
 */
public record NationalIdentityResult(
        boolean matched,
        String nationalCode,
        String firstName,
        String lastName,
        String fatherName,
        boolean alive) {

    public boolean verified() {
        return matched && alive;
    }
}