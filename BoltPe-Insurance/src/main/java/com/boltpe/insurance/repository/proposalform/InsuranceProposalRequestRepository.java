package com.boltpe.insurance.repository.proposalform;

import com.boltpe.insurance.entity.proposalform.InsuranceProposalRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProposalRequestRepository extends JpaRepository<InsuranceProposalRequestEntity, Long> {
    Optional<InsuranceProposalRequestEntity> findByRequestId(String requestId);
}
