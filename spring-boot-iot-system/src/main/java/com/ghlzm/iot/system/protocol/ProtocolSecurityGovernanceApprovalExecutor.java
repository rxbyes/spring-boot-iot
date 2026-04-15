package com.ghlzm.iot.system.protocol;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileSnapshot;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileSnapshotMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionSnapshotMapper;
import com.ghlzm.iot.system.entity.GovernanceApprovalOrder;
import com.ghlzm.iot.system.service.GovernanceApprovalActionExecutor;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionExecutionResult;
import com.ghlzm.iot.system.service.model.GovernanceImpactSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceRollbackSnapshot;
import com.ghlzm.iot.system.service.model.GovernanceSimulationResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProtocolSecurityGovernanceApprovalExecutor implements GovernanceApprovalActionExecutor {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String SNAPSHOT_STATUS_PUBLISHED = "PUBLISHED";
    private static final String SNAPSHOT_STATUS_ROLLED_BACK = "ROLLED_BACK";
    private static final List<String> AFFECTED_TYPES = List.of("PROTOCOL", "SECURITY");

    private final ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    private final ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    private final ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper;
    private final ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper;

    public ProtocolSecurityGovernanceApprovalExecutor(ProtocolFamilyDefinitionRecordMapper familyRecordMapper,
                                                      ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper,
                                                      ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper,
                                                      ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper) {
        this.familyRecordMapper = familyRecordMapper;
        this.decryptProfileRecordMapper = decryptProfileRecordMapper;
        this.familySnapshotMapper = familySnapshotMapper;
        this.decryptProfileSnapshotMapper = decryptProfileSnapshotMapper;
    }

    @Override
    public boolean supports(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return false;
        }
        String normalized = actionCode.trim();
        return ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH.equals(normalized)
                || ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_ROLLBACK.equals(normalized)
                || ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH.equals(normalized)
                || ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK.equals(normalized);
    }

    @Override
    public GovernanceApprovalActionExecutionResult execute(GovernanceApprovalOrder order) {
        String actionCode = normalizeActionCode(order);
        return switch (actionCode) {
            case ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH -> executeFamilyPublish(order);
            case ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_ROLLBACK -> executeFamilyRollback(order);
            case ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH -> executeDecryptPublish(order);
            case ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK -> executeDecryptRollback(order);
            default -> throw new BizException("协议治理审批动作不支持执行: " + actionCode);
        };
    }

    @Override
    public GovernanceSimulationResult simulate(GovernanceApprovalOrder order) {
        String actionCode = normalizeActionCode(order);
        return switch (actionCode) {
            case ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_PUBLISH,
                 ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_FAMILY_ROLLBACK,
                 ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH,
                 ProtocolSecurityGovernanceApprovalPayloads.ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK -> buildSimulation(order);
            default -> throw new BizException("协议治理审批动作不支持预演: " + actionCode);
        };
    }

    private GovernanceApprovalActionExecutionResult executeFamilyPublish(GovernanceApprovalOrder order) {
        ProtocolSecurityGovernanceApprovalPayloads.FamilyApprovalPayload payload =
                ProtocolSecurityGovernanceApprovalPayloads.readFamilyPublishPayload(order.getPayloadJson());
        ProtocolFamilyDefinitionRecord record = requireFamily(payload.familyId());
        ensureExpectedVersion(record.getVersionNo(), payload.expectedVersionNo(), "协议族定义");

        ProtocolFamilyDefinitionSnapshot snapshot = new ProtocolFamilyDefinitionSnapshot();
        snapshot.setFamilyId(record.getId());
        snapshot.setApprovalOrderId(order.getId());
        snapshot.setPublishedVersionNo(record.getVersionNo());
        snapshot.setSnapshotJson(ProtocolSecurityGovernanceApprovalPayloads.writeFamilySnapshotJson(record));
        snapshot.setLifecycleStatus(SNAPSHOT_STATUS_PUBLISHED);
        if (familySnapshotMapper.insert(snapshot) <= 0) {
            throw new BizException("协议族定义发布快照写入失败");
        }

        ProtocolFamilyDefinitionRecord update = new ProtocolFamilyDefinitionRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_ACTIVE);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (familyRecordMapper.updateById(update) <= 0) {
            throw new BizException("协议族定义发布状态更新失败");
        }

        String payloadJson = ProtocolSecurityGovernanceApprovalPayloads.writeFamilyExecutionPayload(
                payload,
                order.getId(),
                record.getVersionNo(),
                SNAPSHOT_STATUS_PUBLISHED
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, payloadJson, null);
    }

    private GovernanceApprovalActionExecutionResult executeFamilyRollback(GovernanceApprovalOrder order) {
        ProtocolSecurityGovernanceApprovalPayloads.FamilyApprovalPayload payload =
                ProtocolSecurityGovernanceApprovalPayloads.readFamilyRollbackPayload(order.getPayloadJson());
        ProtocolFamilyDefinitionRecord record = requireFamily(payload.familyId());
        ensureExpectedVersion(record.getVersionNo(), payload.expectedVersionNo(), "协议族定义");

        ProtocolFamilyDefinitionSnapshot snapshot = familySnapshotMapper.selectLatestPublishedByFamilyId(record.getId());
        if (snapshot == null || snapshot.getId() == null) {
            throw new BizException("协议族定义不存在可回滚的正式快照: " + record.getId());
        }
        ProtocolFamilyDefinitionSnapshot updateSnapshot = new ProtocolFamilyDefinitionSnapshot();
        updateSnapshot.setId(snapshot.getId());
        updateSnapshot.setLifecycleStatus(SNAPSHOT_STATUS_ROLLED_BACK);
        if (familySnapshotMapper.updateById(updateSnapshot) <= 0) {
            throw new BizException("协议族定义回滚快照更新失败");
        }

        ProtocolFamilyDefinitionRecord update = new ProtocolFamilyDefinitionRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_DRAFT);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (familyRecordMapper.updateById(update) <= 0) {
            throw new BizException("协议族定义回滚状态更新失败");
        }

        String payloadJson = ProtocolSecurityGovernanceApprovalPayloads.writeFamilyExecutionPayload(
                payload,
                order.getId(),
                snapshot.getPublishedVersionNo(),
                SNAPSHOT_STATUS_ROLLED_BACK
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, null, payloadJson);
    }

    private GovernanceApprovalActionExecutionResult executeDecryptPublish(GovernanceApprovalOrder order) {
        ProtocolSecurityGovernanceApprovalPayloads.DecryptProfileApprovalPayload payload =
                ProtocolSecurityGovernanceApprovalPayloads.readDecryptProfilePublishPayload(order.getPayloadJson());
        ProtocolDecryptProfileRecord record = requireProfile(payload.profileId());
        ensureExpectedVersion(record.getVersionNo(), payload.expectedVersionNo(), "协议解密档案");

        ProtocolDecryptProfileSnapshot snapshot = new ProtocolDecryptProfileSnapshot();
        snapshot.setProfileId(record.getId());
        snapshot.setApprovalOrderId(order.getId());
        snapshot.setPublishedVersionNo(record.getVersionNo());
        snapshot.setSnapshotJson(ProtocolSecurityGovernanceApprovalPayloads.writeDecryptProfileSnapshotJson(record));
        snapshot.setLifecycleStatus(SNAPSHOT_STATUS_PUBLISHED);
        if (decryptProfileSnapshotMapper.insert(snapshot) <= 0) {
            throw new BizException("协议解密档案发布快照写入失败");
        }

        ProtocolDecryptProfileRecord update = new ProtocolDecryptProfileRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_ACTIVE);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (decryptProfileRecordMapper.updateById(update) <= 0) {
            throw new BizException("协议解密档案发布状态更新失败");
        }

        String payloadJson = ProtocolSecurityGovernanceApprovalPayloads.writeDecryptProfileExecutionPayload(
                payload,
                order.getId(),
                record.getVersionNo(),
                SNAPSHOT_STATUS_PUBLISHED
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, payloadJson, null);
    }

    private GovernanceApprovalActionExecutionResult executeDecryptRollback(GovernanceApprovalOrder order) {
        ProtocolSecurityGovernanceApprovalPayloads.DecryptProfileApprovalPayload payload =
                ProtocolSecurityGovernanceApprovalPayloads.readDecryptProfileRollbackPayload(order.getPayloadJson());
        ProtocolDecryptProfileRecord record = requireProfile(payload.profileId());
        ensureExpectedVersion(record.getVersionNo(), payload.expectedVersionNo(), "协议解密档案");

        ProtocolDecryptProfileSnapshot snapshot = decryptProfileSnapshotMapper.selectLatestPublishedByProfileId(record.getId());
        if (snapshot == null || snapshot.getId() == null) {
            throw new BizException("协议解密档案不存在可回滚的正式快照: " + record.getId());
        }
        ProtocolDecryptProfileSnapshot updateSnapshot = new ProtocolDecryptProfileSnapshot();
        updateSnapshot.setId(snapshot.getId());
        updateSnapshot.setLifecycleStatus(SNAPSHOT_STATUS_ROLLED_BACK);
        if (decryptProfileSnapshotMapper.updateById(updateSnapshot) <= 0) {
            throw new BizException("协议解密档案回滚快照更新失败");
        }

        ProtocolDecryptProfileRecord update = new ProtocolDecryptProfileRecord();
        update.setId(record.getId());
        update.setStatus(STATUS_DRAFT);
        update.setApprovalOrderId(order.getId());
        update.setUpdateBy(order.getOperatorUserId());
        if (decryptProfileRecordMapper.updateById(update) <= 0) {
            throw new BizException("协议解密档案回滚状态更新失败");
        }

        String payloadJson = ProtocolSecurityGovernanceApprovalPayloads.writeDecryptProfileExecutionPayload(
                payload,
                order.getId(),
                snapshot.getPublishedVersionNo(),
                SNAPSHOT_STATUS_ROLLED_BACK
        );
        return new GovernanceApprovalActionExecutionResult(payloadJson, null, payloadJson);
    }

    private GovernanceSimulationResult buildSimulation(GovernanceApprovalOrder order) {
        GovernanceImpactSnapshot impact = new GovernanceImpactSnapshot();
        impact.setAffectedCount(1L);
        impact.setAffectedTypes(AFFECTED_TYPES);
        impact.setRollbackable(Boolean.TRUE);
        impact.setRollbackPlanSummary("审批通过后可通过协议治理回滚动作恢复上一正式版本");

        GovernanceRollbackSnapshot rollback = new GovernanceRollbackSnapshot();
        rollback.setRollbackable(Boolean.TRUE);
        rollback.setRollbackPlanSummary("审批通过后可通过协议治理回滚动作恢复上一正式版本");

        return new GovernanceSimulationResult(
                order.getId(),
                order.getWorkItemId(),
                order.getActionCode(),
                true,
                1L,
                AFFECTED_TYPES,
                true,
                impact.getRollbackPlanSummary(),
                null,
                impact,
                rollback,
                false,
                null
        );
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

    private void ensureExpectedVersion(Integer currentVersion, Integer expectedVersion, String subjectName) {
        if (currentVersion == null || expectedVersion == null || currentVersion.equals(expectedVersion)) {
            return;
        }
        throw new BizException(subjectName + "版本已变更，请刷新后重试");
    }

    private String normalizeActionCode(GovernanceApprovalOrder order) {
        if (order == null || !StringUtils.hasText(order.getActionCode())) {
            throw new BizException("审批动作不存在");
        }
        return order.getActionCode().trim();
    }
}
