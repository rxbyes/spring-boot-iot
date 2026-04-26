package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
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
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final int BAD_REQUEST_CODE = 400;
    private static final Set<String> PROMOTABLE_STATUSES = Set.of("PENDING_METRIC_GOVERNANCE", "PARTIALLY_PROMOTED");
    private static final Set<String> PROPERTY_LOG_TYPES = Set.of("property", "status");
    private static final Set<String> ROOT_WRAPPER_KEYS = Set.of("properties", "property", "status", "telemetry", "data", "params", "payload");
    private static final Set<String> IGNORED_ROOT_KEYS = Set.of(
            "messagetype",
            "devicecode",
            "productkey",
            "traceid",
            "protocolcode",
            "timestamp",
            "time",
            "reporttime",
            "reported",
            "sessionid",
            "tenantid",
            "deviceid",
            "riskpointid",
            "batchno",
            "operatorid",
            "operatorname",
            "header",
            "headers",
            "bodies",
            "encoding",
            "rawtext",
            "jsoncandidate",
            "payloadbase64"
    );
    private static final int MESSAGE_LOG_SCAN_LIMIT = 20;

    private final RiskPointPendingBindingService pendingBindingService;
    private final DeviceService deviceService;
    private final ProductModelMapper productModelMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final RiskPointDevicePendingPromotionMapper pendingPromotionMapper;
    private final RiskMetricCatalogService riskMetricCatalogService;
    private final RiskMetricCatalogPublishRule riskMetricCatalogPublishRule;
    private ProductMapper productMapper;
    private final RiskPointPendingMetricGovernanceRules metricGovernanceRules = new RiskPointPendingMetricGovernanceRules();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskPointPendingRecommendationServiceImpl(RiskPointPendingBindingService pendingBindingService,
                                                     DeviceService deviceService,
                                                     ProductModelMapper productModelMapper,
                                                     DevicePropertyMapper devicePropertyMapper,
                                                     DeviceMessageLogMapper deviceMessageLogMapper,
                                                     RiskPointDevicePendingPromotionMapper pendingPromotionMapper) {
        this(
                pendingBindingService,
                deviceService,
                productModelMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                pendingPromotionMapper,
                null,
                null
        );
    }

    public RiskPointPendingRecommendationServiceImpl(RiskPointPendingBindingService pendingBindingService,
                                                     DeviceService deviceService,
                                                     ProductModelMapper productModelMapper,
                                                     DevicePropertyMapper devicePropertyMapper,
                                                     DeviceMessageLogMapper deviceMessageLogMapper,
                                                     RiskPointDevicePendingPromotionMapper pendingPromotionMapper,
                                                     RiskMetricCatalogService riskMetricCatalogService) {
        this(
                pendingBindingService,
                deviceService,
                productModelMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                pendingPromotionMapper,
                riskMetricCatalogService,
                null
        );
    }

    @Autowired
    public RiskPointPendingRecommendationServiceImpl(RiskPointPendingBindingService pendingBindingService,
                                                     DeviceService deviceService,
                                                     ProductModelMapper productModelMapper,
                                                     DevicePropertyMapper devicePropertyMapper,
                                                     DeviceMessageLogMapper deviceMessageLogMapper,
                                                     RiskPointDevicePendingPromotionMapper pendingPromotionMapper,
                                                     RiskMetricCatalogService riskMetricCatalogService,
                                                     RiskMetricCatalogPublishRule riskMetricCatalogPublishRule) {
        this.pendingBindingService = pendingBindingService;
        this.deviceService = deviceService;
        this.productModelMapper = productModelMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.pendingPromotionMapper = pendingPromotionMapper;
        this.riskMetricCatalogService = riskMetricCatalogService;
        this.riskMetricCatalogPublishRule = riskMetricCatalogPublishRule == null
                ? new DefaultRiskMetricCatalogPublishRule()
                : riskMetricCatalogPublishRule;
    }

    @Autowired(required = false)
    void setProductMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public RiskPointPendingCandidateBundleVO getCandidates(Long pendingId, Long currentUserId) {
        RiskPointDevicePendingBinding pending = pendingBindingService.getRequiredPending(pendingId, currentUserId);
        validatePendingForRecommendation(pending);
        Device device = deviceService.getRequiredById(pending.getDeviceId());
        LinkedHashMap<String, CandidateAccumulator> candidateMap = new LinkedHashMap<>();
        List<ProductModel> releasedProductModels = listReleasedProductModels(device);
        publishRiskMetricCatalog(device, releasedProductModels);

        mergeProductModelEvidence(candidateMap, releasedProductModels);
        mergeLatestPropertyEvidence(candidateMap, device);
        mergeMessageLogEvidence(candidateMap, device);

        RiskPointPendingCandidateBundleVO bundle = toBundle(pending);
        List<RiskPointPendingMetricCandidateVO> rawCandidates = candidateMap.values().stream()
                .map(this::toCandidate)
                .toList();
        Set<String> recommendedIdentifiers = device == null || device.getProductId() == null
                ? Set.of()
                : new LinkedHashSet<>(listRecommendedMetricIdentifiers(device.getProductId()));
        bundle.setCandidates(decorateRiskMetricCatalog(device, metricGovernanceRules.governCandidates(pending, device, rawCandidates)).stream()
                .map(candidate -> applyCatalogRecommendation(candidate, recommendedIdentifiers))
                .sorted(candidateComparator())
                .toList());
        bundle.setPromotionHistory(loadPromotionHistory(pending.getId()));
        return bundle;
    }

    @Override
    public List<String> listRecommendedMetricIdentifiers(Long productId) {
        if (riskMetricCatalogService == null || productId == null) {
            return List.of();
        }
        return riskMetricCatalogService.listRiskBindingRecommendedIdentifiers(productId);
    }

    private void validatePendingForRecommendation(RiskPointDevicePendingBinding pending) {
        if (!StringUtils.hasText(pending.getResolutionStatus())
                || !PROMOTABLE_STATUSES.contains(pending.getResolutionStatus().trim().toUpperCase(Locale.ROOT))) {
            throw new BizException(BAD_REQUEST_CODE, "当前待治理状态不支持查看候选测点");
        }
        if (pending.getRiskPointId() == null) {
            throw new BizException(BAD_REQUEST_CODE, "待治理记录缺少风险点");
        }
        if (pending.getDeviceId() == null) {
            throw new BizException(BAD_REQUEST_CODE, "待治理记录缺少设备");
        }
    }

    private List<ProductModel> listReleasedProductModels(Device device) {
        if (device == null || device.getProductId() == null) {
            return List.of();
        }
        List<ProductModel> productModels = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getProductId, device.getProductId())
                .eq(ProductModel::getModelType, "property")
                .orderByAsc(ProductModel::getSortNo)
                .orderByAsc(ProductModel::getIdentifier));
        return productModels == null ? List.of() : productModels;
    }

    private void publishRiskMetricCatalog(Device device, List<ProductModel> productModels) {
        if (riskMetricCatalogService == null || device == null || device.getProductId() == null || productModels == null || productModels.isEmpty()) {
            return;
        }
        Product product = productMapper == null ? null : productMapper.selectById(device.getProductId());
        Set<String> riskEnabledIdentifiers = riskMetricCatalogPublishRule.resolveRiskEnabledIdentifiers(
                product,
                null,
                device,
                productModels
        );
        if (riskEnabledIdentifiers.isEmpty()) {
            return;
        }
        riskMetricCatalogService.publishFromReleasedContracts(device.getProductId(), null, productModels, riskEnabledIdentifiers);
    }

    private void mergeProductModelEvidence(Map<String, CandidateAccumulator> candidateMap, List<ProductModel> productModels) {
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
            if (!isPropertyLog(log)) {
                continue;
            }
            Map<String, String> fields = extractPropertyLeaves(log.getPayload());
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                String identifier = entry.getKey();
                String value = entry.getValue();
                if (!StringUtils.hasText(identifier) || !StringUtils.hasText(value)) {
                    continue;
                }
                CandidateAccumulator candidate = candidateMap.computeIfAbsent(identifier, CandidateAccumulator::new);
                candidate.addEvidenceSource("MESSAGE_LOG");
                candidate.incrementSeenCount();
                candidate.updateLastSeenTime(log.getReportTime());
                candidate.applySampleValue(value, log.getReportTime());
                candidate.applyDataType(resolveValueDataType(value));
            }
        }
    }

    private Map<String, String> extractPropertyLeaves(String payload) {
        String normalizedPayload = normalizeOptional(JsonPayloadUtils.normalizeJsonDocument(payload));
        if (normalizedPayload == null) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(normalizedPayload);
            Map<String, String> extracted = new LinkedHashMap<>();
            collectLeafValues(root, "", extracted, true);
            return extracted;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private void collectLeafValues(JsonNode node, String prefix, Map<String, String> extracted, boolean rootLevel) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            if (StringUtils.hasText(prefix)) {
                extracted.put(prefix, node.asText());
            }
            return;
        }
        if (node.isArray()) {
            return;
        }
        if (!(node instanceof ObjectNode objectNode)) {
            return;
        }
        objectNode.properties().forEach(entry -> {
            String fieldName = normalizeOptional(entry.getKey());
            if (fieldName == null) {
                return;
            }
            String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT);
            if (rootLevel && IGNORED_ROOT_KEYS.contains(normalizedFieldName)) {
                return;
            }
            boolean unwrapRoot = rootLevel && ROOT_WRAPPER_KEYS.contains(normalizedFieldName);
            String nextPrefix = unwrapRoot ? "" : appendIdentifier(prefix, fieldName);
            collectLeafValues(entry.getValue(), nextPrefix, extracted, false);
        });
    }

    private boolean isPropertyLog(DeviceMessageLog log) {
        String messageType = normalizeOptional(log == null ? null : log.getMessageType());
        return messageType != null && PROPERTY_LOG_TYPES.contains(messageType.toLowerCase(Locale.ROOT));
    }

    private List<RiskPointPendingMetricCandidateVO> decorateRiskMetricCatalog(Device device,
                                                                              List<RiskPointPendingMetricCandidateVO> candidates) {
        if (riskMetricCatalogService == null || device == null || device.getProductId() == null || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }
        List<RiskMetricCatalog> catalogs = riskMetricCatalogService.listEnabledByProduct(device.getProductId());
        if (catalogs == null || catalogs.isEmpty()) {
            return candidates;
        }
        Map<String, RiskMetricCatalog> catalogByIdentifier = new LinkedHashMap<>();
        for (RiskMetricCatalog catalog : catalogs) {
            if (catalog != null && StringUtils.hasText(catalog.getContractIdentifier())) {
                catalogByIdentifier.put(catalog.getContractIdentifier(), catalog);
            }
        }
        for (RiskPointPendingMetricCandidateVO candidate : candidates) {
            if (candidate == null || !StringUtils.hasText(candidate.getMetricIdentifier())) {
                continue;
            }
            RiskMetricCatalog catalog = catalogByIdentifier.get(candidate.getMetricIdentifier());
            if (catalog == null) {
                continue;
            }
            candidate.setRiskMetricId(catalog.getId());
            if (StringUtils.hasText(catalog.getRiskMetricName())) {
                candidate.setMetricName(catalog.getRiskMetricName());
            }
        }
        return candidates;
    }

    private RiskPointPendingMetricCandidateVO applyCatalogRecommendation(RiskPointPendingMetricCandidateVO candidate,
                                                                        Set<String> recommendedIdentifiers) {
        if (candidate == null || recommendedIdentifiers == null || recommendedIdentifiers.isEmpty()
                || !StringUtils.hasText(candidate.getMetricIdentifier())) {
            return candidate;
        }
        if (!recommendedIdentifiers.contains(candidate.getMetricIdentifier())) {
            return candidate;
        }
        candidate.setCatalogRecommended(true);
        if (candidate.getRecommendationScore() == null || candidate.getRecommendationScore() < 95) {
            candidate.setRecommendationScore(95);
        }
        candidate.setRecommendationLevel("HIGH");
        String prefix = "已命中正式风险目录，建议优先绑定";
        if (!StringUtils.hasText(candidate.getReasonSummary())) {
            candidate.setReasonSummary(prefix);
            return candidate;
        }
        if (!candidate.getReasonSummary().contains(prefix)) {
            candidate.setReasonSummary(prefix + "；" + candidate.getReasonSummary());
        }
        return candidate;
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
        RecommendationDecision decision = decideRecommendation(candidate);
        item.setRecommendationScore(decision.score());
        item.setRecommendationLevel(decision.level());
        item.setReasonSummary(decision.reasonSummary());
        return item;
    }

    private RecommendationDecision decideRecommendation(CandidateAccumulator candidate) {
        boolean modelBacked = candidate.hasEvidence("PRODUCT_MODEL");
        boolean latestBacked = candidate.hasEvidence("LATEST_PROPERTY");
        boolean logBacked = candidate.hasEvidence("MESSAGE_LOG");
        int runtimeEvidenceCount = candidate.runtimeEvidenceCount();

        if ((modelBacked && runtimeEvidenceCount > 0)
                || (!modelBacked && candidate.hasStrongRepeatedRuntimeEvidence())) {
            int score = modelBacked ? 92 : 84;
            String reason = modelBacked
                    ? "物模型与真实上报同时命中，且运行证据稳定"
                    : "最近真实上报重复出现，字段标识稳定";
            return new RecommendationDecision("HIGH", score, reason + "；运行证据 " + runtimeEvidenceCount + " 次");
        }
        if ((latestBacked || modelBacked) || candidate.hasModerateRuntimeEvidence()) {
            int score = modelBacked && runtimeEvidenceCount == 0 ? 56 : 68;
            String reason;
            if (latestBacked || candidate.hasModerateRuntimeEvidence()) {
                reason = "已命中真实上报，但证据强度仍需人工确认";
            } else {
                reason = "当前仅命中物模型，运行证据较弱";
            }
            return new RecommendationDecision("MEDIUM", score, reason + "；运行证据 " + runtimeEvidenceCount + " 次");
        }
        int score = logBacked ? 24 : 20;
        String reason = logBacked
                ? "仅在历史日志中偶发出现，默认不建议优先选择"
                : "证据较弱，建议继续观察后再确认";
        return new RecommendationDecision("LOW", score, reason + "；日志证据 " + candidate.messageLogSeenCount + " 次");
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

    private String resolveValueDataType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
            return "boolean";
        }
        if (normalized.matches("^-?\\d+$")) {
            return "long";
        }
        if (normalized.matches("^-?\\d+\\.\\d+$")) {
            return "double";
        }
        return "string";
    }

    private String appendIdentifier(String prefix, String fieldName) {
        return !StringUtils.hasText(prefix) ? fieldName : prefix + "." + fieldName;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

        private int runtimeEvidenceCount() {
            int runtimeCount = 0;
            if (hasEvidence("LATEST_PROPERTY")) {
                runtimeCount++;
            }
            runtimeCount += messageLogSeenCount;
            return runtimeCount;
        }

        private boolean hasStrongRepeatedRuntimeEvidence() {
            return !hasEvidence("PRODUCT_MODEL")
                    && runtimeEvidenceCount() >= 4
                    && (hasEvidence("LATEST_PROPERTY") || messageLogSeenCount >= 3);
        }

        private boolean hasModerateRuntimeEvidence() {
            return !hasStrongRepeatedRuntimeEvidence()
                    && (hasEvidence("LATEST_PROPERTY") || messageLogSeenCount >= 2);
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

    private record RecommendationDecision(String level, int score, String reasonSummary) {
    }
}
