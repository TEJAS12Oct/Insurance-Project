package com.boltpe.insurance.dto.request;

import com.boltpe.insurance.enums.InsuranceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused") // Bean Validation invokes @AssertTrue methods reflectively.
public record InsuranceRequest(
        @NotNull InsuranceType insuranceType,
        Map<String, Object> userInformation,
        @NotNull Map<String, Object> data) {
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_SPOUSE_AGE = "spouseAge";
    private static final String FIELD_CHILD1_AGE = "child1Age";
    private static final String FIELD_CHILD2_AGE = "child2Age";
    private static final String FIELD_CHILD3_AGE = "child3Age";
    private static final String FIELD_CHILD4_AGE = "child4Age";
    private static final String FIELD_COVER_AMOUNT = "coverAmount";


    public InsuranceRequest {
        userInformation = userInformation == null ? Map.of() : Map.copyOf(userInformation);
        data = data == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }
    @AssertTrue(message = "Kindly select cover amount")
    public boolean isCoverAmountSelected() {
        if (insuranceType == null) return true;
        return coverAmountValue() != null;
    }

    @AssertTrue(message = "Invalid cover amount. Please select a cover amount from the available coverage options.")
    public boolean isCoverAmountValid() {
        if (insuranceType == null) return true;
        Long coverAmount = coverAmountValue();
        return coverAmount == null || switch (insuranceType) {
            case HEALTH -> HealthInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount);
            case SUPER_TOP_UP -> SuperTopUpInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount);
            case PERSONAL_ACCIDENT -> PersonalAccidentInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount);
            case HOSPITAL_CASH -> HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount);
        };
    }

    @AssertTrue(message = "age is required")
    public boolean isAgeSelected() {
        if (insuranceType == null) return true;
        return data.containsKey("age") && data.get("age") != null;
    }

    @AssertTrue(message = "age must be at least 18")
    public boolean isMinimumAdultValid() {
        if (insuranceType == null) return true;
        return switch (insuranceType) {
            case HEALTH, SUPER_TOP_UP, PERSONAL_ACCIDENT, HOSPITAL_CASH -> {
                Integer age = intValue("age");
                yield age == null || age >= 18;
            }
        };
    }

    @AssertTrue(message = "zone is required")
    public boolean isHealthZoneSelected() {
        if (insuranceType != InsuranceType.HEALTH) return true;
        return hasText("zone");
    }

    @AssertTrue(message = "zone must be one of: 1, 2, 3")
    public boolean isHealthZoneValid() {
        if (insuranceType != InsuranceType.HEALTH || !hasText("zone")) return true;
        return isExactCode123("zone");
    }
    @AssertTrue(message = "Kindly select category")
    public boolean isPersonalAccidentCategorySelected() {
        if (insuranceType != InsuranceType.PERSONAL_ACCIDENT) return true;
        return hasText(FIELD_CATEGORY);
    }

    @AssertTrue(message = "category must be one of: 1, 2, 3")
    public boolean isPersonalAccidentCategoryValid() {
        if (insuranceType != InsuranceType.PERSONAL_ACCIDENT || !hasText(FIELD_CATEGORY)) return true;
        return isExactCode123(FIELD_CATEGORY);
    }

    @AssertTrue(message = "Kindly select at least 1 adult")
    public boolean isHospitalCashMinimumAdultValid() {
        if (insuranceType != InsuranceType.HOSPITAL_CASH) return true;
        Integer adults = intValue("noOfAdults");
        return adults == null || adults >= 1;
    }

    @AssertTrue(message = "age must be a valid whole number")
    public boolean isAdultAgeWholeNumber() {
        if (insuranceType == null) return true;
        return isWholeNumber("age");
    }

    @AssertTrue(message = "spouse_age must be a valid whole number")
    public boolean isSpouseAgeWholeNumber() {
        if (insuranceType != InsuranceType.HEALTH && insuranceType != InsuranceType.SUPER_TOP_UP) return true;
        return isWholeNumber(FIELD_SPOUSE_AGE);
    }

    @AssertTrue(message = "spouse_age must be at least 18")
    public boolean isSpouseMinimumAgeValid() {
        if (insuranceType != InsuranceType.HEALTH && insuranceType != InsuranceType.SUPER_TOP_UP) return true;
        Integer spouse = intValue(FIELD_SPOUSE_AGE);
        return spouse == null || spouse >= 18;
    }

    @AssertTrue(message = "child_N_age must be a valid whole number")
    public boolean areChildAgesWholeNumbers() {
        if (isNotFamilyProduct()) return true;
        return isWholeNumber(FIELD_CHILD1_AGE) && isWholeNumber(FIELD_CHILD2_AGE) && isWholeNumber(FIELD_CHILD3_AGE) && isWholeNumber(FIELD_CHILD4_AGE);
    }

    @AssertTrue(message = "child_N_age cannot be negative")
    public boolean areChildAgesNonNegative() {
        if (isNotFamilyProduct()) return true;
        for (String key : childKeys()) {
            Integer child = intValue(key);
            if (child != null && child < 0) return false;
        }
        return true;
    }

    @AssertTrue(message = "parent age must be at least 21 years greater than child_N_age")
    public boolean isAdultChildGapValid() {
        if (isNotFamilyProduct()) return true;
        Integer adult = intValue("age");
        if (adult == null) return true;
        for (String key : childKeys()) {
            Integer child = intValue(key);
            if (child != null && adult - child < 21) return false;
        }
        return true;
    }

    @AssertTrue(message = "spouse age must be at least 21 years greater than child_N_age")
    public boolean isSpouseChildGapValid() {
        if (isNotFamilyProduct()) return true;
        Integer spouse = intValue(FIELD_SPOUSE_AGE);
        if (spouse == null) return true;
        for (String key : childKeys()) {
            Integer child = intValue(key);
            if (child != null && spouse - child < 21) return false;
        }
        return true;
    }

    @AssertTrue(message = "minimum age difference between children must be 2 years")
    public boolean isChildChildGapValid() {
        if (isNotFamilyProduct()) return true;
        java.util.List<Integer> children = new java.util.ArrayList<>();
        for (String key : childKeys()) {
            Integer child = intValue(key);
            if (child != null) children.add(child);
        }
        for (int i = 0; i < children.size(); i++)
            for (int j = i + 1; j < children.size(); j++)
                if (Math.abs(children.get(i) - children.get(j)) < 2) return false;
        return true;
    }

    @AssertTrue(message = "Invalid insurance request data")
    public boolean isDataValid() {
        if (insuranceType == null || data == null || data.isEmpty()) return false;
        return switch (insuranceType) {
            case HEALTH -> validHealthData();
            case SUPER_TOP_UP -> validSuperTopUpData();
            case PERSONAL_ACCIDENT -> validPersonalAccidentData();
            case HOSPITAL_CASH -> validHospitalCashData();
        };
    }


    private boolean validPersonalAccidentData() {
        Long coverAmount = coverAmountValue();
        Integer category = intValue(FIELD_CATEGORY);
        return coverAmount != null
                && PersonalAccidentInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount)
                && category != null;
    }

    private boolean validHospitalCashData() {
        Long coverAmount = coverAmountValue();
        Integer adults = intValue("noOfAdults");
        Integer children = intValue("noOfChildren");
        Integer days = intValue("noOfDays");
        return coverAmount != null
                && HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount)
                && adults != null && adults >= 1
                && children != null && children >= 0
                && days != null
                && HospitalCashInsuranceRequest.ALLOWED_DURATIONS.contains(days);
    }

    private boolean validHealthData() {
        Long coverAmount = coverAmountValue();
        Integer age = intValue("age");
        Object zone = data.get("zone");
        return coverAmount != null
                && HealthInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount)
                && zone != null
                && ("1".equals(String.valueOf(zone)) || "2".equals(String.valueOf(zone)) || "3".equals(String.valueOf(zone)))
                && age != null;
    }

    private boolean validSuperTopUpData() {
        Long coverAmount = coverAmountValue();
        Integer age = intValue("age");
        return coverAmount != null
                && SuperTopUpInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(coverAmount)
                && age != null;
    }


    private boolean hasText(String key) {
        if (!data.containsKey(key) || data.get(key) == null) return false;
        return !String.valueOf(data.get(key)).trim().isEmpty();
    }

    private boolean isExactCode123(String key) {
        Object value = data.get(key);
        if (value == null) return false;
        String text = String.valueOf(value).trim();
        return "1".equals(text) || "2".equals(text) || "3".equals(text);
    }

    private boolean isNotFamilyProduct() {
        return insuranceType != InsuranceType.HEALTH && insuranceType != InsuranceType.SUPER_TOP_UP;
    }

    private static final String[] CHILD_KEYS = {FIELD_CHILD1_AGE, FIELD_CHILD2_AGE, FIELD_CHILD3_AGE, FIELD_CHILD4_AGE};

    private static String[] childKeys() {
        return CHILD_KEYS.clone();
    }

    private boolean isWholeNumber(String key) {
        if (!data.containsKey(key) || data.get(key) == null) return true;
        Object value = data.get(key);
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger) return true;
        if (value instanceof java.math.BigDecimal decimal) return decimal.stripTrailingZeros().scale() <= 0;
        if (value instanceof Float || value instanceof Double) {
            double d = ((Number) value).doubleValue();
            return Double.isFinite(d) && d == Math.rint(d);
        }
        try { Integer.valueOf(String.valueOf(value)); return true; }
        catch (NumberFormatException ex) { return false; }
    }

    private Integer intValue(String key) {
        Object value = data.get(key);
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ex) { return null; }
    }

    private Long coverAmountValue() {
        Object value = data.get(FIELD_COVER_AMOUNT);
        if (value == null) {
            return null;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof java.math.BigInteger integer) {
            return exactLong(integer);
        }
        if (value instanceof java.math.BigDecimal decimal) {
            return exactLong(decimal);
        }
        if (value instanceof Float || value instanceof Double) {
            return exactFloatingLong(((Number) value).doubleValue());
        }
        return parseLong(value);
    }

    private static Long exactLong(java.math.BigInteger number) {
        try {
            return number.longValueExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private static Long exactLong(java.math.BigDecimal number) {
        try {
            return number.longValueExact();
        } catch (ArithmeticException ex) {
            return null;
        }
    }

    private static Long exactFloatingLong(double number) {
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < Long.MIN_VALUE || number > Long.MAX_VALUE) {
            return null;
        }
        return (long) number;
    }

    private static Long parseLong(Object value) {
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Keeps existing Health unit tests/callers source-compatible. */
    public InsuranceRequest(InsuranceType insuranceType, Map<String, Object> userInformation, HealthInsuranceRequest health) {
        this(insuranceType, userInformation, healthToMap(health));
    }

    private static Map<String, Object> healthToMap(HealthInsuranceRequest h) {
        if (h == null) return Map.of();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(FIELD_COVER_AMOUNT, h.coverAmount());
        m.put("zone", h.zone());
        m.put("age", h.age());
        m.put(FIELD_SPOUSE_AGE, h.spouseAge());
        m.put(FIELD_CHILD1_AGE, h.child1Age());
        m.put(FIELD_CHILD2_AGE, h.child2Age());
        m.put(FIELD_CHILD3_AGE, h.child3Age());
        m.put(FIELD_CHILD4_AGE, h.child4Age());
        return m;
    }
}
