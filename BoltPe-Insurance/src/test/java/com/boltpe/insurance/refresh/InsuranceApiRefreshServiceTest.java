package com.boltpe.insurance.refresh;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.boltpe.insurance.cache.InsuranceApiCacheService;
import com.boltpe.insurance.client.LivePolicyPlannerDiscoveryClient;
import com.boltpe.insurance.entity.InsuranceCronProductExecution;
import com.boltpe.insurance.enums.InsuranceType;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsuranceApiRefreshServiceTest {
 private static DiscoverySnapshot snap(){return new DiscoverySnapshot(List.of(new VendorApiRecord("/h","1","2")),"{}");}
 @Test void successRefreshesDbThenRedis() {
   var d=mock(LivePolicyPlannerDiscoveryClient.class); var db=mock(InsuranceVendorApiConfigService.class); var c=mock(InsuranceApiCacheService.class); var a=mock(InsuranceCronAuditService.class); var audit=new InsuranceCronProductExecution();
   when(a.startProduct("e",InsuranceType.HEALTH)).thenReturn(audit); when(d.discover(InsuranceType.HEALTH)).thenReturn(snap()); when(db.replaceProductConfiguration(any(),anyList())).thenReturn(1);
   var r=new InsuranceApiRefreshService(d,db,c,a).refresh("e",InsuranceType.HEALTH);
   assertTrue(r.fullySuccessful()); assertEquals("SUCCESS",r.status()); verify(c).put(eq(InsuranceType.HEALTH),anyList()); verify(a).finishProduct(eq(audit),any(),eq(1),any());
 }
 @Test void discoveryFailureKeepsOldDbAndRedis() {
   var d=mock(LivePolicyPlannerDiscoveryClient.class); var db=mock(InsuranceVendorApiConfigService.class); var c=mock(InsuranceApiCacheService.class); var a=mock(InsuranceCronAuditService.class);
   when(a.startProduct(anyString(),any())).thenReturn(new InsuranceCronProductExecution()); when(d.discover(InsuranceType.SUPER_TOP_UP)).thenThrow(new RuntimeException("timeout"));
   var r=new InsuranceApiRefreshService(d,db,c,a).refresh("e",InsuranceType.SUPER_TOP_UP);
   assertEquals("FAILED",r.status()); assertFalse(r.dbRefreshed()); verifyNoInteractions(db,c);
 }
 @Test void redisFailureLeavesCommittedDbAndReportsPartial() {
   var d=mock(LivePolicyPlannerDiscoveryClient.class); var db=mock(InsuranceVendorApiConfigService.class); var c=mock(InsuranceApiCacheService.class); var a=mock(InsuranceCronAuditService.class);
   when(a.startProduct(anyString(),any())).thenReturn(new InsuranceCronProductExecution()); when(d.discover(InsuranceType.PERSONAL_ACCIDENT)).thenReturn(snap()); when(db.replaceProductConfiguration(any(),anyList())).thenReturn(1); doThrow(new RuntimeException("redis down")).when(c).put(any(),anyList());
   var r=new InsuranceApiRefreshService(d,db,c,a).refresh("e",InsuranceType.PERSONAL_ACCIDENT);
   assertEquals("PARTIAL_SUCCESS",r.status()); assertTrue(r.dbRefreshed()); assertFalse(r.redisRefreshed());
 }
 @Test void exceptionWithoutMessageFallsBackToExceptionTypeName() {
   var d=mock(LivePolicyPlannerDiscoveryClient.class); var db=mock(InsuranceVendorApiConfigService.class); var c=mock(InsuranceApiCacheService.class); var a=mock(InsuranceCronAuditService.class); var audit=new InsuranceCronProductExecution();
   when(a.startProduct(anyString(),any())).thenReturn(audit); when(d.discover(InsuranceType.HOSPITAL_CASH)).thenThrow(new IllegalStateException());
   var r=new InsuranceApiRefreshService(d,db,c,a).refresh("e",InsuranceType.HOSPITAL_CASH);
   assertEquals("FAILED",r.status()); assertEquals("IllegalStateException",r.errorMessage());
   verify(a).finishProduct(eq(audit),isNull(),eq(0),eq(r)); verifyNoInteractions(db,c);
 }
 @Test void exceptionWithBlankMessageFallsBackToExceptionTypeName() {
   var d=mock(LivePolicyPlannerDiscoveryClient.class); var db=mock(InsuranceVendorApiConfigService.class); var c=mock(InsuranceApiCacheService.class); var a=mock(InsuranceCronAuditService.class);
   when(a.startProduct(anyString(),any())).thenReturn(new InsuranceCronProductExecution()); when(d.discover(InsuranceType.HEALTH)).thenReturn(snap()); when(db.replaceProductConfiguration(any(),anyList())).thenReturn(1);
   doThrow(new UnsupportedOperationException("  ")).when(c).put(any(),anyList());
   var r=new InsuranceApiRefreshService(d,db,c,a).refresh("e",InsuranceType.HEALTH);
   assertEquals("PARTIAL_SUCCESS",r.status()); assertEquals("UnsupportedOperationException",r.errorMessage());
 }
}
