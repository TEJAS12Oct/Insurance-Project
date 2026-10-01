package com.boltpe.insurance.service;

import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HospitalCashInsuranceRequest;
import com.boltpe.insurance.dto.response.HospitalCashInsuranceResponse;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import com.boltpe.insurance.validation.FamilyAgeValidator;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class HospitalCashInsuranceService {
    private final PolicyPlannerClient client;
    private final JsonMapper jsonMapper;
    public HospitalCashInsuranceService(PolicyPlannerClient client, JsonMapper jsonMapper) { this.client=client; this.jsonMapper=jsonMapper; }

    public HospitalCashQuoteBatch fetch(HospitalCashInsuranceRequest r) {
        validate(r);
        return client.fetchHospitalCashQuoteBatch(new HospitalCashVendorRequest(r.age(),r.coverAmount(),r.noOfAdults(),r.noOfChildren(),r.noOfDays()));
    }
    static void validate(HospitalCashInsuranceRequest r) {
        if(r.coverAmount()==null || !HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS.contains(r.coverAmount()))
            throw new IllegalArgumentException("Invalid cover amount. Please select ₹500, ₹1000, ₹2000, ₹2500, ₹3000 or ₹5000.");
        if(r.noOfDays()==null || !HospitalCashInsuranceRequest.ALLOWED_DURATIONS.contains(r.noOfDays()))
            throw new IllegalArgumentException("Invalid duration. Please select 30, 60, 90 or 180 days.");
        FamilyAgeValidator.validateAdult(r.age());
        if(r.noOfAdults()==null || r.noOfAdults()<1) throw new IllegalArgumentException("Kindly select at least 1 adult");
        if(r.noOfChildren()==null || r.noOfChildren()<0) throw new IllegalArgumentException("Invalid number of children");
    }
    public HospitalCashInsuranceResponse build(String requestId, HospitalCashInsuranceRequest request, HospitalCashQuoteBatch batch) {
        var plans=batch.quotes().stream().map(q->mapPlan(q,request)).filter(Objects::nonNull).toList();
        return new HospitalCashInsuranceResponse(requestId,"HOSPITAL_CASH",plans.size(),plans);
    }
    private HospitalCashInsuranceResponse.Plan mapPlan(HospitalCashVendorResponse q, HospitalCashInsuranceRequest r) {
        if(q==null || q.premium()==null) return null;
        var includes=features(q,"INCLUDE"); var excludes=features(q,"EXCLUDE"); var addons=features(q,"ADDON");
        List<HospitalCashInsuranceResponse.Feature> all=new ArrayList<>(); all.addAll(includes); all.addAll(excludes); all.addAll(addons);
        String[] ids=ids(q);
        return new HospitalCashInsuranceResponse.Plan(
            new HospitalCashInsuranceResponse.Company(ids[0],q.company(),clean(q.logoUrl())),
            new HospitalCashInsuranceResponse.PlanDetails(ids[1],q.plan(),clean(q.brochureUrl()),clean(q.onePagerUrl())),
            new HospitalCashInsuranceResponse.Coverage(q.coverAmount()!=null?q.coverAmount():r.coverAmount(),null,r.noOfAdults(),r.noOfChildren(),r.age(),r.age()),
            new HospitalCashInsuranceResponse.Premium(null,null,q.premium()), List.of(), List.copyOf(all), Map.copyOf(otherDetails(q.otherDetails())),
            includes.stream().limit(3).toList(), includes, excludes, addons, null,null,List.of(),true,"YEAR");
    }
    private List<HospitalCashInsuranceResponse.Feature> features(HospitalCashVendorResponse q,String group){
        if(q.features()==null) return List.of(); LinkedHashMap<String,HospitalCashInsuranceResponse.Feature> out=new LinkedHashMap<>();
        for(var f:q.features()){ if(f==null) continue; String v=switch(group){case "INCLUDE"->f.includes(); case "EXCLUDE"->f.excludes(); default->f.addons();};
            if(v!=null&&!v.isBlank()){v=v.trim(); out.putIfAbsent(v,new HospitalCashInsuranceResponse.Feature(group,v,v));}}
        return List.copyOf(out.values());
    }
    private String[] ids(HospitalCashVendorResponse q){ if(q.features()!=null) for(var f:q.features()) if(f!=null&&(f.companyId()!=null||f.planId()!=null)) return new String[]{f.companyId(),f.planId()}; return new String[]{null,null}; }
    @SuppressWarnings("unchecked") private Map<String,Object> otherDetails(Object raw){ if(raw==null) return Map.of(); if(raw instanceof Map<?,?> m){Map<String,Object> x=new LinkedHashMap<>();m.forEach((k,v)->x.put(String.valueOf(k),v));return x;} if(raw instanceof String s&&!s.isBlank()) try{return jsonMapper.readValue(s,Map.class);}catch(RuntimeException ignored){return Map.of("details",s);} return Map.of(); }
    private static String clean(String s){return s==null?null:s.trim();}
}
