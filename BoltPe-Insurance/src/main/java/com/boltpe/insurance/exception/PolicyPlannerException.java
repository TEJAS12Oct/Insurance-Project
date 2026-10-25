package com.boltpe.insurance.exception;

public class PolicyPlannerException extends RuntimeException {
    private final transient ApiError apiError;

    public PolicyPlannerException(String message) { super(message); this.apiError = null; }
    public PolicyPlannerException(String message, Throwable cause) { super(message, cause); this.apiError = null; }
    public PolicyPlannerException(ApiError apiError, Throwable cause) {
        super(apiError.message(), cause);
        this.apiError = apiError;
    }
    public ApiError apiError() { return apiError; }
}
