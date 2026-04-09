package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceSecretRotateDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceSecretRotationLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceSecretRotationLogMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceSecretRotateResultVO;
import com.ghlzm.iot.system.entity.AuditLog;
import com.ghlzm.iot.system.service.AuditLogService;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceSecretCustodyServiceImplTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private ProductService productService;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceSecretRotationLogMapper deviceSecretRotationLogMapper;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    @Mock
    private AuditLogService auditLogService;

    private DeviceSecretCustodyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceSecretCustodyServiceImpl(
                deviceService,
                productService,
                deviceMapper,
                deviceSecretRotationLogMapper,
                permissionGuard,
                auditLogService
        );
    }

    @Test
    void rotateDeviceSecretShouldPersistRotationAndAudit() {
        DeviceSecretRotateDTO dto = new DeviceSecretRotateDTO();
        dto.setNewDeviceSecret("secret-new");
        dto.setReason("routine-rotation");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(1L);
        device.setProductId(9001L);
        device.setDeviceCode("device-3001");
        device.setDeviceSecret("secret-old");
        when(deviceService.getRequiredById(1001L, 3001L)).thenReturn(device);
        when(deviceMapper.updateById(any(Device.class))).thenReturn(1);

        Product product = new Product();
        product.setId(9001L);
        product.setProductKey("prod-9001");
        when(productService.getById(9001L)).thenReturn(product);

        DeviceSecretRotateResultVO result = service.rotateDeviceSecret(1001L, 3001L, 2002L, dto);

        assertEquals(3001L, result.getDeviceId());
        assertEquals("device-3001", result.getDeviceCode());
        assertEquals("prod-9001", result.getProductKey());
        assertEquals(1001L, result.getRotatedBy());
        assertEquals(2002L, result.getApprovedBy());
        assertEquals("routine-rotation", result.getReason());
        assertNotNull(result.getRotationBatchId());
        assertNotNull(result.getRotatedAt());
        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "device-secret-rotate",
                "iot:secret-custody:rotate",
                "iot:secret-custody:approve"
        );
        verify(deviceMapper).updateById(any(Device.class));
        verify(deviceSecretRotationLogMapper).insert(any(DeviceSecretRotationLog.class));
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).addLog(auditLogCaptor.capture());
        assertEquals(
                "{\"governanceAction\":\"DEVICE_SECRET_ROTATE\",\"rotationBatchId\":\""
                        + result.getRotationBatchId()
                        + "\",\"approverUserId\":2002,\"reason\":\"routine-rotation\",\"dualControl\":true}",
                auditLogCaptor.getValue().getRequestParams()
        );
    }

    @Test
    void rotateDeviceSecretShouldRejectSameSecret() {
        DeviceSecretRotateDTO dto = new DeviceSecretRotateDTO();
        dto.setNewDeviceSecret("secret-old");
        dto.setReason("routine-rotation");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(1L);
        device.setProductId(9001L);
        device.setDeviceCode("device-3001");
        device.setDeviceSecret("secret-old");
        when(deviceService.getRequiredById(1001L, 3001L)).thenReturn(device);

        assertThrows(BizException.class, () -> service.rotateDeviceSecret(1001L, 3001L, 2002L, dto));
    }
}
