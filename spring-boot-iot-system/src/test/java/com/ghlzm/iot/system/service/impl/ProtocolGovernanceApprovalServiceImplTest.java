package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.system.protocol.ProtocolSecurityGovernanceApprovalPayloads;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocolGovernanceApprovalServiceImplTest {

    @Mock
    private ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    @Mock
    private ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    @Mock
    private GovernanceApprovalPolicyResolver policyResolver;
    @Mock
    private GovernanceApprovalService approvalService;

    private ProtocolGovernanceApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProtocolGovernanceApprovalServiceImpl(
                familyRecordMapper,
                decryptProfileRecordMapper,
                policyResolver,
                approvalService
        );
    }

    @Test
    void submitFamilyPublishShouldCreateApprovalUsingProtocolGovernanceReviewer() {
        when(familyRecordMapper.selectById(9101L)).thenReturn(familyRecord(9101L, "legacy-dp-crack", "DRAFT", 2));
        when(policyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH,
                10001L
        )).thenReturn(20002L);
        when(approvalService.submitAction(any())).thenReturn(99101L);

        GovernanceSubmissionResultVO result = service.submitFamilyPublish(9101L, 10001L, "发布裂缝协议族");

        assertEquals(99101L, result.getApprovalOrderId());
        assertEquals("PENDING", result.getApprovalStatus());
        verify(approvalService).submitAction(argThat(command ->
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH.equals(command.actionCode())
                        && Long.valueOf(20002L).equals(command.approverUserId())
                        && command.payloadJson().contains("\"familyCode\":\"legacy-dp-crack\"")
                        && command.payloadJson().contains("\"expectedVersionNo\":2")
        ));
    }

    @Test
    void submitDecryptProfileRollbackShouldCreateApprovalUsingProtocolGovernanceReviewer() {
        when(decryptProfileRecordMapper.selectById(9201L))
                .thenReturn(profileRecord(9201L, "des-62000001", "ACTIVE", 3));
        when(policyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK,
                10001L
        )).thenReturn(20002L);
        when(approvalService.submitAction(any())).thenReturn(99102L);

        GovernanceSubmissionResultVO result = service.submitDecryptProfileRollback(9201L, 10001L, "回滚 DES 档案");

        assertEquals(99102L, result.getApprovalOrderId());
        assertEquals("PENDING_APPROVAL", result.getExecutionStatus());
        verify(approvalService).submitAction(argThat(command ->
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK.equals(command.actionCode())
                        && Long.valueOf(20002L).equals(command.approverUserId())
                        && command.payloadJson().contains("\"profileCode\":\"des-62000001\"")
        ));
    }

    private ProtocolFamilyDefinitionRecord familyRecord(Long id,
                                                        String familyCode,
                                                        String status,
                                                        Integer versionNo) {
        ProtocolFamilyDefinitionRecord record = new ProtocolFamilyDefinitionRecord();
        record.setId(id);
        record.setFamilyCode(familyCode);
        record.setProtocolCode("mqtt-json");
        record.setDisplayName("demo");
        record.setDecryptProfileCode("des-62000001");
        record.setStatus(status);
        record.setVersionNo(versionNo);
        record.setDeleted(0);
        return record;
    }

    private ProtocolDecryptProfileRecord profileRecord(Long id,
                                                       String profileCode,
                                                       String status,
                                                       Integer versionNo) {
        ProtocolDecryptProfileRecord record = new ProtocolDecryptProfileRecord();
        record.setId(id);
        record.setProfileCode(profileCode);
        record.setAlgorithm("DES");
        record.setMerchantSource("IOT_PROTOCOL_CRYPTO");
        record.setMerchantKey("62000001");
        record.setStatus(status);
        record.setVersionNo(versionNo);
        record.setDeleted(0);
        return record;
    }
}
