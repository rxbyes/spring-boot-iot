package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductModelControllerTest {

    @Mock
    private ProductModelService productModelService;

    private ProductModelController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductModelController(productModelService);
    }

    @Test
    void listShouldReturnProductScopedModels() {
        when(productModelService.listModels(1001L)).thenReturn(List.of(modelVO(2001L, "temperature", 10)));

        R<List<ProductModelVO>> response = controller.list(1001L);

        assertEquals(1, response.getData().size());
        assertEquals("temperature", response.getData().get(0).getIdentifier());
        verify(productModelService).listModels(1001L);
    }

    @Test
    void addShouldDelegateToService() {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("property");
        dto.setIdentifier("temperature");
        dto.setModelName("温度");
        when(productModelService.createModel(1001L, dto)).thenReturn(modelVO(2001L, "temperature", 10));

        R<ProductModelVO> response = controller.add(1001L, dto);

        assertEquals("temperature", response.getData().getIdentifier());
        verify(productModelService).createModel(1001L, dto);
    }

    @Test
    void updateShouldDelegateToService() {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("property");
        dto.setIdentifier("temperature");
        dto.setModelName("温度");
        when(productModelService.updateModel(1001L, 2001L, dto)).thenReturn(modelVO(2001L, "temperature", 10));

        R<ProductModelVO> response = controller.update(1001L, 2001L, dto);

        assertEquals(2001L, response.getData().getId());
        verify(productModelService).updateModel(1001L, 2001L, dto);
    }

    @Test
    void deleteShouldDelegateToService() {
        R<Void> response = controller.delete(1001L, 2001L);

        assertNull(response.getData());
        verify(productModelService).deleteModel(1001L, 2001L);
    }

    @Test
    void compareGovernanceShouldDelegateToService() {
        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareVO result = new ProductModelGovernanceCompareVO();
        result.setProductId(1001L);
        when(productModelService.compareGovernance(1001L, dto)).thenReturn(result);

        R<ProductModelGovernanceCompareVO> response = controller.compareGovernance(1001L, dto);

        assertEquals(1001L, response.getData().getProductId());
        verify(productModelService).compareGovernance(1001L, dto);
    }

    @Test
    void applyGovernanceShouldDelegateToService() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setCreatedCount(1);
        when(productModelService.applyGovernance(1001L, dto)).thenReturn(result);

        R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto);

        assertEquals(1, response.getData().getCreatedCount());
        verify(productModelService).applyGovernance(1001L, dto);
    }

    private ProductModelVO modelVO(Long id, String identifier, Integer sortNo) {
        ProductModelVO vo = new ProductModelVO();
        vo.setId(id);
        vo.setProductId(1001L);
        vo.setModelType("property");
        vo.setIdentifier(identifier);
        vo.setModelName(identifier);
        vo.setSortNo(sortNo);
        return vo;
    }
}
