package com.boltpe.insurance.client;

import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import java.util.List;

public record HealthQuoteBatch(List<HealthVendorResponse> quotes, List<ApiStatus> apiStatuses) {
    public record ApiStatus(String apiUrl, boolean active, String companyId, String planId, String failureReason) {}
}
