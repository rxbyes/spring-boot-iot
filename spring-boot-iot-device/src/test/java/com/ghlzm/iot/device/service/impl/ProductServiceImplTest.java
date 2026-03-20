package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
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
}
