package com.boltpe.insurance.dto.request;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductRequestRecordCoverageTest {
    @Test void healthValidationCoversBoundariesAndNull() {
        assertTrue(new HealthInsuranceRequest(null,"1",null,null,null,null,null,null).isCoverAmountSupported());
        assertTrue(new HealthInsuranceRequest(1000000L,"1",18,null,null,null,null,null).isMinimumAdultValid());
        assertFalse(new HealthInsuranceRequest(999L,"1",18,null,null,null,null,null).isCoverAmountSupported());
        assertFalse(new HealthInsuranceRequest(1000000L,"1",17,null,null,null,null,null).isMinimumAdultValid());
    }
    @Test void superTopUpValidationCoversBoundariesAndNull() {
        assertTrue(new SuperTopUpInsuranceRequest(null,null,null,null,null,null,null).isCoverAmountSupported());
        assertTrue(new SuperTopUpInsuranceRequest(1000000L,18,null,null,null,null,null).isMinimumAdultValid());
        assertFalse(new SuperTopUpInsuranceRequest(999L,18,null,null,null,null,null).isCoverAmountSupported());
        assertFalse(new SuperTopUpInsuranceRequest(1000000L,17,null,null,null,null,null).isMinimumAdultValid());
    }
    @Test void hospitalCashValidationsCoverNullAndInvalidCases() {
        var empty = new HospitalCashInsuranceRequest(null,null,null,null,null);
        assertTrue(empty.isAgeValid()); assertTrue(empty.isMinimumAdultValid());
        assertTrue(empty.isChildrenCountValid()); assertTrue(empty.isCoverAmountSupported());
        assertTrue(empty.isDurationSupported());
        var valid = new HospitalCashInsuranceRequest(18,1000L,1,0,30);
        assertTrue(valid.isAgeValid()); assertTrue(valid.isMinimumAdultValid());
        assertTrue(valid.isChildrenCountValid()); assertTrue(valid.isCoverAmountSupported());
        assertTrue(valid.isDurationSupported());
        var invalid = new HospitalCashInsuranceRequest(17,1500L,0,-1,31);
        assertFalse(invalid.isAgeValid()); assertFalse(invalid.isMinimumAdultValid());
        assertFalse(invalid.isChildrenCountValid()); assertFalse(invalid.isCoverAmountSupported());
        assertFalse(invalid.isDurationSupported());
    }
    @Test void healthAndSuperTopUpAcceptMissingAgeAndAllowedCoverAmount() {
        assertTrue(new HealthInsuranceRequest(1000000L,"1",null,null,null,null,null,null).isMinimumAdultValid());
        assertTrue(new HealthInsuranceRequest(1000000L,"1",18,null,null,null,null,null).isCoverAmountSupported());
        assertTrue(new SuperTopUpInsuranceRequest(1000000L,null,null,null,null,null,null).isMinimumAdultValid());
        assertTrue(new SuperTopUpInsuranceRequest(1000000L,18,null,null,null,null,null).isCoverAmountSupported());
    }
    @Test void personalAccidentValidationCoversCategoriesAgeAndCoverAmount() {
        for (Integer category : new Integer[]{null, 1, 2, 3}) {
            assertTrue(new PersonalAccidentInsuranceRequest(1000000L,category,30).isCategorySupported(), "category " + category);
        }
        for (Integer category : new Integer[]{0, 4}) {
            assertFalse(new PersonalAccidentInsuranceRequest(1000000L,category,30).isCategorySupported(), "category " + category);
        }
        assertTrue(new PersonalAccidentInsuranceRequest(1000000L,1,null).isMinimumAdultValid());
        assertTrue(new PersonalAccidentInsuranceRequest(1000000L,1,18).isMinimumAdultValid());
        assertFalse(new PersonalAccidentInsuranceRequest(1000000L,1,17).isMinimumAdultValid());
        assertTrue(new PersonalAccidentInsuranceRequest(null,1,30).isCoverAmountSupported());
        assertTrue(new PersonalAccidentInsuranceRequest(1000000L,1,30).isCoverAmountSupported());
        assertFalse(new PersonalAccidentInsuranceRequest(999L,1,30).isCoverAmountSupported());
    }
}
