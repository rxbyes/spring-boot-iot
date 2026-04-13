package com.ghlzm.iot.alarm.listener;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogPublishRule;
import com.ghlzm.iot.alarm.service.RiskMetricCatalogService;
import com.ghlzm.iot.common.event.governance.ProductContractReleasedEvent;
import com.ghlzm.iot.device.entity.ProductMetricResolverSnapshot;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMetricResolverSnapshotMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductContractReleasedEventListenerTest {

    @Mock
    private ProductModelMapper productModelMapper;

    @Mock
    private RiskMetricCatalogPublishRule publishRule;

    @Mock
    private RiskMetricCatalogService riskMetricCatalogService;

    @Mock
    private ProductMetricResolverSnapshotMapper resolverSnapshotMapper;

    @BeforeAll
    static void initLambdaCache() {
        if (TableInfoHelper.getTableInfo(ProductModel.class) != null) {
            return;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "");
        assistant.setCurrentNamespace(ProductModel.class.getName());
        LambdaUtils.installCache(TableInfoHelper.initTableInfo(assistant, ProductModel.class));
    }

    @Test
    void onProductContractReleasedShouldPublishMetricCatalogFromReleasedProperties() {
        ProductContractReleasedEventListener listener = new ProductContractReleasedEventListener(
                productModelMapper,
                publishRule,
                riskMetricCatalogService,
                resolverSnapshotMapper
        );
        ProductModel value = propertyModel(3101L, 1001L, "value", "裂缝监测值");
        ProductModel sensorState = propertyModel(3102L, 1001L, "sensor_state", "传感器状态");
        when(productModelMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(value, sensorState));
        when(publishRule.resolveRiskEnabledIdentifiers(null, List.of(value, sensorState))).thenReturn(Set.of("value"));

        listener.onProductContractReleased(new ProductContractReleasedEvent(
                1L,
                1001L,
                7001L,
                "phase1-crack",
                List.of("value", "sensor_state"),
                9001L,
                99001L
        ));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProductModel>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productModelMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<ProductModel> wrapper = wrapperCaptor.getValue();
        wrapper.getSqlSegment();
        assertTrue(wrapper.getParamNameValuePairs().values().contains(1001L));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("property"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("value"));
        assertTrue(wrapper.getParamNameValuePairs().values().contains("sensor_state"));

        verify(riskMetricCatalogService).publishFromReleasedContracts(
                1001L,
                7001L,
                List.of(value, sensorState),
                Set.of("value")
        );
    }

    @Test
    void onProductContractReleasedShouldIgnoreEmptyReleasedIdentifiers() {
        ProductContractReleasedEventListener listener = new ProductContractReleasedEventListener(
                productModelMapper,
                publishRule,
                riskMetricCatalogService,
                resolverSnapshotMapper
        );

        listener.onProductContractReleased(new ProductContractReleasedEvent(
                1L,
                1001L,
                7001L,
                "phase1-crack",
                Arrays.asList(" ", null),
                9001L,
                99001L
        ));

        verify(productModelMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(riskMetricCatalogService, never()).publishFromReleasedContracts(any(), any(), any(), any());
    }

    @Test
    void onProductContractReleasedShouldCompileResolverSnapshotBeforePublishingCatalog() {
        ProductContractReleasedEventListener listener = new ProductContractReleasedEventListener(
                productModelMapper,
                publishRule,
                riskMetricCatalogService,
                resolverSnapshotMapper
        );
        ProductModel value = propertyModel(3101L, 1001L, "L1_LF_1.value", "裂缝监测值");
        when(productModelMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(value));
        when(publishRule.resolveRiskEnabledIdentifiers(any(), any())).thenReturn(Set.of("value"));

        listener.onProductContractReleased(new ProductContractReleasedEvent(
                1L,
                1001L,
                7001L,
                "phase1-crack",
                List.of("L1_LF_1.value"),
                9001L,
                99001L
        ));

        verify(resolverSnapshotMapper).insert(org.mockito.ArgumentMatchers.argThat((ProductMetricResolverSnapshot row) ->
                Long.valueOf(1001L).equals(row.getProductId())
                        && Long.valueOf(7001L).equals(row.getReleaseBatchId())
                        && row.getSnapshotJson() != null
                        && row.getSnapshotJson().contains("\"L1_LF_1.value\"")
                        && row.getSnapshotJson().contains("\"value\"")
        ));
    }

    private ProductModel propertyModel(Long id, Long productId, String identifier, String modelName) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(productId);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(modelName);
        return model;
    }
}
