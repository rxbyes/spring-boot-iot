package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.alarm.service.spi.KeywordRiskMetricScenarioResolver;
import com.ghlzm.iot.alarm.service.spi.RiskMetricScenarioResolver;
import com.ghlzm.iot.alarm.entity.RiskMetricCatalog;
import com.ghlzm.iot.device.entity.NormativeMetricDefinition;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.NormativeMetricDefinitionService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskMetricCatalogServiceImplTest {

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private NormativeMetricDefinitionService normativeMetricDefinitionService;

    @Test
    void publishFromReleasedContractShouldPersistSemanticMetadataForRiskEnabledMetric() {
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(
                riskMetricCatalogMapper,
                productMapper,
                normativeMetricDefinitionService,
                List.of(new KeywordRiskMetricScenarioResolver())
        );
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("phase1-crack-product");
        when(productMapper.selectById(1001L)).thenReturn(product);
        when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
                normative("phase1-crack", "value", "mm", 1,
                        "{\"thresholdKind\":\"absolute\",\"gisEnabled\":false,\"riskCategory\":\"CRACK\",\"metricRole\":\"PRIMARY\"}")
        ));

        ProductModel releasedValue = new ProductModel();
        releasedValue.setId(3101L);
        releasedValue.setProductId(1001L);
        releasedValue.setIdentifier("value");
        releasedValue.setModelName("Crack value");
        releasedValue.setDataType("double");
        releasedValue.setSpecsJson("{\"dimension\":\"displacement\"}");

        ProductModel releasedSensorState = new ProductModel();
        releasedSensorState.setId(3102L);
        releasedSensorState.setProductId(1001L);
        releasedSensorState.setIdentifier("sensor_state");
        releasedSensorState.setModelName("Sensor status");

        service.publishFromReleasedContracts(1001L, 7001L, List.of(releasedValue, releasedSensorState), Set.of("value"));

        verify(riskMetricCatalogMapper).insert(argThat((RiskMetricCatalog row) ->
                Long.valueOf(1001L).equals(row.getProductId())
                        && Long.valueOf(7001L).equals(row.getReleaseBatchId())
                        && Long.valueOf(3101L).equals(row.getProductModelId())
                        && "value".equals(row.getNormativeIdentifier())
                        && "value".equals(row.getContractIdentifier())
                        && "RM_1001_VALUE".equals(row.getRiskMetricCode())
                        && "Crack value".equals(row.getRiskMetricName())
                        && "phase1-crack".equals(row.getSourceScenarioCode())
                        && "CRACK".equals(row.getRiskCategory())
                        && "PRIMARY".equals(row.getMetricRole())
                        && "ACTIVE".equals(row.getLifecycleStatus())
                        && "mm".equals(row.getMetricUnit())
                        && "displacement".equals(row.getMetricDimension())
                        && "absolute".equals(row.getThresholdType())
                        && Integer.valueOf(1).equals(row.getTrendEnabled())
                        && Integer.valueOf(0).equals(row.getGisEnabled())
                        && Integer.valueOf(1).equals(row.getInsightEnabled())
                        && Integer.valueOf(1).equals(row.getAnalyticsEnabled())
                        && Integer.valueOf(1).equals(row.getEnabled())
        ));
        verify(riskMetricCatalogMapper, never()).insert(argThat(
                (RiskMetricCatalog row) -> "sensor_state".equals(row.getContractIdentifier())
        ));
    }

    @Test
    void publishFromReleasedContractsShouldIgnoreGpsInitialButPublishGnssTotals() {
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(
                riskMetricCatalogMapper,
                productMapper,
                normativeMetricDefinitionService,
                List.of(new KeywordRiskMetricScenarioResolver())
        );
        Product product = new Product();
        product.setId(3003L);
        product.setProductKey("gnss-monitor-v1");
        when(productMapper.selectById(3003L)).thenReturn(product);
        when(normativeMetricDefinitionService.listByScenario("phase2-gnss")).thenReturn(List.of(
                normative("phase2-gnss", "gpsTotalX", "mm", 1, "{\"thresholdKind\":\"absolute\"}")
        ));

        ProductModel gpsInitial = new ProductModel();
        gpsInitial.setId(3201L);
        gpsInitial.setProductId(3003L);
        gpsInitial.setIdentifier("gpsInitial");
        gpsInitial.setModelName("GNSS initial value");

        ProductModel gpsTotalX = new ProductModel();
        gpsTotalX.setId(3202L);
        gpsTotalX.setProductId(3003L);
        gpsTotalX.setIdentifier("gpsTotalX");
        gpsTotalX.setModelName("GNSS total X");
        gpsTotalX.setDataType("double");

        service.publishFromReleasedContracts(3003L, 7002L, List.of(gpsInitial, gpsTotalX), Set.of("gpsTotalX"));

        verify(riskMetricCatalogMapper).insert(argThat(
                (RiskMetricCatalog row) -> Long.valueOf(7002L).equals(row.getReleaseBatchId())
                        && "gpsTotalX".equals(row.getNormativeIdentifier())
                        && "gpsTotalX".equals(row.getContractIdentifier())
                        && "GNSS total X".equals(row.getRiskMetricName())
                        && "phase2-gnss".equals(row.getSourceScenarioCode())
                        && "absolute".equals(row.getThresholdType())
        ));
        verify(riskMetricCatalogMapper, never()).insert(argThat(
                (RiskMetricCatalog row) -> "gpsInitial".equals(row.getContractIdentifier())
        ));
    }

    @Test
    void publishFromReleasedContractsShouldUseCustomScenarioResolverBeforeKeywordResolver() {
        RiskMetricScenarioResolver customResolver = product -> "custom-scene";
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(
                riskMetricCatalogMapper,
                productMapper,
                normativeMetricDefinitionService,
                List.of(customResolver, new KeywordRiskMetricScenarioResolver())
        );
        Product product = new Product();
        product.setId(4004L);
        product.setProductKey("gnss-monitor-v2");
        when(productMapper.selectById(4004L)).thenReturn(product);
        when(normativeMetricDefinitionService.listByScenario("custom-scene")).thenReturn(List.of(
                normative("custom-scene", "value", "cm", 1, "{\"thresholdKind\":\"ratio\"}")
        ));

        ProductModel value = new ProductModel();
        value.setId(3301L);
        value.setProductId(4004L);
        value.setIdentifier("value");
        value.setModelName("Custom metric");
        value.setDataType("double");

        service.publishFromReleasedContracts(4004L, 7003L, List.of(value), Set.of("value"));

        verify(normativeMetricDefinitionService).listByScenario("custom-scene");
        verify(normativeMetricDefinitionService, never()).listByScenario("phase2-gnss");
        verify(riskMetricCatalogMapper).insert(argThat((RiskMetricCatalog row) ->
                "custom-scene".equals(row.getSourceScenarioCode())
                        && Long.valueOf(7003L).equals(row.getReleaseBatchId())
                        && "value".equals(row.getNormativeIdentifier())
                        && "ratio".equals(row.getThresholdType())
                        && "cm".equals(row.getMetricUnit())
        ));
    }

    private NormativeMetricDefinition normative(String scenarioCode,
                                                String identifier,
                                                String unit,
                                                Integer trendEnabled,
                                                String metadataJson) {
        NormativeMetricDefinition definition = new NormativeMetricDefinition();
        definition.setScenarioCode(scenarioCode);
        definition.setIdentifier(identifier);
        definition.setUnit(unit);
        definition.setTrendEnabled(trendEnabled);
        definition.setMetadataJson(metadataJson);
        return definition;
    }
}
