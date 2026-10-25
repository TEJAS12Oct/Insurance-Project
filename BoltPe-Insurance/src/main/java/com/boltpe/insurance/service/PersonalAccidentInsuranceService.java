package com.boltpe.insurance.service;

import com.boltpe.insurance.client.PersonalAccidentQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.PersonalAccidentInsuranceRequest;
import com.boltpe.insurance.dto.response.PersonalAccidentInsuranceResponse;
import com.boltpe.insurance.vendor.request.PersonalAccidentVendorRequest;
import com.boltpe.insurance.vendor.response.PersonalAccidentVendorResponse;
import com.boltpe.insurance.validation.FamilyAgeValidator;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PersonalAccidentInsuranceService {
    private final PolicyPlannerClient client;
    private final JsonMapper jsonMapper;
    private final InsurerLogoService logoService;

    public PersonalAccidentInsuranceService(PolicyPlannerClient client, JsonMapper jsonMapper, InsurerLogoService logoService) {
        this.client = client;
        this.jsonMapper = jsonMapper;
        this.logoService = logoService;
    }

    public PersonalAccidentQuoteBatch fetch(PersonalAccidentInsuranceRequest request) {
        if (request.coverAmount() == null || !PersonalAccidentInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(request.coverAmount())) {
            throw new IllegalArgumentException(
                    "Invalid cover amount. Please select a cover amount from the available Personal Accident coverage options.");
        }
        FamilyAgeValidator.validateAdult(request.age());
        if (request.category() == null) {
            throw new IllegalArgumentException("category is required");
        }
        if (request.category() < 1 || request.category() > 3) {
            throw new IllegalArgumentException("category must be one of: 1, 2, 3");
        }
        return client.fetchPersonalAccidentQuoteBatch(
                new PersonalAccidentVendorRequest(
                        request.coverAmount(),
                        request.category(),
                        request.age()
                )
        );
    }

    public PersonalAccidentInsuranceResponse build(String requestId, PersonalAccidentQuoteBatch batch) {
        // Policy Planner shows one card/plan per insurer plan. Multiple PA premium types
        // (basic, PTD+PPD, TTD, etc.) are pricing options of that same plan, not separate plans.
        List<PersonalAccidentInsuranceResponse.Plan> plans = batch.quotes().stream()
                .map(this::mapPlan)
                .filter(Objects::nonNull)
                .toList();
        return new PersonalAccidentInsuranceResponse(requestId, "PERSONAL_ACCIDENT", plans.size(), plans);
    }

    private PersonalAccidentInsuranceResponse.Plan mapPlan(PersonalAccidentVendorResponse quote) {
        if (quote.premiums() == null || quote.premiums().isEmpty()) return null;

        // PA vendor data can contain repeated entries for the same premium type.
        // Keep the first value for each type so BoltPe mirrors the single PA option
        // shown by Policy Planner (for example: basic=507, PTD+PPD=826, TTD=1475).
        // Do not merge/accumulate repeated values of the same premium type.
        LinkedHashMap<String, PersonalAccidentVendorResponse.Premium> premiumsByType = new LinkedHashMap<>();
        for (PersonalAccidentVendorResponse.Premium premium : quote.premiums()) {
            if (premium == null || premium.type() == null || premium.type().isBlank() || premium.value() == null) {
                continue;
            }
            String normalizedType = premium.type().trim().toLowerCase(Locale.ROOT);
            premiumsByType.putIfAbsent(normalizedType,
                    new PersonalAccidentVendorResponse.Premium(premium.type().trim(), premium.value()));
        }
        List<PersonalAccidentVendorResponse.Premium> vendorPremiums = List.copyOf(premiumsByType.values());
        if (vendorPremiums.isEmpty()) return null;

        var includes = features(quote, "INCLUDE");
        var excludes = features(quote, "EXCLUDE");
        var addons = features(quote, "ADDON");
        List<PersonalAccidentInsuranceResponse.Feature> all = new ArrayList<>();
        all.addAll(includes);
        all.addAll(excludes);
        all.addAll(addons);

        Map<String, Object> details = new LinkedHashMap<>(otherDetails(quote.otherDetails()));
        String[] ids = ids(quote);

        List<PersonalAccidentInsuranceResponse.PremiumOption> premiumOptions = vendorPremiums.stream()
                .map(p -> new PersonalAccidentInsuranceResponse.PremiumOption(
                        null, null, null, null, null, null, p.type(), p.value()))
                .toList();

        // The normal/default Buy Now price is the "basic" premium. If a vendor does not
        // return a basic option, use its first available premium as the display premium.
        PersonalAccidentVendorResponse.Premium displayPremium = vendorPremiums.stream()
                .filter(p -> p.type() != null && "basic".equalsIgnoreCase(p.type().trim()))
                .findFirst()
                .orElse(vendorPremiums.getFirst());

        return new PersonalAccidentInsuranceResponse.Plan(
                new PersonalAccidentInsuranceResponse.Company(ids[0], quote.company(), logoService.resolveLogoUrl(ids[0], quote.logoUrl())),
                new PersonalAccidentInsuranceResponse.PlanDetails(ids[1], quote.plan(), quote.brochureUrl(), quote.onePagerUrl()),
                new PersonalAccidentInsuranceResponse.Coverage(quote.coverAmount(), null, null, null, null, null),
                new PersonalAccidentInsuranceResponse.Premium(null, null, displayPremium.value()),
                List.of(), List.copyOf(all), Map.copyOf(details), includes.stream().limit(3).toList(),
                includes, excludes, addons, null, null, premiumOptions, true, "YEAR");
    }

    private String[] ids(PersonalAccidentVendorResponse q) {
        if (q.features() != null) {
            for (var f : q.features()) {
                if (f != null && (f.companyId() != null || f.planId() != null)) {
                    return new String[]{f.companyId(), f.planId()};
                }
            }
        }
        return new String[]{null, null};
    }

    private List<PersonalAccidentInsuranceResponse.Feature> features(PersonalAccidentVendorResponse q, String group) {
        if (q.features() == null) return List.of();
        LinkedHashMap<String, PersonalAccidentInsuranceResponse.Feature> out = new LinkedHashMap<>();
        for (var f : q.features()) {
            if (f == null) continue;
            String value = switch (group) {
                case "INCLUDE" -> f.includes();
                case "EXCLUDE" -> f.excludes();
                default -> f.addons();
            };
            if (value != null && !value.isBlank()) {
                value = value.trim();
                out.putIfAbsent(value, new PersonalAccidentInsuranceResponse.Feature(group, value, value));
            }
        }
        return List.copyOf(out.values());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> otherDetails(Object raw) {
        if (raw == null) return Map.of();
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return jsonMapper.readValue(s, Map.class);
            } catch (RuntimeException ignored) {
                return Map.of("details", s);
            }
        }
        return Map.of("details", raw);
    }
}
