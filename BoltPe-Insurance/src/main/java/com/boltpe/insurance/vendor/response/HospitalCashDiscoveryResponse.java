package com.boltpe.insurance.vendor.response;
import java.util.List;
public record HospitalCashDiscoveryResponse(boolean success, Integer count, String policy_filter, List<HospitalCashPlanApiDescriptor> data) {
    public HospitalCashDiscoveryResponse { data = data == null ? List.of() : List.copyOf(data); }
}
