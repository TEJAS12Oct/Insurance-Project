package com.boltpe.insurance.client;

import com.boltpe.insurance.cache.InsuranceApiCacheService;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.refresh.VendorApiRecord;
import com.boltpe.insurance.vendor.response.*;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class CachedPolicyPlannerHttpGateway extends PolicyPlannerHttpGateway {
    private final InsuranceApiCacheService cacheService;

    public CachedPolicyPlannerHttpGateway(RestClient policyPlannerRestClient, InsuranceApiCacheService cacheService) {
        super(policyPlannerRestClient); this.cacheService = cacheService;
    }

    @Override public HealthDiscoveryResponse discoverHealthPlans(String ignoredPath) {
        List<VendorApiRecord> rows = required(InsuranceType.HEALTH);
        return new HealthDiscoveryResponse(true, rows.size(), "Health", rows.stream().map(r -> new HealthPlanApiDescriptor(r.apiType())).toList());
    }
    @Override public SuperTopUpDiscoveryResponse discoverSuperTopUpPlans(String ignoredPath) {
        List<VendorApiRecord> rows = required(InsuranceType.SUPER_TOP_UP);
        return new SuperTopUpDiscoveryResponse(true, rows.size(), "super_top_up", rows.stream().map(r -> new SuperTopUpPlanApiDescriptor(r.apiType())).toList());
    }
    @Override public PersonalAccidentDiscoveryResponse discoverPersonalAccidentPlans(String ignoredPath) {
        List<VendorApiRecord> rows = required(InsuranceType.PERSONAL_ACCIDENT);
        return new PersonalAccidentDiscoveryResponse(true, rows.size(), "pa", rows.stream().map(r -> new PersonalAccidentPlanApiDescriptor(r.apiType())).toList());
    }
    @Override public HospitalCashDiscoveryResponse discoverHospitalCashPlans(String ignoredPath) {
        List<VendorApiRecord> rows = required(InsuranceType.HOSPITAL_CASH);
        return new HospitalCashDiscoveryResponse(true, rows.size(), "hc", rows.stream().map(r -> new HospitalCashPlanApiDescriptor(r.apiType())).toList());
    }
    private List<VendorApiRecord> required(InsuranceType type) {
        List<VendorApiRecord> rows = cacheService.getOrLoad(type);
        if (rows.isEmpty()) throw new PolicyPlannerException("No cached or database vendor API configuration available for " + type);
        return rows;
    }
}
