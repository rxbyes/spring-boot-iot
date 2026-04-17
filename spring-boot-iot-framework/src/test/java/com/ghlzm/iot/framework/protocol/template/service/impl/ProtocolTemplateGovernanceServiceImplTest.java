package com.ghlzm.iot.framework.protocol.template.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateSubmitDTO;
import com.ghlzm.iot.framework.protocol.template.dto.ProtocolTemplateUpsertDTO;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionRecord;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionRecordMapper;
import com.ghlzm.iot.framework.protocol.template.mapper.ProtocolTemplateDefinitionSnapshotMapper;
import com.ghlzm.iot.framework.protocol.template.service.ProtocolTemplateGovernanceService;
import com.ghlzm.iot.framework.protocol.template.vo.ProtocolTemplateDefinitionVO;
import java.util.List;
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
class ProtocolTemplateGovernanceServiceImplTest {

    @Mock
    private ProtocolTemplateDefinitionRecordMapper recordMapper;
    @Mock
    private ProtocolTemplateDefinitionSnapshotMapper snapshotMapper;

    private ProtocolTemplateGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new ProtocolTemplateGovernanceServiceImpl(recordMapper, snapshotMapper);
    }

    @Test
    void saveTemplateShouldCreateDraftRecordAndNormalizeFields() {
        when(recordMapper.selectLatestByTemplateCode("legacy-dp-crack-v1")).thenReturn(null);
        when(recordMapper.insert(any(ProtocolTemplateDefinitionRecord.class))).thenReturn(1);

        ProtocolTemplateUpsertDTO dto = new ProtocolTemplateUpsertDTO();
        dto.setTemplateCode(" legacy-dp-crack-v1 ");
        dto.setFamilyCode(" legacy-dp ");
        dto.setProtocolCode(" mqtt-json ");
        dto.setDisplayName("裂缝 legacy 子模板");
        dto.setExpressionJson("{\"logicalPattern\":\"^L1_LF_\\\\d+$\"}");
        dto.setOutputMappingJson("{\"value\":\"$.value\"}");

        ProtocolTemplateDefinitionVO result = service.saveTemplate(dto, 10001L);

        assertEquals("legacy-dp-crack-v1", result.getTemplateCode());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(1, result.getVersionNo());
        verify(recordMapper).insert(argThat((ProtocolTemplateDefinitionRecord record) ->
                "legacy-dp-crack-v1".equals(record.getTemplateCode())
                        && "legacy-dp".equals(record.getFamilyCode())
                        && "mqtt-json".equals(record.getProtocolCode())
                        && "裂缝 legacy 子模板".equals(record.getDisplayName())
                        && "{\"logicalPattern\":\"^L1_LF_\\\\d+$\"}".equals(record.getExpressionJson())
                        && "{\"value\":\"$.value\"}".equals(record.getOutputMappingJson())
                        && "DRAFT".equals(record.getStatus())
                        && Integer.valueOf(1).equals(record.getVersionNo())
                        && Long.valueOf(10001L).equals(record.getCreateBy())
                        && Long.valueOf(10001L).equals(record.getUpdateBy())
        ));
    }

    @Test
    void pageTemplatesShouldExposeLatestPublishedSnapshotState() {
        ProtocolTemplateDefinitionRecord record = templateRecord(
                9401L,
                "legacy-dp-crack-v1",
                "legacy-dp",
                "mqtt-json",
                "裂缝 legacy 子模板",
                "DRAFT",
                3
        );
        Page<ProtocolTemplateDefinitionRecord> page = new Page<>(1, 10);
        page.setTotal(1);
        page.setRecords(List.of(record));
        when(recordMapper.selectPage(any(), any())).thenReturn(page);
        when(snapshotMapper.selectPublishedByTemplateIds(List.of(9401L)))
                .thenReturn(List.of(templateSnapshot(9501L, 9401L, 2, 99101L)));

        PageResult<ProtocolTemplateDefinitionVO> result = service.pageTemplates(null, null, 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals("legacy-dp-crack-v1", result.getRecords().get(0).getTemplateCode());
        assertEquals("PUBLISHED", result.getRecords().get(0).getPublishedStatus());
        assertEquals(2, result.getRecords().get(0).getPublishedVersionNo());
        assertEquals(99101L, result.getRecords().get(0).getApprovalOrderId());
    }

    @Test
    void publishTemplateShouldCreatePublishedSnapshotAndMarkDraftActive() {
        ProtocolTemplateDefinitionRecord record = templateRecord(
                9401L,
                "legacy-dp-crack-v1",
                "legacy-dp",
                "mqtt-json",
                "裂缝 legacy 子模板",
                "DRAFT",
                3
        );
        when(recordMapper.selectById(9401L)).thenReturn(record);
        when(recordMapper.updateById(any(ProtocolTemplateDefinitionRecord.class))).thenReturn(1);
        when(snapshotMapper.insert(any(ProtocolTemplateDefinitionSnapshot.class))).thenReturn(1);
        when(snapshotMapper.selectLatestPublishedByTemplateId(9401L))
                .thenReturn(templateSnapshot(9501L, 9401L, 3, 99102L));

        ProtocolTemplateDefinitionVO published = service.publishTemplate(
                9401L,
                10001L,
                new ProtocolTemplateSubmitDTO("首次发布")
        );

        assertEquals("ACTIVE", published.getStatus());
        assertEquals("PUBLISHED", published.getPublishedStatus());
        assertEquals(3, published.getPublishedVersionNo());
        verify(snapshotMapper).insert(argThat((ProtocolTemplateDefinitionSnapshot snapshot) ->
                Long.valueOf(9401L).equals(snapshot.getTemplateId())
                        && "legacy-dp-crack-v1".equals(snapshot.getTemplateCode())
                        && Integer.valueOf(3).equals(snapshot.getPublishedVersionNo())
                        && "PUBLISHED".equals(snapshot.getLifecycleStatus())
        ));
    }

    private ProtocolTemplateDefinitionRecord templateRecord(Long id,
                                                            String templateCode,
                                                            String familyCode,
                                                            String protocolCode,
                                                            String displayName,
                                                            String status,
                                                            Integer versionNo) {
        ProtocolTemplateDefinitionRecord record = new ProtocolTemplateDefinitionRecord();
        record.setId(id);
        record.setTemplateCode(templateCode);
        record.setFamilyCode(familyCode);
        record.setProtocolCode(protocolCode);
        record.setDisplayName(displayName);
        record.setExpressionJson("{\"logicalPattern\":\"^L1_LF_\\\\d+$\"}");
        record.setOutputMappingJson("{\"value\":\"$.value\"}");
        record.setStatus(status);
        record.setVersionNo(versionNo);
        record.setDeleted(0);
        return record;
    }

    private ProtocolTemplateDefinitionSnapshot templateSnapshot(Long id,
                                                                Long templateId,
                                                                Integer publishedVersionNo,
                                                                Long approvalOrderId) {
        ProtocolTemplateDefinitionSnapshot snapshot = new ProtocolTemplateDefinitionSnapshot();
        snapshot.setId(id);
        snapshot.setTemplateId(templateId);
        snapshot.setTemplateCode("legacy-dp-crack-v1");
        snapshot.setPublishedVersionNo(publishedVersionNo);
        snapshot.setApprovalOrderId(approvalOrderId);
        snapshot.setLifecycleStatus("PUBLISHED");
        snapshot.setSnapshotJson("{\"templateCode\":\"legacy-dp-crack-v1\"}");
        return snapshot;
    }
}
