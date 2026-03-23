package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
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

    public DevicePropertyMetadataServiceImpl(ProductModelMapper productModelMapper) {
        this.productModelMapper = productModelMapper;
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
        Map<String, DevicePropertyMetadata> metadataMap = new LinkedHashMap<>();
        for (ProductModel productModel : productModels) {
            if (productModel.getIdentifier() == null || productModel.getIdentifier().isBlank()) {
                continue;
            }
            DevicePropertyMetadata metadata = new DevicePropertyMetadata();
            metadata.setIdentifier(productModel.getIdentifier());
            metadata.setPropertyName(productModel.getModelName());
            metadata.setDataType(productModel.getDataType());
            metadataMap.put(metadata.getIdentifier(), metadata);
        }
        return metadataMap;
    }
}
