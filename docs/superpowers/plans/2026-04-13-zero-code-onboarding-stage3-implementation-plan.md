# Zero-Code Onboarding Stage 3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前 `spring-boot-iot` 仓库中，把“对象洞察/风险/遥测/运行时映射各自猜字段”的桥接模式，演进为“正式合同 + 映射规则 + 发布批次 + 关系策略”驱动的配置化主链，为后续 IoT 设备零代码接入建立稳定底座。

**Architecture:** 先把产品正式合同、厂商映射规则、复合设备关系和发布批次编译成可缓存的 `published resolver snapshot`，再让 `PAYLOAD_APPLY`、latest/history/object insight、风险指标目录统一消费同一个 `MetricIdentifierResolver`。现有大小写兼容和 latest 属性猜测继续保留为 legacy fallback，但降级为兜底路径，不再作为主真相。

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, MySQL, TDengine, Redis, JUnit 5, Mockito, Maven

---

## Scope Check

这不是一个适合“一次性大改完”的单任务，而是 4 条必须串行收口的子路线：

1. `解析真相源`：把正式合同、映射规则、关系策略和发布批次编译成统一解析快照。
2. `运行时主链`：让 `PAYLOAD_APPLY` 与证据沉淀只写 canonical identifier。
3. `读侧统一`：让 latest/history/object insight/risk 全部走同一个 canonical resolver。
4. `性能与扩展`：把 resolver 编译、缓存、失效和多层 scope 规则做成可扩展底座。

这份文档是主路线图。执行时建议按任务分 4 个独立实现批次推进，不要把全部范围压进同一轮改动。

## File Structure

### Existing files to modify

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\handler\DevicePayloadApplyStageHandler.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DevicePropertyMetadataServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceTelemetryMappingServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductMetricEvidenceServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductContractReleaseServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\governance\ProductContractGovernanceApprovalExecutor.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\NormalizedTelemetryHistoryReader.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\listener\ProductContractReleasedEventListener.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

### New files to create

- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\MetricIdentifierResolver.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\PublishedProductContractSnapshotService.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DefaultMetricIdentifierResolver.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImpl.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\MetricIdentifierResolution.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\PublishedProductContractSnapshot.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\ProductMetricResolverSnapshot.java`
- `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\ProductMetricResolverSnapshotMapper.java`
- `E:\idea\ghatg\spring-boot-iot\sql\upgrade\20260413_stage3_metric_resolver_snapshot.sql`

### Tests to create or expand

- Create:
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DefaultMetricIdentifierResolverTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImplTest.java`
- Expand:
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\handler\DevicePayloadApplyStageHandlerTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\ProductMetricEvidenceServiceImplTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImplTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImplTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\listener\ProductContractReleasedEventListenerTest.java`
  - `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImplTest.java`

## Implementation Notes

- 阶段 3 不是重做 Pipeline，固定链路仍保持 `INGRESS -> TOPIC_ROUTE -> PROTOCOL_DECODE -> DEVICE_CONTRACT -> MESSAGE_LOG -> PAYLOAD_APPLY -> TELEMETRY_PERSIST -> DEVICE_STATE -> RISK_DISPATCH -> COMPLETE`。
- `MetricIdentifierResolver` 是新主入口，目标是让写侧、读侧、风险侧都消费同一套解析结果。
- `TelemetryQueryServiceImpl` 里当前“产品正式字段 + 当前属性 + 大小写兜底”的读侧兼容逻辑要保留，但只能作为 `legacy fallback`，不再承担主真相职责。
- `VendorMetricMappingRuntimeServiceImpl` 当前仍直接查规则表并即时匹配；阶段 3 要把它收口为“消费编译后的 resolver snapshot”。
- `ProductContractReleaseServiceImpl` 与 `ProductContractReleasedEventListener` 是最合适的编译时机：正式批次生成后即可产出 resolver snapshot、刷新风险指标目录并驱动缓存失效。
- 当前对象洞察配置真相源仍是 `iot_product.metadata_json.objectInsight.customMetrics[]`，但其中 `identifier` 的合法性校验应切到“必须命中已发布 canonical identifier”。

### Task 1: Build the Canonical Resolver Backbone

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\MetricIdentifierResolver.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\PublishedProductContractSnapshotService.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DefaultMetricIdentifierResolver.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\MetricIdentifierResolution.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\model\PublishedProductContractSnapshot.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DevicePropertyMetadataServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DeviceTelemetryMappingServiceImpl.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\DefaultMetricIdentifierResolverTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldPreferPublishedCanonicalIdentifierOverCaseInsensitiveGuessing() {
    PublishedProductContractSnapshot snapshot = PublishedProductContractSnapshot.builder()
            .productId(1001L)
            .releaseBatchId(9001L)
            .canonicalAlias("l1_lf_1.value", "value")
            .canonicalAlias("VALUE", "value")
            .build();

    MetricIdentifierResolution resolution =
            resolver.resolveForRead(snapshot, "VALUE");

    assertEquals("value", resolution.canonicalIdentifier());
    assertEquals("PUBLISHED_SNAPSHOT", resolution.source());
}
```

```java
@Test
void shouldExposeOnlyPublishedPropertyIdentifiersForObjectInsight() {
    PublishedProductContractSnapshot snapshot = snapshotService.getRequiredSnapshot(1001L);
    assertTrue(snapshot.publishedIdentifiers().contains("value"));
    assertFalse(snapshot.publishedIdentifiers().contains("L1_LF_1.value"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DefaultMetricIdentifierResolverTest,PublishedProductContractSnapshotServiceImplTest" test
```

Expected: FAIL because resolver and published snapshot services do not exist yet.

- [ ] **Step 3: Write the minimal implementation**

Implement:
- `MetricIdentifierResolver` with read/runtime/governance resolution methods.
- `PublishedProductContractSnapshotService` that reads current formal product models plus latest released batch context.
- `DevicePropertyMetadataServiceImpl` and `DeviceTelemetryMappingServiceImpl` to enrich metadata with canonical identifier semantics instead of only legacy TDengine mapping.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DefaultMetricIdentifierResolverTest,PublishedProductContractSnapshotServiceImplTest,DevicePropertyMetadataServiceImplTest,DeviceTelemetryMappingServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl
git commit -m "feat: add canonical metric resolver backbone"
```

### Task 2: Move the Runtime Write Path to Canonical Identifiers

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\handler\DevicePayloadApplyStageHandler.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductMetricEvidenceServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImpl.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\handler\DevicePayloadApplyStageHandlerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\ProductMetricEvidenceServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldPersistLatestPropertiesUsingCanonicalIdentifier() {
    DeviceUpMessage upMessage = messageWithProperty("L1_LF_1.value", 0.2136d);

    handler.apply(target(product("nf-monitor-laser-rangefinder-v1"), upMessage));

    verify(devicePropertyMapper).insert(argThat(row ->
            "value".equals(row.getIdentifier()) && "0.2136".equals(row.getPropertyValue())));
}
```

```java
@Test
void shouldCaptureEvidenceWithRawAliasAndCanonicalIdentifierTogether() {
    service.captureRuntimeEvidence(product, upMessageWithEvidence("L1_LF_1.value", "value"));
    assertEquals("value", savedEvidence.getCanonicalIdentifier());
    assertEquals("L1_LF_1.value", savedEvidence.getRawIdentifier());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest,ProductMetricEvidenceServiceImplTest,VendorMetricMappingRuntimeServiceImplTest" test
```

Expected: FAIL because runtime path still relies on ad hoc rule lookup and property rewriting.

- [ ] **Step 3: Write the minimal implementation**

Implement:
- `DevicePayloadApplyStageHandler` resolves canonical identifiers through `MetricIdentifierResolver`.
- `VendorMetricMappingRuntimeServiceImpl` becomes a thin adapter around published resolver snapshots instead of the hot-path decision maker.
- `ProductMetricEvidenceServiceImpl` always stores `rawIdentifier + canonicalIdentifier + logicalChannelCode` together.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DevicePayloadApplyStageHandlerTest,ProductMetricEvidenceServiceImplTest,VendorMetricMappingRuntimeServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/handler/DevicePayloadApplyStageHandler.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service
git commit -m "feat: canonicalize payload apply and metric evidence"
```

### Task 3: Converge Latest/History/Object Insight on the Same Resolver

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\main\java\com\ghlzm\iot\telemetry\service\impl\NormalizedTelemetryHistoryReader.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductServiceImpl.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-telemetry\src\test\java\com\ghlzm\iot\telemetry\service\impl\TelemetryQueryServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\ProductServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldReadHistoryByPublishedCanonicalIdentifierBeforeCurrentPropertyFallback() {
    TelemetryHistoryBatchResponse response = service.getHistoryBatch(request(deviceId, List.of("VALUE")));
    assertEquals("value", response.getSeries().get(0).getIdentifier());
}
```

```java
@Test
void shouldRejectObjectInsightMetricConfiguredWithUnpublishedAlias() {
    ProductAddDTO dto = productWithObjectInsightMetric("L1_LF_1.value");
    assertThrows(BizException.class, () -> service.updateProduct(1001L, dto));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=TelemetryQueryServiceImplTest" test
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductServiceImplTest" test
```

Expected: FAIL because history and object insight still accept compatibility aliases on the main path.

- [ ] **Step 3: Write the minimal implementation**

Implement:
- `TelemetryQueryServiceImpl` first resolves identifiers from published snapshots; current-property lookup remains a fallback branch only.
- `NormalizedTelemetryHistoryReader` reads canonical metric code and canonical display name.
- `ProductServiceImpl` validates `metadataJson.objectInsight.customMetrics[]` against published canonical identifiers.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-telemetry -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=TelemetryQueryServiceImplTest" test
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-telemetry/src/main/java/com/ghlzm/iot/telemetry/service/impl spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java spring-boot-iot-telemetry/src/test/java/com/ghlzm/iot/telemetry/service/impl/TelemetryQueryServiceImplTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java
git commit -m "feat: align telemetry reads and insight config with canonical resolver"
```

### Task 4: Compile and Persist Published Resolver Snapshots on Release

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\entity\ProductMetricResolverSnapshot.java`
- Create: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\mapper\ProductMetricResolverSnapshotMapper.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\ProductContractReleaseServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\governance\ProductContractGovernanceApprovalExecutor.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\listener\ProductContractReleasedEventListener.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\main\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImpl.java`
- Create: `E:\idea\ghatg\spring-boot-iot\sql\upgrade\20260413_stage3_metric_resolver_snapshot.sql`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\listener\ProductContractReleasedEventListenerTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-alarm\src\test\java\com\ghlzm\iot\alarm\service\impl\RiskMetricCatalogServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldCompileResolverSnapshotWhenContractReleaseEventIsPublished() {
    listener.onProductContractReleased(new ProductContractReleasedEvent(1001L, 9001L, List.of("value")));
    verify(snapshotMapper).insert(argThat(row -> row.getReleaseBatchId().equals(9001L)));
}
```

```java
@Test
void shouldPublishRiskCatalogUsingCanonicalIdentifiersFromResolverSnapshot() {
    service.publishFromReleasedContracts(productId, releaseBatchId, contracts, Set.of("value"));
    assertEquals("value", savedCatalog.getContractIdentifier());
    assertEquals(releaseBatchId, savedCatalog.getReleaseBatchId());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductContractReleasedEventListenerTest,RiskMetricCatalogServiceImplTest" test
```

Expected: FAIL because no compiled resolver snapshot exists yet.

- [ ] **Step 3: Write the minimal implementation**

Implement:
- new resolver snapshot table keyed by `product_id + release_batch_id`.
- release execution path compiles canonical aliases, display names, telemetry mapping, risk-ready flags, and relation strategy into snapshot JSON.
- `ProductContractReleasedEventListener` uses the compiled snapshot to drive risk catalog publishing and cache invalidation.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductContractReleasedEventListenerTest,RiskMetricCatalogServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm sql/upgrade/20260413_stage3_metric_resolver_snapshot.sql spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm
git commit -m "feat: compile published metric resolver snapshots on contract release"
```

### Task 5: Add Performance Guards and Multi-Scope Extensibility

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuleServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImpl.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\main\java\com\ghlzm\iot\device\service\impl\DefaultMetricIdentifierResolver.java`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\04-数据库设计与初始化数据.md`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\VendorMetricMappingRuntimeServiceImplTest.java`
- Test: `E:\idea\ghatg\spring-boot-iot\spring-boot-iot-device\src\test\java\com\ghlzm\iot\device\service\impl\PublishedProductContractSnapshotServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldPickMostSpecificRuleScopeWithoutRuntimeTableScan() {
    MetricIdentifierResolution resolution = resolver.resolveForRuntime(snapshot, upMessage, "distance", "L1_LF_1");
    assertEquals("value", resolution.canonicalIdentifier());
    assertEquals("PRODUCT", resolution.scopeType());
}
```

```java
@Test
void shouldServeResolverSnapshotFromCacheAfterFirstLoad() {
    snapshotService.getRequiredSnapshot(productId);
    snapshotService.getRequiredSnapshot(productId);
    verify(snapshotMapper, times(1)).selectLatestReleasedByProductId(productId);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuntimeServiceImplTest,PublishedProductContractSnapshotServiceImplTest" test
```

Expected: FAIL because resolver snapshots are not cached and mapping scope precedence is not explicit.

- [ ] **Step 3: Write the minimal implementation**

Implement:
- cache compiled snapshots by `productId + releaseBatchId` in memory and/or Redis.
- define scope precedence as `PRODUCT > DEVICE_FAMILY > SCENARIO > PROTOCOL > TENANT_DEFAULT`.
- reject conflicting rules at governance write side; runtime only reads precompiled unique aliases.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=VendorMetricMappingRuntimeServiceImplTest,PublishedProductContractSnapshotServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md
git commit -m "feat: add cached multi-scope metric resolver snapshots"
```

### Task 6: Sync Docs and Real-Environment Acceptance

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\README.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\AGENTS.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\01-系统概览与架构说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\02-业务功能与流程说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\03-接口规范与接口清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\07-部署运行与配置说明.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update docs in place**

Document:
- canonical resolver as the new truth source
- resolver snapshot release/rollback semantics
- object insight only consuming published canonical identifiers
- vendor mapping scope precedence and cache invalidation rules

- [ ] **Step 2: Run module regression tests**

Run:

```powershell
mvn -pl spring-boot-iot-device -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=DefaultMetricIdentifierResolverTest,PublishedProductContractSnapshotServiceImplTest,DevicePayloadApplyStageHandlerTest,ProductMetricEvidenceServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductServiceImplTest" test
mvn -pl spring-boot-iot-telemetry -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=TelemetryQueryServiceImplTest" test
mvn -pl spring-boot-iot-alarm -am -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductContractReleasedEventListenerTest,RiskMetricCatalogServiceImplTest" test
```

Expected: PASS.

- [ ] **Step 3: Run packaging verification**

Run:

```powershell
mvn -pl spring-boot-iot-admin -am clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Run real-environment smoke checks**

Check in `application-dev.yml` environment:
- `/api/telemetry/history/batch` with canonical and alias identifiers should both return the same canonical series.
- `/products` object insight metric configuration should reject unpublished aliases.
- `/insight?deviceCode=...` trend metrics should display canonical identifiers and non-zero data when TDengine has values.

- [ ] **Step 5: Commit**

```powershell
git add README.md AGENTS.md docs
git commit -m "docs: document canonical resolver based zero-code onboarding baseline"
```

## Self-Review

- Spec coverage:
  - 配置化主真相 -> Task 1 / Task 4 / Task 5
  - 运行时 canonical 化 -> Task 2
  - latest/history/object insight 统一 -> Task 3
  - 风险目录与发布批次统一 -> Task 4
  - 性能和扩展性进阶 -> Task 5
  - 文档与真实环境验收 -> Task 6
- Placeholder scan:
  - 未保留占位符。
  - 所有任务都标明了模块、文件和验证命令。
- Type consistency:
  - 统一使用 `MetricIdentifierResolver / MetricIdentifierResolution / PublishedProductContractSnapshot` 三个核心概念。
  - 统一把 `releaseBatchId` 作为正式版本与 snapshot 失效的基准。

## Recommended Execution Order

1. Task 1 + Task 2
2. Task 3
3. Task 4
4. Task 5
5. Task 6

不要跳过 Task 1 直接做 Task 3；那样只会把当前读侧兼容逻辑继续扩大，而不是建立零代码接入主链。
