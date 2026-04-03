package com.ghlzm.iot.report.controller;

import com.ghlzm.iot.framework.advice.GlobalExceptionHandler;
import com.ghlzm.iot.report.service.AutomationResultQueryService;
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
    void shouldExposeAutomationResultDetailRoute() throws Exception {
        when(automationResultQueryService.getRunDetail("20260402155432")).thenReturn(null);

        mockMvc.perform(get("/api/report/automation-results/20260402155432"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(automationResultQueryService).getRunDetail("20260402155432");
    }
}
