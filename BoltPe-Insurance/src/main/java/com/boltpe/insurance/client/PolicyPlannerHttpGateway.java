package com.boltpe.insurance.client;

import com.boltpe.insurance.vendor.request.HealthVendorRequest;
import com.boltpe.insurance.vendor.request.SuperTopUpVendorRequest;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HealthDiscoveryResponse;
import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import com.boltpe.insurance.vendor.response.SuperTopUpDiscoveryResponse;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentDiscoveryResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import com.boltpe.insurance.vendor.response.HospitalCashDiscoveryResponse;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PolicyPlannerHttpGateway {
    private final RestClient restClient;

    public PolicyPlannerHttpGateway(RestClient policyPlannerRestClient) {
        this.restClient = policyPlannerRestClient;
    }

    public HealthDiscoveryResponse discoverHealthPlans(String path) {
        return restClient.get()
                .uri(path)
                .retrieve()
                .body(HealthDiscoveryResponse.class);
    }

    public HealthVendorResponse fetchHealthQuote(String path, HealthVendorRequest request) {
        return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(HealthVendorResponse.class);
    }
    public SuperTopUpDiscoveryResponse discoverSuperTopUpPlans(String path) {
        return restClient.get().uri(path).retrieve().body(SuperTopUpDiscoveryResponse.class);
    }

    public SuperTopUpVendorResponse fetchSuperTopUpQuote(String path, SuperTopUpVendorRequest request) {
        return restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(SuperTopUpVendorResponse.class);
    }

    public PersonalAccidentDiscoveryResponse discoverPersonalAccidentPlans(String path) {
        return restClient.get().uri(path).retrieve().body(PersonalAccidentDiscoveryResponse.class);
    }

    public PersonalAccidentVendorResponse fetchPersonalAccidentQuote(String path, PersonalAccidentVendorRequest request) {
        return restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(PersonalAccidentVendorResponse.class);
    }

    public HospitalCashDiscoveryResponse discoverHospitalCashPlans(String path) {
        return restClient.get().uri(path).retrieve().body(HospitalCashDiscoveryResponse.class);
    }

    public HospitalCashVendorResponse fetchHospitalCashQuote(String path, HospitalCashVendorRequest request) {
        return restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(request).retrieve().body(HospitalCashVendorResponse.class);
    }

}
