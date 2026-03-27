package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
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
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateVO;
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
    private static final int EXTRACTION_WINDOW_DAYS = 30;
    private static final Pattern EVENT_TOPIC_PATTERN = Pattern.compile("/event/([^/]+)/?");
    private static final Pattern POINT_IDENTIFIER_PATTERN = Pattern.compile("^L(\\d+)_([A-Z]+)_\\d+$");
    private static final Set<String> PROPERTY_LOG_TYPES = Set.of("property", "status");
    private static final Set<String> ROOT_WRAPPER_KEYS = Set.of(
            "properties",
            "property",
            "status",
            "telemetry",
            "data",
            "params",
            "payload",
            "reported"
    );
    private static final Set<String> IGNORED_ROOT_KEYS = Set.of(
            "messageType",
            "deviceCode",
            "productKey",
            "traceId",
            "protocolCode",
            "timestamp",
            "time",
            "reportTime"
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

        ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
        summary.setPropertyEvidenceCount(propertyBundle.evidenceCount());
        summary.setPropertyCandidateCount(propertyBundle.candidates().size());
        summary.setEventEvidenceCount(eventBundle.evidenceCount());
        summary.setEventCandidateCount(eventBundle.candidates().size());
        summary.setServiceEvidenceCount(serviceBundle.evidenceCount());
        summary.setServiceCandidateCount(serviceBundle.candidates().size());
        summary.setNeedsReviewCount(propertyBundle.needsReviewCount()
                + countNeedsReview(eventBundle.candidates())
                + countNeedsReview(serviceBundle.candidates()));
        summary.setExistingModelCount(existingModels.size());
        summary.setCreatedCount(0);
        summary.setSkippedCount(0);
        summary.setConflictCount(0);
        summary.setEventHint(eventBundle.hint());
        summary.setServiceHint(serviceBundle.hint());
        summary.setLastExtractedAt(LocalDateTime.now());

        ProductModelCandidateResultVO result = new ProductModelCandidateResultVO();
        result.setProductId(productId);
        result.setSummary(summary);
        result.setPropertyCandidates(propertyBundle.candidates());
        result.setEventCandidates(eventBundle.candidates());
        result.setServiceCandidates(serviceBundle.candidates());
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
        boolean needsReview = isSuspiciousIdentifier(accumulator.identifier) || "unknown".equals(groupKey);
        String modelName = suggestPropertyModelName(accumulator.identifier, accumulator.propertyName, groupKey);
        String description = buildPropertyDescription(accumulator.identifier, modelName, groupKey, needsReview);

        ProductModelCandidateVO candidate = new ProductModelCandidateVO();
        candidate.setModelType(MODEL_TYPE_PROPERTY);
        candidate.setIdentifier(accumulator.identifier);
        candidate.setModelName(modelName);
        candidate.setDataType(resolvePropertyDataType(accumulator.valueType, accumulator.sampleValue, accumulator.identifier));
        candidate.setSortNo(defaultSortNo(groupKey));
        candidate.setRequiredFlag(0);
        candidate.setDescription(description);
        candidate.setGroupKey(groupKey);
        candidate.setConfidence(resolveConfidence(groupKey, needsReview, accumulator.messageEvidenceCount > 0));
        candidate.setNeedsReview(needsReview);
        candidate.setCandidateStatus(needsReview ? STATUS_NEEDS_REVIEW : STATUS_READY);
        candidate.setReviewReason(needsReview ? "命名需人工归一后再入正式契约" : null);
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
            if (rootLevel && IGNORED_ROOT_KEYS.contains(fieldName)) {
                return;
            }
            boolean unwrapRoot = rootLevel && ROOT_WRAPPER_KEYS.contains(fieldName.toLowerCase(Locale.ROOT));
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

    private String buildPropertyDescription(String identifier, String modelName, String groupKey, boolean needsReview) {
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

    private static final class PropertyAccumulator {
        private final String identifier;
        private String propertyName;
        private String valueType;
        private String sampleValue;
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
