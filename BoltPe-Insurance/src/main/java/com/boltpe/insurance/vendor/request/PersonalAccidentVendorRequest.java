package com.boltpe.insurance.vendor.request;

public record PersonalAccidentVendorRequest(
        Long coverAmount,
        Integer category,
        Integer age
) {}