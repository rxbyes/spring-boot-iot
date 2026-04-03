package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingPromotionHistoryVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 风险点待治理候选推荐服务实现。
 */
@Service
public class RiskPointPendingRecommendationServiceImpl implements RiskPointPendingRecommendationService {

    private static final Set<String> PROMOTABLE_STATUSES = Set.of("PENDING_METRIC_GOVERNANCE", "PARTIALLY_PROMOTED");
    private static final Set<String> ROOT_WRAPPER_KEYS = Set.of("properties", "property", "status", "telemetry", "data", "params", "payload");
    private static final int MESSAGE_LOG_SCAN_LIMIT = 20;

    private final RiskPointPendingBindingService pendingBindingService;
    private final DeviceService deviceService;
    private final ProductModelMapper productModelMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final RiskPointDevicePendingPromotionMapper pendingPromotionMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskPointPendingRecommendationServiceImpl(RiskPointPendingBindingService pendingBindingService,
                                                     DeviceService deviceService,
                                                     ProductModelMapper productModelMapper,
                                                     DevicePropertyMapper devicePropertyMapper,
                                                     DeviceMessageLogMapper deviceMessageLogMapper,
                                                     RiskPointDevicePendingPromotionMapper pendingPromotionMapper) {
        this.pendingBindingService = pendingBindingService;
        this.deviceService = deviceService;
        this.productModelMapper = productModelMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.pendingPromotionMapper = pendingPromotionMapper;
    }

    @Override
    public RiskPointPendingCandidateBundleVO getCandidates(Long pendingId, Long currentUserId) {
        RiskPointDevicePendingBinding pending = pendingBindingService.getRequiredPending(pendingId, currentUserId);
        validatePendingForRecommendation(pending);
        Device device = deviceService.getRequiredById(pending.getDeviceId());
        LinkedHashMap<String, CandidateAccumulator> candidateMap = new LinkedHashMap<>();

        mergeProductModelEvidence(candidateMap, device);
        mergeLatestPropertyEvidence(candidateMap, device);
        mergeMessageLogEvidence(candidateMap, device);

        RiskPointPendingCandidateBundleVO bundle = toBundle(pending);
        bundle.setCandidates(candidateMap.values().stream()
                .map(this::toCandidate)
                .sorted(candidateComparator())
                .toList());
        bundle.setPromotionHistory(loadPromotionHistory(pending.getId()));
        return bundle;
    }

    private void validatePendingForRecommendation(RiskPointDevicePendingBinding pending) {
        if (!StringUtils.hasText(pending.getResolutionStatus())
                || !PROMOTABLE_STATUSES.contains(pending.getResolutionStatus().trim().toUpperCase(Locale.ROOT))) {
            throw new BizException("当前待治理状态不支持查看候选测点");
        }
        if (pending.getRiskPointId() == null) {
            throw new BizException("待治理记录缺少风险点");
        }
        if (pending.getDeviceId() == null) {
            throw new BizException("待治理记录缺少设备");
        }
    }

    private void mergeProductModelEvidence(Map<String, CandidateAccumulator> candidateMap, Device device) {
        List<ProductModel> productModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, device.getProductId())
                .eq(ProductModel::getModelType, "property")
                .orderByAsc(ProductModel::getSortNo)
                .orderByAsc(ProductModel::getIdentifier));
        for (ProductModel productModel : productModels) {
            if (!StringUtils.hasText(productModel.getIdentifier())) {
                continue;
            }
            CandidateAccumulator candidate = candidateMap.computeIfAbsent(productModel.getIdentifier(), CandidateAccumulator::new);
            candidate.addEvidenceSource("PRODUCT_MODEL");
            candidate.incrementSeenCount();
            candidate.applyModelName(productModel.getModelName());
            candidate.applyDataType(productModel.getDataType());
        }
    }

    private void mergeLatestPropertyEvidence(Map<String, CandidateAccumulator> candidateMap, Device device) {
        List<DeviceProperty> properties = devicePropertyMapper.selectList(new LambdaQueryWrapper<DeviceProperty>()
                .eq(DeviceProperty::getDeviceId, device.getId())
                .orderByDesc(DeviceProperty::getReportTime)
                .orderByAsc(DeviceProperty::getIdentifier));
        for (DeviceProperty property : properties) {
            if (!StringUtils.hasText(property.getIdentifier())) {
                continue;
            }
            CandidateAccumulator candidate = candidateMap.computeIfAbsent(property.getIdentifier(), CandidateAccumulator::new);
            candidate.addEvidenceSource("LATEST_PROPERTY");
            candidate.incrementSeenCount();
            candidate.applyPropertyName(property.getPropertyName());
            candidate.applyDataType(property.getValueType());
            candidate.updateLastSeenTime(property.getReportTime());
            candidate.applySampleValue(property.getPropertyValue(), property.getReportTime());
        }
    }

    private void mergeMessageLogEvidence(Map<String, CandidateAccumulator> candidateMap, Device device) {
        List<DeviceMessageLog> logs = deviceMessageLogMapper.selectList(new LambdaQueryWrapper<DeviceMessageLog>()
                .eq(DeviceMessageLog::getDeviceId, device.getId())
                .orderByDesc(DeviceMessageLog::getReportTime)
                .last("limit " + MESSAGE_LOG_SCAN_LIMIT));
        for (DeviceMessageLog log : logs) {
            Map<String, JsonNode> fields = extractPayloadFields(log.getPayload());
            for (Map.Entry<String, JsonNode> entry : fields.entrySet()) {
                String identifier = entry.getKey();
                JsonNode value = entry.getValue();
                if (!StringUtils.hasText(identifier) || value == null || !value.isValueNode()) {
                    continue;
                }
                CandidateAccumulator candidate = candidateMap.computeIfAbsent(identifier, CandidateAccumulator::new);
                candidate.addEvidenceSource("MESSAGE_LOG");
                candidate.incrementSeenCount();
                candidate.updateLastSeenTime(log.getReportTime());
                candidate.applySampleValue(resolveSampleValue(value), log.getReportTime());
                candidate.applyDataType(resolveJsonDataType(value));
            }
        }
    }

    private Map<String, JsonNode> extractPayloadFields(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(JsonPayloadUtils.normalizeJsonDocument(payload));
            if (root == null || !root.isObject()) {
                return Map.of();
            }
            JsonNode content = unwrapRootNode(root);
            if (!(content instanceof ObjectNode objectNode)) {
                return Map.of();
            }
            LinkedHashMap<String, JsonNode> fields = new LinkedHashMap<>();
            objectNode.properties().forEach(entry -> fields.put(entry.getKey(), entry.getValue()));
            return fields;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private JsonNode unwrapRootNode(JsonNode root) {
        for (String key : ROOT_WRAPPER_KEYS) {
            JsonNode child = root.get(key);
            if (child != null && child.isObject()) {
                return child;
            }
        }
        return root;
    }

    private List<RiskPointPendingPromotionHistoryVO> loadPromotionHistory(Long pendingId) {
        return pendingPromotionMapper.selectList(new LambdaQueryWrapper<RiskPointDevicePendingPromotion>()
                        .eq(RiskPointDevicePendingPromotion::getDeleted, 0)
                        .eq(RiskPointDevicePendingPromotion::getPendingBindingId, pendingId)
                        .orderByDesc(RiskPointDevicePendingPromotion::getCreateTime))
                .stream()
                .map(this::toHistory)
                .toList();
    }

    private RiskPointPendingCandidateBundleVO toBundle(RiskPointDevicePendingBinding pending) {
        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setPendingId(pending.getId());
        bundle.setBatchNo(pending.getBatchNo());
        bundle.setRiskPointId(pending.getRiskPointId());
        bundle.setRiskPointCode(pending.getRiskPointCode());
        bundle.setRiskPointName(pending.getRiskPointName());
        bundle.setDeviceId(pending.getDeviceId());
        bundle.setDeviceCode(pending.getDeviceCode());
        bundle.setDeviceName(pending.getDeviceName());
        bundle.setResolutionStatus(pending.getResolutionStatus());
        bundle.setResolutionNote(pending.getResolutionNote());
        bundle.setMetricIdentifier(pending.getMetricIdentifier());
        bundle.setMetricName(pending.getMetricName());
        bundle.setCreateTime(pending.getCreateTime());
        return bundle;
    }

    private RiskPointPendingPromotionHistoryVO toHistory(RiskPointDevicePendingPromotion promotion) {
        RiskPointPendingPromotionHistoryVO history = new RiskPointPendingPromotionHistoryVO();
        history.setId(promotion.getId());
        history.setPendingBindingId(promotion.getPendingBindingId());
        history.setRiskPointDeviceId(promotion.getRiskPointDeviceId());
        history.setMetricIdentifier(promotion.getMetricIdentifier());
        history.setMetricName(promotion.getMetricName());
        history.setPromotionStatus(promotion.getPromotionStatus());
        history.setRecommendationLevel(promotion.getRecommendationLevel());
        history.setRecommendationScore(promotion.getRecommendationScore());
        history.setPromotionNote(promotion.getPromotionNote());
        history.setOperatorId(promotion.getOperatorId());
        history.setOperatorName(promotion.getOperatorName());
        history.setCreateTime(promotion.getCreateTime());
        return history;
    }

    private RiskPointPendingMetricCandidateVO toCandidate(CandidateAccumulator candidate) {
        RiskPointPendingMetricCandidateVO item = new RiskPointPendingMetricCandidateVO();
        item.setMetricIdentifier(candidate.metricIdentifier);
        item.setMetricName(candidate.resolveMetricName());
        item.setDataType(candidate.dataType);
        item.setEvidenceSources(new ArrayList<>(candidate.evidenceSources));
        item.setLastSeenTime(candidate.lastSeenTime);
        item.setSampleValue(candidate.sampleValue);
        item.setSeenCount(candidate.seenCount);
        int score = calculateScore(candidate);
        item.setRecommendationScore(score);
        item.setRecommendationLevel(resolveRecommendationLevel(score));
        item.setReasonSummary(buildReasonSummary(candidate, score));
        return item;
    }

    private int calculateScore(CandidateAccumulator candidate) {
        int score = 0;
        if (candidate.hasEvidence("PRODUCT_MODEL")) {
            score += 50;
        }
        if (candidate.hasEvidence("LATEST_PROPERTY")) {
            score += 30;
        }
        if (candidate.hasEvidence("MESSAGE_LOG")) {
            score += 15;
        }
        score += Math.min(Math.max(candidate.seenCount - 1, 0) * 5, 10);
        return Math.min(score, 100);
    }

    private String resolveRecommendationLevel(int score) {
        if (score >= 80) {
            return "HIGH";
        }
        if (score >= 45) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildReasonSummary(CandidateAccumulator candidate, int score) {
        List<String> reasons = new ArrayList<>();
        if (candidate.hasEvidence("PRODUCT_MODEL")) {
            reasons.add("已存在物模型定义");
        }
        if (candidate.hasEvidence("LATEST_PROPERTY")) {
            reasons.add("设备最新属性存在同名测点");
        }
        if (candidate.hasEvidence("MESSAGE_LOG")) {
            reasons.add("最近报文出现 " + candidate.messageLogSeenCount + " 次");
        }
        reasons.add("推荐分 " + score);
        return String.join("；", reasons);
    }

    private Comparator<RiskPointPendingMetricCandidateVO> candidateComparator() {
        return Comparator.comparingInt((RiskPointPendingMetricCandidateVO item) -> recommendationWeight(item.getRecommendationLevel())).reversed()
                .thenComparing(RiskPointPendingMetricCandidateVO::getLastSeenTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RiskPointPendingMetricCandidateVO::getSeenCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RiskPointPendingMetricCandidateVO::getMetricIdentifier, Comparator.nullsLast(String::compareTo));
    }

    private int recommendationWeight(String level) {
        if ("HIGH".equalsIgnoreCase(level)) {
            return 3;
        }
        if ("MEDIUM".equalsIgnoreCase(level)) {
            return 2;
        }
        return 1;
    }

    private String resolveJsonDataType(JsonNode value) {
        if (value == null) {
            return null;
        }
        if (value.isIntegralNumber()) {
            return "long";
        }
        if (value.isFloatingPointNumber()) {
            return "double";
        }
        if (value.isBoolean()) {
            return "boolean";
        }
        if (value.isTextual()) {
            return "string";
        }
        return "json";
    }

    private String resolveSampleValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isTextual()) {
            return value.asText();
        }
        return value.toString();
    }

    private static final class CandidateAccumulator {
        private final String metricIdentifier;
        private final LinkedHashSet<String> evidenceSources = new LinkedHashSet<>();
        private String modelName;
        private String propertyName;
        private String dataType;
        private LocalDateTime lastSeenTime;
        private String sampleValue;
        private LocalDateTime sampleValueTime;
        private int seenCount;
        private int messageLogSeenCount;

        private CandidateAccumulator(String metricIdentifier) {
            this.metricIdentifier = metricIdentifier;
        }

        private void addEvidenceSource(String evidenceSource) {
            evidenceSources.add(evidenceSource);
            if ("MESSAGE_LOG".equals(evidenceSource)) {
                messageLogSeenCount++;
            }
        }

        private boolean hasEvidence(String evidenceSource) {
            return evidenceSources.contains(evidenceSource);
        }

        private void incrementSeenCount() {
            seenCount++;
        }

        private void applyModelName(String value) {
            if (StringUtils.hasText(value)) {
                this.modelName = value.trim();
            }
        }

        private void applyPropertyName(String value) {
            if (StringUtils.hasText(value)) {
                this.propertyName = value.trim();
            }
        }

        private void applyDataType(String value) {
            if (!StringUtils.hasText(this.dataType) && StringUtils.hasText(value)) {
                this.dataType = value.trim();
            }
        }

        private void updateLastSeenTime(LocalDateTime candidateTime) {
            if (candidateTime != null && (lastSeenTime == null || candidateTime.isAfter(lastSeenTime))) {
                lastSeenTime = candidateTime;
            }
        }

        private void applySampleValue(String value, LocalDateTime candidateTime) {
            if (!StringUtils.hasText(value)) {
                return;
            }
            if (sampleValueTime == null || (candidateTime != null && sampleValueTime != null && candidateTime.isAfter(sampleValueTime))) {
                sampleValue = value;
                sampleValueTime = candidateTime;
                return;
            }
            if (sampleValueTime == null) {
                sampleValue = value;
            }
        }

        private String resolveMetricName() {
            if (StringUtils.hasText(propertyName)) {
                return propertyName;
            }
            if (StringUtils.hasText(modelName)) {
                return modelName;
            }
            return metricIdentifier;
        }
    }
}
