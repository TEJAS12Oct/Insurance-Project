package com.boltpe.insurance.exception.proposalform;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProposalContactValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public ProposalContactValidationException(Map<String, String> fieldErrors) {
        super("Proposal contact and identity validation failed");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
