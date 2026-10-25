package com.boltpe.insurance.entity.proposalform;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InsuranceProposalRequestEntityTest {
    private static final Instant SENT_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2026-01-01T10:00:05Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T09:59:59Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T10:00:06Z");

    @Test void newEntityHasAllFieldsUnset() {
        InsuranceProposalRequestEntity entity = new InsuranceProposalRequestEntity();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getRequestId()).isNull();
        assertThat(entity.getInsuranceType()).isNull();
        assertThat(entity.getFormType()).isNull();
        assertThat(entity.getClientName()).isNull();
        assertThat(entity.getPolicyPlannerProposalId()).isNull();
        assertThat(entity.getBoltpeRequestJson()).isNull();
        assertThat(entity.getVendorRequestJson()).isNull();
        assertThat(entity.getVendorResponseJson()).isNull();
        assertThat(entity.getBoltpeResponseJson()).isNull();
        assertThat(entity.getVendorHttpStatus()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getSentAt()).isNull();
        assertThat(entity.getResponseReceivedAt()).isNull();
        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test void settersStoreValuesReturnedByGetters() {
        InsuranceProposalRequestEntity entity = populated();

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getRequestId()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(entity.getInsuranceType()).isEqualTo("PERSONAL_ACCIDENT");
        assertThat(entity.getFormType()).isEqualTo("INDIVIDUAL");
        assertThat(entity.getClientName()).isEqualTo("TEST_CLIENT");
        assertThat(entity.getPolicyPlannerProposalId()).isEqualTo("PP-PROPOSAL-1");
        assertThat(entity.getBoltpeRequestJson()).isEqualTo("{\"source\":\"boltpe\"}");
        assertThat(entity.getVendorRequestJson()).isEqualTo("{\"source\":\"vendor-request\"}");
        assertThat(entity.getVendorResponseJson()).isEqualTo("{\"source\":\"vendor-response\"}");
        assertThat(entity.getBoltpeResponseJson()).isEqualTo("{\"source\":\"boltpe-response\"}");
        assertThat(entity.getVendorHttpStatus()).isEqualTo(200);
        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getErrorMessage()).isEqualTo("none");
        assertThat(entity.getSentAt()).isEqualTo(SENT_AT);
        assertThat(entity.getResponseReceivedAt()).isEqualTo(RECEIVED_AT);
        assertThat(entity.getCreatedBy()).isEqualTo("system");
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getUpdatedBy()).isEqualTo("system-updater");
        assertThat(entity.getUpdatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test void optionalFieldsCanBeClearedBackToNull() {
        InsuranceProposalRequestEntity entity = populated();

        entity.setPolicyPlannerProposalId(null);
        entity.setVendorResponseJson(null);
        entity.setBoltpeResponseJson(null);
        entity.setVendorHttpStatus(null);
        entity.setErrorMessage(null);
        entity.setSentAt(null);
        entity.setResponseReceivedAt(null);

        assertThat(entity.getPolicyPlannerProposalId()).isNull();
        assertThat(entity.getVendorResponseJson()).isNull();
        assertThat(entity.getBoltpeResponseJson()).isNull();
        assertThat(entity.getVendorHttpStatus()).isNull();
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getSentAt()).isNull();
        assertThat(entity.getResponseReceivedAt()).isNull();
        assertThat(entity.getRequestId()).isEqualTo("00000000-0000-0000-0000-000000000001");
    }

    @Test void failureStateCanOverwriteEarlierValues() {
        InsuranceProposalRequestEntity entity = populated();

        entity.setStatus("FAILED");
        entity.setVendorHttpStatus(500);
        entity.setErrorMessage("Vendor timeout");
        entity.setUpdatedBy("retry-job");

        assertThat(entity.getStatus()).isEqualTo("FAILED");
        assertThat(entity.getVendorHttpStatus()).isEqualTo(500);
        assertThat(entity.getErrorMessage()).isEqualTo("Vendor timeout");
        assertThat(entity.getUpdatedBy()).isEqualTo("retry-job");
    }

    @Test void mapsToInsuranceProposalRequestTable() {
        assertThat(InsuranceProposalRequestEntity.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(InsuranceProposalRequestEntity.class.getAnnotation(Table.class).name()).isEqualTo("insurance_proposal_request");
    }

    @Test void idIsIdentityGenerated() throws NoSuchFieldException {
        var field = InsuranceProposalRequestEntity.class.getDeclaredField("id");

        assertThat(field.isAnnotationPresent(Id.class)).isTrue();
        assertThat(field.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test void requestIdIsRequiredUniqueAndImmutable() throws NoSuchFieldException {
        Column column = column("requestId");

        assertThat(column.name()).isEqualTo("request_id");
        assertThat(column.nullable()).isFalse();
        assertThat(column.unique()).isTrue();
        assertThat(column.updatable()).isFalse();
        assertThat(column.length()).isEqualTo(36);
    }

    @Test void requiredColumnsAreNotNullable() throws NoSuchFieldException {
        for (String field : new String[] {"insuranceType", "formType", "clientName", "boltpeRequestJson",
                "vendorRequestJson", "status", "createdBy", "createdAt", "updatedBy", "updatedAt"}) {
            assertThat(column(field).nullable()).as(field).isFalse();
        }
    }

    @Test void optionalColumnsAreNullable() throws NoSuchFieldException {
        for (String field : new String[] {"policyPlannerProposalId", "vendorResponseJson", "boltpeResponseJson",
                "vendorHttpStatus", "errorMessage", "sentAt", "responseReceivedAt"}) {
            assertThat(column(field).nullable()).as(field).isTrue();
        }
    }

    @Test void jsonPayloadAndErrorColumnsUseTextType() throws NoSuchFieldException {
        for (String field : new String[] {"boltpeRequestJson", "vendorRequestJson", "vendorResponseJson",
                "boltpeResponseJson", "errorMessage"}) {
            assertThat(column(field).columnDefinition()).as(field).isEqualTo("TEXT");
        }
    }

    @Test void stringColumnLengthsMatchSchema() throws NoSuchFieldException {
        assertThat(column("insuranceType").length()).isEqualTo(50);
        assertThat(column("formType").length()).isEqualTo(30);
        assertThat(column("clientName").length()).isEqualTo(50);
        assertThat(column("policyPlannerProposalId").length()).isEqualTo(100);
        assertThat(column("status").length()).isEqualTo(30);
        assertThat(column("createdBy").length()).isEqualTo(100);
        assertThat(column("updatedBy").length()).isEqualTo(100);
    }

    private static Column column(String fieldName) throws NoSuchFieldException {
        return InsuranceProposalRequestEntity.class.getDeclaredField(fieldName).getAnnotation(Column.class);
    }

    private static InsuranceProposalRequestEntity populated() {
        InsuranceProposalRequestEntity entity = new InsuranceProposalRequestEntity();
        entity.setId(1L);
        entity.setRequestId("00000000-0000-0000-0000-000000000001");
        entity.setInsuranceType("PERSONAL_ACCIDENT");
        entity.setFormType("INDIVIDUAL");
        entity.setClientName("TEST_CLIENT");
        entity.setPolicyPlannerProposalId("PP-PROPOSAL-1");
        entity.setBoltpeRequestJson("{\"source\":\"boltpe\"}");
        entity.setVendorRequestJson("{\"source\":\"vendor-request\"}");
        entity.setVendorResponseJson("{\"source\":\"vendor-response\"}");
        entity.setBoltpeResponseJson("{\"source\":\"boltpe-response\"}");
        entity.setVendorHttpStatus(200);
        entity.setStatus("SUCCESS");
        entity.setErrorMessage("none");
        entity.setSentAt(SENT_AT);
        entity.setResponseReceivedAt(RECEIVED_AT);
        entity.setCreatedBy("system");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedBy("system-updater");
        entity.setUpdatedAt(UPDATED_AT);
        return entity;
    }
}
