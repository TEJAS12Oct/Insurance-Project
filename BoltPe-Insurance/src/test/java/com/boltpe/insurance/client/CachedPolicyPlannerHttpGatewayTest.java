package com.boltpe.insurance.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.boltpe.insurance.cache.InsuranceApiCacheService;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.exception.PolicyPlannerException;
import com.boltpe.insurance.refresh.VendorApiRecord;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class CachedPolicyPlannerHttpGatewayTest {
 @Test void buildsAllDiscoveryResponsesFromCache() {
   var c=mock(InsuranceApiCacheService.class); when(c.getOrLoad(any())).thenReturn(List.of(new VendorApiRecord("/api","1","2")));
   var g=new CachedPolicyPlannerHttpGateway(RestClient.builder().build(),c);
   assertEquals("/api",g.discoverHealthPlans("ignored").data().getFirst().api_type());
   assertEquals("/api",g.discoverSuperTopUpPlans("ignored").data().getFirst().api_type());
   assertEquals("/api",g.discoverPersonalAccidentPlans("ignored").data().getFirst().api_type());
   assertEquals("/api",g.discoverHospitalCashPlans("ignored").data().getFirst().api_type());
   verify(c).getOrLoad(InsuranceType.HEALTH); verify(c).getOrLoad(InsuranceType.HOSPITAL_CASH);
 }
 @Test void emptyCacheAndDbProducesControlledVendorConfigurationFailure() {
   var c=mock(InsuranceApiCacheService.class); when(c.getOrLoad(InsuranceType.HEALTH)).thenReturn(List.of());
   var g=new CachedPolicyPlannerHttpGateway(RestClient.builder().build(),c);
   assertThrows(PolicyPlannerException.class,()->g.discoverHealthPlans("ignored"));
 }
}
