# Laser Rangefinder Semantic Contract Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reuse the existing `phase1-crack` governance chain for the South laser rangefinder product while isolating all product-facing names, evidence, release, and object-insight data as laser-specific semantics.

**Architecture:** The device module remains the semantic-control center. `ProductModelNormativeMatcher` will route laser products into the existing `phase1-crack` scenario, while `ProductModelServiceImpl` keeps canonical identifiers unchanged and overrides only product-facing `normativeName` text for the laser product. Shared `dev` data stays product-scoped: formal models, object-insight metadata, evidence, release batch, and risk catalog are aligned only for `nf-monitor-laser-rangefinder-v1`.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, JUnit 5, Mockito

---

## File Structure

### Task 1 ownership: laser semantic recognition and compare/apply behavior in `spring-boot-iot-device`

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

### Task 2 ownership: shared `dev` database alignment

- Review: `sql/init-data.sql`
- Update in shared DB only: `iot_normative_metric_definition`
- Update in shared DB only: `iot_product`
- Update in shared DB only: `iot_product_model`
- Update in shared DB only: `iot_vendor_metric_evidence`
- Update in shared DB only: `iot_product_contract_release_batch`
- Update in shared DB only: `risk_metric_catalog`

### Task 3 ownership: documentation sync

- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`

## Task 1: Add laser scenario recognition and laser-facing display semantics

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelNormativeMatcher.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

- [ ] **Step 1: Write the failing device tests**

```java
@Test
void compareGovernanceShouldDecorateLaserRowsWithLaserFacingNormativeNames() {
    when(productMapper.selectById(5005L)).thenReturn(product(
            5005L,
            "nf-monitor-laser-rangefinder-v1",
            "南方测绘 监测型 激光测距仪"
    ));
    when(productModelMapper.selectList(any())).thenReturn(List.of());
    when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
            normativeDefinition("phase1-crack", "value", "裂缝监测值", 1),
            normativeDefinition("phase1-crack", "sensor_state", "传感器状态", 0)
    ));

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("business");
    manualExtract.setDeviceStructure("composite");
    manualExtract.setParentDeviceCode("SK00EA0D1307988");
    manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
    manualExtract.setSamplePayload("""
            {"SK00EA0D1307988":{"L1_LF_1":{"2026-04-09T13:47:28.000Z":10.86}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(5005L, dto);

    ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
    assertEquals("value", row.getIdentifier());
    assertEquals("value", row.getNormativeIdentifier());
    assertEquals("激光测距值", row.getNormativeName());
    assertTrue(row.getRiskReady());
    assertEquals(List.of("L1_LF_1"), row.getRawIdentifiers());
}

@Test
void compareGovernanceShouldDecorateLaserSensorStateRowsWithoutParentCollectorStatusLeakage() {
    when(productMapper.selectById(5005L)).thenReturn(product(
            5005L,
            "nf-monitor-laser-rangefinder-v1",
            "南方测绘 监测型 激光测距仪"
    ));
    when(productModelMapper.selectList(any())).thenReturn(List.of());
    when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
            normativeDefinition("phase1-crack", "value", "裂缝监测值", 1),
            normativeDefinition("phase1-crack", "sensor_state", "传感器状态", 0)
    ));

    ProductModelGovernanceCompareDTO dto = new ProductModelGovernanceCompareDTO();
    ProductModelGovernanceCompareDTO.ManualExtractInput manualExtract =
            new ProductModelGovernanceCompareDTO.ManualExtractInput();
    manualExtract.setSampleType("status");
    manualExtract.setDeviceStructure("composite");
    manualExtract.setParentDeviceCode("SK00EA0D1307988");
    manualExtract.setRelationMappings(List.of(relationMapping("L1_LF_1", "202018108")));
    manualExtract.setSamplePayload("""
            {"SK00EA0D1307988":{"S1_ZT_1":{"2026-04-09T13:47:28.000Z":{"temp":20.31,"humidity":89.04,"sensor_state":{"L1_LF_1":0}}}}}
            """);
    dto.setManualExtract(manualExtract);

    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(5005L, dto);

    ProductModelGovernanceCompareRowVO row = result.getCompareRows().get(0);
    assertEquals("sensor_state", row.getIdentifier());
    assertEquals("sensor_state", row.getNormativeIdentifier());
    assertEquals("传感器状态", row.getNormativeName());
    assertTrue(result.getCompareRows().stream().noneMatch(item -> "temp".equals(item.getIdentifier())));
}

@Test
void applyGovernanceShouldCreateLaserReleaseBatchThroughPhase1CrackScenario() {
    when(productMapper.selectById(5005L)).thenReturn(product(
            5005L,
            "nf-monitor-laser-rangefinder-v1",
            "南方测绘 监测型 激光测距仪"
    ));
    when(productModelMapper.selectOne(any())).thenReturn(null);
    when(productContractReleaseService.createBatch(
            eq(5005L),
            eq("phase1-crack"),
            eq("manual_compare_apply"),
            eq(1),
            eq(10001L),
            eq(null),
            eq("manual_compare_apply")
    )).thenReturn(55667L);

    ProductModelGovernanceApplyDTO dto = new ProductModelGovernanceApplyDTO();
    dto.setItems(List.of(applyItem("create", null, "property", "value", "激光测距值")));

    ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(5005L, dto, 10001L);

    assertEquals(55667L, result.getReleaseBatchId());
}
```

- [ ] **Step 2: Run the device tests and verify RED**

Run:

```bash
mvn -pl spring-boot-iot-device -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest test
```

Expected: `BUILD FAILURE`, with failures showing that laser products do not yet resolve to `phase1-crack` or still return crack-facing names.

- [ ] **Step 3: Implement the minimal production code**

```java
static final String SCENARIO_PHASE1_CRACK = "phase1-crack";
static final String PRODUCT_KEY_LASER_RANGEFINDER = "nf-monitor-laser-rangefinder-v1";

String resolveScenarioCode(Product product) {
    if (product == null) {
        return null;
    }
    if (matchesGnss(product.getProductKey())
            || matchesGnss(product.getProductName())
            || matchesGnss(product.getManufacturer())
            || matchesGnss(product.getDescription())) {
        return SCENARIO_PHASE2_GNSS;
    }
    if (matchesLaser(product.getProductKey())
            || matchesLaser(product.getProductName())
            || matchesLaser(product.getManufacturer())
            || matchesLaser(product.getDescription())) {
        return SCENARIO_PHASE1_CRACK;
    }
    if (matchesCrack(product.getProductKey())
            || matchesCrack(product.getProductName())
            || matchesCrack(product.getManufacturer())
            || matchesCrack(product.getDescription())) {
        return SCENARIO_PHASE1_CRACK;
    }
    return null;
}

private boolean matchesLaser(String value) {
    if (!StringUtils.hasText(value)) {
        return false;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.contains("laser-rangefinder")
            || normalized.contains("laser_rangefinder")
            || normalized.contains("south_laser_rangefinder")
            || value.contains("激光")
            || value.contains("测距");
}
```

```java
private void decorateCompareResultWithNormativeMetadata(Product product,
                                                        ProductModelGovernanceCompareVO compareResult) {
    String scenarioCode = normativeMatcher.resolveScenarioCode(product);
    if (!StringUtils.hasText(scenarioCode) || compareResult == null || compareResult.getCompareRows() == null) {
        return;
    }
    Map<String, String> displayAliases = resolveNormativeDisplayAliases(product);
    List<NormativeMetricDefinition> definitions = safeNormativeDefinitions(
            normativeMetricDefinitionService.listByScenario(scenarioCode)
    );
    for (ProductModelGovernanceCompareRowVO row : compareResult.getCompareRows()) {
        ProductModelNormativeMatcher.NormativeMatchResult match =
                normativeMatcher.matchProperty(row.getIdentifier(), resolveRawIdentifiers(row), definitions);
        if (match == null) {
            continue;
        }
        row.setNormativeIdentifier(match.normativeIdentifier());
        row.setNormativeName(displayAliases.getOrDefault(match.normativeIdentifier(), match.normativeName()));
        row.setRiskReady(match.riskReady());
        row.setRawIdentifiers(match.rawIdentifiers());
    }
}

private Map<String, String> resolveNormativeDisplayAliases(Product product) {
    String productKey = product == null ? null : normalizeOptional(product.getProductKey());
    if (!"nf-monitor-laser-rangefinder-v1".equals(productKey)) {
        return Map.of();
    }
    return Map.of(
            "value", "激光测距值",
            "sensor_state", "传感器状态"
    );
}
```

- [ ] **Step 4: Re-run the same device tests and verify GREEN**

Run:

```bash
mvn -pl spring-boot-iot-device -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest test
```

Expected: `BUILD SUCCESS`

## Task 2: Align the shared `dev` database for the laser product

**Files:**
- Review: `sql/init-data.sql`
- Update in shared DB only: `iot_normative_metric_definition`
- Update in shared DB only: `iot_product`
- Update in shared DB only: `iot_product_model`
- Update in shared DB only: `iot_vendor_metric_evidence`
- Update in shared DB only: `iot_product_contract_release_batch`
- Update in shared DB only: `risk_metric_catalog`

- [ ] **Step 1: Verify the live baseline before changes**

Run:

```bash
@'
import pymysql
conn = pymysql.connect(host="8.130.107.120", port=3306, user="root", password="mI8%pB1*gD", database="rm_iot", charset="utf8mb4")
with conn.cursor() as cur:
    cur.execute("select id, product_key, product_name, metadata_json from iot_product where product_key='nf-monitor-laser-rangefinder-v1'")
    print(cur.fetchall())
    cur.execute("select identifier, model_name from iot_product_model where product_id=202603192100560258 and deleted=0 order by identifier")
    print(cur.fetchall())
    cur.execute("select count(*) from iot_normative_metric_definition")
    print(cur.fetchall())
conn.close()
'@ | python -
```

Expected: laser product exists; model names are still crack-facing; `iot_normative_metric_definition` may be empty.

- [ ] **Step 2: Apply the minimal idempotent SQL**

Run:

```sql
INSERT INTO iot_normative_metric_definition (
    id, tenant_id, scenario_code, device_family, identifier, display_name, unit, precision_digits,
    monitor_content_code, monitor_type_code, risk_enabled, trend_enabled, metadata_json
)
SELECT seed.id, seed.tenant_id, seed.scenario_code, seed.device_family, seed.identifier, seed.display_name, seed.unit,
       seed.precision_digits, seed.monitor_content_code, seed.monitor_type_code, seed.risk_enabled, seed.trend_enabled,
       seed.metadata_json
FROM (
    SELECT 910001 AS id, 1 AS tenant_id, 'phase1-crack' AS scenario_code, 'LF' AS device_family, 'value' AS identifier,
           '裂缝监测值' AS display_name, 'mm' AS unit, 4 AS precision_digits, 'L1' AS monitor_content_code,
           'LF' AS monitor_type_code, 1 AS risk_enabled, 1 AS trend_enabled,
           JSON_OBJECT('thresholdKind', 'absolute') AS metadata_json
    UNION ALL
    SELECT 910002, 1, 'phase1-crack', 'LF', 'sensor_state', '传感器状态', NULL, 0, 'S1', 'ZT', 0, 0,
           JSON_OBJECT('usage', 'health_state')
) seed
LEFT JOIN iot_normative_metric_definition existing
    ON existing.scenario_code = seed.scenario_code
   AND existing.identifier = seed.identifier
WHERE existing.id IS NULL;

UPDATE iot_product_model
SET model_name = CASE identifier
        WHEN 'value' THEN '激光测距值'
        WHEN 'sensor_state' THEN '传感器状态'
        ELSE model_name
    END
WHERE product_id = 202603192100560258
  AND deleted = 0
  AND identifier IN ('value', 'sensor_state');

UPDATE iot_product
SET metadata_json = JSON_SET(
        COALESCE(metadata_json, JSON_OBJECT()),
        '$.objectInsight',
        JSON_OBJECT(
            'customMetrics',
            JSON_ARRAY(
                JSON_OBJECT(
                    'identifier', 'value',
                    'displayName', '激光测距值',
                    'group', 'measure',
                    'includeInTrend', TRUE,
                    'includeInExtension', FALSE,
                    'enabled', TRUE,
                    'sortNo', 10
                )
            )
        )
    )
WHERE id = 202603192100560258;
```

```sql
INSERT INTO iot_vendor_metric_evidence (
    id, tenant_id, product_id, scenario_code, canonical_identifier, raw_identifier, evidence_source,
    evidence_payload, metadata_json, create_by, create_time, update_by, update_time, deleted
)
SELECT 2042000000000001001, 1, 202603192100560258, 'phase1-crack', 'value', 'L1_LF_1', 'manual_compare',
       '{"productKey":"nf-monitor-laser-rangefinder-v1","sampleType":"business"}',
       '{"sampleType":"business"}', 1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM iot_vendor_metric_evidence
    WHERE product_id = 202603192100560258 AND canonical_identifier = 'value' AND raw_identifier = 'L1_LF_1' AND deleted = 0
);

INSERT INTO iot_vendor_metric_evidence (
    id, tenant_id, product_id, scenario_code, canonical_identifier, raw_identifier, evidence_source,
    evidence_payload, metadata_json, create_by, create_time, update_by, update_time, deleted
)
SELECT 2042000000000001002, 1, 202603192100560258, 'phase1-crack', 'sensor_state', 'S1_ZT_1.sensor_state.L1_LF_1', 'manual_compare',
       '{"productKey":"nf-monitor-laser-rangefinder-v1","sampleType":"status"}',
       '{"sampleType":"status"}', 1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM iot_vendor_metric_evidence
    WHERE product_id = 202603192100560258 AND canonical_identifier = 'sensor_state'
      AND raw_identifier = 'S1_ZT_1.sensor_state.L1_LF_1' AND deleted = 0
);
```

```sql
INSERT INTO iot_product_contract_release_batch (
    id, tenant_id, product_id, scenario_code, release_source, release_status,
    release_reason, model_count, create_by, create_time, update_by, update_time, deleted
)
SELECT 2042000000000002001, 1, 202603192100560258, 'phase1-crack', 'manual_compare_apply', 'released',
       'laser semantic alignment baseline', 2, 1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM iot_product_contract_release_batch
    WHERE product_id = 202603192100560258 AND scenario_code = 'phase1-crack' AND deleted = 0
);

INSERT INTO risk_metric_catalog (
    id, tenant_id, product_id, release_batch_id, contract_identifier, normative_identifier,
    metric_name, risk_category, metric_role, lifecycle_status, create_by, create_time, update_by, update_time, deleted
)
SELECT 2042000000000003001, 1, 202603192100560258,
       (SELECT id FROM iot_product_contract_release_batch
        WHERE product_id = 202603192100560258 AND scenario_code = 'phase1-crack' AND deleted = 0
        ORDER BY id DESC LIMIT 1),
       'value', 'value', '激光测距值', 'monitoring', 'metric', 'active', 1, NOW(), 1, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM risk_metric_catalog
    WHERE product_id = 202603192100560258 AND contract_identifier = 'value' AND deleted = 0
);
```

- [ ] **Step 3: Re-query the shared DB and verify GREEN**

Run:

```bash
@'
import pymysql
conn = pymysql.connect(host="8.130.107.120", port=3306, user="root", password="mI8%pB1*gD", database="rm_iot", charset="utf8mb4")
with conn.cursor() as cur:
    cur.execute("select identifier, model_name from iot_product_model where product_id=202603192100560258 and deleted=0 order by identifier")
    print('product_model', cur.fetchall())
    cur.execute("select metadata_json from iot_product where id=202603192100560258")
    print('metadata', cur.fetchone())
    cur.execute(\"\"\"select canonical_identifier, raw_identifier from iot_vendor_metric_evidence
                   where product_id=202603192100560258 and deleted=0 order by canonical_identifier, raw_identifier\"\"\")
    print('evidence', cur.fetchall())
    cur.execute(\"\"\"select scenario_code, release_source, release_status from iot_product_contract_release_batch
                   where product_id=202603192100560258 and deleted=0 order by id desc limit 3\"\"\")
    print('release', cur.fetchall())
    cur.execute(\"\"\"select contract_identifier, normative_identifier, metric_name from risk_metric_catalog
                   where product_id=202603192100560258 and deleted=0 order by id desc limit 5\"\"\")
    print('risk_catalog', cur.fetchall())
conn.close()
'@ | python -
```

Expected: the laser product exposes `激光测距值 / 传感器状态`, object-insight keeps only `value`, evidence contains `value` and `sensor_state`, and risk catalog contains only `value`.

## Task 3: Sync docs to the approved laser alignment boundary

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Add the approved capability statements**

```md
- `2026-04-10` 起，南方测绘监测型激光测距仪已接入产品契约字段治理闭环：
  - 复用 `phase1-crack` 底层治理链路，保持 `L1_LF_* -> value`、`S1_ZT_1.sensor_state.L1_LF_* -> sensor_state`
  - 激光产品对外展示语义独立于裂缝产品，正式字段显示为 `激光测距值 / 传感器状态`
  - `compare/apply` 返回的 `normativeIdentifier` 仍为 `value / sensor_state`，但 `normativeName` 对激光产品显示为激光语义
  - `metadata_json.objectInsight.customMetrics[]` 默认仅保留 `value`
  - 风险目录仅发布 `value`
```

- [ ] **Step 2: Run doc and diff hygiene verification**

Run:

```bash
git diff --check
```

Expected: no whitespace or merge-marker errors.

- [ ] **Step 3: Run the final targeted regression pack**

Run:

```bash
mvn -pl spring-boot-iot-device -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProductModelServiceImplTest test
```

Expected: `BUILD SUCCESS`

## Self-Review

Spec coverage:

1. Laser product keeps its own product identity while reusing `phase1-crack` through Task 1 and Task 2.
2. Product-facing semantics are isolated to `激光测距值 / 传感器状态` through compare decoration and DB model/object-insight updates.
3. Shared `dev` governance support data is aligned through Task 2 without creating new tables or a new scenario.
4. Required documentation updates are covered in Task 3.

Placeholder scan:

1. No `TODO`, `TBD`, or deferred placeholders remain.
2. Each task contains concrete files, commands, and expected results.

Type consistency:

1. Laser continues to use `value / sensor_state` as canonical identifiers everywhere.
2. Laser scenario routing always resolves to `phase1-crack`, not a new `phase3-laser`.
3. Product-scoped display names use `激光测距值 / 传感器状态` consistently across compare output and DB alignment.
