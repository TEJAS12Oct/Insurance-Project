package com.boltpe.insurance.vendor.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SuperTopUpVendorResponse(
        String message, String error,
        @JsonAlias({"companyId","company_id"}) String companyId,
        @JsonAlias({"planId","plan_id"}) String planId,
        @JsonAlias({"companyName","company_name"}) String companyName,
        @JsonAlias({"logoUrl","logo_url"}) String logoUrl,
        @JsonAlias({"brochureUrl","brochure_url","brochure","broucher"}) String brochureUrl,
        @JsonAlias({"onePagerUrl","one_pager_url","onePager"}) String onePagerUrl,
        @JsonAlias({"planName","plan_name"}) String planName,
        @JsonAlias({"coverAmount","cover_amount"}) Long coverAmount,
        @JsonAlias({"adults","noOfAdults"}) Integer adults,
        @JsonAlias({"children","noOfChildren"}) Integer children,
        @JsonAlias({"eldestActual","eldestActualAge"}) Integer eldestActual,
        @JsonAlias({"eldestLookup","eldestLookupAge"}) Integer eldestLookup,
        List<Premium> premiums, List<Member> members,
        @JsonAlias({"otherDetails","other_details"}) Object otherDetails,
        List<Feature> features) {
    @JsonIgnoreProperties(ignoreUnknown = true) public record Premium(BigDecimal premium, String deductible) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Member(String label, Integer age) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Feature(String includes, String excludes, @JsonAlias({"addons","addOns"}) String addons) {}
}
