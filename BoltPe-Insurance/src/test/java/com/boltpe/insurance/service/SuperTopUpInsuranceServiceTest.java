package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.client.SuperTopUpQuoteBatch;
import com.boltpe.insurance.dto.request.SuperTopUpInsuranceRequest;
import com.boltpe.insurance.vendor.request.SuperTopUpVendorRequest;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SuperTopUpInsuranceServiceTest {
    private static final InsurerLogoService LOGOS = new InsurerLogoService("https://s3.example.test", "test-bucket", "publictpap", "insurancelogo");
    private PolicyPlannerClient client;
    private SuperTopUpInsuranceService service;

    @BeforeEach void setUp() {
        client = mock(PolicyPlannerClient.class);
        service = new SuperTopUpInsuranceService(client, JsonMapper.builder().build(), LOGOS);
    }

    @Test void shouldFetchUsingVendorRequestWithoutZone() {
        var request = new SuperTopUpInsuranceRequest(1000000L, 30, 28, 5, null, null, null);
        var vendor = new SuperTopUpVendorRequest(1000000L, 30, 28, 5, null, null, null);
        var batch = new SuperTopUpQuoteBatch(List.of(), List.of());
        when(client.fetchSuperTopUpQuoteBatch(vendor)).thenReturn(batch);
        assertThat(service.fetch(request)).isSameAs(batch);
        verify(client).fetchSuperTopUpQuoteBatch(vendor);
    }

    @Test void shouldRejectInvalidCoverAmount() {
        var request = new SuperTopUpInsuranceRequest(0L, 30, null, null, null, null, null);
        assertThatThrownBy(() -> service.fetch(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cover amount");
    }

    @Test void shouldBuildHealthStyleResponseAndPreserveDeductiblePremiumOptions() {
        var quote = quote("{\"waitingPeriod\":\"12 months\"}", "500000", "800000");
        var batch = new SuperTopUpQuoteBatch(List.of(quote), List.of());
        var response = service.build("req-1", new SuperTopUpInsuranceRequest(1000000L,18,null,null,null,null,null), batch);
        assertThat(response.requestId()).isEqualTo("req-1");
        assertThat(response.insuranceType()).isEqualTo("SUPER_TOP_UP");
        assertThat(response.planCount()).isEqualTo(2);

        var firstPlan = response.plans().get(0);
        var secondPlan = response.plans().get(1);
        assertThat(firstPlan.company().companyId()).isEqualTo("1005");
        assertThat(firstPlan.company().logoUrl()).isEqualTo("https://s3.example.test/test-bucket/publictpap/insurancelogo/nic_1005.svg");
        assertThat(firstPlan.plan().planId()).isEqualTo("193");
        assertThat(secondPlan.plan().planId()).isEqualTo("193");
        assertThat(firstPlan.coverage().zone()).isNull();
        assertThat(firstPlan.members().getFirst().relation()).isEqualTo("SELF");

        assertThat(firstPlan.premiumOptions()).hasSize(1);
        assertThat(firstPlan.premiumOptions().getFirst().deductible()).isEqualByComparingTo("500000");
        assertThat(firstPlan.premiumOptions().getFirst().premium()).isEqualByComparingTo("1838");
        assertThat(firstPlan.premiumOptions().getFirst().years()).isNull();
        assertThat(firstPlan.premiumOptions().getFirst().basePremium()).isNull();
        assertThat(firstPlan.premiumOptions().getFirst().discount()).isNull();
        assertThat(firstPlan.premiumOptions().getFirst().payablePremium()).isNull();

        assertThat(secondPlan.premiumOptions()).hasSize(1);
        assertThat(secondPlan.premiumOptions().getFirst().deductible()).isEqualByComparingTo("800000");
        assertThat(secondPlan.premiumOptions().getFirst().premium()).isEqualByComparingTo("1658");
        assertThat(firstPlan.otherDetails()).containsEntry("waitingPeriod", "12 months");
        assertThat(firstPlan.includes()).hasSize(1);
        assertThat(firstPlan.excludes()).hasSize(1);
        assertThat(firstPlan.addons()).hasSize(1);
        assertThat(firstPlan.features()).hasSize(3);
        assertThat(firstPlan.compareEligible()).isTrue();
        assertThat(firstPlan.premiumFrequency()).isEqualTo("YEAR");
    }

    @Test void shouldHandleMapPlainTextNullsAndBadDeductible() {
        var mapped = quote(Map.of("x", 1), "bad", null);
        var response = service.build("r", new SuperTopUpInsuranceRequest(1L,18,null,null,null,null,null),
                new SuperTopUpQuoteBatch(List.of(mapped), List.of()));
        var plan = response.plans().getFirst();
        assertThat(plan.otherDetails()).containsEntry("x", 1);
        assertThat(plan.premiumOptions().getFirst().deductible()).isNull();

        var plain = quote("not-json", "500000", null);
        var plainResponse = service.build("r2", new SuperTopUpInsuranceRequest(1L,18,null,null,null,null,null),
                new SuperTopUpQuoteBatch(List.of(plain), List.of()));
        assertThat(plainResponse.plans().getFirst().otherDetails()).containsEntry("details", "not-json");
    }

    @Test void shouldPreserveOtherDetailsFallbackForNullAndBlankValues() {
        var request = new SuperTopUpInsuranceRequest(1000000L, 18, null, null, null, null, null);
        var nullResponse = service.build("null-details", request,
                new SuperTopUpQuoteBatch(List.of(quote(null, "500000", null)), List.of()));
        assertThat(nullResponse.plans().getFirst().otherDetails()).isEmpty();

        var blankResponse = service.build("blank-details", request,
                new SuperTopUpQuoteBatch(List.of(quote(" ", "500000", null)), List.of()));
        assertThat(blankResponse.plans().getFirst().otherDetails()).containsEntry("details", " ");
    }

    @Test void shouldRejectNullCoverAmountWithoutCallingVendor() {
        var request = new SuperTopUpInsuranceRequest(null, 30, null, null, null, null, null);
        assertThatThrownBy(() -> service.fetch(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cover amount");
        verifyNoInteractions(client);
    }

    @Test void shouldPropagateFamilyAgeValidationFailures() {
        var request = new SuperTopUpInsuranceRequest(1000000L, 30, 16, null, null, null, null);
        assertThatThrownBy(() -> service.fetch(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("spouse_age must be at least 18");
        verifyNoInteractions(client);
    }

    @Test void shouldProduceNoPlansWhenPremiumsAreNullOrEmpty() {
        var request = new SuperTopUpInsuranceRequest(1000000L, 18, null, null, null, null, null);
        var nullPremiums = vendorQuote(null, List.of());
        var emptyPremiums = vendorQuote(List.of(), List.of());
        var response = service.build("r", request, new SuperTopUpQuoteBatch(List.of(nullPremiums, emptyPremiums), List.of()));
        assertThat(response.planCount()).isZero();
        assertThat(response.plans()).isEmpty();
    }

    @Test void shouldSkipNullPremiumsAndKeepNullDeductible() {
        var premiums = new ArrayList<SuperTopUpVendorResponse.Premium>();
        premiums.add(null);
        premiums.add(new SuperTopUpVendorResponse.Premium(BigDecimal.valueOf(1200), null));
        var response = service.build("r", new SuperTopUpInsuranceRequest(1000000L, 18, null, null, null, null, null),
                new SuperTopUpQuoteBatch(List.of(vendorQuote(premiums, null)), List.of()));
        assertThat(response.planCount()).isEqualTo(1);
        var plan = response.plans().getFirst();
        assertThat(plan.premiumOptions().getFirst().deductible()).isNull();
        assertThat(plan.premiumOptions().getFirst().premium()).isEqualByComparingTo("1200");
        assertThat(plan.members()).isEmpty();
    }

    @Test void shouldUpperCaseMemberLabelsAndKeepNullLabels() {
        var members = List.of(new SuperTopUpVendorResponse.Member("spouse", 28),
                new SuperTopUpVendorResponse.Member(null, 5));
        var premiums = List.of(new SuperTopUpVendorResponse.Premium(BigDecimal.valueOf(1200), "300000"));
        var plan = service.build("r", new SuperTopUpInsuranceRequest(1000000L, 30, 28, 5, null, null, null),
                new SuperTopUpQuoteBatch(List.of(vendorQuote(premiums, members)), List.of())).plans().getFirst();
        assertThat(plan.members()).extracting(m -> m.relation()).containsExactly("SPOUSE", null);
        assertThat(plan.members()).extracting(m -> m.age()).containsExactly(28, 5);
    }

    private static SuperTopUpVendorResponse vendorQuote(List<SuperTopUpVendorResponse.Premium> premiums,
                                                        List<SuperTopUpVendorResponse.Member> members) {
        return new SuperTopUpVendorResponse("ok", null, "C-1", "P-1", "Synthetic Insurer", null,
                null, null, "Synthetic Top Up", 1000000L, 1, 0, 30, 30,
                premiums, members, null, null);
    }

    private static SuperTopUpVendorResponse quote(Object details, String firstDeductible, String secondDeductible) {
        var premiums = new java.util.ArrayList<SuperTopUpVendorResponse.Premium>();
        premiums.add(new SuperTopUpVendorResponse.Premium(BigDecimal.valueOf(1838), firstDeductible));
        if (secondDeductible != null) premiums.add(new SuperTopUpVendorResponse.Premium(BigDecimal.valueOf(1658), secondDeductible));
        return new SuperTopUpVendorResponse("ok", null, "1005", "193", "National Insurance", "national.png",
                "brochure.pdf", "one.jpeg", "Super Top Up Mediclaim policy", 1000000L, 1, 0, 18, 18,
                premiums, List.of(new SuperTopUpVendorResponse.Member("self",18)), details,
                List.of(new SuperTopUpVendorResponse.Feature("In-patient Treatment", "Pre-Existing Diseases", "Maternity")));
    }
}
