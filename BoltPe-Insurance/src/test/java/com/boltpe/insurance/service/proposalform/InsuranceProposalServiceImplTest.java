package com.boltpe.insurance.service.proposalform;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.audit.proposalform.InsuranceProposalAuditService;
import com.boltpe.insurance.client.proposalform.PolicyPlannerProposalCallResult;
import com.boltpe.insurance.client.proposalform.PolicyPlannerProposalClient;
import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.exception.ApiError;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.response.proposalform.PolicyPlannerProposalResponse;
import com.boltpe.insurance.validation.proposalform.ProposalFormAgeValidator;
import com.boltpe.insurance.validation.proposalform.ProposalFormAdditionalValidator;
import com.boltpe.insurance.validation.proposalform.ProposalFormContactIdentityValidator;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InsuranceProposalServiceImplTest {
    private GroupProposalService groupService;
    private PersonalAccidentProposalService paService;
    private PolicyPlannerProposalClient client;
    private InsuranceProposalAuditService audit;
    private InsuranceProposalServiceImpl service;
    private ProposalFormAgeValidator ageValidator;
    private ProposalFormContactIdentityValidator contactIdentityValidator;
    private ProposalFormAdditionalValidator additionalValidator;

    @BeforeEach
    void setup() {
        groupService = mock(GroupProposalService.class);
        paService = mock(PersonalAccidentProposalService.class);
        client = mock(PolicyPlannerProposalClient.class);
        audit = mock(InsuranceProposalAuditService.class);
        ageValidator = mock(ProposalFormAgeValidator.class);
        contactIdentityValidator = mock(ProposalFormContactIdentityValidator.class);
        additionalValidator = mock(ProposalFormAdditionalValidator.class);
        service = new InsuranceProposalServiceImpl(
                groupService, paService, client, audit, new JsonMapper(),
                Validation.buildDefaultValidatorFactory().getValidator(), ageValidator, contactIdentityValidator, additionalValidator);
    }

    @Test
    void savesHealthProposalAndReturnsStableBoltPeResponse() {
        InsuranceProposalRequest request = groupRequest("health");
        Map<String, Object> vendorRequest = Map.of("formType", "health", "proposerDOB", "1988-05-12");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                201,
                "{\"success\":true,\"proposalId\":123}",
                new PolicyPlannerProposalResponse(true, "saved", 123, "health", List.of())));

        var response = service.save(request);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Proposal saved successfully");
        assertThat(response.requestId()).isNotBlank();
        assertThat(response.data().proposalId()).isEqualTo("123");
        assertThat(response.data().formType()).isEqualTo("health");
        assertThat(response.data().status()).isEqualTo("SUBMITTED");
        verify(audit).received(eq(response.requestId()), eq("HEALTH"), eq("health"), eq("bolt_pay"),
                contains("12-05-1988"), contains("1988-05-12"));
        verify(audit).sent(response.requestId());
        verify(audit).success(eq(response.requestId()), eq(201), eq("123"), anyString(), contains("SUBMITTED"));
        verify(contactIdentityValidator).validate(any(com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest.class));
        verify(additionalValidator).validateRawCoverAmount(anyMap(), eq("HEALTH"));
        verify(additionalValidator).validateRawData(anyMap());
        verify(additionalValidator).validate(any(com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest.class));
        verify(ageValidator).validate(any());
        verifyNoInteractions(paService);
    }

    @Test
    void routesPersonalAccidentToDedicatedService() {
        InsuranceProposalRequest request = paRequest();
        Map<String, Object> vendorRequest = Map.of("formType", "pa", "dateOfBirth", "1990-01-01");
        when(paService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                201, "ok", new PolicyPlannerProposalResponse(true, "saved", "PA-1", "pa", List.of())));

        var response = service.save(request);

        assertThat(response.data().proposalId()).isEqualTo("PA-1");
        assertThat(response.data().formType()).isEqualTo("pa");
        verify(contactIdentityValidator).validate(any(com.boltpe.insurance.dto.request.proposalform.PersonalAccidentProposalRequest.class));
        verify(additionalValidator).validateRawCoverAmount(anyMap(), eq("PERSONAL_ACCIDENT"));
        verify(paService).toVendorRequest(request);
        verifyNoInteractions(groupService);
    }

    @Test
    void mapsVendor400ToBoltPeBadRequestAndFailedAudit() {
        InsuranceProposalRequest request = groupRequest("supertopup");
        Map<String, Object> vendorRequest = Map.of("formType", "supertopup");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                400,
                "{\"success\":false}",
                new PolicyPlannerProposalResponse(false, "Invalid productType", null, "supertopup", List.of())));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> {
                    PolicyPlannerException policyPlannerException = (PolicyPlannerException) ex;
                    assertThat(policyPlannerException.apiError().status()).isEqualTo(400);
                    assertThat(policyPlannerException.apiError().fieldErrors()).isEmpty();
                });
        verify(audit).failed(anyString(), eq(400), anyString(), anyString(), eq("Invalid productType"));
    }

    @Test
    void mapsVendorMissingFieldsIntoBoltPeFieldErrors() {
        InsuranceProposalRequest request = paRequest();
        Map<String, Object> vendorRequest = Map.of("formType", "pa");
        when(paService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                400,
                "{\"success\":false,\"missingFields\":[\"productName\",\"mobileNumber\"]}",
                new PolicyPlannerProposalResponse(false, "Missing required PA fields", null, "pa",
                        List.of("productName", "mobileNumber"))));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> {
                    PolicyPlannerException policyPlannerException = (PolicyPlannerException) ex;
                    assertThat(policyPlannerException.apiError().status()).isEqualTo(400);
                    assertThat(policyPlannerException.apiError().message()).isEqualTo("Proposal validation failed");
                    assertThat(policyPlannerException.apiError().fieldErrors())
                            .containsEntry("productName", "Required field is missing")
                            .containsEntry("mobileNumber", "Required field is missing");
                });

        verify(audit).failed(anyString(), eq(400), anyString(), contains("productName"),
                eq("Missing required PA fields"));
    }

    @Test
    void mapsVendor500OrUnconfirmedResponseToServiceUnavailable() {
        InsuranceProposalRequest request = groupRequest("hospicash");
        Map<String, Object> vendorRequest = Map.of("formType", "hospicash");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(500, "oops", null));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> assertThat(((PolicyPlannerException) ex).apiError().status()).isEqualTo(503));
        verify(audit).failed(anyString(), eq(500), eq("oops"), anyString(),
                eq("Policy Planner service is temporarily unavailable. Please try again later."));
    }

    @Test
    void timeoutIsStoredAsUnknown() {
        InsuranceProposalRequest request = groupRequest("health");
        Map<String, Object> vendorRequest = Map.of("formType", "health");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> assertThat(((PolicyPlannerException) ex).apiError().status()).isEqualTo(503));
        verify(audit).unknown(anyString(), eq("Policy Planner response was not received"), contains("503"));
    }

    @Test
    void rejectsInvalidClientFormTypeAndProductTypeBeforeVendorCall() {
        InsuranceProposalRequest wrongClient = groupRequest("health");
        wrongClient.setClientName("pp");
        assertThatThrownBy(() -> service.save(wrongClient))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("client_name must be bolt_pay");

        InsuranceProposalRequest wrongForm = groupRequest("health");
        wrongForm.setFormType("other");
        assertThatThrownBy(() -> service.save(wrongForm))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid formType");

        InsuranceProposalRequest wrongProduct = groupRequest("health");
        ((Map<String, Object>) wrongProduct.getFields().get("data")).put("productType", "Renewal");
        assertThatThrownBy(() -> service.save(wrongProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid productType. Allowed values: New, Portability");

        verifyNoInteractions(client, audit);
    }

    @Test
    void stopsBeforeVendorAndAuditWhenProposalAgeValidationFails() {
        InsuranceProposalRequest request = groupRequest("health");
        doThrow(new com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException(
                Map.of("proposerDOB", "Self age must be between 18 and 80 years")))
                .when(ageValidator).validate(any());

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException.class);

        verifyNoInteractions(client, audit);
        verifyNoInteractions(groupService, paService);
    }


    @Test
    void stopsBeforeVendorAndAuditWhenContactValidationFails() {
        InsuranceProposalRequest request = groupRequest("health");
        doThrow(new com.boltpe.insurance.exception.proposalform.ProposalContactValidationException(
                Map.of("pincode", "Pincode must be a valid 6-digit Indian pincode and must not start with 0")))
                .when(contactIdentityValidator).validate(any(com.boltpe.insurance.dto.request.proposalform.GroupProposalRequest.class));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(com.boltpe.insurance.exception.proposalform.ProposalContactValidationException.class);

        verifyNoInteractions(client, audit);
        verifyNoInteractions(groupService, paService);
        verifyNoInteractions(ageValidator);
    }

    @Test
    void rejectsMissingRequiredGroupField() {
        InsuranceProposalRequest request = groupRequest("health");
        ((Map<String, Object>) request.getFields().get("data")).remove("productName");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("productName is required");
        verifyNoInteractions(client, audit);
    }

    @Test
    void rejectsMissingDataObject() {
        InsuranceProposalRequest request = groupRequest("health");
        request.put("data", "not-an-object");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("data is required");
        verifyNoInteractions(client, audit, groupService, paService);
    }

    @Test
    void rejectsMissingNonTextOrBlankInsuranceType() {
        InsuranceProposalRequest missing = groupRequest("health");
        missing.getFields().remove("insuranceType");
        InsuranceProposalRequest nonText = groupRequest("health");
        nonText.put("insuranceType", 1);
        InsuranceProposalRequest blank = groupRequest("health");
        blank.put("insuranceType", "  ");

        for (InsuranceProposalRequest request : List.of(missing, nonText, blank)) {
            assertThatThrownBy(() -> service.save(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("insuranceType is required");
        }
        verifyNoInteractions(client, audit);
    }

    @Test
    void rejectsInsuranceTypeThatDoesNotMatchFormType() {
        InsuranceProposalRequest request = groupRequest("supertopup");
        request.put("insuranceType", "HEALTH");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("insuranceType does not match formType. Expected: SUPER_TOP_UP");
        verifyNoInteractions(client, audit);
    }

    @Test
    void acceptsPortabilityProductType() {
        InsuranceProposalRequest request = groupRequest("health");
        ((Map<String, Object>) request.getFields().get("data")).put("productType", "Portability");
        Map<String, Object> vendorRequest = Map.of("formType", "health");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                201, "ok", new PolicyPlannerProposalResponse(true, "saved", 77, "health", List.of())));

        var response = service.save(request);

        assertThat(response.success()).isTrue();
        assertThat(response.data().proposalId()).isEqualTo("77");
    }

    @Test
    void mapsUnconfirmedSuccessStatusToServiceUnavailable() {
        InsuranceProposalRequest request = groupRequest("health");
        Map<String, Object> vendorRequest = Map.of("formType", "health");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                200, "{\"success\":false}",
                new PolicyPlannerProposalResponse(false, "not saved", null, "health", null)));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> {
                    PolicyPlannerException policyPlannerException = (PolicyPlannerException) ex;
                    assertThat(policyPlannerException.apiError().status()).isEqualTo(503);
                    assertThat(policyPlannerException.apiError().fieldErrors()).isEmpty();
                });
        verify(audit).failed(anyString(), eq(200), eq("{\"success\":false}"), contains("503"), eq("not saved"));
    }

    @Test
    void ignoresNullAndBlankVendorMissingFieldNames() {
        InsuranceProposalRequest request = paRequest();
        Map<String, Object> vendorRequest = Map.of("formType", "pa");
        when(paService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenReturn(new PolicyPlannerProposalCallResult(
                422, "{}",
                new PolicyPlannerProposalResponse(false, "Missing fields", null, "pa",
                        java.util.Arrays.asList("city", null, "  "))));

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> {
                    PolicyPlannerException policyPlannerException = (PolicyPlannerException) ex;
                    assertThat(policyPlannerException.apiError().status()).isEqualTo(400);
                    assertThat(policyPlannerException.apiError().fieldErrors())
                            .containsExactly(entry("city", "Required field is missing"));
                });
    }

    @Test
    void auditSnapshotSerializationFailureStopsBeforeVendorCall() {
        JsonMapper failingMapper = spy(new JsonMapper());
        doThrow(new IllegalArgumentException("serialization failed")).when(failingMapper).writeValueAsString(any());
        InsuranceProposalServiceImpl failingService = new InsuranceProposalServiceImpl(
                groupService, paService, client, audit, failingMapper,
                Validation.buildDefaultValidatorFactory().getValidator(), ageValidator, contactIdentityValidator, additionalValidator);
        InsuranceProposalRequest request = groupRequest("health");
        when(groupService.toVendorRequest(request)).thenReturn(Map.of("formType", "health"));

        assertThatThrownBy(() -> failingService.save(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to serialize proposal audit snapshot")
                .hasCauseInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client, audit);
    }

    @Test
    void errorSerializationFailureStoresEmptyJsonInAudit() {
        JsonMapper failingMapper = spy(new JsonMapper());
        doThrow(new IllegalArgumentException("serialization failed"))
                .when(failingMapper).writeValueAsString(argThat(value -> value instanceof ApiError));
        InsuranceProposalServiceImpl failingService = new InsuranceProposalServiceImpl(
                groupService, paService, client, audit, failingMapper,
                Validation.buildDefaultValidatorFactory().getValidator(), ageValidator, contactIdentityValidator, additionalValidator);
        InsuranceProposalRequest request = groupRequest("health");
        Map<String, Object> vendorRequest = Map.of("formType", "health");
        when(groupService.toVendorRequest(request)).thenReturn(vendorRequest);
        when(client.save(vendorRequest)).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> failingService.save(request))
                .isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> assertThat(((PolicyPlannerException) ex).apiError().status()).isEqualTo(503));
        verify(audit).unknown(anyString(), eq("Policy Planner response was not received"), eq("{}"));
    }

    private InsuranceProposalRequest groupRequest(String formType) {
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        request.setClientName("bolt_pay");
        request.setFormType(formType);
        request.put("insuranceType", switch (formType) {
            case "health" -> "HEALTH";
            case "supertopup" -> "SUPER_TOP_UP";
            case "hospicash" -> "HOSPITAL_CASH";
            default -> "HEALTH";
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productName", "Product");
        data.put("productType", "New");
        data.put("pincode", "110001");
        data.put("cityState", "Delhi, Delhi");
        data.put("zone", "A");
        data.put("upgradeZone", "No");
        data.put("adultCount", 1);
        data.put("childCount", 0);
        data.put("proposerName", "Sample Adult");
        data.put("proposerDOB", "12-05-1988");
        data.put("proposerGender", "male");
        data.put("proposerEmail", "sample@example.com");
        data.put("proposerPhone", "9000000000");
        data.put("sumInsured", BigDecimal.valueOf(500000));
        data.put("selectedTenure", BigDecimal.ONE);
        data.put("annualIncome", "5L_8L");
        data.put("nriDiscount", "No");
        data.put("includeSelf", "Yes");
        data.put("bureauDiscount", "No");
        data.put("aadharNumber", "123456789012");
        data.put("panNumber", "ABCDE1234F");
        data.put("basePremium", BigDecimal.valueOf(12000));
        data.put("totalPremium", BigDecimal.valueOf(14160));
        data.put("adults", List.of());
        data.put("children", List.of());
        request.put("data", data);
        return request;
    }

    private InsuranceProposalRequest paRequest() {
        InsuranceProposalRequest request = new InsuranceProposalRequest();
        request.setClientName("bolt_pay");
        request.setFormType("pa");
        request.put("insuranceType", "PERSONAL_ACCIDENT");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productName", "Personal Accident");
        data.put("productType", "New");
        data.put("pincode", "110001");
        data.put("city", "Delhi");
        data.put("proposerName", "Sample User");
        data.put("gender", "male");
        data.put("dateOfBirth", "01-01-1990");
        data.put("mobileNumber", "9000000000");
        data.put("natureOfWorkDesignation", "Software Engineer");
        data.put("coverAmount", BigDecimal.valueOf(1000000));
        request.put("data", data);
        return request;
    }

}
