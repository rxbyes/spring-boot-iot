package com.ghlzm.iot.alarm.service.impl;

import com.ghlzm.iot.alarm.mapper.RiskMetricCatalogMapper;
import com.ghlzm.iot.device.entity.ProductModel;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskMetricCatalogServiceImplTest {

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Test
    void publishFromReleasedContractShouldCreateCrackRiskMetricRows() {
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(riskMetricCatalogMapper);

        ProductModel releasedValue = new ProductModel();
        releasedValue.setId(3101L);
        releasedValue.setProductId(1001L);
        releasedValue.setIdentifier("value");
        releasedValue.setModelName("裂缝监测值");

        ProductModel releasedSensorState = new ProductModel();
        releasedSensorState.setId(3102L);
        releasedSensorState.setProductId(1001L);
        releasedSensorState.setIdentifier("sensor_state");
        releasedSensorState.setModelName("传感器状态");

        service.publishFromReleasedContracts(1001L, List.of(releasedValue, releasedSensorState), Set.of("value"));

        verify(riskMetricCatalogMapper).insert(org.mockito.ArgumentMatchers.<com.ghlzm.iot.alarm.entity.RiskMetricCatalog>argThat(row ->
                Long.valueOf(1001L).equals(row.getProductId())
                        && "value".equals(row.getContractIdentifier())
                        && "裂缝监测值".equals(row.getRiskMetricName())
                        && Integer.valueOf(1).equals(row.getEnabled())
        ));
        verify(riskMetricCatalogMapper, never()).insert(org.mockito.ArgumentMatchers.<com.ghlzm.iot.alarm.entity.RiskMetricCatalog>argThat(
                row -> "sensor_state".equals(row.getContractIdentifier())
        ));
    }

    @Test
    void publishFromReleasedContractsShouldIgnoreGpsInitialButPublishGnssTotals() {
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(riskMetricCatalogMapper);

        ProductModel gpsInitial = new ProductModel();
        gpsInitial.setId(3201L);
        gpsInitial.setProductId(3003L);
        gpsInitial.setIdentifier("gpsInitial");
        gpsInitial.setModelName("GNSS 原始观测基础数据");

        ProductModel gpsTotalX = new ProductModel();
        gpsTotalX.setId(3202L);
        gpsTotalX.setProductId(3003L);
        gpsTotalX.setIdentifier("gpsTotalX");
        gpsTotalX.setModelName("GNSS 累计位移 X");

        service.publishFromReleasedContracts(3003L, List.of(gpsInitial, gpsTotalX), Set.of("gpsTotalX"));

        verify(riskMetricCatalogMapper).insert(org.mockito.ArgumentMatchers.<com.ghlzm.iot.alarm.entity.RiskMetricCatalog>argThat(
                row -> "gpsTotalX".equals(row.getContractIdentifier())
                        && "GNSS 累计位移 X".equals(row.getRiskMetricName())
        ));
        verify(riskMetricCatalogMapper, never()).insert(org.mockito.ArgumentMatchers.<com.ghlzm.iot.alarm.entity.RiskMetricCatalog>argThat(
                row -> "gpsInitial".equals(row.getContractIdentifier())
        ));
    }
}
