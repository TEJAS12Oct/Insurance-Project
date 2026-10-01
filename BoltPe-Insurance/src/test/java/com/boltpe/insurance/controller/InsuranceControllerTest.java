package com.boltpe.insurance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.boltpe.insurance.dto.response.HealthInsuranceResponse;
import com.boltpe.insurance.exception.GlobalExceptionHandler;
import com.boltpe.insurance.service.InsuranceService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

class InsuranceControllerTest {
    private InsuranceService service;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = Mockito.mock(InsuranceService.class);
        mvc = MockMvcBuilders.standaloneSetup(new InsuranceController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void shouldReturnHealthQuotes() throws Exception {
        when(service.getQuotes(any())).thenReturn(new HealthInsuranceResponse("req","HEALTH",0,List.of()));
        mvc.perform(post("/api/v1/insurance/plans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"insuranceType\":\"HEALTH\",\"userInformation\":{\"userId\":\"USR001\",\"name\":\"Test User\"},\"data\":{\"coverAmount\":1000000,\"zone\":\"1\",\"age\":30}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.insuranceType").value("HEALTH"));
    }

    @Test void shouldAcceptRequestWithoutUserInformation() throws Exception {
        when(service.getQuotes(any())).thenReturn(new HealthInsuranceResponse("req","HEALTH",0,List.of()));
        mvc.perform(post("/api/v1/insurance/plans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"insuranceType\":\"HEALTH\",\"data\":{\"coverAmount\":1000000,\"zone\":\"1\",\"age\":30}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insuranceType").value("HEALTH"));
    }

    @Test void shouldReturnBadRequestForInvalidHealthRequest() throws Exception {
        mvc.perform(post("/api/v1/insurance/plans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"insuranceType\":\"HEALTH\",\"userInformation\":{\"userId\":\"USR001\",\"name\":\"Test User\"},\"data\":{\"coverAmount\":0,\"zone\":\"1\",\"age\":10}}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test void shouldRejectDecimalCoverAmountEvenWhenTruncatedValueWouldBeAllowed() throws Exception {
        mvc.perform(post("/api/v1/insurance/plans").contentType(MediaType.APPLICATION_JSON)
                .content("{\"insuranceType\":\"HEALTH\",\"data\":{\"coverAmount\":1000000.5,\"zone\":\"1\",\"age\":30}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }
}
