package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.service.ProductModelGovernanceReceiptStore;
import com.ghlzm.iot.device.service.ProductMetricEvidenceService;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.service.VendorMetricMappingRuntimeService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceAppliedItemVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelProtocolTemplateEvidenceVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 产品物模型服务实现，负责产品维度的模型治理和基础校验。
 */
@Service
public class ProductModelServiceImpl extends ServiceImpl<ProductModelMapper, ProductModel> implements ProductModelService {

    private static final List<String> ALLOWED_MODEL_TYPES = List.of("property", "event", "service");
    private static final String MODEL_TYPE_PROPERTY = "property";
    private static final String MODEL_TYPE_EVENT = "event";
    private static final String MODEL_TYPE_SERVICE = "service";
    private static final String NON_PROPERTY_COMPAT_DATA_TYPE = "json";
    private static final String STATUS_READY = "ready";
    private static final String STATUS_NEEDS_REVIEW = "needs_review";
    private static final String EXTRACTION_MODE_RUNTIME = "runtime";
    private static final String EXTRACTION_MODE_MANUAL = "manual";
    private static final String PARENT_SENSOR_STATE_PREFIX = "S1_ZT_1.sensor_state.";
    private static final String SAMPLE_TYPE_BUSINESS = "business";
    private static final String SAMPLE_TYPE_STATUS = "status";
    private static final String DEVICE_STRUCTURE_SINGLE = "single";
    private static final String DEVICE_STRUCTURE_COMPOSITE = "composite";
    private static final String RELEASE_SOURCE_MANUAL_COMPARE_APPLY = "manual_compare_apply";
    private static final String LASER_RANGEFINDER_PRODUCT_KEY = "nf-monitor-laser-rangefinder-v1";
    private static final Pattern POINT_IDENTIFIER_PATTERN = Pattern.compile("^L(\\d+)_([A-Z]+)_\\d+$");
    private static final Pattern TIMESTAMP_KEY_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.+$");
    private static final String SNAPSHOT_STAGE_BEFORE_APPLY = "BEFORE_APPLY";
    private static final String SNAPSHOT_STAGE_AFTER_APPLY = "AFTER_APPLY";
    private static final Set<String> SUPPORTED_COMPOSITE_CANONICALIZATION_STRATEGIES = Set.of("LEGACY", "LF_VALUE");
    private static final Set<String> SUPPORTED_COMPOSITE_STATUS_MIRROR_STRATEGIES = Set.of("NONE", "SENSOR_STATE");
    private static final Set<String> ROOT_WRAPPER_KEYS = Set.of(
            "properties",
            "property",
            "status",
            "telemetry",
            "data",
            "params",
            "payload"
    );
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
            "header",
            "headers",
            "bodies",
            "encoding",
            "rawtext",
            "jsoncandidate",
            "payloadbase64"
    );
    private static final Map<String, String> SENSOR_TYPE_LABELS = Map.of(
            "QJ", "倾角",
            "JS", "加速度",
            "GP", "GNSS",
            "JZ", "基准站",
            "YL", "雨量",
            "SW", "深位移",
            "GNSS", "GNSS"
    );
    private static final Map<String, String> PROPERTY_LABELS = Map.ofEntries(
            Map.entry("angle", "角度"),
            Map.entry("value", "监测值"),
            Map.entry("gx", "X 轴加速度"),
            Map.entry("gy", "Y 轴加速度"),
            Map.entry("gz", "Z 轴加速度"),
            Map.entry("x", "X 轴位移"),
            Map.entry("y", "Y 轴位移"),
            Map.entry("z", "Z 轴位移"),
            Map.entry("azi", "方位角"),
            Map.entry("gpstotalx", "GNSS 累计位移 X"),
            Map.entry("gpstotaly", "GNSS 累计位移 Y"),
            Map.entry("gpstotalz", "GNSS 累计位移 Z"),
            Map.entry("gpssinglex", "GNSS 单期位移 X"),
            Map.entry("gpssingley", "GNSS 单期位移 Y"),
            Map.entry("gpssinglez", "GNSS 单期位移 Z"),
            Map.entry("dispsx", "顺滑动方向累计变形量"),
            Map.entry("dispsy", "垂直坡面方向累计变形量"),
            Map.entry("lat", "纬度"),
            Map.entry("lon", "经度"),
            Map.entry("longitude", "经度"),
            Map.entry("latitude", "纬度"),
            Map.entry("temp", "设备温度"),
            Map.entry("temp_out", "外部温度"),
            Map.entry("humidity", "设备湿度"),
            Map.entry("humidity_out", "外部湿度"),
            Map.entry("signal_4g", "4G 信号强度"),
            Map.entry("signal_nb", "NB 信号强度"),
            Map.entry("signal_db", "信号值"),
            Map.entry("singal_nb", "NB 信号强度"),
            Map.entry("singal_db", "信号值"),
            Map.entry("solar_volt", "太阳能电压"),
            Map.entry("battery_volt", "电池电压"),
            Map.entry("battery_dump_energy", "电池剩余电量")
    );
    private static final Set<String> TELEMETRY_LAST_SEGMENTS = Set.of(
            "angle",
            "value",
            "gx",
            "gy",
            "gz",
            "x",
            "y",
            "z",
            "azi",
            "gpstotalx",
            "gpstotaly",
            "gpstotalz",
            "gpssinglex",
            "gpssingley",
            "gpssinglez",
            "dispsx",
            "dispsy"
    );

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final NormativeMetricDefinitionService normativeMetricDefinitionService;
    private final ProductMetricEvidenceService productMetricEvidenceService;
    private final ProductContractReleaseService productContractReleaseService;
    private final ProductModelGovernanceComparator governanceComparator = new ProductModelGovernanceComparator();
    private final ProductModelPropertyCandidateFilter propertyCandidateFilter = new ProductModelPropertyCandidateFilter();
    private final ProductModelNormativeMatcher normativeMatcher = new ProductModelNormativeMatcher();
    private final ProductModelGovernanceReceiptStore governanceReceiptStore;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final VendorMetricMappingRuntimeService vendorMetricMappingRuntimeService;
    private final CollectorChildMetricBoundaryPolicy collectorChildMetricBoundaryPolicy =
            new CollectorChildMetricBoundaryPolicy();

    public ProductModelServiceImpl(ProductMapper productMapper,
                                   ProductModelMapper productModelMapper,
                                   NormativeMetricDefinitionService normativeMetricDefinitionService,
                                   ProductMetricEvidenceService productMetricEvidenceService,
                                   ProductContractReleaseService productContractReleaseService,
                                   ProductModelGovernanceReceiptStore governanceReceiptStore,
                                   ApplicationEventPublisher applicationEventPublisher,
                                   VendorMetricMappingRuntimeService vendorMetricMappingRuntimeService) {
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.normativeMetricDefinitionService = normativeMetricDefinitionService;
        this.productMetricEvidenceService = productMetricEvidenceService;
        this.productContractReleaseService = productContractReleaseService;
        this.governanceReceiptStore = governanceReceiptStore;
        this.applicationEventPublisher = applicationEventPublisher;
        this.vendorMetricMappingRuntimeService = vendorMetricMappingRuntimeService;
    }

    @Override
    public List<ProductModelVO> listModels(Long productId) {
        getRequiredProduct(productId);
        return listActiveModels(productId).stream()
                .sorted(Comparator
                        .comparing(ProductModel::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(model -> normalizeOptional(model.getIdentifier()), Comparator.nullsLast(String::compareTo)))
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelVO createModel(Long productId, ProductModelUpsertDTO dto) {
        getRequiredProduct(productId);
        String modelType = normalizeModelType(dto.getModelType());
        String identifier = normalizeRequired(dto.getIdentifier(), "物模型标识");
        validateByModelType(modelType, dto);
        ensureIdentifierUnique(productId, identifier, null);

        ProductModel model = new ProductModel();
        model.setProductId(productId);
        applyEditableFields(model, modelType, identifier, dto);
        productModelMapper.insert(model);
        return toVO(model);
    }

    @Override
    public ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
        Product product = getRequiredProduct(productId);
        List<ProductModel> existingModels = listActiveModels(productId);
        ProductModelCandidateResultVO manualResult = buildManualGovernanceCandidates(product, existingModels.size(), dto);
        ProductModelCandidateResultVO runtimeResult = emptyCandidateResult(productId, existingModels.size(), EXTRACTION_MODE_RUNTIME);
        ProductModelGovernanceCompareVO compareResult =
                governanceComparator.compare(productId, existingModels, manualResult, runtimeResult);
        decorateCompareResultWithNormativeMetadata(product, compareResult);
        persistManualMetricEvidence(product, manualResult);
        governanceReceiptStore.replaceProtocolTemplateEvidence(productId, Map.of());
        return compareResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelGovernanceApplyResultVO applyGovernance(Long productId,
                                                               ProductModelGovernanceApplyDTO dto,
                                                               Long operatorId) {
        return applyGovernance(productId, dto, operatorId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelGovernanceApplyResultVO applyGovernance(Long productId,
                                                               ProductModelGovernanceApplyDTO dto,
                                                               Long operatorId,
                                                               Long approvalOrderId) {
        Product product = getRequiredProduct(productId);
        List<ReleaseModelSnapshotItem> beforeSnapshot = captureReleaseSnapshot(productId);
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        List<ProductModelGovernanceApplyDTO.ApplyItem> normalizedItems = normalizeApplyItems(product, safeApplyItems(dto));
        for (ProductModelGovernanceApplyDTO.ApplyItem item : normalizedItems) {
            String decision = normalizeRequired(item.getDecision(), "治理决策").toLowerCase(Locale.ROOT);
            switch (decision) {
                case "create" -> {
                    createFromGovernanceItem(productId, item);
                    createdCount++;
                }
                case "update" -> {
                    updateFromGovernanceItem(productId, item);
                    updatedCount++;
                }
                case "skip" -> skippedCount++;
                default -> throw new BizException("治理决策不支持: " + decision);
            }
        }
        List<ReleaseModelSnapshotItem> afterSnapshot = captureReleaseSnapshot(productId);
        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setCreatedCount(createdCount);
        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setConflictCount(0);
        result.setLastAppliedAt(LocalDateTime.now());
        result.setReleaseBatchId(createReleaseBatch(
                product,
                createdCount + updatedCount,
                operatorId,
                approvalOrderId,
                beforeSnapshot,
                afterSnapshot
        ));
        result.setAppliedItems(buildGovernanceAppliedItems(productId, normalizedItems));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelVO updateModel(Long productId, Long modelId, ProductModelUpsertDTO dto) {
        getRequiredProduct(productId);
        ProductModel model = getRequiredModel(productId, modelId);
        String modelType = normalizeModelType(dto.getModelType());
        String identifier = normalizeRequired(dto.getIdentifier(), "物模型标识");
        validateByModelType(modelType, dto);
        ensureIdentifierUnique(productId, identifier, modelId);

        applyEditableFields(model, modelType, identifier, dto);
        productModelMapper.updateById(model);
        return toVO(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long productId, Long modelId) {
        getRequiredProduct(productId);
        ProductModel model = getRequiredModel(productId, modelId);
        if (productModelMapper.deleteById(model.getId()) <= 0) {
            throw new BizException("产品物模型删除失败，请稍后重试");
        }
    }

    private Product getRequiredProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BizException("产品不存在: " + productId);
        }
        return product;
    }

    private ProductModel getRequiredModel(Long productId, Long modelId) {
        ProductModel model = productModelMapper.selectById(modelId);
        if (model == null || Integer.valueOf(1).equals(model.getDeleted()) || !productId.equals(model.getProductId())) {
            throw new BizException("产品物模型不存在: " + modelId);
        }
        return model;
    }

    private List<ProductModel> listActiveModels(Long productId) {
        return productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getDeleted, 0)
        );
    }

    private ProductModelCandidateResultVO buildCandidateResult(Long productId,
                                                               int existingModelCount,
                                                               PropertyEvidenceBundle propertyBundle,
                                                               EventEvidenceBundle eventBundle,
                                                               ServiceEvidenceBundle serviceBundle,
                                                               String extractionMode,
                                                               String sampleType,
                                                               String sampleDeviceCode,
                                                               int ignoredFieldCount) {
        ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
        summary.setExtractionMode(extractionMode);
        summary.setSampleType(sampleType);
        summary.setSampleDeviceCode(sampleDeviceCode);
        summary.setPropertyEvidenceCount(propertyBundle.evidenceCount());
        summary.setPropertyCandidateCount(propertyBundle.candidates().size());
        summary.setEventEvidenceCount(eventBundle.evidenceCount());
        summary.setEventCandidateCount(eventBundle.candidates().size());
        summary.setServiceEvidenceCount(serviceBundle.evidenceCount());
        summary.setServiceCandidateCount(serviceBundle.candidates().size());
        summary.setNeedsReviewCount(propertyBundle.needsReviewCount()
                + countNeedsReview(eventBundle.candidates())
                + countNeedsReview(serviceBundle.candidates()));
        summary.setExistingModelCount(existingModelCount);
        summary.setCreatedCount(0);
        summary.setSkippedCount(0);
        summary.setConflictCount(0);
        summary.setEventHint(eventBundle.hint());
        summary.setServiceHint(serviceBundle.hint());
        summary.setIgnoredFieldCount(ignoredFieldCount);
        summary.setLastExtractedAt(LocalDateTime.now());

        ProductModelCandidateResultVO result = new ProductModelCandidateResultVO();
        result.setProductId(productId);
        result.setSummary(summary);
        result.setPropertyCandidates(propertyBundle.candidates());
        result.setEventCandidates(eventBundle.candidates());
        result.setServiceCandidates(serviceBundle.candidates());
        return result;
    }

    private ProductModelCandidateResultVO buildManualGovernanceCandidates(Product product,
                                                                          int existingModelCount,
                                                                          ProductModelGovernanceCompareDTO dto) {
        if (dto == null || dto.getManualExtract() == null) {
            throw new BizException("请先提供上报样本");
        }
        ProductModelCandidateResultVO result = manualExtractGovernanceCandidates(product, existingModelCount, dto.getManualExtract());
        refreshCandidateSummary(result, existingModelCount);
        return result;
    }

    private void decorateCompareResultWithNormativeMetadata(Product product,
                                                            ProductModelGovernanceCompareVO compareResult) {
        if (compareResult == null || compareResult.getCompareRows() == null || compareResult.getCompareRows().isEmpty()) {
            return;
        }
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode)) {
            return;
        }
        List<NormativeMetricDefinition> definitions = safeNormativeDefinitions(
                normativeMetricDefinitionService.listByScenario(scenarioCode)
        );
        if (definitions.isEmpty()) {
            return;
        }
        Map<String, String> displayAliases = resolveNormativeDisplayAliases(product);
        for (ProductModelGovernanceCompareRowVO row : compareResult.getCompareRows()) {
            if (row == null || !MODEL_TYPE_PROPERTY.equals(normalizeOptional(row.getModelType()))) {
                continue;
            }
            String identifier = normalizeOptional(row.getIdentifier());
            if (identifier == null || definitions.stream().noneMatch(item -> identifier.equals(item.getIdentifier()))) {
                continue;
            }
            ProductModelNormativeMatcher.NormativeMatchResult match = normativeMatcher.matchProperty(
                    identifier,
                    resolveRawIdentifiers(row),
                    definitions
            );
            if (match == null) {
                continue;
            }
            row.setNormativeIdentifier(match.normativeIdentifier());
            row.setNormativeName(displayAliases.getOrDefault(match.normativeIdentifier(), match.normativeName()));
            row.setRiskReady(match.riskReady());
            row.setRawIdentifiers(match.rawIdentifiers());
            if (match.riskReady() && !row.getRiskFlags().contains("risk_ready")) {
                row.getRiskFlags().add("risk_ready");
            }
        }
    }

    private Map<String, String> resolveNormativeDisplayAliases(Product product) {
        String productKey = normalizeOptional(product == null ? null : product.getProductKey());
        if (productKey == null || !LASER_RANGEFINDER_PRODUCT_KEY.equalsIgnoreCase(productKey)) {
            return Map.of();
        }
        return Map.of(
                "value", "激光测距值",
                "sensor_state", "传感器状态"
        );
    }

    private void persistManualMetricEvidence(Product product,
                                            ProductModelCandidateResultVO manualResult) {
        if (product == null || manualResult == null || manualResult.getPropertyCandidates() == null) {
            return;
        }
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode)) {
            return;
        }
        List<VendorMetricEvidence> rows = new ArrayList<>();
        for (ProductModelCandidateVO candidate : manualResult.getPropertyCandidates()) {
            if (candidate == null || !StringUtils.hasText(candidate.getIdentifier())) {
                continue;
            }
            List<String> rawIdentifiers = candidate.getRawIdentifiers() == null || candidate.getRawIdentifiers().isEmpty()
                    ? List.of(candidate.getIdentifier())
                    : candidate.getRawIdentifiers();
            for (String rawIdentifier : rawIdentifiers) {
                String normalizedRawIdentifier = normalizeOptional(rawIdentifier);
                if (normalizedRawIdentifier == null) {
                    continue;
                }
                VendorMetricEvidence row = new VendorMetricEvidence();
                row.setProductId(product.getId());
                row.setRawIdentifier(normalizedRawIdentifier);
                row.setCanonicalIdentifier(candidate.getIdentifier());
                row.setLogicalChannelCode(resolveLogicalChannelCode(normalizedRawIdentifier));
                row.setEvidenceOrigin("manual_compare");
                row.setValueType(candidate.getDataType());
                row.setEvidenceCount(firstPositive(candidate.getEvidenceCount(), 1));
                row.setLastSeenTime(LocalDateTime.now());
                row.setMetadataJson(buildManualEvidenceMetadata(scenarioCode, manualResult));
                rows.add(row);
            }
        }
        productMetricEvidenceService.replaceManualEvidence(product.getId(), scenarioCode, rows);
    }

    private Long createReleaseBatch(Product product,
                                    int releasedFieldCount,
                                    Long operatorId,
                                    Long approvalOrderId,
                                    List<ReleaseModelSnapshotItem> beforeSnapshot,
                                    List<ReleaseModelSnapshotItem> afterSnapshot) {
        if (product == null || releasedFieldCount <= 0) {
            return null;
        }
        if (operatorId == null || operatorId <= 0) {
            throw new BizException("发布操作缺少有效操作者");
        }
        String scenarioCode = normativeMatcher.resolveScenarioCode(product);
        if (!StringUtils.hasText(scenarioCode)) {
            return null;
        }
        Long batchId = productContractReleaseService.createBatch(
                product.getId(),
                scenarioCode,
                RELEASE_SOURCE_MANUAL_COMPARE_APPLY,
                releasedFieldCount,
                operatorId,
                approvalOrderId,
                RELEASE_SOURCE_MANUAL_COMPARE_APPLY
        );
        if (batchId == null) {
            return null;
        }
        productContractReleaseService.saveBatchSnapshot(
                batchId,
                product.getId(),
                SNAPSHOT_STAGE_BEFORE_APPLY,
                serializeReleaseSnapshot(beforeSnapshot),
                operatorId
        );
        productContractReleaseService.saveBatchSnapshot(
                batchId,
                product.getId(),
                SNAPSHOT_STAGE_AFTER_APPLY,
                serializeReleaseSnapshot(afterSnapshot),
                operatorId
        );
        publishContractReleasedEvent(product, batchId, scenarioCode, afterSnapshot, operatorId, approvalOrderId);
        return batchId;
    }

    private void publishContractReleasedEvent(Product product,
                                              Long batchId,
                                              String scenarioCode,
                                              List<ReleaseModelSnapshotItem> afterSnapshot,
                                              Long operatorId,
                                              Long approvalOrderId) {
        if (applicationEventPublisher == null || product == null || batchId == null) {
            return;
        }
        List<String> releasedIdentifiers = (afterSnapshot == null ? List.<ReleaseModelSnapshotItem>of() : afterSnapshot).stream()
                .map(ReleaseModelSnapshotItem::identifier)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        applicationEventPublisher.publishEvent(new ProductContractReleasedEvent(
                defaultTenantId(product.getTenantId()),
                product.getId(),
                batchId,
                scenarioCode,
                releasedIdentifiers,
                operatorId,
                approvalOrderId
        ));
    }

    private List<ReleaseModelSnapshotItem> captureReleaseSnapshot(Long productId) {
        return listActiveModels(productId).stream()
                .sorted(Comparator
                        .comparing((ProductModel model) -> normalizeOptional(model.getModelType()), Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductModel::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(model -> normalizeOptional(model.getIdentifier()), Comparator.nullsLast(String::compareTo)))
                .map(model -> new ReleaseModelSnapshotItem(
                        normalizeOptional(model.getModelType()),
                        normalizeOptional(model.getIdentifier()),
                        normalizeOptional(model.getModelName()),
                        normalizeOptional(model.getDataType()),
                        normalizeOptional(model.getSpecsJson()),
                        normalizeOptional(model.getEventType()),
                        normalizeOptional(model.getServiceInputJson()),
                        normalizeOptional(model.getServiceOutputJson()),
                        model.getSortNo(),
                        model.getRequiredFlag(),
                        normalizeOptional(model.getDescription())
                ))
                .toList();
    }

    private String serializeReleaseSnapshot(List<ReleaseModelSnapshotItem> snapshotItems) {
        List<ReleaseModelSnapshotItem> safeItems = snapshotItems == null ? List.of() : snapshotItems;
        try {
            return objectMapper.writeValueAsString(safeItems);
        } catch (Exception ex) {
            throw new BizException("生成发布快照失败");
        }
    }

    private List<ProductModelGovernanceAppliedItemVO> buildGovernanceAppliedItems(Long productId,
                                                                                  List<ProductModelGovernanceApplyDTO.ApplyItem> items) {
        List<ProductModelGovernanceApplyDTO.ApplyItem> safeItems = items == null ? List.of() : items;
        Map<String, ProductModelProtocolTemplateEvidenceVO> protocolTemplateEvidenceByIdentifier =
                governanceReceiptStore.loadProtocolTemplateEvidence(productId);
        return safeItems.stream()
                .map(item -> toAppliedItem(item, protocolTemplateEvidenceByIdentifier.get(normalizeOptional(item.getIdentifier()))))
                .toList();
    }

    private ProductModelGovernanceAppliedItemVO toAppliedItem(ProductModelGovernanceApplyDTO.ApplyItem item,
                                                              ProductModelProtocolTemplateEvidenceVO protocolTemplateEvidence) {
        ProductModelGovernanceAppliedItemVO appliedItem = new ProductModelGovernanceAppliedItemVO();
        appliedItem.setModelType(normalizeOptional(item.getModelType()));
        appliedItem.setIdentifier(normalizeOptional(item.getIdentifier()));
        appliedItem.setDecision(normalizeOptional(item.getDecision()));
        if (protocolTemplateEvidence != null) {
            appliedItem.setTemplateCodes(protocolTemplateEvidence.getTemplateCodes());
            appliedItem.setCanonicalizationStrategies(protocolTemplateEvidence.getCanonicalizationStrategies());
            appliedItem.setChildDeviceCodes(protocolTemplateEvidence.getChildDeviceCodes());
        }
        return appliedItem;
    }

    private ProductModelCandidateResultVO manualExtractGovernanceCandidates(Product product,
                                                                            int existingModelCount,
                                                                            ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract) {
        ManualSampleSnapshot snapshot = parseManualSample(product, manualExtract);
        PropertyEvidenceBundle propertyBundle = collectManualPropertyCandidates(product, snapshot, Set.of());
        EventEvidenceBundle eventBundle = new EventEvidenceBundle(
                List.of(),
                0,
                "手动提炼当前仅生成属性候选，事件请在正式模型中人工补充。"
        );
        ServiceEvidenceBundle serviceBundle = new ServiceEvidenceBundle(
                List.of(),
                0,
                "手动提炼当前仅生成属性候选，服务请在正式模型中人工补充。"
        );
        return buildCandidateResult(
                product == null ? null : product.getId(),
                existingModelCount,
                propertyBundle,
                eventBundle,
                serviceBundle,
                EXTRACTION_MODE_MANUAL,
                snapshot.sampleType(),
                snapshot.deviceCode(),
                snapshot.ignoredFieldCount()
        );
    }

    private ProductModelCandidateResultVO emptyCandidateResult(Long productId, int existingModelCount, String extractionMode) {
        return buildCandidateResult(
                productId,
                existingModelCount,
                new PropertyEvidenceBundle(List.of(), 0, 0),
                new EventEvidenceBundle(List.of(), 0, null),
                new ServiceEvidenceBundle(List.of(), 0, null),
                extractionMode,
                null,
                null,
                0
        );
    }

    private void refreshCandidateSummary(ProductModelCandidateResultVO result, int existingModelCount) {
        if (result == null) {
            return;
        }
        ProductModelCandidateSummaryVO summary = result.getSummary();
        if (summary == null) {
            summary = new ProductModelCandidateSummaryVO();
            result.setSummary(summary);
        }
        summary.setPropertyEvidenceCount(countEvidence(result.getPropertyCandidates()));
        summary.setPropertyCandidateCount(result.getPropertyCandidates().size());
        summary.setEventEvidenceCount(countEvidence(result.getEventCandidates()));
        summary.setEventCandidateCount(result.getEventCandidates().size());
        summary.setServiceEvidenceCount(countEvidence(result.getServiceCandidates()));
        summary.setServiceCandidateCount(result.getServiceCandidates().size());
        summary.setNeedsReviewCount(countNeedsReview(result.getPropertyCandidates())
                + countNeedsReview(result.getEventCandidates())
                + countNeedsReview(result.getServiceCandidates()));
        summary.setExistingModelCount(existingModelCount);
        if (summary.getCreatedCount() == null) {
            summary.setCreatedCount(0);
        }
        if (summary.getSkippedCount() == null) {
            summary.setSkippedCount(0);
        }
        if (summary.getConflictCount() == null) {
            summary.setConflictCount(0);
        }
        summary.setLastExtractedAt(LocalDateTime.now());
    }

    private int countEvidence(List<ProductModelCandidateVO> candidates) {
        return candidates.stream()
                .map(ProductModelCandidateVO::getEvidenceCount)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private List<ProductModelGovernanceApplyDTO.ApplyItem> safeApplyItems(ProductModelGovernanceApplyDTO dto) {
        return dto == null || dto.getItems() == null ? List.of() : dto.getItems();
    }

    private List<ProductModelGovernanceApplyDTO.ApplyItem> normalizeApplyItems(Product product,
                                                                               List<ProductModelGovernanceApplyDTO.ApplyItem> items) {
        return (items == null ? List.<ProductModelGovernanceApplyDTO.ApplyItem>of() : items)
                .stream()
                .map(item -> normalizeApplyItem(product, item))
                .toList();
    }

    private ProductModelGovernanceApplyDTO.ApplyItem normalizeApplyItem(Product product,
                                                                        ProductModelGovernanceApplyDTO.ApplyItem item) {
        ProductModelGovernanceApplyDTO.ApplyItem normalizedItem = copyApplyItem(item);
        if (normalizedItem == null || !MODEL_TYPE_PROPERTY.equals(normalizeOptional(normalizedItem.getModelType()))) {
            return normalizedItem;
        }
        String normalizedIdentifier =
                vendorMetricMappingRuntimeService.normalizeApplyIdentifier(product, normalizedItem.getIdentifier());
        normalizedItem.setIdentifier(firstNonNull(normalizedIdentifier, normalizedItem.getIdentifier()));
        return normalizedItem;
    }

    private ProductModelGovernanceApplyDTO.ApplyItem copyApplyItem(ProductModelGovernanceApplyDTO.ApplyItem item) {
        if (item == null) {
            return null;
        }
        ProductModelGovernanceApplyDTO.ApplyItem copy = new ProductModelGovernanceApplyDTO.ApplyItem();
        copy.setDecision(item.getDecision());
        copy.setTargetModelId(item.getTargetModelId());
        copy.setModelType(item.getModelType());
        copy.setIdentifier(item.getIdentifier());
        copy.setModelName(item.getModelName());
        copy.setDataType(item.getDataType());
        copy.setSpecsJson(item.getSpecsJson());
        copy.setEventType(item.getEventType());
        copy.setServiceInputJson(item.getServiceInputJson());
        copy.setServiceOutputJson(item.getServiceOutputJson());
        copy.setSortNo(item.getSortNo());
        copy.setRequiredFlag(item.getRequiredFlag());
        copy.setDescription(item.getDescription());
        copy.setCompareStatus(item.getCompareStatus());
        return copy;
    }

    private void createFromGovernanceItem(Long productId, ProductModelGovernanceApplyDTO.ApplyItem item) {
        createModel(productId, toUpsertDTO(item));
    }

    private void updateFromGovernanceItem(Long productId, ProductModelGovernanceApplyDTO.ApplyItem item) {
        if (item.getTargetModelId() == null) {
            throw new BizException("治理修订必须指定 targetModelId");
        }
        updateModel(productId, item.getTargetModelId(), toUpsertDTO(item));
    }

    private ProductModelUpsertDTO toUpsertDTO(ProductModelGovernanceApplyDTO.ApplyItem item) {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType(item.getModelType());
        dto.setIdentifier(item.getIdentifier());
        dto.setModelName(item.getModelName());
        dto.setDataType(item.getDataType());
        dto.setSpecsJson(item.getSpecsJson());
        dto.setEventType(item.getEventType());
        dto.setServiceInputJson(item.getServiceInputJson());
        dto.setServiceOutputJson(item.getServiceOutputJson());
        dto.setSortNo(item.getSortNo());
        dto.setRequiredFlag(item.getRequiredFlag());
        dto.setDescription(item.getDescription());
        return dto;
    }

    private PropertyEvidenceBundle collectManualPropertyCandidates(Product product,
                                                                   ManualSampleSnapshot snapshot,
                                                                   Set<String> existingIdentifiers) {
        Map<String, PropertyAccumulator> accumulators = new LinkedHashMap<>();
        int evidenceCount = 0;
        for (ManualLeafEvidence evidence : snapshot.leaves()) {
            evidenceCount++;
            ProductModelPropertyCandidateFilter.NormalizedPropertyIdentifier normalizedIdentifier =
                    propertyCandidateFilter.normalizeIdentifier(normalizeOptional(evidence.identifier()));
            List<String> observedRawIdentifiers = mergeRawIdentifiers(
                    evidence.rawIdentifiers(),
                    normalizedIdentifier.rawIdentifiers()
            );
            String identifier = resolveGovernanceCandidateIdentifier(product, normalizedIdentifier.identifier(), observedRawIdentifiers);
            if (identifier == null || existingIdentifiers.contains(identifier)) {
                continue;
            }
            accumulators.computeIfAbsent(identifier, PropertyAccumulator::new)
                    .acceptManualSample(snapshot.sampleType(), LocalDateTime.now(), evidence, observedRawIdentifiers);
        }

        List<ProductModelCandidateVO> candidates = accumulators.values().stream()
                .map(this::toPropertyCandidate)
                .sorted(Comparator
                        .comparing(ProductModelCandidateVO::getNeedsReview)
                        .thenComparing(ProductModelCandidateVO::getGroupKey, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductModelCandidateVO::getIdentifier))
                .toList();
        return new PropertyEvidenceBundle(candidates, evidenceCount, countNeedsReview(candidates));
    }

    private String resolveGovernanceCandidateIdentifier(Product product,
                                                        String defaultIdentifier,
                                                        List<String> observedRawIdentifiers) {
        List<String> safeRawIdentifiers = observedRawIdentifiers == null ? List.of() : observedRawIdentifiers;
        String logicalChannelCode = resolveLogicalChannelCode(safeRawIdentifiers);
        for (String rawIdentifier : safeRawIdentifiers) {
            String normalizedRawIdentifier = normalizeOptional(rawIdentifier);
            if (normalizedRawIdentifier == null) {
                continue;
            }
            VendorMetricMappingRuntimeService.MappingResolution resolution =
                    vendorMetricMappingRuntimeService.resolveForGovernance(product, normalizedRawIdentifier, logicalChannelCode);
            if (resolution != null && StringUtils.hasText(resolution.targetNormativeIdentifier())) {
                return resolution.targetNormativeIdentifier();
            }
        }
        return normalizeOptional(defaultIdentifier);
    }

    private ManualSampleSnapshot parseManualSample(Product product,
                                                   ProductModelGovernanceCompareDTO.ManualExtractInput input) {
        String deviceStructure = normalizeDeviceStructure(input == null ? null : input.getDeviceStructure());
        ManualSampleSnapshot rawSnapshot = parseManualSampleRaw(input);
        if (DEVICE_STRUCTURE_SINGLE.equals(deviceStructure)) {
            return rawSnapshot;
        }
        return applyCompositeManualSnapshot(product, input, rawSnapshot);
    }

    private ManualSampleSnapshot parseManualSampleRaw(ProductModelGovernanceCompareDTO.ManualExtractInput input) {
        String sampleType = normalizeSampleType(input == null ? null : input.getSampleType());
        String normalizedPayload = normalizeOptional(JsonPayloadUtils.normalizeJsonDocument(input == null ? null : input.getSamplePayload()));
        if (normalizedPayload == null) {
            throw new BizException("样本报文不能为空");
        }
        try {
            JsonNode root = objectMapper.readTree(normalizedPayload);
            if (!(root instanceof ObjectNode objectNode) || objectNode.size() != 1) {
                throw new BizException("单次只支持解析一个设备样本");
            }
            var iterator = objectNode.properties().iterator();
            Map.Entry<String, JsonNode> entry = iterator.next();
            String deviceCode = normalizeRequired(entry.getKey(), "设备编码");
            if (!(entry.getValue() instanceof ObjectNode deviceNode)) {
                throw new BizException("设备样本内容必须是 JSON 对象");
            }
            ManualLeafCollector collector = new ManualLeafCollector();
            collectManualLeafValues(deviceNode, "", collector, true);
            return new ManualSampleSnapshot(deviceCode, sampleType, List.copyOf(collector.leaves()), collector.ignoredFieldCount());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("样本报文必须是合法 JSON");
        }
    }

    private ManualSampleSnapshot applyCompositeManualSnapshot(Product product,
                                                             ProductModelGovernanceCompareDTO.ManualExtractInput input,
                                                             ManualSampleSnapshot rawSnapshot) {
        String parentDeviceCode = normalizeRequired(input == null ? null : input.getParentDeviceCode(), "父设备编码");
        if (!parentDeviceCode.equals(rawSnapshot.deviceCode())) {
            throw new BizException("父设备编码与样本根设备不一致");
        }
        List<NormalizedRelationMapping> relationMappings = normalizeRelationMappings(input);
        if (collectorChildMetricBoundaryPolicy.applies(product, input == null ? null : input.getDeviceStructure())) {
            return applyCollectorCompositeManualSnapshot(parentDeviceCode, rawSnapshot, relationMappings);
        }
        List<ManualLeafEvidence> canonicalLeaves = new ArrayList<>();
        for (ManualLeafEvidence leaf : rawSnapshot.leaves()) {
            String canonicalIdentifier = canonicalizeCompositeIdentifier(leaf.identifier(), rawSnapshot.sampleType(), relationMappings);
            if (canonicalIdentifier == null) {
                continue;
            }
            canonicalLeaves.add(new ManualLeafEvidence(
                    canonicalIdentifier,
                    leaf.sampleValue(),
                    leaf.valueType(),
                    mergeRawIdentifiers(leaf.rawIdentifiers(), List.of(leaf.identifier()))
            ));
        }
        return new ManualSampleSnapshot(
                parentDeviceCode,
                rawSnapshot.sampleType(),
                canonicalLeaves,
                rawSnapshot.ignoredFieldCount() + Math.max(0, rawSnapshot.leaves().size() - canonicalLeaves.size())
        );
    }

    private ManualSampleSnapshot applyCollectorCompositeManualSnapshot(String parentDeviceCode,
                                                                      ManualSampleSnapshot rawSnapshot,
                                                                      List<NormalizedRelationMapping> relationMappings) {
        List<String> logicalChannelCodes = relationMappings == null
                ? List.of()
                : relationMappings.stream()
                .map(NormalizedRelationMapping::logicalChannelCode)
                .toList();
        List<ManualLeafEvidence> collectorLeaves = new ArrayList<>();
        for (ManualLeafEvidence leaf : rawSnapshot.leaves()) {
            if (!collectorChildMetricBoundaryPolicy.shouldKeepLeaf(rawSnapshot.sampleType(), leaf.identifier(), logicalChannelCodes)) {
                continue;
            }
            String collectorIdentifier =
                    collectorChildMetricBoundaryPolicy.toCollectorIdentifier(rawSnapshot.sampleType(), leaf.identifier());
            if (collectorIdentifier == null) {
                continue;
            }
            collectorLeaves.add(new ManualLeafEvidence(
                    collectorIdentifier,
                    leaf.sampleValue(),
                    leaf.valueType(),
                    mergeRawIdentifiers(leaf.rawIdentifiers(), List.of(leaf.identifier()))
            ));
        }
        return new ManualSampleSnapshot(
                parentDeviceCode,
                rawSnapshot.sampleType(),
                collectorLeaves,
                rawSnapshot.ignoredFieldCount() + Math.max(0, rawSnapshot.leaves().size() - collectorLeaves.size())
        );
    }

    private List<NormalizedRelationMapping> normalizeRelationMappings(
            ProductModelGovernanceCompareDTO.ManualExtractInput input) {
        List<ProductModelGovernanceCompareDTO.RelationMappingInput> sourceItems =
                input == null || input.getRelationMappings() == null ? List.of() : input.getRelationMappings();
        Map<String, NormalizedRelationMapping> normalized = new LinkedHashMap<>();
        for (ProductModelGovernanceCompareDTO.RelationMappingInput item : sourceItems) {
            String logicalChannelCode = normalizeOptional(item == null ? null : item.getLogicalChannelCode());
            String childDeviceCode = normalizeOptional(item == null ? null : item.getChildDeviceCode());
            if (logicalChannelCode == null || childDeviceCode == null) {
                continue;
            }
            String canonicalizationStrategy = normalizeCompositeCanonicalizationStrategy(
                    item == null ? null : item.getCanonicalizationStrategy(),
                    logicalChannelCode
            );
            String statusMirrorStrategy = normalizeCompositeStatusMirrorStrategy(
                    item == null ? null : item.getStatusMirrorStrategy(),
                    logicalChannelCode
            );
            NormalizedRelationMapping normalizedItem = new NormalizedRelationMapping(
                    logicalChannelCode,
                    childDeviceCode,
                    canonicalizationStrategy,
                    statusMirrorStrategy
            );
            normalized.put(logicalChannelCode, normalizedItem);
        }
        if (normalized.isEmpty()) {
            throw new BizException("复合设备模式下至少需要 1 条映射关系");
        }
        return List.copyOf(normalized.values());
    }

    private String canonicalizeCompositeIdentifier(String identifier,
                                                   String sampleType,
                                                   List<NormalizedRelationMapping> relationMappings) {
        String normalizedIdentifier = normalizeOptional(identifier);
        if (normalizedIdentifier == null || relationMappings == null || relationMappings.isEmpty()) {
            return null;
        }
        for (NormalizedRelationMapping item : relationMappings) {
            String logicalChannelCode = item == null ? null : item.logicalChannelCode();
            if (logicalChannelCode == null) {
                continue;
            }
            if (SAMPLE_TYPE_STATUS.equals(sampleType)) {
                if ("SENSOR_STATE".equals(item.statusMirrorStrategy())
                        && normalizedIdentifier.equals(PARENT_SENSOR_STATE_PREFIX + logicalChannelCode)) {
                    return "sensor_state";
                }
                continue;
            }
            if (normalizedIdentifier.equals(logicalChannelCode)) {
                return "value";
            }
            if (!normalizedIdentifier.startsWith(logicalChannelCode + ".")) {
                continue;
            }
            if ("LEGACY".equals(item.canonicalizationStrategy())) {
                return normalizeOptional(normalizedIdentifier.substring(logicalChannelCode.length() + 1));
            }
            if ("LF_VALUE".equals(item.canonicalizationStrategy())) {
                return "value";
            }
        }
        return null;
    }

    private String normalizeCompositeCanonicalizationStrategy(String strategy, String logicalChannelCode) {
        String normalized = normalizeOptional(strategy);
        if (normalized == null) {
            return inferCompositeCanonicalizationStrategy(logicalChannelCode);
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_COMPOSITE_CANONICALIZATION_STRATEGIES.contains(upper)) {
            throw new BizException("复合设备归一化策略不支持: " + strategy);
        }
        return upper;
    }

    private String normalizeCompositeStatusMirrorStrategy(String strategy, String logicalChannelCode) {
        String normalized = normalizeOptional(strategy);
        if (normalized == null) {
            return inferCompositeStatusMirrorStrategy(logicalChannelCode);
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_COMPOSITE_STATUS_MIRROR_STRATEGIES.contains(upper)) {
            throw new BizException("复合设备状态镜像策略不支持: " + strategy);
        }
        return upper;
    }

    private String inferCompositeCanonicalizationStrategy(String logicalChannelCode) {
        String normalizedLogicalChannelCode = normalizeOptional(logicalChannelCode);
        if (normalizedLogicalChannelCode != null
                && normalizedLogicalChannelCode.toUpperCase(Locale.ROOT).contains("_SW_")) {
            return "LEGACY";
        }
        return "LF_VALUE";
    }

    private String inferCompositeStatusMirrorStrategy(String logicalChannelCode) {
        String normalizedLogicalChannelCode = normalizeOptional(logicalChannelCode);
        if (normalizedLogicalChannelCode != null
                && (normalizedLogicalChannelCode.toUpperCase(Locale.ROOT).contains("_LF_")
                || normalizedLogicalChannelCode.toUpperCase(Locale.ROOT).contains("_SW_"))) {
            return "SENSOR_STATE";
        }
        return "NONE";
    }

    private void collectManualLeafValues(JsonNode node,
                                         String prefix,
                                         ManualLeafCollector collector,
                                         boolean rootLevel) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isValueNode()) {
            if (StringUtils.hasText(prefix)) {
                collector.add(prefix, node.asText(), resolveNodeValueType(node));
            }
            return;
        }
        if (node.isArray()) {
            collector.incrementIgnoredFieldCount();
            return;
        }
        if (!(node instanceof ObjectNode objectNode)) {
            collector.incrementIgnoredFieldCount();
            return;
        }
        if (objectNode.size() == 0) {
            collector.incrementIgnoredFieldCount();
            return;
        }
        objectNode.properties().forEach(entry -> {
            String fieldName = normalizeOptional(entry.getKey());
            if (fieldName == null) {
                return;
            }
            if (rootLevel && IGNORED_ROOT_KEYS.contains(fieldName)) {
                return;
            }
            boolean unwrapRoot = rootLevel && ROOT_WRAPPER_KEYS.contains(fieldName.toLowerCase(Locale.ROOT));
            boolean unwrapTimestamp = isTimestampKey(fieldName);
            String nextPrefix = unwrapRoot || unwrapTimestamp ? prefix : appendIdentifier(prefix, fieldName);
            collectManualLeafValues(entry.getValue(), nextPrefix, collector, false);
        });
    }

    private String normalizeSampleType(String sampleType) {
        String normalized = normalizeRequired(sampleType, "样本类型").toLowerCase(Locale.ROOT);
        if (!Set.of(SAMPLE_TYPE_BUSINESS, SAMPLE_TYPE_STATUS).contains(normalized)) {
            throw new BizException("样本类型不支持: " + normalized);
        }
        return normalized;
    }

    private String normalizeDeviceStructure(String deviceStructure) {
        String normalized = normalizeRequired(deviceStructure, "设备结构").toLowerCase(Locale.ROOT);
        if (!Set.of(DEVICE_STRUCTURE_SINGLE, DEVICE_STRUCTURE_COMPOSITE).contains(normalized)) {
            throw new BizException("设备结构不支持: " + normalized);
        }
        return normalized;
    }

    private boolean isTimestampKey(String fieldName) {
        return TIMESTAMP_KEY_PATTERN.matcher(fieldName).matches();
    }

    private String resolveNodeValueType(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return "bool";
        }
        if (node.isIntegralNumber()) {
            return "integer";
        }
        if (node.isNumber()) {
            return "double";
        }
        if (node.isTextual()) {
            return "string";
        }
        return null;
    }

    private ProductModelCandidateVO toPropertyCandidate(PropertyAccumulator accumulator) {
        String groupKey = classifyPropertyGroup(accumulator.identifier);
        boolean needsReview = isSuspiciousIdentifier(accumulator.identifier)
                || "unknown".equals(groupKey);
        String modelName = suggestPropertyModelName(accumulator.identifier, accumulator.propertyName, groupKey);
        String description = buildPropertyDescription(
                accumulator.identifier,
                modelName,
                groupKey,
                needsReview,
                accumulator.sampleType
        );

        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(MODEL_TYPE_PROPERTY);
        candidate.setIdentifier(accumulator.identifier);
        candidate.setModelName(modelName);
        candidate.setDataType(resolvePropertyDataType(accumulator.valueType, accumulator.sampleValue, accumulator.identifier));
        candidate.setSortNo(defaultSortNo(groupKey));
        candidate.setRequiredFlag(0);
        candidate.setDescription(description);
        candidate.setGroupKey(groupKey);
        candidate.setEvidenceOrigin(accumulator.sampleType == null ? "runtime" : "sample_json");
        candidate.setRawIdentifiers(accumulator.rawIdentifiers.isEmpty() ? null : List.copyOf(accumulator.rawIdentifiers));
        boolean hasMessageEvidence = accumulator.messageEvidenceCount > 0;
        candidate.setConfidence(adjustPropertyConfidenceForEvidenceThreshold(
                resolveConfidence(groupKey, needsReview, hasMessageEvidence),
                accumulator.evidenceCount,
                accumulator.messageEvidenceCount,
                needsReview
        ));
        candidate.setNeedsReview(needsReview);
        candidate.setCandidateStatus(needsReview ? STATUS_NEEDS_REVIEW : STATUS_READY);
        candidate.setReviewReason(resolvePropertyReviewReason(needsReview, accumulator.sampleType));
        candidate.setEvidenceCount(accumulator.evidenceCount);
        candidate.setMessageEvidenceCount(accumulator.messageEvidenceCount);
        candidate.setLastReportTime(accumulator.lastReportTime);
        candidate.setSourceTables(new ArrayList<>(accumulator.sourceTables));
        return candidate;
    }

    private void ensureIdentifierUnique(Long productId, String identifier, Long excludeId) {
        LambdaQueryWrapper<ProductModel> wrapper = new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getIdentifier, identifier)
                .eq(ProductModel::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(ProductModel::getId, excludeId);
        }
        ProductModel existing = productModelMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BizException("同一产品下物模型标识已存在: " + identifier);
        }
    }

    private void applyEditableFields(ProductModel model, String modelType, String identifier, ProductModelUpsertDTO dto) {
        model.setModelType(modelType);
        model.setIdentifier(identifier);
        model.setModelName(normalizeRequired(dto.getModelName(), "物模型名称"));
        model.setSortNo(dto.getSortNo());
        model.setRequiredFlag(dto.getRequiredFlag());
        model.setDescription(normalizeOptional(dto.getDescription()));

        if (MODEL_TYPE_PROPERTY.equals(modelType)) {
            model.setDataType(normalizeRequired(dto.getDataType(), "dataType"));
            model.setSpecsJson(validateJsonField(dto.getSpecsJson(), "specsJson"));
            model.setEventType(null);
            model.setServiceInputJson(null);
            model.setServiceOutputJson(null);
            return;
        }

        if (MODEL_TYPE_EVENT.equals(modelType)) {
            model.setDataType(NON_PROPERTY_COMPAT_DATA_TYPE);
            model.setSpecsJson(null);
            model.setEventType(normalizeRequired(dto.getEventType(), "eventType"));
            model.setServiceInputJson(null);
            model.setServiceOutputJson(null);
            return;
        }

        model.setDataType(NON_PROPERTY_COMPAT_DATA_TYPE);
        model.setSpecsJson(null);
        model.setEventType(null);
        model.setServiceInputJson(validateJsonField(dto.getServiceInputJson(), "serviceInputJson"));
        model.setServiceOutputJson(validateJsonField(dto.getServiceOutputJson(), "serviceOutputJson"));
    }

    private void validateByModelType(String modelType, ProductModelUpsertDTO dto) {
        if (MODEL_TYPE_PROPERTY.equals(modelType)) {
            if (!StringUtils.hasText(dto.getDataType())) {
                throw new BizException("属性物模型必须填写 dataType");
            }
            if (StringUtils.hasText(dto.getEventType())
                    || StringUtils.hasText(dto.getServiceInputJson())
                    || StringUtils.hasText(dto.getServiceOutputJson())) {
                throw new BizException("属性物模型只允许填写 dataType 和 specsJson");
            }
            validateJsonField(dto.getSpecsJson(), "specsJson");
            return;
        }

        if (MODEL_TYPE_EVENT.equals(modelType)) {
            if (StringUtils.hasText(dto.getDataType())
                    || StringUtils.hasText(dto.getSpecsJson())
                    || StringUtils.hasText(dto.getServiceInputJson())
                    || StringUtils.hasText(dto.getServiceOutputJson())) {
                throw new BizException("事件物模型只允许填写 eventType");
            }
            return;
        }

        if (StringUtils.hasText(dto.getDataType())
                || StringUtils.hasText(dto.getSpecsJson())
                || StringUtils.hasText(dto.getEventType())) {
            throw new BizException("服务物模型只允许填写 serviceInputJson 和 serviceOutputJson");
        }
        validateJsonField(dto.getServiceInputJson(), "serviceInputJson");
        validateJsonField(dto.getServiceOutputJson(), "serviceOutputJson");
    }

    private String validateJsonField(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (Exception ex) {
            throw new BizException(fieldName + " 必须是合法 JSON");
        }
    }

    private String normalizeModelType(String modelType) {
        String normalized = normalizeRequired(modelType, "物模型类型").toLowerCase(Locale.ROOT);
        if (!ALLOWED_MODEL_TYPES.contains(normalized)) {
            throw new BizException("物模型类型不支持: " + normalized);
        }
        return normalized;
    }

    private String normalizeRequired(String value, String label) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(label + "不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private int countNeedsReview(List<ProductModelCandidateVO> candidates) {
        return (int) candidates.stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getNeedsReview()))
                .count();
    }

    private String classifyPropertyGroup(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        String lastSegment = lastIdentifierSegment(identifier).toLowerCase(Locale.ROOT);
        if (List.of("lat", "lon", "latitude", "longitude").contains(lastSegment)) {
            return "location";
        }
        if (normalized.startsWith("s1_zt_")
                || normalized.contains("signal")
                || normalized.contains("battery")
                || "sensor_state".equals(lastSegment)
                || normalized.contains("sensor_state")
                || normalized.contains("sw_version")
                || normalized.endsWith(".temp")
                || "temp".equals(lastSegment)) {
            return "device_status";
        }
        if ("value".equals(lastSegment)) {
            return "telemetry";
        }
        if (TELEMETRY_LAST_SEGMENTS.contains(lastSegment)
                || lastSegment.startsWith("disps")) {
            return "telemetry";
        }
        return "unknown";
    }

    private boolean isSuspiciousIdentifier(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        return normalized.startsWith("codex_verify_") || normalized.contains("singal_");
    }

    private String suggestPropertyModelName(String identifier, String propertyName, String groupKey) {
        String normalizedPropertyName = normalizeOptional(propertyName);
        if (normalizedPropertyName != null && !looksLikeIdentifierLabel(identifier, normalizedPropertyName)) {
            if ("telemetry".equals(groupKey)) {
                String pointLabel = resolvePointLabel(identifier);
                if (pointLabel != null && !normalizedPropertyName.contains(pointLabel)) {
                    return pointLabel + normalizedPropertyName;
                }
            }
            return normalizedPropertyName;
        }

        if (isSensorStateIdentifier(identifier)) {
            String sensorStateLabel = resolveSensorStateLabel(identifier);
            if (sensorStateLabel != null) {
                return sensorStateLabel;
            }
        }

        String pointLabel = resolvePointLabel(identifier);
        String lastSegment = lastIdentifierSegment(identifier).toLowerCase(Locale.ROOT);
        String baseLabel = PROPERTY_LABELS.getOrDefault(lastSegment, identifier);
        if (pointLabel != null && "telemetry".equals(groupKey)) {
            return pointLabel + baseLabel;
        }
        return baseLabel;
    }

    private String resolvePointLabel(String identifier) {
        int splitIndex = identifier.indexOf('.');
        String pointIdentifier = splitIndex >= 0 ? identifier.substring(0, splitIndex) : identifier;
        return resolvePointLabelByToken(pointIdentifier);
    }

    private String resolvePointLabelByToken(String pointIdentifier) {
        Matcher matcher = POINT_IDENTIFIER_PATTERN.matcher(pointIdentifier);
        if (!matcher.matches()) {
            return null;
        }
        String pointNo = matcher.group(1);
        String sensorType = matcher.group(2);
        String sensorLabel = SENSOR_TYPE_LABELS.get(sensorType);
        if (sensorLabel == null) {
            return null;
        }
        return pointNo + "号" + sensorLabel + "测点";
    }

    private boolean looksLikeIdentifierLabel(String identifier, String propertyName) {
        String normalizedName = propertyName.toLowerCase(Locale.ROOT);
        String normalizedIdentifier = identifier.toLowerCase(Locale.ROOT);
        return normalizedName.equals(normalizedIdentifier)
                || propertyName.contains(".")
                || normalizedName.startsWith("l1_")
                || normalizedName.startsWith("s1_zt_");
    }

    private boolean isSensorStateIdentifier(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        return "sensor_state".equals(normalized) || normalized.contains("sensor_state.");
    }

    private String resolveSensorStateLabel(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        if ("sensor_state".equals(normalized)) {
            return "传感器状态";
        }
        int markerIndex = normalized.indexOf("sensor_state.");
        if (markerIndex < 0) {
            return null;
        }
        String pointIdentifier = identifier.substring(markerIndex + "sensor_state.".length());
        String pointLabel = resolvePointLabelByToken(pointIdentifier);
        if (pointLabel == null) {
            return "传感器状态";
        }
        return pointLabel + "传感器状态";
    }

    private String buildPropertyDescription(String identifier,
                                            String modelName,
                                            String groupKey,
                                            boolean needsReview,
                                            String sampleType) {
        if (sampleType != null) {
            if ("telemetry".equals(groupKey)) {
                return "来源于手动录入样本，归属测点属性，反映 " + modelName + " 的核心监测值，建议确认后写入正式产品契约。";
            }
            if ("device_status".equals(groupKey)) {
                return "来源于手动录入样本，归属设备状态属性，用于反映终端运行、联网或传感器状态，不应与业务测点混写。";
            }
            if ("location".equals(groupKey)) {
                return "来源于手动录入样本，归属定位属性，用于表达设备安装位置或空间坐标，建议保持经纬度成对维护。";
            }
        }
        if (needsReview) {
            return "来源于真实上报，但当前标识 "
                    + identifier
                    + " 存在临时验证或命名漂移风险，需人工归一后再写入正式契约。";
        }
        if ("telemetry".equals(groupKey)) {
            return "归属测点属性，反映 " + modelName + " 的核心监测值，建议作为正式产品契约维护。";
        }
        if ("device_status".equals(groupKey)) {
            return "归属设备状态属性，用于反映终端运行、联网或传感器状态，不应与业务测点混写。";
        }
        if ("location".equals(groupKey)) {
            return "归属定位属性，用于表达设备安装位置或空间坐标，建议保持经纬度成对维护。";
        }
        return "来源于真实上报，但当前边界仍不稳定，建议人工确认命名与归类后再入库。";
    }

    private String resolvePropertyReviewReason(boolean needsReview, String sampleType) {
        if (!needsReview) {
            return null;
        }
        return "命名需人工归一后再入正式契约";
    }

    private Double resolveConfidence(String groupKey, boolean needsReview, boolean hasMessageEvidence) {
        if (needsReview) {
            return hasMessageEvidence ? 0.62D : 0.48D;
        }
        return switch (groupKey) {
            case "telemetry" -> hasMessageEvidence ? 0.96D : 0.91D;
            case "device_status" -> hasMessageEvidence ? 0.93D : 0.87D;
            case "location" -> 0.9D;
            default -> 0.68D;
        };
    }

    private Double adjustPropertyConfidenceForEvidenceThreshold(Double confidence,
                                                                Integer evidenceCount,
                                                                Integer messageEvidenceCount,
                                                                boolean needsReview) {
        if (confidence == null || needsReview) {
            return confidence;
        }
        if (firstNonNull(messageEvidenceCount, 0) > 0) {
            return confidence;
        }
        if (firstNonNull(evidenceCount, 0) <= 1) {
            return Math.max(0.35D, confidence - 0.15D);
        }
        return confidence;
    }

    private String resolvePropertyDataType(String valueType, String sampleValue, String identifier) {
        String normalizedValueType = normalizeOptional(valueType);
        if (normalizedValueType != null) {
            return normalizedValueType;
        }

        String lowerIdentifier = identifier.toLowerCase(Locale.ROOT);
        if (lowerIdentifier.endsWith(".lat")
                || lowerIdentifier.endsWith(".lon")
                || "lat".equals(lowerIdentifier)
                || "lon".equals(lowerIdentifier)) {
            return "double";
        }
        String normalizedSample = normalizeOptional(sampleValue);
        if (normalizedSample == null) {
            return "string";
        }
        if ("true".equalsIgnoreCase(normalizedSample) || "false".equalsIgnoreCase(normalizedSample)) {
            return "bool";
        }
        if (normalizedSample.matches("-?\\d+")) {
            return "integer";
        }
        if (normalizedSample.matches("-?\\d+\\.\\d+")) {
            return "double";
        }
        return "string";
    }

    private int defaultSortNo(String groupKey) {
        return switch (groupKey) {
            case "telemetry" -> 10;
            case "device_status" -> 110;
            case "location" -> 210;
            default -> 310;
        };
    }

    private String lastIdentifierSegment(String identifier) {
        int index = identifier.lastIndexOf('.');
        return index >= 0 ? identifier.substring(index + 1) : identifier;
    }

    private String appendIdentifier(String prefix, String fieldName) {
        if (!StringUtils.hasText(prefix)) {
            return fieldName;
        }
        return prefix + "." + fieldName;
    }

    private ProductModelVO toVO(ProductModel model) {
        ProductModelVO vo = new ProductModelVO();
        vo.setId(model.getId());
        vo.setProductId(model.getProductId());
        vo.setModelType(model.getModelType());
        vo.setIdentifier(model.getIdentifier());
        vo.setModelName(model.getModelName());
        vo.setDataType(MODEL_TYPE_PROPERTY.equals(model.getModelType()) ? model.getDataType() : null);
        vo.setSpecsJson(model.getSpecsJson());
        vo.setEventType(model.getEventType());
        vo.setServiceInputJson(model.getServiceInputJson());
        vo.setServiceOutputJson(model.getServiceOutputJson());
        vo.setSortNo(model.getSortNo());
        vo.setRequiredFlag(model.getRequiredFlag());
        vo.setDescription(model.getDescription());
        vo.setCreateTime(model.getCreateTime());
        vo.setUpdateTime(model.getUpdateTime());
        return vo;
    }

    private record ReleaseModelSnapshotItem(String modelType,
                                            String identifier,
                                            String modelName,
                                            String dataType,
                                            String specsJson,
                                            String eventType,
                                            String serviceInputJson,
                                            String serviceOutputJson,
                                            Integer sortNo,
                                            Integer requiredFlag,
                                            String description) {
    }

    private record PropertyEvidenceBundle(List<ProductModelCandidateVO> candidates, int evidenceCount, int needsReviewCount) {
    }

    private record EventEvidenceBundle(List<ProductModelCandidateVO> candidates, int evidenceCount, String hint) {
    }

    private record ServiceEvidenceBundle(List<ProductModelCandidateVO> candidates, int evidenceCount, String hint) {
    }

    private record ManualSampleSnapshot(String deviceCode,
                                        String sampleType,
                                        List<ManualLeafEvidence> leaves,
                                        int ignoredFieldCount) {
    }

    private List<NormativeMetricDefinition> safeNormativeDefinitions(List<NormativeMetricDefinition> definitions) {
        return definitions == null ? List.of() : definitions;
    }

    private List<String> resolveRawIdentifiers(ProductModelGovernanceCompareRowVO row) {
        if (row.getManualCandidate() != null && row.getManualCandidate().getRawIdentifiers() != null
                && !row.getManualCandidate().getRawIdentifiers().isEmpty()) {
            return row.getManualCandidate().getRawIdentifiers();
        }
        if (row.getRuntimeCandidate() != null && row.getRuntimeCandidate().getRawIdentifiers() != null
                && !row.getRuntimeCandidate().getRawIdentifiers().isEmpty()) {
            return row.getRuntimeCandidate().getRawIdentifiers();
        }
        return List.of();
    }

    private String resolveLogicalChannelCode(String rawIdentifier) {
        String normalized = normalizeOptional(rawIdentifier);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PARENT_SENSOR_STATE_PREFIX)) {
            return normalized.substring(PARENT_SENSOR_STATE_PREFIX.length());
        }
        Matcher matcher = POINT_IDENTIFIER_PATTERN.matcher(normalized);
        if (matcher.matches()) {
            return normalized;
        }
        int firstSeparatorIndex = normalized.indexOf('.');
        if (firstSeparatorIndex > 0) {
            String pointIdentifier = normalized.substring(0, firstSeparatorIndex);
            Matcher pointMatcher = POINT_IDENTIFIER_PATTERN.matcher(pointIdentifier);
            if (pointMatcher.matches()) {
                return pointIdentifier;
            }
        }
        return null;
    }

    private String resolveLogicalChannelCode(List<String> rawIdentifiers) {
        if (rawIdentifiers == null || rawIdentifiers.isEmpty()) {
            return null;
        }
        for (String rawIdentifier : rawIdentifiers) {
            String logicalChannelCode = resolveLogicalChannelCode(rawIdentifier);
            if (logicalChannelCode != null) {
                return logicalChannelCode;
            }
        }
        return null;
    }

    private String buildManualEvidenceMetadata(String scenarioCode, ProductModelCandidateResultVO manualResult) {
        String sampleType = manualResult == null || manualResult.getSummary() == null
                ? null
                : normalizeOptional(manualResult.getSummary().getSampleType());
        if (!StringUtils.hasText(scenarioCode) && !StringUtils.hasText(sampleType)) {
            return null;
        }
        if (StringUtils.hasText(scenarioCode) && StringUtils.hasText(sampleType)) {
            return "{\"scenarioCode\":\"" + scenarioCode + "\",\"sampleType\":\"" + sampleType + "\"}";
        }
        if (StringUtils.hasText(scenarioCode)) {
            return "{\"scenarioCode\":\"" + scenarioCode + "\"}";
        }
        return "{\"sampleType\":\"" + sampleType + "\"}";
    }

    @SafeVarargs
    private final List<String> mergeRawIdentifiers(List<String>... groups) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (groups == null) {
            return List.of();
        }
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String item : group) {
                String normalized = normalizeOptional(item);
                if (normalized != null) {
                    merged.add(normalized);
                }
            }
        }
        return merged.isEmpty() ? List.of() : List.copyOf(merged);
    }

    private int firstPositive(Integer value, int fallback) {
        if (value == null || value <= 0) {
            return fallback;
        }
        return value;
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null || tenantId <= 0 ? 1L : tenantId;
    }

    private record ManualLeafEvidence(String identifier,
                                      String sampleValue,
                                      String valueType,
                                      List<String> rawIdentifiers) {
    }

    private record NormalizedRelationMapping(String logicalChannelCode,
                                             String childDeviceCode,
                                             String canonicalizationStrategy,
                                             String statusMirrorStrategy) {
    }

    private static final class ManualLeafCollector {
        private final List<ManualLeafEvidence> leaves = new ArrayList<>();
        private int ignoredFieldCount;

        private void add(String identifier, String sampleValue, String valueType) {
            leaves.add(new ManualLeafEvidence(identifier, sampleValue, valueType, List.of(identifier)));
        }

        private void incrementIgnoredFieldCount() {
            ignoredFieldCount++;
        }

        private List<ManualLeafEvidence> leaves() {
            return leaves;
        }

        private int ignoredFieldCount() {
            return ignoredFieldCount;
        }
    }

    private static final class PropertyAccumulator {
        private final String identifier;
        private String propertyName;
        private String valueType;
        private String sampleValue;
        private String sampleType;
        private int evidenceCount;
        private int messageEvidenceCount;
        private LocalDateTime lastReportTime;
        private final LinkedHashSet<String> sourceTables = new LinkedHashSet<>();
        private final LinkedHashSet<String> rawIdentifiers = new LinkedHashSet<>();

        private PropertyAccumulator(String identifier) {
            this.identifier = identifier;
        }

        private void acceptManualSample(String sampleType,
                                        LocalDateTime reportTime,
                                        ManualLeafEvidence evidence,
                                        List<String> observedRawIdentifiers) {
            evidenceCount++;
            messageEvidenceCount++;
            sourceTables.add("manual_sample");
            appendRawIdentifiers(observedRawIdentifiers);
            this.sampleType = sampleType;
            String identifierTail = tailSegment(evidence.identifier());
            if (normalizeText(identifierTail) != null) {
                propertyName = normalizeText(identifierTail);
            }
            if (normalizeText(evidence.valueType()) != null) {
                valueType = normalizeText(evidence.valueType());
            }
            if (normalizeText(evidence.sampleValue()) != null) {
                sampleValue = normalizeText(evidence.sampleValue());
            }
            updateLastReportTime(reportTime);
        }

        private void updateLastReportTime(LocalDateTime candidateTime) {
            if (candidateTime == null) {
                return;
            }
            if (lastReportTime == null || candidateTime.isAfter(lastReportTime)) {
                lastReportTime = candidateTime;
            }
        }

        private void appendRawIdentifiers(List<String> observedRawIdentifiers) {
            if (observedRawIdentifiers == null || observedRawIdentifiers.isEmpty()) {
                return;
            }
            for (String rawIdentifier : observedRawIdentifiers) {
                String normalized = normalizeText(rawIdentifier);
                if (normalized != null) {
                    rawIdentifiers.add(normalized);
                }
            }
        }

        private static String normalizeText(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }

        private static String tailSegment(String identifier) {
            if (identifier == null) {
                return null;
            }
            int index = identifier.lastIndexOf('.');
            return index >= 0 ? identifier.substring(index + 1) : identifier;
        }
    }
}
