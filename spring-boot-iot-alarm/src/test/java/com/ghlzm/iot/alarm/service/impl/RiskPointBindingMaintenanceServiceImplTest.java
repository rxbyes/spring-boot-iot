package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointBindingMaintenanceServiceImplTest {

    @Test
    void listBindingSummariesShouldAggregateFormalBindingsAndPendingCounts() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointService.getById(12L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(100L, 11L, 201L, "DEV-201", "一号设备", "metric_a", "测点A", new Date(1000L)),
                binding(101L, 11L, 201L, "DEV-201", "一号设备", "metric_b", "测点B", new Date(2000L)),
                binding(102L, 11L, 202L, "DEV-202", "二号设备", "metric_c", "测点C", new Date(3000L)),
                binding(103L, 12L, 301L, "DEV-301", "三号设备", "metric_x", "测点X", new Date(4000L))
        ));
        when(pendingBindingMapper.selectList(any())).thenReturn(List.of(
                pending(11L, "PENDING_METRIC_GOVERNANCE"),
                pending(11L, "PARTIALLY_PROMOTED"),
                pending(11L, "IGNORED"),
                pending(12L, "PARTIALLY_PROMOTED")
        ));

        List<RiskPointBindingSummaryVO> result = service.listBindingSummaries(List.of(11L, 12L), 1001L);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getRiskPointId());
        assertEquals(2, result.get(0).getBoundDeviceCount());
        assertEquals(3, result.get(0).getBoundMetricCount());
        assertEquals(2, result.get(0).getPendingBindingCount());
        assertEquals(12L, result.get(1).getRiskPointId());
        assertEquals(1, result.get(1).getBoundDeviceCount());
        assertEquals(1, result.get(1).getBoundMetricCount());
        assertEquals(1, result.get(1).getPendingBindingCount());
        verify(riskPointService).getById(11L, 1001L);
        verify(riskPointService).getById(12L, 1001L);
    }

    @Test
    void listBindingGroupsShouldGroupMetricsByDeviceAndMarkPromotionSource() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(501L, 11L, 9202L, "DEV-B", "B设备", "z_metric", "Z测点", new Date(3000L)),
                binding(502L, 11L, 9201L, "DEV-A", "A设备", "b_metric", "B测点", new Date(2000L)),
                binding(503L, 11L, 9201L, "DEV-A", "A设备", "a_metric", "A测点", new Date(1000L))
        ));
        when(pendingPromotionMapper.selectList(any())).thenReturn(List.of(
                pendingPromotion(501L),
                pendingPromotion(9999L)
        ));

        List<RiskPointBindingDeviceGroupVO> result = service.listBindingGroups(11L, 1001L);

        assertEquals(2, result.size());
        assertEquals(9201L, result.get(0).getDeviceId());
        assertEquals("DEV-A", result.get(0).getDeviceCode());
        assertEquals(2, result.get(0).getMetricCount());
        assertEquals("a_metric", result.get(0).getMetrics().get(0).getMetricIdentifier());
        assertEquals("MANUAL", result.get(0).getMetrics().get(0).getBindingSource());
        assertEquals("b_metric", result.get(0).getMetrics().get(1).getMetricIdentifier());
        assertEquals("MANUAL", result.get(0).getMetrics().get(1).getBindingSource());
        assertEquals(9202L, result.get(1).getDeviceId());
        assertEquals("DEV-B", result.get(1).getDeviceCode());
        assertEquals(1, result.get(1).getMetricCount());
        assertEquals("z_metric", result.get(1).getMetrics().get(0).getMetricIdentifier());
        assertEquals("PENDING_PROMOTION", result.get(1).getMetrics().get(0).getBindingSource());
        verify(riskPointService).getById(11L, 1001L);
    }

    private RiskPointDevice binding(Long id,
                                    Long riskPointId,
                                    Long deviceId,
                                    String deviceCode,
                                    String deviceName,
                                    String metricIdentifier,
                                    String metricName,
                                    Date createTime) {
        RiskPointDevice value = new RiskPointDevice();
        value.setId(id);
        value.setRiskPointId(riskPointId);
        value.setDeviceId(deviceId);
        value.setDeviceCode(deviceCode);
        value.setDeviceName(deviceName);
        value.setMetricIdentifier(metricIdentifier);
        value.setMetricName(metricName);
        value.setCreateTime(createTime);
        value.setDeleted(0);
        return value;
    }

    private RiskPointDevicePendingBinding pending(Long riskPointId, String status) {
        RiskPointDevicePendingBinding value = new RiskPointDevicePendingBinding();
        value.setRiskPointId(riskPointId);
        value.setResolutionStatus(status);
        value.setDeleted(0);
        return value;
    }

    private RiskPointDevicePendingPromotion pendingPromotion(Long riskPointDeviceId) {
        RiskPointDevicePendingPromotion value = new RiskPointDevicePendingPromotion();
        value.setRiskPointDeviceId(riskPointDeviceId);
        value.setDeleted(0);
        return value;
    }
}
