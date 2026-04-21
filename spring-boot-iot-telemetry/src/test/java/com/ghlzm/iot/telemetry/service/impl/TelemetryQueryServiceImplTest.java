package com.ghlzm.iot.telemetry.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.RuntimeMetricDisplayRuleService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchRequest;
import com.ghlzm.iot.telemetry.service.dto.TelemetryHistoryBatchResponse;
import com.ghlzm.iot.telemetry.service.model.TelemetryLatestPoint;
import com.ghlzm.iot.telemetry.service.model.TelemetryV2Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryQueryServiceImplTest {

    private static final DateTimeFormatter BUCKET_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private DevicePropertyMetadataService devicePropertyMetadataService;
    @Mock
    private DeviceTelemetryMappingService deviceTelemetryMappingService;
    @Mock
    private PublishedProductContractSnapshotService snapshotService;
    @Mock
    private MetricIdentifierResolver metricIdentifierResolver;
    @Mock
    private RuntimeMetricDisplayRuleService runtimeMetricDisplayRuleService;
    @Mock
    private TdengineTelemetryFacade tdengineTelemetryFacade;
    @Mock
    private TelemetryStorageModeResolver storageModeResolver;
    @Mock
    private TelemetryReadRouter telemetryReadRouter;
    @Mock
    private TelemetryLatestProjectionRepository telemetryLatestProjectionRepository;
    @Mock
    private NormalizedTelemetryHistoryReader normalizedTelemetryHistoryReader;
    @Mock
    private LegacyTelemetryHistoryReader legacyTelemetryHistoryReader;
    @Mock
    private TelemetryRawHistoryReader telemetryRawHistoryReader;

    private TelemetryQueryServiceImpl telemetryQueryService;

    @BeforeEach
    void setUp() {
        telemetryQueryService = new TelemetryQueryServiceImpl(
                deviceMapper,
                productMapper,
                devicePropertyMapper,
                devicePropertyMetadataService,
                deviceTelemetryMappingService,
                tdengineTelemetryFacade,
                storageModeResolver,
                telemetryReadRouter,
                telemetryLatestProjectionRepository,
                normalizedTelemetryHistoryReader,
                legacyTelemetryHistoryReader,
                telemetryRawHistoryReader,
                snapshotService,
                metricIdentifierResolver,
                runtimeMetricDisplayRuleService
        );
    }

    @Test
    void getHistoryBatchShouldZeroFillRequestedBuckets() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        DevicePropertyMetadata statusMetadata = metadata("传感器在线状态", "int");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata,
                "S1_ZT_1.sensor_state.L4_NW_1", statusMetadata
        ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(true);
        when(normalizedTelemetryHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        )).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位高程", 2.6D, historyTimeInCurrentWeekWindow()),
                historyPoint("S1_ZT_1.sensor_state.L4_NW_1", "传感器在线状态", 1L, historyTimeInCurrentWeekWindow())
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals(2, result.getPoints().size());
        assertEquals("day", result.getBucket());
        assertEquals(7, result.getPoints().get(0).getBuckets().size());
        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 0D));
        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 2.6D));
        assertEquals("measure", result.getPoints().get(0).getSeriesType());
        assertEquals("status", result.getPoints().get(1).getSeriesType());
    }

    @Test
    void getHistoryBatchShouldRejectBlankIdentifiers() {
        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of());
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        assertThrows(BizException.class, () -> telemetryQueryService.getHistoryBatch(request));
    }

    @Test
    void getHistoryBatchShouldUseRuntimeDisplayRuleWhenFormalMetadataMissing() {
        Device device = buildDevice();
        Product product = buildProduct();
        DeviceProperty currentProperty = new DeviceProperty();
        currentProperty.setIdentifier("S1_ZT_1.humidity");
        currentProperty.setPropertyName("humidity");
        currentProperty.setValueType("double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(currentProperty));
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of());
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
        when(telemetryRawHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("S1_ZT_1.humidity")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        )).thenReturn(List.of(
                historyPoint("S1_ZT_1.humidity", "humidity", 67.2D, historyTimeInCurrentWeekWindow())
        ));
        when(runtimeMetricDisplayRuleService.resolveForDisplay(product, "S1_ZT_1.humidity"))
                .thenReturn(new RuntimeMetricDisplayRuleService.DisplayResolution(8101L, "PRODUCT", "相对湿度", "%RH"));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("S1_ZT_1.humidity"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals("相对湿度", result.getPoints().get(0).getDisplayName());
    }

    @Test
    void getHistoryBatchShouldRejectQuarterRangeAfterRangeConsolidation() {
        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("90d");
        request.setFillPolicy("ZERO");

        BizException error = assertThrows(BizException.class, () -> telemetryQueryService.getHistoryBatch(request));

        assertEquals("不支持的时间范围: 90d", error.getMessage());
    }

    @Test
    void getHistoryBatchShouldFallbackToRawV2HistoryWhenLegacyAndNormalizedHistoryAreMissing() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        DevicePropertyMetadata statusMetadata = metadata("传感器在线状态", "int");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("legacy");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(true);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata,
                "S1_ZT_1.sensor_state.L4_NW_1", statusMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(legacyTelemetryHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), anyMap(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of());
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
        when(telemetryRawHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of(
                        historyPoint("L4_NW_1", "泥水位高程", 2.6D, historyTimeInCurrentWeekWindow()),
                        historyPoint("S1_ZT_1.sensor_state.L4_NW_1", "传感器在线状态", 1L, historyTimeInCurrentWeekWindow())
                ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 2.6D));
        assertEquals(true, result.getPoints().get(1).getBuckets().stream().anyMatch(item -> item.getValue() == 1D));
        verify(telemetryRawHistoryReader).listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1", "S1_ZT_1.sensor_state.L4_NW_1")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldKeepRawV2HistoryWhenNormalizedProbeFails() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("legacy");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(true);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(legacyTelemetryHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), anyMap(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of());
        when(telemetryRawHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of(
                        historyPoint("L4_NW_1", "泥水位高程", 2.6D, historyTimeInCurrentWeekWindow())
                ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L))
                .thenThrow(new RuntimeException("iot_device_telemetry_point unavailable"));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 2.6D));
    }

    @Test
    void getHistoryBatchShouldFallbackToV2HistoryWhenLegacyPrimaryReadFails() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("legacy");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(true);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(legacyTelemetryHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), anyMap(), any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenThrow(new RuntimeException("legacy stable unavailable"));
        when(telemetryRawHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of(
                        historyPoint("L4_NW_1", "泥水位高程", 2.6D, historyTimeInCurrentWeekWindow())
                ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 2.6D));
    }

    @Test
    void getHistoryBatchShouldBucketByIngestedAtBeforeReportedAt() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime reportedAt = now.minusDays(2).withHour(10);
        LocalDateTime ingestedAt = now.minusDays(1).withHour(10);
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(telemetryRawHistoryReader.listHistory(
                eq(device), eq(product), anyMap(), eq(List.of("L4_NW_1")),
                any(LocalDateTime.class), any(LocalDateTime.class), anyInt()
        ))
                .thenReturn(List.of(
                        historyPoint("L4_NW_1", "泥水位高程", 2.6D, reportedAt, ingestedAt)
                ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        Map<String, Double> bucketMap = result.getPoints().get(0).getBuckets().stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.getTime(),
                        item -> item.getValue(),
                        (left, right) -> right,
                        java.util.LinkedHashMap::new
                ));

        assertEquals(2.6D, bucketMap.get(BUCKET_TIME_FORMATTER.format(ingestedAt.withHour(0))));
        assertEquals(0D, bucketMap.get(BUCKET_TIME_FORMATTER.format(reportedAt.withHour(0))));
    }

    @Test
    void getHistoryBatchShouldPassRequestedWindowToRawHistoryReader() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
        when(telemetryRawHistoryReader.listHistory(
                eq(device),
                eq(product),
                anyMap(),
                eq(List.of("L4_NW_1")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.now().minusDays(1))
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        telemetryQueryService.getHistoryBatch(request);

        verify(telemetryRawHistoryReader).listHistory(
                eq(device),
                eq(product),
                anyMap(),
                eq(List.of("L4_NW_1")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldPassRequestedWindowToNormalizedHistoryReader() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(telemetryRawHistoryReader.listHistory(
                eq(device),
                eq(product),
                anyMap(),
                eq(List.of("L4_NW_1")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of());
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(true);
        when(normalizedTelemetryHistoryReader.listHistory(
                eq(device),
                eq(product),
                anyMap(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.now().minusDays(1))
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        telemetryQueryService.getHistoryBatch(request);

        verify(normalizedTelemetryHistoryReader).listHistory(
                eq(device),
                eq(product),
                anyMap(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldPassRequestedWindowToLegacyHistoryReader() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata measureMetadata = metadata("泥水位高程", "double");
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("legacy");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L4_NW_1", measureMetadata
        ));
        when(deviceTelemetryMappingService.listMetricMappingMap(1001L)).thenReturn(Map.of());
        when(legacyTelemetryHistoryReader.listHistory(
                eq(device),
                eq(product),
                anyMap(),
                anyMap(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位高程", 2.6D, LocalDateTime.now().minusDays(1))
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        telemetryQueryService.getHistoryBatch(request);

        verify(legacyTelemetryHistoryReader).listHistory(
                eq(device),
                eq(product),
                anyMap(),
                anyMap(),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldFallbackToCurrentPropertyMetadataWhenProductMetadataIsMissing() {
        Device device = buildDevice();
        Product product = buildProduct();
        DeviceProperty currentProperty = new DeviceProperty();
        currentProperty.setDeviceId(2001L);
        currentProperty.setIdentifier("L4_NW_1");
        currentProperty.setPropertyName("泥水位");
        currentProperty.setValueType("int");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of());
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(currentProperty));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
        when(telemetryRawHistoryReader.listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> {
                    DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get("L4_NW_1");
                    return metadata != null
                            && "泥水位".equals(metadata.getPropertyName())
                            && "int".equals(metadata.getDataType());
                }),
                eq(List.of("L4_NW_1")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("L4_NW_1", "泥水位", 1L, historyTimeInCurrentWeekWindow())
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L4_NW_1"));
        request.setRangeCode("7d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals("泥水位", result.getPoints().get(0).getDisplayName());
        assertEquals("measure", result.getPoints().get(0).getSeriesType());
        verify(telemetryRawHistoryReader).listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> {
                    DevicePropertyMetadata metadata = metadataMap == null ? null : metadataMap.get("L4_NW_1");
                    return metadata != null
                            && "泥水位".equals(metadata.getPropertyName())
                            && "int".equals(metadata.getDataType());
                }),
                eq(List.of("L4_NW_1")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldResolveRequestedIdentifiersIgnoringCaseBeforeQueryingHistory() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata formalMetadata = metadata("X轴加速度", "double");
        DeviceProperty currentProperty = new DeviceProperty();
        currentProperty.setDeviceId(2001L);
        currentProperty.setIdentifier("L1_JS_1.gX");
        currentProperty.setPropertyName("X轴加速度");
        currentProperty.setValueType("double");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "L1_JS_1.gX", formalMetadata
        ));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(currentProperty));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(false);
        when(telemetryRawHistoryReader.listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> metadataMap != null && metadataMap.containsKey("L1_JS_1.gX")),
                eq(List.of("L1_JS_1.gX")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("L1_JS_1.gX", "X轴加速度", 0.1667D, historyTimeInCurrentDayWindow())
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("l1_js_1.gx"));
        request.setRangeCode("1d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals("L1_JS_1.gX", result.getPoints().get(0).getIdentifier());
        assertEquals("X轴加速度", result.getPoints().get(0).getDisplayName());
        verify(telemetryRawHistoryReader).listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> metadataMap != null && metadataMap.containsKey("L1_JS_1.gX")),
                eq(List.of("L1_JS_1.gX")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void getHistoryBatchShouldResolveRequestedAliasToPublishedCanonicalIdentifierBeforeHistoryQuery() {
        Device device = buildDevice();
        Product product = buildProduct();
        DevicePropertyMetadata canonicalMetadata = metadata("裂缝值", "double");
        PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
                .productId(1001L)
                .releaseBatchId(9001L)
                .publishedIdentifier("value")
                .canonicalAlias("L1_LF_1.value", "value")
                .build();

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.historySource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(false);
        when(snapshotService.getRequiredSnapshot(1001L)).thenReturn(snapshot);
        when(metricIdentifierResolver.resolveForRead(snapshot, "L1_LF_1.value"))
                .thenReturn(MetricIdentifierResolution.of(
                        "L1_LF_1.value",
                        "value",
                        MetricIdentifierResolution.SOURCE_PUBLISHED_SNAPSHOT
                ));
        when(devicePropertyMetadataService.listPropertyMetadataMap(1001L)).thenReturn(Map.of(
                "value", canonicalMetadata
        ));
        when(normalizedTelemetryHistoryReader.hasHistory(2001L)).thenReturn(true);
        when(telemetryRawHistoryReader.listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> metadataMap != null && metadataMap.containsKey("value")),
                eq(List.of("value")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of());
        when(normalizedTelemetryHistoryReader.listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> metadataMap != null && metadataMap.containsKey("value")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        )).thenReturn(List.of(
                historyPoint("value", "裂缝值", 0.2136D, historyTimeInCurrentDayWindow())
        ));

        TelemetryHistoryBatchRequest request = new TelemetryHistoryBatchRequest();
        request.setDeviceId(2001L);
        request.setIdentifiers(List.of("L1_LF_1.value"));
        request.setRangeCode("1d");
        request.setFillPolicy("ZERO");

        TelemetryHistoryBatchResponse result = telemetryQueryService.getHistoryBatch(request);

        assertEquals("value", result.getPoints().get(0).getIdentifier());
        assertEquals("裂缝值", result.getPoints().get(0).getDisplayName());
        assertEquals(true, result.getPoints().get(0).getBuckets().stream().anyMatch(item -> item.getValue() == 0.2136D));
        verify(telemetryRawHistoryReader).listHistory(
                eq(device),
                eq(product),
                argThat(metadataMap -> metadataMap != null && metadataMap.containsKey("value")),
                eq(List.of("value")),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                anyInt()
        );
    }

    @Test
    void shouldReadV2LatestBeforeLegacyFallback() {
        Device device = buildDevice();
        Product product = buildProduct();
        when(storageModeResolver.isTdengineEnabled()).thenReturn(true);
        when(telemetryReadRouter.latestSource()).thenReturn("v2");
        when(telemetryReadRouter.isLegacyReadFallbackEnabled()).thenReturn(true);
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(telemetryLatestProjectionRepository.listLatestPoints(2001L)).thenReturn(List.of(
                latestPoint("temperature", 26.8D)
        ));
        when(tdengineTelemetryFacade.listLatestPoints(device, product)).thenReturn(List.of(
                latestPoint("temperature", 26.5D),
                latestPoint("humidity", 68)
        ));

        Map<String, Object> result = telemetryQueryService.getLatest(2001L);

        assertEquals("tdengine", result.get("storageType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) result.get("properties");
        assertEquals(26.8D, properties.get("temperature"));
        assertEquals(68, properties.get("humidity"));
        verify(tdengineTelemetryFacade).listLatestPoints(device, product);
        verify(telemetryLatestProjectionRepository).listLatestPoints(2001L);
    }

    @Test
    void getLatestShouldFallbackToMysqlWhenConfigured() {
        when(storageModeResolver.isTdengineEnabled()).thenReturn(false);
        when(deviceMapper.selectOne(any())).thenReturn(buildDevice());
        when(productMapper.selectById(1001L)).thenReturn(buildProduct());
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
                property("temperature", "double", "26.5"),
                property("enabled", "bool", "true")
        ));

        Map<String, Object> result = telemetryQueryService.getLatest(2001L);

        assertEquals("mysql", result.get("storageType"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) result.get("properties");
        assertEquals(26.5D, properties.get("temperature"));
        assertEquals(Boolean.TRUE, properties.get("enabled"));
        verifyNoInteractions(tdengineTelemetryFacade);
    }

    @Test
    void getLatestShouldRejectUnknownDevice() {
        when(deviceMapper.selectOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> telemetryQueryService.getLatest(9999L));
    }

    private Device buildDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        return device;
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        return product;
    }

    private TelemetryLatestPoint latestPoint(String metricCode, Object value) {
        TelemetryLatestPoint point = new TelemetryLatestPoint();
        point.setMetricCode(metricCode);
        point.setValue(value);
        point.setReportedAt(LocalDateTime.of(2026, 3, 23, 10, 0));
        point.setTraceId("trace-001");
        return point;
    }

    private DeviceProperty property(String identifier, String valueType, String value) {
        DeviceProperty property = new DeviceProperty();
        property.setIdentifier(identifier);
        property.setValueType(valueType);
        property.setPropertyValue(value);
        property.setReportTime(LocalDateTime.of(2026, 3, 23, 10, 0));
        return property;
    }

    private DevicePropertyMetadata metadata(String propertyName, String dataType) {
        DevicePropertyMetadata metadata = new DevicePropertyMetadata();
        metadata.setPropertyName(propertyName);
        metadata.setDataType(dataType);
        return metadata;
    }

    private LocalDateTime historyTimeInCurrentWeekWindow() {
        return LocalDateTime.now()
                .minusDays(1)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime historyTimeInCurrentDayWindow() {
        return LocalDateTime.now()
                .minusHours(2)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private TelemetryV2Point historyPoint(String metricCode,
                                          String metricName,
                                          Object value,
                                          LocalDateTime reportedAt) {
        return historyPoint(metricCode, metricName, value, reportedAt, reportedAt);
    }

    private TelemetryV2Point historyPoint(String metricCode,
                                          String metricName,
                                          Object value,
                                          LocalDateTime reportedAt,
                                          LocalDateTime ingestedAt) {
        TelemetryV2Point point = new TelemetryV2Point();
        point.setMetricCode(metricCode);
        point.setMetricName(metricName);
        point.setReportedAt(reportedAt);
        point.setIngestedAt(ingestedAt);
        if (value instanceof Double number) {
            point.setValueDouble(number);
            point.setValueType("double");
        } else if (value instanceof Long number) {
            point.setValueLong(number);
            point.setValueType("long");
        }
        return point;
    }
}
