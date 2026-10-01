package com.boltpe.insurance.validation.proposalform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.dto.request.proposalform.PersonalAccidentProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalContactValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProposalFormContactIdentityValidatorTest {
    private final ProposalFormContactIdentityValidator validator = new ProposalFormContactIdentityValidator();

    @Test void acceptsValidGroupContactIdentityAndPincode() {
        assertThatCode(() -> validator.validate(group("9000000000", "sample@example.com", "123456789012", "ABCDE1234F", "411033"))).doesNotThrowAnyException();
    }
    @Test void acceptsPlus91Mobile() {
        assertThatCode(() -> validator.validate(group("+919000000000", "sample@example.com", "123456789012", "ABCDE1234F", "110001"))).doesNotThrowAnyException();
    }
    @Test void returnsAllGroupFieldErrorsTogether() {
        assertThatThrownBy(() -> validator.validate(group("1234567890", "bad-email", "1234", "abcde1234f", "011001")))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsKeys("proposerPhone", "proposerEmail", "aadharNumber", "panNumber", "pincode"));
    }
    @Test void rejectsBlankRequiredGroupValues() {
        assertThatThrownBy(() -> validator.validate(group("", "", "", "", "")))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex))
                        .containsEntry("proposerPhone", "Mobile number is required")
                        .containsEntry("proposerEmail", "Email ID is required")
                        .containsEntry("aadharNumber", "Aadhaar number is required")
                        .containsEntry("panNumber", "PAN number is required")
                        .containsEntry("pincode", "Pincode is required"));
    }

    @Test void mobileRejectsWrongLength() { assertGroupError(group("987654321", validEmail(), validAadhaar(), validPan(), validPin()), "proposerPhone", "Mobile number must contain exactly 10 digits"); }
    @Test void mobileRejectsInvalidStartingDigit() { assertGroupError(group("1234567890", validEmail(), validAadhaar(), validPan(), validPin()), "proposerPhone", "Mobile number must start with 6, 7, 8, or 9"); }
    @Test void mobileRejectsAlphabet() { assertGroupError(group("98765abc10", validEmail(), validAadhaar(), validPan(), validPin()), "proposerPhone", "Mobile number must contain digits only, with optional +91 prefix"); }
    @Test void mobileRejectsMalformedPlus91() { assertGroupError(group("+91-9876543210", validEmail(), validAadhaar(), validPan(), validPin()), "proposerPhone", "Mobile number must be a valid 10-digit Indian number or +91 followed by 10 digits"); }

    @Test void emailRejectsMissingAt() { assertGroupError(group(validPhone(), "sampleexample.com", validAadhaar(), validPan(), validPin()), "proposerEmail", "Email ID must contain a valid @ symbol"); }
    @Test void emailRejectsMissingUsername() { assertGroupError(group(validPhone(), "@example.com", validAadhaar(), validPan(), validPin()), "proposerEmail", "Email ID must contain a valid username before @"); }
    @Test void emailRejectsMissingDomain() { assertGroupError(group(validPhone(), "sample@", validAadhaar(), validPan(), validPin()), "proposerEmail", "Email ID must contain a valid domain"); }
    @Test void emailRejectsMissingExtension() { assertGroupError(group(validPhone(), "sample@example", validAadhaar(), validPan(), validPin()), "proposerEmail", "Email ID must contain a valid domain extension"); }
    @Test void emailRejectsMultipleAt() { assertGroupError(group(validPhone(), "sample@@example.com", validAadhaar(), validPan(), validPin()), "proposerEmail", "Invalid email ID format"); }
    @Test void emailRejectsSpace() { assertGroupError(group(validPhone(), "sample @example.com", validAadhaar(), validPan(), validPin()), "proposerEmail", "Invalid email ID format"); }

    @Test void aadhaarRejectsWrongLength() { assertGroupError(group(validPhone(), validEmail(), "12345678901", validPan(), validPin()), "aadharNumber", "Aadhaar number must contain exactly 12 digits"); }
    @Test void aadhaarRejectsAlphabet() { assertGroupError(group(validPhone(), validEmail(), "ABCD56789012", validPan(), validPin()), "aadharNumber", "Aadhaar number must contain digits only"); }
    @Test void aadhaarRejectsSpecialFormat() { assertGroupError(group(validPhone(), validEmail(), "1234-5678901", validPan(), validPin()), "aadharNumber", "Invalid Aadhaar number format"); }
    @Test void aadhaarRejectsAllZeros() { assertGroupError(group(validPhone(), validEmail(), "000000000000", validPan(), validPin()), "aadharNumber", "Aadhaar number cannot contain the same digit repeated 12 times"); }
    @Test void aadhaarRejectsAnyRepeatedDigit() { assertGroupError(group(validPhone(), validEmail(), "111111111111", validPan(), validPin()), "aadharNumber", "Aadhaar number cannot contain the same digit repeated 12 times"); }

    @Test void panRejectsWrongLength() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), "ABCD1234F", validPin()), "panNumber", "PAN number must be exactly 10 characters"); }
    @Test void panRejectsInvalidFirstFive() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), "AB12E1234F", validPin()), "panNumber", "First 5 PAN characters must be alphabets"); }
    @Test void panRejectsInvalidMiddleFour() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), "ABCDE12A4F", validPin()), "panNumber", "PAN characters 6 to 9 must be digits"); }
    @Test void panRejectsInvalidLastCharacter() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), "ABCDE12345", validPin()), "panNumber", "Last PAN character must be an alphabet"); }
    @Test void panRejectsSpecialCharacter() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), "ABCDE12#4F", validPin()), "panNumber", "Invalid PAN number format"); }

    @Test void pincodeRejectsWrongLength() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), validPan(), "11001"), "pincode", "Pincode must contain exactly 6 digits"); }
    @Test void pincodeRejectsLeadingZero() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), validPan(), "011001"), "pincode", "Pincode must not start with 0"); }
    @Test void pincodeRejectsAlphabet() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), validPan(), "41A033"), "pincode", "Pincode must contain digits only"); }
    @Test void pincodeRejectsSpecialCharacter() { assertGroupError(group(validPhone(), validEmail(), validAadhaar(), validPan(), "411-03"), "pincode", "Pincode must be a valid 6-digit Indian pincode and must not start with 0"); }

    @Test void acceptsValidPaAndOptionalBlankAadhaarPan() {
        assertThatCode(() -> validator.validate(pa("+919876543210", "pa@example.com", null, null, "400001"))).doesNotThrowAnyException();
    }
    @Test void rejectsInvalidPaValues() {
        assertThatThrownBy(() -> validator.validate(pa("98765abc10", "sample@example", "123", "ABCDE12345", "411-033")))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsKeys("mobileNumber", "email", "aadharNumber", "panNumber", "pincode"));
    }
    @Test void requiresPaEmail() {
        assertThatThrownBy(() -> validator.validate(pa("9876543210", "", null, null, "110001")))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry("email", "Email ID is required"));
    }
    @Test void validatesOptionalPaAadhaarWhenProvided() {
        assertThatThrownBy(() -> validator.validate(pa(validPhone(), validEmail(), "999999999999", null, validPin())))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry("aadharNumber", "Aadhaar number cannot contain the same digit repeated 12 times"));
    }
    @Test void validatesOptionalPaPanWhenProvided() {
        assertThatThrownBy(() -> validator.validate(pa(validPhone(), validEmail(), null, "ABCDE12345", validPin())))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry("panNumber", "Last PAN character must be an alphabet"));
    }

    private void assertGroupError(GroupProposalRequest request, String field, String message) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ProposalContactValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry(field, message));
    }

    private Map<String, String> errors(Throwable ex) { return ((ProposalContactValidationException) ex).getFieldErrors(); }
    private String validPhone() { return "9876543210"; }
    private String validEmail() { return "sample@example.com"; }
    private String validAadhaar() { return "123456789012"; }
    private String validPan() { return "ABCDE1234F"; }
    private String validPin() { return "411033"; }

    private GroupProposalRequest group(String phone, String email, String aadhaar, String pan, String pincode) {
        return new GroupProposalRequest("Product", "New", pincode, "Delhi, Delhi", "A", "No",
                1, 0, "Sample Adult", "12-05-1988", "male", email, phone,
                BigDecimal.valueOf(500000), BigDecimal.ONE, "5L_8L",
                "No", "Yes", "No", aadhaar, pan, BigDecimal.valueOf(12000), BigDecimal.valueOf(14160),
                List.of(), List.of());
    }

    private PersonalAccidentProposalRequest pa(String mobile, String email, String aadhaar, String pan, String pincode) {
        return new PersonalAccidentProposalRequest("PA", "New", pincode, "Delhi", "Sample User", "male",
                "01-01-1990", mobile, email, aadhaar, pan, "Salaried", "5L_8L",
                "Engineer", BigDecimal.valueOf(500000), null);
    }
}
