package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class VendorFeatureSupportTest {
    private record Feature(String category, String title) { }

    private final JsonMapper mapper = JsonMapper.builder().build();

    private static List<Feature> features(List<String> items) {
        return VendorFeatureSupport.features(items, "INCLUDE", Function.identity(), Feature::new);
    }

    @Test void featuresReturnsEmptyListForNullItems() {
        assertThat(features(null)).isEmpty();
    }

    @Test void featuresReturnsEmptyListForEmptyItems() {
        assertThat(features(List.of())).isEmpty();
    }

    @Test void featuresBuildsOneFeaturePerTitleWithGivenGroup() {
        assertThat(features(List.of("Room rent", "ICU cover")))
                .containsExactly(new Feature("INCLUDE", "Room rent"), new Feature("INCLUDE", "ICU cover"));
    }

    @Test void featuresSkipsNullItemsAndNullOrBlankTitles() {
        List<String> items = Arrays.asList(null, "", "   ", "Ambulance");

        assertThat(features(items)).containsExactly(new Feature("INCLUDE", "Ambulance"));
    }

    @Test void featuresTrimsTitles() {
        assertThat(features(List.of("  Day care  "))).containsExactly(new Feature("INCLUDE", "Day care"));
    }

    @Test void featuresKeepsFirstOccurrenceOfDuplicateTitlesInOrder() {
        assertThat(features(List.of("B", "A", " B ", "A", "C")))
                .extracting(Feature::title)
                .containsExactly("B", "A", "C");
    }

    @Test void featuresUsesExtractorOnEachItem() {
        record VendorItem(String name) { }
        List<VendorItem> items = Arrays.asList(new VendorItem("Maternity"), null, new VendorItem(null));

        List<Feature> result = VendorFeatureSupport.features(items, "ADDON", VendorItem::name, Feature::new);

        assertThat(result).containsExactly(new Feature("ADDON", "Maternity"));
    }

    @Test void featuresResultIsImmutable() {
        List<Feature> result = features(List.of("Room rent"));
        Feature extra = new Feature("INCLUDE", "Other");

        assertThatThrownBy(() -> result.add(extra)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void featuresIsNotAffectedByLaterChangesToInput() {
        List<String> items = new ArrayList<>(List.of("Room rent"));
        List<Feature> result = features(items);

        items.add("ICU cover");

        assertThat(result).containsExactly(new Feature("INCLUDE", "Room rent"));
    }

    @Test void otherDetailsReturnsEmptyMapForNull() {
        assertThat(VendorFeatureSupport.otherDetails(null, mapper, true)).isEmpty();
        assertThat(VendorFeatureSupport.otherDetails(null, mapper, false)).isEmpty();
    }

    @Test void otherDetailsCopiesMapAndStringifiesKeysInOrder() {
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put(2, "two");
        raw.put("waitingPeriod", 30);
        raw.put(null, "none");

        Map<String, Object> result = VendorFeatureSupport.otherDetails(raw, mapper, false);

        assertThat(result).containsExactly(
                Map.entry("2", "two"), Map.entry("waitingPeriod", 30), Map.entry("null", "none"));
        raw.put("later", true);
        assertThat(result).doesNotContainKey("later");
    }

    @Test void otherDetailsReturnsEmptyMapForEmptyMap() {
        assertThat(VendorFeatureSupport.otherDetails(Map.of(), mapper, true)).isEmpty();
    }

    @Test void otherDetailsParsesJsonObjectString() {
        Map<String, Object> result = VendorFeatureSupport.otherDetails(
                "{\"copay\":\"10%\",\"waitingPeriod\":30}", mapper, false);

        assertThat(result).containsEntry("copay", "10%").containsEntry("waitingPeriod", 30);
    }

    @Test void otherDetailsWrapsInvalidJsonStringUnderDetailsKey() {
        assertThat(VendorFeatureSupport.otherDetails("not json", mapper, false))
                .containsExactly(Map.entry("details", "not json"));
    }

    @Test void otherDetailsWrapsNonObjectJsonStringUnderDetailsKey() {
        assertThat(VendorFeatureSupport.otherDetails("[1,2]", mapper, true))
                .containsExactly(Map.entry("details", "[1,2]"));
    }

    @Test void otherDetailsKeepsBlankStringOnlyWhenKeepingUnknown() {
        assertThat(VendorFeatureSupport.otherDetails("  ", mapper, true))
                .containsExactly(Map.entry("details", "  "));
        assertThat(VendorFeatureSupport.otherDetails("", mapper, false)).isEmpty();
    }

    @Test void otherDetailsKeepsUnsupportedTypeOnlyWhenKeepingUnknown() {
        assertThat(VendorFeatureSupport.otherDetails(42, mapper, true))
                .containsExactly(Map.entry("details", 42));
        assertThat(VendorFeatureSupport.otherDetails(List.of("a"), mapper, false)).isEmpty();
    }
}
