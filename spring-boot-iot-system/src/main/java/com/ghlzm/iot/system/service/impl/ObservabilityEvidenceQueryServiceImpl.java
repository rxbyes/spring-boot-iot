package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.service.ObservabilityEvidenceQueryService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySpanVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceItemVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceVO;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ObservabilityEvidenceQueryServiceImpl implements ObservabilityEvidenceQueryService {

    private static final int TRACE_LIMIT = 500;
    private static final int DEFAULT_SLOW_SUMMARY_LIMIT = 20;
    private static final int MAX_SLOW_SUMMARY_LIMIT = 50;
    private static final DateTimeFormatter SPACE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;

    public ObservabilityEvidenceQueryServiceImpl(JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
    }

    @Override
    public PageResult<ObservabilityBusinessEventVO> pageBusinessEvents(ObservabilityBusinessEventPageQuery query,
                                                                       Long currentUserId) {
        ObservabilityBusinessEventPageQuery criteria = query == null ? new ObservabilityBusinessEventPageQuery() : query;
        long pageNum = PageQueryUtils.normalizePageNum(criteria.getPageNum());
        long pageSize = PageQueryUtils.normalizePageSize(criteria.getPageSize());
        List<Object> args = new ArrayList<>();
        String where = buildBusinessEventWhere(criteria, resolveTenantId(currentUserId), args);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_business_event_log WHERE " + where,
                Long.class,
                args.toArray()
        );
        if (total == null || total == 0L) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Object> rowArgs = new ArrayList<>(args);
        rowArgs.add(pageSize);
        rowArgs.add((pageNum - 1L) * pageSize);
        List<ObservabilityBusinessEventVO> records = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, event_code, event_name, domain_code, action_code,
                       object_type, object_id, object_name, actor_user_id, actor_name, result_status,
                       source_type, evidence_type, evidence_id, request_method, request_uri,
                       duration_ms, error_code, error_message, metadata_json, occurred_at, create_time
                FROM sys_business_event_log
                WHERE %s
                ORDER BY occurred_at DESC, id DESC
                LIMIT ? OFFSET ?
                """.formatted(where),
                this::mapBusinessEvent,
                rowArgs.toArray()
        );
        return PageResult.of(total, pageNum, pageSize, records);
    }

    @Override
    public PageResult<ObservabilitySpanVO> pageSpans(ObservabilitySpanPageQuery query, Long currentUserId) {
        ObservabilitySpanPageQuery criteria = query == null ? new ObservabilitySpanPageQuery() : query;
        long pageNum = PageQueryUtils.normalizePageNum(criteria.getPageNum());
        long pageSize = PageQueryUtils.normalizePageSize(criteria.getPageSize());
        List<Object> args = new ArrayList<>();
        String where = buildSpanWhere(criteria, resolveTenantId(currentUserId), args);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_observability_span_log WHERE " + where,
                Long.class,
                args.toArray()
        );
        if (total == null || total == 0L) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Object> rowArgs = new ArrayList<>(args);
        rowArgs.add(pageSize);
        rowArgs.add((pageNum - 1L) * pageSize);
        List<ObservabilitySpanVO> records = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, parent_span_id, span_type, span_name, domain_code,
                       event_code, object_type, object_id, transport_type, status, duration_ms,
                       started_at, finished_at, error_class, error_message, tags_json, create_time
                FROM sys_observability_span_log
                WHERE %s
                ORDER BY started_at DESC, id DESC
                LIMIT ? OFFSET ?
                """.formatted(where),
                this::mapSpan,
                rowArgs.toArray()
        );
        return PageResult.of(total, pageNum, pageSize, records);
    }

    @Override
    public List<ObservabilitySlowSpanSummaryVO> listSlowSpanSummaries(ObservabilitySlowSpanSummaryQuery query,
                                                                      Long currentUserId) {
        ObservabilitySlowSpanSummaryQuery criteria =
                query == null ? new ObservabilitySlowSpanSummaryQuery() : query;
        List<Object> args = new ArrayList<>();
        String where = buildSlowSpanSummaryWhere(criteria, resolveTenantId(currentUserId), args);
        args.add(normalizeSlowSummaryLimit(criteria.getLimit()));
        return jdbcTemplate.query(
                """
                SELECT span_type, domain_code, event_code, object_type, object_id,
                       COUNT(1) AS total_count,
                       CAST(ROUND(AVG(duration_ms)) AS SIGNED) AS avg_duration_ms,
                       MAX(duration_ms) AS max_duration_ms,
                       SUBSTRING_INDEX(GROUP_CONCAT(trace_id ORDER BY started_at DESC, id DESC), ',', 1) AS latest_trace_id,
                       MAX(started_at) AS latest_started_at
                FROM sys_observability_span_log
                WHERE %s
                GROUP BY span_type, domain_code, event_code, object_type, object_id
                ORDER BY max_duration_ms DESC, total_count DESC
                LIMIT ?
                """.formatted(where),
                this::mapSlowSpanSummary,
                args.toArray()
        );
    }

    @Override
    public ObservabilityTraceEvidenceVO getTraceEvidence(String traceId, Long currentUserId) {
        String normalizedTraceId = normalize(traceId);
        Long tenantId = resolveTenantId(currentUserId);
        ObservabilityTraceEvidenceVO result = new ObservabilityTraceEvidenceVO();
        result.setTraceId(normalizedTraceId);
        if (!StringUtils.hasText(normalizedTraceId) || tenantId == null) {
            return result;
        }
        List<ObservabilityBusinessEventVO> businessEvents = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, event_code, event_name, domain_code, action_code,
                       object_type, object_id, object_name, actor_user_id, actor_name, result_status,
                       source_type, evidence_type, evidence_id, request_method, request_uri,
                       duration_ms, error_code, error_message, metadata_json, occurred_at, create_time
                FROM sys_business_event_log
                WHERE deleted = 0 AND tenant_id = ? AND trace_id = ?
                ORDER BY occurred_at ASC, id ASC
                LIMIT ?
                """,
                this::mapBusinessEvent,
                tenantId,
                normalizedTraceId,
                TRACE_LIMIT
        );
        List<ObservabilitySpanVO> spans = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, parent_span_id, span_type, span_name, domain_code,
                       event_code, object_type, object_id, transport_type, status, duration_ms,
                       started_at, finished_at, error_class, error_message, tags_json, create_time
                FROM sys_observability_span_log
                WHERE deleted = 0 AND tenant_id = ? AND trace_id = ?
                ORDER BY started_at ASC, id ASC
                LIMIT ?
                """,
                this::mapSpan,
                tenantId,
                normalizedTraceId,
                TRACE_LIMIT
        );
        result.setBusinessEvents(businessEvents);
        result.setSpans(spans);
        List<ObservabilityTraceEvidenceItemVO> timeline = new ArrayList<>();
        businessEvents.stream().map(this::toTimelineItem).forEach(timeline::add);
        spans.stream().map(this::toTimelineItem).forEach(timeline::add);
        timeline.sort(Comparator
                .comparing(ObservabilityTraceEvidenceItemVO::getOccurredAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ObservabilityTraceEvidenceItemVO::getItemId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        result.setTimeline(timeline);
        return result;
    }

    private String buildBusinessEventWhere(ObservabilityBusinessEventPageQuery query, Long tenantId, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        clauses.add("deleted = 0");
        if (tenantId != null) {
            clauses.add("tenant_id = ?");
            args.add(tenantId);
        }
        appendEquals(clauses, args, "trace_id", query.getTraceId());
        appendEquals(clauses, args, "event_code", query.getEventCode());
        appendEquals(clauses, args, "domain_code", query.getDomainCode());
        appendEquals(clauses, args, "action_code", query.getActionCode());
        appendEquals(clauses, args, "object_type", query.getObjectType());
        appendEquals(clauses, args, "object_id", query.getObjectId());
        appendEquals(clauses, args, "result_status", query.getResultStatus());
        appendDateRange(clauses, args, "occurred_at", query.getDateFrom(), query.getDateTo());
        return String.join(" AND ", clauses);
    }

    private String buildSpanWhere(ObservabilitySpanPageQuery query, Long tenantId, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        clauses.add("deleted = 0");
        if (tenantId != null) {
            clauses.add("tenant_id = ?");
            args.add(tenantId);
        }
        appendEquals(clauses, args, "trace_id", query.getTraceId());
        appendEquals(clauses, args, "span_type", query.getSpanType());
        appendEquals(clauses, args, "event_code", query.getEventCode());
        appendEquals(clauses, args, "domain_code", query.getDomainCode());
        appendEquals(clauses, args, "object_type", query.getObjectType());
        appendEquals(clauses, args, "object_id", query.getObjectId());
        appendEquals(clauses, args, "status", query.getStatus());
        if (query.getMinDurationMs() != null && query.getMinDurationMs() > 0L) {
            clauses.add("duration_ms >= ?");
            args.add(query.getMinDurationMs());
        }
        appendDateRange(clauses, args, "started_at", query.getDateFrom(), query.getDateTo());
        return String.join(" AND ", clauses);
    }

    private String buildSlowSpanSummaryWhere(ObservabilitySlowSpanSummaryQuery query, Long tenantId, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        clauses.add("deleted = 0");
        clauses.add("duration_ms IS NOT NULL");
        if (tenantId != null) {
            clauses.add("tenant_id = ?");
            args.add(tenantId);
        }
        appendEquals(clauses, args, "span_type", query.getSpanType());
        appendEquals(clauses, args, "event_code", query.getEventCode());
        appendEquals(clauses, args, "domain_code", query.getDomainCode());
        appendEquals(clauses, args, "object_type", query.getObjectType());
        appendEquals(clauses, args, "object_id", query.getObjectId());
        appendEquals(clauses, args, "status", query.getStatus());
        if (query.getMinDurationMs() != null && query.getMinDurationMs() > 0L) {
            clauses.add("duration_ms >= ?");
            args.add(query.getMinDurationMs());
        }
        appendDateRange(clauses, args, "started_at", query.getDateFrom(), query.getDateTo());
        return String.join(" AND ", clauses);
    }

    private int normalizeSlowSummaryLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_SLOW_SUMMARY_LIMIT;
        }
        return Math.min(limit, MAX_SLOW_SUMMARY_LIMIT);
    }

    private Long resolveTenantId(Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        DataPermissionContext context = permissionService.getDataPermissionContext(currentUserId);
        return context == null ? null : context.tenantId();
    }

    private void appendEquals(List<String> clauses, List<Object> args, String column, String value) {
        String normalized = normalize(value);
        if (StringUtils.hasText(normalized)) {
            clauses.add(column + " = ?");
            args.add(normalized);
        }
    }

    private void appendDateRange(List<String> clauses, List<Object> args, String column, String dateFrom, String dateTo) {
        LocalDateTime from = parseDateTime(dateFrom, false);
        LocalDateTime to = parseDateTime(dateTo, true);
        if (from != null) {
            clauses.add(column + " >= ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            clauses.add(column + " <= ?");
            args.add(Timestamp.valueOf(to));
        }
    }

    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized);
                return endOfDay ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
            }
            if (normalized.contains(" ")) {
                return LocalDateTime.parse(normalized, SPACE_DATE_TIME);
            }
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BizException(400, "时间参数格式不正确，请使用 yyyy-MM-dd、yyyy-MM-dd HH:mm:ss 或 ISO-8601 格式");
        }
    }

    private ObservabilityBusinessEventVO mapBusinessEvent(ResultSet rs, int rowNum) throws SQLException {
        ObservabilityBusinessEventVO vo = new ObservabilityBusinessEventVO();
        vo.setId(nullableLong(rs, "id"));
        vo.setTenantId(nullableLong(rs, "tenant_id"));
        vo.setTraceId(rs.getString("trace_id"));
        vo.setEventCode(rs.getString("event_code"));
        vo.setEventName(rs.getString("event_name"));
        vo.setDomainCode(rs.getString("domain_code"));
        vo.setActionCode(rs.getString("action_code"));
        vo.setObjectType(rs.getString("object_type"));
        vo.setObjectId(rs.getString("object_id"));
        vo.setObjectName(rs.getString("object_name"));
        vo.setActorUserId(nullableLong(rs, "actor_user_id"));
        vo.setActorName(rs.getString("actor_name"));
        vo.setResultStatus(rs.getString("result_status"));
        vo.setSourceType(rs.getString("source_type"));
        vo.setEvidenceType(rs.getString("evidence_type"));
        vo.setEvidenceId(rs.getString("evidence_id"));
        vo.setRequestMethod(rs.getString("request_method"));
        vo.setRequestUri(rs.getString("request_uri"));
        vo.setDurationMs(nullableLong(rs, "duration_ms"));
        vo.setErrorCode(rs.getString("error_code"));
        vo.setErrorMessage(rs.getString("error_message"));
        vo.setMetadataJson(rs.getString("metadata_json"));
        vo.setOccurredAt(nullableDateTime(rs, "occurred_at"));
        vo.setCreateTime(nullableDateTime(rs, "create_time"));
        return vo;
    }

    private ObservabilitySpanVO mapSpan(ResultSet rs, int rowNum) throws SQLException {
        ObservabilitySpanVO vo = new ObservabilitySpanVO();
        vo.setId(nullableLong(rs, "id"));
        vo.setTenantId(nullableLong(rs, "tenant_id"));
        vo.setTraceId(rs.getString("trace_id"));
        vo.setParentSpanId(nullableLong(rs, "parent_span_id"));
        vo.setSpanType(rs.getString("span_type"));
        vo.setSpanName(rs.getString("span_name"));
        vo.setDomainCode(rs.getString("domain_code"));
        vo.setEventCode(rs.getString("event_code"));
        vo.setObjectType(rs.getString("object_type"));
        vo.setObjectId(rs.getString("object_id"));
        vo.setTransportType(rs.getString("transport_type"));
        vo.setStatus(rs.getString("status"));
        vo.setDurationMs(nullableLong(rs, "duration_ms"));
        vo.setStartedAt(nullableDateTime(rs, "started_at"));
        vo.setFinishedAt(nullableDateTime(rs, "finished_at"));
        vo.setErrorClass(rs.getString("error_class"));
        vo.setErrorMessage(rs.getString("error_message"));
        vo.setTagsJson(rs.getString("tags_json"));
        vo.setCreateTime(nullableDateTime(rs, "create_time"));
        return vo;
    }

    private ObservabilitySlowSpanSummaryVO mapSlowSpanSummary(ResultSet rs, int rowNum) throws SQLException {
        ObservabilitySlowSpanSummaryVO vo = new ObservabilitySlowSpanSummaryVO();
        vo.setSpanType(rs.getString("span_type"));
        vo.setDomainCode(rs.getString("domain_code"));
        vo.setEventCode(rs.getString("event_code"));
        vo.setObjectType(rs.getString("object_type"));
        vo.setObjectId(rs.getString("object_id"));
        vo.setTotalCount(nullableLong(rs, "total_count"));
        vo.setAvgDurationMs(nullableLong(rs, "avg_duration_ms"));
        vo.setMaxDurationMs(nullableLong(rs, "max_duration_ms"));
        vo.setLatestTraceId(rs.getString("latest_trace_id"));
        vo.setLatestStartedAt(nullableDateTime(rs, "latest_started_at"));
        return vo;
    }

    private ObservabilityTraceEvidenceItemVO toTimelineItem(ObservabilityBusinessEventVO event) {
        ObservabilityTraceEvidenceItemVO item = new ObservabilityTraceEvidenceItemVO();
        item.setItemType("BUSINESS_EVENT");
        item.setItemId(event.getId());
        item.setTraceId(event.getTraceId());
        item.setCode(event.getEventCode());
        item.setName(event.getEventName());
        item.setDomainCode(event.getDomainCode());
        item.setObjectType(event.getObjectType());
        item.setObjectId(event.getObjectId());
        item.setStatus(event.getResultStatus());
        item.setDurationMs(event.getDurationMs());
        item.setOccurredAt(event.getOccurredAt());
        return item;
    }

    private ObservabilityTraceEvidenceItemVO toTimelineItem(ObservabilitySpanVO span) {
        ObservabilityTraceEvidenceItemVO item = new ObservabilityTraceEvidenceItemVO();
        item.setItemType("SPAN");
        item.setItemId(span.getId());
        item.setTraceId(span.getTraceId());
        item.setCode(span.getSpanType());
        item.setName(span.getSpanName());
        item.setDomainCode(span.getDomainCode());
        item.setObjectType(span.getObjectType());
        item.setObjectId(span.getObjectId());
        item.setStatus(span.getStatus());
        item.setDurationMs(span.getDurationMs());
        item.setOccurredAt(span.getStartedAt());
        return item;
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value instanceof Number number ? number.longValue() : null;
    }

    private LocalDateTime nullableDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
