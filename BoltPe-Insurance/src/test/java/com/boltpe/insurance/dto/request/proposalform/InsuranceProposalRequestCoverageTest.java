package com.boltpe.insurance.dto.request.proposalform;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceProposalRequestCoverageTest {
    @Test void preservesFrontendWrapperAndAdditionalFields() {
        var request = new InsuranceProposalRequest();
        request.setClientName("bolt_pay");
        request.setFormType("health");
        request.put("insuranceType", "HEALTH");
        request.put("data", Map.of("proposerName", "Test"));
        assertEquals("bolt_pay", request.getClientName());
        assertEquals("health", request.getFormType());
        assertEquals("HEALTH", request.getFields().get("insuranceType"));
        var flat = request.toFlatMap();
        assertEquals("bolt_pay", flat.get("client_name"));
        assertEquals("health", flat.get("formType"));
        assertEquals(request.getFields().get("data"), flat.get("data"));
        assertEquals(4, flat.size());
    }

    @Test void emptyRequestRetainsExpectedWrapperKeys() {
        var request = new InsuranceProposalRequest();
        assertNull(request.getClientName());
        assertNull(request.getFormType());
        assertTrue(request.getFields().isEmpty());
        assertEquals(2, request.toFlatMap().size());
    }
}
