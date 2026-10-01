package com.boltpe.insurance.controller.proposalform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.boltpe.insurance.dto.response.proposalform.InsuranceProposalResponse;
import com.boltpe.insurance.dto.response.proposalform.ProposalResponseData;
import com.boltpe.insurance.exception.GlobalExceptionHandler;
import com.boltpe.insurance.exception.proposalform.ProposalAgeValidationException;
import java.util.Map;
import com.boltpe.insurance.service.proposalform.InsuranceProposalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InsuranceProposalControllerTest {
    private InsuranceProposalService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(InsuranceProposalService.class);
        mvc = MockMvcBuilders.standaloneSetup(new InsuranceProposalController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsCreatedWithBoltPeStandardResponseForNestedDataBody() throws Exception {
        when(service.save(any())).thenReturn(new InsuranceProposalResponse(
                true, "Proposal saved successfully", "req-1",
                new ProposalResponseData("123", "health", "SUBMITTED")));

        mvc.perform(post("/api/v1/insurance/proposals/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "client_name":"bolt_pay",
                                  "formType":"health",
                                  "insuranceType":"HEALTH",
                                  "data": {
                                    "productName":"Health Insurance",
                                    "productType":"New",
                                    "proposerDOB":"12-05-1988"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").value("req-1"))
                .andExpect(jsonPath("$.data.proposalId").value("123"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void returnsProposalAgeFieldErrors() throws Exception {
        when(service.save(any())).thenThrow(new ProposalAgeValidationException(Map.of(
                "adults[0].dob", "Father age must be at least 21 years greater than self age",
                "children[0].dob", "Self age must be at least 21 years greater than child age")));

        mvc.perform(post("/api/v1/insurance/proposals/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "client_name":"bolt_pay",
                                  "formType":"health",
                                  "insuranceType":"HEALTH",
                                  "data":{"productType":"New"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Proposal age validation failed"))
                .andExpect(jsonPath("$.fieldErrors['adults[0].dob']")
                        .value("Father age must be at least 21 years greater than self age"))
                .andExpect(jsonPath("$.fieldErrors['children[0].dob']")
                        .value("Self age must be at least 21 years greater than child age"));
    }

    @Test
    void rejectsMissingClientNameAtControllerBoundary() throws Exception {
        mvc.perform(post("/api/v1/insurance/proposals/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formType\":\"health\",\"insuranceType\":\"HEALTH\",\"data\":{\"productType\":\"New\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.clientName").value("client_name is required"));
        verifyNoInteractions(service);
    }
}
