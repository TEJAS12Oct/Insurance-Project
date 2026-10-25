package com.boltpe.insurance.dto.request;

import com.boltpe.insurance.enums.InsuranceType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class InsuranceRequestValidationTest {
    private InsuranceRequest request(InsuranceType type, Map<String, Object> values) {
        return new InsuranceRequest(type, Map.of("userId", "TEST"), values);
    }

    @Test void healthAcceptsValidCoverageAndZone() {
        var req = request(InsuranceType.HEALTH, Map.of("coverAmount", 1000000L, "zone", "1", "age", 42));
        assertThat(req.isCoverAmountSelected()).isTrue();
        assertThat(req.isCoverAmountValid()).isTrue();
        assertThat(req.isHealthZoneSelected()).isTrue();
        assertThat(req.isHealthZoneValid()).isTrue();
        assertThat(req.isDataValid()).isTrue();
    }

    @Test void invalidCoverageValuesCannotBeSilentlyTruncated() {
        for (Object invalid : new Object[] {BigDecimal.valueOf(1000000.5), "not-a-number", BigInteger.TEN.pow(40), Double.NaN}) {
            var req = request(InsuranceType.HEALTH, Map.of("coverAmount", invalid, "zone", "1", "age", 42));
            assertThat(req.isCoverAmountSelected()).isFalse();
            assertThat(req.isDataValid()).isFalse();
        }
    }

    @Test void invalidAgeAndZoneAreRejected() {
        var req = request(InsuranceType.HEALTH, Map.of("coverAmount", 1000000L, "zone", "4", "age", 17));
        assertThat(req.isHealthZoneValid()).isFalse();
        assertThat(req.isMinimumAdultValid()).isFalse();
        assertThat(req.isDataValid()).isFalse();
    }

    @Test void spouseAndChildrenAgeRulesAreChecked() {
        var data = Map.<String, Object>of("coverAmount", 1000000L, "zone", "1", "age", 37,
                "spouseAge", 36, "child1Age", 18, "child2Age", 17);
        var req = request(InsuranceType.HEALTH, data);
        assertThat(req.isAdultChildGapValid()).isFalse();
        assertThat(req.isSpouseChildGapValid()).isFalse();
        assertThat(req.isChildChildGapValid()).isFalse();
    }

    @Test void invalidAgeTypesAndNegativeChildrenFailValidation() {
        var req = request(InsuranceType.SUPER_TOP_UP, Map.of("age", "thirty", "child1Age", -1,
                "spouseAge", "bad", "coverAmount", 500000L));
        assertThat(req.isAdultAgeWholeNumber()).isFalse();
        assertThat(req.isSpouseAgeWholeNumber()).isFalse();
        assertThat(req.areChildAgesNonNegative()).isFalse();
    }

    @Test void personalAccidentRequiresValidCategory() {
        var req = request(InsuranceType.PERSONAL_ACCIDENT,
                Map.of("coverAmount", 1000000L, "age", 33, "category", 4));
        assertThat(req.isPersonalAccidentCategorySelected()).isTrue();
        assertThat(req.isPersonalAccidentCategoryValid()).isFalse();
    }

    @Test void hospitalCashRejectsZeroAdults() {
        var req = request(InsuranceType.HOSPITAL_CASH,
                Map.of("coverAmount", 1000L, "age", 35, "noOfAdults", 0, "noOfChildren", 0, "noOfDays", 30));
        assertThat(req.isHospitalCashMinimumAdultValid()).isFalse();
        assertThat(req.isDataValid()).isFalse();
    }

    @Test void nullDataAndUserAreNormalized() {
        var req = new InsuranceRequest(InsuranceType.HEALTH, null, (Map<String, Object>) null);
        assertThat(req.data()).isEmpty();
        assertThat(req.userInformation()).isEmpty();
        assertThat(req.isDataValid()).isFalse();
    }

    @Test void inputMapsAreDefensivelyCopied() {
        var values = new HashMap<String, Object>();
        values.put("age", 35);
        var req = request(InsuranceType.HEALTH, values);
        values.put("age", 10);
        assertThat(req.data()).containsEntry("age", 35);
    }
}
