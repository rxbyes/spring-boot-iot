package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
import com.ghlzm.iot.device.dto.ProductModelManualExtractDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
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
    void listCandidatesShouldDelegateToService() {
        ProductModelCandidateResultVO result = new ProductModelCandidateResultVO();
        result.setProductId(1001L);
        when(productModelService.listModelCandidates(1001L)).thenReturn(result);

        R<ProductModelCandidateResultVO> response = controller.listCandidates(1001L);

        assertEquals(1001L, response.getData().getProductId());
        verify(productModelService).listModelCandidates(1001L);
    }

    @Test
    void confirmCandidatesShouldDelegateToService() {
        ProductModelCandidateConfirmDTO dto = new ProductModelCandidateConfirmDTO();
        ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
        summary.setCreatedCount(1);
        when(productModelService.confirmModelCandidates(1001L, dto)).thenReturn(summary);

        R<ProductModelCandidateSummaryVO> response = controller.confirmCandidates(1001L, dto);

        assertEquals(1, response.getData().getCreatedCount());
        verify(productModelService).confirmModelCandidates(1001L, dto);
    }

    @Test
    void manualExtractShouldDelegateToService() {
        ProductModelManualExtractDTO dto = new ProductModelManualExtractDTO();
        dto.setSampleType("business");
        dto.setSamplePayload("{\"SK11\":{\"L1_QJ_1\":{\"2026-03-31T04:05:55.000Z\":{\"X\":-0.0376}}}}");
        ProductModelCandidateResultVO result = new ProductModelCandidateResultVO();
        ProductModelCandidateSummaryVO summary = new ProductModelCandidateSummaryVO();
        summary.setExtractionMode("manual");
        summary.setSampleType("business");
        summary.setSampleDeviceCode("SK11");
        result.setProductId(1001L);
        result.setSummary(summary);
        when(productModelService.manualExtractModelCandidates(1001L, dto)).thenReturn(result);

        R<ProductModelCandidateResultVO> response = controller.manualExtract(1001L, dto);

        assertEquals("SK11", response.getData().getSummary().getSampleDeviceCode());
        verify(productModelService).manualExtractModelCandidates(1001L, dto);
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
