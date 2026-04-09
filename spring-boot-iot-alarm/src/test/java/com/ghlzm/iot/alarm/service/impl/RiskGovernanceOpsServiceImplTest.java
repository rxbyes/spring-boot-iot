package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.common.event.governance.GovernanceOpsAlertRaisedEvent;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import com.ghlzm.iot.system.entity.NotificationChannel;
import com.ghlzm.iot.system.service.NotificationChannelDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskGovernanceOpsServiceImplTest {

    @Mock
    private VendorMetricEvidenceMapper vendorMetricEvidenceMapper;

    @Mock
    private ProductModelMapper productModelMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Mock
    private ProductContractReleaseSnapshotMapper releaseSnapshotMapper;

    @Mock
    private DeviceMessageService deviceMessageService;

    @Mock
    private DeviceAccessErrorLogService deviceAccessErrorLogService;

    @Mock
    private RiskGovernanceService riskGovernanceService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private NotificationChannelDispatcher notificationChannelDispatcher;

    @Test
    void pageOpsAlertsShouldExposeFieldDriftAndMissingRiskMetric() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                catalogMapper(List.of()),
                productMapper,
                releaseBatchMapper,
                releaseSnapshotMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher,
                notificationChannelDispatcher
        );

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-p1");
        product.setProductName("裂缝产品");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        ProductModel model = new ProductModel();
        model.setProductId(1001L);
        model.setModelType("property");
        model.setIdentifier("value");
        when(productModelMapper.selectList(any())).thenReturn(List.of(model));

        VendorMetricEvidence evidence = new VendorMetricEvidence();
        evidence.setProductId(1001L);
        evidence.setRawIdentifier("L1_SW_1.dispsX");
        evidence.setCanonicalIdentifier("dispsX");
        when(vendorMetricEvidenceMapper.selectList(any())).thenReturn(List.of(evidence));

        when(notificationChannelDispatcher.listSceneChannels("observability_alert", "FIELD_DRIFT"))
                .thenReturn(List.of(
                        dispatchChannel("ops-field-drift", List.of("FIELD_DRIFT")),
                        dispatchChannel("ops-governance-all", List.of())
                ));
        when(notificationChannelDispatcher.listSceneChannels("observability_alert", "MISSING_RISK_METRIC"))
                .thenReturn(List.of(
                        dispatchChannel("ops-governance-all", List.of())
                ));

        PageResult<RiskGovernanceOpsAlertItemVO> page = service.pageOpsAlerts(null, null, 1L, 10L);

        assertEquals(2L, page.getTotal());
        assertTrue(page.getRecords().stream().anyMatch(item -> "FIELD_DRIFT".equals(item.getAlertType())));
        assertTrue(page.getRecords().stream().anyMatch(item -> "MISSING_RISK_METRIC".equals(item.getAlertType())));
        verify(applicationEventPublisher).publishEvent(argThat((GovernanceOpsAlertRaisedEvent event) ->
                "FIELD_DRIFT".equals(event.alertType())
                        && "PRODUCT".equals(event.subjectType())
                        && Long.valueOf(1001L).equals(event.subjectId())
                        && Long.valueOf(1001L).equals(event.productId())
                        && Long.valueOf(1L).equals(event.affectedCount())
        ));
        verify(applicationEventPublisher).publishEvent(argThat((GovernanceOpsAlertRaisedEvent event) ->
                "MISSING_RISK_METRIC".equals(event.alertType())
                        && "PRODUCT".equals(event.subjectType())
                        && Long.valueOf(1001L).equals(event.subjectId())
                        && Long.valueOf(1001L).equals(event.productId())
                        && Long.valueOf(1L).equals(event.affectedCount())
        ));
        RiskGovernanceOpsAlertItemVO fieldDrift = page.getRecords().stream()
                .filter(item -> "FIELD_DRIFT".equals(item.getAlertType()))
                .findFirst()
                .orElseThrow();
        assertEquals("observability_alert", fieldDrift.getSubscriptionScene());
        assertEquals(2L, fieldDrift.getSubscriptionChannelCount());
        assertEquals(List.of("ops-field-drift", "ops-governance-all"), fieldDrift.getSubscriptionChannelCodes());
    }

    @Test
    void replayShouldAggregateMessageAndAccessErrorContext() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                catalogMapper(List.of()),
                productMapper,
                releaseBatchMapper,
                releaseSnapshotMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher,
                notificationChannelDispatcher
        );

        DeviceMessageLog message = new DeviceMessageLog();
        message.setId(9001L);
        message.setTraceId("trace-001");
        message.setDeviceCode("dev-001");
        message.setProductKey("p1");
        PageResult<DeviceMessageLog> messagePage = PageResult.of(1L, 1L, 20L, List.of(message));
        when(deviceMessageService.pageMessageTraceLogs(
                argThat(currentUserId -> currentUserId != null && currentUserId.equals(1001L)),
                any(DeviceMessageTraceQuery.class),
                argThat(pageNum -> pageNum != null && pageNum == 1),
                argThat(pageSize -> pageSize != null && pageSize == 20)
        )).thenReturn(messagePage);

        MessageTraceDetailVO detail = new MessageTraceDetailVO();
        detail.setTraceId("trace-001");
        when(deviceMessageService.getMessageTraceDetail(1001L, 9001L)).thenReturn(detail);

        DeviceAccessErrorLog errorLog = new DeviceAccessErrorLog();
        errorLog.setTraceId("trace-001");
        PageResult<DeviceAccessErrorLog> errorPage = PageResult.of(1L, 1L, 10L, List.of(errorLog));
        when(deviceAccessErrorLogService.pageLogs(
                argThat(currentUserId -> currentUserId != null && currentUserId.equals(1001L)),
                any(DeviceAccessErrorQuery.class),
                argThat(pageNum -> pageNum != null && pageNum == 1),
                argThat(pageSize -> pageSize != null && pageSize == 10)
        )).thenReturn(errorPage);

        when(productMapper.selectList(any())).thenReturn(List.of());
        when(riskGovernanceService.listMissingBindings(any(RiskGovernanceGapQuery.class)))
                .thenReturn(PageResult.empty(1L, 10L));
        when(riskGovernanceService.listMissingPolicies(any(RiskGovernanceGapQuery.class)))
                .thenReturn(PageResult.empty(1L, 10L));

        RiskGovernanceReplayVO replay = service.replay(1001L, "trace-001", null, null, null);

        assertEquals("trace-001", replay.getTraceId());
        assertEquals(1L, replay.getMatchedMessageCount());
        assertEquals(1L, replay.getMatchedAccessErrorCount());
        assertEquals("trace-001", replay.getLatestMessageDetail().getTraceId());
    }

    @Test
    void replayShouldSupportReleaseBatchDimensionAndResolveProductContext() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                catalogMapper(List.of(catalog(1001L, "value"))),
                productMapper,
                releaseBatchMapper,
                releaseSnapshotMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher,
                notificationChannelDispatcher
        );
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(7001L);
        batch.setProductId(1001L);
        batch.setScenarioCode("phase1-crack");
        batch.setReleaseStatus("RELEASED");
        batch.setReleaseReason("initial release");
        batch.setCreateTime(LocalDateTime.of(2026, 4, 7, 10, 30));
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-product");
        when(releaseBatchMapper.selectById(7001L)).thenReturn(batch);
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(releaseSnapshotMapper.selectList(any())).thenReturn(List.of(
                snapshot(7001L, 1001L, "BEFORE_APPLY", "[]"),
                snapshot(7001L, 1001L, "AFTER_APPLY", """
                        [
                          {"modelType":"property","identifier":"value","modelName":"Value","dataType":"decimal"},
                          {"modelType":"property","identifier":"sensor_state","modelName":"State","dataType":"int"}
                        ]
                        """)
        ));

        when(deviceMessageService.pageMessageTraceLogs(
                argThat(currentUserId -> currentUserId != null && currentUserId.equals(1001L)),
                argThat(query -> query != null
                        && "phase1-crack-product".equals(query.getProductKey())
                        && query.getTraceId() == null
                        && query.getDeviceCode() == null),
                argThat(pageNum -> pageNum != null && pageNum == 1),
                argThat(pageSize -> pageSize != null && pageSize == 20)
        )).thenReturn(PageResult.empty(1L, 20L));

        when(deviceAccessErrorLogService.pageLogs(
                argThat(currentUserId -> currentUserId != null && currentUserId.equals(1001L)),
                argThat(query -> query != null
                        && "phase1-crack-product".equals(query.getProductKey())
                        && query.getTraceId() == null
                        && query.getDeviceCode() == null),
                argThat(pageNum -> pageNum != null && pageNum == 1),
                argThat(pageSize -> pageSize != null && pageSize == 10)
        )).thenReturn(PageResult.empty(1L, 10L));

        ProductModel currentValue = new ProductModel();
        currentValue.setProductId(1001L);
        currentValue.setModelType("property");
        currentValue.setIdentifier("value");
        ProductModel currentTemp = new ProductModel();
        currentTemp.setProductId(1001L);
        currentTemp.setModelType("property");
        currentTemp.setIdentifier("temp");
        when(productModelMapper.selectList(any())).thenReturn(List.of(currentValue, currentTemp));
        RiskGovernanceReplayVO replay = service.replay(1001L, null, null, null, 7001L);

        assertEquals(7001L, replay.getReleaseBatchId());
        assertEquals("phase1-crack", replay.getReleaseScenarioCode());
        assertEquals("phase1-crack-product", replay.getProductKey());
        assertEquals(2L, replay.getBatchReconciliation().getAfterApplyFieldCount());
        assertEquals(2L, replay.getBatchReconciliation().getCurrentFormalFieldCount());
        assertEquals(1L, replay.getBatchReconciliation().getMissingCurrentFieldCount());
        assertEquals(1L, replay.getBatchReconciliation().getExtraCurrentFieldCount());
        assertEquals("sensor_state", replay.getBatchReconciliation().getSampleMissingCurrentIdentifier());
        assertEquals("temp", replay.getBatchReconciliation().getSampleExtraCurrentIdentifier());
        assertTrue(replay.getReplayChainSteps().stream().anyMatch(step ->
                "BATCH_RECONCILIATION".equals(step.getStepCode())
                        && "ACTION_REQUIRED".equals(step.getStatus())
        ));
    }

    private NotificationChannelDispatcher.DispatchChannel dispatchChannel(String code, List<String> opsAlertTypes) {
        NotificationChannel channel = new NotificationChannel();
        channel.setId(1L);
        channel.setTenantId(1L);
        channel.setChannelCode(code);
        channel.setChannelName(code);
        channel.setChannelType("webhook");
        channel.setStatus(1);
        channel.setDeleted(0);
        return new NotificationChannelDispatcher.DispatchChannel(
                channel,
                new NotificationChannelDispatcher.ChannelConfig(
                        "https://example.com/" + code,
                        Map.of(),
                        List.of("observability_alert"),
                        opsAlertTypes,
                        3000,
                        300
                )
        );
    }

    private ProductContractReleaseSnapshot snapshot(Long batchId, Long productId, String stage, String snapshotJson) {
        ProductContractReleaseSnapshot snapshot = new ProductContractReleaseSnapshot();
        snapshot.setBatchId(batchId);
        snapshot.setProductId(productId);
        snapshot.setSnapshotStage(stage);
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setDeleted(0);
        return snapshot;
    }

    private RiskMetricCatalogMapper catalogMapper(List<RiskMetricCatalog> catalogs) {
        return (RiskMetricCatalogMapper) Proxy.newProxyInstance(
                RiskMetricCatalogMapper.class.getClassLoader(),
                new Class<?>[]{RiskMetricCatalogMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return catalogs;
                    }
                    if ("toString".equals(method.getName())) {
                        return "RiskMetricCatalogMapperStub";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return null;
                }
        );
    }

    private RiskMetricCatalog catalog(Long productId, String contractIdentifier) {
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setProductId(productId);
        catalog.setContractIdentifier(contractIdentifier);
        catalog.setEnabled(1);
        catalog.setDeleted(0);
        return catalog;
    }
}
