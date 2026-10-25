package com.boltpe.insurance.service;

import com.boltpe.insurance.client.PersonalAccidentQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.PersonalAccidentInsuranceRequest;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalAccidentInsuranceServiceTest {
    private static final InsurerLogoService LOGOS = new InsurerLogoService("https://s3.example.test", "test-bucket", "publictpap", "insurancelogo");

    @Test
    void shouldFetchCoverAmountCategoryAndAgeAndBuildStandardResponse() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(
                client,
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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

        assertThat(plan.company().logoUrl())
                .isEqualTo("https://s3.example.test/test-bucket/publictpap/insurancelogo/star_1003.svg");

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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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
                JsonMapper.builder().build(),
                LOGOS
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

    @Test
    void shouldRejectNullCoverAmount() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(client, JsonMapper.builder().build(), LOGOS);

        assertThatThrownBy(() -> service.fetch(new PersonalAccidentInsuranceRequest(null, 1, 26)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cover amount");

        verifyNoInteractions(client);
    }

    @Test
    void shouldRejectMissingCategory() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(client, JsonMapper.builder().build(), LOGOS);

        assertThatThrownBy(() -> service.fetch(new PersonalAccidentInsuranceRequest(1_000_000L, null, 26)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("category is required");

        verifyNoInteractions(client);
    }

    @Test
    void shouldRejectCategoryOutsideSupportedRange() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(client, JsonMapper.builder().build(), LOGOS);

        for (Integer category : List.of(0, 4)) {
            assertThatThrownBy(() -> service.fetch(new PersonalAccidentInsuranceRequest(1_000_000L, category, 26)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("category must be one of: 1, 2, 3");
        }

        verifyNoInteractions(client);
    }

    @Test
    void shouldRejectMinorAgeBeforeCallingVendor() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(client, JsonMapper.builder().build(), LOGOS);

        assertThatThrownBy(() -> service.fetch(new PersonalAccidentInsuranceRequest(1_000_000L, 1, 17)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("age must be at least 18");

        verifyNoInteractions(client);
    }

    @Test
    void shouldAcceptCategoryBoundaries() {
        PolicyPlannerClient client = mock(PolicyPlannerClient.class);
        var service = new PersonalAccidentInsuranceService(client, JsonMapper.builder().build(), LOGOS);
        var batch = new PersonalAccidentQuoteBatch(List.of(), List.of());
        when(client.fetchPersonalAccidentQuoteBatch(any(PersonalAccidentVendorRequest.class))).thenReturn(batch);

        assertThat(service.fetch(new PersonalAccidentInsuranceRequest(1_000_000L, 1, 18))).isSameAs(batch);
        assertThat(service.fetch(new PersonalAccidentInsuranceRequest(1_000_000L, 3, 18))).isSameAs(batch);

        verify(client).fetchPersonalAccidentQuoteBatch(new PersonalAccidentVendorRequest(1_000_000L, 1, 18));
        verify(client).fetchPersonalAccidentQuoteBatch(new PersonalAccidentVendorRequest(1_000_000L, 3, 18));
    }

    @Test
    void shouldDropPlanWhenPremiumsAreNull() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, null, List.of(), null);

        var res = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of()));

        assertThat(res.insuranceType()).isEqualTo("PERSONAL_ACCIDENT");
        assertThat(res.planCount()).isZero();
        assertThat(res.plans()).isEmpty();
    }

    @Test
    void shouldSkipInvalidPremiumEntriesAndDropPlanWhenNoneRemain() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var premiums = new ArrayList<PersonalAccidentVendorResponse.Premium>();
        premiums.add(null);
        premiums.add(new PersonalAccidentVendorResponse.Premium(null, new BigDecimal("100")));
        premiums.add(new PersonalAccidentVendorResponse.Premium("   ", new BigDecimal("200")));
        premiums.add(new PersonalAccidentVendorResponse.Premium("basic", null));
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, premiums, List.of(), null);

        var res = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of()));

        assertThat(res.planCount()).isZero();
        assertThat(res.plans()).isEmpty();
    }

    @Test
    void shouldIgnoreInvalidPremiumsAndNormalizeTypeCaseWhenDeduplicating() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var premiums = new ArrayList<PersonalAccidentVendorResponse.Premium>();
        premiums.add(null);
        premiums.add(new PersonalAccidentVendorResponse.Premium(" TTD ", new BigDecimal("900")));
        premiums.add(new PersonalAccidentVendorResponse.Premium("ttd", new BigDecimal("950")));
        premiums.add(new PersonalAccidentVendorResponse.Premium(" Basic ", new BigDecimal("400")));
        premiums.add(new PersonalAccidentVendorResponse.Premium("basic", null));
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, premiums, null, null);

        var plan = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of())).plans().getFirst();

        assertThat(plan.premiumOptions()).extracting(p -> p.type()).containsExactly("TTD", "Basic");
        assertThat(plan.premiumOptions()).extracting(p -> p.value())
                .containsExactly(new BigDecimal("900"), new BigDecimal("400"));
        assertThat(plan.premium().payablePremium()).isEqualByComparingTo("400");
        // features == null: no ids and empty feature groups
        assertThat(plan.company().companyId()).isNull();
        assertThat(plan.plan().planId()).isNull();
        assertThat(plan.includes()).isEmpty();
        assertThat(plan.excludes()).isEmpty();
        assertThat(plan.addons()).isEmpty();
        assertThat(plan.features()).isEmpty();
        assertThat(plan.otherDetails()).isEmpty();
    }

    @Test
    void shouldSkipNullAndBlankFeaturesAndResolveIdsFromFirstFeatureWithAnyId() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var features = new ArrayList<PersonalAccidentVendorResponse.Feature>();
        features.add(null);
        features.add(new PersonalAccidentVendorResponse.Feature(null, null, "  ", null, "  "));
        features.add(new PersonalAccidentVendorResponse.Feature(null, "PLAN-9", "Accidental Death", "War", "Hospital Cash"));
        features.add(new PersonalAccidentVendorResponse.Feature("CO-1", "PLAN-1", " Accidental Death ", "War", null));
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, List.of(new PersonalAccidentVendorResponse.Premium("basic", new BigDecimal("500"))),
                features, null);

        var plan = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of())).plans().getFirst();

        assertThat(plan.company().companyId()).isNull();
        assertThat(plan.plan().planId()).isEqualTo("PLAN-9");
        assertThat(plan.includes()).extracting(f -> f.title()).containsExactly("Accidental Death");
        assertThat(plan.excludes()).extracting(f -> f.title()).containsExactly("War");
        assertThat(plan.addons()).extracting(f -> f.title()).containsExactly("Hospital Cash");
        assertThat(plan.features()).hasSize(3);
    }

    @Test
    void shouldReturnNullIdsWhenNoFeatureCarriesIds() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var features = new ArrayList<PersonalAccidentVendorResponse.Feature>();
        features.add(null);
        features.add(new PersonalAccidentVendorResponse.Feature(null, null, "Accidental Death", null, null));
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, List.of(new PersonalAccidentVendorResponse.Premium("basic", new BigDecimal("500"))),
                features, null);

        var plan = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of())).plans().getFirst();

        assertThat(plan.company().companyId()).isNull();
        assertThat(plan.plan().planId()).isNull();
        assertThat(plan.includes()).extracting(f -> f.title()).containsExactly("Accidental Death");
    }

    @Test
    void shouldConvertMapOtherDetailsKeysToStrings() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var raw = new LinkedHashMap<Object, Object>();
        raw.put(1, "one");
        raw.put("Entry Age", "18 years");
        var q = new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, raw,
                1_000_000L, List.of(new PersonalAccidentVendorResponse.Premium("basic", new BigDecimal("500"))),
                List.of(), null);

        var plan = service.build("req", new PersonalAccidentQuoteBatch(List.of(q), List.of())).plans().getFirst();

        assertThat(plan.otherDetails()).hasSize(2).containsEntry("1", "one").containsEntry("Entry Age", "18 years");
    }

    @Test
    void shouldWrapNonStringNonMapOtherDetailsAndParseJsonString() {
        var service = new PersonalAccidentInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var premiums = List.of(new PersonalAccidentVendorResponse.Premium("basic", new BigDecimal("500")));
        var numeric = new PersonalAccidentVendorResponse("Insurer A", "P1", null, null, null, 42,
                1_000_000L, premiums, List.of(), null);
        var json = new PersonalAccidentVendorResponse("Insurer B", "P2", null, null, null, "{\"Entry Age\":\"18\"}",
                1_000_000L, premiums, List.of(), null);

        var plans = service.build("req", new PersonalAccidentQuoteBatch(List.of(numeric, json), List.of())).plans();

        assertThat(plans.get(0).otherDetails()).hasSize(1).containsEntry("details", 42);
        assertThat(plans.get(1).otherDetails()).hasSize(1).containsEntry("Entry Age", "18");
    }
}