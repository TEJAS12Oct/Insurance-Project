package com.boltpe.insurance.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

@SuppressWarnings("unused") // Bean Validation invokes @AssertTrue methods reflectively.
public record PersonalAccidentInsuranceRequest(
        @NotNull(message = "Kindly select cover amount") Long coverAmount,
        @NotNull(message = "Kindly select category") Integer category,
        @NotNull(message = "Kindly select age") Integer age) {

    // Personal Accident cover amounts available in the Policy Planner/BoltPe dropdown:
    // 10L, 15L, 20L, 25L, 30L, 35L, 50L, 75L, 1Cr, 2Cr, 3Cr, 4Cr, 5Cr.
    public static final Set<Long> ALLOWED_COVER_AMOUNTS = Set.of(
            1_000_000L, 1_500_000L, 2_000_000L, 2_500_000L,
            3_000_000L, 3_500_000L, 5_000_000L, 7_500_000L,
            10_000_000L, 20_000_000L, 30_000_000L, 40_000_000L, 50_000_000L);


    @AssertTrue(message = "category must be one of: 1, 2, 3")
    public boolean isCategorySupported() {
        return category == null || category == 1 || category == 2 || category == 3;
    }

    @AssertTrue(message = "Kindly select at least 1 adult")
    public boolean isMinimumAdultValid() {
        return age == null || age >= 18;
    }

    @AssertTrue(message = "Invalid cover amount. Please select a cover amount from the available Personal Accident coverage options.")
    public boolean isCoverAmountSupported() {
        return coverAmount == null || ALLOWED_COVER_AMOUNTS.contains(coverAmount);
    }
}
