package com.boltpe.insurance.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record SuperTopUpInsuranceRequest(
        @NotNull(message = "Kindly select cover amount") Long coverAmount,
        @NotNull(message = "Kindly select age for 1 adult") Integer age,
        Integer spouseAge,
        Integer child1Age,
        Integer child2Age,
        Integer child3Age,
        Integer child4Age) {

    // Fixed Super Top-Up cover amounts from the product dropdown:
    // 5L, 10L, 15L, 20L, 25L, 50L, 1Cr, 3Cr.
    public static final Set<Long> ALLOWED_COVER_AMOUNTS = Set.of(
            500_000L, 1_000_000L, 1_500_000L, 2_000_000L,
            2_500_000L, 5_000_000L, 10_000_000L, 30_000_000L);

    @AssertTrue(message = "Kindly select at least 1 adult")
    public boolean isMinimumAdultValid() {
        return age == null || age >= 18;
    }

    @AssertTrue(message = "Invalid cover amount. Please select a cover amount from the available coverage options.")
    public boolean isCoverAmountSupported() {
        return coverAmount == null || ALLOWED_COVER_AMOUNTS.contains(coverAmount);
    }
}
