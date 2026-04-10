package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanWrapperImpl;

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

    @Mock
    private RiskMetricLinkageBindingMapper linkageBindingMapper;

    @Mock
    private RiskMetricEmergencyPlanBindingMapper emergencyPlanBindingMapper;

    @Mock
    private VendorMetricEvidenceMapper vendorMetricEvidenceMapper;

    @Mock
    private RiskMetricActionBindingBackfillService backfillService;

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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
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

        PageResult<RiskMetricCatalogItemVO> page = service.pageMetricCatalogs(1001L, null, 1L, 10L);

        assertEquals(1L, page.getTotal());
        assertEquals("value", page.getRecords().get(0).getContractIdentifier());
        assertEquals("RM_1001_VALUE", page.getRecords().get(0).getRiskMetricCode());
    }

    @Test
    void pageMetricCatalogsShouldFilterByReleaseBatchId() {
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );
        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(9101L);
        catalog.setProductId(1001L);
        catalog.setReleaseBatchId(7001L);
        catalog.setContractIdentifier("value");
        catalog.setRiskMetricCode("RM_1001_VALUE");
        catalog.setRiskMetricName("裂缝监测值");
        when(riskMetricCatalogMapper.selectPage(any(), any())).thenReturn(new Page<RiskMetricCatalog>(1L, 10L, 1L)
                .setRecords(List.of(catalog)));

        PageResult<RiskMetricCatalogItemVO> page = service.pageMetricCatalogs(1001L, 7001L, 1L, 10L);

        assertEquals(1L, page.getTotal());
        assertEquals(7001L, page.getRecords().get(0).getReleaseBatchId());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RiskMetricCatalog>> captor =
                ArgumentCaptor.forClass((Class) com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(riskMetricCatalogMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getParamNameValuePairs().values().contains(7001L));
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
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
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        when(productModelMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        RiskPointDevice coveredA = binding(8001L, 5001L, 9101L, "value", "裂缝监测值");
        RiskPointDevice coveredB = binding(8002L, 5002L, 9101L, "value", "裂缝监测值");
        RiskPointDevice missingByIdA = binding(8101L, 5101L, 9201L, "gpsTotalZ", "Z轴累计位移");
        RiskPointDevice missingByIdB = binding(8102L, 5102L, 9201L, "gpsTotalZ", "Z轴累计位移");
        RiskPointDevice missingByIdentifierA = binding(8201L, 5201L, null, "gpsTotalX", "X轴累计位移");
        RiskPointDevice missingByIdentifierB = binding(8202L, 5202L, null, "gpsTotalX", "X轴累计位移");
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
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
        binding.setMetricName("?????");
        RiskPointDevice missingPolicyBinding = new RiskPointDevice();
        missingPolicyBinding.setDeviceId(8001L);
        missingPolicyBinding.setRiskMetricId(9102L);
        missingPolicyBinding.setMetricIdentifier("gpsTotalX");
        missingPolicyBinding.setMetricName("X ?????");
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding, missingPolicyBinding));

        RuleDefinition rule = new RuleDefinition();
        rule.setId(6001L);
        rule.setRiskMetricId(9101L);
        rule.setMetricIdentifier("value");
        rule.setStatus(0);
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(9901L, 1L, 9101L, 7001L, "AUTO_INFERRED", "ACTIVE", 0)
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(
                planBinding(9951L, 1L, 9102L, 7101L, "BACKFILL", "ACTIVE", 0)
        ));

        Device reportedAndBound = new Device();
        reportedAndBound.setId(8001L);
        reportedAndBound.setDeviceCode("dev-1");
        reportedAndBound.setDeviceName("??1");
        reportedAndBound.setLastReportTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        Device reportedButUnbound = new Device();
        reportedButUnbound.setId(8002L);
        reportedButUnbound.setDeviceCode("dev-2");
        reportedButUnbound.setDeviceName("??2");
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
    void getDashboardOverviewShouldExposeRawStageVendorAndProductBacklog() {
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                vendorMetricEvidenceMapper,
                backfillService
        );

        Product governedProduct = new Product();
        governedProduct.setId(1001L);
        governedProduct.setProductKey("nf-monitor-crack-meter-v1");
        governedProduct.setProductName("裂缝计");
        governedProduct.setManufacturer("南方测绘");
        governedProduct.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));

        Product rawStageProduct = new Product();
        rawStageProduct.setId(1002L);
        rawStageProduct.setProductKey("nf-monitor-gnss-v1");
        rawStageProduct.setProductName("GNSS位移监测仪");
        rawStageProduct.setManufacturer("中海达");
        rawStageProduct.setCreateTime(LocalDateTime.of(2026, 4, 2, 10, 0));

        when(productMapper.selectList(any())).thenReturn(List.of(governedProduct, rawStageProduct));

        ProductContractReleaseBatch releaseBatch = new ProductContractReleaseBatch();
        releaseBatch.setId(5001L);
        releaseBatch.setProductId(1001L);
        releaseBatch.setCreateTime(LocalDateTime.of(2026, 4, 3, 10, 0));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch));

        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(9101L);
        catalog.setProductId(1001L);
        catalog.setContractIdentifier("value");
        catalog.setEnabled(1);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(catalog));

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        when(vendorMetricEvidenceMapper.selectList(any())).thenReturn(List.of(
                vendorEvidence(1002L, "gpsTotalX", 9, LocalDateTime.of(2026, 4, 5, 9, 0)),
                vendorEvidence(1002L, "gpsTotalY", 3, LocalDateTime.of(2026, 4, 5, 8, 0))
        ));

        RiskGovernanceDashboardOverviewVO overview = service.getDashboardOverview();
        BeanWrapperImpl wrapper = new BeanWrapperImpl(overview);

        assertEquals(1L, wrapper.getPropertyValue("rawStageProductCount"));
        assertEquals(1L, wrapper.getPropertyValue("rawStageVendorCount"));
        assertEquals(List.of("中海达"), wrapper.getPropertyValue("rawStageVendorNames"));
        assertEquals(List.of("GNSS位移监测仪"), wrapper.getPropertyValue("rawStageProductNames"));
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        Product product = new Product();
        product.setId(1001L);
        product.setCreateTime(LocalDateTime.of(2026, 4, 1, 10, 0));
        when(productMapper.selectList(any())).thenReturn(List.of(product));
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of());
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of());
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());
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

    @Test
    void getCoverageOverviewShouldUseExplicitBindingsInsteadOfTextGuessing() {
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        ProductModel value = new ProductModel();
        value.setId(3001L);
        value.setProductId(1001L);
        value.setModelType("property");
        value.setIdentifier("value");
        ProductModel totalX = new ProductModel();
        totalX.setId(3002L);
        totalX.setProductId(1001L);
        totalX.setModelType("property");
        totalX.setIdentifier("gpsTotalX");
        when(productModelMapper.selectList(any())).thenReturn(List.of(value, totalX));

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

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(8001L, 5001L, 9101L, "value", "裂缝监测值"),
                binding(8001L, 5001L, 9102L, "gpsTotalX", "X轴累计位移")
        ));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule(6001L, 9101L, "value")));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(9901L, 1L, 9101L, 7001L, "AUTO_INFERRED", "ACTIVE", 0)
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of(
                planBinding(9951L, 1L, 9102L, 7101L, "BACKFILL", "ACTIVE", 0)
        ));

        RiskGovernanceCoverageOverviewVO overview = service.getCoverageOverview(1001L);

        assertEquals(1L, overview.getLinkageCoveredRiskMetricCount());
        assertEquals(1L, overview.getEmergencyPlanCoveredRiskMetricCount());
        assertEquals(0L, overview.getLinkagePlanCoveredRiskMetricCount());
        assertEquals(50.0, overview.getLinkageCoverageRate());
        assertEquals(50.0, overview.getEmergencyPlanCoverageRate());
        assertEquals(0.0, overview.getLinkagePlanCoverageRate());
    }

    @Test
    void getDashboardOverviewShouldTriggerOneShotBackfillBeforeCountingBindings() {
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
                emergencyPlanMapper,
                linkageBindingMapper,
                emergencyPlanBindingMapper,
                backfillService
        );

        Product product = new Product();
        product.setId(1001L);
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        ProductContractReleaseBatch releaseBatch = new ProductContractReleaseBatch();
        releaseBatch.setId(5001L);
        releaseBatch.setProductId(1001L);
        when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch));

        RiskMetricCatalog metric = new RiskMetricCatalog();
        metric.setId(9101L);
        metric.setProductId(1001L);
        metric.setContractIdentifier("value");
        metric.setEnabled(1);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(metric));

        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(
                binding(8001L, 5001L, 9101L, "value", "裂缝监测值")
        ));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule(6001L, 9101L, "value")));
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of(
                linkageBinding(9901L, 1L, 9101L, 7001L, "BACKFILL", "ACTIVE", 0)
        ));
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        service.getDashboardOverview();

        verify(backfillService).ensureBindingsReadyForRead();
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

    private RuleDefinition rule(Long id, Long riskMetricId, String metricIdentifier) {
        RuleDefinition value = new RuleDefinition();
        value.setId(id);
        value.setRiskMetricId(riskMetricId);
        value.setMetricIdentifier(metricIdentifier);
        value.setStatus(0);
        return value;
    }

    private RiskMetricLinkageBinding linkageBinding(Long id,
                                                    Long tenantId,
                                                    Long riskMetricId,
                                                    Long linkageRuleId,
                                                    String bindingOrigin,
                                                    String bindingStatus,
                                                    Integer deleted) {
        RiskMetricLinkageBinding value = new RiskMetricLinkageBinding();
        value.setId(id);
        value.setTenantId(tenantId);
        value.setRiskMetricId(riskMetricId);
        value.setLinkageRuleId(linkageRuleId);
        value.setBindingOrigin(bindingOrigin);
        value.setBindingStatus(bindingStatus);
        value.setDeleted(deleted);
        return value;
    }

    private RiskMetricEmergencyPlanBinding planBinding(Long id,
                                                       Long tenantId,
                                                       Long riskMetricId,
                                                       Long emergencyPlanId,
                                                       String bindingOrigin,
                                                       String bindingStatus,
                                                       Integer deleted) {
        RiskMetricEmergencyPlanBinding value = new RiskMetricEmergencyPlanBinding();
        value.setId(id);
        value.setTenantId(tenantId);
        value.setRiskMetricId(riskMetricId);
        value.setEmergencyPlanId(emergencyPlanId);
        value.setBindingOrigin(bindingOrigin);
        value.setBindingStatus(bindingStatus);
        value.setDeleted(deleted);
        return value;
    }

    private VendorMetricEvidence vendorEvidence(Long productId,
                                                String rawIdentifier,
                                                Integer evidenceCount,
                                                LocalDateTime lastSeenTime) {
        VendorMetricEvidence value = new VendorMetricEvidence();
        value.setProductId(productId);
        value.setRawIdentifier(rawIdentifier);
        value.setEvidenceCount(evidenceCount);
        value.setLastSeenTime(lastSeenTime);
        return value;
    }
}
