package com.boltpe.insurance.service;

import com.boltpe.insurance.client.HospitalCashQuoteBatch;
import com.boltpe.insurance.client.PolicyPlannerClient;
import com.boltpe.insurance.dto.request.HospitalCashInsuranceRequest;
import com.boltpe.insurance.vendor.request.HospitalCashVendorRequest;
import com.boltpe.insurance.vendor.response.HospitalCashVendorResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HospitalCashInsuranceServiceTest {
    @Test void shouldAcceptAllConfiguredCoverAmountsAndDurations() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build());
        var batch=new HospitalCashQuoteBatch(List.of(),List.of()); when(client.fetchHospitalCashQuoteBatch(any())).thenReturn(batch);
        for(Long cover:HospitalCashInsuranceRequest.ALLOWED_COVER_AMOUNTS) for(Integer days:HospitalCashInsuranceRequest.ALLOWED_DURATIONS)
            assertThat(service.fetch(new HospitalCashInsuranceRequest(26,cover,1,0,days))).isSameAs(batch);
    }
    @Test void shouldRejectUnsupportedCoverAmount() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build());
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,4000L,1,0,30))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid cover amount");
    }
    @Test void shouldRejectUnsupportedDuration() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build());
        assertThatThrownBy(()->service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,0,45))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid duration");
    }
    @Test void shouldSendExactVendorBody() {
        var client=mock(PolicyPlannerClient.class); var service=new HospitalCashInsuranceService(client,JsonMapper.builder().build()); var batch=new HospitalCashQuoteBatch(List.of(),List.of());
        when(client.fetchHospitalCashQuoteBatch(new HospitalCashVendorRequest(26,3000L,1,0,30))).thenReturn(batch);
        assertThat(service.fetch(new HospitalCashInsuranceRequest(26,3000L,1,0,30))).isSameAs(batch);
    }
    @Test void shouldBuildGeneraliResponseInCommonShape() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build());
        var f=new HospitalCashVendorResponse.Feature("1017","224","Daily Hospital Cash","24-Hour Minimum Stay",null);
        var q=new HospitalCashVendorResponse("Generali","Hospicash Plan","generali.jpg\n","b.pdf","o.png","{\"ICU Benefit\":\"2× home / 3× outside\"}",3000L,new BigDecimal("2078"),30,List.of(f),null);
        var r=service.build("req",new HospitalCashInsuranceRequest(26,3000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of()));
        assertThat(r.insuranceType()).isEqualTo("HOSPITAL_CASH"); assertThat(r.planCount()).isEqualTo(1);
        var p=r.plans().getFirst(); assertThat(p.company().companyId()).isEqualTo("1017"); assertThat(p.plan().planId()).isEqualTo("224");
        assertThat(p.company().logoUrl()).isEqualTo("generali.jpg"); assertThat(p.premium().payablePremium()).isEqualByComparingTo("2078");
        assertThat(p.coverage().coverAmount()).isEqualTo(3000L); assertThat(p.coverage().noOfAdults()).isEqualTo(1); assertThat(p.coverage().noOfChildren()).isZero();
        assertThat(p.includes()).hasSize(1); assertThat(p.excludes()).hasSize(1); assertThat(p.addons()).isEmpty(); assertThat(p.premiumOptions()).isEmpty();
        assertThat(p.otherDetails()).containsEntry("ICU Benefit","2× home / 3× outside");
    }
    @Test void shouldDropQuoteWithoutPremium() {
        var service=new HospitalCashInsuranceService(mock(PolicyPlannerClient.class),JsonMapper.builder().build());
        var q=new HospitalCashVendorResponse("X","Y",null,null,null,null,3000L,null,30,List.of(),null);
        var r=service.build("r",new HospitalCashInsuranceRequest(26,3000L,1,0,30),new HospitalCashQuoteBatch(List.of(q),List.of()));
        assertThat(r.planCount()).isZero();
    }
}
