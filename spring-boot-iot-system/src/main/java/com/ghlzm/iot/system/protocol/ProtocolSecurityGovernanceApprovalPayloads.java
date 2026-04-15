package com.ghlzm.iot.system.protocol;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.system.service.model.GovernanceApprovalActionCommand;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class ProtocolSecurityGovernanceApprovalPayloads {

    public static final String ACTION_PROTOCOL_FAMILY_PUBLISH = "PROTOCOL_FAMILY_PUBLISH";
    public static final String ACTION_PROTOCOL_FAMILY_ROLLBACK = "PROTOCOL_FAMILY_ROLLBACK";
    public static final String ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH = "PROTOCOL_DECRYPT_PROFILE_PUBLISH";
    public static final String ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK = "PROTOCOL_DECRYPT_PROFILE_ROLLBACK";

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

    private ProtocolSecurityGovernanceApprovalPayloads() {
    }

    public static GovernanceApprovalActionCommand buildFamilyPublishCommand(ProtocolFamilyDefinitionRecord record,
                                                                           Long operatorUserId,
                                                                           Long approverUserId,
                                                                           String submitReason) {
        FamilyApprovalPayload payload = buildFamilyPayload(record, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_PROTOCOL_FAMILY_PUBLISH,
                "协议族定义发布",
                "PROTOCOL_FAMILY",
                record == null ? null : record.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static GovernanceApprovalActionCommand buildFamilyRollbackCommand(ProtocolFamilyDefinitionRecord record,
                                                                            Long operatorUserId,
                                                                            Long approverUserId,
                                                                            String submitReason) {
        FamilyApprovalPayload payload = buildFamilyPayload(record, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_PROTOCOL_FAMILY_ROLLBACK,
                "协议族定义回滚",
                "PROTOCOL_FAMILY",
                record == null ? null : record.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static GovernanceApprovalActionCommand buildDecryptProfilePublishCommand(ProtocolDecryptProfileRecord record,
                                                                                    Long operatorUserId,
                                                                                    Long approverUserId,
                                                                                    String submitReason) {
        DecryptProfileApprovalPayload payload = buildDecryptPayload(record, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_PROTOCOL_DECRYPT_PROFILE_PUBLISH,
                "协议解密档案发布",
                "PROTOCOL_DECRYPT_PROFILE",
                record == null ? null : record.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static GovernanceApprovalActionCommand buildDecryptProfileRollbackCommand(ProtocolDecryptProfileRecord record,
                                                                                     Long operatorUserId,
                                                                                     Long approverUserId,
                                                                                     String submitReason) {
        DecryptProfileApprovalPayload payload = buildDecryptPayload(record, submitReason);
        return new GovernanceApprovalActionCommand(
                ACTION_PROTOCOL_DECRYPT_PROFILE_ROLLBACK,
                "协议解密档案回滚",
                "PROTOCOL_DECRYPT_PROFILE",
                record == null ? null : record.getId(),
                operatorUserId,
                approverUserId,
                writePayload(payload),
                normalizeText(submitReason)
        );
    }

    public static FamilyApprovalPayload readFamilyPublishPayload(String payloadJson) {
        return readPayload(payloadJson, FamilyApprovalPayload.class, "协议族定义发布审批载荷解析失败");
    }

    public static FamilyApprovalPayload readFamilyRollbackPayload(String payloadJson) {
        return readPayload(payloadJson, FamilyApprovalPayload.class, "协议族定义回滚审批载荷解析失败");
    }

    public static DecryptProfileApprovalPayload readDecryptProfilePublishPayload(String payloadJson) {
        return readPayload(payloadJson, DecryptProfileApprovalPayload.class, "协议解密档案发布审批载荷解析失败");
    }

    public static DecryptProfileApprovalPayload readDecryptProfileRollbackPayload(String payloadJson) {
        return readPayload(payloadJson, DecryptProfileApprovalPayload.class, "协议解密档案回滚审批载荷解析失败");
    }

    public static String writeFamilyExecutionPayload(FamilyApprovalPayload payload,
                                                     Long approvalOrderId,
                                                     Integer publishedVersionNo,
                                                     String lifecycleStatus) {
        return writePayload(copyFamilyExecution(payload, approvalOrderId, publishedVersionNo, lifecycleStatus));
    }

    public static String writeDecryptProfileExecutionPayload(DecryptProfileApprovalPayload payload,
                                                             Long approvalOrderId,
                                                             Integer publishedVersionNo,
                                                             String lifecycleStatus) {
        return writePayload(copyDecryptExecution(payload, approvalOrderId, publishedVersionNo, lifecycleStatus));
    }

    public static String writeFamilySnapshotJson(ProtocolFamilyDefinitionRecord record) {
        if (record == null || record.getId() == null) {
            throw new BizException("协议族定义不存在");
        }
        return writePayload(new FamilyApprovalPayload(
                record.getId(),
                normalizeText(record.getFamilyCode()),
                normalizeText(record.getProtocolCode()),
                normalizeText(record.getDecryptProfileCode()),
                record.getVersionNo(),
                null,
                null
        ));
    }

    public static String writeDecryptProfileSnapshotJson(ProtocolDecryptProfileRecord record) {
        if (record == null || record.getId() == null) {
            throw new BizException("协议解密档案不存在");
        }
        return writePayload(new DecryptProfileApprovalPayload(
                record.getId(),
                normalizeText(record.getProfileCode()),
                normalizeText(record.getAlgorithm()),
                normalizeText(record.getMerchantSource()),
                normalizeText(record.getMerchantKey()),
                record.getVersionNo(),
                null,
                null
        ));
    }

    private static FamilyApprovalPayload buildFamilyPayload(ProtocolFamilyDefinitionRecord record, String submitReason) {
        if (record == null || record.getId() == null) {
            throw new BizException("协议族定义不存在");
        }
        return new FamilyApprovalPayload(
                record.getId(),
                normalizeText(record.getFamilyCode()),
                normalizeText(record.getProtocolCode()),
                normalizeText(record.getDecryptProfileCode()),
                record.getVersionNo(),
                normalizeText(submitReason),
                null
        );
    }

    private static DecryptProfileApprovalPayload buildDecryptPayload(ProtocolDecryptProfileRecord record, String submitReason) {
        if (record == null || record.getId() == null) {
            throw new BizException("协议解密档案不存在");
        }
        return new DecryptProfileApprovalPayload(
                record.getId(),
                normalizeText(record.getProfileCode()),
                normalizeText(record.getAlgorithm()),
                normalizeText(record.getMerchantSource()),
                normalizeText(record.getMerchantKey()),
                record.getVersionNo(),
                normalizeText(submitReason),
                null
        );
    }

    private static FamilyApprovalPayload copyFamilyExecution(FamilyApprovalPayload payload,
                                                             Long approvalOrderId,
                                                             Integer publishedVersionNo,
                                                             String lifecycleStatus) {
        FamilyApprovalPayload normalized = requireFamilyPayload(payload);
        return new FamilyApprovalPayload(
                normalized.familyId(),
                normalized.familyCode(),
                normalized.protocolCode(),
                normalized.decryptProfileCode(),
                normalized.expectedVersionNo(),
                normalized.submitReason(),
                new ApprovalExecution(approvalOrderId, publishedVersionNo, normalizeText(lifecycleStatus))
        );
    }

    private static DecryptProfileApprovalPayload copyDecryptExecution(DecryptProfileApprovalPayload payload,
                                                                      Long approvalOrderId,
                                                                      Integer publishedVersionNo,
                                                                      String lifecycleStatus) {
        DecryptProfileApprovalPayload normalized = requireDecryptPayload(payload);
        return new DecryptProfileApprovalPayload(
                normalized.profileId(),
                normalized.profileCode(),
                normalized.algorithm(),
                normalized.merchantSource(),
                normalized.merchantKey(),
                normalized.expectedVersionNo(),
                normalized.submitReason(),
                new ApprovalExecution(approvalOrderId, publishedVersionNo, normalizeText(lifecycleStatus))
        );
    }

    private static FamilyApprovalPayload requireFamilyPayload(FamilyApprovalPayload payload) {
        if (payload == null || payload.familyId() == null) {
            throw new BizException("协议族定义审批载荷缺少 familyId");
        }
        return payload;
    }

    private static DecryptProfileApprovalPayload requireDecryptPayload(DecryptProfileApprovalPayload payload) {
        if (payload == null || payload.profileId() == null) {
            throw new BizException("协议解密档案审批载荷缺少 profileId");
        }
        return payload;
    }

    private static <T> T readPayload(String payloadJson, Class<T> type, String errorMessage) {
        try {
            return OBJECT_MAPPER.readValue(payloadJson, type);
        } catch (Exception ex) {
            throw new BizException(errorMessage);
        }
    }

    private static String writePayload(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BizException("协议治理审批载荷序列化失败");
        }
    }

    private static String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record FamilyApprovalPayload(Long familyId,
                                        String familyCode,
                                        String protocolCode,
                                        String decryptProfileCode,
                                        Integer expectedVersionNo,
                                        String submitReason,
                                        ApprovalExecution execution) {
    }

    public record DecryptProfileApprovalPayload(Long profileId,
                                                String profileCode,
                                                String algorithm,
                                                String merchantSource,
                                                String merchantKey,
                                                Integer expectedVersionNo,
                                                String submitReason,
                                                ApprovalExecution execution) {
    }

    public record ApprovalExecution(Long approvalOrderId,
                                    Integer publishedVersionNo,
                                    String lifecycleStatus) {
    }
}
