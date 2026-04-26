package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityMessageArchiveBatchPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityScheduledTaskPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanTrendQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchVO;
import com.ghlzm.iot.system.vo.ObservabilityScheduledTaskVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanTrendVO;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
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
        lenient().when(permissionService.getDataPermissionContext(10001L))
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
    void pageScheduledTasksShouldApplyTaskFiltersAndKeepScheduledScope() {
        ObservabilityScheduledTaskPageQuery query = new ObservabilityScheduledTaskPageQuery();
        query.setTraceId("trace-scheduled-1");
        query.setTaskCode("DeviceSessionTimeoutScheduler#closeTimedOutSessions");
        query.setTriggerType("FIXED_DELAY");
        query.setStatus("FAILURE");
        query.setMinDurationMs(200L);
        query.setDateFrom("2026-04-25 08:00:00");
        query.setDateTo("2026-04-25 18:00:00");
        query.setPageNum(1L);
        query.setPageSize(10L);

        ObservabilityScheduledTaskVO row = new ObservabilityScheduledTaskVO();
        row.setTraceId("trace-scheduled-1");
        row.setTaskCode("DeviceSessionTimeoutScheduler#closeTimedOutSessions");
        row.setTaskName("DeviceSessionTimeoutScheduler#closeTimedOutSessions");
        row.setTriggerType("FIXED_DELAY");
        row.setStatus("FAILURE");
        row.setDurationMs(550L);
        when(jdbcTemplate.queryForObject(contains("FROM sys_observability_span_log"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        doReturn(List.of(row)).when(jdbcTemplate).query(
                contains("span_type = 'SCHEDULED_TASK'"),
                any(RowMapper.class),
                any(Object[].class)
        );

        PageResult<ObservabilityScheduledTaskVO> result = service.pageScheduledTasks(query, 10001L);

        assertEquals(1L, result.getPageNum());
        assertEquals(10L, result.getPageSize());
        assertEquals("FIXED_DELAY", result.getRecords().get(0).getTriggerType());
        assertEquals("DeviceSessionTimeoutScheduler#closeTimedOutSessions", result.getRecords().get(0).getTaskCode());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("span_type = 'SCHEDULED_TASK'"));
        assertTrue(sql.contains("trace_id = ?"));
        assertTrue(sql.contains("status = ?"));
        assertTrue(sql.contains("duration_ms >= ?"));
        assertTrue(sql.contains("JSON_UNQUOTE(JSON_EXTRACT(tags_json, '$.taskCode')) = ?"));
        assertTrue(sql.contains("JSON_UNQUOTE(JSON_EXTRACT(tags_json, '$.triggerType')) = ?"));
        assertTrue(sql.contains("started_at >= ?"));
        assertTrue(sql.contains("started_at <= ?"));
    }

    @Test
    void pageMessageArchiveBatchesShouldApplyBatchFiltersWithoutTenantScope() {
        ObservabilityMessageArchiveBatchPageQuery query = new ObservabilityMessageArchiveBatchPageQuery();
        query.setBatchNo(" iot_message_log-20260426000119 ");
        query.setSourceTable("iot_message_log");
        query.setStatus("SUCCEEDED");
        query.setDateFrom("2026-04-26");
        query.setDateTo("2026-04-26");
        query.setPageNum(1L);
        query.setPageSize(20L);

        ObservabilityMessageArchiveBatchVO row = new ObservabilityMessageArchiveBatchVO();
        row.setBatchNo("iot_message_log-20260426000119");
        row.setSourceTable("iot_message_log");
        row.setStatus("SUCCEEDED");
        row.setArchivedRows(16098);
        row.setDeletedRows(16098);
        when(jdbcTemplate.queryForObject(contains("FROM iot_message_log_archive_batch"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L);
        doReturn(List.of(row)).when(jdbcTemplate).query(
                contains("ORDER BY create_time DESC"),
                any(RowMapper.class),
                any(Object[].class)
        );

        PageResult<ObservabilityMessageArchiveBatchVO> result = service.pageMessageArchiveBatches(query, 10001L);

        assertEquals(1L, result.getTotal());
        assertEquals("SUCCEEDED", result.getRecords().get(0).getStatus());
        assertEquals(16098, result.getRecords().get(0).getArchivedRows());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sqlCaptor.capture(), eq(Long.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("batch_no = ?"));
        assertTrue(sql.contains("source_table = ?"));
        assertTrue(sql.contains("status = ?"));
        assertTrue(sql.contains("create_time >= ?"));
        assertTrue(sql.contains("create_time <= ?"));
        assertFalse(sql.contains("tenant_id = ?"));
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
    void listSlowSpanTrendsShouldAggregateBucketsAndFillRangeGaps() {
        ObservabilitySlowSpanTrendQuery query = new ObservabilitySlowSpanTrendQuery();
        query.setSpanType("SLOW_SQL");
        query.setDomainCode("system");
        query.setEventCode("system.error.archive");
        query.setObjectType("sql");
        query.setObjectId("iot_message_log");
        query.setMinDurationMs(1000L);
        query.setBucket("HOUR");
        query.setDateFrom("2026-04-25 09:00:00");
        query.setDateTo("2026-04-25 11:59:59");

        ObservabilitySpanVO first = new ObservabilitySpanVO();
        first.setTraceId("trace-slow-1");
        first.setSpanType("SLOW_SQL");
        first.setDomainCode("system");
        first.setEventCode("system.error.archive");
        first.setObjectType("sql");
        first.setObjectId("iot_message_log");
        first.setStatus("SUCCESS");
        first.setDurationMs(1000L);
        first.setStartedAt(LocalDateTime.of(2026, 4, 25, 9, 10));

        ObservabilitySpanVO second = new ObservabilitySpanVO();
        second.setTraceId("trace-slow-2");
        second.setSpanType("SLOW_SQL");
        second.setDomainCode("system");
        second.setEventCode("system.error.archive");
        second.setObjectType("sql");
        second.setObjectId("iot_message_log");
        second.setStatus("SUCCESS");
        second.setDurationMs(2000L);
        second.setStartedAt(LocalDateTime.of(2026, 4, 25, 9, 20));

        ObservabilitySpanVO third = new ObservabilitySpanVO();
        third.setTraceId("trace-slow-3");
        third.setSpanType("SLOW_SQL");
        third.setDomainCode("system");
        third.setEventCode("system.error.archive");
        third.setObjectType("sql");
        third.setObjectId("iot_message_log");
        third.setStatus("ERROR");
        third.setDurationMs(5000L);
        third.setStartedAt(LocalDateTime.of(2026, 4, 25, 9, 50));

        ObservabilitySpanVO fourth = new ObservabilitySpanVO();
        fourth.setTraceId("trace-slow-4");
        fourth.setSpanType("SLOW_SQL");
        fourth.setDomainCode("system");
        fourth.setEventCode("system.error.archive");
        fourth.setObjectType("sql");
        fourth.setObjectId("iot_message_log");
        fourth.setStatus("SUCCESS");
        fourth.setDurationMs(1500L);
        fourth.setStartedAt(LocalDateTime.of(2026, 4, 25, 10, 5));

        ObservabilitySpanVO fifth = new ObservabilitySpanVO();
        fifth.setTraceId("trace-slow-5");
        fifth.setSpanType("SLOW_SQL");
        fifth.setDomainCode("system");
        fifth.setEventCode("system.error.archive");
        fifth.setObjectType("sql");
        fifth.setObjectId("iot_message_log");
        fifth.setStatus("ERROR");
        fifth.setDurationMs(3000L);
        fifth.setStartedAt(LocalDateTime.of(2026, 4, 25, 10, 40));

        doReturn(List.of(first, second, third, fourth, fifth)).when(jdbcTemplate).query(
                contains("ORDER BY started_at ASC, id ASC"),
                any(RowMapper.class),
                any(Object[].class)
        );

        List<ObservabilitySlowSpanTrendVO> result = service.listSlowSpanTrends(query, 10001L);

        assertEquals(3, result.size());

        ObservabilitySlowSpanTrendVO firstBucket = result.get(0);
        assertEquals("HOUR", firstBucket.getBucket());
        assertEquals(LocalDateTime.of(2026, 4, 25, 9, 0), firstBucket.getBucketStart());
        assertEquals(3L, firstBucket.getTotalCount());
        assertEquals(2L, firstBucket.getSuccessCount());
        assertEquals(1L, firstBucket.getErrorCount());
        assertEquals(33, firstBucket.getErrorRate());
        assertEquals(2667L, firstBucket.getAvgDurationMs());
        assertEquals(5000L, firstBucket.getMaxDurationMs());
        assertEquals(5000L, firstBucket.getP95DurationMs());
        assertEquals(5000L, firstBucket.getP99DurationMs());

        ObservabilitySlowSpanTrendVO secondBucket = result.get(1);
        assertEquals(LocalDateTime.of(2026, 4, 25, 10, 0), secondBucket.getBucketStart());
        assertEquals(2L, secondBucket.getTotalCount());
        assertEquals(1L, secondBucket.getSuccessCount());
        assertEquals(1L, secondBucket.getErrorCount());
        assertEquals(50, secondBucket.getErrorRate());
        assertEquals(2250L, secondBucket.getAvgDurationMs());
        assertEquals(3000L, secondBucket.getMaxDurationMs());
        assertEquals(3000L, secondBucket.getP95DurationMs());
        assertEquals(3000L, secondBucket.getP99DurationMs());

        ObservabilitySlowSpanTrendVO thirdBucket = result.get(2);
        assertEquals(LocalDateTime.of(2026, 4, 25, 11, 0), thirdBucket.getBucketStart());
        assertEquals(0L, thirdBucket.getTotalCount());
        assertEquals(0L, thirdBucket.getSuccessCount());
        assertEquals(0L, thirdBucket.getErrorCount());
        assertEquals(0, thirdBucket.getErrorRate());
        assertNull(thirdBucket.getAvgDurationMs());
        assertNull(thirdBucket.getMaxDurationMs());
        assertNull(thirdBucket.getP95DurationMs());
        assertNull(thirdBucket.getP99DurationMs());

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("tenant_id = ?"));
        assertTrue(sql.contains("span_type = ?"));
        assertTrue(sql.contains("domain_code = ?"));
        assertTrue(sql.contains("event_code = ?"));
        assertTrue(sql.contains("object_type = ?"));
        assertTrue(sql.contains("object_id = ?"));
        assertTrue(sql.contains("duration_ms >= ?"));
        assertTrue(sql.contains("started_at >= ?"));
        assertTrue(sql.contains("started_at <= ?"));
        assertTrue(sql.contains("ORDER BY started_at ASC, id ASC"));
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
