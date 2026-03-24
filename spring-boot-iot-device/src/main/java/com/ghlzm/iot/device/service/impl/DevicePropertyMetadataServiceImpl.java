package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品物模型属性元数据解析服务实现。
 */
@Service
public class DevicePropertyMetadataServiceImpl implements DevicePropertyMetadataService {

    private final ProductModelMapper productModelMapper;
    private final DeviceTelemetryMappingService deviceTelemetryMappingService;

    public DevicePropertyMetadataServiceImpl(ProductModelMapper productModelMapper,
                                             DeviceTelemetryMappingService deviceTelemetryMappingService) {
        this.productModelMapper = productModelMapper;
        this.deviceTelemetryMappingService = deviceTelemetryMappingService;
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
        Map<String, TelemetryMetricMapping> metricMappings = deviceTelemetryMappingService.listMetricMappings(productId);
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            if (productModel.getIdentifier() == null || productModel.getIdentifier().isBlank()) {
                continue;
            }
            DevicePropertyMetadata metadata = new DevicePropertyMetadata();
            metadata.setIdentifier(productModel.getIdentifier());
            metadata.setPropertyName(productModel.getModelName());
            metadata.setDataType(productModel.getDataType());
            metadata.setTdengineLegacyMapping(toLegacyMapping(metricMappings.get(metadata.getIdentifier())));
            metadataMap.put(metadata.getIdentifier(), metadata);
        }
        return metadataMap;
    }

    private DevicePropertyMetadata.TdengineLegacyMapping toLegacyMapping(TelemetryMetricMapping metricMapping) {
        if (metricMapping == null || metricMapping.getStable() == null || metricMapping.getColumn() == null) {
            return null;
        }
        DevicePropertyMetadata.TdengineLegacyMapping mapping = new DevicePropertyMetadata.TdengineLegacyMapping();
        mapping.setEnabled(metricMapping.getEnabled());
        mapping.setStable(metricMapping.getStable());
        mapping.setColumn(metricMapping.getColumn());
        return mapping;
    }
}
