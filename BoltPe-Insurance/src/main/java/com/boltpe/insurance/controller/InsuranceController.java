package com.boltpe.insurance.controller;

import com.boltpe.insurance.dto.request.InsuranceRequest;
import com.boltpe.insurance.service.InsuranceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurance")
public class InsuranceController {
    private final InsuranceService service;

    public InsuranceController(InsuranceService service) {
        this.service = service; 
    }

    @PostMapping("/plans")
    public ResponseEntity<Object> getQuotes(@Valid @RequestBody InsuranceRequest request) {
        return ResponseEntity.ok(service.getQuotes(request));
    }
}
