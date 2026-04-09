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
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import com.ghlzm.iot.device.service.DeviceAccessErrorLogService;
import com.ghlzm.iot.device.service.DeviceMessageService;
import com.ghlzm.iot.device.vo.messageflow.MessageTraceDetailVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

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
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Mock
    private DeviceMessageService deviceMessageService;

    @Mock
    private DeviceAccessErrorLogService deviceAccessErrorLogService;

    @Mock
    private RiskGovernanceService riskGovernanceService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void pageOpsAlertsShouldExposeFieldDriftAndMissingRiskMetric() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                riskMetricCatalogMapper,
                productMapper,
                releaseBatchMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher
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

        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());

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
    }

    @Test
    void replayShouldAggregateMessageAndAccessErrorContext() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                riskMetricCatalogMapper,
                productMapper,
                releaseBatchMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher
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
                riskMetricCatalogMapper,
                productMapper,
                releaseBatchMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService,
                applicationEventPublisher
        );
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setId(7001L);
        batch.setProductId(1001L);
        batch.setScenarioCode("phase1-crack");
        batch.setCreateTime(LocalDateTime.of(2026, 4, 7, 10, 30));
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-product");
        when(releaseBatchMapper.selectById(7001L)).thenReturn(batch);
        when(productMapper.selectById(1001L)).thenReturn(product);

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

        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());

        RiskGovernanceReplayVO replay = service.replay(1001L, null, null, null, 7001L);

        assertEquals(7001L, replay.getReleaseBatchId());
        assertEquals("phase1-crack", replay.getReleaseScenarioCode());
        assertEquals("phase1-crack-product", replay.getProductKey());
    }
}
