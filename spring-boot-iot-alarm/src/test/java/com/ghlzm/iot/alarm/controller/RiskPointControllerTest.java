package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointControllerTest {

    @Test
    void bindDeviceShouldExtractCurrentUserAndDelegateToGovernanceSubmission() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);
        RiskPointDevice request = new RiskPointDevice();
        request.setRiskPointId(7001L);
        request.setDeviceId(3001L);
        request.setMetricIdentifier("dispsX");
        request.setMetricName("X轴位移");
        GovernanceSubmissionResultVO result = new GovernanceSubmissionResultVO();
        result.setWorkItemId(8801L);
        result.setExecutionStatus("DIRECT_APPLIED");
        when(maintenanceService.submitBindDevice(request, 1001L)).thenReturn(result);

        R<GovernanceSubmissionResultVO> response = controller.bindDevice(request, authentication(1001L));

        assertEquals(8801L, response.getData().getWorkItemId());
        assertEquals("DIRECT_APPLIED", response.getData().getExecutionStatus());
        verify(maintenanceService).submitBindDevice(request, 1001L);
    }

    @Test
    void unbindDeviceShouldExtractCurrentUserAndDelegateToGovernanceSubmission() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);
        GovernanceSubmissionResultVO result = new GovernanceSubmissionResultVO();
        result.setWorkItemId(8802L);
        result.setApprovalOrderId(9902L);
        result.setApprovalStatus("PENDING");
        result.setExecutionStatus("PENDING_APPROVAL");
        when(maintenanceService.submitUnbindDevice(7001L, 3001L, 1001L)).thenReturn(result);

        R<GovernanceSubmissionResultVO> response = controller.unbindDevice(7001L, 3001L, authentication(1001L));

        assertEquals(9902L, response.getData().getApprovalOrderId());
        assertEquals("PENDING_APPROVAL", response.getData().getExecutionStatus());
        verify(maintenanceService).submitUnbindDevice(7001L, 3001L, 1001L);
    }

    @Test
    void listBindingSummariesShouldExtractCurrentUserAndDelegate() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);
        RiskPointBindingSummaryVO summary = new RiskPointBindingSummaryVO();
        summary.setRiskPointId(7001L);
        summary.setBoundDeviceCount(2);
        summary.setBoundMetricCount(6);
        summary.setPendingBindingCount(1);
        when(maintenanceService.listBindingSummaries(List.of(7001L, 7002L), 1001L))
                .thenReturn(List.of(summary));

        R<List<RiskPointBindingSummaryVO>> response = controller.listBindingSummaries(
                List.of(7001L, 7002L),
                authentication(1001L)
        );

        assertEquals(1, response.getData().size());
        assertEquals(7001L, response.getData().get(0).getRiskPointId());
        verify(maintenanceService).listBindingSummaries(List.of(7001L, 7002L), 1001L);
    }

    @Test
    void listBindingGroupsShouldExtractCurrentUserAndDelegate() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);
        RiskPointBindingMetricVO metric = new RiskPointBindingMetricVO();
        metric.setBindingId(88L);
        metric.setRiskMetricId(6101L);
        metric.setMetricIdentifier("dispsX");
        metric.setMetricName("X轴位移");
        metric.setBindingSource("PENDING_PROMOTION");
        metric.setCreateTime(new Date(1000L));
        RiskPointBindingDeviceGroupVO group = new RiskPointBindingDeviceGroupVO();
        group.setDeviceId(3001L);
        group.setDeviceCode("DEVICE-3001");
        group.setDeviceName("一号设备");
        group.setMetricCount(1);
        group.setMetrics(List.of(metric));
        when(maintenanceService.listBindingGroups(7001L, 1001L)).thenReturn(List.of(group));

        R<List<RiskPointBindingDeviceGroupVO>> response = controller.listBindingGroups(7001L, authentication(1001L));

        assertEquals(1, response.getData().size());
        assertEquals("DEVICE-3001", response.getData().get(0).getDeviceCode());
        assertEquals(6101L, response.getData().get(0).getMetrics().get(0).getRiskMetricId());
        assertEquals("PENDING_PROMOTION", response.getData().get(0).getMetrics().get(0).getBindingSource());
        verify(maintenanceService).listBindingGroups(7001L, 1001L);
    }

    @Test
    void removeBindingShouldExtractCurrentUserAndDelegate() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);

        R<Void> response = controller.removeBinding(88L, authentication(1001L));

        assertEquals(200, response.getCode());
        verify(maintenanceService).removeBinding(88L, 1001L);
    }

    @Test
    void replaceBindingMetricShouldExtractCurrentUserAndDelegate() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointBindingMaintenanceService maintenanceService = mock(RiskPointBindingMaintenanceService.class);
        RiskPointController controller = new RiskPointController(riskPointService, maintenanceService);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setRiskMetricId(6102L);
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");
        RiskPointBindingMetricVO replaced = new RiskPointBindingMetricVO();
        replaced.setBindingId(199L);
        replaced.setRiskMetricId(6102L);
        replaced.setMetricIdentifier("AZI");
        replaced.setMetricName("方位角");
        replaced.setBindingSource("MANUAL");
        when(maintenanceService.replaceBindingMetric(88L, request, 1001L)).thenReturn(replaced);

        R<RiskPointBindingMetricVO> response = controller.replaceBindingMetric(88L, request, authentication(1001L));

        assertEquals(199L, response.getData().getBindingId());
        assertEquals(6102L, response.getData().getRiskMetricId());
        assertEquals("AZI", response.getData().getMetricIdentifier());
        verify(maintenanceService).replaceBindingMetric(88L, request, 1001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
