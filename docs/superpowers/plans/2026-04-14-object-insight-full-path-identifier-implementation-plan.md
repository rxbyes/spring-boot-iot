# Object Insight Full-Path Identifier Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make collector parent runtime-status fields use full-path formal identifiers as the only truth across `/products`, `/insight`, latest property snapshots, metric options, risk catalog/binding/rule chains, and historical cleanup.

**Architecture:** Tighten collector governance compare/apply so parent status leaves keep exact identifiers such as `S1_ZT_1.signal_4g`, while child products keep their existing canonical identifiers (`value / sensor_state / dispsX / dispsY`). Then align published snapshot resolution, latest-property writes, backend read surfaces, risk catalog republish, frontend insight rendering, and product-scoped SQL cleanup around exact-formal matching with only trim/case compatibility. Finally, refresh seed data and docs so the system stops reintroducing short collector aliases like `signal_4g`.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Jackson, Vue 3, TypeScript, Vitest, JUnit 5, SQL migration scripts, Markdown docs

---

## Scope Check

This work spans multiple modules, but they are not independent subsystems. `/products`, published contract snapshots, `/insight`, `iot_device_property`, risk catalog/binding/rule tables, and migration cleanup all consume the same identifier truth. Splitting them into separate plans would leave intermediate states that still generate duplicate identifiers and dirty data, so this stays as one coordinated implementation plan.

## File Structure

### Backend governance write path

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`

`CollectorChildMetricBoundaryPolicy.java` is the choke point that currently strips `S1_ZT_1.` from collector parent status leaves. That must change first so compare/apply stops generating new short collector identifiers. `ProductModelServiceImplTest.java` already has the direct regression coverage for collector compare/apply and should be updated before implementation.

### Published snapshot and metadata truth

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/PublishedProductContractSnapshot.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`

The snapshot service must treat current `iot_product_model.identifier` values as the authoritative published identifiers while only preserving persisted resolver aliases that still target a current formal field. `PublishedProductContractSnapshot.java` needs a read accessor for existing alias entries so the merge can be done without stringly re-parsing JSON.

### Latest-property writes and backend read surfaces

- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandlerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`

`DevicePayloadApplyStageHandler.java` is the write-side guard that can stop stale short latest rows from surviving after a full-path formal write lands. `DeviceServiceImpl.java` is the metric-option read surface used by risk binding and later point-property selection, so it must hide short/full duplicates even before the migration script has been run everywhere.

### Risk chain and SQL cleanup

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Create: `sql/upgrade/20260414_collector_parent_full_path_identifier_migration.sql`
- Modify: `sql/upgrade/20260414_device_property_full_path_dedup.sql`
- Modify: `sql/init-data.sql`

The risk catalog publisher must update stale short catalog rows in place when the current released contract is now full-path. The new SQL migration script handles product model, product metadata, risk catalog, risk binding, rule definition, and latest-property cleanup for same-pattern collector products. The existing dedup script should become the narrow post-migration cleanup script instead of a broad suffix-based delete against the whole table.

### Frontend exact-identifier read surfaces

- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

These files build the object insight hero metrics, trend identifiers, and property snapshot cards. They need a last-line exact-formal dedupe so stale short aliases are suppressed whenever the same page already has the corresponding full-path collector parent metric.

### Documentation

- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

`AGENTS.md` and the main docs currently still describe collector parent formal fields as short identifiers in several places. They must be updated to say the formal identifier is the full path, while `signal_NB / singal_NB` remains only an input normalization concern rather than a second published contract truth.

### Task 1: Switch Collector Governance to Full-Path Parent Identifiers

**Files:**
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java`

- [ ] **Step 1: Add failing compare/apply regressions for collector parent full-path identifiers**

```java
@Test
void compareGovernanceShouldKeepCollectorRuntimeStatusButDropChildSensorState() {
    ProductModelGovernanceCompareVO result = productModelService.compareGovernance(6006L, dto);

    assertEquals(
            List.of("S1_ZT_1.humidity", "S1_ZT_1.signal_4g", "S1_ZT_1.temp"),
            result.getCompareRows().stream()
                    .map(ProductModelGovernanceCompareRowVO::getIdentifier)
                    .sorted()
                    .toList()
    );
    assertTrue(result.getCompareRows().stream().noneMatch(item -> "sensor_state".equals(item.getIdentifier())));
}

@Test
void applyGovernanceShouldAllowDirectCollectorRtuRuntimeStatusFields() {
    dto.setItems(List.of(
            applyItem("create", null, "property", "S1_ZT_1.ext_power_volt", "外接电源电压"),
            applyItem("create", null, "property", "S1_ZT_1.lat", "纬度"),
            applyItem("create", null, "property", "S1_ZT_1.signal_NB", "NB 信号强度"),
            applyItem("create", null, "property", "S1_ZT_1.sw_version", "软件版本")
    ));

    ProductModelGovernanceApplyResultVO result = productModelService.applyGovernance(6007L, dto, 10001L);
    assertEquals(4, result.getCreatedCount());
}
```

- [ ] **Step 2: Run the collector governance test file and confirm the new assertions fail**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
```

Expected: `FAIL`, because compare/apply still returns stripped identifiers such as `temp`, `humidity`, `signal_4g`, and `signal_NB`.

- [ ] **Step 3: Keep `S1_ZT_1.` on collector parent status leaves while still filtering child sensor-state mirrors**

```java
String toCollectorIdentifier(String sampleType, String rawIdentifier) {
    String normalizedIdentifier = normalizeText(rawIdentifier);
    if (normalizedIdentifier == null) {
        return null;
    }
    if (SAMPLE_TYPE_STATUS.equals(normalizeKeyword(sampleType))
            && normalizedIdentifier.startsWith(STATUS_PREFIX)
            && !normalizedIdentifier.startsWith(PARENT_SENSOR_STATE_PREFIX)) {
        String leafIdentifier = normalizeText(normalizedIdentifier.substring(STATUS_PREFIX.length()));
        return leafIdentifier == null ? null : STATUS_PREFIX + leafIdentifier;
    }
    return normalizedIdentifier;
}
```

This change must preserve the existing child filter behavior:

```java
return !isChildBusinessIdentifier(normalizedIdentifier, logicalChannelCodes)
        && !isChildSensorState(sampleType, normalizedIdentifier, logicalChannelCodes);
```

- [ ] **Step 4: Re-run the collector governance test file and confirm the compare/apply boundary is correct**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest" test
```

Expected: `PASS`, including the collector compare/apply cases and the existing child-product canonicalization cases.

- [ ] **Step 5: Commit the governance boundary change**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/CollectorChildMetricBoundaryPolicy.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductModelServiceImplTest.java
git commit -m "fix: keep collector parent identifiers as full paths"
```

### Task 2: Rebuild Published Snapshot Truth from Current Formal Fields

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/PublishedProductContractSnapshot.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`

- [ ] **Step 1: Add a failing snapshot-service regression for stale short persisted snapshots**

```java
@Test
void shouldOverlayPersistedShortCollectorSnapshotWithCurrentFullPathFormalIdentifier() {
    when(productModelMapper.selectList(any())).thenReturn(List.of(property("S1_ZT_1.signal_4g")));

    ProductMetricResolverSnapshot persisted = new ProductMetricResolverSnapshot();
    persisted.setSnapshotJson("""
            {
              "publishedIdentifiers": ["signal_4g"],
              "canonicalAliases": {
                "signal_4g": "signal_4g"
              }
            }
            """);
    when(snapshotMapper.selectList(any())).thenReturn(List.of(persisted));

    PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(1001L);

    assertTrue(snapshot.publishedIdentifiers().contains("S1_ZT_1.signal_4g"));
    assertFalse(snapshot.publishedIdentifiers().contains("signal_4g"));
    assertEquals("S1_ZT_1.signal_4g", snapshot.canonicalAliasOf("S1_ZT_1.signal_4g").orElse(null));
}
```

- [ ] **Step 2: Run the snapshot and product metadata tests and confirm the stale-short regression fails**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=PublishedProductContractSnapshotServiceImplTest+ProductServiceImplTest" test
```

Expected: `FAIL`, because persisted `publishedIdentifiers=["signal_4g"]` currently survives as the published truth.

- [ ] **Step 3: Merge persisted aliases only when they still target a current formal identifier**

```java
public Map<String, String> canonicalAliases() {
    return canonicalAliases;
}
```

```java
private PublishedProductContractSnapshot mergeSnapshotWithCurrentFormal(Long productId,
                                                                        Long releaseBatchId,
                                                                        PublishedProductContractSnapshot persistedSnapshot) {
    Map<String, String> currentFormalByLower = loadCurrentFormalIdentifierMap(productId);
    if (currentFormalByLower.isEmpty()) {
        return persistedSnapshot == null ? PublishedProductContractSnapshot.empty(productId) : persistedSnapshot;
    }
    PublishedProductContractSnapshot.Builder builder = PublishedProductContractSnapshot.builder()
            .productId(productId)
            .releaseBatchId(releaseBatchId)
            .publishedIdentifiers(currentFormalByLower.values());
    currentFormalByLower.values().forEach(identifier -> builder.canonicalAlias(identifier, identifier));
    if (persistedSnapshot != null) {
        persistedSnapshot.canonicalAliases().forEach((alias, target) -> {
            String currentFormal = currentFormalByLower.get(target.trim().toLowerCase(Locale.ROOT));
            if (StringUtils.hasText(currentFormal)) {
                builder.canonicalAlias(alias, currentFormal);
            }
        });
    }
    return builder.build();
}
```

Call `mergeSnapshotWithCurrentFormal(...)` for both persisted and mapper-built snapshots so current `iot_product_model.identifier` values stay authoritative while valid child-product aliases like `L1_LF_1.value -> value` still survive.

- [ ] **Step 4: Re-run the snapshot and product metadata tests and confirm current-formal truth wins**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=PublishedProductContractSnapshotServiceImplTest+ProductServiceImplTest" test
```

Expected: `PASS`, and `ProductServiceImplTest#updateProductShouldKeepFullPathIdentifierWhenLegacyReleasedSnapshotStillUsesShortCanonicalAlias` continues to pass.

- [ ] **Step 5: Commit the snapshot-truth change**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/model/PublishedProductContractSnapshot.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImpl.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/PublishedProductContractSnapshotServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java
git commit -m "fix: rebuild published snapshot truth from current formal identifiers"
```

### Task 3: Stop Latest-Property and Metric-Option Surfaces from Reintroducing Short Duplicates

**Files:**
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandlerTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java`

- [ ] **Step 1: Add failing write-side and read-side regressions for collector short/full duplicates**

```java
@Test
void applyShouldDeleteShortCollectorDuplicateWhenFullPathLatestPropertyIsWritten() {
    when(devicePropertyMetadataService.listPropertyMetadataMap(6007L)).thenReturn(Map.of(
            "S1_ZT_1.signal_4g",
            metadata("S1_ZT_1.signal_4g", "4G 信号强度", "int")
    ));
    upMessage.setProperties(new LinkedHashMap<>(Map.of("S1_ZT_1.signal_4g", -71)));

    handler.apply(target);

    verify(devicePropertyMapper).delete(argThat((LambdaQueryWrapper<DeviceProperty> wrapper) ->
            wrapper.getParamNameValuePairs().values().contains(7001L)
                    && wrapper.getParamNameValuePairs().values().contains("signal_4g")
    ));
}

@Test
void listMetricOptionsShouldPreferFullPathFormalIdentifierOverShortLatestDuplicate() {
    when(productModelMapper.selectList(any())).thenReturn(List.of(productModel("S1_ZT_1.signal_4g", "4G 信号强度")));
    when(devicePropertyMapper.selectList(any())).thenReturn(List.of(
            deviceProperty(4001L, "signal_4g", "旧 4G 信号", "-82", "int", LocalDateTime.now()),
            deviceProperty(4001L, "S1_ZT_1.signal_4g", "4G 信号强度", "-81", "int", LocalDateTime.now())
    ));

    List<DeviceMetricOptionVO> options = deviceService.listMetricOptions(99L, 4001L);

    assertEquals(List.of("S1_ZT_1.signal_4g"), options.stream().map(DeviceMetricOptionVO::getIdentifier).toList());
}
```

- [ ] **Step 2: Run the latest-property and metric-option tests and confirm they fail**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest+DeviceServiceImplTest" test
```

Expected: `FAIL`, because the write path does not delete stale short rows and `listMetricOptions(...)` still returns both `signal_4g` and `S1_ZT_1.signal_4g`.

- [ ] **Step 3: Delete collector short duplicates on write and collapse them on read**

```java
private void removeCollectorShortDuplicate(Long deviceId, String identifier) {
    if (deviceId == null || !isCollectorParentFullPathIdentifier(identifier)) {
        return;
    }
    String shortIdentifier = lastIdentifierSegment(identifier);
    if (!hasText(shortIdentifier) || shortIdentifier.equalsIgnoreCase(identifier)) {
        return;
    }
    devicePropertyMapper.delete(new LambdaQueryWrapper<DeviceProperty>()
            .eq(DeviceProperty::getDeviceId, deviceId)
            .eq(DeviceProperty::getIdentifier, shortIdentifier));
}
```

Call it immediately after the insert/update path inside `updateLatestProperties(...)`:

```java
removeCollectorShortDuplicate(target.getDevice().getId(), property.getIdentifier());
```

Use the same targeted collector-parent mapping in `DeviceServiceImpl.java`:

```java
private Map<String, String> buildCollectorParentFormalByLeaf(List<ProductModel> productModels) {
    Map<String, String> result = new LinkedHashMap<>();
    for (ProductModel productModel : productModels) {
        String identifier = normalizeIdentifier(productModel.getIdentifier());
        if (isCollectorParentFullPathIdentifier(identifier)) {
            result.putIfAbsent(lastIdentifierSegment(identifier).toLowerCase(Locale.ROOT), identifier);
        }
    }
    return result;
}
```

```java
String resolvedIdentifier = collectorParentFormalByLeaf.getOrDefault(
        deviceProperty.getIdentifier().trim().toLowerCase(Locale.ROOT),
        deviceProperty.getIdentifier()
);
```

Only use that leaf-based bridge for collector parent full-path formal identifiers so the code does not reintroduce generic dotted-suffix guessing for child products.

- [ ] **Step 4: Re-run the targeted device tests and confirm duplicate options disappear**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest+DeviceServiceImplTest" test
```

Expected: `PASS`, with only `S1_ZT_1.signal_4g` exposed and stale short latest rows deleted when a formal full-path write lands.

- [ ] **Step 5: Commit the latest-property and metric-option cleanup**

```bash
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/DeviceServiceImpl.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandlerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/DeviceServiceImplTest.java
git commit -m "fix: dedupe collector short aliases in latest and metric options"
```

### Task 4: Update the Risk Chain and Ship a Product-Scoped Migration Script

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Create: `sql/upgrade/20260414_collector_parent_full_path_identifier_migration.sql`
- Modify: `sql/upgrade/20260414_device_property_full_path_dedup.sql`
- Modify: `sql/init-data.sql`

- [ ] **Step 1: Add a failing catalog regression for stale short collector identifiers**

```java
@Test
void publishFromReleasedContractsShouldRewriteShortCollectorCatalogRowToCurrentFullPathIdentifier() {
    RiskMetricCatalog existing = new RiskMetricCatalog();
    existing.setId(6103L);
    existing.setProductId(6007L);
    existing.setContractIdentifier("signal_4g");
    existing.setEnabled(1);
    when(riskMetricCatalogMapper.selectList(any())).thenReturn(List.of(existing));
    when(snapshotService.getRequiredSnapshot(6007L)).thenReturn(PublishedProductContractSnapshot.builder()
            .productId(6007L)
            .releaseBatchId(7001L)
            .publishedIdentifier("S1_ZT_1.signal_4g")
            .canonicalAlias("S1_ZT_1.signal_4g", "S1_ZT_1.signal_4g")
            .build());

    ProductModel released = new ProductModel();
    released.setProductId(6007L);
    released.setIdentifier("S1_ZT_1.signal_4g");
    released.setModelName("4G 信号强度");

    service.publishFromReleasedContracts(6007L, 7001L, List.of(released), Set.of("S1_ZT_1.signal_4g"));

    verify(riskMetricCatalogMapper).updateById(argThat((RiskMetricCatalog row) ->
            Long.valueOf(6103L).equals(row.getId())
                    && "S1_ZT_1.signal_4g".equals(row.getContractIdentifier())
    ));
}
```

- [ ] **Step 2: Run the risk catalog test file and confirm the stale-short case fails**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskMetricCatalogServiceImplTest" test
```

Expected: `FAIL`, because the existing catalog row keyed by `signal_4g` does not currently normalize onto the released full-path contract.

- [ ] **Step 3: Normalize stale collector rows through a current-formal alias map and write the SQL migration**

Use a released-contract alias bridge in `RiskMetricCatalogServiceImpl.java`:

```java
private Map<String, String> buildCollectorParentFormalByLeaf(List<ProductModel> releasedContracts) {
    Map<String, String> result = new LinkedHashMap<>();
    for (ProductModel contract : releasedContracts) {
        String identifier = normalize(contract == null ? null : contract.getIdentifier());
        if (isCollectorParentFullPathIdentifier(identifier)) {
            result.putIfAbsent(lastIdentifierSegment(identifier).toLowerCase(Locale.ROOT), identifier);
        }
    }
    return result;
}
```

```java
map.put(normalizeExistingCatalogIdentifier(row.getContractIdentifier(), snapshot, collectorParentFormalByLeaf), row);
```

Create `sql/upgrade/20260414_collector_parent_full_path_identifier_migration.sql` with a product-scoped mapping CTE:

```sql
WITH mapping AS (
    SELECT p.id AS product_id, 'signal_4g' AS legacy_identifier, 'S1_ZT_1.signal_4g' AS target_identifier
      FROM iot_product p
     WHERE p.deleted = 0
       AND (
            LOWER(p.product_key) LIKE '%collector%'
         OR LOWER(p.product_key) LIKE '%collect-rtu%'
         OR (p.product_name LIKE '%采集器%')
         OR (p.product_name LIKE '%采集%' AND p.product_name LIKE '%终端%')
       )
    UNION ALL
    SELECT p.id, 'battery_dump_energy', 'S1_ZT_1.battery_dump_energy' FROM iot_product p WHERE p.deleted = 0
    UNION ALL
    SELECT p.id, 'temp', 'S1_ZT_1.temp' FROM iot_product p WHERE p.deleted = 0
)
UPDATE iot_product_model pm
JOIN mapping m
  ON m.product_id = pm.product_id
 AND pm.identifier = m.legacy_identifier
SET pm.identifier = m.target_identifier,
    pm.update_time = NOW();
```

Then extend the same script to update:

```sql
UPDATE risk_metric_catalog rmc ... SET rmc.contract_identifier = m.target_identifier;
UPDATE risk_point_device rpd ... SET rpd.metric_identifier = m.target_identifier;
UPDATE risk_point_device_pending_binding rppb ... SET rppb.metric_identifier = m.target_identifier;
UPDATE risk_point_device_pending_promotion rpp ... SET rpp.metric_identifier = m.target_identifier;
UPDATE rule_definition rd ... SET rd.metric_identifier = m.target_identifier;
DELETE short_row FROM iot_device_property short_row ... WHERE short_row.identifier = m.legacy_identifier;
```

Keep `sql/upgrade/20260414_device_property_full_path_dedup.sql` as the narrow cleanup companion that only removes leftover short latest rows after the main migration has updated formal truth. Update `sql/init-data.sql` so seeded collector products, object-insight metadata, and sample latest properties already use `S1_ZT_1.*` identifiers.

- [ ] **Step 4: Re-run the risk catalog tests and smoke-check the SQL files**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskMetricCatalogServiceImplTest" test
git diff --check -- spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java sql/upgrade/20260414_collector_parent_full_path_identifier_migration.sql sql/upgrade/20260414_device_property_full_path_dedup.sql sql/init-data.sql
```

Expected: the Maven test passes and `git diff --check` reports no whitespace errors in the Java and SQL patches.

- [ ] **Step 5: Commit the risk-chain and migration work**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java sql/upgrade/20260414_collector_parent_full_path_identifier_migration.sql sql/upgrade/20260414_device_property_full_path_dedup.sql sql/init-data.sql
git commit -m "fix: migrate risk chain to collector full-path identifiers"
```

### Task 5: Harden Frontend Insight Surfaces Against Stale Short Aliases

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/deviceInsightCapability.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceInsightView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts`

- [ ] **Step 1: Add failing frontend regressions for short/full duplicate collector metrics**

```ts
it('prefers full-path collector runtime metrics when short latest aliases still coexist', () => {
  const profile = getInsightCapabilityProfile({
    deviceCode: 'COLLECT-DEDUP-001',
    productName: '雨量采集终端',
    properties: [
      { id: 1, identifier: 'signal_4g', propertyName: '旧 4G 信号', propertyValue: '-82', valueType: 'int' },
      { id: 2, identifier: 'S1_ZT_1.signal_4g', propertyName: '4G 信号强度', propertyValue: '-81', valueType: 'int' }
    ]
  })

  expect(profile.extensionParameters.map((item) => item.identifier)).toContain('S1_ZT_1.signal_4g')
  expect(profile.extensionParameters.map((item) => item.identifier)).not.toContain('signal_4g')
})
```

```ts
it('hides stale short collector snapshot rows when product models already define the full-path field', async () => {
  expect(wrapper.text()).toContain('S1_ZT_1.signal_4g')
  expect(wrapper.text()).not.toContain('signal_4g')
})
```

- [ ] **Step 2: Run the targeted frontend suites and confirm the duplicate regressions fail**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/views/DeviceInsightView.test.ts
```

Expected: `FAIL`, because the frontend still allows both the short and full-path collector runtime identifiers to survive in profile/snapshot output.

- [ ] **Step 3: Filter short aliases whenever the same page already sees the collector full-path metric**

Add a targeted collector-parent dedupe helper in `deviceInsightCapability.ts`:

```ts
function buildCollectorParentFullPathByLeaf(properties: DeviceProperty[]) {
  const result = new Map<string, string>()
  properties.forEach((item) => {
    const identifier = item.identifier?.trim()
    if (identifier && identifier.startsWith('S1_ZT_1.') && !identifier.startsWith('S1_ZT_1.sensor_state.')) {
      result.set(identifier.slice(identifier.lastIndexOf('.') + 1).toLowerCase(), identifier)
    }
  })
  return result
}
```

Use it to suppress duplicate short runtime metrics:

```ts
const collectorParentFullPathByLeaf = buildCollectorParentFullPathByLeaf(source.properties ?? [])
const dedupedProperties = (source.properties ?? []).filter((item) => {
  const identifier = item.identifier?.trim()
  if (!identifier || identifier.includes('.')) {
    return true
  }
  return !collectorParentFullPathByLeaf.has(identifier.toLowerCase())
})
```

Apply the same deduped property list in `DeviceInsightView.vue` before building snapshot cards so the UI keeps the exact formal full-path row when both rows exist.

- [ ] **Step 4: Re-run the targeted frontend suites and confirm only full-path collector metrics remain**

Run:

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/views/DeviceInsightView.test.ts
```

Expected: `PASS`, and the page-level snapshot/history metric sets no longer surface `signal_4g` when `S1_ZT_1.signal_4g` is already present.

- [ ] **Step 5: Commit the frontend dedupe changes**

```bash
git add spring-boot-iot-ui/src/utils/deviceInsightCapability.ts spring-boot-iot-ui/src/views/DeviceInsightView.vue spring-boot-iot-ui/src/__tests__/utils/deviceInsightCapability.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceInsightView.test.ts
git commit -m "fix: prefer full-path collector metrics in insight surfaces"
```

### Task 6: Update the Docs and Run Final Verification

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the docs so they describe collector parent formal identifiers as exact full paths**

Refresh the examples in the repo docs from:

```md
temp / humidity / signal_4g / signal_NB / signal_db
```

to:

```md
S1_ZT_1.temp / S1_ZT_1.humidity / S1_ZT_1.signal_4g / S1_ZT_1.signal_NB / S1_ZT_1.signal_db
```

Also add one explicit sentence to `AGENTS.md` and `docs/02-业务功能与流程说明.md` stating that `signal_NB / singal_NB` is an input normalization concern only, not a second published contract identifier.

- [ ] **Step 2: Add a dated changelog entry and the frontend-governance note**

Use a changelog entry shaped like:

```md
- 2026-04-14：采集器父产品正式字段统一切换为全路径标识符真相。`/products` compare/apply、published snapshot、`iot_device_property`、对象洞察台、风险目录/绑定/规则、以及产品级迁移脚本都改为围绕 `S1_ZT_1.*` 正式字段收口；同类产品历史 `signal_4g` 一类短标识只保留一次性迁移与清理语义，不再作为正式合同字段继续存在。
```

In `docs/15-前端优化与治理计划.md`, record the prevention rule that object insight and point-selection read surfaces must suppress stale short aliases when the exact full-path formal field is already present.

- [ ] **Step 3: Run the final targeted verification commands**

Run:

```powershell
mvn -s .mvn/settings.xml -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductModelServiceImplTest+PublishedProductContractSnapshotServiceImplTest+ProductServiceImplTest+DevicePayloadApplyStageHandlerTest+DeviceServiceImplTest" test
mvn -s .mvn/settings.xml -pl spring-boot-iot-alarm -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskMetricCatalogServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/deviceInsightCapability.test.ts src/__tests__/views/DeviceInsightView.test.ts
node scripts/run-governance-contract-gates.mjs
git diff --check -- AGENTS.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
```

Expected:

```text
All targeted Maven/Vitest suites pass.
Governance contract gates pass.
git diff --check prints no output.
```

- [ ] **Step 4: Review `README.md` for fallout and only patch it if the visible product-governance summary is now stale**

```powershell
Select-String -Path README.md -Pattern "products|对象洞察|契约字段|signal_4g|S1_ZT_1" -Encoding UTF8
```

Expected: either no README change is needed, or any stale short-identifier example is updated in the same commit as the main docs.

- [ ] **Step 5: Commit the docs and verification updates**

```bash
git add AGENTS.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: document collector full-path identifier governance"
```
