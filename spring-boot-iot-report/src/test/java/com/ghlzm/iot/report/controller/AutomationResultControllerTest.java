package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceContentVO;
import com.ghlzm.iot.report.vo.AutomationResultEvidenceItemVO;
import com.ghlzm.iot.report.vo.AutomationResultRunSummaryVO;
import com.ghlzm.iot.common.response.PageResult;
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
class AutomationResultControllerTest {

    @Mock
    private AutomationResultQueryService automationResultQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AutomationResultController(automationResultQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldExposeRecentAutomationResultsRoute() throws Exception {
        when(automationResultQueryService.listRecentRuns(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/report/automation-results/recent").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService).listRecentRuns(5);
    }

    @Test
    void shouldExposeAutomationResultPageRoute() throws Exception {
        when(automationResultQueryService.pageRuns(2, 20, "risk", "failed", "riskDrill", "2026-04-01", "2026-04-03"))
                .thenReturn(PageResult.of(0L, 2L, 20L, List.<AutomationResultRunSummaryVO>of()));

        mockMvc.perform(get("/api/report/automation-results/page")
                        .param("pageNum", "2")
                        .param("pageSize", "20")
                        .param("keyword", "risk")
                        .param("status", "failed")
                        .param("runnerType", "riskDrill")
                        .param("dateFrom", "2026-04-01")
                        .param("dateTo", "2026-04-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService)
                .pageRuns(2, 20, "risk", "failed", "riskDrill", "2026-04-01", "2026-04-03");
    }

    @Test
    void shouldExposeAutomationResultDetailRoute() throws Exception {
        when(automationResultQueryService.getRunDetail("20260402155432")).thenReturn(null);

        mockMvc.perform(get("/api/report/automation-results/20260402155432"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService).getRunDetail("20260402155432");
    }

    @Test
    void shouldExposeAutomationResultEvidenceRoute() throws Exception {
        when(automationResultQueryService.listRunEvidence("20260402155432")).thenReturn(List.of(new AutomationResultEvidenceItemVO()));

        mockMvc.perform(get("/api/report/automation-results/20260402155432/evidence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService).listRunEvidence("20260402155432");
    }

    @Test
    void shouldExposeAutomationResultEvidenceContentRoute() throws Exception {
        when(automationResultQueryService.getEvidenceContent("20260402155432", "logs/acceptance/registry-run-20260402155432.json"))
                .thenReturn(new AutomationResultEvidenceContentVO());

        mockMvc.perform(get("/api/report/automation-results/20260402155432/evidence/content")
                        .param("path", "logs/acceptance/registry-run-20260402155432.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService).getEvidenceContent("20260402155432", "logs/acceptance/registry-run-20260402155432.json");
    }
}
