# RD Workbench Automation Modularization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an RD-first automation workbench that splits asset authoring into dedicated modules while keeping execution and results as shared pages.

**Architecture:** Keep the current browser-plan and acceptance-registry data model, add two new landing views (`质量工场总览` and `研发工场总览`), and split the overloaded asset editor into four focused RD pages. Reuse `useAutomationPlanBuilder` as the single plan source, expose thinner page-level composables, and keep `/automation-assets` and `/automation-test` as compatibility routes.

**Tech Stack:** Vue 3 SFCs, Vue Router, Vitest, Element Plus, existing shared page-shell components, `sql/init-data.sql`, Markdown docs.

---

## File Map

### New files

- `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
  Dedicated quality-workbench overview page for `研发工场 / 执行中心 / 结果与基线中心`.
- `spring-boot-iot-ui/src/views/RdWorkbenchLandingView.vue`
  Dedicated RD-workbench overview page for the four RD modules.
- `spring-boot-iot-ui/src/views/AutomationInventoryView.vue`
  Focused page inventory and coverage-gap page.
- `spring-boot-iot-ui/src/views/AutomationTemplatesView.vue`
  Focused scenario template launchpad page.
- `spring-boot-iot-ui/src/views/AutomationPlansView.vue`
  Focused plan composition page for scenario editing/import/export.
- `spring-boot-iot-ui/src/views/AutomationHandoffView.vue`
  Focused handoff/packaging page for plan summary, command guidance, and delivery notes.
- `spring-boot-iot-ui/src/composables/useAutomationInventoryWorkbench.ts`
  Thin adapter exposing inventory-only state from `useAutomationPlanBuilder`.
- `spring-boot-iot-ui/src/composables/useAutomationTemplateWorkbench.ts`
  Thin adapter exposing template cards and add-template actions.
- `spring-boot-iot-ui/src/composables/useAutomationPlanComposer.ts`
  Thin adapter exposing scenario editing, plan import/export, and previews.
- `spring-boot-iot-ui/src/composables/useAutomationHandoffWorkbench.ts`
  Thin adapter exposing summary, suggestions, command preview, and handoff notes.

### Modified files

- `spring-boot-iot-ui/src/router/index.ts`
  Add `rd-workbench` and the four RD child routes; point quality-workbench to the new landing page; keep compatibility routes.
- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
  Add RD-workbench schema, update quality-workbench cards, and update route meta copy for compatibility pages.
- `spring-boot-iot-ui/src/views/AutomationAssetsView.vue`
  Reduce to a compatibility wrapper that renders the new RD-workbench landing page.
- `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
  Reduce to a compatibility wrapper that renders the new RD-workbench landing page.
- `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
  Lock the quality-workbench → RD-workbench card mapping and the new RD-workbench route meta/card map.
- `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
  Lock each RD page to its own responsibility and verify compatibility wrappers.
- `sql/init-data.sql`
  Add menu seeds for the RD workbench and four RD pages; downgrade `/automation-assets` and `/automation-test` to compatibility entries.
- `docs/02-业务功能与流程说明.md`
  Document the RD-workbench flow and the new quality-workbench responsibilities.
- `docs/05-自动化测试与质量保障.md`
  Document the RD-first automation-authoring flow.
- `docs/08-变更记录与技术债清单.md`
  Record the RD modularization rollout and the remaining shared-data tradeoffs.
- `docs/21-业务功能清单与验收标准.md`
  Update route/menu matrices, page acceptance contracts, and compatibility route notes.

## Task 1: Lock the New Quality/RD Route Contract

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Extend the failing contract tests for the RD-workbench map**

```typescript
it('maps quality workbench to rd-workbench plus shared execution/results pages', () => {
  const config = getSectionHomeConfigByPath('/quality-workbench');

  expect(config?.cards.map((item) => item.path)).toEqual([
    '/rd-workbench',
    '/automation-execution',
    '/automation-results'
  ]);
  expect(getRouteMetaPreset('/rd-workbench')).toMatchObject({
    title: '研发工场',
    description: '围绕页面盘点、模板沉淀、计划编排与交付打包组织研发自动化资产能力。'
  });
});

it('maps rd-workbench to four rd authoring pages', () => {
  const config = getSectionHomeConfigByPath('/rd-workbench');

  expect(config?.cards.map((item) => item.path)).toEqual([
    '/rd-automation-inventory',
    '/rd-automation-templates',
    '/rd-automation-plans',
    '/rd-automation-handoff'
  ]);
  expect(getRouteMetaPreset('/automation-assets')).toMatchObject({
    title: '自动化资产中心',
    description: '兼容旧入口，第一轮直接落到研发工场总览。'
  });
});
```

```typescript
it('registers rd-workbench routes and keeps compatibility routes alive', () => {
  const source = readRouter();

  expect(source).toContain("path: '/quality-workbench'");
  expect(source).toContain("path: '/rd-workbench'");
  expect(source).toContain("path: '/rd-automation-inventory'");
  expect(source).toContain("path: '/rd-automation-templates'");
  expect(source).toContain("path: '/rd-automation-plans'");
  expect(source).toContain("path: '/rd-automation-handoff'");
  expect(source).toContain("path: '/automation-assets'");
  expect(source).toContain("path: '/automation-test'");
});
```

- [ ] **Step 2: Run the focused contract suite and verify it fails before schema/router changes**

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because `rd-workbench` routes and cards do not exist yet.

- [ ] **Step 3: Update workspace schema and router entries**

```typescript
{
  key: 'quality-workbench',
  path: '/quality-workbench',
  cards: [
    { path: '/rd-workbench', label: '研发工场', description: '面向研发的自动化资产编排主入口。', short: '研' },
    { path: '/automation-execution', label: '执行中心', description: '统一维护目标环境、执行范围与验收注册表。', short: '执' },
    { path: '/automation-results', label: '结果与基线中心', description: '统一导入结果、查看失败并维护基线证据。', short: '果' }
  ]
},
{
  key: 'rd-workbench',
  path: '/rd-workbench',
  navLabel: '研发总览',
  navCaption: '查看研发自动化资产编排主链路',
  navShort: '研',
  title: '研发工场',
  description: '围绕页面盘点、模板沉淀、计划编排与交付打包组织研发自动化资产能力。',
  intro: '建议先盘点页面和模板，再进入计划编排，最后整理交付包并转到执行中心。',
  menuTitle: '研发工场',
  menuHint: '覆盖研发自动化资产编排与交付准备。',
  matchKeys: ['rd-workbench', 'automation-rd'],
  matchLabels: ['研发工场', '研发自动化'],
  cards: [
    { path: '/rd-automation-inventory', label: '页面盘点台', description: '维护页面清单、覆盖缺口与人工补录页面。', short: '盘' },
    { path: '/rd-automation-templates', label: '场景模板台', description: '沉淀页面冒烟、表单提交与列表详情模板。', short: '模' },
    { path: '/rd-automation-plans', label: '计划编排台', description: '维护场景顺序、步骤、断言、导入与导出。', short: '编' },
    { path: '/rd-automation-handoff', label: '交付打包台', description: '整理计划摘要、执行建议、基线说明与验收备注。', short: '交' }
  ],
  steps: ['先盘点页面。', '再沉淀模板。', '然后编排正式计划。', '最后整理交付包并转执行中心。']
}
```

```typescript
{
  path: '/quality-workbench',
  name: 'quality-workbench',
  component: () => import('../views/QualityWorkbenchLandingView.vue'),
  meta: routeMeta('/quality-workbench')
},
{
  path: '/rd-workbench',
  name: 'rd-workbench',
  component: () => import('../views/RdWorkbenchLandingView.vue'),
  meta: routeMeta('/rd-workbench')
},
{
  path: '/rd-automation-inventory',
  name: 'rd-automation-inventory',
  component: () => import('../views/AutomationInventoryView.vue'),
  meta: routeMeta('/rd-automation-inventory')
}
```

- [ ] **Step 4: Re-run the focused contract suite**

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS for the new route and workspace contract.

- [ ] **Step 5: Commit the shared route contract**

```bash
git add spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add rd workbench route contract"
```

## Task 2: Build the Landing Views and Compatibility Wrappers

**Files:**
- Create: `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
- Create: `spring-boot-iot-ui/src/views/RdWorkbenchLandingView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationAssetsView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Add failing source-contract checks for the two landing views**

```typescript
it('keeps quality workbench focused on rd/shared-center navigation', () => {
  const source = readView('QualityWorkbenchLandingView.vue');

  expect(source).toContain('/rd-workbench');
  expect(source).toContain('/automation-execution');
  expect(source).toContain('/automation-results');
  expect(source).not.toContain('<AutomationScenarioEditor');
});

it('keeps rd-workbench focused on four rd authoring modules', () => {
  const source = readView('RdWorkbenchLandingView.vue');

  expect(source).toContain('/rd-automation-inventory');
  expect(source).toContain('/rd-automation-templates');
  expect(source).toContain('/rd-automation-plans');
  expect(source).toContain('/rd-automation-handoff');
  expect(source).not.toContain('<AutomationRegistryPanel');
  expect(source).not.toContain('<AutomationResultImportPanel');
});
```

- [ ] **Step 2: Run the focused view-contract suite and verify it fails**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because the new landing pages do not exist yet.

- [ ] **Step 3: Create the landing views and downgrade compatibility routes**

```vue
<!-- spring-boot-iot-ui/src/views/AutomationAssetsView.vue -->
<template>
  <RdWorkbenchLandingView />
</template>

<script setup lang="ts">
import RdWorkbenchLandingView from './RdWorkbenchLandingView.vue';
</script>
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationTestCenterView.vue -->
<template>
  <RdWorkbenchLandingView />
</template>

<script setup lang="ts">
import RdWorkbenchLandingView from './RdWorkbenchLandingView.vue';
</script>
```

The landing views should use `StandardPageShell + StandardWorkbenchPanel + PanelCard + RouterLink`, show a few summary chips, and keep the content purely navigational.

- [ ] **Step 4: Re-run the focused view-contract suite**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS for both landing pages and the compatibility wrappers.

- [ ] **Step 5: Commit the landing-view layer**

```bash
git add spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue spring-boot-iot-ui/src/views/RdWorkbenchLandingView.vue spring-boot-iot-ui/src/views/AutomationAssetsView.vue spring-boot-iot-ui/src/views/AutomationTestCenterView.vue spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add rd and quality landing views"
```

## Task 3: Split the Asset Editor into Four RD Pages

**Files:**
- Create: `spring-boot-iot-ui/src/composables/useAutomationInventoryWorkbench.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationTemplateWorkbench.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationPlanComposer.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationHandoffWorkbench.ts`
- Create: `spring-boot-iot-ui/src/views/AutomationInventoryView.vue`
- Create: `spring-boot-iot-ui/src/views/AutomationTemplatesView.vue`
- Create: `spring-boot-iot-ui/src/views/AutomationPlansView.vue`
- Create: `spring-boot-iot-ui/src/views/AutomationHandoffView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Extend the failing view-contract test to lock page ownership**

```typescript
it('keeps inventory page focused on page discovery only', () => {
  const source = readView('AutomationInventoryView.vue');

  expect(source).toContain('<AutomationPageDiscoveryPanel');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationRegistryPanel');
});

it('keeps template page focused on template launch actions', () => {
  const source = readView('AutomationTemplatesView.vue');

  expect(source).toContain("addScenario('pageSmoke')");
  expect(source).toContain("addScenario('formSubmit')");
  expect(source).toContain("addScenario('listDetail')");
  expect(source).not.toContain('<AutomationRegistryPanel');
  expect(source).not.toContain('<AutomationResultImportPanel');
});

it('keeps plans page focused on scenario editing and plan io', () => {
  const source = readView('AutomationPlansView.vue');

  expect(source).toContain('<AutomationScenarioEditor');
  expect(source).toContain('<AutomationPlanImportDrawer');
  expect(source).toContain('<ResponsePanel');
  expect(source).not.toContain('<AutomationRegistryPanel');
});

it('keeps handoff page focused on summary and delivery guidance', () => {
  const source = readView('AutomationHandoffView.vue');

  expect(source).toContain('执行建议');
  expect(source).toContain('交付备注');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationResultImportPanel');
});
```

- [ ] **Step 2: Run the focused suite and verify it fails before the split**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because the four RD pages and their composables do not exist yet.

- [ ] **Step 3: Add thin composables and focused views**

```typescript
// spring-boot-iot-ui/src/composables/useAutomationInventoryWorkbench.ts
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';

export function useAutomationInventoryWorkbench() {
  const planBuilder = useAutomationPlanBuilder();

  return {
    inventoryTableRef: planBuilder.inventoryTableRef,
    inventoryMetrics: planBuilder.inventoryMetrics,
    inventorySourceText: planBuilder.inventorySourceText,
    pageInventory: planBuilder.pageInventory,
    buildInventorySourceLabel: planBuilder.buildInventorySourceLabel,
    buildTemplateLabel: planBuilder.buildTemplateLabel,
    isRouteCovered: planBuilder.isRouteCovered,
    handleInventorySelectionChange: planBuilder.handleInventorySelectionChange,
    refreshPageInventory: planBuilder.refreshPageInventory,
    selectUncoveredPages: planBuilder.selectUncoveredPages,
    generateSelectedInventoryScenarios: planBuilder.generateSelectedInventoryScenarios,
    generateUncoveredInventoryScenarios: planBuilder.generateUncoveredInventoryScenarios,
    openManualPageDialog: planBuilder.openManualPageDialog,
    removeManualPage: planBuilder.removeManualPage,
    showManualPageDialog: planBuilder.showManualPageDialog,
    scopeOptions: planBuilder.scopeOptions,
    inventoryTemplateOptions: planBuilder.inventoryTemplateOptions,
    saveManualPage: planBuilder.saveManualPage
  };
}
```

The four views should split the current `AutomationAssetsView.vue` content along these lines:

1. `AutomationInventoryView.vue`
   - inventory metrics
   - `AutomationPageDiscoveryPanel`
   - `AutomationManualPageDrawer`
2. `AutomationTemplatesView.vue`
   - built-in template cards
   - add-template actions
   - current template usage summary
3. `AutomationPlansView.vue`
   - scenario editor
   - import/export/reset
   - scenario preview table
   - JSON response panel
4. `AutomationHandoffView.vue`
   - plan metrics
   - command preview
   - quality suggestions
   - handoff summary and execution guidance

- [ ] **Step 4: Re-run the focused view-contract suite**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS with each RD page owning only its own panels.

- [ ] **Step 5: Commit the RD page split**

```bash
git add spring-boot-iot-ui/src/composables/useAutomationInventoryWorkbench.ts spring-boot-iot-ui/src/composables/useAutomationTemplateWorkbench.ts spring-boot-iot-ui/src/composables/useAutomationPlanComposer.ts spring-boot-iot-ui/src/composables/useAutomationHandoffWorkbench.ts spring-boot-iot-ui/src/views/AutomationInventoryView.vue spring-boot-iot-ui/src/views/AutomationTemplatesView.vue spring-boot-iot-ui/src/views/AutomationPlansView.vue spring-boot-iot-ui/src/views/AutomationHandoffView.vue spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: split rd automation authoring pages"
```

## Task 4: Update Menu Seeds, Docs, and Verification

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the failing text-contract checks**

```powershell
Select-String -Path 'sql/init-data.sql' -SimpleMatch -Pattern 'system:rd-workbench','system:rd-automation-inventory','system:rd-automation-templates','system:rd-automation-plans','system:rd-automation-handoff'
Select-String -Path 'docs/21-业务功能清单与验收标准.md' -SimpleMatch -Pattern '/rd-workbench','/rd-automation-inventory','/rd-automation-templates','/rd-automation-plans','/rd-automation-handoff'
```

Expected: no matches before the rollout.

- [ ] **Step 2: Update the menu seeds and role grants**

```sql
(93003015, 1, 93000005, '研发工场', 'system:rd-workbench', '/rd-workbench', 'RdWorkbenchLandingView', 'edit-pen', '{"caption":"研发自动化资产编排主入口"}', 51, 1, 1, '/rd-workbench', 'system:rd-workbench', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003016, 1, 93000005, '页面盘点台', 'system:rd-automation-inventory', '/rd-automation-inventory', 'AutomationInventoryView', 'document', '{"caption":"页面清单、覆盖缺口与人工补录"}', 52, 1, 1, '/rd-automation-inventory', 'system:rd-automation-inventory', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003017, 1, 93000005, '场景模板台', 'system:rd-automation-templates', '/rd-automation-templates', 'AutomationTemplatesView', 'files', '{"caption":"沉淀页面冒烟、表单提交与列表详情模板"}', 53, 1, 1, '/rd-automation-templates', 'system:rd-automation-templates', 53, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003018, 1, 93000005, '计划编排台', 'system:rd-automation-plans', '/rd-automation-plans', 'AutomationPlansView', 'edit', '{"caption":"维护场景顺序、步骤、断言与导入导出"}', 54, 1, 1, '/rd-automation-plans', 'system:rd-automation-plans', 54, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003019, 1, 93000005, '交付打包台', 'system:rd-automation-handoff', '/rd-automation-handoff', 'AutomationHandoffView', 'promotion', '{"caption":"整理执行建议、基线说明与验收备注"}', 55, 1, 1, '/rd-automation-handoff', 'system:rd-automation-handoff', 55, 1, 1, 1, NOW(), 1, NOW(), 0)
```

- [ ] **Step 3: Update the docs for the RD-first flow**

Document these exact responsibilities:

1. `质量工场总览` 负责 `研发工场 / 执行中心 / 结果与基线中心` 的上层导航。
2. `研发工场总览` 负责研发自动化资产编排主入口。
3. `页面盘点台 / 场景模板台 / 计划编排台 / 交付打包台` 构成研发资产编排链路。
4. `/automation-assets` 与 `/automation-test` 仅保留兼容入口。

- [ ] **Step 4: Run the targeted tests and the quality gate**

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

Run: `npm run component:guard`

Expected: PASS.

Run: `npm run list:guard`

Expected: PASS.

Run: `npm run style:guard`

Expected: PASS.

Run: `node scripts/docs/check-topology.mjs`

Expected: PASS.

Run: `node scripts/run-quality-gates.mjs`

Expected: PASS or a concrete environment blocker unrelated to the RD-workbench split itself.

- [ ] **Step 5: Commit the rollout and docs**

```bash
git add sql/init-data.sql docs/02-业务功能与流程说明.md docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "feat: modularize rd automation workbench"
```

## Self-Review

### Spec coverage

This plan covers the spec sections:

1. Module structure: Tasks 1-3 add `质量工场总览 -> 研发工场 -> 四个研发模块` plus shared execution/results pages.
2. Page responsibilities: Task 3 isolates inventory, templates, plans, and handoff responsibilities.
3. Data-flow compatibility: Task 3 keeps `useAutomationPlanBuilder` as the single plan source and continues to reuse execution/results composables.
4. Route/menu/docs rollout: Tasks 1 and 4 update route schema, menu seeds, and docs.
5. Verification: Task 4 runs the focused suite and repository quality gate.

### Placeholder scan

No `TODO`, `TBD`, “implement later”, or “similar to Task N” placeholders remain.

### Type consistency

The route names, menu permissions, and view file names are consistent across the plan:

1. `/rd-workbench` <-> `RdWorkbenchLandingView.vue` <-> `system:rd-workbench`
2. `/rd-automation-inventory` <-> `AutomationInventoryView.vue` <-> `system:rd-automation-inventory`
3. `/rd-automation-templates` <-> `AutomationTemplatesView.vue` <-> `system:rd-automation-templates`
4. `/rd-automation-plans` <-> `AutomationPlansView.vue` <-> `system:rd-automation-plans`
5. `/rd-automation-handoff` <-> `AutomationHandoffView.vue` <-> `system:rd-automation-handoff`
