package com.boltpe.insurance.service.proposalform;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.mapper.proposalform.PolicyPlannerProposalMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProposalRoutingServicesTest {
    @Test
    void groupServiceDelegatesToMapper() {
        PolicyPlannerProposalMapper mapper = mock(PolicyPlannerProposalMapper.class);
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        Map<String, Object> expected = Map.of("formType", "health");
        when(mapper.toVendorRequest(request)).thenReturn(expected);

        assertThat(new GroupProposalService(mapper).toVendorRequest(request)).isSameAs(expected);
    }

    @Test
    void personalAccidentServiceDelegatesToMapper() {
        PolicyPlannerProposalMapper mapper = mock(PolicyPlannerProposalMapper.class);
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        Map<String, Object> expected = Map.of("formType", "pa");
        when(mapper.toVendorRequest(request)).thenReturn(expected);

        assertThat(new PersonalAccidentProposalService(mapper).toVendorRequest(request)).isSameAs(expected);
    }
}
