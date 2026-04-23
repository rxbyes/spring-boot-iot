package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackCreateDTO;
import com.ghlzm.iot.device.dto.OnboardingTemplatePackPageQueryDTO;
import com.ghlzm.iot.device.service.OnboardingTemplatePackService;
import com.ghlzm.iot.device.vo.OnboardingTemplatePackVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingTemplatePackControllerTest {

    @Mock
    private OnboardingTemplatePackService service;

    private OnboardingTemplatePackController controller;

    @BeforeEach
    void setUp() {
        controller = new OnboardingTemplatePackController(service);
    }

    @Test
    void pagePacksShouldDelegateToService() {
        when(service.pagePacks(any(OnboardingTemplatePackPageQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row())));

        R<PageResult<OnboardingTemplatePackVO>> response = controller.pagePacks(new OnboardingTemplatePackPageQueryDTO());

        assertEquals(1L, response.getData().getTotal());
        assertEquals("PACK-CRACK-V1", response.getData().getRecords().get(0).getPackCode());
        verify(service).pagePacks(any(OnboardingTemplatePackPageQueryDTO.class));
    }

    @Test
    void createPackShouldUseCurrentUserId() {
        OnboardingTemplatePackCreateDTO dto = new OnboardingTemplatePackCreateDTO();
        dto.setPackCode("PACK-CRACK-V1");
        dto.setPackName("裂缝模板包");
        when(service.createPack(dto, 10001L)).thenReturn(row());

        R<OnboardingTemplatePackVO> response = controller.createPack(dto, authentication(10001L));

        assertEquals(7201L, response.getData().getId());
        verify(service).createPack(dto, 10001L);
    }

    private OnboardingTemplatePackVO row() {
        OnboardingTemplatePackVO row = new OnboardingTemplatePackVO();
        row.setId(7201L);
        row.setPackCode("PACK-CRACK-V1");
        row.setPackName("裂缝模板包");
        row.setStatus("ACTIVE");
        row.setVersionNo(1);
        return row;
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
