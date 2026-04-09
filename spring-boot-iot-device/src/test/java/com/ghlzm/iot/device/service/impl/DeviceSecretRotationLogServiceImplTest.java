package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceSecretRotationLogQuery;
import com.ghlzm.iot.device.entity.DeviceSecretRotationLog;
import com.ghlzm.iot.device.mapper.DeviceSecretRotationLogMapper;
import com.ghlzm.iot.device.vo.DeviceSecretRotationLogPageItemVO;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.vo.UserAuthContextVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSecretRotationLogServiceImplTest {

    @Mock
    private DeviceSecretRotationLogMapper mapper;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    @Mock
    private PermissionService permissionService;

    private DeviceSecretRotationLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceSecretRotationLogServiceImpl(mapper, permissionGuard, permissionService);
    }

    @Test
    void pageLogsShouldFilterByDeviceCodeAndRequireSecretCustodyViewPermission() {
        DeviceSecretRotationLogQuery query = new DeviceSecretRotationLogQuery();
        query.setDeviceCode("device-3001");

        UserAuthContextVO authContext = new UserAuthContextVO();
        authContext.setTenantId(1L);
        when(permissionService.getUserAuthContext(1001L)).thenReturn(authContext);

        DeviceSecretRotationLog log = new DeviceSecretRotationLog();
        log.setId(9001L);
        log.setTenantId(1L);
        log.setDeviceId(3001L);
        log.setDeviceCode("device-3001");
        log.setProductKey("prod-9001");
        log.setRotationBatchId("ROT-3001-AAAA0001");
        log.setPreviousSecretDigest("old-digest");
        log.setCurrentSecretDigest("new-digest");
        log.setRotateTime(LocalDateTime.of(2026, 4, 9, 12, 0));

        Page<DeviceSecretRotationLog> page = new Page<>(1L, 10L);
        page.setTotal(1L);
        page.setRecords(List.of(log));
        when(mapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<DeviceSecretRotationLogPageItemVO> result = service.pageLogs(1001L, query, 1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals("device-3001", result.getRecords().get(0).getDeviceCode());
        assertEquals("prod-9001", result.getRecords().get(0).getProductKey());
        assertNotNull(result.getRecords().get(0).getPreviousSecretDigest());
        assertNotNull(result.getRecords().get(0).getCurrentSecretDigest());
        verify(permissionGuard).requireAnyPermission(1001L, "密钥托管查看", "iot:secret-custody:view");
        verify(permissionService).getUserAuthContext(1001L);
        verify(mapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
}
