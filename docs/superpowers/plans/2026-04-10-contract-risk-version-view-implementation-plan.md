# Contract And Risk Version View Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-class contract release history and risk metric catalog-by-batch version view in `/products`, so operators can answer which contract version was released and which risk metrics were published with it.

**Architecture:** Reuse existing release-batch read APIs as the contract version truth source, extend `risk-governance/metric-catalogs` with `releaseBatchId` filtering, and compose both datasets inside `ProductModelDesignerWorkspace` as a product-scoped version ledger. Keep the scope read-only: no new tables, no new write flows, and no parallel version objects.

**Tech Stack:** Spring Boot 4, Java 17, MyBatis-Plus, Vue 3, TypeScript, Vitest, JUnit 5, Mockito

---

## File Structure

### Task 1 ownership: risk metric catalog batch filter in `spring-boot-iot-alarm`

- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskGovernanceController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskGovernanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`

### Task 2 ownership: product workbench version ledger in `spring-boot-iot-ui`

- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

### Task 3 ownership: documentation sync

- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

## Task 1: Add release-batch filtering to risk metric catalog paging

**Files:**
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/controller/RiskGovernanceController.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/RiskGovernanceService.java`
- Modify: `spring-boot-iot-alarm/src/main/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImpl.java`
- Modify: `spring-boot-iot-alarm/src/test/java/com/ghlzm/iot/alarm/service/impl/RiskGovernanceServiceImplTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@Test
void pageMetricCatalogsShouldFilterByReleaseBatchId() {
    RiskGovernanceServiceImpl service = new RiskGovernanceServiceImpl(
            deviceMapper,
            riskPointMapper,
            riskPointDeviceMapper,
            ruleDefinitionMapper,
            riskMetricCatalogMapper,
            productModelMapper,
            productMapper,
            productContractReleaseBatchMapper,
            linkageRuleMapper,
            emergencyPlanMapper,
            linkageBindingMapper,
            emergencyPlanBindingMapper,
            backfillService
    );
    RiskMetricCatalog catalog = new RiskMetricCatalog();
    catalog.setId(9101L);
    catalog.setProductId(1001L);
    catalog.setReleaseBatchId(7001L);
    catalog.setContractIdentifier("value");
    catalog.setRiskMetricCode("RM_1001_VALUE");
    catalog.setRiskMetricName("裂缝监测值");
    when(riskMetricCatalogMapper.selectPage(any(), any())).thenReturn(new Page<RiskMetricCatalog>(1L, 10L, 1L)
            .setRecords(List.of(catalog)));

    PageResult<RiskMetricCatalogItemVO> page = service.pageMetricCatalogs(1001L, 7001L, 1L, 10L);

    assertEquals(1L, page.getTotal());
    assertEquals(7001L, page.getRecords().get(0).getReleaseBatchId());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RiskMetricCatalog>> captor =
            ArgumentCaptor.forClass((Class) com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
    verify(riskMetricCatalogMapper).selectPage(any(), captor.capture());
    assertTrue(captor.getValue().getExpression().getNormal().stream()
            .anyMatch(segment -> String.valueOf(segment).contains("release_batch_id")));
}
```

- [ ] **Step 2: Run the targeted backend test and verify RED**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskGovernanceServiceImplTest" test
```

Expected: `BUILD FAILURE`, with `pageMetricCatalogs` signature mismatch or missing release-batch filter assertion failure.

- [ ] **Step 3: Implement the minimal backend changes**

```java
@GetMapping("/metric-catalogs")
public R<PageResult<RiskMetricCatalogItemVO>> pageMetricCatalogs(@RequestParam(required = false) Long productId,
                                                                 @RequestParam(required = false) Long releaseBatchId,
                                                                 @RequestParam(required = false) Long pageNum,
                                                                 @RequestParam(required = false) Long pageSize) {
    return R.ok(riskGovernanceService.pageMetricCatalogs(productId, releaseBatchId, pageNum, pageSize));
}
```

```java
PageResult<RiskMetricCatalogItemVO> pageMetricCatalogs(Long productId,
                                                       Long releaseBatchId,
                                                       Long pageNum,
                                                       Long pageSize);
```

```java
public PageResult<RiskMetricCatalogItemVO> pageMetricCatalogs(Long productId,
                                                              Long releaseBatchId,
                                                              Long pageNum,
                                                              Long pageSize) {
    Page<RiskMetricCatalog> page = PageQueryUtils.buildPage(pageNum, pageSize);
    Page<RiskMetricCatalog> result = riskMetricCatalogMapper.selectPage(page, new LambdaQueryWrapper<RiskMetricCatalog>()
            .eq(RiskMetricCatalog::getDeleted, 0)
            .eq(productId != null, RiskMetricCatalog::getProductId, productId)
            .eq(releaseBatchId != null, RiskMetricCatalog::getReleaseBatchId, releaseBatchId)
            .orderByDesc(RiskMetricCatalog::getUpdateTime)
            .orderByDesc(RiskMetricCatalog::getCreateTime)
            .orderByDesc(RiskMetricCatalog::getId));
    List<RiskMetricCatalogItemVO> records = result.getRecords().stream()
            .map(this::toMetricCatalogItem)
            .toList();
    return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
}
```

- [ ] **Step 4: Re-run the targeted backend test and verify GREEN**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskGovernanceServiceImplTest" test
```

Expected: `BUILD SUCCESS`

## Task 2: Surface a product-scoped version ledger in the contract field workspace

**Files:**
- Modify: `spring-boot-iot-ui/src/api/riskGovernance.ts`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Write the failing front-end test**

```ts
it('loads release history and shows risk metric catalog rows for the selected release batch', async () => {
  mockPageProductContractReleaseBatches.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 2,
      pageNum: 1,
      pageSize: 20,
      records: [
        { id: 99002, scenarioCode: 'phase1-crack', releaseStatus: 'RELEASED', releasedFieldCount: 2, createTime: '2026-04-10 18:00:00' },
        { id: 99001, scenarioCode: 'phase1-crack', releaseStatus: 'ROLLED_BACK', releasedFieldCount: 1, createTime: '2026-04-09 18:00:00' }
      ]
    }
  })
  mockPageRiskMetricCatalogs.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 20,
      records: [
        { id: 9101, releaseBatchId: 99002, contractIdentifier: 'value', riskMetricName: '裂缝监测值', metricRole: 'PRIMARY', lifecycleStatus: 'ACTIVE' }
      ]
    }
  })

  const wrapper = mountWorkspace()
  await flushPromises()
  await nextTick()

  expect(mockPageRiskMetricCatalogs).toHaveBeenCalledWith({ productId: 1001, releaseBatchId: 99002, pageNum: 1, pageSize: 20 })
  expect(wrapper.text()).toContain('版本台账')
  expect(wrapper.text()).toContain('批次 99002')
  expect(wrapper.text()).toContain('裂缝监测值')
})
```

- [ ] **Step 2: Run the targeted front-end test and verify RED**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: FAIL because the component does not yet request risk catalogs or render version-ledger text.

- [ ] **Step 3: Implement the minimal front-end changes**

```ts
export function pageRiskMetricCatalogs(
  params: Pick<RiskGovernanceGapQuery, 'productId' | 'pageNum' | 'pageSize'> & { releaseBatchId?: IdType | null } = {}
): Promise<ApiEnvelope<PageResult<RiskMetricCatalogItem>>> {
  const queryString = buildQueryString(params);
  const path = queryString
    ? `/api/risk-governance/metric-catalogs?${queryString}`
    : '/api/risk-governance/metric-catalogs';
  return request<PageResult<RiskMetricCatalogItem>>(path, { method: 'GET' });
}
```

```ts
const releaseLedgerRows = ref<ProductContractReleaseBatch[]>([])
const selectedLedgerBatchId = ref<IdType | null>(null)
const selectedLedgerMetrics = ref<RiskMetricCatalogItem[]>([])
```

```ts
const [modelResponse, releaseResponse] = await Promise.all([
  productApi.listProductModels(productId),
  productApi.pageProductContractReleaseBatches(productId, { pageNum: 1, pageSize: 20 })
])
releaseLedgerRows.value = releaseResponse.data?.records ?? []
selectedLedgerBatchId.value = releaseLedgerRows.value[0]?.id ?? null
await loadReleaseLedgerMetrics()
```

```ts
async function loadReleaseLedgerMetrics() {
  if (!props.product?.id || selectedLedgerBatchId.value == null) {
    selectedLedgerMetrics.value = []
    return
  }
  const response = await pageRiskMetricCatalogs({
    productId: props.product.id,
    releaseBatchId: selectedLedgerBatchId.value,
    pageNum: 1,
    pageSize: 20
  })
  selectedLedgerMetrics.value = response.data?.records ?? []
}
```

```vue
<section v-if="releaseLedgerRows.length" class="product-model-designer__version-ledger" data-testid="contract-version-ledger">
  <div class="product-model-designer__rollback-preview-head">
    <div>
      <strong>版本台账</strong>
      <p>查看每次合同发布批次，以及该批次同步发布的风险指标目录。</p>
    </div>
  </div>
  <div class="product-model-designer__version-ledger-grid">
    <button
      v-for="batch in releaseLedgerRows"
      :key="String(batch.id)"
      type="button"
      class="product-model-designer__version-ledger-batch"
      :class="{ 'is-active': String(selectedLedgerBatchId) === String(batch.id) }"
      @click="selectLedgerBatch(batch.id)"
    >
      <strong>{{ `批次 ${batch.id}` }}</strong>
      <span>{{ batch.releaseStatus || '--' }} · {{ batch.scenarioCode || '--' }}</span>
    </button>
  </div>
  <div class="product-model-designer__rollback-preview-list">
    <article
      v-for="metric in selectedLedgerMetrics"
      :key="`${metric.id || metric.contractIdentifier || '--'}`"
      class="product-model-designer__rollback-preview-item"
    >
      <strong>{{ metric.riskMetricName || metric.contractIdentifier || '--' }}</strong>
      <span>{{ metric.contractIdentifier || '--' }} · {{ metric.metricRole || '--' }}</span>
      <span>{{ metric.lifecycleStatus || '--' }}</span>
    </article>
  </div>
</section>
```

- [ ] **Step 4: Re-run the targeted front-end test and verify GREEN**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: PASS

## Task 3: Sync docs to the delivered version-view boundary

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the approved capability statements**

```md
- `2026-04-10` 起，`/products -> 契约字段` 已补齐首版“版本台账”：当前可按产品查看合同发布批次历史，并在同页按 `releaseBatchId` 查看该批次同步发布的风险指标目录明细，用于回答“哪一版合同发布了哪些正式指标”。
```

- [ ] **Step 2: Run diff hygiene verification**

Run:

```bash
git diff --check
```

Expected: no whitespace or merge-marker errors.

- [ ] **Step 3: Run the final targeted regression pack**

Run:

```bash
mvn -pl spring-boot-iot-alarm -am "-DskipTests=false" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=RiskGovernanceServiceImplTest" test
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts
```

Expected: both commands succeed.

## Self-Review

Spec coverage:

1. Contract release history is surfaced through existing release-batch APIs.
2. Risk metric catalog rows become batch-addressable through `releaseBatchId`.
3. Product workbench exposes a read-only version ledger without adding new tables or write flows.
4. Documentation sync covers behavior, API, and acceptance scope.

Placeholder scan:

1. No `TODO`, `TBD`, or deferred placeholders remain.
2. Each task includes file paths, commands, and expected results.

Type consistency:

1. The new backend filter uses `releaseBatchId` consistently in controller, service, and UI query params.
2. The version ledger composes `ProductContractReleaseBatch` rows with `RiskMetricCatalogItem` rows without inventing new DTO names.
