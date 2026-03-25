package com.ghlzm.iot.device.service.impl;

import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductModelServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModelMapper productModelMapper;

    private ProductModelServiceImpl productModelService;

    @BeforeEach
    void setUp() {
        productModelService = new ProductModelServiceImpl(productMapper, productModelMapper);
    }

    @Test
    void createModelShouldRejectDuplicateIdentifierWithinSameProduct() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(existingModel(2001L, "temperature", 1));

        BizException ex = assertThrows(
                BizException.class,
                () -> productModelService.createModel(1001L, propertyDto("temperature", "double"))
        );

        assertTrue(ex.getMessage().contains("物模型标识已存在"));
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createModelShouldRejectUnsupportedModelType() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("command");
        dto.setIdentifier("set-temp");
        dto.setModelName("设置温度");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("物模型类型不支持: command", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createPropertyModelShouldRequireDataType() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = propertyDto("temperature", null);

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("属性物模型必须填写 dataType", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createPropertyModelShouldRejectInvalidSpecsJson() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = propertyDto("temperature", "double");
        dto.setSpecsJson("{invalid");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("specsJson 必须是合法 JSON", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createEventModelShouldOnlyAllowEventTypeField() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("event");
        dto.setIdentifier("alarmRaised");
        dto.setModelName("告警触发");
        dto.setEventType("warning");
        dto.setServiceInputJson("{\"unexpected\":true}");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("事件物模型只允许填写 eventType", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createServiceModelShouldOnlyAllowServiceInputAndOutputFields() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("设置阈值");
        dto.setServiceInputJson("{\"threshold\":10}");
        dto.setServiceOutputJson("{\"accepted\":true}");
        dto.setEventType("warning");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("服务物模型只允许填写 serviceInputJson 和 serviceOutputJson", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createServiceModelShouldRejectInvalidServiceInputJson() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("设置阈值");
        dto.setServiceInputJson("{invalid");

        BizException ex = assertThrows(BizException.class, () -> productModelService.createModel(1001L, dto));

        assertEquals("serviceInputJson 必须是合法 JSON", ex.getMessage());
        verify(productModelMapper, never()).insert(any(ProductModel.class));
    }

    @Test
    void createEventModelShouldPersistCompatibilityDataTypeForExistingSchema() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(null);

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("event");
        dto.setIdentifier("alarmRaised");
        dto.setModelName("告警触发");
        dto.setEventType("warning");
        dto.setSortNo(20);

        ProductModelVO result = productModelService.createModel(1001L, dto);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("json", captor.getValue().getDataType());
        assertNull(result.getDataType());
    }

    @Test
    void createServiceModelShouldPersistCompatibilityDataTypeForExistingSchema() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectOne(any())).thenReturn(null);

        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("service");
        dto.setIdentifier("setThreshold");
        dto.setModelName("设置阈值");
        dto.setServiceInputJson("{\"threshold\":10}");
        dto.setServiceOutputJson("{\"accepted\":true}");
        dto.setSortNo(30);

        ProductModelVO result = productModelService.createModel(1001L, dto);

        ArgumentCaptor<ProductModel> captor = ArgumentCaptor.forClass(ProductModel.class);
        verify(productModelMapper).insert(captor.capture());
        assertEquals("json", captor.getValue().getDataType());
        assertNull(result.getDataType());
    }

    @Test
    void listModelsShouldSortBySortNoThenIdentifier() {
        when(productMapper.selectById(1001L)).thenReturn(product(1001L));
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                existingModel(2001L, "zeta", 20),
                existingModel(2002L, "alpha", 10),
                existingModel(2003L, "beta", 10)
        ));

        List<ProductModelVO> result = productModelService.listModels(1001L);

        assertEquals(3, result.size());
        assertIterableEquals(List.of("alpha", "beta", "zeta"), result.stream().map(ProductModelVO::getIdentifier).toList());
        assertIterableEquals(List.of(10, 10, 20), result.stream().map(ProductModelVO::getSortNo).toList());
        verify(productModelMapper).selectList(any());
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setProductKey("accept-product");
        product.setProductName("验收产品");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        return product;
    }

    private ProductModel existingModel(Long id, String identifier, Integer sortNo) {
        ProductModel model = new ProductModel();
        model.setId(id);
        model.setProductId(1001L);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(identifier);
        model.setDataType("double");
        model.setSortNo(sortNo);
        model.setDeleted(0);
        return model;
    }

    private ProductModelUpsertDTO propertyDto(String identifier, String dataType) {
        ProductModelUpsertDTO dto = new ProductModelUpsertDTO();
        dto.setModelType("property");
        dto.setIdentifier(identifier);
        dto.setModelName("温度");
        dto.setDataType(dataType);
        dto.setSpecsJson("{\"unit\":\"℃\"}");
        dto.setSortNo(10);
        dto.setRequiredFlag(1);
        dto.setDescription("温度属性");
        return dto;
    }
}
