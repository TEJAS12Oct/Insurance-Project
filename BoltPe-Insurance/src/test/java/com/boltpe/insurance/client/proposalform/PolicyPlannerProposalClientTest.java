package com.boltpe.insurance.client.proposalform;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.boltpe.insurance.exception.PolicyPlannerException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class PolicyPlannerProposalClientTest {
    private MockRestServiceServer server;
    private PolicyPlannerProposalClient client;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://policyplanner.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PolicyPlannerProposalClient(builder.build(), new JsonMapper());
    }

    @Test
    void parsesSuccessful201Response() {
        server.expect(once(), requestTo("https://policyplanner.com/health-insurance/proposals/save"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":true,\"message\":\"saved\",\"proposalId\":123,\"formType\":\"health\"}"));

        PolicyPlannerProposalCallResult result = client.save(Map.of("formType", "health"));

        assertThat(result.httpStatus()).isEqualTo(201);
        assertThat(result.saveConfirmed()).isTrue();
        assertThat(result.response().proposalId()).isEqualTo(123);
        server.verify();
    }

    @Test
    void returnsStructuredResultForVendor400() {
        server.expect(once(), requestTo("https://policyplanner.com/health-insurance/proposals/save"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"message\":\"Missing required PA fields\",\"missingFields\":[\"productName\"]}"));

        PolicyPlannerProposalCallResult result = client.save(Map.of("formType", "pa"));

        assertThat(result.httpStatus()).isEqualTo(400);
        assertThat(result.saveConfirmed()).isFalse();
        assertThat(result.response().message()).isEqualTo("Missing required PA fields");
        assertThat(result.response().missingFields()).containsExactly("productName");
    }

    @Test
    void malformedVendorErrorBodyIsStillReturnedWithoutParserFailure() {
        server.expect(once(), requestTo("https://policyplanner.com/health-insurance/proposals/save"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("not-json"));

        PolicyPlannerProposalCallResult result = client.save(Map.of("formType", "health"));

        assertThat(result.httpStatus()).isEqualTo(500);
        assertThat(result.response()).isNull();
        assertThat(result.rawBody()).isEqualTo("not-json");
    }

    @Test
    void emptySuccessfulResponseIsRejected() {
        server.expect(once(), requestTo("https://policyplanner.com/health-insurance/proposals/save"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.save(Map.of("formType", "health")))
                .isInstanceOf(PolicyPlannerException.class)
                .hasMessageContaining("Unable to call Policy Planner proposal API");
    }
}
