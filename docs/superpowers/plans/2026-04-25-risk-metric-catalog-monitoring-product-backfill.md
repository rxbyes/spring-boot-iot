# Monitoring Product Risk Metric Catalog Backfill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make monitoring products with published formal contracts publish the correct risk metric catalog rows so risk-point formal binding can select `CXH15522812`'s crack metric and other approved monitoring metrics.

**Architecture:** Keep `risk_metric_catalog` as the only formal binding truth. Add field-level semantic parsing in the alarm module so full-path formal contract identifiers such as `L1_LF_1.value` and `L1_GP_1.gpsTotalX` are recognized as risk-ready metrics while preserving the real contract identifier in catalog rows. Do not introduce schema changes or a direct `iot_product_model` fallback in risk binding.

**Tech Stack:** Java 17, Spring Boot 4 modular monolith, MyBatis-Plus, JUnit 5, Mockito, Maven, Markdown docs.

---

## File Structure

- Modify `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`: add RED tests for full-path multi-displacement, full-path GNSS, and generic `value` false-positive prevention.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`: replace all-scenario union matching with field-level semantic parsing that returns real contract identifiers.
- Modify `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`: add RED tests proving full-path catalog rows preserve `contractIdentifier` and write semantic metadata.
- Modify `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`: stop canonicalizing full-path contract identifiers to short identifiers for catalog rows, and resolve semantic profiles per field.
- Modify `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java`: update/add event listener regression for full-path publication.
- Modify docs as required by `AGENTS.md`: `README.md`, `AGENTS.md`, `docs/02-业务功能与流程说明.md`, `docs/04-数据库设计与初始化数据.md`, `docs/08-变更记录与技术债清单.md`, and `docs/21-业务功能清单与验收标准.md` if the touched sections contain risk-directory scope.

## Existing Dirty Worktree Guard

Before editing, run:

```bash
git status --short
```

Expected: existing unrelated edits may remain in `spring-boot-iot-device/**`, `README.md`, `AGENTS.md`, `docs/02-*`, `docs/08-*`, and `docs/appendix/iot-field-governance-and-sop.md`. Do not revert them. When staging/committing this work, stage only files changed for this plan after reviewing diffs.

### Task 1: RED Tests For Field-Level Publish Rule

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`

- [ ] **Step 1: Add failing tests**

Add these tests to `DefaultRiskMetricCatalogPublishRuleTest`:

```java
@Test
void resolveRiskEnabledIdentifiersShouldPublishOnlyCrackMetricFromMultiDisplacementFullPath() {
    ProductModel crackValue = productModel("L1_LF_1.value");
    ProductModel tiltAngle = productModel("L1_QJ_1.angle");
    ProductModel accelX = productModel("L1_JS_1.gX");

    Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
            null,
            List.of(crackValue, tiltAngle, accelX)
    );

    assertEquals(Set.of("L1_LF_1.value"), identifiers);
}

@Test
void resolveRiskEnabledIdentifiersShouldPublishOnlyGnssTotalFullPathMetrics() {
    ProductModel gpsInitial = productModel("L1_GP_1.gpsInitial");
    ProductModel gpsTotalX = productModel("L1_GP_1.gpsTotalX");
    ProductModel gpsTotalY = productModel("L1_GP_1.gpsTotalY");
    ProductModel gpsTotalZ = productModel("L1_GP_1.gpsTotalZ");
    ProductModel accelX = productModel("L1_JS_1.gX");

    Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
            null,
            List.of(gpsInitial, gpsTotalX, gpsTotalY, gpsTotalZ, accelX)
    );

    assertEquals(Set.of("L1_GP_1.gpsTotalX", "L1_GP_1.gpsTotalY", "L1_GP_1.gpsTotalZ"), identifiers);
}

@Test
void resolveRiskEnabledIdentifiersShouldNotPublishGenericValueWithoutRiskContext() {
    ProductModel genericValue = productModel("value");

    Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(null, List.of(genericValue));

    assertEquals(Set.of(), identifiers);
}
```

- [ ] **Step 2: Run RED command**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=DefaultRiskMetricCatalogPublishRuleTest test
```

Expected before implementation: at least one new test fails because `L1_LF_1.value` and `L1_GP_1.gpsTotalX/Y/Z` are not published, or generic `value` is still incorrectly published.

### Task 2: GREEN Field-Level Publish Rule

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`

- [ ] **Step 1: Implement minimal field semantic parsing**

Replace union matching in `DefaultRiskMetricCatalogPublishRule` with helper-based parsing. The implementation must return the original formal contract identifier when the field is risk-ready.

Use this shape:

```java
private static final Set<String> GNSS_TOTALS = Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ");
private static final Set<String> DEEP_DISPLACEMENTS = Set.of("dispsX", "dispsY");

@Override
public Set<String> resolveRiskEnabledIdentifiers(Device device, List<ProductModel> releasedContracts) {
    if (releasedContracts == null || releasedContracts.isEmpty()) {
        return Set.of();
    }
    Set<String> rawIdentifiers = collectReleasedIdentifiers(releasedContracts);
    Set<String> enabledIdentifiers = new LinkedHashSet<>();
    for (String identifier : rawIdentifiers) {
        if (isRiskReady(identifier, rawIdentifiers)) {
            enabledIdentifiers.add(identifier);
        }
    }
    return enabledIdentifiers;
}

private boolean isRiskReady(String identifier, Set<String> allIdentifiers) {
    ParsedIdentifier parsed = ParsedIdentifier.parse(identifier);
    if (parsed == null) {
        return false;
    }
    if ("L1".equals(parsed.level()) && "LF".equals(parsed.type())) {
        return !StringUtils.hasText(parsed.leaf()) || "value".equals(parsed.leaf());
    }
    if ("L1".equals(parsed.level()) && "GP".equals(parsed.type())) {
        return GNSS_TOTALS.contains(parsed.leaf());
    }
    if (DEEP_DISPLACEMENTS.contains(parsed.original())) {
        return true;
    }
    if (GNSS_TOTALS.contains(parsed.original())) {
        return true;
    }
    if ("value".equals(parsed.original())) {
        return allIdentifiers.contains("sensor_state") || allIdentifiers.contains("totalValue");
    }
    return false;
}
```

The final code may use a private record:

```java
private record ParsedIdentifier(String original, String level, String type, String channel, String leaf) {
}
```

`ParsedIdentifier.parse("L1_LF_1.value")` should produce `level=L1`, `type=LF`, `channel=1`, `leaf=value`. `ParsedIdentifier.parse("value")` should produce `original=value` with blank level/type/channel and `leaf=value`.

- [ ] **Step 2: Run GREEN command**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=DefaultRiskMetricCatalogPublishRuleTest test
```

Expected after implementation: all `DefaultRiskMetricCatalogPublishRuleTest` tests pass.

### Task 3: RED Tests For Full-Path Catalog Persistence

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`

- [ ] **Step 1: Add failing full-path semantic test**

Add this test:

```java
@Test
void publishFromReleasedContractsShouldPreserveFullPathContractIdentifierAndWriteCrackSemantics() {
    RiskMetricCatalogServiceImpl service = new RiskMetricCatalogServiceImpl(
            riskMetricCatalogMapper,
            productMapper,
            normativeMetricDefinitionService,
            List.of(new KeywordRiskMetricScenarioResolver()),
            applicationEventPublisher,
            snapshotService
    );
    Product product = new Product();
    product.setId(2002L);
    product.setTenantId(1L);
    product.setProductKey("zhd-monitor-multi-displacement-v1");
    when(productMapper.selectById(2002L)).thenReturn(product);
    when(normativeMetricDefinitionService.listByScenario("phase1-crack")).thenReturn(List.of(
            normative("phase1-crack", "value", "mm", 1,
                    "{\"thresholdKind\":\"absolute\",\"riskCategory\":\"CRACK\",\"metricRole\":\"PRIMARY\"}")
    ));

    ProductModel crackValue = new ProductModel();
    crackValue.setId(4101L);
    crackValue.setProductId(2002L);
    crackValue.setIdentifier("L1_LF_1.value");
    crackValue.setModelName("裂缝量");
    crackValue.setDataType("double");

    service.publishFromReleasedContracts(2002L, 8001L, List.of(crackValue), Set.of("L1_LF_1.value"));

    verify(riskMetricCatalogMapper).insert(argThat((RiskMetricCatalog row) ->
            Long.valueOf(2002L).equals(row.getProductId())
                    && Long.valueOf(8001L).equals(row.getReleaseBatchId())
                    && "L1_LF_1.value".equals(row.getContractIdentifier())
                    && "value".equals(row.getNormativeIdentifier())
                    && "phase1-crack".equals(row.getSourceScenarioCode())
                    && "CRACK".equals(row.getRiskCategory())
                    && "裂缝量".equals(row.getRiskMetricName())
    ));
}
```

- [ ] **Step 2: Update resolver snapshot expectation**

Change the existing `publishFromReleasedContractsShouldUseCanonicalIdentifiersFromResolverSnapshot` expectation so full-path aliases are preserved:

```java
service.publishFromReleasedContracts(1001L, 7001L, List.of(releasedAlias), Set.of("L1_LF_1.value"));

verify(riskMetricCatalogMapper).insert(argThat((RiskMetricCatalog row) ->
        Long.valueOf(7001L).equals(row.getReleaseBatchId())
                && "value".equals(row.getNormativeIdentifier())
                && "L1_LF_1.value".equals(row.getContractIdentifier())
                && "RM_1001_L1_LF_1_VALUE".equals(row.getRiskMetricCode())
                && "激光测距值".equals(row.getRiskMetricName())
));
```

- [ ] **Step 3: Run RED command**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=RiskMetricCatalogServiceImplTest test
```

Expected before service implementation: the new test fails because `sourceScenarioCode` or `normativeIdentifier` is missing, or an existing test fails because the service canonicalizes `L1_LF_1.value` to `value`.

### Task 4: GREEN Catalog Service Semantic Persistence

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java`
- Test: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`

- [ ] **Step 1: Preserve real contract identifiers**

In `publishFromReleasedContracts`, use trimmed real identifiers for:

```java
String identifier = normalize(contract == null ? null : contract.getIdentifier());
```

Do not pass full-path identifiers through `snapshot.canonicalAliasOf(...)` when deciding `contractIdentifier` or `riskMetricCode`.

- [ ] **Step 2: Add field-level semantic reference helper**

Add a private record and helper inside `RiskMetricCatalogServiceImpl`:

```java
private record MetricSemanticReference(String scenarioCode, String normativeIdentifier) {
}

private MetricSemanticReference resolveSemanticReference(String identifier, String fallbackScenarioCode) {
    String normalized = normalize(identifier);
    if (!StringUtils.hasText(normalized)) {
        return new MetricSemanticReference(fallbackScenarioCode, normalized);
    }
    String leaf = normalized.contains(".") ? normalized.substring(normalized.lastIndexOf('.') + 1) : normalized;
    String prefix = normalized.contains(".") ? normalized.substring(0, normalized.lastIndexOf('.')) : null;
    if (StringUtils.hasText(prefix)) {
        String upperPrefix = prefix.toUpperCase(Locale.ROOT);
        if (upperPrefix.matches("L1_LF_\\d+") && (!StringUtils.hasText(leaf) || "value".equals(leaf))) {
            return new MetricSemanticReference("phase1-crack", "value");
        }
        if (upperPrefix.matches("L1_GP_\\d+") && Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ").contains(leaf)) {
            return new MetricSemanticReference("phase2-gnss", leaf);
        }
    }
    if (Set.of("gpsTotalX", "gpsTotalY", "gpsTotalZ").contains(normalized)) {
        return new MetricSemanticReference("phase2-gnss", normalized);
    }
    if (Set.of("dispsX", "dispsY").contains(normalized)) {
        return new MetricSemanticReference("phase3-deep-displacement", normalized);
    }
    if ("value".equals(normalized)) {
        return new MetricSemanticReference(fallbackScenarioCode, "value");
    }
    return new MetricSemanticReference(fallbackScenarioCode, normalized);
}
```

- [ ] **Step 3: Resolve normative metadata by semantic reference**

Change `resolveSemanticProfiles(...)` so each enabled contract:

1. Resolves a `MetricSemanticReference`.
2. Loads definitions by that reference's `scenarioCode`.
3. Looks up definition by `normativeIdentifier`.
4. Calls `buildSemanticProfile(contract, definition, reference.scenarioCode(), reference.normativeIdentifier())`.

Use a local cache:

```java
Map<String, Map<String, NormativeMetricDefinition>> normativeCache = new LinkedHashMap<>();
Map<String, NormativeMetricDefinition> normativeByIdentifier = normativeCache.computeIfAbsent(
        reference.scenarioCode(),
        this::loadNormativeByIdentifier
);
```

- [ ] **Step 4: Sanitize risk metric code**

Change `buildRiskMetricCode` so `L1_LF_1.value` becomes `RM_<productId>_L1_LF_1_VALUE`:

```java
String codeIdentifier = normalizedIdentifier.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
codeIdentifier = codeIdentifier.replaceAll("^_+|_+$", "");
return "RM_" + (productId == null ? "GLOBAL" : productId) + "_" + codeIdentifier;
```

- [ ] **Step 5: Run GREEN command**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=RiskMetricCatalogServiceImplTest test
```

Expected after implementation: all `RiskMetricCatalogServiceImplTest` tests pass.

### Task 5: Listener Regression For Full-Path Publication

**Files:**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java`
- Modify if needed: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListener.java`

- [ ] **Step 1: Update/add listener test**

Add or adjust a test so the publish rule receives full-path contract identifiers and the catalog service is called with full-path risk-enabled identifiers:

```java
@Test
void onProductContractReleasedShouldPublishFullPathRiskMetricCatalogIdentifiers() {
    ProductContractReleasedEventListener listener = new ProductContractReleasedEventListener(
            productModelMapper,
            publishRule,
            riskMetricCatalogService,
            resolverSnapshotMapper
    );
    ProductModel crackValue = propertyModel(4101L, 2002L, "L1_LF_1.value", "裂缝量");
    ProductModel tiltAngle = propertyModel(4102L, 2002L, "L1_QJ_1.angle", "水平面夹角");
    when(productModelMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(crackValue, tiltAngle));
    when(publishRule.resolveRiskEnabledIdentifiers(null, List.of(crackValue, tiltAngle)))
            .thenReturn(Set.of("L1_LF_1.value"));

    listener.onProductContractReleased(new ProductContractReleasedEvent(
            1L,
            2002L,
            8001L,
            "phase1-crack",
            List.of("L1_LF_1.value", "L1_QJ_1.angle"),
            9001L,
            99001L
    ));

    verify(riskMetricCatalogService).publishFromReleasedContracts(
            2002L,
            8001L,
            List.of(crackValue, tiltAngle),
            Set.of("L1_LF_1.value")
    );
}
```

- [ ] **Step 2: Keep listener implementation minimal**

If the test fails only because `canonicalizeContractsForPublishRule(...)` changes identifiers, update listener to pass original `releasedContracts` to the publish rule:

```java
Set<String> riskEnabledIdentifiers = riskMetricCatalogPublishRule == null
        ? Set.of()
        : riskMetricCatalogPublishRule.resolveRiskEnabledIdentifiers(null, releasedContracts);
```

Keep resolver snapshot persistence unchanged unless a test demonstrates it rewrites full-path identifiers incorrectly.

- [ ] **Step 3: Run listener GREEN command**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=ProductContractReleasedEventListenerTest test
```

Expected after implementation: listener tests pass and full-path identifiers reach `RiskMetricCatalogService`.

### Task 6: Docs And Final Verification

**Files:**
- Modify as needed: `README.md`
- Modify as needed: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify if relevant: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify if relevant: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update behavior docs**

Document these facts in the existing risk-directory / product-governance sections:

```markdown
`2026-04-25` 起，风险指标目录发布链路补齐监测型产品的字段级风险语义识别：多维位移产品只把 `L1_LF_1.value` 作为裂缝量发布到风险目录，GNSS 位移产品只把 `L1_GP_1.gpsTotalX / gpsTotalY / gpsTotalZ` 发布到风险目录；倾角、加速度、泥位和 GNSS 基准站仍不进入本轮风险绑定正式目录。风险绑定候选继续以 `risk_metric_catalog` 为唯一读侧真相，不从 `iot_product_model` 旁路兜底。
```

Do not overwrite unrelated existing edits in these files. If a doc already contains new local text from another task, edit around it carefully.

- [ ] **Step 2: Run full targeted verification**

Run:

```bash
mvn -pl spring-boot-iot-alarm -DskipTests=false -Dtest=DefaultRiskMetricCatalogPublishRuleTest,RiskMetricCatalogServiceImplTest,ProductContractReleasedEventListenerTest test
```

Expected: all targeted alarm tests pass.

- [ ] **Step 3: Inspect diff and stage only this task's files**

Run:

```bash
git diff -- spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImpl.java spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListener.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java
```

Expected: diff only contains planned changes. Then stage exact files changed for this task and commit:

```bash
git add <exact planned files>
git commit -m "fix: publish monitoring product risk metric catalogs"
```

## Plan Self-Review

- Spec coverage: covered target products, excluded products, full-path `contractIdentifier`, normative metadata, no schema change, docs, and tests.
- Placeholder scan: no placeholder steps; each task has concrete files, code snippets, commands, and expected results.
- Type consistency: all referenced Java classes and methods exist in the current codebase; new helper records are private to their implementation classes.
