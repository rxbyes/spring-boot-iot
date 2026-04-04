package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.report.service.BusinessAcceptanceService;
import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BusinessAcceptanceControllerTest {

    @Mock
    private BusinessAcceptanceService businessAcceptanceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BusinessAcceptanceController(businessAcceptanceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldExposeBusinessAcceptancePackagesRoute() throws Exception {
        when(businessAcceptanceService.listPackages()).thenReturn(List.of(new BusinessAcceptancePackageVO()));

        mockMvc.perform(get("/api/report/business-acceptance/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(businessAcceptanceService).listPackages();
    }

    @Test
    void shouldExposeBusinessAcceptanceAccountTemplatesRoute() throws Exception {
        when(businessAcceptanceService.listAccountTemplates()).thenReturn(List.of(new BusinessAcceptanceAccountTemplateVO()));

        mockMvc.perform(get("/api/report/business-acceptance/account-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(businessAcceptanceService).listAccountTemplates();
    }
}
