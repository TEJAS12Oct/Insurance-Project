package com.boltpe.insurance.client.proposalform;

import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.response.proposalform.PolicyPlannerProposalResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PolicyPlannerProposalClient {
    private static final String PROPOSAL_PATH = "/health-insurance/proposals/save";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public PolicyPlannerProposalClient(RestClient policyPlannerRestClient, JsonMapper jsonMapper) {
        this.restClient = policyPlannerRestClient;
        this.jsonMapper = jsonMapper;
    }

    public PolicyPlannerProposalCallResult save(Object vendorRequest) {
        try {
            ResponseEntity<String> entity = restClient.post()
                    .uri(PROPOSAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vendorRequest)
                    .retrieve()
                    .toEntity(String.class);
            String body = entity.getBody() == null ? "" : entity.getBody();
            return new PolicyPlannerProposalCallResult(
                    entity.getStatusCode().value(), body, parse(body));
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            return new PolicyPlannerProposalCallResult(
                    ex.getStatusCode().value(), body, parseSafely(body));
        } catch (RuntimeException ex) {
            throw new PolicyPlannerException("Unable to call Policy Planner proposal API", ex);
        }
    }

    private PolicyPlannerProposalResponse parse(String body) {
        if (body == null || body.isBlank()) {
            throw new PolicyPlannerException("Policy Planner proposal API returned an empty response");
        }
        try {
            return jsonMapper.readValue(body, PolicyPlannerProposalResponse.class);
        } catch (RuntimeException ex) {
            throw new PolicyPlannerException("Unable to parse Policy Planner proposal response", ex);
        }
    }

    private PolicyPlannerProposalResponse parseSafely(String body) {
        try {
            return body == null || body.isBlank() ? null : jsonMapper.readValue(body, PolicyPlannerProposalResponse.class);
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
