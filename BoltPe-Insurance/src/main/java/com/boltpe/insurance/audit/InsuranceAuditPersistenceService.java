package com.boltpe.insurance.audit;

import com.boltpe.insurance.entity.InsuranceQuoteRequest;
import com.boltpe.insurance.repository.InsuranceQuoteRequestRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceAuditPersistenceService {
    private static final String SYSTEM_USER = "SYSTEM";
    private final InsuranceQuoteRequestRepository repository;

    public InsuranceAuditPersistenceService(InsuranceQuoteRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String requestId, String insuranceType, String userInformation, String boltpeRequest,
                     String allApiTrue, String allApiFalse, String vendorResponse, String boltpeResponse) {
        Instant now = Instant.now();
        InsuranceQuoteRequest row = new InsuranceQuoteRequest();
        row.setRequestId(requestId);
        row.setInsuranceType(insuranceType);
        row.setUserInformation(userInformation);
        row.setBoltpeRequestJson(boltpeRequest);
        row.setAllApiTrue(allApiTrue);
        row.setAllApiFalse(allApiFalse);
        row.setVendorResponseJson(vendorResponse);
        row.setBoltpeResponseJson(boltpeResponse);
        row.setCreatedBy(SYSTEM_USER);
        row.setCreatedAt(now);
        row.setUpdatedBy(SYSTEM_USER);
        row.setUpdatedAt(now);
        repository.save(row);
    }
}
