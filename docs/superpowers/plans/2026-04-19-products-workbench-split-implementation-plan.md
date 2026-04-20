# Products Workbench Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor `/products` from a single mixed workbench into a list entry plus dedicated product pages for overview, devices, contracts, mapping rules, and release ledger.

**Architecture:** Keep `/products` as the list route, add detail routes under `/products/:productId/*`, introduce a lightweight backend overview-summary endpoint, then split the current `ProductWorkbenchView` responsibilities into focused workspaces with one compatibility redirect layer for legacy `openProductId + workbenchView` links.

**Tech Stack:** Vue 3, Vue Router, Vitest, Spring Boot, Java 17, MyBatis-Plus, Element Plus

---

### Task 1: Add backend overview-summary support for the new overview page

**Files:**
- Create: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductOverviewSummaryVO.java`
- Create: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductControllerTest.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductService.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java`
- Modify: `spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductController.java`
- Modify: `spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java`

- [ ] **Step 1: Write the failing backend tests**

```java
@Test
void getOverviewSummaryShouldDelegateToService() {
    ProductOverviewSummaryVO summary = new ProductOverviewSummaryVO();
    summary.setProductId(1001L);
    summary.setFormalFieldCount(3);
    summary.setLatestReleaseBatchId(7001L);
    when(productService.getOverviewSummary(1001L)).thenReturn(summary);

    R<ProductOverviewSummaryVO> response = controller.getOverviewSummary(1001L);

    assertEquals(1001L, response.getData().getProductId());
    assertEquals(7001L, response.getData().getLatestReleaseBatchId());
}

@Test
void getOverviewSummaryShouldAggregateProductAndLatestBatch() {
    Product product = new Product();
    product.setId(1001L);
    product.setProductName("裂缝监测产品");
    doReturn(product).when(productService).getRequiredById(1001L);
    when(productModelMapper.selectCount(any())).thenReturn(3L);

    ProductOverviewSummaryVO summary = productService.getOverviewSummary(1001L);

    assertEquals(1001L, summary.getProductId());
    assertEquals(3, summary.getFormalFieldCount());
}
```

- [ ] **Step 2: Run the backend tests and confirm failure**

```powershell
mvn -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductControllerTest,ProductServiceImplTest" test
```

- [ ] **Step 3: Add VO, service contract, and controller endpoint**

```java
@Data
public class ProductOverviewSummaryVO {
    private Long productId;
    private String productKey;
    private String productName;
    private Long deviceCount;
    private Long onlineDeviceCount;
    private Integer formalFieldCount;
    private Long latestReleaseBatchId;
    private Integer latestReleasedFieldCount;
    private String latestReleaseStatus;
}
```

```java
public interface ProductService extends IService<Product> {
    ProductOverviewSummaryVO getOverviewSummary(Long id);
}
```

```java
@GetMapping("/api/device/product/{id}/overview-summary")
public R<ProductOverviewSummaryVO> getOverviewSummary(@PathVariable Long id) {
    return R.ok(productService.getOverviewSummary(id));
}
```

- [ ] **Step 4: Implement the aggregation in `ProductServiceImpl`**

```java
@Override
public ProductOverviewSummaryVO getOverviewSummary(Long id) {
    ProductDetailVO detail = getDetailById(id);
    long formalFieldCount = productModelMapper.selectCount(
            new LambdaQueryWrapper<ProductModel>()
                    .eq(ProductModel::getProductId, id)
                    .eq(ProductModel::getDeleted, 0)
    );
    ProductContractReleaseBatch latestBatch = productContractReleaseBatchMapper.selectOne(
            new LambdaQueryWrapper<ProductContractReleaseBatch>()
                    .eq(ProductContractReleaseBatch::getProductId, id)
                    .eq(ProductContractReleaseBatch::getDeleted, 0)
                    .orderByDesc(ProductContractReleaseBatch::getId)
                    .last("limit 1")
    );
    ProductOverviewSummaryVO vo = new ProductOverviewSummaryVO();
    vo.setProductId(detail.getId());
    vo.setProductKey(detail.getProductKey());
    vo.setProductName(detail.getProductName());
    vo.setDeviceCount(detail.getDeviceCount());
    vo.setOnlineDeviceCount(detail.getOnlineDeviceCount());
    vo.setFormalFieldCount((int) formalFieldCount);
    if (latestBatch != null) {
        vo.setLatestReleaseBatchId(latestBatch.getId());
        vo.setLatestReleasedFieldCount(latestBatch.getReleasedFieldCount());
        vo.setLatestReleaseStatus(latestBatch.getReleaseStatus());
    }
    return vo;
}
```

- [ ] **Step 5: Re-run the backend tests and confirm pass**

```powershell
mvn -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductControllerTest,ProductServiceImplTest" test
```

- [ ] **Step 6: Commit**

```powershell
git add spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/vo/ProductOverviewSummaryVO.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/ProductService.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/service/impl/ProductServiceImpl.java spring-boot-iot-device/src/main/java/com/ghlzm/iot/device/controller/ProductController.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/controller/ProductControllerTest.java spring-boot-iot-device/src/test/java/com/ghlzm/iot/device/service/impl/ProductServiceImplTest.java
git commit -m "feat: add product overview summary endpoint"
```

### Task 2: Add route helpers and compatibility redirects for `/products/:productId/*`

**Files:**
- Create: `spring-boot-iot-ui/src/utils/productWorkbenchRoutes.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/utils/governanceTaskDispatch.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing route tests**

```ts
it('builds dedicated product detail routes', () => {
  expect(buildProductOverviewPath(1001)).toBe('/products/1001/overview')
  expect(buildProductContractsPath(1001)).toBe('/products/1001/contracts')
  expect(buildProductMappingRulesPath(1001)).toBe('/products/1001/mapping-rules')
  expect(buildProductReleasesPath(1001)).toBe('/products/1001/releases')
})

it('redirects legacy models workbench query to contracts route', async () => {
  mockRoute.path = '/products'
  mockRoute.query = { openProductId: '1001', workbenchView: 'models' }
  mountView()
  await flushPromises()
  expect(mockRouter.replace).toHaveBeenCalledWith('/products/1001/contracts')
})
```

- [ ] **Step 2: Run the frontend tests and confirm failure**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

- [ ] **Step 3: Add route helper module**

```ts
export const buildProductOverviewPath = (productId: string | number) => `/products/${productId}/overview`
export const buildProductDevicesPath = (productId: string | number) => `/products/${productId}/devices`
export const buildProductContractsPath = (productId: string | number) => `/products/${productId}/contracts`
export const buildProductMappingRulesPath = (productId: string | number) => `/products/${productId}/mapping-rules`
export const buildProductReleasesPath = (productId: string | number) => `/products/${productId}/releases`
```

- [ ] **Step 4: Register the new routes and add legacy query redirect logic**

```ts
{
  path: '/products/:productId/overview',
  name: 'product-overview',
  component: () => import('../views/ProductDetailWorkbenchView.vue'),
  meta: routeMeta('/products')
},
{
  path: '/products/:productId/devices',
  name: 'product-devices',
  component: () => import('../views/ProductDetailWorkbenchView.vue'),
  meta: routeMeta('/products')
},
{
  path: '/products/:productId/contracts',
  name: 'product-contracts',
  component: () => import('../views/ProductDetailWorkbenchView.vue'),
  meta: routeMeta('/products')
},
{
  path: '/products/:productId/mapping-rules',
  name: 'product-mapping-rules',
  component: () => import('../views/ProductDetailWorkbenchView.vue'),
  meta: routeMeta('/products')
},
{
  path: '/products/:productId/releases',
  name: 'product-releases',
  component: () => import('../views/ProductDetailWorkbenchView.vue'),
  meta: routeMeta('/products')
}
```

- [ ] **Step 5: Update deep links to use the new routes**

```ts
return {
  path: buildProductContractsPath(String(item.productId)),
  query: {
    governanceSource: 'task',
    workItemCode: item.workItemCode,
    ...governanceContextQuery(item)
  }
}
```

- [ ] **Step 6: Re-run the frontend tests and confirm pass**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

- [ ] **Step 7: Commit**

```powershell
git add spring-boot-iot-ui/src/utils/productWorkbenchRoutes.ts spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/utils/governanceTaskDispatch.ts spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
git commit -m "feat: add product detail routes and legacy redirects"
```

### Task 3: Create the product detail shell and overview / devices workspaces

**Files:**
- Create: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductDetailShell.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductOverviewWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/api/product.ts`
- Create: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing detail-shell test**

```ts
it('renders overview workspace for /products/:productId/overview', async () => {
  mockRoute.path = '/products/1001/overview'
  mockRoute.params = { productId: '1001' }
  mockGetProductById.mockResolvedValue({ code: 200, msg: 'success', data: { id: 1001, productName: '裂缝监测产品' } })
  mockGetProductOverviewSummary.mockResolvedValue({ code: 200, msg: 'success', data: { productId: 1001, formalFieldCount: 3 } })
  const wrapper = shallowMount(ProductDetailWorkbenchView, { global: { stubs } })
  await flushPromises()
  expect(wrapper.text()).toContain('裂缝监测产品')
  expect(wrapper.text()).toContain('formal-fields:3')
})
```

- [ ] **Step 2: Run the test and confirm failure**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

- [ ] **Step 3: Add API contract and detail shell**

```ts
export interface ProductOverviewSummary {
  productId?: IdType | null
  productKey?: string | null
  productName?: string | null
  deviceCount?: number | null
  onlineDeviceCount?: number | null
  formalFieldCount?: number | null
  latestReleaseBatchId?: IdType | null
}

getProductOverviewSummary(id: IdType) {
  return request<ProductOverviewSummary>(`/api/device/product/${id}/overview-summary`, { method: 'GET' })
}
```

```vue
<ProductDetailShell :product="productDetail" :active-key="activeWorkspace">
  <ProductOverviewWorkspace v-if="activeWorkspace === 'overview'" :summary="overviewSummary" />
  <ProductDeviceListWorkspace
    v-else-if="activeWorkspace === 'devices'"
    :devices="deviceRows"
    :loading="devicesLoading"
    :error-message="devicesErrorMessage"
    :empty="!devicesLoading && deviceRows.length === 0"
    :devices-loading="devicesLoading"
  />
</ProductDetailShell>
```

- [ ] **Step 4: Implement route-based loading**

```ts
const activeWorkspace = computed(() => route.path.endsWith('/devices') ? 'devices' : 'overview')

async function loadOverview(productId: string) {
  const [detailRes, summaryRes] = await Promise.all([
    productApi.getProductById(productId),
    productApi.getProductOverviewSummary(productId)
  ])
  productDetail.value = detailRes.data ?? null
  overviewSummary.value = summaryRes.data ?? null
}
```

- [ ] **Step 5: Re-run the test and confirm pass**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

- [ ] **Step 6: Commit**

```powershell
git add spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue spring-boot-iot-ui/src/components/product/ProductDetailShell.vue spring-boot-iot-ui/src/components/product/ProductOverviewWorkspace.vue spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue spring-boot-iot-ui/src/api/product.ts spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts
git commit -m "feat: add product detail shell overview and devices pages"
```

### Task 4: Split contracts, mapping-rules, and releases into dedicated workspaces

**Files:**
- Create: `spring-boot-iot-ui/src/components/product/ProductContractsWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductMappingRulesWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductReleasesWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductReleaseLedgerPanel.vue`
- Create: `spring-boot-iot-ui/src/components/product/ProductReleaseRollbackPreviewPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductReleaseLedgerPanel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductReleaseRollbackPreviewPanel.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductMappingRulesWorkspace.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/product/ProductReleasesWorkspace.test.ts`

- [ ] **Step 1: Write the failing page-boundary tests**

```ts
it('contracts workspace hides mapping rules and release ledger sections', () => {
  const wrapper = shallowMount(ProductContractsWorkspace, { props: baseProps, global: { stubs } })
  expect(wrapper.text()).toContain('当前已生效字段')
  expect(wrapper.text()).not.toContain('映射规则建议')
  expect(wrapper.text()).not.toContain('版本台账')
})

it('mapping rules workspace renders suggestion and ledger panels', () => {
  const wrapper = shallowMount(ProductMappingRulesWorkspace, { props: { productId: 1001 }, global: { stubs } })
  expect(wrapper.text()).toContain('映射规则建议')
  expect(wrapper.text()).toContain('映射规则台账')
})
```

- [ ] **Step 2: Run the component tests and confirm failure**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductReleaseLedgerPanel.test.ts src/__tests__/components/product/ProductReleaseRollbackPreviewPanel.test.ts src/__tests__/components/product/ProductMappingRulesWorkspace.test.ts src/__tests__/components/product/ProductReleasesWorkspace.test.ts
```

- [ ] **Step 3: Extract the contracts-only workspace**

```vue
<template>
  <section class="product-contracts-workspace">
    <ProductModelDesignerWorkspace :product="product" :contracts-only="true" />
  </section>
</template>
```

```ts
const showMappingRuleSections = computed(() => props.contractsOnly !== true)
const showReleaseSections = computed(() => props.contractsOnly !== true)
```

- [ ] **Step 4: Add mapping-rules and releases workspaces**

```vue
<template>
  <section class="product-mapping-rules-workspace">
    <ProductVendorMappingSuggestionPanel :product-id="productId" />
    <ProductVendorMappingRuleLedgerPanel :product-id="productId" />
  </section>
</template>
```

```vue
<template>
  <section class="product-releases-workspace">
    <h3>版本台账</h3>
    <ProductReleaseLedgerPanel :product-id="productId" />
    <ProductReleaseRollbackPreviewPanel :product-id="productId" />
  </section>
</template>
```

- [ ] **Step 5: Wire the new workspaces into `ProductDetailWorkbenchView`**

```vue
<ProductContractsWorkspace v-else-if="activeWorkspace === 'contracts'" :product="productDetail" />
<ProductMappingRulesWorkspace v-else-if="activeWorkspace === 'mapping-rules'" :product-id="resolvedProductId" />
<ProductReleasesWorkspace v-else-if="activeWorkspace === 'releases'" :product-id="resolvedProductId" />
```

- [ ] **Step 6: Re-run the component tests and confirm pass**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductReleaseLedgerPanel.test.ts src/__tests__/components/product/ProductReleaseRollbackPreviewPanel.test.ts src/__tests__/components/product/ProductMappingRulesWorkspace.test.ts src/__tests__/components/product/ProductReleasesWorkspace.test.ts
```

- [ ] **Step 7: Commit**

```powershell
git add spring-boot-iot-ui/src/components/product/ProductContractsWorkspace.vue spring-boot-iot-ui/src/components/product/ProductMappingRulesWorkspace.vue spring-boot-iot-ui/src/components/product/ProductReleasesWorkspace.vue spring-boot-iot-ui/src/components/product/ProductReleaseLedgerPanel.vue spring-boot-iot-ui/src/components/product/ProductReleaseRollbackPreviewPanel.vue spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue spring-boot-iot-ui/src/views/ProductDetailWorkbenchView.vue spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductReleaseLedgerPanel.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductReleaseRollbackPreviewPanel.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductMappingRulesWorkspace.test.ts spring-boot-iot-ui/src/__tests__/components/product/ProductReleasesWorkspace.test.ts
git commit -m "feat: split product contracts mapping rules and releases workspaces"
```

### Task 5: Convert `/products` into a list-first entry page and update docs/regression coverage

**Files:**
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/03-接口规范与接口清单.md`
- Modify: `docs/04-数据库设计与初始化数据.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write the failing list-page navigation test**

```ts
it('navigates to overview instead of opening the mixed drawer', async () => {
  mockPageProducts.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 1, pageNum: 1, pageSize: 10, records: [{ id: 1001, productName: '裂缝监测产品', productKey: 'nf-monitor-crack-v1', status: 1 }] }
  })
  const wrapper = mountView()
  await flushPromises()
  await wrapper.find('[data-testid="product-row-open-overview"]').trigger('click')
  expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/overview')
})
```

- [ ] **Step 2: Run the regression and confirm failure**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

- [ ] **Step 3: Replace drawer-first actions with route-first navigation**

```ts
function openProductOverview(productId: string | number) {
  return router.push(buildProductOverviewPath(productId))
}

function openProductContracts(productId: string | number) {
  return router.push(buildProductContractsPath(productId))
}
```

- [ ] **Step 4: Update docs for the new IA**

```markdown
- `/products` 当前只作为产品定义中心台账入口；进入具体产品后拆分为 `产品总览 / 关联设备 / 契约字段 / 映射规则 / 版本台账` 五个独立工作页。
- `契约字段` 当前只保留样本输入、识别结果、本次生效和当前已生效字段；映射规则与版本台账已迁出。
- 旧 `openProductId + workbenchView` 路径仍保留一轮兼容跳转，但不再作为正式入口。
```

- [ ] **Step 5: Run focused frontend/backend regressions**

```powershell
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/views/ProductDetailWorkbenchView.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductReleaseLedgerPanel.test.ts src/__tests__/components/product/ProductReleaseRollbackPreviewPanel.test.ts src/__tests__/components/product/ProductMappingRulesWorkspace.test.ts src/__tests__/components/product/ProductReleasesWorkspace.test.ts
mvn -pl spring-boot-iot-device -DskipTests=false "-Dsurefire.failIfNoSpecifiedTests=false" "-Dtest=ProductControllerTest,ProductServiceImplTest,ProductContractReleaseControllerTest,ProductModelControllerTest,VendorMetricMappingRuleControllerTest" test
```

- [ ] **Step 6: Commit**

```powershell
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts README.md docs/02-业务功能与流程说明.md docs/03-接口规范与接口清单.md docs/04-数据库设计与初始化数据.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md docs/21-业务功能清单与验收标准.md
git commit -m "feat: split products workbench into dedicated pages"
```
