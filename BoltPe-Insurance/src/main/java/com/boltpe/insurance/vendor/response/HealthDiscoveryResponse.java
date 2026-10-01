package com.boltpe.insurance.vendor.response;

import java.util.List;

public record HealthDiscoveryResponse(
        boolean success,
        int count,
        String policy_filter,
        List<HealthPlanApiDescriptor> data) {

    public HealthDiscoveryResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }
}
