package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.DeviceTelemetryMappingService;
import com.ghlzm.iot.device.service.model.TelemetryMetricMapping;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品物模型 legacy 遥测映射读取实现。
 */
@Service
public class DeviceTelemetryMappingServiceImpl implements DeviceTelemetryMappingService {

    static final String SOURCE_PRODUCT_SPECS_TDENGINE_LEGACY = "PRODUCT_SPECS_TDENGINE_LEGACY";
    static final String REASON_MISSING_MAPPING = "MISSING_TDENGINE_LEGACY_MAPPING";
    static final String REASON_INVALID_SPECS_JSON = "INVALID_SPECS_JSON";
    static final String REASON_DISABLED = "DISABLED";
    static final String REASON_MISSING_STABLE = "MISSING_STABLE";
    static final String REASON_INVALID_STABLE = "INVALID_STABLE";
    static final String REASON_MISSING_COLUMN = "MISSING_COLUMN";
    static final String REASON_INVALID_COLUMN = "INVALID_COLUMN";

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ProductModelMapper productModelMapper;

    public DeviceTelemetryMappingServiceImpl(ProductModelMapper productModelMapper) {
        this.productModelMapper = productModelMapper;
    }

    @Override
    public Map<String, TelemetryMetricMapping> listMetricMappings(Long productId) {
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
            mappingMap.put(productModel.getIdentifier(), resolveMetricMapping(productModel.getIdentifier(), productModel.getSpecsJson()));
        }
        return mappingMap;
    }

    private TelemetryMetricMapping resolveMetricMapping(String metricCode, String specsJson) {
        TelemetryMetricMapping mapping = new TelemetryMetricMapping();
        mapping.setMetricCode(metricCode);
        mapping.setSource(SOURCE_PRODUCT_SPECS_TDENGINE_LEGACY);
        if (specsJson == null || specsJson.isBlank()) {
            mapping.setReason(REASON_MISSING_MAPPING);
            return mapping;
        }
        try {
            JsonNode root = objectMapper.readTree(specsJson);
            JsonNode mappingNode = root == null ? null : root.get("tdengineLegacy");
            if (mappingNode == null || !mappingNode.isObject()) {
                mapping.setReason(REASON_MISSING_MAPPING);
                return mapping;
            }

            if (mappingNode.has("enabled")) {
                mapping.setEnabled(mappingNode.path("enabled").asBoolean(true));
            }
            String rawStable = normalizeText(mappingNode.path("stable").asText(null));
            String rawColumn = normalizeText(mappingNode.path("column").asText(null));
            String stable = normalizeIdentifier(rawStable);
            String column = normalizeIdentifier(rawColumn);
            mapping.setStable(stable);
            mapping.setColumn(column);
            if (Boolean.FALSE.equals(mapping.getEnabled())) {
                mapping.setReason(REASON_DISABLED);
                return mapping;
            }
            if (rawStable == null) {
                mapping.setReason(REASON_MISSING_STABLE);
                return mapping;
            }
            if (stable == null) {
                mapping.setReason(REASON_INVALID_STABLE);
                return mapping;
            }
            if (rawColumn == null) {
                mapping.setReason(REASON_MISSING_COLUMN);
                return mapping;
            }
            if (column == null) {
                mapping.setReason(REASON_INVALID_COLUMN);
                return mapping;
            }
            return mapping;
        } catch (Exception ex) {
            mapping.setReason(REASON_INVALID_SPECS_JSON);
            return mapping;
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        return value.matches("[A-Za-z_][A-Za-z0-9_]*") ? value : null;
    }
}
