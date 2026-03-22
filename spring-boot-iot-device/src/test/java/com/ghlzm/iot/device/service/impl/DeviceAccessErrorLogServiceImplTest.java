package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.model.RawDeviceMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceAccessErrorLogServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private DeviceAccessErrorLogSchemaSupport schemaSupport;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;

    private DeviceAccessErrorLogServiceImpl service;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Protocol protocol = new IotProperties.Protocol();
        protocol.setDefaultCode("mqtt-json");
        iotProperties.setProtocol(protocol);
        service = new DeviceAccessErrorLogServiceImpl(
                jdbcTemplate,
                schemaSupport,
                deviceMapper,
                productMapper,
                iotProperties
        );
    }

    @Test
    void archiveMqttFailureShouldPersistContractSnapshot() {
        when(schemaSupport.getColumns()).thenReturn(new LinkedHashSet<>(List.of("id", "contract_snapshot")));

        Device device = new Device();
        device.setDeviceCode("demo-device-01");
        device.setProductId(1001L);
        device.setProtocolCode("");
        when(deviceMapper.selectOne(any())).thenReturn(device);

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");
        when(productMapper.selectById(1001L)).thenReturn(product);

        RawDeviceMessage rawDeviceMessage = new RawDeviceMessage();
        rawDeviceMessage.setTopicRouteType("legacy");
        rawDeviceMessage.setProtocolCode("mqtt-json");
        rawDeviceMessage.setDeviceCode("demo-device-01");
        rawDeviceMessage.setProductKey("demo-product");

        service.archiveMqttFailure(
                "$dp",
                "{\"body\":1}".getBytes(StandardCharsets.UTF_8),
                rawDeviceMessage,
                "device_validate",
                new BizException("设备未绑定产品: demo-device-01")
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), argsCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("contract_snapshot"));
        String contractSnapshot = (String) argsCaptor.getValue()[1];
        assertTrue(contractSnapshot.contains("\"routeType\":\"legacy\""));
        assertTrue(contractSnapshot.contains("\"expectedProtocolCode\":\"mqtt-json\""));
        assertTrue(contractSnapshot.contains("\"protocolSource\":\"product-fallback\""));
    }

    @Test
    void mapRowShouldReadContractSnapshot() throws Exception {
        ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(resultSet.getLong("id")).thenReturn(2001L);
        when(resultSet.wasNull()).thenReturn(false);
        when(resultSet.getString("contract_snapshot")).thenReturn("{\"expectedProtocolCode\":\"mqtt-json\"}");

        Method mapRow = DeviceAccessErrorLogServiceImpl.class
                .getDeclaredMethod("mapRow", ResultSet.class, Set.class);
        mapRow.setAccessible(true);

        DeviceAccessErrorLog log = (DeviceAccessErrorLog) mapRow.invoke(
                service,
                resultSet,
                new LinkedHashSet<>(List.of("id", "contract_snapshot"))
        );

        assertEquals(2001L, log.getId());
        assertEquals("{\"expectedProtocolCode\":\"mqtt-json\"}", log.getContractSnapshot());
    }
}
