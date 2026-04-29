package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchCreateDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchStartAcceptanceDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseBatchTemplateApplyDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingCaseCreateDTO;
import com.ghlzm.iot.device.entity.DeviceOnboardingCase;
import com.ghlzm.iot.device.entity.OnboardingTemplatePack;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.DeviceOnboardingAcceptanceGateway;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceLaunch;
import com.ghlzm.iot.device.service.model.DeviceOnboardingAcceptanceProgress;
import com.ghlzm.iot.device.mapper.DeviceOnboardingCaseMapper;
import com.ghlzm.iot.device.mapper.OnboardingTemplatePackMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseBatchResultVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingCaseVO;
import com.ghlzm.iot.framework.observability.evidence.BusinessEventLogRecord;
import com.ghlzm.iot.framework.observability.evidence.ObservabilityEvidenceRecorder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceOnboardingCaseServiceImplTest {

    private static final class RecordingEvidenceRecorder implements ObservabilityEvidenceRecorder {
        private final AtomicReference<BusinessEventLogRecord> lastEvent = new AtomicReference<>();

        @Override
        public void recordBusinessEvent(BusinessEventLogRecord event) {
            lastEvent.set(event);
        }
    }

    @Mock
    private DeviceOnboardingCaseMapper mapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private OnboardingTemplatePackMapper templatePackMapper;

    @Mock
    private DeviceOnboardingAcceptanceGateway acceptanceGateway;

    @Test
    void springContextShouldInstantiateDeviceOnboardingCaseServiceBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(DeviceOnboardingCaseMapper.class, () -> mapper);
            context.registerBean(ProductMapper.class, () -> productMapper);
            context.registerBean(OnboardingTemplatePackMapper.class, () -> templatePackMapper);
            context.register(DeviceOnboardingCaseServiceImpl.class);

            assertDoesNotThrow(context::refresh);
            assertNotNull(context.getBean(DeviceOnboardingCaseServiceImpl.class));
        }
    }

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

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

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
    void createCaseShouldPersistTemplatePackIdWhenTemplatePackExists() {
        when(mapper.insert(any(DeviceOnboardingCase.class))).thenAnswer(invocation -> {
            DeviceOnboardingCase entity = invocation.getArgument(0);
            entity.setId(9102L);
            return 1;
        });
        OnboardingTemplatePack templatePack = new OnboardingTemplatePack();
        templatePack.setId(7001L);
        templatePack.setStatus("ACTIVE");
        when(templatePackMapper.selectById(7001L)).thenReturn(templatePack);

        DeviceOnboardingCaseCreateDTO dto = new DeviceOnboardingCaseCreateDTO();
        dto.setCaseCode(" CASE-9102 ");
        dto.setCaseName(" GNSS 接入 ");
        dto.setTemplatePackId(7001L);

        DeviceOnboardingCaseServiceImpl service =
                new DeviceOnboardingCaseServiceImpl(mapper, productMapper, templatePackMapper, acceptanceGateway);

        DeviceOnboardingCaseVO result = service.createCase(dto, 10001L);

        assertEquals(9102L, result.getId());
        assertEquals(7001L, result.getTemplatePackId());

        ArgumentCaptor<DeviceOnboardingCase> captor = ArgumentCaptor.forClass(DeviceOnboardingCase.class);
        verify(mapper).insert(captor.capture());
        assertEquals(7001L, captor.getValue().getTemplatePackId());
    }

    @Test
    void createCaseShouldRejectWhenTemplatePackDoesNotExist() {
        when(templatePackMapper.selectById(7999L)).thenReturn(null);

        DeviceOnboardingCaseCreateDTO dto = new DeviceOnboardingCaseCreateDTO();
        dto.setCaseCode("CASE-9199");
        dto.setCaseName("无效模板包案例");
        dto.setTemplatePackId(7999L);

        DeviceOnboardingCaseServiceImpl service =
                new DeviceOnboardingCaseServiceImpl(mapper, productMapper, templatePackMapper, acceptanceGateway);

        BizException error = assertThrows(BizException.class, () -> service.createCase(dto, 10001L));

        assertEquals("模板包不存在: 7999", error.getMessage());
    }

    @Test
    void refreshStatusShouldBlockAtProtocolGovernanceWhenProtocolFieldsMissing() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode(null);
        entity.setDecryptProfileCode(null);
        entity.setProtocolTemplateCode(null);
        when(mapper.selectById(9101L)).thenReturn(entity);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

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
        entity.setDeviceCode("DEV-9101");
        when(mapper.selectById(9101L)).thenReturn(entity);
        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("ACCEPTANCE", result.getCurrentStep());
        assertEquals("READY", result.getStatus());
        assertTrue(result.getBlockers().isEmpty());
        assertEquals(88001L, result.getReleaseBatchId());
        assertEquals("DEV-9101", result.getDeviceCode());
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

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("PRODUCT_GOVERNANCE", result.getCurrentStep());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals(List.of("待绑定产品并完成契约治理"), result.getBlockers());
        assertNull(result.getProductId());
    }

    @Test
    void refreshStatusShouldBlockAcceptanceWhenDeviceCodeMissing() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode("legacy-dp-crack");
        entity.setDecryptProfileCode("aes-62000002");
        entity.setProtocolTemplateCode("nf-crack-v1");
        entity.setProductId(1001L);
        entity.setReleaseBatchId(88001L);
        entity.setDeviceCode(null);
        when(mapper.selectById(9101L)).thenReturn(entity);
        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("ACCEPTANCE", result.getCurrentStep());
        assertEquals("BLOCKED", result.getStatus());
        assertEquals(List.of("待补齐验收设备编码"), result.getBlockers());
        assertNull(result.getDeviceCode());
    }

    @Test
    void startAcceptanceShouldPersistLaunchReferenceAndRunningSummary() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode("legacy-dp-crack");
        entity.setDecryptProfileCode("aes-62000002");
        entity.setProtocolTemplateCode("nf-crack-v1");
        entity.setProductId(1001L);
        entity.setReleaseBatchId(88001L);
        entity.setDeviceCode("DEV-9101");
        when(mapper.selectById(9101L)).thenReturn(entity);
        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(acceptanceGateway.launch(any()))
                .thenReturn(new DeviceOnboardingAcceptanceLaunch("job-onboarding-1"));

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);
        RecordingEvidenceRecorder evidenceRecorder = new RecordingEvidenceRecorder();
        service.setObservabilityEvidenceRecorder(evidenceRecorder);

        DeviceOnboardingCaseVO result = service.startAcceptance(9101L, 10001L);

        assertEquals("ACCEPTANCE", result.getCurrentStep());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals("DEV-9101", result.getDeviceCode());
        assertNotNull(result.getAcceptance());
        assertEquals("RUNNING", result.getAcceptance().getStatus());
        assertEquals("job-onboarding-1", result.getAcceptance().getJobId());
        assertEquals("标准接入验收执行中", result.getAcceptance().getSummary());

        BusinessEventLogRecord event = evidenceRecorder.lastEvent.get();
        assertNotNull(event);
        assertEquals("acceptance.onboarding_case.launched", event.getEventCode());
        assertEquals("9101", event.getObjectId());
        assertEquals("job-onboarding-1", event.getMetadata().get("acceptanceJobId"));
        assertEquals("DEV-9101", event.getMetadata().get("deviceCode"));
        assertEquals(88001L, event.getMetadata().get("releaseBatchId"));
    }

    @Test
    void refreshStatusShouldCapturePassedAcceptanceResult() {
        DeviceOnboardingCase entity = baseCase();
        entity.setProtocolFamilyCode("legacy-dp-crack");
        entity.setDecryptProfileCode("aes-62000002");
        entity.setProtocolTemplateCode("nf-crack-v1");
        entity.setProductId(1001L);
        entity.setReleaseBatchId(88001L);
        entity.setDeviceCode("DEV-9101");
        entity.setAcceptanceJobId("job-onboarding-1");
        when(mapper.selectById(9101L)).thenReturn(entity);
        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(acceptanceGateway.getProgress("job-onboarding-1", null))
                .thenReturn(new DeviceOnboardingAcceptanceProgress(
                        "job-onboarding-1",
                        "20260418193000",
                        "PASSED",
                        "8/8 检查项通过",
                        List.of(),
                        "/automation-results?runId=20260418193000"
                ));

        DeviceOnboardingCaseServiceImpl service = new DeviceOnboardingCaseServiceImpl(mapper, productMapper, acceptanceGateway);

        DeviceOnboardingCaseVO result = service.refreshStatus(9101L, 10001L);

        assertEquals("ACCEPTANCE", result.getCurrentStep());
        assertEquals("READY", result.getStatus());
        assertTrue(result.getBlockers().isEmpty());
        assertNotNull(result.getAcceptance());
        assertEquals("PASSED", result.getAcceptance().getStatus());
        assertEquals("20260418193000", result.getAcceptance().getRunId());
        assertEquals("/automation-results?runId=20260418193000", result.getAcceptance().getJumpPath());
    }

    @Test
    void batchCreateCasesShouldCreateValidCasesAndGroupFailures() {
        when(mapper.selectOne(any())).thenReturn(null, existingCase("CASE-9199"));
        when(mapper.insert(any(DeviceOnboardingCase.class))).thenAnswer(invocation -> {
            DeviceOnboardingCase entity = invocation.getArgument(0);
            entity.setId(9201L);
            return 1;
        });

        DeviceOnboardingCaseCreateDTO created = new DeviceOnboardingCaseCreateDTO();
        created.setCaseCode("CASE-9201");
        created.setCaseName("裂缝案例 01");

        DeviceOnboardingCaseCreateDTO duplicated = new DeviceOnboardingCaseCreateDTO();
        duplicated.setCaseCode("CASE-9199");
        duplicated.setCaseName("裂缝案例 99");

        DeviceOnboardingCaseBatchCreateDTO dto = new DeviceOnboardingCaseBatchCreateDTO();
        dto.setItems(List.of(created, duplicated));

        DeviceOnboardingCaseServiceImpl service =
                new DeviceOnboardingCaseServiceImpl(mapper, productMapper, templatePackMapper, acceptanceGateway);

        DeviceOnboardingCaseBatchResultVO result = service.batchCreateCases(dto, 10001L);

        assertEquals("BATCH_CREATE", result.getAction());
        assertEquals(2, result.getRequestedCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("CASE-9201", result.getSuccessItems().get(0).getCaseCode());
        assertEquals("接入案例编码已存在: CASE-9199", result.getFailureItems().get(0).getMessage());
        assertEquals("接入案例编码已存在: CASE-9199", result.getFailureGroups().get(0).getSummary());
    }

    @Test
    void batchApplyTemplatePackShouldUpdateCasesAndGroupFailures() {
        OnboardingTemplatePack templatePack = new OnboardingTemplatePack();
        templatePack.setId(7001L);
        templatePack.setPackCode("PACK-CRACK-V1");
        templatePack.setPackName("裂缝模板包");
        templatePack.setScenarioCode("phase1-crack");
        templatePack.setDeviceFamily("crack_sensor");
        templatePack.setProtocolFamilyCode("legacy-dp-crack");
        templatePack.setDecryptProfileCode("aes-62000002");
        templatePack.setProtocolTemplateCode("nf-crack-v1");
        when(templatePackMapper.selectById(7001L)).thenReturn(templatePack);

        DeviceOnboardingCase entity = baseCase();
        entity.setId(9101L);
        when(mapper.selectById(9101L)).thenReturn(entity);
        when(mapper.selectById(9199L)).thenReturn(null);

        DeviceOnboardingCaseBatchTemplateApplyDTO dto = new DeviceOnboardingCaseBatchTemplateApplyDTO();
        dto.setCaseIds(List.of(9101L, 9199L));
        dto.setTemplatePackId(7001L);

        DeviceOnboardingCaseServiceImpl service =
                new DeviceOnboardingCaseServiceImpl(mapper, productMapper, templatePackMapper, acceptanceGateway);

        DeviceOnboardingCaseBatchResultVO result = service.batchApplyTemplatePack(dto, 10001L);

        assertEquals("BATCH_APPLY_TEMPLATE", result.getAction());
        assertEquals(2, result.getRequestedCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("PRODUCT_GOVERNANCE", result.getSuccessItems().get(0).getCurrentStep());
        assertEquals("接入案例不存在: 9199", result.getFailureItems().get(0).getMessage());
        assertEquals("接入案例不存在: 9199", result.getFailureGroups().get(0).getSummary());

        ArgumentCaptor<DeviceOnboardingCase> captor = ArgumentCaptor.forClass(DeviceOnboardingCase.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(7001L, captor.getValue().getTemplatePackId());
        assertEquals("phase1-crack", captor.getValue().getScenarioCode());
        assertEquals("crack_sensor", captor.getValue().getDeviceFamily());
        assertEquals("legacy-dp-crack", captor.getValue().getProtocolFamilyCode());
        assertEquals("aes-62000002", captor.getValue().getDecryptProfileCode());
        assertEquals("nf-crack-v1", captor.getValue().getProtocolTemplateCode());
    }

    @Test
    void batchStartAcceptanceShouldStartReadyCasesAndGroupFailures() {
        DeviceOnboardingCase readyCase = baseCase();
        readyCase.setId(9101L);
        readyCase.setProtocolFamilyCode("legacy-dp-crack");
        readyCase.setDecryptProfileCode("aes-62000002");
        readyCase.setProtocolTemplateCode("nf-crack-v1");
        readyCase.setProductId(1001L);
        readyCase.setReleaseBatchId(88001L);
        readyCase.setDeviceCode("DEV-9101");

        DeviceOnboardingCase blockedCase = baseCase();
        blockedCase.setId(9102L);
        blockedCase.setCaseCode("CASE-9102");
        blockedCase.setCaseName("裂缝传感器接入 02");
        blockedCase.setProtocolFamilyCode("legacy-dp-crack");
        blockedCase.setDecryptProfileCode("aes-62000002");
        blockedCase.setProtocolTemplateCode("nf-crack-v1");
        blockedCase.setProductId(1001L);
        blockedCase.setReleaseBatchId(88001L);
        blockedCase.setDeviceCode(null);

        Product product = new Product();
        product.setId(1001L);

        when(mapper.selectById(9101L)).thenReturn(readyCase);
        when(mapper.selectById(9102L)).thenReturn(blockedCase);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(acceptanceGateway.launch(any())).thenReturn(new DeviceOnboardingAcceptanceLaunch("job-onboarding-1"));

        DeviceOnboardingCaseBatchStartAcceptanceDTO dto = new DeviceOnboardingCaseBatchStartAcceptanceDTO();
        dto.setCaseIds(List.of(9101L, 9102L));

        DeviceOnboardingCaseServiceImpl service =
                new DeviceOnboardingCaseServiceImpl(mapper, productMapper, templatePackMapper, acceptanceGateway);

        DeviceOnboardingCaseBatchResultVO result = service.batchStartAcceptance(dto, 10001L);

        assertEquals("BATCH_START_ACCEPTANCE", result.getAction());
        assertEquals(2, result.getRequestedCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
        assertEquals("CASE-9101", result.getSuccessItems().get(0).getCaseCode());
        assertEquals("待补齐验收设备编码", result.getFailureItems().get(0).getMessage());
        assertEquals("待补齐验收设备编码", result.getFailureGroups().get(0).getSummary());
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

    private DeviceOnboardingCase existingCase(String caseCode) {
        DeviceOnboardingCase entity = new DeviceOnboardingCase();
        entity.setId(9999L);
        entity.setTenantId(1L);
        entity.setCaseCode(caseCode);
        entity.setDeleted(0);
        return entity;
    }
}
