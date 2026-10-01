package com.boltpe.insurance.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;

public record HealthInsuranceRequest(
        @NotNull(message = "Kindly select cover amount") Long coverAmount,
        @NotNull @Pattern(regexp = "^[123]$", message = "Invalid zone. Health Insurance supports only Zone 1, Zone 2 and Zone 3.") String zone,
        @NotNull(message = "Kindly select age for 1 adult") Integer age,
        Integer spouseAge,
        Integer child1Age,
        Integer child2Age,
        Integer child3Age,
        Integer child4Age) {

    public static final Set<Long> ALLOWED_COVER_AMOUNTS = Set.of(
            50_000L, 75_000L, 500_000L, 700_000L, 1_000_000L,
            1_500_000L, 2_000_000L, 2_500_000L, 3_000_000L, 3_500_000L,
            4_000_000L, 4_500_000L, 5_000_000L, 7_500_000L, 10_000_000L,
            20_000_000L, 30_000_000L, 40_000_000L, 50_000_000L);

    @AssertTrue(message = "Kindly select at least 1 adult")
    public boolean isMinimumAdultValid() {
        return age == null || age >= 18;
    }

    @AssertTrue(message = "Invalid cover amount. Please select a cover amount from the available coverage options.")
    public boolean isCoverAmountSupported() {
        return coverAmount == null || ALLOWED_COVER_AMOUNTS.contains(coverAmount);
    }
}
