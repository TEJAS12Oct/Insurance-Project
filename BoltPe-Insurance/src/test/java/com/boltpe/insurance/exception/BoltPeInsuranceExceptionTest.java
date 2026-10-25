package com.boltpe.insurance.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BoltPeInsuranceExceptionTest {
    private static ApiError apiError(String message) {
        return new ApiError(Instant.parse("2026-01-01T00:00:00Z"), 502, "Bad Gateway", message,
                "00000000-0000-0000-0000-000000000001", Map.of("coverAmount", "invalid"));
    }

    @Test void exposesApiErrorAndUsesItsMessage() {
        ApiError error = apiError("Vendor unavailable");
        Throwable cause = new IllegalStateException("timeout");

        BoltPeInsuranceException ex = new BoltPeInsuranceException(error, cause);

        assertThat(ex.apiError()).isSameAs(error);
        assertThat(ex.getMessage()).isEqualTo("Vendor unavailable");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test void allowsNullCause() {
        BoltPeInsuranceException ex = new BoltPeInsuranceException(apiError("Vendor unavailable"), null);

        assertThat(ex.getCause()).isNull();
        assertThat(ex.getMessage()).isEqualTo("Vendor unavailable");
    }

    @Test void allowsApiErrorWithNullMessage() {
        ApiError error = apiError(null);

        BoltPeInsuranceException ex = new BoltPeInsuranceException(error, null);

        assertThat(ex.getMessage()).isNull();
        assertThat(ex.apiError()).isSameAs(error);
    }

    @Test void rejectsNullApiError() {
        Throwable cause = new IllegalStateException("timeout");

        assertThatThrownBy(() -> new BoltPeInsuranceException(null, cause))
                .isInstanceOf(NullPointerException.class);
    }

    @Test void isUncheckedAndCanBeThrownAndCaught() {
        ApiError error = apiError("Vendor unavailable");

        assertThatThrownBy(() -> { throw new BoltPeInsuranceException(error, null); })
                .isInstanceOf(RuntimeException.class)
                .isInstanceOfSatisfying(BoltPeInsuranceException.class,
                        ex -> assertThat(ex.apiError().status()).isEqualTo(502));
    }
}
