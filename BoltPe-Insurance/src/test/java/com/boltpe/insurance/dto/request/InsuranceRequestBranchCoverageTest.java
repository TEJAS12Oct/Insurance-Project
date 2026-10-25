package com.boltpe.insurance.dto.request;

import com.boltpe.insurance.enums.InsuranceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for insurance quote request validation and conversion edge cases. */
class InsuranceRequestBranchCoverageTest {
    private InsuranceRequest req(InsuranceType type, Object... entries) {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) data.put((String) entries[i], entries[i + 1]);
        return new InsuranceRequest(type, Map.of(), data);
    }

    @Test void absentInsuranceTypeAndRequiredFields() {
        var missingType = req(null);
        assertTrue(missingType.isCoverAmountSelected());
        assertTrue(missingType.isCoverAmountValid());
        assertTrue(missingType.isAgeSelected());
        assertTrue(missingType.isMinimumAdultValid());
        assertTrue(missingType.isAdultAgeWholeNumber());
        assertFalse(missingType.isDataValid());
        for (InsuranceType type : InsuranceType.values()) {
            var empty = req(type);
            assertFalse(empty.isCoverAmountSelected());
            assertTrue(empty.isCoverAmountValid());
            assertFalse(empty.isAgeSelected());
            assertFalse(empty.isDataValid());
        }
    }

    @Test void healthZoneAndAgeBoundaries() {
        for (Object zone : new Object[]{"1", "2", "3", 1, 2, 3}) {
            var valid = req(InsuranceType.HEALTH, "coverAmount", 1000000, "age", 18, "zone", zone);
            assertTrue(valid.isHealthZoneSelected());
            assertTrue(valid.isHealthZoneValid());
            assertTrue(valid.isDataValid());
            assertTrue(valid.isMinimumAdultValid());
        }
        for (Object zone : new Object[]{"0", "4", "01", "invalid"}) {
            assertFalse(req(InsuranceType.HEALTH, "zone", zone).isHealthZoneValid(), "Invalid zone accepted: " + zone);
        }
        assertFalse(req(InsuranceType.HEALTH).isHealthZoneSelected());
        assertFalse(req(InsuranceType.HEALTH, "zone", " ").isHealthZoneSelected());
        // Blank zone is handled by the required-field check, not the allowed-values check.
        assertTrue(req(InsuranceType.HEALTH, "zone", " ").isHealthZoneValid());
        assertFalse(req(InsuranceType.HEALTH, "age", 17).isMinimumAdultValid());
        assertTrue(req(InsuranceType.HEALTH, "age", 18).isMinimumAdultValid());
    }

    @Test void coverAmountNumericFormats() {
        Object[] valid = {1000000, 1000000L, "1000000", BigInteger.valueOf(1000000),
                new BigDecimal("1000000.00"), 1000000.0d, 1000000.0f};
        for (Object amount : valid) {
            var request = req(InsuranceType.HEALTH, "coverAmount", amount);
            assertTrue(request.isCoverAmountSelected(), "amount=" + amount);
            assertTrue(request.isCoverAmountValid(), "amount=" + amount);
        }
        Object[] invalid = {"abc", "1000000.1", new BigDecimal("1000000.1"),
                BigInteger.TEN.pow(30), Double.NaN, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, 1000000.5d, 1000000.5f};
        for (Object amount : invalid) {
            assertFalse(req(InsuranceType.HEALTH, "coverAmount", amount).isCoverAmountSelected(), "amount=" + amount);
        }
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 123).isCoverAmountValid());
    }

    @Test void personalAccidentCategoryValidation() {
        for (Object category : new Object[]{1, 2, 3, "1", "2", "3"}) {
            var valid = req(InsuranceType.PERSONAL_ACCIDENT, "coverAmount", 1000000,
                    "age", 26, "category", category);
            assertTrue(valid.isPersonalAccidentCategorySelected());
            assertTrue(valid.isPersonalAccidentCategoryValid());
            assertTrue(valid.isDataValid());
        }
        for (Object category : new Object[]{0, 4, "01", "x"}) {
            assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "category", category).isPersonalAccidentCategoryValid());
        }
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT).isPersonalAccidentCategorySelected());
        assertTrue(req(InsuranceType.HEALTH).isPersonalAccidentCategoryValid());
    }

    @Test void hospitalCashDurationAndMemberCounts() {
        for (int days : new int[]{30, 60, 90, 180}) {
            var valid = req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000,
                    "age", 18, "noOfAdults", 1, "noOfChildren", 0, "noOfDays", days);
            assertTrue(valid.isHospitalCashMinimumAdultValid());
            assertTrue(valid.isDataValid());
        }
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000,
                "age", 18, "noOfAdults", 1, "noOfChildren", -1, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000,
                "age", 18, "noOfAdults", 1, "noOfChildren", 0, "noOfDays", 31).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "noOfAdults", 0).isHospitalCashMinimumAdultValid());
        assertTrue(req(InsuranceType.HEALTH).isHospitalCashMinimumAdultValid());
    }

    @Test void familyAgeBoundariesAndSiblingGap() {
        for (InsuranceType type : new InsuranceType[]{InsuranceType.HEALTH, InsuranceType.SUPER_TOP_UP}) {
            var valid = req(type, "age", 45, "spouseAge", 42, "child1Age", 20, "child2Age", 18);
            assertTrue(valid.isSpouseMinimumAgeValid());
            assertTrue(valid.isAdultChildGapValid());
            assertTrue(valid.isSpouseChildGapValid());
            assertTrue(valid.isChildChildGapValid());
            assertTrue(valid.areChildAgesNonNegative());
            assertTrue(valid.areChildAgesWholeNumbers());
            assertFalse(req(type, "spouseAge", 17).isSpouseMinimumAgeValid());
            assertFalse(req(type, "age", 30, "child1Age", 10).isAdultChildGapValid());
            assertFalse(req(type, "spouseAge", 30, "child1Age", 10).isSpouseChildGapValid());
            assertFalse(req(type, "child1Age", 8, "child2Age", 9).isChildChildGapValid());
            assertFalse(req(type, "child1Age", -1).areChildAgesNonNegative());
            assertFalse(req(type, "child3Age", "not-an-age").areChildAgesWholeNumbers());
            assertFalse(req(type, "spouseAge", "invalid").isSpouseAgeWholeNumber());
        }
        assertTrue(req(InsuranceType.PERSONAL_ACCIDENT, "child1Age", -1).areChildAgesNonNegative());
        assertTrue(req(InsuranceType.HOSPITAL_CASH, "spouseAge", 15).isSpouseMinimumAgeValid());
    }

    @Test void wholeNumberParsingRejectsFractionsAndInvalidStrings() {
        for (Object invalid : new Object[]{"bad", "12.5", 12.5d, Double.NaN, Double.POSITIVE_INFINITY,
                new BigDecimal("12.5")}) {
            assertFalse(req(InsuranceType.HEALTH, "age", invalid).isAdultAgeWholeNumber());
        }
        for (Object valid : new Object[]{18, 18L, (short) 18, (byte) 18, "18", BigInteger.valueOf(18),
                new BigDecimal("18.000"), 18.0d, 18.0f}) {
            assertTrue(req(InsuranceType.HEALTH, "age", valid).isAdultAgeWholeNumber());
        }
        assertFalse(req(InsuranceType.HEALTH, "age", Float.NaN).isAdultAgeWholeNumber());
        assertFalse(req(InsuranceType.HEALTH, "age", Float.POSITIVE_INFINITY).isAdultAgeWholeNumber());
    }

    @Test void coverAmountIsCheckedAgainstEachProductsAllowedList() {
        Map<InsuranceType, Object> allowed = Map.of(
                InsuranceType.HEALTH, 50000,
                InsuranceType.SUPER_TOP_UP, 500000,
                InsuranceType.PERSONAL_ACCIDENT, 1000000,
                InsuranceType.HOSPITAL_CASH, 1000);
        for (var entry : allowed.entrySet()) {
            assertTrue(req(entry.getKey(), "coverAmount", entry.getValue()).isCoverAmountValid(), entry.getKey().name());
            assertFalse(req(entry.getKey(), "coverAmount", 1234).isCoverAmountValid(), entry.getKey().name());
        }
        // An amount allowed for one product is not accepted for another.
        assertFalse(req(InsuranceType.SUPER_TOP_UP, "coverAmount", 50000).isCoverAmountValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000000).isCoverAmountValid());
    }

    @Test void coverAmountAcceptsSmallIntegerTypesAndRejectsOutOfRangeDoubles() {
        assertTrue(req(InsuranceType.HOSPITAL_CASH, "coverAmount", (short) 1000).isCoverAmountValid());
        assertTrue(req(InsuranceType.HEALTH, "coverAmount", (byte) 100).isCoverAmountSelected());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", (byte) 100).isCoverAmountValid());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 1e30d).isCoverAmountSelected());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", -1e30d).isCoverAmountSelected());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", null).isCoverAmountSelected());
    }

    @Test void ageMustBePresentAndNonNull() {
        for (InsuranceType type : InsuranceType.values()) {
            assertTrue(req(type, "age", 30).isAgeSelected(), type.name());
            assertFalse(req(type, "age", null).isAgeSelected(), type.name());
            // Missing age is reported by isAgeSelected, not the minimum-age rule.
            assertTrue(req(type, "age", null).isMinimumAdultValid(), type.name());
            assertTrue(req(type).isMinimumAdultValid(), type.name());
        }
    }

    @Test void nonNumericValuesAreTreatedAsAbsentByRangeRules() {
        assertTrue(req(InsuranceType.HEALTH, "age", "abc").isMinimumAdultValid());
        assertTrue(req(InsuranceType.HEALTH, "child1Age", "x").areChildAgesNonNegative());
        assertTrue(req(InsuranceType.HEALTH, "spouseAge", "x").isSpouseMinimumAgeValid());
        assertTrue(req(InsuranceType.HOSPITAL_CASH, "noOfAdults", "x").isHospitalCashMinimumAdultValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000, "noOfAdults", "x",
                "noOfChildren", 0, "noOfDays", 30).isDataValid());
    }

    @Test void productSpecificRulesIgnoreOtherProducts() {
        for (InsuranceType type : new InsuranceType[]{InsuranceType.SUPER_TOP_UP, InsuranceType.PERSONAL_ACCIDENT, InsuranceType.HOSPITAL_CASH}) {
            var request = req(type, "zone", "9");
            assertTrue(request.isHealthZoneSelected(), type.name());
            assertTrue(request.isHealthZoneValid(), type.name());
            assertTrue(req(type).isHealthZoneSelected(), type.name());
        }
        for (InsuranceType type : new InsuranceType[]{InsuranceType.HEALTH, InsuranceType.SUPER_TOP_UP, InsuranceType.HOSPITAL_CASH}) {
            assertTrue(req(type).isPersonalAccidentCategorySelected(), type.name());
            assertTrue(req(type, "category", "9").isPersonalAccidentCategoryValid(), type.name());
        }
        for (InsuranceType type : new InsuranceType[]{InsuranceType.HEALTH, InsuranceType.SUPER_TOP_UP, InsuranceType.PERSONAL_ACCIDENT}) {
            assertTrue(req(type, "noOfAdults", 0).isHospitalCashMinimumAdultValid(), type.name());
        }
    }

    @Test void familyRulesIgnoreNonFamilyProducts() {
        for (InsuranceType type : new InsuranceType[]{InsuranceType.PERSONAL_ACCIDENT, InsuranceType.HOSPITAL_CASH}) {
            var request = req(type, "age", 30, "spouseAge", "bad", "child1Age", "bad",
                    "child2Age", 29, "child3Age", 29);
            assertTrue(request.isSpouseAgeWholeNumber(), type.name());
            assertTrue(request.areChildAgesWholeNumbers(), type.name());
            assertTrue(request.isAdultChildGapValid(), type.name());
            assertTrue(request.isSpouseChildGapValid(), type.name());
            assertTrue(request.isChildChildGapValid(), type.name());
            assertTrue(req(type, "spouseAge", 15).isSpouseMinimumAgeValid(), type.name());
            assertTrue(req(type, "child1Age", -5).areChildAgesNonNegative(), type.name());
        }
    }

    @Test void familyRulesPassWhenOptionalMembersAreMissing() {
        for (InsuranceType type : new InsuranceType[]{InsuranceType.HEALTH, InsuranceType.SUPER_TOP_UP}) {
            var noMembers = req(type);
            assertTrue(noMembers.isSpouseAgeWholeNumber(), type.name());
            assertTrue(noMembers.isSpouseMinimumAgeValid(), type.name());
            assertTrue(noMembers.areChildAgesWholeNumbers(), type.name());
            assertTrue(noMembers.areChildAgesNonNegative(), type.name());
            assertTrue(noMembers.isChildChildGapValid(), type.name());
            // Children without an adult or spouse age cannot violate the parent gap.
            assertTrue(req(type, "child1Age", 10).isAdultChildGapValid(), type.name());
            assertTrue(req(type, "child1Age", 10).isSpouseChildGapValid(), type.name());
            assertTrue(req(type, "spouseAge", 18).isSpouseMinimumAgeValid(), type.name());
            assertTrue(req(type, "age", 31, "child1Age", 10).isAdultChildGapValid(), type.name());
            assertTrue(req(type, "spouseAge", 31, "child1Age", 10).isSpouseChildGapValid(), type.name());
            assertTrue(req(type, "child1Age", 10, "child2Age", 12).isChildChildGapValid(), type.name());
            assertTrue(req(type, "child1Age", 0).areChildAgesNonNegative(), type.name());
            assertFalse(req(type, "child1Age", "bad").areChildAgesWholeNumbers(), type.name());
            assertFalse(req(type, "child4Age", 4.5d).areChildAgesWholeNumbers(), type.name());
        }
    }

    @Test void blankCategoryIsReportedOnlyByRequiredCheck() {
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "category", " ").isPersonalAccidentCategorySelected());
        assertTrue(req(InsuranceType.PERSONAL_ACCIDENT, "category", " ").isPersonalAccidentCategoryValid());
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "category", null).isPersonalAccidentCategorySelected());
        assertFalse(req(InsuranceType.HEALTH, "zone", null).isHealthZoneSelected());
    }

    @Test void healthDataRejectsEachMissingOrInvalidField() {
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 1234, "zone", "1", "age", 30).isDataValid());
        assertFalse(req(InsuranceType.HEALTH, "zone", "1", "age", 30).isDataValid());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 1000000, "age", 30).isDataValid());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 1000000, "zone", "4", "age", 30).isDataValid());
        assertFalse(req(InsuranceType.HEALTH, "coverAmount", 1000000, "zone", "1").isDataValid());
    }

    @Test void superTopUpDataRejectsMissingOrUnsupportedCoverAmount() {
        assertFalse(req(InsuranceType.SUPER_TOP_UP, "age", 30).isDataValid());
        assertFalse(req(InsuranceType.SUPER_TOP_UP, "coverAmount", 50000, "age", 30).isDataValid());
    }

    @Test void personalAccidentDataRejectsMissingOrUnsupportedCoverAmount() {
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "category", 1, "age", 30).isDataValid());
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "coverAmount", 1234, "category", 1, "age", 30).isDataValid());
        assertFalse(req(InsuranceType.PERSONAL_ACCIDENT, "coverAmount", 1000000, "category", "x", "age", 30).isDataValid());
    }

    @Test void hospitalCashDataRejectsEachMissingOrInvalidField() {
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "noOfAdults", 1, "noOfChildren", 0, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1234, "noOfAdults", 1, "noOfChildren", 0, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000, "noOfChildren", 0, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000, "noOfAdults", 0, "noOfChildren", 0, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000, "noOfAdults", 1, "noOfDays", 30).isDataValid());
        assertFalse(req(InsuranceType.HOSPITAL_CASH, "coverAmount", 1000, "noOfAdults", 1, "noOfChildren", 0).isDataValid());
    }
}
