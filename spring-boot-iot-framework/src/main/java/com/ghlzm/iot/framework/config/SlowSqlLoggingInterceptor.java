package com.ghlzm.iot.framework.config;

import com.ghlzm.iot.framework.observability.ObservabilityEventLogSupport;
import com.ghlzm.iot.framework.observability.SensitiveLogSanitizer;
import com.ghlzm.iot.framework.observability.TraceContextHolder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceStatus;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilitySpanTypes;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 慢 SQL 摘要日志，不打印完整参数。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SlowSqlLoggingInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticLoggingConstants.DIAGNOSTIC_SQL_LOGGER_NAME);
    private static final int MAX_SQL_LENGTH = 1000;

    private final IotProperties iotProperties;
    private ObservabilityEvidenceRecorder evidenceRecorder = ObservabilityEvidenceRecorder.noop();

    public SlowSqlLoggingInterceptor(IotProperties iotProperties) {
        this.iotProperties = iotProperties;
    }

    @Autowired(required = false)
    public void setObservabilityEvidenceRecorder(ObservabilityEvidenceRecorder evidenceRecorder) {
        if (evidenceRecorder != null) {
            this.evidenceRecorder = evidenceRecorder;
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long thresholdMs = resolveThresholdMs();
        if (thresholdMs <= 0 || !log.isInfoEnabled()) {
            return invocation.proceed();
        }

        long startNs = System.nanoTime();
        LocalDateTime startedAt = LocalDateTime.now();
        Object result = null;
        Throwable error = null;
        try {
            result = invocation.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            long costMs = (System.nanoTime() - startNs) / 1_000_000L;
            if (costMs >= thresholdMs) {
                MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
                BoundSql boundSql = mappedStatement.getBoundSql(invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null);
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("traceId", TraceContextHolder.getTraceId());
                details.put("statementId", mappedStatement.getId());
                details.put("commandType", mappedStatement.getSqlCommandType());
                details.put("rowCount", resolveRowCount(result));
                if (error != null) {
                    details.put("errorClass", error.getClass().getSimpleName());
                }
                String normalizedSql = normalizeSql(boundSql == null ? null : boundSql.getSql());
                details.put("sql", normalizedSql);
                log.info(ObservabilityEventLogSupport.summary(
                        "slow_sql",
                        error == null ? "success" : "failure",
                        costMs,
                        details
                ));
                recordSlowSqlSpan(mappedStatement, costMs, startedAt, error, details);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }

    private long resolveThresholdMs() {
        IotProperties.Observability observability = iotProperties.getObservability();
        if (observability == null || observability.getPerformance() == null) {
            return 0L;
        }
        Long thresholdMs = observability.getPerformance().getSlowSqlThresholdMs();
        return thresholdMs == null ? 0L : thresholdMs;
    }

    private Integer resolveRowCount(Object result) {
        if (result instanceof Integer count) {
            return count;
        }
        if (result instanceof Collection<?> collection) {
            return collection.size();
        }
        return null;
    }

    private void recordSlowSqlSpan(MappedStatement mappedStatement,
                                   long costMs,
                                   LocalDateTime startedAt,
                                   Throwable error,
                                   Map<String, Object> details) {
        ObservabilitySpanLogRecord span = new ObservabilitySpanLogRecord();
        span.setTenantId(1L);
        span.setTraceId(TraceContextHolder.getTraceId());
        span.setSpanType(ObservabilitySpanTypes.SLOW_SQL);
        span.setSpanName(mappedStatement.getId());
        span.setDomainCode("database");
        span.setEventCode("platform.performance.slow_sql");
        span.setStatus(error == null ? ObservabilityEvidenceStatus.SUCCESS : ObservabilityEvidenceStatus.FAILURE);
        span.setDurationMs(costMs);
        span.setStartedAt(startedAt);
        span.setFinishedAt(LocalDateTime.now());
        if (error != null) {
            span.setErrorClass(error.getClass().getName());
            span.setErrorMessage(SensitiveLogSanitizer.sanitize(error.getMessage()));
        }
        span.getTags().putAll(details);
        evidenceRecorder.recordSpan(span);
    }

    private String normalizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return null;
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        normalized = SensitiveLogSanitizer.sanitize(normalized);
        if (normalized.length() <= MAX_SQL_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_SQL_LENGTH) + "...(truncated)";
    }
}
