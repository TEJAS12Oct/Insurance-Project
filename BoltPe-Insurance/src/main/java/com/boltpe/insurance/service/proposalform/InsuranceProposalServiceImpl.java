package com.boltpe.insurance.service.proposalform;

import com.boltpe.insurance.audit.proposalform.InsuranceProposalAuditService;
import com.boltpe.insurance.client.proposalform.PolicyPlannerProposalCallResult;
import com.boltpe.insurance.client.proposalform.PolicyPlannerProposalClient;
import com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest;
import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.dto.request.proposalform.PersonalAccidentProposalRequest;
import com.boltpe.insurance.dto.response.proposalform.InsuranceProposalResponse;
import com.boltpe.insurance.dto.response.proposalform.ProposalResponseData;
import com.boltpe.insurance.exception.ApiError;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.validation.proposalform.ProposalFormAgeValidator;
import com.boltpe.insurance.validation.proposalform.ProposalFormAdditionalValidator;
import com.boltpe.insurance.validation.proposalform.ProposalFormContactIdentityValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InsuranceProposalServiceImpl implements InsuranceProposalService {
    private static final String BOLTPE_CLIENT_NAME = "bolt_pay";

    private final GroupProposalService groupProposalService;
    private final PersonalAccidentProposalService personalAccidentProposalService;
    private final PolicyPlannerProposalClient client;
    private final InsuranceProposalAuditService auditService;
    private final JsonMapper jsonMapper;
    private final Validator validator;
    private final ProposalFormAgeValidator proposalFormAgeValidator;
    private final ProposalFormAdditionalValidator proposalFormAdditionalValidator;
    private final ProposalFormContactIdentityValidator proposalFormContactIdentityValidator;

    public InsuranceProposalServiceImpl(GroupProposalService groupProposalService,
                                        PersonalAccidentProposalService personalAccidentProposalService,
                                        PolicyPlannerProposalClient client,
                                        InsuranceProposalAuditService auditService,
                                        JsonMapper jsonMapper,
                                        Validator validator,
                                        ProposalFormAgeValidator proposalFormAgeValidator,
                                        ProposalFormContactIdentityValidator proposalFormContactIdentityValidator,
                                        ProposalFormAdditionalValidator proposalFormAdditionalValidator) {
        this.groupProposalService = groupProposalService;
        this.personalAccidentProposalService = personalAccidentProposalService;
        this.client = client;
        this.auditService = auditService;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
        this.proposalFormAgeValidator = proposalFormAgeValidator;
        this.proposalFormContactIdentityValidator = proposalFormContactIdentityValidator;
        this.proposalFormAdditionalValidator = proposalFormAdditionalValidator;
    }

    @Override
    public InsuranceProposalResponse save(InsuranceProposalRequest request) {
        Map<String, Object> data = getData(request);
        String insuranceType = validateAndResolveInsuranceType(request, data);
        Map<String, Object> vendorRequest = buildVendorRequest(request, data, insuranceType);
        String requestId = UUID.randomUUID().toString();

        // BoltPe audit snapshot keeps the request exactly as received, including DD-MM-YYYY dates.
        String boltpeRequestJson = toJson(request.toFlatMap());
        // Policy Planner receives a flat body: client_name + formType + fields from data.
        // insuranceType and the data wrapper are not forwarded. DOB/date values are converted to YYYY-MM-DD.
        String vendorRequestJson = toJson(vendorRequest);

        auditService.received(requestId, insuranceType, request.getFormType(),
                request.getClientName(), boltpeRequestJson, vendorRequestJson);
        auditService.sent(requestId);

        PolicyPlannerProposalCallResult result;
        try {
            result = client.save(vendorRequest);
        } catch (RuntimeException ex) {
            ApiError error = serviceUnavailable(requestId);
            auditService.unknown(requestId, "Policy Planner response was not received", toJsonSafely(error));
            throw new PolicyPlannerException(error, ex);
        }

        if (result.saveConfirmed()) {
            String proposalId = String.valueOf(result.response().proposalId());
            InsuranceProposalResponse response = new InsuranceProposalResponse(
                    true,
                    "Proposal saved successfully",
                    requestId,
                    new ProposalResponseData(proposalId, request.getFormType().toLowerCase(), "SUBMITTED"));
            auditService.success(requestId, result.httpStatus(), proposalId,
                    result.rawBody(), toJson(response));
            return response;
        }

        HttpStatus status = result.httpStatus() >= 400 && result.httpStatus() < 500
                ? HttpStatus.BAD_REQUEST : HttpStatus.SERVICE_UNAVAILABLE;
        String message = status == HttpStatus.BAD_REQUEST
                ? "Proposal validation failed"
                : "Policy Planner service is temporarily unavailable. Please try again later.";
        Map<String, String> fieldErrors = buildFieldErrors(result);
        ApiError error = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, requestId, fieldErrors);
        auditService.failed(requestId, result.httpStatus(), result.rawBody(),
                toJsonSafely(error), result.response() == null ? message : result.response().message());
        throw new PolicyPlannerException(error,
                new IllegalStateException("Policy Planner did not confirm proposal save"));
    }

    private Map<String, Object> buildVendorRequest(InsuranceProposalRequest request,
                                                    Map<String, Object> data,
                                                    String insuranceType) {
        // Validate the proposal-form cover amount using the resolved insurance type before
        // Jackson DTO conversion and before any downstream Policy Planner call.
        proposalFormAdditionalValidator.validateRawCoverAmount(data, insuranceType);

        if ("pa".equalsIgnoreCase(request.getFormType())) {
            PersonalAccidentProposalRequest pa = jsonMapper.convertValue(
                    data, PersonalAccidentProposalRequest.class);
            validateData(pa);
            proposalFormContactIdentityValidator.validate(pa);
            return personalAccidentProposalService.toVendorRequest(request);
        }

        proposalFormAdditionalValidator.validateRawData(data);
        GroupProposalRequest group = jsonMapper.convertValue(
                data, GroupProposalRequest.class);
        validateData(group);
        proposalFormContactIdentityValidator.validate(group);
        proposalFormAdditionalValidator.validate(group);
        proposalFormAgeValidator.validate(group);
        return groupProposalService.toVendorRequest(request);
    }

    private String validateAndResolveInsuranceType(InsuranceProposalRequest request,
                                                   Map<String, Object> data) {
        if (!BOLTPE_CLIENT_NAME.equals(request.getClientName())) {
            throw new IllegalArgumentException("client_name must be bolt_pay");
        }

        String formType = request.getFormType().toLowerCase();
        String insuranceType = switch (formType) {
            case "health" -> "HEALTH";
            case "supertopup" -> "SUPER_TOP_UP";
            case "pa" -> "PERSONAL_ACCIDENT";
            case "hospicash" -> "HOSPITAL_CASH";
            default -> throw new IllegalArgumentException(
                    "Invalid formType. Allowed values: health, supertopup, pa, hospicash");
        };

        Object requestedInsuranceType = request.getFields().get("insuranceType");
        if (!(requestedInsuranceType instanceof String requestedType) || requestedType.isBlank()) {
            throw new IllegalArgumentException("insuranceType is required");
        }
        if (!insuranceType.equals(requestedType)) {
            throw new IllegalArgumentException(
                    "insuranceType does not match formType. Expected: " + insuranceType);
        }

        Object productType = data.get("productType");
        if (!"New".equals(productType) && !"Portability".equals(productType)) {
            throw new IllegalArgumentException("Invalid productType. Allowed values: New, Portability");
        }
        return insuranceType;
    }

    private Map<String, Object> getData(InsuranceProposalRequest request) {
        Object rawData = request.getFields().get("data");
        if (!(rawData instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("data is required");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> data.put(String.valueOf(key), value));
        return data;
    }

    private <T> void validateData(T value) {
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            ConstraintViolation<T> violation = violations.iterator().next();
            throw new IllegalArgumentException(violation.getMessage());
        }
    }


    private Map<String, String> buildFieldErrors(PolicyPlannerProposalCallResult result) {
        if (result.response() == null
                || result.response().missingFields() == null
                || result.response().missingFields().isEmpty()) {
            return Map.of();
        }

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (String field : result.response().missingFields()) {
            if (field != null && !field.isBlank()) {
                fieldErrors.put(field, "Required field is missing");
            }
        }
        return fieldErrors;
    }

    private ApiError serviceUnavailable(String requestId) {
        return new ApiError(Instant.now(), HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Policy Planner service is temporarily unavailable. Please try again later.",
                requestId, Map.of());
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to serialize proposal audit snapshot", ex);
        }
    }

    private String toJsonSafely(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (RuntimeException ex) {
            return "{}";
        }
    }
}
