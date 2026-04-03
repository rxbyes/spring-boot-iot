package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingMetricCandidateVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskPointPendingRecommendationServiceImplTest {

    @Test
    void getCandidatesShouldMergeEvidenceAndSortByRecommendationStrength() {
        RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        DevicePropertyMapper devicePropertyMapper = mock(DevicePropertyMapper.class);
        DeviceMessageLogMapper deviceMessageLogMapper = mock(DeviceMessageLogMapper.class);
        RiskPointDevicePendingPromotionMapper promotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointPendingRecommendationServiceImpl service = new RiskPointPendingRecommendationServiceImpl(
                bindingService,
                deviceService,
                productModelMapper,
                devicePropertyMapper,
                deviceMessageLogMapper,
                promotionMapper
        );

        RiskPointDevicePendingBinding pending = new RiskPointDevicePendingBinding();
        pending.setId(9001L);
        pending.setRiskPointId(8001L);
        pending.setRiskPointName("K79+620边坡");
        pending.setDeviceId(3002L);
        pending.setDeviceCode("demo-device-01");
        pending.setDeviceName("演示设备");
        pending.setResolutionStatus("PENDING_METRIC_GOVERNANCE");
        pending.setBatchNo("BATCH-20260403");
        pending.setCreateTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 3, 9, 0, 0)));

        Device device = new Device();
        device.setId(3002L);
        device.setProductId(2001L);
        device.setDeviceCode("demo-device-01");
        device.setDeviceName("演示设备");

        ProductModel displacement = new ProductModel();
        displacement.setProductId(2001L);
        displacement.setModelType("property");
        displacement.setIdentifier("displacement");
        displacement.setModelName("位移");
        displacement.setDataType("double");
        displacement.setSortNo(1);
        displacement.setDeleted(0);

        DeviceProperty displacementProperty = new DeviceProperty();
        displacementProperty.setDeviceId(3002L);
        displacementProperty.setIdentifier("displacement");
        displacementProperty.setPropertyName("实时位移");
        displacementProperty.setPropertyValue("12.6");
        displacementProperty.setValueType("double");
        displacementProperty.setReportTime(LocalDateTime.of(2026, 4, 3, 11, 0, 0));

        DeviceProperty temperatureProperty = new DeviceProperty();
        temperatureProperty.setDeviceId(3002L);
        temperatureProperty.setIdentifier("temperature");
        temperatureProperty.setPropertyName("温度");
        temperatureProperty.setPropertyValue("26.5");
        temperatureProperty.setValueType("double");
        temperatureProperty.setReportTime(LocalDateTime.of(2026, 4, 3, 10, 30, 0));

        DeviceMessageLog recentPayload = new DeviceMessageLog();
        recentPayload.setDeviceId(3002L);
        recentPayload.setPayload("{\"displacement\":12.6,\"temperature\":26.5,\"battery\":88}");
        recentPayload.setReportTime(LocalDateTime.of(2026, 4, 3, 11, 5, 0));

        DeviceMessageLog olderPayload = new DeviceMessageLog();
        olderPayload.setDeviceId(3002L);
        olderPayload.setPayload("{\"displacement\":12.1,\"battery\":87}");
        olderPayload.setReportTime(LocalDateTime.of(2026, 4, 3, 10, 55, 0));

        RiskPointDevicePendingPromotion promotion = new RiskPointDevicePendingPromotion();
        promotion.setPendingBindingId(9001L);
        promotion.setMetricIdentifier("legacyMetric");
        promotion.setPromotionStatus("REJECTED");
        promotion.setRecommendationLevel("LOW");
        promotion.setRecommendationScore(30);
        promotion.setCreateTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 2, 8, 0, 0)));

        when(bindingService.getRequiredPending(9001L, 1001L)).thenReturn(pending);
        when(deviceService.getRequiredById(3002L)).thenReturn(device);
        when(productModelMapper.selectList(any())).thenReturn(List.of(displacement));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(displacementProperty, temperatureProperty));
        when(deviceMessageLogMapper.selectList(any())).thenReturn(List.of(recentPayload, olderPayload));
        when(promotionMapper.selectList(any())).thenReturn(List.of(promotion));

        RiskPointPendingCandidateBundleVO result = service.getCandidates(9001L, 1001L);

        assertEquals(9001L, result.getPendingId());
        assertEquals("PENDING_METRIC_GOVERNANCE", result.getResolutionStatus());
        assertEquals(1, result.getPromotionHistory().size());

        List<RiskPointPendingMetricCandidateVO> candidates = result.getCandidates();
        assertEquals(3, candidates.size());
        assertIterableEquals(
                List.of("displacement", "temperature", "battery"),
                candidates.stream().map(RiskPointPendingMetricCandidateVO::getMetricIdentifier).toList()
        );

        RiskPointPendingMetricCandidateVO first = candidates.get(0);
        assertEquals("实时位移", first.getMetricName());
        assertEquals("HIGH", first.getRecommendationLevel());
        assertEquals(List.of("PRODUCT_MODEL", "LATEST_PROPERTY", "MESSAGE_LOG"), first.getEvidenceSources());
        assertEquals("12.6", first.getSampleValue());
        assertEquals(4, first.getSeenCount());

        RiskPointPendingMetricCandidateVO second = candidates.get(1);
        assertEquals("MEDIUM", second.getRecommendationLevel());
        assertEquals(List.of("LATEST_PROPERTY", "MESSAGE_LOG"), second.getEvidenceSources());

        RiskPointPendingMetricCandidateVO third = candidates.get(2);
        assertEquals("LOW", third.getRecommendationLevel());
        assertEquals(List.of("MESSAGE_LOG"), third.getEvidenceSources());
        assertEquals("88", third.getSampleValue());
    }
}
