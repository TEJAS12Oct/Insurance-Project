package com.boltpe.insurance.refresh;

import java.util.List;

public record DiscoverySnapshot(List<VendorApiRecord> records, String responseJson) {
    public DiscoverySnapshot {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
