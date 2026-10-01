package com.boltpe.insurance.exception;

import com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException;
import com.boltpe.insurance.exception.proposalform.ProposalAdditionalValidationException;
import com.boltpe.insurance.exception.proposalform.ProposalContactValidationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", null, fields);
    }

    @ExceptionHandler(UnsupportedInsuranceTypeException.class)
    ResponseEntity<ApiError> unsupported(UnsupportedInsuranceTypeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null, Map.of());
    }


    @ExceptionHandler(ProposalContactValidationException.class)
    ResponseEntity<ApiError> proposalContactValidation(ProposalContactValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Proposal validation failed", null, ex.getFieldErrors());
    }

    @ExceptionHandler(ProposalAdditionalValidationException.class)
    ResponseEntity<ApiError> proposalAdditionalValidation(ProposalAdditionalValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Proposal validation failed", null, ex.getFieldErrors());
    }

    @ExceptionHandler(ProposalAgeValidationException.class)
    ResponseEntity<ApiError> proposalAgeValidation(ProposalAgeValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, "Proposal age validation failed", null, ex.getFieldErrors());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null, Map.of());
    }

    @ExceptionHandler(PolicyPlannerException.class)
    ResponseEntity<ApiError> vendor(PolicyPlannerException ex) {
        ApiError error = ex.apiError();
        if (error != null) return ResponseEntity.status(error.status()).body(error);
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Policy Planner service is temporarily unavailable. Please try again later.", null, Map.of());
    }

    @ExceptionHandler(BoltPeInsuranceException.class)
    ResponseEntity<ApiError> boltpe(BoltPeInsuranceException ex) {
        ApiError error = ex.apiError();
        return ResponseEntity.status(error.status()).body(error);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "BoltPe Insurance service encountered an unexpected error. Please try again later.", null, Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, String requestId, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, requestId, fields));
    }
}
