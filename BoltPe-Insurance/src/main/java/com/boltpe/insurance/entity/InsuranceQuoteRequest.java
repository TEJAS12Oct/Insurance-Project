package com.boltpe.insurance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "insurance_quote_request")
public class InsuranceQuoteRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "request_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 36
    )
    private String requestId;

    @Column(
            name = "insurance_type",
            nullable = false,
            length = 50
    )
    private String insuranceType;

    @Column(
            name = "user_information",
            columnDefinition = "TEXT"
    )
    private String userInformation;

    @Column(
            name = "boltpe_request_json",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String boltpeRequestJson;

    @Column(
            name = "all_api_true",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String allApiTrue;

    @Column(
            name = "all_api_false",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String allApiFalse;

    @Column(
            name = "vendor_response_json",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String vendorResponseJson;

    @Column(
            name = "boltpe_response_json",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String boltpeResponseJson;

    @Column(
            name = "created_by",
            nullable = false,
            length = 100
    )
    private String createdBy;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_by",
            nullable = false,
            length = 100
    )
    private String updatedBy;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;


    // =========================
    // Getters and Setters
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public void setInsuranceType(String insuranceType) {
        this.insuranceType = insuranceType;
    }

    public String getUserInformation() {
        return userInformation;
    }

    public void setUserInformation(String userInformation) {
        this.userInformation = userInformation;
    }

    public String getBoltpeRequestJson() {
        return boltpeRequestJson;
    }

    public void setBoltpeRequestJson(String boltpeRequestJson) {
        this.boltpeRequestJson = boltpeRequestJson;
    }

    public String getAllApiTrue() {
        return allApiTrue;
    }

    public void setAllApiTrue(String allApiTrue) {
        this.allApiTrue = allApiTrue;
    }

    public String getAllApiFalse() {
        return allApiFalse;
    }

    public void setAllApiFalse(String allApiFalse) {
        this.allApiFalse = allApiFalse;
    }

    public String getVendorResponseJson() {
        return vendorResponseJson;
    }

    public void setVendorResponseJson(String vendorResponseJson) {
        this.vendorResponseJson = vendorResponseJson;
    }

    public String getBoltpeResponseJson() {
        return boltpeResponseJson;
    }

    public void setBoltpeResponseJson(String boltpeResponseJson) {
        this.boltpeResponseJson = boltpeResponseJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}