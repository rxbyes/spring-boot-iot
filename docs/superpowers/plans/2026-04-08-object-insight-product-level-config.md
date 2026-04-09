# 对象洞察台产品级正式配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在产品定义中心正式维护 `iot_product.metadata_json.objectInsight`，让 `/insight` 按“设备级覆盖 > 产品级正式配置 > 前端内置注册表 > 运行时自动识别”读取单设备对象洞察配置，并保持现有单设备分析基线不回退。

**Architecture:** 后端不新增独立配置表与专用对象洞察 API，而是在 `iot_product` 增加 `metadata_json` 字段，并通过现有产品新增/编辑/详情接口统一维护，再由 `ProductServiceImpl` 对 JSON 与 `customMetrics` 结构做强校验。前端在产品编辑工作区新增“对象洞察配置”结构化编辑区，通过共享序列化工具把行编辑结果写入 `metadataJson.objectInsight.customMetrics`，对象洞察页在加载设备详情后按产品维度补取产品详情并合并配置。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Vue 3, TypeScript, Element Plus, Vitest, Python unittest, Maven

---

## 背景约束

- 当前分支必须保持在 `codex/dev`，不得切到 `master` 开发。
- 当前工作区已经存在对象洞察前序改造的未提交文件，执行时必须精确暂存本次文件，避免误带 `scripts/__pycache__`、`.superpowers/brainstorm/*` 等无关噪音。
- 真实环境仍以 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 为验收基线，不得回退 H2。
- 本轮只建设产品级正式配置入口，不建设设备级配置编辑入口、不建设独立表、不建设独立菜单。
- 配置字段固定落在 `iot_product.metadata_json.objectInsight.customMetrics[]`。
- 后端必须拒绝脏配置：非法 JSON、重复 `identifier`、非法 `group`、超过 20 项、模板长度超过 300 都必须抛 `BizException`。
- `/insight` 必须继续是单设备分析页面，不得回流为风险点整体分析页面。
- 文档必须原位更新 `docs/` 既有文档；同时检查 `README.md` 与 `AGENTS.md` 是否需要同步，本轮预计仅记录“已检查，无需修改”。

## File Map

### Backend schema and contract

- Modify: `sql/init.sql`
  - 给 `iot_product` 增加 `metadata_json JSON DEFAULT NULL COMMENT '产品扩展元数据'`。
- Modify: `scripts/run-real-env-schema-sync.py`
  - 在 `COLUMNS_TO_ADD["iot_product"]` 中增加 `metadata_json` 补列规则。
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
  - 增加 schema sync 覆盖测试，锁定 `iot_product.metadata_json` 补齐逻辑。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/Product.java`
  - 暴露 `metadataJson` 实体字段。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductAddDTO.java`
  - 接收产品扩展元数据。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductDetailVO.java`
  - 向产品详情返回 `metadataJson`。
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
  - 赋值并透传 `metadataJson`。
- Modify Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
  - 锁定详情 VO 与保存链路都能带出 `metadataJson`。

### Frontend shared types and config utilities

- Modify: `spring-boot-iot-ui/src/types/api.ts`
  - 给 `Product` / `ProductAddPayload` 增加 `metadataJson`，补产品对象洞察配置类型定义。
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
  - 镜像补齐 `metadataJson` 与对象洞察配置类型，避免旧声明漂移。
- Create: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`
  - 负责解析、校验、序列化 `metadataJson.objectInsight.customMetrics`。
- Create Test: `spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts`
  - 锁定解析、去空、重复检测、20 项限制与 JSON 输出。

### Frontend product editing surface

- Create: `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue`
  - 负责自定义指标行编辑，不让 `ProductEditWorkspace` 和新增抽屉复制同一套表单片段。
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
  - 在“补充说明”后追加“对象洞察配置”分区并接入共享编辑器。
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  - 扩展表单状态、详情回填、提交序列化与新增抽屉复用对象洞察配置编辑器。
- Create Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

### Frontend insight read path

- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
  - 支持同时读取设备级与产品级 `metadataJson`，并按优先级合并。
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
  - 在设备详情成功后按 `productId` 补取产品详情，并在产品接口失败时降级。
- Modify Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

### Docs

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

---

### Task 1: 补齐 `iot_product.metadata_json` 字段与产品接口透传契约

**Files:**
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `scripts/tests/test_run_real_env_schema_sync.py`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/Product.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductAddDTO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductDetailVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- Test: `scripts/tests/test_run_real_env_schema_sync.py`

- [ ] **Step 1: 先写失败测试，锁定 schema sync 与详情透传都要包含 `metadataJson`**

```python
class ProductMetadataColumnCoverageTest(unittest.TestCase):
    def test_product_metadata_json_column_is_declared_for_schema_sync(self):
        self.assertIn("iot_product", schema_sync.COLUMNS_TO_ADD)
        self.assertIn(
            ("metadata_json", "JSON DEFAULT NULL COMMENT '产品扩展元数据'"),
            schema_sync.COLUMNS_TO_ADD["iot_product"],
        )
```

```java
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
```

- [ ] **Step 2: 运行测试，确认当前尚未声明该字段而失败**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
mvn -pl spring-boot-iot-device "-Dtest=ProductServiceImplTest" test
```

Expected:
- Python 测试因 `COLUMNS_TO_ADD["iot_product"]` 缺失而失败。
- `ProductServiceImplTest` 因 `Product` / `ProductDetailVO` 没有 `metadataJson` 字段或 `toDetailVO` 未赋值而失败。

- [ ] **Step 3: 写最小实现，让产品表、DTO/VO 与服务透传链路先打通**

```sql
CREATE TABLE iot_product (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_key VARCHAR(64) NOT NULL COMMENT '产品Key',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    protocol_code VARCHAR(64) NOT NULL COMMENT '协议编码',
    node_type TINYINT NOT NULL DEFAULT 1 COMMENT '节点类型 1直连设备 2网关设备 3网关子设备',
    data_format VARCHAR(32) NOT NULL DEFAULT 'JSON' COMMENT '数据格式',
    manufacturer VARCHAR(128) DEFAULT NULL COMMENT '厂商',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    metadata_json JSON DEFAULT NULL COMMENT '产品扩展元数据',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
```

```python
COLUMNS_TO_ADD: ColumnSpecMap = {
    "iot_product": [
        ("metadata_json", "JSON DEFAULT NULL COMMENT '产品扩展元数据'"),
    ],
    "iot_device": [
        ("org_id", "BIGINT DEFAULT NULL COMMENT 'organization id' AFTER `tenant_id`"),
        ("org_name", "VARCHAR(128) DEFAULT NULL COMMENT 'organization name' AFTER `org_id`"),
    ],
```

```java
@Data
public class Product extends BaseEntity {

    private String productKey;
    private String productName;
    private String protocolCode;
    private Integer nodeType;
    private String dataFormat;
    private String manufacturer;
    private String description;
    private String metadataJson;
    private Integer status;
}
```

```java
@Data
public class ProductAddDTO {

    @NotBlank
    private String productKey;
    @NotBlank
    private String productName;
    @NotBlank
    private String protocolCode;
    @NotNull
    private Integer nodeType;
    private String dataFormat;
    private String manufacturer;
    private String description;
    private String metadataJson;
    private Integer status;
}
```

```java
@Data
public class ProductDetailVO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String productKey;
    private String productName;
    private String protocolCode;
    private Integer nodeType;
    private String dataFormat;
    private String manufacturer;
    private String description;
    private String metadataJson;
    private Integer status;
    // 其余统计字段保持不变
}
```

```java
private void applyEditableFields(Product product, ProductAddDTO dto) {
    product.setProductName(normalizeRequired(dto.getProductName(), "产品名称"));
    product.setProtocolCode(normalizeRequired(dto.getProtocolCode(), "协议编码"));
    product.setNodeType(dto.getNodeType());
    product.setDataFormat(resolveOptionalText(dto.getDataFormat(), "JSON"));
    product.setManufacturer(resolveOptionalText(dto.getManufacturer(), null));
    product.setDescription(resolveOptionalText(dto.getDescription(), null));
    product.setMetadataJson(resolveOptionalText(dto.getMetadataJson(), null));
    product.setStatus(resolveProductStatus(dto.getStatus()));
}

private ProductDetailVO toDetailVO(Product product, ProductDeviceStatRow stat, ProductActivityStatRow activityStat) {
    ProductDetailVO detail = new ProductDetailVO();
    detail.setId(product.getId());
    detail.setProductKey(product.getProductKey());
    detail.setProductName(product.getProductName());
    detail.setProtocolCode(product.getProtocolCode());
    detail.setNodeType(product.getNodeType());
    detail.setDataFormat(product.getDataFormat());
    detail.setManufacturer(product.getManufacturer());
    detail.setDescription(product.getDescription());
    detail.setMetadataJson(product.getMetadataJson());
    detail.setStatus(product.getStatus());
    // 其余统计字段保持不变
    return detail;
}
```

- [ ] **Step 4: 重新运行 schema 与后端单测，确认字段声明与透传已转绿**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
mvn -pl spring-boot-iot-device "-Dtest=ProductServiceImplTest" test
```

Expected:
- `OK`
- `BUILD SUCCESS`
- 新增测试证明 `metadata_json` 会被补列，产品详情也会带出 `metadataJson`。

- [ ] **Step 5: 提交产品元数据字段与契约透传基础改动**

```bash
git add sql/init.sql \
        scripts/run-real-env-schema-sync.py \
        scripts/tests/test_run_real_env_schema_sync.py \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/Product.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/ProductAddDTO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductDetailVO.java \
        spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java
git commit -m "feat: add product metadata json contract"
```

### Task 2: 给产品 `metadataJson.objectInsight.customMetrics` 增加服务端强校验

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Modify Test: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`

- [ ] **Step 1: 先写失败测试，锁定非法 JSON、重复指标和合法配置保存行为**

```java
@Test
void addProductShouldRejectInvalidMetadataJson() {
    ProductAddDTO dto = new ProductAddDTO();
    dto.setProductKey("muddy-water-product");
    dto.setProductName("泥水位监测产品");
    dto.setProtocolCode("mqtt-json");
    dto.setNodeType(1);
    dto.setMetadataJson("{");

    BizException ex = assertThrows(BizException.class, () -> productService.addProduct(dto));

    assertEquals("产品扩展元数据必须是合法JSON对象", ex.getMessage());
}

@Test
void addProductShouldRejectDuplicateObjectInsightIdentifiers() {
    ProductAddDTO dto = new ProductAddDTO();
    dto.setProductKey("muddy-water-product");
    dto.setProductName("泥水位监测产品");
    dto.setProtocolCode("mqtt-json");
    dto.setNodeType(1);
    dto.setMetadataJson("""
        {
          \"objectInsight\": {
            \"customMetrics\": [
              {\"identifier\":\"S1_ZT_1.humidity\",\"displayName\":\"相对湿度\",\"group\":\"status\"},
              {\"identifier\":\"S1_ZT_1.humidity\",\"displayName\":\"重复湿度\",\"group\":\"status\"}
            ]
          }
        }
        """);

    BizException ex = assertThrows(BizException.class, () -> productService.addProduct(dto));

    assertEquals("对象洞察自定义指标标识符不能重复: S1_ZT_1.humidity", ex.getMessage());
}

@Test
void updateProductShouldPersistValidatedObjectInsightMetadata() {
    Product existing = new Product();
    existing.setId(1001L);
    existing.setProductKey("muddy-water-product");
    existing.setProductName("泥水位监测产品");
    existing.setProtocolCode("mqtt-json");
    existing.setNodeType(1);
    existing.setStatus(ProductStatusEnum.ENABLED.getCode());
    doReturn(existing).when(productService).getRequiredById(1001L);
    doReturn(true).when(productService).updateById(any(Product.class));
    doReturn(new ProductDetailVO()).when(productService).getDetailById(1001L);

    ProductAddDTO dto = new ProductAddDTO();
    dto.setProductKey("muddy-water-product");
    dto.setProductName("泥水位监测产品");
    dto.setProtocolCode("mqtt-json");
    dto.setNodeType(1);
    dto.setMetadataJson("""
        {
          \"objectInsight\": {
            \"customMetrics\": [
              {
                \"identifier\": \"S1_ZT_1.humidity\",
                \"displayName\": \"相对湿度\",
                \"group\": \"status\",
                \"includeInTrend\": true,
                \"includeInExtension\": true,
                \"analysisTemplate\": \"{{label}}当前为{{value}}\"
              }
            ]
          }
        }
        """);

    productService.updateProduct(1001L, dto);

    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
    verify(productService).updateById(captor.capture());
    assertTrue(captor.getValue().getMetadataJson().contains("S1_ZT_1.humidity"));
}
```

- [ ] **Step 2: 运行后端单测，确认当前没有校验逻辑而失败**

Run:

```bash
mvn -pl spring-boot-iot-device "-Dtest=ProductServiceImplTest" test
```

Expected:
- 非法 JSON 与重复 `identifier` 用例失败，因为当前服务只做字符串透传。
- 合法配置保存用例可能失败，因为当前没有规范化 JSON 输出。

- [ ] **Step 3: 写最小实现，用 `ObjectMapper` 规范化 JSON 并强校验 `customMetrics` 结构**

```java
private static final int MAX_OBJECT_INSIGHT_CUSTOM_METRICS = 20;
private static final int MAX_ANALYSIS_TEMPLATE_LENGTH = 300;
private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

private void applyEditableFields(Product product, ProductAddDTO dto) {
    product.setProductName(normalizeRequired(dto.getProductName(), "产品名称"));
    product.setProtocolCode(normalizeRequired(dto.getProtocolCode(), "协议编码"));
    product.setNodeType(dto.getNodeType());
    product.setDataFormat(resolveOptionalText(dto.getDataFormat(), "JSON"));
    product.setManufacturer(resolveOptionalText(dto.getManufacturer(), null));
    product.setDescription(resolveOptionalText(dto.getDescription(), null));
    product.setMetadataJson(normalizeMetadataJson(dto.getMetadataJson()));
    product.setStatus(resolveProductStatus(dto.getStatus()));
}

private String normalizeMetadataJson(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) {
        return null;
    }
    try {
        JsonNode root = objectMapper.readTree(metadataJson.trim());
        if (!(root instanceof ObjectNode objectNode)) {
            throw new BizException("产品扩展元数据必须是合法JSON对象");
        }
        validateObjectInsightConfig(objectNode.path("objectInsight"));
        return objectMapper.writeValueAsString(objectNode);
    } catch (BizException ex) {
        throw ex;
    } catch (Exception ex) {
        throw new BizException("产品扩展元数据必须是合法JSON对象");
    }
}

private void validateObjectInsightConfig(JsonNode objectInsightNode) {
    if (objectInsightNode == null || objectInsightNode.isMissingNode() || objectInsightNode.isNull()) {
        return;
    }
    if (!objectInsightNode.isObject()) {
        throw new BizException("对象洞察配置必须是JSON对象");
    }
    JsonNode customMetricsNode = objectInsightNode.path("customMetrics");
    if (customMetricsNode.isMissingNode() || customMetricsNode.isNull()) {
        return;
    }
    if (!customMetricsNode.isArray()) {
        throw new BizException("对象洞察自定义指标必须是数组");
    }
    if (customMetricsNode.size() > MAX_OBJECT_INSIGHT_CUSTOM_METRICS) {
        throw new BizException("对象洞察自定义指标最多允许20项");
    }
    Set<String> identifiers = new LinkedHashSet<>();
    for (JsonNode item : customMetricsNode) {
        String identifier = requireMetricText(item, "identifier", "对象洞察指标标识不能为空");
        String displayName = requireMetricText(item, "displayName", "对象洞察指标中文名称不能为空");
        String group = requireMetricText(item, "group", "对象洞察指标分组不能为空");
        if (!"measure".equals(group) && !"status".equals(group)) {
            throw new BizException("对象洞察指标分组仅支持 measure 或 status");
        }
        if (!identifiers.add(identifier)) {
            throw new BizException("对象洞察自定义指标标识符不能重复: " + identifier);
        }
        JsonNode templateNode = item.path("analysisTemplate");
        if (templateNode.isTextual() && templateNode.asText().trim().length() > MAX_ANALYSIS_TEMPLATE_LENGTH) {
            throw new BizException("对象洞察分析描述模板长度不能超过300");
        }
    }
}
```

Implementation notes:
- 只校验 `objectInsight.customMetrics`，不清洗其他产品元数据，避免误伤未来扩展字段。
- `enabled`、`includeInTrend`、`includeInExtension`、`sortNo` 保持可选，后端只校验结构合法性与长度限制。
- 返回入库 JSON 时统一走 `objectMapper.writeValueAsString(...)`，确保设备页读取时拿到规范 JSON。

- [ ] **Step 4: 重新运行后端单测，确认脏配置被拦截、合法配置可保存**

Run:

```bash
mvn -pl spring-boot-iot-device "-Dtest=ProductServiceImplTest" test
```

Expected:
- `BUILD SUCCESS`
- 新增测试证明：非法 JSON、重复标识、非法结构都会抛 `BizException`，合法配置能被标准化保存。

- [ ] **Step 5: 提交产品对象洞察元数据校验逻辑**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java \
        spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java
git commit -m "feat: validate product object insight metadata"
```

### Task 3: 建立前端产品对象洞察配置类型与序列化工具

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/types/api.d.ts`
- Create: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`
- Create Test: `spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts`

- [ ] **Step 1: 先写失败测试，锁定解析、序列化和前端基础校验行为**

```ts
import { describe, expect, it } from 'vitest'
import {
  buildProductMetadataJson,
  createEmptyProductObjectInsightMetric,
  parseProductObjectInsightMetrics,
  validateProductObjectInsightMetrics
} from '@/utils/productObjectInsightConfig'

describe('productObjectInsightConfig', () => {
  it('parses custom metrics from metadataJson into editable rows', () => {
    const rows = parseProductObjectInsightMetrics(
      JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.humidity',
              displayName: '相对湿度',
              group: 'status',
              includeInTrend: true,
              includeInExtension: true,
              enabled: true,
              sortNo: 10
            }
          ]
        }
      })
    )

    expect(rows).toHaveLength(1)
    expect(rows[0].displayName).toBe('相对湿度')
    expect(rows[0].group).toBe('status')
  })

  it('serializes editable rows back into metadataJson.objectInsight.customMetrics', () => {
    const metadataJson = buildProductMetadataJson([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.signal_4g',
        displayName: '4G 信号强度',
        group: 'status',
        analysisTemplate: '{{label}}当前为{{value}}',
        sortNo: 20
      }
    ])

    expect(metadataJson).toContain('objectInsight')
    expect(metadataJson).toContain('S1_ZT_1.signal_4g')
  })

  it('rejects duplicate identifiers before submit', () => {
    const message = validateProductObjectInsightMetrics([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '相对湿度',
        group: 'status'
      },
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '重复湿度',
        group: 'status'
      }
    ])

    expect(message).toBe('对象洞察配置中存在重复指标标识：S1_ZT_1.humidity')
  })
})
```

- [ ] **Step 2: 运行前端工具测试，确认当前还没有共享配置工具而失败**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run src/__tests__/utils/productObjectInsightConfig.test.ts
```

Expected:
- `Cannot find module '@/utils/productObjectInsightConfig'`
- 或类型定义缺失导致测试编译失败。

- [ ] **Step 3: 写最小实现，补齐产品对象洞察配置类型与序列化工具**

```ts
export interface ProductObjectInsightCustomMetricConfig {
  identifier: string
  displayName: string
  group: 'measure' | 'status'
  includeInTrend?: boolean | null
  includeInExtension?: boolean | null
  analysisTitle?: string | null
  analysisTag?: string | null
  analysisTemplate?: string | null
  enabled?: boolean | null
  sortNo?: number | null
}

export interface ProductObjectInsightConfig {
  customMetrics?: ProductObjectInsightCustomMetricConfig[] | null
}

export interface ProductMetadata {
  objectInsight?: ProductObjectInsightConfig | null
}

export interface Product {
  id: IdType
  productKey: string
  productName: string
  protocolCode: string
  nodeType: number
  metadataJson?: string | null
  // 其余字段保持不变
}

export interface ProductAddPayload {
  productKey: string
  productName: string
  protocolCode: string
  nodeType: number
  dataFormat?: string
  manufacturer?: string
  description?: string
  metadataJson?: string
  status?: number
}
```

```ts
const MAX_CUSTOM_METRICS = 20

export function createEmptyProductObjectInsightMetric(): ProductObjectInsightCustomMetricConfig {
  return {
    identifier: '',
    displayName: '',
    group: 'status',
    includeInTrend: true,
    includeInExtension: true,
    analysisTitle: '',
    analysisTag: '系统自定义参数',
    analysisTemplate: '',
    enabled: true,
    sortNo: 10
  }
}

export function parseProductObjectInsightMetrics(metadataJson?: string | null): ProductObjectInsightCustomMetricConfig[] {
  if (!metadataJson?.trim()) {
    return []
  }
  try {
    const parsed = JSON.parse(metadataJson) as { objectInsight?: { customMetrics?: unknown[] } }
    const customMetrics = Array.isArray(parsed?.objectInsight?.customMetrics) ? parsed.objectInsight.customMetrics : []
    return customMetrics.flatMap((item) => {
      if (!item || typeof item !== 'object') {
        return []
      }
      const row = item as Record<string, unknown>
      return [{
        ...createEmptyProductObjectInsightMetric(),
        identifier: String(row.identifier ?? '').trim(),
        displayName: String(row.displayName ?? '').trim(),
        group: row.group === 'measure' ? 'measure' : 'status',
        includeInTrend: typeof row.includeInTrend === 'boolean' ? row.includeInTrend : true,
        includeInExtension: typeof row.includeInExtension === 'boolean' ? row.includeInExtension : true,
        analysisTitle: String(row.analysisTitle ?? '').trim(),
        analysisTag: String(row.analysisTag ?? '').trim(),
        analysisTemplate: String(row.analysisTemplate ?? '').trim(),
        enabled: typeof row.enabled === 'boolean' ? row.enabled : true,
        sortNo: Number.isFinite(Number(row.sortNo)) ? Number(row.sortNo) : 10
      }]
    })
  } catch {
    return []
  }
}

export function validateProductObjectInsightMetrics(rows: ProductObjectInsightCustomMetricConfig[]): string | null {
  if (rows.length > MAX_CUSTOM_METRICS) {
    return '对象洞察配置最多允许 20 个指标'
  }
  const identifiers = new Set<string>()
  for (const row of rows) {
    const identifier = row.identifier.trim()
    if (!identifier) {
      return '对象洞察指标标识不能为空'
    }
    if (!row.displayName?.trim()) {
      return `对象洞察指标 ${identifier} 缺少中文名称`
    }
    if (identifiers.has(identifier)) {
      return `对象洞察配置中存在重复指标标识：${identifier}`
    }
    identifiers.add(identifier)
    if ((row.analysisTemplate || '').trim().length > 300) {
      return `对象洞察指标 ${identifier} 的分析描述模板不能超过300字`
    }
  }
  return null
}

export function buildProductMetadataJson(rows: ProductObjectInsightCustomMetricConfig[], baseMetadataJson?: string | null): string | undefined {
  const normalizedRows = rows
    .map((item) => ({
      identifier: item.identifier.trim(),
      displayName: item.displayName.trim(),
      group: item.group,
      includeInTrend: item.includeInTrend ?? true,
      includeInExtension: item.includeInExtension ?? true,
      analysisTitle: item.analysisTitle?.trim() || undefined,
      analysisTag: item.analysisTag?.trim() || undefined,
      analysisTemplate: item.analysisTemplate?.trim() || undefined,
      enabled: item.enabled ?? true,
      sortNo: item.sortNo ?? 10
    }))
    .filter((item) => item.identifier && item.displayName)

  const base = baseMetadataJson?.trim() ? JSON.parse(baseMetadataJson) as Record<string, unknown> : {}
  const next = {
    ...base,
    objectInsight: {
      ...((base.objectInsight as Record<string, unknown>) || {}),
      customMetrics: normalizedRows
    }
  }
  return JSON.stringify(next)
}
```

- [ ] **Step 4: 重新运行前端工具测试，确认对象洞察配置工具链可用**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run src/__tests__/utils/productObjectInsightConfig.test.ts
```

Expected:
- `PASS`
- 解析、序列化、重复检测、20 项上限都已转绿。

- [ ] **Step 5: 提交前端产品对象洞察配置类型与工具层**

```bash
git add spring-boot-iot-ui/src/types/api.ts \
        spring-boot-iot-ui/src/types/api.d.ts \
        spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts \
        spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts
git commit -m "feat: add product object insight config utils"
```

### Task 4: 在产品定义中心新增“对象洞察配置”结构化编辑入口

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue`
- Create Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify Test: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: 先写失败测试，锁定编辑入口、行编辑器和提交序列化行为**

```ts
it('renders object insight config rows and add button', async () => {
  const wrapper = mount(ProductObjectInsightConfigEditor, {
    props: {
      modelValue: [
        {
          identifier: 'S1_ZT_1.humidity',
          displayName: '相对湿度',
          group: 'status',
          includeInTrend: true,
          includeInExtension: true,
          enabled: true,
          sortNo: 10
        }
      ]
    }
  })

  expect(wrapper.text()).toContain('对象洞察配置')
  expect(wrapper.text()).toContain('新增指标')
  expect(wrapper.text()).toContain('相对湿度')
})
```

```ts
it('shows object insight config section inside ProductEditWorkspace', () => {
  const wrapper = mount(ProductEditWorkspace, {
    props: {
      model: {
        productKey: 'muddy-water-product',
        productName: '泥水位监测产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        description: '泥水位设备',
        metadataJson: '',
        status: 1,
        objectInsightCustomMetrics: []
      },
      rules: {}
    },
    global: {
      stubs: {
        ProductObjectInsightConfigEditor: true,
        StandardButton: StandardButtonStub,
        StandardInlineState: StandardInlineStateStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true
      }
    }
  })

  expect(wrapper.text()).toContain('对象洞察配置')
})
```

```ts
it('serializes object insight rows into metadataJson when submitting product form', async () => {
  const wrapper = mountView()
  ;(wrapper.vm as any).handleAdd()
  ;(wrapper.vm as any).formData.productKey = 'muddy-water-product'
  ;(wrapper.vm as any).formData.productName = '泥水位监测产品'
  ;(wrapper.vm as any).formData.objectInsightCustomMetrics = [
    {
      identifier: 'S1_ZT_1.humidity',
      displayName: '相对湿度',
      group: 'status',
      includeInTrend: true,
      includeInExtension: true,
      analysisTitle: '现场环境补充',
      analysisTag: '系统自定义参数',
      analysisTemplate: '{{label}}当前为{{value}}',
      enabled: true,
      sortNo: 10
    }
  ]
  mockAddProduct.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      id: 1002,
      productKey: 'muddy-water-product',
      productName: '泥水位监测产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      metadataJson: '{"objectInsight":{"customMetrics":[{"identifier":"S1_ZT_1.humidity"}]}}',
      status: 1
    }
  })

  await (wrapper.vm as any).handleSubmit()

  expect(mockAddProduct).toHaveBeenCalledWith(
    expect.objectContaining({
      metadataJson: expect.stringContaining('S1_ZT_1.humidity')
    })
  )
})
```

- [ ] **Step 2: 运行组件与页面测试，确认当前页面还没有该入口而失败**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run \
  src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts \
  src/__tests__/components/product/ProductEditWorkspace.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected:
- 新组件测试因文件不存在而失败。
- `ProductEditWorkspace` 断言因缺少“对象洞察配置”文案而失败。
- `ProductWorkbenchView` 断言因提交 payload 里没有 `metadataJson` 或没有 `objectInsightCustomMetrics` 表单态而失败。

- [ ] **Step 3: 写最小实现，复用统一行编辑器接入新增/编辑两条产品维护路径**

```vue
<!-- ProductObjectInsightConfigEditor.vue -->
<template>
  <section class="ops-drawer-section product-object-insight-config">
    <div class="ops-drawer-section__header">
      <div>
        <h3>对象洞察配置</h3>
        <p class="product-object-insight-config__note">维护单台设备对象洞察的扩展指标、趋势归属与分析描述。</p>
      </div>
      <StandardButton action="add" :disabled="rows.length >= 20" @click="handleAdd">新增指标</StandardButton>
    </div>

    <div v-if="rows.length" class="product-object-insight-config__list">
      <article v-for="(row, index) in rows" :key="`${row.identifier || 'metric'}-${index}`" class="product-object-insight-config__row">
        <!-- 指标标识、中文名称、group、switch、模板、排序、删除按钮 -->
      </article>
    </div>
    <div v-else class="empty-state">当前产品尚未配置对象洞察自定义指标。</div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import StandardButton from '@/components/StandardButton.vue'
import {
  createEmptyProductObjectInsightMetric,
  type ProductObjectInsightCustomMetricConfig
} from '@/utils/productObjectInsightConfig'

const props = defineProps<{ modelValue: ProductObjectInsightCustomMetricConfig[] }>()
const emit = defineEmits<{ (event: 'update:modelValue', value: ProductObjectInsightCustomMetricConfig[]): void }>()
const rows = computed(() => props.modelValue ?? [])

function updateRow(index: number, patch: Partial<ProductObjectInsightCustomMetricConfig>) {
  const next = rows.value.map((item, rowIndex) => (rowIndex === index ? { ...item, ...patch } : item))
  emit('update:modelValue', next)
}

function handleAdd() {
  emit('update:modelValue', [...rows.value, createEmptyProductObjectInsightMetric()])
}

function handleRemove(index: number) {
  emit('update:modelValue', rows.value.filter((_, rowIndex) => rowIndex !== index))
}
</script>
```

```vue
<!-- ProductEditWorkspace.vue 片段 -->
<ProductObjectInsightConfigEditor v-model="model.objectInsightCustomMetrics" />
```

```ts
interface ProductFormState extends ProductAddPayload {
  objectInsightCustomMetrics: ProductObjectInsightCustomMetricConfig[]
}

const createDefaultFormData = (): ProductFormState => ({
  productKey: '',
  productName: '',
  protocolCode: 'mqtt-json',
  nodeType: 1,
  dataFormat: 'JSON',
  manufacturer: '',
  description: '',
  metadataJson: '',
  objectInsightCustomMetrics: [],
  status: 1
})

function applyFormDataWithoutDirty(source?: Partial<Product>) {
  suppressFormDirtyTracking = true
  Object.assign(formData, createDefaultFormData(), {
    ...source,
    metadataJson: source?.metadataJson ?? '',
    objectInsightCustomMetrics: parseProductObjectInsightMetrics(source?.metadataJson)
  })
  nextTick(() => {
    suppressFormDirtyTracking = false
  })
}

function buildSubmitPayload(form: ProductFormState): ProductAddPayload {
  return {
    productKey: form.productKey.trim(),
    productName: form.productName.trim(),
    protocolCode: form.protocolCode.trim(),
    nodeType: form.nodeType,
    dataFormat: form.dataFormat?.trim() || undefined,
    manufacturer: form.manufacturer?.trim() || undefined,
    description: form.description?.trim() || undefined,
    metadataJson: buildProductMetadataJson(form.objectInsightCustomMetrics, form.metadataJson),
    status: form.status ?? 1
  }
}

async function handleSubmit() {
  const valid = editingProductId.value
    ? await editWorkspaceRef.value?.validate()
    : await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const objectInsightError = validateProductObjectInsightMetrics(formData.objectInsightCustomMetrics)
  if (objectInsightError) {
    ElMessage.error(objectInsightError)
    return
  }

  const payload = buildSubmitPayload(formData)
  if (editingProductId.value) {
    const res = await productApi.updateProduct(editingProductId.value, payload)
    // 原有成功分支保持不变
  } else {
    const res = await productApi.addProduct(payload)
    // 原有成功分支保持不变
  }
}
```

Implementation notes:
- 新增抽屉也要复用 `ProductObjectInsightConfigEditor`，不能只让编辑态可维护。
- 行编辑区只做“最小结构化表单”，不直接暴露原始 JSON 文本框。
- `ProductEditWorkspace` 与 `ProductWorkbenchView` 的表单态都要兼容旧产品 `metadataJson` 为空的场景。

- [ ] **Step 4: 重新运行产品编辑相关测试，确认入口、回填与提交序列化转绿**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run \
  src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts \
  src/__tests__/components/product/ProductEditWorkspace.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected:
- `PASS`
- 新增/编辑产品时都能维护 `objectInsightCustomMetrics`，并在提交时写入 `metadataJson.objectInsight.customMetrics`。

- [ ] **Step 5: 提交产品定义中心对象洞察配置编辑入口**

```bash
git add spring-boot-iot-ui/src/components/product/ProductObjectInsightConfigEditor.vue \
        spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue \
        spring-boot-iot-ui/src/views/ProductWorkbenchView.vue \
        spring-boot-iot-ui/src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts \
        spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts \
        spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "feat: add product object insight editor"
```

### Task 5: 让 `/insight` 读取产品级正式配置并保留设备级覆盖优先级

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify Test: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify Test: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: 先写失败测试，锁定“产品级回退 + 设备级覆盖 + 产品接口失败降级”三件事**

```ts
it('merges product metadata first and lets device metadata override the same identifier', () => {
  const profile = getInsightCapabilityProfile({
    deviceCode: 'COLLECT-003',
    productName: '雨量采集终端',
    productMetadataJson: JSON.stringify({
      objectInsight: {
        customMetrics: [
          {
            identifier: 'S1_ZT_1.signal_4g',
            displayName: '4G 信号强度',
            group: 'status',
            analysisTemplate: '{{label}}当前为{{value}}，用于判断设备回传链路稳定性。'
          }
        ]
      }
    }),
    deviceMetadataJson: JSON.stringify({
      objectInsight: {
        customMetrics: [
          {
            identifier: 'S1_ZT_1.signal_4g',
            displayName: '现场传输信号',
            group: 'status',
            analysisTemplate: '{{label}}当前为{{value}}，优先使用设备级覆写文案。'
          }
        ]
      }
    })
  })

  expect(profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.signal_4g')?.displayName).toBe('现场传输信号')
})
```

```ts
it('loads product metadata by productId and uses it when device metadata is empty', async () => {
  vi.mocked(getDeviceByCode).mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      id: 3002,
      productId: 9001,
      deviceCode: 'COLLECT-002',
      deviceName: '雨量采集设备',
      productName: '雨量采集终端',
      metadataJson: ''
    }
  })
  mockGetProductById.mockResolvedValueOnce({
    code: 200,
    msg: 'success',
    data: {
      id: 9001,
      productKey: 'rain-product',
      productName: '雨量采集终端',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      metadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.humidity',
              displayName: '相对湿度',
              group: 'status',
              analysisTemplate: '{{label}}当前为{{value}}，可辅助判断现场环境湿润程度。'
            }
          ]
        }
      })
    }
  })

  const wrapper = shallowMount(DeviceInsightView, { global: { stubs: commonStubs } })
  await flushPromises()

  expect(mockGetProductById).toHaveBeenCalledWith(9001, expect.anything())
  expect(wrapper.text()).toContain('相对湿度')
})
```

```ts
it('falls back to existing device insight path when product detail request fails', async () => {
  mockGetProductById.mockRejectedValueOnce(new Error('产品详情加载失败'))

  const wrapper = shallowMount(DeviceInsightView, { global: { stubs: commonStubs } })
  await flushPromises()

  expect(wrapper.text()).not.toContain('对象洞察加载失败')
  expect(getTelemetryHistoryBatch).toHaveBeenCalled()
})
```

- [ ] **Step 2: 运行对象洞察相关测试，确认当前还没有产品级读取链路而失败**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run \
  src/__tests__/utils/deviceInsightCapability.test.ts \
  src/__tests__/views/DeviceInsightView.test.ts
```

Expected:
- 工具测试因 `getInsightCapabilityProfile` 还不支持 `productMetadataJson` / `deviceMetadataJson` 参数而失败。
- 视图测试因没有补取产品详情或没有降级逻辑而失败。

- [ ] **Step 3: 写最小实现，让对象洞察页按优先级合并设备级与产品级配置**

```ts
interface InsightCapabilitySource {
  deviceCode?: string | null
  productName?: string | null
  metricIdentifier?: string | null
  metricName?: string | null
  riskPointName?: string | null
  properties?: DeviceProperty[] | null
  deviceMetadataJson?: string | null
  productMetadataJson?: string | null
}

export function getInsightCapabilityProfile(source: InsightCapabilitySource): InsightCapabilityProfile {
  const deviceCode = source.deviceCode?.trim()
  const productName = source.productName?.trim() ?? ''
  const baseProfile = deviceCode === 'SK00EB0D1308313' || /泥水位/.test(productName)
    ? MUDDY_WATER_PROFILE
    : buildRuntimeProfile(source)
  return applyCustomMetrics(baseProfile, source)
}

function applyCustomMetrics(profile: InsightCapabilityProfile, source: InsightCapabilitySource): InsightCapabilityProfile {
  const customMetrics = uniqueCustomMetrics([
    ...parseObjectInsightMetadata(source.productMetadataJson),
    ...parseObjectInsightMetadata(source.deviceMetadataJson)
  ]
    .map((item) => toCustomMetricDefinition(item.identifier, item, profile, source))
    .filter((item): item is InsightCustomMetricDefinition => Boolean(item)))

  // 其余 heroMetrics / trendGroups / extensionParameters / historyIdentifiers 仍沿现有逻辑扩展，
  // 但要以 customMetrics 的 displayName、group、includeInTrend、includeInExtension 为准。
}
```

```ts
const productMetadataJson = ref<string | null>(null)

async function loadInsight(_source: 'route-change' | 'manual-query' | 'range-change') {
  const code = normalizedDeviceCode.value
  if (!code) {
    resetInsightState()
    return
  }

  const version = ++requestVersion.value
  isLoading.value = true
  errorMessage.value = ''
  trendErrorMessage.value = ''

  try {
    const deviceResponse = await getDeviceByCode(code)
    if (version !== requestVersion.value) {
      return
    }
    device.value = deviceResponse.data

    const productPromise = device.value?.productId
      ? productApi.getProductById(device.value.productId, {}).catch((error) => {
          console.warn('对象洞察产品配置补充失败', error)
          return null
        })
      : Promise.resolve(null)

    const [propertyResponse, bindingResponse, productResponse] = await Promise.all([
      getDeviceProperties(code),
      getRiskMonitoringList({ deviceCode: code, pageNum: 1, pageSize: 50 }),
      productPromise
    ])
    if (version !== requestVersion.value) {
      return
    }

    properties.value = propertyResponse.data ?? []
    riskBindings.value = bindingResponse.data.records ?? []
    productMetadataJson.value = productResponse?.data?.metadataJson ?? null

    capabilityProfile.value = getInsightCapabilityProfile({
      deviceCode: device.value?.deviceCode,
      productName: device.value?.productName,
      metricIdentifier: riskDetail.value?.metricIdentifier,
      metricName: riskDetail.value?.metricName,
      riskPointName: riskDetail.value?.riskPointName,
      properties: properties.value,
      deviceMetadataJson: device.value?.metadataJson,
      productMetadataJson: productMetadataJson.value
    })

    // 其余趋势查询、路由同步逻辑保持不变
  } catch (error) {
    if (version !== requestVersion.value) {
      return
    }
    errorMessage.value = error instanceof Error ? error.message : '对象洞察加载失败'
  } finally {
    if (version === requestVersion.value) {
      isLoading.value = false
    }
  }
}

function resetInsightState() {
  errorMessage.value = ''
  trendErrorMessage.value = ''
  device.value = null
  properties.value = []
  riskBindings.value = []
  riskDetail.value = null
  productMetadataJson.value = null
  trendGroups.value = []
  lastFetchTime.value = null
}
```

Implementation notes:
- 合并顺序必须是“产品级先入、设备级后入”，因为 `uniqueCustomMetrics` 的后写覆盖要体现设备级优先。
- 产品详情接口失败只能 `console.warn` + 降级，不能把整个对象洞察页面打成错误态。
- 不要让 `productApi.getProductById` 阻塞风险详情、属性快照与 TDengine 趋势查询的主成功路径。

- [ ] **Step 4: 重新运行对象洞察相关测试，确认产品级回退链路与降级逻辑转绿**

Run:

```bash
cd spring-boot-iot-ui && npm test -- --run \
  src/__tests__/utils/deviceInsightCapability.test.ts \
  src/__tests__/views/DeviceInsightView.test.ts
```

Expected:
- `PASS`
- 工具测试证明设备级可覆盖产品级同指标配置。
- 页面测试证明有 `productId` 时会补取产品详情，产品详情失败时页面仍能继续分析设备。

- [ ] **Step 5: 提交对象洞察页产品级读取回退链路**

```bash
git add spring-boot-iot-ui/src/utils/deviceInsightCapability.ts \
        spring-boot-iot-ui/src/views/DeviceInsightView.vue \
        spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts \
        spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts
git commit -m "feat: load product insight metadata fallback"
```

### Task 6: 同步文档并完成回归验证

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: 原位补文档，明确产品级对象洞察配置的行为、接口、库表与前端复用规则**

```md
- `docs/02-业务功能与流程说明.md`
  - 补“产品定义中心 -> 对象洞察配置”业务流程，说明该入口维护的是单设备对象洞察正式配置。
- `docs/03-接口规范与接口清单.md`
  - 在 `POST /api/device/product/add`、`PUT /api/device/product/{id}`、`GET /api/device/product/{id}` 增加 `metadataJson` 字段说明与校验约束。
- `docs/04-数据库设计与初始化数据.md`
  - 记录 `iot_product.metadata_json` 字段及 `objectInsight.customMetrics[]` 结构。
- `docs/06-前端开发与CSS规范.md`
  - 记录对象洞察配置编辑器复用策略，禁止新增独立对象洞察配置页面。
- `docs/08-变更记录与技术债清单.md`
  - 记录本轮把前端轻配置演进为产品级正式配置的收口情况与后续设备级覆盖入口仍未建设。
- `docs/15-前端优化与治理计划.md`
  - 记录通过共享编辑器避免产品新增/编辑表单重复布局。
- `docs/21-业务功能清单与验收标准.md`
  - 把产品级对象洞察配置纳入“产品定义中心 / 对象洞察台”验收口径。
- `README.md` / `AGENTS.md`
  - 本轮只检查是否需要更新；若无新增启动方式或协作规则，则明确记为“已检查，无需修改”。
```

- [ ] **Step 2: 运行回归验证，确认 schema、后端、前端与构建链路全部通过**

Run:

```bash
python -m unittest scripts.tests.test_run_real_env_schema_sync
mvn -pl spring-boot-iot-device "-Dtest=ProductServiceImplTest" test
cd spring-boot-iot-ui && npm test -- --run \
  src/__tests__/utils/productObjectInsightConfig.test.ts \
  src/__tests__/components/product/ProductObjectInsightConfigEditor.test.ts \
  src/__tests__/components/product/ProductEditWorkspace.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  src/__tests__/utils/deviceInsightCapability.test.ts \
  src/__tests__/views/DeviceInsightView.test.ts
cd /Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/spring-boot-iot-ui && npm run build
cd /Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot && mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:
- Python unittest `OK`
- `ProductServiceImplTest` `BUILD SUCCESS`
- 以上 Vitest 全部 `PASS`
- `npm run build` 成功
- `mvn -pl spring-boot-iot-admin -am clean package -DskipTests` 成功，证明跨模块编译未被破坏

- [ ] **Step 3: 自检交付说明，确认文档与代码行为完全对齐**

Checklist:

```md
- 产品新增/编辑/详情 API 已能维护并返回 `metadataJson`
- 后端已拦截非法 `objectInsight.customMetrics`
- 产品定义中心新增“对象洞察配置”结构化编辑入口
- `/insight` 已补取产品详情并按“设备 > 产品 > 内置 > 自动识别”合并
- 对象洞察仍然只分析单设备，不回流风险点整体分析
- 已检查 `README.md` 与 `AGENTS.md` 是否需要更新，并在最终总结中说明结果
```

- [ ] **Step 4: 提交文档与最终联调收口改动**

```bash
git add docs/02-业务功能与流程说明.md \
        docs/03-接口规范与接口清单.md \
        docs/04-数据库设计与初始化数据.md \
        docs/06-前端开发与CSS规范.md \
        docs/08-变更记录与技术债清单.md \
        docs/15-前端优化与治理计划.md \
        docs/21-业务功能清单与验收标准.md
git commit -m "docs: document product insight configuration"
```

---

## Self-Review

### 1. Spec coverage

- **产品级正式配置承载层**：Task 1 + Task 2 实现 `iot_product.metadata_json` 与后端校验。
- **不新增独立表/独立菜单**：Architecture 与 Task 4 明确在现有产品定义中心内建设。
- **读取优先级 device > product > builtin > runtime**：Task 5 的合并顺序与测试已覆盖。
- **配置结构 `customMetrics[]` 与字段约束**：Task 2 后端校验、Task 3 前端类型/工具均已覆盖。
- **产品编辑入口**：Task 4 通过共享编辑器覆盖新增与编辑两条维护路径。
- **降级与兼容**：Task 5 明确产品详情失败降级、不影响对象洞察主成功路径。
- **文档同步**：Task 6 覆盖业务、接口、数据库、前端治理与验收文档。

### 2. Placeholder scan

- 未使用 `TODO`、`TBD`、`实现后补` 一类占位语。
- 每个改代码步骤都给出具体文件、具体代码片段与可执行命令。
- 每个任务都给出验证命令与预期结果。

### 3. Type consistency

- 后端统一使用 `metadataJson`（Java 字段名）对应数据库 `metadata_json`。
- 前端统一使用 `ProductObjectInsightCustomMetricConfig` 表示 `customMetrics[]` 行配置。
- `/insight` 工具层统一接收 `deviceMetadataJson` 与 `productMetadataJson`，避免继续复用单一 `metadataJson` 参数造成语义混淆。
