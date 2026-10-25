package com.boltpe.insurance.client.proposalform;

import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.response.proposalform.PolicyPlannerProposalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PolicyPlannerProposalClient {
    private static final Logger log = LoggerFactory.getLogger(PolicyPlannerProposalClient.class);
    private static final String PROPOSAL_PATH = "/health-insurance/proposals/save";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public PolicyPlannerProposalClient(RestClient policyPlannerRestClient, JsonMapper jsonMapper) {
        this.restClient = policyPlannerRestClient;
        this.jsonMapper = jsonMapper;
    }

    public PolicyPlannerProposalCallResult save(Object vendorRequest) {
        log.info("Policy Planner proposal HTTP call started: method=POST, path={}", PROPOSAL_PATH);
        try {
            ResponseEntity<String> entity = restClient.post()
                    .uri(PROPOSAL_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vendorRequest)
                    .retrieve()
                    .toEntity(String.class);
            String body = entity.getBody() == null ? "" : entity.getBody();
            log.info("Policy Planner proposal HTTP call completed: path={}, status={}",
                    PROPOSAL_PATH, entity.getStatusCode().value());
            return new PolicyPlannerProposalCallResult(
                    entity.getStatusCode().value(), body, parse(body));
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Policy Planner proposal HTTP error response: path={}, status={}, message={}",
                    PROPOSAL_PATH, ex.getStatusCode().value(), ex.getMessage());
            return new PolicyPlannerProposalCallResult(
                    ex.getStatusCode().value(), body, parseSafely(body));
        } catch (RuntimeException ex) {
            log.error("Policy Planner proposal HTTP transport failure: path={}, exceptionType={}, message={}",
                    PROPOSAL_PATH, ex.getClass().getSimpleName(), ex.getMessage(), ex);
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
