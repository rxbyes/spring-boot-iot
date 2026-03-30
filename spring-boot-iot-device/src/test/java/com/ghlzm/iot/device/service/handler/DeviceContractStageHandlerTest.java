package com.ghlzm.iot.device.service.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.model.DeviceProcessingTarget;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceContractStageHandlerTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;

    private DeviceContractStageHandler deviceContractStageHandler;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

    @BeforeEach
    void setUp() {
        deviceContractStageHandler = new DeviceContractStageHandler(deviceMapper, productMapper);
    }

    @Test
    void resolveShouldIncludeTenantIdWhenLookingUpDevice() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setDeviceCode("demo-device-01");
        device.setProductId(3001L);
        device.setProtocolCode("mqtt-json");
        when(deviceMapper.selectOne(any())).thenReturn(device);

        Product product = new Product();
        product.setId(3001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");
        product.setStatus(1);
        when(productMapper.selectById(3001L)).thenReturn(product);

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setTenantId("1");
        upMessage.setDeviceCode("demo-device-01");
        upMessage.setProductKey("demo-product");
        upMessage.setProtocolCode("mqtt-json");

        DeviceProcessingTarget target = deviceContractStageHandler.resolve(upMessage);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Device>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMapper).selectOne(wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
        assertEquals(device, target.getDevice());
        assertEquals(product, target.getProduct());
    }
}
