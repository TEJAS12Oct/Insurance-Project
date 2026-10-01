package com.boltpe.insurance.client;

import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;

import java.util.List;

public record PersonalAccidentQuoteBatch(
        List<PersonalAccidentVendorResponse> quotes,
        List<ApiStatus> apiStatuses
) {

    public PersonalAccidentQuoteBatch {
        quotes = quotes == null
                ? List.of()
                : List.copyOf(quotes);

        apiStatuses = apiStatuses == null
                ? List.of()
                : List.copyOf(apiStatuses);
    }

    public record ApiStatus(
            String apiUrl,
            boolean active,
            String companyId,
            String planId,
            String reason
    ) {
    }
}