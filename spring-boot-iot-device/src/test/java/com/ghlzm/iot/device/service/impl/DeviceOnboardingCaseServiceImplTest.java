package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.entity.DeviceOnboardingCase;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceOnboardingCaseMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingCaseServiceImplTest {

    @Mock
    private DeviceOnboardingCaseMapper mapper;

    @Mock
    private ProductMapper productMapper;

    @Test
    void createCaseShouldPersistDerivedProtocolBlockerWhenProtocolFieldsMissing() {
        when(mapper.insert(any(DeviceOnboardingCase.class))).thenAnswer(invocation -> {
            DeviceOnboardingCase entity = invocation.getArgument(0);
            entity.setId(9101L);
            return 1;
        });

        DeviceOnboardingCaseCreateDTO dto = new DeviceOnboardingCaseCreateDTO();
        dto.setCaseCode(" CASE-9101 ");
        dto.setCaseName(" 裂缝传感器接入 ");
        dto.setScenarioCode(" phase1-crack ");
        dto.setDeviceFamily(" crack_sensor ");

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper);

        DeviceOnboardingCaseVO result = service.createCase(dto, 10001L);

        assertEquals(9101L, result.getId());
        assertEquals("CASE-9101", result.getCaseCode());
        assertEquals("裂缝传感器接入", result.getCaseName());
        assertEquals("PROTOCOL_GOVERNANCE", result.getCurrentStep());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals(List.of("待补齐协议族/解密档案/协议模板"), result.getBlockers());

        ArgumentCaptor<DeviceOnboardingCase> captor = ArgumentCaptor.forClass(DeviceOnboardingCase.class);
        verify(mapper).insert(captor.capture());
        assertEquals("CASE-9101", captor.getValue().getCaseCode());
        assertEquals("裂缝传感器接入", captor.getValue().getCaseName());
        assertEquals("phase1-crack", captor.getValue().getScenarioCode());
        assertEquals("crack_sensor", captor.getValue().getDeviceFamily());
        assertEquals("PROTOCOL_GOVERNANCE", captor.getValue().getCurrentStep());
        assertEquals("BLOCKED", captor.getValue().getStatus());
    }

    @Test
    void refreshStatusShouldBlockAtProtocolGovernanceWhenProtocolFieldsMissing() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode(null);
        entity.setDecryptProfileCode(null);
        entity.setProtocolTemplateCode(null);
        when(mapper.selectById(9101L)).thenReturn(entity);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("PROTOCOL_GOVERNANCE", result.getCurrentStep());
        assertEquals("BLOCKED", result.getStatus());
        assertTrue(result.getBlockers().contains("待补齐协议族/解密档案/协议模板"));
    }

    @Test
    void refreshStatusShouldMoveToAcceptanceWhenReleaseBatchExists() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode("legacy-dp-crack");
        entity.setDecryptProfileCode("aes-62000002");
        entity.setProtocolTemplateCode("nf-crack-v1");
        entity.setProductId(1001L);
        entity.setReleaseBatchId(88001L);
        when(mapper.selectById(9101L)).thenReturn(entity);
        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("ACCEPTANCE", result.getCurrentStep());
        assertEquals("READY", result.getStatus());
        assertTrue(result.getBlockers().isEmpty());
        assertEquals(88001L, result.getReleaseBatchId());
    }

    @Test
    void refreshStatusShouldBlockAtProductGovernanceWhenProductMissing() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode("legacy-dp-crack");
        entity.setDecryptProfileCode("aes-62000002");
        entity.setProtocolTemplateCode("nf-crack-v1");
        entity.setProductId(null);
        entity.setReleaseBatchId(null);
        when(mapper.selectById(9101L)).thenReturn(entity);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("PRODUCT_GOVERNANCE", result.getCurrentStep());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals(List.of("待绑定产品并完成契约治理"), result.getBlockers());
        assertNull(result.getProductId());
    }

    private DeviceOnboardingCase baseCase() {
        DeviceOnboardingCase entity = new DeviceOnboardingCase();
        entity.setId(9101L);
        entity.setTenantId(1L);
        entity.setCaseCode("CASE-9101");
        entity.setCaseName("裂缝传感器接入");
        entity.setScenarioCode("phase1-crack");
        entity.setDeviceFamily("crack_sensor");
        entity.setCurrentStep("PROTOCOL_GOVERNANCE");
        entity.setStatus("BLOCKED");
        return entity;
    }
}
