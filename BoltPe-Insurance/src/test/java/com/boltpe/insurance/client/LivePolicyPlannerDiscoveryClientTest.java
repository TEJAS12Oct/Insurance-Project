package com.boltpe.insurance.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.boltpe.insurance.config.PolicyPlannerProperties;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.PolicyPlannerException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class LivePolicyPlannerDiscoveryClientTest {

    private record Fixture(
            LivePolicyPlannerDiscoveryClient client,
            MockRestServiceServer server
    ) {}

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://policyplanner.com");

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        PolicyPlannerProperties properties =
                new PolicyPlannerProperties(
                        "https://policyplanner.com",
                        new PolicyPlannerProperties.Health("/h"),
                        new PolicyPlannerProperties.SuperTopUp("/s"),
                        new PolicyPlannerProperties.PersonalAccident("/p"),
                        new PolicyPlannerProperties.HospitalCash("/c"),
                        5000,
                        15000
                );

        LivePolicyPlannerDiscoveryClient client =
                new LivePolicyPlannerDiscoveryClient(
                        builder.build(),
                        properties,
                        JsonMapper.builder().build()
                );

        return new Fixture(client, server);
    }

    @Test
    void discoversAndExtractsIdsForAllProducts() {

        Fixture fixture = fixture();

        /*
         * IMPORTANT:
         * Register ALL expectations before making the first HTTP request.
         */

        fixture.server()
                .expect(requestTo("https://policyplanner.com/h"))
                .andRespond(withSuccess(
                        """
                        {
                          "success": true,
                          "count": 1,
                          "policy_filter": "Health",
                          "data": [
                            {
                              "api_type": "/health-insurance/sbi/1004/225/premium"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        fixture.server()
                .expect(requestTo("https://policyplanner.com/s"))
                .andRespond(withSuccess(
                        """
                        {
                          "success": true,
                          "count": 1,
                          "policy_filter": "super_top_up",
                          "data": [
                            {
                              "api_type": "/health-insurance/national/1005/193/supertopup"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        fixture.server()
                .expect(requestTo("https://policyplanner.com/p"))
                .andRespond(withSuccess(
                        """
                        {
                          "success": true,
                          "count": 1,
                          "policy_filter": "pa",
                          "data": [
                            {
                              "api_type": "/health-insurance/star/1003/227/pa"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        fixture.server()
                .expect(requestTo("https://policyplanner.com/c"))
                .andRespond(withSuccess(
                        """
                        {
                          "success": true,
                          "count": 1,
                          "policy_filter": "hc",
                          "data": [
                            {
                              "api_type": "/health-insurance/x/1017/224/hc"
                            }
                          ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        // Execute only AFTER all expectations are registered.

        var health =
                fixture.client().discover(InsuranceType.HEALTH);

        var superTopUp =
                fixture.client().discover(InsuranceType.SUPER_TOP_UP);

        var personalAccident =
                fixture.client().discover(InsuranceType.PERSONAL_ACCIDENT);

        var hospitalCash =
                fixture.client().discover(InsuranceType.HOSPITAL_CASH);

        // HEALTH
        assertEquals(
                "1004",
                health.records().getFirst().companyId()
        );

        assertEquals(
                "225",
                health.records().getFirst().planId()
        );

        // SUPER TOP UP
        assertEquals(
                "1005",
                superTopUp.records().getFirst().companyId()
        );

        assertEquals(
                "193",
                superTopUp.records().getFirst().planId()
        );

        // PERSONAL ACCIDENT
        assertEquals(
                "1003",
                personalAccident.records().getFirst().companyId()
        );

        assertEquals(
                "227",
                personalAccident.records().getFirst().planId()
        );

        // HOSPITAL CASH
        assertEquals(
                "1017",
                hospitalCash.records().getFirst().companyId()
        );

        assertEquals(
                "224",
                hospitalCash.records().getFirst().planId()
        );

        fixture.server().verify();
    }

    @Test
    void emptyDiscoveryIsRejectedBeforeAnyDbReplacement() {

        Fixture fixture = fixture();

        fixture.server()
                .expect(requestTo("https://policyplanner.com/h"))
                .andRespond(withSuccess(
                        """
                        {
                          "success": true,
                          "count": 0,
                          "policy_filter": "Health",
                          "data": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThrows(
                PolicyPlannerException.class,
                () -> fixture.client().discover(InsuranceType.HEALTH)
        );

        fixture.server().verify();
    }
}