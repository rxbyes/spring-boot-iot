package com.ghlzm.iot.framework.protocol.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.util.JsonPayloadUtils;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateSubmitDTO;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateUpsertDTO;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionRecord;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateGovernanceService;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateDefinitionVO;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ProtocolTemplateGovernanceServiceImpl implements ProtocolTemplateGovernanceService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final ProtocolTemplateDefinitionRecordMapper recordMapper;
    private final ProtocolTemplateDefinitionSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public ProtocolTemplateGovernanceServiceImpl(ProtocolTemplateDefinitionRecordMapper recordMapper,
                                                 ProtocolTemplateDefinitionSnapshotMapper snapshotMapper) {
        this.recordMapper = recordMapper;
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public PageResult<ProtocolTemplateDefinitionVO> pageTemplates(String keyword, String status, Long pageNum, Long pageSize) {
        Page<ProtocolTemplateDefinitionRecord> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<ProtocolTemplateDefinitionRecord> result = recordMapper.selectPage(page,
                new LambdaQueryWrapper<ProtocolTemplateDefinitionRecord>()
                        .eq(ProtocolTemplateDefinitionRecord::getDeleted, 0)
                        .eq(StringUtils.hasText(status), ProtocolTemplateDefinitionRecord::getStatus, normalizeUpper(status))
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(ProtocolTemplateDefinitionRecord::getTemplateCode, normalizeLower(keyword))
                                .or()
                                .like(ProtocolTemplateDefinitionRecord::getFamilyCode, normalizeLower(keyword))
                                .or()
                                .like(ProtocolTemplateDefinitionRecord::getProtocolCode, normalizeLower(keyword))
                                .or()
                                .like(ProtocolTemplateDefinitionRecord::getDisplayName, normalizeText(keyword)))
                        .orderByDesc(ProtocolTemplateDefinitionRecord::getUpdateTime)
                        .orderByDesc(ProtocolTemplateDefinitionRecord::getId));
        Map<Long, ProtocolTemplateDefinitionSnapshot> snapshots = loadLatestPublishedSnapshots(result.getRecords());
        List<ProtocolTemplateDefinitionVO> records = result.getRecords().stream()
                .map(record -> toVO(record, snapshots.get(record.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public ProtocolTemplateDefinitionVO getTemplateDetail(Long templateId) {
        ProtocolTemplateDefinitionRecord record = requireRecord(templateId);
        return toVO(record, snapshotMapper.selectLatestPublishedByTemplateId(record.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProtocolTemplateDefinitionVO saveTemplate(ProtocolTemplateUpsertDTO dto, Long operatorUserId) {
        String templateCode = normalizeLower(requireText(dto == null ? null : dto.getTemplateCode(), "templateCode 不能为空"));
        ProtocolTemplateDefinitionRecord existing = recordMapper.selectLatestByTemplateCode(templateCode);

        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            ProtocolTemplateDefinitionRecord created = new ProtocolTemplateDefinitionRecord();
            applyEditableFields(created, dto);
            created.setTemplateCode(templateCode);
            created.setStatus(STATUS_DRAFT);
            created.setVersionNo(1);
            created.setCreateBy(normalizePositiveLong(operatorUserId));
            created.setUpdateBy(normalizePositiveLong(operatorUserId));
            if (recordMapper.insert(created) <= 0) {
                throw new BizException("协议模板草稿保存失败");
            }
            return toVO(created, null);
        }

        applyEditableFields(existing, dto);
        existing.setTemplateCode(templateCode);
        existing.setStatus(STATUS_DRAFT);
        existing.setVersionNo(nextVersion(existing.getVersionNo()));
        existing.setApprovalOrderId(null);
        existing.setUpdateBy(normalizePositiveLong(operatorUserId));
        if (recordMapper.updateById(existing) <= 0) {
            throw new BizException("协议模板草稿更新失败");
        }
        return toVO(existing, snapshotMapper.selectLatestPublishedByTemplateId(existing.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProtocolTemplateDefinitionVO publishTemplate(Long templateId, Long operatorUserId, ProtocolTemplateSubmitDTO dto) {
        ProtocolTemplateDefinitionRecord record = requireRecord(templateId);
        record.setStatus(STATUS_ACTIVE);
        record.setUpdateBy(normalizePositiveLong(operatorUserId));
        if (recordMapper.updateById(record) <= 0) {
            throw new BizException("协议模板发布失败");
        }

        ProtocolTemplateDefinitionSnapshot snapshot = new ProtocolTemplateDefinitionSnapshot();
        snapshot.setTemplateId(record.getId());
        snapshot.setTemplateCode(record.getTemplateCode());
        snapshot.setFamilyCode(record.getFamilyCode());
        snapshot.setProtocolCode(record.getProtocolCode());
        snapshot.setPublishedVersionNo(record.getVersionNo());
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setApprovalOrderId(null);
        snapshot.setSubmitReason(dto == null ? null : normalizeText(dto.getSubmitReason()));
        snapshot.setSnapshotJson(buildSnapshotJson(record));
        snapshot.setCreateBy(normalizePositiveLong(operatorUserId));
        if (snapshotMapper.insert(snapshot) <= 0) {
            throw new BizException("协议模板发布快照保存失败");
        }

        ProtocolTemplateDefinitionSnapshot latestSnapshot = snapshotMapper.selectLatestPublishedByTemplateId(record.getId());
        return toVO(record, latestSnapshot == null ? snapshot : latestSnapshot);
    }

    private void applyEditableFields(ProtocolTemplateDefinitionRecord record, ProtocolTemplateUpsertDTO dto) {
        record.setFamilyCode(normalizeLower(requireText(dto == null ? null : dto.getFamilyCode(), "familyCode 不能为空")));
        record.setProtocolCode(normalizeLower(requireText(dto == null ? null : dto.getProtocolCode(), "protocolCode 不能为空")));
        record.setDisplayName(normalizeText(requireText(dto == null ? null : dto.getDisplayName(), "displayName 不能为空")));
        record.setExpressionJson(normalizeJson(requireText(dto == null ? null : dto.getExpressionJson(), "expressionJson 不能为空"), "expressionJson"));
        record.setOutputMappingJson(normalizeJson(dto == null ? null : dto.getOutputMappingJson(), "outputMappingJson"));
    }

    private ProtocolTemplateDefinitionRecord requireRecord(Long templateId) {
        if (templateId == null || templateId <= 0) {
            throw new BizException("协议模板不存在: " + templateId);
        }
        ProtocolTemplateDefinitionRecord record = recordMapper.selectById(templateId);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            throw new BizException("协议模板不存在: " + templateId);
        }
        return record;
    }

    private Map<Long, ProtocolTemplateDefinitionSnapshot> loadLatestPublishedSnapshots(List<ProtocolTemplateDefinitionRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> templateIds = records.stream()
                .map(ProtocolTemplateDefinitionRecord::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProtocolTemplateDefinitionSnapshot> result = new HashMap<>();
        for (ProtocolTemplateDefinitionSnapshot snapshot : snapshotMapper.selectPublishedByTemplateIds(templateIds)) {
            if (snapshot == null || snapshot.getTemplateId() == null) {
                continue;
            }
            result.merge(snapshot.getTemplateId(), snapshot, this::preferHigherSnapshot);
        }
        return result;
    }

    private ProtocolTemplateDefinitionSnapshot preferHigherSnapshot(ProtocolTemplateDefinitionSnapshot left,
                                                                    ProtocolTemplateDefinitionSnapshot right) {
        Integer leftVersion = left == null ? null : left.getPublishedVersionNo();
        Integer rightVersion = right == null ? null : right.getPublishedVersionNo();
        if (leftVersion == null && rightVersion != null) {
            return right;
        }
        if (leftVersion != null && rightVersion == null) {
            return left;
        }
        if (leftVersion != null && rightVersion != null && !leftVersion.equals(rightVersion)) {
            return leftVersion > rightVersion ? left : right;
        }
        Long leftId = left == null ? null : left.getId();
        Long rightId = right == null ? null : right.getId();
        if (leftId == null) {
            return right;
        }
        if (rightId == null) {
            return left;
        }
        return leftId >= rightId ? left : right;
    }

    private ProtocolTemplateDefinitionVO toVO(ProtocolTemplateDefinitionRecord record,
                                              ProtocolTemplateDefinitionSnapshot snapshot) {
        ProtocolTemplateDefinitionVO vo = new ProtocolTemplateDefinitionVO();
        vo.setId(record.getId());
        vo.setTemplateCode(record.getTemplateCode());
        vo.setFamilyCode(record.getFamilyCode());
        vo.setProtocolCode(record.getProtocolCode());
        vo.setDisplayName(record.getDisplayName());
        vo.setExpressionJson(record.getExpressionJson());
        vo.setOutputMappingJson(record.getOutputMappingJson());
        vo.setStatus(record.getStatus());
        vo.setVersionNo(record.getVersionNo());
        vo.setPublishedStatus(snapshot == null ? null : snapshot.getLifecycleStatus());
        vo.setPublishedVersionNo(snapshot == null ? null : snapshot.getPublishedVersionNo());
        vo.setApprovalOrderId(snapshot == null ? record.getApprovalOrderId() : snapshot.getApprovalOrderId());
        vo.setCreateBy(record.getCreateBy());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateBy(record.getUpdateBy());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private String buildSnapshotJson(ProtocolTemplateDefinitionRecord record) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("templateCode", record.getTemplateCode());
        snapshot.put("familyCode", record.getFamilyCode());
        snapshot.put("protocolCode", record.getProtocolCode());
        snapshot.put("displayName", record.getDisplayName());
        snapshot.put("expressionJson", record.getExpressionJson());
        snapshot.put("outputMappingJson", record.getOutputMappingJson());
        snapshot.put("status", record.getStatus());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String normalizeJson(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String json = JsonPayloadUtils.normalizeJsonDocument(normalized);
        try {
            objectMapper.readTree(json);
            return json;
        } catch (Exception ex) {
            throw new BizException(fieldName + " 必须是合法 JSON");
        }
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toLowerCase(Locale.ROOT) : null;
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized.toUpperCase(Locale.ROOT) : null;
    }

    private Long normalizePositiveLong(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private Integer nextVersion(Integer versionNo) {
        return versionNo == null || versionNo <= 0 ? 1 : versionNo + 1;
    }
}
