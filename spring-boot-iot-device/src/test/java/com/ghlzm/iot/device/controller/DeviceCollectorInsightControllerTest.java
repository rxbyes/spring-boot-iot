package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.CollectorChildInsightService;
import com.ghlzm.iot.device.vo.CollectorChildInsightOverviewVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCollectorInsightControllerTest {

    @Mock
    private CollectorChildInsightService collectorChildInsightService;

    private DeviceCollectorInsightController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceCollectorInsightController(collectorChildInsightService);
    }

    @Test
    void overviewEndpointShouldDelegateToCollectorInsightService() {
        CollectorChildInsightOverviewVO overview = new CollectorChildInsightOverviewVO();
        overview.setParentDeviceCode("SK00EA0D1307988");
        overview.setChildCount(1);
        when(collectorChildInsightService.getOverview(1001L, "SK00EA0D1307988")).thenReturn(overview);

        R<CollectorChildInsightOverviewVO> response = controller.getOverview("SK00EA0D1307988", authentication(1001L));

        assertEquals("SK00EA0D1307988", response.getData().getParentDeviceCode());
        assertEquals(1, response.getData().getChildCount());
        verify(collectorChildInsightService).getOverview(1001L, "SK00EA0D1307988");
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
