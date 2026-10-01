package com.boltpe.insurance.refresh;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import com.boltpe.insurance.entity.InsuranceVendorApiConfig;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.InsuranceVendorApiConfigRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsuranceVendorApiConfigServiceTest {
 @Test void replacesOnlyRequestedProduct() {
   var repo=mock(InsuranceVendorApiConfigRepository.class); when(repo.saveAll(anyList())).thenAnswer(i->i.getArgument(0));
   var service=new InsuranceVendorApiConfigService(repo);
   int count=service.replaceProductConfiguration(InsuranceType.SUPER_TOP_UP,List.of(new VendorApiRecord("/stu","1005","193")));
   assertEquals(1,count); verify(repo).deleteAllByInsuranceType("SUPER_TOP_UP");
   verify(repo).saveAll(argThat(rows->{ InsuranceVendorApiConfig r=((List<InsuranceVendorApiConfig>)rows).getFirst(); return "SUPER_TOP_UP".equals(r.getInsuranceType())&&"/stu".equals(r.getApiType())&&r.getRefreshedAt()!=null; }));
 }
 @Test void emptyDiscoveryIsNeverAllowedToDeleteOldData() {
   var repo=mock(InsuranceVendorApiConfigRepository.class); var service=new InsuranceVendorApiConfigService(repo);
   assertThrows(IllegalArgumentException.class,()->service.replaceProductConfiguration(InsuranceType.HEALTH,List.of())); verifyNoInteractions(repo);
 }
}
