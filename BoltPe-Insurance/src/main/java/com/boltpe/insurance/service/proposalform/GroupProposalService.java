package com.boltpe.insurance.service.proposalform;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.mapper.proposalform.PolicyPlannerProposalMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GroupProposalService {
    private final PolicyPlannerProposalMapper mapper;

    public GroupProposalService(PolicyPlannerProposalMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> toVendorRequest(InsuranceProposalRequest request) {
        return mapper.toVendorRequest(request);
    }
}
