package com.boltpe.insurance.client.proposalform;

import com.boltpe.insurance.vendor.response.proposalform.PolicyPlannerProposalResponse;

public record PolicyPlannerProposalCallResult(
        int httpStatus,
        String rawBody,
        PolicyPlannerProposalResponse response
) {
    public boolean saveConfirmed() {
        return httpStatus == 201
                && response != null
                && Boolean.TRUE.equals(response.success())
                && response.proposalId() != null;
    }
}
