package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DevicePropertyMetadataService;
import com.ghlzm.iot.device.service.model.DevicePropertyMetadata;
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

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
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
            metadata.setTdengineLegacyMapping(resolveTdengineLegacyMapping(productModel.getSpecsJson()));
            metadataMap.put(metadata.getIdentifier(), metadata);
        }
        return metadataMap;
    }

    private DevicePropertyMetadata.TdengineLegacyMapping resolveTdengineLegacyMapping(String specsJson) {
        if (specsJson == null || specsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(specsJson);
            JsonNode mappingNode = root == null ? null : root.get("tdengineLegacy");
            if (mappingNode == null || !mappingNode.isObject()) {
                return null;
            }
            DevicePropertyMetadata.TdengineLegacyMapping mapping = new DevicePropertyMetadata.TdengineLegacyMapping();
            if (mappingNode.has("enabled")) {
                mapping.setEnabled(mappingNode.path("enabled").asBoolean(true));
            }
            String stable = normalizeIdentifier(mappingNode.path("stable").asText(null));
            String column = normalizeIdentifier(mappingNode.path("column").asText(null));
            if (stable == null || column == null) {
                return null;
            }
            mapping.setStable(stable);
            mapping.setColumn(column);
            return mapping;
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.matches("[A-Za-z_][A-Za-z0-9_]*") ? normalized : null;
    }
}
