package com.boltpe.insurance.service;

import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.client.SuperTopUpQuoteBatch;
import com.boltpe.insurance.dto.request.SuperTopUpInsuranceRequest;
import com.boltpe.insurance.dto.response.SuperTopUpInsuranceResponse;
import com.boltpe.insurance.vendor.request.SuperTopUpVendorRequest;
import com.boltpe.insurance.vendor.response.SuperTopUpVendorResponse;
import com.boltpe.insurance.validation.FamilyAgeValidator;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SuperTopUpInsuranceService {
    private final PolicyPlannerClient client;
    private final JsonMapper jsonMapper;
    private final InsurerLogoService logoService;
    public SuperTopUpInsuranceService(PolicyPlannerClient client, JsonMapper jsonMapper, InsurerLogoService logoService) { this.client=client; this.jsonMapper=jsonMapper; this.logoService=logoService; }

    public SuperTopUpQuoteBatch fetch(SuperTopUpInsuranceRequest r) {
        if (r.coverAmount() == null || !SuperTopUpInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(r.coverAmount())) throw new IllegalArgumentException("Invalid cover amount. Please select a cover amount from the available coverage options.");
        FamilyAgeValidator.validate(r.age(), r.spouseAge(), r.child1Age(), r.child2Age(), r.child3Age(), r.child4Age());
        return client.fetchSuperTopUpQuoteBatch(new SuperTopUpVendorRequest(r.coverAmount(),r.age(),r.spouseAge(),r.child1Age(),r.child2Age(),r.child3Age(),r.child4Age()));
    }

    public SuperTopUpInsuranceResponse build(String requestId, SuperTopUpInsuranceRequest request, SuperTopUpQuoteBatch batch) {
        List<SuperTopUpInsuranceResponse.Plan> plans = batch.quotes().stream()
                .flatMap(q -> mapPlans(q).stream())
                .toList();
        return new SuperTopUpInsuranceResponse(requestId, "SUPER_TOP_UP", plans.size(), plans);
    }

    /**
     * A Super Top-Up vendor plan can return multiple deductible/premium combinations.
     * BoltPe exposes each combination as a separate plan so the frontend never has
     * to choose between multiple deductible prices inside one plan.
     */
    private List<SuperTopUpInsuranceResponse.Plan> mapPlans(SuperTopUpVendorResponse q) {
        if (q.premiums() == null || q.premiums().isEmpty()) return List.of();
        return q.premiums().stream()
                .filter(Objects::nonNull)
                .map(p -> mapPlan(q, p))
                .toList();
    }

    private SuperTopUpInsuranceResponse.Plan mapPlan(SuperTopUpVendorResponse q, SuperTopUpVendorResponse.Premium vendorPremium) {
        List<SuperTopUpInsuranceResponse.Feature> includes=features(q,"INCLUDE");
        List<SuperTopUpInsuranceResponse.Feature> excludes=features(q,"EXCLUDE");
        List<SuperTopUpInsuranceResponse.Feature> addons=features(q,"ADDON");
        List<SuperTopUpInsuranceResponse.Feature> all=new ArrayList<>(); all.addAll(includes); all.addAll(excludes); all.addAll(addons);

        // Standard premiumOptions contract: all eight keys are always serialized.
        // Super Top-Up populates deductible + premium; Health-only fields stay null.
        SuperTopUpInsuranceResponse.PremiumOption option = new SuperTopUpInsuranceResponse.PremiumOption(
                null, null, null, null, decimal(vendorPremium.deductible()), vendorPremium.premium(), null, null);

        return new SuperTopUpInsuranceResponse.Plan(
                new SuperTopUpInsuranceResponse.Company(q.companyId(),q.companyName(),logoService.resolveLogoUrl(q.companyId(),q.logoUrl())),
                new SuperTopUpInsuranceResponse.PlanDetails(q.planId(),q.planName(),q.brochureUrl(),q.onePagerUrl()),
                new SuperTopUpInsuranceResponse.Coverage(q.coverAmount(),null,q.adults(),q.children(),q.eldestActual(),q.eldestLookup()),
                new SuperTopUpInsuranceResponse.Premium(null,null,null),
                q.members()==null?List.of():q.members().stream().map(m->new SuperTopUpInsuranceResponse.Member(m.label()==null?null:m.label().toUpperCase(Locale.ROOT),m.age())).toList(),
                List.copyOf(all), otherDetails(q.otherDetails()), includes.stream().limit(3).toList(), includes, excludes, addons,
                null,null,List.of(option),true,"YEAR");
    }
    private List<SuperTopUpInsuranceResponse.Feature> features(SuperTopUpVendorResponse quote, String group) {
        return VendorFeatureSupport.features(quote.features(), group,
                feature -> switch (group) {
                    case "INCLUDE" -> feature.includes();
                    case "EXCLUDE" -> feature.excludes();
                    default -> feature.addons();
                },
                (category, title) -> new SuperTopUpInsuranceResponse.Feature(category, title, title));
    }

    private Map<String, Object> otherDetails(Object raw) {
        return VendorFeatureSupport.otherDetails(raw, jsonMapper, true);
    }
    private BigDecimal decimal(String s){try{return s==null?null:new BigDecimal(s);}catch(NumberFormatException e){return null;}}
}
