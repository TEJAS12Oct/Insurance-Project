package com.boltpe.insurance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "policy-planner")
public record PolicyPlannerProperties(
        String baseUrl,
        Health health,
        SuperTopUp superTopUp,
        PersonalAccident personalAccident,
        HospitalCash hospitalCash,
        int connectTimeoutMs,
        int readTimeoutMs
) {

    public record Health(String discoveryPath) {
    }

    public record SuperTopUp(String discoveryPath) {
    }

    public record PersonalAccident(String discoveryPath) {
    }

    public record HospitalCash(String discoveryPath) {
    }
}