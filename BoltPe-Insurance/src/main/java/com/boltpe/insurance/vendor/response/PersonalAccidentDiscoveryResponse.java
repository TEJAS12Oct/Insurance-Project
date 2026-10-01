package com.boltpe.insurance.vendor.response;
import java.util.List;
public record PersonalAccidentDiscoveryResponse(boolean success, Integer count, String policy_filter, List<PersonalAccidentPlanApiDescriptor> data) {
    public PersonalAccidentDiscoveryResponse { data = data == null ? List.of() : List.copyOf(data); }
}
