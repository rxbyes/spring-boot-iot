package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.service.DeviceRelationService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceRelationVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorChildInsightServiceImplTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceRelationService deviceRelationService;

    private CollectorChildInsightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CollectorChildInsightServiceImpl(deviceService, deviceRelationService);
    }

    @Test
    void getOverviewShouldAggregateChildLatestMetricsAndFormalSensorState() {
        LocalDateTime reportTime = LocalDateTime.of(2026, 4, 9, 21, 47, 28);
        when(deviceService.getDetailByCode(1001L, "SK00EA0D1307988"))
                .thenReturn(parentDevice(1, "SK00EA0D1307988"));
        when(deviceRelationService.listByParentDeviceCode(1001L, "SK00EA0D1307988"))
                .thenReturn(List.of(relation("L1_LF_1", "202018108", "nf-monitor-laser-rangefinder-v1")));
        when(deviceService.getDetailByCode(1001L, "202018108"))
                .thenReturn(childDevice("202018108", "1# 激光测点", 1, reportTime));
        when(deviceService.listProperties(1001L, "202018108"))
                .thenReturn(List.of(
                        property("value", "激光测距值", "10.86", reportTime),
                        property("sensor_state", "传感器状态", "0", reportTime)
                ));

        CollectorChildInsightOverviewVO overview = service.getOverview(1001L, "SK00EA0D1307988");

        assertEquals(1, overview.getChildCount());
        assertEquals(1, overview.getReachableChildCount());
        assertEquals(1, overview.getSensorStateReportedCount());
        assertEquals("L1_LF_1", overview.getChildren().get(0).getLogicalChannelCode());
        assertEquals("reachable", overview.getChildren().get(0).getCollectorLinkState());
        assertEquals("0", overview.getChildren().get(0).getSensorStateValue());
        assertEquals(1, overview.getChildren().get(0).getMetrics().size());
        assertEquals("value", overview.getChildren().get(0).getMetrics().get(0).getIdentifier());
    }

    @Test
    void getOverviewShouldMarkChildUnreachableWhenParentOffline() {
        when(deviceService.getDetailByCode(1001L, "SK00EA0D1307988"))
                .thenReturn(parentDevice(0, "SK00EA0D1307988"));
        when(deviceRelationService.listByParentDeviceCode(1001L, "SK00EA0D1307988"))
                .thenReturn(List.of(relation("L1_LF_1", "202018108", "nf-monitor-laser-rangefinder-v1")));
        when(deviceService.getDetailByCode(1001L, "202018108"))
                .thenReturn(childDevice("202018108", "1# 激光测点", 1, LocalDateTime.of(2026, 4, 9, 21, 47, 28)));
        when(deviceService.listProperties(1001L, "202018108")).thenReturn(List.of());

        CollectorChildInsightOverviewVO overview = service.getOverview(1001L, "SK00EA0D1307988");

        assertEquals(1, overview.getChildCount());
        assertEquals(0, overview.getReachableChildCount());
        assertEquals("unreachable", overview.getChildren().get(0).getCollectorLinkState());
    }

    private DeviceDetailVO parentDevice(int onlineStatus, String deviceCode) {
        DeviceDetailVO vo = new DeviceDetailVO();
        vo.setDeviceCode(deviceCode);
        vo.setOnlineStatus(onlineStatus);
        return vo;
    }

    private DeviceDetailVO childDevice(String deviceCode,
                                       String deviceName,
                                       int onlineStatus,
                                       LocalDateTime lastReportTime) {
        DeviceDetailVO vo = new DeviceDetailVO();
        vo.setDeviceCode(deviceCode);
        vo.setDeviceName(deviceName);
        vo.setOnlineStatus(onlineStatus);
        vo.setLastReportTime(lastReportTime);
        return vo;
    }

    private DeviceRelationVO relation(String logicalChannelCode,
                                      String childDeviceCode,
                                      String childProductKey) {
        DeviceRelationVO vo = new DeviceRelationVO();
        vo.setLogicalChannelCode(logicalChannelCode);
        vo.setChildDeviceCode(childDeviceCode);
        vo.setChildProductKey(childProductKey);
        return vo;
    }

    private DeviceProperty property(String identifier,
                                    String name,
                                    String value,
                                    LocalDateTime reportTime) {
        DeviceProperty property = new DeviceProperty();
        property.setIdentifier(identifier);
        property.setPropertyName(name);
        property.setPropertyValue(value);
        property.setReportTime(reportTime);
        return property;
    }
}
