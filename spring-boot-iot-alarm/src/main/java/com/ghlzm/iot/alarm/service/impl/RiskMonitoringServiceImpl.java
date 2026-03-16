package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.alarm.dto.RiskMonitoringListQuery;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.AlarmRecordMapper;
import com.ghlzm.iot.alarm.mapper.EventRecordMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.service.RiskMonitoringService;
import com.ghlzm.iot.alarm.vo.RiskMonitoringAlarmSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringDetailVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringEventSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringGisPointVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringListItemVO;
import com.ghlzm.iot.alarm.vo.RiskMonitoringTrendPointVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 风险监测服务实现。
 */
@Service
public class RiskMonitoringServiceImpl implements RiskMonitoringService {

    private static final List<Integer> ACTIVE_ALARM_STATUSES = List.of(0, 1, 2);
    private static final int DETAIL_LIMIT = 10;
    private static final int TREND_LIMIT = 200;
    private static final String RISK_MONITORING_SCHEMA_HINT =
            "风险监测依赖表 risk_point_device 缺失，请先执行 sql/upgrade/20260316_phase4_task3_risk_monitoring_schema_sync.sql";

    private final RiskPointMapper riskPointMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final EventRecordMapper eventRecordMapper;
    private final DeviceMapper deviceMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final ProductMapper productMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RiskMonitoringServiceImpl(RiskPointMapper riskPointMapper,
                                     RiskPointDeviceMapper riskPointDeviceMapper,
                                     AlarmRecordMapper alarmRecordMapper,
                                     EventRecordMapper eventRecordMapper,
                                     DeviceMapper deviceMapper,
                                     DevicePropertyMapper devicePropertyMapper,
                                     DeviceMessageLogMapper deviceMessageLogMapper,
                                     ProductMapper productMapper,
                                     JdbcTemplate jdbcTemplate) {
        this.riskPointMapper = riskPointMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.alarmRecordMapper = alarmRecordMapper;
        this.eventRecordMapper = eventRecordMapper;
        this.deviceMapper = deviceMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.productMapper = productMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResult<RiskMonitoringListItemVO> listRealtimeItems(RiskMonitoringListQuery query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10L : Math.min(query.getPageSize(), 100L);

        List<RiskPointDevice> bindings = listBindings(query.getRiskPointId());
        if (bindings.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        MonitoringContext context = buildContext(bindings);
        List<RiskMonitoringListItemVO> items = bindings.stream()
                .map(binding -> buildListItem(binding, context))
                .filter(Objects::nonNull)
                .filter(item -> matchQuery(item, query))
                .sorted(Comparator.comparing(RiskMonitoringListItemVO::getLatestReportTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return toPage(items, pageNum, pageSize);
    }

    @Override
    public RiskMonitoringDetailVO getRealtimeDetail(Long bindingId) {
        ensureBindingTableReady();
        RiskPointDevice binding = riskPointDeviceMapper.selectById(bindingId);
        if (binding == null || Integer.valueOf(1).equals(binding.getDeleted())) {
            throw new BizException("风险监测绑定不存在");
        }

        MonitoringContext context = buildContext(List.of(binding));
        RiskMonitoringListItemVO listItem = buildListItem(binding, context);
        if (listItem == null) {
            throw new BizException("风险监测绑定数据不完整");
        }

        Device device = context.deviceMap.get(binding.getDeviceId());
        DeviceProperty property = context.propertyMap.get(propertyKey(binding.getDeviceId(), binding.getMetricIdentifier()));
        RiskPoint riskPoint = context.riskPointMap.get(binding.getRiskPointId());

        RiskMonitoringDetailVO detail = new RiskMonitoringDetailVO();
        detail.setBindingId(binding.getId());
        detail.setRegionId(listItem.getRegionId());
        detail.setRegionName(listItem.getRegionName());
        detail.setRiskPointId(listItem.getRiskPointId());
        detail.setRiskPointCode(riskPoint == null ? null : riskPoint.getRiskPointCode());
        detail.setRiskPointName(listItem.getRiskPointName());
        detail.setRiskLevel(listItem.getRiskLevel());
        detail.setDeviceId(listItem.getDeviceId());
        detail.setDeviceCode(listItem.getDeviceCode());
        detail.setDeviceName(listItem.getDeviceName());
        detail.setProductName(listItem.getProductName());
        detail.setMetricIdentifier(listItem.getMetricIdentifier());
        detail.setMetricName(listItem.getMetricName());
        detail.setCurrentValue(listItem.getCurrentValue());
        detail.setUnit(listItem.getUnit());
        detail.setValueType(property == null ? null : property.getValueType());
        detail.setMonitorStatus(listItem.getMonitorStatus());
        detail.setOnlineStatus(listItem.getOnlineStatus());
        detail.setLatestReportTime(listItem.getLatestReportTime());
        if (device != null) {
            detail.setLongitude(device.getLongitude());
            detail.setLatitude(device.getLatitude());
            detail.setAddress(device.getAddress());
        }

        List<AlarmRecord> alarms = listRecentAlarms(binding);
        List<EventRecord> events = listRecentEvents(binding);
        detail.setActiveAlarmCount((long) alarms.size());
        detail.setRecentEventCount((long) events.size());
        detail.setRecentAlarms(alarms.stream().map(this::toAlarmSummary).toList());
        detail.setRecentEvents(events.stream().map(this::toEventSummary).toList());
        detail.setTrendPoints(buildTrend(binding, property));
        return detail;
    }

    @Override
    public List<RiskMonitoringGisPointVO> listGisPoints(Long regionId) {
        List<RiskPoint> riskPoints = riskPointMapper.selectList(new LambdaQueryWrapper<RiskPoint>()
                .eq(RiskPoint::getDeleted, 0)
                .eq(regionId != null, RiskPoint::getRegionId, regionId)
                .orderByDesc(RiskPoint::getCreateTime));
        if (riskPoints.isEmpty()) {
            return Collections.emptyList();
        }

        ensureBindingTableReady();
        Set<Long> riskPointIds = riskPoints.stream().map(RiskPoint::getId).collect(Collectors.toSet());
        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .in(RiskPointDevice::getRiskPointId, riskPointIds));
        Map<Long, List<RiskPointDevice>> bindingsByRiskPoint = bindings.stream()
                .collect(Collectors.groupingBy(RiskPointDevice::getRiskPointId));

        Set<Long> deviceIds = bindings.stream().map(RiskPointDevice::getDeviceId).collect(Collectors.toSet());
        Map<Long, Device> deviceMap = listDevices(deviceIds);
        Map<Long, Long> onlineCounts = bindings.stream()
                .filter(binding -> {
                    Device device = deviceMap.get(binding.getDeviceId());
                    return device != null && Integer.valueOf(1).equals(device.getOnlineStatus());
                })
                .collect(Collectors.groupingBy(RiskPointDevice::getRiskPointId, Collectors.counting()));

        Map<Long, Long> activeAlarmCounts = alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDeleted, 0)
                        .in(AlarmRecord::getStatus, ACTIVE_ALARM_STATUSES)
                        .in(AlarmRecord::getRiskPointId, riskPointIds))
                .stream()
                .collect(Collectors.groupingBy(AlarmRecord::getRiskPointId, Collectors.counting()));

        List<RiskMonitoringGisPointVO> result = new ArrayList<>();
        for (RiskPoint riskPoint : riskPoints) {
            List<RiskPointDevice> riskBindings = bindingsByRiskPoint.getOrDefault(riskPoint.getId(), Collections.emptyList());
            RiskMonitoringGisPointVO point = new RiskMonitoringGisPointVO();
            point.setRegionId(riskPoint.getRegionId());
            point.setRegionName(riskPoint.getRegionName());
            point.setRiskPointId(riskPoint.getId());
            point.setRiskPointCode(riskPoint.getRiskPointCode());
            point.setRiskPointName(riskPoint.getRiskPointName());
            point.setRiskLevel(riskPoint.getRiskLevel());
            point.setDeviceCount(riskBindings.size());
            point.setOnlineDeviceCount(onlineCounts.getOrDefault(riskPoint.getId(), 0L).intValue());
            point.setActiveAlarmCount(activeAlarmCounts.getOrDefault(riskPoint.getId(), 0L).intValue());
            fillCoordinates(point, riskBindings, deviceMap);
            result.add(point);
        }
        return result;
    }

    private List<RiskPointDevice> listBindings(Long riskPointId) {
        ensureBindingTableReady();
        return riskPointDeviceMapper.selectList(new LambdaQueryWrapper<RiskPointDevice>()
                .eq(RiskPointDevice::getDeleted, 0)
                .eq(riskPointId != null, RiskPointDevice::getRiskPointId, riskPointId));
    }

    private void ensureBindingTableReady() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                "risk_point_device");
        if (tableCount == null || tableCount < 1) {
            throw new BizException(RISK_MONITORING_SCHEMA_HINT);
        }
    }

    private MonitoringContext buildContext(List<RiskPointDevice> bindings) {
        Set<Long> riskPointIds = bindings.stream().map(RiskPointDevice::getRiskPointId).collect(Collectors.toSet());
        Set<Long> deviceIds = bindings.stream().map(RiskPointDevice::getDeviceId).collect(Collectors.toSet());

        Map<Long, RiskPoint> riskPointMap = riskPointMapper.selectList(new LambdaQueryWrapper<RiskPoint>()
                        .eq(RiskPoint::getDeleted, 0)
                        .in(!riskPointIds.isEmpty(), RiskPoint::getId, riskPointIds))
                .stream()
                .collect(Collectors.toMap(RiskPoint::getId, Function.identity(), (left, right) -> left));

        Map<Long, Device> deviceMap = listDevices(deviceIds);

        Set<Long> productIds = deviceMap.values().stream()
                .map(Device::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left));

        Map<String, DeviceProperty> propertyMap = deviceIds.isEmpty() ? Collections.emptyMap()
                : devicePropertyMapper.selectList(new LambdaQueryWrapper<DeviceProperty>()
                .in(DeviceProperty::getDeviceId, deviceIds)).stream()
                .collect(Collectors.toMap(
                        property -> propertyKey(property.getDeviceId(), property.getIdentifier()),
                        Function.identity(),
                        (left, right) -> left));

        Map<String, Boolean> activeAlarmFlags = listActiveAlarmFlags(deviceIds);
        return new MonitoringContext(riskPointMap, deviceMap, productMap, propertyMap, activeAlarmFlags);
    }

    private Map<Long, Device> listDevices(Collection<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceMapper.selectBatchIds(deviceIds).stream()
                .filter(Objects::nonNull)
                .filter(device -> !Integer.valueOf(1).equals(device.getDeleted()))
                .collect(Collectors.toMap(Device::getId, Function.identity(), (left, right) -> left));
    }

    private Map<String, Boolean> listActiveAlarmFlags(Set<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDeleted, 0)
                        .in(AlarmRecord::getStatus, ACTIVE_ALARM_STATUSES)
                        .in(AlarmRecord::getDeviceId, deviceIds))
                .stream()
                .filter(alarm -> alarm.getDeviceId() != null && alarm.getMetricName() != null)
                .collect(Collectors.toMap(
                        alarm -> alarmKey(alarm.getDeviceId(), alarm.getMetricName()),
                        alarm -> Boolean.TRUE,
                        (left, right) -> left));
    }

    private RiskMonitoringListItemVO buildListItem(RiskPointDevice binding, MonitoringContext context) {
        RiskPoint riskPoint = context.riskPointMap.get(binding.getRiskPointId());
        Device device = context.deviceMap.get(binding.getDeviceId());
        if (riskPoint == null || device == null) {
            return null;
        }

        Product product = context.productMap.get(device.getProductId());
        DeviceProperty property = context.propertyMap.get(propertyKey(binding.getDeviceId(), binding.getMetricIdentifier()));
        boolean activeAlarm = context.activeAlarmFlags.containsKey(alarmKey(binding.getDeviceId(), binding.getMetricName()));

        RiskMonitoringListItemVO item = new RiskMonitoringListItemVO();
        item.setBindingId(binding.getId());
        item.setRegionId(riskPoint.getRegionId());
        item.setRegionName(riskPoint.getRegionName());
        item.setRiskPointId(riskPoint.getId());
        item.setRiskPointName(riskPoint.getRiskPointName());
        item.setRiskLevel(riskPoint.getRiskLevel());
        item.setDeviceId(device.getId());
        item.setDeviceCode(device.getDeviceCode());
        item.setDeviceName(device.getDeviceName());
        item.setProductName(product == null ? null : product.getProductName());
        item.setMetricIdentifier(binding.getMetricIdentifier());
        item.setMetricName(binding.getMetricName());
        item.setCurrentValue(property == null ? null : property.getPropertyValue());
        item.setUnit(binding.getThresholdUnit());
        item.setOnlineStatus(device.getOnlineStatus());
        item.setLatestReportTime(property == null ? device.getLastReportTime() : property.getReportTime());
        item.setAlarmFlag(activeAlarm);
        item.setMonitorStatus(resolveMonitorStatus(activeAlarm, device, property));
        return item;
    }

    private boolean matchQuery(RiskMonitoringListItemVO item, RiskMonitoringListQuery query) {
        if (query.getRegionId() != null && !Objects.equals(query.getRegionId(), item.getRegionId())) {
            return false;
        }
        if (query.getRiskPointId() != null && !Objects.equals(query.getRiskPointId(), item.getRiskPointId())) {
            return false;
        }
        if (query.getRiskLevel() != null && !query.getRiskLevel().isBlank()
                && !query.getRiskLevel().equalsIgnoreCase(item.getRiskLevel())) {
            return false;
        }
        if (query.getOnlineStatus() != null && !Objects.equals(query.getOnlineStatus(), item.getOnlineStatus())) {
            return false;
        }
        if (query.getDeviceCode() != null && !query.getDeviceCode().isBlank()) {
            return item.getDeviceCode() != null && item.getDeviceCode().contains(query.getDeviceCode());
        }
        return true;
    }

    private PageResult<RiskMonitoringListItemVO> toPage(List<RiskMonitoringListItemVO> items, long pageNum, long pageSize) {
        if (items.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return PageResult.of((long) items.size(), pageNum, pageSize, items.subList(fromIndex, toIndex));
    }

    private List<AlarmRecord> listRecentAlarms(RiskPointDevice binding) {
        return alarmRecordMapper.selectList(new LambdaQueryWrapper<AlarmRecord>()
                .eq(AlarmRecord::getDeleted, 0)
                .eq(AlarmRecord::getDeviceId, binding.getDeviceId())
                .eq(AlarmRecord::getMetricName, binding.getMetricName())
                .orderByDesc(AlarmRecord::getCreateTime)
                .last("limit " + DETAIL_LIMIT));
    }

    private List<EventRecord> listRecentEvents(RiskPointDevice binding) {
        return eventRecordMapper.selectList(new LambdaQueryWrapper<EventRecord>()
                .eq(EventRecord::getDeleted, 0)
                .eq(EventRecord::getDeviceId, binding.getDeviceId())
                .eq(EventRecord::getMetricName, binding.getMetricName())
                .orderByDesc(EventRecord::getCreateTime)
                .last("limit " + DETAIL_LIMIT));
    }

    private List<RiskMonitoringTrendPointVO> buildTrend(RiskPointDevice binding, DeviceProperty property) {
        LocalDateTime start = LocalDateTime.now().minusHours(24);
        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, binding.getDeviceId())
                .ge(DeviceMessageLog::getReportTime, start)
                .orderByAsc(DeviceMessageLog::getReportTime)
                .last("limit " + TREND_LIMIT));

        List<RiskMonitoringTrendPointVO> points = logs.stream()
                .map(log -> extractTrendPoint(log, binding.getMetricIdentifier()))
                .filter(Objects::nonNull)
                .toList();

        if (!points.isEmpty()) {
            return points;
        }

        if (property == null || property.getPropertyValue() == null) {
            return Collections.emptyList();
        }

        RiskMonitoringTrendPointVO point = new RiskMonitoringTrendPointVO();
        point.setReportTime(property.getReportTime());
        point.setValue(property.getPropertyValue());
        point.setNumericValue(parseDouble(property.getPropertyValue()));
        return List.of(point);
    }

    private RiskMonitoringTrendPointVO extractTrendPoint(DeviceMessageLog log, String metricIdentifier) {
        String value = extractMetricValue(log.getPayload(), metricIdentifier);
        if (value == null) {
            return null;
        }
        RiskMonitoringTrendPointVO point = new RiskMonitoringTrendPointVO();
        point.setReportTime(log.getReportTime());
        point.setValue(value);
        point.setNumericValue(parseDouble(value));
        return point;
    }

    private String extractMetricValue(String payload, String metricIdentifier) {
        if (payload == null || payload.isBlank() || metricIdentifier == null || metricIdentifier.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> root = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            Object value = searchMetricValue(root, metricIdentifier);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object searchMetricValue(Map<String, Object> root, String metricIdentifier) {
        if (root.containsKey(metricIdentifier)) {
            return root.get(metricIdentifier);
        }
        Object properties = root.get("properties");
        if (properties instanceof Map<?, ?> propertyMap && propertyMap.containsKey(metricIdentifier)) {
            return propertyMap.get(metricIdentifier);
        }
        Object data = root.get("data");
        if (data instanceof Map<?, ?> dataMap && dataMap.containsKey(metricIdentifier)) {
            return dataMap.get(metricIdentifier);
        }
        for (Object value : root.values()) {
            if (value instanceof Map<?, ?> childMap) {
                Object nested = searchMetricValue((Map<String, Object>) childMap, metricIdentifier);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private RiskMonitoringAlarmSummaryVO toAlarmSummary(AlarmRecord alarm) {
        RiskMonitoringAlarmSummaryVO summary = new RiskMonitoringAlarmSummaryVO();
        summary.setId(alarm.getId());
        summary.setAlarmCode(alarm.getAlarmCode());
        summary.setAlarmTitle(alarm.getAlarmTitle());
        summary.setAlarmLevel(alarm.getAlarmLevel());
        summary.setStatus(alarm.getStatus());
        summary.setCurrentValue(alarm.getCurrentValue());
        summary.setThresholdValue(alarm.getThresholdValue());
        summary.setTriggerTime(alarm.getTriggerTime());
        return summary;
    }

    private RiskMonitoringEventSummaryVO toEventSummary(EventRecord event) {
        RiskMonitoringEventSummaryVO summary = new RiskMonitoringEventSummaryVO();
        summary.setId(event.getId());
        summary.setEventCode(event.getEventCode());
        summary.setEventTitle(event.getEventTitle());
        summary.setRiskLevel(event.getRiskLevel());
        summary.setStatus(event.getStatus());
        summary.setCurrentValue(event.getCurrentValue());
        summary.setTriggerTime(event.getTriggerTime());
        return summary;
    }

    private String resolveMonitorStatus(boolean activeAlarm, Device device, DeviceProperty property) {
        if (activeAlarm) {
            return "ALARM";
        }
        if (device == null || !Integer.valueOf(1).equals(device.getOnlineStatus())) {
            return "OFFLINE";
        }
        if (property == null || property.getPropertyValue() == null) {
            return "NO_DATA";
        }
        return "NORMAL";
    }

    private void fillCoordinates(RiskMonitoringGisPointVO point, List<RiskPointDevice> bindings, Map<Long, Device> deviceMap) {
        List<Device> devices = bindings.stream()
                .map(binding -> deviceMap.get(binding.getDeviceId()))
                .filter(Objects::nonNull)
                .filter(device -> device.getLongitude() != null && device.getLatitude() != null)
                .toList();
        if (devices.isEmpty()) {
            return;
        }

        double longitude = devices.stream()
                .map(Device::getLongitude)
                .mapToDouble(value -> value.doubleValue())
                .average()
                .orElse(0D);
        double latitude = devices.stream()
                .map(Device::getLatitude)
                .mapToDouble(value -> value.doubleValue())
                .average()
                .orElse(0D);
        point.setLongitude(longitude);
        point.setLatitude(latitude);
    }

    private String propertyKey(Long deviceId, String identifier) {
        return deviceId + "#" + identifier;
    }

    private String alarmKey(Long deviceId, String metricName) {
        return deviceId + "#" + metricName;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record MonitoringContext(
            Map<Long, RiskPoint> riskPointMap,
            Map<Long, Device> deviceMap,
            Map<Long, Product> productMap,
            Map<String, DeviceProperty> propertyMap,
            Map<String, Boolean> activeAlarmFlags) {
    }
}
