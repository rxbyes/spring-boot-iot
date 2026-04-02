# Quality Workbench Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the quality workbench from one overloaded `/automation-test` page into `质量工场总览 + 自动化资产中心 + 执行中心 + 结果与基线中心`, while keeping the old `/automation-test` entry compatible.

**Architecture:** Keep the existing `SectionLandingView` overview pattern and the current browser-plan / acceptance-registry data model. Split the current `AutomationTestCenterView` responsibilities across three dedicated views, route new traffic through `/quality-workbench`, and keep `/automation-test` as a compatibility path to the asset editor during the first rollout.

**Tech Stack:** Vue 3 SFCs, Vue Router, Vitest, Element Plus, existing `StandardPageShell` / `StandardWorkbenchPanel` shared UI contract, `sql/init-data.sql`, Markdown docs.

---

## File Map

### New files

- `spring-boot-iot-ui/src/views/AutomationAssetsView.vue`
  Dedicated asset editor page for inventory, scenario editing, plan preview, and JSON export.
- `spring-boot-iot-ui/src/views/AutomationExecutionView.vue`
  Dedicated execution page for target config, command preview, and acceptance registry visibility.
- `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
  Dedicated result page for run-summary import, failed scenario review, and quality suggestions.
- `spring-boot-iot-ui/src/composables/useAutomationExecutionWorkbench.ts`
  Thin page-level adapter that exposes execution-only state from the plan builder plus registry helpers.
- `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
  Thin page-level adapter that exposes imported run summaries and suggestion data without the scenario editor baggage.
- `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
  Source-level route/view contract tests that prove each new page contains only the panels it owns.

### Modified files

- `spring-boot-iot-ui/src/router/index.ts`
  Add `/automation-assets`, `/automation-execution`, `/automation-results`, and map `/automation-test` to the compatibility asset page.
- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
  Expand the quality-workbench cards from one entry to three entries and add route meta copy for the new pages.
- `spring-boot-iot-ui/src/utils/shellPanelContent.ts`
  Repoint quality help and developer shortcuts from `/automation-test` to the quality workbench overview.
- `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
  Lock in the new quality-workbench cards and route meta presets.
- `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts`
  Lock in the help/notice routing changes for the quality workbench.
- `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
  Reduce to a compatibility wrapper that renders `AutomationAssetsView`.
- `sql/init-data.sql`
  Add three quality-workbench child menus and keep `system:automation-test` as the compatibility entry during rollout.
- `docs/02-业务功能与流程说明.md`
  Update quality-workbench page responsibilities and user flow.
- `docs/05-自动化测试与质量保障.md`
  Update the automation workbench execution / result-reading path.
- `docs/08-变更记录与技术债清单.md`
  Record the restructuring change and remaining follow-up.
- `docs/21-业务功能清单与验收标准.md`
  Update the quality-workbench route matrix and page-level acceptance contract.

## Task 1: Lock the Quality Workbench Route and Workspace Contract

**Files:**
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/utils/shellPanelContent.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts`

- [ ] **Step 1: Write the failing workspace and shell tests for the new quality-workbench structure**

```typescript
it('exposes the quality workbench as overview plus three specialty pages', () => {
  const config = getSectionHomeConfigByPath('/quality-workbench');

  expect(config?.cards.map((item) => item.path)).toEqual([
    '/automation-assets',
    '/automation-execution',
    '/automation-results'
  ]);
  expect(getRouteMetaPreset('/automation-assets')).toMatchObject({
    title: '自动化资产中心',
    description: '沉淀页面盘点、场景模板、执行计划与导入导出资产。'
  });
  expect(getRouteMetaPreset('/automation-execution')).toMatchObject({
    title: '执行中心',
    description: '统一查看执行配置、命令预览和验收注册表依赖关系。'
  });
  expect(getRouteMetaPreset('/automation-results')).toMatchObject({
    title: '结果与基线中心',
    description: '统一导入运行结果、查看失败场景并维护质量建议与基线证据。'
  });
  expect(getRouteMetaPreset('/automation-test')).toMatchObject({
    title: '自动化工场',
    description: '兼容旧入口，第一轮直接落到自动化资产中心。'
  });
});

it('routes quality help entries through the overview instead of the legacy /automation-test page', () => {
  const content = buildShellHelpPopoverContent({
    roleProfile: createRoleProfile({
      key: 'developer',
      label: '开发人员',
      roleCodes: ['DEVELOPER_STAFF'],
      roleNameKeywords: ['开发'],
      defaultPath: '/device-access',
      preferredWorkspaceKeys: ['iot-access', 'risk-config', 'quality-workbench'],
      featuredPaths: ['/reporting', '/system-log', '/message-trace', '/quality-workbench'],
      cockpitRole: 'rd',
      focusLabel: '链路与质量',
      focusDescription: '优先联调接入链路、异常观测、消息追踪和自动化回归。'
    }),
    homePath: '/device-access',
    currentPath: '/reporting',
    activeGroup: createActiveGroup({
      key: 'iot-access',
      label: '接入智维',
      items: [
        { to: '/reporting', label: '链路验证中心', caption: 'caption', short: '验' },
        { to: '/system-log', label: '异常观测台', caption: 'caption', short: '观' }
      ]
    }),
    allowedPaths: ['/reporting', '/system-log', '/message-trace', '/quality-workbench', '/automation-assets'],
    activities: []
  });

  const itemPaths = content.sections.flatMap((section) => section.items.map((item) => item.path).filter(Boolean));

  expect(itemPaths).toContain('/quality-workbench');
  expect(itemPaths).not.toContain('/automation-test');
});
```

- [ ] **Step 2: Run the targeted Vitest suite and verify it fails on the old one-page quality-workbench contract**

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/shellPanelContent.test.ts`

Expected: FAIL because `sectionWorkspaces.ts` still only exposes `/automation-test` and shell help still points at `/automation-test`.

- [ ] **Step 3: Update the shared workspace schema and shell help entries**

```typescript
{
  key: 'quality-workbench',
  path: '/quality-workbench',
  navLabel: '工场总览',
  navCaption: '查看质量工场能力与专项入口',
  navShort: '概',
  title: '质量工场',
  description: '围绕自动化资产、执行组织与结果基线组织工程质量能力。',
  intro: '建议先在总览判断本轮要沉淀资产、组织执行，还是复盘运行结果，再进入对应专项页。',
  menuTitle: '质量工场',
  menuHint: '覆盖自动化资产、执行组织与结果基线治理。',
  matchKeys: ['quality-workbench', 'quality-core'],
  matchLabels: ['质量工场', '测试工具'],
  cards: [
    { path: '/automation-assets', label: '自动化资产中心', description: '维护页面盘点、场景模板和计划导入导出。', short: '资', keywords: ['自动化资产中心', '页面盘点', '场景模板'] },
    { path: '/automation-execution', label: '执行中心', description: '查看执行配置、命令预览与验收注册表。', short: '执', keywords: ['执行中心', '执行配置', '验收注册表'] },
    { path: '/automation-results', label: '结果与基线中心', description: '导入运行结果、查看失败场景与质量建议。', short: '果', keywords: ['结果与基线中心', '运行结果', '视觉基线'] }
  ],
  steps: ['先沉淀自动化资产。', '再按执行中心组织回归。', '最后在结果与基线中心沉淀证据与改进建议。']
}
```

```typescript
specialRouteMetaPresets['/automation-test'] = {
  title: '自动化工场',
  description: '兼容旧入口，第一轮直接落到自动化资产中心。',
  requiresAuth: true
};
```

```typescript
{
  id: 'help-quality-workbench',
  category: 'technical',
  title: '质量工场使用说明',
  description: '先从质量工场总览进入自动化资产、执行中心或结果与基线中心，避免在同一页混用编排、执行和复盘动作。',
  path: '/quality-workbench',
  audienceKeys: ['developer', 'super-admin'],
  relatedPaths: ['/quality-workbench', '/automation-assets', '/automation-execution', '/automation-results']
}
```

- [ ] **Step 4: Re-run the targeted Vitest suite and verify the new quality-workbench contract passes**

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/shellPanelContent.test.ts`

Expected: PASS with the new `/quality-workbench` cards and `/quality-workbench` help routing.

- [ ] **Step 5: Commit the shared quality-workbench contract change**

```bash
git add spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/utils/shellPanelContent.ts spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts
git commit -m "feat: split quality workbench navigation"
```

## Task 2: Extract the Asset Center and Keep `/automation-test` Compatible

**Files:**
- Create: `spring-boot-iot-ui/src/views/AutomationAssetsView.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Create: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Write the failing view contract test for the asset page and the compatibility wrapper**

```typescript
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

function readView(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8');
}

describe('automation workbench route splits', () => {
  it('keeps the asset page focused on inventory and scenario editing', () => {
    const source = readView('AutomationAssetsView.vue');

    expect(source).toContain('<AutomationPageDiscoveryPanel');
    expect(source).toContain('<AutomationScenarioEditor');
    expect(source).not.toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });

  it('keeps the legacy automation-test view as a compatibility wrapper', () => {
    const source = readView('AutomationTestCenterView.vue');

    expect(source).toContain('<AutomationAssetsView');
    expect(source).not.toContain('<AutomationRegistryPanel');
    expect(source).not.toContain('<AutomationResultImportPanel');
  });
});
```

- [ ] **Step 2: Run the focused Vitest contract and verify it fails because the new asset view does not exist yet**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because `AutomationAssetsView.vue` does not exist and `AutomationTestCenterView.vue` is still the overloaded all-in-one page.

- [ ] **Step 3: Create the asset page, map `/automation-assets`, and reduce `AutomationTestCenterView.vue` to a compatibility shell**

```vue
<!-- spring-boot-iot-ui/src/views/AutomationAssetsView.vue -->
<template>
  <StandardPageShell class="automation-assets-view">
    <StandardWorkbenchPanel
      title="自动化资产中心"
      description="沉淀页面盘点、场景模板、执行计划与导入导出资产。"
      show-notices
    >
      <template #notices>
        <div class="automation-chip-list">
          <span>页面盘点</span>
          <span>场景模板</span>
          <span>计划导入导出</span>
          <span>覆盖分析</span>
        </div>
      </template>

      <section class="tri-grid">
        <PanelCard title="计划概况" description="先用场景粒度组织业务，再按步骤粒度沉淀可复用自动化模板。">
          <div class="quad-grid automation-plan-metrics">
            <MetricCard
              v-for="metric in planMetrics"
              :key="metric.label"
              class="automation-plan-metrics__card"
              :label="metric.label"
              :value="metric.value"
              :badge="metric.badge"
            />
          </div>
        </PanelCard>

        <PanelCard title="导出与执行" description="资产中心只负责维护计划和导出 JSON，不再承载注册表与结果导入。">
          <div class="command-box">
            <code>{{ commandPreview }}</code>
          </div>
          <StandardActionGroup gap="sm">
            <StandardButton action="confirm" @click="copyCommand">复制命令</StandardButton>
            <StandardButton action="reset" @click="downloadPlan">导出 JSON</StandardButton>
          </StandardActionGroup>
        </PanelCard>
      </section>

      <section>
        <AutomationPageDiscoveryPanel
          ref="inventoryTableRef"
          :metrics="inventoryMetrics"
          :inventory-source-text="inventorySourceText"
          :page-inventory="pageInventory"
          :build-inventory-source-label="buildInventorySourceLabel"
          :build-template-label="buildTemplateLabel"
          :is-route-covered="isRouteCovered"
          @refresh="refreshPageInventory"
          @select-uncovered="selectUncoveredPages"
          @generate-selected="generateSelectedInventoryScenarios"
          @generate-uncovered="generateUncoveredInventoryScenarios"
          @open-manual-page="openManualPageDialog"
          @selection-change="handleInventorySelectionChange"
          @remove-manual-page="removeManualPage"
        />
      </section>

      <section>
        <PanelCard title="场景编排" description="先通过模板快速起步，再替换页面路由、选择器、接口匹配与断言规则。">
          <template #actions>
            <StandardActionGroup gap="sm">
              <StandardButton action="add" @click="addScenario('pageSmoke')">新增页面冒烟模板</StandardButton>
              <StandardButton action="add" plain @click="addScenario('formSubmit')">新增表单提交模板</StandardButton>
              <StandardButton action="add" plain @click="addScenario('listDetail')">新增列表详情模板</StandardButton>
              <StandardButton action="batch" @click="showImportDialog = true">导入计划</StandardButton>
              <StandardButton action="reset" @click="resetPlan">恢复默认计划</StandardButton>
            </StandardActionGroup>
          </template>

          <AutomationScenarioEditor
            v-for="(scenario, scenarioIndex) in plan.scenarios"
            :key="scenario.key"
            :scenario="scenario"
            :scenario-index="scenarioIndex"
            :scenario-count="plan.scenarios.length"
            :scope-options="scopeOptions"
            :locator-type-options="locatorTypeOptions"
            :step-type-options="stepTypeOptions"
            @move-scenario="moveScenario(scenarioIndex, $event)"
            @copy-scenario="copyScenario(scenarioIndex)"
            @remove-scenario="removeScenario(scenarioIndex)"
            @add-initial-api="addInitialApi(scenario)"
            @add-step="addStep(scenario)"
            @move-step="moveStep(scenario, $event.stepIndex, $event.offset)"
            @remove-step="scenario.steps.splice($event.stepIndex, 1)"
            @change-step-type="handleStepTypeChange($event.step)"
            @change-screenshot-target="handleScreenshotTargetChange($event.step)"
            @add-capture="addCapture($event.step)"
          />
        </PanelCard>
      </section>

      <section class="two-column-grid">
        <PanelCard title="场景预览" description="这里用于快速查看每个场景的覆盖粒度。">
          <StandardTableToolbar
            compact
            :meta-items="[
              `当前场景 ${scenarioPreviews.length} 个`,
              `含断言 ${scenarioPreviews.filter((item) => item.hasAssertion).length} 个`
            ]"
          />
          <el-table :data="scenarioPreviews" size="small" border>
            <StandardTableTextColumn prop="key" label="编码" :min-width="160" />
            <StandardTableTextColumn prop="scope" label="范围" :width="110" />
            <StandardTableTextColumn prop="stepCount" label="步骤" :width="90" />
            <StandardTableTextColumn prop="apiCount" label="接口" :width="90" />
            <StandardTableTextColumn prop="featureCount" label="业务点" :width="100" />
          </el-table>
        </PanelCard>
        <ResponsePanel title="导出计划 JSON" description="可直接交给浏览器执行器运行。" :body="plan" />
      </section>
    </StandardWorkbenchPanel>

    <AutomationPlanImportDrawer v-model="showImportDialog" @confirm="applyImport" />
    <AutomationManualPageDrawer
      v-model="showManualPageDialog"
      :scope-options="scopeOptions"
      :template-options="inventoryTemplateOptions"
      :build-template-label="buildTemplateLabel"
      @save="saveManualPage"
    />
  </StandardPageShell>
</template>
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationTestCenterView.vue -->
<template>
  <AutomationAssetsView />
</template>

<script setup lang="ts">
import AutomationAssetsView from './AutomationAssetsView.vue';
</script>
```

```typescript
{
  path: '/automation-assets',
  name: 'automation-assets',
  component: () => import('../views/AutomationAssetsView.vue'),
  meta: routeMeta('/automation-assets')
},
{
  path: '/automation-test',
  name: 'automation-test',
  component: () => import('../views/AutomationTestCenterView.vue'),
  meta: routeMeta('/automation-test')
}
```

- [ ] **Step 4: Re-run the route/view contract test and verify the asset split is green**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS with `AutomationAssetsView.vue` owning inventory/editor panels and `AutomationTestCenterView.vue` reduced to a compatibility wrapper.

- [ ] **Step 5: Commit the asset-center extraction**

```bash
git add spring-boot-iot-ui/src/views/AutomationAssetsView.vue spring-boot-iot-ui/src/views/AutomationTestCenterView.vue spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: extract automation asset center"
```

## Task 3: Add the Execution and Results Specialty Pages

**Files:**
- Create: `spring-boot-iot-ui/src/views/AutomationExecutionView.vue`
- Create: `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
- Create: `spring-boot-iot-ui/src/composables/useAutomationExecutionWorkbench.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Extend the failing view contract test so execution/results pages must only render their own panels**

```typescript
it('keeps the execution page focused on run configuration and registry visibility', () => {
  const source = readView('AutomationExecutionView.vue');

  expect(source).toContain('<AutomationExecutionConfigPanel');
  expect(source).toContain('<AutomationRegistryPanel');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationResultImportPanel');
});

it('keeps the results page focused on imported run summaries and quality guidance', () => {
  const source = readView('AutomationResultsView.vue');

  expect(source).toContain('<AutomationResultImportPanel');
  expect(source).toContain('<AutomationSuggestionPanel');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationExecutionConfigPanel');
});
```

- [ ] **Step 2: Run the focused Vitest contract again and verify it fails because the new specialty pages are still missing**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because `AutomationExecutionView.vue` and `AutomationResultsView.vue` do not exist yet.

- [ ] **Step 3: Implement thin execution/results composables and the new specialty views**

```typescript
// spring-boot-iot-ui/src/composables/useAutomationExecutionWorkbench.ts
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';
import { useAutomationRegistryWorkbench } from './useAutomationRegistryWorkbench';

export function useAutomationExecutionWorkbench() {
  const planBuilder = useAutomationPlanBuilder();
  const registryWorkbench = useAutomationRegistryWorkbench();

  return {
    scopeOptions: planBuilder.scopeOptions,
    target: planBuilder.plan.value.target,
    commandPreview: planBuilder.commandPreview,
    copyCommand: planBuilder.copyCommand,
    registryScenarios: registryWorkbench.registryScenarios,
    registrySummary: registryWorkbench.registrySummary
  };
}
```

```typescript
// spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts
import { useAutomationPlanBuilder } from './useAutomationPlanBuilder';
import { useAutomationRegistryWorkbench } from './useAutomationRegistryWorkbench';

export function useAutomationResultsWorkbench() {
  const planBuilder = useAutomationPlanBuilder();
  const registryWorkbench = useAutomationRegistryWorkbench();

  return {
    suggestions: planBuilder.suggestions,
    importedRun: registryWorkbench.importedRun,
    importRegistryRunSummary: registryWorkbench.importRegistryRunSummary,
    clearImportedRun: registryWorkbench.clearImportedRun
  };
}
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationExecutionView.vue -->
<template>
  <StandardPageShell class="automation-execution-view">
    <StandardWorkbenchPanel
      title="执行中心"
      description="统一查看执行配置、命令预览和验收注册表依赖关系。"
    >
      <section class="two-column-grid">
        <AutomationExecutionConfigPanel :target="target" :scope-options="scopeOptions" />
        <PanelCard title="执行方式" description="导出的 JSON 计划可直接交给 scripts/auto 执行器运行。">
          <div class="command-box">
            <code>{{ commandPreview }}</code>
          </div>
          <StandardActionGroup gap="sm">
            <StandardButton action="confirm" @click="copyCommand">复制命令</StandardButton>
          </StandardActionGroup>
        </PanelCard>
      </section>

      <section>
        <AutomationRegistryPanel :scenarios="registryScenarios" :summary="registrySummary" />
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationResultsView.vue -->
<template>
  <StandardPageShell class="automation-results-view">
    <StandardWorkbenchPanel
      title="结果与基线中心"
      description="统一导入运行结果、查看失败场景并维护质量建议与基线证据。"
    >
      <section class="two-column-grid">
        <AutomationResultImportPanel :imported-run="importedRun" @import-json="importRegistryRunSummary" @clear="clearImportedRun" />
        <AutomationSuggestionPanel :suggestions="suggestions" />
      </section>
    </StandardWorkbenchPanel>
  </StandardPageShell>
</template>
```

```typescript
{
  path: '/automation-execution',
  name: 'automation-execution',
  component: () => import('../views/AutomationExecutionView.vue'),
  meta: routeMeta('/automation-execution')
},
{
  path: '/automation-results',
  name: 'automation-results',
  component: () => import('../views/AutomationResultsView.vue'),
  meta: routeMeta('/automation-results')
}
```

- [ ] **Step 4: Re-run the view contract test and the workspace contract test to verify the split stays green**

Run: `npm test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/shellPanelContent.test.ts`

Expected: PASS with the asset/editor panels isolated from execution and result panels.

- [ ] **Step 5: Commit the execution/results page split**

```bash
git add spring-boot-iot-ui/src/views/AutomationExecutionView.vue spring-boot-iot-ui/src/views/AutomationResultsView.vue spring-boot-iot-ui/src/composables/useAutomationExecutionWorkbench.ts spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add quality execution and result pages"
```

## Task 4: Update Menu Seeds, Docs, and Final Verification

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the failing text-contract checks for the new menu codes and routes**

```powershell
Select-String -Path 'sql/init-data.sql' -SimpleMatch -Pattern 'system:automation-assets','system:automation-execution','system:automation-results'
Select-String -Path 'docs/21-业务功能清单与验收标准.md' -SimpleMatch -Pattern '/automation-assets','/automation-execution','/automation-results'
```

Expected: no matches, because the quality workbench still only documents `/automation-test`.

- [ ] **Step 2: Update the menu seeds so the quality workbench can authorize all three new specialty pages**

```sql
(93003009, 1, 93000005, '自动化资产中心', 'system:automation-assets', '/automation-assets', 'AutomationAssetsView', 'monitor', '{"caption":"页面盘点、场景模板与计划导入导出"}', 51, 1, 1, '/automation-assets', 'system:automation-assets', 51, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003012, 1, 93000005, '执行中心', 'system:automation-execution', '/automation-execution', 'AutomationExecutionView', 'promotion', '{"caption":"执行配置、命令预览与验收注册表"}', 52, 1, 1, '/automation-execution', 'system:automation-execution', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003013, 1, 93000005, '结果与基线中心', 'system:automation-results', '/automation-results', 'AutomationResultsView', 'data-analysis', '{"caption":"运行结果导入、失败复盘与基线证据治理"}', 53, 1, 1, '/automation-results', 'system:automation-results', 53, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003014, 1, 93000005, '自动化工场（兼容入口）', 'system:automation-test', '/automation-test', 'AutomationTestCenterView', 'monitor', '{"caption":"兼容旧入口，第一轮直接落到自动化资产中心"}', 54, 1, 1, '/automation-test', 'system:automation-test', 54, 1, 1, 1, NOW(), 1, NOW(), 0),
```

- [ ] **Step 3: Update the quality-workbench docs to match the new page responsibilities**

```markdown
| 质量工场 | 自动化资产中心 / 执行中心 / 结果与基线中心 | 开发人员、测试人员、超级管理员 |

- `质量工场总览` 当前只负责入口导航、最近运行与待治理摘要，不再承载场景编排、执行配置或结果导入正文。
- `自动化资产中心` 负责页面盘点、场景模板、计划导入导出与覆盖分析。
- `执行中心` 负责执行配置、命令预览与验收注册表依赖关系。
- `结果与基线中心` 负责运行结果导入、失败场景汇总、质量建议和基线证据入口。
- `/automation-test` 当前仅保留兼容入口，第一轮直接落到自动化资产中心。
```

- [ ] **Step 4: Re-run the text-contract checks, the focused Vitest suite, and the repository quality gate**

Run: `Select-String -Path 'sql/init-data.sql' -SimpleMatch -Pattern 'system:automation-assets','system:automation-execution','system:automation-results'`

Expected: three new menu-code matches plus the updated compatibility `system:automation-test` row.

Run: `npm test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/shellPanelContent.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS.

Run: `node scripts/run-quality-gates.mjs`

Expected: `All local minimum quality gates passed` or a concrete environment/tooling blocker that does not come from the quality-workbench split itself.

- [ ] **Step 5: Commit the menu-seed and documentation rollout**

```bash
git add sql/init-data.sql docs/02-业务功能与流程说明.md docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document quality workbench split"
```

## Self-Review

### Spec coverage

The plan covers every confirmed section from `docs/superpowers/specs/2026-04-02-quality-workbench-restructure-design.md`:

1. Information architecture: Task 1 plus Tasks 2-3 split the overview, asset, execution, and result pages.
2. Routing/navigation: Tasks 1-3 add the new cards, route meta, and router entries.
3. Compatibility strategy: Task 2 keeps `/automation-test` alive as the asset-center wrapper; Task 4 keeps the menu seed alive.
4. Menu/data rollout: Task 4 updates `sql/init-data.sql`.
5. Acceptance/documentation: Task 4 updates `02 / 05 / 08 / 21` and runs the focused suite plus `node scripts/run-quality-gates.mjs`.

### Placeholder scan

No `TODO`, `TBD`, “implement later”, “similar to Task N”, or unnamed verification steps remain.

### Type consistency

The route names, menu codes, and view file names are consistent across all tasks:

- `/automation-assets` <-> `AutomationAssetsView.vue` <-> `system:automation-assets`
- `/automation-execution` <-> `AutomationExecutionView.vue` <-> `system:automation-execution`
- `/automation-results` <-> `AutomationResultsView.vue` <-> `system:automation-results`
- `/automation-test` remains the compatibility path and maps to the asset-center shell in Task 2 and the compatibility menu row in Task 4.
