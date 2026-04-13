package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.enums.ProductStatusEnum;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.mapper.DeviceMapper;
import com.ghlzm.iot.device.mapper.ProductModelMapper;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.DeviceOnlineSessionService;
import com.ghlzm.iot.device.vo.ProductActivityStatRow;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductDeviceStatRow;
import com.ghlzm.iot.device.vo.ProductPageVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductModelMapper productModelMapper;
    @Mock
    private DeviceOnlineSessionService deviceOnlineSessionService;

    private ProductServiceImpl productService;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
    }

    @BeforeEach
    void setUp() {
        productService = spy(new ProductServiceImpl(deviceMapper, productModelMapper, deviceOnlineSessionService));
    }

    @Test
    void updateProductShouldRejectDisableWhenEnabledDevicesStillExist() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        doReturn(product).when(productService).getRequiredById(1001L);
        when(deviceMapper.selectCount(any())).thenReturn(2L);

        ProductAddDTO dto = new ProductAddDTO();
        dto.setProductKey("demo-product");
        dto.setProductName("Demo Product");
        dto.setProtocolCode("mqtt-json");
        dto.setNodeType(1);
        dto.setStatus(ProductStatusEnum.DISABLED.getCode());

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));
        assertEquals("产品下仍有 2 台启用设备，请先核查库存设备是否仍在使用后再停用", ex.getMessage());
        verify(deviceMapper).selectCount(any());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void deleteProductShouldUseLogicRemoveWhenNoDevicesExist() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        doReturn(product).when(productService).getRequiredById(1001L);
        when(deviceMapper.selectCount(any())).thenReturn(0L);
        doReturn(true).when(productService).removeById(1001L);

        productService.deleteProduct(1001L);

        verify(productService).removeById(1001L);
    }

    @Test
    void getDetailByIdShouldUseAggregatedDeviceStats() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        doReturn(product).when(productService).getRequiredById(1001L);

        ProductDeviceStatRow statRow = new ProductDeviceStatRow();
        statRow.setProductId(1001L);
        statRow.setDeviceCount(6L);
        statRow.setOnlineDeviceCount(2L);
        LocalDateTime lastReportTime = LocalDateTime.of(2026, 3, 20, 10, 30);
        statRow.setLastReportTime(lastReportTime);
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of(statRow));

        ProductActivityStatRow activityStatRow = new ProductActivityStatRow();
        activityStatRow.setProductId(1001L);
        activityStatRow.setTodayActiveCount(3L);
        activityStatRow.setSevenDaysActiveCount(5L);
        activityStatRow.setThirtyDaysActiveCount(8L);
        ProductActivityStatRow durationStatRow = new ProductActivityStatRow();
        durationStatRow.setProductId(1001L);
        durationStatRow.setAvgOnlineDuration(120L);
        durationStatRow.setMaxOnlineDuration(720L);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        when(deviceMapper.selectProductActivityStat(1001L, todayStart, todayStart.minusDays(7), todayStart.minusDays(30)))
                .thenReturn(activityStatRow);
        when(deviceOnlineSessionService.loadProductDurationStat(eq(1001L), eq(todayStart.minusDays(30)), any(LocalDateTime.class)))
                .thenReturn(durationStatRow);

        ProductDetailVO detail = productService.getDetailById(1001L);

        assertEquals(6L, detail.getDeviceCount());
        assertEquals(2L, detail.getOnlineDeviceCount());
        assertEquals(lastReportTime, detail.getLastReportTime());
        assertEquals(3L, detail.getTodayActiveCount());
        assertEquals(5L, detail.getSevenDaysActiveCount());
        assertEquals(8L, detail.getThirtyDaysActiveCount());
        assertEquals(120L, detail.getAvgOnlineDuration());
        assertEquals(720L, detail.getMaxOnlineDuration());
        verify(deviceMapper).selectProductStats(any());
        verify(deviceMapper).selectProductActivityStat(1001L, todayStart, todayStart.minusDays(7), todayStart.minusDays(30));
        verify(deviceOnlineSessionService).loadProductDurationStat(eq(1001L), eq(todayStart.minusDays(30)), any(LocalDateTime.class));
    }

    @Test
    void getDetailByIdShouldExposeMetadataJsonInDetailVo() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("muddy-water-product");
        product.setProductName("泥水位监测产品");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        product.setMetadataJson("{\"objectInsight\":{\"customMetrics\":[]}}");
        doReturn(product).when(productService).getRequiredById(1001L);
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of());
        when(deviceMapper.selectProductActivityStat(any(), any(), any(), any())).thenReturn(new ProductActivityStatRow());

        ProductDetailVO detail = productService.getDetailById(1001L);

        assertEquals(product.getMetadataJson(), detail.getMetadataJson());
    }

    @Test
    void pageProductsShouldMapAggregatedStatsIntoRows() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("demo-product");
        product.setProductName("Demo Product");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        Page<Product> page = new Page<>(1, 10);
        page.setCurrent(1L);
        page.setSize(10L);
        page.setTotal(1L);
        page.setRecords(List.of(product));
        doReturn(page).when(productService).page(any(Page.class), any(LambdaQueryWrapper.class));

        ProductDeviceStatRow statRow = new ProductDeviceStatRow();
        statRow.setProductId(1001L);
        statRow.setDeviceCount(8L);
        statRow.setOnlineDeviceCount(3L);
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of(statRow));

        PageResult<ProductPageVO> result = productService.pageProducts(null, "Demo", null, null, null, 1L, 10L);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(8L, result.getRecords().get(0).getDeviceCount());
        assertEquals(3L, result.getRecords().get(0).getOnlineDeviceCount());
        verify(deviceMapper).selectProductStats(any());
        verify(deviceMapper, never()).selectProductActivityStat(any(), any(), any(), any());
        verifyNoInteractions(deviceOnlineSessionService);
    }

    @Test
    void pageProductsShouldApplyQuickSearchAcrossProductNameKeyAndManufacturer() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("accept-http-product-01");
        product.setProductName("压力泵监测产品");
        product.setManufacturer("GHLZM");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());

        Page<Product> page = new Page<>(1, 10);
        page.setCurrent(1L);
        page.setSize(10L);
        page.setTotal(1L);
        page.setRecords(List.of(product));
        doReturn(page).when(productService).page(any(Page.class), any(LambdaQueryWrapper.class));
        when(deviceMapper.selectProductStats(any())).thenReturn(List.of());

        productService.pageProducts(null, "accept-http", null, null, null, 1L, 10L);

        ArgumentCaptor<LambdaQueryWrapper<Product>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(productService).page(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();

        assertTrue(sqlSegment.contains("product_name"));
        assertTrue(sqlSegment.contains("product_key"));
        assertTrue(sqlSegment.contains("manufacturer"));
        assertTrue(sqlSegment.contains("OR"));
    }

    @Test
    void productDetailVoShouldKeepNullableOnlineDurationFieldsInJson() throws Exception {
        ProductDetailVO detail = new ProductDetailVO();
        detail.setId(1001L);
        detail.setProductKey("demo-product");
        detail.setAvgOnlineDuration(null);
        detail.setMaxOnlineDuration(null);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        String json = objectMapper.writeValueAsString(detail);

        assertTrue(json.contains("\"avgOnlineDuration\":null"));
        assertTrue(json.contains("\"maxOnlineDuration\":null"));
    }

    @Test
    void addProductShouldRejectInvalidMetadataJson() {
        doReturn(productMapper).when(productService).getBaseMapper();
        when(productMapper.selectOne(any())).thenReturn(null);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("{");

        BizException ex = assertThrows(BizException.class, () -> productService.addProduct(dto));

        assertEquals("产品扩展元数据必须是合法JSON对象", ex.getMessage());
        verify(productService, never()).save(any(Product.class));
    }

    @Test
    void updateProductShouldRejectDuplicateObjectInsightIdentifiers() {
        doReturn(buildExistingProduct()).when(productService).getRequiredById(1001L);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"S1_ZT_1.humidity","displayName":"相对湿度","group":"status"},
                      {"identifier":"S1_ZT_1.humidity","displayName":"重复湿度","group":"status"}
                    ]
                  }
                }
                """);

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));

        assertEquals("对象洞察自定义指标标识符不能重复: S1_ZT_1.humidity", ex.getMessage());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldRejectInvalidObjectInsightGroup() {
        doReturn(buildExistingProduct()).when(productService).getRequiredById(1001L);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"S1_ZT_1.humidity","displayName":"相对湿度","group":"env"}
                    ]
                  }
                }
                """);

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));

        assertEquals("对象洞察指标分组仅支持 measure、status 或 runtime", ex.getMessage());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldRejectTooManyObjectInsightMetrics() {
        doReturn(buildExistingProduct()).when(productService).getRequiredById(1001L);

        String metrics = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(index -> """
                        {"identifier":"S1_ZT_1.metric_%d","displayName":"指标%d","group":"status"}
                        """.formatted(index, index))
                .collect(java.util.stream.Collectors.joining(","));

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [%s]
                  }
                }
                """.formatted(metrics));

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));

        assertEquals("对象洞察自定义指标最多允许20项", ex.getMessage());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldRejectOverlongAnalysisTemplate() {
        doReturn(buildExistingProduct()).when(productService).getRequiredById(1001L);

        String overlongTemplate = "x".repeat(301);
        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier":"S1_ZT_1.humidity",
                        "displayName":"相对湿度",
                        "group":"status",
                        "analysisTemplate":"%s"
                      }
                    ]
                  }
                }
                """.formatted(overlongTemplate));

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));

        assertEquals("对象洞察分析描述模板长度不能超过300", ex.getMessage());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldRejectMissingMetricDisplayName() {
        doReturn(buildExistingProduct()).when(productService).getRequiredById(1001L);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {"identifier":"S1_ZT_1.humidity","displayName":"","group":"status"}
                    ]
                  }
                }
                """);

        BizException ex = assertThrows(BizException.class, () -> productService.updateProduct(1001L, dto));

        assertEquals("对象洞察指标中文名称不能为空", ex.getMessage());
        verify(productService, never()).updateById(any(Product.class));
    }

    @Test
    void updateProductShouldPersistValidatedObjectInsightMetadata() {
        Product existing = buildExistingProduct();
        doReturn(existing).when(productService).getRequiredById(1001L);
        doReturn(true).when(productService).updateById(any(Product.class));
        doReturn(new ProductDetailVO()).when(productService).getDetailById(1001L);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "S1_ZT_1.humidity",
                        "displayName": "相对湿度",
                        "group": "status",
                        "includeInTrend": true,
                        "includeInExtension": true,
                        "analysisTemplate": "{{label}}当前为{{value}}"
                      }
                    ]
                  }
                }
                """);

        productService.updateProduct(1001L, dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).updateById(captor.capture());
        assertTrue(captor.getValue().getMetadataJson().contains("S1_ZT_1.humidity"));
        assertTrue(captor.getValue().getMetadataJson().contains("\"objectInsight\""));
    }

    @Test
    void updateProductShouldAllowRuntimeObjectInsightGroup() {
        Product existing = buildExistingProduct();
        doReturn(existing).when(productService).getRequiredById(1001L);
        doReturn(true).when(productService).updateById(any(Product.class));
        doReturn(new ProductDetailVO()).when(productService).getDetailById(1001L);

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "S1_ZT_1.battery_dump_energy",
                        "displayName": "电池余量",
                        "group": "runtime",
                        "includeInTrend": true,
                        "includeInExtension": true
                      }
                    ]
                  }
                }
                """);

        productService.updateProduct(1001L, dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).updateById(captor.capture());
        assertTrue(captor.getValue().getMetadataJson().contains("\"group\":\"runtime\""));
        assertTrue(captor.getValue().getMetadataJson().contains("S1_ZT_1.battery_dump_energy"));
    }

    @Test
    void updateProductShouldNormalizeObjectInsightIdentifiersToFormalModelCasing() {
        Product existing = buildExistingProduct();
        doReturn(existing).when(productService).getRequiredById(1001L);
        doReturn(true).when(productService).updateById(any(Product.class));
        doReturn(new ProductDetailVO()).when(productService).getDetailById(1001L);
        when(productModelMapper.selectList(any())).thenReturn(List.of(
                buildProductModel(1001L, "L1_JS_1.gX", "X轴加速度"),
                buildProductModel(1001L, "S1_ZT_1.signal_4g", "4G信号")
        ));

        ProductAddDTO dto = buildProductDto();
        dto.setMetadataJson("""
                {
                  "objectInsight": {
                    "customMetrics": [
                      {
                        "identifier": "l1_js_1.gx",
                        "displayName": "X轴加速度",
                        "group": "measure",
                        "includeInTrend": true
                      },
                      {
                        "identifier": "s1_zt_1.signal_4g",
                        "displayName": "4G信号",
                        "group": "runtime",
                        "includeInTrend": true
                      }
                    ]
                  }
                }
                """);

        productService.updateProduct(1001L, dto);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).updateById(captor.capture());
        assertTrue(captor.getValue().getMetadataJson().contains("\"identifier\":\"L1_JS_1.gX\""));
        assertTrue(captor.getValue().getMetadataJson().contains("\"identifier\":\"S1_ZT_1.signal_4g\""));
        assertTrue(!captor.getValue().getMetadataJson().contains("\"identifier\":\"l1_js_1.gx\""));
        assertTrue(!captor.getValue().getMetadataJson().contains("\"identifier\":\"s1_zt_1.signal_4g\""));
    }

    private Product buildExistingProduct() {
        Product product = new Product();
        product.setId(1001L);
        product.setProductKey("muddy-water-product");
        product.setProductName("泥水位监测产品");
        product.setProtocolCode("mqtt-json");
        product.setNodeType(1);
        product.setStatus(ProductStatusEnum.ENABLED.getCode());
        return product;
    }

    private ProductModel buildProductModel(Long productId, String identifier, String modelName) {
        ProductModel model = new ProductModel();
        model.setProductId(productId);
        model.setModelType("property");
        model.setIdentifier(identifier);
        model.setModelName(modelName);
        model.setDeleted(0);
        return model;
    }

    private ProductAddDTO buildProductDto() {
        ProductAddDTO dto = new ProductAddDTO();
        dto.setProductKey("muddy-water-product");
        dto.setProductName("泥水位监测产品");
        dto.setProtocolCode("mqtt-json");
        dto.setNodeType(1);
        dto.setStatus(ProductStatusEnum.ENABLED.getCode());
        return dto;
    }
}
