# IoT Access / Product Center IA Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-align `接入智维` so `/device-onboarding` acts as an orchestration page, `/protocol-governance` stays a shared governance page, and all product-context execution remains under `/products/:productId/*` with friendly fallbacks.

**Architecture:** Keep the current route tree and menu seed structure intact, but tighten page responsibilities. The implementation stays front-end heavy: clarify access-hub/menu copy, refine onboarding row actions into jump-only orchestration, and replace raw product-context failures with guided return-to-products behavior. Documentation and seed captions are updated last so the shipped UX contract, acceptance docs, and initialization data all describe the same boundary.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Vitest, SQL seed data, Markdown docs

---

## File Map

- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
  - Refresh `接入智维` card copy so `无代码接入台` is described as orchestration and `/products` is described as the product-context execution entry.
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  - Adjust page-level description and empty-state/help copy so the list page clearly reads as “产品上下文执行入口”.
- Modify: `spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue`
  - Keep the page as a case/template/orchestration surface; add product-summary jump actions and refine next-step labels/targets without embedding product-governance bodies.
- Modify: `spring-boot-iot-ui/src/composables/useProductDetailWorkbench.ts`
  - Replace raw missing-product messages with explicit “return to 产品定义中心” guidance.
- Modify: `sql/init-data.sql`
  - Update menu captions only; do not add new deep-link menu nodes.
- Modify: `docs/02-业务功能与流程说明.md`
  - Sync official page-role wording with the new three-layer boundary.
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
  - Record the IA boundary reset as a front-end/acceptance baseline refinement.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Update acceptance points for `/device-onboarding` and `/products`.
- Test: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`

### Task 1: Tighten access-hub copy and menu captions

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `sql/init-data.sql`
- Test: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing tests for the new IA wording**

```ts
it('exposes updated iot access hub overview copy', () => {
  const config = getSectionHomeConfigByPath('/device-access');
  expect(config?.cards.find((item) => item.path === '/device-onboarding')?.description)
    .toBe('创建接入案例、查看阻塞原因，并跳到协议治理或产品工作台继续处理。');
  expect(config?.cards.find((item) => item.path === '/products')?.description)
    .toBe('维护产品定义，并作为进入产品工作台的统一入口承接契约、映射和版本治理。');
  expect(config?.cards.find((item) => item.path === '/protocol-governance')?.description)
    .toBe('维护跨产品复用的协议族、解密档案与协议模板。');
});
```

```ts
it('renders product center as the product workbench entry', async () => {
  const wrapper = mountView();
  await flushPromises();

  expect(wrapper.text()).toContain('产品定义中心');
  expect(wrapper.text()).toContain('统一维护产品定义，并作为进入产品工作台的统一入口承接契约、映射与版本治理。');
});
```

- [ ] **Step 2: Run the focused tests to verify they fail on the old copy**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: FAIL because the current strings still say `维护产品定义，并承接契约治理、版本治理与风险目录入口。` and the onboarding card still reads as a generic case list.

- [ ] **Step 3: Update the hub copy, product page copy, and seed captions**

```ts
// spring-boot-iot-ui/src/utils/sectionWorkspaces.ts
cards: [
  {
    path: '/device-onboarding',
    label: '无代码接入台',
    description: '创建接入案例、查看阻塞原因，并跳到协议治理或产品工作台继续处理。',
    short: '接',
    keywords: ['无代码接入台', '零代码接入', '设备接入案例', '接入编排']
  },
  {
    path: '/products',
    label: '产品定义中心',
    description: '维护产品定义，并作为进入产品工作台的统一入口承接契约、映射和版本治理。',
    short: '产',
    keywords: ['产品定义中心', '产品工作台', '产品建档']
  },
  {
    path: '/protocol-governance',
    label: '协议治理工作台',
    description: '维护跨产品复用的协议族、解密档案与协议模板。',
    short: '协',
    keywords: ['协议治理工作台', '协议族定义', '解密档案', '协议治理']
  }
]
```

```vue
<!-- spring-boot-iot-ui/src/views/ProductWorkbenchView.vue -->
<StandardWorkbenchPanel
  title="产品定义中心"
  description="统一维护产品定义，并作为进入产品工作台的统一入口承接契约、映射与版本治理。"
  show-filters
  :show-applied-filters="hasAppliedFilters"
  show-notices
  show-toolbar
  :show-inline-state="showListInlineState"
  show-pagination
>
```

```ts
// spring-boot-iot-ui/src/views/ProductWorkbenchView.vue
const emptyStateDescription = computed(() =>
  hasAppliedFilters.value
    ? '已生效筛选暂时没有匹配结果，可以调整条件，或者直接清空当前筛选。'
    : '当前还没有产品定义，先新增产品，再从这里进入产品工作台继续契约、映射和版本治理。'
)
```

```sql
-- sql/init-data.sql
(93001008, ..., '{"caption":"创建接入案例、查看阻塞原因，并跳到协议治理或产品工作台继续处理"}', ...),
(93001009, ..., '{"caption":"跨产品复用的协议族、解密档案与协议模板治理"}', ...),
(93001001, ..., '{"caption":"产品定义与产品工作台统一入口"}', ...)
```

- [ ] **Step 4: Re-run the copy tests**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts sql/init-data.sql
git commit -m "feat: align iot access entry copy with product workbench boundary"
```

### Task 2: Turn onboarding case cards into orchestration-only cards

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing test for step-specific routing and product-workbench jumps**

```ts
it('routes bound cases to contracts and exposes a direct product workbench jump', async () => {
  const wrapper = mountView();
  await flushPromises();

  await wrapper.get('[data-testid="onboarding-open-product-9201"]').trigger('click');
  await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click');

  expect(mockRouter.push).toHaveBeenNthCalledWith(1, '/products/1001/overview');
  expect(mockRouter.push).toHaveBeenNthCalledWith(2, '/products');
  expect(wrapper.text()).toContain('进入产品工作台');
  expect(wrapper.text()).toContain('前往产品列表');
});
```

```ts
it('routes contract-release rows into product contracts', async () => {
  mockRefreshDeviceOnboardingCaseStatus.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      id: 9101,
      caseCode: 'CASE-9101',
      caseName: '裂缝传感器接入',
      currentStep: 'CONTRACT_RELEASE',
      status: 'IN_PROGRESS',
      productId: 1001,
      blockers: ['待发布正式合同批次']
    }
  });

  const wrapper = mountView();
  await flushPromises();

  await wrapper.get('[data-testid="onboarding-refresh-9101"]').trigger('click');
  await flushPromises();
  await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click');

  expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/contracts');
});
```

- [ ] **Step 2: Run the onboarding view test to verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

Expected: FAIL because the template does not render `onboarding-open-product-*` actions and `nextActionLabel()` still returns the generic `前往产品治理`.

- [ ] **Step 3: Refine the onboarding template and routing helpers**

```vue
<!-- spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue -->
<IotAccessPageShell
  title="无代码接入台"
  description="统一查看接入案例、当前步骤、阻塞原因和下一步动作；产品相关执行统一跳到产品工作台完成。"
>
```

```vue
<!-- spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue -->
<div class="device-onboarding-workbench__actions">
  <StandardButton
    v-if="row.productId != null"
    :data-testid="`onboarding-open-product-${row.id}`"
    @click="handleOpenProductWorkbench(row)"
  >
    进入产品工作台
  </StandardButton>
  <StandardButton v-permission="'iot:device-onboarding:update-case'" @click="handleEdit(row)">编辑</StandardButton>
  <StandardButton
    v-permission="'iot:device-onboarding:refresh-status'"
    :data-testid="`onboarding-refresh-${row.id}`"
    :disabled="refreshingId === String(row.id)"
    @click="handleRefreshRow(row)"
  >
    {{ refreshingId === String(row.id) ? '刷新中...' : '刷新状态' }}
  </StandardButton>
  <StandardButton
    :data-testid="`onboarding-next-${row.id}`"
    :disabled="row.currentStep === 'ACCEPTANCE'"
    @click="handleNext(row)"
  >
    {{ nextActionLabel(row) }}
  </StandardButton>
</div>
```

```ts
// spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue
function handleOpenProductWorkbench(row: DeviceOnboardingCase): void {
  if (row.productId == null) {
    return
  }
  void router.push(buildProductWorkbenchSectionPath(row.productId, 'overview'))
}

function nextActionLabel(row: DeviceOnboardingCase): string {
  if (row.currentStep === 'PROTOCOL_GOVERNANCE') {
    return '前往协议治理'
  }
  if (row.currentStep === 'PRODUCT_GOVERNANCE') {
    return row.productId == null ? '前往产品列表' : '前往契约字段'
  }
  if (row.currentStep === 'CONTRACT_RELEASE') {
    return '前往契约字段'
  }
  return '已具备验收条件'
}
```

- [ ] **Step 4: Re-run the onboarding tests**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
git commit -m "feat: refocus onboarding workbench on orchestration jumps"
```

### Task 3: Replace raw product-context failures with guided return copy

**Files:**
- Modify: `spring-boot-iot-ui/src/composables/useProductDetailWorkbench.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing tests for missing and invalid product context**

```ts
it('shows a guided message when the route has no productId', async () => {
  mockRouteState.path = '/products//overview';
  mockRouteState.name = 'product-overview';
  mockRouteState.params = {};

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('当前链接缺少有效产品上下文，请返回产品定义中心重新选择产品。');
});
```

```ts
it('shows a guided message when product detail loading fails', async () => {
  mockGetProductById.mockRejectedValueOnce(new Error('404'));

  const wrapper = mountView();
  await flushPromises();
  await nextTick();

  expect(wrapper.text()).toContain('未找到可用的产品上下文，请返回产品定义中心重新选择产品。');
});
```

- [ ] **Step 2: Run the product detail tests to verify they fail**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: FAIL because the composable still returns `产品编号缺失，无法打开工作区。` and `加载产品工作区失败`.

- [ ] **Step 3: Implement explicit product-context guidance in the composable**

```ts
// spring-boot-iot-ui/src/composables/useProductDetailWorkbench.ts
const missingProductContextMessage = '当前链接缺少有效产品上下文，请返回产品定义中心重新选择产品。'
const invalidProductContextMessage = '未找到可用的产品上下文，请返回产品定义中心重新选择产品。'

async function loadProductContext() {
  const requestId = ++latestProductRequestId

  if (!productId.value) {
    product.value = null
    overviewSummary.value = null
    clearDeviceState()
    errorMessage.value = missingProductContextMessage
    return false
  }

  loading.value = true
  errorMessage.value = ''
  try {
    // existing Promise.allSettled(...)
  } catch (error) {
    if (requestId !== latestProductRequestId) {
      return false
    }
    product.value = null
    overviewSummary.value = null
    clearDeviceState()
    errorMessage.value = invalidProductContextMessage
    return false
  } finally {
    if (requestId === latestProductRequestId) {
      loading.value = false
    }
  }
}
```

- [ ] **Step 4: Re-run the product detail tests**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add spring-boot-iot-ui/src/composables/useProductDetailWorkbench.ts spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts
git commit -m "fix: guide users back to products on missing product context"
```

### Task 4: Sync the official docs and acceptance language

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/19-第四阶段交付边界与复验进展.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the failing coverage note in the implementation log**

```md
- `接入智维` 中的 `无代码接入台 / 产品定义中心 / 协议治理工作台` 已明确按“编排 / 产品上下文执行 / 共享治理”分层；不再把 `contracts / mapping-rules / releases` 作为无产品上下文一级执行页理解。
```

- [ ] **Step 2: Update the business-flow and acceptance docs**

```md
<!-- docs/02-业务功能与流程说明.md -->
- `/device-onboarding` 固定为接入编排页：统一查看接入案例、当前步骤、阻塞原因与下一步动作；产品相关执行统一跳转 `/products` 或 `/products/:productId/*`，不再在该页直接承载产品契约、映射或版本治理主体。
- `/products` 固定为产品定义主入口 + 进入产品工作台的统一入口；`overview / devices / contracts / mapping-rules / releases` 继续作为唯一正式产品执行页。
```

```md
<!-- docs/21-业务功能清单与验收标准.md -->
7. 点击“下一步”时，协议治理卡点是否跳转 `/protocol-governance`，未绑定产品时是否跳转 `/products`，已绑定产品但未发布合同时是否跳转 `/products/:productId/contracts`。
8. 若案例已绑定产品，页面是否只展示产品摘要与“进入产品工作台”动作，而不在无代码接入台内部嵌入产品治理主体组件。
```

```md
<!-- docs/19-第四阶段交付边界与复验进展.md -->
| 2026-04-25 | 接入智维与产品定义中心 IA 边界收口：无代码接入台固定为编排页，产品相关执行统一回到 `/products/:productId/*`，协议治理继续保持共享治理独立页 | UI / SQL / docs | Codex | 消除无产品上下文的产品执行页误用 |
```

- [ ] **Step 3: Review the docs locally**

Run:

```bash
rg -n "编排页|产品工作台|共享治理|无产品上下文" docs/02-业务功能与流程说明.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md
```

Expected: each document contains the new boundary wording, and no old sentence still implies that onboarding itself hosts product execution.

- [ ] **Step 4: Commit**

```bash
git add docs/02-业务功能与流程说明.md docs/19-第四阶段交付边界与复验进展.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: clarify iot access and product workbench boundaries"
```

### Task 5: Run final verification and prepare integration

**Files:**
- Modify: none
- Test: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductDetailWorkbenchView.test.ts`
- Test: `sql/init-data.sql` via audit script

- [ ] **Step 1: Run the focused front-end verification suite**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run \
  src/__tests__/utils/sectionHomes.test.ts \
  src/__tests__/views/ProductWorkbenchView.test.ts \
  src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts \
  src/__tests__/views/ProductDetailWorkbenchView.test.ts
```

Expected: PASS across all four files.

- [ ] **Step 2: Re-audit the menu seed after caption changes**

Run:

```bash
node scripts/audit-menu-permission-seed.mjs
```

Expected: no missing menu nodes, no missing permissions, no failures.

- [ ] **Step 3: Check for whitespace / patch hygiene**

Run:

```bash
git diff --check
```

Expected: no output.

- [ ] **Step 4: Confirm the worktree only contains task-related changes**

```bash
git status --short
```

Expected: clean worktree or only task-related files; do not stage the external `scripts/__pycache__/*.pyc` changes.

## Self-Review

1. **Spec coverage:**  
   - `无代码接入台` 只做编排与跳转: Task 2  
   - `产品定义中心` 继续作为产品上下文执行入口: Tasks 1 and 4  
   - `协议治理` 保持共享治理独立: Tasks 1 and 4  
   - 产品上下文缺失时友好回跳: Task 3  
   - 菜单 seed / docs / acceptance 同步: Tasks 1, 4, and 5

2. **Placeholder scan:**  
   - No placeholder markers remain.  
   - Every code-changing step includes a concrete snippet and exact test command.

3. **Type consistency:**  
   - `buildProductWorkbenchSectionPath(...)` is the only product-workbench route helper introduced in onboarding changes.  
   - `nextActionLabel()` and `handleNext()` stay aligned on `PROTOCOL_GOVERNANCE / PRODUCT_GOVERNANCE / CONTRACT_RELEASE / ACCEPTANCE`.
