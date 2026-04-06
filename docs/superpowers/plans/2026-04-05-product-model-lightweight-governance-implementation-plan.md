# Product Model Lightweight Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evolve the `/products` governance drawer from the first lightweight version into a denser customer workbench with a compact header, a light result-group strip, stronger result cards, and a tighter confirmation footer.

**Architecture:** Keep the current `/products` route, compare/apply requests, and relation bootstrap flow. Build on the existing lightweight drawer by reworking the information density and result hierarchy: compact detail-page-style shell, `B3` result grouping, stronger single-column cards, and a footer that constantly answers “how many items are selected and what will happen”.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing product workbench shared components and styles.

---

## File Structure

- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  Responsibility: compact the workbench shell, tighten spacing/copy, surface the result-group strip context, and strengthen the footer summary.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  Responsibility: evolve the result cards into the `B3` layout with a compact group strip, stronger metadata hierarchy, more explicit formal-baseline/status presentation, and tighter card rhythm.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  Responsibility: lock the compact workbench and `B3` result behavior so old verbose structure cannot return.
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: update the product-model governance description to the compact workbench wording.

### Task 1: Write Failing Tests For The Compact B3 Workbench

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`

- [ ] **Step 1: Add failing drawer expectations for the denser workbench summary**

Add assertions that lock the compact header/footer summary instead of the earlier loose layout:

```ts
expect(wrapper.text()).toContain('已生效')
expect(wrapper.text()).toContain('本次识别')
expect(wrapper.text()).toContain('待生效')
expect(wrapper.text()).toContain('本次将生效 1 个字段')
expect(wrapper.text()).toContain('当前已生效字段')
```

- [ ] **Step 2: Add failing compare-table expectations for the B3 result-group strip**

Add expectations that the compare table keeps the light group strip and stronger result-card metadata:

```ts
expect(wrapper.text()).toContain('可直接生效')
expect(wrapper.text()).toContain('待确认')
expect(wrapper.text()).toContain('继续观察')
expect(wrapper.text()).toContain('存在差异')
expect(wrapper.text()).toContain('identifier:')
expect(wrapper.text()).toContain('类型:')
```

- [ ] **Step 3: Add failing expectations for stronger formal-baseline/result emphasis**

Assert the result cards explicitly surface formal baseline and compact status wording:

```ts
expect(wrapper.text()).toContain('正式模型已存在')
expect(wrapper.text()).toContain('当前字段可直接确认生效')
expect(wrapper.text()).toContain('正式模型已存在，可按需纳入修订')
```

- [ ] **Step 4: Run the focused Vitest suite and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  --run
```

Expected: FAIL if the compact `B3` workbench expectations are not fully reflected yet.

### Task 2: Implement The Compact Drawer Shell

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`

- [ ] **Step 1: Tighten the header and section rhythm**

Refine the drawer shell so the header behaves more like a compact detail workbench:

```vue
<div class="product-model-designer-drawer__summary-item">
  <span>已生效</span>
  <strong>{{ models.length }}</strong>
</div>
```

and ensure the section headers stay short-only:

```vue
<h3>上报数据</h3>
<h3>识别结果</h3>
<h3>本次生效</h3>
```

- [ ] **Step 2: Compress the input toolbar area into one compact workbench row**

Keep the same controls, but align them as one denser operator strip:

```vue
<div class="product-model-designer-drawer__extract-mode">...</div>
<div class="product-model-designer-drawer__runtime-toggle">...</div>
<div class="product-model-designer-drawer__actions">...</div>
```

The result should feel closer to a detail page toolbar than a narrated wizard.

- [ ] **Step 3: Tighten the relation-child helper panel**

Keep only the short helper, compact form, preset preview, and bootstrap button:

```vue
<p class="product-model-designer-drawer__relation-helper">
  已识别为父设备样本，将自动归一到子产品字段
</p>
```

and avoid introducing any new explanatory paragraph blocks.

- [ ] **Step 4: Strengthen the footer summary into a constant “selection outcome” answer**

Replace the generic footer summary with compact selection-oriented wording:

```ts
const footerSummaryText = computed(() => {
  if (selectedApplyItems.value.length) {
    return `已选 ${selectedApplyItems.value.length} 项，确认后将写入正式字段`
  }
  if (compareRows.value.length) {
    return `已识别 ${compareRows.value.length} 个字段，请选择需要生效的项`
  }
  return '贴上报数据后，系统会自动提炼字段'
})
```

- [ ] **Step 5: Keep the middle apply list concise**

Ensure the `本次生效` section stays card-like and compact instead of regrowing helper paragraphs:

```vue
<div v-else class="detail-empty">
  当前还没有待生效字段。
</div>
```

- [ ] **Step 6: Run the drawer-focused test and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  --run
```

Expected: PASS.

### Task 3: Implement The B3 Result Cards

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`

- [ ] **Step 1: Keep the light group strip as the top-level sorting answer**

Retain and polish the compact group strip:

```ts
const groupOptions = [
  { label: '可直接生效', key: 'direct' },
  { label: '待确认', key: 'review' },
  { label: '继续观察', key: 'observe' },
  { label: '存在差异', key: 'conflict' }
]
```

- [ ] **Step 2: Strengthen the first row of each result card**

Ensure the first visible metadata line is consistently:

```vue
<div class="product-model-governance-compare-table__row-meta">
  <span>identifier: {{ row.identifier }}</span>
  <span>类型: {{ rowTypeLabel(row) }}</span>
  <span>{{ rowDataHint(row) }}</span>
</div>
```

- [ ] **Step 3: Keep sample value and source chips as the main evidence answer**

The evidence area should stay compact and result-oriented:

```vue
<div class="product-model-governance-compare-table__sample-card">
  <span>样例值</span>
  <strong>{{ rowSampleValue(row) }}</strong>
</div>
```

and

```vue
<span class="product-model-governance-compare-table__source-chip">
  {{ chip }}
</span>
```

- [ ] **Step 4: Surface formal baseline and status as short explicit answers**

Keep the status short and direct:

```ts
if (row.compareStatus === 'formal_exists') {
  return '正式模型已存在，可按需纳入修订'
}
return '当前字段可直接确认生效'
```

- [ ] **Step 5: Keep protocol-template context compact**

If `protocolTemplateEvidence` exists, keep only compact inline summary badges:

```ts
return [
  friendlyTemplateName(protocolTemplateEvidence.templateCodes?.[0]),
  protocolTemplateEvidence.childDeviceCodes?.join(' / '),
  protocolTemplateEvidence.canonicalizationStrategies?.join(' / ')
].filter(Boolean)
```

Do not reintroduce folded technical detail panels.

- [ ] **Step 6: Run the compare-table-focused test and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  --run
```

Expected: PASS.

### Task 4: Sync Docs And Run Final Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update docs to the compact B3 wording**

Document that `/products` now uses:

```md
- 上报数据
- 识别结果
- 本次生效
- 可直接生效 / 待确认 / 继续观察 / 存在差异
```

and explicitly remove references to the old three-step narrative or three-column evidence cards.

- [ ] **Step 2: Run the broader product-governance Vitest suite**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  --run
```

Expected: PASS.

- [ ] **Step 3: Run the frontend build**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
```

Expected: PASS.

- [ ] **Step 4: Run the component contract guard**

Run:

```bash
npm --prefix spring-boot-iot-ui run component:guard
```

Expected: `Component contract guard passed.`

- [ ] **Step 5: Run the local minimum quality gates and record any unrelated blocker**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: if it fails outside the touched frontend scope, record the exact unrelated blocker instead of claiming the full gate passed.
