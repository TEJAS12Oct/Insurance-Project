package com.boltpe.insurance.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;

/** Shared vendor feature normalization and additional-detail parsing. */
final class VendorFeatureSupport {
    private static final String DETAILS_KEY = "details";

    private VendorFeatureSupport() { }

    static <T, R> List<R> features(List<T> items, String group,
            Function<T, String> extractor, FeatureFactory<R> factory) {
        if (items == null) {
            return List.of();
        }
        Map<String, R> unique = new LinkedHashMap<>();
        for (T item : items) {
            if (item == null) {
                continue;
            }
            String value = extractor.apply(item);
            if (value != null && !value.isBlank()) {
                String title = value.trim();
                unique.putIfAbsent(title, factory.create(group, title));
            }
        }
        return List.copyOf(unique.values());
    }

    @FunctionalInterface
    interface FeatureFactory<R> {
        R create(String category, String title);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> otherDetails(Object raw, JsonMapper mapper, boolean keepUnknown) {
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> details) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            details.forEach((key, value) -> mapped.put(String.valueOf(key), value));
            return mapped;
        }
        if (raw instanceof String details) {
            if (details.isBlank()) {
                return keepUnknown ? Map.of(DETAILS_KEY, details) : Map.of();
            }
            try {
                return mapper.readValue(details, Map.class);
            } catch (RuntimeException ignored) {
                return Map.of(DETAILS_KEY, details);
            }
        }
        return keepUnknown ? Map.of(DETAILS_KEY, raw) : Map.of();
    }
}
