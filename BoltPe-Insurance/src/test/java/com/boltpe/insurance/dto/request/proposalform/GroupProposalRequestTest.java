package com.boltpe.insurance.dto.request.proposalform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupProposalRequestTest {
    private GroupProposalRequest request(List<AdultMemberRequest> adults, List<ChildMemberRequest> children) {
        return new GroupProposalRequest("Sample Health Plan", "Fresh", "400001", "Mumbai, Maharashtra", "1", "1",
                1, 1, "Test User", "01-01-1990", "Male", "test.user@example.com", "9000000001",
                new BigDecimal("500000"), BigDecimal.ONE, "5-10 Lakh", "No", "Yes", "No",
                "234567890123", "ABCDE1234F", new BigDecimal("10000"), new BigDecimal("11800"), adults, children);
    }

    @Test void nullMemberListsBecomeEmpty() {
        var r = request(null, null);
        assertTrue(r.adults().isEmpty());
        assertTrue(r.children().isEmpty());
    }

    @Test void memberListsAreCopiedAndUnmodifiable() {
        var adult = new AdultMemberRequest("Self", "Mr", "Test User", "01-01-1990",
                new BigDecimal("170"), new BigDecimal("70"), null, "No", null);
        var child = new ChildMemberRequest("Son", "Test Child", "01-01-2015",
                new BigDecimal("120"), new BigDecimal("25"), null);
        List<AdultMemberRequest> adults = new ArrayList<>(List.of(adult));
        List<ChildMemberRequest> children = new ArrayList<>(List.of(child));

        var r = request(adults, children);
        adults.clear();
        children.clear();

        assertEquals(List.of(adult), r.adults());
        assertEquals(List.of(child), r.children());
        assertThrows(UnsupportedOperationException.class, () -> r.adults().add(adult));
        assertThrows(UnsupportedOperationException.class, () -> r.children().add(child));
    }

    @Test void additionalCoversExposeSelectedBenefits() {
        var covers = new AdditionalCoversRequest(true, false, null);
        assertTrue(covers.ptdBenefitBase());
        assertFalse(covers.ppdBenefitBase());
        assertNull(covers.ttdBenefitBase());
        assertEquals(new AdditionalCoversRequest(true, false, null), covers);
    }
}
