package com.ghlzm.iot.framework.protocol.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.mybatis.PageQueryUtils;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptProfileUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceReplayDTO;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileSnapshot;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileSnapshotMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.service.ProtocolSecurityGovernanceService;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptPreviewVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptProfileVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolFamilyDefinitionVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolGovernanceReplayVO;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProtocolSecurityGovernanceServiceImpl implements ProtocolSecurityGovernanceService {

    private static final String DEFAULT_PROTOCOL_CODE = "mqtt-json";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String HIT_SOURCE_FAMILY_DRAFT = "FAMILY_DRAFT";
    private static final String HIT_SOURCE_APP_ID_DRAFT = "APP_ID_DRAFT";
    private static final String HIT_SOURCE_MISS = "MISS";

    private final ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    private final ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    private final ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper;
    private final ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper;

    public ProtocolSecurityGovernanceServiceImpl(ProtocolFamilyDefinitionRecordMapper familyRecordMapper,
                                                 ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper,
                                                 ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper,
                                                 ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper) {
        this.familyRecordMapper = familyRecordMapper;
        this.decryptProfileRecordMapper = decryptProfileRecordMapper;
        this.familySnapshotMapper = familySnapshotMapper;
        this.decryptProfileSnapshotMapper = decryptProfileSnapshotMapper;
    }

    @Override
    public PageResult<ProtocolFamilyDefinitionVO> pageFamilies(String keyword, String status, Long pageNum, Long pageSize) {
        Page<ProtocolFamilyDefinitionRecord> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<ProtocolFamilyDefinitionRecord> result = familyRecordMapper.selectPage(page,
                new LambdaQueryWrapper<ProtocolFamilyDefinitionRecord>()
                        .eq(ProtocolFamilyDefinitionRecord::getDeleted, 0)
                        .eq(StringUtils.hasText(status), ProtocolFamilyDefinitionRecord::getStatus, normalizeUpper(status))
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(ProtocolFamilyDefinitionRecord::getFamilyCode, normalizeLower(keyword))
                                .or()
                                .like(ProtocolFamilyDefinitionRecord::getDisplayName, normalizeText(keyword))
                                .or()
                                .like(ProtocolFamilyDefinitionRecord::getProtocolCode, normalizeLower(keyword)))
                        .orderByDesc(ProtocolFamilyDefinitionRecord::getUpdateTime)
                        .orderByDesc(ProtocolFamilyDefinitionRecord::getId));
        Map<Long, ProtocolFamilyDefinitionSnapshot> snapshots = loadLatestFamilySnapshots(result.getRecords());
        List<ProtocolFamilyDefinitionVO> records = result.getRecords().stream()
                .map(record -> toFamilyVO(record, snapshots.get(record.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public PageResult<ProtocolDecryptProfileVO> pageDecryptProfiles(String keyword, String status, Long pageNum, Long pageSize) {
        Page<ProtocolDecryptProfileRecord> page = PageQueryUtils.buildPage(pageNum, pageSize);
        Page<ProtocolDecryptProfileRecord> result = decryptProfileRecordMapper.selectPage(page,
                new LambdaQueryWrapper<ProtocolDecryptProfileRecord>()
                        .eq(ProtocolDecryptProfileRecord::getDeleted, 0)
                        .eq(StringUtils.hasText(status), ProtocolDecryptProfileRecord::getStatus, normalizeUpper(status))
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(ProtocolDecryptProfileRecord::getProfileCode, normalizeLower(keyword))
                                .or()
                                .like(ProtocolDecryptProfileRecord::getAlgorithm, normalizeUpper(keyword))
                                .or()
                                .like(ProtocolDecryptProfileRecord::getMerchantSource, normalizeUpper(keyword))
                                .or()
                                .like(ProtocolDecryptProfileRecord::getMerchantKey, normalizeText(keyword)))
                        .orderByDesc(ProtocolDecryptProfileRecord::getUpdateTime)
                        .orderByDesc(ProtocolDecryptProfileRecord::getId));
        Map<Long, ProtocolDecryptProfileSnapshot> snapshots = loadLatestProfileSnapshots(result.getRecords());
        List<ProtocolDecryptProfileVO> records = result.getRecords().stream()
                .map(record -> toProfileVO(record, snapshots.get(record.getId())))
                .toList();
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProtocolFamilyDefinitionVO saveFamily(ProtocolFamilyDefinitionUpsertDTO dto, Long operatorUserId) {
        String familyCode = normalizeLower(requireText(dto == null ? null : dto.getFamilyCode(), "familyCode 不能为空"));
        String protocolCode = normalizeLower(requireText(dto == null ? null : dto.getProtocolCode(), "protocolCode 不能为空"));
        String displayName = requireText(dto == null ? null : dto.getDisplayName(), "displayName 不能为空");
        String decryptProfileCode = normalizeLower(dto == null ? null : dto.getDecryptProfileCode());
        if (StringUtils.hasText(decryptProfileCode)
                && decryptProfileRecordMapper.selectLatestByProfileCode(decryptProfileCode) == null) {
            throw new BizException("decryptProfileCode 对应的协议解密档案不存在: " + decryptProfileCode);
        }

        ProtocolFamilyDefinitionRecord existing = familyRecordMapper.selectLatestByFamilyCode(familyCode);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            ProtocolFamilyDefinitionRecord created = new ProtocolFamilyDefinitionRecord();
            created.setFamilyCode(familyCode);
            created.setProtocolCode(protocolCode);
            created.setDisplayName(displayName);
            created.setDecryptProfileCode(decryptProfileCode);
            created.setSignAlgorithm(normalizeUpper(dto == null ? null : dto.getSignAlgorithm()));
            created.setNormalizationStrategy(normalizeUpper(dto == null ? null : dto.getNormalizationStrategy()));
            created.setStatus(STATUS_DRAFT);
            created.setVersionNo(1);
            created.setCreateBy(normalizePositiveLong(operatorUserId));
            created.setUpdateBy(normalizePositiveLong(operatorUserId));
            if (familyRecordMapper.insert(created) <= 0) {
                throw new BizException("协议族定义保存失败");
            }
            return toFamilyVO(created, null);
        }

        existing.setProtocolCode(protocolCode);
        existing.setDisplayName(displayName);
        existing.setDecryptProfileCode(decryptProfileCode);
        existing.setSignAlgorithm(normalizeUpper(dto == null ? null : dto.getSignAlgorithm()));
        existing.setNormalizationStrategy(normalizeUpper(dto == null ? null : dto.getNormalizationStrategy()));
        existing.setStatus(STATUS_DRAFT);
        existing.setVersionNo(nextVersion(existing.getVersionNo()));
        existing.setApprovalOrderId(null);
        existing.setUpdateBy(normalizePositiveLong(operatorUserId));
        if (familyRecordMapper.updateById(existing) <= 0) {
            throw new BizException("协议族定义更新失败");
        }
        return toFamilyVO(existing, familySnapshotMapper.selectLatestPublishedByFamilyId(existing.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProtocolDecryptProfileVO saveDecryptProfile(ProtocolDecryptProfileUpsertDTO dto, Long operatorUserId) {
        String profileCode = normalizeLower(requireText(dto == null ? null : dto.getProfileCode(), "profileCode 不能为空"));
        String algorithm = normalizeUpper(requireText(dto == null ? null : dto.getAlgorithm(), "algorithm 不能为空"));
        String merchantSource = normalizeUpper(requireText(dto == null ? null : dto.getMerchantSource(), "merchantSource 不能为空"));
        String merchantKey = requireText(dto == null ? null : dto.getMerchantKey(), "merchantKey 不能为空");

        ProtocolDecryptProfileRecord existing = decryptProfileRecordMapper.selectLatestByProfileCode(profileCode);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            ProtocolDecryptProfileRecord created = new ProtocolDecryptProfileRecord();
            created.setProfileCode(profileCode);
            created.setAlgorithm(algorithm);
            created.setMerchantSource(merchantSource);
            created.setMerchantKey(merchantKey);
            created.setTransformation(normalizeText(dto == null ? null : dto.getTransformation()));
            created.setSignatureSecret(normalizeText(dto == null ? null : dto.getSignatureSecret()));
            created.setStatus(STATUS_DRAFT);
            created.setVersionNo(1);
            created.setCreateBy(normalizePositiveLong(operatorUserId));
            created.setUpdateBy(normalizePositiveLong(operatorUserId));
            if (decryptProfileRecordMapper.insert(created) <= 0) {
                throw new BizException("协议解密档案保存失败");
            }
            return toProfileVO(created, null);
        }

        existing.setAlgorithm(algorithm);
        existing.setMerchantSource(merchantSource);
        existing.setMerchantKey(merchantKey);
        existing.setTransformation(normalizeText(dto == null ? null : dto.getTransformation()));
        existing.setSignatureSecret(normalizeText(dto == null ? null : dto.getSignatureSecret()));
        existing.setStatus(STATUS_DRAFT);
        existing.setVersionNo(nextVersion(existing.getVersionNo()));
        existing.setApprovalOrderId(null);
        existing.setUpdateBy(normalizePositiveLong(operatorUserId));
        if (decryptProfileRecordMapper.updateById(existing) <= 0) {
            throw new BizException("协议解密档案更新失败");
        }
        return toProfileVO(existing, decryptProfileSnapshotMapper.selectLatestPublishedByProfileId(existing.getId()));
    }

    @Override
    public ProtocolDecryptPreviewVO previewDecrypt(ProtocolDecryptPreviewDTO dto) {
        String familyCode = normalizeLower(dto == null ? null : dto.getFamilyCode());
        String protocolCode = normalizeLower(dto == null ? null : dto.getProtocolCode());
        String appId = normalizeText(dto == null ? null : dto.getAppId());

        if (StringUtils.hasText(familyCode)) {
            ProtocolFamilyDefinitionRecord family = familyRecordMapper.selectLatestEnabledByFamilyCode(familyCode);
            if (family != null && matchesProtocol(family.getProtocolCode(), protocolCode)) {
                ProtocolDecryptProfileRecord profile =
                        decryptProfileRecordMapper.selectLatestEnabledByProfileCode(normalizeLower(family.getDecryptProfileCode()));
                if (profile != null) {
                    return toPreviewVO(profile, familyCode, HIT_SOURCE_FAMILY_DRAFT);
                }
            }
        }

        if (StringUtils.hasText(appId)) {
            ProtocolDecryptProfileRecord profile = decryptProfileRecordMapper.selectLatestEnabledByMerchantKey(appId);
            if (profile != null) {
                return toPreviewVO(profile, familyCode, HIT_SOURCE_APP_ID_DRAFT);
            }
        }

        ProtocolDecryptPreviewVO miss = new ProtocolDecryptPreviewVO();
        miss.setMatched(Boolean.FALSE);
        miss.setHitSource(HIT_SOURCE_MISS);
        miss.setFamilyCode(familyCode);
        return miss;
    }

    @Override
    public ProtocolFamilyDefinitionVO getFamilyDetail(Long familyId) {
        ProtocolFamilyDefinitionRecord record = requireFamilyById(familyId);
        return toFamilyVO(record, familySnapshotMapper.selectLatestPublishedByFamilyId(record.getId()));
    }

    @Override
    public ProtocolDecryptProfileVO getDecryptProfileDetail(Long profileId) {
        ProtocolDecryptProfileRecord record = requireProfileById(profileId);
        return toProfileVO(record, decryptProfileSnapshotMapper.selectLatestPublishedByProfileId(record.getId()));
    }

    @Override
    public ProtocolGovernanceReplayVO replayDecrypt(ProtocolGovernanceReplayDTO dto) {
        String familyCode = normalizeLower(dto == null ? null : dto.getFamilyCode());
        String protocolCode = normalizeLower(dto == null ? null : dto.getProtocolCode());
        String appId = normalizeText(dto == null ? null : dto.getAppId());

        ProtocolDecryptPreviewVO preview = previewDecrypt(new ProtocolDecryptPreviewDTO(appId, protocolCode, familyCode));
        ProtocolGovernanceReplayVO replay = new ProtocolGovernanceReplayVO();
        replay.setMatched(preview.getMatched());
        replay.setHitSource(preview.getHitSource());
        replay.setFamilyCode(preview.getFamilyCode() == null ? familyCode : preview.getFamilyCode());
        replay.setProtocolCode(protocolCode);
        replay.setAppId(appId);
        replay.setResolvedProfileCode(preview.getResolvedProfileCode());
        replay.setAlgorithm(preview.getAlgorithm());
        replay.setMerchantSource(preview.getMerchantSource());
        replay.setMerchantKey(preview.getMerchantKey());
        replay.setTransformation(preview.getTransformation());
        return replay;
    }

    private Map<Long, ProtocolFamilyDefinitionSnapshot> loadLatestFamilySnapshots(List<ProtocolFamilyDefinitionRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> familyIds = records.stream()
                .map(ProtocolFamilyDefinitionRecord::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (familyIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProtocolFamilyDefinitionSnapshot> result = new HashMap<>();
        for (ProtocolFamilyDefinitionSnapshot snapshot : familySnapshotMapper.selectPublishedByFamilyIds(familyIds)) {
            if (snapshot == null || snapshot.getFamilyId() == null) {
                continue;
            }
            result.merge(snapshot.getFamilyId(), snapshot, this::preferHigherFamilySnapshot);
        }
        return result;
    }

    private Map<Long, ProtocolDecryptProfileSnapshot> loadLatestProfileSnapshots(List<ProtocolDecryptProfileRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> profileIds = records.stream()
                .map(ProtocolDecryptProfileRecord::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (profileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProtocolDecryptProfileSnapshot> result = new HashMap<>();
        for (ProtocolDecryptProfileSnapshot snapshot : decryptProfileSnapshotMapper.selectPublishedByProfileIds(profileIds)) {
            if (snapshot == null || snapshot.getProfileId() == null) {
                continue;
            }
            result.merge(snapshot.getProfileId(), snapshot, this::preferHigherProfileSnapshot);
        }
        return result;
    }

    private ProtocolFamilyDefinitionSnapshot preferHigherFamilySnapshot(ProtocolFamilyDefinitionSnapshot left,
                                                                        ProtocolFamilyDefinitionSnapshot right) {
        return compareSnapshot(left == null ? null : left.getPublishedVersionNo(),
                right == null ? null : right.getPublishedVersionNo(),
                left == null ? null : left.getId(),
                right == null ? null : right.getId()) >= 0 ? left : right;
    }

    private ProtocolDecryptProfileSnapshot preferHigherProfileSnapshot(ProtocolDecryptProfileSnapshot left,
                                                                      ProtocolDecryptProfileSnapshot right) {
        return compareSnapshot(left == null ? null : left.getPublishedVersionNo(),
                right == null ? null : right.getPublishedVersionNo(),
                left == null ? null : left.getId(),
                right == null ? null : right.getId()) >= 0 ? left : right;
    }

    private int compareSnapshot(Integer leftVersion, Integer rightVersion, Long leftId, Long rightId) {
        if (leftVersion == null && rightVersion != null) {
            return -1;
        }
        if (leftVersion != null && rightVersion == null) {
            return 1;
        }
        if (leftVersion != null && rightVersion != null && !leftVersion.equals(rightVersion)) {
            return Integer.compare(leftVersion, rightVersion);
        }
        if (leftId == null && rightId != null) {
            return -1;
        }
        if (leftId != null && rightId == null) {
            return 1;
        }
        if (leftId == null) {
            return 0;
        }
        return Long.compare(leftId, rightId);
    }

    private ProtocolFamilyDefinitionVO toFamilyVO(ProtocolFamilyDefinitionRecord record,
                                                  ProtocolFamilyDefinitionSnapshot snapshot) {
        ProtocolFamilyDefinitionVO vo = new ProtocolFamilyDefinitionVO();
        vo.setId(record.getId());
        vo.setFamilyCode(record.getFamilyCode());
        vo.setProtocolCode(record.getProtocolCode());
        vo.setDisplayName(record.getDisplayName());
        vo.setDecryptProfileCode(record.getDecryptProfileCode());
        vo.setSignAlgorithm(record.getSignAlgorithm());
        vo.setNormalizationStrategy(record.getNormalizationStrategy());
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

    private ProtocolDecryptProfileVO toProfileVO(ProtocolDecryptProfileRecord record,
                                                 ProtocolDecryptProfileSnapshot snapshot) {
        ProtocolDecryptProfileVO vo = new ProtocolDecryptProfileVO();
        vo.setId(record.getId());
        vo.setProfileCode(record.getProfileCode());
        vo.setAlgorithm(record.getAlgorithm());
        vo.setMerchantSource(record.getMerchantSource());
        vo.setMerchantKey(record.getMerchantKey());
        vo.setTransformation(record.getTransformation());
        vo.setSignatureSecret(record.getSignatureSecret());
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

    private ProtocolDecryptPreviewVO toPreviewVO(ProtocolDecryptProfileRecord record,
                                                 String familyCode,
                                                 String hitSource) {
        ProtocolDecryptPreviewVO vo = new ProtocolDecryptPreviewVO();
        vo.setMatched(Boolean.TRUE);
        vo.setHitSource(hitSource);
        vo.setFamilyCode(familyCode);
        vo.setResolvedProfileCode(record.getProfileCode());
        vo.setAlgorithm(record.getAlgorithm());
        vo.setMerchantSource(record.getMerchantSource());
        vo.setMerchantKey(record.getMerchantKey());
        vo.setTransformation(record.getTransformation());
        return vo;
    }

    private boolean matchesProtocol(String familyProtocolCode, String requestedProtocolCode) {
        String normalizedRequested = normalizeLower(requestedProtocolCode);
        if (!StringUtils.hasText(normalizedRequested)) {
            normalizedRequested = DEFAULT_PROTOCOL_CODE;
        }
        String normalizedFamily = normalizeLower(familyProtocolCode);
        return !StringUtils.hasText(normalizedFamily) || normalizedRequested.equals(normalizedFamily);
    }

    private Integer nextVersion(Integer versionNo) {
        return versionNo == null || versionNo <= 0 ? 1 : versionNo + 1;
    }

    private ProtocolFamilyDefinitionRecord requireFamilyById(Long familyId) {
        if (familyId == null || familyId <= 0) {
            throw new BizException("协议族定义不存在: " + familyId);
        }
        ProtocolFamilyDefinitionRecord record = familyRecordMapper.selectById(familyId);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            throw new BizException("协议族定义不存在: " + familyId);
        }
        return record;
    }

    private ProtocolDecryptProfileRecord requireProfileById(Long profileId) {
        if (profileId == null || profileId <= 0) {
            throw new BizException("协议解密档案不存在: " + profileId);
        }
        ProtocolDecryptProfileRecord record = decryptProfileRecordMapper.selectById(profileId);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            throw new BizException("协议解密档案不存在: " + profileId);
        }
        return record;
    }

    private Long normalizePositiveLong(Long value) {
        return value != null && value > 0 ? value : null;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeLower(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
