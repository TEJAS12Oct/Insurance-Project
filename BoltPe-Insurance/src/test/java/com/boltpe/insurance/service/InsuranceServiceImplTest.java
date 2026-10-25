package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.audit.InsuranceAuditPersistenceService;
import com.boltpe.insurance.client.HealthQuoteBatch;
import com.boltpe.insurance.dto.request.*;
import com.boltpe.insurance.dto.response.HealthInsuranceResponse;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.BoltPeInsuranceException;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.exception.UnsupportedInsuranceTypeException;
import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class InsuranceServiceImplTest {
    private HealthInsuranceService healthService;
    private InsuranceAuditPersistenceService auditService;
    private InsuranceServiceImpl service;

    @BeforeEach void setup() {
        healthService = mock(HealthInsuranceService.class);
        auditService = mock(InsuranceAuditPersistenceService.class);
        service = new InsuranceServiceImpl(healthService, auditService, new JsonMapper());
    }

    private InsuranceRequest healthRequest(long coverAmount) {
        return new InsuranceRequest(InsuranceType.HEALTH,
                Map.of("userId", "USR001", "name", "Test User", "mobile", "9999999999"),
                new HealthInsuranceRequest(coverAmount,"1",30,null,null,null,null,null));
    }

    @Test void shouldStoreExactlyOneAuditRowWithSuccessResponseAndSeparatedApis() {
        var request = healthRequest(1000000L);
        var health = new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null);
        HealthVendorResponse vendor = new HealthVendorResponse(null, "1004", "225", "Health Edge", 1000000L, 1, 1, 0, 30, 30,
                new BigDecimal("9980"), new BigDecimal("9980"), BigDecimal.ZERO, List.of(), null, null, List.of(), List.of(), List.of(), List.of(), List.of());
        var statuses = List.of(
                new HealthQuoteBatch.ApiStatus("/premium/success", true, "1004", "225", null),
                new HealthQuoteBatch.ApiStatus("/premium/failed", false, "1005", "190", "No premium found"));
        var batch = new HealthQuoteBatch(List.of(vendor), statuses);
        when(healthService.fetch(health)).thenReturn(batch);
        var mapped = new HealthInsuranceResponse.Plan(null,null,null,null,List.of(),List.of(),Map.of(),List.of(),List.of(),List.of(),List.of(),null,null,List.of(),true,"YEAR");
        var response = new HealthInsuranceResponse("REQ", "HEALTH", 1, List.of(mapped));
        when(healthService.build(anyString(), eq(health), eq(batch))).thenAnswer(inv ->
                new HealthInsuranceResponse(inv.getArgument(0), "HEALTH", 1, List.of(mapped)));

        var result = (HealthInsuranceResponse) service.getQuotes(request);
        assertThat(result.planCount()).isEqualTo(1);
        verify(auditService, times(1)).save(
                eq(result.requestId()), eq("HEALTH"), contains("USR001"), contains("HEALTH"),
                argThat(v -> v.contains("/premium/success") && !v.contains("/premium/failed")),
                argThat(v -> v.contains("/premium/failed") && !v.contains("/premium/success")),
                contains("Health Edge"), contains(result.requestId()));
    }

    @Test void shouldStore503ResponseWhenPolicyPlannerIsUnavailable() {
        var request = healthRequest(1000000L);
        when(healthService.fetch(any())).thenThrow(new PolicyPlannerException("vendor down"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> assertThat(((PolicyPlannerException) ex).apiError().status()).isEqualTo(503));
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(), eq("[]"), eq("[]"), eq("[]"),
                contains("Policy Planner service is temporarily unavailable"));
    }

    @Test void shouldStoreEveryFailedApiInAllApiFalseWhenAllPremiumApisFail() {
        var request = healthRequest(1000000L);
        var statuses = List.of(
                new HealthQuoteBatch.ApiStatus("/premium/first", false, "1009", "191", "timeout"),
                new HealthQuoteBatch.ApiStatus("/premium/second", false, "1004", "225", "No premium found"));
        when(healthService.fetch(any())).thenReturn(new HealthQuoteBatch(List.of(), statuses));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(), eq("[]"),
                argThat(v -> v.contains("/premium/first") && v.contains("/premium/second")), eq("[]"),
                contains("Policy Planner service is temporarily unavailable"));
    }

    @Test void shouldStore500ResponseWhenBoltPeProcessingFails() {
        var request = healthRequest(1000000L);
        HealthVendorResponse vendor = new HealthVendorResponse(null, "1004", "225", "Health Edge", 1000000L, 1, 1, 0, 30, 30,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, List.of(), null, null, List.of(), List.of(), List.of(), List.of(), List.of());
        var batch = new HealthQuoteBatch(List.of(vendor), List.of());
        when(healthService.fetch(any())).thenReturn(batch);
        when(healthService.build(anyString(), any(), eq(batch))).thenThrow(new RuntimeException("mapping broke"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(BoltPeInsuranceException.class);
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(), anyString(), anyString(),
                contains("Health Edge"), contains("BoltPe Insurance service encountered an unexpected error"));
    }

    @Test void shouldRejectUnsupportedInsuranceTypeWhenProductServiceMissing() {
        var request = new InsuranceRequest(InsuranceType.SUPER_TOP_UP, Map.of("userId","USR001"),
                new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(UnsupportedInsuranceTypeException.class);
        verifyNoInteractions(auditService);
    }

    @Test void shouldKeepHealthValidationFailureAsBadRequest() {
        var request = healthRequest(1200000L);
        when(healthService.fetch(any())).thenThrow(new IllegalArgumentException("invalid health request"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid health request");
        verifyNoInteractions(auditService);
    }

    @Test void shouldRejectPersonalAccidentWhenProductServiceMissing() {
        var request = new InsuranceRequest(InsuranceType.PERSONAL_ACCIDENT, Map.of("userId","USR001"),
                Map.<String, Object>of("coverAmount", 1_000_000L, "category", 1, "age", 35));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(UnsupportedInsuranceTypeException.class)
                .hasMessageContaining("PERSONAL_ACCIDENT");
        verifyNoInteractions(auditService, healthService);
    }

    @Test void shouldRejectHospitalCashWhenProductServiceMissing() {
        var request = new InsuranceRequest(InsuranceType.HOSPITAL_CASH, Map.of("userId","USR001"),
                Map.<String, Object>of("coverAmount", 1000L, "age", 35));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(UnsupportedInsuranceTypeException.class)
                .hasMessageContaining("HOSPITAL_CASH");
        verifyNoInteractions(auditService, healthService);
    }

    @Test void shouldRejectRequestWithoutInsuranceType() {
        var request = new InsuranceRequest(null, Map.of("userId","USR001"),
                new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(UnsupportedInsuranceTypeException.class)
                .hasMessage("Insurance type is not implemented in this release");
        verifyNoInteractions(auditService, healthService);
    }

    @Test void shouldStore500ResponseWhenHealthFetchFailsUnexpectedly() {
        var request = healthRequest(1000000L);
        var cause = new IllegalStateException("unexpected client state");
        when(healthService.fetch(any())).thenThrow(cause);
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(BoltPeInsuranceException.class)
                .hasCause(cause)
                .satisfies(ex -> assertThat(((BoltPeInsuranceException) ex).apiError().status()).isEqualTo(500));
        verify(auditService).save(anyString(), eq("HEALTH"), contains("USR001"), contains("HEALTH"),
                eq("[]"), eq("[]"), eq("[]"), contains("BoltPe Insurance service encountered an unexpected error"));
        verify(healthService, never()).build(anyString(), any(), any());
    }

    @Test void shouldSeparateActiveAndFailedApisWhenNoHealthQuoteIsReturned() {
        var request = healthRequest(1000000L);
        var statuses = List.of(
                new HealthQuoteBatch.ApiStatus("/premium/active-empty", true, "1004", "225", null),
                new HealthQuoteBatch.ApiStatus("/premium/failed", false, "1005", "190", "timeout"));
        when(healthService.fetch(any())).thenReturn(new HealthQuoteBatch(List.of(), statuses));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .satisfies(ex -> assertThat(((PolicyPlannerException) ex).apiError().status()).isEqualTo(503));
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(),
                argThat(v -> v.contains("/premium/active-empty") && !v.contains("/premium/failed")),
                argThat(v -> v.contains("/premium/failed") && !v.contains("/premium/active-empty")),
                eq("[]"), contains("Policy Planner service is temporarily unavailable"));
    }

    @Test void shouldSeparateActiveAndFailedApisWhenHealthMappingFails() {
        var request = healthRequest(1000000L);
        var statuses = List.of(
                new HealthQuoteBatch.ApiStatus("/premium/success", true, "1004", "225", null),
                new HealthQuoteBatch.ApiStatus("/premium/failed", false, "1005", "190", "No premium found"));
        var batch = new HealthQuoteBatch(List.of(vendor()), statuses);
        when(healthService.fetch(any())).thenReturn(batch);
        when(healthService.build(anyString(), any(), eq(batch))).thenThrow(new IllegalStateException("mapping broke"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(BoltPeInsuranceException.class)
                .satisfies(ex -> assertThat(((BoltPeInsuranceException) ex).apiError().status()).isEqualTo(500));
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(),
                argThat(v -> v.contains("/premium/success") && !v.contains("/premium/failed")),
                argThat(v -> v.contains("/premium/failed") && !v.contains("/premium/success")),
                contains("Health Edge"), contains("BoltPe Insurance service encountered an unexpected error"));
    }

    @Test void shouldAuditEmptyVendorSnapshotWhenFailureSnapshotCannotBeSerialized() {
        JsonMapper mapper = spy(new JsonMapper());
        doThrow(new IllegalStateException("snapshot not serializable")).when(mapper)
                .writeValueAsString(argThat(v -> v instanceof List<?> list && !list.isEmpty()
                        && list.getFirst() instanceof HealthVendorResponse));
        var failingSnapshotService = new InsuranceServiceImpl(healthService, auditService, mapper);
        var batch = new HealthQuoteBatch(List.of(vendor()), List.of());
        when(healthService.fetch(any())).thenReturn(batch);
        when(healthService.build(anyString(), any(), eq(batch))).thenThrow(new IllegalStateException("mapping broke"));

        assertThatThrownBy(() -> failingSnapshotService.getQuotes(healthRequest(1000000L)))
                .isInstanceOf(BoltPeInsuranceException.class);
        verify(auditService).save(anyString(), eq("HEALTH"), anyString(), anyString(), eq("[]"), eq("[]"),
                eq("{}"), contains("BoltPe Insurance service encountered an unexpected error"));
    }

    @Test void shouldFailWithoutAuditWhenRequestSnapshotCannotBeSerialized() {
        Map<String, Object> userInformation = Map.of("userId", "USR001", "device", new UnserializableValue());
        var request = new InsuranceRequest(InsuranceType.HEALTH, userInformation,
                new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .hasMessage("Unable to serialize insurance request snapshot")
                .hasCauseInstanceOf(tools.jackson.core.JacksonException.class);
        verifyNoInteractions(auditService, healthService);
    }

    /** Jackson wraps the getter failure in a JacksonException during serialization. */
    static final class UnserializableValue {
        public String getValue() { throw new IllegalStateException("cannot read value"); }
    }

    private HealthVendorResponse vendor() {
        return new HealthVendorResponse(null, "1004", "225", "Health Edge", 1000000L, 1, 1, 0, 30, 30,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, List.of(), null, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
