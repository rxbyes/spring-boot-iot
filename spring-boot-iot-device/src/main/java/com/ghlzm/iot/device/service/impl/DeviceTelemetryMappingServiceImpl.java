package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备遥测映射读取服务实现。
 */
@Service
public class DeviceTelemetryMappingServiceImpl implements DeviceTelemetryMappingService {

    private final ProductModelMapper productModelMapper;
    private final PublishedProductContractSnapshotService snapshotService;
    private final MetricIdentifierResolver metricIdentifierResolver;
    private final DeviceTelemetryMappingResolver telemetryMappingResolver = new DeviceTelemetryMappingResolver();

    @Autowired
    public DeviceTelemetryMappingServiceImpl(ProductModelMapper productModelMapper,
                                             PublishedProductContractSnapshotService snapshotService,
                                             MetricIdentifierResolver metricIdentifierResolver) {
        this.productModelMapper = productModelMapper;
        this.snapshotService = snapshotService;
        this.metricIdentifierResolver = metricIdentifierResolver;
    }

    public DeviceTelemetryMappingServiceImpl(ProductModelMapper productModelMapper) {
        this(
                productModelMapper,
                new PublishedProductContractSnapshotServiceImpl(productModelMapper, null),
                new DefaultMetricIdentifierResolver()
        );
    }

    @Override
    public Map<String, TelemetryMetricMapping> listMetricMappingMap(Long productId) {
        if (productId == null) {
            return Map.of();
        }
        List<ProductModel> productModels = productModelMapper.selectList(
                new LambdaQueryWrapper<ProductModel>()
                        .eq(ProductModel::getProductId, productId)
                        .eq(ProductModel::getModelType, "property")
                        .eq(ProductModel::getDeleted, 0)
                        .orderByAsc(ProductModel::getSortNo)
                        .orderByAsc(ProductModel::getIdentifier)
        );
        PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(productId);
        Map<String, TelemetryMetricMapping> mappingMap = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            if (productModel.getIdentifier() == null || productModel.getIdentifier().isBlank()) {
                continue;
            }
            MetricIdentifierResolution resolution =
                    metricIdentifierResolver.resolveForRead(snapshot, productModel.getIdentifier());
            String canonicalIdentifier = resolution.canonicalIdentifier();
            if (canonicalIdentifier == null || canonicalIdentifier.isBlank()) {
                continue;
            }
            TelemetryMetricMapping mapping = telemetryMappingResolver.resolve(canonicalIdentifier, productModel.getSpecsJson());
            mappingMap.put(
                    canonicalIdentifier,
                    mappingMap.containsKey(canonicalIdentifier) ? mappingMap.get(canonicalIdentifier) : mapping
            );
        }
        return mappingMap;
    }
}
