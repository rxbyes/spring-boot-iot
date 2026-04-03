package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.vo.DeviceAccessErrorStatsVO;
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
class DeviceAccessErrorLogControllerTest {

    @Mock
    private DeviceAccessErrorLogService deviceAccessErrorLogService;

    private DeviceAccessErrorLogController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceAccessErrorLogController(deviceAccessErrorLogService);
    }

    @Test
    void pageLogsShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        DeviceAccessErrorQuery query = new DeviceAccessErrorQuery();
        PageResult<DeviceAccessErrorLog> result = PageResult.of(0L, 1L, 10L, List.of());
        when(deviceAccessErrorLogService.pageLogs(99L, query, 1, 10)).thenReturn(result);

        R<PageResult<DeviceAccessErrorLog>> response = controller.pageLogs(query, 1, 10, authentication);

        assertEquals(0L, response.getData().getTotal());
        verify(deviceAccessErrorLogService).pageLogs(99L, query, 1, 10);
    }

    @Test
    void getStatsShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        DeviceAccessErrorQuery query = new DeviceAccessErrorQuery();
        DeviceAccessErrorStatsVO stats = new DeviceAccessErrorStatsVO();
        stats.setTotal(5L);
        when(deviceAccessErrorLogService.getStats(99L, query)).thenReturn(stats);

        R<DeviceAccessErrorStatsVO> response = controller.getStats(query, authentication);

        assertEquals(5L, response.getData().getTotal());
        verify(deviceAccessErrorLogService).getStats(99L, query);
    }

    @Test
    void getByIdShouldDelegateWithCurrentUser() {
        Authentication authentication = authentication(99L);
        DeviceAccessErrorLog log = new DeviceAccessErrorLog();
        log.setId(2001L);
        when(deviceAccessErrorLogService.getById(99L, 2001L)).thenReturn(log);

        R<DeviceAccessErrorLog> response = controller.getById(2001L, authentication);

        assertEquals(2001L, response.getData().getId());
        verify(deviceAccessErrorLogService).getById(99L, 2001L);
    }

    private Authentication authentication(Long userId) {
        return new UsernamePasswordAuthenticationToken(new JwtUserPrincipal(userId, "tester"), null);
    }
}
