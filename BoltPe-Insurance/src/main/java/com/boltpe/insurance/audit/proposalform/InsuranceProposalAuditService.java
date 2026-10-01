package com.boltpe.insurance.audit.proposalform;

import com.boltpe.insurance.entity.proposalform.InsuranceProposalRequestEntity;
import com.boltpe.insurance.repository.proposalform.InsuranceProposalRequestRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceProposalAuditService {
    private static final String SYSTEM_USER = "SYSTEM";
    private final InsuranceProposalRequestRepository repository;

    public InsuranceProposalAuditService(InsuranceProposalRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void received(String requestId, String insuranceType, String formType, String clientName,
                         String boltpeRequestJson, String vendorRequestJson) {
        Instant now = Instant.now();
        InsuranceProposalRequestEntity row = new InsuranceProposalRequestEntity();
        row.setRequestId(requestId);
        row.setInsuranceType(insuranceType);
        row.setFormType(formType);
        row.setClientName(clientName);
        row.setBoltpeRequestJson(boltpeRequestJson);
        row.setVendorRequestJson(vendorRequestJson);
        row.setStatus("RECEIVED");
        row.setCreatedBy(SYSTEM_USER);
        row.setCreatedAt(now);
        row.setUpdatedBy(SYSTEM_USER);
        row.setUpdatedAt(now);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sent(String requestId) {
        update(requestId, "SENT_TO_POLICY_PLANNER", null, null, null, null, Instant.now(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(String requestId, int httpStatus, String proposalId,
                        String vendorResponse, String boltpeResponse) {
        update(requestId, "SUCCESS", httpStatus, proposalId, vendorResponse, boltpeResponse,
                null, Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(String requestId, Integer httpStatus, String vendorResponse,
                       String boltpeResponse, String errorMessage) {
        InsuranceProposalRequestEntity row = required(requestId);
        row.setStatus("FAILED");
        row.setVendorHttpStatus(httpStatus);
        row.setVendorResponseJson(vendorResponse);
        row.setBoltpeResponseJson(boltpeResponse);
        row.setErrorMessage(errorMessage);
        row.setResponseReceivedAt(Instant.now());
        touch(row);
        repository.save(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unknown(String requestId, String errorMessage, String boltpeResponse) {
        InsuranceProposalRequestEntity row = required(requestId);
        row.setStatus("UNKNOWN");
        row.setErrorMessage(errorMessage);
        row.setBoltpeResponseJson(boltpeResponse);
        touch(row);
        repository.save(row);
    }

    private void update(String requestId, String status, Integer httpStatus, String proposalId,
                        String vendorResponse, String boltpeResponse, Instant sentAt, Instant responseAt) {
        InsuranceProposalRequestEntity row = required(requestId);
        row.setStatus(status);
        if (httpStatus != null) row.setVendorHttpStatus(httpStatus);
        if (proposalId != null) row.setPolicyPlannerProposalId(proposalId);
        if (vendorResponse != null) row.setVendorResponseJson(vendorResponse);
        if (boltpeResponse != null) row.setBoltpeResponseJson(boltpeResponse);
        if (sentAt != null) row.setSentAt(sentAt);
        if (responseAt != null) row.setResponseReceivedAt(responseAt);
        touch(row);
        repository.save(row);
    }

    private InsuranceProposalRequestEntity required(String requestId) {
        return repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Proposal audit row not found for requestId=" + requestId));
    }

    private void touch(InsuranceProposalRequestEntity row) {
        row.setUpdatedBy(SYSTEM_USER);
        row.setUpdatedAt(Instant.now());
    }
}
