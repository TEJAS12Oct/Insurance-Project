package com.boltpe.insurance.client.proposalform;

import static org.assertj.core.api.Assertions.*;

import com.boltpe.insurance.vendor.response.proposalform.PolicyPlannerProposalResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyPlannerProposalCallResultTest {
    @Test
    void confirmsOnlyComplete201Success() {
        var success = new PolicyPlannerProposalResponse(true, "saved", 123, "health", List.of());
        assertThat(new PolicyPlannerProposalCallResult(201, "{}", success).saveConfirmed()).isTrue();
        assertThat(new PolicyPlannerProposalCallResult(200, "{}", success).saveConfirmed()).isFalse();
        assertThat(new PolicyPlannerProposalCallResult(201, "{}", null).saveConfirmed()).isFalse();
        assertThat(new PolicyPlannerProposalCallResult(201, "{}",
                new PolicyPlannerProposalResponse(false, "no", 123, "health", List.of())).saveConfirmed()).isFalse();
        assertThat(new PolicyPlannerProposalCallResult(201, "{}",
                new PolicyPlannerProposalResponse(true, "saved", null, "health", List.of())).saveConfirmed()).isFalse();
    }
}
