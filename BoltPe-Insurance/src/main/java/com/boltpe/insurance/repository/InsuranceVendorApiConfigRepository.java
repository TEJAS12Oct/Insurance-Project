package com.boltpe.insurance.repository;

import com.boltpe.insurance.entity.InsuranceVendorApiConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceVendorApiConfigRepository extends JpaRepository<InsuranceVendorApiConfig, Long> {
    List<InsuranceVendorApiConfig> findAllByInsuranceTypeOrderByIdAsc(String insuranceType);
    void deleteAllByInsuranceType(String insuranceType);
}
