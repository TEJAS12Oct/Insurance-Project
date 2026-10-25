package com.boltpe.insurance.service;

import com.boltpe.insurance.audit.InsuranceAuditPersistenceService;
import com.boltpe.insurance.client.HealthQuoteBatch;
import com.boltpe.insurance.client.SuperTopUpQuoteBatch;
import com.boltpe.insurance.client.PersonalAccidentQuoteBatch;
import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.dto.request.HealthInsuranceRequest;
import com.boltpe.insurance.dto.request.InsuranceRequest;
import com.boltpe.insurance.dto.request.SuperTopUpInsuranceRequest;
import com.boltpe.insurance.dto.request.PersonalAccidentInsuranceRequest;
import com.boltpe.insurance.dto.request.HospitalCashInsuranceRequest;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.ApiError;
import com.boltpe.insurance.exception.BoltPeInsuranceException;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.exception.UnsupportedInsuranceTypeException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InsuranceServiceImpl implements InsuranceService {
    private static final String VENDOR_UNAVAILABLE_MESSAGE = "Policy Planner service is temporarily unavailable. Please try again later.";
    private static final String SERVICE_UNAVAILABLE_LABEL = "Service Unavailable";

    private final HealthInsuranceService healthService;
    private final InsuranceAuditPersistenceService auditService;
    private final JsonMapper jsonMapper;
    private final SuperTopUpInsuranceService superTopUpService;
    private final PersonalAccidentInsuranceService personalAccidentService;
    private final HospitalCashInsuranceService hospitalCashService;

    @Autowired
    public InsuranceServiceImpl(HealthInsuranceService healthService,
                                InsuranceAuditPersistenceService auditService, JsonMapper jsonMapper, SuperTopUpInsuranceService superTopUpService,
                                PersonalAccidentInsuranceService personalAccidentService, HospitalCashInsuranceService hospitalCashService) {
        this.healthService = healthService;
        this.auditService = auditService;
        this.jsonMapper = jsonMapper;
        this.superTopUpService = superTopUpService;
        this.personalAccidentService = personalAccidentService;
        this.hospitalCashService = hospitalCashService;
    }

    /** Convenience constructor used by focused service unit tests. */
    InsuranceServiceImpl(HealthInsuranceService healthService,
                         InsuranceAuditPersistenceService auditService, JsonMapper jsonMapper) {
        this(healthService, auditService, jsonMapper, null, null, null);
    }

    @Override
    public Object getQuotes(InsuranceRequest request) {
        if (request.insuranceType() == InsuranceType.SUPER_TOP_UP) {
            if (superTopUpService == null) throw new UnsupportedInsuranceTypeException("SUPER_TOP_UP service is not configured");
            return getSuperTopUp(request);
        }
        if (request.insuranceType() == InsuranceType.PERSONAL_ACCIDENT) {
            if (personalAccidentService == null) throw new UnsupportedInsuranceTypeException("PERSONAL_ACCIDENT service is not configured");
            return getPersonalAccident(request);
        }
        if (request.insuranceType() == InsuranceType.HOSPITAL_CASH) {
            if (hospitalCashService == null) throw new UnsupportedInsuranceTypeException("HOSPITAL_CASH service is not configured");
            return getHospitalCash(request);
        }
        if (request.insuranceType() != InsuranceType.HEALTH) {
            throw new UnsupportedInsuranceTypeException("Insurance type is not implemented in this release");
        }
        HealthInsuranceRequest health = jsonMapper.convertValue(request.data(), HealthInsuranceRequest.class);
        // Generate before the vendor call so success and failure responses use the same audit/request ID.
        String requestId = UUID.randomUUID().toString();
        String requestJson = toJson(request);
        String userJson = toJson(request.userInformation());

        HealthQuoteBatch batch;
        try {
            batch = healthService.fetch(health);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (PolicyPlannerException ex) {
            ApiError error = new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    VENDOR_UNAVAILABLE_MESSAGE,
                    requestId, Map.of());
            saveFailureAudit(requestId, request, userJson, requestJson, "[]", "[]", "[]", error);
            throw new PolicyPlannerException(error, ex);
        } catch (RuntimeException ex) {
            ApiError error = internalError(requestId);
            saveFailureAudit(requestId, request, userJson, requestJson, "[]", "[]", "[]", error);
            throw new BoltPeInsuranceException(error, ex);
        }

        // Discovery succeeded, but every discovered premium API failed. Preserve the
        // individual failures in all_api_false before returning a vendor-unavailable response.
        if (batch.quotes().isEmpty()) {
            ApiError error = new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                    HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    VENDOR_UNAVAILABLE_MESSAGE,
                    requestId, Map.of());
            saveFailureAudit(
                    requestId, request, userJson, requestJson,
                    toJsonSafely(batch.apiStatuses().stream().filter(HealthQuoteBatch.ApiStatus::active).toList()),
                    toJsonSafely(batch.apiStatuses().stream().filter(status -> !status.active()).toList()),
                    toJsonSafely(batch.quotes()), error);
            throw new PolicyPlannerException(error, new PolicyPlannerException(
                    "No Policy Planner health provider returned a quote"));
        }

        try {
            Object response = healthService.build(requestId, health, batch);

            auditService.save(
                    requestId,
                    request.insuranceType().name(),
                    userJson,
                    requestJson,
                    toJson(batch.apiStatuses().stream().filter(HealthQuoteBatch.ApiStatus::active).toList()),
                    toJson(batch.apiStatuses().stream().filter(status -> !status.active()).toList()),
                    toJson(batch.quotes()),
                    toJson(response));
            return response;
        } catch (RuntimeException ex) {
            ApiError error = internalError(requestId);
            saveFailureAudit(
                    requestId, request, userJson, requestJson,
                    toJsonSafely(batch.apiStatuses().stream().filter(HealthQuoteBatch.ApiStatus::active).toList()),
                    toJsonSafely(batch.apiStatuses().stream().filter(status -> !status.active()).toList()),
                    toJsonSafely(batch.quotes()), error);
            throw new BoltPeInsuranceException(error, ex);
        }
    }

    private Object getSuperTopUp(InsuranceRequest request) {
        SuperTopUpInsuranceRequest topUp = jsonMapper.convertValue(request.data(), SuperTopUpInsuranceRequest.class);
        String requestId = UUID.randomUUID().toString(); String requestJson=toJson(request); String userJson=toJson(request.userInformation());
        SuperTopUpQuoteBatch batch;
        try { batch=superTopUpService.fetch(topUp); }
        catch (RuntimeException ex) {
            ApiError error = new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    VENDOR_UNAVAILABLE_MESSAGE, requestId, Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,"[]","[]","[]",error);
            throw new PolicyPlannerException(error,ex);
        }
        if(batch.quotes().isEmpty()) {
            ApiError error = new ApiError(Instant.now(),503,SERVICE_UNAVAILABLE_LABEL,VENDOR_UNAVAILABLE_MESSAGE,requestId,Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,toJsonSafely(batch.apiStatuses().stream().filter(SuperTopUpQuoteBatch.ApiStatus::active).toList()),
                    toJsonSafely(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJsonSafely(batch.quotes()),error);
            throw new PolicyPlannerException(error,new PolicyPlannerException("No Policy Planner Super Top-Up provider returned a quote"));
        }
        Object response=superTopUpService.build(requestId,topUp,batch);
        auditService.save(requestId,request.insuranceType().name(),userJson,requestJson,
                toJson(batch.apiStatuses().stream().filter(SuperTopUpQuoteBatch.ApiStatus::active).toList()),
                toJson(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJson(batch.quotes()),toJson(response));
        return response;
    }


    private Object getPersonalAccident(InsuranceRequest request) {
        PersonalAccidentInsuranceRequest pa = jsonMapper.convertValue(request.data(), PersonalAccidentInsuranceRequest.class);
        String requestId = UUID.randomUUID().toString(); String requestJson=toJson(request); String userJson=toJson(request.userInformation());
        PersonalAccidentQuoteBatch batch;
        // Personal Accident validation errors must remain HTTP 400 and must not be
        // converted into a Policy Planner 503 response. Vendor/service failures are 503.
        try { batch=personalAccidentService.fetch(pa); }
        catch (IllegalArgumentException ex) { throw ex; }
        catch (RuntimeException ex) {
            ApiError error = new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(), HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                    VENDOR_UNAVAILABLE_MESSAGE, requestId, Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,"[]","[]","[]",error);
            throw new PolicyPlannerException(error,ex);
        }
        if(batch.quotes().isEmpty()) {
            ApiError error = new ApiError(Instant.now(),503,SERVICE_UNAVAILABLE_LABEL,VENDOR_UNAVAILABLE_MESSAGE,requestId,Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,toJsonSafely(batch.apiStatuses().stream().filter(PersonalAccidentQuoteBatch.ApiStatus::active).toList()),
                    toJsonSafely(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJsonSafely(batch.quotes()),error);
            throw new PolicyPlannerException(error,new PolicyPlannerException("No Policy Planner Personal Accident provider returned a quote"));
        }
        Object response=personalAccidentService.build(requestId,batch);
        auditService.save(requestId,request.insuranceType().name(),userJson,requestJson,
                toJson(batch.apiStatuses().stream().filter(PersonalAccidentQuoteBatch.ApiStatus::active).toList()),
                toJson(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJson(batch.quotes()),toJson(response));
        return response;
    }

    private Object getHospitalCash(InsuranceRequest request) {
        HospitalCashInsuranceRequest hc=jsonMapper.convertValue(request.data(),HospitalCashInsuranceRequest.class);
        String requestId=UUID.randomUUID().toString(); String requestJson=toJson(request); String userJson=toJson(request.userInformation());
        HospitalCashQuoteBatch batch;
        try { batch=hospitalCashService.fetch(hc); }
        catch(IllegalArgumentException ex){ throw ex; }
        catch(RuntimeException ex){
            ApiError error=new ApiError(Instant.now(),503,SERVICE_UNAVAILABLE_LABEL,VENDOR_UNAVAILABLE_MESSAGE,requestId,Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,"[]","[]","[]",error);
            throw new PolicyPlannerException(error,ex);
        }
        if(batch.quotes().isEmpty()){
            ApiError error=new ApiError(Instant.now(),503,SERVICE_UNAVAILABLE_LABEL,VENDOR_UNAVAILABLE_MESSAGE,requestId,Map.of());
            saveFailureAudit(requestId,request,userJson,requestJson,toJsonSafely(batch.apiStatuses().stream().filter(HospitalCashQuoteBatch.ApiStatus::active).toList()),toJsonSafely(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJsonSafely(batch.quotes()),error);
            throw new PolicyPlannerException(error,new PolicyPlannerException("No Policy Planner Hospital Cash provider returned a quote"));
        }
        Object response=hospitalCashService.build(requestId,hc,batch);
        auditService.save(requestId,request.insuranceType().name(),userJson,requestJson,toJson(batch.apiStatuses().stream().filter(HospitalCashQuoteBatch.ApiStatus::active).toList()),toJson(batch.apiStatuses().stream().filter(x->!x.active()).toList()),toJson(batch.quotes()),toJson(response));
        return response;
    }

    private void saveFailureAudit(String requestId, InsuranceRequest request, String userJson, String requestJson,
                                  String allApiTrue, String allApiFalse, String vendorResponse, ApiError error) {
        auditService.save(requestId, request.insuranceType().name(), userJson, requestJson,
                allApiTrue, allApiFalse, vendorResponse, toJsonSafely(error));
    }

    private ApiError internalError(String requestId) {
        return new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "BoltPe Insurance service encountered an unexpected error. Please try again later.",
                requestId, Map.of());
    }

    private String toJson(Object value) {
        try { return jsonMapper.writeValueAsString(value); }
        catch (JacksonException ex) { throw new PolicyPlannerException("Unable to serialize insurance request snapshot", ex); }
    }

    private String toJsonSafely(Object value) {
        try { return jsonMapper.writeValueAsString(value); }
        catch (RuntimeException ex) { return "{}"; }
    }

}
