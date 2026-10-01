package com.boltpe.insurance.dto.request.proposalform;

import java.math.BigDecimal;

public record ChildMemberRequest(
        String relationship,
        String fullName,
        String dob,
        BigDecimal height,
        BigDecimal weight,
        String medicalCondition
) {
}
