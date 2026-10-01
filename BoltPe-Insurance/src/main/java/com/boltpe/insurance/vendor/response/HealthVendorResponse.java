package com.boltpe.insurance.vendor.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HealthVendorResponse(
        String message,
        @JsonAlias({"companyId", "company_id"}) String companyId,
        @JsonAlias({"planId", "plan_id"}) String planId,
        @JsonAlias({"planName", "plan_name", "name"}) String planName,
        @JsonAlias({"coverAmount", "cover_amount", "sumInsured", "sum_insured"}) Long coverAmount,
        Integer zone,
        Integer noOfAdults,
        Integer noOfChildren,
        Integer eldestActualAge,
        Integer eldestLookupAge,
        @JsonAlias({"totalBasePremium", "total_base_premium", "basePremium", "base_premium"}) BigDecimal totalBasePremium,
        @JsonAlias({"totalPayablePremium", "total_payable_premium", "payablePremium", "payable_premium", "premium"}) BigDecimal totalPayablePremium,
        @JsonAlias({"totalDiscount", "total_discount", "discount"}) BigDecimal totalDiscount,
        List<Member> members,
        Company company,
        Plan plan,
        List<Feature> features,
        @JsonAlias({"includes", "included", "inclusions"}) List<Feature> includes,
        @JsonAlias({"excludes", "excluded", "exclusions"}) List<Feature> excludes,
        @JsonAlias({"addons", "addOns", "add_ons", "add-ons"}) List<Feature> addons,
        @JsonAlias({"premiumOptions", "premium_options", "tenureOptions", "tenure_options"}) List<PremiumOption> premiumOptions) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Member(String label, Integer age) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Company(
            @JsonAlias({"companyId", "company_id", "id"}) String companyId,
            @JsonAlias({"companyName", "company_name", "name"}) String companyName,
            @JsonAlias({"logoUrl", "logo_url", "logo"}) String logoUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Plan(
            @JsonAlias({"planId", "plan_id", "id"}) String planId,
            @JsonAlias({"planName", "plan_name", "name"}) String planName,
            @JsonAlias({"brochureUrl", "brochure_url", "brochure", "broucher"}) String brochureUrl,
            @JsonAlias({"onePagerUrl", "one_pager_url", "onePager"}) String onePagerUrl,
            @JsonAlias({"apiType", "api_type"}) String apiType,
            @JsonAlias({"otherDetails", "other_details"}) String otherDetails) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(
            @JsonAlias({"type", "featureType", "feature_type"}) String type,
            @JsonAlias({"category", "featureCategory", "feature_category"}) String category,
            @JsonAlias({"title", "featureTitle", "feature_title"}) String title,
            @JsonAlias({"name", "featureName", "feature_name"}) String name,
            @JsonAlias({"description", "details", "detail"}) String description,
            @JsonAlias({"value", "featureValue", "feature_value"}) String value,
            @JsonAlias({"company_id", "companyId"}) String companyId,
            @JsonAlias({"plan_id", "planId"}) String planId,
            String includes,
            String excludes,
            @JsonAlias({"addons", "addOns", "add_ons", "add-ons"}) String addons) {

        // Keeps compatibility with the older normalized feature structure used by tests/code.
        public Feature(String type, String category, String title, String name, String description, String value) {
            this(type, category, title, name, description, value, null, null, null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PremiumOption(
            @JsonAlias({"years", "year", "tenure", "policyYears", "policy_years"}) Integer years,
            @JsonAlias({"basePremium", "base_premium"}) BigDecimal basePremium,
            @JsonAlias({"discount", "totalDiscount", "total_discount"}) BigDecimal discount,
            @JsonAlias({"payablePremium", "payable_premium", "premium", "totalPayablePremium", "total_payable_premium"}) BigDecimal payablePremium) {}
}
