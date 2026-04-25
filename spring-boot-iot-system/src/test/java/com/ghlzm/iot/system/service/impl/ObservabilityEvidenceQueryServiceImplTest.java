package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservabilityEvidenceQueryServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PermissionService permissionService;

    private ObservabilityEvidenceQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ObservabilityEvidenceQueryServiceImpl(jdbcTemplate, permissionService);
        when(permissionService.getDataPermissionContext(10001L))
                .thenReturn(new DataPermissionContext(10001L, 1L, 10L, null, false));
    }

    @Test
    void pageBusinessEventsShouldApplyFiltersAndNormalizePageSize() {
        ObservabilityBusinessEventPageQuery query = new ObservabilityBusinessEventPageQuery();
        query.setTraceId(" trace-1 ");
        query.setEventCode("product.contract.apply");
        query.setDomainCode("product");
        query.setResultStatus("SUCCESS");
        query.setDateFrom("2026-04-25");
        query.setDateTo("2026-04-25");
        query.setPageNum(0L);
        query.setPageSize(500L);

        ObservabilityBusinessEventVO event = new ObservabilityBusinessEventVO();
        event.setTraceId("trace-1");
        event.setEventCode("product.contract.apply");
        when(jdbcTemplate.queryForObject(contains("FROM sys_business_event_log"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        doReturn(List.of(event)).when(jdbcTemplate).query(
                contains("ORDER BY occurred_at DESC"),
                any(RowMapper.class),
                any(Object[].class)
        );

        PageResult<ObservabilityBusinessEventVO> result = service.pageBusinessEvents(query, 10001L);

        assertEquals(1L, result.getPageNum());
        assertEquals(100L, result.getPageSize());
        assertEquals("product.contract.apply", result.getRecords().get(0).getEventCode());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("trace_id = ?"));
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("event_code = ?"));
        assertTrue(sql.contains("domain_code = ?"));
        assertTrue(sql.contains("result_status = ?"));
        assertTrue(sql.contains("occurred_at >= ?"));
        assertTrue(sql.contains("occurred_at <= ?"));
    }

    @Test
    void pageSpansShouldApplySlowThresholdFilter() {
        ObservabilitySpanPageQuery query = new ObservabilitySpanPageQuery();
        query.setTraceId("trace-2");
        query.setSpanType("SLOW_SQL");
        query.setStatus("SUCCESS");
        query.setMinDurationMs(1000L);
        query.setPageNum(2L);
        query.setPageSize(50L);

        ObservabilitySpanVO span = new ObservabilitySpanVO();
        span.setTraceId("trace-2");
        span.setSpanType("SLOW_SQL");
        span.setDurationMs(1200L);
        when(jdbcTemplate.queryForObject(contains("FROM sys_observability_span_log"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        doReturn(List.of(span)).when(jdbcTemplate).query(
                contains("ORDER BY started_at DESC"),
                any(RowMapper.class),
                any(Object[].class)
        );

        PageResult<ObservabilitySpanVO> result = service.pageSpans(query, 10001L);

        assertEquals(2L, result.getPageNum());
        assertEquals(50L, result.getPageSize());
        assertEquals(1200L, result.getRecords().get(0).getDurationMs());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("span_type = ?"));
        assertTrue(sql.contains("duration_ms >= ?"));
        assertTrue(sql.contains("status = ?"));
    }

    @Test
    void listSlowSpanSummariesShouldGroupHotspotsAndKeepTenantScope() {
        ObservabilitySlowSpanSummaryQuery query = new ObservabilitySlowSpanSummaryQuery();
        query.setSpanType("SLOW_SQL");
        query.setDomainCode("system");
        query.setMinDurationMs(1000L);
        query.setDateFrom("2026-04-25");
        query.setDateTo("2026-04-25");
        query.setLimit(200);

        ObservabilitySlowSpanSummaryVO summary = new ObservabilitySlowSpanSummaryVO();
        summary.setSpanType("SLOW_SQL");
        summary.setDomainCode("system");
        summary.setTotalCount(3L);
        summary.setAvgDurationMs(1280L);
        summary.setMaxDurationMs(2400L);
        summary.setLatestTraceId("trace-slow-1");
        doReturn(List.of(summary)).when(jdbcTemplate).query(
                contains("GROUP BY span_type, domain_code, event_code, object_type, object_id"),
                any(RowMapper.class),
                any(Object[].class)
        );

        List<ObservabilitySlowSpanSummaryVO> result = service.listSlowSpanSummaries(query, 10001L);

        assertEquals(1, result.size());
        assertEquals("SLOW_SQL", result.get(0).getSpanType());
        assertEquals(3L, result.get(0).getTotalCount());
        assertEquals(1280L, result.get(0).getAvgDurationMs());
        assertEquals(2400L, result.get(0).getMaxDurationMs());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("span_type = ?"));
        assertTrue(sql.contains("domain_code = ?"));
        assertTrue(sql.contains("duration_ms >= ?"));
        assertTrue(sql.contains("started_at >= ?"));
        assertTrue(sql.contains("started_at <= ?"));
        assertTrue(sql.contains("ORDER BY max_duration_ms DESC, total_count DESC"));
    }

    @Test
    void getTraceEvidenceShouldMergeTimelineByOccurredTime() {
        ObservabilityBusinessEventVO event = new ObservabilityBusinessEventVO();
        event.setId(20L);
        event.setTraceId("trace-3");
        event.setEventCode("product.contract.apply");
        event.setEventName("合同生效");
        event.setOccurredAt(LocalDateTime.of(2026, 4, 25, 10, 5));
        event.setResultStatus("SUCCESS");

        ObservabilitySpanVO span = new ObservabilitySpanVO();
        span.setId(10L);
        span.setTraceId("trace-3");
        span.setSpanType("HTTP_REQUEST");
        span.setSpanName("POST /api/device/product/1/model/apply");
        span.setStartedAt(LocalDateTime.of(2026, 4, 25, 10, 0));
        span.setStatus("SUCCESS");

        doReturn(List.of(event)).when(jdbcTemplate).query(
                contains("FROM sys_business_event_log"),
                any(RowMapper.class),
                eq(1L),
                eq("trace-3"),
                eq(500)
        );
        doReturn(List.of(span)).when(jdbcTemplate).query(
                contains("FROM sys_observability_span_log"),
                any(RowMapper.class),
                eq(1L),
                eq("trace-3"),
                eq(500)
        );

        ObservabilityTraceEvidenceVO result = service.getTraceEvidence(" trace-3 ", 10001L);

        assertEquals("trace-3", result.getTraceId());
        assertEquals(2, result.getTimeline().size());
        assertEquals("SPAN", result.getTimeline().get(0).getItemType());
        assertEquals("BUSINESS_EVENT", result.getTimeline().get(1).getItemType());
    }
}
