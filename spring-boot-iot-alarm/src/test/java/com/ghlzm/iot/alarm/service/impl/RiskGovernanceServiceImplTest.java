package com.ghlzm.iot.alarm.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.alarm.entity.RiskPointDevice;
import com.ghlzm.iot.alarm.entity.RuleDefinition;
import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointDeviceMapper;
import com.ghlzm.iot.alarm.mapper.RiskPointMapper;
import com.ghlzm.iot.alarm.mapper.RuleDefinitionMapper;
import com.ghlzm.iot.alarm.vo.RiskGovernanceCoverageOverviewVO;
import com.ghlzm.iot.alarm.vo.RiskMetricCatalogItemVO;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.entity.Device;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void pageMetricCatalogsShouldReturnProductScopedRows() {
        RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
                deviceMapper,
                riskPointMapper,
                riskPointDeviceMapper,
                ruleDefinitionMapper,
                riskMetricCatalogMapper,
                productModelMapper
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
                productModelMapper
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
}
