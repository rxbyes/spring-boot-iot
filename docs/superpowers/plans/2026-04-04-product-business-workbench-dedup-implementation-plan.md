# Product Business Workbench Dedup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove repeated scale, contract, archive, and governance copy from the `/products` business workbench while keeping the existing four-view drawer structure intact.

**Architecture:** Keep `ProductWorkbenchView.vue` as the orchestration layer and only trim the existing workbench subcomponents: the drawer header owns the contract sentence and single total-device metric, the overview owns the compact activity/archive row, the device workspace keeps only the registry table, and the edit/model workspaces keep only necessary guidance. All behavior stays on the current APIs and current component boundaries.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing shared workbench components.

---

## File Structure

- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
  Responsibility: remove long running-period copy and collapse header contract information into one sentence.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
  Responsibility: remove duplicated scale/contract presentation and keep activity/archive information in one-line rows.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
  Responsibility: delete the repeated summary ruler so the device registry becomes the only main body.
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
  Responsibility: delete the repeated revision ruler and keep only concise editing guidance plus the form.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: shorten repeated governance wording without changing behavior.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
  Responsibility: lock the new deduplicated copy contract.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: document the reduced product-workbench information contract.

### Task 1: Write Red Tests For The Deduplicated Information Contract

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Add the failing header-copy assertions**

Update `ProductBusinessWorkbenchDrawer.test.ts` to assert the header now uses one contract sentence and no long running-period judgement:

```ts
expect(wrapper.find('.product-business-workbench__meta-line').text()).toContain(
  '产品key：demo-product｜接入协议：mqtt-json｜节点类型：直连设备｜数据格式：JSON'
)
expect(wrapper.text()).not.toContain('已进入运行期')
expect(wrapper.text()).not.toContain('围绕规模、契约与档案校准')
```

- [ ] **Step 2: Add the failing overview dedup assertions**

Update `ProductDetailWorkbench.test.ts` to prove overview no longer repeats the contract split or the core-scale block:

```ts
expect(wrapper.text()).not.toContain('接入协议')
expect(wrapper.text()).not.toContain('节点类型')
expect(wrapper.text()).not.toContain('数据格式')
expect(wrapper.text()).toContain('厂商')
expect(wrapper.text()).toContain('最近更新')
expect(wrapper.text()).toContain('补充说明')
expect(wrapper.findAll('.product-detail-workbench__archive-note')).toHaveLength(3)
```

Also keep the activity row locked to three items:

```ts
expect(wrapper.findAll('.product-detail-workbench__scale-note')).toHaveLength(3)
```

- [ ] **Step 3: Add the failing device/edit/model assertions**

Update the three workspace tests to assert the repeated rulers are gone and the model-governance wording is shorter:

```ts
expect(wrapper.find('.device-workspace__ledger-ruler').exists()).toBe(false)
expect(wrapper.text()).not.toContain('设备总数')
expect(wrapper.text()).not.toContain('在线比例')
```

```ts
expect(wrapper.find('.product-edit-workspace__revision-ruler').exists()).toBe(false)
expect(wrapper.text()).not.toContain('节点与状态')
```

```ts
expect(wrapper.text()).not.toContain('样本 JSON 继续保留为辅助核对工具')
expect(wrapper.text()).toContain('样本 JSON 仅作辅助核对')
```

- [ ] **Step 4: Run the focused Vitest suite and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected: FAIL because the header copy, overview contract block, device ruler, edit ruler, and model wording are still present.

### Task 2: Implement The Minimal UI Dedup Changes

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: Collapse the header contract line**

In `ProductBusinessWorkbenchDrawer.vue`, replace the `metaItems` list with one computed sentence:

```ts
const contractStatement = computed(() => {
  return `产品key：${productKeyText.value}｜接入协议：${protocolText.value}｜节点类型：${nodeTypeText.value}｜数据格式：${dataFormatText.value}`
})
```

Render it as a single `meta-line` string and replace the long status statement with shorter copy such as:

```ts
if (props.product.status === 0) return '当前产品处于停用状态，请先核对契约与档案。'
if (totalDevices <= 0) return '当前已完成建档，等待首批设备接入。'
if (onlineCoverage > 0) return '当前已形成运行样本，继续核对在线覆盖。'
return '当前已有关联设备，待补齐在线基线。'
```

- [ ] **Step 2: Remove overview contract duplication and keep compact rows**

In `ProductDetailWorkbench.vue`, delete the `contract-sheet` section and keep:

```vue
<section class="product-detail-workbench__lead-sheet">
  <section class="product-detail-workbench__scale-ledger">...</section>
</section>
<section class="product-detail-workbench__archive-sheet">...</section>
```

Keep the three activity metrics on one row and render the archive items in a single three-column row.

- [ ] **Step 3: Delete repeated device and edit rulers**

In `ProductDeviceListWorkspace.vue`, remove `summaryMetrics`, `onlineRatioText`, and the entire `device-workspace__ledger-ruler` block so the component only renders state messages plus `设备清册`.

In `ProductEditWorkspace.vue`, delete `nodeTypeText`, `statusText`, and the entire `product-edit-workspace__revision-ruler` section so the page keeps only concise intro copy, inline state, form, and footer.

- [ ] **Step 4: Shorten model-governance wording**

In `ProductModelDesignerWorkspace.vue`, keep the same flow but shorten repeated copy, for example:

```ts
return '先按规范字段发起 compare，再用报文证据核对稳定上报。'
return '当前产品暂无适用规范预设，先按通用双证据治理当前契约。'
```

And shorten the runtime step to:

```ts
description: '运行期证据会并列拉取属性快照、消息日志与命令记录，样本 JSON 仅作辅助核对。'
```

- [ ] **Step 5: Run the focused Vitest suite and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected: PASS.

### Task 3: Update The View-Level Contract, Docs, And Final Verification

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Lock the integrated `/products` contract**

Add a view test that proves the unified workbench still opens, but the duplicate summary wording does not leak through:

```ts
expect(wrapper.text()).toContain('演示产品')
expect(wrapper.text()).toContain('demo-product')
expect(wrapper.text()).not.toContain('已进入运行期')
```

- [ ] **Step 2: Update docs in place**

Record that:

- `/products` workbench header now owns the single contract sentence
- overview keeps only compact activity and archive rows
- device and edit workspaces no longer repeat summary rulers
- model-governance copy is intentionally concise

- [ ] **Step 3: Run the integrated verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: PASS.

Then run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly scoped existing unrelated failure.
