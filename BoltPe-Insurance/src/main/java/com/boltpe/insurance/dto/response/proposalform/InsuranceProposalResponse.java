package com.boltpe.insurance.dto.response.proposalform;

public record InsuranceProposalResponse(
        boolean success,
        String message,
        String requestId,
        ProposalResponseData data
) {
}
