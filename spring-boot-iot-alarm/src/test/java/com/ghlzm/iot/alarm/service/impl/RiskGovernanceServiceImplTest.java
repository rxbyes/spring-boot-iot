package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.EmergencyPlan;
import com.ghlzm.iot.alarm.entity.LinkageRule;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskGovernanceServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private RiskPointMapper riskPointMapper;

    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;

    @Mock
    private RuleDefinitionMapper ruleDefinitionMapper;

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private ProductModelMapper productModelMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductContractReleaseBatchMapper productContractReleaseBatchMapper;

    @Mock
    private LinkageRuleMapper linkageRuleMapper;

    @Mock
    private EmergencyPlanMapper emergencyPlanMapper;

    @Test
    void pageMetricCatalogsShouldReturnProductScopedRows() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(9101L);
        catalog.setProductId(1001L);
        catalog.setContractIdentifier("value");
        catalog.setRiskMetricCode("RM_1001_VALUE");
        catalog.setRiskMetricName("裂缝监测值");
        catalog.setEnabled(1);
        catalog.setTrendEnabled(1);
        catalog.setGisEnabled(1);
        catalog.setInsightEnabled(1);
        catalog.setAnalyticsEnabled(1);
        catalog.setUpdateTime(LocalDateTime.of(2026, 4, 6, 10, 30));
        when(riskMetricCatalogMapper.selectPage(any(), any())).thenReturn(new Page<RiskMetricCatalog>(1L, 10L, 1L)
                .setRecords(List.of(catalog)));

        PageResult<RiskMetricCatalogItemVO> page = service.pageMetricCatalogs(1001L, 1L, 10L);

        assertEquals(1L, page.getTotal());
        assertEquals("value", page.getRecords().get(0).getContractIdentifier());
        assertEquals("RM_1001_VALUE", page.getRecords().get(0).getRiskMetricCode());
    }

    @Test
    void getCoverageOverviewShouldAggregateContractMetricBindingAndRuleCoverage() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );

        ProductModel value = new ProductModel();
        value.setId(3001L);
        value.setProductId(1001L);
        value.setModelType("property");
        value.setIdentifier("value");
        ProductModel sensorState = new ProductModel();
        sensorState.setId(3002L);
        sensorState.setProductId(1001L);
        sensorState.setModelType("property");
        sensorState.setIdentifier("sensor_state");
        when(productModelMapper.selectList(any())).thenReturn(List.of(value, sensorState));

        RiskMetricCatalog metricValue = new RiskMetricCatalog();
        metricValue.setId(9101L);
        metricValue.setProductId(1001L);
        metricValue.setContractIdentifier("value");
        metricValue.setEnabled(1);
        RiskMetricCatalog metricX = new RiskMetricCatalog();
        metricX.setId(9102L);
        metricX.setProductId(1001L);
        metricX.setContractIdentifier("gpsTotalX");
        metricX.setEnabled(1);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(metricValue, metricX));

        Device device = new Device();
        device.setId(8001L);
        device.setProductId(1001L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        RiskPointDevice binding = new RiskPointDevice();
        binding.setDeviceId(8001L);
        binding.setRiskMetricId(9101L);
        binding.setMetricIdentifier("value");
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));

        RuleDefinition rule = new RuleDefinition();
        rule.setId(6001L);
        rule.setRiskMetricId(9101L);
        rule.setMetricIdentifier("value");
        rule.setStatus(0);
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        RiskGovernanceCoverageOverviewVO overview = service.getCoverageOverview(1001L);

        assertEquals(1001L, overview.getProductId());
        assertEquals(2L, overview.getContractPropertyCount());
        assertEquals(2L, overview.getPublishedRiskMetricCount());
        assertEquals(1L, overview.getBoundRiskMetricCount());
        assertEquals(1L, overview.getRuleCoveredRiskMetricCount());
        assertEquals(100.0, overview.getRuleCoverageRate());
    }

    @Test
    void getCoverageOverviewShouldOnlySelectColumnsNeededForCatalogProjection() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );

        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());

        service.getCoverageOverview(1001L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RiskMetricCatalog>> captor =
                ArgumentCaptor.forClass((Class) com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        verify(riskMetricCatalogMapper).selectList(captor.capture());
        String sqlSelect = captor.getValue().getSqlSelect();

        assertNotNull(sqlSelect);
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("contract_identifier"));
        assertFalse(sqlSelect.contains("release_batch_id"));
    }

    @Test
    void listMissingPolicyAlertSignalsShouldAggregateByRiskMetricIdOrIdentifierAndSkipCoveredMetrics() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );

        RiskPointDevice coveredA = binding(8001L, 5001L, 9101L, "value", "裂缝监测值");
        RiskPointDevice coveredB = binding(8002L, 5002L, 9101L, "value", "裂缝监测值");
        RiskPointDevice missingByIdA = binding(8101L, 5101L, 9201L, "gpsTotalZ", "Z向累计位移");
        RiskPointDevice missingByIdB = binding(8102L, 5102L, 9201L, "gpsTotalZ", "Z向累计位移");
        RiskPointDevice missingByIdentifierA = binding(8201L, 5201L, null, "gpsTotalX", "X向累计位移");
        RiskPointDevice missingByIdentifierB = binding(8202L, 5202L, null, "gpsTotalX", "X向累计位移");
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                coveredA,
                coveredB,
                missingByIdA,
                missingByIdB,
                missingByIdentifierA,
                missingByIdentifierB
        ));

        RuleDefinition coveredRule = new RuleDefinition();
        coveredRule.setId(6001L);
        coveredRule.setStatus(0);
        coveredRule.setRiskMetricId(9101L);
        coveredRule.setMetricIdentifier("value");
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(coveredRule));

        List<RiskGovernanceService.MissingPolicyAlertSignal> signals = service.listMissingPolicyAlertSignals();

        assertEquals(2, signals.size());
        Map<String, RiskGovernanceService.MissingPolicyAlertSignal> signalsByKey = signals.stream()
                .collect(java.util.stream.Collectors.toMap(RiskGovernanceService.MissingPolicyAlertSignal::dimensionKey, value -> value));
        assertEquals(2L, signalsByKey.get("risk_metric_id:9201").bindingCount());
        assertEquals(2L, signalsByKey.get("risk_metric_id:9201").riskPointCount());
        assertEquals(2L, signalsByKey.get("metric_identifier:gpstotalx").bindingCount());
        assertEquals(2L, signalsByKey.get("metric_identifier:gpstotalx").riskPointCount());
    }

    @Test
    void getDashboardOverviewShouldAggregateGovernanceAndBacklogMetrics() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );

        Product productA = new Product();
        productA.setId(1001L);
        productA.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        Product productB = new Product();
        productB.setId(1002L);
        productB.setCreateTime(LocalDateTime.of(2026, 4, 3, 10, 0));
        when(productMapper.selectList(any())).thenReturn(List.of(productA, productB));

        ProductContractReleaseBatch releaseBatch = new ProductContractReleaseBatch();
        releaseBatch.setId(5001L);
        releaseBatch.setProductId(1001L);
        releaseBatch.setCreateTime(LocalDateTime.of(2026, 4, 2, 10, 0));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch));

        RiskMetricCatalog metricA = new RiskMetricCatalog();
        metricA.setId(9101L);
        metricA.setProductId(1001L);
        metricA.setContractIdentifier("value");
        metricA.setEnabled(1);
        RiskMetricCatalog metricB = new RiskMetricCatalog();
        metricB.setId(9102L);
        metricB.setProductId(1002L);
        metricB.setContractIdentifier("gpsTotalX");
        metricB.setEnabled(1);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(metricA, metricB));

        RiskPointDevice binding = new RiskPointDevice();
        binding.setDeviceId(8001L);
        binding.setRiskMetricId(9101L);
        binding.setMetricIdentifier("value");
        binding.setMetricName("裂缝监测值");
        RiskPointDevice missingPolicyBinding = new RiskPointDevice();
        missingPolicyBinding.setDeviceId(8001L);
        missingPolicyBinding.setRiskMetricId(9102L);
        missingPolicyBinding.setMetricIdentifier("gpsTotalX");
        missingPolicyBinding.setMetricName("X 向累计位移");
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding, missingPolicyBinding));

        RuleDefinition rule = new RuleDefinition();
        rule.setId(6001L);
        rule.setRiskMetricId(9101L);
        rule.setMetricIdentifier("value");
        rule.setStatus(0);
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        LinkageRule linkageRule = new LinkageRule();
        linkageRule.setId(7001L);
        linkageRule.setStatus(0);
        linkageRule.setTriggerCondition("[{\"metricIdentifier\":\"value\",\"operator\":\">\",\"value\":0.5}]");
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of(linkageRule));

        EmergencyPlan emergencyPlan = new EmergencyPlan();
        emergencyPlan.setId(7101L);
        emergencyPlan.setStatus(0);
        emergencyPlan.setPlanName("gpsTotalX 指标预案");
        emergencyPlan.setDescription("针对 gpsTotalX 超阈值场景");
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of(emergencyPlan));

        Device reportedAndBound = new Device();
        reportedAndBound.setId(8001L);
        reportedAndBound.setDeviceCode("dev-1");
        reportedAndBound.setDeviceName("设备1");
        reportedAndBound.setLastReportTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        Device reportedButUnbound = new Device();
        reportedButUnbound.setId(8002L);
        reportedButUnbound.setDeviceCode("dev-2");
        reportedButUnbound.setDeviceName("设备2");
        reportedButUnbound.setLastReportTime(LocalDateTime.of(2026, 4, 6, 11, 0));
        when(deviceMapper.selectList(any())).thenReturn(List.of(reportedAndBound, reportedButUnbound));

        RiskGovernanceDashboardOverviewVO overview = service.getDashboardOverview();

        assertEquals(2L, overview.getTotalProductCount());
        assertEquals(2L, overview.getGovernedProductCount());
        assertEquals(0L, overview.getPendingProductGovernanceCount());
        assertEquals(1L, overview.getReleasedProductCount());
        assertEquals(1L, overview.getPendingContractReleaseCount());
        assertEquals(2L, overview.getPublishedRiskMetricCount());
        assertEquals(2L, overview.getBoundRiskMetricCount());
        assertEquals(1L, overview.getRuleCoveredRiskMetricCount());
        assertEquals(1L, overview.getPendingRiskBindingCount());
        assertEquals(1L, overview.getPendingThresholdPolicyCount());
        assertEquals(1L, overview.getPendingLinkageCount());
        assertEquals(1L, overview.getPendingEmergencyPlanCount());
        assertEquals(2L, overview.getPendingLinkagePlanCount());
        assertEquals(1L, overview.getPendingReplayCount());
        assertEquals(100.0, overview.getGovernanceCompletionRate(), 1e-6);
        assertEquals(100.0, overview.getMetricBindingCoverageRate(), 1e-6);
        assertEquals(50.0, overview.getPolicyCoverageRate(), 1e-6);
        assertEquals(50.0, overview.getLinkageCoverageRate(), 1e-6);
        assertEquals(50.0, overview.getEmergencyPlanCoverageRate(), 1e-6);
        assertEquals(50.0, overview.getLinkagePlanCoverageRate(), 1e-6);
        assertEquals(24.0, overview.getAverageOnboardingDurationHours(), 1e-6);
        assertEquals(0.0, overview.getBottleneckPendingProductGovernanceRate(), 1e-6);
        assertEquals(16.666666666666668, overview.getBottleneckPendingContractReleaseRate(), 1e-6);
        assertEquals(16.666666666666668, overview.getBottleneckPendingRiskBindingRate(), 1e-6);
        assertEquals(16.666666666666668, overview.getBottleneckPendingThresholdPolicyRate(), 1e-6);
        assertEquals(33.333333333333336, overview.getBottleneckPendingLinkagePlanRate(), 1e-6);
        assertEquals(16.666666666666668, overview.getBottleneckPendingReplayRate(), 1e-6);
    }

    @Test
    void getDashboardOverviewShouldOnlySelectColumnsNeededForCatalogProjection() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper,
                productMapper,
                productContractReleaseBatchMapper,
                linkageRuleMapper,
                emergencyPlanMapper
        );

        Product product = new Product();
        product.setId(1001L);
        product.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        when(productMapper.selectList(any())).thenReturn(List.of(product));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageRuleMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        service.getDashboardOverview();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<RiskMetricCatalog>> captor =
                ArgumentCaptor.forClass((Class) com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        verify(riskMetricCatalogMapper).selectList(captor.capture());
        String sqlSelect = captor.getValue().getSqlSelect();

        assertNotNull(sqlSelect);
        assertTrue(sqlSelect.contains("id"));
        assertTrue(sqlSelect.contains("product_id"));
        assertTrue(sqlSelect.contains("contract_identifier"));
        assertFalse(sqlSelect.contains("release_batch_id"));
    }

    private RiskPointDevice binding(Long deviceId,
                                    Long riskPointId,
                                    Long riskMetricId,
                                    String metricIdentifier,
                                    String metricName) {
        RiskPointDevice value = new RiskPointDevice();
        value.setDeviceId(deviceId);
        value.setRiskPointId(riskPointId);
        value.setRiskMetricId(riskMetricId);
        value.setMetricIdentifier(metricIdentifier);
        value.setMetricName(metricName);
        return value;
    }
}
