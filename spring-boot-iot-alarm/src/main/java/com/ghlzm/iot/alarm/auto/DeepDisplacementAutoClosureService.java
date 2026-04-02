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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
import java.util.concurrent.TimeUnit;
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
    private static final long AUTO_CLOSURE_LOCK_LEASE_SECONDS = 15L;
    private static final List<String> EMERGENCY_PLAN_SCENE_KEYWORDS = List.of(
            "深部位移", "深层位移", "位移监测", "位移", "滑坡", "边坡", "地灾", "地质灾害", "变形监测"
    );
    private static final List<String> EMERGENCY_PLAN_SECONDARY_KEYWORDS = List.of(
            "滑动", "坡面", "累计变形", "变形", "dispsx", "dispsy"
    );

    private final AlarmRecordService alarmRecordService;
    private final EventRecordService eventRecordService;
    private final RiskPointMapper riskPointMapper;
    private final RiskPointDeviceMapper riskPointDeviceMapper;
    private final LinkageRuleMapper linkageRuleMapper;
    private final EmergencyPlanMapper emergencyPlanMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final IotProperties iotProperties;
    private final RiskPolicyResolver riskPolicyResolver;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public DeepDisplacementAutoClosureService(AlarmRecordService alarmRecordService,
                                              EventRecordService eventRecordService,
                                              RiskPointMapper riskPointMapper,
                                              RiskPointDeviceMapper riskPointDeviceMapper,
                                              LinkageRuleMapper linkageRuleMapper,
                                              EmergencyPlanMapper emergencyPlanMapper,
                                              DevicePropertyMapper devicePropertyMapper,
                                              IotProperties iotProperties,
                                              RiskPolicyResolver riskPolicyResolver,
                                              RedissonClient redissonClient) {
        this.alarmRecordService = alarmRecordService;
        this.eventRecordService = eventRecordService;
        this.riskPointMapper = riskPointMapper;
        this.riskPointDeviceMapper = riskPointDeviceMapper;
        this.linkageRuleMapper = linkageRuleMapper;
        this.emergencyPlanMapper = emergencyPlanMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.iotProperties = iotProperties;
        this.riskPolicyResolver = riskPolicyResolver;
        this.redissonClient = redissonClient;
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
        RiskPolicyDecision decision = riskPolicyResolver.resolve(resolveTenantId(event.getTenantId(), riskPoint.getTenantId()), binding, absoluteValue);
        if (!decision.shouldCreateAlarm()) {
            return;
        }
        DuplicateLockHandle duplicateLock = tryAcquireDuplicateLock(event.getDeviceCode(), binding.getMetricIdentifier(), decision.getAlarmLevel(), event.getTraceId());
        if (!duplicateLock.acquired()) {
            log.debug("深部位移自动闭环并发锁已被占用, deviceCode={}, metricIdentifier={}, level={}, traceId={}",
                    event.getDeviceCode(), binding.getMetricIdentifier(), decision.getAlarmLevel(), event.getTraceId());
            return;
        }
        try {
            if (existsDuplicateAlarm(event.getDeviceCode(), binding.getMetricIdentifier(), decision.getAlarmLevel())) {
                log.debug("深部位移自动闭环命中冷却窗口, deviceCode={}, metricIdentifier={}, level={}, traceId={}",
                        event.getDeviceCode(), binding.getMetricIdentifier(), decision.getAlarmLevel(), event.getTraceId());
                return;
            }

            List<Map<String, Object>> matchedLinkageRules = matchLinkageRules(event.getTenantId(), binding.getMetricIdentifier(), absoluteValue);
            EmergencyPlan matchedPlan = matchEmergencyPlan(event.getTenantId(), event, riskPoint, binding, decision.getSeverity());

            AlarmRecord alarmRecord = buildAlarmRecord(event, riskPoint, binding, absoluteValue, decision, matchedLinkageRules, matchedPlan);
            alarmRecordService.addAlarm(alarmRecord);

            if (!decision.shouldCreateEvent()) {
                return;
            }

            Long dispatchUser = resolveDispatchUser(riskPoint);
            EventRecord eventRecord = buildEventRecord(event, riskPoint, binding, absoluteValue, decision, matchedLinkageRules, matchedPlan,
                    alarmRecord.getId(), alarmRecord.getAlarmCode(), dispatchUser);
            eventRecordService.addEvent(eventRecord);
            eventRecordService.dispatchEvent(eventRecord.getId(), dispatchUser, dispatchUser);
        } finally {
            releaseDuplicateLock(duplicateLock.lock());
        }
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

    private DuplicateLockHandle tryAcquireDuplicateLock(String deviceCode,
                                                        String metricIdentifier,
                                                        String alarmLevel,
                                                        String traceId) {
        if (redissonClient == null) {
            return DuplicateLockHandle.proceedWithoutLock();
        }
        String lockKey = "iot:alarm:auto-closure:" + defaultString(deviceCode) + ":" + defaultString(metricIdentifier) + ":" + defaultString(alarmLevel);
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = lock.tryLock(0, AUTO_CLOSURE_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                return DuplicateLockHandle.skipped();
            }
            return DuplicateLockHandle.acquired(lock);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("深部位移自动闭环获取并发锁被中断, deviceCode={}, metricIdentifier={}, level={}, traceId={}",
                    deviceCode, metricIdentifier, alarmLevel, traceId);
            return DuplicateLockHandle.skipped();
        } catch (Exception ex) {
            log.warn("深部位移自动闭环获取并发锁失败，回退为无锁模式, deviceCode={}, metricIdentifier={}, level={}, traceId={}",
                    deviceCode, metricIdentifier, alarmLevel, traceId, ex);
            return DuplicateLockHandle.proceedWithoutLock();
        }
    }

    private void releaseDuplicateLock(RLock lock) {
        if (lock == null) {
            return;
        }
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception ex) {
            log.warn("深部位移自动闭环释放并发锁失败", ex);
        }
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

    private EmergencyPlan matchEmergencyPlan(Long tenantId,
                                             DeviceRiskEvaluationEvent event,
                                             RiskPoint riskPoint,
                                             RiskPointDevice binding,
                                             AutoClosureSeverity severity) {
        List<EmergencyPlan> candidates = emergencyPlanMapper.selectList(
                new LambdaQueryWrapper<EmergencyPlan>()
                        .eq(EmergencyPlan::getDeleted, 0)
                        .eq(EmergencyPlan::getStatus, 0)
                        .eq(tenantId != null, EmergencyPlan::getTenantId, tenantId)
                        .orderByDesc(EmergencyPlan::getCreateTime)
        );
        Set<String> contextKeywords = buildEmergencyPlanContextKeywords(event, riskPoint, binding);
        Optional<EmergencyPlanMatch> exact = selectScopedEmergencyPlan(candidates.stream()
                .filter(plan -> severity.getRiskPointLevel().equalsIgnoreCase(normalizeLevel(plan.getRiskLevel())))
                .toList(), contextKeywords);
        if (exact.isPresent()) {
            logMatchedEmergencyPlan(event, riskPoint, binding, severity, exact.get());
            return exact.get().plan();
        }
        String fallbackLevel = fallbackPlanLevel(severity);
        EmergencyPlanMatch matchedPlan = selectScopedEmergencyPlan(candidates.stream()
                .filter(plan -> fallbackLevel.equalsIgnoreCase(normalizeLevel(plan.getRiskLevel())))
                .toList(), contextKeywords)
                .orElse(null);
        if (matchedPlan == null) {
            log.info("深部位移自动闭环未命中场景化应急预案, deviceCode={}, riskPointCode={}, metricIdentifier={}, severity={}, traceId={}",
                    event.getDeviceCode(), riskPoint.getRiskPointCode(), binding.getMetricIdentifier(), severity.name(), event.getTraceId());
            logEmergencyPlanCandidates(event, riskPoint, binding, severity, candidates, contextKeywords);
            return null;
        }
        logMatchedEmergencyPlan(event, riskPoint, binding, severity, matchedPlan);
        return matchedPlan.plan();
    }

    private String fallbackPlanLevel(AutoClosureSeverity severity) {
        return switch (severity) {
            case RED -> "red";
            case ORANGE, YELLOW -> "orange";
            case BLUE -> "blue";
        };
    }

    private Optional<EmergencyPlanMatch> selectScopedEmergencyPlan(List<EmergencyPlan> candidates, Set<String> contextKeywords) {
        return candidates.stream()
                .map(plan -> evaluateEmergencyPlan(plan, contextKeywords))
                .filter(EmergencyPlanMatch::eligible)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .findFirst();
    }

    private int scoreEmergencyPlan(EmergencyPlan plan, Set<String> contextKeywords) {
        return evaluateEmergencyPlan(plan, contextKeywords).score();
    }

    private EmergencyPlanMatch evaluateEmergencyPlan(EmergencyPlan plan, Set<String> contextKeywords) {
        if (plan == null) {
            return EmergencyPlanMatch.empty(null);
        }
        String planName = normalizeSceneText(plan.getPlanName());
        String searchableText = normalizeSceneText(String.join(" ",
                defaultString(plan.getPlanName()),
                defaultString(plan.getDescription()),
                defaultString(plan.getResponseSteps()),
                defaultString(plan.getContactList())
        ));
        if (!StringUtils.hasText(searchableText)) {
            return EmergencyPlanMatch.empty(plan);
        }
        List<String> sceneMatches = collectMatchedKeywords(planName, searchableText, EMERGENCY_PLAN_SCENE_KEYWORDS);
        List<String> secondaryMatches = collectMatchedKeywords(planName, searchableText, EMERGENCY_PLAN_SECONDARY_KEYWORDS);
        boolean sceneQualified = !sceneMatches.isEmpty() || !secondaryMatches.isEmpty();
        if (!sceneQualified) {
            return new EmergencyPlanMatch(plan, 0, false, sceneMatches, secondaryMatches, List.of());
        }
        List<String> contextMatches = collectMatchedKeywords(planName, searchableText, contextKeywords);
        int score = 0;
        score += scoreKeywordMatches(planName, EMERGENCY_PLAN_SCENE_KEYWORDS, 120);
        score += scoreKeywordMatches(searchableText, EMERGENCY_PLAN_SCENE_KEYWORDS, 60);
        score += scoreKeywordMatches(planName, EMERGENCY_PLAN_SECONDARY_KEYWORDS, 40);
        score += scoreKeywordMatches(searchableText, EMERGENCY_PLAN_SECONDARY_KEYWORDS, 20);
        score += scoreKeywordMatches(planName, contextKeywords, 80);
        score += scoreKeywordMatches(searchableText, contextKeywords, 30);
        return new EmergencyPlanMatch(plan, score, true, sceneMatches, secondaryMatches, contextMatches);
    }

    private int scoreKeywordMatches(String content, Iterable<String> keywords, int scorePerMatch) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        int score = 0;
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && content.contains(keyword.toLowerCase(Locale.ROOT))) {
                score += scorePerMatch;
            }
        }
        return score;
    }

    private List<String> collectMatchedKeywords(String primaryContent,
                                                String secondaryContent,
                                                Iterable<String> keywords) {
        if (keywords == null) {
            return List.of();
        }
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            String normalized = keyword.toLowerCase(Locale.ROOT);
            if ((StringUtils.hasText(primaryContent) && primaryContent.contains(normalized))
                    || (StringUtils.hasText(secondaryContent) && secondaryContent.contains(normalized))) {
                matched.add(keyword);
            }
        }
        return matched;
    }

    private void logMatchedEmergencyPlan(DeviceRiskEvaluationEvent event,
                                         RiskPoint riskPoint,
                                         RiskPointDevice binding,
                                         AutoClosureSeverity severity,
                                         EmergencyPlanMatch match) {
        if (!log.isInfoEnabled() || match == null || match.plan() == null) {
            return;
        }
        log.info("深部位移自动闭环命中场景化应急预案, deviceCode={}, riskPointCode={}, metricIdentifier={}, severity={}, planId={}, planName={}, score={}, sceneKeywords={}, secondaryKeywords={}, contextKeywords={}, traceId={}",
                event == null ? null : event.getDeviceCode(),
                riskPoint == null ? null : riskPoint.getRiskPointCode(),
                binding == null ? null : binding.getMetricIdentifier(),
                severity == null ? null : severity.name(),
                match.plan().getId(),
                match.plan().getPlanName(),
                match.score(),
                match.sceneKeywords(),
                match.secondaryKeywords(),
                match.contextKeywords(),
                event == null ? null : event.getTraceId());
    }

    private void logEmergencyPlanCandidates(DeviceRiskEvaluationEvent event,
                                            RiskPoint riskPoint,
                                            RiskPointDevice binding,
                                            AutoClosureSeverity severity,
                                            List<EmergencyPlan> candidates,
                                            Set<String> contextKeywords) {
        if (!log.isDebugEnabled() || candidates == null || candidates.isEmpty()) {
            return;
        }
        List<String> scoredPlans = candidates.stream()
                .map(plan -> evaluateEmergencyPlan(plan, contextKeywords))
                .filter(match -> match != null && match.plan() != null)
                .sorted((left, right) -> Integer.compare(right.score(), left.score()))
                .map(match -> "id=" + match.plan().getId()
                        + ",name=" + match.plan().getPlanName()
                        + ",level=" + match.plan().getRiskLevel()
                        + ",eligible=" + match.eligible()
                        + ",score=" + match.score()
                        + ",scene=" + match.sceneKeywords()
                        + ",secondary=" + match.secondaryKeywords()
                        + ",context=" + match.contextKeywords())
                .limit(5)
                .toList();
        log.debug("深部位移自动闭环预案候选打分, deviceCode={}, riskPointCode={}, metricIdentifier={}, severity={}, contextKeywords={}, topCandidates={}, traceId={}",
                event == null ? null : event.getDeviceCode(),
                riskPoint == null ? null : riskPoint.getRiskPointCode(),
                binding == null ? null : binding.getMetricIdentifier(),
                severity == null ? null : severity.name(),
                contextKeywords,
                scoredPlans,
                event == null ? null : event.getTraceId());
    }

    private Set<String> buildEmergencyPlanContextKeywords(DeviceRiskEvaluationEvent event,
                                                          RiskPoint riskPoint,
                                                          RiskPointDevice binding) {
        Set<String> keywords = new LinkedHashSet<>();
        addEmergencyPlanContextKeyword(keywords, riskPoint == null ? null : riskPoint.getRiskPointCode());
        addEmergencyPlanContextKeyword(keywords, riskPoint == null ? null : riskPoint.getRiskPointName());
        addEmergencyPlanContextKeyword(keywords, binding == null ? null : binding.getMetricIdentifier());
        addEmergencyPlanContextKeyword(keywords, binding == null ? null : binding.getMetricName());
        addEmergencyPlanContextKeyword(keywords, event == null ? null : event.getProductKey());
        if (binding != null && hasAnyKeyword(binding.getMetricIdentifier(), "dispsx", "dispsy")) {
            keywords.add("位移");
            keywords.add("变形");
            keywords.add("滑动");
            keywords.add("坡面");
        }
        if (binding != null && hasAnyKeyword(binding.getMetricName(), "滑动")) {
            keywords.add("滑动");
        }
        if (binding != null && hasAnyKeyword(binding.getMetricName(), "坡面")) {
            keywords.add("坡面");
        }
        if (binding != null && hasAnyKeyword(binding.getMetricName(), "变形")) {
            keywords.add("变形");
        }
        if (riskPoint != null && hasAnyKeyword(riskPoint.getRiskPointName(), "边坡", "滑坡")) {
            keywords.add("边坡");
            keywords.add("滑坡");
        }
        if (event != null && hasAnyKeyword(event.getProductKey(), "deep", "displacement", "slope", "landslide")) {
            keywords.add("深部位移");
            keywords.add("位移");
        }
        return keywords;
    }

    private void addEmergencyPlanContextKeyword(Set<String> keywords, String rawKeyword) {
        String normalized = normalizeSceneText(rawKeyword);
        if (StringUtils.hasText(normalized)) {
            keywords.add(normalized);
        }
    }

    private boolean hasAnyKeyword(String content, String... keywords) {
        String normalized = normalizeSceneText(content);
        if (!StringUtils.hasText(normalized) || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeSceneText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
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
        RiskPolicyDecision highest = allBindings.stream()
                .map(binding -> resolveLatestDecision(binding, riskPoint.getTenantId()))
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(RiskPolicyDecision::getPriority))
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

    private RiskPolicyDecision resolveLatestDecision(RiskPointDevice binding, Long tenantId) {
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
        return riskPolicyResolver.resolve(tenantId, binding, value.abs());
    }

    private AlarmRecord buildAlarmRecord(DeviceRiskEvaluationEvent event,
                                         RiskPoint riskPoint,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         RiskPolicyDecision decision,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan) {
        AlarmRecord alarmRecord = new AlarmRecord();
        alarmRecord.setTenantId(resolveTenantId(event.getTenantId(), riskPoint.getTenantId()));
        alarmRecord.setAlarmCode(generateCode("ALARM"));
        alarmRecord.setAlarmTitle(buildAlarmTitle(riskPoint, binding, decision.getSeverity()));
        alarmRecord.setAlarmType("threshold");
        alarmRecord.setAlarmLevel(decision.getAlarmLevel());
        alarmRecord.setRegionId(riskPoint.getRegionId());
        alarmRecord.setRegionName(riskPoint.getRegionName());
        alarmRecord.setRiskPointId(riskPoint.getId());
        alarmRecord.setRiskPointName(riskPoint.getRiskPointName());
        alarmRecord.setDeviceId(event.getDeviceId());
        alarmRecord.setDeviceCode(event.getDeviceCode());
        alarmRecord.setDeviceName(event.getDeviceName());
        alarmRecord.setMetricName(resolveMetricName(binding));
        alarmRecord.setCurrentValue(toPlainString(absoluteValue));
        alarmRecord.setThresholdValue(decision.getThresholdText());
        alarmRecord.setTriggerTime(formatDateTime(event.getReportedAt()));
        alarmRecord.setRuleId(decision.getRuleId());
        alarmRecord.setRuleName(StringUtils.hasText(decision.getRuleName()) ? decision.getRuleName() : AUTO_RULE_NAME);
        alarmRecord.setRemark(buildAlarmRemark(event, binding, absoluteValue, decision, matchedLinkageRules, matchedPlan));
        return alarmRecord;
    }

    private EventRecord buildEventRecord(DeviceRiskEvaluationEvent event,
                                         RiskPoint riskPoint,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         RiskPolicyDecision decision,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan,
                                         Long alarmId,
                                         String alarmCode,
                                         Long dispatchUser) {
        EventRecord eventRecord = new EventRecord();
        eventRecord.setTenantId(resolveTenantId(event.getTenantId(), riskPoint.getTenantId()));
        eventRecord.setEventCode(generateCode("EVENT"));
        eventRecord.setEventTitle(buildEventTitle(riskPoint, binding, decision.getSeverity()));
        eventRecord.setAlarmId(alarmId);
        eventRecord.setAlarmCode(alarmCode);
        eventRecord.setAlarmLevel(decision.getAlarmLevel());
        eventRecord.setRiskLevel(decision.getRiskPointLevel());
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
        eventRecord.setUrgencyLevel(decision.getAlarmLevel());
        eventRecord.setTriggerTime(formatDateTime(event.getReportedAt()));
        eventRecord.setReviewNotes(buildEventReviewNotes(event, binding, absoluteValue, decision, matchedLinkageRules, matchedPlan));
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

    private String buildAlarmRemark(DeviceRiskEvaluationEvent event,
                                    RiskPointDevice binding,
                                    BigDecimal absoluteValue,
                                    RiskPolicyDecision decision,
                                    List<Map<String, Object>> matchedLinkageRules,
                                    EmergencyPlan matchedPlan) {
        Map<String, Object> remark = new LinkedHashMap<>();
        remark.put("source", AUTO_SOURCE);
        remark.put("policySource", decision.getSource());
        remark.put("traceId", event.getTraceId());
        remark.put("metricIdentifier", binding.getMetricIdentifier());
        remark.put("color", decision.getColorCode());
        remark.put("absValue", toPlainString(absoluteValue));
        remark.put("threshold", decision.getThresholdText());
        remark.put("linkageRuleIds", matchedLinkageRules.stream().map(item -> item.get("id")).toList());
        remark.put("planId", matchedPlan == null ? null : matchedPlan.getId());
        return writeJson(remark);
    }

    private String buildEventReviewNotes(DeviceRiskEvaluationEvent event,
                                         RiskPointDevice binding,
                                         BigDecimal absoluteValue,
                                         RiskPolicyDecision decision,
                                         List<Map<String, Object>> matchedLinkageRules,
                                         EmergencyPlan matchedPlan) {
        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("source", AUTO_SOURCE);
        notes.put("policySource", decision.getSource());
        notes.put("traceId", event.getTraceId());
        notes.put("metricIdentifier", binding.getMetricIdentifier());
        notes.put("metricName", resolveMetricName(binding));
        notes.put("color", decision.getColorCode());
        notes.put("alarmLevel", decision.getAlarmLevel());
        notes.put("absValue", toPlainString(absoluteValue));
        notes.put("threshold", decision.getThresholdText());
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
        if (!StringUtils.hasText(level)) {
            return "";
        }
        return switch (level.trim().toLowerCase(Locale.ROOT)) {
            case "critical" -> "red";
            case "warning" -> "orange";
            case "info" -> "blue";
            default -> level.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
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

    private record EmergencyPlanMatch(EmergencyPlan plan,
                                      int score,
                                      boolean eligible,
                                      List<String> sceneKeywords,
                                      List<String> secondaryKeywords,
                                      List<String> contextKeywords) {
        private static EmergencyPlanMatch empty(EmergencyPlan plan) {
            return new EmergencyPlanMatch(plan, 0, false, List.of(), List.of(), List.of());
        }
    }

    private record DuplicateLockHandle(RLock lock, boolean acquired) {
        private static DuplicateLockHandle acquired(RLock lock) {
            return new DuplicateLockHandle(lock, true);
        }

        private static DuplicateLockHandle proceedWithoutLock() {
            return new DuplicateLockHandle(null, true);
        }

        private static DuplicateLockHandle skipped() {
            return new DuplicateLockHandle(null, false);
        }
    }
}
