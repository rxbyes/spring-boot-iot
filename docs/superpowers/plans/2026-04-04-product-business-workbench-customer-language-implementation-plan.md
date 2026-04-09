# Product Business Workbench Customer Language Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the `/products` unified business workbench into a customer-facing information architecture with three readable tabs and a header-level edit action.

**Architecture:** Keep `ProductWorkbenchView.vue` as the single orchestration layer and preserve the existing unified drawer/context. Only relabel the visible information architecture, move the edit entry from the tab strip to the header action area, and keep the existing edit workspace and contract-governance workspace mounted inside the same drawer.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing shared workbench components.

---

## File Structure

- Add: `docs/superpowers/specs/2026-04-04-product-business-workbench-customer-language-design.md`
  Responsibility: record the approved customer-facing IA.
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
  Responsibility: expose the three customer-facing tab labels and keep edit as a hidden internal workspace reachable through header actions.
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  Responsibility: wire the new `编辑档案` header action into the existing edit workspace while preserving unified drawer behavior.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: rename internal mode wording to customer-facing contract language.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
  Responsibility: lock the new customer-facing IA through red/green tests.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: update the product workbench structure and governance copy rules in place.

### Task 1: Write Red Tests For The Customer-Facing IA

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Add failing tab-label and header-action assertions**

Update `ProductBusinessWorkbenchDrawer.test.ts` so it expects:

```ts
expect(wrapper.text()).toContain('产品总览')
expect(wrapper.text()).toContain('关联设备')
expect(wrapper.text()).toContain('契约字段')
expect(wrapper.text()).not.toContain('经营总览')
expect(wrapper.text()).not.toContain('物模型治理')
expect(wrapper.text()).not.toContain('编辑治理')
expect(wrapper.find('.product-business-workbench__header-action-slot').text()).toContain('编辑档案')
```

- [ ] **Step 2: Add failing designer-wording assertions**

Update `ProductModelDesignerWorkspace.test.ts` so the embedded workspace expects:

```ts
expect(wrapper.text()).toContain('证据比对')
expect(wrapper.text()).toContain('正式字段')
expect(wrapper.text()).not.toContain('手动提炼')
expect(wrapper.text()).not.toContain('正式模型')
```

- [ ] **Step 3: Add failing view-level orchestration assertions**

Update `ProductWorkbenchView.test.ts` so the workbench stub renders `header-actions` and the view test proves:

```ts
expect(wrapper.text()).toContain('编辑档案')
expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('overview')
```

Then trigger the new header action and expect:

```ts
await wrapper.get('[data-testid="open-product-workbench-edit"]').trigger('click')
expect(wrapper.get('[data-testid="product-business-workbench-active-view"]').text()).toBe('edit')
expect(wrapper.find('.product-edit-workspace-stub').exists()).toBe(true)
```

- [ ] **Step 4: Run the focused Vitest suite and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: FAIL because the old labels and the tab-based edit entry are still present.

### Task 2: Implement The Minimal Customer-Language UI Changes

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`

- [ ] **Step 1: Reduce the visible tab strip to three customer tabs**

In `ProductBusinessWorkbenchDrawer.vue`, keep the internal view union intact but change the visible `viewOptions` to:

```ts
const viewOptions: Array<{ key: Exclude<ProductBusinessWorkbenchView, 'edit'>; label: string }> = [
  { key: 'overview', label: '产品总览' },
  { key: 'devices', label: '关联设备' },
  { key: 'models', label: '契约字段' }
]
```

Keep the `edit` slot mounted below for internal use, but do not expose it in the tab strip.

- [ ] **Step 2: Wire the header edit action into the unified workbench**

In `ProductWorkbenchView.vue`, pass a header action slot:

```vue
<template #header-actions>
  <StandardButton data-testid="open-product-workbench-edit" @click="handleBusinessWorkbenchEdit">
    编辑档案
  </StandardButton>
</template>
```

Add a helper that keeps the existing edit workspace behavior:

```ts
function handleBusinessWorkbenchEdit() {
  activeEditSessionId += 1
  abortEditRequest()
  editingProductId.value = businessWorkbenchProduct.value?.id ?? currentProduct.value?.id ?? null
  formDirtySinceOpen = false
  clearFormRefreshState()
  applyFormDataWithoutDirty(businessWorkbenchProduct.value || currentProduct.value || undefined)
  handleBusinessWorkbenchViewChange('edit')
  editWorkspaceRef.value?.clearValidate()
}
```

- [ ] **Step 3: Rename the designer mode wording**

In `ProductModelDesignerWorkspace.vue`, rename:

```vue
证据比对
正式字段
```

And update supporting copy so the mode summary becomes:

```ts
value: designerMode.value === 'manual' ? '证据比对' : '正式字段'
```

- [ ] **Step 4: Run the focused Vitest suite and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: PASS.

### Task 3: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update product-workbench behavior docs in place**

Record that:

- the unified workbench now shows `产品总览 / 关联设备 / 契约字段`
- `编辑档案` is now a header action instead of a visible tab
- contract governance wording is intentionally customer-facing

- [ ] **Step 2: Run integrated verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/views/ProductWorkbenchView.test.ts --run
```

Expected: PASS.

Then run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly scoped unrelated existing failure.
