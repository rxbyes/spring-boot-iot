package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackCreateDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackPageQueryDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackUpdateDTO;
import com.ghlzm.iot.device.entity.OnboardingTemplatePack;
import com.ghlzm.iot.device.mapper.OnboardingTemplatePackMapper;
import com.ghlzm.iot.device.vo.OnboardingTemplatePackVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTemplatePackServiceImplTest {

    @Mock
    private OnboardingTemplatePackMapper mapper;

    @Test
    void createPackShouldTrimFieldsAndDefaultStatusAndVersion() {
        when(mapper.insert(any(OnboardingTemplatePack.class))).thenAnswer(invocation -> {
            OnboardingTemplatePack entity = invocation.getArgument(0);
            entity.setId(7201L);
            return 1;
        });

        OnboardingTemplatePackCreateDTO dto = new OnboardingTemplatePackCreateDTO();
        dto.setPackCode(" PACK-CRACK-V1 ");
        dto.setPackName(" 裂缝模板包 ");
        dto.setScenarioCode(" phase1-crack ");
        dto.setDeviceFamily(" crack_sensor ");
        dto.setProtocolFamilyCode(" legacy-dp-crack ");
        dto.setDecryptProfileCode(" aes-62000002 ");
        dto.setProtocolTemplateCode(" nf-crack-v1 ");
        dto.setDescription(" 首版裂缝接入模板 ");

        OnboardingTemplatePackServiceImpl service = new OnboardingTemplatePackServiceImpl(mapper);

        OnboardingTemplatePackVO result = service.createPack(dto, 10001L);

        assertEquals(7201L, result.getId());
        assertEquals("PACK-CRACK-V1", result.getPackCode());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(1, result.getVersionNo());

        ArgumentCaptor<OnboardingTemplatePack> captor = ArgumentCaptor.forClass(OnboardingTemplatePack.class);
        verify(mapper).insert(captor.capture());
        assertEquals("PACK-CRACK-V1", captor.getValue().getPackCode());
        assertEquals("裂缝模板包", captor.getValue().getPackName());
        assertEquals("ACTIVE", captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getVersionNo());
    }

    @Test
    void updatePackShouldIncreaseVersionAndPersistChanges() {
        OnboardingTemplatePack entity = new OnboardingTemplatePack();
        entity.setId(7201L);
        entity.setTenantId(1L);
        entity.setPackCode("PACK-CRACK-V1");
        entity.setPackName("裂缝模板包");
        entity.setStatus("ACTIVE");
        entity.setVersionNo(1);
        when(mapper.selectById(7201L)).thenReturn(entity);

        OnboardingTemplatePackUpdateDTO dto = new OnboardingTemplatePackUpdateDTO();
        dto.setPackCode(" PACK-CRACK-V1 ");
        dto.setPackName(" 裂缝模板包二版 ");
        dto.setStatus(" INACTIVE ");
        dto.setDescription(" 停用旧包 ");

        OnboardingTemplatePackServiceImpl service = new OnboardingTemplatePackServiceImpl(mapper);

        OnboardingTemplatePackVO result = service.updatePack(7201L, dto, 10001L);

        assertEquals("裂缝模板包二版", result.getPackName());
        assertEquals("INACTIVE", result.getStatus());
        assertEquals(2, result.getVersionNo());

        ArgumentCaptor<OnboardingTemplatePack> captor = ArgumentCaptor.forClass(OnboardingTemplatePack.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getVersionNo());
        assertEquals("INACTIVE", captor.getValue().getStatus());
    }

    @Test
    void pagePacksShouldMapRecords() {
        OnboardingTemplatePack entity = new OnboardingTemplatePack();
        entity.setId(7201L);
        entity.setPackCode("PACK-CRACK-V1");
        entity.setPackName("裂缝模板包");
        entity.setStatus("ACTIVE");
        entity.setVersionNo(1);
        Page<OnboardingTemplatePack> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(entity));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(page);

        OnboardingTemplatePackServiceImpl service = new OnboardingTemplatePackServiceImpl(mapper);

        PageResult<OnboardingTemplatePackVO> result = service.pagePacks(new OnboardingTemplatePackPageQueryDTO());

        assertEquals(1L, result.getTotal());
        assertEquals("PACK-CRACK-V1", result.getRecords().get(0).getPackCode());
        assertEquals("ACTIVE", result.getRecords().get(0).getStatus());
    }
}
