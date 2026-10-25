package com.boltpe.insurance.service;

import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HospitalCashInsuranceRequest;
import com.boltpe.insurance.dto.response.HospitalCashInsuranceResponse;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import com.boltpe.insurance.validation.FamilyAgeValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class HospitalCashInsuranceService {

    private static final String INSURANCE_TYPE = "HOSPITAL_CASH";
    private static final String PREMIUM_FREQUENCY = "YEAR";

    private static final String INCLUDE = "INCLUDE";
    private static final String EXCLUDE = "EXCLUDE";
    private static final String ADDON = "ADDON";

    private final PolicyPlannerClient client;
    private final JsonMapper jsonMapper;
    private final InsurerLogoService logoService;

    public HospitalCashInsuranceService(
            PolicyPlannerClient client,
            JsonMapper jsonMapper,
            InsurerLogoService logoService) {

        this.client = client;
        this.jsonMapper = jsonMapper;
        this.logoService = logoService;
    }

    public HospitalCashQuoteBatch fetch(
            HospitalCashInsuranceRequest request) {

        validate(request);

        HospitalCashVendorRequest vendorRequest =
                new HospitalCashVendorRequest(
                        request.age(),
                        request.coverAmount(),
                        request.noOfAdults(),
                        request.noOfChildren(),
                        request.noOfDays()
                );

        return client.fetchHospitalCashQuoteBatch(vendorRequest);
    }

    static void validate(HospitalCashInsuranceRequest request) {

        if (request.coverAmount() == null
                || !HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS
                .contains(request.coverAmount())) {

            throw new IllegalArgumentException(
                    "Invalid cover amount. Please select ₹500, ₹1000, "
                            + "₹2000, ₹2500, ₹3000 or ₹5000."
            );
        }

        if (request.noOfDays() == null
                || !HospitalCashInsuranceRequest.ALLOWED_DURATIONS
                .contains(request.noOfDays())) {

            throw new IllegalArgumentException(
                    "Invalid duration. Please select 30, 60, 90 or 180 days."
            );
        }

        FamilyAgeValidator.validateAdult(request.age());

        if (request.noOfAdults() == null
                || request.noOfAdults() < 1) {

            throw new IllegalArgumentException(
                    "Kindly select at least 1 adult"
            );
        }

        if (request.noOfChildren() == null
                || request.noOfChildren() < 0) {

            throw new IllegalArgumentException(
                    "Invalid number of children"
            );
        }
    }

    public HospitalCashInsuranceResponse build(
            String requestId,
            HospitalCashInsuranceRequest request,
            HospitalCashQuoteBatch batch) {

        var plans = batch.quotes()
                .stream()
                .map(quote -> mapPlan(quote, request))
                .filter(Objects::nonNull)
                .toList();

        return new HospitalCashInsuranceResponse(
                requestId,
                INSURANCE_TYPE,
                plans.size(),
                plans
        );
    }

    private HospitalCashInsuranceResponse.Plan mapPlan(
            HospitalCashVendorResponse quote,
            HospitalCashInsuranceRequest request) {

        if (quote == null || quote.premium() == null) {
            return null;
        }

        var includes = features(quote, INCLUDE);
        var excludes = features(quote, EXCLUDE);
        var addons = features(quote, ADDON);

        List<HospitalCashInsuranceResponse.Feature> allFeatures =
                new ArrayList<>();

        allFeatures.addAll(includes);
        allFeatures.addAll(excludes);
        allFeatures.addAll(addons);

        String[] identifiers = ids(quote);

        return new HospitalCashInsuranceResponse.Plan(
                new HospitalCashInsuranceResponse.Company(
                        identifiers[0],
                        quote.company(),
                        logoService.resolveLogoUrl(identifiers[0], quote.logoUrl())
                ),

                new HospitalCashInsuranceResponse.PlanDetails(
                        identifiers[1],
                        quote.plan(),
                        clean(quote.brochureUrl()),
                        clean(quote.onePagerUrl())
                ),

                new HospitalCashInsuranceResponse.Coverage(
                        quote.coverAmount() != null
                                ? quote.coverAmount()
                                : request.coverAmount(),
                        null,
                        request.noOfAdults(),
                        request.noOfChildren(),
                        request.age(),
                        request.age()
                ),

                new HospitalCashInsuranceResponse.Premium(
                        null,
                        null,
                        quote.premium()
                ),

                List.of(),
                List.copyOf(allFeatures),
                Map.copyOf(otherDetails(quote.otherDetails())),
                includes.stream().limit(3).toList(),
                includes,
                excludes,
                addons,
                null,
                null,
                List.of(),
                true,
                PREMIUM_FREQUENCY
        );
    }

    private List<HospitalCashInsuranceResponse.Feature> features(
            HospitalCashVendorResponse quote,
            String group) {

        return VendorFeatureSupport.features(
                quote.features(),
                group,
                feature -> switch (group) {
                    case INCLUDE -> feature.includes();
                    case EXCLUDE -> feature.excludes();
                    default -> feature.addons();
                },
                (category, title) ->
                        new HospitalCashInsuranceResponse.Feature(
                                category,
                                title,
                                title
                        )
        );
    }

    private Map<String, Object> otherDetails(Object raw) {

        return VendorFeatureSupport.otherDetails(
                raw,
                jsonMapper,
                false
        );
    }

    private String[] ids(HospitalCashVendorResponse quote) {

        if (quote.features() == null) {
            return new String[]{null, null};
        }

        for (var feature : quote.features()) {

            if (feature == null) {
                continue;
            }

            if (feature.companyId() != null
                    || feature.planId() != null) {

                return new String[]{
                        feature.companyId(),
                        feature.planId()
                };
            }
        }

        return new String[]{null, null};
    }

    private static String clean(String value) {

        return value == null
                ? null
                : value.trim();
    }
}