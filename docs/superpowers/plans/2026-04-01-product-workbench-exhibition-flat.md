# Product Workbench Exhibition Flat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current `/products` business workbench visual language with a new 2026 exhibition-style flat design while keeping the existing four-view information architecture intact.

**Architecture:** Keep the same runtime structure (`ProductBusinessWorkbenchDrawer` + four embedded workspaces) and rewrite only the presentation grammar. The new grammar uses editorial typography, exhibition-style section framing, thinner navigation, fewer card surfaces, and stronger whitespace hierarchy instead of briefing-style symmetric cards.

**Tech Stack:** Vue 3, TypeScript, scoped CSS, Vitest, Vite, existing `Standard*` shared components.

---

### Task 1: Lock the new visual contract in tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductEditWorkspace.test.ts`

- [ ] **Step 1: Write failing tests for the new exhibition-style selectors**

```ts
expect(wrapper.find('.product-business-workbench__exhibit-head').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__identity-frame').exists()).toBe(false)
expect(wrapper.find('.product-detail-workbench__hero-plinth').exists()).toBe(true)
expect(wrapper.find('.product-model-designer__curation-board').exists()).toBe(true)
expect(wrapper.find('.device-workspace__ledger-marquee').exists()).toBe(true)
expect(wrapper.find('.product-edit-workspace__revision-board').exists()).toBe(true)
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected: FAIL because the old briefing selectors still exist and the new exhibition selectors do not.

- [ ] **Step 3: Keep assertions focused on structure, not literal CSS values**

```ts
expect(wrapper.text()).toContain('经营总览')
expect(wrapper.text()).not.toContain('当前判断')
expect(wrapper.findAll('.product-model-designer__curation-item')).toHaveLength(3)
```

- [ ] **Step 4: Re-run tests after updates**

Run the same Vitest command and confirm the failures describe missing exhibition structure instead of unrelated syntax/runtime errors.


### Task 2: Replace the drawer header and overview with exhibition-flat composition

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`

- [ ] **Step 1: Implement the new exhibition header shell**

```vue
<section class="product-business-workbench__header">
  <div class="product-business-workbench__exhibit-head">
    <div class="product-business-workbench__exhibit-title-block">
      <p class="product-business-workbench__eyebrow">Product Exhibition</p>
      <h3 class="product-business-workbench__headline">{{ productHeadline }}</h3>
      <p class="product-business-workbench__status-statement">{{ statusStatement }}</p>
    </div>
    <nav class="product-business-workbench__tab-index">...</nav>
  </div>
</section>
```

- [ ] **Step 2: Convert the overview into a single editorial plinth + exhibit sheet**

```vue
<section class="product-detail-workbench__hero-plinth">
  <div class="product-detail-workbench__hero-copy">...</div>
  <div class="product-detail-workbench__metric-ribbon">...</div>
</section>
<section class="product-detail-workbench__exhibit-sheet">
  <section class="product-detail-workbench__contract-ledger">...</section>
  <section class="product-detail-workbench__archive-notes">...</section>
</section>
```

- [ ] **Step 3: Rewrite the scoped CSS away from briefing symmetry**

```css
.product-business-workbench__exhibit-head {
  display: grid;
  gap: 1.5rem;
  padding: 2rem 2.2rem 1.4rem;
}

.product-detail-workbench__hero-plinth {
  display: grid;
  gap: 1.2rem;
  padding: 1.8rem;
}
```

- [ ] **Step 4: Run focused tests**

Run:

```powershell
npm run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected: PASS.


### Task 3: Recompose the three sub-workspaces into exhibition boards

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductEditWorkspace.vue`

- [ ] **Step 1: Turn model governance into a curation board**

```vue
<section class="product-model-designer__curation-board">
  <div class="product-model-designer__curation-strip">...</div>
  <ol class="product-model-designer__curation-flow">...</ol>
</section>
```

- [ ] **Step 2: Turn the device view into a ledger marquee + table hall**

```vue
<section class="device-workspace__ledger-marquee">
  <ul class="device-workspace__marquee-metrics">...</ul>
</section>
<section class="device-workspace__table-hall">...</section>
```

- [ ] **Step 3: Turn edit view into a revision board**

```vue
<section class="product-edit-workspace__revision-board">
  <div class="product-edit-workspace__revision-strip">...</div>
</section>
```

- [ ] **Step 4: Run focused tests**

Run:

```powershell
npm run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected: PASS.


### Task 4: Reconcile page-level regression, docs, and guards

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Test: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`

- [ ] **Step 1: Update the documentation to replace “政企简报式” references**

```md
- 产品经营工作台当前固定采用“2026 展陈型高级扁平”语法：展签式头部、目录式页签、主展板与策展式正文，不再回流简报卡墙。
```

- [ ] **Step 2: Run page-level regression**

Run:

```powershell
npm run test -- src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductEditWorkspace.test.ts --run
```

Expected: PASS with all product-workbench tests green.

- [ ] **Step 3: Run guards and build**

Run:

```powershell
npm run component:guard
npm run list:guard
npm run style:guard
npm run build
```

Expected: all commands exit `0`.
