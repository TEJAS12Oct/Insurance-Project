package com.boltpe.insurance.dto.request.proposalform;

import java.math.BigDecimal;

public record AdultMemberRequest(
        String relationship,
        String title,
        String fullName,
        String dob,
        BigDecimal height,
        BigDecimal weight,
        String abhaId,
        String preExistingDisease,
        String medicalCondition
) {
}
