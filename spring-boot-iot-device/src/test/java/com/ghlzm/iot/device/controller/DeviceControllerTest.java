package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceOnboardingBatchActivateDTO;
import com.ghlzm.iot.device.service.DeviceOnboardingActivationService;
import com.ghlzm.iot.device.service.DeviceSecretCustodyService;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceOnboardingBatchResultVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    private DeviceService deviceService;
    @Mock
    private DeviceSecretCustodyService deviceSecretCustodyService;
    @Mock
    private DeviceOnboardingActivationService deviceOnboardingActivationService;

    private DeviceController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceController(deviceService, deviceSecretCustodyService, deviceOnboardingActivationService);
    }

    @Test
    void batchActivateShouldDelegateToActivationService() {
        DeviceOnboardingBatchActivateDTO dto = new DeviceOnboardingBatchActivateDTO();
        dto.setTraceIds(List.of("trace-unregistered-001", "trace-unregistered-002"));
        dto.setConfirmed(Boolean.TRUE);

        DeviceOnboardingBatchResultVO result = new DeviceOnboardingBatchResultVO();
        result.setRequestedCount(2);
        result.setActivatedCount(2);
        result.setRejectedCount(0);
        result.setActivatedTraceIds(List.of("trace-unregistered-001", "trace-unregistered-002"));
        result.setActivatedDeviceCodes(List.of("shadow-device-01", "shadow-device-02"));

        when(deviceOnboardingActivationService.activate(10001L, dto)).thenReturn(result);

        R<DeviceOnboardingBatchResultVO> response = controller.batchActivate(dto, authentication(10001L));

        assertEquals(2, response.getData().getActivatedCount());
        verify(deviceOnboardingActivationService).activate(10001L, dto);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
