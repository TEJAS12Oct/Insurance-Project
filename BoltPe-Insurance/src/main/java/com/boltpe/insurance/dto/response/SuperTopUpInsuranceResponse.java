package com.boltpe.insurance.dto.response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
public record SuperTopUpInsuranceResponse(String requestId, String insuranceType, int planCount, List<Plan> plans) {
    public record Plan(Company company, PlanDetails plan, Coverage coverage, Premium premium, List<Member> members,
                       List<Feature> features, Map<String,Object> otherDetails, List<Feature> keyFeatures,
                       List<Feature> includes, List<Feature> excludes, List<Feature> addons,
                       BigDecimal twoYearPremium, BigDecimal threeYearPremium, List<PremiumOption> premiumOptions,
                       boolean compareEligible, String premiumFrequency) {}
    public record Company(String companyId, String name, String logoUrl) {}
    public record PlanDetails(String planId, String name, String brochureUrl, String onePagerUrl) {}
    public record Coverage(Long coverAmount, Integer zone, Integer noOfAdults, Integer noOfChildren, Integer eldestActualAge, Integer eldestLookupAge) {}
    public record Premium(BigDecimal basePremium, BigDecimal discount, BigDecimal payablePremium) {}
    public record Member(String relation, Integer age) {}
    public record Feature(String category, String title, String description) {}
    public record PremiumOption(
            Integer years,
            BigDecimal basePremium,
            BigDecimal discount,
            BigDecimal payablePremium,
            BigDecimal deductible,
            BigDecimal premium,
            String type,
            BigDecimal value) {}
}
