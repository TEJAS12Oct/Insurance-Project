package com.boltpe.insurance.repository;

import com.boltpe.insurance.entity.InsuranceQuoteRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceQuoteRequestRepository extends JpaRepository<InsuranceQuoteRequest, Long> {
    Optional<InsuranceQuoteRequest> findByRequestId(String requestId);
}
