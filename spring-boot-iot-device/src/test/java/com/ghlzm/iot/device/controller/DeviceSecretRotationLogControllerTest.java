package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceSecretRotationLogQuery;
import com.ghlzm.iot.device.service.DeviceSecretRotationLogService;
import com.ghlzm.iot.device.vo.DeviceSecretRotationLogPageItemVO;
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
class DeviceSecretRotationLogControllerTest {

    @Mock
    private DeviceSecretRotationLogService deviceSecretRotationLogService;

    private DeviceSecretRotationLogController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceSecretRotationLogController(deviceSecretRotationLogService);
    }

    @Test
    void pageLogsShouldDelegateToService() {
        DeviceSecretRotationLogQuery query = new DeviceSecretRotationLogQuery();
        query.setProductKey("prod-9001");

        DeviceSecretRotationLogPageItemVO item = new DeviceSecretRotationLogPageItemVO();
        item.setRotationBatchId("ROT-3001-AAAA0001");
        item.setDeviceCode("device-3001");
        when(deviceSecretRotationLogService.pageLogs(1001L, query, 1, 10))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(item)));

        R<PageResult<DeviceSecretRotationLogPageItemVO>> response = controller.pageLogs(query, 1, 10, authentication(1001L));

        assertEquals(200, response.getCode());
        assertEquals(1L, response.getData().getTotal());
        assertEquals("ROT-3001-AAAA0001", response.getData().getRecords().get(0).getRotationBatchId());
        verify(deviceSecretRotationLogService).pageLogs(1001L, query, 1, 10);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
