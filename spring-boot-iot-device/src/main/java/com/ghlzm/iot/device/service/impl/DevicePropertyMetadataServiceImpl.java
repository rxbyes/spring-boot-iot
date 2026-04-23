package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.MetricIdentifierResolver;
import com.ghlzm.iot.device.service.PublishedProductContractSnapshotService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.MetricIdentifierResolution;
import com.ghlzm.iot.device.service.model.PublishedProductContractSnapshot;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品物模型属性元数据解析服务实现。
 */
@Service
public class DevicePropertyMetadataServiceImpl implements DevicePropertyMetadataService {

    private final ProductModelMapper productModelMapper;
    private final PublishedProductContractSnapshotService snapshotService;
    private final MetricIdentifierResolver metricIdentifierResolver;
    private final DeviceTelemetryMappingResolver telemetryMappingResolver = new DeviceTelemetryMappingResolver();
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Autowired
    public DevicePropertyMetadataServiceImpl(ProductModelMapper productModelMapper,
                                             PublishedProductContractSnapshotService snapshotService,
                                             MetricIdentifierResolver metricIdentifierResolver) {
        this.productModelMapper = productModelMapper;
        this.snapshotService = snapshotService;
        this.metricIdentifierResolver = metricIdentifierResolver;
    }

    public DevicePropertyMetadataServiceImpl(ProductModelMapper productModelMapper) {
        this(
                productModelMapper,
                new PublishedProductContractSnapshotServiceImpl(productModelMapper, null),
                new DefaultMetricIdentifierResolver()
        );
    }

    @Override
    public Map<String, DevicePropertyMetadata> listPropertyMetadataMap(Long productId) {
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
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
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
            DevicePropertyMetadata metadata = new DevicePropertyMetadata();
            metadata.setIdentifier(canonicalIdentifier);
            metadata.setPropertyName(productModel.getModelName());
            metadata.setDataType(productModel.getDataType());
            metadata.setUnit(parseUnit(productModel.getSpecsJson()));
            TelemetryMetricMapping telemetryMetricMapping =
                    telemetryMappingResolver.resolve(canonicalIdentifier, productModel.getSpecsJson());
            metadata.setTdengineLegacyMapping(telemetryMappingResolver.toLegacyMapping(telemetryMetricMapping));
            metadataMap.putIfAbsent(metadata.getIdentifier(), metadata);
        }
        return metadataMap;
    }

    private String parseUnit(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode specs = objectMapper.readTree(specsJson);
            JsonNode unitNode = specs == null ? null : specs.get("unit");
            if (unitNode == null || unitNode.isNull()) {
                return null;
            }
            String unit = unitNode.asText();
            return unit == null || unit.isBlank() ? null : unit.trim();
        } catch (Exception ignored) {
            return null;
        }
    }
}
