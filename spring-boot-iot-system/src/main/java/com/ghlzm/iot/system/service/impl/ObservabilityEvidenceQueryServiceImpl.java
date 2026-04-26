package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.service.ObservabilityEvidenceQueryService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import com.ghlzm.iot.system.service.model.ObservabilityBusinessEventPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityMessageArchiveBatchPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilityScheduledTaskPageQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanSummaryQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySlowSpanTrendQuery;
import com.ghlzm.iot.system.service.model.ObservabilitySpanPageQuery;
import com.ghlzm.iot.system.vo.ObservabilityBusinessEventVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchCompareSourceVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchCompareSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchCompareTableVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchCompareVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchReportPreviewVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchReportTableSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilityMessageArchiveBatchVO;
import com.ghlzm.iot.system.vo.ObservabilityScheduledTaskVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanSummaryVO;
import com.ghlzm.iot.system.vo.ObservabilitySlowSpanTrendVO;
import com.ghlzm.iot.system.vo.ObservabilitySpanVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceItemVO;
import com.ghlzm.iot.system.vo.ObservabilityTraceEvidenceVO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ObservabilityEvidenceQueryServiceImpl implements ObservabilityEvidenceQueryService {

    private static final int TRACE_LIMIT = 500;
    private static final int DEFAULT_SLOW_SUMMARY_LIMIT = 20;
    private static final int MAX_SLOW_SUMMARY_LIMIT = 50;
    private static final String SLOW_TREND_BUCKET_HOUR = "HOUR";
    private static final String SLOW_TREND_BUCKET_DAY = "DAY";
    private static final String ARCHIVE_COMPARE_MATCHED = "MATCHED";
    private static final String ARCHIVE_COMPARE_DRIFTED = "DRIFTED";
    private static final String ARCHIVE_COMPARE_PARTIAL = "PARTIAL";
    private static final String ARCHIVE_COMPARE_UNAVAILABLE = "UNAVAILABLE";
    private static final int MARKDOWN_PREVIEW_MAX_LINES = 80;
    private static final int MARKDOWN_PREVIEW_MAX_CHARS = 6000;
    private static final DateTimeFormatter SPACE_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final Path repoRoot;
    private final Path observabilityLogDir;

    public ObservabilityEvidenceQueryServiceImpl(JdbcTemplate jdbcTemplate, PermissionService permissionService) {
        this(jdbcTemplate, permissionService, Paths.get("").toAbsolutePath().normalize());
    }

    ObservabilityEvidenceQueryServiceImpl(JdbcTemplate jdbcTemplate, PermissionService permissionService, Path repoRoot) {
        this.jdbcTemplate = jdbcTemplate;
        this.permissionService = permissionService;
        this.repoRoot = repoRoot.toAbsolutePath().normalize();
        this.observabilityLogDir = this.repoRoot.resolve("logs").resolve("observability").normalize();
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
    public PageResult<ObservabilityScheduledTaskVO> pageScheduledTasks(ObservabilityScheduledTaskPageQuery query,
                                                                       Long currentUserId) {
        ObservabilityScheduledTaskPageQuery criteria =
                query == null ? new ObservabilityScheduledTaskPageQuery() : query;
        long pageNum = PageQueryUtils.normalizePageNum(criteria.getPageNum());
        long pageSize = PageQueryUtils.normalizePageSize(criteria.getPageSize());
        List<Object> args = new ArrayList<>();
        String where = buildScheduledTaskWhere(criteria, resolveTenantId(currentUserId), args);
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
        List<ObservabilityScheduledTaskVO> records = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, domain_code, status, duration_ms,
                       started_at, finished_at, error_class, error_message, tags_json
                FROM sys_observability_span_log
                WHERE %s
                ORDER BY started_at DESC, id DESC
                LIMIT ? OFFSET ?
                """.formatted(where),
                this::mapScheduledTask,
                rowArgs.toArray()
        );
        return PageResult.of(total, pageNum, pageSize, records);
    }

    @Override
    public PageResult<ObservabilityMessageArchiveBatchVO> pageMessageArchiveBatches(
            ObservabilityMessageArchiveBatchPageQuery query,
            Long currentUserId
    ) {
        ObservabilityMessageArchiveBatchPageQuery criteria =
                query == null ? new ObservabilityMessageArchiveBatchPageQuery() : query;
        long pageNum = PageQueryUtils.normalizePageNum(criteria.getPageNum());
        long pageSize = PageQueryUtils.normalizePageSize(criteria.getPageSize());
        List<Object> args = new ArrayList<>();
        String where = buildMessageArchiveBatchWhere(criteria, args);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM iot_message_log_archive_batch WHERE " + where,
                Long.class,
                args.toArray()
        );
        if (total == null || total == 0L) {
            return PageResult.empty(pageNum, pageSize);
        }
        List<Object> rowArgs = new ArrayList<>(args);
        rowArgs.add(pageSize);
        rowArgs.add((pageNum - 1L) * pageSize);
        List<ObservabilityMessageArchiveBatchVO> records = jdbcTemplate.query(
                """
                SELECT id, batch_no, source_table, governance_mode, status, retention_days,
                       cutoff_at, confirm_report_path, confirm_report_generated_at,
                       confirmed_expired_rows, candidate_rows, archived_rows, deleted_rows,
                       failed_reason, artifacts_json, create_time, update_time
                FROM iot_message_log_archive_batch
                WHERE %s
                ORDER BY create_time DESC, id DESC
                LIMIT ? OFFSET ?
                """.formatted(where),
                this::mapMessageArchiveBatch,
                rowArgs.toArray()
        );
        return PageResult.of(total, pageNum, pageSize, records);
    }

    @Override
    public ObservabilityMessageArchiveBatchReportPreviewVO getMessageArchiveBatchReportPreview(String batchNo,
                                                                                               Long currentUserId) {
        String normalizedBatchNo = normalize(batchNo);
        if (!StringUtils.hasText(normalizedBatchNo)) {
            throw new BizException(400, "batchNo 不能为空");
        }
        ObservabilityMessageArchiveBatchVO batch = loadMessageArchiveBatchByBatchNo(normalizedBatchNo);
        if (batch == null) {
            throw new BizException(404, "归档批次不存在");
        }
        return buildMessageArchiveBatchReportPreview(batch);
    }

    @Override
    public ObservabilityMessageArchiveBatchCompareVO getMessageArchiveBatchCompare(String batchNo, Long currentUserId) {
        String normalizedBatchNo = normalize(batchNo);
        if (!StringUtils.hasText(normalizedBatchNo)) {
            throw new BizException(400, "batchNo 不能为空");
        }
        ObservabilityMessageArchiveBatchVO batch = loadMessageArchiveBatchByBatchNo(normalizedBatchNo);
        if (batch == null) {
            throw new BizException(404, "归档批次不存在");
        }
        return buildMessageArchiveBatchCompare(batch);
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
    public List<ObservabilitySlowSpanTrendVO> listSlowSpanTrends(ObservabilitySlowSpanTrendQuery query,
                                                                 Long currentUserId) {
        ObservabilitySlowSpanTrendQuery criteria =
                query == null ? new ObservabilitySlowSpanTrendQuery() : query;
        LocalDateTime from = parseDateTime(criteria.getDateFrom(), false);
        LocalDateTime to = parseDateTime(criteria.getDateTo(), true);
        if (from != null && to != null && from.isAfter(to)) {
            throw new BizException(400, "dateFrom 不能晚于 dateTo");
        }
        String bucket = normalizeSlowTrendBucket(criteria.getBucket());
        List<Object> args = new ArrayList<>();
        String where = buildSlowSpanTrendWhere(criteria, resolveTenantId(currentUserId), from, to, args);
        List<ObservabilitySpanVO> spans = jdbcTemplate.query(
                """
                SELECT id, tenant_id, trace_id, parent_span_id, span_type, span_name, domain_code,
                       event_code, object_type, object_id, transport_type, status, duration_ms,
                       started_at, finished_at, error_class, error_message, tags_json, create_time
                FROM sys_observability_span_log
                WHERE %s
                ORDER BY started_at ASC, id ASC
                """.formatted(where),
                this::mapSpan,
                args.toArray()
        );
        return aggregateSlowSpanTrends(spans, bucket, from, to);
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

    private ObservabilityMessageArchiveBatchVO loadMessageArchiveBatchByBatchNo(String batchNo) {
        List<ObservabilityMessageArchiveBatchVO> rows = jdbcTemplate.query(
                """
                SELECT id, batch_no, source_table, governance_mode, status, retention_days,
                       cutoff_at, confirm_report_path, confirm_report_generated_at,
                       confirmed_expired_rows, candidate_rows, archived_rows, deleted_rows,
                       failed_reason, artifacts_json, create_time, update_time
                FROM iot_message_log_archive_batch
                WHERE batch_no = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                this::mapMessageArchiveBatch,
                batchNo
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ObservabilityMessageArchiveBatchReportPreviewVO buildMessageArchiveBatchReportPreview(
            ObservabilityMessageArchiveBatchVO batch
    ) {
        ObservabilityMessageArchiveBatchReportPreviewVO preview = new ObservabilityMessageArchiveBatchReportPreviewVO();
        preview.setBatchNo(batch.getBatchNo());
        preview.setSourceTable(batch.getSourceTable());
        preview.setStatus(batch.getStatus());
        preview.setConfirmReportPath(batch.getConfirmReportPath());
        preview.setConfirmReportGeneratedAt(batch.getConfirmReportGeneratedAt());
        preview.setAvailable(false);
        preview.setMarkdownAvailable(false);
        preview.setMarkdownTruncated(false);
        preview.setTableSummaries(List.of());

        String reportPath = normalize(batch.getConfirmReportPath());
        if (!StringUtils.hasText(reportPath)) {
            return markPreviewUnavailable(preview, "MISSING_REPORT_PATH", "当前批次未绑定确认报告");
        }
        Path resolvedJsonPath = resolveAllowedObservabilityPath(reportPath);
        if (resolvedJsonPath == null) {
            return markPreviewUnavailable(preview, "REPORT_PATH_REJECTED", "确认报告路径超出允许目录");
        }
        preview.setResolvedJsonPath(toDisplayPath(resolvedJsonPath));
        if (!Files.exists(resolvedJsonPath)) {
            return markPreviewUnavailable(preview, "REPORT_JSON_NOT_FOUND", "确认报告 JSON 文件不存在");
        }
        try {
            Map<String, Object> payload = readJsonMap(resolvedJsonPath);
            preview.setAvailable(true);
            preview.setSummary(buildPreviewSummary(payload));
            preview.setTableSummaries(buildTableSummaries(payload.get("tables")));
            preview.setFileLastModifiedAt(lastModifiedAt(resolvedJsonPath));

            Path resolvedMarkdownPath = resolveMarkdownPath(resolvedJsonPath);
            preview.setResolvedMarkdownPath(toDisplayPath(resolvedMarkdownPath));
            if (Files.exists(resolvedMarkdownPath)) {
                MarkdownPreview markdownPreview = readMarkdownPreview(resolvedMarkdownPath);
                preview.setMarkdownAvailable(true);
                preview.setMarkdownPreview(markdownPreview.content());
                preview.setMarkdownTruncated(markdownPreview.truncated());
            }
            return preview;
        } catch (IOException ex) {
            return markPreviewUnavailable(preview, "REPORT_PARSE_FAILED", "确认报告解析失败");
        }
    }

    private ObservabilityMessageArchiveBatchCompareVO buildMessageArchiveBatchCompare(
            ObservabilityMessageArchiveBatchVO batch
    ) {
        String reportPath = normalize(batch.getConfirmReportPath());
        if (!StringUtils.hasText(reportPath)) {
            return buildUnavailableCompare(batch, "当前批次未绑定确认报告");
        }
        Path dryRunJsonPath = resolveAllowedObservabilityPath(reportPath);
        if (dryRunJsonPath == null) {
            return buildUnavailableCompare(batch, "确认报告路径超出允许目录");
        }
        if (!Files.exists(dryRunJsonPath)) {
            return buildUnavailableCompare(batch, "确认报告 JSON 文件不存在");
        }

        Map<String, Object> dryRunPayload;
        try {
            dryRunPayload = readJsonMap(dryRunJsonPath);
        } catch (IOException ex) {
            return buildUnavailableCompare(batch, "确认报告解析失败");
        }
        String applyReportPath = extractReportJsonPath(batch.getArtifactsJson());
        if (!StringUtils.hasText(applyReportPath)) {
            return buildPartialCompare(batch, dryRunJsonPath, dryRunPayload, "缺少 apply 报告，仅完成部分比对");
        }
        Path applyJsonPath = resolveAllowedObservabilityPath(applyReportPath);
        if (applyJsonPath == null) {
            return buildPartialCompare(batch, dryRunJsonPath, dryRunPayload, "apply 报告路径超出允许目录，仅完成部分比对");
        }
        if (!Files.exists(applyJsonPath)) {
            return buildPartialCompare(batch, dryRunJsonPath, dryRunPayload, "apply 报告 JSON 文件不存在，仅完成部分比对");
        }

        Map<String, Object> applyPayload;
        try {
            applyPayload = readJsonMap(applyJsonPath);
        } catch (IOException ex) {
            return buildPartialCompare(batch, dryRunJsonPath, dryRunPayload, "apply 报告解析失败，仅完成部分比对");
        }
        return buildCompleteCompare(batch, dryRunJsonPath, applyJsonPath, dryRunPayload, applyPayload);
    }

    private ObservabilityMessageArchiveBatchCompareVO buildBaseCompare(ObservabilityMessageArchiveBatchVO batch) {
        ObservabilityMessageArchiveBatchCompareVO compare = new ObservabilityMessageArchiveBatchCompareVO();
        compare.setBatchNo(batch.getBatchNo());
        compare.setSourceTable(batch.getSourceTable());
        compare.setStatus(batch.getStatus());
        compare.setTableComparisons(List.of());
        ObservabilityMessageArchiveBatchCompareSourceVO sources = new ObservabilityMessageArchiveBatchCompareSourceVO();
        sources.setConfirmReportPath(batch.getConfirmReportPath());
        sources.setDryRunAvailable(false);
        sources.setApplyAvailable(false);
        compare.setSources(sources);
        return compare;
    }

    private ObservabilityMessageArchiveBatchCompareVO buildUnavailableCompare(
            ObservabilityMessageArchiveBatchVO batch,
            String reasonMessage
    ) {
        ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
        compare.setCompareStatus(ARCHIVE_COMPARE_UNAVAILABLE);
        compare.setCompareMessage(reasonMessage);
        return compare;
    }

    private ObservabilityMessageArchiveBatchCompareVO buildPartialCompare(
            ObservabilityMessageArchiveBatchVO batch,
            Path dryRunJsonPath,
            Map<String, Object> dryRunPayload,
            String reasonMessage
    ) {
        ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
        compare.setCompareStatus(ARCHIVE_COMPARE_PARTIAL);
        compare.setCompareMessage(reasonMessage);
        compare.getSources().setDryRunAvailable(true);
        compare.getSources().setApplyAvailable(false);
        compare.getSources().setResolvedDryRunJsonPath(toDisplayPath(dryRunJsonPath));
        compare.setSummaryCompare(buildPartialSummary(batch, dryRunPayload));
        compare.setTableComparisons(buildPartialTableComparisons(dryRunPayload));
        return compare;
    }

    private ObservabilityMessageArchiveBatchCompareVO buildCompleteCompare(
            ObservabilityMessageArchiveBatchVO batch,
            Path dryRunJsonPath,
            Path applyJsonPath,
            Map<String, Object> dryRunPayload,
            Map<String, Object> applyPayload
    ) {
        ObservabilityMessageArchiveBatchCompareVO compare = buildBaseCompare(batch);
        compare.getSources().setDryRunAvailable(true);
        compare.getSources().setApplyAvailable(true);
        compare.getSources().setResolvedDryRunJsonPath(toDisplayPath(dryRunJsonPath));
        compare.getSources().setResolvedApplyJsonPath(toDisplayPath(applyJsonPath));
        ObservabilityMessageArchiveBatchCompareSummaryVO summaryCompare =
                buildCompleteSummary(batch, dryRunPayload, applyPayload);
        List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons =
                buildTableComparisons(dryRunPayload, applyPayload);
        boolean tablesMatched = tableComparisons.stream()
                .allMatch(item -> !Boolean.FALSE.equals(item.getMatched()));
        boolean matched = Boolean.TRUE.equals(summaryCompare.getMatched()) && tablesMatched;
        compare.setSummaryCompare(summaryCompare);
        compare.setTableComparisons(tableComparisons);
        compare.setCompareStatus(matched ? ARCHIVE_COMPARE_MATCHED : ARCHIVE_COMPARE_DRIFTED);
        compare.setCompareMessage(matched ? "已按确认结果落地" : "执行结果与确认结果存在偏差");
        return compare;
    }

    private ObservabilityMessageArchiveBatchCompareSummaryVO buildPartialSummary(
            ObservabilityMessageArchiveBatchVO batch,
            Map<String, Object> dryRunPayload
    ) {
        Map<String, Object> dryRunSummary = nestedMap(dryRunPayload.get("summary"));
        ObservabilityMessageArchiveBatchCompareSummaryVO summary = new ObservabilityMessageArchiveBatchCompareSummaryVO();
        summary.setConfirmedExpiredRows(toLong(batch.getConfirmedExpiredRows()));
        summary.setDryRunExpiredRows(longValue(dryRunSummary.get("expiredRows")));
        summary.setApplyArchivedRows(toLong(batch.getArchivedRows()));
        summary.setApplyDeletedRows(toLong(batch.getDeletedRows()));
        summary.setRemainingExpiredRows(null);
        summary.setDeltaConfirmedVsDeleted(safeSubtract(summary.getConfirmedExpiredRows(), summary.getApplyDeletedRows()));
        summary.setDeltaDryRunVsDeleted(safeSubtract(summary.getDryRunExpiredRows(), summary.getApplyDeletedRows()));
        summary.setMatched(null);
        return summary;
    }

    private ObservabilityMessageArchiveBatchCompareSummaryVO buildCompleteSummary(
            ObservabilityMessageArchiveBatchVO batch,
            Map<String, Object> dryRunPayload,
            Map<String, Object> applyPayload
    ) {
        Map<String, Object> dryRunSummary = nestedMap(dryRunPayload.get("summary"));
        Map<String, Object> applySummary = nestedMap(applyPayload.get("summary"));
        List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons =
                buildTableComparisons(dryRunPayload, applyPayload);
        ObservabilityMessageArchiveBatchCompareSummaryVO summary = new ObservabilityMessageArchiveBatchCompareSummaryVO();
        summary.setConfirmedExpiredRows(toLong(batch.getConfirmedExpiredRows()));
        summary.setDryRunExpiredRows(longValue(dryRunSummary.get("expiredRows")));
        summary.setApplyArchivedRows(defaultLong(longValue(applySummary.get("archivedRows")), toLong(batch.getArchivedRows())));
        summary.setApplyDeletedRows(defaultLong(longValue(applySummary.get("deletedRows")), toLong(batch.getDeletedRows())));
        summary.setRemainingExpiredRows(sumRemainingExpiredRows(tableComparisons));
        summary.setDeltaConfirmedVsDeleted(safeSubtract(summary.getConfirmedExpiredRows(), summary.getApplyDeletedRows()));
        summary.setDeltaDryRunVsDeleted(safeSubtract(summary.getDryRunExpiredRows(), summary.getApplyDeletedRows()));
        boolean matched = Objects.equals(summary.getConfirmedExpiredRows(), summary.getApplyDeletedRows())
                && Objects.equals(summary.getDryRunExpiredRows(), summary.getApplyDeletedRows())
                && Objects.equals(summary.getRemainingExpiredRows(), 0L)
                && (!"iot_message_log".equalsIgnoreCase(normalize(batch.getSourceTable()))
                || Objects.equals(summary.getApplyArchivedRows(), summary.getApplyDeletedRows()));
        summary.setMatched(matched);
        return summary;
    }

    private List<ObservabilityMessageArchiveBatchCompareTableVO> buildPartialTableComparisons(
            Map<String, Object> dryRunPayload
    ) {
        Map<String, Object> dryRunTables = nestedMap(dryRunPayload.get("tables"));
        if (dryRunTables.isEmpty()) {
            return List.of();
        }
        List<ObservabilityMessageArchiveBatchCompareTableVO> items = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dryRunTables.entrySet()) {
            Map<String, Object> dryRunTable = nestedMap(entry.getValue());
            ObservabilityMessageArchiveBatchCompareTableVO item = new ObservabilityMessageArchiveBatchCompareTableVO();
            item.setTableName(entry.getKey());
            item.setLabel(defaultIfBlank(stringValue(dryRunTable.get("label")), entry.getKey()));
            item.setDryRunExpiredRows(longValue(dryRunTable.get("expiredRows")));
            item.setMatched(null);
            item.setReason("缺少 apply 分表证据");
            items.add(item);
        }
        return items;
    }

    private List<ObservabilityMessageArchiveBatchCompareTableVO> buildTableComparisons(
            Map<String, Object> dryRunPayload,
            Map<String, Object> applyPayload
    ) {
        Map<String, Object> dryRunTables = nestedMap(dryRunPayload.get("tables"));
        Map<String, Object> applyTables = nestedMap(applyPayload.get("tables"));
        Set<String> tableNames = new LinkedHashSet<>();
        tableNames.addAll(dryRunTables.keySet());
        tableNames.addAll(applyTables.keySet());
        if (tableNames.isEmpty()) {
            return List.of();
        }
        List<ObservabilityMessageArchiveBatchCompareTableVO> items = new ArrayList<>();
        for (String tableName : tableNames) {
            Map<String, Object> dryRunTable = nestedMap(dryRunTables.get(tableName));
            Map<String, Object> applyTable = nestedMap(applyTables.get(tableName));
            ObservabilityMessageArchiveBatchCompareTableVO item = new ObservabilityMessageArchiveBatchCompareTableVO();
            item.setTableName(tableName);
            item.setLabel(defaultIfBlank(
                    stringValue(applyTable.get("label")),
                    defaultIfBlank(stringValue(dryRunTable.get("label")), tableName)
            ));
            item.setDryRunExpiredRows(longValue(dryRunTable.get("expiredRows")));
            item.setApplyArchivedRows(longValue(applyTable.get("archivedRows")));
            item.setApplyDeletedRows(longValue(applyTable.get("deletedRows")));
            item.setApplyRemainingExpiredRows(longValue(applyTable.get("remainingExpiredRows")));
            item.setDeltaDryRunVsDeleted(safeSubtract(item.getDryRunExpiredRows(), item.getApplyDeletedRows()));
            boolean matched = Objects.equals(item.getDryRunExpiredRows(), item.getApplyDeletedRows())
                    && Objects.equals(item.getApplyRemainingExpiredRows(), 0L)
                    && (item.getApplyArchivedRows() == null || Objects.equals(item.getApplyArchivedRows(), item.getApplyDeletedRows()));
            item.setMatched(matched);
            if (!matched) {
                item.setReason(resolveTableReason(item));
            }
            items.add(item);
        }
        return items;
    }

    private String resolveTableReason(ObservabilityMessageArchiveBatchCompareTableVO item) {
        if (!Objects.equals(item.getDryRunExpiredRows(), item.getApplyDeletedRows())) {
            return "dry-run 过期量与 apply 删除量不一致";
        }
        if (!Objects.equals(item.getApplyRemainingExpiredRows(), 0L)) {
            return "apply 后仍存在剩余过期量";
        }
        if (item.getApplyArchivedRows() != null && !Objects.equals(item.getApplyArchivedRows(), item.getApplyDeletedRows())) {
            return "apply 归档量与删除量不一致";
        }
        return "存在未分类偏差";
    }

    private Map<String, Object> nestedMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return result;
    }

    private String extractReportJsonPath(String artifactsJson) {
        Map<String, Object> artifacts = parseJsonMap(artifactsJson);
        return normalize(stringValue(artifacts.get("reportJsonPath")));
    }

    private Long sumRemainingExpiredRows(List<ObservabilityMessageArchiveBatchCompareTableVO> tableComparisons) {
        Long total = null;
        for (ObservabilityMessageArchiveBatchCompareTableVO table : tableComparisons) {
            if (table.getApplyRemainingExpiredRows() == null) {
                continue;
            }
            total = total == null ? table.getApplyRemainingExpiredRows() : total + table.getApplyRemainingExpiredRows();
        }
        return total;
    }

    private Long safeSubtract(Long left, Long right) {
        if (left == null || right == null) {
            return null;
        }
        return left - right;
    }

    private Long defaultLong(Long preferred, Long fallback) {
        return preferred != null ? preferred : fallback;
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
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

    private String buildScheduledTaskWhere(ObservabilityScheduledTaskPageQuery query,
                                           Long tenantId,
                                           List<Object> args) {
        List<String> clauses = new ArrayList<>();
        clauses.add("deleted = 0");
        clauses.add("span_type = 'SCHEDULED_TASK'");
        if (tenantId != null) {
            clauses.add("tenant_id = ?");
            args.add(tenantId);
        }
        appendEquals(clauses, args, "trace_id", query.getTraceId());
        appendEquals(clauses, args, "domain_code", query.getDomainCode());
        appendEquals(clauses, args, "status", query.getStatus());
        appendJsonEquals(clauses, args, "$.taskCode", query.getTaskCode());
        appendJsonEquals(clauses, args, "$.triggerType", query.getTriggerType());
        if (query.getMinDurationMs() != null && query.getMinDurationMs() > 0L) {
            clauses.add("duration_ms >= ?");
            args.add(query.getMinDurationMs());
        }
        appendDateRange(clauses, args, "started_at", query.getDateFrom(), query.getDateTo());
        return String.join(" AND ", clauses);
    }

    private String buildMessageArchiveBatchWhere(ObservabilityMessageArchiveBatchPageQuery query, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        clauses.add("1 = 1");
        appendEquals(clauses, args, "batch_no", query.getBatchNo());
        appendEquals(clauses, args, "source_table", query.getSourceTable());
        appendEquals(clauses, args, "status", query.getStatus());
        appendDateRange(clauses, args, "create_time", query.getDateFrom(), query.getDateTo());
        return String.join(" AND ", clauses);
    }

    private String buildSlowSpanTrendWhere(ObservabilitySlowSpanTrendQuery query,
                                           Long tenantId,
                                           LocalDateTime from,
                                           LocalDateTime to,
                                           List<Object> args) {
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
        appendDateRange(clauses, args, "started_at", from, to);
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

    private void appendJsonEquals(List<String> clauses, List<Object> args, String jsonPath, String value) {
        String normalized = normalize(value);
        if (StringUtils.hasText(normalized)) {
            clauses.add("JSON_UNQUOTE(JSON_EXTRACT(tags_json, '" + jsonPath + "')) = ?");
            args.add(normalized);
        }
    }

    private void appendDateRange(List<String> clauses, List<Object> args, String column, String dateFrom, String dateTo) {
        LocalDateTime from = parseDateTime(dateFrom, false);
        LocalDateTime to = parseDateTime(dateTo, true);
        appendDateRange(clauses, args, column, from, to);
    }

    private void appendDateRange(List<String> clauses, List<Object> args, String column, LocalDateTime from, LocalDateTime to) {
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

    private List<ObservabilitySlowSpanTrendVO> aggregateSlowSpanTrends(List<ObservabilitySpanVO> spans,
                                                                       String bucket,
                                                                       LocalDateTime from,
                                                                       LocalDateTime to) {
        Map<LocalDateTime, List<ObservabilitySpanVO>> bucketSpans = new TreeMap<>();
        for (ObservabilitySpanVO span : spans) {
            if (span == null || span.getStartedAt() == null) {
                continue;
            }
            LocalDateTime bucketStart = truncateTrendBucket(span.getStartedAt(), bucket);
            bucketSpans.computeIfAbsent(bucketStart, key -> new ArrayList<>()).add(span);
        }
        LocalDateTime effectiveFrom = from == null ? firstBucketStart(bucketSpans) : truncateTrendBucket(from, bucket);
        LocalDateTime effectiveTo = to == null ? lastBucketStart(bucketSpans) : truncateTrendBucket(to, bucket);
        if (effectiveFrom == null || effectiveTo == null || effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }
        List<ObservabilitySlowSpanTrendVO> result = new ArrayList<>();
        LocalDateTime cursor = effectiveFrom;
        while (!cursor.isAfter(effectiveTo)) {
            LocalDateTime bucketEnd = plusTrendBucket(cursor, bucket);
            result.add(buildSlowTrend(cursor, bucketEnd, bucket, bucketSpans.get(cursor)));
            cursor = bucketEnd;
        }
        return result;
    }

    private LocalDateTime firstBucketStart(Map<LocalDateTime, List<ObservabilitySpanVO>> bucketSpans) {
        return bucketSpans.keySet().stream().findFirst().orElse(null);
    }

    private LocalDateTime lastBucketStart(Map<LocalDateTime, List<ObservabilitySpanVO>> bucketSpans) {
        LocalDateTime last = null;
        for (LocalDateTime bucketStart : bucketSpans.keySet()) {
            last = bucketStart;
        }
        return last;
    }

    private ObservabilitySlowSpanTrendVO buildSlowTrend(LocalDateTime bucketStart,
                                                        LocalDateTime bucketEnd,
                                                        String bucket,
                                                        List<ObservabilitySpanVO> spans) {
        List<ObservabilitySpanVO> safeSpans = spans == null ? List.of() : spans;
        ObservabilitySlowSpanTrendVO vo = new ObservabilitySlowSpanTrendVO();
        vo.setBucket(bucket);
        vo.setBucketStart(bucketStart);
        vo.setBucketEnd(bucketEnd);
        long totalCount = safeSpans.size();
        long successCount = safeSpans.stream().filter(this::isSuccessfulSpan).count();
        long errorCount = totalCount - successCount;
        vo.setTotalCount(totalCount);
        vo.setSuccessCount(successCount);
        vo.setErrorCount(errorCount);
        vo.setErrorRate(totalCount == 0L ? 0 : (int) Math.round(errorCount * 100.0d / totalCount));

        List<Long> durations = safeSpans.stream()
                .map(ObservabilitySpanVO::getDurationMs)
                .filter(duration -> duration != null && duration >= 0L)
                .sorted()
                .toList();
        if (durations.isEmpty()) {
            return vo;
        }
        long sum = durations.stream().mapToLong(Long::longValue).sum();
        vo.setAvgDurationMs(Math.round(sum / (double) durations.size()));
        vo.setMaxDurationMs(durations.get(durations.size() - 1));
        vo.setP95DurationMs(calculatePercentile(durations, 0.95d));
        vo.setP99DurationMs(calculatePercentile(durations, 0.99d));
        return vo;
    }

    private boolean isSuccessfulSpan(ObservabilitySpanVO span) {
        return span != null && "SUCCESS".equalsIgnoreCase(normalize(span.getStatus()));
    }

    private Long calculatePercentile(List<Long> sortedDurations, double percentile) {
        if (sortedDurations == null || sortedDurations.isEmpty()) {
            return null;
        }
        int index = (int) Math.ceil(percentile * sortedDurations.size()) - 1;
        int safeIndex = Math.max(0, Math.min(index, sortedDurations.size() - 1));
        return sortedDurations.get(safeIndex);
    }

    private String normalizeSlowTrendBucket(String bucket) {
        String normalized = normalize(bucket);
        if (!StringUtils.hasText(normalized)) {
            return SLOW_TREND_BUCKET_HOUR;
        }
        if (SLOW_TREND_BUCKET_HOUR.equalsIgnoreCase(normalized)) {
            return SLOW_TREND_BUCKET_HOUR;
        }
        if (SLOW_TREND_BUCKET_DAY.equalsIgnoreCase(normalized)) {
            return SLOW_TREND_BUCKET_DAY;
        }
        throw new BizException(400, "bucket 仅支持 HOUR 或 DAY");
    }

    private LocalDateTime truncateTrendBucket(LocalDateTime value, String bucket) {
        if (value == null) {
            return null;
        }
        if (SLOW_TREND_BUCKET_DAY.equals(bucket)) {
            return value.toLocalDate().atStartOfDay();
        }
        return value.truncatedTo(ChronoUnit.HOURS);
    }

    private LocalDateTime plusTrendBucket(LocalDateTime value, String bucket) {
        if (value == null) {
            return null;
        }
        if (SLOW_TREND_BUCKET_DAY.equals(bucket)) {
            return value.plusDays(1L);
        }
        return value.plusHours(1L);
    }

    private Path resolveAllowedObservabilityPath(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return null;
        }
        Path candidate = Paths.get(rawPath);
        Path resolved = candidate.isAbsolute()
                ? candidate.normalize()
                : repoRoot.resolve(candidate).normalize();
        return resolved.startsWith(observabilityLogDir) ? resolved : null;
    }

    private Path resolveMarkdownPath(Path jsonPath) {
        String fileName = jsonPath.getFileName().toString();
        if (fileName.endsWith(".md")) {
            return jsonPath;
        }
        if (fileName.endsWith(".json")) {
            String markdownName = fileName.substring(0, fileName.length() - ".json".length()) + ".md";
            return jsonPath.resolveSibling(markdownName);
        }
        return jsonPath.resolveSibling(fileName + ".md");
    }

    private Map<String, Object> readJsonMap(Path path) throws IOException {
        Map<String, Object> parsed = objectMapper.readValue(Files.readString(path, StandardCharsets.UTF_8), MAP_TYPE);
        return parsed == null ? Map.of() : new LinkedHashMap<>(parsed);
    }

    private Map<String, Object> buildPreviewSummary(Map<String, Object> payload) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("generatedAt", stringValue(payload.get("generatedAt")));
        summary.put("mode", stringValue(payload.get("mode")));
        Object summaryNode = payload.get("summary");
        if (summaryNode instanceof Map<?, ?> nestedSummary) {
            for (Map.Entry<?, ?> entry : nestedSummary.entrySet()) {
                if (entry.getKey() != null) {
                    summary.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        return summary;
    }

    private List<ObservabilityMessageArchiveBatchReportTableSummaryVO> buildTableSummaries(Object tablesNode) {
        if (!(tablesNode instanceof Map<?, ?> tableMap)) {
            return List.of();
        }
        List<ObservabilityMessageArchiveBatchReportTableSummaryVO> summaries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : tableMap.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> valueMap) || entry.getKey() == null) {
                continue;
            }
            ObservabilityMessageArchiveBatchReportTableSummaryVO summary =
                    new ObservabilityMessageArchiveBatchReportTableSummaryVO();
            summary.setTableName(String.valueOf(entry.getKey()));
            summary.setLabel(stringValue(valueMap.get("label")));
            summary.setRetentionDays(integerValue(valueMap.get("retentionDays")));
            summary.setTimeField(stringValue(valueMap.get("timeField")));
            summary.setCutoffAt(stringValue(valueMap.get("cutoffAt")));
            summary.setTotalRows(longValue(valueMap.get("totalRows")));
            summary.setExpiredRows(longValue(valueMap.get("expiredRows")));
            summary.setDeletedRows(longValue(valueMap.get("deletedRows")));
            summary.setRemainingExpiredRows(longValue(valueMap.get("remainingExpiredRows")));
            summary.setEarliestRecordAt(stringValue(valueMap.get("earliestRecordAt")));
            summary.setLatestRecordAt(stringValue(valueMap.get("latestRecordAt")));
            summaries.add(summary);
        }
        return summaries;
    }

    private MarkdownPreview readMarkdownPreview(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String preview = String.join(System.lineSeparator(), lines.stream().limit(MARKDOWN_PREVIEW_MAX_LINES).toList());
        boolean truncated = lines.size() > MARKDOWN_PREVIEW_MAX_LINES;
        if (preview.length() > MARKDOWN_PREVIEW_MAX_CHARS) {
            preview = preview.substring(0, MARKDOWN_PREVIEW_MAX_CHARS);
            truncated = true;
        }
        return new MarkdownPreview(preview, truncated);
    }

    private LocalDateTime lastModifiedAt(Path path) throws IOException {
        return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault());
    }

    private String toDisplayPath(Path path) {
        if (path == null) {
            return null;
        }
        return repoRoot.relativize(path).toString().replace('\\', '/');
    }

    private ObservabilityMessageArchiveBatchReportPreviewVO markPreviewUnavailable(
            ObservabilityMessageArchiveBatchReportPreviewVO preview,
            String reasonCode,
            String reasonMessage
    ) {
        preview.setAvailable(false);
        preview.setReasonCode(reasonCode);
        preview.setReasonMessage(reasonMessage);
        return preview;
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

    private ObservabilityMessageArchiveBatchVO mapMessageArchiveBatch(ResultSet rs, int rowNum) throws SQLException {
        ObservabilityMessageArchiveBatchVO vo = new ObservabilityMessageArchiveBatchVO();
        vo.setId(nullableLong(rs, "id"));
        vo.setBatchNo(rs.getString("batch_no"));
        vo.setSourceTable(rs.getString("source_table"));
        vo.setGovernanceMode(rs.getString("governance_mode"));
        vo.setStatus(rs.getString("status"));
        vo.setRetentionDays(nullableInteger(rs, "retention_days"));
        vo.setCutoffAt(nullableDateTime(rs, "cutoff_at"));
        vo.setConfirmReportPath(rs.getString("confirm_report_path"));
        vo.setConfirmReportGeneratedAt(nullableDateTime(rs, "confirm_report_generated_at"));
        vo.setConfirmedExpiredRows(nullableInteger(rs, "confirmed_expired_rows"));
        vo.setCandidateRows(nullableInteger(rs, "candidate_rows"));
        vo.setArchivedRows(nullableInteger(rs, "archived_rows"));
        vo.setDeletedRows(nullableInteger(rs, "deleted_rows"));
        vo.setFailedReason(rs.getString("failed_reason"));
        vo.setArtifactsJson(rs.getString("artifacts_json"));
        vo.setCreateTime(nullableDateTime(rs, "create_time"));
        vo.setUpdateTime(nullableDateTime(rs, "update_time"));
        return vo;
    }

    private ObservabilityScheduledTaskVO mapScheduledTask(ResultSet rs, int rowNum) throws SQLException {
        ObservabilityScheduledTaskVO vo = new ObservabilityScheduledTaskVO();
        vo.setId(nullableLong(rs, "id"));
        vo.setTenantId(nullableLong(rs, "tenant_id"));
        vo.setTraceId(rs.getString("trace_id"));
        vo.setDomainCode(rs.getString("domain_code"));
        vo.setStatus(rs.getString("status"));
        vo.setDurationMs(nullableLong(rs, "duration_ms"));
        vo.setStartedAt(nullableDateTime(rs, "started_at"));
        vo.setFinishedAt(nullableDateTime(rs, "finished_at"));
        vo.setErrorClass(rs.getString("error_class"));
        vo.setErrorMessage(rs.getString("error_message"));
        String tagsJson = rs.getString("tags_json");
        vo.setTagsJson(tagsJson);
        Map<String, Object> tags = parseJsonMap(tagsJson);
        vo.setTaskCode(stringValue(tags.get("taskCode")));
        vo.setTaskName(defaultIfBlank(stringValue(tags.get("taskName")), vo.getTaskCode()));
        vo.setTaskClassName(stringValue(tags.get("taskClassName")));
        vo.setTaskMethodName(stringValue(tags.get("taskMethodName")));
        vo.setTriggerType(stringValue(tags.get("triggerType")));
        vo.setTriggerExpression(stringValue(tags.get("triggerExpression")));
        vo.setInitialDelayExpression(stringValue(tags.get("initialDelayExpression")));
        vo.setInitialDelayMs(longValue(tags.get("initialDelayMs")));
        vo.setRetryCount(integerValue(tags.get("retryCount")));
        vo.setThreadName(stringValue(tags.get("threadName")));
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

    private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value instanceof Number number ? number.intValue() : null;
    }

    private LocalDateTime nullableDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private Map<String, Object> parseJsonMap(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return Map.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(normalized, MAP_TYPE);
            return parsed == null ? Map.of() : new LinkedHashMap<>(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private record MarkdownPreview(String content, boolean truncated) {
    }
}
