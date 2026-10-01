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
    public SuperTopUpInsuranceService(PolicyPlannerClient client, JsonMapper jsonMapper) { this.client=client; this.jsonMapper=jsonMapper; }

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
                new SuperTopUpInsuranceResponse.Company(q.companyId(),q.companyName(),q.logoUrl()),
                new SuperTopUpInsuranceResponse.PlanDetails(q.planId(),q.planName(),q.brochureUrl(),q.onePagerUrl()),
                new SuperTopUpInsuranceResponse.Coverage(q.coverAmount(),null,q.adults(),q.children(),q.eldestActual(),q.eldestLookup()),
                new SuperTopUpInsuranceResponse.Premium(null,null,null),
                q.members()==null?List.of():q.members().stream().map(m->new SuperTopUpInsuranceResponse.Member(m.label()==null?null:m.label().toUpperCase(Locale.ROOT),m.age())).toList(),
                List.copyOf(all), otherDetails(q.otherDetails()), includes.stream().limit(3).toList(), includes, excludes, addons,
                null,null,List.of(option),true,"YEAR");
    }
    private List<SuperTopUpInsuranceResponse.Feature> features(SuperTopUpVendorResponse q,String group) {
        if(q.features()==null) return List.of(); LinkedHashMap<String,SuperTopUpInsuranceResponse.Feature> out=new LinkedHashMap<>();
        for(var f:q.features()){ if(f==null)continue; String v=switch(group){case "INCLUDE"->f.includes();case "EXCLUDE"->f.excludes();default->f.addons();};
            if(v!=null&&!v.isBlank()){v=v.trim();out.putIfAbsent(v,new SuperTopUpInsuranceResponse.Feature(group,v,v));}}
        return List.copyOf(out.values());
    }
    @SuppressWarnings("unchecked") private Map<String,Object> otherDetails(Object raw){
        if(raw==null)return Map.of(); if(raw instanceof Map<?,?> m){Map<String,Object> r=new LinkedHashMap<>();m.forEach((k,v)->r.put(String.valueOf(k),v));return r;}
        if(raw instanceof String s&&!s.isBlank()){try{return jsonMapper.readValue(s,Map.class);}catch(RuntimeException ignored){return Map.of("details",s);}}
        return Map.of("details",raw);
    }
    private BigDecimal decimal(String s){try{return s==null?null:new BigDecimal(s);}catch(NumberFormatException e){return null;}}
}
