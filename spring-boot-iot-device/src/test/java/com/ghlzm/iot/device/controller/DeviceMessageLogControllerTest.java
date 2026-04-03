package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
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
class DeviceMessageLogControllerTest {

    @Mock
    private DeviceMessageService deviceMessageService;

    private DeviceMessageLogController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceMessageLogController(deviceMessageService);
    }

    @Test
    void getLogsShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        when(deviceMessageService.listMessageLogs(99L, "demo-device-01")).thenReturn(List.of(new DeviceMessageLog()));

        R<List<DeviceMessageLog>> response = controller.getLogs("demo-device-01", authentication);

        assertEquals(1, response.getData().size());
        verify(deviceMessageService).listMessageLogs(99L, "demo-device-01");
    }

    @Test
    void pageTraceLogsShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        DeviceMessageTraceQuery query = new DeviceMessageTraceQuery();
        PageResult<DeviceMessageLog> result = PageResult.of(0L, 1L, 10L, List.of());
        when(deviceMessageService.pageMessageTraceLogs(99L, query, 1, 10)).thenReturn(result);

        R<PageResult<DeviceMessageLog>> response = controller.pageTraceLogs(query, 1, 10, authentication);

        assertEquals(0L, response.getData().getTotal());
        verify(deviceMessageService).pageMessageTraceLogs(99L, query, 1, 10);
    }

    @Test
    void getTraceStatsShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        DeviceMessageTraceStatsVO stats = new DeviceMessageTraceStatsVO();
        stats.setTotal(12L);
        DeviceMessageTraceQuery query = new DeviceMessageTraceQuery();
        when(deviceMessageService.getMessageTraceStats(99L, query)).thenReturn(stats);

        R<DeviceMessageTraceStatsVO> response = controller.getTraceStats(query, authentication);

        assertEquals(12L, response.getData().getTotal());
        verify(deviceMessageService).getMessageTraceStats(99L, query);
    }

    private Authentication authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(new JwtUserPrincipal(userId, "tester"), null);
    }
}
