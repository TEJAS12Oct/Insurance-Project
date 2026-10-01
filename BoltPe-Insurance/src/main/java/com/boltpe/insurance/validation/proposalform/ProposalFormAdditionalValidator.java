package com.boltpe.insurance.validation.proposalform;

import com.boltpe.insurance.dto.request.proposalform.AdultMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.ChildMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalAdditionalValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProposalFormAdditionalValidator {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal MIN_HEIGHT = BigDecimal.valueOf(30);
    private static final BigDecimal MAX_HEIGHT = BigDecimal.valueOf(250);
    private static final BigDecimal MIN_WEIGHT = BigDecimal.valueOf(5);
    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(300);
    private static final Pattern ABHA = Pattern.compile("^[0-9]{14}$");
    private static final Pattern DIGITS = Pattern.compile("^[0-9]+$");
    private static final int MAX_ADULTS = 4;
    private static final int MAX_CHILDREN = 4;
    private static final Set<String> ALLOWED_ZONES = Set.of("1", "2", "3");

    // Proposal-form cover amounts are stored and validated as their full backend numeric values.
    // Example: frontend display ₹50 Thousand -> backend value 50000.
    private static final Set<Long> HEALTH_COVER_AMOUNTS = Set.of(
            50000L, 75000L, 200000L, 300000L, 400000L, 500000L, 700000L,
            1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
            4000000L, 4500000L, 5000000L, 7500000L, 10000000L, 20000000L,
            30000000L, 40000000L, 50000000L);

    private static final Set<Long> SUPER_TOP_UP_COVER_AMOUNTS = Set.of(
            200000L, 300000L, 400000L, 500000L, 1000000L, 1500000L,
            2000000L, 2500000L, 5000000L, 10000000L, 30000000L);

    private static final Set<Long> PERSONAL_ACCIDENT_COVER_AMOUNTS = Set.of(
            1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
            5000000L, 7500000L, 10000000L, 20000000L, 30000000L,
            40000000L, 50000000L);

    private static final Set<Long> HOSPITAL_CASH_COVER_AMOUNTS = Set.of(
            1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
            5000000L, 7500000L, 10000000L, 20000000L, 30000000L,
            40000000L, 50000000L);

    /** Validates raw JSON values before Jackson can coerce incompatible values into DTO number fields. */
    public void validateRawData(Map<String, Object> data) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateRawZone(data, errors);
        validateRawWholeNumber(data.get("adultCount"), "adultCount", errors);
        validateRawWholeNumber(data.get("childCount"), "childCount", errors);
        validateRawTenure(data.get("selectedTenure"), errors);
        validateRawYesNo(data.get("nriDiscount"), "nriDiscount", errors);
        validateRawYesNo(data.get("includeSelf"), "includeSelf", errors);
        validateRawYesNo(data.get("bureauDiscount"), "bureauDiscount", errors);
        validateRawMembers(data.get("adults"), "adults", errors);
        validateRawMembers(data.get("children"), "children", errors);
        throwIfInvalid(errors);
    }

    /**
     * Validates proposal cover amount before DTO conversion so decimals, text and other
     * incompatible JSON values cannot be coerced by Jackson.
     * Group proposals use sumInsured in the existing contract; PA uses coverAmount.
     */
    public void validateRawCoverAmount(Map<String, Object> data, String insuranceType) {
        Map<String, String> errors = new LinkedHashMap<>();
        String sourceField = "PERSONAL_ACCIDENT".equals(insuranceType) ? "coverAmount" : "sumInsured";
        Object value = data.get(sourceField);
        validateCoverAmount(value, insuranceType, errors);
        throwIfInvalid(errors);
    }

    public void validateRawCounts(Map<String, Object> data) {
        validateRawData(data);
    }

    public void validate(GroupProposalRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateZone(request.zone(), errors);
        validateCounts(request, errors);
        validateTenure(request.selectedTenure(), errors);
        validateYesNo("nriDiscount", request.nriDiscount(), errors);
        validateYesNo("includeSelf", request.includeSelf(), errors);
        validateYesNo("bureauDiscount", request.bureauDiscount(), errors);
        validateAdults(request.adults(), errors);
        validateChildren(request.children(), errors);
        throwIfInvalid(errors);
    }

    private void validateCoverAmount(Object value, String insuranceType, Map<String, String> errors) {
        if (value == null) {
            errors.put("coverAmount", "coverAmount is required");
            return;
        }
        if (!(value instanceof Number)) {
            errors.put("coverAmount", invalidCoverAmountMessage(insuranceType));
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(value.toString());
        } catch (RuntimeException ex) {
            errors.put("coverAmount", invalidCoverAmountMessage(insuranceType));
            return;
        }

        if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0) {
            errors.put("coverAmount", invalidCoverAmountMessage(insuranceType));
            return;
        }

        long backendValue;
        try {
            backendValue = amount.longValueExact();
        } catch (ArithmeticException ex) {
            errors.put("coverAmount", invalidCoverAmountMessage(insuranceType));
            return;
        }

        Set<Long> allowed = switch (insuranceType) {
            case "HEALTH" -> HEALTH_COVER_AMOUNTS;
            case "SUPER_TOP_UP" -> SUPER_TOP_UP_COVER_AMOUNTS;
            case "PERSONAL_ACCIDENT" -> PERSONAL_ACCIDENT_COVER_AMOUNTS;
            case "HOSPITAL_CASH" -> HOSPITAL_CASH_COVER_AMOUNTS;
            default -> Set.of();
        };

        if (!allowed.contains(backendValue)) {
            errors.put("coverAmount", invalidCoverAmountMessage(insuranceType));
        }
    }

    private String invalidCoverAmountMessage(String insuranceType) {
        return "Invalid coverAmount for " + insuranceType + " insurance";
    }

    private void validateRawZone(Map<String, Object> data, Map<String, String> errors) {
        if (!data.containsKey("zone") || data.get("zone") == null) {
            errors.put("zone", "zone is required");
            return;
        }

        Object rawZone = data.get("zone");
        if (rawZone instanceof String zone && zone.isBlank()) {
            errors.put("zone", "zone is required");
            return;
        }

        // Proposal zone accepts only the exact values 1, 2 or 3.
        // Numeric, decimal, alphabetic, special-character and multi-digit inputs
        // are rejected unless their raw value is exactly the string 1, 2 or 3.
        if (!(rawZone instanceof String zone) || !ALLOWED_ZONES.contains(zone)) {
            errors.put("zone", "zone must be one of: 1, 2, 3");
        }
    }

    private void validateZone(String zone, Map<String, String> errors) {
        if (zone == null || zone.isBlank()) {
            errors.put("zone", "zone is required");
        } else if (!ALLOWED_ZONES.contains(zone)) {
            errors.put("zone", "zone must be one of: 1, 2, 3");
        }
    }

    private void validateRawWholeNumber(Object value, String field, Map<String, String> errors) {
        if (value == null) return; // Bean validation supplies the required message.
        if (!(value instanceof Number)) {
            errors.put(field, field + " must be a whole number");
            return;
        }
        try {
            BigDecimal number = new BigDecimal(value.toString());
            if (number.stripTrailingZeros().scale() > 0) {
                errors.put(field, field + " must be a whole number");
            }
        } catch (RuntimeException ex) {
            errors.put(field, field + " must be a whole number");
        }
    }

    private void validateRawTenure(Object value, Map<String, String> errors) {
        if (value == null) return;
        if (!(value instanceof Number)) {
            errors.put("selectedTenure", "selectedTenure must be 1 year");
        }
    }

    private void validateRawMembers(Object value, String arrayField, Map<String, String> errors) {
        if (!(value instanceof List<?> members)) return;
        for (int i = 0; i < members.size(); i++) {
            Object member = members.get(i);
            if (!(member instanceof Map<?, ?> map)) continue;
            validateRawMeasurement(map.get("height"), arrayField + "[" + i + "].height",
                    "Height must be a numeric value", errors);
            validateRawMeasurement(map.get("weight"), arrayField + "[" + i + "].weight",
                    "Weight must be a numeric value", errors);
        }
    }

    private void validateRawMeasurement(Object value, String field, String message, Map<String, String> errors) {
        if (value != null && !(value instanceof Number)) errors.put(field, message);
    }

    private void validateCounts(
            GroupProposalRequest request,
            Map<String, String> errors) {

        Integer adultCount = request.adultCount();
        Integer childCount = request.childCount();

        List<AdultMemberRequest> adults =
                request.adults() == null ? List.of() : request.adults();

        List<ChildMemberRequest> children =
                request.children() == null ? List.of() : request.children();

        // ---------------- ADULT ----------------

        int actualAdults =
                (int) adults.stream()
                        .filter(this::isPopulated)
                        .count();

        if (adultCount != null) {
            if (adultCount < 1 || adultCount > 4) {
                errors.put(
                        "adultCount",
                        "adultCount must be between 1 and 4"
                );
            } else if (actualAdults != adultCount) {
                errors.put(
                        "adultCount",
                        "adultCount must match the number of adult members"
                );
            }
        }

        if (actualAdults > 4) {
            errors.put(
                    "adults",
                    "Maximum 4 adult members are allowed"
            );
        }


        // ---------------- CHILD ----------------
        // Children are optional.

        int actualChildren =
                (int) children.stream()
                        .filter(this::isPopulated)
                        .count();

        if (childCount != null) {

            // 0 means no children.
            // Maximum total children = 4.
            if (childCount < 0 || childCount > 4) {
                errors.put(
                        "childCount",
                        "childCount must be between 0 and 4"
                );
            } else if (actualChildren != childCount) {
                errors.put(
                        "childCount",
                        "childCount must match the number of child members"
                );
            }
        }

        if (actualChildren > 4) {
            errors.put(
                    "children",
                    "Maximum 4 child members are allowed"
            );
        }
    }

    private void validateTenure(BigDecimal tenure, Map<String, String> errors) {
        if (tenure != null && tenure.compareTo(ONE) != 0) {
            errors.put("selectedTenure", "selectedTenure must be 1 year");
        }
    }

    private void validateAdults(List<AdultMemberRequest> adults, Map<String, String> errors) {
        Set<String> relationships = new HashSet<>();
        for (int i = 0; i < adults.size(); i++) {
            AdultMemberRequest adult = adults.get(i);
            if (!isPopulated(adult)) continue;
            String prefix = "adults[" + i + "]";
            validateMeasurement(adult.height(), MIN_HEIGHT, MAX_HEIGHT, prefix + ".height",
                    "Height is required", "Height must be between 30 and 250 cm", errors);
            validateMeasurement(adult.weight(), MIN_WEIGHT, MAX_WEIGHT, prefix + ".weight",
                    "Weight is required", "Weight must be between 5 and 300 kg", errors);
            validateAbha(adult.abhaId(), prefix + ".abhaId", errors);

            String relationship = normalize(adult.relationship());
            if (!relationship.isEmpty() && !relationships.add(relationship)) {
                errors.putIfAbsent(prefix + ".relationship", "Duplicate adult relationship is not allowed");
            }
            String title = adult.title() == null ? "" : adult.title().trim();
            if ("father".equals(relationship) && !"mr".equalsIgnoreCase(title)) {
                errors.put(prefix + ".title", "Title must be Mr when relationship is father");
            } else if ("mother".equals(relationship) && !"mrs".equalsIgnoreCase(title)) {
                errors.put(prefix + ".title", "Title must be Mrs when relationship is mother");
            }
        }
    }

    private void validateChildren(List<ChildMemberRequest> children, Map<String, String> errors) {
        for (int i = 0; i < children.size(); i++) {
            ChildMemberRequest child = children.get(i);
            if (!isPopulated(child)) continue;
            String prefix = "children[" + i + "]";
            validateMeasurement(child.height(), MIN_HEIGHT, MAX_HEIGHT, prefix + ".height",
                    "Height is required", "Height must be between 30 and 250 cm", errors);
            validateMeasurement(child.weight(), MIN_WEIGHT, MAX_WEIGHT, prefix + ".weight",
                    "Weight is required", "Weight must be between 5 and 300 kg", errors);
        }
    }

    private void validateMeasurement(BigDecimal value, BigDecimal min, BigDecimal max, String field,
                                     String requiredMessage, String rangeMessage, Map<String, String> errors) {
        if (value == null) {
            errors.put(field, requiredMessage);
        } else if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            errors.put(field, rangeMessage);
        }
    }

    private void validateAbha(String value, String field, Map<String, String> errors) {
        if (value == null || value.isBlank()) return; // ABHA is optional.
        if (value.length() != 14 && DIGITS.matcher(value).matches()) {
            errors.put(field, "ABHA Number must contain exactly 14 digits");
        } else if (value.matches(".*[A-Za-z].*") && !value.matches(".*[\\s\\-@#$%^&*()_+=].*")) {
            errors.put(field, "ABHA Number must contain digits only");
        } else if (!ABHA.matcher(value).matches()) {
            errors.put(field, "Invalid ABHA Number format");
        }
    }

    private boolean isPopulated(AdultMemberRequest a) {
        return a != null && (notBlank(a.relationship()) || notBlank(a.title()) || notBlank(a.fullName()) ||
                notBlank(a.dob()) || a.height() != null || a.weight() != null || notBlank(a.abhaId()) ||
                notBlank(a.preExistingDisease()) || notBlank(a.medicalCondition()));
    }

    private boolean isPopulated(ChildMemberRequest c) {
        return c != null && (notBlank(c.relationship()) || notBlank(c.fullName()) || notBlank(c.dob()) ||
                c.height() != null || c.weight() != null || notBlank(c.medicalCondition()));
    }

    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private void throwIfInvalid(Map<String, String> errors) {
        if (!errors.isEmpty()) throw new ProposalAdditionalValidationException(errors);
    }

    private void validateYesNo(
            String fieldName,
            String value,
            Map<String, String> errors
    ) {
        if (value == null || value.isBlank()) {
            errors.put(fieldName, fieldName + " is required");
            return;
        }

        if (!value.equalsIgnoreCase("Yes")
                && !value.equalsIgnoreCase("No")) {
            errors.put(
                    fieldName,
                    fieldName + " must be either Yes or No"
            );
        }
    }


    private void validateRawYesNo(
            Object value,
            String fieldName,
            Map<String, String> errors
    ) {
        if (value == null) {
            errors.put(fieldName, fieldName + " is required");
            return;
        }

        if (!(value instanceof String stringValue)) {
            errors.put(fieldName, fieldName + " must be either Yes or No");
            return;
        }

        validateYesNo(fieldName, stringValue, errors);
    }

}
