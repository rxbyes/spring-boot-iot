package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel;
import com.ghlzm.iot.device.mapper.RiskMetricCatalogReadMapper;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.CollectorChildInsightChildVO;
import com.ghlzm.iot.device.vo.CollectorChildInsightMetricVO;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectorChildInsightServiceImplTest {

    private final DeviceService deviceService = mock(DeviceService.class);
    private final DeviceRelationService deviceRelationService = mock(DeviceRelationService.class);
    private final RiskMetricCatalogReadMapper riskMetricCatalogReadMapper = mock(RiskMetricCatalogReadMapper.class);

    private final CollectorChildInsightServiceImpl service = new CollectorChildInsightServiceImpl(
            deviceService,
            deviceRelationService,
            riskMetricCatalogReadMapper
    );

    @Test
    void getOverviewShouldMarkCollectorChildRecommendedMetricsFromPublishedCatalog() {
        DeviceDetailVO parent = new DeviceDetailVO();
        parent.setDeviceCode("COLLECTOR-001");
        parent.setOnlineStatus(1);

        DeviceDetailVO child = new DeviceDetailVO();
        child.setDeviceCode("LASER-001");
        child.setDeviceName("1# 激光测点");
        child.setProductId(2001L);
        child.setProductKey("nf-monitor-laser-rangefinder-v1");
        child.setOnlineStatus(1);
        child.setLastReportTime(LocalDateTime.of(2026, 4, 18, 11, 30, 0));

        DeviceRelationVO relation = new DeviceRelationVO();
        relation.setLogicalChannelCode("L1_LF_1");
        relation.setChildDeviceCode("LASER-001");
        relation.setChildProductKey("nf-monitor-laser-rangefinder-v1");

        DeviceProperty value = new DeviceProperty();
        value.setIdentifier("value");
        value.setPropertyName("激光测距值");
        value.setPropertyValue("10.86");
        value.setReportTime(LocalDateTime.of(2026, 4, 18, 11, 30, 0));

        DeviceProperty sensorState = new DeviceProperty();
        sensorState.setIdentifier("sensor_state");
        sensorState.setPropertyName("传感器状态");
        sensorState.setPropertyValue("0");
        sensorState.setReportTime(LocalDateTime.of(2026, 4, 18, 11, 30, 0));

        RiskMetricCatalogReadModel recommendedCatalog = new RiskMetricCatalogReadModel();
        recommendedCatalog.setContractIdentifier("value");
        recommendedCatalog.setInsightEnabled(1);
        recommendedCatalog.setEnabled(1);
        recommendedCatalog.setDeleted(0);

        when(deviceService.getDetailByCode(1001L, "COLLECTOR-001")).thenReturn(parent);
        when(deviceRelationService.listByParentDeviceCode(1001L, "COLLECTOR-001")).thenReturn(List.of(relation));
        when(deviceService.getDetailByCode(1001L, "LASER-001")).thenReturn(child);
        when(deviceService.listProperties(1001L, "LASER-001")).thenReturn(List.of(value, sensorState));
        when(riskMetricCatalogReadMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(recommendedCatalog));

        CollectorChildInsightOverviewVO overview = service.getOverview(1001L, "COLLECTOR-001");

        assertEquals(1, overview.getRecommendedMetricCount());
        assertEquals(List.of("value"), service.listRecommendedMetrics(2001L));
        CollectorChildInsightChildVO childOverview = overview.getChildren().get(0);
        assertEquals(List.of("value"), childOverview.getRecommendedMetricIdentifiers());
        CollectorChildInsightMetricVO metric = childOverview.getMetrics().get(0);
        assertEquals("value", metric.getIdentifier());
        assertTrue(Boolean.TRUE.equals(metric.getRecommended()));
    }
}
