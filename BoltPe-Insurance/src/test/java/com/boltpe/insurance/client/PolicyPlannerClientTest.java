package com.boltpe.insurance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.boltpe.insurance.config.PolicyPlannerProperties;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.request.HealthVendorRequest;
import com.boltpe.insurance.vendor.request.SuperTopUpVendorRequest;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HealthDiscoveryResponse;
import com.boltpe.insurance.vendor.response.HealthPlanApiDescriptor;
import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import com.boltpe.insurance.vendor.response.SuperTopUpDiscoveryResponse;
import com.boltpe.insurance.vendor.response.SuperTopUpPlanApiDescriptor;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentDiscoveryResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentPlanApiDescriptor;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import com.boltpe.insurance.vendor.response.HospitalCashDiscoveryResponse;
import com.boltpe.insurance.vendor.response.HospitalCashPlanApiDescriptor;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PolicyPlannerClientTest {

    private PolicyPlannerHttpGateway gateway;
    private PolicyPlannerClient client;
    private HealthVendorRequest request;

    @BeforeEach
    void setUp() {
        gateway = mock(PolicyPlannerHttpGateway.class);

        var properties = new PolicyPlannerProperties(
                "https://policyplanner.com",
                new PolicyPlannerProperties.Health(
                        "/health-insurance/companies/plans?policy=Health"
                ),
                new PolicyPlannerProperties.SuperTopUp(
                        "/health-insurance/companies/plans?policy=super_top_up"
                ),
                new PolicyPlannerProperties.PersonalAccident(
                        "/health-insurance/companies/plans?policy=pa"
                ),
                new PolicyPlannerProperties.HospitalCash(
                        "/health-insurance/companies/plans?policy=hc"
                ),
                1000,
                1000
        );

        client = new PolicyPlannerClient(gateway, properties);

        request = new HealthVendorRequest(
                1000000L,
                "1",
                30,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldFetchQuotesAndIgnoreOneProviderFailure() {
        var first = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/icici/1009/191/premium"
        );

        var second = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/sbi/1004/225/premium"
        );

        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                2,
                                "Health",
                                List.of(first, second)
                        )
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/icici/1009/191/premium",
                request))
                .thenReturn(response());

        when(gateway.fetchHealthQuote(
                "/health-insurance/sbi/1004/225/premium",
                request))
                .thenThrow(new RuntimeException("down"));

        assertThat(client.fetchHealthQuotes(request))
                .hasSize(1);
    }

    @Test
    void shouldSkipBlankPremiumUrl() {
        var blank = new HealthPlanApiDescriptor(" ");

        var valid = new HealthPlanApiDescriptor(
                "/health-insurance/icici/1009/191/premium"
        );

        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                2,
                                "Health",
                                List.of(blank, valid)
                        )
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/icici/1009/191/premium",
                request))
                .thenReturn(response());

        assertThat(client.fetchHealthQuotes(request))
                .hasSize(1);
    }

    @Test
    void shouldRejectExternalAbsoluteUrl() {
        assertThatThrownBy(() ->
                client.validateAndNormalizeVendorUrl(
                        "https://evil.example.com/premium"
                )
        ).isInstanceOf(PolicyPlannerException.class);
    }

    @Test
    void shouldAcceptAbsoluteUrlOnSameHost() {
        assertThat(
                client.validateAndNormalizeVendorUrl(
                        "https://policyplanner.com/health-insurance/icici/1009/191/premium?a=1"
                )
        ).isEqualTo(
                "/health-insurance/icici/1009/191/premium?a=1"
        );
    }

    @Test
    void shouldAcceptRelativeUrl() {
        assertThat(
                client.validateAndNormalizeVendorUrl(
                        "health-insurance/icici/1009/191/premium"
                )
        ).isEqualTo(
                "/health-insurance/icici/1009/191/premium"
        );
    }

    @Test
    void shouldFailWhenDiscoveryHasNoPlans() {
        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                0,
                                "Health",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchHealthQuotes(request))
                .isInstanceOf(PolicyPlannerException.class)
                .hasMessageContaining(
                        "no health plan APIs"
                );
    }

    @Test
    void shouldFailWhenDiscoveryReportsFailure() {
        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                false,
                                0,
                                "Health",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchHealthQuotes(request))
                .isInstanceOf(PolicyPlannerException.class);
    }

    @Test
    void shouldRejectSchemeRelativeUrl() {
        assertThatThrownBy(() ->
                client.validateAndNormalizeVendorUrl(
                        "//evil.example.com/premium"
                )
        ).isInstanceOf(PolicyPlannerException.class);
    }

    @Test
    void shouldFailWhenDiscoveryCallThrows() {
        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenThrow(
                        new RuntimeException("timeout")
                );

        assertThatThrownBy(() ->
                client.fetchHealthQuotes(request))
                .isInstanceOf(PolicyPlannerException.class)
                .hasMessageContaining("discover");
    }

    @Test
    void shouldFailWhenAllQuoteCallsFail() {
        var descriptor = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/icici/1009/191/premium"
        );

        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                1,
                                "Health",
                                List.of(descriptor)
                        )
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/icici/1009/191/premium",
                request))
                .thenThrow(
                        new RuntimeException("down")
                );

        assertThatThrownBy(() ->
                client.fetchHealthQuotes(request))
                .isInstanceOf(PolicyPlannerException.class)
                .hasMessageContaining(
                        "No Policy Planner health provider"
                );
    }

    @Test
    void shouldIgnoreNullQuoteResponseAndReturnSuccessfulQuotes() {
        var nullResponsePlan = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/star/1003/218/premium"
        );

        var successfulPlan = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/icici/1009/191/premium"
        );

        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                2,
                                "Health",
                                List.of(
                                        nullResponsePlan,
                                        successfulPlan
                                )
                        )
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/star/1003/218/premium",
                request))
                .thenReturn(null);

        when(gateway.fetchHealthQuote(
                "/health-insurance/icici/1009/191/premium",
                request))
                .thenReturn(response());

        assertThat(client.fetchHealthQuotes(request))
                .hasSize(1);
    }

    @Test
    void shouldReturnAllFailedApiStatusesWhenBatchQuoteCallsFail() {
        var first = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/icici/1009/191/premium"
        );

        var second = new HealthPlanApiDescriptor(
                "https://policyplanner.com/health-insurance/sbi/1004/225/premium"
        );

        when(gateway.discoverHealthPlans(
                "/health-insurance/companies/plans?policy=Health"))
                .thenReturn(
                        new HealthDiscoveryResponse(
                                true,
                                2,
                                "Health",
                                List.of(first, second)
                        )
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/icici/1009/191/premium",
                request))
                .thenThrow(
                        new RuntimeException("timeout")
                );

        when(gateway.fetchHealthQuote(
                "/health-insurance/sbi/1004/225/premium",
                request))
                .thenThrow(
                        new RuntimeException("no premium")
                );

        HealthQuoteBatch batch =
                client.fetchHealthQuoteBatch(request);

        assertThat(batch.quotes()).isEmpty();

        assertThat(batch.apiStatuses())
                .hasSize(2)
                .allMatch(status -> !status.active());

        assertThat(batch.apiStatuses())
                .extracting(
                        HealthQuoteBatch.ApiStatus::failureReason
                )
                .containsExactly(
                        "timeout",
                        "no premium"
                );
    }

    @Test
    void shouldFetchSuperTopUpQuotesAndKeepSuccessAndFailureStatuses() {
        var request =
                new SuperTopUpVendorRequest(
                        1000000L,
                        18,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        var good =
                new SuperTopUpPlanApiDescriptor(
                        "https://policyplanner.com/health-insurance/nic/1005/193/supertopup"
                );

        var bad =
                new SuperTopUpPlanApiDescriptor(
                        "https://policyplanner.com/health-insurance/hdfc/1008/194/supertopup"
                );

        when(gateway.discoverSuperTopUpPlans(
                "/health-insurance/companies/plans?policy=super_top_up"))
                .thenReturn(
                        new SuperTopUpDiscoveryResponse(
                                true,
                                2,
                                "super_top_up",
                                List.of(good, bad)
                        )
                );

        var ok = superTopUpResponse(
                null,
                List.of(
                        new SuperTopUpVendorResponse.Premium(
                                BigDecimal.valueOf(1838),
                                "500000"
                        )
                )
        );

        when(gateway.fetchSuperTopUpQuote(
                "/health-insurance/nic/1005/193/supertopup",
                request))
                .thenReturn(ok);

        when(gateway.fetchSuperTopUpQuote(
                "/health-insurance/hdfc/1008/194/supertopup",
                request))
                .thenThrow(
                        new RuntimeException("timeout")
                );

        var batch =
                client.fetchSuperTopUpQuoteBatch(request);

        assertThat(batch.quotes()).hasSize(1);
        assertThat(batch.apiStatuses()).hasSize(2);

        assertThat(batch.apiStatuses())
                .extracting(
                        SuperTopUpQuoteBatch.ApiStatus::active
                )
                .containsExactly(true, false);

        assertThat(
                batch.apiStatuses()
                        .get(1)
                        .failureReason()
        ).isEqualTo("timeout");
    }

    @Test
    void shouldTreatSuperTopUpNoPremiumAsPlanFailureAndContinue() {
        var request =
                new SuperTopUpVendorRequest(
                        1000000L,
                        18,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        var noPremium =
                new SuperTopUpPlanApiDescriptor(
                        "/health-insurance/nic/1005/193/supertopup"
                );

        var success =
                new SuperTopUpPlanApiDescriptor(
                        "/health-insurance/hdfc/1008/194/supertopup"
                );

        when(gateway.discoverSuperTopUpPlans(
                "/health-insurance/companies/plans?policy=super_top_up"))
                .thenReturn(
                        new SuperTopUpDiscoveryResponse(
                                true,
                                2,
                                "super_top_up",
                                List.of(
                                        noPremium,
                                        success
                                )
                        )
                );

        when(gateway.fetchSuperTopUpQuote(
                "/health-insurance/nic/1005/193/supertopup",
                request))
                .thenReturn(
                        superTopUpResponse(
                                "No premiums found",
                                List.of()
                        )
                );

        when(gateway.fetchSuperTopUpQuote(
                "/health-insurance/hdfc/1008/194/supertopup",
                request))
                .thenReturn(
                        superTopUpResponse(
                                null,
                                List.of(
                                        new SuperTopUpVendorResponse.Premium(
                                                BigDecimal.TEN,
                                                "800000"
                                        )
                                )
                        )
                );

        var batch =
                client.fetchSuperTopUpQuoteBatch(request);

        assertThat(batch.quotes()).hasSize(1);
        assertThat(batch.apiStatuses()).hasSize(2);

        assertThat(
                batch.apiStatuses()
                        .getFirst()
                        .failureReason()
        ).isEqualTo(
                "No premiums found"
        );
    }

    @Test
    void shouldFailSuperTopUpWhenDiscoveryUnavailable() {
        var request =
                new SuperTopUpVendorRequest(
                        1000000L,
                        18,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(gateway.discoverSuperTopUpPlans(
                "/health-insurance/companies/plans?policy=super_top_up"))
                .thenThrow(
                        new RuntimeException("down")
                );

        assertThatThrownBy(() ->
                client.fetchSuperTopUpQuoteBatch(request))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining("discover");
    }

    @Test
    void shouldFailSuperTopUpWhenDiscoveryEmpty() {
        var request =
                new SuperTopUpVendorRequest(
                        1000000L,
                        18,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(gateway.discoverSuperTopUpPlans(
                "/health-insurance/companies/plans?policy=super_top_up"))
                .thenReturn(
                        new SuperTopUpDiscoveryResponse(
                                true,
                                0,
                                "super_top_up",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchSuperTopUpQuoteBatch(request))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining(
                        "no Super Top-Up plan APIs"
                );
    }

    // =========================================================
    // PERSONAL ACCIDENT
    // =========================================================

    @Test
    void shouldFetchPersonalAccidentQuotesAndKeepSuccessAndFailureStatuses() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        var good =
                new PersonalAccidentPlanApiDescriptor(
                        "https://policyplanner.com/health-insurance/star/1003/227/pa"
                );

        var bad =
                new PersonalAccidentPlanApiDescriptor(
                        "https://policyplanner.com/health-insurance/care/1002/202/pa"
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                true,
                                2,
                                "pa",
                                List.of(good, bad)
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/star/1003/227/pa",
                paRequest))
                .thenReturn(
                        personalAccidentResponse(
                                null,
                                List.of(
                                        new PersonalAccidentVendorResponse.Premium(
                                                "basic",
                                                BigDecimal.valueOf(507)
                                        )
                                )
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/care/1002/202/pa",
                paRequest))
                .thenThrow(
                        new RuntimeException("timeout")
                );

        var batch =
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                );

        assertThat(batch.quotes())
                .hasSize(1);

        assertThat(batch.apiStatuses())
                .hasSize(2);

        assertThat(batch.apiStatuses())
                .extracting(
                        PersonalAccidentQuoteBatch
                                .ApiStatus::active
                )
                .containsExactly(
                        true,
                        false
                );

        var successStatus =
                batch.apiStatuses().get(0);

        assertThat(successStatus.active())
                .isTrue();

        assertThat(successStatus.companyId())
                .isEqualTo("1003");

        assertThat(successStatus.planId())
                .isEqualTo("227");

        assertThat(successStatus.reason())
                .isNull();

        var failedStatus =
                batch.apiStatuses().get(1);

        assertThat(failedStatus.active())
                .isFalse();

        assertThat(failedStatus.reason())
                .isEqualTo("timeout");
    }

    @Test
    void shouldTreatPersonalAccidentNoPremiumAsPlanFailureAndContinue() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        var noPremium =
                new PersonalAccidentPlanApiDescriptor(
                        "/health-insurance/care/1002/202/pa"
                );

        var success =
                new PersonalAccidentPlanApiDescriptor(
                        "/health-insurance/star/1003/227/pa"
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                true,
                                2,
                                "pa",
                                List.of(
                                        noPremium,
                                        success
                                )
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/care/1002/202/pa",
                paRequest))
                .thenReturn(
                        personalAccidentResponse(
                                "No premiums found",
                                List.of()
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/star/1003/227/pa",
                paRequest))
                .thenReturn(
                        personalAccidentResponse(
                                null,
                                List.of(
                                        new PersonalAccidentVendorResponse.Premium(
                                                "TTD",
                                                BigDecimal.valueOf(1475)
                                        )
                                )
                        )
                );

        var batch =
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                );

        assertThat(batch.quotes())
                .hasSize(1);

        assertThat(batch.apiStatuses())
                .hasSize(2);

        var failedStatus =
                batch.apiStatuses()
                        .getFirst();

        assertThat(failedStatus.active())
                .isFalse();

        assertThat(failedStatus.reason())
                .isEqualTo(
                        "No premiums found"
                );

        var successStatus =
                batch.apiStatuses()
                        .get(1);

        assertThat(successStatus.active())
                .isTrue();
    }

    @Test
    void shouldTreatNullPersonalAccidentResponseAsNoPremiumFailure() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        var plan =
                new PersonalAccidentPlanApiDescriptor(
                        "/health-insurance/star/1003/227/pa"
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                true,
                                1,
                                "pa",
                                List.of(plan)
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/star/1003/227/pa",
                paRequest))
                .thenReturn(null);

        var batch =
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                );

        assertThat(batch.quotes())
                .isEmpty();

        assertThat(batch.apiStatuses())
                .singleElement()
                .satisfies(status -> {

                    assertThat(status.active())
                            .isFalse();

                    assertThat(status.reason())
                            .isEqualTo(
                                    "No premiums found"
                            );
                });
    }

    @Test
    void shouldSkipBlankPersonalAccidentApiType() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        var blank =
                new PersonalAccidentPlanApiDescriptor(
                        " "
                );

        var valid =
                new PersonalAccidentPlanApiDescriptor(
                        "/health-insurance/star/1003/227/pa"
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                true,
                                2,
                                "pa",
                                List.of(
                                        blank,
                                        valid
                                )
                        )
                );

        when(gateway.fetchPersonalAccidentQuote(
                "/health-insurance/star/1003/227/pa",
                paRequest))
                .thenReturn(
                        personalAccidentResponse(
                                null,
                                List.of(
                                        new PersonalAccidentVendorResponse.Premium(
                                                "basic",
                                                BigDecimal.ONE
                                        )
                                )
                        )
                );

        var batch =
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                );

        assertThat(batch.quotes())
                .hasSize(1);

        assertThat(batch.apiStatuses())
                .hasSize(1);
    }

    @Test
    void shouldFailPersonalAccidentWhenDiscoveryUnavailable() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenThrow(
                        new RuntimeException("down")
                );

        assertThatThrownBy(() ->
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining(
                        "discover"
                );
    }

    @Test
    void shouldFailPersonalAccidentWhenDiscoveryEmptyOrFailed() {

        var paRequest =
                new PersonalAccidentVendorRequest(
                        1_000_000L,
                        2,
                        26
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                true,
                                0,
                                "pa",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining(
                        "no Personal Accident plan APIs"
                );

        when(gateway.discoverPersonalAccidentPlans(
                "/health-insurance/companies/plans?policy=pa"))
                .thenReturn(
                        new PersonalAccidentDiscoveryResponse(
                                false,
                                0,
                                "pa",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchPersonalAccidentQuoteBatch(
                        paRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                );
    }

    // =========================================================
    // HOSPITAL CASH
    // =========================================================

    @Test
    void shouldFetchHospitalCashQuotesAndKeepSuccessAndFailureStatuses() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        3000L,
                        1,
                        0,
                        30
                );

        var good =
                new HospitalCashPlanApiDescriptor(
                        "https://policyplanner.com/health-insurance/generali/1017/224/hc"
                );

        var bad =
                new HospitalCashPlanApiDescriptor(
                        "/health-insurance/other/1099/299/hospitalcash"
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                true,
                                2,
                                "hc",
                                List.of(good, bad)
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/generali/1017/224/hc",
                hcRequest))
                .thenReturn(
                        hospitalCashResponse(
                                null,
                                BigDecimal.valueOf(2078)
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/other/1099/299/hospitalcash",
                hcRequest))
                .thenThrow(
                        new RuntimeException("timeout")
                );

        var batch =
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                );

        assertThat(batch.quotes())
                .hasSize(1);

        assertThat(batch.apiStatuses())
                .hasSize(2);

        assertThat(
                batch.apiStatuses()
                        .get(0)
                        .active()
        ).isTrue();

        assertThat(
                batch.apiStatuses()
                        .get(0)
                        .companyId()
        ).isEqualTo("1017");

        assertThat(
                batch.apiStatuses()
                        .get(0)
                        .planId()
        ).isEqualTo("224");

        assertThat(
                batch.apiStatuses()
                        .get(1)
                        .active()
        ).isFalse();

        assertThat(
                batch.apiStatuses()
                        .get(1)
                        .reason()
        ).isEqualTo("timeout");
    }

    @Test
    void shouldTreatHospitalCashMissingPremiumAndVendorErrorAsFailures() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        3000L,
                        1,
                        0,
                        60
                );

        var missing =
                new HospitalCashPlanApiDescriptor(
                        "/health-insurance/generali/1017/224/hc"
                );

        var error =
                new HospitalCashPlanApiDescriptor(
                        "/health-insurance/other/1099/299/hospital-cash"
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                true,
                                2,
                                "hc",
                                List.of(
                                        missing,
                                        error
                                )
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/generali/1017/224/hc",
                hcRequest))
                .thenReturn(
                        hospitalCashResponse(
                                null,
                                null
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/other/1099/299/hospital-cash",
                hcRequest))
                .thenReturn(
                        hospitalCashResponse(
                                "Premium unavailable",
                                BigDecimal.valueOf(1)
                        )
                );

        var batch =
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                );

        assertThat(batch.quotes())
                .isEmpty();

        assertThat(batch.apiStatuses())
                .extracting(
                        HospitalCashQuoteBatch
                                .ApiStatus::reason
                )
                .containsExactly(
                        "No premium found",
                        "Premium unavailable"
                );
    }

    @Test
    void shouldTreatNullHospitalCashResponseAsNoPremiumFailure() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        5000L,
                        1,
                        0,
                        90
                );

        var plan =
                new HospitalCashPlanApiDescriptor(
                        "/health-insurance/generali/1017/224/hc"
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                true,
                                1,
                                "hc",
                                List.of(plan)
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/generali/1017/224/hc",
                hcRequest))
                .thenReturn(null);

        var batch =
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                );

        assertThat(batch.quotes())
                .isEmpty();

        assertThat(batch.apiStatuses())
                .singleElement()
                .satisfies(status -> {

                    assertThat(status.active())
                            .isFalse();

                    assertThat(status.reason())
                            .isEqualTo(
                                    "No premium found"
                            );
                });
    }

    @Test
    void shouldSkipBlankHospitalCashApiAndSupportHospitalCashPathVariant() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        2500L,
                        1,
                        0,
                        180
                );

        var blank =
                new HospitalCashPlanApiDescriptor(
                        " "
                );

        var valid =
                new HospitalCashPlanApiDescriptor(
                        "health-insurance/generali/1017/224/hospital-cash"
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                true,
                                2,
                                "hc",
                                List.of(
                                        blank,
                                        valid
                                )
                        )
                );

        when(gateway.fetchHospitalCashQuote(
                "/health-insurance/generali/1017/224/hospital-cash",
                hcRequest))
                .thenReturn(
                        hospitalCashResponse(
                                null,
                                BigDecimal.valueOf(2500)
                        )
                );

        var batch =
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                );

        assertThat(batch.quotes())
                .hasSize(1);

        assertThat(batch.apiStatuses())
                .singleElement()
                .satisfies(status -> {

                    assertThat(status.active())
                            .isTrue();

                    assertThat(status.companyId())
                            .isEqualTo("1017");

                    assertThat(status.planId())
                            .isEqualTo("224");
                });
    }

    @Test
    void shouldFailHospitalCashWhenDiscoveryUnavailable() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        3000L,
                        1,
                        0,
                        30
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenThrow(
                        new RuntimeException("down")
                );

        assertThatThrownBy(() ->
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining(
                        "discover"
                );
    }

    @Test
    void shouldFailHospitalCashWhenDiscoveryEmptyOrFailed() {

        var hcRequest =
                new HospitalCashVendorRequest(
                        26,
                        3000L,
                        1,
                        0,
                        30
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                true,
                                0,
                                "hc",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                )
                .hasMessageContaining(
                        "no Hospital Cash plan APIs"
                );

        when(gateway.discoverHospitalCashPlans(
                "/health-insurance/companies/plans?policy=hc"))
                .thenReturn(
                        new HospitalCashDiscoveryResponse(
                                false,
                                0,
                                "hc",
                                List.of()
                        )
                );

        assertThatThrownBy(() ->
                client.fetchHospitalCashQuoteBatch(
                        hcRequest
                ))
                .isInstanceOf(
                        PolicyPlannerException.class
                );
    }

    private static HospitalCashVendorResponse hospitalCashResponse(
            String error,
            BigDecimal premium
    ) {
        return new HospitalCashVendorResponse(
                "Generali",
                "Hospicash Plan",
                "generali.jpg",
                "brochure.pdf",
                "onepager.png",
                "{\"ICU Benefit\":\"2x\"}",
                3000L,
                premium,
                30,
                List.of(
                        new HospitalCashVendorResponse.Feature(
                                "1017",
                                "224",
                                "Daily Hospital Cash",
                                "Waiting period",
                                null
                        )
                ),
                error
        );
    }

    private static PersonalAccidentVendorResponse personalAccidentResponse(
            String error,
            List<PersonalAccidentVendorResponse.Premium> premiums
    ) {
        return new PersonalAccidentVendorResponse(
                "STAR HEALTH",
                "Accident Care Individual",
                "star.png",
                "brochure.pdf",
                "",
                "{\"Entry Age\":\"91 Days onwards\"}",
                1_000_000L,
                premiums,
                List.of(
                        new PersonalAccidentVendorResponse.Feature(
                                "1003",
                                "227",
                                "Accidental Death",
                                "Natural death",
                                null
                        )
                ),
                error
        );
    }

    private static SuperTopUpVendorResponse superTopUpResponse(
            String error,
            List<SuperTopUpVendorResponse.Premium> premiums
    ) {
        return new SuperTopUpVendorResponse(
                "ok",
                error,
                "1005",
                "193",
                "National",
                "logo",
                null,
                null,
                "Top Up",
                1000000L,
                1,
                0,
                18,
                18,
                premiums,
                List.of(),
                null,
                List.of()
        );
    }

    private static HealthVendorResponse response() {
        return new HealthVendorResponse(
                "ok",
                "1009",
                "191",
                "ICICI Health Plan",
                1000000L,
                1,
                1,
                0,
                30,
                30,
                BigDecimal.valueOf(9980),
                BigDecimal.valueOf(9980),
                BigDecimal.ZERO,
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}