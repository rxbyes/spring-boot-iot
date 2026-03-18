package com.ghlzm.iot.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.mapper.AuditLogMapper;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.vo.AuditLogStatsBucketVO;
import com.ghlzm.iot.system.vo.SystemErrorStatsVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog>
        implements AuditLogService {

    private static final String TABLE_NAME = "sys_audit_log";
    private static final String SYSTEM_ERROR_TYPE = "system_error";
    private static final String LEGACY_OPERATION_TYPE_COLUMN = "log_type";
    private static final String LEGACY_REQUEST_URL_COLUMN = "operation_uri";

    private final JdbcTemplate jdbcTemplate;
    private final AuditLogSchemaSupport auditLogSchemaSupport;

    public AuditLogServiceImpl(JdbcTemplate jdbcTemplate, AuditLogSchemaSupport auditLogSchemaSupport) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogSchemaSupport = auditLogSchemaSupport;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLog(AuditLog log) {
        // 兼容历史库缺列场景，按真实存在的列动态写入审计日志。
        AuditLog normalized = normalizeForPersist(log);
        Set<String> columns = auditLogSchemaSupport.getColumns();

        Map<String, Object> values = new LinkedHashMap<>();
        putValue(values, columns, "id", normalized.getId());
        putValue(values, columns, "tenant_id", normalized.getTenantId());
        putValue(values, columns, "user_id", normalized.getUserId());
        putValue(values, columns, "user_name", normalized.getUserName());
        putAliasedValue(values, columns, normalized.getOperationType(), "operation_type", LEGACY_OPERATION_TYPE_COLUMN);
        putValue(values, columns, "operation_module", normalized.getOperationModule());
        putValue(values, columns, "operation_method", normalized.getOperationMethod());
        putAliasedValue(values, columns, normalized.getRequestUrl(), "request_url", LEGACY_REQUEST_URL_COLUMN);
        putValue(values, columns, "request_method", normalized.getRequestMethod());
        putValue(values, columns, "request_params", normalized.getRequestParams());
        putValue(values, columns, "response_result", normalized.getResponseResult());
        putValue(values, columns, "ip_address", normalized.getIpAddress());
        putValue(values, columns, "location", normalized.getLocation());
        putValue(values, columns, "operation_result", normalized.getOperationResult());
        putValue(values, columns, "result_message", normalized.getResultMessage());
        putValue(values, columns, "operation_time", normalized.getOperationTime());
        putValue(values, columns, "trace_id", normalized.getTraceId());
        putValue(values, columns, "device_code", normalized.getDeviceCode());
        putValue(values, columns, "product_key", normalized.getProductKey());
        putValue(values, columns, "error_code", normalized.getErrorCode());
        putValue(values, columns, "exception_class", normalized.getExceptionClass());
        putValue(values, columns, "create_time", normalized.getCreateTime());
        putValue(values, columns, "deleted", normalized.getDeleted());

        String sql = "INSERT INTO " + TABLE_NAME + " (" + String.join(", ", values.keySet()) + ") VALUES ("
                + String.join(", ", values.keySet().stream().map(item -> "?").toList()) + ")";
        jdbcTemplate.update(sql, values.values().toArray());
    }

    @Override
    public List<AuditLog> listLogs(AuditLog log, Boolean excludeSystemError) {
        return queryLogs(log, excludeSystemError, null, null);
    }

    @Override
    public PageResult<AuditLog> pageLogs(AuditLog log, Boolean excludeSystemError, Integer pageNum, Integer pageSize) {
        long safePageNum = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long safePageSize = pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize.longValue(), 200L);
        Set<String> columns = auditLogSchemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(log, excludeSystemError, columns);
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

        long offset = (safePageNum - 1) * safePageSize;
        List<AuditLog> records = queryLogs(log, excludeSystemError, safePageSize, offset);
        return PageResult.of(total, safePageNum, safePageSize, records);
    }

    @Override
    public SystemErrorStatsVO getSystemErrorStats(AuditLog log) {
        Set<String> columns = auditLogSchemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(normalizeSystemErrorFilter(log), false, columns);
        if (querySpec.emptyResult()) {
            return new SystemErrorStatsVO();
        }

        SystemErrorStatsVO stats = new SystemErrorStatsVO();
        stats.setTotal(queryCount(querySpec, null));
        stats.setTodayCount(queryTodayCount(querySpec, columns));
        stats.setMqttCount(queryEqualsCount(querySpec, resolveColumn(columns, "request_method"), "MQTT"));
        stats.setSystemCount(queryEqualsCount(querySpec, resolveColumn(columns, "request_method"), "SYSTEM"));
        stats.setDistinctTraceCount(queryDistinctCount(querySpec, resolveColumn(columns, "trace_id")));
        stats.setDistinctDeviceCount(queryDistinctCount(querySpec, resolveColumn(columns, "device_code")));
        stats.setTopModules(queryTopBuckets(querySpec, resolveColumn(columns, "operation_module")));
        stats.setTopExceptionClasses(queryTopBuckets(querySpec, resolveColumn(columns, "exception_class")));
        stats.setTopErrorCodes(queryTopBuckets(querySpec, resolveColumn(columns, "error_code")));
        return stats;
    }

    @Override
    public AuditLog getById(Long id) {
        if (id == null) {
            return null;
        }
        Set<String> columns = auditLogSchemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(null, false, columns);
        String sql = "SELECT " + buildSelectClause(columns)
                + " FROM " + TABLE_NAME
                + querySpec.whereClause()
                + " AND id = ?"
                + buildOrderByClause(columns)
                + " LIMIT 1";
        List<Object> params = new ArrayList<>(querySpec.params());
        params.add(id);
        List<AuditLog> logs = jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs, columns), params.toArray());
        return logs.isEmpty() ? null : logs.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLog(Long id) {
        if (id == null) {
            return;
        }
        Set<String> columns = auditLogSchemaSupport.getColumns();
        if (columns.contains("deleted")) {
            jdbcTemplate.update("UPDATE " + TABLE_NAME + " SET deleted = 1 WHERE id = ?", id);
            return;
        }
        jdbcTemplate.update("DELETE FROM " + TABLE_NAME + " WHERE id = ?", id);
    }

    private List<AuditLog> queryLogs(AuditLog log, Boolean excludeSystemError, Long limit, Long offset) {
        Set<String> columns = auditLogSchemaSupport.getColumns();
        QuerySpec querySpec = buildQuerySpec(log, excludeSystemError, columns);
        if (querySpec.emptyResult()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(buildSelectClause(columns))
                .append(" FROM ")
                .append(TABLE_NAME)
                .append(querySpec.whereClause())
                .append(buildOrderByClause(columns));
        List<Object> params = new ArrayList<>(querySpec.params());
        if (limit != null) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset == null ? 0L : offset);
        }
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapRow(rs, columns), params.toArray());
    }

    private AuditLog normalizeSystemErrorFilter(AuditLog log) {
        AuditLog target = new AuditLog();
        if (log != null) {
            target.setTenantId(log.getTenantId());
            target.setUserId(log.getUserId());
            target.setTraceId(log.getTraceId());
            target.setDeviceCode(log.getDeviceCode());
            target.setProductKey(log.getProductKey());
            target.setOperationModule(log.getOperationModule());
            target.setRequestMethod(log.getRequestMethod());
            target.setRequestUrl(log.getRequestUrl());
            target.setResultMessage(log.getResultMessage());
            target.setErrorCode(log.getErrorCode());
            target.setExceptionClass(log.getExceptionClass());
            target.setOperationResult(log.getOperationResult());
        }
        target.setOperationType(SYSTEM_ERROR_TYPE);
        return target;
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

    private Long queryTodayCount(QuerySpec querySpec, Set<String> columns) {
        String timeColumn = resolveColumn(columns, "operation_time", "create_time");
        if (timeColumn == null) {
            return 0L;
        }
        return queryCount(
                querySpec,
                " AND " + timeColumn + " >= CURDATE() AND " + timeColumn + " < DATE_ADD(CURDATE(), INTERVAL 1 DAY)"
        );
    }

    private Long queryEqualsCount(QuerySpec querySpec, String column, Object value) {
        if (column == null || value == null) {
            return 0L;
        }
        return queryCount(querySpec, " AND " + column + " = ?", value);
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

    private List<AuditLogStatsBucketVO> queryTopBuckets(QuerySpec querySpec, String column) {
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
                (rs, rowNum) -> new AuditLogStatsBucketVO(
                        rs.getString("bucket_value"),
                        rs.getString("bucket_value"),
                        rs.getLong("bucket_count")
                ),
                querySpec.params().toArray()
        );
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

    private QuerySpec buildQuerySpec(AuditLog log, Boolean excludeSystemError, Set<String> columns) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (columns.contains("deleted")) {
            where.append(" AND deleted = 0");
        }
        if (log == null) {
            if (Boolean.TRUE.equals(excludeSystemError)) {
                String operationTypeColumn = resolveColumn(columns, "operation_type", LEGACY_OPERATION_TYPE_COLUMN);
                if (operationTypeColumn == null) {
                    return QuerySpec.emptySpec();
                }
                where.append(" AND ").append(operationTypeColumn).append(" <> ?");
                params.add(SYSTEM_ERROR_TYPE);
            }
            return new QuerySpec(where.toString(), params, false);
        }

        if (appendEquals(where, params, resolveColumn(columns, "tenant_id"), log.getTenantId())) {
            return QuerySpec.emptySpec();
        }
        if (appendEquals(where, params, resolveColumn(columns, "user_id"), log.getUserId())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "trace_id"), log.getTraceId())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "device_code"), log.getDeviceCode())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "product_key"), log.getProductKey())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "user_name"), log.getUserName())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "operation_type", LEGACY_OPERATION_TYPE_COLUMN), log.getOperationType())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "operation_module"), log.getOperationModule())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "request_method"), log.getRequestMethod())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "request_url", LEGACY_REQUEST_URL_COLUMN), log.getRequestUrl())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "result_message"), log.getResultMessage())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextEquals(where, params, resolveColumn(columns, "error_code"), log.getErrorCode())) {
            return QuerySpec.emptySpec();
        }
        if (appendTextLike(where, params, resolveColumn(columns, "exception_class"), log.getExceptionClass())) {
            return QuerySpec.emptySpec();
        }
        if (appendEquals(where, params, resolveColumn(columns, "operation_result"), log.getOperationResult())) {
            return QuerySpec.emptySpec();
        }
        if (Boolean.TRUE.equals(excludeSystemError)) {
            String operationTypeColumn = resolveColumn(columns, "operation_type", LEGACY_OPERATION_TYPE_COLUMN);
            if (operationTypeColumn == null) {
                return QuerySpec.emptySpec();
            }
            where.append(" AND ").append(operationTypeColumn).append(" <> ?");
            params.add(SYSTEM_ERROR_TYPE);
        }
        return new QuerySpec(where.toString(), params, false);
    }

    private AuditLog normalizeForPersist(AuditLog log) {
        AuditLog target = log == null ? new AuditLog() : log;
        Date now = new Date();
        if (target.getId() == null) {
            target.setId(IdWorker.getId());
        }
        if (target.getCreateTime() == null) {
            target.setCreateTime(now);
        }
        if (target.getOperationTime() == null) {
            target.setOperationTime(now);
        }
        if (target.getDeleted() == null) {
            target.setDeleted(0);
        }
        return target;
    }

    private void putValue(Map<String, Object> values, Set<String> columns, String column, Object value) {
        if (value != null && columns.contains(column)) {
            values.put(column, toJdbcValue(value));
        }
    }

    private void putAliasedValue(Map<String, Object> values, Set<String> columns, Object value, String... candidates) {
        if (value == null) {
            return;
        }
        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                values.put(candidate, toJdbcValue(value));
            }
        }
    }

    private String buildSelectClause(Set<String> columns) {
        List<String> selectColumns = new ArrayList<>();
        addSelect(selectColumns, columns, "id", "id");
        addSelect(selectColumns, columns, "tenant_id", "tenant_id");
        addSelect(selectColumns, columns, "user_id", "user_id");
        addSelect(selectColumns, columns, "user_name", "user_name");
        addSelect(selectColumns, columns, "trace_id", "trace_id");
        addSelect(selectColumns, columns, "device_code", "device_code");
        addSelect(selectColumns, columns, "product_key", "product_key");
        addSelect(selectColumns, columns, "operation_type", "operation_type", LEGACY_OPERATION_TYPE_COLUMN);
        addSelect(selectColumns, columns, "operation_module", "operation_module");
        addSelect(selectColumns, columns, "operation_method", "operation_method");
        addSelect(selectColumns, columns, "request_url", "request_url", LEGACY_REQUEST_URL_COLUMN);
        addSelect(selectColumns, columns, "request_method", "request_method");
        addSelect(selectColumns, columns, "request_params", "request_params");
        addSelect(selectColumns, columns, "response_result", "response_result");
        addSelect(selectColumns, columns, "ip_address", "ip_address");
        addSelect(selectColumns, columns, "location", "location");
        addSelect(selectColumns, columns, "operation_result", "operation_result");
        addSelect(selectColumns, columns, "result_message", "result_message");
        addSelect(selectColumns, columns, "error_code", "error_code");
        addSelect(selectColumns, columns, "exception_class", "exception_class");
        addSelect(selectColumns, columns, "operation_time", "operation_time");
        addSelect(selectColumns, columns, "create_time", "create_time");
        return String.join(", ", selectColumns);
    }

    private void addSelect(List<String> selectColumns, Set<String> columns, String alias, String... candidates) {
        String column = resolveColumn(columns, candidates);
        if (column == null) {
            return;
        }
        if (alias.equals(column)) {
            selectColumns.add(column);
            return;
        }
        selectColumns.add(column + " AS " + alias);
    }

    private String buildOrderByClause(Set<String> columns) {
        List<String> orderColumns = new ArrayList<>();
        if (resolveColumn(columns, "operation_time") != null) {
            orderColumns.add("operation_time DESC");
        }
        if (resolveColumn(columns, "create_time") != null) {
            orderColumns.add("create_time DESC");
        }
        orderColumns.add("id DESC");
        return " ORDER BY " + String.join(", ", orderColumns);
    }

    private String resolveColumn(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean appendEquals(StringBuilder where, List<Object> params, String column, Object value) {
        if (value == null) {
            return false;
        }
        if (column == null) {
            return true;
        }
        where.append(" AND ").append(column).append(" = ?");
        params.add(value);
        return false;
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

    private AuditLog mapRow(ResultSet rs, Set<String> columns) throws SQLException {
        AuditLog log = new AuditLog();
        if (resolveColumn(columns, "id") != null) {
            log.setId(getLong(rs, "id"));
        }
        if (resolveColumn(columns, "tenant_id") != null) {
            log.setTenantId(getLong(rs, "tenant_id"));
        }
        if (resolveColumn(columns, "user_id") != null) {
            log.setUserId(getLong(rs, "user_id"));
        }
        if (resolveColumn(columns, "user_name") != null) {
            log.setUserName(rs.getString("user_name"));
        }
        if (resolveColumn(columns, "trace_id") != null) {
            log.setTraceId(rs.getString("trace_id"));
        }
        if (resolveColumn(columns, "device_code") != null) {
            log.setDeviceCode(rs.getString("device_code"));
        }
        if (resolveColumn(columns, "product_key") != null) {
            log.setProductKey(rs.getString("product_key"));
        }
        if (resolveColumn(columns, "operation_type", LEGACY_OPERATION_TYPE_COLUMN) != null) {
            log.setOperationType(rs.getString("operation_type"));
        }
        if (resolveColumn(columns, "operation_module") != null) {
            log.setOperationModule(rs.getString("operation_module"));
        }
        if (resolveColumn(columns, "operation_method") != null) {
            log.setOperationMethod(rs.getString("operation_method"));
        }
        if (resolveColumn(columns, "request_url", LEGACY_REQUEST_URL_COLUMN) != null) {
            log.setRequestUrl(rs.getString("request_url"));
        }
        if (resolveColumn(columns, "request_method") != null) {
            log.setRequestMethod(rs.getString("request_method"));
        }
        if (resolveColumn(columns, "request_params") != null) {
            log.setRequestParams(rs.getString("request_params"));
        }
        if (resolveColumn(columns, "response_result") != null) {
            log.setResponseResult(rs.getString("response_result"));
        }
        if (resolveColumn(columns, "ip_address") != null) {
            log.setIpAddress(rs.getString("ip_address"));
        }
        if (resolveColumn(columns, "location") != null) {
            log.setLocation(rs.getString("location"));
        }
        if (resolveColumn(columns, "operation_result") != null) {
            log.setOperationResult(getInteger(rs, "operation_result"));
        }
        if (resolveColumn(columns, "result_message") != null) {
            log.setResultMessage(rs.getString("result_message"));
        }
        if (resolveColumn(columns, "error_code") != null) {
            log.setErrorCode(rs.getString("error_code"));
        }
        if (resolveColumn(columns, "exception_class") != null) {
            log.setExceptionClass(rs.getString("exception_class"));
        }
        if (resolveColumn(columns, "operation_time") != null) {
            log.setOperationTime(getDate(rs, "operation_time"));
        }
        if (resolveColumn(columns, "create_time") != null) {
            log.setCreateTime(getDate(rs, "create_time"));
        }
        return log;
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Date getDate(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    private Object toJdbcValue(Object value) {
        if (value instanceof Date date) {
            return new Timestamp(date.getTime());
        }
        return value;
    }

    private record QuerySpec(String whereClause, List<Object> params, boolean emptyResult) {

        private static QuerySpec emptySpec() {
            return new QuerySpec("", List.of(), true);
        }
    }
}
