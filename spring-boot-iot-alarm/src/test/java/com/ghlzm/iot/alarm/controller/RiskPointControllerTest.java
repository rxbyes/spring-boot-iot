package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.service.RiskPointBindingMaintenanceService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
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
        metric.setMetricIdentifier("dispsX");
        metric.setMetricName("X向位移");
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
        assertEquals("PENDING_PROMOTION", response.getData().get(0).getMetrics().get(0).getBindingSource());
        verify(maintenanceService).listBindingGroups(7001L, 1001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
