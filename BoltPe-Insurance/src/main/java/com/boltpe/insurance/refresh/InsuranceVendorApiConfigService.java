package com.boltpe.insurance.refresh;

import com.boltpe.insurance.entity.InsuranceVendorApiConfig;
import com.boltpe.insurance.enums.InsuranceType;
import com.boltpe.insurance.repository.InsuranceVendorApiConfigRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceVendorApiConfigService {
    private final InsuranceVendorApiConfigRepository repository;
    public InsuranceVendorApiConfigService(InsuranceVendorApiConfigRepository repository) { this.repository = repository; }

    @Transactional
    public int replaceProductConfiguration(InsuranceType type, List<VendorApiRecord> records) {
        if (records == null || records.isEmpty()) throw new IllegalArgumentException("Discovery records must not be empty");
        repository.deleteAllByInsuranceType(type.name());
        Instant now = Instant.now();
        List<InsuranceVendorApiConfig> rows = records.stream().map(r -> {
            InsuranceVendorApiConfig row = new InsuranceVendorApiConfig();
            row.setInsuranceType(type.name()); row.setCompanyId(r.companyId()); row.setPlanId(r.planId()); row.setApiType(r.apiType());
            row.setRefreshedAt(now); row.setCreatedAt(now); row.setUpdatedAt(now); return row;
        }).toList();
        return repository.saveAll(rows).size();
    }
}
