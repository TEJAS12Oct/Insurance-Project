package com.boltpe.insurance.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;

class InsuranceHealthControllerTest {
    @Test
    void healthReturnsExpectedSuccessfulPayload() {
        var result = new InsuranceHealthController().health();
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("success", true)
                .containsEntry("status", 200)
                .containsEntry("message", "BoltPe Insurance Service is working");
    }
}
