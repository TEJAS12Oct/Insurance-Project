package com.boltpe.insurance.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "insurance_vendor_api_config",
        indexes = @Index(name = "idx_vendor_api_insurance_type", columnList = "insurance_type"))
public class InsuranceVendorApiConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "insurance_type", nullable = false, length = 50) private String insuranceType;
    @Column(name = "company_id", length = 50) private String companyId;
    @Column(name = "plan_id", length = 50) private String planId;
    @Column(name = "api_type", nullable = false, columnDefinition = "TEXT") private String apiType;
    @Column(name = "refreshed_at", nullable = false) private Instant refreshedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public Long getId() { return id; }
    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }
    public Instant getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(Instant refreshedAt) { this.refreshedAt = refreshedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
