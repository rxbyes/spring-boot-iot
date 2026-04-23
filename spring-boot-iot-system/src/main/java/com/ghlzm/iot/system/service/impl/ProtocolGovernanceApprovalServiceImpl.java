package com.ghlzm.iot.system.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.system.protocol.ProtocolSecurityGovernanceApprovalPayloads;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.ProtocolGovernanceApprovalService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.springframework.stereotype.Service;

@Service
public class ProtocolGovernanceApprovalServiceImpl implements ProtocolGovernanceApprovalService {

    private final ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    private final ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    private final GovernanceApprovalPolicyResolver approvalPolicyResolver;
    private final GovernanceApprovalService governanceApprovalService;

    public ProtocolGovernanceApprovalServiceImpl(ProtocolFamilyDefinitionRecordMapper familyRecordMapper,
                                                 ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper,
                                                 GovernanceApprovalPolicyResolver approvalPolicyResolver,
                                                 GovernanceApprovalService governanceApprovalService) {
        this.familyRecordMapper = familyRecordMapper;
        this.decryptProfileRecordMapper = decryptProfileRecordMapper;
        this.approvalPolicyResolver = approvalPolicyResolver;
        this.governanceApprovalService = governanceApprovalService;
    }

    @Override
    public GovernanceSubmissionResultVO submitFamilyPublish(Long familyId, Long operatorUserId, String submitReason) {
        ProtocolFamilyDefinitionRecord record = requireFamily(familyId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                ProtocolSecurityGovernanceApprovalPayloads.buildFamilyPublishCommand(
                        record,
                        operatorUserId,
                        approverUserId,
                        submitReason
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public GovernanceSubmissionResultVO submitFamilyRollback(Long familyId, Long operatorUserId, String submitReason) {
        ProtocolFamilyDefinitionRecord record = requireFamily(familyId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_ROLLBACK,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                ProtocolSecurityGovernanceApprovalPayloads.buildFamilyRollbackCommand(
                        record,
                        operatorUserId,
                        approverUserId,
                        submitReason
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public GovernanceSubmissionResultVO submitDecryptProfilePublish(Long profileId, Long operatorUserId, String submitReason) {
        ProtocolDecryptProfileRecord record = requireProfile(profileId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                ProtocolSecurityGovernanceApprovalPayloads.buildDecryptProfilePublishCommand(
                        record,
                        operatorUserId,
                        approverUserId,
                        submitReason
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    @Override
    public GovernanceSubmissionResultVO submitDecryptProfileRollback(Long profileId, Long operatorUserId, String submitReason) {
        ProtocolDecryptProfileRecord record = requireProfile(profileId);
        Long approverUserId = approvalPolicyResolver.resolveApproverUserId(
                ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK,
                operatorUserId
        );
        Long approvalOrderId = governanceApprovalService.submitAction(
                ProtocolSecurityGovernanceApprovalPayloads.buildDecryptProfileRollbackCommand(
                        record,
                        operatorUserId,
                        approverUserId,
                        submitReason
                )
        );
        return GovernanceSubmissionResultVO.pendingApproval(null, approvalOrderId);
    }

    private ProtocolFamilyDefinitionRecord requireFamily(Long familyId) {
        if (familyId == null || familyId <= 0) {
            throw new BizException("协议族定义不存在: " + familyId);
        }
        ProtocolFamilyDefinitionRecord record = familyRecordMapper.selectById(familyId);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            throw new BizException("协议族定义不存在: " + familyId);
        }
        return record;
    }

    private ProtocolDecryptProfileRecord requireProfile(Long profileId) {
        if (profileId == null || profileId <= 0) {
            throw new BizException("协议解密档案不存在: " + profileId);
        }
        ProtocolDecryptProfileRecord record = decryptProfileRecordMapper.selectById(profileId);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            throw new BizException("协议解密档案不存在: " + profileId);
        }
        return record;
    }
}
