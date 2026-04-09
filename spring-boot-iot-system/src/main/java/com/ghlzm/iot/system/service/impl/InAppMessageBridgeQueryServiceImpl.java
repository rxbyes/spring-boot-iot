package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeAttemptLogMapper;
import com.ghlzm.iot.system.mapper.InAppMessageBridgeLogMapper;
import com.ghlzm.iot.system.service.InAppMessageBridgeQueryService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.InAppMessageBridgeAttemptVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeLogVO;
import com.ghlzm.iot.system.vo.InAppMessageBridgeStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InAppMessageBridgeQueryServiceImpl implements InAppMessageBridgeQueryService {

    private static final long DEFAULT_RANGE_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper;
    private final InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper;
    private final PermissionService permissionService;
    private final SystemContentSchemaSupport systemContentSchemaSupport;

    public InAppMessageBridgeQueryServiceImpl(InAppMessageBridgeLogMapper inAppMessageBridgeLogMapper,
                                              InAppMessageBridgeAttemptLogMapper inAppMessageBridgeAttemptLogMapper,
                                              PermissionService permissionService,
                                              SystemContentSchemaSupport systemContentSchemaSupport) {
        this.inAppMessageBridgeLogMapper = inAppMessageBridgeLogMapper;
        this.inAppMessageBridgeAttemptLogMapper = inAppMessageBridgeAttemptLogMapper;
        this.permissionService = permissionService;
        this.systemContentSchemaSupport = systemContentSchemaSupport;
    }

    @Override
    public InAppMessageBridgeStatsVO getBridgeStats(Date startTime,
                                                    Date endTime,
                                                    String messageType,
                                                    String sourceType,
                                                    String priority,
                                                    String channelCode,
                                                    Integer bridgeStatus) {
        return getBridgeStats(null, startTime, endTime, messageType, sourceType, priority, channelCode, bridgeStatus);
    }

    @Override
    public InAppMessageBridgeStatsVO getBridgeStats(Long currentUserId,
                                                    Date startTime,
                                                    Date endTime,
                                                    String messageType,
                                                    String sourceType,
                                                    String priority,
                                                    String channelCode,
                                                    Integer bridgeStatus) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        systemContentSchemaSupport.ensureInAppMessageBridgeLogReady();

        Date[] normalizedRange = normalizeRange(startTime, endTime);
        List<InAppMessageBridgeLogVO> logs = inAppMessageBridgeLogMapper.listBridgeLogsForStats(
                normalizedRange[0],
                normalizedRange[1],
                normalizeText(messageType),
                normalizeText(sourceType),
                normalizeText(priority),
                normalizeText(channelCode),
                bridgeStatus,
                resolveTenantId(currentUserId)
        );
        InAppMessageBridgeStatsVO stats = new InAppMessageBridgeStatsVO();
        stats.setStartTime(formatDateTime(normalizedRange[0]));
        stats.setEndTime(formatDateTime(normalizedRange[1]));
        if (logs == null || logs.isEmpty()) {
            return stats;
        }

        Map<String, InAppMessageBridgeStatsVO.TrendBucket> trendBuckets = new LinkedHashMap<>();
        Map<String, InAppMessageBridgeStatsVO.ChannelBucket> channelBuckets = new LinkedHashMap<>();
        Map<String, InAppMessageBridgeStatsVO.SourceTypeBucket> sourceTypeBuckets = new LinkedHashMap<>();

        for (InAppMessageBridgeLogVO log : logs) {
            boolean success = Integer.valueOf(1).equals(log.getBridgeStatus());
            stats.setTotalBridgeCount(stats.getTotalBridgeCount() + 1L);
            stats.setTotalAttemptCount(stats.getTotalAttemptCount() + safeLong(log.getAttemptCount()));
            if (success) {
                stats.setSuccessCount(stats.getSuccessCount() + 1L);
            } else {
                stats.setPendingRetryCount(stats.getPendingRetryCount() + 1L);
            }

            String trendKey = resolveTrendDate(log.getLastAttemptTime());
            InAppMessageBridgeStatsVO.TrendBucket trendBucket = trendBuckets.computeIfAbsent(trendKey, key -> {
                InAppMessageBridgeStatsVO.TrendBucket bucket = new InAppMessageBridgeStatsVO.TrendBucket();
                bucket.setDate(key);
                return bucket;
            });
            trendBucket.setBridgeCount(trendBucket.getBridgeCount() + 1L);
            trendBucket.setTotalAttemptCount(trendBucket.getTotalAttemptCount() + safeLong(log.getAttemptCount()));
            if (success) {
                trendBucket.setSuccessCount(trendBucket.getSuccessCount() + 1L);
            } else {
                trendBucket.setPendingRetryCount(trendBucket.getPendingRetryCount() + 1L);
            }

            String normalizedChannelCode = normalizeText(log.getChannelCode());
            InAppMessageBridgeStatsVO.ChannelBucket channelBucket = channelBuckets.computeIfAbsent(normalizedChannelCode, key -> {
                InAppMessageBridgeStatsVO.ChannelBucket bucket = new InAppMessageBridgeStatsVO.ChannelBucket();
                bucket.setKey(key);
                bucket.setLabel(StringUtils.hasText(log.getChannelName()) ? log.getChannelName() : key);
                bucket.setChannelType(log.getChannelType());
                return bucket;
            });
            channelBucket.setBridgeCount(channelBucket.getBridgeCount() + 1L);
            if (success) {
                channelBucket.setSuccessCount(channelBucket.getSuccessCount() + 1L);
            } else {
                channelBucket.setPendingRetryCount(channelBucket.getPendingRetryCount() + 1L);
            }

            String normalizedSourceType = normalizeText(log.getSourceType());
            InAppMessageBridgeStatsVO.SourceTypeBucket sourceTypeBucket = sourceTypeBuckets.computeIfAbsent(normalizedSourceType, key -> {
                InAppMessageBridgeStatsVO.SourceTypeBucket bucket = new InAppMessageBridgeStatsVO.SourceTypeBucket();
                bucket.setKey(key);
                bucket.setLabel(resolveSourceTypeLabel(key));
                return bucket;
            });
            sourceTypeBucket.setBridgeCount(sourceTypeBucket.getBridgeCount() + 1L);
            if (success) {
                sourceTypeBucket.setSuccessCount(sourceTypeBucket.getSuccessCount() + 1L);
            } else {
                sourceTypeBucket.setPendingRetryCount(sourceTypeBucket.getPendingRetryCount() + 1L);
            }
        }

        stats.setSuccessRate(calculateRate(stats.getSuccessCount(), stats.getTotalBridgeCount()));
        stats.setTrend(trendBuckets.values().stream()
                .sorted(Comparator.comparing(InAppMessageBridgeStatsVO.TrendBucket::getDate))
                .toList());
        stats.setChannelBuckets(channelBuckets.values().stream()
                .peek(bucket -> bucket.setSuccessRate(calculateRate(bucket.getSuccessCount(), bucket.getBridgeCount())))
                .sorted(Comparator.comparing(InAppMessageBridgeStatsVO.ChannelBucket::getBridgeCount, Comparator.reverseOrder())
                        .thenComparing(InAppMessageBridgeStatsVO.ChannelBucket::getSuccessCount, Comparator.reverseOrder())
                        .thenComparing(bucket -> bucket.getKey() == null ? "" : bucket.getKey()))
                .toList());
        stats.setSourceTypeBuckets(sourceTypeBuckets.values().stream()
                .peek(bucket -> bucket.setSuccessRate(calculateRate(bucket.getSuccessCount(), bucket.getBridgeCount())))
                .sorted(Comparator.comparing(InAppMessageBridgeStatsVO.SourceTypeBucket::getBridgeCount, Comparator.reverseOrder())
                        .thenComparing(InAppMessageBridgeStatsVO.SourceTypeBucket::getSuccessCount, Comparator.reverseOrder())
                        .thenComparing(bucket -> bucket.getKey() == null ? "" : bucket.getKey()))
                .toList());
        return stats;
    }

    @Override
    public PageResult<InAppMessageBridgeLogVO> pageBridgeLogs(Date startTime,
                                                              Date endTime,
                                                              String messageType,
                                                              String sourceType,
                                                              String priority,
                                                              String channelCode,
                                                              Integer bridgeStatus,
                                                              Long pageNum,
                                                              Long pageSize) {
        return pageBridgeLogs(null, startTime, endTime, messageType, sourceType, priority, channelCode, bridgeStatus, pageNum, pageSize);
    }

    @Override
    public PageResult<InAppMessageBridgeLogVO> pageBridgeLogs(Long currentUserId,
                                                              Date startTime,
                                                              Date endTime,
                                                              String messageType,
                                                              String sourceType,
                                                              String priority,
                                                              String channelCode,
                                                              Integer bridgeStatus,
                                                              Long pageNum,
                                                              Long pageSize) {
        systemContentSchemaSupport.ensureInAppMessageReady();
        systemContentSchemaSupport.ensureInAppMessageBridgeLogReady();

        Date[] normalizedRange = normalizeRange(startTime, endTime);
        long safePageNum = PageQueryUtils.normalizePageNum(pageNum);
        long safePageSize = PageQueryUtils.normalizePageSize(pageSize);
        Long total = inAppMessageBridgeLogMapper.countBridgeLogs(
                normalizedRange[0],
                normalizedRange[1],
                normalizeText(messageType),
                normalizeText(sourceType),
                normalizeText(priority),
                normalizeText(channelCode),
                bridgeStatus,
                resolveTenantId(currentUserId)
        );
        if (total == null || total <= 0L) {
            return PageResult.empty(safePageNum, safePageSize);
        }
        long offset = (safePageNum - 1L) * safePageSize;
        List<InAppMessageBridgeLogVO> records = inAppMessageBridgeLogMapper.pageBridgeLogs(
                normalizedRange[0],
                normalizedRange[1],
                normalizeText(messageType),
                normalizeText(sourceType),
                normalizeText(priority),
                normalizeText(channelCode),
                bridgeStatus,
                resolveTenantId(currentUserId),
                offset,
                safePageSize
        );
        return PageResult.of(total, safePageNum, safePageSize, records == null ? List.of() : records);
    }

    @Override
    public List<InAppMessageBridgeAttemptVO> listBridgeAttempts(Long bridgeLogId) {
        return listBridgeAttempts(null, bridgeLogId);
    }

    @Override
    public List<InAppMessageBridgeAttemptVO> listBridgeAttempts(Long currentUserId, Long bridgeLogId) {
        if (bridgeLogId == null) {
            throw new BizException("桥接记录ID不能为空");
        }
        systemContentSchemaSupport.ensureInAppMessageBridgeAttemptLogReady();
        List<InAppMessageBridgeAttemptVO> attempts = inAppMessageBridgeAttemptLogMapper.listAttemptsByBridgeLogId(
                bridgeLogId,
                resolveTenantId(currentUserId)
        );
        return attempts == null ? List.of() : attempts;
    }

    private Date[] normalizeRange(Date startTime, Date endTime) {
        if (startTime != null && endTime != null && startTime.after(endTime)) {
            throw new BizException("开始时间不能晚于结束时间");
        }
        if (startTime == null && endTime == null) {
            Date now = new Date();
            return new Date[]{new Date(now.getTime() - DEFAULT_RANGE_MILLIS), now};
        }
        return new Date[]{startTime, endTime};
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Long resolveTenantId(Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return permissionService.getDataPermissionContext(currentUserId).tenantId();
    }

    private String formatDateTime(Date value) {
        if (value == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value);
    }

    private String resolveTrendDate(String lastAttemptTime) {
        String normalized = normalizeText(lastAttemptTime);
        if (normalized == null) {
            return "未知日期";
        }
        return normalized.length() >= 10 ? normalized.substring(0, 10) : normalized;
    }

    private String resolveSourceTypeLabel(String sourceType) {
        String normalized = normalizeText(sourceType);
        if (!StringUtils.hasText(normalized)) {
            return "未知来源";
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "manual" -> "手工广播";
            case "system_error" -> "系统异常";
            case "event_dispatch" -> "事件派工";
            case "work_order" -> "工单状态";
            case "governance" -> "治理任务";
            default -> normalized;
        };
    }

    private long safeLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private double calculateRate(Long numerator, Long denominator) {
        if (numerator == null || denominator == null || denominator <= 0L) {
            return 0D;
        }
        return (double) numerator / denominator;
    }
}
