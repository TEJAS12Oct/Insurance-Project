package com.boltpe.insurance.dto.request.proposalform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record GroupProposalRequest(
        @NotBlank(message = "productName is required") String productName,
        @NotBlank(message = "productType is required") String productType,
        @NotBlank(message = "pincode is required") String pincode,
        @NotBlank(message = "cityState is required") String cityState,
        @NotBlank(message = "zone is required") String zone,
        @NotBlank(message = "upgradeZone is required") String upgradeZone,
        @NotNull(message = "adultCount is required") Integer adultCount,
        @NotNull(message = "childCount is required") Integer childCount,
        @NotBlank(message = "proposerName is required") String proposerName,
        @NotBlank(message = "proposerDOB is required") String proposerDOB,
        @NotBlank(message = "proposerGender is required") String proposerGender,
        @NotBlank(message = "proposerEmail is required") String proposerEmail,
        @NotBlank(message = "proposerPhone is required") String proposerPhone,
        @NotNull(message = "sumInsured is required") BigDecimal sumInsured,
        @NotNull(message = "selectedTenure is required") BigDecimal selectedTenure,
        @NotBlank(message = "annualIncome is required") String annualIncome,
        @NotNull(message = "nriDiscount is required") String nriDiscount,
        @NotNull(message = "includeSelf is required") String includeSelf,
        @NotNull(message = "bureauDiscount is required") String bureauDiscount,
        @NotBlank(message = "aadharNumber is required") String aadharNumber,
        @NotBlank(message = "panNumber is required") String panNumber,
        @NotNull(message = "basePremium is required") BigDecimal basePremium,
        @NotNull(message = "totalPremium is required") BigDecimal totalPremium,
        List<@Valid AdultMemberRequest> adults,
        List<@Valid ChildMemberRequest> children
) {
    public GroupProposalRequest {
        adults = adults == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(adults));
        children = children == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(children));
    }
}
