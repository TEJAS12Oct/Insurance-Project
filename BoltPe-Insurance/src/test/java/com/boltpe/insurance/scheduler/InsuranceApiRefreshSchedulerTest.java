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
   verify(refresh,times(4)).refresh(anyString(),any()); verify(audit).finish(eq(execution),eq(3),eq(1),eq(false),eq("down"));
 }
 @Test void unexpectedProductExceptionDoesNotStopRemainingProducts() {
   var refresh=mock(InsuranceApiRefreshService.class); var audit=mock(InsuranceCronAuditService.class); var execution=new InsuranceCronExecution(); when(audit.start(anyString())).thenReturn(execution);
   when(refresh.refresh(anyString(),any())).thenAnswer(i->{ InsuranceType t=i.getArgument(1); if(t==InsuranceType.HEALTH) throw new RuntimeException("boom"); return new ProductRefreshResult(true,true,"SUCCESS",null); });
   assertDoesNotThrow(()->new InsuranceApiRefreshScheduler(refresh,audit).refreshAllInsuranceApis()); verify(refresh,times(4)).refresh(anyString(),any());
 }
}
