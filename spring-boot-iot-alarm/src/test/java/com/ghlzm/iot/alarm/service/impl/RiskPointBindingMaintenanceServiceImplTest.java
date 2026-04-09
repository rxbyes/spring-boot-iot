package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

        List<RiskPointBindingSummaryVO> result = service.listBindingSummaries(Arrays.asList(11L, null, 11L, 12L), 1001L);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getRiskPointId());
        assertEquals(2, result.get(0).getBoundDeviceCount());
        assertEquals(3, result.get(0).getBoundMetricCount());
        assertEquals(2, result.get(0).getPendingBindingCount());
        assertEquals(12L, result.get(1).getRiskPointId());
        assertEquals(1, result.get(1).getBoundDeviceCount());
        assertEquals(1, result.get(1).getBoundMetricCount());
        assertEquals(1, result.get(1).getPendingBindingCount());
        verify(riskPointService, times(1)).getById(11L, 1001L);
        verify(riskPointService, times(1)).getById(12L, 1001L);
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
                pendingPromotion(501L, "SUCCESS"),
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

    @Test
    void listBindingGroupsShouldKeepManualWhenPromotionHistoryOnlyHasDuplicateSkipped() {
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
                binding(701L, 11L, 9301L, "DEV-MANUAL", "人工绑定设备", "dispsX", "X向位移", new Date(1000L))
        ));
        when(pendingPromotionMapper.selectList(any())).thenReturn(List.of(
                pendingPromotion(701L, "DUPLICATE_SKIPPED")
        ));

        List<RiskPointBindingDeviceGroupVO> result = service.listBindingGroups(11L, 1001L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMetricCount());
        assertEquals("MANUAL", result.get(0).getMetrics().get(0).getBindingSource());
    }

    @Test
    void removeBindingShouldDeleteOnlyTargetMetricBinding() {
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
        RiskPointDevice targetBinding = binding(
                2001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        when(riskPointDeviceMapper.selectById(2001L)).thenReturn(targetBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.deleteById(2001L)).thenReturn(1);

        service.removeBinding(2001L, 1001L);

        verify(riskPointDeviceMapper).deleteById(2001L);
        verify(riskPointService, never()).unbindDevice(11L, 201L, 1001L);
    }

    @Test
    void replaceBindingMetricShouldCreateNewBindingAndDeleteOldBinding() {
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
        RiskPointDevice oldBinding = binding(
                3001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                3999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(3001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(3001L)).thenReturn(1);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        RiskPointBindingMetricVO result = service.replaceBindingMetric(3001L, request, 1001L);

        assertEquals(3999L, result.getBindingId());
        assertEquals("AZI", result.getMetricIdentifier());
        verify(riskPointDeviceMapper).deleteById(3001L);
        ArgumentCaptor<RiskPointDevice> bindingCaptor = ArgumentCaptor.forClass(RiskPointDevice.class);
        verify(riskPointService).bindDeviceAndReturn(bindingCaptor.capture(), any());
        assertEquals(11L, bindingCaptor.getValue().getRiskPointId());
        assertEquals(201L, bindingCaptor.getValue().getDeviceId());
        assertEquals("DEV-201", bindingCaptor.getValue().getDeviceCode());
        assertEquals("一号设备", bindingCaptor.getValue().getDeviceName());
        assertEquals("AZI", bindingCaptor.getValue().getMetricIdentifier());
        assertEquals("方位角", bindingCaptor.getValue().getMetricName());
    }

    @Test
    void replaceBindingMetricShouldFallbackMetricNameToIdentifierWhenBlank() {
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
        RiskPointDevice oldBinding = binding(
                4001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                4999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "AZI",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(4001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(4001L)).thenReturn(1);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier(" AZI ");
        request.setMetricName("   ");

        RiskPointBindingMetricVO result = service.replaceBindingMetric(4001L, request, 1001L);

        assertEquals(4999L, result.getBindingId());
        ArgumentCaptor<RiskPointDevice> bindingCaptor = ArgumentCaptor.forClass(RiskPointDevice.class);
        verify(riskPointService).bindDeviceAndReturn(bindingCaptor.capture(), any());
        assertEquals("AZI", bindingCaptor.getValue().getMetricIdentifier());
        assertEquals("AZI", bindingCaptor.getValue().getMetricName());
    }

    @Test
    void replaceBindingMetricShouldRejectWhenReplacingWithSameMetric() {
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
        RiskPointDevice oldBinding = binding(
                5001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(1000L)
        );
        when(riskPointDeviceMapper.selectById(5001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier(" AZI ");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(5001L, request, 1001L));

        verify(riskPointDeviceMapper, never()).selectOne(any());
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
    }

    @Test
    void replaceBindingMetricShouldRejectWhenTargetMetricAlreadyBound() {
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
        RiskPointDevice oldBinding = binding(
                6001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice duplicateBinding = binding(
                6002L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(1200L)
        );
        when(riskPointDeviceMapper.selectById(6001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(duplicateBinding);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(6001L, request, 1001L));

        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(riskPointDeviceMapper, never()).deleteById(6001L);
    }

    @Test
    void replaceBindingMetricShouldThrowWhenDeleteOldBindingFailsAfterNewBindingCreated() {
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
        RiskPointDevice oldBinding = binding(
                7001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                7999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(7001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(7001L)).thenReturn(0);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(7001L, request, 1001L));

        verify(riskPointService).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(riskPointDeviceMapper).deleteById(7001L);
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
        return pendingPromotion(riskPointDeviceId, null);
    }

    private RiskPointDevicePendingPromotion pendingPromotion(Long riskPointDeviceId, String promotionStatus) {
        RiskPointDevicePendingPromotion value = new RiskPointDevicePendingPromotion();
        value.setRiskPointDeviceId(riskPointDeviceId);
        value.setPromotionStatus(promotionStatus);
        value.setDeleted(0);
        return value;
    }
}
