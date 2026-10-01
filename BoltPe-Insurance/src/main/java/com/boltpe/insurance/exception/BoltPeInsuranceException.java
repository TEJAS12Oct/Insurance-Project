package com.boltpe.insurance.exception;

public class BoltPeInsuranceException extends RuntimeException {
    private final ApiError apiError;

    public BoltPeInsuranceException(ApiError apiError, Throwable cause) {
        super(apiError.message(), cause);
        this.apiError = apiError;
    }

    public ApiError apiError() { return apiError; }
}
