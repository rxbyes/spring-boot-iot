package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.framework.config.IotProperties;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
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
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private UnregisteredDeviceRosterService unregisteredDeviceRosterService;
    @Mock
    private DeviceInvalidReportStateService invalidReportStateService;

    private DeviceServiceImpl deviceService;
    private IotProperties iotProperties;

    @BeforeEach
    void setUp() {
        iotProperties = new IotProperties();
        IotProperties.Device device = new IotProperties.Device();
        device.setActivateDefault(true);
        iotProperties.setDevice(device);
        deviceService = spy(new DeviceServiceImpl(
                productService,
                devicePropertyMapper,
                productModelMapper,
                unregisteredDeviceRosterService,
                iotProperties,
                invalidReportStateService
        ));
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
    void addDeviceShouldRejectProductWithoutProtocol() {
        Product product = new Product();
        product.setId(1002L);
        product.setProductKey("protocol-missing-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        product.setProtocolCode("");
        product.setNodeType(1);
        when(productService.getRequiredByProductKey("protocol-missing-product")).thenReturn(product);

        DeviceAddDTO dto = buildDeviceAddDTO("protocol-missing-product", "demo-device-03");

        BizException ex = assertThrows(BizException.class, () -> deviceService.addDevice(dto));
        assertEquals("产品未配置接入协议，禁止继续建档: protocol-missing-product", ex.getMessage());
    }

    @Test
    void addDeviceShouldRejectProductWithoutNodeType() {
        Product product = new Product();
        product.setId(1003L);
        product.setProductKey("node-missing-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        product.setProtocolCode("mqtt-json");
        product.setNodeType(null);
        when(productService.getRequiredByProductKey("node-missing-product")).thenReturn(product);

        DeviceAddDTO dto = buildDeviceAddDTO("node-missing-product", "demo-device-04");

        BizException ex = assertThrows(BizException.class, () -> deviceService.addDevice(dto));
        assertEquals("产品未配置节点类型，禁止继续建档: node-missing-product", ex.getMessage());
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

    @Test
    void applyRelationFieldsShouldIgnoreNullRelationIdsWhenMapIsEmpty() throws Exception {
        DevicePageVO row = new DevicePageVO();
        Method applyRelationFieldsMethod = DeviceServiceImpl.class
                .getDeclaredMethod("applyRelationFields", DevicePageVO.class, Map.class);
        applyRelationFieldsMethod.setAccessible(true);

        assertDoesNotThrow(() -> applyRelationFieldsMethod.invoke(deviceService, row, Map.of()));
        assertNull(row.getGatewayDeviceCode());
        assertNull(row.getGatewayDeviceName());
        assertNull(row.getParentDeviceCode());
        assertNull(row.getParentDeviceName());
    }

    @Test
    void pageDevicesShouldAppendUnregisteredRowsAfterRegisteredRows() {
        Device registeredA = new Device();
        registeredA.setId(2001L);
        registeredA.setDeviceCode("registered-a");
        registeredA.setDeviceName("已登记 A");

        Device registeredB = new Device();
        registeredB.setId(2002L);
        registeredB.setDeviceCode("registered-b");
        registeredB.setDeviceName("已登记 B");

        Page<Device> registeredPage = new Page<>(2, 10);
        registeredPage.setTotal(12);
        registeredPage.setRecords(List.of(registeredA, registeredB));

        doReturn(12L).when(deviceService).count(any());
        doReturn(registeredPage).when(deviceService).page(any(Page.class), any());

        DevicePageVO unregisteredA = new DevicePageVO();
        unregisteredA.setDeviceCode("unregistered-a");
        unregisteredA.setDeviceName("未登记设备");
        unregisteredA.setRegistrationStatus(0);

        DevicePageVO unregisteredB = new DevicePageVO();
        unregisteredB.setDeviceCode("unregistered-b");
        unregisteredB.setDeviceName("未登记设备");
        unregisteredB.setRegistrationStatus(0);

        when(unregisteredDeviceRosterService.countByFilters(null, null)).thenReturn(2L);
        when(unregisteredDeviceRosterService.listByFilters(null, null, 0L, 8L))
                .thenReturn(List.of(unregisteredA, unregisteredB));

        PageResult<DevicePageVO> result = deviceService.pageDevices(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                2L,
                10L
        );

        assertEquals(14L, result.getTotal());
        assertEquals(
                List.of("registered-a", "registered-b", "unregistered-a", "unregistered-b"),
                result.getRecords().stream().map(DevicePageVO::getDeviceCode).toList()
        );
        assertEquals(
                List.of(1, 1, 0, 0),
                result.getRecords().stream().map(DevicePageVO::getRegistrationStatus).toList()
        );
    }

    @Test
    void pageDevicesShouldSupportUnregisteredOnlyFilter() {
        DevicePageVO unregisteredRow = new DevicePageVO();
        unregisteredRow.setDeviceCode("shadow-device-01");
        unregisteredRow.setDeviceName("未登记设备");
        unregisteredRow.setRegistrationStatus(0);

        when(unregisteredDeviceRosterService.countByFilters("shadow-product", "shadow-device"))
                .thenReturn(1L);
        when(unregisteredDeviceRosterService.listByFilters("shadow-product", "shadow-device", 0L, 10L))
                .thenReturn(List.of(unregisteredRow));

        PageResult<DevicePageVO> result = deviceService.pageDevices(
                null,
                "shadow-product",
                "shadow-device",
                null,
                null,
                null,
                null,
                0,
                1L,
                10L
        );

        assertEquals(1L, result.getTotal());
        assertEquals(List.of("shadow-device-01"), result.getRecords().stream().map(DevicePageVO::getDeviceCode).toList());
        assertEquals(List.of(0), result.getRecords().stream().map(DevicePageVO::getRegistrationStatus).toList());
        verify(deviceService, never()).count(any());
    }

    @Test
    void addDeviceShouldResolveInvalidReportStateAfterArchiveCreate() {
        DeviceServiceImpl resolvingService = spy(new DeviceServiceImpl(
                productService,
                devicePropertyMapper,
                productModelMapper,
                unregisteredDeviceRosterService,
                iotProperties,
                invalidReportStateService
        ));
        doReturn(deviceMapper).when(resolvingService).getBaseMapper();
        when(deviceMapper.selectOne(any())).thenReturn(null);
        doReturn(true).when(resolvingService).save(any(Device.class));

        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setId(3001L);
        detail.setDeviceCode("missing-01");
        doReturn(detail).when(resolvingService).getDetailById(any());

        DeviceAddDTO dto = buildDeviceAddDTO("obs-product", "missing-01");
        when(productService.getRequiredByProductKey("obs-product")).thenReturn(enabledProduct("obs-product"));

        resolvingService.addDevice(dto);

        verify(invalidReportStateService).markResolvedByDevice(eq("obs-product"), eq("missing-01"), any(LocalDateTime.class));
    }

    private DeviceAddDTO buildDeviceAddDTO(String productKey, String deviceCode) {
        DeviceAddDTO dto = new DeviceAddDTO();
        dto.setProductKey(productKey);
        dto.setDeviceName("Demo Device");
        dto.setDeviceCode(deviceCode);
        return dto;
    }

    private Product enabledProduct(String productKey) {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey(productKey);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        return product;
    }
}
