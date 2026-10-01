package com.boltpe.insurance.exception.proposalform;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProposalAgeValidationException extends IllegalArgumentException {
    private final Map<String, String> fieldErrors;

    public ProposalAgeValidationException(Map<String, String> fieldErrors) {
        super("Proposal age validation failed");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
