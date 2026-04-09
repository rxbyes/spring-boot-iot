package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.vo.DeviceAccessErrorStatsVO;
import com.ghlzm.iot.device.vo.DeviceStatsBucketVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.framework.observability.invalidreport.InvalidReportCounterStore;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 设备接入失败归档服务实现。
 */
@Slf4j
@Service
public class DeviceAccessErrorLogServiceImpl implements DeviceAccessErrorLogService {

    private static final String TABLE_NAME = "iot_device_access_error_log";
    private static final String REQUEST_METHOD_MQTT = "MQTT";
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAYLOAD_CAPTURE_LENGTH = 4000;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_ERROR_CODE_LENGTH = 64;
    private static final int MAX_EXCEPTION_CLASS_LENGTH = 255;
    private static final List<String> FAILURE_STAGE_BUCKETS = List.of(
            "ingress",
            "topic_route",
            "protocol_decode",
            "device_contract",
            "device_validate",
            "message_log",
            "payload_apply",
            "telemetry_persist",
            "device_state",
            "risk_dispatch",
            "message_dispatch"
    );

    private final JdbcTemplate jdbcTemplate;
    private final DeviceAccessErrorLogSchemaSupport schemaSupport;
    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;
    private final IotProperties iotProperties;
    private final InvalidReportCounterStore invalidReportCounterStore;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeviceAccessErrorLogServiceImpl(JdbcTemplate jdbcTemplate,
                                           DeviceAccessErrorLogSchemaSupport schemaSupport,
                                           DeviceMapper deviceMapper,
                                           ProductMapper productMapper,
                                           IotProperties iotProperties,
                                           InvalidReportCounterStore invalidReportCounterStore,
                                           PermissionService permissionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaSupport = schemaSupport;
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
        this.iotProperties = iotProperties;
        this.invalidReportCounterStore = invalidReportCounterStore;
        this.permissionService = permissionService;
    }

    @Override
    public void archiveMqttFailure(String topic,
                                   byte[] payload,
                                   RawDeviceMessage rawDeviceMessage,
                                   String failureStage,
                                   Throwable throwable) {
        Set<String> columns = schemaSupport.getColumns();
        if (columns.isEmpty() || throwable == null) {
            return;
        }
        try {
            DeviceAccessErrorLog logRecord = buildLogRecord(topic, payload, rawDeviceMessage, failureStage, throwable);
            Map<String, Object> values = new LinkedHashMap<>();
            putValue(values, columns, "id", logRecord.getId());
            putValue(values, columns, "tenant_id", logRecord.getTenantId());
            putValue(values, columns, "trace_id", logRecord.getTraceId());
            putValue(values, columns, "protocol_code", logRecord.getProtocolCode());
            putValue(values, columns, "request_method", logRecord.getRequestMethod());
            putValue(values, columns, "failure_stage", logRecord.getFailureStage());
            putValue(values, columns, "device_code", logRecord.getDeviceCode());
            putValue(values, columns, "product_key", logRecord.getProductKey());
            putValue(values, columns, "gateway_device_code", logRecord.getGatewayDeviceCode());
            putValue(values, columns, "sub_device_code", logRecord.getSubDeviceCode());
            putValue(values, columns, "topic_route_type", logRecord.getTopicRouteType());
            putValue(values, columns, "message_type", logRecord.getMessageType());
            putValue(values, columns, "topic", logRecord.getTopic());
            putValue(values, columns, "client_id", logRecord.getClientId());
            putValue(values, columns, "payload_size", logRecord.getPayloadSize());
            putValue(values, columns, "payload_encoding", logRecord.getPayloadEncoding());
            putValue(values, columns, "payload_truncated", logRecord.getPayloadTruncated());
            putValue(values, columns, "raw_payload", logRecord.getRawPayload());
            putValue(values, columns, "error_code", logRecord.getErrorCode());
            putValue(values, columns, "exception_class", logRecord.getExceptionClass());
            putValue(values, columns, "error_message", logRecord.getErrorMessage());
            putValue(values, columns, "contract_snapshot", logRecord.getContractSnapshot());
            putValue(values, columns, "create_time", logRecord.getCreateTime());
            putValue(values, columns, "deleted", logRecord.getDeleted());
            if (values.isEmpty()) {
                return;
            }
            String sql = "INSERT INTO " + TABLE_NAME + " (" + String.join(", ", values.keySet()) + ") VALUES ("
                    + String.join(", ", values.keySet().stream().map(item -> "?").toList()) + ")";
            jdbcTemplate.update(sql, values.values().toArray());
        } catch (Exception ex) {
            log.warn("归档 MQTT 失败报文时出错，将忽略本次归档, topic={}, error={}", topic, ex.getMessage());
        }
    }

    @Override
    public PageResult<DeviceAccessErrorLog> pageLogs(Long currentUserId,
                                                     DeviceAccessErrorQuery query,
                                                     Integer pageNum,
                                                     Integer pageSize) {
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize.longValue(), MAX_PAGE_SIZE);
        Set<String> columns = schemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(currentUserId, query, columns);
        if (querySpec.emptyResult()) {
            return PageResult.empty(safePageNum, safePageSize);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + TABLE_NAME + querySpec.whereClause(),
                Long.class,
                querySpec.params().toArray()
        );
        if (total == null || total < 1) {
            return PageResult.empty(safePageNum, safePageSize);
        }

        String sql = "SELECT " + buildSelectClause(columns)
                + " FROM " + TABLE_NAME
                + querySpec.whereClause()
                + buildOrderByClause(columns)
                + " LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(querySpec.params());
        params.add(safePageSize);
        params.add((safePageNum - 1) * safePageSize);
        List<DeviceAccessErrorLog> records = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs, columns),
                params.toArray()
        );
        return PageResult.of(total, safePageNum, safePageSize, records);
    }

    @Override
    public DeviceAccessErrorStatsVO getStats(Long currentUserId, DeviceAccessErrorQuery query) {
        DeviceAccessErrorStatsVO stats = new DeviceAccessErrorStatsVO();
        Set<String> columns = schemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(currentUserId, query, columns);
        if (querySpec.emptyResult()) {
            return stats;
        }

        stats.setTotal(queryCount(querySpec, null));
        if (stats.getTotal() == null || stats.getTotal() < 1) {
            return stats;
        }

        if (shouldUseGlobalFailureStageSummary(currentUserId, query)) {
            stats.setRecentHourCount(sumFailureStageCounts(Instant.now().minus(Duration.ofHours(1))));
            stats.setRecent24HourCount(sumFailureStageCounts(Instant.now().minus(Duration.ofHours(24))));
            stats.setTopFailureStages(listFailureStageBuckets(Instant.now().minus(Duration.ofHours(24))));
        } else {
            stats.setRecentHourCount(queryRecentCount(querySpec, columns, 1));
            stats.setRecent24HourCount(queryRecentCount(querySpec, columns, 24));
            stats.setTopFailureStages(queryTopBuckets(querySpec, resolveColumn(columns, "failure_stage")));
        }
        stats.setDistinctTraceCount(queryDistinctCount(querySpec, resolveColumn(columns, "trace_id")));
        stats.setDistinctDeviceCount(queryDistinctCount(querySpec, resolveColumn(columns, "device_code")));
        stats.setTopErrorCodes(queryTopBuckets(querySpec, resolveColumn(columns, "error_code")));
        stats.setTopExceptionClasses(queryTopBuckets(querySpec, resolveColumn(columns, "exception_class")));
        stats.setTopProtocolCodes(queryTopBuckets(querySpec, resolveColumn(columns, "protocol_code")));
        stats.setTopTopics(queryTopBuckets(querySpec, resolveColumn(columns, "topic")));
        return stats;
    }

    @Override
    public List<DeviceAccessErrorLogService.FailureStageCount> listFailureStageCountsSince(java.util.Date startTime) {
        Instant start = startTime == null
                ? Instant.now().minus(Duration.ofMinutes(10))
                : startTime.toInstant();
        return FAILURE_STAGE_BUCKETS.stream()
                .map(stage -> new DeviceAccessErrorLogService.FailureStageCount(
                        stage,
                        invalidReportCounterStore.sumFailureStageSince(stage, start)
                ))
                .filter(item -> item.failureCount() > 0L)
                .sorted(Comparator.comparingLong(DeviceAccessErrorLogService.FailureStageCount::failureCount)
                        .reversed()
                        .thenComparing(DeviceAccessErrorLogService.FailureStageCount::failureStage))
                .toList();
    }

    @Override
    public DeviceAccessErrorLog getById(Long currentUserId, Long id) {
        if (id == null) {
            return null;
        }
        Set<String> columns = schemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(currentUserId, null, columns);
        if (querySpec.emptyResult()) {
            return null;
        }
        String sql = "SELECT " + buildSelectClause(columns)
                + " FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND id = ?"
                + buildOrderByClause(columns)
                + " LIMIT 1";
        List<Object> params = new ArrayList<>(querySpec.params());
        params.add(id);
        List<DeviceAccessErrorLog> records = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRow(rs, columns),
                params.toArray()
        );
        return records.isEmpty() ? null : records.get(0);
    }

    private Long queryCount(QuerySpec querySpec, String extraCondition, Object... extraParams) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM ")
                .append(TABLE_NAME)
                .append(querySpec.whereClause());
        List<Object> params = new ArrayList<>(querySpec.params());
        appendExtraCondition(sql, params, extraCondition, extraParams);
        Long value = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return value == null ? 0L : value;
    }

    private Long queryRecentCount(QuerySpec querySpec, Set<String> columns, int hours) {
        String timeColumn = resolveColumn(columns, "create_time");
        if (timeColumn == null || hours < 1) {
            return 0L;
        }
        return queryCount(
                querySpec,
                " AND " + timeColumn + " >= DATE_SUB(NOW(), INTERVAL " + hours + " HOUR)"
        );
    }

    private Long queryDistinctCount(QuerySpec querySpec, String column) {
        if (column == null) {
            return 0L;
        }
        String sql = "SELECT COUNT(DISTINCT " + column + ") FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, querySpec.params().toArray());
        return value == null ? 0L : value;
    }

    private List<DeviceStatsBucketVO> queryTopBuckets(QuerySpec querySpec, String column) {
        if (column == null) {
            return List.of();
        }
        String bucketExpression = "TRIM(" + column + ")";
        String sql = "SELECT " + bucketExpression + " AS bucket_value, COUNT(1) AS bucket_count"
                + " FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''"
                + " GROUP BY " + bucketExpression
                + " ORDER BY bucket_count DESC, bucket_value ASC"
                + " LIMIT 5";
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new DeviceStatsBucketVO(
                        rs.getString("bucket_value"),
                        rs.getString("bucket_value"),
                        rs.getLong("bucket_count")
                ),
                querySpec.params().toArray()
        );
    }

    private long sumFailureStageCounts(Instant startInclusive) {
        return listFailureStageBuckets(startInclusive).stream()
                .map(DeviceStatsBucketVO::getCount)
                .filter(value -> value != null && value > 0L)
                .mapToLong(Long::longValue)
                .sum();
    }

    private List<DeviceStatsBucketVO> listFailureStageBuckets(Instant startInclusive) {
        return FAILURE_STAGE_BUCKETS.stream()
                .map(stage -> new DeviceStatsBucketVO(
                        stage,
                        stage,
                        invalidReportCounterStore.sumFailureStageSince(stage, startInclusive)
                ))
                .filter(bucket -> bucket.getCount() != null && bucket.getCount() > 0L)
                .sorted(Comparator.comparingLong(DeviceStatsBucketVO::getCount).reversed()
                        .thenComparing(DeviceStatsBucketVO::getValue))
                .toList();
    }

    private boolean isUnfilteredQuery(DeviceAccessErrorQuery query) {
        return query == null
                || (!StringUtils.hasText(query.getTraceId())
                && !StringUtils.hasText(query.getProtocolCode())
                && !StringUtils.hasText(query.getFailureStage())
                && !StringUtils.hasText(query.getDeviceCode())
                && !StringUtils.hasText(query.getProductKey())
                && !StringUtils.hasText(query.getTopicRouteType())
                && !StringUtils.hasText(query.getMessageType())
                && !StringUtils.hasText(query.getTopic())
                && !StringUtils.hasText(query.getClientId())
                && !StringUtils.hasText(query.getErrorCode())
                && !StringUtils.hasText(query.getExceptionClass()));
    }

    private void appendExtraCondition(StringBuilder sql, List<Object> params, String extraCondition, Object... extraParams) {
        if (!StringUtils.hasText(extraCondition)) {
            return;
        }
        sql.append(extraCondition);
        if (extraParams == null || extraParams.length == 0) {
            return;
        }
        for (Object extraParam : extraParams) {
            params.add(extraParam);
        }
    }

    private DeviceAccessErrorLog buildLogRecord(String topic,
                                                byte[] payload,
                                                RawDeviceMessage rawDeviceMessage,
                                                String failureStage,
                                                Throwable throwable) {
        byte[] sourcePayload = payload;
        if (rawDeviceMessage != null && rawDeviceMessage.getPayload() != null) {
            sourcePayload = rawDeviceMessage.getPayload();
        }
        PayloadCapture payloadCapture = capturePayload(sourcePayload);

        DeviceAccessErrorLog logRecord = new DeviceAccessErrorLog();
        logRecord.setId(IdWorker.getId());
        logRecord.setTenantId(resolveTenantId(rawDeviceMessage));
        logRecord.setTraceId(resolveText(rawDeviceMessage == null ? null : rawDeviceMessage.getTraceId(), TraceContextHolder.getTraceId()));
        logRecord.setProtocolCode(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getProtocolCode()));
        logRecord.setRequestMethod(REQUEST_METHOD_MQTT);
        logRecord.setFailureStage(trimToNull(failureStage));
        logRecord.setDeviceCode(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode()));
        logRecord.setProductKey(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey()));
        logRecord.setGatewayDeviceCode(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getGatewayDeviceCode()));
        logRecord.setSubDeviceCode(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getSubDeviceCode()));
        logRecord.setTopicRouteType(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getTopicRouteType()));
        logRecord.setMessageType(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getMessageType()));
        logRecord.setTopic(resolveText(rawDeviceMessage == null ? null : rawDeviceMessage.getTopic(), topic));
        logRecord.setClientId(trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getClientId()));
        logRecord.setPayloadSize(payloadCapture.payloadSize());
        logRecord.setPayloadEncoding(payloadCapture.encoding());
        logRecord.setPayloadTruncated(payloadCapture.truncated() ? 1 : 0);
        logRecord.setRawPayload(payloadCapture.payload());
        logRecord.setErrorCode(resolveErrorCode(throwable));
        logRecord.setExceptionClass(truncate(throwable.getClass().getName(), MAX_EXCEPTION_CLASS_LENGTH));
        logRecord.setErrorMessage(truncate(resolveErrorMessage(throwable), MAX_ERROR_MESSAGE_LENGTH));
        logRecord.setContractSnapshot(buildContractSnapshot(rawDeviceMessage));
        logRecord.setCreateTime(LocalDateTime.now());
        logRecord.setDeleted(0);
        return logRecord;
    }

    private PayloadCapture capturePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return new PayloadCapture(0, "UTF-8", false, "");
        }
        String decoded = tryDecodeUtf8(payload);
        if (decoded != null) {
            String normalized = truncate(decoded, MAX_PAYLOAD_CAPTURE_LENGTH);
            return new PayloadCapture(payload.length, "UTF-8", normalized.length() < decoded.length(), normalized);
        }
        String base64 = Base64.getEncoder().encodeToString(payload);
        String normalized = truncate(base64, MAX_PAYLOAD_CAPTURE_LENGTH);
        return new PayloadCapture(payload.length, "BASE64", normalized.length() < base64.length(), normalized);
    }

    private String tryDecodeUtf8(byte[] payload) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload))
                    .toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private QuerySpec buildQuerySpec(Long currentUserId, DeviceAccessErrorQuery query, Set<String> columns) {
        if (columns.isEmpty()) {
            return QuerySpec.emptySpec();
        }
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (columns.contains("deleted")) {
            where.append(" AND deleted = 0");
        }
        Long tenantId = resolveScopedTenantId(currentUserId);
        if (tenantId != null) {
            String tenantColumn = resolveColumn(columns, "tenant_id");
            if (tenantColumn == null) {
                return QuerySpec.emptySpec();
            }
            where.append(" AND ").append(tenantColumn).append(" = ?");
            params.add(tenantId);
        }
        appendAccessibleDeviceScope(where, params, currentUserId);
        if (query == null) {
            return new QuerySpec(where.toString(), params, false);
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "trace_id"), query.getTraceId())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "protocol_code"), query.getProtocolCode())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "failure_stage"), query.getFailureStage())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "device_code"), query.getDeviceCode())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "product_key"), query.getProductKey())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "topic_route_type"), query.getTopicRouteType())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "message_type"), query.getMessageType())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "topic"), query.getTopic())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "client_id"), query.getClientId())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "error_code"), query.getErrorCode())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "exception_class"), query.getExceptionClass())) {
            return QuerySpec.emptySpec();
        }
        return new QuerySpec(where.toString(), params, false);
    }

    private boolean shouldUseGlobalFailureStageSummary(Long currentUserId, DeviceAccessErrorQuery query) {
        return resolveScopedTenantId(currentUserId) == null
                && !hasOrganizationRestrictedScope(currentUserId)
                && isUnfilteredQuery(query);
    }

    private Long resolveScopedTenantId(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return null;
        }
        return context.tenantId();
    }

    private DataPermissionContext resolveDataPermissionContext(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return null;
        }
        return permissionService.getDataPermissionContext(currentUserId);
    }

    private boolean hasOrganizationRestrictedScope(Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return false;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        return dataScopeType == DataScopeType.ORG || dataScopeType == DataScopeType.ORG_AND_CHILDREN;
    }

    private void appendAccessibleDeviceScope(StringBuilder where, List<Object> params, Long currentUserId) {
        DataPermissionContext context = resolveDataPermissionContext(currentUserId);
        if (context == null || context.superAdmin()) {
            return;
        }
        DataScopeType dataScopeType = normalizeDeviceDataScope(context.dataScopeType());
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.TENANT) {
            return;
        }
        where.append(" AND device_code IN (SELECT device_code FROM iot_device WHERE deleted = 0");
        if (context.tenantId() != null) {
            where.append(" AND tenant_id = ?");
            params.add(context.tenantId());
        }
        if (dataScopeType == DataScopeType.ORG) {
            if (context.orgId() == null || context.orgId() <= 0) {
                where.append(" AND 1 = 0)");
                return;
            }
            where.append(" AND org_id = ?)");
            params.add(context.orgId());
            return;
        }
        Set<Long> accessibleOrgIds = listAccessibleOrganizationIds(currentUserId);
        if (accessibleOrgIds.isEmpty()) {
            where.append(" AND 1 = 0)");
            return;
        }
        where.append(" AND org_id IN (");
        appendSqlPlaceholders(where, accessibleOrgIds.size());
        where.append("))");
        params.addAll(accessibleOrgIds);
    }

    private DataScopeType normalizeDeviceDataScope(DataScopeType dataScopeType) {
        if (dataScopeType == null) {
            return DataScopeType.TENANT;
        }
        return dataScopeType == DataScopeType.SELF ? DataScopeType.ORG : dataScopeType;
    }

    private Set<Long> listAccessibleOrganizationIds(Long currentUserId) {
        if (currentUserId == null || permissionService == null) {
            return Set.of();
        }
        Set<Long> accessibleOrgIds = permissionService.listAccessibleOrganizationIds(currentUserId);
        return accessibleOrgIds == null ? Set.of() : accessibleOrgIds;
    }

    private void appendSqlPlaceholders(StringBuilder sql, int size) {
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
    }

    private String buildSelectClause(Set<String> columns) {
        List<String> selectColumns = new ArrayList<>();
        addSelect(selectColumns, columns, "id");
        addSelect(selectColumns, columns, "tenant_id");
        addSelect(selectColumns, columns, "trace_id");
        addSelect(selectColumns, columns, "protocol_code");
        addSelect(selectColumns, columns, "request_method");
        addSelect(selectColumns, columns, "failure_stage");
        addSelect(selectColumns, columns, "device_code");
        addSelect(selectColumns, columns, "product_key");
        addSelect(selectColumns, columns, "gateway_device_code");
        addSelect(selectColumns, columns, "sub_device_code");
        addSelect(selectColumns, columns, "topic_route_type");
        addSelect(selectColumns, columns, "message_type");
        addSelect(selectColumns, columns, "topic");
        addSelect(selectColumns, columns, "client_id");
        addSelect(selectColumns, columns, "payload_size");
        addSelect(selectColumns, columns, "payload_encoding");
        addSelect(selectColumns, columns, "payload_truncated");
        addSelect(selectColumns, columns, "raw_payload");
        addSelect(selectColumns, columns, "error_code");
        addSelect(selectColumns, columns, "exception_class");
        addSelect(selectColumns, columns, "error_message");
        addSelect(selectColumns, columns, "contract_snapshot");
        addSelect(selectColumns, columns, "create_time");
        addSelect(selectColumns, columns, "deleted");
        return String.join(", ", selectColumns);
    }

    private void addSelect(List<String> selectColumns, Set<String> columns, String column) {
        if (columns.contains(column)) {
            selectColumns.add(column);
        }
    }

    private String buildOrderByClause(Set<String> columns) {
        List<String> orderColumns = new ArrayList<>();
        if (columns.contains("create_time")) {
            orderColumns.add("create_time DESC");
        }
        orderColumns.add("id DESC");
        return " ORDER BY " + String.join(", ", orderColumns);
    }

    private DeviceAccessErrorLog mapRow(ResultSet rs, Set<String> columns) throws SQLException {
        DeviceAccessErrorLog logRecord = new DeviceAccessErrorLog();
        if (columns.contains("id")) {
            logRecord.setId(getLong(rs, "id"));
        }
        if (columns.contains("tenant_id")) {
            logRecord.setTenantId(getLong(rs, "tenant_id"));
        }
        if (columns.contains("trace_id")) {
            logRecord.setTraceId(rs.getString("trace_id"));
        }
        if (columns.contains("protocol_code")) {
            logRecord.setProtocolCode(rs.getString("protocol_code"));
        }
        if (columns.contains("request_method")) {
            logRecord.setRequestMethod(rs.getString("request_method"));
        }
        if (columns.contains("failure_stage")) {
            logRecord.setFailureStage(rs.getString("failure_stage"));
        }
        if (columns.contains("device_code")) {
            logRecord.setDeviceCode(rs.getString("device_code"));
        }
        if (columns.contains("product_key")) {
            logRecord.setProductKey(rs.getString("product_key"));
        }
        if (columns.contains("gateway_device_code")) {
            logRecord.setGatewayDeviceCode(rs.getString("gateway_device_code"));
        }
        if (columns.contains("sub_device_code")) {
            logRecord.setSubDeviceCode(rs.getString("sub_device_code"));
        }
        if (columns.contains("topic_route_type")) {
            logRecord.setTopicRouteType(rs.getString("topic_route_type"));
        }
        if (columns.contains("message_type")) {
            logRecord.setMessageType(rs.getString("message_type"));
        }
        if (columns.contains("topic")) {
            logRecord.setTopic(rs.getString("topic"));
        }
        if (columns.contains("client_id")) {
            logRecord.setClientId(rs.getString("client_id"));
        }
        if (columns.contains("payload_size")) {
            logRecord.setPayloadSize(getInteger(rs, "payload_size"));
        }
        if (columns.contains("payload_encoding")) {
            logRecord.setPayloadEncoding(rs.getString("payload_encoding"));
        }
        if (columns.contains("payload_truncated")) {
            logRecord.setPayloadTruncated(getInteger(rs, "payload_truncated"));
        }
        if (columns.contains("raw_payload")) {
            logRecord.setRawPayload(rs.getString("raw_payload"));
        }
        if (columns.contains("error_code")) {
            logRecord.setErrorCode(rs.getString("error_code"));
        }
        if (columns.contains("exception_class")) {
            logRecord.setExceptionClass(rs.getString("exception_class"));
        }
        if (columns.contains("error_message")) {
            logRecord.setErrorMessage(rs.getString("error_message"));
        }
        if (columns.contains("contract_snapshot")) {
            logRecord.setContractSnapshot(rs.getString("contract_snapshot"));
        }
        if (columns.contains("create_time")) {
            logRecord.setCreateTime(getLocalDateTime(rs, "create_time"));
        }
        if (columns.contains("deleted")) {
            logRecord.setDeleted(getInteger(rs, "deleted"));
        }
        return logRecord;
    }

    private void putValue(Map<String, Object> values, Set<String> columns, String column, Object value) {
        if (value != null && columns.contains(column)) {
            values.put(column, toJdbcValue(value));
        }
    }

    private String resolveColumn(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean appendTextEquals(StringBuilder where, List<Object> params, String column, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (column == null) {
            return true;
        }
        where.append(" AND ").append(column).append(" = ?");
        params.add(value.trim());
        return false;
    }

    private boolean appendTextLike(StringBuilder where, List<Object> params, String column, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (column == null) {
            return true;
        }
        where.append(" AND ").append(column).append(" LIKE ?");
        params.add("%" + value.trim() + "%");
        return false;
    }

    private Long resolveTenantId(RawDeviceMessage rawDeviceMessage) {
        if (rawDeviceMessage == null || !StringUtils.hasText(rawDeviceMessage.getTenantId())) {
            return DEFAULT_TENANT_ID;
        }
        try {
            return Long.valueOf(rawDeviceMessage.getTenantId().trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_TENANT_ID;
        }
    }

    private String resolveErrorCode(Throwable throwable) {
        if (throwable instanceof BizException bizException && bizException.getCode() != null) {
            return truncate(String.valueOf(bizException.getCode()), MAX_ERROR_CODE_LENGTH);
        }
        return null;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (!StringUtils.hasText(throwable.getMessage())) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
    }

    private String resolveText(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary != null ? normalizedPrimary : trimToNull(fallback);
    }

    private String buildContractSnapshot(RawDeviceMessage rawDeviceMessage) {
        try {
            return objectMapper.writeValueAsString(resolveContractSnapshot(rawDeviceMessage));
        } catch (Exception ex) {
            log.warn("构建设备接入契约快照失败，将继续归档基础失败信息, deviceCode={}, error={}",
                    rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode(),
                    ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> resolveContractSnapshot(RawDeviceMessage rawDeviceMessage) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        String actualProtocolCode = resolveText(
                rawDeviceMessage == null ? null : rawDeviceMessage.getProtocolCode(),
                iotProperties.getProtocol() == null ? null : iotProperties.getProtocol().getDefaultCode()
        );
        Device device = findDeviceByCode(rawDeviceMessage == null ? null : rawDeviceMessage.getDeviceCode());
        Product resolvedProduct = resolveProduct(device, rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey());
        String deviceProtocolCode = trimToNull(device == null ? null : device.getProtocolCode());
        String productProtocolCode = trimToNull(resolvedProduct == null ? null : resolvedProduct.getProtocolCode());
        String expectedProtocolCode = actualProtocolCode;
        String protocolSource = "router-default";
        if (deviceProtocolCode != null) {
            expectedProtocolCode = deviceProtocolCode;
            protocolSource = "device";
        } else if (productProtocolCode != null) {
            expectedProtocolCode = productProtocolCode;
            protocolSource = "product-fallback";
        }

        snapshot.put("routeType", trimToNull(rawDeviceMessage == null ? null : rawDeviceMessage.getTopicRouteType()));
        snapshot.put("expectedProtocolCode", expectedProtocolCode);
        snapshot.put("actualProtocolCode", actualProtocolCode);
        snapshot.put("protocolSource", protocolSource);
        snapshot.put("deviceProtocolCode", deviceProtocolCode);
        snapshot.put("productProtocolCode", productProtocolCode);
        snapshot.put("deviceProductId", device == null ? null : device.getProductId());
        snapshot.put("resolvedProductId", resolvedProduct == null ? null : resolvedProduct.getId());
        snapshot.put("productKey", resolveText(
                resolvedProduct == null ? null : resolvedProduct.getProductKey(),
                rawDeviceMessage == null ? null : rawDeviceMessage.getProductKey()
        ));
        return snapshot;
    }

    private Device findDeviceByCode(String deviceCode) {
        String normalizedDeviceCode = trimToNull(deviceCode);
        if (normalizedDeviceCode == null) {
            return null;
        }
        return deviceMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, normalizedDeviceCode)
                .eq(Device::getDeleted, 0)
                .last("limit 1"));
    }

    private Product resolveProduct(Device device, String productKey) {
        Product product = findProductById(device == null ? null : device.getProductId());
        if (product != null) {
            return product;
        }
        String normalizedProductKey = trimToNull(productKey);
        if (normalizedProductKey == null) {
            return null;
        }
        return productMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product>()
                .eq(Product::getProductKey, normalizedProductKey)
                .eq(Product::getDeleted, 0)
                .last("limit 1"));
    }

    private Product findProductById(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            return null;
        }
        return product;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Object toJdbcValue(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return Timestamp.valueOf(dateTime);
        }
        return value;
    }

    private record QuerySpec(String whereClause, List<Object> params, boolean emptyResult) {

        private static QuerySpec emptySpec() {
            return new QuerySpec("", List.of(), true);
        }
    }

    private record PayloadCapture(int payloadSize, String encoding, boolean truncated, String payload) {
    }
}
