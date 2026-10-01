package com.boltpe.insurance.vendor.response;
import java.util.List;
public record SuperTopUpDiscoveryResponse(boolean success, int count, String policy_filter, List<SuperTopUpPlanApiDescriptor> data) {
    public SuperTopUpDiscoveryResponse { data = data == null ? List.of() : List.copyOf(data); }
}
