package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.service.RiskGovernanceOpsService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskGovernanceControllerTest {

    @Test
    void listMissingBindingsShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_BINDING");
        PageResult<RiskGovernanceGapItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        when(service.listMissingBindings(query)).thenReturn(page);

        R<PageResult<RiskGovernanceGapItemVO>> response = controller.listMissingBindings(query);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("MISSING_BINDING", response.getData().getRecords().get(0).getIssueType());
    }

    @Test
    void listMissingPoliciesShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        RiskGovernanceGapItemVO item = new RiskGovernanceGapItemVO();
        item.setIssueType("MISSING_POLICY");
        PageResult<RiskGovernanceGapItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        when(service.listMissingPolicies(query)).thenReturn(page);

        R<PageResult<RiskGovernanceGapItemVO>> response = controller.listMissingPolicies(query);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("MISSING_POLICY", response.getData().getRecords().get(0).getIssueType());
    }

    @Test
    void pageMetricCatalogsShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskMetricCatalogItemVO item = new RiskMetricCatalogItemVO();
        item.setId(9101L);
        item.setContractIdentifier("value");
        PageResult<RiskMetricCatalogItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        when(service.pageMetricCatalogs(1001L, 1L, 10L)).thenReturn(page);

        R<PageResult<RiskMetricCatalogItemVO>> response = controller.pageMetricCatalogs(1001L, 1L, 10L);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("value", response.getData().getRecords().get(0).getContractIdentifier());
    }

    @Test
    void getMetricCatalogShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskMetricCatalogItemVO item = new RiskMetricCatalogItemVO();
        item.setId(9101L);
        item.setRiskMetricCode("RM_1001_VALUE");
        item.setReleaseBatchId(7001L);
        item.setNormativeIdentifier("value");
        item.setRiskCategory("CRACK");
        item.setMetricRole("PRIMARY");
        item.setLifecycleStatus("ACTIVE");
        when(service.getMetricCatalog(9101L)).thenReturn(item);

        R<RiskMetricCatalogItemVO> response = controller.getMetricCatalog(9101L);

        assertEquals("RM_1001_VALUE", response.getData().getRiskMetricCode());
        assertEquals(7001L, response.getData().getReleaseBatchId());
        assertEquals("value", response.getData().getNormativeIdentifier());
        assertEquals("CRACK", response.getData().getRiskCategory());
        assertEquals("PRIMARY", response.getData().getMetricRole());
        assertEquals("ACTIVE", response.getData().getLifecycleStatus());
    }

    @Test
    void getCoverageOverviewShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskGovernanceCoverageOverviewVO overview = new RiskGovernanceCoverageOverviewVO();
        overview.setProductId(1001L);
        overview.setContractPropertyCount(4L);
        overview.setPublishedRiskMetricCount(2L);
        when(service.getCoverageOverview(1001L)).thenReturn(overview);

        R<RiskGovernanceCoverageOverviewVO> response = controller.getCoverageOverview(1001L);

        assertEquals(1001L, response.getData().getProductId());
        assertEquals(2L, response.getData().getPublishedRiskMetricCount());
    }

    @Test
    void getDashboardOverviewShouldDelegateToService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskGovernanceDashboardOverviewVO overview = new RiskGovernanceDashboardOverviewVO();
        overview.setTotalProductCount(12L);
        overview.setPendingPolicyCount(3L);
        when(service.getDashboardOverview()).thenReturn(overview);

        R<RiskGovernanceDashboardOverviewVO> response = controller.getDashboardOverview();

        assertEquals(12L, response.getData().getTotalProductCount());
        assertEquals(3L, response.getData().getPendingPolicyCount());
    }

    @Test
    void pageOpsAlertsShouldDelegateToOpsService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        RiskGovernanceOpsAlertItemVO alert = new RiskGovernanceOpsAlertItemVO();
        alert.setAlertType("FIELD_DRIFT");
        alert.setAffectedCount(2L);
        when(opsService.pageOpsAlerts(1001L, "FIELD_DRIFT", 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(alert)));

        R<PageResult<RiskGovernanceOpsAlertItemVO>> response =
                controller.pageOpsAlerts(1001L, "FIELD_DRIFT", 1L, 10L);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("FIELD_DRIFT", response.getData().getRecords().get(0).getAlertType());
    }

    @Test
    void replayShouldExtractCurrentUserAndDelegateToOpsService() {
        RiskGovernanceService service = mock(RiskGovernanceService.class);
        RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
        RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
        Authentication authentication = authentication(1001L);
        RiskGovernanceReplayVO replay = new RiskGovernanceReplayVO();
        replay.setTraceId("trace-001");
        replay.setReleaseBatchId(7001L);
        replay.setMatchedMessageCount(3L);
        when(opsService.replay(1001L, "trace-001", null, null, 7001L)).thenReturn(replay);

        R<RiskGovernanceReplayVO> response = controller.replay("trace-001", null, null, 7001L, authentication);

        assertEquals("trace-001", response.getData().getTraceId());
        assertEquals(7001L, response.getData().getReleaseBatchId());
        assertEquals(3L, response.getData().getMatchedMessageCount());
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
