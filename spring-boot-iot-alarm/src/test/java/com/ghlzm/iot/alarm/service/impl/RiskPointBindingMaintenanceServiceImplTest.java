package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskPointBatchBindDeviceRequest;
import com.ghlzm.iot.alarm.dto.RiskPointBindMetricDTO;
import com.ghlzm.iot.alarm.dto.RiskPointBindingRenameRequest;
import com.ghlzm.iot.alarm.dto.RiskPointBindingReplaceRequest;
import com.ghlzm.iot.alarm.dto.RiskPointDeviceCapabilityBindingRequest;
import com.ghlzm.iot.alarm.entity.RiskPoint;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevicePendingPromotion;
import com.ghlzm.iot.alarm.governance.RiskPointGovernanceApprovalExecutor;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceCapabilityBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDevicePendingPromotionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogRebuildService;
import com.ghlzm.iot.alarm.service.RiskPointService;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.alarm.vo.RiskPointBindingDeviceGroupVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingMetricVO;
import com.ghlzm.iot.alarm.vo.RiskPointBindingSummaryVO;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceService;
import com.ghlzm.iot.device.vo.DeviceMetricOptionVO;
import com.ghlzm.iot.system.service.GovernanceApprovalPolicyResolver;
import com.ghlzm.iot.system.service.GovernanceApprovalService;
import com.ghlzm.iot.system.service.GovernanceWorkItemService;
import com.ghlzm.iot.system.vo.GovernanceSubmissionResultVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskPointBindingMaintenanceServiceImplTest {

    @Test
    void submitBindDeviceShouldDirectlyApplyWhenApprovalPolicyMissing() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        GovernanceApprovalPolicyResolver approvalPolicyResolver = mock(GovernanceApprovalPolicyResolver.class);
        GovernanceApprovalService approvalService = mock(GovernanceApprovalService.class);
        GovernanceWorkItemService workItemService = mock(GovernanceWorkItemService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                approvalPolicyResolver,
                approvalService,
                workItemService,
                deviceService
        );
        RiskPointBatchBindDeviceRequest request = batchRequest(
                11L,
                201L,
                "DEV-201",
                "一号设备",
                metric(6101L, "pitch", "倾角"),
                metric(6102L, "AZI", "方位角")
        );
        RiskPointDevice savedPitch = binding(
                9001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedAzi = binding(
                9002L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(2000L)
        );
        when(approvalPolicyResolver.resolveOptionalApproverUserId(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                1001L
        )).thenReturn(null);
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(
                formalOption("pitch", "倾角", 6101L),
                formalOption("AZI", "方位角", 6102L)
        ));
        when(workItemService.openOrRefreshAndGetId(any())).thenReturn(7001L);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), eq(1001L))).thenReturn(savedPitch, savedAzi);

        GovernanceSubmissionResultVO result = service.submitBindDevice(request, 1001L);

        assertEquals(7001L, result.getWorkItemId());
        assertEquals("DIRECT_APPLIED", result.getExecutionStatus());
        verify(riskPointService, times(2)).bindDeviceAndReturn(any(RiskPointDevice.class), eq(1001L));
        verify(approvalService, never()).submitAction(any());
        verify(workItemService).openOrRefreshAndGetId(argThat(command ->
                command != null
                        && "PENDING_RISK_BINDING".equals(command.workItemCode())
                        && RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE.equals(command.actionCode())
                        && command.snapshotJson() != null
                        && command.snapshotJson().contains("\"riskPointId\":11")
                        && command.snapshotJson().contains("\"metricIdentifier\":\"pitch\"")
                        && command.snapshotJson().contains("\"metricIdentifier\":\"AZI\"")
        ));
        verify(workItemService).resolve(
                eq("PENDING_RISK_BINDING"),
                eq(RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE),
                any(Long.class),
                eq(1001L),
                eq("DIRECT_APPLIED")
        );
    }

    @Test
    void submitBindDeviceShouldCreateApprovalOrderInsteadOfWritingWhenApprovalPolicyExists() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        GovernanceApprovalPolicyResolver approvalPolicyResolver = mock(GovernanceApprovalPolicyResolver.class);
        GovernanceApprovalService approvalService = mock(GovernanceApprovalService.class);
        GovernanceWorkItemService workItemService = mock(GovernanceWorkItemService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                approvalPolicyResolver,
                approvalService,
                workItemService,
                deviceService
        );
        RiskPointBatchBindDeviceRequest request = batchRequest(
                11L,
                201L,
                "DEV-201",
                "一号设备",
                metric(6101L, "pitch", "倾角"),
                metric(6102L, "AZI", "方位角")
        );
        when(approvalPolicyResolver.resolveOptionalApproverUserId(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                1001L
        )).thenReturn(2002L);
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(
                formalOption("pitch", "倾角", 6101L),
                formalOption("AZI", "方位角", 6102L)
        ));
        when(workItemService.openOrRefreshAndGetId(any())).thenReturn(7002L);
        when(approvalService.submitAction(any())).thenReturn(9901L);

        GovernanceSubmissionResultVO result = service.submitBindDevice(request, 1001L);

        assertEquals(7002L, result.getWorkItemId());
        assertEquals(9901L, result.getApprovalOrderId());
        assertEquals("PENDING", result.getApprovalStatus());
        assertEquals("PENDING_APPROVAL", result.getExecutionStatus());
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(approvalService).submitAction(argThat(command ->
                command != null
                        && RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE.equals(command.actionCode())
                        && Long.valueOf(7002L).equals(command.workItemId())
                        && Long.valueOf(1001L).equals(command.operatorUserId())
                        && Long.valueOf(2002L).equals(command.approverUserId())
                        && command.payloadJson() != null
                        && command.payloadJson().contains("\"metricIdentifier\":\"pitch\"")
                        && command.payloadJson().contains("\"metricIdentifier\":\"AZI\"")
        ));
    }

    @Test
    void submitBindDeviceCapabilityShouldDirectlyApplyDeviceOnlyBinding() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper = mock(RiskPointDeviceCapabilityBindingMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        GovernanceApprovalPolicyResolver approvalPolicyResolver = mock(GovernanceApprovalPolicyResolver.class);
        GovernanceApprovalService approvalService = mock(GovernanceApprovalService.class);
        GovernanceWorkItemService workItemService = mock(GovernanceWorkItemService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                capabilityBindingMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                approvalPolicyResolver,
                approvalService,
                workItemService,
                deviceService
        );
        RiskPointDeviceCapabilityBindingRequest request = capabilityRequest(11L, 201L, "WARNING");
        RiskPointDeviceCapabilityBinding saved = capabilityBinding(
                9101L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "WARNING",
                null,
                new Date(1000L)
        );
        when(approvalPolicyResolver.resolveOptionalApproverUserId(
                RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE,
                1001L
        )).thenReturn(null);
        when(workItemService.openOrRefreshAndGetId(any())).thenReturn(7003L);
        when(riskPointService.bindDeviceCapabilityAndReturn(any(RiskPointDeviceCapabilityBindingRequest.class), eq(1001L)))
                .thenReturn(saved);

        GovernanceSubmissionResultVO result = service.submitBindDeviceCapability(request, 1001L);

        assertEquals(7003L, result.getWorkItemId());
        assertEquals("DIRECT_APPLIED", result.getExecutionStatus());
        verify(riskPointService).bindDeviceCapabilityAndReturn(any(RiskPointDeviceCapabilityBindingRequest.class), eq(1001L));
        verify(approvalService, never()).submitAction(any());
        verify(workItemService).openOrRefreshAndGetId(argThat(command ->
                command != null
                        && RiskPointGovernanceApprovalExecutor.ACTION_RISK_POINT_BIND_DEVICE.equals(command.actionCode())
                        && command.snapshotJson() != null
                        && command.snapshotJson().contains("\"bindingMode\":\"DEVICE_ONLY\"")
                        && command.snapshotJson().contains("\"deviceCapabilityType\":\"WARNING\"")
        ));
    }

    @Test
    void listBindingSummariesShouldAggregateFormalBindingsAndPendingCounts() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper = mock(RiskPointDeviceCapabilityBindingMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                capabilityBindingMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointService.getById(12L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(100L, 11L, 201L, "DEV-201", "一号设备", "metric_a", "测点A", new Date(1000L)),
                binding(101L, 11L, 201L, "DEV-201", "一号设备", "metric_b", "测点B", new Date(2000L)),
                binding(102L, 11L, 202L, "DEV-202", "二号设备", "metric_c", "测点C", new Date(3000L)),
                binding(103L, 12L, 301L, "DEV-301", "三号设备", "metric_x", "测点X", new Date(4000L))
        ));
        when(capabilityBindingMapper.selectList(any())).thenReturn(List.of(
                capabilityBinding(201L, 11L, 203L, "DEV-203", "三号预警设备", "WARNING", null, new Date(3500L))
        ));
        when(pendingBindingMapper.selectList(any())).thenReturn(List.of(
                pending(11L, "PENDING_METRIC_GOVERNANCE"),
                pending(11L, "PARTIALLY_PROMOTED"),
                pending(11L, "IGNORED"),
                pending(12L, "PARTIALLY_PROMOTED")
        ));

        List<RiskPointBindingSummaryVO> result = service.listBindingSummaries(Arrays.asList(11L, null, 11L, 12L), 1001L);

        assertEquals(2, result.size());
        assertEquals(11L, result.get(0).getRiskPointId());
        assertEquals(3, result.get(0).getBoundDeviceCount());
        assertEquals(3, result.get(0).getBoundMetricCount());
        assertEquals(2, result.get(0).getPendingBindingCount());
        assertEquals(12L, result.get(1).getRiskPointId());
        assertEquals(1, result.get(1).getBoundDeviceCount());
        assertEquals(1, result.get(1).getBoundMetricCount());
        assertEquals(1, result.get(1).getPendingBindingCount());
        verify(riskPointService, times(1)).getById(11L, 1001L);
        verify(riskPointService, times(1)).getById(12L, 1001L);
    }

    @Test
    void listBindingGroupsShouldGroupMetricsByDeviceAndMarkPromotionSource() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper = mock(RiskPointDeviceCapabilityBindingMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                capabilityBindingMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(501L, 11L, 9202L, "DEV-B", "B设备", "z_metric", "Z测点", new Date(3000L)),
                binding(502L, 11L, 9201L, "DEV-A", "A设备", "b_metric", "B测点", new Date(2000L)),
                binding(503L, 11L, 9201L, "DEV-A", "A设备", "a_metric", "A测点", new Date(1000L))
        ));
        when(capabilityBindingMapper.selectList(any())).thenReturn(List.of(
                capabilityBinding(601L, 11L, 9301L, "DEV-C", "C视频设备", "VIDEO", "AI_EVENT_RESERVED", new Date(3500L))
        ));
        when(pendingPromotionMapper.selectList(any())).thenReturn(List.of(
                pendingPromotion(501L, "SUCCESS"),
                pendingPromotion(9999L)
        ));

        List<RiskPointBindingDeviceGroupVO> result = service.listBindingGroups(11L, 1001L);

        assertEquals(3, result.size());
        assertEquals(9201L, result.get(0).getDeviceId());
        assertEquals("DEV-A", result.get(0).getDeviceCode());
        assertEquals("METRIC", result.get(0).getBindingMode());
        assertEquals(2, result.get(0).getMetricCount());
        assertEquals("a_metric", result.get(0).getMetrics().get(0).getMetricIdentifier());
        assertEquals("MANUAL", result.get(0).getMetrics().get(0).getBindingSource());
        assertEquals("b_metric", result.get(0).getMetrics().get(1).getMetricIdentifier());
        assertEquals("MANUAL", result.get(0).getMetrics().get(1).getBindingSource());
        assertEquals(9202L, result.get(1).getDeviceId());
        assertEquals("DEV-B", result.get(1).getDeviceCode());
        assertEquals("METRIC", result.get(1).getBindingMode());
        assertEquals(1, result.get(1).getMetricCount());
        assertEquals("z_metric", result.get(1).getMetrics().get(0).getMetricIdentifier());
        assertEquals("PENDING_PROMOTION", result.get(1).getMetrics().get(0).getBindingSource());
        assertEquals(9301L, result.get(2).getDeviceId());
        assertEquals("DEVICE_ONLY", result.get(2).getBindingMode());
        assertEquals("VIDEO", result.get(2).getDeviceCapabilityType());
        assertEquals(0, result.get(2).getMetricCount());
        assertEquals(true, result.get(2).getAiEventExpandable());
        verify(riskPointService).getById(11L, 1001L);
    }

    @Test
    void listBindingGroupsShouldKeepManualWhenPromotionHistoryOnlyHasDuplicateSkipped() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(701L, 11L, 9301L, "DEV-MANUAL", "人工绑定设备", "dispsX", "X轴位移", new Date(1000L))
        ));
        when(pendingPromotionMapper.selectList(any())).thenReturn(List.of(
                pendingPromotion(701L, "DUPLICATE_SKIPPED")
        ));

        List<RiskPointBindingDeviceGroupVO> result = service.listBindingGroups(11L, 1001L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getMetricCount());
        assertEquals("MANUAL", result.get(0).getMetrics().get(0).getBindingSource());
    }

    @Test
    void listFormalBindingMetricOptionsShouldReturnOnlyPublishedCatalogMetrics() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );

        DeviceMetricOptionVO formalMetric = new DeviceMetricOptionVO();
        formalMetric.setIdentifier("value");
        formalMetric.setName("激光测距值");
        formalMetric.setDataType("double");
        formalMetric.setRiskMetricId(6101L);

        DeviceMetricOptionVO governanceOnlyMetric = new DeviceMetricOptionVO();
        governanceOnlyMetric.setIdentifier("sensor_state");
        governanceOnlyMetric.setName("传感器状态");
        governanceOnlyMetric.setDataType("int");

        when(deviceService.listMetricOptions(1001L, 3001L)).thenReturn(List.of(formalMetric, governanceOnlyMetric));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3001L, 1001L);

        assertEquals(List.of("value"), result.stream().map(DeviceMetricOptionVO::getIdentifier).toList());
        assertEquals(List.of(6101L), result.stream().map(DeviceMetricOptionVO::getRiskMetricId).toList());
    }

    @Test
    void listFormalBindingMetricOptionsShouldBackfillMissingMonitoringCatalogBeforeFiltering() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        service.setRiskMetricCatalogBackfillDependencies(
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule(),
                rebuildService
        );
        Device device = new Device();
        device.setId(3001L);
        device.setProductId(2002L);
        device.setDeviceName("CXH15522812 - 多维检测仪");
        when(deviceService.getRequiredById(3001L)).thenReturn(device);
        when(rebuildService.rebuildLatestRelease(2002L)).thenReturn(true);

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("zhd-monitor-multi-displacement-v1");
        product.setProductName("中海达多维位移产品");
        product.setMetadataJson(measureMetadata("L1_LF_1.value"));
        when(productMapper.selectById(2002L)).thenReturn(product);

        ProductModel crackValue = productModel(4101L, 2002L, "L1_LF_1.value", "裂缝量");
        ProductModel tiltAngle = productModel(4102L, 2002L, "L1_QJ_1.angle", "水平面夹角");
        when(productModelMapper.selectList(any())).thenReturn(List.of(crackValue, tiltAngle));

        when(deviceService.listMetricOptions(1001L, 3001L))
                .thenReturn(List.of(formalOption("L1_LF_1.value", "裂缝量", null)))
                .thenReturn(List.of(
                        formalOption("L1_LF_1.value", "裂缝量", 9101L),
                        formalOption("L1_QJ_1.angle", "水平面夹角", null)
                ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3001L, 1001L);

        assertEquals(List.of("L1_LF_1.value"), result.stream().map(DeviceMetricOptionVO::getIdentifier).toList());
        assertEquals(List.of(9101L), result.stream().map(DeviceMetricOptionVO::getRiskMetricId).toList());
        verify(rebuildService).rebuildLatestRelease(2002L);
    }

    @Test
    void listFormalBindingMetricOptionsShouldBackfillGnssTotalsForFullPathMonitoringProducts() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        service.setRiskMetricCatalogBackfillDependencies(
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule(),
                rebuildService
        );
        Device device = new Device();
        device.setId(3003L);
        device.setProductId(2004L);
        device.setDeviceName("NF-GNSS-01 - GNSS位移监测仪");
        when(deviceService.getRequiredById(3003L)).thenReturn(device);
        when(rebuildService.rebuildLatestRelease(2004L)).thenReturn(true);

        Product product = new Product();
        product.setId(2004L);
        product.setProductKey("nf-monitor-gnss-monitor-v1");
        product.setProductName("南方测绘 监测型 GNSS位移监测仪");
        product.setMetadataJson(measureMetadata(
                "L1_GP_1.gpsTotalX",
                "L1_GP_1.gpsTotalY",
                "L1_GP_1.gpsTotalZ"
        ));
        when(productMapper.selectById(2004L)).thenReturn(product);

        ProductModel gpsTotalX = productModel(4301L, 2004L, "L1_GP_1.gpsTotalX", "X方向累计变形量");
        ProductModel gpsTotalY = productModel(4302L, 2004L, "L1_GP_1.gpsTotalY", "Y方向累计变形量");
        ProductModel gpsTotalZ = productModel(4303L, 2004L, "L1_GP_1.gpsTotalZ", "Z方向累计变形量");
        ProductModel accelX = productModel(4304L, 2004L, "L1_JS_1.gX", "X轴加速度");
        ProductModel tiltAngle = productModel(4305L, 2004L, "L1_QJ_1.angle", "水平面夹角");
        when(productModelMapper.selectList(any())).thenReturn(List.of(gpsTotalX, gpsTotalY, gpsTotalZ, accelX, tiltAngle));

        when(deviceService.listMetricOptions(1001L, 3003L))
                .thenReturn(List.of(
                        formalOption("L1_GP_1.gpsTotalX", "X方向累计变形量", null),
                        formalOption("L1_GP_1.gpsTotalY", "Y方向累计变形量", null),
                        formalOption("L1_GP_1.gpsTotalZ", "Z方向累计变形量", null),
                        formalOption("L1_JS_1.gX", "X轴加速度", null),
                        formalOption("L1_QJ_1.angle", "水平面夹角", null)
                ))
                .thenReturn(List.of(
                        formalOption("L1_GP_1.gpsTotalX", "X方向累计变形量", 9201L),
                        formalOption("L1_GP_1.gpsTotalY", "Y方向累计变形量", 9202L),
                        formalOption("L1_GP_1.gpsTotalZ", "Z方向累计变形量", 9203L),
                        formalOption("L1_JS_1.gX", "X轴加速度", null),
                        formalOption("L1_QJ_1.angle", "水平面夹角", null)
                ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3003L, 1001L);

        assertEquals(
                List.of("L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalZ"),
                result.stream().map(DeviceMetricOptionVO::getIdentifier).toList()
        );
        assertEquals(List.of(9201L, 9202L, 9203L), result.stream().map(DeviceMetricOptionVO::getRiskMetricId).toList());
        verify(rebuildService).rebuildLatestRelease(2004L);
    }

    @Test
    void listFormalBindingMetricOptionsShouldRebuildWhenCatalogIsPartiallyStale() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskMetricCatalogRebuildService rebuildService = mock(RiskMetricCatalogRebuildService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        service.setRiskMetricCatalogBackfillDependencies(
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule(),
                rebuildService
        );
        Device device = new Device();
        device.setId(3006L);
        device.setProductId(2002L);
        device.setDeviceName("CXH15522812 - 多维检测仪");
        when(deviceService.getRequiredById(3006L)).thenReturn(device);
        when(rebuildService.rebuildLatestRelease(2002L)).thenReturn(true);

        Product product = new Product();
        product.setId(2002L);
        product.setProductKey("zhd-monitor-multi-displacement-v1");
        product.setProductName("中海达多维位移产品");
        product.setMetadataJson(measureMetadata(
                "L1_LF_1.value",
                "L1_QJ_1.X",
                "L1_QJ_1.Y",
                "L1_QJ_1.Z",
                "L1_JS_1.gX",
                "L1_JS_1.gY",
                "L1_JS_1.gZ"
        ));
        when(productMapper.selectById(2002L)).thenReturn(product);

        ProductModel crackValue = productModel(4601L, 2002L, "L1_LF_1.value", "裂缝量");
        ProductModel tiltX = productModel(4602L, 2002L, "L1_QJ_1.X", "X轴倾角");
        ProductModel tiltY = productModel(4603L, 2002L, "L1_QJ_1.Y", "Y轴倾角");
        ProductModel tiltZ = productModel(4604L, 2002L, "L1_QJ_1.Z", "Z轴倾角");
        ProductModel accelX = productModel(4605L, 2002L, "L1_JS_1.gX", "X轴加速度");
        ProductModel accelY = productModel(4606L, 2002L, "L1_JS_1.gY", "Y轴加速度");
        ProductModel accelZ = productModel(4607L, 2002L, "L1_JS_1.gZ", "Z轴加速度");
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                crackValue,
                tiltX,
                tiltY,
                tiltZ,
                accelX,
                accelY,
                accelZ
        ));

        when(deviceService.listMetricOptions(1001L, 3006L))
                .thenReturn(List.of(
                        formalOption("L1_LF_1.value", "裂缝量", 9301L),
                        formalOption("L1_QJ_1.X", "X轴倾角", null),
                        formalOption("L1_QJ_1.Y", "Y轴倾角", null),
                        formalOption("L1_QJ_1.Z", "Z轴倾角", null),
                        formalOption("L1_JS_1.gX", "X轴加速度", null),
                        formalOption("L1_JS_1.gY", "Y轴加速度", null),
                        formalOption("L1_JS_1.gZ", "Z轴加速度", null)
                ))
                .thenReturn(List.of(
                        formalOption("L1_LF_1.value", "裂缝量", 9301L),
                        formalOption("L1_QJ_1.X", "X轴倾角", 9302L),
                        formalOption("L1_QJ_1.Y", "Y轴倾角", 9303L),
                        formalOption("L1_QJ_1.Z", "Z轴倾角", 9304L),
                        formalOption("L1_JS_1.gX", "X轴加速度", 9305L),
                        formalOption("L1_JS_1.gY", "Y轴加速度", 9306L),
                        formalOption("L1_JS_1.gZ", "Z轴加速度", 9307L)
                ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3006L, 1001L);

        assertEquals(
                List.of(
                        "L1_JS_1.gX",
                        "L1_JS_1.gY",
                        "L1_JS_1.gZ",
                        "L1_LF_1.value",
                        "L1_QJ_1.X",
                        "L1_QJ_1.Y",
                        "L1_QJ_1.Z"
                ),
                result.stream().map(DeviceMetricOptionVO::getIdentifier).toList()
        );
        assertEquals(
                List.of(9305L, 9306L, 9307L, 9301L, 9302L, 9303L, 9304L),
                result.stream().map(DeviceMetricOptionVO::getRiskMetricId).toList()
        );
        verify(rebuildService).rebuildLatestRelease(2002L);
    }

    @Test
    void listFormalBindingMetricOptionsShouldNotBackfillBareCrackChannelWithoutLeafIntoFormalCatalog() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        Device device = new Device();
        device.setId(3004L);
        device.setProductId(2005L);
        device.setDeviceName("NF-CRACK-01 - 裂缝计");
        when(deviceService.getRequiredById(3004L)).thenReturn(device);

        Product product = new Product();
        product.setId(2005L);
        product.setProductKey("nf-monitor-crack-meter-v1");
        product.setProductName("南方测绘 监测型 裂缝计");
        when(productMapper.selectById(2005L)).thenReturn(product);

        ProductModel crackChannel = productModel(4401L, 2005L, "L1_LF_1", "裂缝值");
        ProductModel sensorState = productModel(4402L, 2005L, "S1_ZT_1.sensor_state.L1_LF_1", "裂缝计状态");
        when(productModelMapper.selectList(any())).thenReturn(List.of(crackChannel, sensorState));
        when(deviceService.listMetricOptions(1001L, 3004L)).thenReturn(List.of(
                formalOption("L1_LF_1", "裂缝值", null),
                formalOption("S1_ZT_1.sensor_state.L1_LF_1", "裂缝计状态", null)
        ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3004L, 1001L);

        assertEquals(List.of(), result);
        verify(riskMetricCatalogService, never()).publishFromReleasedContracts(any(), any(), any(), any());
        verify(deviceService, times(1)).listMetricOptions(1001L, 3004L);
    }

    @Test
    void listFormalBindingMetricOptionsShouldNotBackfillMudLevelPlaceholderIntoFormalCatalog() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        Device device = new Device();
        device.setId(3005L);
        device.setProductId(2006L);
        device.setDeviceName("NF-MUD-01 - 泥位计");
        when(deviceService.getRequiredById(3005L)).thenReturn(device);

        Product product = new Product();
        product.setId(2006L);
        product.setProductKey("nf-monitor-mud-level-meter-v1");
        product.setProductName("南方测绘 监测型 泥位计");
        when(productMapper.selectById(2006L)).thenReturn(product);

        ProductModel mudLevel = productModel(4501L, 2006L, "L4_NW_1", "泥位高程值");
        ProductModel sensorState = productModel(4502L, 2006L, "S1_ZT_1.sensor_state.L4_NW_1", "传感器状态");
        when(productModelMapper.selectList(any())).thenReturn(List.of(mudLevel, sensorState));
        when(deviceService.listMetricOptions(1001L, 3005L)).thenReturn(List.of(
                formalOption("L4_NW_1", "泥位高程值", null),
                formalOption("S1_ZT_1.sensor_state.L4_NW_1", "传感器状态", null)
        ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3005L, 1001L);

        assertEquals(List.of(), result);
        verify(riskMetricCatalogService, never()).publishFromReleasedContracts(any(), any(), any(), any());
        verify(deviceService, times(1)).listMetricOptions(1001L, 3005L);
    }

    @Test
    void listFormalBindingMetricOptionsShouldNotBackfillBaseStationMetricsIntoFormalCatalog() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        ProductModelMapper productModelMapper = mock(ProductModelMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        RiskMetricCatalogService riskMetricCatalogService = mock(RiskMetricCatalogService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                null,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService,
                productModelMapper,
                productMapper,
                riskMetricCatalogService,
                new DefaultRiskMetricCatalogPublishRule()
        );
        Device device = new Device();
        device.setId(3002L);
        device.setProductId(2003L);
        device.setDeviceName("SJ11F2148734260A - GNSS基准站");
        when(deviceService.getRequiredById(3002L)).thenReturn(device);

        Product product = new Product();
        product.setId(2003L);
        product.setProductKey("nf-monitor-gnss-base-station-v1");
        product.setProductName("南方测绘 监测型 GNSS基准站");
        when(productMapper.selectById(2003L)).thenReturn(product);

        ProductModel gpsTotalX = productModel(4201L, 2003L, "L1_GP_1.gpsTotalX", "GNSS累计位移 X");
        ProductModel gpsTotalY = productModel(4202L, 2003L, "L1_GP_1.gpsTotalY", "GNSS累计位移 Y");
        ProductModel gpsTotalZ = productModel(4203L, 2003L, "L1_GP_1.gpsTotalZ", "GNSS累计位移 Z");
        when(productModelMapper.selectList(any())).thenReturn(List.of(gpsTotalX, gpsTotalY, gpsTotalZ));
        when(deviceService.listMetricOptions(1001L, 3002L)).thenReturn(List.of(
                formalOption("L1_GP_1.gpsTotalX", "GNSS累计位移 X", null),
                formalOption("L1_GP_1.gpsTotalY", "GNSS累计位移 Y", null),
                formalOption("L1_GP_1.gpsTotalZ", "GNSS累计位移 Z", null)
        ));

        List<DeviceMetricOptionVO> result = service.listFormalBindingMetricOptions(3002L, 1001L);

        assertEquals(List.of(), result);
        verify(riskMetricCatalogService, never()).publishFromReleasedContracts(any(), any(), any(), any());
        verify(deviceService, times(1)).listMetricOptions(1001L, 3002L);
    }

    @Test
    void submitBindDeviceShouldRejectMetricOutsideFormalCatalog() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        GovernanceWorkItemService workItemService = mock(GovernanceWorkItemService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                workItemService,
                deviceService
        );

        RiskPointBatchBindDeviceRequest request = batchRequest(
                11L,
                201L,
                "DEV-201",
                "一号设备",
                metric(null, "sensor_state", "传感器状态")
        );
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("value", "激光测距值", 6101L)));

        BizException error = assertThrows(BizException.class, () -> service.submitBindDevice(request, 1001L));

        assertEquals("当前测点未发布到风险指标目录，不能用于正式绑定", error.getMessage());
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(workItemService, never()).openOrRefreshAndGetId(any());
    }

    @Test
    void submitBindDeviceShouldRejectDuplicateMetricWithinBatch() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointBatchBindDeviceRequest request = batchRequest(
                11L,
                201L,
                "DEV-201",
                "一号设备",
                metric(6101L, " pitch ", "倾角"),
                metric(6101L, "pitch", "倾角")
        );
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("pitch", "倾角", 6101L)));

        BizException error = assertThrows(BizException.class, () -> service.submitBindDevice(request, 1001L));

        assertEquals("请勿重复选择测点", error.getMessage());
        verify(deviceService).listMetricOptions(1001L, 201L);
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
    }

    @Test
    void replaceBindingMetricShouldRejectMetricOutsideFormalCatalog() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );

        RiskPointDevice existing = binding(3001L, 11L, 201L, "DEV-201", "一号设备", "value", "激光测距值", new Date());
        when(riskPointDeviceMapper.selectById(3001L)).thenReturn(existing);
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("value", "激光测距值", 6101L)));
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());

        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("sensor_state");
        request.setMetricName("传感器状态");

        BizException error = assertThrows(BizException.class, () -> service.replaceBindingMetric(3001L, request, 1001L));

        assertEquals("当前测点未发布到风险指标目录，不能用于正式绑定", error.getMessage());
        verify(riskPointDeviceMapper, never()).deleteById(any());
    }

    @Test
    void removeBindingShouldDeleteOnlyTargetMetricBinding() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper
        );
        RiskPointDevice targetBinding = binding(
                2001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        when(riskPointDeviceMapper.selectById(2001L)).thenReturn(targetBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(riskPointDeviceMapper.deleteById(2001L)).thenReturn(1);

        service.removeBinding(2001L, 1001L);

        verify(riskPointDeviceMapper).deleteById(2001L);
        verify(riskPointService, never()).unbindDevice(11L, 201L, 1001L);
    }

    @Test
    void renameBindingMetricShouldOnlyUpdateMetricNameAndAuditFields() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                null
        );
        RiskPointDevice oldBinding = binding(
                2001L,
                11L,
                201L,
                "DEV-201",
                "北坡一体机",
                "gX",
                "X轴加速度",
                new Date(1000L)
        );
        oldBinding.setRiskMetricId(6101L);
        when(riskPointDeviceMapper.selectById(2001L)).thenReturn(oldBinding);
        when(riskPointDeviceMapper.updateById(any(RiskPointDevice.class))).thenReturn(1);
        RiskPointBindingRenameRequest request = new RiskPointBindingRenameRequest();
        request.setMetricName("北坡 X 轴加速度");

        RiskPointBindingMetricVO result = service.renameBindingMetric(2001L, request, 1001L);

        assertEquals(2001L, result.getBindingId());
        assertEquals(6101L, result.getRiskMetricId());
        assertEquals("gX", result.getMetricIdentifier());
        assertEquals("北坡 X 轴加速度", result.getMetricName());
        verify(riskPointService).getById(11L, 1001L);
        ArgumentCaptor<RiskPointDevice> bindingCaptor = ArgumentCaptor.forClass(RiskPointDevice.class);
        verify(riskPointDeviceMapper).updateById(bindingCaptor.capture());
        assertEquals(2001L, bindingCaptor.getValue().getId());
        assertEquals("北坡 X 轴加速度", bindingCaptor.getValue().getMetricName());
        assertEquals(1001L, bindingCaptor.getValue().getUpdateBy());
        assertNotNull(bindingCaptor.getValue().getUpdateTime());
        assertEquals("gX", oldBinding.getMetricIdentifier());
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(riskPointDeviceMapper, never()).deleteById(any());
    }

    @Test
    void renameBindingMetricShouldRejectBlankMetricName() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                null
        );
        RiskPointBindingRenameRequest request = new RiskPointBindingRenameRequest();
        request.setMetricName("   ");

        assertThrows(BizException.class, () -> service.renameBindingMetric(2001L, request, 1001L));

        verify(riskPointDeviceMapper, never()).selectById(any());
        verify(riskPointDeviceMapper, never()).updateById(any(RiskPointDevice.class));
    }

    @Test
    void replaceBindingMetricShouldCreateNewBindingAndDeleteOldBinding() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointDevice oldBinding = binding(
                3001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                3999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(3001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("AZI", "方位角", 6102L)));
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(3001L)).thenReturn(1);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        RiskPointBindingMetricVO result = service.replaceBindingMetric(3001L, request, 1001L);

        assertEquals(3999L, result.getBindingId());
        assertEquals("AZI", result.getMetricIdentifier());
        verify(riskPointDeviceMapper).deleteById(3001L);
        ArgumentCaptor<RiskPointDevice> bindingCaptor = ArgumentCaptor.forClass(RiskPointDevice.class);
        verify(riskPointService).bindDeviceAndReturn(bindingCaptor.capture(), any());
        assertEquals(11L, bindingCaptor.getValue().getRiskPointId());
        assertEquals(201L, bindingCaptor.getValue().getDeviceId());
        assertEquals("DEV-201", bindingCaptor.getValue().getDeviceCode());
        assertEquals("一号设备", bindingCaptor.getValue().getDeviceName());
        assertEquals("AZI", bindingCaptor.getValue().getMetricIdentifier());
        assertEquals("方位角", bindingCaptor.getValue().getMetricName());
    }

    @Test
    void replaceBindingMetricShouldFallbackMetricNameToIdentifierWhenBlank() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointDevice oldBinding = binding(
                4001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                4999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "AZI",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(4001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("AZI", "AZI", 6102L)));
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(4001L)).thenReturn(1);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier(" AZI ");
        request.setMetricName("   ");

        RiskPointBindingMetricVO result = service.replaceBindingMetric(4001L, request, 1001L);

        assertEquals(4999L, result.getBindingId());
        ArgumentCaptor<RiskPointDevice> bindingCaptor = ArgumentCaptor.forClass(RiskPointDevice.class);
        verify(riskPointService).bindDeviceAndReturn(bindingCaptor.capture(), any());
        assertEquals("AZI", bindingCaptor.getValue().getMetricIdentifier());
        assertEquals("AZI", bindingCaptor.getValue().getMetricName());
    }

    @Test
    void replaceBindingMetricShouldRejectWhenReplacingWithSameMetric() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointDevice oldBinding = binding(
                5001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(1000L)
        );
        when(riskPointDeviceMapper.selectById(5001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("AZI", "方位角", 6102L)));
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier(" AZI ");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(5001L, request, 1001L));

        verify(riskPointDeviceMapper, never()).selectOne(any());
        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
    }

    @Test
    void replaceBindingMetricShouldRejectWhenTargetMetricAlreadyBound() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointDevice oldBinding = binding(
                6001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice duplicateBinding = binding(
                6002L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(1200L)
        );
        when(riskPointDeviceMapper.selectById(6001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("AZI", "方位角", 6102L)));
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(duplicateBinding);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(6001L, request, 1001L));

        verify(riskPointService, never()).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(riskPointDeviceMapper, never()).deleteById(6001L);
    }

    @Test
    void replaceBindingMetricShouldThrowWhenDeleteOldBindingFailsAfterNewBindingCreated() {
        RiskPointService riskPointService = mock(RiskPointService.class);
        RiskPointDeviceMapper riskPointDeviceMapper = mock(RiskPointDeviceMapper.class);
        RiskPointDevicePendingBindingMapper pendingBindingMapper = mock(RiskPointDevicePendingBindingMapper.class);
        RiskPointDevicePendingPromotionMapper pendingPromotionMapper = mock(RiskPointDevicePendingPromotionMapper.class);
        DeviceService deviceService = mock(DeviceService.class);
        RiskPointBindingMaintenanceServiceImpl service = new RiskPointBindingMaintenanceServiceImpl(
                riskPointService,
                riskPointDeviceMapper,
                pendingBindingMapper,
                pendingPromotionMapper,
                null,
                null,
                null,
                deviceService
        );
        RiskPointDevice oldBinding = binding(
                7001L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "pitch",
                "倾角",
                new Date(1000L)
        );
        RiskPointDevice savedBinding = binding(
                7999L,
                11L,
                201L,
                "DEV-201",
                "一号设备",
                "AZI",
                "方位角",
                new Date(2000L)
        );
        when(riskPointDeviceMapper.selectById(7001L)).thenReturn(oldBinding);
        when(riskPointService.getById(11L, 1001L)).thenReturn(new RiskPoint());
        when(deviceService.listMetricOptions(1001L, 201L)).thenReturn(List.of(formalOption("AZI", "方位角", 6102L)));
        when(riskPointDeviceMapper.selectOne(any())).thenReturn(null);
        when(riskPointService.bindDeviceAndReturn(any(RiskPointDevice.class), any())).thenReturn(savedBinding);
        when(riskPointDeviceMapper.deleteById(7001L)).thenReturn(0);
        RiskPointBindingReplaceRequest request = new RiskPointBindingReplaceRequest();
        request.setMetricIdentifier("AZI");
        request.setMetricName("方位角");

        assertThrows(BizException.class, () -> service.replaceBindingMetric(7001L, request, 1001L));

        verify(riskPointService).bindDeviceAndReturn(any(RiskPointDevice.class), any());
        verify(riskPointDeviceMapper).deleteById(7001L);
    }

    private RiskPointDevice binding(Long id,
                                    Long riskPointId,
                                    Long deviceId,
                                    String deviceCode,
                                    String deviceName,
                                    String metricIdentifier,
                                    String metricName,
                                    Date createTime) {
        RiskPointDevice value = new RiskPointDevice();
        value.setId(id);
        value.setRiskPointId(riskPointId);
        value.setDeviceId(deviceId);
        value.setDeviceCode(deviceCode);
        value.setDeviceName(deviceName);
        value.setMetricIdentifier(metricIdentifier);
        value.setMetricName(metricName);
        value.setCreateTime(createTime);
        value.setDeleted(0);
        return value;
    }

    private RiskPointBatchBindDeviceRequest batchRequest(Long riskPointId,
                                                         Long deviceId,
                                                         String deviceCode,
                                                         String deviceName,
                                                         RiskPointBindMetricDTO... metrics) {
        RiskPointBatchBindDeviceRequest request = new RiskPointBatchBindDeviceRequest();
        request.setRiskPointId(riskPointId);
        request.setDeviceId(deviceId);
        request.setDeviceCode(deviceCode);
        request.setDeviceName(deviceName);
        request.setMetrics(metrics == null ? List.of() : List.of(metrics));
        return request;
    }

    private RiskPointBindMetricDTO metric(Long riskMetricId, String metricIdentifier, String metricName) {
        RiskPointBindMetricDTO value = new RiskPointBindMetricDTO();
        value.setRiskMetricId(riskMetricId);
        value.setMetricIdentifier(metricIdentifier);
        value.setMetricName(metricName);
        return value;
    }

    private RiskPointDevicePendingBinding pending(Long riskPointId, String status) {
        RiskPointDevicePendingBinding value = new RiskPointDevicePendingBinding();
        value.setRiskPointId(riskPointId);
        value.setResolutionStatus(status);
        value.setDeleted(0);
        return value;
    }

    private RiskPointDevicePendingPromotion pendingPromotion(Long riskPointDeviceId) {
        return pendingPromotion(riskPointDeviceId, null);
    }

    private RiskPointDevicePendingPromotion pendingPromotion(Long riskPointDeviceId, String promotionStatus) {
        RiskPointDevicePendingPromotion value = new RiskPointDevicePendingPromotion();
        value.setRiskPointDeviceId(riskPointDeviceId);
        value.setPromotionStatus(promotionStatus);
        value.setDeleted(0);
        return value;
    }

    private ProductModel productModel(Long id, Long productId, String identifier, String modelName) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(productId);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(modelName);
        model.setDataType("double");
        model.setDeleted(0);
        return model;
    }

    private DeviceMetricOptionVO formalOption(String identifier, String name, Long riskMetricId) {
        DeviceMetricOptionVO option = new DeviceMetricOptionVO();
        option.setIdentifier(identifier);
        option.setName(name);
        option.setDataType("double");
        option.setRiskMetricId(riskMetricId);
        return option;
    }

    private String measureMetadata(String... identifiers) {
        String metricEntries = identifiers == null ? "" : Arrays.stream(identifiers)
                .filter(Objects::nonNull)
                .map(identifier -> """
                        {
                          "identifier":"%s",
                          "displayName":"%s",
                          "group":"measure",
                          "enabled":true,
                          "includeInTrend":true
                        }
                        """.formatted(identifier, identifier))
                .collect(Collectors.joining(","));
        return """
                {
                  "objectInsight": {
                    "customMetrics": [%s]
                  }
                }
                """.formatted(metricEntries);
    }

    private RiskPointDeviceCapabilityBindingRequest capabilityRequest(Long riskPointId, Long deviceId, String deviceCapabilityType) {
        RiskPointDeviceCapabilityBindingRequest request = new RiskPointDeviceCapabilityBindingRequest();
        request.setRiskPointId(riskPointId);
        request.setDeviceId(deviceId);
        request.setDeviceCapabilityType(deviceCapabilityType);
        return request;
    }

    private RiskPointDeviceCapabilityBinding capabilityBinding(Long id,
                                                              Long riskPointId,
                                                              Long deviceId,
                                                              String deviceCode,
                                                              String deviceName,
                                                              String deviceCapabilityType,
                                                              String extensionStatus,
                                                              Date createTime) {
        RiskPointDeviceCapabilityBinding value = new RiskPointDeviceCapabilityBinding();
        value.setId(id);
        value.setRiskPointId(riskPointId);
        value.setDeviceId(deviceId);
        value.setDeviceCode(deviceCode);
        value.setDeviceName(deviceName);
        value.setDeviceCapabilityType(deviceCapabilityType);
        value.setExtensionStatus(extensionStatus);
        value.setCreateTime(createTime);
        value.setDeleted(0);
        return value;
    }
}
