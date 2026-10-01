package com.boltpe.insurance.service;

import com.boltpe.insurance.client.HealthQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HealthInsuranceRequest;
import com.boltpe.insurance.dto.response.HealthInsuranceResponse;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.util.HashingUtils;
import com.boltpe.insurance.vendor.request.HealthVendorRequest;
import com.boltpe.insurance.validation.FamilyAgeValidator;
import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Health-specific business service. It owns validation, vendor request creation and
 * conversion of Policy Planner Health responses into the common BoltPe response.
 * This keeps Health consistent with Super Top-Up, Personal Accident and Hospital Cash.
 */
@Service
public class HealthInsuranceService {
    private final PolicyPlannerClient client;
    private final JsonMapper jsonMapper;

    public HealthInsuranceService(PolicyPlannerClient client, JsonMapper jsonMapper) {
        this.client = client;
        this.jsonMapper = jsonMapper;
    }

    public HealthQuoteBatch fetch(HealthInsuranceRequest request) {
        validate(request);
        return client.fetchHealthQuoteBatch(toVendorRequest(request));
    }

    public HealthInsuranceResponse build(String requestId, HealthInsuranceRequest request, HealthQuoteBatch batch) {
        List<HealthInsuranceResponse.Plan> plans = batch.quotes().stream()
                .map(quote -> mapPlan(quote, request))
                .filter(Objects::nonNull)
                .toList();
        return new HealthInsuranceResponse(requestId, "HEALTH", plans.size(), plans);
    }

    static void validate(HealthInsuranceRequest request) {
        if (request.coverAmount() == null || !HealthInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(request.coverAmount())) {
            throw new IllegalArgumentException(
                    "Invalid cover amount. Please select a cover amount from the available Health Insurance coverage options.");
        }
        FamilyAgeValidator.validate(request.age(), request.spouseAge(), request.child1Age(), request.child2Age(), request.child3Age(), request.child4Age());
        if (request.zone() == null || request.zone().isBlank()) {
            throw new IllegalArgumentException("zone is required");
        }
        if (!(request.zone().equals("1") || request.zone().equals("2") || request.zone().equals("3"))) {
            throw new IllegalArgumentException(
                    "Invalid zone. Health Insurance supports only Zone 1, Zone 2 and Zone 3. Please select a supported city.");
        }
    }

    static HealthVendorRequest toVendorRequest(HealthInsuranceRequest request) {
        return new HealthVendorRequest(request.coverAmount(), request.zone(), request.age(), request.spouseAge(),
                request.child1Age(), request.child2Age(), request.child3Age(), request.child4Age());
    }

    private HealthInsuranceResponse.Plan mapPlan(HealthVendorResponse source, HealthInsuranceRequest request) {
        if (source == null) return null;

        String companyId = firstNonBlank(source.companyId(), source.company() == null ? null : source.company().companyId());
        String planId = firstNonBlank(source.planId(), source.plan() == null ? null : source.plan().planId());
        if (companyId == null || planId == null) {
            throw new PolicyPlannerException("Policy Planner response is missing companyId or planId");
        }

        String companyName = source.company() == null
                ? "Unknown Company"
                : defaultString(source.company().companyName(), "Unknown Company");
        String planName = firstNonBlank(source.planName(), source.plan() == null ? null : source.plan().planName());
        if (planName == null) planName = "Plan " + planId;

        Map<String, Object> otherDetails = readOtherDetails(source.plan() == null ? null : source.plan().otherDetails());
        List<HealthInsuranceResponse.Feature> features = mapFeatures(source.features());
        if (features.isEmpty()) features = featuresFromOtherDetails(otherDetails);

        List<HealthInsuranceResponse.Feature> includes = mergeFeatures(
                mapFeatures(source.includes()), embeddedFeatures(source.features(), FeatureGroup.INCLUDE));
        List<HealthInsuranceResponse.Feature> excludes = mergeFeatures(
                mapFeatures(source.excludes()), embeddedFeatures(source.features(), FeatureGroup.EXCLUDE));
        List<HealthInsuranceResponse.Feature> addons = mergeFeatures(
                mapFeatures(source.addons()), embeddedFeatures(source.features(), FeatureGroup.ADDON));

        List<HealthInsuranceResponse.PremiumOption> premiumOptions = mapPremiumOptions(source.premiumOptions());
        List<HealthInsuranceResponse.Member> members = mapMembers(source.members());
        if (members.isEmpty()) members = buildMembers(request);

        return new HealthInsuranceResponse.Plan(
                new HealthInsuranceResponse.Company(companyId, companyName,
                        source.company() == null ? null : source.company().logoUrl()),
                new HealthInsuranceResponse.PlanDetails(planId, planName,
                        source.plan() == null ? null : source.plan().brochureUrl(),
                        source.plan() == null ? null : source.plan().onePagerUrl()),
                new HealthInsuranceResponse.Coverage(source.coverAmount(), source.zone(), source.noOfAdults(),
                        source.noOfChildren(), source.eldestActualAge(), source.eldestLookupAge()),
                new HealthInsuranceResponse.Premium(defaultMoney(source.totalBasePremium()),
                        defaultMoney(source.totalDiscount()), defaultMoney(source.totalPayablePremium())),
                members,
                features,
                otherDetails,
                features.stream().limit(3).toList(),
                includes,
                excludes,
                addons,
                premiumForYears(premiumOptions, 2),
                premiumForYears(premiumOptions, 3),
                premiumOptions,
                true,
                "YEAR");
    }

    private enum FeatureGroup { INCLUDE, EXCLUDE, ADDON }

    private List<HealthInsuranceResponse.Feature> mapFeatures(List<HealthVendorResponse.Feature> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        Map<String, HealthInsuranceResponse.Feature> unique = new LinkedHashMap<>();
        for (HealthVendorResponse.Feature item : raw) {
            if (item == null) continue;
            String category = firstNonBlank(item.category(), item.type());
            String title = firstNonBlank(item.title(), item.name(), item.value());
            if (title == null) continue;
            String description = defaultString(item.description(), item.value());
            String key = HashingUtils.sha256(defaultString(category, "") + "|" + title + "|" + defaultString(description, ""));
            unique.putIfAbsent(key, new HealthInsuranceResponse.Feature(category, title, description));
        }
        return List.copyOf(unique.values());
    }

    private List<HealthInsuranceResponse.Feature> embeddedFeatures(List<HealthVendorResponse.Feature> raw, FeatureGroup group) {
        if (raw == null || raw.isEmpty()) return List.of();
        Map<String, HealthInsuranceResponse.Feature> unique = new LinkedHashMap<>();
        for (HealthVendorResponse.Feature item : raw) {
            if (item == null) continue;
            String text = switch (group) {
                case INCLUDE -> item.includes();
                case EXCLUDE -> item.excludes();
                case ADDON -> item.addons();
            };
            if (text == null || text.isBlank()) continue;
            text = text.trim();
            String category = switch (group) {
                case INCLUDE -> "INCLUDE";
                case EXCLUDE -> "EXCLUDE";
                case ADDON -> "ADDON";
            };
            unique.putIfAbsent(HashingUtils.sha256(category + "|" + text),
                    new HealthInsuranceResponse.Feature(category, text, text));
        }
        return List.copyOf(unique.values());
    }

    private List<HealthInsuranceResponse.Feature> mergeFeatures(
            List<HealthInsuranceResponse.Feature> first, List<HealthInsuranceResponse.Feature> second) {
        Map<String, HealthInsuranceResponse.Feature> unique = new LinkedHashMap<>();
        if (first != null) for (var item : first) unique.putIfAbsent(featureKey(item), item);
        if (second != null) for (var item : second) unique.putIfAbsent(featureKey(item), item);
        return List.copyOf(unique.values());
    }

    private String featureKey(HealthInsuranceResponse.Feature item) {
        return HashingUtils.sha256(defaultString(item.category(), "") + "|" +
                defaultString(item.title(), "") + "|" + defaultString(item.description(), ""));
    }

    private List<HealthInsuranceResponse.Feature> featuresFromOtherDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) return List.of();
        List<HealthInsuranceResponse.Feature> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) continue;
            result.add(new HealthInsuranceResponse.Feature("PLAN_DETAIL", entry.getKey(), String.valueOf(entry.getValue())));
        }
        return List.copyOf(result);
    }

    private List<HealthInsuranceResponse.Member> mapMembers(List<HealthVendorResponse.Member> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream().filter(Objects::nonNull)
                .map(m -> new HealthInsuranceResponse.Member(
                        m.label() == null ? "UNKNOWN" : m.label().toUpperCase(), m.age()))
                .toList();
    }

    private List<HealthInsuranceResponse.Member> buildMembers(HealthInsuranceRequest request) {
        List<HealthInsuranceResponse.Member> members = new ArrayList<>();
        members.add(new HealthInsuranceResponse.Member("SELF", request.age()));
        addMember(members, "SPOUSE", request.spouseAge());
        addMember(members, "CHILD1", request.child1Age());
        addMember(members, "CHILD2", request.child2Age());
        addMember(members, "CHILD3", request.child3Age());
        addMember(members, "CHILD4", request.child4Age());
        return List.copyOf(members);
    }

    private static void addMember(List<HealthInsuranceResponse.Member> members, String relation, Integer age) {
        if (age != null) members.add(new HealthInsuranceResponse.Member(relation, age));
    }

    private List<HealthInsuranceResponse.PremiumOption> mapPremiumOptions(List<HealthVendorResponse.PremiumOption> raw) {
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .filter(item -> item != null && item.years() != null && item.years() > 0)
                .map(item -> new HealthInsuranceResponse.PremiumOption(
                        item.years(), defaultMoney(item.basePremium()), defaultMoney(item.discount()),
                        defaultMoney(item.payablePremium()), null, null, null, null))
                .toList();
    }

    private BigDecimal premiumForYears(List<HealthInsuranceResponse.PremiumOption> options, int years) {
        return options.stream()
                .filter(option -> option.years() != null && option.years() == years)
                .map(HealthInsuranceResponse.PremiumOption::payablePremium)
                .findFirst().orElse(null);
    }

    private Map<String, Object> readOtherDetails(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            Object value = jsonMapper.readValue(raw, Object.class);
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> result = new LinkedHashMap<>();
                map.forEach((k, v) -> result.put(String.valueOf(k), v));
                return Map.copyOf(result);
            }
            return Map.of("raw", value);
        } catch (JacksonException ex) {
            return Map.of("raw", raw);
        }
    }

    private static BigDecimal defaultMoney(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static String defaultString(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }
}
