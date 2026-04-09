# Product Workbench Compact Overview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten the `/products` unified `产品经营工作台` into a compact, customer-facing drawer with a two-card header, card-based overview, lighter contract workspace, and simpler associated-device wording.

**Architecture:** Keep `ProductWorkbenchView.vue` as the single orchestration layer and preserve the existing unified drawer plus three visible workbench tabs. Concentrate the implementation inside the existing product drawer subcomponents so the route structure, API calls, and backend contract stay unchanged while the visual rhythm becomes closer to the device asset detail page.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing shared workbench components and design tokens.

---

## File Structure

- Add: `docs/superpowers/plans/2026-04-05-product-workbench-compact-overview-implementation-plan.md`
  Responsibility: record the approved execution path for the compact workbench refresh.
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
  Responsibility: collapse the workbench header into equal-height `产品信息卡 + 关联设备卡`, remove `契约状态 / 下一步建议`, and replace asymmetric divider rails with a compact identity row and lighter tabs.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
  Responsibility: convert the overview into three metric cards plus one archive summary card with adaptive manufacturer/update fields and single-line description ellipsis.
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
  Responsibility: rename `设备清册` to `设备清单` and align the device tab container with the lighter card language.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: remove the heavy numbered introduction, reduce extra borders, and keep the contract workspace compact and customer-readable.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
  Responsibility: lock the new header, overview cards, device wording, and compact contract workspace in red/green tests.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
  Responsibility: update the `/products` customer-facing workbench structure, wording, and acceptance expectations in place.

### Task 1: Write Red Tests For The Compact Header And Overview

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`

- [ ] **Step 1: Add failing assertions for the new two-card header**

Update `ProductBusinessWorkbenchDrawer.test.ts` so the primary scenario expects the compact header shape:

```ts
expect(wrapper.find('.product-business-workbench__journal-masthead').exists()).toBe(false)
expect(wrapper.find('.product-business-workbench__masthead-grid').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__info-card').exists()).toBe(true)
expect(wrapper.find('.product-business-workbench__relation-card').exists()).toBe(true)
expect(wrapper.findAll('.product-business-workbench__scale-panel')).toHaveLength(0)
expect(wrapper.text()).toContain('关联设备')
expect(wrapper.text()).toContain('12 台')
expect(wrapper.text()).not.toContain('契约状态')
expect(wrapper.text()).not.toContain('下一步建议')
expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('产品Key')
expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('接入协议')
```

- [ ] **Step 2: Add failing assertions for the card-based overview**

Update `ProductDetailWorkbench.test.ts` so it expects card containers instead of line-based ledgers:

```ts
expect(wrapper.find('[data-testid="product-detail-scale-ledger"]').exists()).toBe(false)
expect(wrapper.find('.product-detail-workbench__metric-grid').exists()).toBe(true)
expect(wrapper.findAll('.product-detail-workbench__metric-card')).toHaveLength(3)
expect(wrapper.find('.product-detail-workbench__archive-card').exists()).toBe(true)
expect(wrapper.find('.product-detail-workbench__archive-meta').exists()).toBe(true)
expect(wrapper.find('.product-detail-workbench__archive-description').exists()).toBe(true)
expect(wrapper.text()).toContain('在线覆盖')
expect(wrapper.text()).toContain('30 日活跃')
expect(wrapper.text()).toContain('平均在线')
expect(wrapper.text()).toContain('档案摘要')
```

- [ ] **Step 3: Run the focused component tests and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts --run
```

Expected: FAIL because the old multi-panel header and line-based overview are still present.

### Task 2: Implement The Compact Header In The Unified Workbench Drawer

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`

- [ ] **Step 1: Replace the three-panel scale grid with equal-height info and relation cards**

In `ProductBusinessWorkbenchDrawer.vue`, replace the current masthead block with:

```vue
<div class="product-business-workbench__masthead-grid">
  <article class="product-business-workbench__info-card">
    <span class="product-business-workbench__journal-kicker">产品经营页</span>
    <h3 class="product-business-workbench__journal-title">{{ productHeadline }}</h3>
    <p class="product-business-workbench__journal-summary">{{ statusStatement }}</p>
  </article>

  <article class="product-business-workbench__relation-card">
    <span class="product-business-workbench__relation-label">关联设备</span>
    <strong class="product-business-workbench__relation-value">{{ relationValueText }}</strong>
  </article>
</div>
```

- [ ] **Step 2: Replace the old contract sentence with a compact identity row**

Still in the same file, replace the single joined `contractStatement` string with an itemized computed list:

```ts
const relationValueText = computed(() => {
  if (!props.product) return '--'
  return `${Math.max(totalDevices.value, 0)} 台`
})

const identityItems = computed(() => [
  { label: '产品Key', value: productKeyText.value },
  { label: '接入协议', value: protocolText.value },
  { label: '节点类型', value: nodeTypeText.value },
  { label: '数据格式', value: dataFormatText.value }
])
```

And render it with:

```vue
<div class="product-business-workbench__identity-row">
  <span
    v-for="item in identityItems"
    :key="item.label"
    class="product-business-workbench__identity-item"
  >
    <span class="product-business-workbench__identity-label">{{ item.label }}</span>
    <strong class="product-business-workbench__identity-value">{{ item.value }}</strong>
  </span>
</div>
```

- [ ] **Step 3: Remove obsolete computed fields and restyle the header for compact spacing**

Delete the unused `contractStatusValue`, `contractStatusCaption`, `nextActionValue`, `nextActionCaption`, `contractStatement`, and `scaleCaption` computed branches. Replace the old header styles with compact card styles such as:

```css
.product-business-workbench__masthead-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(15rem, 16rem);
  gap: 0.88rem;
  align-items: stretch;
}

.product-business-workbench__info-card,
.product-business-workbench__relation-card {
  display: grid;
  min-height: 9.5rem;
  padding: 1rem 1.1rem;
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: rgba(255, 255, 255, 0.94);
}

.product-business-workbench__identity-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem 1.1rem;
  padding-top: 0.2rem;
}
```

Also change the tab active indicator from the brand underline to a light neutral line:

```css
.product-business-workbench__tab--active {
  color: var(--text-heading);
}

.product-business-workbench__tab--active::after {
  background: color-mix(in srgb, var(--text-secondary) 42%, transparent);
}
```

- [ ] **Step 4: Run the drawer test and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts --run
```

Expected: PASS.

### Task 3: Convert The Overview And Device Tab To Compact Cards

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductDetailWorkbench.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductDeviceListWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDetailWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductDeviceListWorkspace.test.ts`

- [ ] **Step 1: Rewrite the overview template as three metric cards plus one archive card**

In `ProductDetailWorkbench.vue`, replace the ledger sections with:

```vue
<section class="product-detail-workbench__metric-grid">
  <article
    v-for="metric in scaleMetrics"
    :key="metric.key"
    class="product-detail-workbench__metric-card"
  >
    <span class="product-detail-workbench__metric-label">{{ metric.label }}</span>
    <strong class="product-detail-workbench__metric-value">{{ metric.value }}</strong>
  </article>
</section>

<section class="product-detail-workbench__archive-card" data-testid="product-detail-archive-sheet">
  <header class="product-detail-workbench__archive-head">
    <strong class="product-detail-workbench__section-title">档案摘要</strong>
  </header>
  <div class="product-detail-workbench__archive-meta">
    <article v-for="item in archiveMetaItems" :key="item.key" class="product-detail-workbench__archive-chip">
      <span>{{ item.label }}</span>
      <strong>{{ item.value }}</strong>
    </article>
    <article class="product-detail-workbench__archive-description">
      <span>补充说明</span>
      <strong>{{ descriptionText }}</strong>
    </article>
  </div>
</section>
```

- [ ] **Step 2: Split archive data into adaptive meta items and a single-line description**

Still in `ProductDetailWorkbench.vue`, replace the old three-item summary array with:

```ts
const archiveMetaItems = computed(() => [
  {
    key: 'manufacturer',
    label: '厂商',
    value: toDisplayText(product.value.manufacturer)
  },
  {
    key: 'updateTime',
    label: '最近更新',
    value: formatDateTime(product.value.updateTime)
  }
])

const descriptionText = computed(() => product.value.description?.trim() || '当前没有补充说明')
```

And apply card styles that keep the description on one line:

```css
.product-detail-workbench__archive-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
}

.product-detail-workbench__archive-description {
  flex: 1 1 18rem;
  min-width: 0;
}

.product-detail-workbench__archive-description strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
```

- [ ] **Step 3: Rename the device registry and flatten extra top rails**

In `ProductDeviceListWorkspace.vue`, update the heading and shell:

```vue
<header class="device-workspace__registry-heading">
  <strong>设备清单</strong>
</header>
```

Replace the section container styles with a compact card shell:

```css
.device-workspace__registry-sheet {
  gap: 0.8rem;
}

.device-workspace__table-shell {
  overflow: hidden;
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: #fff;
}
```

- [ ] **Step 4: Run overview and device tests and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts --run
```

Expected: PASS.

### Task 4: Simplify The Contract Workspace Copy And Divider Grammar

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`

- [ ] **Step 1: Remove the numbered introduction list from manual mode**

In `ProductModelDesignerWorkspace.vue`, delete the `manualLedgerItems` computed block and remove the ordered list:

```vue
<ol class="product-model-designer__governance-steps">
  ...
</ol>
```

Replace the manual-mode body with a compact head and one short hint row:

```vue
<section v-if="designerMode === 'manual'" class="product-model-designer__governance-sheet">
  <div class="product-model-designer__sheet-head product-model-designer__sheet-head--compact">
    <div class="product-model-designer__sheet-copy">
      <strong>{{ governanceSheetHeading }}</strong>
      <span>{{ governanceSheetHint }}</span>
    </div>
    <StandardButton action="confirm" data-testid="confirm-model-candidates" @click="handlePrimaryAction">
      {{ primaryActionText }}
    </StandardButton>
  </div>
  <div class="product-model-designer__hint-strip">
    <span class="product-model-designer__hint-chip">直接贴上报数据即可自动识别字段</span>
  </div>
</section>
```

- [ ] **Step 2: Shorten the remaining copy and remove heavy line-based styling**

Replace the old long copy helpers with shorter versions:

```ts
const headerStatement = computed(() => {
  if (designerMode.value === 'formal') {
    return '这里只展示已经生效的正式字段。'
  }
  if (totalDevices.value <= 0) {
    return '当前还没有运行设备，先查看正式字段。'
  }
  return '贴上设备上报数据后，系统会自动识别待核对字段。'
})

const governanceSheetHint = computed(() => {
  if (totalDevices.value <= 0) {
    return '待设备接入后再开始核对。'
  }
  return '只保留自动识别和确认生效这两个关键动作。'
})
```

Then replace the border-heavy styles with compact cards:

```css
.product-model-designer__journal-head,
.product-model-designer__governance-sheet,
.product-model-designer__formal-sheet {
  padding-top: 0;
  border-top: 0;
}

.product-model-designer__journal-ruler {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.72rem;
  border: 0;
}

.product-model-designer__journal-ruler-item,
.product-model-designer__formal-overview-card,
.product-model-designer__hint-chip {
  border: 1px solid var(--panel-border);
  border-radius: 1rem;
  background: #fff;
}
```

- [ ] **Step 3: Update the contract workspace test to lock the compact manual mode**

In `ProductModelDesignerWorkspace.test.ts`, replace the old step-list assertions with:

```ts
expect(wrapper.find('.product-model-designer__governance-steps').exists()).toBe(false)
expect(wrapper.find('.product-model-designer__hint-strip').exists()).toBe(true)
expect(wrapper.text()).toContain('贴上设备上报数据后，系统会自动识别待核对字段。')
expect(wrapper.text()).not.toContain('运行数据核对')
expect(wrapper.text()).not.toContain('确认后生效')
```

- [ ] **Step 4: Run the contract workspace test and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected: PASS.

### Task 5: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update the product-workbench business wording in place**

Record that `/products` now uses:

```md
- 头部改为“产品信息卡 + 关联设备卡”双卡结构，不再展示“契约状态 / 下一步建议”。
- 产品总览改为“3 张经营指标卡 + 1 张档案摘要卡”。
- 关联设备页统一使用“设备清单”命名。
- 契约字段页删除编号介绍和多余线条，保留更短的客户语言。
```

- [ ] **Step 2: Update the front-end governance and acceptance docs**

In `docs/15-前端优化与治理计划.md` and `docs/21-业务功能清单与验收标准.md`, replace the old `契约状态 / 下一步建议 / 设备清册` wording with the new compact workbench contract so future refinements do not regress.

- [ ] **Step 3: Run the full focused verification suite**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts src/__tests__/components/product/ProductDetailWorkbench.test.ts src/__tests__/components/product/ProductDeviceListWorkspace.test.ts src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts --run
```

Expected: PASS.

- [ ] **Step 4: Run the front-end quality gate**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly captured unrelated pre-existing failure with the failing gate named in the handoff notes.
