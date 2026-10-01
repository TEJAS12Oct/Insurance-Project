package com.boltpe.insurance.client;

import com.boltpe.insurance.config.PolicyPlannerProperties;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.refresh.DiscoverySnapshot;
import com.boltpe.insurance.refresh.VendorApiRecord;
import com.boltpe.insurance.vendor.response.*;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LivePolicyPlannerDiscoveryClient {
    private final RestClient restClient;
    private final PolicyPlannerProperties properties;
    private final JsonMapper jsonMapper;

    public LivePolicyPlannerDiscoveryClient(RestClient policyPlannerRestClient, PolicyPlannerProperties properties, JsonMapper jsonMapper) {
        this.restClient = policyPlannerRestClient; this.properties = properties; this.jsonMapper = jsonMapper;
    }

    public DiscoverySnapshot discover(InsuranceType type) {
        return switch (type) {
            case HEALTH -> health();
            case SUPER_TOP_UP -> superTopUp();
            case PERSONAL_ACCIDENT -> personalAccident();
            case HOSPITAL_CASH -> hospitalCash();
        };
    }

    private DiscoverySnapshot health() {
        HealthDiscoveryResponse r = get(properties.health().discoveryPath(), HealthDiscoveryResponse.class);
        requireValid(r != null && r.success(), r == null ? null : r.data(), "Health");
        return snapshot(r, r.data().stream().map(x -> record(x.api_type(), "premium")).toList());
    }
    private DiscoverySnapshot superTopUp() {
        SuperTopUpDiscoveryResponse r = get(properties.superTopUp().discoveryPath(), SuperTopUpDiscoveryResponse.class);
        requireValid(r != null && r.success(), r == null ? null : r.data(), "Super Top-Up");
        return snapshot(r, r.data().stream().map(x -> record(x.api_type(), "supertopup")).toList());
    }
    private DiscoverySnapshot personalAccident() {
        PersonalAccidentDiscoveryResponse r = get(properties.personalAccident().discoveryPath(), PersonalAccidentDiscoveryResponse.class);
        requireValid(r != null && r.success(), r == null ? null : r.data(), "Personal Accident");
        return snapshot(r, r.data().stream().map(x -> record(x.api_type(), "pa")).toList());
    }
    private DiscoverySnapshot hospitalCash() {
        HospitalCashDiscoveryResponse r = get(properties.hospitalCash().discoveryPath(), HospitalCashDiscoveryResponse.class);
        requireValid(r != null && r.success(), r == null ? null : r.data(), "Hospital Cash");
        List<VendorApiRecord> records = r.data().stream().map(x -> recordHospitalCash(x.api_type())).toList();
        return snapshot(r, records);
    }

    private <T> T get(String path, Class<T> type) {
        try { return restClient.get().uri(path).retrieve().body(type); }
        catch (RuntimeException ex) { throw new PolicyPlannerException("Policy Planner discovery request failed", ex); }
    }
    private void requireValid(boolean success, List<?> data, String product) {
        if (!success || data == null || data.isEmpty()) throw new PolicyPlannerException("Policy Planner returned no valid " + product + " discovery APIs");
    }
    private DiscoverySnapshot snapshot(Object response, List<VendorApiRecord> records) {
        List<VendorApiRecord> valid = records.stream().filter(r -> r.apiType() != null && !r.apiType().isBlank()).toList();
        if (valid.isEmpty()) throw new PolicyPlannerException("Policy Planner discovery contained no usable API URLs");
        return new DiscoverySnapshot(valid, jsonMapper.writeValueAsString(response));
    }
    private static VendorApiRecord record(String url, String marker) {
        String[] ids = ids(url, marker); return new VendorApiRecord(url, ids[0], ids[1]);
    }
    private static VendorApiRecord recordHospitalCash(String url) {
        String[] ids = ids(url, "hc");
        if (ids[0] == null) ids = ids(url, "hospitalcash");
        if (ids[0] == null) ids = ids(url, "hospital-cash");
        return new VendorApiRecord(url, ids[0], ids[1]);
    }
    private static String[] ids(String url, String marker) {
        try {
            String[] p = URI.create(url).getPath().split("/");
            for (int i=0;i<p.length;i++) if (marker.equalsIgnoreCase(p[i]) && i>=2) return new String[]{p[i-2],p[i-1]};
        } catch (RuntimeException ignored) { }
        return new String[]{null,null};
    }
}
