package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelManualExtractDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.CommandRecord;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.CommandRecordMapper;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceEvidenceVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final String SAMPLE_TYPE_BUSINESS = "business";
    private static final String SAMPLE_TYPE_STATUS = "status";
    private static final String SAMPLE_TYPE_OTHER = "other";
    private static final int EXTRACTION_WINDOW_DAYS = 30;
    private static final Pattern EVENT_TOPIC_PATTERN = Pattern.compile("/event/([^/]+)/?");
    private static final Pattern POINT_IDENTIFIER_PATTERN = Pattern.compile("^L(\\d+)_([A-Z]+)_\\d+$");
    private static final Pattern TIMESTAMP_KEY_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T.+$");
    private static final Set<String> PROPERTY_LOG_TYPES = Set.of("property", "status");
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
            "gpssinglez"
    );

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;
    private final DeviceMapper deviceMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DeviceMessageLogMapper deviceMessageLogMapper;
    private final CommandRecordMapper commandRecordMapper;
    private final ProductModelGovernanceComparator governanceComparator = new ProductModelGovernanceComparator();
    private final ProductModelNormativePresetRegistry normativePresetRegistry = new ProductModelNormativePresetRegistry();

    public ProductModelServiceImpl(ProductMapper productMapper,
                                   ProductModelMapper productModelMapper,
                                   DeviceMapper deviceMapper,
                                   DevicePropertyMapper devicePropertyMapper,
                                   DeviceMessageLogMapper deviceMessageLogMapper,
                                   CommandRecordMapper commandRecordMapper) {
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
        this.deviceMapper = deviceMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.deviceMessageLogMapper = deviceMessageLogMapper;
        this.commandRecordMapper = commandRecordMapper;
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
    public ProductModelCandidateResultVO listModelCandidates(Long productId) {
        Product product = getRequiredProduct(productId);
        List<ProductModel> existingModels = listActiveModels(productId);
        Set<String> existingIdentifiers = toIdentifierSet(existingModels.stream().map(ProductModel::getIdentifier).toList());
        List<Device> devices = listActiveDevices(productId);
        List<DeviceProperty> properties = listDeviceProperties(devices.stream().map(Device::getId).toList());
        List<DeviceMessageLog> logs = listRecentMessageLogs(productId);

        PropertyEvidenceBundle propertyBundle = collectPropertyCandidates(properties, logs, existingIdentifiers);
        EventEvidenceBundle eventBundle = collectEventCandidates(logs, existingIdentifiers);
        ServiceEvidenceBundle serviceBundle = collectServiceCandidates(product, existingIdentifiers);
        return buildCandidateResult(
                productId,
                existingModels.size(),
                propertyBundle,
                eventBundle,
                serviceBundle,
                EXTRACTION_MODE_RUNTIME,
                null,
                null,
                0
        );
    }

    @Override
    public ProductModelCandidateResultVO manualExtractModelCandidates(Long productId, ProductModelManualExtractDTO dto) {
        getRequiredProduct(productId);
        List<ProductModel> existingModels = listActiveModels(productId);
        Set<String> existingIdentifiers = toIdentifierSet(existingModels.stream().map(ProductModel::getIdentifier).toList());
        ManualSampleSnapshot snapshot = parseManualSample(dto);
        PropertyEvidenceBundle propertyBundle = collectManualPropertyCandidates(snapshot, existingIdentifiers);
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
                productId,
                existingModels.size(),
                propertyBundle,
                eventBundle,
                serviceBundle,
                EXTRACTION_MODE_MANUAL,
                snapshot.sampleType(),
                snapshot.deviceCode(),
                snapshot.ignoredFieldCount()
        );
    }

    @Override
    public ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
        Product product = getRequiredProduct(productId);
        List<ProductModel> existingModels = listActiveModels(productId);
        String normalizedNormativePresetCode = isNormativeMode(dto)
                ? normalizeAndValidateNormativePresetCode(product, dto)
                : null;
        ProductModelCandidateResultVO manualResult = isNormativeMode(dto)
                ? buildNormativeGovernanceCandidates(productId, existingModels.size(), dto, normalizedNormativePresetCode)
                : buildManualGovernanceCandidates(productId, existingModels.size(), dto);
        ProductModelCandidateResultVO runtimeResult = shouldLoadRuntimeCandidates(dto)
                ? buildRuntimeGovernanceCandidates(productId, product, existingModels.size(), dto, normalizedNormativePresetCode)
                : emptyCandidateResult(productId, existingModels.size(), EXTRACTION_MODE_RUNTIME);
        return governanceComparator.compare(productId, existingModels, manualResult, runtimeResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelGovernanceApplyResultVO applyGovernance(Long productId, ProductModelGovernanceApplyDTO dto) {
        getRequiredProduct(productId);
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        for (ProductModelGovernanceApplyDTO.ApplyItem item : safeApplyItems(dto)) {
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
        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setCreatedCount(createdCount);
        result.setUpdatedCount(updatedCount);
        result.setSkippedCount(skippedCount);
        result.setConflictCount(0);
        result.setLastAppliedAt(LocalDateTime.now());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelCandidateSummaryVO confirmModelCandidates(Long productId, ProductModelCandidateConfirmDTO dto) {
        getRequiredProduct(productId);
        List<ProductModel> existingModels = listActiveModels(productId);
        Set<String> existingIdentifiers = toIdentifierSet(existingModels.stream().map(ProductModel::getIdentifier).toList());
        int nextSortNo = existingModels.stream()
                .map(ProductModel::getSortNo)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 10;

        int createdCount = 0;
        int skippedCount = 0;
        int conflictCount = 0;
        List<ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem> items =
                dto == null || dto.getItems() == null ? List.of() : dto.getItems();
        for (ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem item : items) {
            String identifier = normalizeRequired(item.getIdentifier(), "物模型标识");
            if (existingIdentifiers.contains(identifier)) {
                skippedCount++;
                conflictCount++;
                continue;
            }

            ProductModelUpsertDTO upsertDTO = toUpsertDTO(item, nextSortNo);
            String modelType = normalizeModelType(upsertDTO.getModelType());
            validateByModelType(modelType, upsertDTO);

            ProductModel model = new ProductModel();
            model.setProductId(productId);
            applyEditableFields(model, modelType, identifier, upsertDTO);
            productModelMapper.insert(model);
            existingIdentifiers.add(identifier);
            createdCount++;
            nextSortNo = (model.getSortNo() == null ? nextSortNo : model.getSortNo()) + 10;
        }

        ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
        summary.setPropertyEvidenceCount(0);
        summary.setPropertyCandidateCount(0);
        summary.setEventEvidenceCount(0);
        summary.setEventCandidateCount(0);
        summary.setServiceEvidenceCount(0);
        summary.setServiceCandidateCount(0);
        summary.setNeedsReviewCount(0);
        summary.setExistingModelCount(existingModels.size());
        summary.setCreatedCount(createdCount);
        summary.setSkippedCount(skippedCount);
        summary.setConflictCount(conflictCount);
        summary.setLastExtractedAt(LocalDateTime.now());
        return summary;
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

    private List<Device> listActiveDevices(Long productId) {
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getProductId, productId)
                        .eq(Device::getDeleted, 0)
        );
    }

    private List<DeviceProperty> listDeviceProperties(Collection<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .in(DeviceProperty::getDeviceId, deviceIds)
                        .orderByDesc(DeviceProperty::getReportTime)
                        .orderByDesc(DeviceProperty::getUpdateTime)
        );
    }

    private List<DeviceMessageLog> listRecentMessageLogs(Long productId) {
        LocalDateTime windowStart = LocalDateTime.now().minusDays(EXTRACTION_WINDOW_DAYS);
        return deviceMessageLogMapper.selectList(
                new LambdaQueryWrapper<DeviceMessageLog>()
                        .eq(DeviceMessageLog::getProductId, productId)
                        .ge(DeviceMessageLog::getCreateTime, windowStart)
                        .orderByDesc(DeviceMessageLog::getReportTime)
                        .orderByDesc(DeviceMessageLog::getCreateTime)
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

    private ProductModelCandidateResultVO buildManualGovernanceCandidates(Long productId,
                                                                          int existingModelCount,
                                                                          ProductModelGovernanceCompareDTO dto) {
        ProductModelCandidateResultVO result = dto != null && dto.getManualExtract() != null
                ? manualExtractGovernanceCandidates(productId, existingModelCount, dto.getManualExtract())
                : emptyCandidateResult(productId, existingModelCount, EXTRACTION_MODE_MANUAL);
        mergeManualDraftItems(result, dto == null ? List.of() : dto.getManualDraftItems());
        refreshCandidateSummary(result, existingModelCount);
        return result;
    }

    private ProductModelCandidateResultVO buildNormativeGovernanceCandidates(Long productId,
                                                                             int existingModelCount,
                                                                             ProductModelGovernanceCompareDTO dto,
                                                                             String normalizedNormativePresetCode) {
        List<ProductModelGovernanceEvidenceVO> definitions = normativePresetRegistry.buildPropertyPreset(
                normalizedNormativePresetCode,
                dto == null ? null : dto.getSelectedNormativeIdentifiers()
        );
        List<ProductModelCandidateVO> propertyCandidates = definitions.stream()
                .map(this::toNormativeCandidate)
                .toList();
        ProductModelCandidateResultVO result = buildCandidateResult(
                productId,
                existingModelCount,
                new PropertyEvidenceBundle(propertyCandidates, propertyCandidates.size(), countNeedsReview(propertyCandidates)),
                new EventEvidenceBundle(List.of(), 0, "规范模式首批不治理事件"),
                new ServiceEvidenceBundle(List.of(), 0, "规范模式首批不治理服务"),
                EXTRACTION_MODE_MANUAL,
                ProductModelNormativePresetRegistry.GOVERNANCE_MODE_NORMATIVE,
                null,
                0
        );
        mergeManualDraftItems(result, dto == null ? List.of() : dto.getManualDraftItems());
        refreshCandidateSummary(result, existingModelCount);
        return result;
    }

    private ProductModelCandidateResultVO buildRuntimeGovernanceCandidates(Long productId,
                                                                           Product product,
                                                                           int existingModelCount,
                                                                           ProductModelGovernanceCompareDTO dto,
                                                                           String normalizedNormativePresetCode) {
        List<Device> devices = listActiveDevices(productId);
        List<DeviceProperty> properties = listDeviceProperties(devices.stream().map(Device::getId).toList());
        List<DeviceMessageLog> logs = listRecentMessageLogs(productId);

        PropertyEvidenceBundle runtimePropertyBundle = collectPropertyCandidates(properties, logs, Set.of());
        List<ProductModelCandidateVO> propertyCandidates = remapRuntimePropertiesByPreset(
                runtimePropertyBundle.candidates(),
                dto,
                normalizedNormativePresetCode
        );
        PropertyEvidenceBundle propertyBundle = new PropertyEvidenceBundle(
                propertyCandidates,
                runtimePropertyBundle.evidenceCount(),
                countNeedsReview(propertyCandidates)
        );
        EventEvidenceBundle eventBundle = collectEventCandidates(logs, Set.of());
        ServiceEvidenceBundle serviceBundle = collectServiceCandidates(product, Set.of());
        return buildCandidateResult(
                productId,
                existingModelCount,
                propertyBundle,
                eventBundle,
                serviceBundle,
                EXTRACTION_MODE_RUNTIME,
                null,
                null,
                0
        );
    }

    private ProductModelCandidateResultVO manualExtractGovernanceCandidates(Long productId,
                                                                            int existingModelCount,
                                                                            ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract) {
        ManualSampleSnapshot snapshot = parseManualSample(toManualExtractDTO(manualExtract));
        PropertyEvidenceBundle propertyBundle = collectManualPropertyCandidates(snapshot, Set.of());
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
                productId,
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

    private ProductModelManualExtractDTO toManualExtractDTO(ProductModelGovernanceCompareDTO.ManualExtractInput input) {
        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType(input.getSampleType());
        dto.setSamplePayload(input.getSamplePayload());
        return dto;
    }

    private boolean isNormativeMode(ProductModelGovernanceCompareDTO dto) {
        return dto != null
                && ProductModelNormativePresetRegistry.GOVERNANCE_MODE_NORMATIVE.equalsIgnoreCase(normalizeOptional(dto.getGovernanceMode()));
    }

    private boolean shouldLoadRuntimeCandidates(ProductModelGovernanceCompareDTO dto) {
        return dto == null || dto.getIncludeRuntimeCandidates() == null || dto.getIncludeRuntimeCandidates();
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

    private void mergeManualDraftItems(ProductModelCandidateResultVO result,
                                       List<ProductModelGovernanceCompareDTO.ManualDraftItem> items) {
        if (result == null || items == null || items.isEmpty()) {
            return;
        }
        Map<String, ProductModelCandidateVO> propertyMap = toCandidateMap(result.getPropertyCandidates());
        Map<String, ProductModelCandidateVO> eventMap = toCandidateMap(result.getEventCandidates());
        Map<String, ProductModelCandidateVO> serviceMap = toCandidateMap(result.getServiceCandidates());
        for (ProductModelGovernanceCompareDTO.ManualDraftItem item : items) {
            ProductModelCandidateVO candidate = toManualDraftCandidate(item);
            Map<String, ProductModelCandidateVO> targetMap = resolveCandidateMap(propertyMap, eventMap, serviceMap, candidate.getModelType());
            String key = governanceCandidateKey(candidate.getModelType(), candidate.getIdentifier());
            ProductModelCandidateVO existing = targetMap.get(key);
            targetMap.put(key, existing == null ? candidate : mergeManualCandidate(existing, candidate));
        }
        result.setPropertyCandidates(sortGovernanceCandidates(propertyMap.values()));
        result.setEventCandidates(sortGovernanceCandidates(eventMap.values()));
        result.setServiceCandidates(sortGovernanceCandidates(serviceMap.values()));
    }

    private Map<String, ProductModelCandidateVO> resolveCandidateMap(Map<String, ProductModelCandidateVO> propertyMap,
                                                                     Map<String, ProductModelCandidateVO> eventMap,
                                                                     Map<String, ProductModelCandidateVO> serviceMap,
                                                                     String modelType) {
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return eventMap;
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return serviceMap;
        }
        return propertyMap;
    }

    private Map<String, ProductModelCandidateVO> toCandidateMap(List<ProductModelCandidateVO> candidates) {
        Map<String, ProductModelCandidateVO> map = new LinkedHashMap<>();
        for (ProductModelCandidateVO candidate : candidates == null ? List.<ProductModelCandidateVO>of() : candidates) {
            map.put(governanceCandidateKey(candidate.getModelType(), candidate.getIdentifier()), candidate);
        }
        return map;
    }

    private List<ProductModelCandidateVO> remapRuntimePropertiesByPreset(List<ProductModelCandidateVO> runtimeCandidates,
                                                                         ProductModelGovernanceCompareDTO dto,
                                                                         String normalizedNormativePresetCode) {
        if (!isNormativeMode(dto) || normalizedNormativePresetCode == null) {
            return runtimeCandidates;
        }
        Map<String, ProductModelCandidateVO> remapped = new LinkedHashMap<>();
        for (ProductModelCandidateVO candidate : runtimeCandidates) {
            ProductModelCandidateVO normalizedCandidate = normativePresetRegistry
                    .findNormativeIdentifier(normalizedNormativePresetCode, candidate.getIdentifier())
                    .map(identifier -> applyNormativeIdentifier(candidate, identifier, normalizedNormativePresetCode))
                    .orElse(candidate);
            String key = governanceCandidateKey(normalizedCandidate.getModelType(), normalizedCandidate.getIdentifier());
            ProductModelCandidateVO existing = remapped.get(key);
            remapped.put(key, existing == null ? normalizedCandidate : mergeManualCandidate(existing, normalizedCandidate));
        }
        return sortGovernanceCandidates(remapped.values());
    }

    private String normalizeAndValidateNormativePresetCode(Product product, ProductModelGovernanceCompareDTO dto) {
        String normalizedPresetCode = normalizeRequired(
                dto == null ? null : normalizeOptional(dto.getNormativePresetCode()),
                "规范预设编码"
        ).toLowerCase(Locale.ROOT);
        if (!normativePresetRegistry.isPresetApplicable(
                normalizedPresetCode,
                product == null ? null : product.getProductKey(),
                product == null ? null : product.getProductName())) {
            throw new BizException("当前产品不适用规范预设: " + normalizedPresetCode);
        }
        return normalizedPresetCode;
    }

    private String governanceCandidateKey(String modelType, String identifier) {
        return normalizeModelType(modelType) + "::" + normalizeRequired(identifier, "物模型标识");
    }

    private List<ProductModelCandidateVO> sortGovernanceCandidates(Collection<ProductModelCandidateVO> candidates) {
        return candidates.stream()
                .sorted(Comparator
                        .comparing(ProductModelCandidateVO::getNeedsReview, Comparator.nullsLast(Boolean::compareTo))
                        .thenComparing(ProductModelCandidateVO::getGroupKey, Comparator.nullsLast(String::compareTo))
                        .thenComparing(ProductModelCandidateVO::getIdentifier, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private ProductModelCandidateVO mergeManualCandidate(ProductModelCandidateVO existing, ProductModelCandidateVO overlay) {
        ProductModelCandidateVO merged = new ProductModelCandidateVO();
        merged.setModelType(existing.getModelType());
        merged.setIdentifier(existing.getIdentifier());
        merged.setModelName(firstNonNull(overlay.getModelName(), existing.getModelName()));
        merged.setDataType(firstNonNull(overlay.getDataType(), existing.getDataType()));
        merged.setSpecsJson(firstNonNull(overlay.getSpecsJson(), existing.getSpecsJson()));
        merged.setEventType(firstNonNull(overlay.getEventType(), existing.getEventType()));
        merged.setServiceInputJson(firstNonNull(overlay.getServiceInputJson(), existing.getServiceInputJson()));
        merged.setServiceOutputJson(firstNonNull(overlay.getServiceOutputJson(), existing.getServiceOutputJson()));
        merged.setSortNo(firstNonNull(overlay.getSortNo(), existing.getSortNo()));
        merged.setRequiredFlag(firstNonNull(overlay.getRequiredFlag(), existing.getRequiredFlag()));
        merged.setDescription(firstNonNull(overlay.getDescription(), existing.getDescription()));
        merged.setGroupKey(firstNonNull(overlay.getGroupKey(), existing.getGroupKey()));
        merged.setEvidenceOrigin(firstNonNull(overlay.getEvidenceOrigin(), existing.getEvidenceOrigin()));
        merged.setUnit(firstNonNull(overlay.getUnit(), existing.getUnit()));
        merged.setNormativeSource(firstNonNull(overlay.getNormativeSource(), existing.getNormativeSource()));
        merged.setRawIdentifiers(mergeStringList(existing.getRawIdentifiers(), overlay.getRawIdentifiers()));
        merged.setMonitorContentCode(firstNonNull(overlay.getMonitorContentCode(), existing.getMonitorContentCode()));
        merged.setMonitorTypeCode(firstNonNull(overlay.getMonitorTypeCode(), existing.getMonitorTypeCode()));
        merged.setSensorCode(firstNonNull(overlay.getSensorCode(), existing.getSensorCode()));
        merged.setConfidence(firstNonNull(overlay.getConfidence(), existing.getConfidence()));
        merged.setNeedsReview(Boolean.TRUE.equals(existing.getNeedsReview()) || Boolean.TRUE.equals(overlay.getNeedsReview()));
        merged.setCandidateStatus(Boolean.TRUE.equals(merged.getNeedsReview()) ? STATUS_NEEDS_REVIEW : STATUS_READY);
        merged.setReviewReason(firstNonNull(overlay.getReviewReason(), existing.getReviewReason()));
        merged.setEvidenceCount(firstNonNull(existing.getEvidenceCount(), 0) + firstNonNull(overlay.getEvidenceCount(), 0));
        merged.setMessageEvidenceCount(firstNonNull(existing.getMessageEvidenceCount(), 0)
                + firstNonNull(overlay.getMessageEvidenceCount(), 0));
        merged.setLastReportTime(resolveLaterTime(existing.getLastReportTime(), overlay.getLastReportTime()));
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (existing.getSourceTables() != null) {
            sources.addAll(existing.getSourceTables());
        }
        if (overlay.getSourceTables() != null) {
            sources.addAll(overlay.getSourceTables());
        }
        merged.setSourceTables(new ArrayList<>(sources));
        return merged;
    }

    private ProductModelCandidateVO toNormativeCandidate(ProductModelGovernanceEvidenceVO definition) {
        String identifier = normalizeRequired(definition.getIdentifier(), "物模型标识");
        String groupKey = classifyPropertyGroup(identifier);
        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(firstNonNull(definition.getModelType(), MODEL_TYPE_PROPERTY));
        candidate.setIdentifier(identifier);
        candidate.setModelName(definition.getModelName());
        candidate.setDataType(definition.getDataType());
        candidate.setSpecsJson(definition.getSpecsJson());
        candidate.setSortNo(firstNonNull(definition.getSortNo(), defaultSortNo(groupKey)));
        candidate.setRequiredFlag(firstNonNull(definition.getRequiredFlag(), 0));
        candidate.setDescription(firstNonNull(definition.getDescription(), "来源于规范字段预设，待与真实报文证据核对后再进入正式契约。"));
        candidate.setGroupKey(groupKey);
        candidate.setEvidenceOrigin(ProductModelNormativePresetRegistry.GOVERNANCE_MODE_NORMATIVE);
        candidate.setUnit(definition.getUnit());
        candidate.setNormativeSource(definition.getNormativeSource());
        candidate.setRawIdentifiers(definition.getRawIdentifiers());
        candidate.setMonitorContentCode(definition.getMonitorContentCode());
        candidate.setMonitorTypeCode(definition.getMonitorTypeCode());
        candidate.setSensorCode(definition.getSensorCode());
        candidate.setConfidence(0.99D);
        candidate.setNeedsReview(Boolean.FALSE);
        candidate.setCandidateStatus(STATUS_READY);
        candidate.setReviewReason(null);
        candidate.setEvidenceCount(1);
        candidate.setMessageEvidenceCount(0);
        candidate.setSourceTables(List.of("normative_preset"));
        return candidate;
    }

    private ProductModelCandidateVO applyNormativeIdentifier(ProductModelCandidateVO runtimeCandidate,
                                                             String normativeIdentifier,
                                                             String presetCode) {
        ProductModelGovernanceEvidenceVO definition = normativePresetRegistry.findPropertyDefinition(presetCode, normativeIdentifier)
                .orElseThrow(() -> new BizException("规范字段不存在: " + normativeIdentifier));
        String groupKey = classifyPropertyGroup(normativeIdentifier);
        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(runtimeCandidate.getModelType());
        candidate.setIdentifier(normativeIdentifier);
        candidate.setModelName(firstNonNull(definition.getModelName(), runtimeCandidate.getModelName()));
        candidate.setDataType(firstNonNull(definition.getDataType(), runtimeCandidate.getDataType()));
        candidate.setSpecsJson(firstNonNull(definition.getSpecsJson(), runtimeCandidate.getSpecsJson()));
        candidate.setSortNo(firstNonNull(definition.getSortNo(), runtimeCandidate.getSortNo()));
        candidate.setRequiredFlag(firstNonNull(definition.getRequiredFlag(), runtimeCandidate.getRequiredFlag()));
        candidate.setDescription(firstNonNull(definition.getDescription(), runtimeCandidate.getDescription()));
        candidate.setGroupKey(groupKey);
        candidate.setEvidenceOrigin("runtime");
        candidate.setUnit(definition.getUnit());
        candidate.setRawIdentifiers(runtimeCandidate.getIdentifier().equals(normativeIdentifier)
                ? runtimeCandidate.getRawIdentifiers()
                : List.of(runtimeCandidate.getIdentifier()));
        candidate.setMonitorContentCode(definition.getMonitorContentCode());
        candidate.setMonitorTypeCode(definition.getMonitorTypeCode());
        candidate.setSensorCode(definition.getSensorCode());
        candidate.setConfidence(resolveConfidence(groupKey, false, firstNonNull(runtimeCandidate.getMessageEvidenceCount(), 0) > 0));
        candidate.setNeedsReview(Boolean.FALSE);
        candidate.setCandidateStatus(STATUS_READY);
        candidate.setReviewReason(null);
        candidate.setEvidenceCount(runtimeCandidate.getEvidenceCount());
        candidate.setMessageEvidenceCount(runtimeCandidate.getMessageEvidenceCount());
        candidate.setLastReportTime(runtimeCandidate.getLastReportTime());
        candidate.setSourceTables(runtimeCandidate.getSourceTables() == null
                ? List.of()
                : new ArrayList<>(runtimeCandidate.getSourceTables()));
        return candidate;
    }

    private ProductModelCandidateVO toManualDraftCandidate(ProductModelGovernanceCompareDTO.ManualDraftItem item) {
        String modelType = normalizeModelType(normalizeOptional(item.getModelType()) == null ? MODEL_TYPE_PROPERTY : item.getModelType());
        String identifier = normalizeRequired(item.getIdentifier(), "物模型标识");
        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(modelType);
        candidate.setIdentifier(identifier);
        candidate.setModelName(resolveManualDraftModelName(modelType, identifier, item.getModelName()));
        candidate.setDataType(MODEL_TYPE_PROPERTY.equals(modelType)
                ? normalizeOptional(item.getDataType())
                : null);
        candidate.setSpecsJson(MODEL_TYPE_PROPERTY.equals(modelType)
                ? validateJsonField(item.getSpecsJson(), "specsJson")
                : null);
        candidate.setEventType(MODEL_TYPE_EVENT.equals(modelType) ? normalizeOptional(item.getEventType()) : null);
        candidate.setServiceInputJson(MODEL_TYPE_SERVICE.equals(modelType)
                ? validateJsonField(item.getServiceInputJson(), "serviceInputJson")
                : null);
        candidate.setServiceOutputJson(MODEL_TYPE_SERVICE.equals(modelType)
                ? validateJsonField(item.getServiceOutputJson(), "serviceOutputJson")
                : null);
        candidate.setSortNo(resolveManualDraftSortNo(modelType, identifier));
        candidate.setRequiredFlag(0);
        candidate.setDescription(resolveManualDraftDescription(modelType, identifier, item.getDescription()));
        candidate.setGroupKey(resolveManualDraftGroupKey(modelType, identifier));
        candidate.setEvidenceOrigin("manual_draft");
        boolean needsReview = MODEL_TYPE_PROPERTY.equals(modelType)
                && (isSuspiciousIdentifier(identifier) || "unknown".equals(candidate.getGroupKey()));
        candidate.setConfidence(resolveManualDraftConfidence(modelType, candidate.getGroupKey(), needsReview));
        candidate.setNeedsReview(needsReview);
        candidate.setCandidateStatus(needsReview ? STATUS_NEEDS_REVIEW : STATUS_READY);
        candidate.setReviewReason(needsReview ? "命名需人工归一后再入正式契约" : null);
        candidate.setEvidenceCount(1);
        candidate.setMessageEvidenceCount(0);
        candidate.setLastReportTime(LocalDateTime.now());
        candidate.setSourceTables(new ArrayList<>(List.of("manual_draft")));
        return candidate;
    }

    private String resolveManualDraftModelName(String modelType, String identifier, String modelName) {
        String normalizedName = normalizeOptional(modelName);
        if (normalizedName != null) {
            return normalizedName;
        }
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return suggestEventName(identifier);
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return suggestServiceName(identifier);
        }
        String groupKey = classifyPropertyGroup(identifier);
        return suggestPropertyModelName(identifier, null, groupKey);
    }

    private Integer resolveManualDraftSortNo(String modelType, String identifier) {
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return 410;
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return 510;
        }
        return defaultSortNo(classifyPropertyGroup(identifier));
    }

    private String resolveManualDraftDescription(String modelType, String identifier, String description) {
        String normalizedDescription = normalizeOptional(description);
        if (normalizedDescription != null) {
            return normalizedDescription;
        }
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return "来源于人工补录候选，建议与真实事件证据对照后再纳入正式契约。";
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return "来源于人工补录候选，建议补齐服务入参与回执结构后再纳入正式契约。";
        }
        return buildPropertyDescription(
                identifier,
                suggestPropertyModelName(identifier, null, classifyPropertyGroup(identifier)),
                classifyPropertyGroup(identifier),
                isSuspiciousIdentifier(identifier) || "unknown".equals(classifyPropertyGroup(identifier)),
                EXTRACTION_MODE_MANUAL
        );
    }

    private String resolveManualDraftGroupKey(String modelType, String identifier) {
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return "event";
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return "service";
        }
        return classifyPropertyGroup(identifier);
    }

    private Double resolveManualDraftConfidence(String modelType, String groupKey, boolean needsReview) {
        if (MODEL_TYPE_EVENT.equals(modelType)) {
            return 0.78D;
        }
        if (MODEL_TYPE_SERVICE.equals(modelType)) {
            return 0.74D;
        }
        return resolveConfidence(groupKey, needsReview, false);
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

    private List<String> mergeStringList(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return merged.isEmpty() ? null : new ArrayList<>(merged);
    }

    private LocalDateTime resolveLaterTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.isAfter(first) ? second : first;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }

    private List<ProductModelGovernanceApplyDTO.ApplyItem> safeApplyItems(ProductModelGovernanceApplyDTO dto) {
        return dto == null || dto.getItems() == null ? List.of() : dto.getItems();
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

    private PropertyEvidenceBundle collectManualPropertyCandidates(ManualSampleSnapshot snapshot, Set<String> existingIdentifiers) {
        Map<String, PropertyAccumulator> accumulators = new LinkedHashMap<>();
        int evidenceCount = 0;
        for (ManualLeafEvidence evidence : snapshot.leaves()) {
            evidenceCount++;
            String identifier = normalizeOptional(evidence.identifier());
            if (identifier == null || existingIdentifiers.contains(identifier)) {
                continue;
            }
            accumulators.computeIfAbsent(identifier, PropertyAccumulator::new)
                    .acceptManualSample(snapshot.sampleType(), LocalDateTime.now(), evidence);
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

    private ManualSampleSnapshot parseManualSample(ProductModelManualExtractDTO dto) {
        String sampleType = normalizeSampleType(dto == null ? null : dto.getSampleType());
        String normalizedPayload = normalizeOptional(JsonPayloadUtils.normalizeJsonDocument(dto == null ? null : dto.getSamplePayload()));
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
        if (!Set.of(SAMPLE_TYPE_BUSINESS, SAMPLE_TYPE_STATUS, SAMPLE_TYPE_OTHER).contains(normalized)) {
            throw new BizException("样本类型不支持: " + normalized);
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

    private PropertyEvidenceBundle collectPropertyCandidates(List<DeviceProperty> properties,
                                                             List<DeviceMessageLog> logs,
                                                             Set<String> existingIdentifiers) {
        Map<String, PropertyAccumulator> accumulators = new LinkedHashMap<>();
        int evidenceCount = 0;
        for (DeviceProperty property : properties) {
            String identifier = normalizeOptional(property.getIdentifier());
            if (identifier == null || existingIdentifiers.contains(identifier)) {
                continue;
            }
            evidenceCount++;
            accumulators.computeIfAbsent(identifier, PropertyAccumulator::new).acceptProperty(property);
        }

        for (DeviceMessageLog log : logs) {
            if (!isPropertyLog(log)) {
                continue;
            }
            Map<String, String> extracted = extractPropertyLeaves(log.getPayload());
            for (Map.Entry<String, String> entry : extracted.entrySet()) {
                String identifier = normalizeOptional(entry.getKey());
                if (identifier == null || existingIdentifiers.contains(identifier)) {
                    continue;
                }
                accumulators.computeIfAbsent(identifier, PropertyAccumulator::new)
                        .acceptLog(resolveLogTime(log), entry.getValue());
            }
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

    private EventEvidenceBundle collectEventCandidates(List<DeviceMessageLog> logs, Set<String> existingIdentifiers) {
        Map<String, EventAccumulator> accumulators = new LinkedHashMap<>();
        int evidenceCount = 0;
        for (DeviceMessageLog log : logs) {
            if (!isEventLog(log)) {
                continue;
            }
            evidenceCount++;
            String identifier = extractEventIdentifier(log);
            if (!StringUtils.hasText(identifier) || existingIdentifiers.contains(identifier)) {
                continue;
            }
            accumulators.computeIfAbsent(identifier, EventAccumulator::new).accept(resolveLogTime(log));
        }

        List<ProductModelCandidateVO> candidates = accumulators.values().stream()
                .map(this::toEventCandidate)
                .sorted(Comparator.comparing(ProductModelCandidateVO::getIdentifier))
                .toList();
        String hint = candidates.isEmpty()
                ? "暂无真实事件证据，当前产品最近 30 天未发现稳定事件上报。"
                : null;
        return new EventEvidenceBundle(candidates, evidenceCount, hint);
    }

    private ServiceEvidenceBundle collectServiceCandidates(Product product, Set<String> existingIdentifiers) {
        try {
            List<CommandRecord> records = commandRecordMapper.selectList(
                    new LambdaQueryWrapper<CommandRecord>()
                            .eq(CommandRecord::getProductKey, product.getProductKey())
                            .orderByDesc(CommandRecord::getSendTime)
            );
            Map<String, ServiceAccumulator> accumulators = new LinkedHashMap<>();
            int evidenceCount = 0;
            for (CommandRecord record : records) {
                evidenceCount++;
                String commandType = normalizeOptional(record.getCommandType());
                String serviceIdentifier = normalizeOptional(record.getServiceIdentifier());
                if (!StringUtils.hasText(commandType)
                        || MODEL_TYPE_PROPERTY.equalsIgnoreCase(commandType)
                        || !StringUtils.hasText(serviceIdentifier)
                        || existingIdentifiers.contains(serviceIdentifier)) {
                    continue;
                }
                accumulators.computeIfAbsent(serviceIdentifier, ServiceAccumulator::new).accept(record);
            }

            List<ProductModelCandidateVO> candidates = accumulators.values().stream()
                    .map(this::toServiceCandidate)
                    .sorted(Comparator.comparing(ProductModelCandidateVO::getIdentifier))
                    .toList();
            String hint = candidates.isEmpty()
                    ? "暂无稳定服务命令证据，当前真实库仍以属性下发为主，暂不自动生成服务模型。"
                    : null;
            return new ServiceEvidenceBundle(candidates, evidenceCount, hint);
        } catch (Exception ex) {
            return new ServiceEvidenceBundle(
                    List.of(),
                    0,
                    "当前真实库 iot_command_record 字段仍未完全对齐服务标识，暂不自动生成服务模型。"
            );
        }
    }

    private ProductModelCandidateVO toPropertyCandidate(PropertyAccumulator accumulator) {
        String groupKey = classifyPropertyGroup(accumulator.identifier);
        boolean manualOtherSample = SAMPLE_TYPE_OTHER.equals(accumulator.sampleType);
        boolean needsReview = manualOtherSample
                || isSuspiciousIdentifier(accumulator.identifier)
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
        candidate.setEvidenceOrigin("runtime");
        candidate.setConfidence(resolveConfidence(groupKey, needsReview, accumulator.messageEvidenceCount > 0));
        candidate.setNeedsReview(needsReview);
        candidate.setCandidateStatus(needsReview ? STATUS_NEEDS_REVIEW : STATUS_READY);
        candidate.setReviewReason(resolvePropertyReviewReason(needsReview, accumulator.sampleType));
        candidate.setEvidenceCount(accumulator.evidenceCount);
        candidate.setMessageEvidenceCount(accumulator.messageEvidenceCount);
        candidate.setLastReportTime(accumulator.lastReportTime);
        candidate.setSourceTables(new ArrayList<>(accumulator.sourceTables));
        return candidate;
    }

    private ProductModelCandidateVO toEventCandidate(EventAccumulator accumulator) {
        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(MODEL_TYPE_EVENT);
        candidate.setIdentifier(accumulator.identifier);
        candidate.setModelName(suggestEventName(accumulator.identifier));
        candidate.setEventType("info");
        candidate.setSortNo(410);
        candidate.setRequiredFlag(0);
        candidate.setDescription("来源于真实事件上报，建议在补齐事件级别与处理语义后再写入正式契约。");
        candidate.setGroupKey("event");
        candidate.setEvidenceOrigin("runtime");
        candidate.setConfidence(0.55D);
        candidate.setNeedsReview(Boolean.TRUE);
        candidate.setCandidateStatus(STATUS_NEEDS_REVIEW);
        candidate.setReviewReason("当前事件命名和级别语义仍需人工确认");
        candidate.setEvidenceCount(accumulator.evidenceCount);
        candidate.setMessageEvidenceCount(accumulator.evidenceCount);
        candidate.setLastReportTime(accumulator.lastReportTime);
        candidate.setSourceTables(List.of("iot_device_message_log"));
        return candidate;
    }

    private ProductModelCandidateVO toServiceCandidate(ServiceAccumulator accumulator) {
        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(MODEL_TYPE_SERVICE);
        candidate.setIdentifier(accumulator.identifier);
        candidate.setModelName(suggestServiceName(accumulator.identifier));
        candidate.setServiceInputJson("[]");
        candidate.setServiceOutputJson("[]");
        candidate.setSortNo(510);
        candidate.setRequiredFlag(0);
        candidate.setDescription("来源于真实命令记录，但当前入参与回执字段尚不稳定，建议人工确认后再写入正式契约。");
        candidate.setGroupKey("service");
        candidate.setEvidenceOrigin("runtime");
        candidate.setConfidence(0.45D);
        candidate.setNeedsReview(Boolean.TRUE);
        candidate.setCandidateStatus(STATUS_NEEDS_REVIEW);
        candidate.setReviewReason("服务标识和入参结构仍需人工确认");
        candidate.setEvidenceCount(accumulator.evidenceCount);
        candidate.setMessageEvidenceCount(0);
        candidate.setLastReportTime(accumulator.lastReportTime);
        candidate.setSourceTables(List.of("iot_command_record"));
        return candidate;
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
        String messageType = normalizeOptional(log.getMessageType());
        return messageType != null && PROPERTY_LOG_TYPES.contains(messageType.toLowerCase(Locale.ROOT));
    }

    private boolean isEventLog(DeviceMessageLog log) {
        String messageType = normalizeOptional(log.getMessageType());
        if (messageType != null && MODEL_TYPE_EVENT.equalsIgnoreCase(messageType)) {
            return true;
        }
        String topic = normalizeOptional(log.getTopic());
        return topic != null && topic.contains("/event/");
    }

    private String extractEventIdentifier(DeviceMessageLog log) {
        String topic = normalizeOptional(log.getTopic());
        if (topic != null) {
            Matcher matcher = EVENT_TOPIC_PATTERN.matcher(topic);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        String normalizedPayload = normalizeOptional(JsonPayloadUtils.normalizeJsonDocument(log.getPayload()));
        if (normalizedPayload == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(normalizedPayload);
            String[] keys = {"event", "eventType", "identifier", "name"};
            for (String key : keys) {
                JsonNode value = root.get(key);
                if (value != null && value.isValueNode() && StringUtils.hasText(value.asText())) {
                    return value.asText().trim();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
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

    private ProductModelUpsertDTO toUpsertDTO(ProductModelCandidateConfirmDTO.ProductModelCandidateConfirmItem item, int nextSortNo) {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType(normalizeOptional(item.getModelType()) == null ? MODEL_TYPE_PROPERTY : item.getModelType());
        dto.setIdentifier(item.getIdentifier());
        dto.setModelName(item.getModelName());
        dto.setDataType(item.getDataType());
        dto.setSpecsJson(item.getSpecsJson());
        dto.setEventType(item.getEventType());
        dto.setServiceInputJson(item.getServiceInputJson());
        dto.setServiceOutputJson(item.getServiceOutputJson());
        dto.setSortNo(item.getSortNo() == null ? nextSortNo : item.getSortNo());
        dto.setRequiredFlag(item.getRequiredFlag() == null ? 0 : item.getRequiredFlag());
        dto.setDescription(item.getDescription());
        return dto;
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

    private Set<String> toIdentifierSet(List<String> identifiers) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String identifier : identifiers) {
            String value = normalizeOptional(identifier);
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized;
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
                || normalized.contains("sensor_state")
                || normalized.contains("sw_version")
                || normalized.endsWith(".temp")
                || "temp".equals(lastSegment)) {
            return "device_status";
        }
        if (normalized.startsWith("l")
                && (TELEMETRY_LAST_SEGMENTS.contains(lastSegment)
                || normalized.contains("disps"))) {
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
        return identifier.toLowerCase(Locale.ROOT).contains("sensor_state.");
    }

    private String resolveSensorStateLabel(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
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
        if (SAMPLE_TYPE_OTHER.equals(sampleType)) {
            return "来源于手动录入的其他数据样本，当前字段 "
                    + identifier
                    + " 默认按待人工确认处理，确认归类后再写入正式契约。";
        }
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
        if (SAMPLE_TYPE_OTHER.equals(sampleType)) {
            return "来源类别为其他数据，默认需人工确认后再入正式契约";
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

    private LocalDateTime resolveLogTime(DeviceMessageLog log) {
        return log.getReportTime() != null ? log.getReportTime() : log.getCreateTime();
    }

    private String suggestEventName(String identifier) {
        return identifier + "事件";
    }

    private String suggestServiceName(String identifier) {
        return identifier + "服务";
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

    private record ManualLeafEvidence(String identifier, String sampleValue, String valueType) {
    }

    private static final class ManualLeafCollector {
        private final List<ManualLeafEvidence> leaves = new ArrayList<>();
        private int ignoredFieldCount;

        private void add(String identifier, String sampleValue, String valueType) {
            leaves.add(new ManualLeafEvidence(identifier, sampleValue, valueType));
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

        private PropertyAccumulator(String identifier) {
            this.identifier = identifier;
        }

        private void acceptProperty(DeviceProperty property) {
            evidenceCount++;
            sourceTables.add("iot_device_property");
            if (normalizeText(property.getPropertyName()) != null) {
                propertyName = normalizeText(property.getPropertyName());
            }
            if (normalizeText(property.getValueType()) != null) {
                valueType = normalizeText(property.getValueType());
            }
            if (normalizeText(property.getPropertyValue()) != null) {
                sampleValue = normalizeText(property.getPropertyValue());
            }
            updateLastReportTime(property.getReportTime() != null ? property.getReportTime() : property.getUpdateTime());
        }

        private void acceptLog(LocalDateTime logTime, String sampleValue) {
            messageEvidenceCount++;
            sourceTables.add("iot_device_message_log");
            if (normalizeText(sampleValue) != null) {
                this.sampleValue = normalizeText(sampleValue);
            }
            updateLastReportTime(logTime);
        }

        private void acceptManualSample(String sampleType, LocalDateTime reportTime, ManualLeafEvidence evidence) {
            evidenceCount++;
            messageEvidenceCount++;
            sourceTables.add("manual_sample");
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

    private static final class EventAccumulator {
        private final String identifier;
        private int evidenceCount;
        private LocalDateTime lastReportTime;

        private EventAccumulator(String identifier) {
            this.identifier = identifier;
        }

        private void accept(LocalDateTime reportTime) {
            evidenceCount++;
            if (lastReportTime == null || (reportTime != null && reportTime.isAfter(lastReportTime))) {
                lastReportTime = reportTime;
            }
        }
    }

    private static final class ServiceAccumulator {
        private final String identifier;
        private int evidenceCount;
        private LocalDateTime lastReportTime;

        private ServiceAccumulator(String identifier) {
            this.identifier = identifier;
        }

        private void accept(CommandRecord record) {
            evidenceCount++;
            if (lastReportTime == null || (record.getSendTime() != null && record.getSendTime().isAfter(lastReportTime))) {
                lastReportTime = record.getSendTime();
            }
        }
    }
}
