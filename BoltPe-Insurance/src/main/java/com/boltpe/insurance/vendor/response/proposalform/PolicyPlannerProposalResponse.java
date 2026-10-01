package com.boltpe.insurance.vendor.response.proposalform;

import java.util.List;

public record PolicyPlannerProposalResponse(
        Boolean success,
        String message,
        Object proposalId,
        String formType,
        List<String> missingFields
) {
}
