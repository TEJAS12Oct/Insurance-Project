package com.boltpe.insurance.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String requestId,
        Map<String, String> fieldErrors) {}
