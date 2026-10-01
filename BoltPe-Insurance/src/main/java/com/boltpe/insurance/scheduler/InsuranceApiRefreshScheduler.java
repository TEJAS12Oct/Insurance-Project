package com.boltpe.insurance.scheduler;

import com.boltpe.insurance.entity.InsuranceCronExecution;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.refresh.InsuranceApiRefreshService;
import com.boltpe.insurance.refresh.InsuranceCronAuditService;
import com.boltpe.insurance.refresh.ProductRefreshResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InsuranceApiRefreshScheduler {
    private static final Logger LOGGER=LoggerFactory.getLogger(InsuranceApiRefreshScheduler.class);
    private final InsuranceApiRefreshService refreshService;
    private final InsuranceCronAuditService auditService;
    public InsuranceApiRefreshScheduler(InsuranceApiRefreshService refreshService, InsuranceCronAuditService auditService) {
        this.refreshService=refreshService; this.auditService=auditService;
    }

    @Scheduled(cron="${insurance.refresh.cron}", zone="${insurance.refresh.zone:Asia/Kolkata}")
    public void refreshAllInsuranceApis() {
        String executionId=UUID.randomUUID().toString();
        LOGGER.info("Insurance nightly API refresh started executionId={}",executionId);
        InsuranceCronExecution execution=auditService.start(executionId);
        List<ProductRefreshResult> results=new ArrayList<>();
        for (InsuranceType type : InsuranceType.values()) {
            try { results.add(refreshService.refresh(executionId,type)); }
            catch (RuntimeException ex) {
                LOGGER.error("Unexpected refresh error for insuranceType={}, executionId={}",type,executionId,ex);
                results.add(new ProductRefreshResult(false,false,"FAILED",ex.getMessage()));
            }
        }
        int full=(int)results.stream().filter(ProductRefreshResult::fullySuccessful).count();
        int failed=(int)results.stream().filter(r->!r.dbRefreshed()).count();
        boolean partial=results.stream().anyMatch(r->r.dbRefreshed()&&!r.redisRefreshed());
        String errors=results.stream().map(ProductRefreshResult::errorMessage).filter(x->x!=null&&!x.isBlank()).reduce((a,b)->a+" | "+b).orElse(null);
        auditService.finish(execution,full,failed,partial,errors);
        LOGGER.info("Insurance nightly API refresh completed executionId={}, fullySuccessful={}, failed={}, partial={}",executionId,full,failed,partial);
    }
}
