package com.boltpe.insurance.dto.request.proposalform;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PersonalAccidentProposalRequest(
        @NotBlank(message = "productName is required") String productName,
        @NotBlank(message = "productType is required") String productType,
        @NotBlank(message = "pincode is required") String pincode,
        @NotBlank(message = "city is required") String city,
        @NotBlank(message = "proposerName is required") String proposerName,
        @NotBlank(message = "gender is required") String gender,
        @NotBlank(message = "dateOfBirth is required") String dateOfBirth,
        @NotBlank(message = "mobileNumber is required") String mobileNumber,
        String email,
        @JsonProperty("aadhar_number") String aadharNumber,
        @JsonProperty("pan_number") String panNumber,
        String occupationOfInsured,
        String annualIncome,
        @NotBlank(message = "natureOfWorkDesignation is required") String natureOfWorkDesignation,
        @NotNull(message = "coverAmount is required") BigDecimal coverAmount,
        @Valid AdditionalCoversRequest additionalCovers
) {
}
