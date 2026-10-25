package com.boltpe.insurance.validation.proposalform;

import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.dto.request.proposalform.PersonalAccidentProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalContactValidationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProposalFormContactIdentityValidator {
    private static final String FIELD_AADHAAR = "aadharNumber";
    private static final String FIELD_PAN = "panNumber";
    private static final String FIELD_PINCODE = "pincode";

    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final Pattern MOBILE = Pattern.compile("^(?:\\+91)?[6-9][0-9]{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern AADHAAR = Pattern.compile("^[0-9]{12}$");
    private static final Pattern AADHAAR_REPEATED_DIGIT = Pattern.compile("^(\\d)\\1{11}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern LETTERS_5 = Pattern.compile("^[A-Z]{5}$");
    private static final Pattern DIGITS_4 = Pattern.compile("^[0-9]{4}$");
    private static final Pattern UPPERCASE_LETTER = Pattern.compile("^[A-Z]$");
    private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");

    public void validate(GroupProposalRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateMobile("proposerPhone", request.proposerPhone(), errors);
        validateEmail("proposerEmail", request.proposerEmail(), true, errors);
        validateAadhaar(request.aadharNumber(), true, errors);
        validatePan(request.panNumber(), true, errors);
        validatePincode(request.pincode(), errors);
        throwIfInvalid(errors);
    }

    public void validate(PersonalAccidentProposalRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateMobile("mobileNumber", request.mobileNumber(), errors);
        validateEmail("email", request.email(), true, errors);
        validateAadhaar(request.aadharNumber(), false, errors);
        validatePan(request.panNumber(), false, errors);
        validatePincode(request.pincode(), errors);
        throwIfInvalid(errors);
    }

    private void validateMobile(String field, String value, Map<String, String> errors) {
        if (isBlank(value)) {
            errors.put(field, "Mobile number is required");
            return;
        }
        String local = value.startsWith("+91") ? value.substring(3) : value;
        if (!DIGITS.matcher(local).matches()) {
            if (value.startsWith("+91")) {
                errors.put(field, "Mobile number must be a valid 10-digit Indian number or +91 followed by 10 digits");
            } else {
                errors.put(field, "Mobile number must contain digits only, with optional +91 prefix");
            }
            return;
        }
        if (local.length() != 10) {
            errors.put(field, "Mobile number must contain exactly 10 digits");
            return;
        }
        if (local.charAt(0) < '6' || local.charAt(0) > '9') {
            errors.put(field, "Mobile number must start with 6, 7, 8, or 9");
            return;
        }
        if (!MOBILE.matcher(value).matches()) {
            errors.put(field, "Mobile number must be a valid 10-digit Indian number or +91 followed by 10 digits");
        }
    }

    private void validateEmail(String field, String value, boolean required, Map<String, String> errors) {
        if (isBlank(value)) {
            if (required) errors.put(field, "Email ID is required");
            return;
        }
        int firstAt = value.indexOf('@');
        if (firstAt < 0) {
            errors.put(field, "Email ID must contain a valid @ symbol");
            return;
        }
        if (firstAt != value.lastIndexOf('@') || value.contains(" ")) {
            errors.put(field, "Invalid email ID format");
            return;
        }
        if (firstAt == 0) {
            errors.put(field, "Email ID must contain a valid username before @");
            return;
        }
        String domain = value.substring(firstAt + 1);
        if (domain.isEmpty()) {
            errors.put(field, "Email ID must contain a valid domain");
            return;
        }
        int lastDot = domain.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == domain.length() - 1) {
            errors.put(field, "Email ID must contain a valid domain extension");
            return;
        }
        if (!EMAIL.matcher(value).matches()) {
            errors.put(field, "Invalid email ID format");
        }
    }

    private void validateAadhaar(String value, boolean required, Map<String, String> errors) {
        if (isBlank(value)) {
            if (required) errors.put(FIELD_AADHAAR, "Aadhaar number is required");
            return;
        }
        if (value.length() != 12 && DIGITS.matcher(value).matches()) {
            errors.put(FIELD_AADHAAR, "Aadhaar number must contain exactly 12 digits");
            return;
        }
        if (value.matches(".*[A-Za-z].*") && !value.matches(".*[\\s\\-@#$%^&*()_+=].*")) {
            errors.put(FIELD_AADHAAR, "Aadhaar number must contain digits only");
            return;
        }
        if (!AADHAAR.matcher(value).matches()) {
            errors.put(FIELD_AADHAAR, "Invalid Aadhaar number format");
            return;
        }
        if (AADHAAR_REPEATED_DIGIT.matcher(value).matches()) {
            errors.put(FIELD_AADHAAR, "Aadhaar number cannot contain the same digit repeated 12 times");
        }
    }

    private void validatePan(String value, boolean required, Map<String, String> errors) {
        if (isBlank(value)) {
            if (required) errors.put(FIELD_PAN, "PAN number is required");
            return;
        }
        if (value.length() != 10) {
            errors.put(FIELD_PAN, "PAN number must be exactly 10 characters");
            return;
        }
        if (!value.matches("[A-Za-z0-9]{10}")) {
            errors.put(FIELD_PAN, "Invalid PAN number format");
            return;
        }
        if (!LETTERS_5.matcher(value.substring(0, 5)).matches()) {
            errors.put(FIELD_PAN, "First 5 PAN characters must be alphabets");
            return;
        }
        if (!DIGITS_4.matcher(value.substring(5, 9)).matches()) {
            errors.put(FIELD_PAN, "PAN characters 6 to 9 must be digits");
            return;
        }
        if (!UPPERCASE_LETTER.matcher(value.substring(9)).matches()) {
            errors.put(FIELD_PAN, "Last PAN character must be an alphabet");
            return;
        }
        if (!PAN.matcher(value).matches()) {
            errors.put(FIELD_PAN, "Invalid PAN number format");
        }
    }

    private void validatePincode(String value, Map<String, String> errors) {
        if (isBlank(value)) {
            errors.put(FIELD_PINCODE, "Pincode is required");
            return;
        }
        if (value.length() != 6 && DIGITS.matcher(value).matches()) {
            errors.put(FIELD_PINCODE, "Pincode must contain exactly 6 digits");
            return;
        }
        if (value.length() == 6 && value.charAt(0) == '0' && DIGITS.matcher(value).matches()) {
            errors.put(FIELD_PINCODE, "Pincode must not start with 0");
            return;
        }
        if (value.matches(".*[A-Za-z].*")) {
            errors.put(FIELD_PINCODE, "Pincode must contain digits only");
            return;
        }
        if (!PINCODE.matcher(value).matches()) {
            errors.put(FIELD_PINCODE, "Pincode must be a valid 6-digit Indian pincode and must not start with 0");
        }
    }

    private void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) throw new ProposalContactValidationException(errors);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
