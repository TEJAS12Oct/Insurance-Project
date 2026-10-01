package com.boltpe.insurance.client;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import java.util.List;
public record HospitalCashQuoteBatch(List<HospitalCashVendorResponse> quotes, List<ApiStatus> apiStatuses) {
    public HospitalCashQuoteBatch { quotes=quotes==null?List.of():List.copyOf(quotes); apiStatuses=apiStatuses==null?List.of():List.copyOf(apiStatuses); }
    public record ApiStatus(String apiUrl, boolean active, String companyId, String planId, String reason) {}
}
