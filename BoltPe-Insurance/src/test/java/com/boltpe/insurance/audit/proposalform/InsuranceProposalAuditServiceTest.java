package com.boltpe.insurance.audit.proposalform;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.entity.proposalform.InsuranceProposalRequestEntity;
import com.boltpe.insurance.repository.proposalform.InsuranceProposalRequestRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InsuranceProposalAuditServiceTest {
    @Test
    void receivedCreatesAuditRow() {
        InsuranceProposalRequestRepository repository = mock(InsuranceProposalRequestRepository.class);
        InsuranceProposalAuditService service = new InsuranceProposalAuditService(repository);

        service.received("req-1", "HEALTH", "health", "bolt_pay", "bolt", "vendor");

        ArgumentCaptor<InsuranceProposalRequestEntity> captor = ArgumentCaptor.forClass(InsuranceProposalRequestEntity.class);
        verify(repository).save(captor.capture());
        InsuranceProposalRequestEntity row = captor.getValue();
        assertThat(row.getRequestId()).isEqualTo("req-1");
        assertThat(row.getStatus()).isEqualTo("RECEIVED");
        assertThat(row.getCreatedBy()).isEqualTo("SYSTEM");
        assertThat(row.getVendorRequestJson()).isEqualTo("vendor");
    }

    @Test
    void sentAndSuccessUpdateExistingRow() {
        InsuranceProposalRequestRepository repository = mock(InsuranceProposalRequestRepository.class);
        InsuranceProposalRequestEntity row = new InsuranceProposalRequestEntity();
        when(repository.findByRequestId("req-1")).thenReturn(Optional.of(row));
        InsuranceProposalAuditService service = new InsuranceProposalAuditService(repository);

        service.sent("req-1");
        assertThat(row.getStatus()).isEqualTo("SENT_TO_POLICY_PLANNER");
        assertThat(row.getSentAt()).isNotNull();

        service.success("req-1", 201, "123", "vendor-response", "bolt-response");
        assertThat(row.getStatus()).isEqualTo("SUCCESS");
        assertThat(row.getVendorHttpStatus()).isEqualTo(201);
        assertThat(row.getPolicyPlannerProposalId()).isEqualTo("123");
        assertThat(row.getResponseReceivedAt()).isNotNull();
    }

    @Test
    void failedAndUnknownArePersisted() {
        InsuranceProposalRequestRepository repository = mock(InsuranceProposalRequestRepository.class);
        InsuranceProposalRequestEntity row = new InsuranceProposalRequestEntity();
        when(repository.findByRequestId("req-1")).thenReturn(Optional.of(row));
        InsuranceProposalAuditService service = new InsuranceProposalAuditService(repository);

        service.failed("req-1", 400, "vendor", "bolt", "invalid");
        assertThat(row.getStatus()).isEqualTo("FAILED");
        assertThat(row.getErrorMessage()).isEqualTo("invalid");
        assertThat(row.getVendorHttpStatus()).isEqualTo(400);

        service.unknown("req-1", "timeout", "bolt-timeout");
        assertThat(row.getStatus()).isEqualTo("UNKNOWN");
        assertThat(row.getErrorMessage()).isEqualTo("timeout");
        assertThat(row.getBoltpeResponseJson()).isEqualTo("bolt-timeout");
        verify(repository, atLeast(2)).save(any());
    }

    @Test
    void missingAuditRowFailsFast() {
        InsuranceProposalRequestRepository repository = mock(InsuranceProposalRequestRepository.class);
        when(repository.findByRequestId("missing")).thenReturn(Optional.empty());
        InsuranceProposalAuditService service = new InsuranceProposalAuditService(repository);

        assertThatThrownBy(() -> service.sent("missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Proposal audit row not found");
    }
}
