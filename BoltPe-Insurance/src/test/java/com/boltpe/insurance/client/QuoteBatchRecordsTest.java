package com.boltpe.insurance.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuoteBatchRecordsTest {

    @Test void personalAccidentBatchShouldDefaultNullListsToEmpty() {
        var batch = new PersonalAccidentQuoteBatch(null, null);
        assertThat(batch.quotes()).isEmpty();
        assertThat(batch.apiStatuses()).isEmpty();
    }

    @Test void personalAccidentBatchShouldDefensivelyCopyLists() {
        var quotes = new ArrayList<PersonalAccidentVendorResponse>();
        quotes.add(new PersonalAccidentVendorResponse("Synthetic Insurer", "PA Plan", null, null, null, null,
                1_000_000L, List.of(new PersonalAccidentVendorResponse.Premium("basic", BigDecimal.TEN)), null, null));
        var statuses = new ArrayList<PersonalAccidentQuoteBatch.ApiStatus>();
        statuses.add(new PersonalAccidentQuoteBatch.ApiStatus("https://vendor.example.test/pa", false, "C-1", "P-1", "timeout"));

        var batch = new PersonalAccidentQuoteBatch(quotes, statuses);
        quotes.clear();
        statuses.clear();

        assertThat(batch.quotes()).hasSize(1);
        assertThat(batch.apiStatuses()).singleElement().satisfies(s -> {
            assertThat(s.active()).isFalse();
            assertThat(s.reason()).isEqualTo("timeout");
        });
        assertThatThrownBy(() -> batch.quotes().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> batch.apiStatuses().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void hospitalCashBatchShouldDefaultNullListsToEmpty() {
        var batch = new HospitalCashQuoteBatch(null, null);
        assertThat(batch.quotes()).isEmpty();
        assertThat(batch.apiStatuses()).isEmpty();
    }

    @Test void hospitalCashBatchShouldDefensivelyCopyLists() {
        var quotes = new ArrayList<HospitalCashVendorResponse>();
        quotes.add(new HospitalCashVendorResponse("Synthetic Insurer", "Cash Plan", null, null, null, null,
                1000L, BigDecimal.ONE, 30, null, null));
        var statuses = new ArrayList<HospitalCashQuoteBatch.ApiStatus>();
        statuses.add(new HospitalCashQuoteBatch.ApiStatus("https://vendor.example.test/hc", true, "C-2", "P-2", null));

        var batch = new HospitalCashQuoteBatch(quotes, statuses);
        quotes.clear();
        statuses.clear();

        assertThat(batch.quotes()).hasSize(1);
        assertThat(batch.apiStatuses()).singleElement().satisfies(s -> assertThat(s.active()).isTrue());
        assertThatThrownBy(() -> batch.quotes().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }
}
