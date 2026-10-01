package com.boltpe.insurance.vendor.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthVendorRequest(
        Long coverAmount,
        String zone,
        Integer age,
        Integer sage,
        @JsonProperty("c1age") Integer child1Age,
        @JsonProperty("c2age") Integer child2Age,
        @JsonProperty("c3age") Integer child3Age,
        @JsonProperty("c4age") Integer child4Age) {
}
