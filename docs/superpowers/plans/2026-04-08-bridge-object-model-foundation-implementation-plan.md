# Bridge Object Model Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the first post-spec implementation wave that formalizes bridge-layer metadata and vendor mapping rules while keeping `iot_product_model` as the released contract truth and preserving the real `application-dev.yml` acceptance baseline.

**Architecture:** This plan intentionally implements only the first sub-project from the 2026-04-08 governance object model spec: enrich the existing normative definitions, release batches, and risk metric catalog with durable bridge metadata, then add a first-class vendor mapping rule object and backend API in `spring-boot-iot-device`. The plan keeps UI changes minimal, reuses current release and risk governance controllers, and limits read-side exposure to the fields needed by future control-plane and task workbenches.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Vue 3, TypeScript, JUnit 5, Mockito, Maven

---

## Scope Check

The approved spec spans four independent follow-up tracks:

1. bridge-layer object model foundation
2. explicit linkage/plan bindings and coverage truth
3. control-plane tasks and workbenches
4. extensibility objects for protocol/decrypt/normalization generation rules

This plan covers only track 1. Do not pull task workbenches, replay cases, or protocol-family DSL work into this implementation. Those get their own plans after the bridge metadata and mapping-rule foundation are stable.

## File Structure

### Database / schema / sync

- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`

### Device domain

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRule.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleUpsertDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductContractReleaseBatchVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`

### Alarm / risk domain

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskMetricCatalogItemVO.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`

### Frontend typing

- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`

### Documentation

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

## Task 1: Extend released-contract and risk-metric bridge metadata

**Files:**
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`

- [ ] **Step 1: Write the failing bridge-metadata tests**

```java
@Test
void createBatchShouldPersistApprovalContextAndReleaseStatus() {
    ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(
            releaseBatchMapper,
            releaseSnapshotMapper,
            productModelMapper
    );

    Long batchId = service.createBatch(1001L, "phase1-crack", "manual_compare_apply", 3, 10001L, 88001L, "首次合同发布");

    assertNotNull(batchId);
    verify(releaseBatchMapper).insert(org.mockito.ArgumentMatchers.<ProductContractReleaseBatch>argThat(batch ->
            Long.valueOf(1001L).equals(batch.getProductId())
                    && "phase1-crack".equals(batch.getScenarioCode())
                    && "manual_compare_apply".equals(batch.getReleaseSource())
                    && Integer.valueOf(3).equals(batch.getReleasedFieldCount())
                    && Long.valueOf(10001L).equals(batch.getCreateBy())
                    && Long.valueOf(88001L).equals(batch.getApprovalOrderId())
                    && "首次合同发布".equals(batch.getReleaseReason())
                    && "RELEASED".equals(batch.getReleaseStatus())
    ));
}
```

```java
@Test
void publishFromReleasedContractShouldPersistReleaseBatchAndNormativeMetadata() {
    RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(
            riskMetricCatalogMapper,
            productMapper,
            normativeMetricDefinitionService,
            List.of(new KeywordRiskMetricScenarioResolver())
    );
    Product product = new Product();
    product.setId(1001L);
    product.setProductKey("phase1-crack-product");
    when(productMapper.selectById(1001L)).thenReturn(product);
    when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
            normative("phase1-crack", "value", "mm", 1,
                    "{\"thresholdKind\":\"absolute\",\"riskCategory\":\"CRACK\",\"metricRole\":\"PRIMARY\"}")
    ));

    ProductModel releasedValue = new ProductModel();
    releasedValue.setId(3101L);
    releasedValue.setProductId(1001L);
    releasedValue.setIdentifier("value");
    releasedValue.setModelName("Crack value");
    releasedValue.setDataType("double");
    releasedValue.setSpecsJson("{\"dimension\":\"displacement\"}");

    service.publishFromReleasedContracts(1001L, 7001L, List.of(releasedValue), Set.of("value"));

    verify(riskMetricCatalogMapper).insert(argThat((RiskMetricCatalog row) ->
            Long.valueOf(7001L).equals(row.getReleaseBatchId())
                    && "value".equals(row.getNormativeIdentifier())
                    && "CRACK".equals(row.getRiskCategory())
                    && "PRIMARY".equals(row.getMetricRole())
                    && "ACTIVE".equals(row.getLifecycleStatus())
                    && "value".equals(row.getContractIdentifier())
    ));
}
```

- [ ] **Step 2: Run tests to verify the new fields and signatures do not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductContractReleaseServiceImplTest,RiskMetricCatalogServiceImplTest test
```

Expected:
- `BUILD FAILURE`
- compile or assertion failures mentioning missing `approvalOrderId`, `releaseReason`, `releaseStatus`, `releaseBatchId`, `normativeIdentifier`, `riskCategory`, `metricRole`, or the new `publishFromReleasedContracts` signature

- [ ] **Step 3: Implement the schema, entity, and service-signature upgrades**

```sql
ALTER TABLE iot_normative_metric_definition
    ADD COLUMN metric_dimension VARCHAR(64) DEFAULT NULL COMMENT '量纲',
    ADD COLUMN threshold_type VARCHAR(32) DEFAULT NULL COMMENT '阈值类型',
    ADD COLUMN semantic_direction VARCHAR(32) DEFAULT NULL COMMENT '语义方向',
    ADD COLUMN gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持GIS',
    ADD COLUMN insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持对象洞察',
    ADD COLUMN analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持运营分析',
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    ADD COLUMN version_no INT NOT NULL DEFAULT 1 COMMENT '版本号';

ALTER TABLE iot_product_contract_release_batch
    ADD COLUMN approval_order_id BIGINT DEFAULT NULL COMMENT '审批单ID',
    ADD COLUMN release_reason VARCHAR(500) DEFAULT NULL COMMENT '发布说明',
    ADD COLUMN release_status VARCHAR(16) NOT NULL DEFAULT 'RELEASED' COMMENT 'RELEASED/ROLLED_BACK';

ALTER TABLE risk_metric_catalog
    ADD COLUMN release_batch_id BIGINT DEFAULT NULL COMMENT '来源发布批次',
    ADD COLUMN normative_identifier VARCHAR(64) DEFAULT NULL COMMENT '来源规范字段标识',
    ADD COLUMN risk_category VARCHAR(64) DEFAULT NULL COMMENT '风险指标类别',
    ADD COLUMN metric_role VARCHAR(32) DEFAULT NULL COMMENT 'PRIMARY/DERIVED/STATE',
    ADD COLUMN lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RETIRED';
```

```java
public interface ProductContractReleaseService {

    Long createBatch(Long productId,
                     String scenarioCode,
                     String releaseSource,
                     int releasedFieldCount,
                     Long operatorId,
                     Long approvalOrderId,
                     String releaseReason);
}
```

```java
public interface RiskMetricCatalogService {

    void publishFromReleasedContracts(Long productId,
                                      Long releaseBatchId,
                                      List<ProductModel> releasedContracts,
                                      Set<String> riskEnabledIdentifiers);
}
```

```java
batch.setApprovalOrderId(approvalOrderId);
batch.setReleaseReason(releaseReason);
batch.setReleaseStatus("RELEASED");
```

```java
row.setReleaseBatchId(releaseBatchId);
row.setNormativeIdentifier(identifier);
row.setRiskCategory(firstNonBlank(readString(metadata, "riskCategory"), profile.sourceScenarioCode()));
row.setMetricRole(firstNonBlank(readString(metadata, "metricRole"), "PRIMARY"));
row.setLifecycleStatus("ACTIVE");
```

Implementation rules:
- `ProductModelServiceImpl` passes the actual `approvalOrderId` into `createBatch(...)` once approval execution writes the real release batch.
- `RiskPointPendingRecommendationServiceImpl` calls `publishFromReleasedContracts(productId, null, ...)` and the service must accept `releaseBatchId=null` without throwing.
- `scripts/run-real-env-schema-sync.py` must add the same columns idempotently for historical environments.

- [ ] **Step 4: Run the same targeted tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductContractReleaseServiceImplTest,RiskMetricCatalogServiceImplTest,RiskPointPendingRecommendationServiceImplTest,ProductModelServiceImplTest test
```

Expected:
- `BUILD SUCCESS`
- updated tests pass
- no remaining compile errors from the changed method signatures

- [ ] **Step 5: Commit the bridge-metadata foundation**

```bash
git add sql/init.sql scripts/run-real-env-schema-sync.py \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java
git commit -m "feat: extend released contract bridge metadata"
```

## Task 2: Add the first-class vendor mapping rule object and backend CRUD

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRule.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleUpsertDTO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleVO.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java`
- Modify: `sql/init.sql`
- Modify: `scripts/run-real-env-schema-sync.py`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java`

- [ ] **Step 1: Write the failing service and controller tests for mapping rules**

```java
@ExtendWith(MockitoExtension.class)
class VendorMetricMappingRuleServiceImplTest {

    @Mock
    private VendorMetricMappingRuleMapper mapper;

    @Test
    void createRuleShouldNormalizeIdentifiersAndPersistProductScope() {
        VendorMetricMappingRuleUpsertDTO dto = new VendorMetricMappingRuleUpsertDTO();
        dto.setScopeType("PRODUCT");
        dto.setProtocolCode("mqtt-json");
        dto.setScenarioCode("phase1-crack");
        dto.setRawIdentifier(" TEMP_A ");
        dto.setLogicalChannelCode("L1_LF_1");
        dto.setTargetNormativeIdentifier("value");
        dto.setNormalizationRuleJson("{\"unit\":\"mm\",\"transform\":\"identity\"}");

        VendorMetricMappingRuleServiceImpl service = new VendorMetricMappingRuleServiceImpl(mapper);

        Long ruleId = service.createRule(1001L, 10001L, dto);

        assertNotNull(ruleId);
        verify(mapper).insert(argThat(rule ->
                Long.valueOf(1001L).equals(rule.getProductId())
                        && "PRODUCT".equals(rule.getScopeType())
                        && "mqtt-json".equals(rule.getProtocolCode())
                        && "phase1-crack".equals(rule.getScenarioCode())
                        && "temp_a".equals(rule.getRawIdentifier())
                        && "L1_LF_1".equals(rule.getLogicalChannelCode())
                        && "value".equals(rule.getTargetNormativeIdentifier())
                        && "DRAFT".equals(rule.getStatus())
                        && Integer.valueOf(1).equals(rule.getVersionNo())
        ));
    }
}
```

```java
@Test
void pageRulesShouldDelegateToService() {
    VendorMetricMappingRuleService service = mock(VendorMetricMappingRuleService.class);
    VendorMetricMappingRuleController controller = new VendorMetricMappingRuleController(service);
    VendorMetricMappingRuleVO row = new VendorMetricMappingRuleVO();
    row.setId(9201L);
    row.setRawIdentifier("temp_a");
    when(service.pageRules(1001L, "ACTIVE", 1L, 10L))
            .thenReturn(PageResult.of(1L, 1L, 10L, List.of(row)));

    R<PageResult<VendorMetricMappingRuleVO>> response = controller.pageRules(1001L, "ACTIVE", 1L, 10L);

    assertEquals(1L, response.getData().getTotal());
    assertEquals("temp_a", response.getData().getRecords().get(0).getRawIdentifier());
}
```

- [ ] **Step 2: Run the new mapping-rule tests to verify the object does not exist yet**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=VendorMetricMappingRuleServiceImplTest,VendorMetricMappingRuleControllerTest test
```

Expected:
- `BUILD FAILURE`
- failures mention missing mapping-rule classes, methods, or SQL-backed entity fields

- [ ] **Step 3: Implement the mapping-rule table, entity, service, and controller**

```sql
CREATE TABLE iot_vendor_metric_mapping_rule (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    scope_type VARCHAR(32) NOT NULL COMMENT 'PRODUCT/PROTOCOL/SCENARIO',
    product_id BIGINT DEFAULT NULL,
    protocol_code VARCHAR(64) DEFAULT NULL,
    scenario_code VARCHAR(64) DEFAULT NULL,
    device_family VARCHAR(64) DEFAULT NULL,
    raw_identifier VARCHAR(128) NOT NULL,
    logical_channel_code VARCHAR(64) DEFAULT NULL,
    relation_condition_json JSON DEFAULT NULL,
    normalization_rule_json JSON DEFAULT NULL,
    target_normative_identifier VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    version_no INT NOT NULL DEFAULT 1,
    approval_order_id BIGINT DEFAULT NULL,
    create_by BIGINT DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段映射规则表';
```

```java
@Data
@TableName("iot_vendor_metric_mapping_rule")
public class VendorMetricMappingRule {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String scopeType;
    private Long productId;
    private String protocolCode;
    private String scenarioCode;
    private String deviceFamily;
    private String rawIdentifier;
    private String logicalChannelCode;
    private String relationConditionJson;
    private String normalizationRuleJson;
    private String targetNormativeIdentifier;
    private String status;
    private Integer versionNo;
    private Long approvalOrderId;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer deleted;
}
```

```java
@RestController
public class VendorMetricMappingRuleController {

    @GetMapping("/api/device/product/{productId}/vendor-mapping-rules")
    public R<PageResult<VendorMetricMappingRuleVO>> pageRules(@PathVariable Long productId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Long pageNum,
                                                              @RequestParam(required = false) Long pageSize) {
        return R.ok(service.pageRules(productId, status, pageNum, pageSize));
    }

    @PostMapping("/api/device/product/{productId}/vendor-mapping-rules")
    public R<VendorMetricMappingRuleVO> addRule(@PathVariable Long productId,
                                                @RequestBody @Valid VendorMetricMappingRuleUpsertDTO dto,
                                                Authentication authentication) {
        return R.ok(service.createAndGet(productId, currentUserId(authentication), dto));
    }

    @PutMapping("/api/device/product/{productId}/vendor-mapping-rules/{ruleId}")
    public R<VendorMetricMappingRuleVO> updateRule(@PathVariable Long productId,
                                                   @PathVariable Long ruleId,
                                                   @RequestBody @Valid VendorMetricMappingRuleUpsertDTO dto,
                                                   Authentication authentication) {
        return R.ok(service.updateAndGet(productId, ruleId, currentUserId(authentication), dto));
    }
}
```

Implementation rules:
- Normalize `rawIdentifier` with `trim().toLowerCase(Locale.ROOT)`.
- Default `status` to `DRAFT`.
- Scope the first API wave to product-bound rules; `scopeType` still persists so later waves can expand to protocol or scenario scope without table redesign.
- Reuse existing product governance permissions for write operations; do not add a new permission seed in this wave.

- [ ] **Step 4: Run the mapping-rule tests until they pass**

Run:

```bash
mvn -pl spring-boot-iot-device -am -Dtest=VendorMetricMappingRuleServiceImplTest,VendorMetricMappingRuleControllerTest test
```

Expected:
- `BUILD SUCCESS`
- controller test returns page/create/update responses from the new backend object

- [ ] **Step 5: Commit the mapping-rule backend**

```bash
git add sql/init.sql scripts/run-real-env-schema-sync.py \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricMappingRule.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/dto/VendorMetricMappingRuleUpsertDTO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/VendorMetricMappingRuleService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricMappingRuleMapper.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/VendorMetricMappingRuleVO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleController.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/VendorMetricMappingRuleServiceImplTest.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/VendorMetricMappingRuleControllerTest.java
git commit -m "feat: add vendor metric mapping rule backend"
```

## Task 3: Expose bridge metadata through existing read-side APIs and frontend typings

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductContractReleaseBatchVO.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskMetricCatalogItemVO.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`

- [ ] **Step 1: Write the failing read-side tests for the new metadata**

```java
@Test
void getBatchShouldExposeApprovalContext() {
    ProductContractReleaseBatchVO batch = batchVO(7001L, "phase1-crack", "manual_compare_apply", 3);
    batch.setApprovalOrderId(88001L);
    batch.setReleaseReason("首次合同发布");
    batch.setReleaseStatus("RELEASED");
    when(productContractReleaseService.getBatch(7001L)).thenReturn(batch);

    R<ProductContractReleaseBatchVO> response = controller.getBatch(7001L);

    assertEquals(88001L, response.getData().getApprovalOrderId());
    assertEquals("首次合同发布", response.getData().getReleaseReason());
    assertEquals("RELEASED", response.getData().getReleaseStatus());
}
```

```java
@Test
void getMetricCatalogShouldExposeBridgeMetadata() {
    RiskGovernanceService service = mock(RiskGovernanceService.class);
    RiskGovernanceOpsService opsService = mock(RiskGovernanceOpsService.class);
    RiskGovernanceController controller = new RiskGovernanceController(service, opsService);
    RiskMetricCatalogItemVO item = new RiskMetricCatalogItemVO();
    item.setId(9101L);
    item.setRiskMetricCode("RM_1001_VALUE");
    item.setReleaseBatchId(7001L);
    item.setNormativeIdentifier("value");
    item.setRiskCategory("CRACK");
    item.setMetricRole("PRIMARY");
    when(service.getMetricCatalog(9101L)).thenReturn(item);

    R<RiskMetricCatalogItemVO> response = controller.getMetricCatalog(9101L);

    assertEquals(7001L, response.getData().getReleaseBatchId());
    assertEquals("value", response.getData().getNormativeIdentifier());
    assertEquals("CRACK", response.getData().getRiskCategory());
    assertEquals("PRIMARY", response.getData().getMetricRole());
}
```

- [ ] **Step 2: Run the read-side tests to verify the VOs and mappers do not expose the fields yet**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductContractReleaseControllerTest,RiskGovernanceControllerTest test
```

Expected:
- `BUILD FAILURE`
- missing-field compile failures or assertion failures from absent VO properties and service mapping

- [ ] **Step 3: Implement the VO, service-mapping, and frontend type updates**

```java
@Data
public class ProductContractReleaseBatchVO {
    private Long id;
    private Long productId;
    private String scenarioCode;
    private String releaseSource;
    private Integer releasedFieldCount;
    private Long createBy;
    private LocalDateTime createTime;
    private Long rollbackBy;
    private LocalDateTime rollbackTime;
    private Long approvalOrderId;
    private String releaseReason;
    private String releaseStatus;
}
```

```java
@Data
public class RiskMetricCatalogItemVO {
    private Long id;
    private Long productId;
    private Long productModelId;
    private Long releaseBatchId;
    private String contractIdentifier;
    private String normativeIdentifier;
    private String riskMetricCode;
    private String riskMetricName;
    private String riskCategory;
    private String metricRole;
    private String lifecycleStatus;
}
```

```ts
export interface ProductContractReleaseBatch {
  id?: IdType | null
  productId?: IdType | null
  scenarioCode?: string | null
  releaseSource?: string | null
  releasedFieldCount?: number | null
  createBy?: IdType | null
  createTime?: string | null
  approvalOrderId?: IdType | null
  releaseReason?: string | null
  releaseStatus?: string | null
}
```

```ts
export interface RiskMetricCatalogItem {
  id?: IdType | null;
  productId?: IdType | null;
  productModelId?: IdType | null;
  releaseBatchId?: IdType | null;
  contractIdentifier?: string | null;
  normativeIdentifier?: string | null;
  riskMetricCode?: string | null;
  riskMetricName?: string | null;
  riskCategory?: string | null;
  metricRole?: string | null;
  lifecycleStatus?: string | null;
}
```

- [ ] **Step 4: Re-run the controller tests and a frontend build**

Run:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -am -Dtest=ProductContractReleaseControllerTest,RiskGovernanceControllerTest test
cd spring-boot-iot-ui && npm run build
```

Expected:
- controller tests pass
- frontend TypeScript build accepts the new response shapes

- [ ] **Step 5: Commit the read-side exposure**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductContractReleaseBatchVO.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductContractReleaseControllerTest.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/vo/RiskMetricCatalogItemVO.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskGovernanceControllerTest.java \
  spring-boot-iot-ui/src/api/product.ts \
  spring-boot-iot-ui/src/api/riskGovernance.ts
git commit -m "feat: expose bridge metadata in read apis"
```

## Task 4: Update architecture and database docs to match the new object boundaries

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Review: `README.md`
- Review: `AGENTS.md`

- [ ] **Step 1: Update docs for the new bridge metadata and mapping rule object**

```md
- `iot_product_contract_release_batch` 新增 `approval_order_id / release_reason / release_status`，用于把“待审批请求”和“审批通过后的真实发布批次”关联起来。
- `risk_metric_catalog` 新增 `release_batch_id / normative_identifier / risk_category / metric_role / lifecycle_status`，作为正式合同与风险闭环之间的桥层元数据。
- 新增 `iot_vendor_metric_mapping_rule`，用于把厂商字段证据沉淀为正式可复用映射规则；`iot_vendor_metric_evidence` 继续只承载证据，不承载正式规则。
```

- [ ] **Step 2: Run doc and codebase verification**

Run:

```bash
node scripts/docs/check-topology.mjs
rg -n "iot_vendor_metric_mapping_rule|release_batch_id|normative_identifier|release_status" docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md
```

Expected:
- docs topology check passes
- each new object or field is discoverable in the updated docs

- [ ] **Step 3: Run the final targeted regression and package build**

Run:

```bash
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
cd spring-boot-iot-ui && npm run build
node scripts/docs/check-topology.mjs
```

Expected:
- Maven package succeeds
- frontend build succeeds
- doc topology remains clean

- [ ] **Step 4: Review whether `README.md` and `AGENTS.md` need wording updates**

Review checklist:
- `README.md` only changes if the feature summary or quick architecture bullets now need to mention “风险指标桥层”和“厂商映射规则对象”
- `AGENTS.md` only changes if task-start,验收或文档维护规则需要新增对象级口径

- [ ] **Step 5: Commit the documentation wave**

```bash
git add docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md
git commit -m "docs: document bridge object model foundation"
```

## Self-Review Checklist

1. Spec coverage:
   - bridge metadata on normative definitions, release batches, and risk metric catalog: covered in Task 1
   - first-class vendor mapping rule object: covered in Task 2
   - read-side exposure for future control-plane consumers: covered in Task 3
   - documentation sync: covered in Task 4
2. Placeholder scan:
   - no `TODO`/`TBD` placeholders should remain in the plan
   - every code-changing step includes concrete SQL, Java, or TypeScript snippets
3. Type consistency:
   - `createBatch(...)` and `publishFromReleasedContracts(...)` signatures must be updated consistently in service interfaces, implementations, and tests
   - `releaseBatchId / normativeIdentifier / riskCategory / metricRole / lifecycleStatus` must use the same names in entities, VOs, API types, and docs

## Completion Criteria

This plan is complete when:

1. `iot_vendor_metric_mapping_rule` exists in schema, sync script, entity, mapper, service, and controller.
2. `iot_product_contract_release_batch` exposes approval and release metadata.
3. `risk_metric_catalog` stores release-batch and normative bridge metadata.
4. Existing release and risk governance read APIs expose the new fields without breaking current consumers.
5. Maven packaging, frontend build, and doc topology checks all pass.
