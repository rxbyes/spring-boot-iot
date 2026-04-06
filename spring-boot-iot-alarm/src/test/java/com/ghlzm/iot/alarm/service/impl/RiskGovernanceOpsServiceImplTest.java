package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceOpsAlertItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceReplayVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.DeviceAccessErrorQuery;
import com.ghlzm.iot.device.dto.DeviceMessageTraceQuery;
import com.ghlzm.iot.device.entity.DeviceAccessErrorLog;
import com.ghlzm.iot.device.entity.DeviceMessageLog;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private DeviceMessageService deviceMessageService;

    @Mock
    private DeviceAccessErrorLogService deviceAccessErrorLogService;

    @Mock
    private RiskGovernanceService riskGovernanceService;

    @Test
    void pageOpsAlertsShouldExposeFieldDriftAndMissingRiskMetric() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                riskMetricCatalogMapper,
                productMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService
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
    }

    @Test
    void replayShouldAggregateMessageAndAccessErrorContext() {
        RiskGovernanceOpsServiceImpl service = new RiskGovernanceOpsServiceImpl(
                vendorMetricEvidenceMapper,
                productModelMapper,
                riskMetricCatalogMapper,
                productMapper,
                deviceMessageService,
                deviceAccessErrorLogService,
                riskGovernanceService
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

        RiskGovernanceReplayVO replay = service.replay(1001L, "trace-001", null, null);

        assertEquals("trace-001", replay.getTraceId());
        assertEquals(1L, replay.getMatchedMessageCount());
        assertEquals(1L, replay.getMatchedAccessErrorCount());
        assertEquals("trace-001", replay.getLatestMessageDetail().getTraceId());
    }
}
