# Risk Metric Catalog Measure Truth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace heuristic monitoring-product risk catalog publication with the explicit contract-field `设为监测数据` truth source, and make `取消趋势展示` remove fields from the catalog everywhere.

**Architecture:** Keep `risk_metric_catalog` as the only downstream truth for risk binding and threshold policies, but rebuild its enabled identifiers from `product.metadataJson.objectInsight.customMetrics[]` entries marked `measure + enabled + includeInTrend` intersected with released `property` contracts. Introduce one rebuild path in `spring-boot-iot-alarm`, call it from contract release, product metadata updates, and read-side self-heal, and preserve the latest real `releaseBatchId` instead of writing `null`.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, Python 3 unittest, Maven

---

## File Map

**Backend truth-source and rebuild path**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogRebuildService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListener.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`

**Device-side event bridge**
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/ProductObjectInsightMetricsChangedEvent.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`

**Alarm listeners for rebuild**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListener.java`

**Frontend constraint and UX**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`

**Audit and docs**
- Modify: `scripts/audit-monitoring-risk-metric-catalog.py`
- Modify: `scripts/tests/test_audit_monitoring_risk_metric_catalog.py`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/superpowers/specs/2026-04-25-risk-metric-catalog-monitoring-product-backfill-design.md`

**Tests**
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListenerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts`

### Task 1: Replace Heuristic Publish Rule With Measure Truth Parsing

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void resolveRiskEnabledIdentifiersShouldUseMeasureEntriesFromProductMetadataOnly() {
    Product product = product("zhd-monitor-multi-displacement-v1", "多维检测仪");
    product.setMetadataJson("""
            {
              "objectInsight": {
                "customMetrics": [
                  {"identifier":"L1_LF_1.value","displayName":"裂缝量","group":"measure","enabled":true,"includeInTrend":true},
                  {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"measure","enabled":true,"includeInTrend":true},
                  {"identifier":"L1_JS_1.gX","displayName":"加速度X","group":"measure","enabled":true,"includeInTrend":true},
                  {"identifier":"S1_ZT_1.sensor_state","displayName":"设备状态","group":"status","enabled":true,"includeInTrend":true}
                ]
              }
            }
            """);

    Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
            product,
            null,
            null,
            List.of(
                    productModel("L1_LF_1.value"),
                    productModel("L1_QJ_1.angle"),
                    productModel("L1_JS_1.gX"),
                    productModel("S1_ZT_1.sensor_state")
            )
    );

    assertEquals(Set.of("L1_LF_1.value", "L1_QJ_1.angle", "L1_JS_1.gX"), identifiers);
}

@Test
void resolveRiskEnabledIdentifiersShouldIgnoreStatusRuntimeAndDisabledMeasureEntries() {
    Product product = product("generic-monitor-v1", "通用监测产品");
    product.setMetadataJson("""
            {
              "objectInsight": {
                "customMetrics": [
                  {"identifier":"L1_LF_1.value","displayName":"裂缝量","group":"measure","enabled":true,"includeInTrend":false},
                  {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"runtime","enabled":true,"includeInTrend":true},
                  {"identifier":"L1_JS_1.gX","displayName":"加速度X","group":"status","enabled":true,"includeInTrend":true}
                ]
              }
            }
            """);

    Set<String> identifiers = rule.resolveRiskEnabledIdentifiers(
            product,
            null,
            null,
            List.of(productModel("L1_LF_1.value"), productModel("L1_QJ_1.angle"), productModel("L1_JS_1.gX"))
    );

    assertEquals(Set.of(), identifiers);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DefaultRiskMetricCatalogPublishRuleTest test`
Expected: FAIL because the current rule still returns identifiers from product/scenario heuristics instead of `metadataJson.objectInsight.customMetrics[]`.

- [ ] **Step 3: Write the minimal implementation**

```java
private Set<String> resolveMeasureTruthIdentifiers(Product product) {
    JsonNode customMetrics = readCustomMetrics(product == null ? null : product.getMetadataJson());
    if (customMetrics == null || !customMetrics.isArray()) {
        return Set.of();
    }
    Set<String> identifiers = new LinkedHashSet<>();
    for (JsonNode item : customMetrics) {
        String identifier = normalize(item.path("identifier").asText(null));
        String group = normalize(item.path("group").asText(null));
        boolean enabled = !item.has("enabled") || item.path("enabled").asBoolean(true);
        boolean includeInTrend = !item.has("includeInTrend") || item.path("includeInTrend").asBoolean(true);
        if (StringUtils.hasText(identifier) && "measure".equalsIgnoreCase(group) && enabled && includeInTrend) {
            identifiers.add(identifier);
        }
    }
    return identifiers;
}

@Override
public Set<String> resolveRiskEnabledIdentifiers(Product product,
                                                 String scenarioCode,
                                                 Device device,
                                                 List<ProductModel> releasedContracts) {
    if (releasedContracts == null || releasedContracts.isEmpty()) {
        return Set.of();
    }
    Set<String> measureTruth = resolveMeasureTruthIdentifiers(product);
    if (measureTruth.isEmpty()) {
        return Set.of();
    }
    return releasedContracts.stream()
            .filter(contract -> contract != null && "property".equalsIgnoreCase(normalize(contract.getModelType())))
            .map(ProductModel::getIdentifier)
            .map(this::normalize)
            .filter(measureTruth::contains)
            .collect(Collectors.toCollection(LinkedHashSet::new));
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=DefaultRiskMetricCatalogPublishRuleTest test`
Expected: PASS with all rule assertions now driven by `measure` entries only.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRule.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/DefaultRiskMetricCatalogPublishRuleTest.java
git commit -m "refactor: publish risk catalog from measure truth"
```

### Task 2: Introduce One Alarm Rebuild Path For Release And Self-Heal

**Files:**
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogRebuildService.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListener.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void rebuildLatestReleaseShouldUseLatestReleaseBatchAndMeasureTruth() {
    Product product = product(1001L, "zhd-monitor-multi-displacement-v1", """
            {"objectInsight":{"customMetrics":[
              {"identifier":"L1_LF_1.value","group":"measure","enabled":true,"includeInTrend":true},
              {"identifier":"L1_QJ_1.angle","group":"measure","enabled":true,"includeInTrend":true}
            ]}}
            """);
    ProductContractReleaseBatch latestBatch = releaseBatch(7001L, 1001L);
    ProductModel crack = property(1001L, "L1_LF_1.value");
    ProductModel tilt = property(1001L, "L1_QJ_1.angle");

    when(productMapper.selectById(1001L)).thenReturn(product);
    when(releaseBatchMapper.selectList(any())).thenReturn(List.of(latestBatch));
    when(productModelMapper.selectList(any())).thenReturn(List.of(crack, tilt));

    rebuildService.rebuildLatestRelease(1001L);

    verify(riskMetricCatalogService).publishFromReleasedContracts(
            1001L,
            7001L,
            List.of(crack, tilt),
            Set.of("L1_LF_1.value", "L1_QJ_1.angle")
    );
}

@Test
void listFormalMetricsShouldRebuildWithLatestReleaseBatchInsteadOfNullBatchId() {
    when(riskMetricCatalogService.listEnabledByProduct(1001L)).thenReturn(List.of());

    service.listFormalMetrics(9001L);

    verify(riskMetricCatalogRebuildService).rebuildLatestRelease(1001L);
    verify(riskMetricCatalogService, never()).publishFromReleasedContracts(eq(1001L), isNull(), any(), any());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMetricCatalogRebuildServiceImplTest,ProductContractReleasedEventListenerTest,RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingRecommendationServiceImplTest test`
Expected: FAIL because no rebuild service exists and self-heal paths still call `publishFromReleasedContracts(..., null, ...)`.

- [ ] **Step 3: Write the minimal implementation**

```java
public interface RiskMetricCatalogRebuildService {

    void rebuildReleasedContracts(Long productId, Long releaseBatchId, List<ProductModel> releasedContracts);

    void rebuildLatestRelease(Long productId);
}
```

```java
@Service
public class RiskMetricCatalogRebuildServiceImpl implements RiskMetricCatalogRebuildService {

    @Override
    public void rebuildReleasedContracts(Long productId, Long releaseBatchId, List<ProductModel> releasedContracts) {
        Product product = productMapper.selectById(productId);
        Set<String> enabledIdentifiers = publishRule.resolveRiskEnabledIdentifiers(product, null, null, releasedContracts);
        riskMetricCatalogService.publishFromReleasedContracts(productId, releaseBatchId, releasedContracts, enabledIdentifiers);
    }

    @Override
    public void rebuildLatestRelease(Long productId) {
        ProductContractReleaseBatch latestBatch = loadLatestReleaseBatch(productId);
        if (latestBatch == null) {
            return;
        }
        List<ProductModel> releasedContracts = loadReleasedPropertyContracts(productId, latestBatch.getId());
        rebuildReleasedContracts(productId, latestBatch.getId(), releasedContracts);
    }
}
```

```java
// ProductContractReleasedEventListener
rebuildService.rebuildReleasedContracts(event.productId(), event.releaseBatchId(), releasedContracts);

// RiskPointBindingMaintenanceServiceImpl / RiskPointPendingRecommendationServiceImpl
if (catalogs.isEmpty()) {
    riskMetricCatalogRebuildService.rebuildLatestRelease(device.getProductId());
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMetricCatalogRebuildServiceImplTest,ProductContractReleasedEventListenerTest,RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingRecommendationServiceImplTest test`
Expected: PASS, and mocks should show the latest non-null `releaseBatchId` on every rebuild path.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskMetricCatalogRebuildService.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListener.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogRebuildServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductContractReleasedEventListenerTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointBindingMaintenanceServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskPointPendingRecommendationServiceImplTest.java
git commit -m "refactor: unify risk catalog rebuild flow"
```

### Task 3: Rebuild Catalog After Product Metadata Monitoring Actions

**Files:**
- Create: `spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/ProductObjectInsightMetricsChangedEvent.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Create: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListener.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`
- Create: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListenerTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void updateProductShouldPublishObjectInsightMetricCatalogSyncEventWhenMetadataChanges() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    productService = spy(new ProductServiceImpl(
            deviceMapper,
            productModelMapper,
            productContractReleaseBatchMapper,
            deviceOnlineSessionService,
            snapshotService,
            metricIdentifierResolver,
            publisher
    ));

    Product existing = buildExistingProduct();
    existing.setMetadataJson("{\"objectInsight\":{\"customMetrics\":[]}}");
    doReturn(existing).when(productService).getRequiredById(1001L);
    when(productContractReleaseBatchMapper.selectList(any())).thenReturn(List.of(releaseBatch(7001L, 1001L)));
    doReturn(true).when(productService).updateById(any(Product.class));
    doReturn(new ProductDetailVO()).when(productService).getDetailById(1001L);

    ProductAddDTO dto = buildProductDto();
    dto.setMetadataJson("""
            {"objectInsight":{"customMetrics":[
              {"identifier":"L1_QJ_1.angle","displayName":"倾角","group":"measure","enabled":true,"includeInTrend":true}
            ]}}
            """);

    productService.updateProduct(1001L, dto);

    verify(publisher).publishEvent(argThat((ProductObjectInsightMetricsChangedEvent event) ->
            Long.valueOf(1001L).equals(event.productId()) && Long.valueOf(7001L).equals(event.releaseBatchId())));
}
```

```java
@Test
void onProductObjectInsightMetricsChangedShouldTriggerCatalogRebuildForLatestRelease() {
    listener.onMetricsChanged(new ProductObjectInsightMetricsChangedEvent(1L, 1001L, 7001L, 9001L));
    verify(rebuildService).rebuildLatestRelease(1001L);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -Dtest=ProductServiceImplTest,ProductObjectInsightMetricsChangedEventListenerTest test`
Expected: FAIL because the event class and listener do not exist and `ProductServiceImpl` does not publish rebuild events.

- [ ] **Step 3: Write the minimal implementation**

```java
public record ProductObjectInsightMetricsChangedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        Long operatorUserId
) {
}
```

```java
// ProductServiceImpl.updateProduct
String previousMetadataJson = product.getMetadataJson();
applyEditableFields(product, dto);
updateById(product);
if (!Objects.equals(previousMetadataJson, product.getMetadataJson())) {
    ProductContractReleaseBatch latestBatch = loadLatestReleaseBatch(id);
    if (latestBatch != null) {
        applicationEventPublisher.publishEvent(new ProductObjectInsightMetricsChangedEvent(
                product.getTenantId(),
                product.getId(),
                latestBatch.getId(),
                null
        ));
    }
}
```

```java
@Component
public class ProductObjectInsightMetricsChangedEventListener {

    @EventListener
    public void onMetricsChanged(ProductObjectInsightMetricsChangedEvent event) {
        if (event == null || event.productId() == null) {
            return;
        }
        rebuildService.rebuildLatestRelease(event.productId());
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl spring-boot-iot-device,spring-boot-iot-alarm -Dtest=ProductServiceImplTest,ProductObjectInsightMetricsChangedEventListenerTest test`
Expected: PASS, with event publication only when `metadataJson.objectInsight.customMetrics[]` actually changes and a latest release batch exists.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-common/src/main/java/com/ghlzm/iot/common/event/governance/ProductObjectInsightMetricsChangedEvent.java \
  spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java \
  spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListener.java \
  spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/listener/ProductObjectInsightMetricsChangedEventListenerTest.java
git commit -m "feat: rebuild risk catalog after monitor metric changes"
```

### Task 4: Enforce Contract-Field UI Rules For Catalog Membership

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts`

- [ ] **Step 1: Write the failing tests**

```ts
it('blocks switching a catalog-backed measure metric to statusEvent before remove', async () => {
  const wrapper = mountWorkspace({
    product: buildProduct({
      metadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            { identifier: 'L1_QJ_1.angle', displayName: '倾角', group: 'measure', enabled: true, includeInTrend: true }
          ]
        }
      })
    }),
    models: [buildModel({ id: 11, identifier: 'L1_QJ_1.angle', modelName: '倾角' })]
  })

  await wrapper.get('[data-testid="formal-model-trend-status-event-11"]').trigger('click')

  expect(ElMessage.warning).toHaveBeenCalledWith('请先取消趋势展示，使该字段退出风险目录后再改成状态事件')
  expect(updateProductMock).not.toHaveBeenCalled()
})

it('removeProductObjectInsightMetricShouldDeleteMeasureTruthEntryCompletely', () => {
  const rows = [
    createProductObjectInsightMetricFromModel(
      { identifier: 'L1_JS_1.gX', modelName: '加速度X', sortNo: 1, specsJson: '{"unit":"mg"}' },
      'measure'
    )
  ]

  expect(removeProductObjectInsightMetric(rows, 'L1_JS_1.gX')).toEqual([])
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd spring-boot-iot-ui && npm test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/utils/productObjectInsightConfig.test.ts`
Expected: FAIL because the workspace currently allows direct group switching and does not explain the risk-catalog constraint.

- [ ] **Step 3: Write the minimal implementation**

```ts
function isCatalogTruthMetric(model: ProductModel) {
  const metric = resolveTrendMetricConfig(model)
  return Boolean(metric && metric.group === 'measure' && metric.enabled !== false && metric.includeInTrend !== false)
}

async function handleSetTrendMetric(model: ProductModel, group: ProductObjectInsightMetricGroup) {
  if (group !== 'measure' && isCatalogTruthMetric(model)) {
    ElMessage.warning(
      group === 'statusEvent'
        ? '请先取消趋势展示，使该字段退出风险目录后再改成状态事件'
        : '请先取消趋势展示，使该字段退出风险目录后再改成运行参数'
    )
    return
  }
  // existing persistence logic
}
```

```vue
<p class="product-model-designer__governance-tip">
  只有执行“设为监测数据”的正式字段才会进入风险指标目录；“取消趋势展示”会同步将其移出目录。
</p>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd spring-boot-iot-ui && npm test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/utils/productObjectInsightConfig.test.ts`
Expected: PASS with warning-message assertions and existing metric serialization behavior intact.

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue \
  spring-boot-iot-ui/src/utils/productObjectInsightConfig.ts \
  spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  spring-boot-iot-ui/src/__tests__/utils/productObjectInsightConfig.test.ts
git commit -m "feat: enforce measure truth risk catalog UX"
```

### Task 5: Rebase Audit Tooling, Governance Read Side, And Docs On Measure Truth

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java`
- Modify: `scripts/audit-monitoring-risk-metric-catalog.py`
- Modify: `scripts/tests/test_audit_monitoring_risk_metric_catalog.py`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/07-部署运行与配置说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/superpowers/specs/2026-04-25-risk-metric-catalog-monitoring-product-backfill-design.md`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void publishFromReleasedContractsShouldNotInsertStatusOrRuntimeMetricsEvenWhenContractsExist() {
    Product product = product(1001L, """
            {"objectInsight":{"customMetrics":[
              {"identifier":"L1_LF_1.value","group":"measure","enabled":true,"includeInTrend":true},
              {"identifier":"S1_ZT_1.sensor_state","group":"status","enabled":true,"includeInTrend":true}
            ]}}
            """);
    when(productMapper.selectById(1001L)).thenReturn(product);

    service.publishFromReleasedContracts(
            1001L,
            7001L,
            List.of(property("L1_LF_1.value"), property("S1_ZT_1.sensor_state")),
            Set.of("L1_LF_1.value")
    );

    verify(riskMetricCatalogMapper, never()).insert(argThat(row -> "S1_ZT_1.sensor_state".equals(row.getContractIdentifier())));
}
```

```python
def test_audit_uses_measure_truth_from_product_metadata():
    product = make_product(
        product_key="zhd-monitor-multi-displacement-v1",
        metadata_json={
            "objectInsight": {
                "customMetrics": [
                    {"identifier": "L1_QJ_1.angle", "group": "measure", "enabled": True, "includeInTrend": True}
                ]
            }
        },
    )
    contracts = [make_contract("L1_QJ_1.angle")]
    catalogs = []

    report = audit_product(product, contracts, catalogs, release_batch_id=7001)

    assert report["status"] == "MISSING_CATALOG_NEEDS_BACKFILL"
    assert report["expectedIdentifiers"] == ["L1_QJ_1.angle"]
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl spring-boot-iot-alarm -Dtest=RiskMetricCatalogServiceImplTest,RiskGovernanceServiceImplTest test`
Expected: FAIL because governance coverage and catalog semantics still assume heuristic rule coverage.

Run: `python3 -m unittest scripts.tests.test_audit_monitoring_risk_metric_catalog`
Expected: FAIL because the audit script still encodes scenario/product heuristics instead of `measure` truth.

- [ ] **Step 3: Write the minimal implementation**

```python
def resolve_measure_truth_identifiers(product_metadata_json, released_contract_identifiers):
    custom_metrics = (((product_metadata_json or {}).get("objectInsight") or {}).get("customMetrics") or [])
    truth = []
    for item in custom_metrics:
        if item.get("group") != "measure":
            continue
        if item.get("enabled", True) is False:
            continue
        if item.get("includeInTrend", True) is False:
            continue
        identifier = normalize_identifier(item.get("identifier"))
        if identifier and identifier in released_contract_identifiers:
            truth.append(identifier)
    return truth
```

```java
long publishableContractPropertyCount = riskMetricCatalogPublishRule
        .resolveRiskEnabledIdentifiers(product, null, null, propertyModels)
        .size();
```

Update docs to explicitly state:

```md
- 风险指标目录当前只发布在契约字段页被明确执行“设为监测数据”的正式 `property` 字段。
- “取消趋势展示”会同步将对应正式字段移出 `risk_metric_catalog`。
- “设为状态事件 / 设为运行参数” 不会进入风险目录真相源。
```

- [ ] **Step 4: Run the full verification suite**

Run: `mvn -pl spring-boot-iot-alarm,spring-boot-iot-device -Dtest=DefaultRiskMetricCatalogPublishRuleTest,RiskMetricCatalogServiceImplTest,RiskMetricCatalogRebuildServiceImplTest,ProductContractReleasedEventListenerTest,ProductObjectInsightMetricsChangedEventListenerTest,RiskPointBindingMaintenanceServiceImplTest,RiskPointPendingRecommendationServiceImplTest,ProductServiceImplTest test`
Expected: PASS

Run: `cd spring-boot-iot-ui && npm test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/utils/productObjectInsightConfig.test.ts`
Expected: PASS

Run: `python3 -m unittest scripts.tests.test_audit_monitoring_risk_metric_catalog`
Expected: PASS

Run: `git diff --check`
Expected: no output

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java \
  spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskMetricCatalogServiceImplTest.java \
  scripts/audit-monitoring-risk-metric-catalog.py \
  scripts/tests/test_audit_monitoring_risk_metric_catalog.py \
  docs/03-接口规范与接口清单.md \
  docs/07-部署运行与配置说明.md \
  docs/08-变更记录与技术债清单.md \
  docs/superpowers/specs/2026-04-25-risk-metric-catalog-monitoring-product-backfill-design.md
git commit -m "docs: rebase risk catalog governance on measure truth"
```

## Self-Review Checklist

- Spec coverage:
  - Truth source moved to `设为监测数据`: Task 1, Task 2, Task 3, Task 4, Task 5
  - `取消趋势展示` removes catalog entries: Task 2, Task 4, Task 5
  - `设为状态事件 / 设为运行参数` stay outside risk catalog: Task 1, Task 4, Task 5
  - Self-heal and audit no longer guess by product key: Task 2, Task 5
  - `releaseBatchId` no longer written as `null`: Task 2
- Placeholder scan: no `TODO`/`TBD` or implicit “write tests later” steps remain.
- Type consistency:
  - Event name is `ProductObjectInsightMetricsChangedEvent`
  - Rebuild entrypoint name is `rebuildLatestRelease(Long productId)`
  - Measure truth checks always use `group=measure && enabled && includeInTrend`

## Execution Handoff

Plan complete and saved to `/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/docs/superpowers/plans/2026-04-26-risk-metric-catalog-measure-truth-implementation-plan.md`.

Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, and use your earlier approval for multi-task development.
2. **Inline Execution** - Execute tasks in this session using executing-plans, with checkpoints here.

Because you already asked me to start execution and previously approved multi-task development, default to **Subagent-Driven** for this plan unless a blocker appears.
