package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.framework.config.IotProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private ProductService productService;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private ProductModelMapper productModelMapper;

    private DeviceServiceImpl deviceService;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setActivateDefault(true);
        iotProperties.setDevice(device);
        deviceService = spy(new DeviceServiceImpl(productService, devicePropertyMapper, productModelMapper, iotProperties));
    }

    @Test
    void addDeviceShouldRejectDisabledProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("disabled-product");
        product.setStatus(ProductStatusEnum.DISABLED.getCode());
        when(productService.getRequiredByProductKey("disabled-product")).thenReturn(product);

        DeviceAddDTO dto = buildDeviceAddDTO("disabled-product", "demo-device-01");

        BizException ex = assertThrows(BizException.class, () -> deviceService.addDevice(dto));
        assertEquals("产品已停用，禁止继续建档: disabled-product", ex.getMessage());
    }

    @Test
    void batchAddDevicesShouldCollectDisabledProductError() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("disabled-product");
        product.setStatus(ProductStatusEnum.DISABLED.getCode());
        when(productService.getRequiredByProductKey("disabled-product")).thenReturn(product);

        DeviceBatchAddResultVO result = deviceService.batchAddDevices(List.of(buildDeviceAddDTO("disabled-product", "demo-device-02")));

        assertEquals(1, result.getTotalCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("产品已停用，禁止继续建档: disabled-product", result.getErrors().get(0).getMessage());
    }

    @Test
    void deleteDeviceShouldUseLogicRemove() {
        Device device = new Device();
        device.setId(2001L);
        doReturn(device).when(deviceService).getRequiredById(2001L);
        doReturn(true).when(deviceService).removeById(2001L);

        deviceService.deleteDevice(2001L);

        verify(deviceService).removeById(2001L);
    }

    private DeviceAddDTO buildDeviceAddDTO(String productKey, String deviceCode) {
        DeviceAddDTO dto = new DeviceAddDTO();
        dto.setProductKey(productKey);
        dto.setDeviceName("Demo Device");
        dto.setDeviceCode(deviceCode);
        return dto;
    }
}
