package com.boltpe.insurance.dto.request;

import com.boltpe.insurance.enums.InsuranceType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceRequestAdditionalCoverageTest {
    private InsuranceRequest request(InsuranceType type, Object... pairs) {
        Map<String,Object> values = new HashMap<>();
        for (int i=0;i<pairs.length;i+=2) values.put((String)pairs[i], pairs[i+1]);
        return new InsuranceRequest(type, null, values);
    }

    @Test void constructorHandlesNullMapsAndDefensiveCopy() {
        var empty = new InsuranceRequest(InsuranceType.HEALTH, null, (Map<String,Object>)null);
        assertTrue(empty.userInformation().isEmpty());
        assertTrue(empty.data().isEmpty());
        Map<String,Object> input = new HashMap<>(); input.put("age", 18);
        var copy = new InsuranceRequest(InsuranceType.HEALTH, null, input);
        input.put("age", 19);
        assertEquals(18, copy.data().get("age"));
        Map<String, Object> immutableData = copy.data();

        assertThrows(
                UnsupportedOperationException.class,
                () -> immutableData.put("age", 20)
        );
    }

    @Test void healthLegacyConstructorPreservesNullableMembers() {
        var health = new HealthInsuranceRequest(1000000L,"3",25,null,null,null,null,null);
        var request = new InsuranceRequest(InsuranceType.HEALTH, null, health);
        assertEquals(1000000L, request.data().get("coverAmount"));
        assertTrue(request.data().containsKey("spouseAge"));
        assertNull(request.data().get("spouseAge"));
        assertTrue(request.isDataValid());
        assertTrue(new InsuranceRequest(InsuranceType.HEALTH,null,(HealthInsuranceRequest)null).data().isEmpty());
    }

    @Test void ageValidationChecksMissingFractionalAndBoundaryValues() {
        assertFalse(request(InsuranceType.HEALTH).isAgeSelected());
        assertTrue(request(InsuranceType.HEALTH,"age",18).isMinimumAdultValid());
        assertFalse(request(InsuranceType.HEALTH,"age",17).isMinimumAdultValid());
        assertTrue(request(InsuranceType.HEALTH,"age",null).isAdultAgeWholeNumber());
        for (Object age : new Object[]{"abc", "18.2", 18.2, Double.NaN, new BigDecimal("18.1")})
            assertFalse(request(InsuranceType.HEALTH,"age",age).isAdultAgeWholeNumber(), String.valueOf(age));
        for (Object age : new Object[]{18L, (short)18, (byte)18, new BigInteger("18"),new BigDecimal("18.00"),18.0f,18.0d,"18"})
            assertTrue(request(InsuranceType.HEALTH,"age",age).isAdultAgeWholeNumber(), String.valueOf(age));
    }

    @Test void zoneAndCategoryRequireExactCodes() {
        for (Object zone : new Object[]{"1","2","3",1,2,3})
            assertTrue(request(InsuranceType.HEALTH,"zone",zone).isHealthZoneValid(),String.valueOf(zone));
        for (Object zone : new Object[]{"0","4","01","invalid"})
            assertFalse(request(InsuranceType.HEALTH,"zone",zone).isHealthZoneValid(),String.valueOf(zone));
        assertFalse(request(InsuranceType.HEALTH).isHealthZoneSelected());
        assertTrue(request(InsuranceType.HEALTH).isHealthZoneValid());
        assertTrue(request(InsuranceType.HEALTH,"zone"," ").isHealthZoneValid());
        for (Object category : new Object[]{"0","4","01","invalid"})
            assertFalse(request(InsuranceType.PERSONAL_ACCIDENT,"category",category).isPersonalAccidentCategoryValid(),String.valueOf(category));
        assertTrue(request(InsuranceType.HEALTH).isPersonalAccidentCategoryValid());
    }

    @Test void allCoverAmountTypesAndInvalidAmounts() {
        for (Object amount : new Object[]{1000000,1000000L,"1000000",BigInteger.valueOf(1000000),new BigDecimal("1000000.00"),1000000d,1000000f}) {
            assertTrue(request(InsuranceType.HEALTH,"coverAmount",amount).isCoverAmountSelected(),String.valueOf(amount));
            assertTrue(request(InsuranceType.HEALTH,"coverAmount",amount).isCoverAmountValid(),String.valueOf(amount));
        }
        for (Object amount : new Object[]{"not-a-number",new BigDecimal("1000000.1"),BigInteger.TEN.pow(30),Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,1000000.5d})
            assertFalse(request(InsuranceType.HEALTH,"coverAmount",amount).isCoverAmountSelected(),String.valueOf(amount));
        assertFalse(request(InsuranceType.HEALTH,"coverAmount",999).isCoverAmountValid());
    }

    @Test void familyValidationChecksAllChildrenAndParentGaps() {
        for (InsuranceType type : new InsuranceType[]{InsuranceType.HEALTH,InsuranceType.SUPER_TOP_UP}) {
            assertTrue(request(type,"age",45,"spouseAge",42,"child1Age",20,"child2Age",18).isChildChildGapValid());
            assertFalse(request(type,"child1Age",10,"child4Age",11).isChildChildGapValid());
            assertFalse(request(type,"child3Age",-1).areChildAgesNonNegative());
            assertFalse(request(type,"child2Age","invalid").areChildAgesWholeNumbers());
            assertFalse(request(type,"age",30,"child4Age",10).isAdultChildGapValid());
            assertFalse(request(type,"spouseAge",30,"child4Age",10).isSpouseChildGapValid());
            assertFalse(request(type,"spouseAge",17).isSpouseMinimumAgeValid());
        }
    }

    @Test void productSpecificRequiredFieldsAndDurations() {
        assertTrue(request(InsuranceType.SUPER_TOP_UP,"coverAmount",1000000,"age",18).isDataValid());
        assertFalse(request(InsuranceType.SUPER_TOP_UP,"coverAmount",1000000).isDataValid());
        assertTrue(request(InsuranceType.PERSONAL_ACCIDENT,"coverAmount",1000000,"category",2,"age",18).isDataValid());
        assertFalse(request(InsuranceType.PERSONAL_ACCIDENT,"coverAmount",1000000,"age",18).isDataValid());
        for (int days : new int[]{30,60,90,180})
            assertTrue(request(InsuranceType.HOSPITAL_CASH,"coverAmount",1000,"noOfAdults",1,"noOfChildren",0,"noOfDays",days,"age",18).isDataValid());
        assertFalse(request(InsuranceType.HOSPITAL_CASH,"coverAmount",1000,"noOfAdults",1,"noOfChildren",0,"noOfDays",10).isDataValid());
        assertFalse(request(InsuranceType.HOSPITAL_CASH,"noOfAdults",0).isHospitalCashMinimumAdultValid());
    }
}
