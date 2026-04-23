package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchStartAcceptanceDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchTemplateApplyDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseQueryDTO;
import com.ghlzm.iot.device.service.DeviceOnboardingCaseService;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingCaseControllerTest {

    @Mock
    private DeviceOnboardingCaseService service;

    private DeviceOnboardingCaseController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceOnboardingCaseController(service);
    }

    @Test
    void pageCasesShouldDelegateToService() {
        DeviceOnboardingCaseVO row = row();
        when(service.pageCases(any(DeviceOnboardingCaseQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

        R<PageResult<DeviceOnboardingCaseVO>> response = controller.pageCases(new DeviceOnboardingCaseQueryDTO());

        assertEquals(1L, response.getData().getTotal());
        assertEquals("裂缝传感器接入", response.getData().getRecords().get(0).getCaseName());
        verify(service).pageCases(any(DeviceOnboardingCaseQueryDTO.class));
    }

    @Test
    void createCaseShouldUseCurrentUserId() {
        DeviceOnboardingCaseCreateDTO dto = new DeviceOnboardingCaseCreateDTO();
        dto.setCaseCode("CASE-9101");
        dto.setCaseName("裂缝传感器接入");
        DeviceOnboardingCaseVO row = row();
        when(service.createCase(dto, 10001L)).thenReturn(row);

        R<DeviceOnboardingCaseVO> response = controller.createCase(dto, authentication(10001L));

        assertEquals(9101L, response.getData().getId());
        verify(service).createCase(dto, 10001L);
    }

    @Test
    void refreshStatusShouldUseCurrentUserId() {
        DeviceOnboardingCaseVO row = row();
        row.setCurrentStep("CONTRACT_RELEASE");
        row.setStatus("IN_PROGRESS");
        when(service.refreshStatus(9101L, 10001L)).thenReturn(row);

        R<DeviceOnboardingCaseVO> response = controller.refreshStatus(9101L, authentication(10001L));

        assertEquals("CONTRACT_RELEASE", response.getData().getCurrentStep());
        assertEquals("IN_PROGRESS", response.getData().getStatus());
        verify(service).refreshStatus(9101L, 10001L);
    }

    @Test
    void startAcceptanceShouldUseCurrentUserId() {
        DeviceOnboardingCaseVO row = row();
        row.setCurrentStep("ACCEPTANCE");
        row.setStatus("IN_PROGRESS");
        when(service.startAcceptance(9101L, 10001L)).thenReturn(row);

        R<DeviceOnboardingCaseVO> response = controller.startAcceptance(9101L, authentication(10001L));

        assertEquals("ACCEPTANCE", response.getData().getCurrentStep());
        assertEquals("IN_PROGRESS", response.getData().getStatus());
        verify(service).startAcceptance(9101L, 10001L);
    }

    @Test
    void batchCreateShouldUseCurrentUserId() {
        DeviceOnboardingCaseBatchCreateDTO dto = new DeviceOnboardingCaseBatchCreateDTO();
        DeviceOnboardingCaseBatchResultVO result = batchResult("BATCH_CREATE");
        when(service.batchCreateCases(dto, 10001L)).thenReturn(result);

        R<DeviceOnboardingCaseBatchResultVO> response = controller.batchCreate(dto, authentication(10001L));

        assertEquals("BATCH_CREATE", response.getData().getAction());
        verify(service).batchCreateCases(dto, 10001L);
    }

    @Test
    void batchApplyTemplateShouldUseCurrentUserId() {
        DeviceOnboardingCaseBatchTemplateApplyDTO dto = new DeviceOnboardingCaseBatchTemplateApplyDTO();
        DeviceOnboardingCaseBatchResultVO result = batchResult("BATCH_APPLY_TEMPLATE");
        when(service.batchApplyTemplatePack(dto, 10001L)).thenReturn(result);

        R<DeviceOnboardingCaseBatchResultVO> response = controller.batchApplyTemplate(dto, authentication(10001L));

        assertEquals("BATCH_APPLY_TEMPLATE", response.getData().getAction());
        verify(service).batchApplyTemplatePack(dto, 10001L);
    }

    @Test
    void batchStartAcceptanceShouldUseCurrentUserId() {
        DeviceOnboardingCaseBatchStartAcceptanceDTO dto = new DeviceOnboardingCaseBatchStartAcceptanceDTO();
        DeviceOnboardingCaseBatchResultVO result = batchResult("BATCH_START_ACCEPTANCE");
        when(service.batchStartAcceptance(dto, 10001L)).thenReturn(result);

        R<DeviceOnboardingCaseBatchResultVO> response = controller.batchStartAcceptance(dto, authentication(10001L));

        assertEquals("BATCH_START_ACCEPTANCE", response.getData().getAction());
        verify(service).batchStartAcceptance(dto, 10001L);
    }

    private DeviceOnboardingCaseVO row() {
        DeviceOnboardingCaseVO row = new DeviceOnboardingCaseVO();
        row.setId(9101L);
        row.setCaseCode("CASE-9101");
        row.setCaseName("裂缝传感器接入");
        row.setCurrentStep("PRODUCT_GOVERNANCE");
        row.setStatus("BLOCKED");
        row.setBlockers(List.of("待绑定产品并完成契约治理"));
        return row;
    }

    private DeviceOnboardingCaseBatchResultVO batchResult(String action) {
        DeviceOnboardingCaseBatchResultVO result = new DeviceOnboardingCaseBatchResultVO();
        result.setAction(action);
        result.setRequestedCount(1);
        result.setSuccessCount(1);
        result.setFailedCount(0);
        return result;
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
