package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductDeviceStatRow;
import com.ghlzm.iot.device.vo.ProductPageVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = spy(new ProductServiceImpl(deviceMapper));
    }

    @Test
    void updateProductShouldRejectDisableWhenEnabledDevicesStillExist() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        doReturn(product).when(productService).getRequiredById(1001L);
        when(deviceMapper.selectCount(any())).thenReturn(2L);

        ProductAddDTO dto = new ProductAddDTO();
        dto.setProductKey("demo-product");
        dto.setProductName("Demo Product");
        dto.setProtocolCode("mqtt-json");
        dto.setNodeType(1);
        dto.setStatus(ProductStatusEnum.DISABLED.getCode());

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));
        assertEquals("产品下仍有 2 台启用设备，请先核查库存设备是否仍在使用后再停用", ex.getMessage());
        verify(deviceMapper).selectCount(any());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void deleteProductShouldUseLogicRemoveWhenNoDevicesExist() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        doReturn(product).when(productService).getRequiredById(1001L);
        when(deviceMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(productService).removeById(1001L);

        productService.deleteProduct(1001L);

        verify(productService).removeById(1001L);
    }

    @Test
    void getDetailByIdShouldUseAggregatedDeviceStats() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        doReturn(product).when(productService).getRequiredById(1001L);

        ProductDeviceStatRow statRow = new ProductDeviceStatRow();
        statRow.setProductId(1001L);
        statRow.setDeviceCount(6L);
        statRow.setOnlineDeviceCount(2L);
        LocalDateTime lastReportTime = LocalDateTime.of(2026, 3, 20, 10, 30);
        statRow.setLastReportTime(lastReportTime);
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of(statRow));

        ProductActivityStatRow activityStatRow = new ProductActivityStatRow();
        activityStatRow.setProductId(1001L);
        activityStatRow.setTodayActiveCount(3L);
        activityStatRow.setSevenDaysActiveCount(5L);
        activityStatRow.setThirtyDaysActiveCount(8L);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(deviceMapper.selectProductActivityStat(1001L, todayStart, todayStart.minusDays(7), todayStart.minusDays(30)))
                .thenReturn(activityStatRow);

        ProductDetailVO detail = productService.getDetailById(1001L);

        assertEquals(6L, detail.getDeviceCount());
        assertEquals(2L, detail.getOnlineDeviceCount());
        assertEquals(lastReportTime, detail.getLastReportTime());
        assertEquals(3L, detail.getTodayActiveCount());
        assertEquals(5L, detail.getSevenDaysActiveCount());
        assertEquals(8L, detail.getThirtyDaysActiveCount());
        verify(deviceMapper).selectProductStats(any());
        verify(deviceMapper).selectProductActivityStat(1001L, todayStart, todayStart.minusDays(7), todayStart.minusDays(30));
    }

    @Test
    void pageProductsShouldMapAggregatedStatsIntoRows() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        Page<Product> page = new Page<>(1, 10);
        page.setCurrent(1L);
        page.setSize(10L);
        page.setTotal(1L);
        page.setRecords(List.of(product));
        doReturn(page).when(productService).page(any(Page.class), any(LambdaQueryWrapper.class));

        ProductDeviceStatRow statRow = new ProductDeviceStatRow();
        statRow.setProductId(1001L);
        statRow.setDeviceCount(8L);
        statRow.setOnlineDeviceCount(3L);
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of(statRow));

        PageResult<ProductPageVO> result = productService.pageProducts(null, "Demo", null, null, null, 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(8L, result.getRecords().get(0).getDeviceCount());
        assertEquals(3L, result.getRecords().get(0).getOnlineDeviceCount());
        verify(deviceMapper).selectProductStats(any());
        verify(deviceMapper, never()).selectProductActivityStat(any(), any(), any(), any());
    }
}
