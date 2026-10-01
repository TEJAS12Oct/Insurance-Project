package com.boltpe.insurance.entity.proposalform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "insurance_proposal_request")
public class InsuranceProposalRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, updatable = false, length = 36)
    private String requestId;

    @Column(name = "insurance_type", nullable = false, length = 50)
    private String insuranceType;

    @Column(name = "form_type", nullable = false, length = 30)
    private String formType;

    @Column(name = "client_name", nullable = false, length = 50)
    private String clientName;

    @Column(name = "policy_planner_proposal_id", length = 100)
    private String policyPlannerProposalId;

    @Column(name = "boltpe_request_json", nullable = false, columnDefinition = "TEXT")
    private String boltpeRequestJson;

    @Column(name = "vendor_request_json", nullable = false, columnDefinition = "TEXT")
    private String vendorRequestJson;

    @Column(name = "vendor_response_json", columnDefinition = "TEXT")
    private String vendorResponseJson;

    @Column(name = "boltpe_response_json", columnDefinition = "TEXT")
    private String boltpeResponseJson;

    @Column(name = "vendor_http_status")
    private Integer vendorHttpStatus;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "response_received_at")
    private Instant responseReceivedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }
    public String getFormType() { return formType; }
    public void setFormType(String formType) { this.formType = formType; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getPolicyPlannerProposalId() { return policyPlannerProposalId; }
    public void setPolicyPlannerProposalId(String policyPlannerProposalId) { this.policyPlannerProposalId = policyPlannerProposalId; }
    public String getBoltpeRequestJson() { return boltpeRequestJson; }
    public void setBoltpeRequestJson(String boltpeRequestJson) { this.boltpeRequestJson = boltpeRequestJson; }
    public String getVendorRequestJson() { return vendorRequestJson; }
    public void setVendorRequestJson(String vendorRequestJson) { this.vendorRequestJson = vendorRequestJson; }
    public String getVendorResponseJson() { return vendorResponseJson; }
    public void setVendorResponseJson(String vendorResponseJson) { this.vendorResponseJson = vendorResponseJson; }
    public String getBoltpeResponseJson() { return boltpeResponseJson; }
    public void setBoltpeResponseJson(String boltpeResponseJson) { this.boltpeResponseJson = boltpeResponseJson; }
    public Integer getVendorHttpStatus() { return vendorHttpStatus; }
    public void setVendorHttpStatus(Integer vendorHttpStatus) { this.vendorHttpStatus = vendorHttpStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getResponseReceivedAt() { return responseReceivedAt; }
    public void setResponseReceivedAt(Instant responseReceivedAt) { this.responseReceivedAt = responseReceivedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
