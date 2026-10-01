package com.boltpe.insurance.service.proposalform;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.dto.response.proposalform.InsuranceProposalResponse;

public interface InsuranceProposalService {
    InsuranceProposalResponse save(InsuranceProposalRequest request);
}
