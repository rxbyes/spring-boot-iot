package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.ProductOverviewSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService);
    }

    @Test
    void getOverviewSummaryShouldDelegateToService() {
        ProductOverviewSummaryVO summary = new ProductOverviewSummaryVO();
        summary.setProductId(1001L);
        summary.setFormalFieldCount(3);
        summary.setLatestReleaseBatchId(7001L);
        when(productService.getOverviewSummary(1001L)).thenReturn(summary);

        R<ProductOverviewSummaryVO> response = controller.getOverviewSummary(1001L);

        assertEquals(1001L, response.getData().getProductId());
        assertEquals(7001L, response.getData().getLatestReleaseBatchId());
        verify(productService).getOverviewSummary(1001L);
    }
}
