package com.boltpe.insurance.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.boltpe.insurance.entity.InsuranceQuoteRequest;
import com.boltpe.insurance.repository.InsuranceQuoteRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InsuranceAuditPersistenceServiceTest {

    private InsuranceQuoteRequestRepository repository;
    private InsuranceAuditPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(InsuranceQuoteRequestRepository.class);
        service = new InsuranceAuditPersistenceService(repository);
    }

    @Test
    void shouldPersistCompleteInsuranceAuditRow() {
        service.save(
                "request-123",
                "PERSONAL_ACCIDENT",
                "{}",
                "{\"insuranceType\":\"PERSONAL_ACCIDENT\"}",
                "[{\"active\":true}]",
                "[{\"active\":false}]",
                "[{\"company\":\"STAR HEALTH\"}]",
                "{\"planCount\":1}"
        );

        ArgumentCaptor<InsuranceQuoteRequest> captor =
                ArgumentCaptor.forClass(InsuranceQuoteRequest.class);
        verify(repository).save(captor.capture());

        InsuranceQuoteRequest row = captor.getValue();
        assertThat(row.getRequestId()).isEqualTo("request-123");
        assertThat(row.getInsuranceType()).isEqualTo("PERSONAL_ACCIDENT");
        assertThat(row.getUserInformation()).isEqualTo("{}");
        assertThat(row.getBoltpeRequestJson()).isEqualTo("{\"insuranceType\":\"PERSONAL_ACCIDENT\"}");
        assertThat(row.getAllApiTrue()).isEqualTo("[{\"active\":true}]");
        assertThat(row.getAllApiFalse()).isEqualTo("[{\"active\":false}]");
        assertThat(row.getVendorResponseJson()).isEqualTo("[{\"company\":\"STAR HEALTH\"}]");
        assertThat(row.getBoltpeResponseJson()).isEqualTo("{\"planCount\":1}");
        assertThat(row.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(row.getUpdatedBy()).isEqualTo("SYSTEM");
        assertThat(row.getCreatedAt()).isNotNull();
        assertThat(row.getUpdatedAt()).isEqualTo(row.getCreatedAt());
    }
}
