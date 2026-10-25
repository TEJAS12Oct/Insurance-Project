package com.boltpe.insurance.mapper.proposalform;

import static org.assertj.core.api.Assertions.*;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PolicyPlannerProposalMapperTest {
    private final PolicyPlannerProposalMapper mapper = new PolicyPlannerProposalMapper();

    @Test
    void flattensGroupProposalAndConvertsDatesWithoutMutatingBoltPeRequest() {
        InsuranceProposalRequest request = request("health", "HEALTH", Map.of(
                "productName", "Health Insurance",
                "productType", "New",
                "proposerDOB", "12-05-1988",
                "adults", List.of(Map.of("dob", "12-05-1988", "fullName", "Adult")),
                "children", List.of(Map.of("dob", "20-09-2016", "fullName", "Child"))));

        Map<String, Object> vendor = mapper.toVendorRequest(request);
        Map<String, Object> originalData = data(request.toFlatMap());

        assertThat(vendor).containsEntry("client_name", "bolt_pay");
        assertThat(vendor).containsEntry("formType", "health");
        assertThat(vendor).containsEntry("productName", "Health Insurance");
        assertThat(vendor).containsEntry("proposerDOB", "1988-05-12");
        assertThat(((Map<?, ?>) ((List<?>) vendor.get("adults")).getFirst()).get("dob"))
                .isEqualTo("1988-05-12");
        assertThat(((Map<?, ?>) ((List<?>) vendor.get("children")).getFirst()).get("dob"))
                .isEqualTo("2016-09-20");

        assertThat(vendor).doesNotContainKeys("insuranceType", "data");
        assertThat(originalData).containsEntry("proposerDOB", "12-05-1988");
    }

    @Test
    void flattensPersonalAccidentAndConvertsDateOfBirth() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productName", "Personal Accident",
                "productType", "New",
                "dateOfBirth", "01-01-1990"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("dateOfBirth", "1990-01-01");
        assertThat(vendor).containsEntry("productName", "Personal Accident");
        assertThat(vendor).containsEntry("formType", "pa");
        assertThat(vendor).containsEntry("client_name", "bolt_pay");
        assertThat(vendor).doesNotContainKeys("insuranceType", "data");
    }

    @Test
    void rejectsInvalidBoltPeDateFormat() {
        InsuranceProposalRequest request = request("health", "HEALTH", Map.of(
                "productType", "New",
                "proposerDOB", "1988-05-12"));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("proposerDOB must be in DD-MM-YYYY format");
    }

    @Test
    void ignoresMissingDatesAndNonListMembers() {
        InsuranceProposalRequest request = request("hospicash", "HOSPITAL_CASH", Map.of(
                "productType", "New",
                "adults", "not-a-list"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("adults", "not-a-list");
    }

    @Test
    void rejectsBlankMemberDate() {
        InsuranceProposalRequest request = request("supertopup", "SUPER_TOP_UP", Map.of(
                "productType", "New",
                "proposerDOB", "01-01-1990",
                "adults", List.of(Map.of("dob", ""))));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dob must be in DD-MM-YYYY format");
    }

    @Test
    void rejectsMissingDataObject() {
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        request.setClientName("bolt_pay");
        request.setFormType("health");
        request.put("insuranceType", "HEALTH");

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("data is required");
    }


    @Test
    void normalizesPlus91GroupMobileBeforeVendorCall() {
        InsuranceProposalRequest request = request("health", "HEALTH", Map.of(
                "productType", "New",
                "proposerPhone", "+919876543210"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("proposerPhone", "9876543210");
    }

    @Test
    void normalizesPlus91PaMobileBeforeVendorCall() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "mobileNumber", "+919876543210"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("mobileNumber", "9876543210");
    }

    @Test
    void keepsTenDigitMobileUnchanged() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "mobileNumber", "9876543210"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("mobileNumber", "9876543210");
    }

    @Test
    void mapsAllSupportedAnnualIncomeSlabsToPolicyPlannerAmounts() {
        Map<String, Object> expected = Map.of(
                "3L_5L", 400000,
                "5L_8L", 650000,
                "8L_12L", 1000000,
                "12L_18L", 1500000,
                "18L_25L", 2150000,
                "25L_40L", 3250000,
                "40L_ABOVE", 5000000);

        expected.forEach((slab, amount) -> {
            InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                    "productType", "New",
                    "annualIncome", slab));

            Map<String, Object> vendor = mapper.toVendorRequest(request);

            assertThat(vendor.get("annualIncome")).hasToString(amount.toString());
        });
    }

    @Test
    void trimsAndNormalizesAnnualIncomeSlabCode() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "annualIncome", " 8l_12l "));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor.get("annualIncome")).hasToString("1000000");
    }

    @Test
    void rejectsUnsupportedAnnualIncomeSlab() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "annualIncome", "2L_3L"));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid annualIncome slab");
    }

    @Test
    void rejectsNumericAnnualIncomeFromBoltPeRequest() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "annualIncome", 1000000));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid annualIncome slab");
    }

    @Test
    void nullFormTypeIsTreatedAsGroupProposal() {
        InsuranceProposalRequest request = request(null, "HEALTH", Map.of(
                "productType", "New",
                "proposerDOB", "12-05-1988",
                "dateOfBirth", "01-01-1990"));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsEntry("formType", null);
        assertThat(vendor).containsEntry("proposerDOB", "1988-05-12");
        assertThat(vendor).containsEntry("dateOfBirth", "01-01-1990");
    }

    @Test
    void leavesExplicitNullAnnualIncomeUntouched() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("productType", "New");
        fields.put("annualIncome", null);
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", fields);

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        assertThat(vendor).containsKey("annualIncome");
        assertThat(vendor.get("annualIncome")).isNull();
    }

    @Test
    void rejectsBlankAnnualIncomeSlab() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "annualIncome", "   "));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid annualIncome slab");
    }

    @Test
    void keepsMobileUnchangedWhenPlus91LengthIsWrongOrValueIsNotText() {
        InsuranceProposalRequest group = request("health", "HEALTH", Map.of(
                "productType", "New",
                "proposerPhone", "+91987654321"));
        InsuranceProposalRequest pa = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "mobileNumber", 9876543210L));

        assertThat(mapper.toVendorRequest(group)).containsEntry("proposerPhone", "+91987654321");
        assertThat(mapper.toVendorRequest(pa)).containsEntry("mobileNumber", 9876543210L);
    }

    @Test
    void skipsNonMapMembersAndMembersWithoutDob() {
        InsuranceProposalRequest request = request("health", "HEALTH", Map.of(
                "productType", "New",
                "adults", List.of("not-a-member", Map.of("fullName", "Adult")),
                "children", List.of(Map.of("dob", "20-09-2016"))));

        Map<String, Object> vendor = mapper.toVendorRequest(request);

        List<?> adults = (List<?>) vendor.get("adults");
        assertThat(adults.getFirst()).isEqualTo("not-a-member");
        assertThat(adults.get(1)).isEqualTo(Map.of("fullName", "Adult"));
        assertThat(((Map<?, ?>) ((List<?>) vendor.get("children")).getFirst()).get("dob")).isEqualTo("2016-09-20");
    }

    @Test
    void rejectsNonTextDate() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "dateOfBirth", 19900101));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dateOfBirth must be in DD-MM-YYYY format");
    }

    @Test
    void rejectsImpossibleCalendarDate() {
        InsuranceProposalRequest request = request("pa", "PERSONAL_ACCIDENT", Map.of(
                "productType", "New",
                "dateOfBirth", "31-02-1990"));

        assertThatThrownBy(() -> mapper.toVendorRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dateOfBirth must be in DD-MM-YYYY format");
    }

    private InsuranceProposalRequest request(String formType,
                                             String insuranceType,
                                             Map<String, Object> fields) {
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        request.setClientName("bolt_pay");
        request.setFormType(formType);
        request.put("insuranceType", insuranceType);
        request.put("data", new LinkedHashMap<>(fields));
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> body) {
        return (Map<String, Object>) body.get("data");
    }
}
