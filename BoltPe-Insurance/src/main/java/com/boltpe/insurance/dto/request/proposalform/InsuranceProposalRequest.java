package com.boltpe.insurance.dto.request.proposalform;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BoltPe proposal request keeps the frontend wrapper as-is:
 * client_name, formType, insuranceType and data.
 * Only DOB/date values inside data are converted before the request is sent to Policy Planner.
 */
public class InsuranceProposalRequest {
    @JsonProperty("client_name")
    @NotBlank(message = "client_name is required")
    private String clientName;

    @NotBlank(message = "formType is required")
    private String formType;

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getFormType() {
        return formType;
    }

    public void setFormType(String formType) {
        this.formType = formType;
    }

    @JsonAnySetter
    public void put(String key, Object value) {
        fields.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getFields() {
        return fields;
    }

    /**
     * Rebuilds the exact BoltPe wrapper body received by the API.
     */
    public Map<String, Object> toFlatMap() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("client_name", clientName);
        body.put("formType", formType);
        body.putAll(fields);
        return body;
    }
}
