package com.boltpe.insurance.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.boltpe.insurance.entity.InsuranceCronExecution;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.refresh.*;
import org.junit.jupiter.api.Test;

class InsuranceApiRefreshSchedulerTest {
 @Test void runsAllFourProductsAndFinishesAudit() {
   var refresh=mock(InsuranceApiRefreshService.class); var audit=mock(InsuranceCronAuditService.class); var execution=new InsuranceCronExecution(); when(audit.start(anyString())).thenReturn(execution);
   when(refresh.refresh(anyString(),any())).thenAnswer(i->{ InsuranceType t=i.getArgument(1); return t==InsuranceType.SUPER_TOP_UP ? new ProductRefreshResult(false,false,"FAILED","down") : new ProductRefreshResult(true,true,"SUCCESS",null); });
   new InsuranceApiRefreshScheduler(refresh,audit).refreshAllInsuranceApis();
   verify(refresh,times(4)).refresh(anyString(),any()); verify(audit).finish(execution,3,1,false,"down");
 }
 @Test void unexpectedProductExceptionDoesNotStopRemainingProducts() {
   var refresh=mock(InsuranceApiRefreshService.class); var audit=mock(InsuranceCronAuditService.class); var execution=new InsuranceCronExecution(); when(audit.start(anyString())).thenReturn(execution);
   when(refresh.refresh(anyString(),any())).thenAnswer(i->{ InsuranceType t=i.getArgument(1); if(t==InsuranceType.HEALTH) throw new RuntimeException("boom"); return new ProductRefreshResult(true,true,"SUCCESS",null); });
   assertDoesNotThrow(()->new InsuranceApiRefreshScheduler(refresh,audit).refreshAllInsuranceApis()); verify(refresh,times(4)).refresh(anyString(),any());
   verify(audit).finish(execution,3,1,false,"boom");
 }
 @Test void redisOnlyFailureIsReportedAsPartialAndErrorsAreJoinedSkippingBlanks() {
   var refresh=mock(InsuranceApiRefreshService.class); var audit=mock(InsuranceCronAuditService.class); var execution=new InsuranceCronExecution(); when(audit.start(anyString())).thenReturn(execution);
   when(refresh.refresh(anyString(),any())).thenAnswer(i->switch((InsuranceType)i.getArgument(1)){
     case HEALTH -> new ProductRefreshResult(true,false,"PARTIAL_SUCCESS","redis down");
     case SUPER_TOP_UP -> new ProductRefreshResult(false,false,"FAILED","timeout");
     case PERSONAL_ACCIDENT -> new ProductRefreshResult(true,true,"SUCCESS","   ");
     case HOSPITAL_CASH -> new ProductRefreshResult(true,true,"SUCCESS",null);
   });
   new InsuranceApiRefreshScheduler(refresh,audit).refreshAllInsuranceApis();
   verify(audit).finish(execution,2,1,true,"redis down | timeout");
 }
 @Test void allSuccessfulProductsFinishWithNoErrorAndNoPartial() {
   var refresh=mock(InsuranceApiRefreshService.class); var audit=mock(InsuranceCronAuditService.class); var execution=new InsuranceCronExecution(); when(audit.start(anyString())).thenReturn(execution);
   when(refresh.refresh(anyString(),any())).thenReturn(new ProductRefreshResult(true,true,"SUCCESS",""));
   new InsuranceApiRefreshScheduler(refresh,audit).refreshAllInsuranceApis();
   verify(audit).finish(execution,4,0,false,null);
 }
}
