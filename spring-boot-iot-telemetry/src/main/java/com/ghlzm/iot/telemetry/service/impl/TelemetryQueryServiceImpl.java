package com.ghlzm.iot.telemetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import com.ghlzm.iot.telemetry.service.TelemetryQueryService;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchSeries;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBucketPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryStreamKind;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 时序查询服务实现。
 */
@Service
public class TelemetryQueryServiceImpl implements TelemetryQueryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryQueryServiceImpl.class);
    private static final int HISTORY_BATCH_SIZE = 10_000;
    private static final String FILL_POLICY_ZERO = "ZERO";
    private static final DateTimeFormatter BUCKET_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;
    private final DevicePropertyMapper devicePropertyMapper;
    private final DevicePropertyMetadataService devicePropertyMetadataService;
    private final DeviceTelemetryMappingService deviceTelemetryMappingService;
    private final TdengineTelemetryFacade tdengineTelemetryFacade;
    private final TelemetryStorageModeResolver storageModeResolver;
    private final TelemetryReadRouter telemetryReadRouter;
    private final TelemetryLatestProjectionRepository telemetryLatestProjectionRepository;
    private final NormalizedTelemetryHistoryReader normalizedTelemetryHistoryReader;
    private final LegacyTelemetryHistoryReader legacyTelemetryHistoryReader;
    private final TelemetryRawHistoryReader telemetryRawHistoryReader;

    public TelemetryQueryServiceImpl(DeviceMapper deviceMapper,
                                     ProductMapper productMapper,
                                     DevicePropertyMapper devicePropertyMapper,
                                     DevicePropertyMetadataService devicePropertyMetadataService,
                                     DeviceTelemetryMappingService deviceTelemetryMappingService,
                                     TdengineTelemetryFacade tdengineTelemetryFacade,
                                     TelemetryStorageModeResolver storageModeResolver,
                                     TelemetryReadRouter telemetryReadRouter,
                                     TelemetryLatestProjectionRepository telemetryLatestProjectionRepository,
                                     NormalizedTelemetryHistoryReader normalizedTelemetryHistoryReader,
                                     LegacyTelemetryHistoryReader legacyTelemetryHistoryReader,
                                     TelemetryRawHistoryReader telemetryRawHistoryReader) {
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
        this.devicePropertyMapper = devicePropertyMapper;
        this.devicePropertyMetadataService = devicePropertyMetadataService;
        this.deviceTelemetryMappingService = deviceTelemetryMappingService;
        this.tdengineTelemetryFacade = tdengineTelemetryFacade;
        this.storageModeResolver = storageModeResolver;
        this.telemetryReadRouter = telemetryReadRouter;
        this.telemetryLatestProjectionRepository = telemetryLatestProjectionRepository;
        this.normalizedTelemetryHistoryReader = normalizedTelemetryHistoryReader;
        this.legacyTelemetryHistoryReader = legacyTelemetryHistoryReader;
        this.telemetryRawHistoryReader = telemetryRawHistoryReader;
    }

    @Override
    public Map<String, Object> getLatest(Long deviceId) {
        Device device = requireDevice(deviceId);
        Product product = device.getProductId() == null ? null : productMapper.selectById(device.getProductId());
        if (storageModeResolver.isTdengineEnabled()) {
            return buildTdengineResponse(device, product);
        }
        return buildMysqlResponse(device, product);
    }

    @Override
    public TelemetryHistoryBatchResponse getHistoryBatch(TelemetryHistoryBatchRequest request) {
        if (!storageModeResolver.isTdengineEnabled()) {
            throw new BizException("当前环境未启用 TDengine 历史查询");
        }
        if (request == null || request.getDeviceId() == null) {
            throw new BizException("deviceId 不能为空");
        }
        List<String> identifiers = normalizeIdentifiers(request.getIdentifiers());
        if (identifiers.isEmpty()) {
            throw new BizException("identifiers 不能为空");
        }
        String fillPolicy = normalizeFillPolicy(request.getFillPolicy());
        if (!FILL_POLICY_ZERO.equals(fillPolicy)) {
            throw new BizException("当前仅支持 ZERO 补零策略");
        }
        Device device = requireDevice(request.getDeviceId());
        Product product = device.getProductId() == null ? null : productMapper.selectById(device.getProductId());
        Map<String, DevicePropertyMetadata> metadataMap = device.getProductId() == null
                ? Map.of()
                : devicePropertyMetadataService.listPropertyMetadataMap(device.getProductId());
        Map<String, TelemetryMetricMapping> mappingMap = device.getProductId() == null
                ? Map.of()
                : deviceTelemetryMappingService.listMetricMappingMap(device.getProductId());
        RangeDefinition rangeDefinition = resolveRangeDefinition(request.getRangeCode());
        List<BucketSlot> slots = buildBucketSlots(rangeDefinition, LocalDateTime.now());
        List<TelemetryV2Point> historyPoints = readHistoryPoints(device, product, metadataMap, mappingMap, identifiers);
        return buildHistoryBatchResponse(device.getId(), identifiers, request.getRangeCode(), rangeDefinition, slots, historyPoints, metadataMap);
    }

    private Map<String, Object> buildTdengineResponse(Device device, Product product) {
        List<TelemetryLatestPoint> latestPoints = readTdengineLatest(device, product);
        Map<String, Object> properties = new LinkedHashMap<>();
        LocalDateTime latestReportedAt = null;
        String latestTraceId = null;
        for (TelemetryLatestPoint latestPoint : latestPoints) {
            properties.put(latestPoint.getMetricCode(), latestPoint.getValue());
            if (latestReportedAt == null
                    || (latestPoint.getReportedAt() != null && latestPoint.getReportedAt().isAfter(latestReportedAt))) {
                latestReportedAt = latestPoint.getReportedAt();
                latestTraceId = latestPoint.getTraceId();
            }
        }
        return buildResponse(device, product, properties, latestReportedAt, latestTraceId);
    }

    private Map<String, Object> buildMysqlResponse(Device device, Product product) {
        List<DeviceProperty> deviceProperties = devicePropertyMapper.selectList(
                new LambdaQueryWrapper<DeviceProperty>()
                        .eq(DeviceProperty::getDeviceId, device.getId())
                        .orderByAsc(DeviceProperty::getIdentifier)
        );
        Map<String, Object> properties = new LinkedHashMap<>();
        LocalDateTime latestReportedAt = null;
        for (DeviceProperty deviceProperty : deviceProperties) {
            properties.put(deviceProperty.getIdentifier(), parseMysqlPropertyValue(deviceProperty));
            if (latestReportedAt == null
                    || (deviceProperty.getReportTime() != null && deviceProperty.getReportTime().isAfter(latestReportedAt))) {
                latestReportedAt = deviceProperty.getReportTime();
            }
        }
        return buildResponse(device, product, properties, latestReportedAt, null);
    }

    private Device requireDevice(Long deviceId) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getId, deviceId)
                        .eq(Device::getDeleted, 0)
                        .last("limit 1")
        );
        if (device == null) {
            throw new BizException("设备不存在: " + deviceId);
        }
        return device;
    }

    private Map<String, Object> buildResponse(Device device,
                                              Product product,
                                              Map<String, Object> properties,
                                              LocalDateTime reportTime,
                                              String traceId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deviceId", device.getId());
        response.put("deviceCode", device.getDeviceCode());
        response.put("productId", device.getProductId());
        response.put("productKey", product == null ? null : product.getProductKey());
        response.put("storageType", storageModeResolver.isTdengineEnabled() ? "tdengine" : "mysql");
        response.put("reportTime", reportTime);
        response.put("traceId", traceId);
        response.put("properties", properties);
        return response;
    }

    private List<TelemetryLatestPoint> readTdengineLatest(Device device, Product product) {
        if ("v2".equalsIgnoreCase(telemetryReadRouter.latestSource())) {
            List<TelemetryLatestPoint> latestPoints = new ArrayList<>(readV2Latest(device.getId()));
            if (telemetryReadRouter.isLegacyReadFallbackEnabled()) {
                mergeMissingLegacyPoints(latestPoints, tdengineTelemetryFacade.listLatestPoints(device, product));
            }
            return latestPoints;
        }
        return tdengineTelemetryFacade.listLatestPoints(device, product);
    }

    private List<TelemetryV2Point> readHistoryPoints(Device device,
                                                     Product product,
                                                     Map<String, DevicePropertyMetadata> metadataMap,
                                                     Map<String, TelemetryMetricMapping> mappingMap,
                                                     List<String> identifiers) {
        String historySource = telemetryReadRouter.historySource();
        boolean primaryV2 = historySource != null && historySource.startsWith("v2");
        boolean fallbackEnabled = telemetryReadRouter.isLegacyReadFallbackEnabled();
        List<TelemetryV2Point> primary = readHistoryRoute(
                device == null ? null : device.getId(),
                primaryV2 ? "v2" : "legacy",
                fallbackEnabled,
                "继续尝试回退链路",
                () -> primaryV2
                        ? readV2History(device, product, metadataMap, identifiers)
                        : readLegacyHistory(device, product, metadataMap, mappingMap)
        );
        if (!fallbackEnabled) {
            return primary;
        }
        List<TelemetryV2Point> secondary = readHistoryRoute(
                device == null ? null : device.getId(),
                primaryV2 ? "legacy" : "v2",
                true,
                "保留主链路结果",
                () -> primaryV2
                        ? readLegacyHistory(device, product, metadataMap, mappingMap)
                        : readV2History(device, product, metadataMap, identifiers)
        );
        return mergeHistoryPoints(primary, secondary);
    }

    private List<TelemetryV2Point> readHistoryRoute(Long deviceId,
                                                    String source,
                                                    boolean continueOnError,
                                                    String fallbackAction,
                                                    Supplier<List<TelemetryV2Point>> reader) {
        try {
            return safeHistoryPoints(reader.get());
        } catch (Exception ex) {
            if (!continueOnError) {
                throw ex;
            }
            log.warn("读取 telemetry 历史链路失败，{}。source={}, deviceId={}, error={}",
                    fallbackAction,
                    source,
                    deviceId,
                    ex.getMessage());
            return List.of();
        }
    }

    private List<TelemetryV2Point> readV2History(Device device,
                                                 Product product,
                                                 Map<String, DevicePropertyMetadata> metadataMap,
                                                 List<String> identifiers) {
        List<TelemetryV2Point> rawHistory = telemetryRawHistoryReader.listHistory(
                device,
                product,
                metadataMap,
                identifiers,
                HISTORY_BATCH_SIZE
        );
        try {
            if (!normalizedTelemetryHistoryReader.hasHistory(device == null ? null : device.getId())) {
                return rawHistory == null ? List.of() : rawHistory;
            }
            List<TelemetryV2Point> normalizedHistory = normalizedTelemetryHistoryReader.listHistory(
                    device,
                    product,
                    metadataMap,
                    HISTORY_BATCH_SIZE
            );
            return mergeHistoryPoints(rawHistory, normalizedHistory);
        } catch (Exception ex) {
            log.warn("读取 TDengine 兼容表历史失败，回退 telemetry v2 raw 历史, deviceId={}, error={}",
                    device == null ? null : device.getId(),
                    ex.getMessage());
            return rawHistory == null ? List.of() : rawHistory;
        }
    }

    private List<TelemetryV2Point> readLegacyHistory(Device device,
                                                     Product product,
                                                     Map<String, DevicePropertyMetadata> metadataMap,
                                                     Map<String, TelemetryMetricMapping> mappingMap) {
        return legacyTelemetryHistoryReader.listHistory(device, product, metadataMap, mappingMap, HISTORY_BATCH_SIZE);
    }

    private List<TelemetryV2Point> mergeHistoryPoints(List<TelemetryV2Point> primary,
                                                      List<TelemetryV2Point> secondary) {
        Map<String, TelemetryV2Point> merged = new LinkedHashMap<>();
        for (TelemetryV2Point point : safeHistoryPoints(primary)) {
            merged.put(historyPointKey(point), point);
        }
        for (TelemetryV2Point point : safeHistoryPoints(secondary)) {
            merged.putIfAbsent(historyPointKey(point), point);
        }
        return new ArrayList<>(merged.values());
    }

    private List<TelemetryV2Point> safeHistoryPoints(List<TelemetryV2Point> points) {
        return points == null ? List.of() : points;
    }

    private String historyPointKey(TelemetryV2Point point) {
        return String.valueOf(point == null ? null : point.getMetricCode())
                + "@"
                + String.valueOf(point == null ? null : point.getReportedAt());
    }

    private TelemetryHistoryBatchResponse buildHistoryBatchResponse(Long deviceId,
                                                                    List<String> identifiers,
                                                                    String rangeCode,
                                                                    RangeDefinition rangeDefinition,
                                                                    List<BucketSlot> slots,
                                                                    List<TelemetryV2Point> historyPoints,
                                                                    Map<String, DevicePropertyMetadata> metadataMap) {
        TelemetryHistoryBatchResponse response = new TelemetryHistoryBatchResponse();
        response.setDeviceId(deviceId);
        response.setRangeCode(normalizeRangeCode(rangeCode));
        response.setBucket(rangeDefinition.bucketCode());
        List<TelemetryHistoryBatchSeries> seriesList = new ArrayList<>();
        for (String identifier : identifiers) {
            seriesList.add(buildSeries(identifier, rangeDefinition, slots, historyPoints, metadataMap.get(identifier)));
        }
        response.setPoints(seriesList);
        return response;
    }

    private TelemetryHistoryBatchSeries buildSeries(String identifier,
                                                    RangeDefinition rangeDefinition,
                                                    List<BucketSlot> slots,
                                                    List<TelemetryV2Point> historyPoints,
                                                    DevicePropertyMetadata metadata) {
        Map<LocalDateTime, Double> bucketValueMap = new LinkedHashMap<>();
        for (TelemetryV2Point point : historyPoints) {
            if (point == null || !Objects.equals(identifier, point.getMetricCode())) {
                continue;
            }
            LocalDateTime reportedAt = point.getReportedAt();
            if (reportedAt == null) {
                continue;
            }
            LocalDateTime bucketStart = alignToBucket(reportedAt, rangeDefinition.unit());
            if (!containsSlot(slots, bucketStart)) {
                continue;
            }
            bucketValueMap.put(bucketStart, resolvePointValue(point));
        }
        List<TelemetryHistoryBucketPoint> buckets = new ArrayList<>();
        for (BucketSlot slot : slots) {
            TelemetryHistoryBucketPoint bucketPoint = new TelemetryHistoryBucketPoint();
            bucketPoint.setTime(BUCKET_TIME_FORMATTER.format(slot.start()));
            Double value = bucketValueMap.get(slot.start());
            if (value == null) {
                bucketPoint.setValue(0D);
                bucketPoint.setFilled(true);
            } else {
                bucketPoint.setValue(value);
                bucketPoint.setFilled(false);
            }
            buckets.add(bucketPoint);
        }
        TelemetryHistoryBatchSeries series = new TelemetryHistoryBatchSeries();
        series.setIdentifier(identifier);
        series.setDisplayName(resolveDisplayName(identifier, metadata));
        series.setSeriesType(resolveSeriesType(identifier, metadata));
        series.setBuckets(buckets);
        return series;
    }

    private boolean containsSlot(List<BucketSlot> slots, LocalDateTime bucketStart) {
        for (BucketSlot slot : slots) {
            if (slot.start().equals(bucketStart)) {
                return true;
            }
        }
        return false;
    }

    private Double resolvePointValue(TelemetryV2Point point) {
        if (point.getValueDouble() != null) {
            return point.getValueDouble();
        }
        if (point.getValueLong() != null) {
            return point.getValueLong().doubleValue();
        }
        if (point.getValueBool() != null) {
            return Boolean.TRUE.equals(point.getValueBool()) ? 1D : 0D;
        }
        if (point.getValueText() != null && !point.getValueText().isBlank()) {
            try {
                return Double.parseDouble(point.getValueText().trim());
            } catch (Exception ignored) {
                return 0D;
            }
        }
        return 0D;
    }

    private String resolveDisplayName(String identifier, DevicePropertyMetadata metadata) {
        if (metadata != null && metadata.getPropertyName() != null && !metadata.getPropertyName().isBlank()) {
            return metadata.getPropertyName();
        }
        return identifier;
    }

    private String resolveSeriesType(String identifier, DevicePropertyMetadata metadata) {
        TelemetryStreamKind streamKind = TelemetryStreamKind.resolve("property", identifier, metadata, null);
        return streamKind == TelemetryStreamKind.MEASURE ? "measure" : "status";
    }

    private List<String> normalizeIdentifiers(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return List.of();
        }
        return identifiers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeFillPolicy(String fillPolicy) {
        if (fillPolicy == null || fillPolicy.isBlank()) {
            return FILL_POLICY_ZERO;
        }
        return fillPolicy.trim().toUpperCase(Locale.ROOT);
    }

    private RangeDefinition resolveRangeDefinition(String rangeCode) {
        return switch (normalizeRangeCode(rangeCode)) {
            case "1d" -> new RangeDefinition("1d", "hour", ChronoUnit.HOURS, 24);
            case "7d" -> new RangeDefinition("7d", "day", ChronoUnit.DAYS, 7);
            case "30d" -> new RangeDefinition("30d", "day", ChronoUnit.DAYS, 30);
            case "90d" -> new RangeDefinition("90d", "week", ChronoUnit.WEEKS, 13);
            case "365d" -> new RangeDefinition("365d", "month", ChronoUnit.MONTHS, 12);
            default -> throw new BizException("不支持的时间范围: " + rangeCode);
        };
    }

    private String normalizeRangeCode(String rangeCode) {
        if (rangeCode == null || rangeCode.isBlank()) {
            return "7d";
        }
        return rangeCode.trim().toLowerCase(Locale.ROOT);
    }

    private List<BucketSlot> buildBucketSlots(RangeDefinition rangeDefinition, LocalDateTime now) {
        LocalDateTime anchor = alignToBucket(now, rangeDefinition.unit());
        List<BucketSlot> slots = new ArrayList<>();
        for (int index = rangeDefinition.slotCount() - 1; index >= 0; index--) {
            LocalDateTime start = anchor.minus(index, rangeDefinition.unit());
            slots.add(new BucketSlot(start, start.plus(1, rangeDefinition.unit())));
        }
        return slots;
    }

    private LocalDateTime alignToBucket(LocalDateTime value, ChronoUnit unit) {
        if (unit == ChronoUnit.HOURS) {
            return value.truncatedTo(ChronoUnit.HOURS);
        }
        if (unit == ChronoUnit.DAYS) {
            return value.toLocalDate().atStartOfDay();
        }
        if (unit == ChronoUnit.WEEKS) {
            return value.toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
        }
        if (unit == ChronoUnit.MONTHS) {
            return value.toLocalDate().withDayOfMonth(1).atStartOfDay();
        }
        return value;
    }

    private List<TelemetryLatestPoint> readV2Latest(Long deviceId) {
        try {
            return telemetryLatestProjectionRepository.listLatestPoints(deviceId);
        } catch (Exception ex) {
            if (telemetryReadRouter.isLegacyReadFallbackEnabled()) {
                return List.of();
            }
            throw ex;
        }
    }

    private void mergeMissingLegacyPoints(List<TelemetryLatestPoint> latestPoints,
                                          List<TelemetryLatestPoint> legacyPoints) {
        Map<String, TelemetryLatestPoint> pointMap = new LinkedHashMap<>();
        for (TelemetryLatestPoint latestPoint : latestPoints) {
            pointMap.put(latestPoint.getMetricCode(), latestPoint);
        }
        for (TelemetryLatestPoint legacyPoint : legacyPoints) {
            pointMap.putIfAbsent(legacyPoint.getMetricCode(), legacyPoint);
        }
        latestPoints.clear();
        latestPoints.addAll(pointMap.values());
    }

    private Object parseMysqlPropertyValue(DeviceProperty deviceProperty) {
        if (deviceProperty.getPropertyValue() == null) {
            return null;
        }
        String normalizedType = deviceProperty.getValueType() == null
                ? ""
                : deviceProperty.getValueType().trim().toLowerCase(Locale.ROOT);
        try {
            return switch (normalizedType) {
                case "int", "long" -> Long.parseLong(deviceProperty.getPropertyValue());
                case "double", "float", "decimal", "number" -> Double.parseDouble(deviceProperty.getPropertyValue());
                case "bool", "boolean" -> parseBoolean(deviceProperty.getPropertyValue());
                default -> deviceProperty.getPropertyValue();
            };
        } catch (Exception ex) {
            return deviceProperty.getPropertyValue();
        }
    }

    private Boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private record RangeDefinition(String rangeCode, String bucketCode, ChronoUnit unit, int slotCount) {
    }

    private record BucketSlot(LocalDateTime start, LocalDateTime end) {
    }

}
