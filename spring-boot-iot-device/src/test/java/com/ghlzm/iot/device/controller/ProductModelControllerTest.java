package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import com.ghlzm.iot.framework.security.JwtUserPrincipal;
import com.ghlzm.iot.system.security.GovernancePermissionGuard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductModelControllerTest {

    @Mock
    private ProductModelService productModelService;

    @Mock
    private GovernancePermissionGuard permissionGuard;

    private ProductModelController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductModelController(productModelService, permissionGuard);
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
        dto.setModelName("temperature");
        Authentication authentication = authentication(1001L);
        when(productModelService.createModel(1001L, dto)).thenReturn(modelVO(2001L, "temperature", 10));

        R<ProductModelVO> response = controller.add(1001L, dto, authentication);

        assertEquals("temperature", response.getData().getIdentifier());
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "产品契约维护",
                "iot:normative-library:write"
        );
        verify(productModelService).createModel(1001L, dto);
    }

    @Test
    void updateShouldDelegateToService() {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("property");
        dto.setIdentifier("temperature");
        dto.setModelName("temperature");
        Authentication authentication = authentication(1001L);
        when(productModelService.updateModel(1001L, 2001L, dto)).thenReturn(modelVO(2001L, "temperature", 10));

        R<ProductModelVO> response = controller.update(1001L, 2001L, dto, authentication);

        assertEquals(2001L, response.getData().getId());
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "产品契约维护",
                "iot:normative-library:write"
        );
        verify(productModelService).updateModel(1001L, 2001L, dto);
    }

    @Test
    void deleteShouldDelegateToService() {
        Authentication authentication = authentication(1001L);

        R<Void> response = controller.delete(1001L, 2001L, authentication);

        assertNull(response.getData());
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "产品契约维护",
                "iot:normative-library:write"
        );
        verify(productModelService).deleteModel(1001L, 2001L);
    }

    @Test
    void compareGovernanceShouldDelegateToService() {
        ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
        ProductModelGovernanceCompareVO result = new ProductModelGovernanceCompareVO();
        result.setProductId(1001L);
        result.setCompareRows(List.of(compareRow("value", "value", "Crack value", true)));
        Authentication authentication = authentication(1001L);
        when(productModelService.compareGovernance(1001L, dto)).thenReturn(result);

        R<ProductModelGovernanceCompareVO> response = controller.compareGovernance(1001L, dto, authentication);

        assertEquals(1001L, response.getData().getProductId());
        assertEquals("value", response.getData().getCompareRows().get(0).getNormativeIdentifier());
        assertEquals(true, response.getData().getCompareRows().get(0).getRiskReady());
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "产品契约治理",
                "iot:product-contract:govern"
        );
        verify(productModelService).compareGovernance(1001L, dto);
    }

    @Test
    void applyGovernanceShouldDelegateToService() {
        ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
        ProductModelGovernanceApplyResultVO result = new ProductModelGovernanceApplyResultVO();
        result.setCreatedCount(1);
        result.setReleaseBatchId(12345L);
        Authentication authentication = authentication(1001L);
        when(productModelService.applyGovernance(1001L, dto, 1001L)).thenReturn(result);

        R<ProductModelGovernanceApplyResultVO> response = controller.applyGovernance(1001L, dto, 2002L, authentication);

        assertEquals(1, response.getData().getCreatedCount());
        assertEquals(12345L, response.getData().getReleaseBatchId());
        verify(permissionGuard).requireDualControl(
                1001L,
                2002L,
                "产品契约发布",
                "iot:product-contract:release",
                "iot:product-contract:approve"
        );
        verify(permissionGuard).requireAnyPermission(
                1001L,
                "风险指标标注",
                "risk:metric-catalog:tag"
        );
        verify(productModelService).applyGovernance(1001L, dto, 1001L);
    }

    private Authentication authentication(Long userId) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, "demo");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
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

    private com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO compareRow(String identifier,
                                                                                   String normativeIdentifier,
                                                                                   String normativeName,
                                                                                   boolean riskReady) {
        com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO row =
                new com.ghlzm.iot.device.vo.ProductModelGovernanceCompareRowVO();
        row.setIdentifier(identifier);
        row.setNormativeIdentifier(normativeIdentifier);
        row.setNormativeName(normativeName);
        row.setRiskReady(riskReady);
        return row;
    }
}
