package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.RiskGovernanceOpsService;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayGapSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRaisedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 风险治理运维能力实现。
 */
@Service
public class RiskGovernanceOpsServiceImpl implements RiskGovernanceOpsService {

    private static final String ALERT_TYPE_FIELD_DRIFT = "FIELD_DRIFT";
    private static final String ALERT_TYPE_CONTRACT_DIFF = "CONTRACT_DIFF";
    private static final String ALERT_TYPE_MISSING_RISK_METRIC = "MISSING_RISK_METRIC";
    private static final String ALERT_SUBJECT_TYPE_PRODUCT = "PRODUCT";
    private static final String ALERT_SEVERITY_WARN = "WARN";
    private static final String ALERT_SOURCE_STAGE = "RISK_GOVERNANCE_OPS";
    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final VendorMetricEvidenceMapper vendorMetricEvidenceMapper;
    private final ProductModelMapper productModelMapper;
    private final RiskMetricCatalogMapper riskMetricCatalogMapper;
    private final ProductMapper productMapper;
    private final ProductContractReleaseBatchMapper productContractReleaseBatchMapper;
    private final DeviceMessageService deviceMessageService;
    private final DeviceAccessErrorLogService deviceAccessErrorLogService;
    private final RiskGovernanceService riskGovernanceService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RiskGovernanceOpsServiceImpl(VendorMetricEvidenceMapper vendorMetricEvidenceMapper,
                                        ProductModelMapper productModelMapper,
                                        RiskMetricCatalogMapper riskMetricCatalogMapper,
                                        ProductMapper productMapper,
                                        ProductContractReleaseBatchMapper productContractReleaseBatchMapper,
                                        DeviceMessageService deviceMessageService,
                                        DeviceAccessErrorLogService deviceAccessErrorLogService,
                                        RiskGovernanceService riskGovernanceService,
                                        ApplicationEventPublisher applicationEventPublisher) {
        this.vendorMetricEvidenceMapper = vendorMetricEvidenceMapper;
        this.productModelMapper = productModelMapper;
        this.riskMetricCatalogMapper = riskMetricCatalogMapper;
        this.productMapper = productMapper;
        this.productContractReleaseBatchMapper = productContractReleaseBatchMapper;
        this.deviceMessageService = deviceMessageService;
        this.deviceAccessErrorLogService = deviceAccessErrorLogService;
        this.riskGovernanceService = riskGovernanceService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public PageResult<RiskGovernanceOpsAlertItemVO> pageOpsAlerts(Long productId,
                                                                   String alertType,
                                                                   Long pageNum,
                                                                   Long pageSize) {
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .eq(productId != null, Product::getId, productId));
        if (products == null || products.isEmpty()) {
            return PageResult.empty(normalizePageNum(pageNum), normalizePageSize(pageSize));
        }
        Set<Long> productIds = products.stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (productIds.isEmpty()) {
            return PageResult.empty(normalizePageNum(pageNum), normalizePageSize(pageSize));
        }
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, value -> value, (left, right) -> left));
        Map<Long, Set<String>> contractIdentifiersByProduct = buildContractIdentifierMap(productIds);
        Map<Long, Set<String>> riskMetricIdentifiersByProduct = buildRiskMetricIdentifierMap(productIds);
        List<VendorMetricEvidence> evidences = vendorMetricEvidenceMapper.selectList(new LambdaQueryWrapper<VendorMetricEvidence>()
                .eq(VendorMetricEvidence::getDeleted, 0)
                .in(VendorMetricEvidence::getProductId, productIds));
        List<RiskGovernanceOpsAlertItemVO> alerts = collectOpsAlerts(
                productMap,
                contractIdentifiersByProduct,
                riskMetricIdentifiersByProduct,
                evidences
        );
        publishRaisedAlertEvents(alerts);
        String normalizedAlertType = normalize(alertType);
        if (StringUtils.hasText(normalizedAlertType)) {
            alerts = alerts.stream()
                    .filter(item -> normalizedAlertType.equalsIgnoreCase(item.getAlertType()))
                    .toList();
        }
        alerts = alerts.stream()
                .sorted((left, right) -> {
                    long leftCount = left.getAffectedCount() == null ? 0L : left.getAffectedCount();
                    long rightCount = right.getAffectedCount() == null ? 0L : right.getAffectedCount();
                    int byCount = Long.compare(rightCount, leftCount);
                    if (byCount != 0) {
                        return byCount;
                    }
                    int byProduct = Long.compare(
                            left.getProductId() == null ? 0L : left.getProductId(),
                            right.getProductId() == null ? 0L : right.getProductId()
                    );
                    if (byProduct != 0) {
                        return byProduct;
                    }
                    return String.valueOf(left.getAlertType()).compareTo(String.valueOf(right.getAlertType()));
                })
                .toList();
        return toPage(alerts, normalizePageNum(pageNum), normalizePageSize(pageSize));
    }

    @Override
    public RiskGovernanceReplayVO replay(Long currentUserId,
                                         String traceId,
                                         String deviceCode,
                                         String productKey,
                                         Long releaseBatchId) {
        ReplayContext replayContext = resolveReplayContext(releaseBatchId, productKey);
        String replayProductKey = replayContext.productKey();
        if (!StringUtils.hasText(traceId)
                && !StringUtils.hasText(deviceCode)
                && !StringUtils.hasText(replayProductKey)) {
            throw new BizException("请至少提供 traceId、deviceCode、productKey、releaseBatchId 之一");
        }
        DeviceMessageTraceQuery traceQuery = new DeviceMessageTraceQuery();
        traceQuery.setTraceId(normalize(traceId));
        traceQuery.setDeviceCode(normalize(deviceCode));
        traceQuery.setProductKey(replayProductKey);
        PageResult<DeviceMessageLog> messagePage = deviceMessageService.pageMessageTraceLogs(currentUserId, traceQuery, 1, 20);
        List<DeviceMessageLog> messages = messagePage == null || messagePage.getRecords() == null
                ? List.of()
                : messagePage.getRecords();
        DeviceMessageLog latestMessage = messages.isEmpty() ? null : messages.get(0);

        DeviceAccessErrorQuery errorQuery = new DeviceAccessErrorQuery();
        errorQuery.setTraceId(normalize(traceId));
        errorQuery.setDeviceCode(normalize(deviceCode));
        errorQuery.setProductKey(replayProductKey);
        PageResult<com.ghlzm.iot.device.entity.DeviceAccessErrorLog> errorPage =
                deviceAccessErrorLogService.pageLogs(currentUserId, errorQuery, 1, 10);
        List<com.ghlzm.iot.device.entity.DeviceAccessErrorLog> errors = errorPage == null || errorPage.getRecords() == null
                ? List.of()
                : errorPage.getRecords();

        MessageTraceDetailVO latestDetail = null;
        if (latestMessage != null && latestMessage.getId() != null) {
            latestDetail = deviceMessageService.getMessageTraceDetail(currentUserId, latestMessage.getId());
        }

        String resolvedTraceId = firstNonBlank(traceId, latestMessage == null ? null : latestMessage.getTraceId(), latestDetail == null ? null : latestDetail.getTraceId());
        String resolvedDeviceCode = firstNonBlank(deviceCode, latestMessage == null ? null : latestMessage.getDeviceCode(), latestDetail == null ? null : latestDetail.getDeviceCode());
        String resolvedProductKey = firstNonBlank(
                replayProductKey,
                latestMessage == null ? null : latestMessage.getProductKey(),
                latestDetail == null ? null : latestDetail.getProductKey()
        );

        Long resolvedProductId = replayContext.productId() != null
                ? replayContext.productId()
                : resolveProductId(latestMessage, resolvedProductKey);
        RiskGovernanceReplayGapSummaryVO gapSummary = new RiskGovernanceReplayGapSummaryVO();
        gapSummary.setMissingBindingCount(queryMissingBindingCount(resolvedDeviceCode));
        gapSummary.setMissingPolicyCount(queryMissingPolicyCount(resolvedDeviceCode));
        gapSummary.setMissingRiskMetricCount(resolveMissingRiskMetricCount(resolvedProductId));

        RiskGovernanceReplayVO replay = new RiskGovernanceReplayVO();
        replay.setTraceId(resolvedTraceId);
        replay.setDeviceCode(resolvedDeviceCode);
        replay.setProductKey(resolvedProductKey);
        replay.setReleaseBatchId(replayContext.releaseBatchId());
        replay.setReleaseScenarioCode(replayContext.releaseScenarioCode());
        replay.setMatchedMessageCount(messagePage == null ? 0L : safeLong(messagePage.getTotal()));
        replay.setMatchedAccessErrorCount(errorPage == null ? 0L : safeLong(errorPage.getTotal()));
        replay.setRecentMessages(new ArrayList<>(messages));
        replay.setRecentAccessErrors(new ArrayList<>(errors));
        replay.setLatestMessageDetail(latestDetail);
        replay.setGapSummary(gapSummary);
        return replay;
    }

    private ReplayContext resolveReplayContext(Long releaseBatchId, String productKey) {
        String normalizedProductKey = normalize(productKey);
        if (releaseBatchId == null) {
            return new ReplayContext(null, null, null, normalizedProductKey);
        }
        ProductContractReleaseBatch batch = productContractReleaseBatchMapper.selectById(releaseBatchId);
        if (batch == null) {
            throw new BizException("发布批次不存在或已删除");
        }
        Long batchProductId = batch.getProductId();
        if (batchProductId == null) {
            throw new BizException("发布批次未绑定产品，无法回放");
        }
        Product product = productMapper.selectById(batchProductId);
        if (product == null) {
            throw new BizException("发布批次对应产品不存在，无法回放");
        }
        String batchProductKey = normalize(product.getProductKey());
        if (!StringUtils.hasText(batchProductKey)) {
            throw new BizException("发布批次对应产品缺少 productKey，无法回放");
        }
        if (StringUtils.hasText(normalizedProductKey)
                && !batchProductKey.equalsIgnoreCase(normalizedProductKey)) {
            throw new BizException("releaseBatchId 与 productKey 不匹配");
        }
        return new ReplayContext(
                releaseBatchId,
                normalize(batch.getScenarioCode()),
                batchProductId,
                batchProductKey
        );
    }

    private Map<Long, Set<String>> buildContractIdentifierMap(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<ProductModel> models = productModelMapper.selectList(new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getDeleted, 0)
                .eq(ProductModel::getModelType, "property")
                .in(ProductModel::getProductId, productIds));
        Map<Long, Set<String>> map = new LinkedHashMap<>();
        for (ProductModel model : models) {
            if (model == null || model.getProductId() == null) {
                continue;
            }
            String identifier = normalize(model.getIdentifier());
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            map.computeIfAbsent(model.getProductId(), key -> new LinkedHashSet<>()).add(identifier);
        }
        return map;
    }

    private Map<Long, Set<String>> buildRiskMetricIdentifierMap(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        List<RiskMetricCatalog> catalogs = riskMetricCatalogMapper.selectList(new LambdaQueryWrapper<RiskMetricCatalog>()
                .eq(RiskMetricCatalog::getDeleted, 0)
                .eq(RiskMetricCatalog::getEnabled, 1)
                .in(RiskMetricCatalog::getProductId, productIds));
        Map<Long, Set<String>> map = new LinkedHashMap<>();
        for (RiskMetricCatalog catalog : catalogs) {
            if (catalog == null || catalog.getProductId() == null) {
                continue;
            }
            String identifier = normalize(catalog.getContractIdentifier());
            if (!StringUtils.hasText(identifier)) {
                continue;
            }
            map.computeIfAbsent(catalog.getProductId(), key -> new LinkedHashSet<>()).add(identifier);
        }
        return map;
    }

    private List<RiskGovernanceOpsAlertItemVO> collectOpsAlerts(Map<Long, Product> productMap,
                                                                Map<Long, Set<String>> contractIdentifiersByProduct,
                                                                Map<Long, Set<String>> riskMetricIdentifiersByProduct,
                                                                List<VendorMetricEvidence> evidences) {
        Map<String, OpsAlertAccumulator> accumulators = new LinkedHashMap<>();
        for (VendorMetricEvidence evidence : evidences) {
            if (evidence == null || evidence.getProductId() == null) {
                continue;
            }
            Long productId = evidence.getProductId();
            Product product = productMap.get(productId);
            Set<String> contractIdentifiers = contractIdentifiersByProduct.getOrDefault(productId, Set.of());
            String canonicalIdentifier = normalize(evidence.getCanonicalIdentifier());
            String rawIdentifier = normalize(evidence.getRawIdentifier());
            if (StringUtils.hasText(canonicalIdentifier) && !contractIdentifiers.contains(canonicalIdentifier)) {
                appendAlert(
                        accumulators,
                        ALERT_TYPE_FIELD_DRIFT,
                        "字段漂移告警",
                        product,
                        canonicalIdentifier,
                        rawIdentifier
                );
            }
            if (StringUtils.hasText(canonicalIdentifier)
                    && StringUtils.hasText(rawIdentifier)
                    && !canonicalIdentifier.equalsIgnoreCase(rawIdentifier)
                    && contractIdentifiers.contains(canonicalIdentifier)) {
                appendAlert(
                        accumulators,
                        ALERT_TYPE_CONTRACT_DIFF,
                        "厂商报文与正式合同差异告警",
                        product,
                        canonicalIdentifier,
                        rawIdentifier
                );
            }
        }

        for (Map.Entry<Long, Product> entry : productMap.entrySet()) {
            Long productId = entry.getKey();
            Product product = entry.getValue();
            Set<String> contractIdentifiers = contractIdentifiersByProduct.getOrDefault(productId, Set.of());
            if (contractIdentifiers.isEmpty()) {
                continue;
            }
            Set<String> riskIdentifiers = riskMetricIdentifiersByProduct.getOrDefault(productId, Set.of());
            Set<String> missingIdentifiers = contractIdentifiers.stream()
                    .filter(identifier -> !riskIdentifiers.contains(identifier))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (missingIdentifiers.isEmpty()) {
                continue;
            }
            appendAlert(
                    accumulators,
                    ALERT_TYPE_MISSING_RISK_METRIC,
                    "风险指标缺失告警",
                    product,
                    missingIdentifiers.iterator().next(),
                    "missing=" + missingIdentifiers.size()
            ).add(missingIdentifiers.size() - 1L);
        }
        return accumulators.values().stream().map(OpsAlertAccumulator::toVO).toList();
    }

    private OpsAlertAccumulator appendAlert(Map<String, OpsAlertAccumulator> accumulators,
                                            String alertType,
                                            String alertLabel,
                                            Product product,
                                            String sampleIdentifier,
                                            String sampleDetail) {
        String key = alertType + ":" + (product == null || product.getId() == null ? 0L : product.getId());
        OpsAlertAccumulator accumulator = accumulators.computeIfAbsent(
                key,
                ignored -> new OpsAlertAccumulator(alertType, alertLabel, product)
        );
        accumulator.setSample(sampleIdentifier, sampleDetail);
        accumulator.add(1L);
        return accumulator;
    }

    private void publishRaisedAlertEvents(List<RiskGovernanceOpsAlertItemVO> alerts) {
        if (applicationEventPublisher == null || alerts == null || alerts.isEmpty()) {
            return;
        }
        for (RiskGovernanceOpsAlertItemVO alert : alerts) {
            String alertCode = buildAlertCode(alert);
            if (!StringUtils.hasText(alertCode) || alert.getProductId() == null) {
                continue;
            }
            applicationEventPublisher.publishEvent(new GovernanceOpsAlertRaisedEvent(
                    SYSTEM_OPERATOR_ID,
                    alert.getAlertType(),
                    alertCode,
                    ALERT_SUBJECT_TYPE_PRODUCT,
                    alert.getProductId(),
                    alert.getProductId(),
                    null,
                    null,
                    null,
                    null,
                    alert.getProductKey(),
                    ALERT_SEVERITY_WARN,
                    safeLong(alert.getAffectedCount()),
                    firstNonBlank(alert.getAlertLabel(), alert.getAlertType()),
                    buildAlertMessage(alert),
                    buildDimensionKey(alert),
                    buildDimensionLabel(alert),
                    ALERT_SOURCE_STAGE,
                    buildAlertSnapshotJson(alert),
                    SYSTEM_OPERATOR_ID
            ));
        }
    }

    private String buildAlertCode(RiskGovernanceOpsAlertItemVO alert) {
        if (alert == null || alert.getProductId() == null || !StringUtils.hasText(alert.getAlertType())) {
            return null;
        }
        return "product:" + alert.getProductId() + ":" + alert.getAlertType().toLowerCase(Locale.ROOT);
    }

    private String buildAlertMessage(RiskGovernanceOpsAlertItemVO alert) {
        if (alert == null) {
            return null;
        }
        String identifier = normalize(alert.getSampleIdentifier());
        String detail = normalize(alert.getSampleDetail());
        if (StringUtils.hasText(identifier) && StringUtils.hasText(detail)) {
            return identifier + " | " + detail;
        }
        return firstNonBlank(detail, identifier, alert.getAlertLabel());
    }

    private String buildDimensionKey(RiskGovernanceOpsAlertItemVO alert) {
        if (alert == null || alert.getProductId() == null) {
            return null;
        }
        String sampleIdentifier = normalize(alert.getSampleIdentifier());
        if (StringUtils.hasText(sampleIdentifier)) {
            return "product:" + alert.getProductId() + ":" + sampleIdentifier;
        }
        return "product:" + alert.getProductId();
    }

    private String buildDimensionLabel(RiskGovernanceOpsAlertItemVO alert) {
        if (alert == null || alert.getProductId() == null) {
            return null;
        }
        String sampleIdentifier = normalize(alert.getSampleIdentifier());
        String productName = firstNonBlank(alert.getProductName(), alert.getProductKey(), "product-" + alert.getProductId());
        return StringUtils.hasText(sampleIdentifier) ? productName + "/" + sampleIdentifier : productName;
    }

    private String buildAlertSnapshotJson(RiskGovernanceOpsAlertItemVO alert) {
        if (alert == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("affectedCount", safeLong(alert.getAffectedCount()));
        if (StringUtils.hasText(alert.getSampleIdentifier())) {
            snapshot.put("sampleIdentifier", alert.getSampleIdentifier());
        }
        if (StringUtils.hasText(alert.getSampleDetail())) {
            snapshot.put("sampleDetail", alert.getSampleDetail());
        }
        if (StringUtils.hasText(alert.getProductKey())) {
            snapshot.put("productKey", alert.getProductKey());
        }
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long resolveMissingRiskMetricCount(Long productId) {
        if (productId == null) {
            return 0L;
        }
        Set<Long> productIds = Set.of(productId);
        Set<String> contractIdentifiers = buildContractIdentifierMap(productIds).getOrDefault(productId, Set.of());
        Set<String> riskIdentifiers = buildRiskMetricIdentifierMap(productIds).getOrDefault(productId, Set.of());
        return contractIdentifiers.stream().filter(identifier -> !riskIdentifiers.contains(identifier)).count();
    }

    private Long queryMissingBindingCount(String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return 0L;
        }
        com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery query = new com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery();
        query.setDeviceCode(deviceCode);
        query.setPageNum(1L);
        query.setPageSize(10L);
        PageResult<com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO> page = riskGovernanceService.listMissingBindings(query);
        return safeLong(page == null ? null : page.getTotal());
    }

    private Long queryMissingPolicyCount(String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return 0L;
        }
        com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery query = new com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery();
        query.setDeviceCode(deviceCode);
        query.setPageNum(1L);
        query.setPageSize(10L);
        PageResult<com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO> page = riskGovernanceService.listMissingPolicies(query);
        return safeLong(page == null ? null : page.getTotal());
    }

    private Long resolveProductId(DeviceMessageLog latestMessage, String productKey) {
        if (latestMessage != null && latestMessage.getProductId() != null) {
            return latestMessage.getProductId();
        }
        String normalizedProductKey = normalize(productKey);
        if (!StringUtils.hasText(normalizedProductKey)) {
            return null;
        }
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getDeleted, 0)
                .eq(Product::getProductKey, normalizedProductKey)
                .last("limit 1"));
        if (products == null || products.isEmpty()) {
            return null;
        }
        return products.get(0).getId();
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : Math.min(pageSize, 100L);
    }

    private <T> PageResult<T> toPage(List<T> items, long pageNum, long pageSize) {
        if (items == null || items.isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, items.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, items.size());
        return PageResult.of((long) items.size(), pageNum, pageSize, items.subList(fromIndex, toIndex));
    }

    private String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static final class OpsAlertAccumulator {
        private final String alertType;
        private final String alertLabel;
        private final Product product;
        private long affectedCount;
        private String sampleIdentifier;
        private String sampleDetail;

        private OpsAlertAccumulator(String alertType, String alertLabel, Product product) {
            this.alertType = alertType;
            this.alertLabel = alertLabel;
            this.product = product;
        }

        private void setSample(String identifier, String detail) {
            if (!StringUtils.hasText(sampleIdentifier) && StringUtils.hasText(identifier)) {
                sampleIdentifier = identifier;
            }
            if (!StringUtils.hasText(sampleDetail) && StringUtils.hasText(detail)) {
                sampleDetail = detail;
            }
        }

        private void add(long delta) {
            if (delta > 0) {
                affectedCount += delta;
            }
        }

        private RiskGovernanceOpsAlertItemVO toVO() {
            RiskGovernanceOpsAlertItemVO vo = new RiskGovernanceOpsAlertItemVO();
            vo.setAlertType(alertType);
            vo.setAlertLabel(alertLabel);
            vo.setProductId(product == null ? null : product.getId());
            vo.setProductKey(product == null ? null : product.getProductKey());
            vo.setProductName(product == null ? null : product.getProductName());
            vo.setAffectedCount(affectedCount);
            vo.setSampleIdentifier(sampleIdentifier);
            vo.setSampleDetail(sampleDetail);
            return vo;
        }
    }

    private record ReplayContext(Long releaseBatchId,
                                 String releaseScenarioCode,
                                 Long productId,
                                 String productKey) {
    }
}
