package com.ghlzm.iot.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.mapper.AlarmRecordMapper;
import com.ghlzm.iot.alarm.mapper.EventRecordMapper;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.report.entity.AlarmStatistics;
import com.ghlzm.iot.report.entity.DeviceHealthStatistics;
import com.ghlzm.iot.report.entity.EventStatistics;
import com.ghlzm.iot.report.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Report service backed by Phase 4 business tables.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final int EVENT_STATUS_PENDING = 0;
    private static final int EVENT_STATUS_DISPATCHED = 1;
    private static final int EVENT_STATUS_PROCESSING = 2;
    private static final int EVENT_STATUS_WAITING_ACCEPTANCE = 3;
    private static final int EVENT_STATUS_CLOSED = 4;
    private static final int DEVICE_ONLINE = 1;
    private static final long HEALTHY_THRESHOLD_HOURS = 24L;
    private static final long WARNING_THRESHOLD_HOURS = 72L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    );

    private final AlarmRecordMapper alarmRecordMapper;
    private final EventRecordMapper eventRecordMapper;
    private final DeviceMapper deviceMapper;

    public ReportServiceImpl(AlarmRecordMapper alarmRecordMapper,
                             EventRecordMapper eventRecordMapper,
                             DeviceMapper deviceMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.eventRecordMapper = eventRecordMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public List<AlarmStatistics> getRiskTrendAnalysis(String startDate, String endDate) {
        // 趋势口径：按天聚合告警与事件，优先使用触发时间，缺失时回退创建时间。
        DateRange dateRange = DateRange.of(startDate, endDate);
        List<AlarmRecord> alarms = listAlarmRecords(dateRange);
        List<EventRecord> events = listEventRecords(dateRange);
        if (alarms.isEmpty() && events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<LocalDate, AlarmStatistics> trendMap = new TreeMap<>();
        if (dateRange.hasClosedRange()) {
            LocalDate cursor = dateRange.startDate();
            while (!cursor.isAfter(dateRange.endDate())) {
                trendMap.put(cursor, createTrendPoint(cursor));
                cursor = cursor.plusDays(1);
            }
        }

        for (AlarmRecord record : alarms) {
            Optional<LocalDate> date = resolveAlarmRecordDate(record);
            if (date.isEmpty()) {
                continue;
            }
            AlarmStatistics point = trendMap.computeIfAbsent(date.get(), this::createTrendPoint);
            point.setAlarmCount(safeInt(point.getAlarmCount()) + 1);
        }

        for (EventRecord record : events) {
            Optional<LocalDate> date = resolveEventRecordDate(record);
            if (date.isEmpty()) {
                continue;
            }
            AlarmStatistics point = trendMap.computeIfAbsent(date.get(), this::createTrendPoint);
            point.setEventCount(safeInt(point.getEventCount()) + 1);
        }

        List<AlarmStatistics> result = new ArrayList<>(trendMap.values());
        result.sort(Comparator.comparing(AlarmStatistics::getDate));
        result.forEach(item -> {
            item.setAlarmCount(safeInt(item.getAlarmCount()));
            item.setEventCount(safeInt(item.getEventCount()));
            item.setCount(item.getAlarmCount());
            item.setTotal(item.getAlarmCount());
        });
        return result;
    }

    @Override
    public AlarmStatistics getAlarmStatistics(String startDate, String endDate) {
        // 统计口径：同时维护新旧字段，保证前端历史版本兼容读取。
        DateRange dateRange = DateRange.of(startDate, endDate);
        List<AlarmRecord> alarms = listAlarmRecords(dateRange);

        AlarmStatistics statistics = new AlarmStatistics();
        statistics.setTotal(alarms.size());
        statistics.setCount(alarms.size());
        statistics.setAlarmCount(alarms.size());
        statistics.setEventCount(0);
        statistics.setCritical(0);
        statistics.setHigh(0);
        statistics.setMedium(0);
        statistics.setLow(0);
        statistics.setCriticalCount(0);
        statistics.setWarningCount(0);
        statistics.setInfoCount(0);

        for (AlarmRecord alarm : alarms) {
            LevelBucket bucket = resolveLevelBucket(alarm.getAlarmLevel());
            statistics.setCritical(safeInt(statistics.getCritical()) + bucket.critical());
            statistics.setHigh(safeInt(statistics.getHigh()) + bucket.high());
            statistics.setMedium(safeInt(statistics.getMedium()) + bucket.medium());
            statistics.setLow(safeInt(statistics.getLow()) + bucket.low());
            statistics.setCriticalCount(safeInt(statistics.getCriticalCount()) + bucket.legacyCritical());
            statistics.setWarningCount(safeInt(statistics.getWarningCount()) + bucket.legacyWarning());
            statistics.setInfoCount(safeInt(statistics.getInfoCount()) + bucket.legacyInfo());
        }
        return statistics;
    }

    @Override
    public EventStatistics getEventClosureAnalysis(String startDate, String endDate) {
        // 闭环分析：根据事件状态计算已闭环/未闭环，并估算平均处理时长。
        DateRange dateRange = DateRange.of(startDate, endDate);
        List<EventRecord> events = listEventRecords(dateRange);

        int pendingCount = 0;
        int processingCount = 0;
        int closedCount = 0;
        long totalClosedDurationMinutes = 0L;
        int closedDurationSamples = 0;

        for (EventRecord event : events) {
            Integer status = event.getStatus();
            if (status == null) {
                pendingCount++;
                continue;
            }
            if (status == EVENT_STATUS_CLOSED) {
                closedCount++;
                Optional<Long> durationMinutes = resolveDurationMinutes(
                        parseDateTime(event.getTriggerTime()).or(() -> Optional.ofNullable(event.getCreateTime())),
                        parseDateTime(event.getCloseTime()).or(() -> Optional.ofNullable(event.getUpdateTime()))
                );
                if (durationMinutes.isPresent()) {
                    totalClosedDurationMinutes += durationMinutes.get();
                    closedDurationSamples++;
                }
                continue;
            }
            if (status == EVENT_STATUS_PENDING || status == EVENT_STATUS_DISPATCHED) {
                pendingCount++;
                continue;
            }
            if (status == EVENT_STATUS_PROCESSING || status == EVENT_STATUS_WAITING_ACCEPTANCE) {
                processingCount++;
            }
        }

        EventStatistics statistics = new EventStatistics();
        statistics.setCount(events.size());
        statistics.setTotal(events.size());
        statistics.setClosed(closedCount);
        statistics.setUnclosed(events.size() - closedCount);
        statistics.setPendingCount(pendingCount);
        statistics.setProcessingCount(processingCount);
        statistics.setClosedCount(closedCount);
        statistics.setAvgProcessingTime(closedDurationSamples == 0
                ? 0D
                : BigDecimal.valueOf((double) totalClosedDurationMinutes / closedDurationSamples / 60D)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue());
        return statistics;
    }

    @Override
    public DeviceHealthStatistics getDeviceHealthAnalysis() {
        // 健康分层：在线且24小时内上报视为健康，72小时内为预警，其余为严重。
        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>());
        int total = devices.size();
        int online = 0;
        int offline = 0;
        int healthy = 0;
        int warning = 0;
        int critical = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Device device : devices) {
            if (DEVICE_ONLINE == safeInt(device.getOnlineStatus())) {
                online++;
            } else {
                offline++;
            }

            LocalDateTime lastReportTime = device.getLastReportTime();
            if (lastReportTime == null) {
                critical++;
                continue;
            }

            long inactiveHours = Duration.between(lastReportTime, now).toHours();
            if (DEVICE_ONLINE == safeInt(device.getOnlineStatus()) && inactiveHours <= HEALTHY_THRESHOLD_HOURS) {
                healthy++;
            } else if (inactiveHours <= WARNING_THRESHOLD_HOURS) {
                warning++;
            } else {
                critical++;
            }
        }

        DeviceHealthStatistics statistics = new DeviceHealthStatistics();
        statistics.setTotal(total);
        statistics.setTotalCount(total);
        statistics.setOnline(online);
        statistics.setOnlineCount(online);
        statistics.setOffline(offline);
        statistics.setOfflineCount(offline);
        statistics.setOnlineRate(total == 0
                ? 0D
                : BigDecimal.valueOf((double) online * 100D / total)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue());
        statistics.setHealthy(healthy);
        statistics.setHealthyCount(healthy);
        statistics.setWarning(warning);
        statistics.setCritical(critical);
        statistics.setUnhealthyCount(warning + critical);
        return statistics;
    }

    private List<AlarmRecord> listAlarmRecords(DateRange dateRange) {
        List<AlarmRecord> alarms = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>());
        if (!dateRange.hasAnyBound()) {
            return alarms;
        }
        return alarms.stream()
                .filter(record -> resolveAlarmRecordDate(record)
                        .map(dateRange::contains)
                        .orElse(false))
                .toList();
    }

    private List<EventRecord> listEventRecords(DateRange dateRange) {
        List<EventRecord> events = eventRecordMapper.selectList(new LambdaQueryWrapper<EventRecord>());
        if (!dateRange.hasAnyBound()) {
            return events;
        }
        return events.stream()
                .filter(record -> resolveEventRecordDate(record)
                        .map(dateRange::contains)
                        .orElse(false))
                .toList();
    }

    private AlarmStatistics createTrendPoint(LocalDate date) {
        AlarmStatistics statistics = new AlarmStatistics();
        statistics.setDate(date.format(DATE_FORMATTER));
        statistics.setAlarmCount(0);
        statistics.setEventCount(0);
        statistics.setCount(0);
        statistics.setTotal(0);
        return statistics;
    }

    private Optional<LocalDate> resolveAlarmRecordDate(AlarmRecord record) {
        return parseDateTime(record.getTriggerTime())
                .or(() -> Optional.ofNullable(record.getCreateTime()))
                .map(LocalDateTime::toLocalDate);
    }

    private Optional<LocalDate> resolveEventRecordDate(EventRecord record) {
        return parseDateTime(record.getTriggerTime())
                .or(() -> Optional.ofNullable(record.getCreateTime()))
                .map(LocalDateTime::toLocalDate);
    }

    private Optional<LocalDateTime> parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        // 兼容历史多种时间格式，避免因格式不一致导致报表统计缺失。
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return Optional.of(LocalDateTime.parse(trimmed, formatter));
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }

        try {
            return Optional.of(OffsetDateTime.parse(trimmed).toLocalDateTime());
        } catch (DateTimeParseException ignored) {
            // Ignore invalid offset datetime formats.
        }

        try {
            return Optional.of(LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay());
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Long> resolveDurationMinutes(Optional<LocalDateTime> start, Optional<LocalDateTime> end) {
        if (start.isEmpty() || end.isEmpty() || end.get().isBefore(start.get())) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(start.get(), end.get()).toMinutes());
    }

    private LevelBucket resolveLevelBucket(String alarmLevel) {
        String normalized = alarmLevel == null ? "" : alarmLevel.trim().toLowerCase();
        return switch (normalized) {
            case "critical" -> new LevelBucket(1, 0, 0, 0, 1, 0, 0);
            case "high" -> new LevelBucket(0, 1, 0, 0, 0, 1, 0);
            case "medium" -> new LevelBucket(0, 0, 1, 0, 0, 0, 1);
            case "low" -> new LevelBucket(0, 0, 0, 1, 0, 0, 1);
            case "warning" -> new LevelBucket(0, 1, 0, 0, 0, 1, 0);
            case "info" -> new LevelBucket(0, 0, 1, 0, 0, 0, 1);
            default -> new LevelBucket(0, 0, 0, 1, 0, 0, 1);
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {

        private static DateRange of(String startDate, String endDate) {
            return new DateRange(parseDate(startDate).orElse(null), parseDate(endDate).orElse(null));
        }

        private static Optional<LocalDate> parseDate(String value) {
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            try {
                return Optional.of(LocalDate.parse(value.trim(), DATE_FORMATTER));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }

        private boolean hasAnyBound() {
            return startDate != null || endDate != null;
        }

        private boolean hasClosedRange() {
            return startDate != null && endDate != null && !startDate.isAfter(endDate);
        }

        private boolean contains(LocalDate date) {
            if (date == null) {
                return false;
            }
            if (startDate != null && date.isBefore(startDate)) {
                return false;
            }
            if (endDate != null && date.isAfter(endDate)) {
                return false;
            }
            return true;
        }
    }

    private record LevelBucket(int critical, int high, int medium, int low,
                               int legacyCritical, int legacyWarning, int legacyInfo) {
    }
}
