package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskPointPendingRecommendationServiceImplTest {

    @Test
    void getCandidatesShouldFlattenNestedPropertyLogsAndIgnoreNonPropertyMetadata() {
        Fixture fixture = new Fixture();

        ProductModel vibration = fixture.productModel("vibrationRms", "振动均方根", "double", 1);
        DeviceProperty vibrationProperty = fixture.deviceProperty("vibrationRms", "振动均方根", "1.25", LocalDateTime.of(2026, 4, 3, 11, 0, 0));

        DeviceMessageLog propertyLog = fixture.messageLog(
                "property",
                "{\"traceId\":\"trace-1\",\"deviceCode\":\"demo-device-01\",\"timestamp\":\"2026-04-03T11:05:00\",\"properties\":{\"vibrationRms\":1.25,\"nested\":{\"tilt.x\":6.2}},\"reported\":{\"ignored\":true}}",
                LocalDateTime.of(2026, 4, 3, 11, 5, 0)
        );
        DeviceMessageLog eventLog = fixture.messageLog(
                "event",
                "{\"eventType\":\"alarm\",\"payload\":{\"fakeMetric\":999}}",
                LocalDateTime.of(2026, 4, 3, 11, 6, 0)
        );

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(vibration));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(vibrationProperty));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(eventLog, propertyLog));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of());

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);

        assertEquals(2, result.getCandidates().size());
        assertIterableEquals(
                List.of("vibrationRms", "nested.tilt.x"),
                result.getCandidates().stream().map(RiskPointPendingMetricCandidateVO::getMetricIdentifier).toList()
        );
    }

    @Test
    void getCandidatesShouldRejectNonPromotablePendingStatus() {
        Fixture fixture = new Fixture();
        fixture.pending.setResolutionStatus("PROMOTED");

        BizException exception = assertThrows(BizException.class, () -> fixture.service.getCandidates(9001L, 1001L));

        assertEquals("当前待治理状态不支持查看候选测点", exception.getMessage());
    }

    @Test
    void getCandidatesShouldClassifyHighMediumAndLowBySpec() {
        Fixture fixture = new Fixture();

        ProductModel modelBacked = fixture.productModel("displacement", "位移", "double", 1);
        ProductModel weakModelOnly = fixture.productModel("modelOnly", "模型预置字段", "double", 2);

        DeviceProperty displacementProperty = fixture.deviceProperty("displacement", "实时位移", "12.6", LocalDateTime.of(2026, 4, 3, 11, 0, 0));
        DeviceProperty runtimeOnlyProperty = fixture.deviceProperty("temperature", "温度", "26.5", LocalDateTime.of(2026, 4, 3, 10, 30, 0));
        DeviceProperty strongRuntimeProperty = fixture.deviceProperty("tilt", "倾角", "3.5", LocalDateTime.of(2026, 4, 3, 10, 40, 0));

        DeviceMessageLog propertyLogA = fixture.messageLog(
                "property",
                "{\"properties\":{\"displacement\":12.6,\"temperature\":26.5,\"battery\":88,\"tilt\":3.5}}",
                LocalDateTime.of(2026, 4, 3, 11, 5, 0)
        );
        DeviceMessageLog propertyLogB = fixture.messageLog(
                "status",
                "{\"status\":{\"tilt\":3.4}}",
                LocalDateTime.of(2026, 4, 3, 11, 4, 0)
        );
        DeviceMessageLog propertyLogC = fixture.messageLog(
                "property",
                "{\"data\":{\"tilt\":3.6}}",
                LocalDateTime.of(2026, 4, 3, 11, 3, 0)
        );

        RiskPointDevicePendingPromotion promotion = new RiskPointDevicePendingPromotion();
        promotion.setPendingBindingId(9001L);
        promotion.setMetricIdentifier("legacyMetric");
        promotion.setPromotionStatus("REJECTED");
        promotion.setRecommendationLevel("LOW");
        promotion.setRecommendationScore(30);
        promotion.setCreateTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 2, 8, 0, 0)));

        when(fixture.productModelMapper.selectList(any())).thenReturn(List.of(modelBacked, weakModelOnly));
        when(fixture.devicePropertyMapper.selectList(any())).thenReturn(List.of(displacementProperty, runtimeOnlyProperty, strongRuntimeProperty));
        when(fixture.deviceMessageLogMapper.selectList(any())).thenReturn(List.of(propertyLogA, propertyLogB, propertyLogC));
        when(fixture.promotionMapper.selectList(any())).thenReturn(List.of(promotion));

        RiskPointPendingCandidateBundleVO result = fixture.service.getCandidates(9001L, 1001L);
        Map<String, RiskPointPendingMetricCandidateVO> candidateMap = result.getCandidates().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RiskPointPendingMetricCandidateVO::getMetricIdentifier,
                        candidate -> candidate
                ));

        assertEquals(1, result.getPromotionHistory().size());
        assertEquals(5, result.getCandidates().size());
        RiskPointPendingMetricCandidateVO highModelAndRuntime = candidateMap.get("displacement");
        assertEquals("HIGH", highModelAndRuntime.getRecommendationLevel());
        assertEquals("实时位移", highModelAndRuntime.getMetricName());

        RiskPointPendingMetricCandidateVO highRuntimeOnly = candidateMap.get("tilt");
        assertEquals("HIGH", highRuntimeOnly.getRecommendationLevel());
        assertEquals("tilt", highRuntimeOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO mediumRuntimeOnly = candidateMap.get("temperature");
        assertEquals("MEDIUM", mediumRuntimeOnly.getRecommendationLevel());
        assertEquals("temperature", mediumRuntimeOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO mediumModelOnly = candidateMap.get("modelOnly");
        assertEquals("MEDIUM", mediumModelOnly.getRecommendationLevel());
        assertEquals("modelOnly", mediumModelOnly.getMetricIdentifier());

        RiskPointPendingMetricCandidateVO low = candidateMap.get("battery");
        assertEquals("LOW", low.getRecommendationLevel());
        assertEquals("battery", low.getMetricIdentifier());
        assertEquals(List.of("MESSAGE_LOG"), low.getEvidenceSources());
    }

    private static final class Fixture {
        private final RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
        private final DeviceService deviceService = mock(DeviceService.class);
        private final ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        private final DevicePropertyMapper devicePropertyMapper = mock(DevicePropertyMapper.class);
        private final DeviceMessageLogMapper deviceMessageLogMapper = mock(DeviceMessageLogMapper.class);
        private final RiskPointDevicePendingPromotionMapper promotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        private final RiskPointPendingRecommendationServiceImpl service = new RiskPointPendingRecommendationServiceImpl(
                bindingService,
                deviceService,
                productModelMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                promotionMapper
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
