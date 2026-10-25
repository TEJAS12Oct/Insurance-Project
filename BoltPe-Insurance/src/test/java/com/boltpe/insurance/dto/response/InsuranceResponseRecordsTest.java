package com.boltpe.insurance.dto.response;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceResponseRecordsTest {
    @Test void personalAccidentMemberHoldsRelationAndAge() {
        var member = new PersonalAccidentInsuranceResponse.Member("SELF", 30);
        assertEquals("SELF", member.relation());
        assertEquals(30, member.age());
        assertEquals(new PersonalAccidentInsuranceResponse.Member("SELF", 30), member);
    }

    @Test void hospitalCashMemberHoldsRelationAndAge() {
        var member = new HospitalCashInsuranceResponse.Member("SPOUSE", 28);
        assertEquals("SPOUSE", member.relation());
        assertEquals(28, member.age());
    }

    @Test void hospitalCashPremiumOptionHoldsAllAmounts() {
        var option = new HospitalCashInsuranceResponse.PremiumOption(1, new BigDecimal("2000"), BigDecimal.ZERO,
                new BigDecimal("2000"), null, new BigDecimal("2000"), "basic", new BigDecimal("2000"));
        assertEquals(1, option.years());
        assertEquals(new BigDecimal("2000"), option.payablePremium());
        assertEquals(BigDecimal.ZERO, option.discount());
        assertNull(option.deductible());
        assertEquals("basic", option.type());
    }
}
