package com.boltpe.insurance.refresh;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.boltpe.insurance.entity.*;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsuranceCronAuditServiceTest {
 @Test void tracksStartFinishAndProductDetails() {
   var er=mock(InsuranceCronExecutionRepository.class); var pr=mock(InsuranceCronProductExecutionRepository.class);
   when(er.save(any())).thenAnswer(i->i.getArgument(0)); when(pr.save(any())).thenAnswer(i->i.getArgument(0));
   var s=new InsuranceCronAuditService(er,pr); var e=s.start("exec-1");
   assertEquals("RUNNING",e.getStatus()); assertEquals(4,e.getTotalProducts());
   var p=s.startProduct("exec-1",InsuranceType.HEALTH); var snap=new DiscoverySnapshot(List.of(new VendorApiRecord("/h","1","2")),"{raw}");
   s.finishProduct(p,snap,1,new ProductRefreshResult(true,true,"SUCCESS",null));
   assertEquals("SUCCESS",p.getStatus()); assertTrue(p.isDbRefreshed()); assertEquals("{raw}",p.getResponseJson());
   s.finish(e,4,0,false,null); assertEquals("SUCCESS",e.getStatus()); assertNotNull(e.getCompletedAt());
 }
 @Test void derivesPartialAndFailedStatuses() {
   var er=mock(InsuranceCronExecutionRepository.class); var pr=mock(InsuranceCronProductExecutionRepository.class); when(er.save(any())).thenAnswer(i->i.getArgument(0));
   var s=new InsuranceCronAuditService(er,pr); var partial=s.start("p"); s.finish(partial,3,1,false,"stu down"); assertEquals("PARTIAL_SUCCESS",partial.getStatus());
   var failed=s.start("f"); s.finish(failed,0,4,false,"all down"); assertEquals("FAILED",failed.getStatus());
 }
 @Test void redisOnlyPartialWithNoFailedProductsIsPartialSuccess() {
   var er=mock(InsuranceCronExecutionRepository.class); var pr=mock(InsuranceCronProductExecutionRepository.class); when(er.save(any())).thenAnswer(i->i.getArgument(0));
   var s=new InsuranceCronAuditService(er,pr); var e=s.start("p2");
   s.finish(e,3,0,true,"redis down");
   assertEquals("PARTIAL_SUCCESS",e.getStatus()); assertEquals(3,e.getSuccessfulProducts()); assertEquals(0,e.getFailedProducts()); assertEquals("redis down",e.getErrorMessage());
 }
 @Test void finishProductWithoutSnapshotRecordsZeroReceivedAndNoResponse() {
   var er=mock(InsuranceCronExecutionRepository.class); var pr=mock(InsuranceCronProductExecutionRepository.class); when(pr.save(any())).thenAnswer(i->i.getArgument(0));
   var s=new InsuranceCronAuditService(er,pr); var p=s.startProduct("exec-2",InsuranceType.SUPER_TOP_UP);
   assertEquals("SUPER_TOP_UP",p.getInsuranceType()); assertEquals("RUNNING",p.getStatus());
   s.finishProduct(p,null,0,new ProductRefreshResult(false,false,"FAILED","timeout"));
   assertEquals("FAILED",p.getStatus()); assertEquals(0,p.getRecordsReceived()); assertEquals(0,p.getRecordsInserted());
   assertNull(p.getResponseJson()); assertFalse(p.isDbRefreshed()); assertFalse(p.isRedisRefreshed()); assertEquals("timeout",p.getErrorMessage()); assertNotNull(p.getCompletedAt());
   verify(pr,times(2)).save(p);
 }
}
