package com.boltpe.insurance.refresh;

import com.boltpe.insurance.cache.InsuranceApiCacheService;
import com.boltpe.insurance.client.LivePolicyPlannerDiscoveryClient;
import com.boltpe.insurance.entity.InsuranceCronProductExecution;
import com.boltpe.insurance.enums.InsuranceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InsuranceApiRefreshService {
    private static final Logger LOGGER=LoggerFactory.getLogger(InsuranceApiRefreshService.class);
    private final LivePolicyPlannerDiscoveryClient discoveryClient;
    private final InsuranceVendorApiConfigService dbService;
    private final InsuranceApiCacheService cacheService;
    private final InsuranceCronAuditService auditService;

    public InsuranceApiRefreshService(LivePolicyPlannerDiscoveryClient discoveryClient, InsuranceVendorApiConfigService dbService,
                                      InsuranceApiCacheService cacheService, InsuranceCronAuditService auditService) {
        this.discoveryClient=discoveryClient; this.dbService=dbService; this.cacheService=cacheService; this.auditService=auditService;
    }

    public ProductRefreshResult refresh(String executionId, InsuranceType type) {
        InsuranceCronProductExecution audit=auditService.startProduct(executionId,type); DiscoverySnapshot snapshot=null; int inserted=0;
        try {
            snapshot=discoveryClient.discover(type);
            inserted=dbService.replaceProductConfiguration(type,snapshot.records());
            try {
                cacheService.put(type,snapshot.records());
                ProductRefreshResult result=new ProductRefreshResult(true,true,"SUCCESS",null);
                auditService.finishProduct(audit,snapshot,inserted,result); return result;
            } catch (RuntimeException cacheEx) {
                String msg=message(cacheEx); LOGGER.warn("DB refreshed but Redis refresh failed for insuranceType={}",type,cacheEx);
                ProductRefreshResult result=new ProductRefreshResult(true,false,"PARTIAL_SUCCESS",msg);
                auditService.finishProduct(audit,snapshot,inserted,result); return result;
            }
        } catch (RuntimeException ex) {
            String msg=message(ex); LOGGER.warn("Nightly refresh failed for insuranceType={}; previous DB/Redis data remains in use",type,ex);
            ProductRefreshResult result=new ProductRefreshResult(false,false,"FAILED",msg);
            auditService.finishProduct(audit,snapshot,inserted,result); return result;
        }
    }
    private static String message(Throwable ex){ return ex.getMessage()!=null&&!ex.getMessage().isBlank()?ex.getMessage():ex.getClass().getSimpleName(); }
}
