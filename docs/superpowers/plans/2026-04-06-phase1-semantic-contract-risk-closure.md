# Phase 1 Semantic Contract Risk Closure Slice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the approved phase-1 crack-monitoring slice that separates normative metric definitions, vendor evidence mapping, released product contracts, and risk metric consumption across the device and alarm domains while preserving the real `application-dev.yml` acceptance baseline.

**Architecture:** The implementation keeps `iot_product_model` as the released contract table, adds a normative catalog and vendor evidence layer in `spring-boot-iot-device`, emits crack raw/canonical evidence from `spring-boot-iot-protocol`, and introduces a risk metric catalog bridge in `spring-boot-iot-alarm` so risk points and policies stop binding raw vendor fields directly. The slice is intentionally limited to crack direct-report products and collector-child crack products; GNSS, deep displacement, and generic zero-code onboarding remain separate follow-up plans.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, Redis, Vue 3, Element Plus, Vitest, JUnit 5, Mockito

---

## Scope Check

The approved spec covers multiple future waves. This plan intentionally implements only the first working, testable slice:

1. crack direct-report products
2. collector-child crack products using `collector_child + LF_VALUE + SENSOR_STATE`
3. released contract -> risk metric catalog -> risk point / rule consumption

Do not pull GNSS, deep displacement, or full vendor self-service onboarding into this implementation. Those need separate plans after this slice is stable in the real dev environment.

## File Structure

### Database / schema / seeds

- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`
- Modify: `scripts/run-real-env-schema-sync.py`

### Device domain (`spring-boot-iot-device`)

- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricEvidence.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/NormativeMetricDefinitionMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricEvidenceMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductContractReleaseBatchMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/NormativeMetricDefinitionService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductMetricEvidenceService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductMetricEvidenceServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java`

### Protocol domain (`spring-boot-iot-protocol`)

- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/ProtocolMetricEvidence.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/template/LegacyDpCrackChildTemplate.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpPropertyNormalizerTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildTemplateFrameworkTest.java`

### Alarm / risk domain (`spring-boot-iot-alarm`)

- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricCatalogMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevice.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RuleDefinition.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`

### Frontend (`spring-boot-iot-ui`)

- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

### Documentation

- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md` (update only if the phase-1 slice changes quick-start, runtime baseline, or feature summary wording)
- Review: `AGENTS.md` (update only if the phase-1 slice changes branch/runtime/doc-sync rules or accepted phase wording)

## Task 1: Add normative contract and release-batch foundations in `device`

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricEvidence.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/NormativeMetricDefinitionMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricEvidenceMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductContractReleaseBatchMapper.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/NormativeMetricDefinitionService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImplTest.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java`
- Modify: `sql/init.sql`
- Modify: `sql/init-data.sql`

- [ ] **Step 1: Write the failing normative catalog test**

```java
@ExtendWith(MockitoExtension.class)
class NormativeMetricDefinitionServiceImplTest {

    @Mock
    private NormativeMetricDefinitionMapper normativeMetricDefinitionMapper;

    @Test
    void listPhaseOneCrackDefinitionsShouldExposeCanonicalRiskEligibleFields() {
        NormativeMetricDefinition value = new NormativeMetricDefinition();
        value.setScenarioCode("phase1-crack");
        value.setIdentifier("value");
        value.setDisplayName("裂缝监测值");
        value.setRiskEnabled(1);

        NormativeMetricDefinition sensorState = new NormativeMetricDefinition();
        sensorState.setScenarioCode("phase1-crack");
        sensorState.setIdentifier("sensor_state");
        sensorState.setDisplayName("传感器状态");
        sensorState.setRiskEnabled(0);

        when(normativeMetricDefinitionMapper.selectList(any())).thenReturn(List.of(value, sensorState));

        NormativeMetricDefinitionServiceImpl service =
                new NormativeMetricDefinitionServiceImpl(normativeMetricDefinitionMapper);

        List<NormativeMetricDefinition> rows = service.listByScenario("phase1-crack");

        assertEquals(List.of("value", "sensor_state"), rows.stream().map(NormativeMetricDefinition::getIdentifier).toList());
        assertEquals(1, rows.get(0).getRiskEnabled());
        assertEquals(0, rows.get(1).getRiskEnabled());
    }
}
```

- [ ] **Step 2: Write the failing release-batch test**

```java
@ExtendWith(MockitoExtension.class)
class ProductContractReleaseServiceImplTest {

    @Mock
    private ProductContractReleaseBatchMapper releaseBatchMapper;

    @Test
    void createBatchShouldPersistProductVersionMetadata() {
        ProductContractReleaseServiceImpl service = new ProductContractReleaseServiceImpl(releaseBatchMapper);

        Long batchId = service.createBatch(1001L, "phase1-crack", "manual_compare_apply", 3, 10001L);

        assertNotNull(batchId);
        verify(releaseBatchMapper).insert(argThat(batch ->
                Long.valueOf(1001L).equals(batch.getProductId())
                        && "phase1-crack".equals(batch.getScenarioCode())
                        && "manual_compare_apply".equals(batch.getReleaseSource())
                        && Integer.valueOf(3).equals(batch.getReleasedFieldCount())
                        && Long.valueOf(10001L).equals(batch.getCreateBy())
        ));
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-device -Dtest=NormativeMetricDefinitionServiceImplTest,ProductContractReleaseServiceImplTest test`

Expected: `BUILD FAILURE` with errors such as `cannot find symbol` for `NormativeMetricDefinition`, `ProductContractReleaseBatchMapper`, or the new service classes.

- [ ] **Step 4: Add the schema, entities, mappers, and service classes**

```sql
CREATE TABLE iot_normative_metric_definition (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT '治理场景编码',
    device_family VARCHAR(64) NOT NULL COMMENT '设备族编码',
    identifier VARCHAR(64) NOT NULL COMMENT '规范字段标识',
    display_name VARCHAR(128) NOT NULL COMMENT '规范字段名称',
    unit VARCHAR(32) DEFAULT NULL COMMENT '单位',
    precision_digits INT DEFAULT NULL COMMENT '精度',
    monitor_content_code VARCHAR(32) DEFAULT NULL COMMENT '监测内容编码',
    monitor_type_code VARCHAR(32) DEFAULT NULL COMMENT '监测类型编码',
    risk_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许进入风险闭环',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许趋势分析',
    metadata_json JSON DEFAULT NULL COMMENT '扩展元数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_normative_metric_scenario_identifier (scenario_code, identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规范字段定义表';

CREATE TABLE iot_vendor_metric_evidence (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    parent_device_code VARCHAR(64) DEFAULT NULL COMMENT '父设备编码',
    child_device_code VARCHAR(64) DEFAULT NULL COMMENT '子设备编码',
    raw_identifier VARCHAR(128) NOT NULL COMMENT '原始字段标识',
    canonical_identifier VARCHAR(64) DEFAULT NULL COMMENT '建议规范字段标识',
    logical_channel_code VARCHAR(64) DEFAULT NULL COMMENT '逻辑通道编码',
    evidence_origin VARCHAR(32) NOT NULL COMMENT '证据来源',
    sample_value VARCHAR(255) DEFAULT NULL COMMENT '样例值',
    value_type VARCHAR(32) DEFAULT NULL COMMENT '值类型',
    evidence_count INT NOT NULL DEFAULT 0 COMMENT '命中次数',
    last_seen_time DATETIME DEFAULT NULL COMMENT '最后出现时间',
    metadata_json JSON DEFAULT NULL COMMENT '扩展元数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_metric_evidence (product_id, raw_identifier, logical_channel_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='厂商字段证据表';

CREATE TABLE iot_product_contract_release_batch (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    scenario_code VARCHAR(64) NOT NULL COMMENT '治理场景编码',
    release_source VARCHAR(64) NOT NULL COMMENT '发布来源',
    released_field_count INT NOT NULL DEFAULT 0 COMMENT '发布字段数',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同发布批次表';
```

```java
@Data
@TableName("iot_normative_metric_definition")
public class NormativeMetricDefinition {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String scenarioCode;
    private String deviceFamily;
    private String identifier;
    private String displayName;
    private String unit;
    private Integer precisionDigits;
    private String monitorContentCode;
    private String monitorTypeCode;
    private Integer riskEnabled;
    private Integer trendEnabled;
    private String metadataJson;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}

@Service
public class NormativeMetricDefinitionServiceImpl implements NormativeMetricDefinitionService {
    private final NormativeMetricDefinitionMapper mapper;

    public NormativeMetricDefinitionServiceImpl(NormativeMetricDefinitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<NormativeMetricDefinition> listByScenario(String scenarioCode) {
        return mapper.selectList(new LambdaQueryWrapper<NormativeMetricDefinition>()
                .eq(NormativeMetricDefinition::getDeleted, 0)
                .eq(NormativeMetricDefinition::getScenarioCode, scenarioCode)
                .orderByAsc(NormativeMetricDefinition::getIdentifier));
    }
}
```

```java
public interface ProductContractReleaseService {
    Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId);
}

@Service
public class ProductContractReleaseServiceImpl implements ProductContractReleaseService {
    private final ProductContractReleaseBatchMapper releaseBatchMapper;

    public ProductContractReleaseServiceImpl(ProductContractReleaseBatchMapper releaseBatchMapper) {
        this.releaseBatchMapper = releaseBatchMapper;
    }

    @Override
    public Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId) {
        ProductContractReleaseBatch batch = new ProductContractReleaseBatch();
        batch.setProductId(productId);
        batch.setScenarioCode(scenarioCode);
        batch.setReleaseSource(releaseSource);
        batch.setReleasedFieldCount(releasedFieldCount);
        batch.setCreateBy(operatorId);
        releaseBatchMapper.insert(batch);
        return batch.getId();
    }
}
```

- [ ] **Step 5: Seed the phase-1 crack normative catalog**

```sql
INSERT INTO iot_normative_metric_definition (
    id, tenant_id, scenario_code, device_family, identifier, display_name, unit,
    precision_digits, monitor_content_code, monitor_type_code, risk_enabled, trend_enabled, metadata_json
) VALUES
    (920001, 1, 'phase1-crack', 'CRACK', 'value', '裂缝监测值', 'mm', 4, 'L1', 'LF', 1, 1, JSON_OBJECT('thresholdKind', 'absolute')),
    (920002, 1, 'phase1-crack', 'CRACK', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0, JSON_OBJECT('usage', 'health_state'))
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    unit = VALUES(unit),
    precision_digits = VALUES(precision_digits),
    risk_enabled = VALUES(risk_enabled),
    trend_enabled = VALUES(trend_enabled),
    metadata_json = VALUES(metadata_json);
```

- [ ] **Step 6: Run the targeted device tests again**

Run: `mvn -pl spring-boot-iot-device -Dtest=NormativeMetricDefinitionServiceImplTest,ProductContractReleaseServiceImplTest test`

Expected: `BUILD SUCCESS` and both new tests pass.

- [ ] **Step 7: Commit**

```bash
git add sql/init.sql sql/init-data.sql \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/NormativeMetricDefinition.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/VendorMetricEvidence.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/entity/ProductContractReleaseBatch.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/NormativeMetricDefinitionMapper.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/VendorMetricEvidenceMapper.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/mapper/ProductContractReleaseBatchMapper.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/NormativeMetricDefinitionService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductContractReleaseService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImpl.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/NormativeMetricDefinitionServiceImplTest.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductContractReleaseServiceImplTest.java
git commit -m "feat: add normative contract catalog foundation"
```

## Task 2: Refactor product governance compare/apply around normative matching and release batches

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductMetricEvidenceService.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductMetricEvidenceServiceImpl.java`
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`

- [ ] **Step 1: Extend the product governance service test with the new normative fields**

```java
@Test
void compareGovernanceShouldDecorateCrackRowsWithNormativeAndRiskMetadata() {
    when(productMapper.selectById(2002L)).thenReturn(product(2002L, "south-crack-sensor-v1", "裂缝监测仪"));
    when(productModelMapper.selectList(any())).thenReturn(List.of());

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("business");
    manualExtract.setDeviceStructure("composite");
    manualExtract.setParentDeviceCode("SK00EA0D1307986");
    manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018143")));
    manualExtract.setSamplePayload("""
            {"SK00EA0D1307986":{"L1_LF_1":{"2026-04-05T20:34:06.000Z":10.86}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(2002L, dto);

    ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
    assertEquals("value", row.getIdentifier());
    assertEquals("value", row.getNormativeIdentifier());
    assertEquals("裂缝监测值", row.getNormativeName());
    assertTrue(row.getRiskReady());
    assertEquals(List.of("L1_LF_1"), row.getRawIdentifiers());
}
```

- [ ] **Step 2: Add the failing apply test for release batch metadata**

```java
@Test
void applyGovernanceShouldReturnReleaseBatchIdAfterPublishingFormalFields() {
    when(productMapper.selectById(1001L)).thenReturn(product(1001L));
    when(productModelMapper.selectOne(any())).thenReturn(null);

    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(applyItem("property", "value", "裂缝监测值", "double")));

    ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(1001L, dto);

    assertNotNull(result.getReleaseBatchId());
    assertEquals(1, result.getCreatedCount());
}
```

- [ ] **Step 3: Run the device governance tests to verify they fail**

Run: `mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest,ProductModelControllerTest test`

Expected: `BUILD FAILURE` with missing getters like `getNormativeIdentifier()`, `getRiskReady()`, or `getReleaseBatchId()`.

- [ ] **Step 4: Add a focused normative matcher and evidence persistence service**

```java
public record NormativeMatchResult(
        String normativeIdentifier,
        String normativeName,
        boolean riskReady,
        List<String> rawIdentifiers
) {
}

final class ProductModelNormativeMatcher {

    NormativeMatchResult matchCrackProperty(String canonicalIdentifier, List<String> rawIdentifiers, List<NormativeMetricDefinition> definitions) {
        NormativeMetricDefinition definition = definitions.stream()
                .filter(item -> canonicalIdentifier.equals(item.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new BizException("未找到规范字段定义: " + canonicalIdentifier));
        return new NormativeMatchResult(
                definition.getIdentifier(),
                definition.getDisplayName(),
                Integer.valueOf(1).equals(definition.getRiskEnabled()),
                rawIdentifiers == null ? List.of() : rawIdentifiers
        );
    }
}

public interface ProductMetricEvidenceService {
    void replaceManualEvidence(Long productId, String scenarioCode, List<VendorMetricEvidence> evidences);
}
```

- [ ] **Step 5: Wire the matcher and release batch into `ProductModelServiceImpl` and API responses**

```java
row.setNormativeIdentifier(match.normativeIdentifier());
row.setNormativeName(match.normativeName());
row.setRiskReady(match.riskReady());
row.setRawIdentifiers(match.rawIdentifiers());

Long releaseBatchId = productContractReleaseService.createBatch(
        productId,
        "phase1-crack",
        "manual_compare_apply",
        createdCount + updatedCount,
        0L
);
result.setReleaseBatchId(releaseBatchId);
```

```java
public class ProductModelGovernanceApplyResultVO {
    private Integer createdCount;
    private Integer updatedCount;
    private Integer skippedCount;
    private Integer conflictCount;
    private LocalDateTime lastAppliedAt;
    private Long releaseBatchId;
    private List<ProductModelGovernanceAppliedItemVO> appliedItems;
}
```

- [ ] **Step 6: Re-run the device governance tests**

Run: `mvn -pl spring-boot-iot-device -Dtest=ProductModelServiceImplTest,ProductModelControllerTest test`

Expected: `BUILD SUCCESS`, and the compare/apply contract tests now pass with the new metadata fields.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductMetricEvidenceService.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductMetricEvidenceServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductModelController.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareRowVO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceCompareVO.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductModelGovernanceApplyResultVO.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java
git commit -m "feat: refactor product governance around normative evidence"
```

## Task 3: Emit crack raw/canonical evidence from the protocol layer

**Files:**
- Create: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/ProtocolMetricEvidence.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java`
- Modify: `spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/template/LegacyDpCrackChildTemplate.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpPropertyNormalizerTest.java`
- Modify: `spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildTemplateFrameworkTest.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductMetricEvidenceServiceImpl.java`

- [ ] **Step 1: Add the failing property normalizer test for crack evidence**

```java
@Test
void shouldEmitRawAndCanonicalEvidenceForCrackFamilies() {
    Object familyResolver = newInstance(
            "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpFamilyResolver",
            new Class<?>[0]
    );
    Object normalizer = newInstance(
            "com.ghlzm.iot.protocol.mqtt.legacy.LegacyDpPropertyNormalizer",
            new Class<?>[]{familyResolver.getClass()},
            familyResolver
    );

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("GW001", Map.of(
            "L1_LF_1", timestampPayload(Map.of("value", 10.86)),
            "S1_ZT_1", timestampPayload(Map.of("sensor_state", Map.of("L1_LF_1", 1)))
    ));

    Object result = invoke(normalizer, "normalize", payload, "GW001");
    @SuppressWarnings("unchecked")
    List<Object> evidence = (List<Object>) invoke(result, "getMetricEvidence");

    assertEquals(2, evidence.size());
    assertTrue(String.valueOf(invoke(evidence.get(0), "getRawIdentifier")).contains("L1_LF_1"));
    assertEquals("value", invoke(evidence.get(0), "getCanonicalIdentifier"));
}
```

- [ ] **Step 2: Run the protocol tests to verify they fail**

Run: `mvn -pl spring-boot-iot-protocol -Dtest=LegacyDpPropertyNormalizerTest,LegacyDpChildTemplateFrameworkTest test`

Expected: `BUILD FAILURE` because `ProtocolMetricEvidence` and `getMetricEvidence()` do not exist yet.

- [ ] **Step 3: Add the protocol evidence model and attach it to `$dp` normalization**

```java
@Data
public class ProtocolMetricEvidence {
    private String rawIdentifier;
    private String canonicalIdentifier;
    private String logicalChannelCode;
    private String parentDeviceCode;
    private String childDeviceCode;
    private String sampleValue;
    private String valueType;
    private String evidenceOrigin;
}
```

```java
@Data
public class LegacyDpNormalizeResult {
    private Map<String, Object> properties = new LinkedHashMap<>();
    private LocalDateTime timestamp;
    private String timestampSource;
    private String messageType;
    private List<String> familyCodes = List.of();
    private List<DeviceUpMessage> childMessages = List.of();
    private Boolean childSplitApplied = Boolean.FALSE;
    private ProtocolTemplateEvidence templateEvidence;
    private List<ProtocolMetricEvidence> metricEvidence = List.of();
}
```

- [ ] **Step 4: Populate crack evidence in the normalizer and child template**

```java
List<ProtocolMetricEvidence> evidence = new ArrayList<>();
evidence.add(metricEvidence("L1_LF_1", "value", "L1_LF_1", resolvedDeviceCode, null, "10.86", "double", "legacy_dp_normalizer"));
evidence.add(metricEvidence("S1_ZT_1.sensor_state.L1_LF_1", "sensor_state", "L1_LF_1", resolvedDeviceCode, "202018143", "1", "integer", "legacy_dp_child_template"));
result.setMetricEvidence(evidence);
```

- [ ] **Step 5: Persist protocol evidence into the new vendor evidence table**

```java
VendorMetricEvidence row = new VendorMetricEvidence();
row.setProductId(productId);
row.setParentDeviceCode(protocolEvidence.getParentDeviceCode());
row.setChildDeviceCode(protocolEvidence.getChildDeviceCode());
row.setRawIdentifier(protocolEvidence.getRawIdentifier());
row.setCanonicalIdentifier(protocolEvidence.getCanonicalIdentifier());
row.setLogicalChannelCode(protocolEvidence.getLogicalChannelCode());
row.setEvidenceOrigin(protocolEvidence.getEvidenceOrigin());
row.setSampleValue(protocolEvidence.getSampleValue());
row.setValueType(protocolEvidence.getValueType());
row.setLastSeenTime(reportTime);
```

- [ ] **Step 6: Re-run the protocol tests**

Run: `mvn -pl spring-boot-iot-protocol -Dtest=LegacyDpPropertyNormalizerTest,LegacyDpChildTemplateFrameworkTest test`

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/core/model/ProtocolMetricEvidence.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpNormalizeResult.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpPropertyNormalizer.java \
  spring-boot-iot-protocol/src/main/java/com/ghlzm/iot/protocol/mqtt/legacy/template/LegacyDpCrackChildTemplate.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/LegacyDpPropertyNormalizerTest.java \
  spring-boot-iot-protocol/src/test/java/com/ghlzm/iot/protocol/mqtt/legacy/LegacyDpChildTemplateFrameworkTest.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductMetricEvidenceServiceImpl.java
git commit -m "feat: emit crack protocol evidence"
```

## Task 4: Introduce the risk metric catalog bridge and stop binding raw vendor fields

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricCatalogMapper.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevice.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RuleDefinition.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`
- Modify: `sql/init.sql`

- [ ] **Step 1: Add the failing catalog publication test**

```java
@ExtendWith(MockitoExtension.class)
class RiskMetricCatalogServiceImplTest {

    @Mock
    private RiskMetricCatalogMapper riskMetricCatalogMapper;

    @Test
    void publishFromReleasedContractShouldCreateCrackRiskMetricRows() {
        RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(riskMetricCatalogMapper);

        ProductModel releasedValue = new ProductModel();
        releasedValue.setId(3101L);
        releasedValue.setProductId(1001L);
        releasedValue.setIdentifier("value");
        releasedValue.setModelName("裂缝监测值");

        ProductModel releasedSensorState = new ProductModel();
        releasedSensorState.setId(3102L);
        releasedSensorState.setProductId(1001L);
        releasedSensorState.setIdentifier("sensor_state");
        releasedSensorState.setModelName("传感器状态");

        service.publishFromReleasedContracts(1001L, List.of(releasedValue, releasedSensorState), Set.of("value"));

        verify(riskMetricCatalogMapper).insert(argThat(row ->
                Long.valueOf(1001L).equals(row.getProductId())
                        && "value".equals(row.getContractIdentifier())
                        && "裂缝监测值".equals(row.getRiskMetricName())
                        && Integer.valueOf(1).equals(row.getEnabled())
        ));
        verify(riskMetricCatalogMapper, never()).insert(argThat(row -> "sensor_state".equals(row.getContractIdentifier())));
    }
}
```

- [ ] **Step 2: Extend the pending promotion test so the formal binding stores `riskMetricId`**

```java
verify(fixture.riskPointService).bindDeviceAndReturn(argThat(
        binding -> "dispsX".equals(binding.getMetricIdentifier())
                && Long.valueOf(7001L).equals(binding.getRiskMetricId())
), eq(1001L));
```

- [ ] **Step 3: Run the alarm tests to verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMetricCatalogServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointPendingRecommendationServiceImplTest,RiskGovernanceServiceImplTest test`

Expected: `BUILD FAILURE` with missing `RiskMetricCatalog`, `riskMetricId`, or publication methods.

- [ ] **Step 4: Add the risk metric catalog schema and domain objects**

```sql
CREATE TABLE risk_metric_catalog (
    id BIGINT NOT NULL COMMENT '主键',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    product_model_id BIGINT DEFAULT NULL COMMENT '合同字段ID',
    contract_identifier VARCHAR(64) NOT NULL COMMENT '合同字段标识',
    risk_metric_code VARCHAR(64) NOT NULL COMMENT '风险指标编码',
    risk_metric_name VARCHAR(128) NOT NULL COMMENT '风险指标名称',
    threshold_direction VARCHAR(32) DEFAULT NULL COMMENT '阈值方向',
    trend_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持趋势分析',
    gis_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于GIS',
    insight_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于对象洞察',
    analytics_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否用于运营分析',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_risk_metric_catalog (product_id, contract_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险指标目录表';

ALTER TABLE risk_point_device ADD COLUMN risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID';
ALTER TABLE rule_definition ADD COLUMN risk_metric_id BIGINT DEFAULT NULL COMMENT '风险指标ID';
```

```java
@Data
@TableName("risk_metric_catalog")
public class RiskMetricCatalog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long productId;
    private Long productModelId;
    private String contractIdentifier;
    private String riskMetricCode;
    private String riskMetricName;
    private String thresholdDirection;
    private Integer trendEnabled;
    private Integer gisEnabled;
    private Integer insightEnabled;
    private Integer analyticsEnabled;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
```

- [ ] **Step 5: Route recommendation, promotion, and policy lookups through the catalog**

```java
RiskMetricCatalog catalog = riskMetricCatalogService.getRequiredByProductAndIdentifier(device.getProductId(), metricIdentifier);
binding.setRiskMetricId(catalog.getId());
binding.setMetricIdentifier(catalog.getContractIdentifier());
binding.setMetricName(catalog.getRiskMetricName());
```

```java
return enabledRulesByMetric.containsKey(binding.getMetricIdentifier())
        || (binding.getRiskMetricId() != null && enabledRulesByRiskMetricId.containsKey(binding.getRiskMetricId()));
```

- [ ] **Step 6: Re-run the alarm tests**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMetricCatalogServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointPendingRecommendationServiceImplTest,RiskGovernanceServiceImplTest,RiskPointControllerTest test`

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add sql/init.sql \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskMetricCatalog.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/mapper/RiskMetricCatalogMapper.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RiskPointDevice.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/entity/RuleDefinition.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskPointController.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingPromotionServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "feat: bridge risk bindings through risk metric catalog"
```

## Task 5: Update the product and risk workbenches to show normative and risk layers

**Files:**
- Modify: `spring-boot-iot-ui/src/types/api.ts`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Modify: `spring-boot-iot-ui/src/api/riskPoint.ts`
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
- Modify: `spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskPointView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts`

- [ ] **Step 1: Extend the product workspace test with normative/risk metadata expectations**

```ts
it('renders normative and risk-ready metadata for compare rows', async () => {
  mockCompareProductModelGovernance.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      productId: 1001,
      summary: {},
      compareRows: [
        {
          modelType: 'property',
          identifier: 'value',
          normativeIdentifier: 'value',
          normativeName: '裂缝监测值',
          riskReady: true,
          rawIdentifiers: ['L1_LF_1'],
          compareStatus: 'double_aligned',
          suggestedAction: '纳入新增',
          riskFlags: ['risk_ready'],
          suspectedMatches: [],
          manualCandidate: { modelType: 'property', identifier: 'value', modelName: '裂缝监测值', dataType: 'double' }
        }
      ]
    }
  })

  const wrapper = mountWorkspace()
  await flushPromises()
  await nextTick()
  await wrapper.get('[data-testid="contract-field-compare-submit"]').trigger('click')
  await flushPromises()
  await nextTick()

  expect(wrapper.text()).toContain('裂缝监测值')
  expect(wrapper.text()).toContain('风险可用')
  expect(wrapper.text()).toContain('L1_LF_1')
})
```

- [ ] **Step 2: Extend the risk point view test to bind `riskMetricId` instead of raw vendor fields**

```ts
const submitPayload = mockPromotePendingBinding.mock.calls[0]?.[1]
expect(submitPayload.metrics?.[0]).toMatchObject({
  riskMetricId: 7001,
  metricIdentifier: 'value',
  metricName: '裂缝监测值'
})
```

- [ ] **Step 3: Run the frontend tests to verify they fail**

Run: `npm --prefix spring-boot-iot-ui exec vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/views/RiskPointView.test.ts`

Expected: failing assertions because the new API fields and labels are not rendered yet.

- [ ] **Step 4: Update the shared API types and product governance UI**

```ts
export interface ProductModelGovernanceCompareRow {
  modelType?: string | null
  identifier?: string | null
  normativeIdentifier?: string | null
  normativeName?: string | null
  riskReady?: boolean | null
  rawIdentifiers?: string[] | null
}

export interface ProductModelGovernanceApplyResult {
  createdCount?: number | null
  updatedCount?: number | null
  skippedCount?: number | null
  releaseBatchId?: IdType | null
}
```

```vue
<div class="product-model-designer__summary-card">
  <span>发布批次</span>
  <strong>{{ latestReleaseBatchId ?? '未发布' }}</strong>
</div>
```

```vue
<small v-if="row.normativeIdentifier">规范字段：{{ row.normativeIdentifier }}</small>
<small v-if="row.rawIdentifiers?.length">原始证据：{{ row.rawIdentifiers.join(' / ') }}</small>
<small v-if="row.riskReady">风险可用</small>
```

- [ ] **Step 5: Update the risk point binding UI to select catalog-backed risk metrics**

```ts
export interface RiskPointPendingMetricCandidate {
  riskMetricId?: IdType | null
  metricIdentifier?: string | null
  metricName?: string | null
}
```

```vue
<el-tag v-if="metric.riskMetricId" type="success">目录指标</el-tag>
<span class="risk-point-binding-maintenance__metric-code">{{ metric.metricIdentifier }}</span>
```

- [ ] **Step 6: Re-run the frontend tests**

Run: `npm --prefix spring-boot-iot-ui exec vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/views/RiskPointView.test.ts`

Expected: all listed Vitest specs pass.

- [ ] **Step 7: Commit**

```bash
git add spring-boot-iot-ui/src/types/api.ts \
  spring-boot-iot-ui/src/api/product.ts \
  spring-boot-iot-ui/src/api/riskPoint.ts \
  spring-boot-iot-ui/src/api/riskGovernance.ts \
  spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue \
  spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue \
  spring-boot-iot-ui/src/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue \
  spring-boot-iot-ui/src/views/RiskPointView.vue \
  spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  spring-boot-iot-ui/src/__tests__/views/RiskPointView.test.ts
git commit -m "feat: surface normative and risk metadata in workbenches"
```

## Task 6: Finalize API contracts, docs, schema sync, and real-environment verification

**Files:**
- Modify: `scripts/run-real-env-schema-sync.py`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md` (update only if the phase-1 slice changes quick-start, runtime baseline, or feature summary wording)
- Review: `AGENTS.md` (update only if the phase-1 slice changes branch/runtime/doc-sync rules or accepted phase wording)
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java`

- [ ] **Step 1: Extend controller tests for the new response fields**

```java
assertEquals(12345L, response.getData().getReleaseBatchId());
assertEquals("value", response.getData().getCompareRows().get(0).getNormativeIdentifier());
assertTrue(response.getData().getCompareRows().get(0).getRiskReady());
```

```java
assertEquals(7001L, response.getData().getRecords().get(0).getRiskMetricId());
assertEquals("value", response.getData().getRecords().get(0).getMetricIdentifier());
```

- [ ] **Step 2: Run the controller tests to verify they fail before the contract cleanup**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -Dtest=ProductModelControllerTest,RiskPointControllerTest test`

Expected: `BUILD FAILURE` if controller serialization still misses `releaseBatchId`, `normativeIdentifier`, `riskReady`, or `riskMetricId`.

- [ ] **Step 3: Make the schema sync script and controller serialization match the new schema**

```python
ensure_column(cursor, "risk_point_device", "risk_metric_id", "BIGINT DEFAULT NULL COMMENT '风险指标ID'")
ensure_column(cursor, "rule_definition", "risk_metric_id", "BIGINT DEFAULT NULL COMMENT '风险指标ID'")
ensure_table(cursor, "risk_metric_catalog", RISK_METRIC_CATALOG_DDL)
ensure_table(cursor, "iot_normative_metric_definition", NORMATIVE_METRIC_DEFINITION_DDL)
ensure_table(cursor, "iot_vendor_metric_evidence", VENDOR_METRIC_EVIDENCE_DDL)
ensure_table(cursor, "iot_product_contract_release_batch", PRODUCT_CONTRACT_RELEASE_BATCH_DDL)
```

- [ ] **Step 4: Update the docs in place**

```md
- `POST /api/device/product/{productId}/model-governance/apply` 当前新增 `releaseBatchId`，用于回溯本次正式合同发布批次。
- compare 行当前新增 `normativeIdentifier / normativeName / riskReady / rawIdentifiers`，用于表达规范字段、风险可用性和厂商原始证据。
- 风险点绑定与 pending 转正当前新增 `riskMetricId`，正式主语义改为风险指标目录，而不是原始厂商字段。
```

Update these files with the implementation facts, not a parallel document:

1. `docs/02-业务功能与流程说明.md`
2. `docs/03-接口规范与接口清单.md`
3. `docs/04-数据库设计与初始化数据.md`
4. `docs/08-变更记录与技术债清单.md`
5. `docs/21-业务功能清单与验收标准.md`
6. Review `README.md` and `AGENTS.md`; update them only if the first-phase slice changes project-level entry guidance or acceptance baseline wording.

- [ ] **Step 5: Run the full targeted verification set**

Run these commands in order:

```bash
mvn -pl spring-boot-iot-device,spring-boot-iot-protocol,spring-boot-iot-alarm -Dtest=NormativeMetricDefinitionServiceImplTest,ProductContractReleaseServiceImplTest,ProductModelServiceImplTest,ProductModelControllerTest,LegacyDpPropertyNormalizerTest,LegacyDpChildTemplateFrameworkTest,RiskMetricCatalogServiceImplTest,RiskPointPendingPromotionServiceImplTest,RiskPointPendingRecommendationServiceImplTest,RiskGovernanceServiceImplTest,RiskPointControllerTest test

npm --prefix spring-boot-iot-ui exec vitest run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts src/__tests__/views/RiskPointView.test.ts

mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected:

1. all targeted backend tests pass
2. all targeted frontend tests pass
3. admin package build succeeds

- [ ] **Step 6: Execute the real-environment verification checklist**

Run:

```bash
python scripts/run-real-env-schema-sync.py
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
java -jar spring-boot-iot-admin/target/spring-boot-iot-admin-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Then verify in the real environment:

1. `/products` can compare and publish crack `value` / `sensor_state` with `releaseBatchId`
2. evidence rows show normative fields plus raw crack identifiers
3. crack risk metrics appear in the risk point binding flow
4. pending promotion binds a `riskMetricId` instead of a raw crack channel field
5. threshold strategy can target the released crack risk metric and still drive the existing linkage / emergency / alarm chain

- [ ] **Step 7: Commit**

```bash
git add scripts/run-real-env-schema-sync.py \
  docs/02-业务功能与流程说明.md \
  docs/03-接口规范与接口清单.md \
  docs/04-数据库设计与初始化数据.md \
  docs/08-变更记录与技术债清单.md \
  docs/21-业务功能清单与验收标准.md \
  README.md AGENTS.md \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductModelControllerTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/controller/RiskPointControllerTest.java
git commit -m "docs: finalize phase1 semantic contract risk closure slice"
```
