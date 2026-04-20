package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.enums.DeviceStatusEnum;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.device.DeviceBindingCapabilityType;
import com.ghlzm.iot.device.dto.DeviceAddDTO;
import com.ghlzm.iot.device.dto.DeviceOnboardingSuggestionQuery;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.RiskMetricCatalogReadMapper;
import com.ghlzm.iot.device.service.DeviceInvalidReportStateService;
import com.ghlzm.iot.device.service.DeviceOnboardingSuggestionService;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.service.UnregisteredDeviceRosterService;
import com.ghlzm.iot.device.vo.DeviceBatchAddResultVO;
import com.ghlzm.iot.device.vo.DeviceDetailVO;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.device.vo.DeviceOnboardingSuggestionVO;
import com.ghlzm.iot.device.vo.DeviceOptionVO;
import com.ghlzm.iot.device.vo.DevicePageVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.system.entity.Organization;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.OrganizationService;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doAnswer;
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
    private RiskMetricCatalogReadMapper riskMetricCatalogReadMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private UnregisteredDeviceRosterService unregisteredDeviceRosterService;
    @Mock
    private DeviceInvalidReportStateService invalidReportStateService;
    @Mock
    private DeviceOnboardingSuggestionService deviceOnboardingSuggestionService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private OrganizationService organizationService;

    private DeviceServiceImpl deviceService;
    private IotProperties iotProperties;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
    }

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
                riskMetricCatalogReadMapper,
                unregisteredDeviceRosterService,
                iotProperties,
                invalidReportStateService,
                deviceOnboardingSuggestionService,
                permissionService,
                organizationService
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
    void addDeviceShouldAssignCurrentUserPrimaryOrganization() {
        when(permissionService.getDataPermissionContext(101L))
                .thenReturn(new DataPermissionContext(101L, null, 7101L, DataScopeType.ORG, false));
        when(organizationService.getById(7101L)).thenReturn(activeOrganization(7101L, "ops-center"));
        when(productService.getRequiredByProductKey("demo-product")).thenReturn(enabledProduct("demo-product"));
        doReturn(deviceMapper).when(deviceService).getBaseMapper();
        when(deviceMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Device saved = invocation.getArgument(0);
            saved.setId(5001L);
            return true;
        }).when(deviceService).save(any(Device.class));

        DeviceDetailVO detail = new DeviceDetailVO();
        detail.setId(5001L);
        detail.setOrgId(7101L);
        detail.setOrgName("ops-center");
        doReturn(detail).when(deviceService).getDetailById(101L, 5001L);

        DeviceDetailVO result = deviceService.addDevice(101L, buildDeviceAddDTO("demo-product", "demo-device-05"));

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceService).save(deviceCaptor.capture());
        assertEquals(7101L, deviceCaptor.getValue().getOrgId());
        assertEquals("ops-center", deviceCaptor.getValue().getOrgName());
        assertEquals(7101L, result.getOrgId());
        assertEquals("ops-center", result.getOrgName());
    }

    @Test
    void addDeviceShouldRejectUserWithoutPrimaryOrganization() {
        when(permissionService.getDataPermissionContext(101L))
                .thenReturn(new DataPermissionContext(101L, null, null, DataScopeType.ORG, false));
        when(productService.getRequiredByProductKey("demo-product")).thenReturn(enabledProduct("demo-product"));
        doReturn(deviceMapper).when(deviceService).getBaseMapper();
        when(deviceMapper.selectOne(any())).thenReturn(null);

        BizException error = assertThrows(BizException.class,
                () -> deviceService.addDevice(101L, buildDeviceAddDTO("demo-product", "demo-device-06")));
        assertEquals("当前账号未绑定主机构，禁止维护设备归属", error.getMessage());
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
    void listPropertiesShouldPreferLatestProductModelNameOverStaleSnapshotName() {
        Device device = new Device();
        device.setId(2001L);
        device.setProductId(1001L);
        device.setDeviceCode("CXH15522832");
        doReturn(device).when(deviceService).getRequiredByCode("CXH15522832");

        DeviceProperty property = new DeviceProperty();
        property.setIdentifier("L1_QJ_1.angle");
        property.setPropertyName("angle");
        property.setPropertyValue("-6.03");
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of(property));

        ProductModel model = new ProductModel();
        model.setIdentifier("L1_QJ_1.angle");
        model.setModelName("水平面夹角");
        when(productModelMapper.selectList(any())).thenReturn(List.of(model));

        List<DeviceProperty> result = deviceService.listProperties("CXH15522832");

        assertEquals(1, result.size());
        assertEquals("水平面夹角", result.get(0).getPropertyName());
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

        when(unregisteredDeviceRosterService.countByFilters(null, null, null, null)).thenReturn(2L);
        when(unregisteredDeviceRosterService.listByFilters(null, null, null, null, 0L, 8L))
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

        when(unregisteredDeviceRosterService.countByFilters("shadow-device", "shadow-product", "北坡监测产品", null))
                .thenReturn(1L);
        when(unregisteredDeviceRosterService.listByFilters("shadow-device", "shadow-product", "北坡监测产品", null, 0L, 10L))
                .thenReturn(List.of(unregisteredRow));

        PageResult<DevicePageVO> result = deviceService.pageDevices(
                null,
                "shadow-device",
                "shadow-product",
                "北坡监测产品",
                null,
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
    void getOnboardingSuggestionShouldDelegateWithScopedTenant() {
        when(permissionService.getDataPermissionContext(101L))
                .thenReturn(new DataPermissionContext(101L, 8L, null, DataScopeType.TENANT, false));
        DeviceOnboardingSuggestionVO suggestion = new DeviceOnboardingSuggestionVO();
        suggestion.setTraceId("trace-unregistered-001");
        suggestion.setRecommendedProductKey("south_rtu");
        when(deviceOnboardingSuggestionService.suggest(eq(8L), any())).thenReturn(suggestion);

        DeviceOnboardingSuggestionQuery query = new DeviceOnboardingSuggestionQuery("trace-unregistered-001");
        DeviceOnboardingSuggestionVO result = deviceService.getOnboardingSuggestion(101L, query);

        assertEquals("south_rtu", result.getRecommendedProductKey());
        verify(deviceOnboardingSuggestionService).suggest(eq(8L), eq(query));
    }

    @Test
    void getOnboardingSuggestionShouldRejectOrganizationScopedUser() {
        when(permissionService.getDataPermissionContext(101L))
                .thenReturn(new DataPermissionContext(101L, 8L, 7101L, DataScopeType.ORG, false));

        BizException error = assertThrows(BizException.class,
                () -> deviceService.getOnboardingSuggestion(101L, new DeviceOnboardingSuggestionQuery("trace-unregistered-001")));

        assertEquals("当前数据权限暂不支持未登记设备接入建议", error.getMessage());
    }

    @Test
    void pageDevicesShouldFilterRegisteredRowsByCurrentTenant() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(0L).when(deviceService).count(any(LambdaQueryWrapper.class));

        deviceService.pageDevices(
                99L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                10L
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Device>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceService).count(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
    }

    @Test
    void pageDevicesShouldFilterRegisteredRowsByCurrentOrganization() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG, false));
        doReturn(0L).when(deviceService).count(any(LambdaQueryWrapper.class));

        deviceService.pageDevices(
                99L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1L,
                10L
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Device>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceService).count(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("org_id"));
    }

    @Test
    void getDetailByIdShouldRejectCrossTenantAccess() {
        Device crossTenantDevice = new Device();
        crossTenantDevice.setId(4001L);
        crossTenantDevice.setTenantId(9L);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(crossTenantDevice).when(deviceService).getRequiredById(4001L);

        BizException error = assertThrows(BizException.class, () -> deviceService.getDetailById(99L, 4001L));
        assertEquals("设备不存在或无权访问", error.getMessage());
    }

    @Test
    void getDetailByIdShouldRejectCrossOrganizationAccess() {
        Device crossOrgDevice = new Device();
        crossOrgDevice.setId(4004L);
        crossOrgDevice.setTenantId(8L);
        crossOrgDevice.setOrgId(7102L);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG, false));
        doReturn(crossOrgDevice).when(deviceService).getRequiredById(4004L);

        BizException error = assertThrows(BizException.class, () -> deviceService.getDetailById(99L, 4004L));
        assertEquals("设备不存在或无权访问", error.getMessage());
    }

    @Test
    void deleteDeviceShouldRejectCrossTenantAccess() {
        Device crossTenantDevice = new Device();
        crossTenantDevice.setId(4002L);
        crossTenantDevice.setTenantId(9L);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(crossTenantDevice).when(deviceService).getRequiredById(4002L);

        BizException error = assertThrows(BizException.class, () -> deviceService.deleteDevice(99L, 4002L));
        assertEquals("设备不存在或无权访问", error.getMessage());
        verify(deviceService, never()).removeById(4002L);
    }

    @Test
    void listDeviceOptionsShouldFilterByCurrentTenant() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(List.of()).when(deviceService).list(any(LambdaQueryWrapper.class));

        deviceService.listDeviceOptions(99L, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Device>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceService).list(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("device_status"));
    }

    @Test
    void listDeviceOptionsShouldFilterByAccessibleOrganizationsForOrgChildrenScope() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG_AND_CHILDREN, false));
        when(permissionService.listAccessibleOrganizationIds(99L)).thenReturn(java.util.Set.of(7101L, 7102L));
        doReturn(List.of()).when(deviceService).list(any(LambdaQueryWrapper.class));

        deviceService.listDeviceOptions(99L, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<Device>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceService).list(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("org_id"));
    }

    @Test
    void listDeviceOptionsShouldInferVideoCapabilityFromDeviceNameWhenProductMissing() {
        Device videoDevice = new Device();
        videoDevice.setId(4005L);
        videoDevice.setProductId(0L);
        videoDevice.setOrgId(7101L);
        videoDevice.setDeviceCode("3c2c1e3d-5f57-4db3-a42a-1cc2a5b21963");
        videoDevice.setDeviceName("G30甘肃天水宝天段K1328+850上行水毁监控");
        videoDevice.setDeviceStatus(DeviceStatusEnum.ENABLED.getCode());

        doReturn(List.of(videoDevice)).when(deviceService).list(any(LambdaQueryWrapper.class));
        when(productService.listByIds(any())).thenReturn(List.of());
        when(riskMetricCatalogReadMapper.selectList(any())).thenReturn(List.of());

        List<DeviceOptionVO> result = deviceService.listDeviceOptions(false);

        assertEquals(1, result.size());
        assertEquals(DeviceBindingCapabilityType.VIDEO.name(), result.get(0).getDeviceCapabilityType());
        assertEquals(Boolean.FALSE, result.get(0).getSupportsMetricBinding());
        assertEquals(Boolean.TRUE, result.get(0).getAiEventExpandable());
        assertEquals("3c2c1e3d-5f57-4db3-a42a-1cc2a5b21963", result.get(0).getDeviceCode());
    }

    @Test
    void listMetricOptionsShouldRejectCrossTenantDevice() {
        Device crossTenantDevice = new Device();
        crossTenantDevice.setId(4003L);
        crossTenantDevice.setTenantId(9L);
        crossTenantDevice.setProductId(1001L);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(crossTenantDevice).when(deviceService).getRequiredById(4003L);

        BizException error = assertThrows(BizException.class, () -> deviceService.listMetricOptions(99L, 4003L));
        assertEquals("设备不存在或无权访问", error.getMessage());
        verify(productModelMapper, never()).selectList(any());
        verify(devicePropertyMapper, never()).selectList(any());
    }

    @Test
    void listMetricOptionsShouldExposeRiskMetricIdForPublishedMetric() {
        Device device = new Device();
        device.setId(4001L);
        device.setTenantId(8L);
        device.setProductId(3003L);

        ProductModel productModel = new ProductModel();
        productModel.setId(3202L);
        productModel.setProductId(3003L);
        productModel.setModelType("property");
        productModel.setIdentifier("gpsTotalX");
        productModel.setModelName("GNSS 累计位移 X");
        productModel.setDataType("double");
        productModel.setDeleted(0);

        com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel catalog =
                new com.ghlzm.iot.device.entity.RiskMetricCatalogReadModel();
        catalog.setId(6102L);
        catalog.setProductId(3003L);
        catalog.setContractIdentifier("gpsTotalX");
        catalog.setEnabled(1);
        catalog.setDeleted(0);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        doReturn(device).when(deviceService).getRequiredById(4001L);
        when(productModelMapper.selectList(any())).thenReturn(List.of(productModel));
        when(devicePropertyMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogReadMapper.selectList(any())).thenReturn(List.of(catalog));

        List<DeviceMetricOptionVO> options = deviceService.listMetricOptions(99L, 4001L);

        assertEquals(1, options.size());
        assertEquals(6102L, options.get(0).getRiskMetricId());
        assertEquals("gpsTotalX", options.get(0).getIdentifier());
    }

    @Test
    void addDeviceShouldResolveInvalidReportStateAfterArchiveCreate() {
        DeviceServiceImpl resolvingService = spy(new DeviceServiceImpl(
                productService,
                devicePropertyMapper,
                productModelMapper,
                riskMetricCatalogReadMapper,
                unregisteredDeviceRosterService,
                iotProperties,
                invalidReportStateService,
                deviceOnboardingSuggestionService,
                permissionService,
                organizationService
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
        product.setTenantId(1L);
        return product;
    }

    private Organization activeOrganization(Long id, String orgName) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setOrgName(orgName);
        organization.setStatus(1);
        organization.setDeleted(0);
        return organization;
    }
}
