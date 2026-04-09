package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.report.service.BusinessAcceptanceService;
import com.ghlzm.iot.report.vo.BusinessAcceptanceAccountTemplateVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceResultVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunLaunchVO;
import com.ghlzm.iot.report.vo.BusinessAcceptanceRunStatusVO;
import com.ghlzm.iot.report.vo.BusinessAcceptancePackageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void shouldLaunchBusinessAcceptanceRun() throws Exception {
        BusinessAcceptanceRunLaunchVO launchVO = new BusinessAcceptanceRunLaunchVO();
        launchVO.setJobId("job-001");
        launchVO.setStatus("running");
        when(businessAcceptanceService.launchRun(org.mockito.ArgumentMatchers.any())).thenReturn(launchVO);

        mockMvc.perform(post("/api/report/business-acceptance/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "packageCode":"product-device",
                                  "environmentCode":"dev",
                                  "accountTemplateCode":"acceptance-default",
                                  "moduleCodes":["product-create"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobId").value("job-001"))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    void shouldExposeBusinessAcceptanceRunStatusRoute() throws Exception {
        BusinessAcceptanceRunStatusVO statusVO = new BusinessAcceptanceRunStatusVO();
        statusVO.setJobId("job-001");
        statusVO.setStatus("completed");
        statusVO.setRunId("20260404153000");
        when(businessAcceptanceService.getRunStatus("job-001")).thenReturn(statusVO);

        mockMvc.perform(get("/api/report/business-acceptance/runs/job-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.runId").value("20260404153000"));

        verify(businessAcceptanceService).getRunStatus("job-001");
    }

    @Test
    void shouldExposeBusinessAcceptanceResultRoute() throws Exception {
        BusinessAcceptanceResultVO resultVO = new BusinessAcceptanceResultVO();
        resultVO.setRunId("20260404153000");
        resultVO.setStatus("failed");
        when(businessAcceptanceService.getRunResult("product-device", "20260404153000")).thenReturn(resultVO);

        mockMvc.perform(get("/api/report/business-acceptance/results/20260404153000")
                        .param("packageCode", "product-device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("failed"));

        verify(businessAcceptanceService).getRunResult("product-device", "20260404153000");
    }
}
