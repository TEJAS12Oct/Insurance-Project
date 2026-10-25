package com.boltpe.insurance.service;

import com.boltpe.insurance.audit.InsuranceAuditPersistenceService;
import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.client.PersonalAccidentQuoteBatch;
import com.boltpe.insurance.client.SuperTopUpQuoteBatch;
import com.boltpe.insurance.dto.request.*;
import com.boltpe.insurance.dto.response.HospitalCashInsuranceResponse;
import com.boltpe.insurance.dto.response.PersonalAccidentInsuranceResponse;
import com.boltpe.insurance.dto.response.SuperTopUpInsuranceResponse;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the three additional insurance routes and their service-unavailable auditing paths. */
class InsuranceServiceRoutingCoverageTest {
    private SuperTopUpInsuranceService superTopUp;
    private PersonalAccidentInsuranceService personalAccident;
    private HospitalCashInsuranceService hospitalCash;
    private InsuranceAuditPersistenceService audit;
    private InsuranceServiceImpl service;

    @BeforeEach
    void init() {
        superTopUp = mock(SuperTopUpInsuranceService.class);
        personalAccident = mock(PersonalAccidentInsuranceService.class);
        hospitalCash = mock(HospitalCashInsuranceService.class);
        audit = mock(InsuranceAuditPersistenceService.class);
        service = new InsuranceServiceImpl(mock(HealthInsuranceService.class), audit,
                new JsonMapper(), superTopUp, personalAccident, hospitalCash);
    }

    private InsuranceRequest request(InsuranceType type, Object data) {
        JsonMapper mapper = new JsonMapper();

        Map<String, Object> payload = mapper.convertValue(
                data,
                new TypeReference<Map<String, Object>>() {}
        );

        return new InsuranceRequest(
                type,
                Map.of("userId", "coverage-test"),
                payload
        );
    }

    @Test
    void superTopUpVendorFailureIsAudited() {
        var request = request(InsuranceType.SUPER_TOP_UP,
                new SuperTopUpInsuranceRequest(500_000L, 35, null, null, null, null, null));
        when(superTopUp.fetch(any())).thenThrow(new IllegalStateException("upstream timeout"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("SUPER_TOP_UP"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void superTopUpEmptyBatchIsAudited() {
        var request = request(InsuranceType.SUPER_TOP_UP,
                new SuperTopUpInsuranceRequest(500_000L, 35, null, null, null, null, null));
        when(superTopUp.fetch(any())).thenReturn(new SuperTopUpQuoteBatch(List.of(), List.of()));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("SUPER_TOP_UP"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void personalAccidentVendorFailureIsAudited() {
        var request = request(InsuranceType.PERSONAL_ACCIDENT,
                new PersonalAccidentInsuranceRequest(1_000_000L, 1, 35));
        when(personalAccident.fetch(any())).thenThrow(new IllegalStateException("upstream timeout"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("PERSONAL_ACCIDENT"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void personalAccidentValidationExceptionIsPreserved() {
        var request = request(InsuranceType.PERSONAL_ACCIDENT,
                new PersonalAccidentInsuranceRequest(1_000_000L, 1, 35));
        when(personalAccident.fetch(any())).thenThrow(new IllegalArgumentException("invalid category"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid category");
        verifyNoInteractions(audit);
    }

    @Test
    void personalAccidentEmptyBatchIsAudited() {
        var request = request(InsuranceType.PERSONAL_ACCIDENT,
                new PersonalAccidentInsuranceRequest(1_000_000L, 1, 35));
        when(personalAccident.fetch(any())).thenReturn(new PersonalAccidentQuoteBatch(List.of(), List.of()));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("PERSONAL_ACCIDENT"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void hospitalCashVendorFailureIsAudited() {
        var request = request(InsuranceType.HOSPITAL_CASH,
                new HospitalCashInsuranceRequest(35, 1000L, 1, 0, 30));
        when(hospitalCash.fetch(any())).thenThrow(new IllegalStateException("upstream timeout"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("HOSPITAL_CASH"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void hospitalCashValidationExceptionIsPreserved() {
        var request = request(InsuranceType.HOSPITAL_CASH,
                new HospitalCashInsuranceRequest(35, 1000L, 1, 0, 30));
        when(hospitalCash.fetch(any())).thenThrow(new IllegalArgumentException("invalid duration"));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid duration");
        verifyNoInteractions(audit);
    }

    @Test
    void hospitalCashEmptyBatchIsAudited() {
        var request = request(InsuranceType.HOSPITAL_CASH,
                new HospitalCashInsuranceRequest(35, 1000L, 1, 0, 30));
        when(hospitalCash.fetch(any())).thenReturn(new HospitalCashQuoteBatch(List.of(), List.of()));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class);
        verify(audit).save(anyString(), eq("HOSPITAL_CASH"), anyString(), anyString(),
                eq("[]"), eq("[]"), eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void superTopUpEmptyBatchSeparatesActiveAndFailedApis() {
        var request = request(InsuranceType.SUPER_TOP_UP,
                new SuperTopUpInsuranceRequest(500_000L, 35, null, null, null, null, null));
        when(superTopUp.fetch(any())).thenReturn(new SuperTopUpQuoteBatch(List.of(), List.of(
                new SuperTopUpQuoteBatch.ApiStatus("/stu/active", true, "C1", "P1", null),
                new SuperTopUpQuoteBatch.ApiStatus("/stu/failed", false, "C2", "P2", "timeout"))));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .hasRootCauseMessage("No Policy Planner Super Top-Up provider returned a quote");
        verify(audit).save(anyString(), eq("SUPER_TOP_UP"), anyString(), anyString(),
                argThat(v -> v.contains("/stu/active") && !v.contains("/stu/failed")),
                argThat(v -> v.contains("/stu/failed") && !v.contains("/stu/active")),
                eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void superTopUpSuccessIsReturnedAndAudited() {
        var request = request(InsuranceType.SUPER_TOP_UP,
                new SuperTopUpInsuranceRequest(500_000L, 35, null, null, null, null, null));
        var quote = new SuperTopUpVendorResponse(null, null, "C1", "P1", "Sample Insurer", null, null, null,
                "Sample Top-Up Plan", 500_000L, 1, 0, 35, 35, List.of(), List.of(), null, List.of());
        var batch = new SuperTopUpQuoteBatch(List.of(quote), List.of(
                new SuperTopUpQuoteBatch.ApiStatus("/stu/active", true, "C1", "P1", null),
                new SuperTopUpQuoteBatch.ApiStatus("/stu/failed", false, "C2", "P2", "timeout")));
        when(superTopUp.fetch(any())).thenReturn(batch);
        when(superTopUp.build(anyString(), any(), eq(batch))).thenAnswer(inv ->
                new SuperTopUpInsuranceResponse(inv.getArgument(0), "SUPER_TOP_UP", 0, List.of()));

        var result = (SuperTopUpInsuranceResponse) service.getQuotes(request);

        assertThat(result.insuranceType()).isEqualTo("SUPER_TOP_UP");
        verify(audit).save(eq(result.requestId()), eq("SUPER_TOP_UP"), contains("coverage-test"), anyString(),
                argThat(v -> v.contains("/stu/active") && !v.contains("/stu/failed")),
                argThat(v -> v.contains("/stu/failed") && !v.contains("/stu/active")),
                contains("Sample Top-Up Plan"), contains(result.requestId()));
    }

    @Test
    void personalAccidentEmptyBatchSeparatesActiveAndFailedApis() {
        var request = request(InsuranceType.PERSONAL_ACCIDENT,
                new PersonalAccidentInsuranceRequest(1_000_000L, 1, 35));
        when(personalAccident.fetch(any())).thenReturn(new PersonalAccidentQuoteBatch(List.of(), List.of(
                new PersonalAccidentQuoteBatch.ApiStatus("/pa/active", true, "C1", "P1", null),
                new PersonalAccidentQuoteBatch.ApiStatus("/pa/failed", false, "C2", "P2", "timeout"))));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .hasRootCauseMessage("No Policy Planner Personal Accident provider returned a quote");
        verify(audit).save(anyString(), eq("PERSONAL_ACCIDENT"), anyString(), anyString(),
                argThat(v -> v.contains("/pa/active") && !v.contains("/pa/failed")),
                argThat(v -> v.contains("/pa/failed") && !v.contains("/pa/active")),
                eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void personalAccidentSuccessIsReturnedAndAudited() {
        var request = request(InsuranceType.PERSONAL_ACCIDENT,
                new PersonalAccidentInsuranceRequest(1_000_000L, 1, 35));
        var quote = new PersonalAccidentVendorResponse("Sample Insurer", "Sample Accident Plan", null, null, null,
                null, 1_000_000L, List.of(), List.of(), null);
        var batch = new PersonalAccidentQuoteBatch(List.of(quote), List.of(
                new PersonalAccidentQuoteBatch.ApiStatus("/pa/active", true, "C1", "P1", null),
                new PersonalAccidentQuoteBatch.ApiStatus("/pa/failed", false, "C2", "P2", "timeout")));
        when(personalAccident.fetch(any())).thenReturn(batch);
        when(personalAccident.build(anyString(), eq(batch))).thenAnswer(inv ->
                new PersonalAccidentInsuranceResponse(inv.getArgument(0), "PERSONAL_ACCIDENT", 0, List.of()));

        var result = (PersonalAccidentInsuranceResponse) service.getQuotes(request);

        assertThat(result.insuranceType()).isEqualTo("PERSONAL_ACCIDENT");
        verify(audit).save(eq(result.requestId()), eq("PERSONAL_ACCIDENT"), contains("coverage-test"), anyString(),
                argThat(v -> v.contains("/pa/active") && !v.contains("/pa/failed")),
                argThat(v -> v.contains("/pa/failed") && !v.contains("/pa/active")),
                contains("Sample Accident Plan"), contains(result.requestId()));
    }

    @Test
    void hospitalCashEmptyBatchSeparatesActiveAndFailedApis() {
        var request = request(InsuranceType.HOSPITAL_CASH,
                new HospitalCashInsuranceRequest(35, 1000L, 1, 0, 30));
        when(hospitalCash.fetch(any())).thenReturn(new HospitalCashQuoteBatch(List.of(), List.of(
                new HospitalCashQuoteBatch.ApiStatus("/hc/active", true, "C1", "P1", null),
                new HospitalCashQuoteBatch.ApiStatus("/hc/failed", false, "C2", "P2", "timeout"))));
        assertThatThrownBy(() -> service.getQuotes(request)).isInstanceOf(PolicyPlannerException.class)
                .hasRootCauseMessage("No Policy Planner Hospital Cash provider returned a quote");
        verify(audit).save(anyString(), eq("HOSPITAL_CASH"), anyString(), anyString(),
                argThat(v -> v.contains("/hc/active") && !v.contains("/hc/failed")),
                argThat(v -> v.contains("/hc/failed") && !v.contains("/hc/active")),
                eq("[]"), contains("temporarily unavailable"));
    }

    @Test
    void hospitalCashSuccessIsReturnedAndAudited() {
        var request = request(InsuranceType.HOSPITAL_CASH,
                new HospitalCashInsuranceRequest(35, 1000L, 1, 0, 30));
        var quote = new HospitalCashVendorResponse("Sample Insurer", "Sample Cash Plan", null, null, null,
                null, 1000L, new BigDecimal("499"), 30, List.of(), null);
        var batch = new HospitalCashQuoteBatch(List.of(quote), List.of(
                new HospitalCashQuoteBatch.ApiStatus("/hc/active", true, "C1", "P1", null),
                new HospitalCashQuoteBatch.ApiStatus("/hc/failed", false, "C2", "P2", "timeout")));
        when(hospitalCash.fetch(any())).thenReturn(batch);
        when(hospitalCash.build(anyString(), any(), eq(batch))).thenAnswer(inv ->
                new HospitalCashInsuranceResponse(inv.getArgument(0), "HOSPITAL_CASH", 0, List.of()));

        var result = (HospitalCashInsuranceResponse) service.getQuotes(request);

        assertThat(result.insuranceType()).isEqualTo("HOSPITAL_CASH");
        verify(audit).save(eq(result.requestId()), eq("HOSPITAL_CASH"), contains("coverage-test"), anyString(),
                argThat(v -> v.contains("/hc/active") && !v.contains("/hc/failed")),
                argThat(v -> v.contains("/hc/failed") && !v.contains("/hc/active")),
                contains("Sample Cash Plan"), contains(result.requestId()));
    }
}
