package com.ghlzm.iot.framework.protocol.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.protocol.dto.ProtocolDecryptPreviewDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolGovernanceReplayDTO;
import com.ghlzm.iot.framework.protocol.dto.ProtocolFamilyDefinitionUpsertDTO;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolDecryptProfileSnapshotMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.mapper.ProtocolFamilyDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.service.ProtocolSecurityGovernanceService;
import com.ghlzm.iot.framework.protocol.vo.ProtocolDecryptPreviewVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolFamilyDefinitionVO;
import com.ghlzm.iot.framework.protocol.vo.ProtocolGovernanceReplayVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocolSecurityGovernanceServiceImplTest {

    @Mock
    private ProtocolFamilyDefinitionRecordMapper familyRecordMapper;
    @Mock
    private ProtocolDecryptProfileRecordMapper decryptProfileRecordMapper;
    @Mock
    private ProtocolFamilyDefinitionSnapshotMapper familySnapshotMapper;
    @Mock
    private ProtocolDecryptProfileSnapshotMapper decryptProfileSnapshotMapper;

    private ProtocolSecurityGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new ProtocolSecurityGovernanceServiceImpl(
                familyRecordMapper,
                decryptProfileRecordMapper,
                familySnapshotMapper,
                decryptProfileSnapshotMapper
        );
    }

    @Test
    void saveFamilyShouldCreateDraftRecordAndValidateReferencedDecryptProfile() {
        when(familyRecordMapper.selectLatestByFamilyCode("legacy-dp-crack")).thenReturn(null);
        when(decryptProfileRecordMapper.selectLatestByProfileCode("des-62000001"))
                .thenReturn(profileRecord(9201L, "des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001", "DRAFT", 2));
        when(familyRecordMapper.insert(any(ProtocolFamilyDefinitionRecord.class))).thenReturn(1);

        ProtocolFamilyDefinitionVO result = service.saveFamily(
                new ProtocolFamilyDefinitionUpsertDTO(
                        "legacy-dp-crack",
                        "mqtt-json",
                        "裂缝旧 $dp 协议族",
                        "des-62000001",
                        "AES",
                        "LEGACY_DP"
                ),
                10001L
        );

        assertEquals("legacy-dp-crack", result.getFamilyCode());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(1, result.getVersionNo());
        verify(familyRecordMapper).insert(org.mockito.ArgumentMatchers.<ProtocolFamilyDefinitionRecord>argThat(record ->
                "legacy-dp-crack".equals(record.getFamilyCode())
                        && "mqtt-json".equals(record.getProtocolCode())
                        && "des-62000001".equals(record.getDecryptProfileCode())
                        && "DRAFT".equals(record.getStatus())
                        && Integer.valueOf(1).equals(record.getVersionNo())
                        && Long.valueOf(10001L).equals(record.getCreateBy())
                        && Long.valueOf(10001L).equals(record.getUpdateBy())
        ));
    }

    @Test
    void pageFamiliesShouldExposePublishedSnapshotState() {
        ProtocolFamilyDefinitionRecord record =
                familyRecord(9101L, "legacy-dp-crack", "mqtt-json", "des-62000001", "DRAFT", 4);
        Page<ProtocolFamilyDefinitionRecord> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(record));
        when(familyRecordMapper.selectPage(any(), any())).thenReturn(page);
        when(familySnapshotMapper.selectPublishedByFamilyIds(List.of(9101L)))
                .thenReturn(List.of(familySnapshot(9301L, 9101L, 3, 99101L)));

        PageResult<ProtocolFamilyDefinitionVO> result = service.pageFamilies(null, null, 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals("DRAFT", result.getRecords().get(0).getStatus());
        assertEquals("PUBLISHED", result.getRecords().get(0).getPublishedStatus());
        assertEquals(3, result.getRecords().get(0).getPublishedVersionNo());
    }

    @Test
    void previewDecryptShouldResolveFamilyBoundProfileBeforeAppIdFallback() {
        when(familyRecordMapper.selectLatestEnabledByFamilyCode("legacy-dp-crack"))
                .thenReturn(familyRecord(9101L, "legacy-dp-crack", "mqtt-json", "des-62000001", "ACTIVE", 3));
        when(decryptProfileRecordMapper.selectLatestEnabledByProfileCode("des-62000001"))
                .thenReturn(profileRecord(9201L, "des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001", "ACTIVE", 2));

        ProtocolDecryptPreviewVO result = service.previewDecrypt(
                new ProtocolDecryptPreviewDTO("62000000", "mqtt-json", "legacy-dp-crack")
        );

        assertEquals("des-62000001", result.getResolvedProfileCode());
        assertEquals("DES", result.getAlgorithm());
        assertEquals("IOT_PROTOCOL_CRYPTO", result.getMerchantSource());
        assertEquals("FAMILY_DRAFT", result.getHitSource());
        assertTrue(Boolean.TRUE.equals(result.getMatched()));
    }

    @Test
    void getFamilyDetailShouldExposeLatestPublishedSnapshotState() {
        when(familyRecordMapper.selectById(9101L))
                .thenReturn(familyRecord(9101L, "legacy-dp-crack", "mqtt-json", "des-62000001", "DRAFT", 4));
        when(familySnapshotMapper.selectLatestPublishedByFamilyId(9101L))
                .thenReturn(familySnapshot(9301L, 9101L, 3, 99101L));

        ProtocolFamilyDefinitionVO detail = service.getFamilyDetail(9101L);

        assertEquals(9101L, detail.getId());
        assertEquals("PUBLISHED", detail.getPublishedStatus());
        assertEquals(3, detail.getPublishedVersionNo());
        assertEquals(99101L, detail.getApprovalOrderId());
    }

    @Test
    void getDecryptProfileDetailShouldThrowWhenProfileNotFound() {
        when(decryptProfileRecordMapper.selectById(9201L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.getDecryptProfileDetail(9201L));
    }

    @Test
    void replayDecryptShouldEchoNormalizedRequestAndResolvedProfile() {
        when(familyRecordMapper.selectLatestEnabledByFamilyCode("legacy-dp-crack"))
                .thenReturn(familyRecord(9101L, "legacy-dp-crack", "mqtt-json", "des-62000001", "ACTIVE", 3));
        when(decryptProfileRecordMapper.selectLatestEnabledByProfileCode("des-62000001"))
                .thenReturn(profileRecord(9201L, "des-62000001", "DES", "IOT_PROTOCOL_CRYPTO", "62000001", "ACTIVE", 2));

        ProtocolGovernanceReplayVO replay = service.replayDecrypt(
                new ProtocolGovernanceReplayDTO(" legacy-dp-crack ", " mqtt-json ", " 62000001 ")
        );

        assertEquals("legacy-dp-crack", replay.getFamilyCode());
        assertEquals("mqtt-json", replay.getProtocolCode());
        assertEquals("62000001", replay.getAppId());
        assertEquals("des-62000001", replay.getResolvedProfileCode());
        assertEquals("FAMILY_DRAFT", replay.getHitSource());
        assertTrue(Boolean.TRUE.equals(replay.getMatched()));
    }

    private ProtocolFamilyDefinitionRecord familyRecord(Long id,
                                                        String familyCode,
                                                        String protocolCode,
                                                        String decryptProfileCode,
                                                        String status,
                                                        Integer versionNo) {
        ProtocolFamilyDefinitionRecord record = new ProtocolFamilyDefinitionRecord();
        record.setId(id);
        record.setFamilyCode(familyCode);
        record.setProtocolCode(protocolCode);
        record.setDisplayName("demo");
        record.setDecryptProfileCode(decryptProfileCode);
        record.setSignAlgorithm("AES");
        record.setNormalizationStrategy("LEGACY_DP");
        record.setStatus(status);
        record.setVersionNo(versionNo);
        record.setDeleted(0);
        return record;
    }

    private ProtocolDecryptProfileRecord profileRecord(Long id,
                                                       String profileCode,
                                                       String algorithm,
                                                       String merchantSource,
                                                       String merchantKey,
                                                       String status,
                                                       Integer versionNo) {
        ProtocolDecryptProfileRecord record = new ProtocolDecryptProfileRecord();
        record.setId(id);
        record.setProfileCode(profileCode);
        record.setAlgorithm(algorithm);
        record.setMerchantSource(merchantSource);
        record.setMerchantKey(merchantKey);
        record.setStatus(status);
        record.setVersionNo(versionNo);
        record.setDeleted(0);
        return record;
    }

    private ProtocolFamilyDefinitionSnapshot familySnapshot(Long id,
                                                            Long familyId,
                                                            Integer publishedVersionNo,
                                                            Long approvalOrderId) {
        ProtocolFamilyDefinitionSnapshot snapshot = new ProtocolFamilyDefinitionSnapshot();
        snapshot.setId(id);
        snapshot.setFamilyId(familyId);
        snapshot.setPublishedVersionNo(publishedVersionNo);
        snapshot.setApprovalOrderId(approvalOrderId);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("{\"familyCode\":\"legacy-dp-crack\",\"decryptProfileCode\":\"des-62000001\"}");
        return snapshot;
    }
}
