package com.boltpe.insurance.controller.proposalform;

import com.boltpe.insurance.dto.request.proposalform.InsuranceProposalRequest;
import com.boltpe.insurance.dto.response.proposalform.InsuranceProposalResponse;
import com.boltpe.insurance.service.proposalform.InsuranceProposalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurance/proposals")
public class InsuranceProposalController {
    private final InsuranceProposalService service;

    public InsuranceProposalController(InsuranceProposalService service) {
        this.service = service;
    }

    @PostMapping("/save")
    public ResponseEntity<InsuranceProposalResponse> save(@Valid @RequestBody InsuranceProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }
}
