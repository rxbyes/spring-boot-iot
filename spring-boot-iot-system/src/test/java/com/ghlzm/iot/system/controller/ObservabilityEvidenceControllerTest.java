package com.ghlzm.iot.system.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.service.ObservabilityEvidenceQueryService;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySpanVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservabilityEvidenceControllerTest {

    @Mock
    private ObservabilityEvidenceQueryService observabilityEvidenceQueryService;

    private ObservabilityEvidenceController controller;

    @BeforeEach
    void setUp() {
        controller = new ObservabilityEvidenceController(observabilityEvidenceQueryService);
    }

    @Test
    void pageBusinessEventsShouldDelegateFilters() {
        ObservabilityBusinessEventPageQuery query = new ObservabilityBusinessEventPageQuery();
        query.setTraceId("trace-apply-1");
        query.setEventCode("product.contract.apply");
        query.setDomainCode("product");
        query.setActionCode("apply");
        query.setObjectType("product_contract");
        query.setObjectId("20430001");
        query.setResultStatus("SUCCESS");
        query.setDateFrom("2026-04-25 00:00:00");
        query.setDateTo("2026-04-25 23:59:59");
        query.setPageNum(1L);
        query.setPageSize(20L);

        ObservabilityBusinessEventVO row = new ObservabilityBusinessEventVO();
        row.setTraceId("trace-apply-1");
        row.setEventCode("product.contract.apply");
        row.setOccurredAt(LocalDateTime.of(2026, 4, 25, 10, 0));
        when(observabilityEvidenceQueryService.pageBusinessEvents(query, 10001L))
                .thenReturn(PageResult.of(1L, 1L, 20L, List.of(row)));

        R<PageResult<ObservabilityBusinessEventVO>> response = controller.pageBusinessEvents(query, authentication(10001L));

        assertEquals(1L, response.getData().getTotal());
        assertEquals("product.contract.apply", response.getData().getRecords().get(0).getEventCode());
        verify(observabilityEvidenceQueryService).pageBusinessEvents(query, 10001L);
    }

    @Test
    void pageSpansShouldDelegateFilters() {
        ObservabilitySpanPageQuery query = new ObservabilitySpanPageQuery();
        query.setTraceId("trace-mqtt-1");
        query.setSpanType("MESSAGE_FLOW");
        query.setEventCode("mqtt.pipeline");
        query.setDomainCode("message");
        query.setObjectType("device_message");
        query.setObjectId("DEV-001");
        query.setStatus("ERROR");
        query.setMinDurationMs(1500L);
        query.setDateFrom("2026-04-25T00:00:00");
        query.setDateTo("2026-04-25T23:59:59");
        query.setPageNum(2L);
        query.setPageSize(50L);

        ObservabilitySpanVO row = new ObservabilitySpanVO();
        row.setTraceId("trace-mqtt-1");
        row.setSpanType("MESSAGE_FLOW");
        row.setDurationMs(1780L);
        when(observabilityEvidenceQueryService.pageSpans(query, 10001L))
                .thenReturn(PageResult.of(1L, 2L, 50L, List.of(row)));

        R<PageResult<ObservabilitySpanVO>> response = controller.pageSpans(query, authentication(10001L));

        assertEquals(2L, response.getData().getPageNum());
        assertEquals("MESSAGE_FLOW", response.getData().getRecords().get(0).getSpanType());
        verify(observabilityEvidenceQueryService).pageSpans(query, 10001L);
    }

    @Test
    void listSlowSpanSummariesShouldDelegateFilters() {
        ObservabilitySlowSpanSummaryQuery query = new ObservabilitySlowSpanSummaryQuery();
        query.setSpanType("SLOW_SQL");
        query.setDomainCode("system");
        query.setMinDurationMs(1000L);
        query.setDateFrom("2026-04-25");
        query.setDateTo("2026-04-25");
        query.setLimit(10);

        ObservabilitySlowSpanSummaryVO row = new ObservabilitySlowSpanSummaryVO();
        row.setSpanType("SLOW_SQL");
        row.setDomainCode("system");
        row.setTotalCount(3L);
        row.setAvgDurationMs(1280L);
        row.setMaxDurationMs(2400L);
        row.setLatestTraceId("trace-slow-1");
        when(observabilityEvidenceQueryService.listSlowSpanSummaries(query, 10001L))
                .thenReturn(List.of(row));

        R<List<ObservabilitySlowSpanSummaryVO>> response =
                controller.listSlowSpanSummaries(query, authentication(10001L));

        assertEquals(1, response.getData().size());
        assertEquals("SLOW_SQL", response.getData().get(0).getSpanType());
        assertEquals(2400L, response.getData().get(0).getMaxDurationMs());
        verify(observabilityEvidenceQueryService).listSlowSpanSummaries(query, 10001L);
    }

    @Test
    void getTraceEvidenceShouldReturnCombinedTrace() {
        ObservabilityTraceEvidenceVO trace = new ObservabilityTraceEvidenceVO();
        trace.setTraceId("trace-combined-1");
        trace.setBusinessEvents(List.of(new ObservabilityBusinessEventVO()));
        trace.setSpans(List.of(new ObservabilitySpanVO()));
        when(observabilityEvidenceQueryService.getTraceEvidence("trace-combined-1", 10001L)).thenReturn(trace);

        R<ObservabilityTraceEvidenceVO> response = controller.getTraceEvidence("trace-combined-1", authentication(10001L));

        assertEquals("trace-combined-1", response.getData().getTraceId());
        assertEquals(1, response.getData().getBusinessEvents().size());
        assertEquals(1, response.getData().getSpans().size());
        verify(observabilityEvidenceQueryService).getTraceEvidence("trace-combined-1", 10001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "tester");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
