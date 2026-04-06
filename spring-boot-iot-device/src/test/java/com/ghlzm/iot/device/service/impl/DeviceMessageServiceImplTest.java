package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.event.DeviceRiskEvaluationEvent;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.DeviceProperty;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.DeviceMessageLogMapper;
import com.ghlzm.iot.device.mapper.DevicePropertyMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.CommandRecordService;
import com.ghlzm.iot.device.service.DeviceFileService;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.DeviceSessionService;
import com.ghlzm.iot.device.service.handler.DeviceContractStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceMessageLogStageHandler;
import com.ghlzm.iot.device.service.handler.DevicePayloadApplyStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceRiskDispatchStageHandler;
import com.ghlzm.iot.device.service.handler.DeviceStateStageHandler;
import com.ghlzm.iot.device.vo.DeviceMessageTraceStatsVO;
import com.ghlzm.iot.device.vo.DeviceStatsBucketVO;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import com.ghlzm.iot.framework.config.IotProperties;
import com.ghlzm.iot.protocol.core.adapter.ProtocolAdapter;
import com.ghlzm.iot.protocol.core.context.ProtocolContext;
import com.ghlzm.iot.protocol.core.model.DeviceUpMessage;
import com.ghlzm.iot.protocol.core.model.DeviceUpProtocolMetadata;
import com.ghlzm.iot.protocol.core.registry.ProtocolAdapterRegistry;
import com.ghlzm.iot.system.enums.DataScopeType;
import com.ghlzm.iot.system.service.PermissionService;
import com.ghlzm.iot.system.service.model.DataPermissionContext;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceMessageServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Device.class);
        TableInfoHelper.initTableInfo(assistant, DeviceMessageLog.class);
    }

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private DeviceMessageLogMapper deviceMessageLogMapper;
    @Mock
    private DevicePropertyMapper devicePropertyMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModelMapper productModelMapper;
    @Mock
    private CommandRecordService commandRecordService;
    @Mock
    private DeviceFileService deviceFileService;
    @Mock
    private DeviceOnlineSessionService deviceOnlineSessionService;
    @Mock
    private DeviceSessionService deviceSessionService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PermissionService permissionService;
    @Mock
    private ProtocolAdapterRegistry protocolAdapterRegistry;
    @Mock
    private ProtocolAdapter protocolAdapter;

    private DeviceMessageServiceImpl deviceMessageService;

    @BeforeEach
    void setUp() {
        IotProperties iotProperties = new IotProperties();
        IotProperties.Device deviceConfig = new IotProperties.Device();
        deviceConfig.setActivateDefault(true);
        iotProperties.setDevice(deviceConfig);
        DeviceContractStageHandler deviceContractStageHandler =
                new DeviceContractStageHandler(deviceMapper, productMapper);
        DeviceMessageLogStageHandler deviceMessageLogStageHandler =
                new DeviceMessageLogStageHandler(deviceMessageLogMapper);
        DeviceTelemetryMappingServiceImpl deviceTelemetryMappingService =
                new DeviceTelemetryMappingServiceImpl(productModelMapper);
        DevicePropertyMetadataService devicePropertyMetadataService =
                new DevicePropertyMetadataServiceImpl(productModelMapper);
        DevicePayloadApplyStageHandler devicePayloadApplyStageHandler =
                new DevicePayloadApplyStageHandler(
                        devicePropertyMapper,
                        devicePropertyMetadataService,
                        commandRecordService,
                        deviceFileService
                );
        DeviceStateStageHandler deviceStateStageHandler =
                new DeviceStateStageHandler(
                        deviceMapper,
                        deviceOnlineSessionService,
                        deviceSessionService,
                        iotProperties
                );
        DeviceRiskDispatchStageHandler deviceRiskDispatchStageHandler =
                new DeviceRiskDispatchStageHandler(eventPublisher);
        deviceMessageService = new DeviceMessageServiceImpl(
                deviceMapper,
                deviceMessageLogMapper,
                devicePropertyMapper,
                productMapper,
                productModelMapper,
                commandRecordService,
                deviceFileService,
                deviceOnlineSessionService,
                jdbcTemplate,
                iotProperties,
                eventPublisher,
                deviceContractStageHandler,
                deviceMessageLogStageHandler,
                devicePayloadApplyStageHandler,
                deviceStateStageHandler,
                deviceRiskDispatchStageHandler,
                permissionService,
                protocolAdapterRegistry
        );
    }

    @Test
    void listMessageLogsShouldRejectCrossTenantDevice() {
        Device crossTenantDevice = new Device();
        crossTenantDevice.setId(3001L);
        crossTenantDevice.setTenantId(9L);
        crossTenantDevice.setDeviceCode("demo-device-tenant-9");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMapper.selectOne(any())).thenReturn(crossTenantDevice);

        assertThrows(BizException.class, () -> deviceMessageService.listMessageLogs(99L, "demo-device-tenant-9"));
        verify(deviceMessageLogMapper, never()).selectList(any());
    }

    @Test
    void listMessageLogsShouldRejectCrossOrganizationDevice() {
        Device crossOrgDevice = new Device();
        crossOrgDevice.setId(3002L);
        crossOrgDevice.setTenantId(8L);
        crossOrgDevice.setOrgId(7102L);
        crossOrgDevice.setDeviceCode("demo-device-org-7102");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG, false));
        when(deviceMapper.selectOne(any())).thenReturn(crossOrgDevice);

        assertThrows(BizException.class, () -> deviceMessageService.listMessageLogs(99L, "demo-device-org-7102"));
        verify(deviceMessageLogMapper, never()).selectList(any());
    }

    @Test
    void pageMessageTraceLogsShouldApplyTenantFilter() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMessageLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Page.class));

        deviceMessageService.pageMessageTraceLogs(99L, new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery(), 1, 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<DeviceMessageLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMessageLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        assertTrue(wrapperCaptor.getValue().getSqlSegment().contains("tenant_id"));
    }

    @Test
    void pageMessageTraceLogsShouldApplyOrganizationFilter() {
        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG, false));
        when(deviceMessageLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Page.class));

        deviceMessageService.pageMessageTraceLogs(99L, new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery(), 1, 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<DeviceMessageLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMessageLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("tenant_id"));
        assertTrue(sqlSegment.contains("org_id"));
    }

    @Test
    void pageMessageTraceLogsShouldApplyKeywordAcrossTraceDeviceAndProduct() {
        com.ghlzm.iot.device.dto.DeviceMessageTraceQuery query = new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery();
        query.setKeyword("demo-device-01");
        query.setMessageType("report");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMessageLogMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Page.class));

        deviceMessageService.pageMessageTraceLogs(99L, query, 1, 10);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<DeviceMessageLog>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deviceMessageLogMapper).selectPage(any(Page.class), wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("trace_id"));
        assertTrue(sqlSegment.contains("device_code"));
        assertTrue(sqlSegment.contains("product_key"));
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue("demo-device-01"));
        assertTrue(wrapperCaptor.getValue().getParamNameValuePairs().containsValue("property"));
    }

    @Test
    void getMessageTraceDetailShouldRecoverPayloadComparisonFromStoredLog() {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setId(1L);
        logRecord.setTenantId(8L);
        logRecord.setDeviceId(3001L);
        logRecord.setProductId(1001L);
        logRecord.setTraceId("trace-001");
        logRecord.setDeviceCode("demo-device-01");
        logRecord.setProductKey("demo-product");
        logRecord.setMessageType("report");
        logRecord.setTopic("$dp");
        logRecord.setPayload("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(8L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("17165802");
        upMessage.setProductKey("demo-product");
        upMessage.setMessageType("property");
        DeviceUpProtocolMetadata protocolMetadata = new DeviceUpProtocolMetadata();
        protocolMetadata.setDecryptedPayloadPreview("{\"17165802\":{\"temperature\":26.5}}");
        protocolMetadata.setDecodedPayloadPreview(Map.of(
                "messageType", "property",
                "deviceCode", "17165802",
                "properties", Map.of("temperature", 26.5)
        ));
        Object templateEvidence = newInstance("com.ghlzm.iot.protocol.core.model.ProtocolTemplateEvidence");
        invokeSetter(templateEvidence, "setTemplateCodes", List.of("crack_child_template"));
        Object execution = newInstance("com.ghlzm.iot.protocol.core.model.ProtocolTemplateExecutionEvidence");
        invokeSetter(execution, "setTemplateCode", "crack_child_template");
        invokeSetter(execution, "setLogicalChannelCode", "L1_LF_1");
        invokeSetter(execution, "setChildDeviceCode", "202018143");
        invokeSetter(execution, "setCanonicalizationStrategy", "LF_VALUE");
        invokeSetter(execution, "setStatusMirrorApplied", Boolean.TRUE);
        invokeSetter(execution, "setParentRemovalKeys", List.of("L1_LF_1"));
        invokeSetter(templateEvidence, "setExecutions", List.of(execution));
        invokeSetter(protocolMetadata, "setTemplateEvidence", templateEvidence);
        upMessage.setProtocolMetadata(protocolMetadata);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMessageLogMapper.selectOne(any())).thenReturn(logRecord);
        when(deviceMapper.selectById(3001L)).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any(ProtocolContext.class))).thenReturn(upMessage);

        MessageTraceDetailVO detail = deviceMessageService.getMessageTraceDetail(99L, 1L);

        assertEquals("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}", detail.getRawPayload());
        assertEquals("{\"17165802\":{\"temperature\":26.5}}", detail.getDecryptedPayload());
        assertEquals("17165802", detail.getDecodedPayload().get("deviceCode"));
        Object detailProtocolMetadata = invokeGetter(detail, "getProtocolMetadata");
        Object detailTemplateEvidence = invokeGetter(detailProtocolMetadata, "getTemplateEvidence");
        assertEquals(List.of("crack_child_template"), invokeGetter(detailTemplateEvidence, "getTemplateCodes"));
        @SuppressWarnings("unchecked")
        List<Object> executions = (List<Object>) invokeGetter(detailTemplateEvidence, "getExecutions");
        assertEquals(1, executions.size());
        assertEquals("202018143", invokeGetter(executions.get(0), "getChildDeviceCode"));
        assertEquals(Boolean.TRUE, invokeGetter(executions.get(0), "getStatusMirrorApplied"));
    }

    @Test
    void getMessageTraceDetailShouldFallbackToMqttJsonProtocolWhenProtocolMetadataMissing() {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setId(1L);
        logRecord.setTenantId(8L);
        logRecord.setDeviceId(3001L);
        logRecord.setProductId(1001L);
        logRecord.setTraceId("trace-001");
        logRecord.setDeviceCode("demo-device-01");
        logRecord.setProductKey("demo-product");
        logRecord.setMessageType("report");
        logRecord.setTopic("$dp");
        logRecord.setPayload("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(8L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");

        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setDeviceCode("17165802");
        upMessage.setProductKey("demo-product");
        upMessage.setMessageType("property");
        DeviceUpProtocolMetadata protocolMetadata = new DeviceUpProtocolMetadata();
        protocolMetadata.setDecryptedPayloadPreview("{\"17165802\":{\"temperature\":26.5}}");
        protocolMetadata.setDecodedPayloadPreview(Map.of(
                "messageType", "property",
                "deviceCode", "17165802",
                "properties", Map.of("temperature", 26.5)
        ));
        upMessage.setProtocolMetadata(protocolMetadata);

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMessageLogMapper.selectOne(any())).thenReturn(logRecord);
        when(deviceMapper.selectById(3001L)).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(protocolAdapterRegistry.getAdapter("mqtt-json")).thenReturn(protocolAdapter);
        when(protocolAdapter.decode(any(), any(ProtocolContext.class))).thenReturn(upMessage);

        MessageTraceDetailVO detail = deviceMessageService.getMessageTraceDetail(99L, 1L);

        assertEquals("{\"17165802\":{\"temperature\":26.5}}", detail.getDecryptedPayload());
        assertEquals("17165802", detail.getDecodedPayload().get("deviceCode"));
        verify(protocolAdapterRegistry).getAdapter("mqtt-json");
    }

    @Test
    void getMessageTraceDetailShouldFallbackToRawPayloadWhenRecoveryPreparationThrows() {
        DeviceMessageLog logRecord = new DeviceMessageLog();
        logRecord.setId(1L);
        logRecord.setTenantId(8L);
        logRecord.setDeviceId(3001L);
        logRecord.setProductId(1001L);
        logRecord.setTraceId("trace-001");
        logRecord.setDeviceCode("demo-device-01");
        logRecord.setProductKey("demo-product");
        logRecord.setMessageType("report");
        logRecord.setTopic("$dp");
        logRecord.setPayload("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}");

        Device device = new Device();
        device.setId(3001L);
        device.setTenantId(8L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(deviceMessageLogMapper.selectOne(any())).thenReturn(logRecord);
        when(deviceMapper.selectById(3001L)).thenReturn(device);
        when(productMapper.selectById(1001L)).thenThrow(new IllegalStateException("catalog unavailable"));

        MessageTraceDetailVO detail = deviceMessageService.getMessageTraceDetail(99L, 1L);

        assertEquals("{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}", detail.getRawPayload());
        assertEquals(detail.getRawPayload(), detail.getDecryptedPayload());
        assertTrue(detail.getDecodedPayload().containsKey("header"));
        assertTrue(detail.getDecodedPayload().containsKey("bodies"));
    }

    @Test
    void getMessageTraceStatsShouldApplyTenantFilter() {
        List<String> executedSql = new ArrayList<>();
        List<Object[]> executedArgs = new ArrayList<>();

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    executedSql.add(sql);
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 11L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 5L;
                    }
                    if (sql.contains("message_type = ?")) {
                        return 3L;
                    }
                    if (sql.contains("INTERVAL 1 HOUR")) {
                        return 4L;
                    }
                    if (sql.contains("INTERVAL 24 HOUR")) {
                        return 18L;
                    }
                    return 22L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return List.of();
                });

        deviceMessageService.getMessageTraceStats(99L, new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery());

        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("tenant_id")));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains(8L)));
    }

    @Test
    void getMessageTraceStatsShouldApplyOrganizationFilter() {
        List<String> executedSql = new ArrayList<>();
        List<Object[]> executedArgs = new ArrayList<>();

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, 7101L, DataScopeType.ORG, false));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    executedSql.add(sql);
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 11L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 5L;
                    }
                    if (sql.contains("message_type = ?")) {
                        return 3L;
                    }
                    if (sql.contains("INTERVAL 1 HOUR")) {
                        return 4L;
                    }
                    if (sql.contains("INTERVAL 24 HOUR")) {
                        return 18L;
                    }
                    return 22L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return List.of();
                });

        deviceMessageService.getMessageTraceStats(99L, new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery());

        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("tenant_id")));
        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("org_id")));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains(8L)));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains(7101L)));
    }

    @Test
    void getMessageTraceStatsShouldApplyKeywordAndNormalizeLegacyReportMessageType() {
        com.ghlzm.iot.device.dto.DeviceMessageTraceQuery query = new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery();
        query.setKeyword("demo-device-01");
        query.setMessageType("report");
        List<String> executedSql = new ArrayList<>();
        List<Object[]> executedArgs = new ArrayList<>();

        when(permissionService.getDataPermissionContext(99L))
                .thenReturn(new DataPermissionContext(99L, 8L, null, DataScopeType.TENANT, false));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return 1L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    executedSql.add(invocation.getArgument(0, String.class));
                    executedArgs.add(Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length));
                    return List.of();
                });

        deviceMessageService.getMessageTraceStats(99L, query);

        assertTrue(executedSql.stream().allMatch(sql -> sql.contains("(trace_id = ? OR device_code = ? OR product_key = ?)")));
        assertTrue(executedArgs.stream().allMatch(args -> Arrays.asList(args).contains("demo-device-01")));
        assertTrue(executedArgs.stream().anyMatch(args -> Arrays.asList(args).contains("property")));
    }

    @Test
    void handleUpMessageShouldPersistLogAndPropertyAndOnlineStatus() {
        Device device = new Device();
        device.setId(2001L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-01");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel propertyModel = new ProductModel();
        propertyModel.setIdentifier("temperature");
        propertyModel.setModelName("temperature");
        propertyModel.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(propertyModel));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-01",
                Map.of("temperature", 26.5), "property", "/sys/demo-product/demo-device-01/thing/property/post");

        deviceMessageService.handleUpMessage(upMessage);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper).insert(logCaptor.capture());
        assertEquals("/sys/demo-product/demo-device-01/thing/property/post", logCaptor.getValue().getTopic());
        assertEquals("property", logCaptor.getValue().getMessageType());

        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).insert(propertyCaptor.capture());
        assertEquals("temperature", propertyCaptor.getValue().getIdentifier());
        assertEquals("temperature", propertyCaptor.getValue().getPropertyName());
        assertEquals("double", propertyCaptor.getValue().getValueType());
        assertEquals("26.5", propertyCaptor.getValue().getPropertyValue());
        verify(devicePropertyMapper, never()).updateById(any(DeviceProperty.class));
        verify(deviceOnlineSessionService).recordOnlineHeartbeat(any(Device.class), any(LocalDateTime.class));

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper).updateById(deviceCaptor.capture());
        assertEquals(1, deviceCaptor.getValue().getOnlineStatus());
        assertEquals(1, deviceCaptor.getValue().getActivateStatus());

        ArgumentCaptor<DeviceRiskEvaluationEvent> eventCaptor = ArgumentCaptor.forClass(DeviceRiskEvaluationEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("demo-device-01", eventCaptor.getValue().getDeviceCode());
        assertEquals("26.5", String.valueOf(eventCaptor.getValue().getProperties().get("temperature")));
    }

    @Test
    void handleUpMessageShouldUpdateExistingPropertyWhenModelMissing() {
        Device device = new Device();
        device.setId(2002L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-02");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        DeviceProperty existing = new DeviceProperty();
        existing.setId(1L);
        existing.setDeviceId(2002L);
        existing.setIdentifier("humidity");
        existing.setPropertyName("humidity");
        existing.setValueType("integer");
        existing.setCreateTime(LocalDateTime.now().minusDays(1));

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(devicePropertyMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("humidity", 68);
        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-02",
                properties, "property", "/sys/demo-product/demo-device-02/thing/property/post");

        deviceMessageService.handleUpMessage(upMessage);

        verify(devicePropertyMapper, never()).insert(any(DeviceProperty.class));
        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper).updateById(propertyCaptor.capture());
        assertEquals("humidity", propertyCaptor.getValue().getPropertyName());
        assertEquals("int", propertyCaptor.getValue().getValueType());
        assertEquals("68", propertyCaptor.getValue().getPropertyValue());
    }

    @Test
    void handleUpMessageShouldThrowWhenDeviceMissing() {
        when(deviceMapper.selectOne(any())).thenReturn(null);
        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "missing-device",
                Map.of("temperature", 25), "property", "/sys/demo-product/missing-device/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备不存在: missing-device", ex.getMessage());
        verifyNoInteractions(productMapper, productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldThrowWhenProtocolMismatch() {
        Device device = new Device();
        device.setId(2003L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-03");
        device.setProtocolCode("tcp-hex");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("tcp-hex");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-03",
                Map.of("temperature", 25), "property", "/sys/demo-product/demo-device-03/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备协议不匹配: demo-device-03, expected=tcp-hex, actual=mqtt-json", ex.getMessage());
        verify(productMapper).selectById(1001L);
        verifyNoInteractions(productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldThrowWhenDeviceProductUnboundBeforeProtocolValidation() {
        Device device = new Device();
        device.setId(2006L);
        device.setTenantId(1L);
        device.setProductId(null);
        device.setDeviceCode("demo-device-06");
        device.setProtocolCode("");

        when(deviceMapper.selectOne(any())).thenReturn(device);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-06",
                Map.of("temperature", 25), "property", "$dp");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("设备未绑定产品: demo-device-06", ex.getMessage());
        verifyNoInteractions(productMapper, productModelMapper, deviceMessageLogMapper, devicePropertyMapper);
    }

    @Test
    void handleUpMessageShouldFallbackToProductProtocolWhenDeviceProtocolBlank() {
        Device device = new Device();
        device.setId(2007L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-07");
        device.setProtocolCode("");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProtocolCode("mqtt-json");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel propertyModel = new ProductModel();
        propertyModel.setIdentifier("temperature");
        propertyModel.setModelName("temperature");
        propertyModel.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(propertyModel));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-07",
                Map.of("temperature", 26.5), "property", "$dp");

        deviceMessageService.handleUpMessage(upMessage);

        verify(deviceMessageLogMapper).insert(any(DeviceMessageLog.class));
        verify(devicePropertyMapper).insert(any(DeviceProperty.class));
        verify(deviceOnlineSessionService).recordOnlineHeartbeat(any(Device.class), any(LocalDateTime.class));
    }

    @Test
    void recordDispatchFailureTraceShouldNormalizeTrailingJsonJunkBeforePersist() throws Exception {
        byte[] payload = """
                {"header":{"appId":"62000001"},"bodies":{"body":"cipher-text"}}}
                """.getBytes(StandardCharsets.UTF_8);

        deviceMessageService.recordDispatchFailureTrace("$dp", payload, null);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper).insert(logCaptor.capture());
        String storedPayload = logCaptor.getValue().getPayload();
        assertEquals(
                "{\"header\":{\"appId\":\"62000001\"},\"bodies\":{\"body\":\"cipher-text\"}}",
                JsonMapper.builder().findAndAddModules().build().readTree(storedPayload).toString()
        );
    }

    @Test
    void handleUpMessageShouldThrowWhenProductDisabled() {
        Device device = new Device();
        device.setId(2005L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-05");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.DISABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "demo-device-05",
                Map.of("temperature", 25), "property", "/sys/demo-product/demo-device-05/thing/property/post");

        BizException ex = assertThrows(BizException.class, () -> deviceMessageService.handleUpMessage(upMessage));
        assertEquals("产品已停用，拒绝设备接入: demo-product", ex.getMessage());
        verifyNoInteractions(productModelMapper, deviceMessageLogMapper, devicePropertyMapper, deviceFileService);
    }

    @Test
    void handleUpMessageShouldFillCommandStatusWhenReplyArrives() {
        Device device = new Device();
        device.setId(2004L);
        device.setTenantId(1L);
        device.setProductId(1001L);
        device.setDeviceCode("demo-device-04");
        device.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(commandRecordService.markSuccessByCommandId(any(), any(), any())).thenReturn(true);

        DeviceUpMessage upMessage = buildMessage(
                "mqtt-json",
                "demo-product",
                "demo-device-04",
                Map.of(),
                "reply",
                "/sys/demo-product/demo-device-04/thing/property/reply"
        );
        upMessage.setRawPayload("{\"messageId\":\"cmd-001\",\"success\":true}");

        deviceMessageService.handleUpMessage(upMessage);

        verify(commandRecordService).markSuccessByCommandId(any(), any(), any());
        verify(devicePropertyMapper, never()).insert(any(DeviceProperty.class));
        verify(devicePropertyMapper, never()).updateById(any(DeviceProperty.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void handleUpMessageShouldFanOutChildMessagesByDeviceCode() {
        Device baseDevice = new Device();
        baseDevice.setId(3001L);
        baseDevice.setTenantId(1L);
        baseDevice.setProductId(1001L);
        baseDevice.setDeviceCode("SK00FB0D1310195");
        baseDevice.setProtocolCode("mqtt-json");

        Device childDevice1 = new Device();
        childDevice1.setId(3002L);
        childDevice1.setTenantId(1L);
        childDevice1.setProductId(1001L);
        childDevice1.setDeviceCode("84330701");
        childDevice1.setProtocolCode("mqtt-json");

        Device childDevice2 = new Device();
        childDevice2.setId(3003L);
        childDevice2.setTenantId(1L);
        childDevice2.setProductId(1001L);
        childDevice2.setDeviceCode("84330695");
        childDevice2.setProtocolCode("mqtt-json");

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        ProductModel dispsX = new ProductModel();
        dispsX.setIdentifier("dispsX");
        dispsX.setModelName("dispsX");
        dispsX.setDataType("double");

        ProductModel dispsY = new ProductModel();
        dispsY.setIdentifier("dispsY");
        dispsY.setModelName("dispsY");
        dispsY.setDataType("double");

        when(deviceMapper.selectOne(any())).thenReturn(baseDevice, childDevice1, childDevice2);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(productModelMapper.selectList(any())).thenReturn(List.of(dispsX, dispsY));
        when(devicePropertyMapper.selectOne(any())).thenReturn(null);

        DeviceUpMessage upMessage = buildMessage("mqtt-json", "demo-product", "SK00FB0D1310195",
                Map.of(), "property", "$dp");
        upMessage.setRawPayload("""
                {"SK00FB0D1310195":{"L1_SW_1":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0445,"dispsY":0.0293}},"L1_SW_2":{"2026-03-20T06:24:02.000Z":{"dispsX":-0.0293,"dispsY":0.0330}}}}
                """);

        DeviceUpMessage childMessage1 = new DeviceUpMessage();
        childMessage1.setDeviceCode("84330701");
        childMessage1.setTimestamp(upMessage.getTimestamp());
        childMessage1.setProperties(buildProperties(-0.0445, 0.0293));

        DeviceUpMessage childMessage2 = new DeviceUpMessage();
        childMessage2.setDeviceCode("84330695");
        childMessage2.setTimestamp(upMessage.getTimestamp());
        childMessage2.setProperties(buildProperties(-0.0293, 0.0330));

        upMessage.setChildMessages(List.of(childMessage1, childMessage2));

        deviceMessageService.handleUpMessage(upMessage);

        ArgumentCaptor<DeviceMessageLog> logCaptor = ArgumentCaptor.forClass(DeviceMessageLog.class);
        verify(deviceMessageLogMapper, times(3)).insert(logCaptor.capture());
        List<String> loggedDeviceCodes = new ArrayList<>();
        for (DeviceMessageLog logRecord : logCaptor.getAllValues()) {
            loggedDeviceCodes.add(logRecord.getDeviceCode());
        }
        assertEquals(List.of("SK00FB0D1310195", "84330701", "84330695"), loggedDeviceCodes);

        ArgumentCaptor<DeviceProperty> propertyCaptor = ArgumentCaptor.forClass(DeviceProperty.class);
        verify(devicePropertyMapper, times(4)).insert(propertyCaptor.capture());
        List<String> propertyKeys = new ArrayList<>();
        for (DeviceProperty property : propertyCaptor.getAllValues()) {
            propertyKeys.add(property.getDeviceId() + ":" + property.getIdentifier() + "=" + property.getPropertyValue());
        }
        assertEquals(List.of(
                "3002:dispsX=-0.0445",
                "3002:dispsY=0.0293",
                "3003:dispsX=-0.0293",
                "3003:dispsY=0.033"
        ), propertyKeys);

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceMapper, times(3)).updateById(deviceCaptor.capture());
        assertEquals(List.of(3001L, 3002L, 3003L),
                deviceCaptor.getAllValues().stream().map(Device::getId).toList());

        ArgumentCaptor<DeviceRiskEvaluationEvent> eventCaptor = ArgumentCaptor.forClass(DeviceRiskEvaluationEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertEquals(List.of("84330701", "84330695"),
                eventCaptor.getAllValues().stream().map(DeviceRiskEvaluationEvent::getDeviceCode).toList());
    }

    @Test
    void getMessageTraceStatsShouldAggregateRecentSummary() {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("COUNT(DISTINCT trace_id)")) {
                        return 11L;
                    }
                    if (sql.contains("COUNT(DISTINCT device_code)")) {
                        return 5L;
                    }
                    if (sql.contains("message_type = ?")) {
                        return 3L;
                    }
                    if (sql.contains("INTERVAL 1 HOUR")) {
                        return 4L;
                    }
                    if (sql.contains("INTERVAL 24 HOUR")) {
                        return 18L;
                    }
                    return 22L;
                });
        when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeviceStatsBucketVO>>any(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("message_type")) {
                        return List.of(new DeviceStatsBucketVO("property", "property", 12L));
                    }
                    if (sql.contains("product_key")) {
                        return List.of(new DeviceStatsBucketVO("demo-product", "demo-product", 10L));
                    }
                    if (sql.contains("device_code")) {
                        return List.of(new DeviceStatsBucketVO("demo-device-01", "demo-device-01", 8L));
                    }
                    return List.of(new DeviceStatsBucketVO(
                            "/sys/demo-product/demo-device-01/thing/property/post",
                            "/sys/demo-product/demo-device-01/thing/property/post",
                            7L
                    ));
                });

        DeviceMessageTraceStatsVO stats = deviceMessageService.getMessageTraceStats(new com.ghlzm.iot.device.dto.DeviceMessageTraceQuery());

        assertEquals(22L, stats.getTotal());
        assertEquals(4L, stats.getRecentHourCount());
        assertEquals(18L, stats.getRecent24HourCount());
        assertEquals(11L, stats.getDistinctTraceCount());
        assertEquals(5L, stats.getDistinctDeviceCount());
        assertEquals(3L, stats.getDispatchFailureCount());
        assertEquals("property", stats.getTopMessageTypes().get(0).getValue());
        assertEquals("demo-product", stats.getTopProductKeys().get(0).getValue());
        assertEquals("demo-device-01", stats.getTopDeviceCodes().get(0).getValue());
        assertEquals("/sys/demo-product/demo-device-01/thing/property/post", stats.getTopTopics().get(0).getValue());
    }

    private DeviceUpMessage buildMessage(String protocolCode,
                                         String productKey,
                                         String deviceCode,
                                         Map<String, Object> properties,
                                         String messageType,
                                         String topic) {
        DeviceUpMessage upMessage = new DeviceUpMessage();
        upMessage.setProtocolCode(protocolCode);
        upMessage.setProductKey(productKey);
        upMessage.setDeviceCode(deviceCode);
        upMessage.setProperties(properties);
        upMessage.setMessageType(messageType);
        upMessage.setTopic(topic);
        upMessage.setRawPayload("{\"properties\":{}}");
        upMessage.setTimestamp(LocalDateTime.now());
        return upMessage;
    }

    private Map<String, Object> buildProperties(double dispsX, double dispsY) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("dispsX", dispsX);
        properties.put("dispsY", dispsY);
        return properties;
    }

    private Object newInstance(String className) {
        try {
            return Class.forName(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected class " + className + " with a public no-args constructor", ex);
        }
    }

    private void invokeSetter(Object target, String methodName, Object arg) {
        invokeMethod(target, methodName, new Class<?>[]{arg.getClass().getInterfaces().length > 0 ? arg.getClass().getInterfaces()[0] : arg.getClass()}, new Object[]{arg});
    }

    private Object invokeGetter(Object target, String methodName) {
        return invokeMethod(target, methodName, new Class<?>[0], new Object[0]);
    }

    private Object invokeMethod(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method method = findCompatibleMethod(target.getClass(), methodName, args.length);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Expected method " + methodName + " on " + target.getClass().getName(), ex);
        }
    }

    private Method findCompatibleMethod(Class<?> type, String methodName, int argCount) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
