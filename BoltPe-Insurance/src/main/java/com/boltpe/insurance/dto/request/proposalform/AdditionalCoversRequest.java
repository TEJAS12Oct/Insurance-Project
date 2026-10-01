package com.boltpe.insurance.dto.request.proposalform;

public record AdditionalCoversRequest(
        Boolean ptdBenefitBase,
        Boolean ppdBenefitBase,
        Boolean ttdBenefitBase
) {
}
