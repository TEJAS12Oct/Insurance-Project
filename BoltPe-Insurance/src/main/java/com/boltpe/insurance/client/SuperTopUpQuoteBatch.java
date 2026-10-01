package com.boltpe.insurance.client;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import java.util.List;
public record SuperTopUpQuoteBatch(List<SuperTopUpVendorResponse> quotes, List<ApiStatus> apiStatuses) {
    public record ApiStatus(String apiUrl, boolean active, String companyId, String planId, String failureReason) {}
}
