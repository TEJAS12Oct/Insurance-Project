package com.boltpe.insurance.service;

import com.boltpe.insurance.client.PersonalAccidentQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.PersonalAccidentInsuranceRequest;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalAccidentInsuranceServiceTest {

    @Test
    void shouldFetchCoverAmountCategoryAndAgeAndBuildStandardResponse() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var req = new PersonalAccidentInsuranceRequest(
                1_000_000L,
                2,
                26
        );

        var batch = new PersonalAccidentQuoteBatch(
                List.of(),
                List.of()
        );

        var vendorRequest = new PersonalAccidentVendorRequest(
                1_000_000L,
                2,
                26
        );

        when(client.fetchPersonalAccidentQuoteBatch(vendorRequest))
                .thenReturn(batch);

        assertThat(service.fetch(req)).isSameAs(batch);

        verify(client).fetchPersonalAccidentQuoteBatch(vendorRequest);
    }

    @Test
    void shouldKeepMultiplePremiumTypesInsideSinglePlan() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var f = new PersonalAccidentVendorResponse.Feature(
                "1003",
                "227",
                "Accidental Death",
                "Natural death",
                null
        );

        var q = new PersonalAccidentVendorResponse(
                "STAR HEALTH",
                "Accident Care Individual",
                "star.png",
                "b.pdf",
                "o.jpeg",
                "{\"Entry Age\":\"91 Days onwards\"}",
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("507")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "PTD+PPD",
                                new BigDecimal("826")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("1475")
                        )
                ),
                List.of(f),
                null
        );

        var res = service.build(
                "r",
                new PersonalAccidentQuoteBatch(List.of(q), List.of())
        );

        assertThat(res.planCount()).isEqualTo(1);
        assertThat(res.plans()).hasSize(1);

        var plan = res.plans().getFirst();

        assertThat(plan.premium().payablePremium())
                .isEqualByComparingTo("507");

        assertThat(plan.premiumOptions()).hasSize(3);

        assertThat(plan.premiumOptions())
                .extracting(p -> p.type())
                .containsExactly(
                        "basic",
                        "PTD+PPD",
                        "TTD"
                );

        assertThat(plan.premiumOptions())
                .extracting(p -> p.value())
                .containsExactly(
                        new BigDecimal("507"),
                        new BigDecimal("826"),
                        new BigDecimal("1475")
                );
    }

    @Test
    void shouldUseFirstPremiumWhenBasicPremiumIsNotAvailable() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var q = new PersonalAccidentVendorResponse(
                "ICICI Lombard",
                "Family Shield",
                null,
                null,
                null,
                null,
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "PTD+PPD",
                                new BigDecimal("638")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("856")
                        )
                ),
                List.of(),
                null
        );

        var res = service.build(
                "req",
                new PersonalAccidentQuoteBatch(List.of(q), List.of())
        );

        assertThat(res.planCount()).isEqualTo(1);

        assertThat(res.plans().getFirst().premium().payablePremium())
                .isEqualByComparingTo("638");

        assertThat(res.plans().getFirst().premiumOptions())
                .hasSize(2);
    }

    @Test
    void shouldCountVendorProductsAsPlansNotPremiumOptions() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var q1 = new PersonalAccidentVendorResponse(
                "STAR",
                "Plan 1",
                null,
                null,
                null,
                null,
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("507")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("1475")
                        )
                ),
                List.of(),
                null
        );

        var q2 = new PersonalAccidentVendorResponse(
                "CARE",
                "Plan 2",
                null,
                null,
                null,
                null,
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("1663")
                        )
                ),
                List.of(),
                null
        );

        var res = service.build(
                "req",
                new PersonalAccidentQuoteBatch(
                        List.of(q1, q2),
                        List.of()
                )
        );

        assertThat(res.planCount()).isEqualTo(2);
        assertThat(res.plans()).hasSize(2);
    }

    @Test
    void shouldRejectInvalidCoverAmount() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        assertThatThrownBy(() ->
                service.fetch(
                        new PersonalAccidentInsuranceRequest(
                                0L,
                                1,
                                26
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cover amount");

        verifyNoInteractions(client);
    }

    @Test
    void shouldBuildPlanWithIncludesExcludesAddonsAndFallbackOtherDetails() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var feature = new PersonalAccidentVendorResponse.Feature(
                "1003",
                "227",
                " Accident Death ",
                " Natural Death ",
                " TTD Addon "
        );

        var q = new PersonalAccidentVendorResponse(
                "STAR HEALTH",
                "Accident Care Individual",
                "star.png",
                "b.pdf",
                "",
                "not-json",
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("1475")
                        )
                ),
                List.of(feature),
                null
        );

        var res = service.build(
                "req",
                new PersonalAccidentQuoteBatch(
                        List.of(q),
                        List.of()
                )
        );

        assertThat(res.planCount()).isEqualTo(1);

        var plan = res.plans().getFirst();

        assertThat(plan.includes()).hasSize(1);
        assertThat(plan.excludes()).hasSize(1);
        assertThat(plan.addons()).hasSize(1);

        assertThat(plan.otherDetails())
                .containsEntry("details", "not-json");

        assertThat(plan.otherDetails())
                .doesNotContainKey("Premium Type");

        assertThat(plan.company().companyId())
                .isEqualTo("1003");

        assertThat(plan.plan().planId())
                .isEqualTo("227");
    }

    @Test
    void shouldReturnNoPlansWhenVendorPremiumsAreEmpty() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var q = new PersonalAccidentVendorResponse(
                "STAR",
                "Plan",
                null,
                null,
                null,
                null,
                1_000_000L,
                List.of(),
                null,
                null
        );

        var res = service.build(
                "req",
                new PersonalAccidentQuoteBatch(
                        List.of(q),
                        List.of()
                )
        );

        assertThat(res.planCount()).isZero();
        assertThat(res.plans()).isEmpty();
    }

    @Test
    void shouldKeepOnlyFirstPremiumPerTypeWhenVendorReturnsRepeatedValues() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var f = new PersonalAccidentVendorResponse.Feature(
                "1003",
                "227",
                "Accidental Death",
                "Natural death",
                null
        );

        var q = new PersonalAccidentVendorResponse(
                "STAR HEALTH",
                "Accident Care Individual",
                "star.png",
                "b.pdf",
                "",
                "",
                1_000_000L,
                List.of(
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("507")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("708")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "basic",
                                new BigDecimal("944")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "PTD+PPD",
                                new BigDecimal("826")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "PTD+PPD",
                                new BigDecimal("1534")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "PTD+PPD",
                                new BigDecimal("2065")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("1475")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("2065")
                        ),
                        new PersonalAccidentVendorResponse.Premium(
                                "TTD",
                                new BigDecimal("2360")
                        )
                ),
                List.of(f),
                null
        );

        var res = service.build(
                "req",
                new PersonalAccidentQuoteBatch(
                        List.of(q),
                        List.of()
                )
        );

        var plan = res.plans().getFirst();

        assertThat(res.planCount()).isEqualTo(1);

        assertThat(plan.premium().payablePremium())
                .isEqualByComparingTo("507");

        assertThat(plan.premiumOptions()).hasSize(3);

        assertThat(plan.premiumOptions())
                .extracting(p -> p.type())
                .containsExactly(
                        "basic",
                        "PTD+PPD",
                        "TTD"
                );

        assertThat(plan.premiumOptions())
                .extracting(p -> p.value())
                .containsExactly(
                        new BigDecimal("507"),
                        new BigDecimal("826"),
                        new BigDecimal("1475")
                );
    }

    @Test
    void shouldAcceptAllConfiguredPersonalAccidentCoverAmounts() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        var batch = new PersonalAccidentQuoteBatch(
                List.of(),
                List.of()
        );

        when(client.fetchPersonalAccidentQuoteBatch(
                any(PersonalAccidentVendorRequest.class)))
                .thenReturn(batch);

        for (Long cover :
                PersonalAccidentInsuranceRequest.ALLOWED_COVER_AMOUNTS) {

            assertThat(
                    service.fetch(
                            new PersonalAccidentInsuranceRequest(
                                    cover,
                                    1,
                                    26
                            )
                    )
            ).isSameAs(batch);
        }

        verify(
                client,
                times(
                        PersonalAccidentInsuranceRequest
                                .ALLOWED_COVER_AMOUNTS
                                .size()
                )
        ).fetchPersonalAccidentQuoteBatch(
                any(PersonalAccidentVendorRequest.class)
        );
    }

    @Test
    void shouldRejectPositiveCoverAmountOutsidePersonalAccidentDropdown() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build()
        );

        assertThatThrownBy(() ->
                service.fetch(
                        new PersonalAccidentInsuranceRequest(
                                6_000_000L,
                                1,
                                26
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Invalid cover amount. Please select a cover amount from the available Personal Accident coverage options."
                );

        verifyNoInteractions(client);
    }

    @Test
    void personalAccidentRequestShouldExposeOnlyConfiguredDropdownCovers() {
        assertThat(
                PersonalAccidentInsuranceRequest.ALLOWED_COVER_AMOUNTS
        ).containsExactlyInAnyOrder(
                1_000_000L,
                1_500_000L,
                2_000_000L,
                2_500_000L,
                3_000_000L,
                3_500_000L,
                5_000_000L,
                7_500_000L,
                10_000_000L,
                20_000_000L,
                30_000_000L,
                40_000_000L,
                50_000_000L
        );

        assertThat(
                new PersonalAccidentInsuranceRequest(
                        1_000_000L,
                        1,
                        26
                ).isCoverAmountSupported()
        ).isTrue();

        assertThat(
                new PersonalAccidentInsuranceRequest(
                        6_000_000L,
                        1,
                        26
                ).isCoverAmountSupported()
        ).isFalse();

        assertThat(
                new PersonalAccidentInsuranceRequest(
                        null,
                        1,
                        26
                ).isCoverAmountSupported()
        ).isTrue();
    }
}