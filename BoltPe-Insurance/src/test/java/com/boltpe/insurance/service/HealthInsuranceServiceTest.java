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
    private final PolicyPlannerClient client = mock(PolicyPlannerClient.class);
    private final HealthInsuranceService service = new HealthInsuranceService(client, new JsonMapper());

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

    private HealthVendorResponse basicVendor(String details) {
        return new HealthVendorResponse(null,"1004","225","Health Edge",1000000L,1,1,0,30,30,
                new BigDecimal("9980"),new BigDecimal("9980"),BigDecimal.ZERO,List.of(),
                new HealthVendorResponse.Company("1004","SBI","logo"),
                new HealthVendorResponse.Plan("225","Health Edge","b","o","POST",details),
                List.of(),List.of(),List.of(),List.of(),List.of());
    }
}
