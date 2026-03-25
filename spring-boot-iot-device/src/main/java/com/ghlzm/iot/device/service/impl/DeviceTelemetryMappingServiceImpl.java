package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
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
    private final DeviceTelemetryMappingResolver telemetryMappingResolver = new DeviceTelemetryMappingResolver();

    public DeviceTelemetryMappingServiceImpl(ProductModelMapper productModelMapper) {
        this.productModelMapper = productModelMapper;
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
        Map<String, TelemetryMetricMapping> mappingMap = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            if (productModel.getIdentifier() == null || productModel.getIdentifier().isBlank()) {
                continue;
            }
            mappingMap.put(
                    productModel.getIdentifier(),
                    telemetryMappingResolver.resolve(productModel.getIdentifier(), productModel.getSpecsJson())
            );
        }
        return mappingMap;
    }
}
