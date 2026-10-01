package com.boltpe.insurance.exception;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.Map;
import org.junit.jupiter.api.Test;
class GlobalExceptionHandlerTest {
    @Test void policyPlannerExceptionShouldPreserveMessageAndCause() {
        RuntimeException cause = new RuntimeException("x");
        PolicyPlannerException ex = new PolicyPlannerException("vendor failed", cause);
        assertThat(ex).hasMessage("vendor failed").hasCause(cause);
    }
    @Test void unsupportedExceptionShouldPreserveMessage() {
        assertThat(new UnsupportedInsuranceTypeException("not supported")).hasMessage("not supported");
    }
    @Test void proposalContactValidationExceptionPreservesFieldErrors() {
        var ex = new com.boltpe.insurance.exception.proposalform.ProposalContactValidationException(
                Map.of("pincode", "invalid"));
        assertThat(ex.getFieldErrors()).containsEntry("pincode", "invalid");
        assertThat(ex).hasMessage("Proposal contact and identity validation failed");
    }
    @Test void proposalAdditionalValidationExceptionPreservesFieldErrors() {
        var ex = new com.boltpe.insurance.exception.proposalform.ProposalAdditionalValidationException(
                Map.of("adultCount", "adultCount must be between 1 and 4"));
        assertThat(ex.getFieldErrors()).containsEntry("adultCount", "adultCount must be between 1 and 4");
        assertThat(ex).hasMessage("Proposal validation failed");
    }
}

