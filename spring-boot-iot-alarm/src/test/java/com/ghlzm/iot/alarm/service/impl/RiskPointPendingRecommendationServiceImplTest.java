package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointPendingRecommendationServiceImplTest {

    @Test
    void getCandidatesShouldFlattenNestedPropertyLogsAndIgnoreNonPropertyMetadata() {
        Fixture fixture = new Fixture();

        ProductModel acceleration = fixture.productModel("gX", "X向加速度", "double", 1);
        DeviceProperty accelerationProperty = fixture.deviceProperty("gX", "X向加速度", "1.25", LocalDateTime.of(2026, 4, 3, 11, 0, 0));

        DeviceMessageLog propertyLog = fixture.messageLog(
                "property",
                "{\"traceId\":\"trace-1\",\"deviceCode\":\"demo-device-01\",\"timestamp\":\"2026-04-03T11:05:00\",\"properties\":{\"gX\":1.25,\"nested\":{\"gpsTotalX\":6.2}},\"reported\":{\"ignored\":true}}",
                LocalDateTime.of(2026, 4, 3, 11, 5, 0)
        );
        DeviceMessageLog eventLog = fixture.messageLog(
                "event",
                "{\"eventType\":\"alarm\",\"payload\":{\"fakeMetric\":999}}",
                LocalDateTime.of(2026, 4, 3, 11, 6, 0)
        );

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(acceleration));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(accelerationProperty));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(eventLog, propertyLog));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);

        assertEquals(2, result.getCandidates().size());
        assertIterableEquals(
                List.of("gX", "gpsTotalX"),
                result.getCandidates().stream().map(RiskPointPendingMetricCandidateVO::getMetricIdentifier).toList()
        );
    }

    @Test
    void getCandidatesShouldRejectNonPromotablePendingStatus() {
        Fixture fixture = new Fixture();
        fixture.pending.setResolutionStatus("PROMOTED");

        BizException exception = assertThrows(BizException.class, () -> fixture.service.getCandidates(9001L, 1001L));

        assertEquals(400, exception.getCode());
        assertEquals("当前待治理状态不支持查看候选测点", exception.getMessage());
    }

    @Test
    void getCandidatesShouldClassifyHighMediumAndLowBySpec() {
        Fixture fixture = new Fixture();
        fixture.device.setDeviceName("声发射监测仪");
        fixture.pending.setDeviceName("声发射监测仪");

        ProductModel modelBacked = fixture.productModel("OSP", "声压级", "double", 1);
        ProductModel weakModelOnly = fixture.productModel("wave", "波形", "double", 2);

        DeviceProperty modelBackedProperty = fixture.deviceProperty("OSP", "实时声压级", "12.6", LocalDateTime.of(2026, 4, 3, 11, 0, 0));
        DeviceProperty runtimeOnlyProperty = fixture.deviceProperty("freq", "主频", "26.5", LocalDateTime.of(2026, 4, 3, 10, 30, 0));
        DeviceProperty strongRuntimeProperty = fixture.deviceProperty("VSP", "振速峰值", "3.5", LocalDateTime.of(2026, 4, 3, 10, 40, 0));

        DeviceMessageLog propertyLogA = fixture.messageLog(
                "property",
                "{\"properties\":{\"OSP\":12.6,\"freq\":26.5,\"VSP\":3.5,\"amplitude\":1.1}}",
                LocalDateTime.of(2026, 4, 3, 11, 5, 0)
        );
        DeviceMessageLog propertyLogB = fixture.messageLog(
                "status",
                "{\"status\":{\"VSP\":3.4}}",
                LocalDateTime.of(2026, 4, 3, 11, 4, 0)
        );
        DeviceMessageLog propertyLogC = fixture.messageLog(
                "property",
                "{\"data\":{\"VSP\":3.6}}",
                LocalDateTime.of(2026, 4, 3, 11, 3, 0)
        );
        DeviceMessageLog propertyLogD = fixture.messageLog(
                "property",
                "{\"data\":{\"VSP\":3.7}}",
                LocalDateTime.of(2026, 4, 3, 11, 2, 0)
        );

        RiskPointDevicePendingPromotion promotion = new RiskPointDevicePendingPromotion();
        promotion.setPendingBindingId(9001L);
        promotion.setMetricIdentifier("legacyMetric");
        promotion.setPromotionStatus("REJECTED");
        promotion.setRecommendationLevel("LOW");
        promotion.setRecommendationScore(30);
        promotion.setCreateTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 2, 8, 0, 0)));

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(modelBacked, weakModelOnly));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(modelBackedProperty, runtimeOnlyProperty, strongRuntimeProperty));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(propertyLogA, propertyLogB, propertyLogC, propertyLogD));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of(promotion));

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);
        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = result.getCandidates().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RiskPointPendingMetricCandidateVO::getMetricIdentifier,
                        candidate -> candidate
                ));

        assertEquals(1, result.getPromotionHistory().size());
        assertEquals(5, result.getCandidates().size());
        RiskPointPendingMetricCandidateVO highModelAndRuntime = candidateMap.get("OSP");
        assertEquals("HIGH", highModelAndRuntime.getRecommendationLevel());
        assertEquals("实时声压级", highModelAndRuntime.getMetricName());

        RiskPointPendingMetricCandidateVO highRuntimeOnly = candidateMap.get("VSP");
        assertEquals("HIGH", highRuntimeOnly.getRecommendationLevel());
        assertEquals("VSP", highRuntimeOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO mediumRuntimeOnly = candidateMap.get("freq");
        assertEquals("MEDIUM", mediumRuntimeOnly.getRecommendationLevel());
        assertEquals("freq", mediumRuntimeOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO mediumModelOnly = candidateMap.get("wave");
        assertEquals("MEDIUM", mediumModelOnly.getRecommendationLevel());
        assertEquals("wave", mediumModelOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO low = candidateMap.get("amplitude");
        assertEquals("LOW", low.getRecommendationLevel());
        assertEquals("amplitude", low.getMetricIdentifier());
        assertEquals(List.of("MESSAGE_LOG"), low.getEvidenceSources());
    }

    @Test
    void getCandidatesShouldIgnoreTransportWrapperFieldsFromRawMessageLogs() {
        Fixture fixture = new Fixture();

        DeviceMessageLog rawWrapperLog = fixture.messageLog(
                "property",
                "{\"bodies\":{\"body\":\"cipher-text\"},\"header\":{\"appId\":\"62000001\"},\"properties\":{\"dispsX\":12.6}}",
                LocalDateTime.of(2026, 4, 3, 12, 0, 0)
        );

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of());
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of());
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(rawWrapperLog));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);

        assertEquals(1, result.getCandidates().size());
        assertIterableEquals(
                List.of("dispsX"),
                result.getCandidates().stream().map(RiskPointPendingMetricCandidateVO::getMetricIdentifier).toList()
        );
    }

    @Test
    void getCandidatesShouldOnlyKeepBusinessMetricsForFixedInclinometerAndPreserveAliasEvidence() {
        Fixture fixture = new Fixture();
        fixture.device.setDeviceName("固定测斜仪-L1");
        fixture.pending.setDeviceName("固定测斜仪-L1");

        DeviceProperty dispX = fixture.deviceProperty("L1_SW_1.dispsX", "深部位移X", "1.25", LocalDateTime.of(2026, 4, 3, 11, 0, 0));
        DeviceProperty dispY = fixture.deviceProperty("L1_SW_1.dispsY", "深部位移Y", "2.50", LocalDateTime.of(2026, 4, 3, 11, 1, 0));
        DeviceProperty battery = fixture.deviceProperty("battery_dump_energy", "电池电量", "88", LocalDateTime.of(2026, 4, 3, 11, 2, 0));
        DeviceProperty signal = fixture.deviceProperty("signal_4g", "4G信号", "-72", LocalDateTime.of(2026, 4, 3, 11, 3, 0));
        DeviceProperty temp = fixture.deviceProperty("temp", "温度", "20.5", LocalDateTime.of(2026, 4, 3, 11, 4, 0));

        DeviceMessageLog propertyLog = fixture.messageLog(
                "property",
                "{\"properties\":{\"L1_SW_1.dispsX\":1.25,\"L1_SW_1.dispsY\":2.5,\"battery_dump_energy\":88,\"signal_4g\":-72,\"temp\":20.5,\"humidity\":60,\"lat\":31.2304,\"lon\":121.4737,\"sw_version\":\"v1.0.3\"}}",
                LocalDateTime.of(2026, 4, 3, 11, 5, 0)
        );

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of());
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(dispX, dispY, battery, signal, temp));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(propertyLog));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);
        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = result.getCandidates().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RiskPointPendingMetricCandidateVO::getMetricIdentifier,
                        candidate -> candidate
                ));

        assertEquals(2, result.getCandidates().size());
        assertIterableEquals(
                List.of("dispsX", "dispsY"),
                result.getCandidates().stream().map(RiskPointPendingMetricCandidateVO::getMetricIdentifier).toList()
        );
        assertTrue(candidateMap.get("dispsX").getReasonSummary().contains("L1_SW_1.dispsX"));
        assertTrue(candidateMap.get("dispsY").getReasonSummary().contains("L1_SW_1.dispsY"));
    }

    @Test
    void getCandidatesShouldAttachCatalogRiskMetricIdForReleasedCrackMetric() {
        Fixture fixture = new Fixture();
        fixture.device.setDeviceName("裂缝计-L1");
        fixture.pending.setDeviceName("裂缝计-L1");
        fixture.pending.setRiskPointName("K79+620 裂缝监测点");

        ProductModel releasedValue = fixture.productModel("value", "裂缝监测值", "double", 1);
        DeviceProperty latestValue = fixture.deviceProperty("value", "裂缝监测值", "10.86", LocalDateTime.of(2026, 4, 3, 11, 0, 0));
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(7001L);
        catalog.setProductId(2001L);
        catalog.setContractIdentifier("value");
        catalog.setRiskMetricName("裂缝监测值");
        catalog.setEnabled(1);

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(releasedValue));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(latestValue));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());
        when(fixture.riskMetricCatalogService.listEnabledByProduct(2001L)).thenReturn(List.of(catalog));

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);

        assertEquals(1, result.getCandidates().size());
        assertEquals(7001L, result.getCandidates().get(0).getRiskMetricId());
        verify(fixture.riskMetricCatalogService).publishFromReleasedContracts(org.mockito.ArgumentMatchers.eq(2001L), any(), org.mockito.ArgumentMatchers.eq(java.util.Set.of("value")));
    }

    @Test
    void getCandidatesShouldPublishGnssRiskMetricsForCatalogGovernance() {
        Fixture fixture = new Fixture();
        fixture.device.setDeviceName("北斗GNSS位移计-L1");
        fixture.pending.setDeviceName("北斗GNSS位移计-L1");
        fixture.pending.setRiskPointName("K79+620 GNSS监测点");

        ProductModel gpsInitial = fixture.productModel("gpsInitial", "初始位移", "double", 1);
        ProductModel gpsTotalX = fixture.productModel("gpsTotalX", "X向累计位移", "double", 2);
        ProductModel gpsTotalY = fixture.productModel("gpsTotalY", "Y向累计位移", "double", 3);
        ProductModel gpsTotalZ = fixture.productModel("gpsTotalZ", "Z向累计位移", "double", 4);
        DeviceProperty latestGpsTotalX = fixture.deviceProperty("gpsTotalX", "X向累计位移", "10.86", LocalDateTime.of(2026, 4, 3, 11, 0, 0));

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(gpsInitial, gpsTotalX, gpsTotalY, gpsTotalZ));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(latestGpsTotalX));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of());
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());

        fixture.service.getCandidates(9001L, 1001L);

        verify(fixture.riskMetricCatalogService).publishFromReleasedContracts(
                org.mockito.ArgumentMatchers.eq(2001L),
                any(),
                org.mockito.ArgumentMatchers.eq(Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ"))
        );
    }

    private static final class Fixture {
        private final RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
        private final DeviceService deviceService = mock(DeviceService.class);
        private final ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        private final DevicePropertyMapper devicePropertyMapper = mock(DevicePropertyMapper.class);
        private final DeviceMessageLogMapper deviceMessageLogMapper = mock(DeviceMessageLogMapper.class);
        private final RiskPointDevicePendingPromotionMapper promotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        private final RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        private final RiskPointPendingRecommendationServiceImpl service = new RiskPointPendingRecommendationServiceImpl(
                bindingService,
                deviceService,
                productModelMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                promotionMapper,
                riskMetricCatalogService
        );
        private final RiskPointDevicePendingBinding pending = pending();
        private final Device device = device();

        private Fixture() {
            when(bindingService.getRequiredPending(9001L, 1001L)).thenReturn(pending);
            when(deviceService.getRequiredById(3002L)).thenReturn(device);
        }

        private RiskPointDevicePendingBinding pending() {
            RiskPointDevicePendingBinding value = new RiskPointDevicePendingBinding();
            value.setId(9001L);
            value.setRiskPointId(8001L);
            value.setRiskPointName("K79+620边坡");
            value.setDeviceId(3002L);
            value.setDeviceCode("demo-device-01");
            value.setDeviceName("演示设备");
            value.setResolutionStatus("PENDING_METRIC_GOVERNANCE");
            value.setBatchNo("BATCH-20260403");
            value.setCreateTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 3, 9, 0, 0)));
            return value;
        }

        private Device device() {
            Device value = new Device();
            value.setId(3002L);
            value.setProductId(2001L);
            value.setDeviceCode("demo-device-01");
            value.setDeviceName("演示设备");
            return value;
        }

        private ProductModel productModel(String identifier, String name, String dataType, int sortNo) {
            ProductModel value = new ProductModel();
            value.setProductId(2001L);
            value.setModelType("property");
            value.setIdentifier(identifier);
            value.setModelName(name);
            value.setDataType(dataType);
            value.setSortNo(sortNo);
            value.setDeleted(0);
            return value;
        }

        private DeviceProperty deviceProperty(String identifier, String name, String propertyValue, LocalDateTime reportTime) {
            DeviceProperty value = new DeviceProperty();
            value.setDeviceId(3002L);
            value.setIdentifier(identifier);
            value.setPropertyName(name);
            value.setPropertyValue(propertyValue);
            value.setValueType("double");
            value.setReportTime(reportTime);
            return value;
        }

        private DeviceMessageLog messageLog(String messageType, String payload, LocalDateTime reportTime) {
            DeviceMessageLog value = new DeviceMessageLog();
            value.setDeviceId(3002L);
            value.setMessageType(messageType);
            value.setPayload(payload);
            value.setReportTime(reportTime);
            return value;
        }
    }
}
