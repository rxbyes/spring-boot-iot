# Product Model Customer-Facing Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the `/products` contract workbench into a customer-facing decision workspace without changing routes, backend compare/apply contracts, or the existing unified drawer structure.

**Architecture:** Keep the current `/products` workbench and governance drawer composition intact. Reframe the header, workspace tabs, governance drawer sections, compare list grouping, and apply receipt language around customer decisions, risks, and confidence explanations derived from existing compare/apply data.

**Tech Stack:** Vue 3 + TypeScript, Element Plus, Vitest, existing `Standard*` shared workbench components.

---

## File Structure

- Add: `docs/superpowers/plans/2026-04-05-product-model-customer-facing-workbench-implementation-plan.md`
  Responsibility: record the execution plan for this approved spec.
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
  Responsibility: promote contract status and next action in the workbench header and reduce scale-only emphasis.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
  Responsibility: rename the contract workspace into `待核对字段 / 正式字段`, add customer-facing status cards, and switch the CTA language.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
  Responsibility: reframe the governance drawer as a three-step decision flow and upgrade the apply receipt into customer-facing result confirmation.
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`
  Responsibility: group compare rows by action/risk, translate compare status and risk flags into customer language, and turn template evidence into trust explanations with folded technical detail.
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`
  Responsibility: lock the new customer-facing wording and structure with focused tests.
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
  Responsibility: update the product workbench customer-facing rules in place.

### Task 1: Write Red Tests For The Customer-Facing Contract Flow

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelDesignerDrawer.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts`

- [ ] **Step 1: Add failing header expectations for contract health and next action**

Update `ProductBusinessWorkbenchDrawer.test.ts` so the workbench header expects customer-facing signals such as:

```ts
expect(wrapper.text()).toContain('契约状态')
expect(wrapper.text()).toContain('下一步建议')
expect(wrapper.text()).toContain('当前已有运行设备，可继续补齐并核对契约字段。')
expect(wrapper.text()).toContain('建议优先进入待核对字段，确认本轮需要生效的契约。')
expect(wrapper.text()).not.toContain('核心规模')
```

- [ ] **Step 2: Add failing workspace expectations for `待核对字段 / 正式字段`**

Update `ProductModelDesignerWorkspace.test.ts` so the embedded workspace expects:

```ts
expect(wrapper.text()).toContain('待核对字段')
expect(wrapper.text()).toContain('正式字段')
expect(wrapper.text()).toContain('开始补齐契约')
expect(wrapper.text()).toContain('已正式生效')
expect(wrapper.text()).toContain('可直接纳入')
expect(wrapper.text()).toContain('待人工确认')
expect(wrapper.text()).toContain('继续观察')
expect(wrapper.text()).not.toContain('证据比对')
expect(wrapper.text()).not.toContain('进入双证据治理')
```

- [ ] **Step 3: Add failing drawer expectations for the three-step decision flow**

Update `ProductModelDesignerDrawer.test.ts` so the drawer expects:

```ts
expect(wrapper.text()).toContain('第一步：选择核对范围')
expect(wrapper.text()).toContain('第二步：核对字段结果')
expect(wrapper.text()).toContain('第三步：确认生效')
expect(wrapper.text()).toContain('确认并生效契约')
expect(wrapper.text()).toContain('结果确认卡')
expect(wrapper.text()).toContain('稍后确认')
expect(wrapper.text()).not.toContain('双证据治理')
expect(wrapper.text()).not.toContain('确认应用')
expect(wrapper.text()).not.toContain('人工裁决')
```

- [ ] **Step 4: Add failing compare-table expectations for customer action grouping and trust explanation**

Update `ProductModelGovernanceCompareTable.test.ts` so it expects:

```ts
expect(wrapper.text()).toContain('可直接纳入')
expect(wrapper.text()).toContain('建议人工确认')
expect(wrapper.text()).toContain('继续观察')
expect(wrapper.text()).toContain('存在冲突')
expect(wrapper.text()).toContain('名称或定义可能不一致，建议人工确认')
expect(wrapper.text()).toContain('当前只有单侧证据，建议继续观察')
expect(wrapper.text()).toContain('该字段来自裂缝模板自动识别')
expect(wrapper.text()).toContain('系统已按 LF_VALUE 统一到正式字段口径')
```

- [ ] **Step 5: Run the focused Vitest suite and verify red**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  --run
```

Expected: FAIL because the current UI still uses engineering wording and the old compare structure.

### Task 2: Implement The Minimal Customer-Facing Workbench Changes

**Files:**
- Modify: `spring-boot-iot-ui/src/components/product/ProductBusinessWorkbenchDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelDesignerDrawer.vue`
- Modify: `spring-boot-iot-ui/src/components/product/ProductModelGovernanceCompareTable.vue`

- [ ] **Step 1: Reframe the business workbench header around contract status**

In `ProductBusinessWorkbenchDrawer.vue`, keep the existing tab structure, but replace the scale-only right column with contract-health summary cards derived from existing product facts. The visible wording should explicitly surface:

```ts
'契约状态'
'下一步建议'
'当前已有运行设备，可继续补齐并核对契约字段。'
'建议优先进入待核对字段，确认本轮需要生效的契约。'
```

The device count can remain as a secondary business metric, but it must not be the only dominant header emphasis.

- [ ] **Step 2: Rename the workspace into `待核对字段 / 正式字段` and add status cards**

In `ProductModelDesignerWorkspace.vue`:

```ts
const designerStageTitle = computed(() =>
  designerMode.value === 'manual' ? '待核对字段' : '正式字段'
)
```

Update the mode chips and summary cards so the manual mode reads like customer review work instead of internal compare mechanics, and make the CTA dynamic:

```ts
'开始补齐契约'
'继续核对字段'
'先查看正式字段'
```

- [ ] **Step 3: Reframe the governance drawer as a three-step decision flow**

In `ProductModelDesignerDrawer.vue`, keep the same data and requests, but rewrite the section headers and body copy to:

```ts
'第一步：选择核对范围'
'第二步：核对字段结果'
'第三步：确认生效'
'确认并生效契约'
'结果确认卡'
```

Keep the same `compareProductModelGovernance` and `applyProductModelGovernance` calls.

- [ ] **Step 4: Reframe decisions and compare rows into customer-facing actions**

In `ProductModelGovernanceCompareTable.vue`, group rows by action bucket first and keep type as a secondary filter. Translate:

```ts
review -> '稍后确认'
definition_mismatch -> '名称或定义可能不一致，建议人工确认'
manual_missing/runtime_missing -> '当前只有单侧证据，建议继续观察'
formal_baseline -> '该字段已存在正式契约，本轮不建议重复纳入'
```

Add a customer-facing reason line and keep the technical evidence as secondary detail.

- [ ] **Step 5: Turn template evidence into trust explanations with foldable technical detail**

Still in `ProductModelGovernanceCompareTable.vue` and the apply receipt area of `ProductModelDesignerDrawer.vue`, derive a short explanation from existing `protocolTemplateEvidence`, for example:

```ts
'该字段来自裂缝模板自动识别'
'已在子设备 202018143 上观测到同类上报'
'系统已按 LF_VALUE 统一到正式字段口径'
```

Keep raw template code, logical channel, mirror strategy, parent removal, and decode failures available as folded detail, not the primary headline.

- [ ] **Step 6: Run the focused Vitest suite and verify green**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  --run
```

Expected: PASS.

### Task 3: Update Docs And Run Final Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update product workbench docs in place**

Document that:

- `/products` now foregrounds contract status and next actions in the business workbench header
- the contract workspace uses `待核对字段 / 正式字段`
- the governance drawer follows a three-step decision flow
- template evidence is surfaced as a customer-facing trust explanation before technical detail

- [ ] **Step 2: Re-run the focused front-end tests**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- \
  src/__tests__/components/product/ProductBusinessWorkbenchDrawer.test.ts \
  src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts \
  src/__tests__/components/product/ProductModelDesignerDrawer.test.ts \
  src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts \
  --run
```

Expected: PASS.

- [ ] **Step 3: Run the repo quality gate**

Run:

```bash
node scripts/run-quality-gates.mjs
```

Expected: PASS, or a clearly scoped unrelated pre-existing failure.
