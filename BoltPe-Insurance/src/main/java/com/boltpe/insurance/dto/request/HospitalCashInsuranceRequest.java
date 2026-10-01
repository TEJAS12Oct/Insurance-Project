package com.boltpe.insurance.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record HospitalCashInsuranceRequest(
        @NotNull(message = "Kindly select age") Integer age,
        @NotNull(message = "Kindly select cover amount") Long coverAmount,
        @NotNull(message = "Kindly select number of adults") Integer noOfAdults,
        @NotNull(message = "Kindly select number of children") Integer noOfChildren,
        @NotNull(message = "Kindly select duration") Integer noOfDays) {

    public static final Set<Long> ALLOWED_COVER_AMOUNTS = Set.of(500L, 1000L, 2000L, 2500L, 3000L, 5000L);
    public static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60, 90, 180);

    @AssertTrue(message = "Kindly select a valid age")
    public boolean isAgeValid() { return age == null || age >= 18; }

    @AssertTrue(message = "Kindly select at least 1 adult")
    public boolean isMinimumAdultValid() { return noOfAdults == null || noOfAdults >= 1; }

    @AssertTrue(message = "Invalid number of children")
    public boolean isChildrenCountValid() { return noOfChildren == null || noOfChildren >= 0; }

    @AssertTrue(message = "Invalid Room Rent. Please select a Room Rent from the available Room Rent options.")
    public boolean isCoverAmountSupported() { return coverAmount == null || ALLOWED_COVER_AMOUNTS.contains(coverAmount); }

    @AssertTrue(message = "Invalid duration. Please select 30, 60, 90 or 180 days.")
    public boolean isDurationSupported() { return noOfDays == null || ALLOWED_DURATIONS.contains(noOfDays); }
}
