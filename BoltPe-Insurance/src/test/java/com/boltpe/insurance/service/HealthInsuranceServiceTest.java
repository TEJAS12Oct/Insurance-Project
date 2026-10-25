package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.boltpe.insurance.client.HealthQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HealthInsuranceRequest;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.vendor.response.HealthVendorResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class HealthInsuranceServiceTest {
    private static final InsurerLogoService LOGOS = new InsurerLogoService("https://s3.example.test", "test-bucket", "publictpap", "insurancelogo");
    private final PolicyPlannerClient client = mock(PolicyPlannerClient.class);
    private final HealthInsuranceService service = new HealthInsuranceService(client, new JsonMapper(), LOGOS);

    @Test void shouldMapRequestFieldsToVendorContract() {
        var request = new HealthInsuranceRequest(1000000L,"1",30,29,5,3,null,null);
        when(client.fetchHealthQuoteBatch(any())).thenReturn(new HealthQuoteBatch(List.of(), List.of()));
        service.fetch(request);
        verify(client).fetchHealthQuoteBatch(argThat(v -> v.coverAmount().equals(1000000L)
                && "1".equals(v.zone()) && v.age().equals(30) && v.sage().equals(29)
                && v.child1Age().equals(5) && v.child2Age().equals(3)));
    }

    @Test void shouldAcceptAllSupportedZones() {
        for (String zone : List.of("1","2","3")) {
            assertThatCode(() -> HealthInsuranceService.validate(
                    new HealthInsuranceRequest(1000000L,zone,30,null,null,null,null,null))).doesNotThrowAnyException();
        }
    }

    @Test void shouldRejectUnsupportedZone() {
        assertThatThrownBy(() -> HealthInsuranceService.validate(
                new HealthInsuranceRequest(1000000L,"4",30,null,null,null,null,null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid zone");
    }

    @Test void shouldRejectUnsupportedCoverAmount() {
        assertThatThrownBy(() -> HealthInsuranceService.validate(
                new HealthInsuranceRequest(1200000L,"1",30,null,null,null,null,null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cover amount");
    }

    @Test void shouldBuildBoltPeHealthPlanDirectlyInsideService() {
        var request = new HealthInsuranceRequest(1000000L,"1",30,29,5,null,null,null);
        var company = new HealthVendorResponse.Company("1004","SBI General","logo.png");
        var plan = new HealthVendorResponse.Plan("225","Health Edge","brochure.pdf","one.png","POST","{\"Room Rent\":\"No sub-limit\"}");
        var feature = new HealthVendorResponse.Feature("benefit","BENEFIT","Hospitalization",null,"Covered",null,
                "1004","225","In-Patient Hospitalization","Permanent exclusions","Optional addon");
        var premium2 = new HealthVendorResponse.PremiumOption(2,new BigDecimal("19000"),new BigDecimal("1000"),new BigDecimal("18000"));
        var premium3 = new HealthVendorResponse.PremiumOption(3,new BigDecimal("28000"),new BigDecimal("2000"),new BigDecimal("26000"));
        var vendor = new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,1,1,30,30,
                new BigDecimal("10000"),new BigDecimal("9980"),new BigDecimal("20"),
                List.of(new HealthVendorResponse.Member("self",30)), company, plan, List.of(feature),
                List.of(),List.of(),List.of(),List.of(premium2,premium3));
        var response = service.build("REQ-1",request,new HealthQuoteBatch(List.of(vendor),List.of()));
        assertThat(response.planCount()).isEqualTo(1);
        var result = response.plans().getFirst();
        assertThat(result.company().companyId()).isEqualTo("1004");
        assertThat(result.plan().planId()).isEqualTo("225");
        assertThat(result.members()).extracting("relation").containsExactly("SELF");
        assertThat(result.includes()).hasSize(1);
        assertThat(result.excludes()).hasSize(1);
        assertThat(result.addons()).hasSize(1);
        assertThat(result.twoYearPremium()).isEqualByComparingTo("18000");
        assertThat(result.threeYearPremium()).isEqualByComparingTo("26000");
        assertThat(result.otherDetails()).containsEntry("Room Rent","No sub-limit");
    }

    @Test void shouldBuildMembersFromRequestWhenVendorMembersAreEmpty() {
        var request = new HealthInsuranceRequest(1000000L,"1",30,29,5,3,null,null);
        var vendor = basicVendor("{}");
        var response = service.build("REQ",request,new HealthQuoteBatch(List.of(vendor),List.of()));
        assertThat(response.plans().getFirst().members()).extracting("relation")
                .containsExactly("SELF","SPOUSE","CHILD1","CHILD2");
    }

    @Test void shouldUseOtherDetailsAsFeaturesWhenVendorFeaturesAreEmpty() {
        var response = service.build("REQ",new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(basicVendor("{\"AYUSH\":\"Covered\"}")),List.of()));
        assertThat(response.plans().getFirst().features()).extracting("title").contains("AYUSH");
    }

    @Test void shouldPreserveInvalidOtherDetailsAsRawValue() {
        var response = service.build("REQ",new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(basicVendor("not-json")),List.of()));
        assertThat(response.plans().getFirst().otherDetails()).containsEntry("raw","not-json");
    }

    @Test void shouldRejectVendorResponseWithoutIds() {
        var vendor = new HealthVendorResponse(null,null,null,"Plan",1000000L,1,1,0,30,30,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,List.of(),null,null,List.of(),List.of(),List.of(),List.of(),List.of());
        assertThatThrownBy(() -> service.build("REQ",new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())))
                .isInstanceOf(PolicyPlannerException.class).hasMessageContaining("companyId or planId");
    }

    @Test void shouldRejectMissingCoverAmount() {
        assertThatThrownBy(() -> HealthInsuranceService.validate(
                new HealthInsuranceRequest(null,"1",30,null,null,null,null,null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cover amount");
    }

    @Test void shouldRejectMissingOrBlankZone() {
        for (String zone : java.util.Arrays.asList(null, "  ")) {
            assertThatThrownBy(() -> HealthInsuranceService.validate(
                    new HealthInsuranceRequest(1000000L,zone,30,null,null,null,null,null)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("zone is required");
        }
    }

    @Test void shouldNotCallVendorWhenRequestIsInvalid() {
        assertThatThrownBy(() -> service.fetch(new HealthInsuranceRequest(1000000L,null,30,null,null,null,null,null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test void shouldSkipNullVendorQuotes() {
        var response = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(java.util.Arrays.asList(null, basicVendor("{}")), List.of()));
        assertThat(response.planCount()).isEqualTo(1);
        assertThat(response.plans()).hasSize(1);
    }

    @Test void shouldRejectVendorResponseWithCompanyButWithoutPlanId() {
        var vendor = new HealthVendorResponse(null,"1004",null,"Plan",1000000L,1,1,0,30,30,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,List.of(),null,null,List.of(),List.of(),List.of(),List.of(),List.of());
        assertThatThrownBy(() -> service.build("REQ",new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())))
                .isInstanceOf(PolicyPlannerException.class).hasMessageContaining("companyId or planId");
    }

    @Test void shouldApplyDefaultsWhenVendorOmitsCompanyPlanAndCollections() {
        var vendor = new HealthVendorResponse(null,"1004","225",null,1000000L,1,1,0,30,30,
                null,null,null,null,null,null,null,null,null,null,null);
        var response = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,4,1),
                new HealthQuoteBatch(List.of(vendor),List.of()));
        var plan = response.plans().getFirst();
        assertThat(plan.company().companyId()).isEqualTo("1004");
        assertThat(plan.company().name()).isEqualTo("Unknown Company");
        assertThat(plan.company().logoUrl()).isEqualTo("https://s3.example.test/test-bucket/publictpap/insurancelogo/sbi_1004.svg");
        assertThat(plan.plan().planId()).isEqualTo("225");
        assertThat(plan.plan().name()).isEqualTo("Plan 225");
        assertThat(plan.plan().brochureUrl()).isNull();
        assertThat(plan.plan().onePagerUrl()).isNull();
        assertThat(plan.premium().basePremium()).isEqualByComparingTo("0");
        assertThat(plan.premium().discount()).isEqualByComparingTo("0");
        assertThat(plan.premium().payablePremium()).isEqualByComparingTo("0");
        assertThat(plan.features()).isEmpty();
        assertThat(plan.otherDetails()).isEmpty();
        assertThat(plan.includes()).isEmpty();
        assertThat(plan.excludes()).isEmpty();
        assertThat(plan.addons()).isEmpty();
        assertThat(plan.premiumOptions()).isEmpty();
        assertThat(plan.twoYearPremium()).isNull();
        assertThat(plan.threeYearPremium()).isNull();
        assertThat(plan.members()).extracting("relation").containsExactly("SELF","CHILD3","CHILD4");
    }

    @Test void shouldFallBackToNestedCompanyAndPlanWhenTopLevelValuesAreBlank() {
        var company = new HealthVendorResponse.Company("1005","  ","logo.png");
        var nestedPlan = new HealthVendorResponse.Plan("190","Nested Plan","brochure.pdf","one.pdf","POST","[1,2]");
        var vendor = new HealthVendorResponse(null," ",null," ",1000000L,1,1,0,30,30,
                BigDecimal.TEN,BigDecimal.TEN,BigDecimal.ZERO,List.of(),company,nestedPlan,
                List.of(),List.of(),List.of(),List.of(),List.of());
        var plan = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())).plans().getFirst();
        assertThat(plan.company().companyId()).isEqualTo("1005");
        assertThat(plan.company().name()).isEqualTo("Unknown Company");
        assertThat(plan.plan().planId()).isEqualTo("190");
        assertThat(plan.plan().name()).isEqualTo("Nested Plan");
        assertThat(plan.otherDetails()).containsEntry("raw", List.of(1, 2));
        assertThat(plan.features()).extracting("category","title","description")
                .containsExactly(tuple("PLAN_DETAIL","raw","[1, 2]"));
    }

    @Test void shouldUseNestedPlanIdAndDefaultPlanNameWhenNestedPlanHasNoName() {
        var nestedPlan = new HealthVendorResponse.Plan("191",null,null,null,"POST","  ");
        var vendor = new HealthVendorResponse(null,"1004",null,null,1000000L,1,1,0,30,30,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,List.of(),null,nestedPlan,
                List.of(),List.of(),List.of(),List.of(),List.of());
        var plan = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())).plans().getFirst();
        assertThat(plan.plan().planId()).isEqualTo("191");
        assertThat(plan.plan().name()).isEqualTo("Plan 191");
        assertThat(plan.otherDetails()).isEmpty();
    }

    @Test void shouldNormalizeDeduplicateAndSkipInvalidFeatures() {
        var ambulance = new HealthVendorResponse.Feature("benefit",null,null,"Ambulance"," ","Covered");
        var untitled = new HealthVendorResponse.Feature("benefit",null,null,null,"no title",null);
        var blankEmbedded = new HealthVendorResponse.Feature(null,null,null,null,null,null,
                "1004","225","  ",null," Road ambulance ");
        var cashless = new HealthVendorResponse.Feature(null,null,null,null,null,null,
                "1004","225"," Cashless ","Cosmetic surgery",null);
        var features = java.util.Arrays.asList(null, ambulance, untitled, ambulance, blankEmbedded, cashless);
        var includes = List.of(new HealthVendorResponse.Feature(null,"INCLUDE","Cashless",null,"Cashless",null));
        var vendor = new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,1,0,30,30,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,List.of(),null,null,
                features,includes,List.of(),List.of(),List.of());

        var plan = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())).plans().getFirst();

        assertThat(plan.features()).extracting("category","title","description")
                .containsExactly(tuple("benefit","Ambulance","Covered"));
        assertThat(plan.keyFeatures()).hasSize(1);
        assertThat(plan.includes()).extracting("category","title").containsExactly(tuple("INCLUDE","Cashless"));
        assertThat(plan.excludes()).extracting("title").containsExactly("Cosmetic surgery");
        assertThat(plan.addons()).extracting("title").containsExactly("Road ambulance");
    }

    @Test void shouldDefaultNullCompanyNameAndUseValueForMissingFeatureDescription() {
        var company = new HealthVendorResponse.Company("1004",null,"logo.png");
        var include = new HealthVendorResponse.Feature(null,null,"Pre-hospitalisation",null,null,"60 days");
        var vendor = new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,1,0,30,30,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,List.of(),company,null,
                List.of(),List.of(include),List.of(),List.of(),List.of());
        var plan = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())).plans().getFirst();
        assertThat(plan.company().name()).isEqualTo("Unknown Company");
        assertThat(plan.company().logoUrl()).isEqualTo("https://s3.example.test/test-bucket/publictpap/insurancelogo/sbi_1004.svg");
        assertThat(plan.includes()).extracting("category","title","description")
                .containsExactly(tuple(null,"Pre-hospitalisation","60 days"));
    }

    @Test void shouldIgnoreBlankOtherDetailKeysWhenDerivingFeatures() {
        var response = service.build("REQ",new HealthInsuranceRequest(1000000L,"1",30,null,null,null,null,null),
                new HealthQuoteBatch(List.of(basicVendor("{\" \":\"ignored\",\"Room Rent\":\"Single room\"}")),List.of()));
        assertThat(response.plans().getFirst().features()).extracting("title").containsExactly("Room Rent");
    }

    @Test void shouldNormalizeVendorMembersAndFilterInvalidPremiumOptions() {
        var members = java.util.Arrays.asList(null, new HealthVendorResponse.Member(null,40),
                new HealthVendorResponse.Member("spouse",38));
        var options = java.util.Arrays.asList(null,
                new HealthVendorResponse.PremiumOption(null,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE),
                new HealthVendorResponse.PremiumOption(0,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE),
                new HealthVendorResponse.PremiumOption(2,null,null,null));
        var vendor = new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,2,0,40,40,
                BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,members,null,null,
                List.of(),List.of(),List.of(),List.of(),options);

        var plan = service.build("REQ", new HealthInsuranceRequest(1000000L,"1",40,38,null,null,null,null),
                new HealthQuoteBatch(List.of(vendor),List.of())).plans().getFirst();

        assertThat(plan.members()).extracting("relation","age")
                .containsExactly(tuple("UNKNOWN",40), tuple("SPOUSE",38));
        assertThat(plan.premiumOptions()).hasSize(1);
        var option = plan.premiumOptions().getFirst();
        assertThat(option.years()).isEqualTo(2);
        assertThat(option.basePremium()).isEqualByComparingTo("0");
        assertThat(option.discount()).isEqualByComparingTo("0");
        assertThat(option.payablePremium()).isEqualByComparingTo("0");
        assertThat(plan.twoYearPremium()).isEqualByComparingTo("0");
        assertThat(plan.threeYearPremium()).isNull();
    }

    private HealthVendorResponse basicVendor(String details) {
        return new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,1,0,30,30,
                new BigDecimal("9980"),new BigDecimal("9980"),BigDecimal.ZERO,List.of(),
                new HealthVendorResponse.Company("1004","SBI","logo"),
                new HealthVendorResponse.Plan("225","Health Edge","b","o","POST",details),
                List.of(),List.of(),List.of(),List.of(),List.of());
    }
}
