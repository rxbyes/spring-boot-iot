package com.ghlzm.iot.alarm.auto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.AlarmRecord;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.EventRecord;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.service.AlarmRecordService;
import com.ghlzm.iot.alarm.service.EventRecordService;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 深部位移平台内自动闭环服务。
 */
@Service
public class DeepDisplacementAutoClosureService {

    private static final Logger log = LoggerFactory.getLogger(DeepDisplacementAutoClosureService.class);
    private static final Long SYSTEM_USER_ID = 1L;
    private static final int RISK_POINT_STATUS_DISABLED = 1;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String AUTO_RULE_NAME = "deep-displacement-auto-closure";
    private static final String AUTO_SOURCE = "deep-displacement-auto-closure";

    private final AlarmRecordService alarmRecordService;
    private final EventRecordService eventRecordService;
    private final RiskPointMapper riskPointMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final LinkageRuleMapper linkageRuleMapper;
    private final EmergencyPlanMapper emergencyPlanMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final IotProperties iotProperties;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeepDisplacementAutoClosureService(AlarmRecordService alarmRecordService,
                                              EventRecordService eventRecordService,
                                              RiskPointMapper riskPointMapper,
                                              RiskPointDeviceMapper riskPointDeviceMapper,
                                              LinkageRuleMapper linkageRuleMapper,
                                              EmergencyPlanMapper emergencyPlanMapper,
                                              DevicePropertyMapper devicePropertyMapper,
                                              IotProperties iotProperties) {
        this.alarmRecordService = alarmRecordService;
        this.eventRecordService = eventRecordService;
        this.riskPointMapper = riskPointMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.linkageRuleMapper = linkageRuleMapper;
        this.emergencyPlanMapper = emergencyPlanMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.iotProperties = iotProperties;
    }

    @Transactional(rollbackFor = Exception.class)
    public void process(DeviceRiskEvaluationEvent event) {
        if (!isEnabled() || event == null || event.getDeviceId() == null || event.getProperties() == null || event.getProperties().isEmpty()) {
            return;
        }

        List<RiskPointDevice> bindings = riskPointDeviceMapper.selectList(
                new LambdaQueryWrapper<RiskPointDevice>()
                        .eq(RiskPointDevice::getDeviceId, event.getDeviceId())
                        .eq(RiskPointDevice::getDeleted, 0)
        );
        if (bindings.isEmpty()) {
            return;
        }

        Map<Long, RiskPoint> riskPointMap = loadRiskPoints(bindings);
        if (riskPointMap.isEmpty()) {
            return;
        }

        Set<Long> touchedRiskPointIds = new LinkedHashSet<>();
        for (RiskPointDevice binding : bindings) {
            if (!event.getProperties().containsKey(binding.getMetricIdentifier())) {
                continue;
            }
            RiskPoint riskPoint = riskPointMap.get(binding.getRiskPointId());
            if (riskPoint == null) {
                continue;
            }
            BigDecimal currentValue = parseNumeric(event.getProperties().get(binding.getMetricIdentifier()));
            if (currentValue == null) {
                log.warn("深部位移自动闭环跳过非数值测点, deviceCode={}, metricIdentifier={}, traceId={}",
                        event.getDeviceCode(), binding.getMetricIdentifier(), event.getTraceId());
                continue;
            }
            touchedRiskPointIds.add(riskPoint.getId());
            handleBinding(event, riskPoint, binding, currentValue.abs());
        }

        for (Long riskPointId : touchedRiskPointIds) {
            refreshRiskPointLevel(riskPointId);
        }
    }

    private boolean isEnabled() {
        return iotProperties != null
                && iotProperties.getAlarm() != null
                && iotProperties.getAlarm().getAutoClosure() != null
                && Boolean.TRUE.equals(iotProperties.getAlarm().getAutoClosure().getEnabled());
    }

    private Map<Long, RiskPoint> loadRiskPoints(List<RiskPointDevice> bindings) {
        Set<Long> riskPointIds = bindings.stream()
                .map(RiskPointDevice::getRiskPointId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (riskPointIds.isEmpty()) {
            return Map.of();
        }
        List<RiskPoint> riskPoints = riskPointMapper.selectList(
                new LambdaQueryWrapper<RiskPoint>()
                        .in(RiskPoint::getId, riskPointIds)
                        .eq(RiskPoint::getDeleted, 0)
                        .ne(RiskPoint::getStatus, RISK_POINT_STATUS_DISABLED)
        );
        return riskPoints.stream().collect(Collectors.toMap(RiskPoint::getId, item -> item, (left, right) -> left));
    }

    private void handleBinding(DeviceRiskEvaluationEvent event,
                               RiskPoint riskPoint,
                               RiskPointDevice binding,
                               BigDecimal absoluteValue) {
        AutoClosureSeverity severity = AutoClosureSeverity.classify(absoluteValue, iotProperties.getAlarm().getAutoClosure());
        if (!severity.shouldCreateAlarm()) {
            return;
        }
        if (existsDuplicateAlarm(event.getDeviceCode(), binding.getMetricIdentifier(), severity.getAlarmLevel())) {
            log.debug("深部位移自动闭环命中冷却窗口, deviceCode={}, metricIdentifier={}, level={}, traceId={}",
                    event.getDeviceCode(), binding.getMetricIdentifier(), severity.getAlarmLevel(), event.getTraceId());
            return;
        }

        List<Map<String, Object>> matchedLinkageRules = matchLinkageRules(event.getTenantId(), binding.getMetricIdentifier(), absoluteValue);
        EmergencyPlan matchedPlan = matchEmergencyPlan(event.getTenantId(), severity);

        AlarmRecord alarmRecord = buildAlarmRecord(event, riskPoint, binding, absoluteValue, severity, matchedLinkageRules, matchedPlan);
        alarmRecordService.addAlarm(alarmRecord);

        if (!severity.shouldCreateEvent()) {
            return;
        }

        Long dispatchUser = resolveDispatchUser(riskPoint);
        EventRecord eventRecord = buildEventRecord(event, riskPoint, binding, absoluteValue, severity, matchedLinkageRules, matchedPlan,
                alarmRecord.getId(), alarmRecord.getAlarmCode(), dispatchUser);
        eventRecordService.addEvent(eventRecord);
        eventRecordService.dispatchEvent(eventRecord.getId(), dispatchUser, dispatchUser);
    }

    private boolean existsDuplicateAlarm(String deviceCode, String metricIdentifier, String alarmLevel) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(getCooldownMinutes());
        AlarmRecord duplicate = alarmRecordService.getOne(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDeviceCode, deviceCode)
                        .eq(AlarmRecord::getAlarmLevel, alarmLevel)
                        .eq(AlarmRecord::getDeleted, 0)
                        .ge(AlarmRecord::getTriggerTime, formatDateTime(cutoff))
                        .like(AlarmRecord::getRemark, "\"metricIdentifier\":\"" + metricIdentifier + "\"")
                        .last("limit 1")
        );
        return duplicate != null;
    }

    private int getCooldownMinutes() {
        Integer configured = iotProperties == null || iotProperties.getAlarm() == null || iotProperties.getAlarm().getAutoClosure() == null
                ? null
                : iotProperties.getAlarm().getAutoClosure().getCooldownMinutes();
        return configured == null || configured < 0 ? 30 : configured;
    }

    private List<Map<String, Object>> matchLinkageRules(Long tenantId, String metricIdentifier, BigDecimal absoluteValue) {
        List<LinkageRule> candidates = linkageRuleMapper.selectList(
                new LambdaQueryWrapper<LinkageRule>()
                        .eq(LinkageRule::getDeleted, 0)
                        .eq(LinkageRule::getStatus, 0)
                        .eq(tenantId != null, LinkageRule::getTenantId, tenantId)
                        .orderByDesc(LinkageRule::getCreateTime)
        );
        List<Map<String, Object>> matched = new ArrayList<>();
        for (LinkageRule rule : candidates) {
            if (matchesTriggerCondition(rule.getTriggerCondition(), metricIdentifier, absoluteValue)) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("id", rule.getId());
                summary.put("name", rule.getRuleName());
                matched.add(summary);
            }
        }
        return matched;
    }

    private boolean matchesTriggerCondition(String triggerCondition, String metricIdentifier, BigDecimal absoluteValue) {
        if (!StringUtils.hasText(triggerCondition)) {
            return false;
        }
        try {
            Object parsed = objectMapper.readValue(triggerCondition, Object.class);
            return matchesConditionObject(parsed, metricIdentifier, absoluteValue);
        } catch (Exception ex) {
            log.warn("联动规则触发条件解析失败, metricIdentifier={}, triggerCondition={}", metricIdentifier, triggerCondition, ex);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean matchesConditionObject(Object parsed, String metricIdentifier, BigDecimal absoluteValue) {
        if (parsed instanceof List<?> list) {
            for (Object item : list) {
                if (matchesConditionObject(item, metricIdentifier, absoluteValue)) {
                    return true;
                }
            }
            return false;
        }
        if (!(parsed instanceof Map<?, ?> rawMap)) {
            return false;
        }
        Map<String, Object> condition = (Map<String, Object>) rawMap;
        Object nested = condition.get("conditions");
        if (nested != null) {
            return matchesConditionObject(nested, metricIdentifier, absoluteValue);
        }
        String metric = firstNonBlank(condition.get("metric"), condition.get("metricIdentifier"), condition.get("identifier"));
        if (StringUtils.hasText(metric) && !metricIdentifier.equalsIgnoreCase(metric.trim())) {
            return false;
        }
        String op = firstNonBlank(condition.get("op"), condition.get("operator"), condition.get("expression"));
        BigDecimal threshold = parseNumeric(firstNonNull(condition.get("threshold"), condition.get("value"), condition.get("target")));
        if (!StringUtils.hasText(op) || threshold == null) {
            return false;
        }
        return compare(absoluteValue, threshold, op.trim());
    }

    private EmergencyPlan matchEmergencyPlan(Long tenantId, AutoClosureSeverity severity) {
        List<EmergencyPlan> candidates = emergencyPlanMapper.selectList(
                new LambdaQueryWrapper<EmergencyPlan>()
                        .eq(EmergencyPlan::getDeleted, 0)
                        .eq(EmergencyPlan::getStatus, 0)
                        .eq(tenantId != null, EmergencyPlan::getTenantId, tenantId)
                        .orderByDesc(EmergencyPlan::getCreateTime)
        );
        Optional<EmergencyPlan> exact = candidates.stream()
                .filter(plan -> severity.getAlarmLevel().equalsIgnoreCase(normalizeLevel(plan.getRiskLevel())))
                .findFirst();
        if (exact.isPresent()) {
            return exact.get();
        }
        String fallbackLevel = fallbackPlanLevel(severity);
        return candidates.stream()
                .filter(plan -> fallbackLevel.equalsIgnoreCase(normalizeLevel(plan.getRiskLevel())))
                .findFirst()
                .orElse(null);
    }

    private String fallbackPlanLevel(AutoClosureSeverity severity) {
        return switch (severity) {
            case RED -> "critical";
            case ORANGE, YELLOW -> "warning";
            case BLUE -> "info";
        };
    }

    private void refreshRiskPointLevel(Long riskPointId) {
        RiskPoint riskPoint = riskPointMapper.selectById(riskPointId);
        if (riskPoint == null || Integer.valueOf(1).equals(riskPoint.getDeleted()) || Integer.valueOf(RISK_POINT_STATUS_DISABLED).equals(riskPoint.getStatus())) {
            return;
        }
        List<RiskPointDevice> allBindings = riskPointDeviceMapper.selectList(
                new LambdaQueryWrapper<RiskPointDevice>()
                        .eq(RiskPointDevice::getRiskPointId, riskPointId)
                        .eq(RiskPointDevice::getDeleted, 0)
        );
        AutoClosureSeverity highest = allBindings.stream()
                .map(this::resolveLatestSeverity)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(AutoClosureSeverity::getPriority))
                .orElse(null);
        if (highest == null) {
            return;
        }
        if (highest.getRiskPointLevel().equalsIgnoreCase(normalizeLevel(riskPoint.getRiskLevel()))) {
            return;
        }
        RiskPoint update = new RiskPoint();
        update.setId(riskPointId);
        update.setRiskLevel(highest.getRiskPointLevel());
        update.setUpdateTime(new java.util.Date());
        riskPointMapper.updateById(update);
    }

    private AutoClosureSeverity resolveLatestSeverity(RiskPointDevice binding) {
        DeviceProperty property = devicePropertyMapper.selectOne(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, binding.getDeviceId())
                        .eq(DeviceProperty::getIdentifier, binding.getMetricIdentifier())
                        .orderByDesc(DeviceProperty::getReportTime)
                        .last("limit 1")
        );
        if (property == null || !StringUtils.hasText(property.getPropertyValue())) {
            return null;
        }
        BigDecimal value = parseNumeric(property.getPropertyValue());
        if (value == null) {
            return null;
        }
        return AutoClosureSeverity.classify(value.abs(), iotProperties.getAlarm().getAutoClosure());
    }

    private AlarmRecord buildAlarmRecord(DeviceRiskEvaluationEvent event,
                                         RiskPoint riskPoint,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         AutoClosureSeverity severity,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan) {
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setTenantId(resolveTenantId(event.getTenantId(), riskPoint.getTenantId()));
        alarmRecord.setAlarmCode(generateCode("ALARM"));
        alarmRecord.setAlarmTitle(buildAlarmTitle(riskPoint, binding, severity));
        alarmRecord.setAlarmType("threshold");
        alarmRecord.setAlarmLevel(severity.getAlarmLevel());
        alarmRecord.setRegionId(riskPoint.getRegionId());
        alarmRecord.setRegionName(riskPoint.getRegionName());
        alarmRecord.setRiskPointId(riskPoint.getId());
        alarmRecord.setRiskPointName(riskPoint.getRiskPointName());
        alarmRecord.setDeviceId(event.getDeviceId());
        alarmRecord.setDeviceCode(event.getDeviceCode());
        alarmRecord.setDeviceName(event.getDeviceName());
        alarmRecord.setMetricName(resolveMetricName(binding));
        alarmRecord.setCurrentValue(toPlainString(absoluteValue));
        alarmRecord.setThresholdValue(buildThresholdText(severity));
        alarmRecord.setTriggerTime(formatDateTime(event.getReportedAt()));
        alarmRecord.setRuleName(AUTO_RULE_NAME);
        alarmRecord.setRemark(buildAlarmRemark(event, binding, absoluteValue, severity, matchedLinkageRules, matchedPlan));
        return alarmRecord;
    }

    private EventRecord buildEventRecord(DeviceRiskEvaluationEvent event,
                                         RiskPoint riskPoint,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         AutoClosureSeverity severity,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan,
                                         Long alarmId,
                                         String alarmCode,
                                         Long dispatchUser) {
        EventRecord eventRecord = new EventRecord();
        eventRecord.setTenantId(resolveTenantId(event.getTenantId(), riskPoint.getTenantId()));
        eventRecord.setEventCode(generateCode("EVENT"));
        eventRecord.setEventTitle(buildEventTitle(riskPoint, binding, severity));
        eventRecord.setAlarmId(alarmId);
        eventRecord.setAlarmCode(alarmCode);
        eventRecord.setAlarmLevel(severity.getAlarmLevel());
        eventRecord.setRiskLevel(severity.getAlarmLevel());
        eventRecord.setRegionId(riskPoint.getRegionId());
        eventRecord.setRegionName(riskPoint.getRegionName());
        eventRecord.setRiskPointId(riskPoint.getId());
        eventRecord.setRiskPointName(riskPoint.getRiskPointName());
        eventRecord.setDeviceId(event.getDeviceId());
        eventRecord.setDeviceCode(event.getDeviceCode());
        eventRecord.setDeviceName(event.getDeviceName());
        eventRecord.setMetricName(resolveMetricName(binding));
        eventRecord.setCurrentValue(toPlainString(absoluteValue));
        eventRecord.setResponsibleUser(dispatchUser);
        eventRecord.setUrgencyLevel(severity.getAlarmLevel());
        eventRecord.setTriggerTime(formatDateTime(event.getReportedAt()));
        eventRecord.setReviewNotes(buildEventReviewNotes(event, binding, absoluteValue, severity, matchedLinkageRules, matchedPlan));
        return eventRecord;
    }

    private String buildAlarmTitle(RiskPoint riskPoint, RiskPointDevice binding, AutoClosureSeverity severity) {
        return riskPoint.getRiskPointName() + "-" + resolveMetricName(binding) + "深部位移" + severity.getColorLabel() + "色告警";
    }

    private String buildEventTitle(RiskPoint riskPoint, RiskPointDevice binding, AutoClosureSeverity severity) {
        return riskPoint.getRiskPointName() + "-" + resolveMetricName(binding) + "深部位移" + severity.getColorLabel() + "色事件";
    }

    private String resolveMetricName(RiskPointDevice binding) {
        return StringUtils.hasText(binding.getMetricName()) ? binding.getMetricName() : binding.getMetricIdentifier();
    }

    private String buildThresholdText(AutoClosureSeverity severity) {
        IotProperties.Alarm.AutoClosure config = iotProperties.getAlarm().getAutoClosure();
        return switch (severity) {
            case YELLOW -> "[" + toPlainString(config.getYellow()) + ", " + toPlainString(config.getOrange()) + ") mm";
            case ORANGE -> "[" + toPlainString(config.getOrange()) + ", " + toPlainString(config.getRed()) + ") mm";
            case RED -> ">= " + toPlainString(config.getRed()) + " mm";
            case BLUE -> "< " + toPlainString(config.getYellow()) + " mm";
        };
    }

    private String buildAlarmRemark(DeviceRiskEvaluationEvent event,
                                    RiskPointDevice binding,
                                    BigDecimal absoluteValue,
                                    AutoClosureSeverity severity,
                                    List<Map<String, Object>> matchedLinkageRules,
                                    EmergencyPlan matchedPlan) {
        Map<String, Object> remark = new LinkedHashMap<>();
        remark.put("source", AUTO_SOURCE);
        remark.put("traceId", event.getTraceId());
        remark.put("metricIdentifier", binding.getMetricIdentifier());
        remark.put("color", severity.getColorCode());
        remark.put("absValue", toPlainString(absoluteValue));
        remark.put("linkageRuleIds", matchedLinkageRules.stream().map(item -> item.get("id")).toList());
        remark.put("planId", matchedPlan == null ? null : matchedPlan.getId());
        return writeJson(remark);
    }

    private String buildEventReviewNotes(DeviceRiskEvaluationEvent event,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         AutoClosureSeverity severity,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan) {
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("source", AUTO_SOURCE);
        notes.put("traceId", event.getTraceId());
        notes.put("metricIdentifier", binding.getMetricIdentifier());
        notes.put("metricName", resolveMetricName(binding));
        notes.put("color", severity.getColorCode());
        notes.put("alarmLevel", severity.getAlarmLevel());
        notes.put("absValue", toPlainString(absoluteValue));
        notes.put("threshold", buildThresholdText(severity));
        notes.put("linkageRules", matchedLinkageRules);
        notes.put("emergencyPlan", matchedPlan == null ? null : Map.of(
                "id", matchedPlan.getId(),
                "name", matchedPlan.getPlanName(),
                "riskLevel", matchedPlan.getRiskLevel()
        ));
        return writeJson(notes);
    }

    private String writeJson(Map<String, Object> content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (Exception ex) {
            log.warn("深部位移自动闭环留痕序列化失败", ex);
            return "{\"source\":\"" + AUTO_SOURCE + "\"}";
        }
    }

    private Long resolveDispatchUser(RiskPoint riskPoint) {
        return riskPoint.getResponsibleUser() == null ? SYSTEM_USER_ID : riskPoint.getResponsibleUser();
    }

    private boolean compare(BigDecimal currentValue, BigDecimal threshold, String rawOperator) {
        String operator = rawOperator.toLowerCase(Locale.ROOT);
        return switch (operator) {
            case ">", "gt" -> currentValue.compareTo(threshold) > 0;
            case ">=", "gte" -> currentValue.compareTo(threshold) >= 0;
            case "<", "lt" -> currentValue.compareTo(threshold) < 0;
            case "<=", "lte" -> currentValue.compareTo(threshold) <= 0;
            case "=", "==", "eq" -> currentValue.compareTo(threshold) == 0;
            default -> false;
        };
    }

    private Long resolveTenantId(Long eventTenantId, Long riskPointTenantId) {
        return riskPointTenantId != null ? riskPointTenantId : eventTenantId;
    }

    private String generateCode(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        return prefix + "-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String formatDateTime(LocalDateTime value) {
        return (value == null ? LocalDateTime.now() : value).format(DATETIME_FORMATTER);
    }

    private String normalizeLevel(String level) {
        return level == null ? "" : level.trim();
    }

    private Object firstNonNull(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private String firstNonBlank(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String text = String.valueOf(candidate).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private BigDecimal parseNumeric(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
