package com.boltpe.insurance.refresh;

import com.boltpe.insurance.entity.InsuranceCronExecution;
import com.boltpe.insurance.entity.InsuranceCronProductExecution;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.InsuranceCronExecutionRepository;
import com.boltpe.insurance.repository.InsuranceCronProductExecutionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class InsuranceCronAuditService {
    private final InsuranceCronExecutionRepository executionRepository;
    private final InsuranceCronProductExecutionRepository productRepository;
    public InsuranceCronAuditService(InsuranceCronExecutionRepository executionRepository, InsuranceCronProductExecutionRepository productRepository) {
        this.executionRepository=executionRepository; this.productRepository=productRepository;
    }
    public InsuranceCronExecution start(String executionId) {
        Instant now=Instant.now(); InsuranceCronExecution x=new InsuranceCronExecution();
        x.setExecutionId(executionId); x.setJobName("INSURANCE_API_REFRESH"); x.setStartedAt(now); x.setStatus("RUNNING");
        x.setTotalProducts(4); x.setSuccessfulProducts(0); x.setFailedProducts(0); x.setCreatedAt(now); return executionRepository.save(x);
    }
    public void finish(InsuranceCronExecution x, int fullySuccessful, int failed, boolean anyPartial, String error) {
        x.setCompletedAt(Instant.now()); x.setSuccessfulProducts(fullySuccessful); x.setFailedProducts(failed);
        x.setStatus(failed==4 ? "FAILED" : (failed>0 || anyPartial ? "PARTIAL_SUCCESS" : "SUCCESS")); x.setErrorMessage(error); executionRepository.save(x);
    }
    public InsuranceCronProductExecution startProduct(String executionId, InsuranceType type) {
        Instant now=Instant.now(); InsuranceCronProductExecution x=new InsuranceCronProductExecution(); x.setExecutionId(executionId); x.setInsuranceType(type.name());
        x.setStartedAt(now); x.setStatus("RUNNING"); x.setCreatedAt(now); return productRepository.save(x);
    }
    public void finishProduct(InsuranceCronProductExecution x, DiscoverySnapshot snapshot, int inserted, ProductRefreshResult result) {
        x.setCompletedAt(Instant.now()); x.setStatus(result.status()); x.setRecordsReceived(snapshot==null?0:snapshot.records().size()); x.setRecordsInserted(inserted);
        x.setDbRefreshed(result.dbRefreshed()); x.setRedisRefreshed(result.redisRefreshed()); x.setResponseJson(snapshot==null?null:snapshot.responseJson()); x.setErrorMessage(result.errorMessage()); productRepository.save(x);
    }
}
