package com.ghlzm.iot.alarm.controller;

import com.ghlzm.iot.alarm.dto.RiskPointPendingBindingQuery;
import com.ghlzm.iot.alarm.service.RiskPointPendingBindingService;
import com.ghlzm.iot.alarm.service.RiskPointPendingRecommendationService;
import com.ghlzm.iot.alarm.vo.RiskPointPendingBindingItemVO;
import com.ghlzm.iot.alarm.vo.RiskPointPendingCandidateBundleVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointPendingControllerTest {

    @Test
    void pagePendingBindingsShouldExtractCurrentUserAndDelegate() {
        RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
        RiskPointPendingRecommendationService recommendationService = mock(RiskPointPendingRecommendationService.class);
        RiskPointPendingController controller = new RiskPointPendingController(bindingService, recommendationService);
        RiskPointPendingBindingQuery query = new RiskPointPendingBindingQuery();
        query.setRiskPointId(8001L);
        RiskPointPendingBindingItemVO item = new RiskPointPendingBindingItemVO();
        item.setId(9001L);
        item.setDeviceCode("demo-device-01");
        PageResult<RiskPointPendingBindingItemVO> page = PageResult.of(1L, 1L, 10L, List.of(item));
        Authentication authentication = authentication(1001L);
        when(bindingService.pagePendingBindings(argThat(candidate ->
                candidate != null
                        && Long.valueOf(8001L).equals(candidate.getRiskPointId())
                        && candidate.getDeviceCode() == null
                        && candidate.getResolutionStatus() == null
                        && candidate.getBatchNo() == null
                        && Long.valueOf(1L).equals(candidate.getPageNum())
                        && Long.valueOf(10L).equals(candidate.getPageSize())
        ), eq(1001L))).thenReturn(page);

        R<PageResult<RiskPointPendingBindingItemVO>> response = controller.pagePendingBindings(
                8001L,
                null,
                null,
                null,
                1L,
                10L,
                authentication
        );

        assertEquals(1L, response.getData().getTotal());
        assertEquals("demo-device-01", response.getData().getRecords().get(0).getDeviceCode());
        verify(bindingService).pagePendingBindings(argThat(candidate ->
                candidate != null
                        && Long.valueOf(8001L).equals(candidate.getRiskPointId())
                        && candidate.getDeviceCode() == null
                        && candidate.getResolutionStatus() == null
                        && candidate.getBatchNo() == null
                        && Long.valueOf(1L).equals(candidate.getPageNum())
                        && Long.valueOf(10L).equals(candidate.getPageSize())
        ), eq(1001L));
    }

    @Test
    void getCandidatesShouldExtractCurrentUserAndDelegate() {
        RiskPointPendingBindingService bindingService = mock(RiskPointPendingBindingService.class);
        RiskPointPendingRecommendationService recommendationService = mock(RiskPointPendingRecommendationService.class);
        RiskPointPendingController controller = new RiskPointPendingController(bindingService, recommendationService);
        RiskPointPendingCandidateBundleVO bundle = new RiskPointPendingCandidateBundleVO();
        bundle.setPendingId(9001L);
        bundle.setResolutionStatus("PENDING_METRIC_GOVERNANCE");
        Authentication authentication = authentication(1001L);
        when(recommendationService.getCandidates(9001L, 1001L)).thenReturn(bundle);

        R<RiskPointPendingCandidateBundleVO> response = controller.getCandidates(9001L, authentication);

        assertEquals(9001L, response.getData().getPendingId());
        verify(recommendationService).getCandidates(9001L, 1001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
