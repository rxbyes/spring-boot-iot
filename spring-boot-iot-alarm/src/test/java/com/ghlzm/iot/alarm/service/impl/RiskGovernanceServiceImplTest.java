package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.dto.RiskGovernanceGapQuery;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskMetricEmergencyPlanBinding;
import com.ghlzm.iot.alarm.entity.RiskMetricLinkageBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDeviceCapabilityBinding;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.EmergencyPlanMapper;
import com.ghlzm.iot.alarm.mapper.LinkageRuleMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricEmergencyPlanBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskMetricLinkageBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceCapabilityBindingMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.service.RiskMetricActionBindingBackfillService;
import com.ghlzm.iot.alarm.service.RiskGovernanceService;
import com.ghlzm.iot.alarm.service.ThresholdPolicyRecommendationService;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceDashboardOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceGapItemVO;
import com.ghlzm.iot.alarm.vo.RiskGovernanceMissingPolicyProductMetricSummaryVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductContractReleaseBatch;
import com.ghlzm.iot.device.entity.ProductContractReleaseSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.entity.VendorMetricEvidence;
import com.ghlzm.iot.device.mapper.ProductContractReleaseBatchMapper;
import com.ghlzm.iot.device.mapper.ProductContractReleaseSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.VendorMetricEvidenceMapper;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskGovernanceServiceImplTest {

    @BeforeAll
    static void initLambdaCache() {
        if (TableInfoHelper.getTableInfo(RiskMetricCatalog.class) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        assistant.setCurrentNamespace(RiskMetricCatalog.class.getName());
        LambdaUtils.installCache(TableInfoHelper.initTableInfo(assistant, RiskMetricCatalog.class));
    }

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private RiskPointMapper riskPointMapper;

    @Mock
    private RiskPointDeviceMapper riskPointDeviceMapper;

    @Mock
    private RiskPointDeviceCapabilityBindingMapper capabilityBindingMapper;

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
    private ProductContractReleaseSnapshotMapper productContractReleaseSnapshotMapper;

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

    @Mock
    private ThresholdPolicyRecommendationService thresholdPolicyRecommendationService;

    @Test
    void listMissingBindingsShouldExcludeDeviceLevelCapabilityBindings() {
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
        service.setRiskPointDeviceCapabilityBindingMapper(capabilityBindingMapper);
        RiskPointDevice metricBinding = new RiskPointDevice();
        metricBinding.setDeviceId(8001L);
        metricBinding.setDeleted(0);
        RiskPointDeviceCapabilityBinding capabilityBinding = new RiskPointDeviceCapabilityBinding();
        capabilityBinding.setDeviceId(8002L);
        capabilityBinding.setDeleted(0);
        Device metricBound = new Device();
        metricBound.setId(8001L);
        metricBound.setDeviceCode("metric-bound");
        metricBound.setDeviceName("metric-bound");
        metricBound.setLastReportTime(LocalDateTime.of(2026, 4, 27, 10, 0));
        Device capabilityBound = new Device();
        capabilityBound.setId(8002L);
        capabilityBound.setDeviceCode("capability-bound");
        capabilityBound.setDeviceName("capability-bound");
        capabilityBound.setLastReportTime(LocalDateTime.of(2026, 4, 27, 11, 0));
        Device unbound = new Device();
        unbound.setId(8003L);
        unbound.setDeviceCode("unbound");
        unbound.setDeviceName("unbound");
        unbound.setLastReportTime(LocalDateTime.of(2026, 4, 27, 12, 0));
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(metricBinding));
        when(capabilityBindingMapper.selectList(any())).thenReturn(List.of(capabilityBinding));
        when(deviceMapper.selectList(any())).thenReturn(List.of(metricBound, capabilityBound, unbound));

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingBindings(null);

        assertEquals(1L, result.getTotal());
        assertEquals("unbound", result.getRecords().get(0).getDeviceCode());
    }

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
        captor.getValue().getSqlSegment();
        assertTrue(captor.getValue().getParamNameValuePairs().values().contains(1001L));
        assertTrue(captor.getValue().getParamNameValuePairs().values().contains(7001L));
    }

    @Test
    void compareReleaseBatchesShouldReturnContractAndMetricDeltas() throws Exception {
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
        ReflectionTestUtils.setField(service, "productContractReleaseSnapshotMapper", productContractReleaseSnapshotMapper);

        ProductContractReleaseBatch baselineBatch = new ProductContractReleaseBatch();
        baselineBatch.setId(7001L);
        baselineBatch.setProductId(1001L);
        baselineBatch.setScenarioCode("phase1-crack");
        baselineBatch.setReleaseStatus("ROLLED_BACK");
        baselineBatch.setReleasedFieldCount(1);
        baselineBatch.setCreateTime(LocalDateTime.of(2026, 4, 9, 18, 0));
        ProductContractReleaseBatch targetBatch = new ProductContractReleaseBatch();
        targetBatch.setId(7002L);
        targetBatch.setProductId(1001L);
        targetBatch.setScenarioCode("phase1-crack");
        targetBatch.setReleaseStatus("RELEASED");
        targetBatch.setReleasedFieldCount(2);
        targetBatch.setCreateTime(LocalDateTime.of(2026, 4, 10, 18, 0));
        when(productContractReleaseBatchMapper.selectById(7001L)).thenReturn(baselineBatch);
        when(productContractReleaseBatchMapper.selectById(7002L)).thenReturn(targetBatch);

        ProductContractReleaseSnapshot baselineSnapshot = new ProductContractReleaseSnapshot();
        baselineSnapshot.setId(8101L);
        baselineSnapshot.setBatchId(7001L);
        baselineSnapshot.setProductId(1001L);
        baselineSnapshot.setSnapshotStage("AFTER_APPLY");
        baselineSnapshot.setSnapshotJson("""
                [
                  {"modelType":"property","identifier":"value","modelName":"裂缝值(旧)","dataType":"double","sortNo":1,"requiredFlag":1},
                  {"modelType":"property","identifier":"sensor_state","modelName":"传感器状态","dataType":"int","sortNo":2,"requiredFlag":0}
                ]
                """);
        ProductContractReleaseSnapshot targetSnapshot = new ProductContractReleaseSnapshot();
        targetSnapshot.setId(8102L);
        targetSnapshot.setBatchId(7002L);
        targetSnapshot.setProductId(1001L);
        targetSnapshot.setSnapshotStage("AFTER_APPLY");
        targetSnapshot.setSnapshotJson("""
                [
                  {"modelType":"property","identifier":"value","modelName":"裂缝值(新)","dataType":"double","sortNo":1,"requiredFlag":1},
                  {"modelType":"property","identifier":"humidity","modelName":"湿度","dataType":"double","sortNo":2,"requiredFlag":0}
                ]
                """);
        when(productContractReleaseSnapshotMapper.selectList(any())).thenReturn(List.of(baselineSnapshot), List.of(targetSnapshot));

        RiskMetricCatalog baselineValue = new RiskMetricCatalog();
        baselineValue.setId(9101L);
        baselineValue.setProductId(1001L);
        baselineValue.setReleaseBatchId(7001L);
        baselineValue.setContractIdentifier("value");
        baselineValue.setRiskMetricCode("RM_1001_VALUE");
        baselineValue.setRiskMetricName("裂缝监测值(旧)");
        baselineValue.setMetricRole("PRIMARY");
        baselineValue.setLifecycleStatus("ACTIVE");
        RiskMetricCatalog baselineState = new RiskMetricCatalog();
        baselineState.setId(9102L);
        baselineState.setProductId(1001L);
        baselineState.setReleaseBatchId(7001L);
        baselineState.setContractIdentifier("sensor_state");
        baselineState.setRiskMetricCode("RM_1001_SENSOR_STATE");
        baselineState.setRiskMetricName("传感器状态");
        baselineState.setMetricRole("SECONDARY");
        baselineState.setLifecycleStatus("ACTIVE");
        RiskMetricCatalog targetValue = new RiskMetricCatalog();
        targetValue.setId(9201L);
        targetValue.setProductId(1001L);
        targetValue.setReleaseBatchId(7002L);
        targetValue.setContractIdentifier("value");
        targetValue.setRiskMetricCode("RM_1001_VALUE");
        targetValue.setRiskMetricName("裂缝监测值");
        targetValue.setMetricRole("PRIMARY");
        targetValue.setLifecycleStatus("ACTIVE");
        RiskMetricCatalog targetHumidity = new RiskMetricCatalog();
        targetHumidity.setId(9202L);
        targetHumidity.setProductId(1001L);
        targetHumidity.setReleaseBatchId(7002L);
        targetHumidity.setContractIdentifier("humidity");
        targetHumidity.setRiskMetricCode("RM_1001_HUMIDITY");
        targetHumidity.setRiskMetricName("湿度监测值");
        targetHumidity.setMetricRole("SECONDARY");
        targetHumidity.setLifecycleStatus("ACTIVE");
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(
                List.of(baselineValue, baselineState),
                List.of(targetValue, targetHumidity)
        );

        Object diff = RiskGovernanceServiceImpl.class
                .getMethod("compareReleaseBatches", Long.class, Long.class)
                .invoke(service, 7001L, 7002L);

        BeanWrapperImpl wrapper = new BeanWrapperImpl(diff);
        assertEquals(1001L, wrapper.getPropertyValue("productId"));
        assertEquals(2, ((Number) wrapper.getPropertyValue("baselineContractFieldCount")).intValue());
        assertEquals(2, ((Number) wrapper.getPropertyValue("targetContractFieldCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("addedContractCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("removedContractCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("changedContractCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("addedMetricCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("removedMetricCount")).intValue());
        assertEquals(1, ((Number) wrapper.getPropertyValue("changedMetricCount")).intValue());
        assertEquals(3, ((List<?>) wrapper.getPropertyValue("contractDiffItems")).size());
        assertEquals(3, ((List<?>) wrapper.getPropertyValue("metricDiffItems")).size());
        assertEquals(7001L, new BeanWrapperImpl(wrapper.getPropertyValue("baselineBatch")).getPropertyValue("id"));
        assertEquals(7002L, new BeanWrapperImpl(wrapper.getPropertyValue("targetBatch")).getPropertyValue("id"));
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
        value.setModelName("裂缝监测值");
        ProductModel sensorState = new ProductModel();
        sensorState.setId(3002L);
        sensorState.setProductId(1001L);
        sensorState.setModelType("property");
        sensorState.setIdentifier("sensor_state");
        when(productModelMapper.selectList(any())).thenReturn(List.of(value, sensorState));
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-p1");
        product.setMetadataJson("""
                {"objectInsight":{"customMetrics":[
                  {"identifier":"value","group":"measure","enabled":true,"includeInTrend":true}
                ]}}
                """);
        product.setProductName("裂缝产品");
        product.setMetadataJson("""
                {"objectInsight":{"customMetrics":[
                  {"identifier":"value","group":"measure","enabled":true,"includeInTrend":true}
                ]}}
                """);
        when(productMapper.selectById(any())).thenReturn(product);

        product.setMetadataJson("""
                {"objectInsight":{"customMetrics":[
                  {"identifier":"value","group":"measure","enabled":true,"includeInTrend":true}
                ]}}
                """);
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
    void getCoverageOverviewShouldExposePublishableContractPropertyCount() {
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

        ProductModel value = new ProductModel();
        value.setId(3001L);
        value.setProductId(1001L);
        value.setModelType("property");
        value.setIdentifier("value");
        value.setModelName("裂缝监测值");
        ProductModel sensorState = new ProductModel();
        sensorState.setId(3002L);
        sensorState.setProductId(1001L);
        sensorState.setModelType("property");
        sensorState.setIdentifier("sensor_state");
        when(productModelMapper.selectList(any())).thenReturn(List.of(value, sensorState));
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-p1");
        product.setProductName("裂缝产品");
        product.setMetadataJson("""
                {"objectInsight":{"customMetrics":[
                  {"identifier":"value","group":"measure","enabled":true,"includeInTrend":true}
                ]}}
                """);
        when(productMapper.selectById(any())).thenReturn(product);

        RiskMetricCatalog metricValue = new RiskMetricCatalog();
        metricValue.setId(9101L);
        metricValue.setProductId(1001L);
        metricValue.setContractIdentifier("value");
        metricValue.setEnabled(1);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(metricValue));
        when(deviceMapper.selectList(any())).thenReturn(List.of());
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(linkageBindingMapper.selectList(any())).thenReturn(List.of());
        when(emergencyPlanBindingMapper.selectList(any())).thenReturn(List.of());

        RiskGovernanceCoverageOverviewVO overview = service.getCoverageOverview(1001L);

        BeanWrapperImpl wrapper = new BeanWrapperImpl(overview);
        assertEquals(1L, wrapper.getPropertyValue("publishableContractPropertyCount"));
        assertEquals(2L, overview.getContractPropertyCount());
        assertEquals(1L, overview.getPublishedRiskMetricCount());
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
    void listMissingPoliciesShouldTreatProductDefaultRuleAsCovered() {
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

        RiskPointDevice coveredByProductDefault = binding(8001L, 5001L, 9101L, "value", "裂缝值");
        coveredByProductDefault.setId(7001L);
        RiskPointDevice missing = binding(8002L, 5002L, 9101L, "value", "裂缝值");
        missing.setId(7002L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(coveredByProductDefault, missing));

        Device productDevice = new Device();
        productDevice.setId(8001L);
        productDevice.setProductId(1001L);
        Device otherProductDevice = new Device();
        otherProductDevice.setId(8002L);
        otherProductDevice.setProductId(1002L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(productDevice, otherProductDevice));
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("crack-v1");
        product.setProductName("裂缝监测仪");
        Product otherProduct = new Product();
        otherProduct.setId(1002L);
        otherProduct.setProductKey("gnss-v1");
        otherProduct.setProductName("GNSS监测仪");
        when(productMapper.selectList(any())).thenReturn(List.of(product, otherProduct));

        RuleDefinition productDefault = rule(6001L, 9101L, "value");
        productDefault.setRuleScope("PRODUCT");
        productDefault.setProductId(1001L);
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(productDefault));

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingPolicies(null);

        assertEquals(1L, result.getTotal());
        assertEquals(8002L, result.getRecords().get(0).getDeviceId());
        assertEquals(1002L, result.getRecords().get(0).getProductId());
        assertEquals("gnss-v1", result.getRecords().get(0).getProductKey());
        assertEquals("GNSS监测仪", result.getRecords().get(0).getProductName());
    }

    @Test
    void listMissingPoliciesShouldTreatProductTypeDefaultRuleAsCoveredOnlyForMatchingType() {
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

        RiskPointDevice monitoringBinding = binding(8001L, 5001L, 9101L, "value", "monitoring value");
        monitoringBinding.setId(7001L);
        RiskPointDevice videoBinding = binding(8002L, 5002L, 9101L, "value", "monitoring value");
        videoBinding.setId(7002L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(monitoringBinding, videoBinding));

        Device monitoringDevice = new Device();
        monitoringDevice.setId(8001L);
        monitoringDevice.setProductId(1001L);
        Device videoDevice = new Device();
        videoDevice.setId(8002L);
        videoDevice.setProductId(1002L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(monitoringDevice, videoDevice));

        Product monitoringProduct = new Product();
        monitoringProduct.setId(1001L);
        monitoringProduct.setProductKey("monitoring-crack-v1");
        monitoringProduct.setProductName("Monitoring Crack");
        Product videoProduct = new Product();
        videoProduct.setId(1002L);
        videoProduct.setProductKey("camera-v1");
        videoProduct.setProductName("Video Camera");
        when(productMapper.selectList(any())).thenReturn(List.of(monitoringProduct, videoProduct), List.of(videoProduct));

        RuleDefinition monitoringDefault = rule(6001L, 9101L, "value");
        monitoringDefault.setRuleScope("PRODUCT_TYPE");
        monitoringDefault.setProductType("MONITORING");
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(monitoringDefault));

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingPolicies(null);

        assertEquals(1L, result.getTotal());
        assertEquals(8002L, result.getRecords().get(0).getDeviceId());
        assertEquals(1002L, result.getRecords().get(0).getProductId());
        assertEquals("camera-v1", result.getRecords().get(0).getProductKey());
    }

    @Test
    void listMissingPoliciesShouldFilterByProductId() {
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

        RiskPointDevice crackBinding = binding(8001L, 5001L, 9101L, "value", "裂缝值");
        crackBinding.setId(7001L);
        RiskPointDevice gnssBinding = binding(8002L, 5002L, 9102L, "gpsTotalX", "X轴累计位移");
        gnssBinding.setId(7002L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(crackBinding, gnssBinding));

        Device crackDevice = new Device();
        crackDevice.setId(8001L);
        crackDevice.setProductId(1001L);
        Device gnssDevice = new Device();
        gnssDevice.setId(8002L);
        gnssDevice.setProductId(1002L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(crackDevice, gnssDevice));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("crack-v1");
        product.setProductName("裂缝监测仪");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        query.setProductId(1001L);

        PageResult<RiskGovernanceGapItemVO> result = service.listMissingPolicies(query);

        assertEquals(1L, result.getTotal());
        assertEquals(8001L, result.getRecords().get(0).getDeviceId());
        assertEquals(1001L, result.getRecords().get(0).getProductId());
        assertEquals("裂缝监测仪", result.getRecords().get(0).getProductName());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldAggregateByProductAndMetric() {
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

        RiskPointDevice crackA = binding(8001L, 5001L, 9101L, "value", "裂缝值");
        crackA.setId(7001L);
        RiskPointDevice crackB = binding(8002L, 5002L, 9101L, "value", "裂缝值");
        crackB.setId(7002L);
        RiskPointDevice gnss = binding(8101L, 5101L, 9201L, "gpsTotalX", "X轴累计位移");
        gnss.setId(7101L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(crackA, crackB, gnss));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());

        Device crackDeviceA = new Device();
        crackDeviceA.setId(8001L);
        crackDeviceA.setProductId(1001L);
        Device crackDeviceB = new Device();
        crackDeviceB.setId(8002L);
        crackDeviceB.setProductId(1001L);
        Device gnssDevice = new Device();
        gnssDevice.setId(8101L);
        gnssDevice.setProductId(1002L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(crackDeviceA, crackDeviceB, gnssDevice));

        Product crackProduct = new Product();
        crackProduct.setId(1001L);
        crackProduct.setProductKey("crack-v1");
        crackProduct.setProductName("裂缝监测仪");
        Product gnssProduct = new Product();
        gnssProduct.setId(1002L);
        gnssProduct.setProductKey("gnss-v1");
        gnssProduct.setProductName("GNSS监测仪");
        when(productMapper.selectList(any())).thenReturn(List.of(crackProduct, gnssProduct));

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(null);

        assertEquals(2L, result.getTotal());
        RiskGovernanceMissingPolicyProductMetricSummaryVO first = result.getRecords().get(0);
        assertEquals(1001L, first.getProductId());
        assertEquals("crack-v1", first.getProductKey());
        assertEquals("value", first.getMetricIdentifier());
        assertEquals(2L, first.getBindingCount());
        assertEquals(2L, first.getRiskPointCount());
        assertEquals(2L, first.getDeviceCount());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldTreatLeafMetricRuleAsCovered() {
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

        RiskPointDevice binding = binding(8001L, 5001L, null, "L1_JS_1.gX", "X axis acceleration");
        binding.setId(7001L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));

        RuleDefinition rule = new RuleDefinition();
        rule.setId(6001L);
        rule.setStatus(0);
        rule.setMetricIdentifier("gX");
        rule.setRuleScope("PRODUCT_TYPE");
        rule.setProductType("MONITORING");
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        Device device = new Device();
        device.setId(8001L);
        device.setProductId(1001L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("nf-monitor-gnss-monitor-v1");
        product.setProductName("GNSS monitor");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(null);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldIncludeRecommendedThreshold() {
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
        service.setThresholdPolicyRecommendationService(thresholdPolicyRecommendationService);

        RiskPointDevice crack = binding(8001L, 5001L, 9101L, "value", "monitoring value");
        crack.setId(7001L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(crack));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());

        Device device = new Device();
        device.setId(8001L);
        device.setProductId(1001L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(device));

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("monitoring-crack-v1");
        product.setProductName("Monitoring Crack");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        when(thresholdPolicyRecommendationService.recommend(product, "value", Set.of(8001L)))
                .thenReturn(new ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation(
                        15,
                        120L,
                        new BigDecimal("1.20"),
                        new BigDecimal("10.00"),
                        new BigDecimal("4.60"),
                        "value >= 12",
                        null,
                        "value >= 12",
                        "READY",
                        "UPPER",
                        "最近15天样本 120 条，按最大值 1.2 倍提炼"
                ));

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(null);

        RiskGovernanceMissingPolicyProductMetricSummaryVO first = result.getRecords().get(0);
        assertEquals("value >= 12", first.getRecommendedExpression());
        assertEquals(15, first.getRecommendationWindowDays());
        assertEquals(120L, first.getRecommendationSampleCount());
        assertEquals("READY", first.getRecommendationStatus());
        assertEquals("UPPER", first.getRecommendationDirection());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldFallbackToCatalogProductForRecommendation() {
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
        service.setThresholdPolicyRecommendationService(thresholdPolicyRecommendationService);

        RiskPointDevice binding = binding(8801L, 5801L, 9301L, "dispsY", "slope displacement");
        binding.setId(7301L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(9301L);
        catalog.setProductId(1003L);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(catalog));

        Product product = new Product();
        product.setId(1003L);
        product.setProductKey("monitoring-deep-displacement-v1");
        product.setProductName("Deep Displacement");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        when(thresholdPolicyRecommendationService.recommend(product, "dispsY", Set.of(8801L)))
                .thenReturn(new ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation(
                        null,
                        2L,
                        new BigDecimal("6.50"),
                        new BigDecimal("8.00"),
                        new BigDecimal("7.25"),
                        "value >= 9.6",
                        null,
                        "value >= 9.6",
                        "LATEST_PROPERTY_SUGGESTED",
                        "UPPER_ONLY",
                        "catalog product fallback"
                ));

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(null);

        RiskGovernanceMissingPolicyProductMetricSummaryVO first = result.getRecords().get(0);
        assertEquals(1003L, first.getProductId());
        assertEquals("monitoring-deep-displacement-v1", first.getProductKey());
        assertEquals("value >= 9.6", first.getRecommendedExpression());
        assertEquals("LATEST_PROPERTY_SUGGESTED", first.getRecommendationStatus());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldUseCatalogProductFallbackForProductScopedRules() {
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

        RiskPointDevice binding = binding(8801L, 5801L, 9301L, "dispsY", "slope displacement");
        binding.setId(7301L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(binding));
        when(deviceMapper.selectList(any())).thenReturn(List.of());

        RiskMetricCatalog catalog = new RiskMetricCatalog();
        catalog.setId(9301L);
        catalog.setProductId(1003L);
        when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(catalog));

        RuleDefinition rule = new RuleDefinition();
        rule.setId(6001L);
        rule.setStatus(0);
        rule.setMetricIdentifier("dispsY");
        rule.setRuleScope("PRODUCT");
        rule.setProductId(1003L);
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of(rule));

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(null);

        assertEquals(0L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void pageMissingPolicyProductMetricSummariesShouldRecommendOnlyPagedRows() {
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
        service.setThresholdPolicyRecommendationService(thresholdPolicyRecommendationService);

        RiskPointDevice firstA = binding(8001L, 5001L, 9101L, "value", "monitoring value");
        firstA.setId(7001L);
        RiskPointDevice firstB = binding(8002L, 5002L, 9101L, "value", "monitoring value");
        firstB.setId(7002L);
        RiskPointDevice second = binding(8003L, 5003L, 9102L, "gpsTotalX", "x total");
        second.setId(7003L);
        when(riskPointDeviceMapper.selectList(any())).thenReturn(List.of(firstA, firstB, second));
        when(ruleDefinitionMapper.selectList(any())).thenReturn(List.of());

        Device device1 = new Device();
        device1.setId(8001L);
        device1.setProductId(1001L);
        Device device2 = new Device();
        device2.setId(8002L);
        device2.setProductId(1001L);
        Device device3 = new Device();
        device3.setId(8003L);
        device3.setProductId(1001L);
        when(deviceMapper.selectList(any())).thenReturn(List.of(device1, device2, device3));

        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("monitoring-crack-v1");
        product.setProductName("Monitoring Crack");
        when(productMapper.selectList(any())).thenReturn(List.of(product));

        when(thresholdPolicyRecommendationService.recommend(product, "value", Set.of(8001L, 8002L)))
                .thenReturn(new ThresholdPolicyRecommendationService.ThresholdPolicyRecommendation(
                        15,
                        12L,
                        new BigDecimal("1.20"),
                        new BigDecimal("10.00"),
                        new BigDecimal("4.60"),
                        "value >= 12",
                        null,
                        "value >= 12",
                        "READY",
                        "UPPER",
                        "page scoped recommendation"
                ));

        RiskGovernanceGapQuery query = new RiskGovernanceGapQuery();
        query.setPageNum(1L);
        query.setPageSize(1L);

        PageResult<RiskGovernanceMissingPolicyProductMetricSummaryVO> result =
                service.pageMissingPolicyProductMetricSummaries(query);

        assertEquals(2L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("value", result.getRecords().get(0).getMetricIdentifier());
        assertEquals("value >= 12", result.getRecords().get(0).getRecommendedExpression());
        verify(thresholdPolicyRecommendationService).recommend(product, "value", Set.of(8001L, 8002L));
        verifyNoMoreInteractions(thresholdPolicyRecommendationService);
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
