package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductContractReleaseControllerTest {

    @Mock
    private ProductContractReleaseService productContractReleaseService;

    private ProductContractReleaseController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductContractReleaseController(productContractReleaseService);
    }

    @Test
    void pageBatchesShouldDelegateToService() {
        ProductContractReleaseBatchVO batch = batchVO(7001L, "phase1-crack", "manual_compare_apply", 3);
        when(productContractReleaseService.pageBatches(1001L, 1L, 10L))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(batch)));

        R<PageResult<ProductContractReleaseBatchVO>> response = controller.pageBatches(1001L, 1L, 10L);

        assertEquals(1L, response.getData().getTotal());
        assertEquals(7001L, response.getData().getRecords().get(0).getId());
        verify(productContractReleaseService).pageBatches(1001L, 1L, 10L);
    }

    @Test
    void getBatchShouldDelegateToService() {
        when(productContractReleaseService.getBatch(7001L))
                .thenReturn(batchVO(7001L, "phase1-crack", "manual_compare_apply", 3));

        R<ProductContractReleaseBatchVO> response = controller.getBatch(7001L);

        assertEquals("phase1-crack", response.getData().getScenarioCode());
        verify(productContractReleaseService).getBatch(7001L);
    }

    private ProductContractReleaseBatchVO batchVO(Long id,
                                                  String scenarioCode,
                                                  String releaseSource,
                                                  Integer releasedFieldCount) {
        ProductContractReleaseBatchVO vo = new ProductContractReleaseBatchVO();
        vo.setId(id);
        vo.setProductId(1001L);
        vo.setScenarioCode(scenarioCode);
        vo.setReleaseSource(releaseSource);
        vo.setReleasedFieldCount(releasedFieldCount);
        vo.setCreateBy(10001L);
        vo.setCreateTime(LocalDateTime.of(2026, 4, 6, 9, 30));
        return vo;
    }
}
