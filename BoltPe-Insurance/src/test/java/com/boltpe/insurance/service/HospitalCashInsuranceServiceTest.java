package com.boltpe.insurance.service;

import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HospitalCashInsuranceRequest;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HospitalCashInsuranceServiceTest {
    private static final InsurerLogoService LOGOS = new InsurerLogoService("https://s3.example.test", "test-bucket", "publictpap", "insurancelogo");
    @Test void shouldAcceptAllConfiguredCoverAmountsAndDurations() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS);
        var batch=new HospitalCashQuoteBatch(List.of(),List.of()); when(client.fetchHospitalCashQuoteBatch(any())).thenReturn(batch);
        for(Long cover:HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS) for(Integer days:HospitalCashInsuranceRequest.ALLOWED_DURATIONS)
            assertThat(service.fetch(new HospitalCashInsuranceRequest(26,cover,1,0,days))).isSameAs(batch);
    }
    @Test void shouldRejectUnsupportedCoverAmount() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,4000L,1,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cover amount");
    }
    @Test void shouldRejectUnsupportedDuration() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,0,45))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid duration");
    }
    @Test void shouldSendExactVendorBody() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS); var batch=new HospitalCashQuoteBatch(List.of(),List.of());
        when(client.fetchHospitalCashQuoteBatch(new HospitalCashVendorRequest(26,3000L,1,0,30))).thenReturn(batch);
        assertThat(service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,0,30))).isSameAs(batch);
    }
    @Test void shouldBuildGeneraliResponseInCommonShape() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var f=new HospitalCashVendorResponse.Feature("1017","224","Daily Hospital Cash","24-Hour Minimum Stay",null);
        var q=new HospitalCashVendorResponse("Generali","Hospicash Plan","generali.jpg\n","b.pdf","o.png","{\"ICU Benefit\":\"2× home / 3× outside\"}",3000L,new BigDecimal("2078"),30,List.of(f),null);
        var r=service.build("req",new HospitalCashInsuranceRequest(26,3000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of()));
        assertThat(r.insuranceType()).isEqualTo("HOSPITAL_CASH"); assertThat(r.planCount()).isEqualTo(1);
        var p=r.plans().getFirst(); assertThat(p.company().companyId()).isEqualTo("1017"); assertThat(p.plan().planId()).isEqualTo("224");
        assertThat(p.company().logoUrl()).isEqualTo("https://s3.example.test/test-bucket/publictpap/insurancelogo/generali_1017.jpg"); assertThat(p.premium().payablePremium()).isEqualByComparingTo("2078");
        assertThat(p.coverage().coverAmount()).isEqualTo(3000L); assertThat(p.coverage().noOfAdults()).isEqualTo(1); assertThat(p.coverage().noOfChildren()).isZero();
        assertThat(p.includes()).hasSize(1); assertThat(p.excludes()).hasSize(1); assertThat(p.addons()).isEmpty(); assertThat(p.premiumOptions()).isEmpty();
        assertThat(p.otherDetails()).containsEntry("ICU Benefit","2× home / 3× outside");
    }
    @Test void shouldPreserveOtherDetailsParsingBehavior() {
        var service = new HospitalCashInsuranceService(mock(PolicyPlannerClient.class), JsonMapper.builder().build(), LOGOS);
        var request = new HospitalCashInsuranceRequest(26, 3000L, 1, 0, 30);

        for (var raw : List.of(Map.of("waitingPeriod", "30 days"), "invalid-json", " ")) {
            var quote = new HospitalCashVendorResponse("Company", "Plan", null, null, null,
                    raw, 3000L, BigDecimal.valueOf(100), 30, List.of(), null);
            var response = service.build("request-id", request, new HospitalCashQuoteBatch(List.of(quote), List.of()));
            var details = response.plans().getFirst().otherDetails();
            if (raw instanceof Map) {
                assertThat(details).containsEntry("waitingPeriod", "30 days");
            } else if ("invalid-json".equals(raw)) {
                assertThat(details).containsEntry("details", "invalid-json");
            } else {
                assertThat(details).isEmpty();
            }
        }
    }

    @Test void shouldDropQuoteWithoutPremium() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var q=new HospitalCashVendorResponse("X","Y",null,null,null,null,3000L,null,30,List.of(),null);
        var r=service.build("r",new HospitalCashInsuranceRequest(26,3000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of()));
        assertThat(r.planCount()).isZero();
    }

    @Test void shouldRejectNullCoverAmountAndNullDuration() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,null,1,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cover amount");
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,0,null))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid duration");
        verifyNoInteractions(client);
    }
    @Test void shouldRejectMissingOrInvalidAge() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(null,3000L,1,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("age is required");
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(17,3000L,1,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("age must be at least 18");
        verifyNoInteractions(client);
    }
    @Test void shouldRejectMissingOrZeroAdults() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,null,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("Kindly select at least 1 adult");
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,0,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("Kindly select at least 1 adult");
        verifyNoInteractions(client);
    }
    @Test void shouldRejectMissingOrNegativeChildren() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS);
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,null,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid number of children");
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,-1,30))).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid number of children");
        verifyNoInteractions(client);
    }
    @Test void shouldAcceptChildrenAndMultipleAdults() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build(), LOGOS); var batch=new HospitalCashQuoteBatch(List.of(),List.of());
        when(client.fetchHospitalCashQuoteBatch(new HospitalCashVendorRequest(40,1000L,2,2,90))).thenReturn(batch);
        assertThat(service.fetch(new HospitalCashInsuranceRequest(40,1000L,2,2,90))).isSameAs(batch);
    }
    @Test void shouldFallBackToRequestCoverAmountAndNullIdsWhenVendorOmitsThem() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var q=new HospitalCashVendorResponse("Synthetic Insurer","Cash Plan",null,null,null,null,null,new BigDecimal("999"),60,null,null);
        var r=service.build("r",new HospitalCashInsuranceRequest(30,2000L,2,1,60),new HospitalCashQuoteBatch(List.of(q),List.of()));
        var p=r.plans().getFirst();
        assertThat(p.coverage().coverAmount()).isEqualTo(2000L);
        assertThat(p.coverage().noOfAdults()).isEqualTo(2); assertThat(p.coverage().noOfChildren()).isEqualTo(1);
        assertThat(p.company().companyId()).isNull(); assertThat(p.plan().planId()).isNull();
        assertThat(p.company().logoUrl()).isNull(); assertThat(p.plan().brochureUrl()).isNull(); assertThat(p.plan().onePagerUrl()).isNull();
        assertThat(p.includes()).isEmpty(); assertThat(p.excludes()).isEmpty(); assertThat(p.addons()).isEmpty(); assertThat(p.features()).isEmpty();
        assertThat(p.otherDetails()).isEmpty();
    }
    @Test void shouldSkipNullFeaturesAndResolveIdsFromFirstFeatureCarryingAnyId() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var features=new java.util.ArrayList<HospitalCashVendorResponse.Feature>();
        features.add(null);
        features.add(new HospitalCashVendorResponse.Feature(null,null,"Daily Cash",null,null));
        features.add(new HospitalCashVendorResponse.Feature(null,"PLAN-7",null,"Day Care",null));
        features.add(new HospitalCashVendorResponse.Feature("CO-2","PLAN-8",null,null,"ICU Cash"));
        var q=new HospitalCashVendorResponse("Synthetic Insurer","Cash Plan"," logo.png ",null,null,42,1000L,new BigDecimal("500"),30,features,null);
        var p=service.build("r",new HospitalCashInsuranceRequest(26,1000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of())).plans().getFirst();
        assertThat(p.company().companyId()).isNull(); assertThat(p.plan().planId()).isEqualTo("PLAN-7");
        assertThat(p.company().logoUrl()).isNull();
        assertThat(p.includes()).extracting(f->f.title()).containsExactly("Daily Cash");
        assertThat(p.excludes()).extracting(f->f.title()).containsExactly("Day Care");
        assertThat(p.addons()).extracting(f->f.title()).containsExactly("ICU Cash");
        // Hospital Cash drops non-string, non-map other details
        assertThat(p.otherDetails()).isEmpty();
    }
    @Test void shouldReturnNullIdsWhenFeaturesCarryNoIds() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var features=new java.util.ArrayList<HospitalCashVendorResponse.Feature>();
        features.add(null);
        features.add(new HospitalCashVendorResponse.Feature(null,null,"Daily Cash",null,null));
        var q=new HospitalCashVendorResponse("Synthetic Insurer","Cash Plan",null,null,null,null,1000L,new BigDecimal("500"),30,features,null);
        var p=service.build("r",new HospitalCashInsuranceRequest(26,1000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of())).plans().getFirst();
        assertThat(p.company().companyId()).isNull(); assertThat(p.plan().planId()).isNull();
        assertThat(p.includes()).hasSize(1);
    }
    @Test void shouldUseCompanyIdWhenPlanIdMissing() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var q=new HospitalCashVendorResponse("Synthetic Insurer","Cash Plan",null,null,null,null,1000L,new BigDecimal("500"),30,
                List.of(new HospitalCashVendorResponse.Feature("CO-3",null,null,null,null)),null);
        var p=service.build("r",new HospitalCashInsuranceRequest(26,1000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of())).plans().getFirst();
        assertThat(p.company().companyId()).isEqualTo("CO-3"); assertThat(p.plan().planId()).isNull();
    }

    // HospitalCashQuoteBatch rejects null quotes, so a mocked batch verifies the mapper's own null guard.
    @Test void shouldSkipNullQuoteInBatch() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build(), LOGOS);
        var batch=mock(HospitalCashQuoteBatch.class); when(batch.quotes()).thenReturn(Arrays.asList((HospitalCashVendorResponse) null));
        var r=service.build("r",new HospitalCashInsuranceRequest(26,3000L,1,0,30),batch);
        assertThat(r.planCount()).isZero(); assertThat(r.plans()).isEmpty();
    }
}
