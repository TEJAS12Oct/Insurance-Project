package com.boltpe.insurance.validation.proposalform;

import static org.assertj.core.api.Assertions.*;

import com.boltpe.insurance.dto.request.proposalform.AdultMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.ChildMemberRequest;
import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.exception.proposalform.ProposalAdditionalValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProposalFormAdditionalValidatorTest {
    private final ProposalFormAdditionalValidator validator = new ProposalFormAdditionalValidator();

    @Test void acceptsAllBoundaryValuesAndOptionalBlankAbha() {
        GroupProposalRequest request = group(4, 4, BigDecimal.ONE,
                List.of(adult("self", "Mr", "", 30, 5), adult("spouse", "Mrs", null, 250, 300),
                        adult("father", "Mr", "12345678901234", 175, 70), adult("mother", "Mrs", null, 160, 60)),
                List.of(child(30, 5), child(250, 300), child(100, 30), child(120, 40)));
        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test void rejectsAdultCountBelowAndAboveRange() {
        assertError(group(0,0,BigDecimal.ONE,List.of(),List.of()), "adultCount", "adultCount must be between 1 and 4");
        assertError(group(5,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of()), "adultCount", "adultCount must be between 1 and 4");
    }

    @Test void rejectsAdultCountMismatchAndMoreThanFourMembers() {
        assertError(group(2,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of()), "adultCount", "adultCount must match the number of adult members");
        var five = List.of(adult("self","Mr",null,170,70), adult("spouse","Mrs",null,170,70), adult("father","Mr",null,170,70), adult("mother","Mrs",null,170,70), adult("other","Mr",null,170,70));
        assertError(group(4,0,BigDecimal.ONE,five,List.of()), "adults", "Maximum 4 adult members are allowed");
    }

    @Test void emptyAdultPlaceholderDoesNotCount() {
        AdultMemberRequest empty = new AdultMemberRequest(null,null,null,null,null,null,null,null,null);
        assertError(group(2,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70), empty),List.of()), "adultCount", "adultCount must match the number of adult members");
    }

    @Test void acceptsZeroThroughFourChildrenAndRejectsMismatchOrOverflow() {
        assertThatCode(() -> validator.validate(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of()))).doesNotThrowAnyException();
        assertError(group(1,-1,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of()), "childCount", "childCount must be between 0 and 4");
        assertError(group(1,5,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of()), "childCount", "childCount must be between 0 and 4");
        assertError(group(1,2,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),List.of(child(100,20))), "childCount", "childCount must match the number of child members");
        var five = List.of(child(100,20),child(100,20),child(100,20),child(100,20),child(100,20));
        assertError(group(1,4,BigDecimal.ONE,List.of(adult("self","Mr",null,170,70)),five), "children", "Maximum 4 child members are allowed");
    }

    @Test void acceptsExactlyFourChildrenUsingIndexesZeroToThree() {
        var adults = List.of(adult("self", "Mr", null, 170, 70));
        var fourChildren = List.of(
                child(100, 20),
                child(101, 21),
                child(102, 22),
                child(103, 23));

        assertThat(fourChildren).hasSize(4);
        assertThatCode(() -> validator.validate(
                group(1, 4, BigDecimal.ONE, adults, fourChildren)))
                .doesNotThrowAnyException();
    }

    @Test void rejectsFifthChildAtIndexFour() {
        var adults = List.of(adult("self", "Mr", null, 170, 70));
        var fiveChildren = List.of(
                child(100, 20),
                child(101, 21),
                child(102, 22),
                child(103, 23),
                child(104, 24));

        assertThatThrownBy(() -> validator.validate(
                group(1, 4, BigDecimal.ONE, adults, fiveChildren)))
                .isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex))
                        .containsEntry("children", "Maximum 4 child members are allowed")
                        .containsEntry("childCount", "childCount must match the number of child members"));
    }

    @Test void rejectsChildCountFiveEvenWhenOnlyFourChildrenAreSent() {
        var adults = List.of(adult("self", "Mr", null, 170, 70));
        var fourChildren = List.of(
                child(100, 20), child(101, 21), child(102, 22), child(103, 23));

        assertError(
                group(1, 5, BigDecimal.ONE, adults, fourChildren),
                "childCount",
                "childCount must be between 0 and 4");
    }

    @Test void rejectsFractionalAndNonNumericRawCounts() {
        assertRawError(Map.of("zone", "1", "adultCount", 1.5, "childCount", 0),
                "adultCount", "adultCount must be a whole number");
        assertRawError(Map.of("zone", "1", "adultCount", 1, "childCount", "2.5"),
                "childCount", "childCount must be a whole number");
        assertRawError(Map.of("zone", "1", "adultCount", "abc", "childCount", 0),
                "adultCount", "adultCount must be a whole number");
        assertThatCode(() -> validator.validateRawData(
                Map.of(
                        "zone", "1",
                        "adultCount", 4,
                        "childCount", 0,
                        "nriDiscount", "No",
                        "includeSelf", "Yes",
                        "bureauDiscount", "No")))
                .doesNotThrowAnyException();
    }

    @Test void rejectsRawNonNumericTenureHeightAndWeight() {
        Map<String,Object> adult = Map.of("height", "tall", "weight", "heavy");
        Map<String,Object> data = Map.of("adultCount", 1, "childCount", 0, "selectedTenure", "one", "adults", List.of(adult));
        assertThatThrownBy(() -> validator.validateRawData(data)).isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex))
                        .containsEntry("selectedTenure", "selectedTenure must be 1 year")
                        .containsEntry("adults[0].height", "Height must be a numeric value")
                        .containsEntry("adults[0].weight", "Weight must be a numeric value"));
    }

    @Test void onlyOneYearTenureIsAccepted() {
        var adults = List.of(adult("self","Mr",null,170,70));
        assertError(group(1,0,BigDecimal.ZERO,adults,List.of()), "selectedTenure", "selectedTenure must be 1 year");
        assertError(group(1,0,BigDecimal.valueOf(2),adults,List.of()), "selectedTenure", "selectedTenure must be 1 year");
        assertError(group(1,0,BigDecimal.valueOf(-1),adults,List.of()), "selectedTenure", "selectedTenure must be 1 year");
    }

    @Test void validatesAdultHeightAndWeightRequiredAndBoundaries() {
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,null,70)),List.of()), "adults[0].height", "Height is required");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,29,70)),List.of()), "adults[0].height", "Height must be between 30 and 250 cm");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,251,70)),List.of()), "adults[0].height", "Height must be between 30 and 250 cm");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,null)),List.of()), "adults[0].weight", "Weight is required");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,4)),List.of()), "adults[0].weight", "Weight must be between 5 and 300 kg");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr",null,170,301)),List.of()), "adults[0].weight", "Weight must be between 5 and 300 kg");
    }

    @Test void validatesChildHeightAndWeightIndependently() {
        var adults=List.of(adult("self","Mr",null,170,70));
        assertError(group(1,1,BigDecimal.ONE,adults,List.of(new ChildMemberRequest("child","C","01-01-2020",null,BigDecimal.TEN,null))), "children[0].height", "Height is required");
        assertError(group(1,1,BigDecimal.ONE,adults,List.of(new ChildMemberRequest("child","C","01-01-2020",BigDecimal.valueOf(100),null,null))), "children[0].weight", "Weight is required");
    }

    @Test void abhaIsOptionalButValidatedWhenProvided() {
        var adults=List.of(adult("self","Mr",null,170,70));
        assertThatCode(() -> validator.validate(group(1,0,BigDecimal.ONE,adults,List.of()))).doesNotThrowAnyException();
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr","123",170,70)),List.of()), "adults[0].abhaId", "ABHA Number must contain exactly 14 digits");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr","123456789012AB",170,70)),List.of()), "adults[0].abhaId", "ABHA Number must contain digits only");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr","1234-567890123",170,70)),List.of()), "adults[0].abhaId", "Invalid ABHA Number format");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("self","Mr","1234 567890123",170,70)),List.of()), "adults[0].abhaId", "Invalid ABHA Number format");
    }

    @Test void validatesFatherAndMotherTitlesCaseInsensitively() {
        assertThatCode(() -> validator.validate(group(2,0,BigDecimal.ONE,List.of(adult("father","mr",null,170,70),adult("mother","MRS",null,160,60)),List.of()))).doesNotThrowAnyException();
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("father","Mrs",null,170,70)),List.of()), "adults[0].title", "Title must be Mr when relationship is father");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("father",null,null,170,70)),List.of()), "adults[0].title", "Title must be Mr when relationship is father");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("mother","Mr",null,170,70)),List.of()), "adults[0].title", "Title must be Mrs when relationship is mother");
        assertError(group(1,0,BigDecimal.ONE,List.of(adult("mother",null,null,170,70)),List.of()), "adults[0].title", "Title must be Mrs when relationship is mother");
    }

    @Test void rejectsDuplicateAdultRelationships() {
        var adults=List.of(adult("father","Mr",null,170,70), adult(" FATHER ","Mr",null,170,70));
        assertError(group(2,0,BigDecimal.ONE,adults,List.of()), "adults[1].relationship", "Duplicate adult relationship is not allowed");
    }

    @Test void collectsMultipleErrorsTogether() {
        var bad = group(2,1,BigDecimal.valueOf(2), List.of(adult("father","Mrs","123",29,301)), List.of());
        assertThatThrownBy(() -> validator.validate(bad)).isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsKeys("adultCount","childCount","selectedTenure","adults[0].height","adults[0].weight","adults[0].abhaId","adults[0].title"));
    }

    @Test void acceptsEveryConfiguredProposalCoverAmountForAllFourProducts() {
        assertAcceptedCoverAmounts("HEALTH", "sumInsured", List.of(
                50000L, 75000L, 200000L, 300000L, 400000L, 500000L, 700000L,
                1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
                4000000L, 4500000L, 5000000L, 7500000L, 10000000L, 20000000L,
                30000000L, 40000000L, 50000000L));
        assertAcceptedCoverAmounts("SUPER_TOP_UP", "sumInsured", List.of(
                200000L, 300000L, 400000L, 500000L, 1000000L, 1500000L,
                2000000L, 2500000L, 5000000L, 10000000L, 30000000L));
        assertAcceptedCoverAmounts("PERSONAL_ACCIDENT", "coverAmount", List.of(
                1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
                5000000L, 7500000L, 10000000L, 20000000L, 30000000L,
                40000000L, 50000000L));
        assertAcceptedCoverAmounts("HOSPITAL_CASH", "sumInsured", List.of(
                1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L,
                5000000L, 7500000L, 10000000L, 20000000L, 30000000L,
                40000000L, 50000000L));
    }

    @Test void rejectsMissingNullZeroNegativeDecimalAndInvalidTypeCoverAmounts() {
        assertCoverError(new java.util.HashMap<>(), "HEALTH", "coverAmount is required");

        Map<String,Object> nullAmount = new java.util.HashMap<>();
        nullAmount.put("sumInsured", null);
        assertCoverError(nullAmount, "HEALTH", "coverAmount is required");

        for (Object value : List.of(0, -50000, 50000.5, "50000", "50K", "abc", "1@")) {
            Map<String,Object> data = new java.util.HashMap<>();
            data.put("sumInsured", value);
            assertCoverError(data, "HEALTH", "Invalid coverAmount for HEALTH insurance");
        }
    }

    @Test void rejectsUnsupportedAndCrossProductProposalCoverAmounts() {
        Map<String,Object> health = new java.util.HashMap<>();
        health.put("sumInsured", 1200000);
        assertCoverError(health, "HEALTH", "Invalid coverAmount for HEALTH insurance");

        Map<String,Object> superTopUp = new java.util.HashMap<>();
        superTopUp.put("sumInsured", 750000);
        assertCoverError(superTopUp, "SUPER_TOP_UP", "Invalid coverAmount for SUPER_TOP_UP insurance");

        Map<String,Object> pa = new java.util.HashMap<>();
        pa.put("coverAmount", 500000);
        assertCoverError(pa, "PERSONAL_ACCIDENT", "Invalid coverAmount for PERSONAL_ACCIDENT insurance");

        Map<String,Object> hospitalCash = new java.util.HashMap<>();
        hospitalCash.put("sumInsured", 50000);
        assertCoverError(hospitalCash, "HOSPITAL_CASH", "Invalid coverAmount for HOSPITAL_CASH insurance");
    }

    @Test void proposalAmountsUseFullBackendNumericValues() {
        Map<String,Object> health = new java.util.HashMap<>();
        health.put("sumInsured", 50000);
        assertThatCode(() -> validator.validateRawCoverAmount(health, "HEALTH"))
                .doesNotThrowAnyException();

        Map<String,Object> displayText = new java.util.HashMap<>();
        displayText.put("sumInsured", "50 Thousand");
        assertCoverError(displayText, "HEALTH", "Invalid coverAmount for HEALTH insurance");
    }

    @Test void acceptsProposalZonesOneTwoAndThree() {
        for (String zone : List.of("1", "2", "3")) {
            Map<String,Object> data = new java.util.HashMap<>();
            data.put("zone", zone);
            data.put("nriDiscount", "No");
            data.put("includeSelf", "Yes");
            data.put("bureauDiscount", "No");
            assertThatCode(() -> validator.validateRawData(data)).doesNotThrowAnyException();
        }
    }

    @Test void rejectsMissingNullEmptyAndBlankZoneAsRequired() {
        assertRawError(new java.util.HashMap<>(), "zone", "zone is required");

        Map<String,Object> nullZone = new java.util.HashMap<>();
        nullZone.put("zone", null);
        assertRawError(nullZone, "zone", "zone is required");

        for (String zone : List.of("", " ", "   ")) {
            Map<String,Object> data = new java.util.HashMap<>();
            data.put("zone", zone);
            assertRawError(data, "zone", "zone is required");
        }
    }

    @Test void rejectsEveryZoneOutsideExactStringOneTwoThree() {
        List<Object> invalidZones = List.of(
                0, 1, 2, 3, 4, 5, 10, -1, -2,
                1.5, 2.5,
                "0", "4", "5", "10", "-1", "-2",
                "1.5", "2.5", "A", "abc", "@", "#", "%",
                "1A", "A1", "1@", "2#", "12", "23", "123"
        );
        for (Object zone : invalidZones) {
            Map<String,Object> data = new java.util.HashMap<>();
            data.put("zone", zone);
            assertRawError(data, "zone", "zone must be one of: 1, 2, 3");
        }
    }

    @Test void validatesZoneAfterDtoConversionToo() {
        var adults = List.of(adult("self","Mr",null,170,70));
        GroupProposalRequest invalid = new GroupProposalRequest(
                "P","New","411033","Pune","4","No",1,0,"P","01-01-1980",
                "male","a@b.com","9876543210",BigDecimal.TEN,BigDecimal.ONE,"8L_12L",
                "No","Yes","No","123456789012","ABCDE1234F",BigDecimal.TEN,BigDecimal.TEN,
                adults,List.of());
        assertError(invalid, "zone", "zone must be one of: 1, 2, 3");
    }

    private void assertAcceptedCoverAmounts(String insuranceType, String field, List<Long> values) {
        for (Long value : values) {
            Map<String,Object> data = new java.util.HashMap<>();
            data.put(field, value);
            assertThatCode(() -> validator.validateRawCoverAmount(data, insuranceType))
                    .doesNotThrowAnyException();
        }
    }

    private void assertCoverError(Map<String,Object> data, String insuranceType, String message) {
        assertThatThrownBy(() -> validator.validateRawCoverAmount(data, insuranceType))
                .isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry("coverAmount", message));
    }

    private AdultMemberRequest adult(String rel,String title,String abha,Integer h,Integer w) {
        return new AdultMemberRequest(rel,title,"Name","01-01-1980",h==null?null:BigDecimal.valueOf(h),w==null?null:BigDecimal.valueOf(w),abha,null,null);
    }
    private ChildMemberRequest child(int h,int w) {
        return new ChildMemberRequest("child","Child","01-01-2020",BigDecimal.valueOf(h),BigDecimal.valueOf(w),null);
    }
    private GroupProposalRequest group(Integer ac,Integer cc,BigDecimal tenure,List<AdultMemberRequest> adults,List<ChildMemberRequest> children) {
        return new GroupProposalRequest("P","New","411033","Pune","1","No",ac,cc,"P","01-01-1980","male","a@b.com","9876543210",BigDecimal.TEN,tenure,"8L_12L","No","Yes","No","123456789012","ABCDE1234F",BigDecimal.TEN,BigDecimal.TEN,adults,children);
    }
    private void assertError(GroupProposalRequest r,String field,String msg) {
        assertThatThrownBy(() -> validator.validate(r)).isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry(field,msg));
    }
    private void assertRawError(Map<String,Object> data,String field,String msg) {
        assertThatThrownBy(() -> validator.validateRawData(data)).isInstanceOf(ProposalAdditionalValidationException.class)
                .satisfies(ex -> assertThat(errors(ex)).containsEntry(field,msg));
    }
    private Map<String,String> errors(Throwable ex) { return ((ProposalAdditionalValidationException) ex).getFieldErrors(); }
}
