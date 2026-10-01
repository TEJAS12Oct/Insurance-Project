package com.boltpe.insurance.vendor.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HospitalCashVendorResponse(
        String company, String plan,
        @JsonAlias({"logoUrl","logo_url"}) String logoUrl,
        @JsonAlias({"brochureUrl","brochure_url"}) String brochureUrl,
        @JsonAlias({"onePagerUrl","one_pager_url"}) String onePagerUrl,
        @JsonAlias({"otherDetails","other_details"}) Object otherDetails,
        @JsonAlias({"coverAmount","cover_amount"}) Long coverAmount,
        BigDecimal premium,
        @JsonAlias({"noOfDays","no_of_days"}) Integer noOfDays,
        List<Feature> features,
        String error) {
    public record Feature(@JsonAlias({"company_id","companyId"}) String companyId,
                          @JsonAlias({"plan_id","planId"}) String planId,
                          String includes, String excludes,
                          @JsonAlias({"addons","addOns"}) String addons) {}
}
