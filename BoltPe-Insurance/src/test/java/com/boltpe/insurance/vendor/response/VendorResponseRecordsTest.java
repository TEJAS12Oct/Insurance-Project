package com.boltpe.insurance.vendor.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class VendorResponseRecordsTest {

    @Test void healthDiscoveryShouldDefaultNullDataAndCopyProvidedData() {
        assertThat(new HealthDiscoveryResponse(true, 0, "health", null).data()).isEmpty();

        var data = new ArrayList<>(List.of(new HealthPlanApiDescriptor("quote")));
        var response = new HealthDiscoveryResponse(true, 1, "health", data);
        data.clear();

        assertThat(response.data()).containsExactly(new HealthPlanApiDescriptor("quote"));
        assertThatThrownBy(() -> response.data().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void hospitalCashDiscoveryShouldDefaultNullDataAndCopyProvidedData() {
        assertThat(new HospitalCashDiscoveryResponse(false, null, null, null).data()).isEmpty();

        var data = new ArrayList<>(List.of(new HospitalCashPlanApiDescriptor("quote")));
        var response = new HospitalCashDiscoveryResponse(true, 1, "hospital_cash", data);
        data.clear();

        assertThat(response.data()).containsExactly(new HospitalCashPlanApiDescriptor("quote"));
        assertThatThrownBy(() -> response.data().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void personalAccidentDiscoveryShouldDefaultNullDataAndCopyProvidedData() {
        assertThat(new PersonalAccidentDiscoveryResponse(false, null, null, null).data()).isEmpty();

        var data = new ArrayList<>(List.of(new PersonalAccidentPlanApiDescriptor("quote")));
        var response = new PersonalAccidentDiscoveryResponse(true, 1, "personal_accident", data);
        data.clear();

        assertThat(response.data()).containsExactly(new PersonalAccidentPlanApiDescriptor("quote"));
        assertThatThrownBy(() -> response.data().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void superTopUpDiscoveryShouldDefaultNullDataAndCopyProvidedData() {
        assertThat(new SuperTopUpDiscoveryResponse(false, 0, null, null).data()).isEmpty();

        var data = new ArrayList<>(List.of(new SuperTopUpPlanApiDescriptor("quote")));
        var response = new SuperTopUpDiscoveryResponse(true, 1, "super_top_up", data);
        data.clear();

        assertThat(response.data()).containsExactly(new SuperTopUpPlanApiDescriptor("quote"));
        assertThatThrownBy(() -> response.data().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void healthFeatureLegacyConstructorShouldLeaveVendorSpecificFieldsNull() {
        var feature = new HealthVendorResponse.Feature("INCLUDE", "Hospitalisation", "Room Rent",
                "room_rent", "Single private room", "Covered");

        assertThat(feature.type()).isEqualTo("INCLUDE");
        assertThat(feature.category()).isEqualTo("Hospitalisation");
        assertThat(feature.title()).isEqualTo("Room Rent");
        assertThat(feature.name()).isEqualTo("room_rent");
        assertThat(feature.description()).isEqualTo("Single private room");
        assertThat(feature.value()).isEqualTo("Covered");
        assertThat(feature.companyId()).isNull();
        assertThat(feature.planId()).isNull();
        assertThat(feature.includes()).isNull();
        assertThat(feature.excludes()).isNull();
        assertThat(feature.addons()).isNull();
        assertThat(feature).isEqualTo(new HealthVendorResponse.Feature("INCLUDE", "Hospitalisation", "Room Rent",
                "room_rent", "Single private room", "Covered", null, null, null, null, null));
    }
}
