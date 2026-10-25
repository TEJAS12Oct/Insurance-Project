package com.boltpe.insurance.exception;

import com.boltpe.insurance.exception.proposalform.ProposalAdditionalValidationException;
import com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException;
import com.boltpe.insurance.exception.proposalform.ProposalContactValidationException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerCoverageTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void argumentValidationPreservesFieldErrors() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "age", "invalid age"));
        binding.addError(new FieldError("request", "age", "duplicate message"));
        var result = handler.validation(new MethodArgumentNotValidException(null, binding));
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("invalid age", result.getBody().fieldErrors().get("age"));
    }

    @Test void validationExceptionVariantsReturnBadRequest() {
        var fields = Map.of("age", "invalid");
        assertEquals(fields, handler.proposalContactValidation(new ProposalContactValidationException(fields)).getBody().fieldErrors());
        assertEquals(fields, handler.proposalAdditionalValidation(new ProposalAdditionalValidationException(fields)).getBody().fieldErrors());
        assertEquals(fields, handler.proposalAgeValidation(new ProposalAgeValidationException(fields)).getBody().fieldErrors());
        assertEquals(HttpStatus.BAD_REQUEST, handler.invalidArgument(new IllegalArgumentException("bad input")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, handler.unsupported(new UnsupportedInsuranceTypeException("unsupported")).getStatusCode());
    }

    @Test void vendorExceptionsSupportProvidedAndFallbackError() {
        var error = new ApiError(Instant.now(), 429, "Too Many Requests", "rate limited", "r-1", Map.of());
        assertEquals(429, handler.vendor(new PolicyPlannerException(error, null)).getStatusCode().value());
        assertSame(error, handler.vendor(new PolicyPlannerException(error, null)).getBody());
        assertEquals(503, handler.vendor(new PolicyPlannerException("vendor down")).getStatusCode().value());
        assertEquals(503, handler.vendor(new PolicyPlannerException("vendor down", new RuntimeException())).getStatusCode().value());
    }

    @Test void internalErrorsDoNotExposeDetails() {
        var error = new ApiError(Instant.now(), 409, "Conflict", "conflict", null, Map.of());
        var exception = new BoltPeInsuranceException(error, new RuntimeException("cause"));
        assertSame(error, exception.apiError());
        assertSame(error, handler.boltpe(exception).getBody());
        var unexpected = handler.unexpected(new RuntimeException("secret"));
        assertEquals(500, unexpected.getStatusCode().value());
        assertFalse(unexpected.getBody().message().contains("secret"));
        assertNotNull(unexpected.getBody().timestamp());
    }

    @Test void argumentValidationKeepsAllFieldsInOrderAndIgnoresObjectErrors() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "zone", "zone is required"));
        binding.addError(new ObjectError("request", "Invalid insurance request data"));
        binding.addError(new FieldError("request", "age", "age is required"));

        ApiError body = handler.validation(new MethodArgumentNotValidException(null, binding)).getBody();

        assertEquals("Request validation failed", body.message());
        assertEquals(List.of("zone", "age"), List.copyOf(body.fieldErrors().keySet()));
        assertEquals("zone is required", body.fieldErrors().get("zone"));
    }

    @Test void argumentValidationWithoutFieldErrorsReturnsEmptyFields() {
        var binding = new BeanPropertyBindingResult(new Object(), "request");

        ApiError body = handler.validation(new MethodArgumentNotValidException(null, binding)).getBody();

        assertEquals(400, body.status());
        assertTrue(body.fieldErrors().isEmpty());
    }

    @Test void builtErrorsCarryStatusReasonMessageAndTimestamp() {
        Instant before = Instant.now();

        var response = handler.unsupported(new UnsupportedInsuranceTypeException("Insurance type MOTOR is not supported"));
        ApiError body = response.getBody();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals("Insurance type MOTOR is not supported", body.message());
        assertNull(body.requestId());
        assertTrue(body.fieldErrors().isEmpty());
        assertFalse(body.timestamp().isBefore(before));
        assertFalse(body.timestamp().isAfter(Instant.now()));
    }

    @Test void invalidArgumentPassesMessageThrough() {
        ApiError body = handler.invalidArgument(new IllegalArgumentException("coverAmount must be positive")).getBody();

        assertEquals("coverAmount must be positive", body.message());
        assertEquals("Bad Request", body.error());
        assertTrue(body.fieldErrors().isEmpty());
    }

    @Test void proposalValidationHandlersUseFixedMessages() {
        var fields = Map.of("pincode", "Pincode is required");

        var contact = handler.proposalContactValidation(new ProposalContactValidationException(fields));
        var additional = handler.proposalAdditionalValidation(new ProposalAdditionalValidationException(fields));
        var age = handler.proposalAgeValidation(new ProposalAgeValidationException(fields));

        assertEquals("Proposal validation failed", contact.getBody().message());
        assertEquals("Proposal validation failed", additional.getBody().message());
        assertEquals("Proposal age validation failed", age.getBody().message());
        for (var response : List.of(contact, additional, age)) {
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNull(response.getBody().requestId());
        }
    }

    @Test void vendorFallbackHidesVendorMessage() {
        ApiError body = handler.vendor(new PolicyPlannerException("connection refused to vendor host")).getBody();

        assertEquals(503, body.status());
        assertEquals("Service Unavailable", body.error());
        assertEquals("Policy Planner service is temporarily unavailable. Please try again later.", body.message());
        assertTrue(body.fieldErrors().isEmpty());
    }

    @Test void boltpeUsesStatusFromApiError() {
        var error = new ApiError(Instant.now(), 502, "Bad Gateway", "Vendor returned invalid data", "r-2", Map.of());

        var response = handler.boltpe(new BoltPeInsuranceException(error, null));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertSame(error, response.getBody());
    }

    @Test void unexpectedReturnsGenericBodyWithoutDetails() {
        ApiError body = handler.unexpected(new Exception("db password rejected")).getBody();

        assertEquals("Internal Server Error", body.error());
        assertEquals("BoltPe Insurance service encountered an unexpected error. Please try again later.", body.message());
        assertNull(body.requestId());
        assertTrue(body.fieldErrors().isEmpty());
    }

    @Test void isRegisteredAsRestControllerAdvice() {
        assertTrue(GlobalExceptionHandler.class.isAnnotationPresent(RestControllerAdvice.class));
    }

    @Test void springRoutesEachExceptionToItsMostSpecificHandler() throws Exception {
        var resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        var error = new ApiError(Instant.now(), 409, "Conflict", "conflict", null, Map.of());
        Map<Exception, String> expected = new LinkedHashMap<>();
        expected.put(new MethodArgumentNotValidException(null, binding), "validation");
        expected.put(new UnsupportedInsuranceTypeException("x"), "unsupported");
        expected.put(new ProposalContactValidationException(Map.of()), "proposalContactValidation");
        // These two extend IllegalArgumentException but must not fall into invalidArgument.
        expected.put(new ProposalAdditionalValidationException(Map.of()), "proposalAdditionalValidation");
        expected.put(new ProposalAgeValidationException(Map.of()), "proposalAgeValidation");
        expected.put(new IllegalArgumentException("x"), "invalidArgument");
        expected.put(new PolicyPlannerException("x"), "vendor");
        expected.put(new BoltPeInsuranceException(error, null), "boltpe");
        expected.put(new IllegalStateException("x"), "unexpected");
        expected.put(new Exception("x"), "unexpected");

        for (var entry : expected.entrySet()) {
            Method method = resolver.resolveMethod(entry.getKey());
            assertNotNull(method, entry.getKey().getClass().getSimpleName());
            assertEquals(entry.getValue(), method.getName(), entry.getKey().getClass().getSimpleName());
        }
    }
}
