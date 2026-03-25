package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 产品物模型服务实现，负责产品维度的模型治理和基础校验。
 */
@Service
public class ProductModelServiceImpl extends ServiceImpl<ProductModelMapper, ProductModel> implements ProductModelService {

    private static final List<String> ALLOWED_MODEL_TYPES = List.of("property", "event", "service");
    private static final String NON_PROPERTY_COMPAT_DATA_TYPE = "json";

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final ProductMapper productMapper;
    private final ProductModelMapper productModelMapper;

    public ProductModelServiceImpl(ProductMapper productMapper, ProductModelMapper productModelMapper) {
        this.productMapper = productMapper;
        this.productModelMapper = productModelMapper;
    }

    @Override
    public List<ProductModelVO> listModels(Long productId) {
        getRequiredProduct(productId);
        return productModelMapper.selectList(
                        new LambdaQueryWrapper<ProductModel>()
                                .eq(ProductModel::getProductId, productId)
                                .eq(ProductModel::getDeleted, 0)
                ).stream()
                .sorted(Comparator
                        .comparing(ProductModel::getSortNo, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(model -> normalizeOptional(model.getIdentifier()), Comparator.nullsLast(String::compareTo)))
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelVO createModel(Long productId, ProductModelUpsertDTO dto) {
        getRequiredProduct(productId);
        String modelType = normalizeModelType(dto.getModelType());
        String identifier = normalizeRequired(dto.getIdentifier(), "物模型标识");
        validateByModelType(modelType, dto);
        ensureIdentifierUnique(productId, identifier, null);

        ProductModel model = new ProductModel();
        model.setProductId(productId);
        applyEditableFields(model, modelType, identifier, dto);
        productModelMapper.insert(model);
        return toVO(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductModelVO updateModel(Long productId, Long modelId, ProductModelUpsertDTO dto) {
        getRequiredProduct(productId);
        ProductModel model = getRequiredModel(productId, modelId);
        String modelType = normalizeModelType(dto.getModelType());
        String identifier = normalizeRequired(dto.getIdentifier(), "物模型标识");
        validateByModelType(modelType, dto);
        ensureIdentifierUnique(productId, identifier, modelId);

        applyEditableFields(model, modelType, identifier, dto);
        productModelMapper.updateById(model);
        return toVO(model);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long productId, Long modelId) {
        getRequiredProduct(productId);
        ProductModel model = getRequiredModel(productId, modelId);
        if (productModelMapper.deleteById(model.getId()) <= 0) {
            throw new BizException("产品物模型删除失败，请稍后重试");
        }
    }

    private Product getRequiredProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || Integer.valueOf(1).equals(product.getDeleted())) {
            throw new BizException("产品不存在: " + productId);
        }
        return product;
    }

    private ProductModel getRequiredModel(Long productId, Long modelId) {
        ProductModel model = productModelMapper.selectById(modelId);
        if (model == null || Integer.valueOf(1).equals(model.getDeleted()) || !productId.equals(model.getProductId())) {
            throw new BizException("产品物模型不存在: " + modelId);
        }
        return model;
    }

    private void ensureIdentifierUnique(Long productId, String identifier, Long excludeId) {
        LambdaQueryWrapper<ProductModel> wrapper = new LambdaQueryWrapper<ProductModel>()
                .eq(ProductModel::getProductId, productId)
                .eq(ProductModel::getIdentifier, identifier)
                .eq(ProductModel::getDeleted, 0);
        if (excludeId != null) {
            wrapper.ne(ProductModel::getId, excludeId);
        }
        ProductModel existing = productModelMapper.selectOne(wrapper);
        if (existing != null) {
            throw new BizException("同一产品下物模型标识已存在: " + identifier);
        }
    }

    private void applyEditableFields(ProductModel model, String modelType, String identifier, ProductModelUpsertDTO dto) {
        model.setModelType(modelType);
        model.setIdentifier(identifier);
        model.setModelName(normalizeRequired(dto.getModelName(), "物模型名称"));
        model.setSortNo(dto.getSortNo());
        model.setRequiredFlag(dto.getRequiredFlag());
        model.setDescription(normalizeOptional(dto.getDescription()));

        if ("property".equals(modelType)) {
            model.setDataType(normalizeRequired(dto.getDataType(), "dataType"));
            model.setSpecsJson(validateJsonField(dto.getSpecsJson(), "specsJson"));
            model.setEventType(null);
            model.setServiceInputJson(null);
            model.setServiceOutputJson(null);
            return;
        }

        if ("event".equals(modelType)) {
            model.setDataType(NON_PROPERTY_COMPAT_DATA_TYPE);
            model.setSpecsJson(null);
            model.setEventType(normalizeRequired(dto.getEventType(), "eventType"));
            model.setServiceInputJson(null);
            model.setServiceOutputJson(null);
            return;
        }

        model.setDataType(NON_PROPERTY_COMPAT_DATA_TYPE);
        model.setSpecsJson(null);
        model.setEventType(null);
        model.setServiceInputJson(validateJsonField(dto.getServiceInputJson(), "serviceInputJson"));
        model.setServiceOutputJson(validateJsonField(dto.getServiceOutputJson(), "serviceOutputJson"));
    }

    private void validateByModelType(String modelType, ProductModelUpsertDTO dto) {
        if ("property".equals(modelType)) {
            if (!StringUtils.hasText(dto.getDataType())) {
                throw new BizException("属性物模型必须填写 dataType");
            }
            if (StringUtils.hasText(dto.getEventType())
                    || StringUtils.hasText(dto.getServiceInputJson())
                    || StringUtils.hasText(dto.getServiceOutputJson())) {
                throw new BizException("属性物模型只允许填写 dataType 和 specsJson");
            }
            validateJsonField(dto.getSpecsJson(), "specsJson");
            return;
        }

        if ("event".equals(modelType)) {
            if (StringUtils.hasText(dto.getDataType())
                    || StringUtils.hasText(dto.getSpecsJson())
                    || StringUtils.hasText(dto.getServiceInputJson())
                    || StringUtils.hasText(dto.getServiceOutputJson())) {
                throw new BizException("事件物模型只允许填写 eventType");
            }
            return;
        }

        if (StringUtils.hasText(dto.getDataType())
                || StringUtils.hasText(dto.getSpecsJson())
                || StringUtils.hasText(dto.getEventType())) {
            throw new BizException("服务物模型只允许填写 serviceInputJson 和 serviceOutputJson");
        }
        validateJsonField(dto.getServiceInputJson(), "serviceInputJson");
        validateJsonField(dto.getServiceOutputJson(), "serviceOutputJson");
    }

    private String validateJsonField(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (Exception ex) {
            throw new BizException(fieldName + " 必须是合法 JSON");
        }
    }

    private String normalizeModelType(String modelType) {
        String normalized = normalizeRequired(modelType, "物模型类型").toLowerCase(Locale.ROOT);
        if (!ALLOWED_MODEL_TYPES.contains(normalized)) {
            throw new BizException("物模型类型不支持: " + normalized);
        }
        return normalized;
    }

    private String normalizeRequired(String value, String label) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BizException(label + "不能为空");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ProductModelVO toVO(ProductModel model) {
        ProductModelVO vo = new ProductModelVO();
        vo.setId(model.getId());
        vo.setProductId(model.getProductId());
        vo.setModelType(model.getModelType());
        vo.setIdentifier(model.getIdentifier());
        vo.setModelName(model.getModelName());
        vo.setDataType("property".equals(model.getModelType()) ? model.getDataType() : null);
        vo.setSpecsJson(model.getSpecsJson());
        vo.setEventType(model.getEventType());
        vo.setServiceInputJson(model.getServiceInputJson());
        vo.setServiceOutputJson(model.getServiceOutputJson());
        vo.setSortNo(model.getSortNo());
        vo.setRequiredFlag(model.getRequiredFlag());
        vo.setDescription(model.getDescription());
        vo.setCreateTime(model.getCreateTime());
        vo.setUpdateTime(model.getUpdateTime());
        return vo;
    }
}
