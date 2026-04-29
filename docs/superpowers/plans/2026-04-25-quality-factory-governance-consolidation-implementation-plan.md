# Quality Factory Governance Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consolidate quality factory into `业务验收台 + 自动化治理台`, retire the old RD/execution/results first-level entrances, and move engineering workflows into one governance workbench with query-driven tabs.

**Architecture:** Keep `/business-acceptance` and `/business-acceptance/results/:runId` as the business-facing flow, then introduce `/automation-governance` as the only engineering-facing quality route. Reuse the existing composables and panels for inventory, templates, plans, handoff, execution, and evidence, but reorganize them behind one governance shell driven by `tab`, `assetTab`, and `runId` query state. Update shared workspace metadata, route permissions, shell navigation, and menu seeds in the same batch so the codebase no longer describes the retired four-entrance model.

**Tech Stack:** Vue 3 SFCs, Vue Router, Pinia, Vitest, Element Plus, SQL seed data, Markdown docs

---

### Task 1: Retire the old quality-factory IA in shared schema and route tests

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/permissionStoreRouteGuard.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`

- [ ] **Step 1: Write the failing IA/schema assertions**

```ts
it('maps quality workbench to business acceptance plus automation governance only', () => {
  const config = getSectionHomeConfigByPath('/quality-workbench');

  expect(config?.cards.map((item) => item.path)).toEqual([
    '/business-acceptance',
    '/automation-governance'
  ]);
  expect(getRouteMetaPreset('/automation-governance')).toMatchObject({
    title: '自动化治理台',
    description: '统一承接资产编排、执行配置与结果证据。'
  });
  expect(getRouteMetaPreset('/rd-workbench')).toBeNull();
  expect(getRouteMetaPreset('/automation-results')).toBeNull();
});

it('grants governance access from the consolidated quality menu only', () => {
  const permissionStore = usePermissionStore();
  permissionStore.setAccessToken('token');
  permissionStore.setAuthContext(
    createAuthContext({
      roleCodes: ['DEVELOPER_STAFF'],
      roles: [{ id: 31, roleCode: 'DEVELOPER_STAFF', roleName: '开发人员' }],
      menus: [
        {
          id: 93000005,
          menuName: '质量工场',
          menuCode: 'quality-workbench',
          path: '',
          type: 0,
          children: [
            {
              id: 93003015,
              parentId: 93000005,
              menuName: '自动化治理台',
              menuCode: 'system:automation-governance',
              path: '/automation-governance',
              type: 1,
              children: []
            }
          ]
        }
      ]
    })
  );

  expect(permissionStore.hasRoutePermission('/automation-governance')).toBe(true);
  expect(permissionStore.hasRoutePermission('/rd-workbench')).toBe(false);
  expect(permissionStore.hasRoutePermission('/automation-execution')).toBe(false);
});
```

- [ ] **Step 2: Run the focused schema tests and verify RED**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/permissionStoreRouteGuard.test.ts src/__tests__/utils/shellPanelContent.test.ts
```

Expected: FAIL because `sectionWorkspaces.ts` and the route permission expansion still expose `/rd-workbench`, `/automation-execution`, and `/automation-results`.

- [ ] **Step 3: Implement the consolidated workspace schema, route registration, and overview cards**

```ts
// spring-boot-iot-ui/src/utils/sectionWorkspaces.ts
{
  key: 'quality-workbench',
  path: '/quality-workbench',
  navLabel: '工场总览',
  navCaption: '查看质量工场能力与两类工作入口',
  navShort: '概',
  title: '质量工场',
  description: '围绕业务验收和自动化治理组织质量能力。',
  intro: '业务角色先进入业务验收台，研发、测试和管理员再进入自动化治理台处理资产、执行和证据。',
  menuTitle: '质量工场',
  menuHint: '覆盖业务验收与自动化治理。',
  matchKeys: ['quality-workbench', 'quality-core'],
  matchLabels: ['质量工场', '测试工具'],
  cards: [
    { path: '/business-acceptance', label: '业务验收台', description: '按交付清单选择预置验收包并一键运行业务验收。', short: '验' },
    { path: '/automation-governance', label: '自动化治理台', description: '统一承接资产编排、执行配置与结果证据。', short: '治' }
  ],
  steps: ['业务验收优先进入业务验收台。', '研发、测试和管理员进入自动化治理台。', '统一在治理台内完成资产、执行与证据闭环。']
}
```

```ts
// spring-boot-iot-ui/src/router/index.ts
{
  path: '/automation-governance',
  name: 'automation-governance',
  component: () => import('../views/AutomationGovernanceWorkbenchView.vue'),
  meta: routeMeta('/automation-governance')
}
```

```ts
// spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue
const qualityCards = [
  {
    path: '/business-acceptance',
    label: '业务验收台',
    description: '按交付清单选择预置验收包并一键运行业务验收。',
    short: '验'
  },
  {
    path: '/automation-governance',
    label: '自动化治理台',
    description: '统一承接资产编排、执行配置与结果证据。',
    short: '治'
  }
] as const;
```

- [ ] **Step 4: Re-run the focused schema tests and verify GREEN**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/permissionStoreRouteGuard.test.ts src/__tests__/utils/shellPanelContent.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit the IA/schema consolidation**

```bash
git add spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/utils/permissionStoreRouteGuard.test.ts spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue
git commit -m "feat: consolidate quality workbench routing"
```

### Task 2: Add the governance query model and the new governance shell

**Files:**
- Create: `spring-boot-iot-ui/src/utils/automationGovernance.ts`
- Create: `spring-boot-iot-ui/src/__tests__/utils/automationGovernance.test.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationGovernanceWorkbench.ts`
- Create: `spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Write the failing query-model and view-shell tests**

```ts
// spring-boot-iot-ui/src/__tests__/utils/automationGovernance.test.ts
import { describe, expect, it } from 'vitest';
import { normalizeAutomationGovernanceQuery } from '@/utils/automationGovernance';

describe('automationGovernance query model', () => {
  it('defaults to assets and inventory when query is empty', () => {
    expect(normalizeAutomationGovernanceQuery({})).toEqual({
      tab: 'assets',
      assetTab: 'inventory',
      runId: ''
    });
  });

  it('forces evidence tab when runId is present', () => {
    expect(normalizeAutomationGovernanceQuery({
      tab: 'execution',
      runId: '20260425123456'
    })).toEqual({
      tab: 'evidence',
      assetTab: 'inventory',
      runId: '20260425123456'
    });
  });
});
```

```ts
// spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
it('keeps quality workbench focused on business acceptance and governance navigation', () => {
  const source = readView('QualityWorkbenchLandingView.vue');

  expect(source).toContain('/business-acceptance');
  expect(source).toContain('/automation-governance');
  expect(source).not.toContain('/rd-workbench');
  expect(source).not.toContain('/automation-execution');
  expect(source).not.toContain('/automation-results');
});

it('registers the consolidated automation-governance route', () => {
  const source = readRouter();

  expect(source).toContain("path: '/automation-governance'");
  expect(source).not.toContain("path: '/rd-workbench'");
  expect(source).not.toContain("path: '/automation-execution'");
  expect(source).not.toContain("path: '/automation-results'");
});
```

- [ ] **Step 2: Run the focused governance-shell tests and verify RED**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/utils/automationGovernance.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: FAIL because `automationGovernance.ts` and `AutomationGovernanceWorkbenchView.vue` do not exist yet and the router still contains the retired routes.

- [ ] **Step 3: Implement the query utility, composable, and governance shell**

```ts
// spring-boot-iot-ui/src/utils/automationGovernance.ts
const PRIMARY_TABS = ['assets', 'execution', 'evidence'] as const;
const ASSET_TABS = ['inventory', 'templates', 'plans', 'handoff'] as const;

export function normalizeAutomationGovernanceQuery(query: Record<string, unknown>) {
  const rawRunId = typeof query.runId === 'string' ? query.runId.trim() : '';
  const rawTab = typeof query.tab === 'string' ? query.tab.trim() : '';
  const rawAssetTab = typeof query.assetTab === 'string' ? query.assetTab.trim() : '';
  const tab = rawRunId
    ? 'evidence'
    : (PRIMARY_TABS.includes(rawTab as never) ? rawTab : 'assets');
  const assetTab = ASSET_TABS.includes(rawAssetTab as never) ? rawAssetTab : 'inventory';

  return { tab, assetTab, runId: rawRunId };
}
```

```ts
// spring-boot-iot-ui/src/composables/useAutomationGovernanceWorkbench.ts
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { normalizeAutomationGovernanceQuery } from '../utils/automationGovernance';

export function useAutomationGovernanceWorkbench() {
  const route = useRoute();
  const router = useRouter();
  const state = computed(() => normalizeAutomationGovernanceQuery(route.query));

  async function updateQuery(patch: Partial<{ tab: string; assetTab: string; runId: string }>) {
    const next = { ...state.value, ...patch };
    await router.replace({
      path: '/automation-governance',
      query: {
        tab: next.tab,
        assetTab: next.tab === 'assets' ? next.assetTab : undefined,
        runId: next.tab === 'evidence' && next.runId ? next.runId : undefined
      }
    });
  }

  return {
    state,
    selectTab: (tab: string) => updateQuery({ tab, runId: tab === 'evidence' ? state.value.runId : '' }),
    selectAssetTab: (assetTab: string) => updateQuery({ tab: 'assets', assetTab }),
    openEvidenceRun: (runId: string) => updateQuery({ tab: 'evidence', runId })
  };
}
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue -->
<section class="automation-governance-workbench__hero">
  <div class="automation-governance-workbench__hero-copy">
    <h2>自动化治理台</h2>
    <p>把资产编排、执行配置和结果证据收进同一个治理闭环。</p>
  </div>
</section>

<section class="automation-governance-workbench__tabs">
  <el-segmented :model-value="state.tab" :options="primaryTabOptions" @change="selectTab(String($event))" />
  <el-segmented
    v-if="state.tab === 'assets'"
    :model-value="state.assetTab"
    :options="assetTabOptions"
    @change="selectAssetTab(String($event))"
  />
</section>
```

- [ ] **Step 4: Re-run the focused governance-shell tests and verify GREEN**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/utils/automationGovernance.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit the governance shell**

```bash
git add spring-boot-iot-ui/src/utils/automationGovernance.ts spring-boot-iot-ui/src/__tests__/utils/automationGovernance.test.ts spring-boot-iot-ui/src/composables/useAutomationGovernanceWorkbench.ts spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add automation governance shell"
```

### Task 3: Move execution and evidence flows under governance and update runId jumps

**Files:**
- Create: `spring-boot-iot-ui/src/components/AutomationGovernanceExecutionWorkspace.vue`
- Create: `spring-boot-iot-ui/src/components/AutomationGovernanceEvidenceWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue`
- Modify: `spring-boot-iot-ui/src/components/BusinessAcceptanceResultSummaryPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`

- [ ] **Step 1: Write the failing deep-link and governance-evidence tests**

```ts
// spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts
it('routes governance evidence entry through automation-governance', async () => {
  const workbench = useBusinessAcceptanceWorkbench();

  await workbench.goToAutomationGovernanceEvidence('20260425101010');

  expect(mockRouter.push).toHaveBeenCalledWith({
    path: '/automation-governance',
    query: {
      tab: 'evidence',
      runId: '20260425101010'
    }
  });
});
```

```ts
// spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
expect(mockRouter.push).toHaveBeenCalledWith('/automation-governance?tab=evidence&runId=20260418193000');
```

- [ ] **Step 2: Run the focused evidence/deep-link tests and verify RED**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
```

Expected: FAIL because the business acceptance composable and device-onboarding jump still target `/automation-results`.

- [ ] **Step 3: Implement governance evidence navigation and embed execution/evidence workspaces**

```ts
// spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts
async function goToAutomationGovernanceEvidence(runId: string) {
  await router.push({
    path: '/automation-governance',
    query: {
      tab: 'evidence',
      runId
    }
  });
}
```

```vue
<!-- spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue -->
<BusinessAcceptanceResultSummaryPanel
  :status="result.status"
  :passed-module-count="result.passedModuleCount"
  :failed-module-count="result.failedModuleCount"
  :failed-module-names="result.failedModuleNames"
  :duration-text="result.durationText"
  :show-governance-entry="hasGovernanceAccess"
  @open-automation-results="goToAutomationGovernanceEvidence(result.runId)"
/>
```

```ts
// spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue
return `/automation-governance?tab=evidence&runId=${row.acceptance.runId}`;
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue -->
<AutomationGovernanceExecutionWorkspace v-if="state.tab === 'execution'" />
<AutomationGovernanceEvidenceWorkspace
  v-else-if="state.tab === 'evidence'"
  :initial-run-id="state.runId"
  @open-run="openEvidenceRun"
/>
```

- [ ] **Step 4: Re-run the focused evidence/deep-link tests and verify GREEN**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit the governance evidence/execution migration**

```bash
git add spring-boot-iot-ui/src/components/AutomationGovernanceExecutionWorkspace.vue spring-boot-iot-ui/src/components/AutomationGovernanceEvidenceWorkspace.vue spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue spring-boot-iot-ui/src/components/BusinessAcceptanceResultSummaryPanel.vue spring-boot-iot-ui/src/views/DeviceOnboardingWorkbenchView.vue spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
git commit -m "feat: move quality evidence flows into governance"
```

### Task 4: Embed the four RD authoring slices under governance assets

**Files:**
- Create: `spring-boot-iot-ui/src/components/AutomationGovernanceAssetsWorkspace.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Write the failing governance-assets view assertions**

```ts
it('keeps the governance assets workspace focused on four authoring slices', () => {
  const source = readView('AutomationGovernanceWorkbenchView.vue');

  expect(source).toContain('资产编排');
  expect(source).toContain('inventory');
  expect(source).toContain('templates');
  expect(source).toContain('plans');
  expect(source).toContain('handoff');
  expect(source).not.toContain("path: '/rd-workbench'");
});
```

- [ ] **Step 2: Run the focused governance view test and verify RED**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: FAIL because the governance view does not yet render the four internal authoring slices.

- [ ] **Step 3: Implement the assets workspace by reusing the existing authoring composables**

```vue
<!-- spring-boot-iot-ui/src/components/AutomationGovernanceAssetsWorkspace.vue -->
<template>
  <section class="automation-governance-assets">
    <div class="automation-governance-assets__summary">
      <MetricCard v-for="metric in assetMetrics" :key="metric.label" size="compact" :label="metric.label" :value="metric.value" :badge="metric.badge" />
    </div>

    <template v-if="activeAssetTab === 'inventory'">
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
      <AutomationManualPageDrawer
        v-model="showManualPageDialog"
        :scope-options="scopeOptions"
        :template-options="inventoryTemplateOptions"
        :build-template-label="buildTemplateLabel"
        @save="saveManualPage"
      />
    </template>

    <template v-else-if="activeAssetTab === 'templates'">
      <PanelCard title="页面冒烟模板" description="适合为新页面快速建立最小可执行脚手架。">
        <ul class="template-focus-list">
          <li>页面进入</li>
          <li>就绪断言</li>
          <li>截图基线</li>
        </ul>
        <StandardButton v-permission="'system:rd-automation-templates:add-page-smoke'" action="add" @click="addScenario('pageSmoke')">新增页面冒烟模板</StandardButton>
      </PanelCard>
      <PanelCard title="表单提交模板" description="适合新增或改造表单页的研发自测起步。">
        <ul class="template-focus-list">
          <li>表单填写</li>
          <li>提交动作</li>
          <li>接口回执</li>
        </ul>
        <StandardButton v-permission="'system:rd-automation-templates:add-form-submit'" action="add" @click="addScenario('formSubmit')">新增表单提交模板</StandardButton>
      </PanelCard>
      <PanelCard title="列表详情模板" description="适合列表查询、详情抽屉与行级动作页面。">
        <ul class="template-focus-list">
          <li>列表筛选</li>
          <li>行级动作</li>
          <li>详情抽屉</li>
        </ul>
        <StandardButton v-permission="'system:rd-automation-templates:add-list-detail'" action="add" @click="addScenario('listDetail')">新增列表详情模板</StandardButton>
      </PanelCard>
    </template>

    <template v-else-if="activeAssetTab === 'plans'">
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
      <AutomationPlanImportDrawer
        v-model="showImportDialog"
        @confirm="applyImport"
      />
      <ResponsePanel
        title="当前计划快照"
        description="这里保留正式计划 JSON 快照，便于交给执行配置与交付打包共用。"
        :body="plan"
      />
    </template>

    <template v-else>
      <ResponsePanel title="交付快照" :body="handoffSummary" />
    </template>
  </section>
</template>
```

```vue
<!-- spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue -->
<AutomationGovernanceAssetsWorkspace
  v-if="state.tab === 'assets'"
  :active-asset-tab="state.assetTab"
/>
```

- [ ] **Step 4: Re-run the focused governance view test and verify GREEN**

Run from `spring-boot-iot-ui`:

```bash
./node_modules/.bin/vitest --run src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS

- [ ] **Step 5: Commit the assets consolidation**

```bash
git add spring-boot-iot-ui/src/components/AutomationGovernanceAssetsWorkspace.vue spring-boot-iot-ui/src/views/AutomationGovernanceWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: fold rd authoring into governance assets"
```

### Task 5: Collapse quality-factory menu seeds, docs, and final verification

**Files:**
- Modify: `sql/init-data.sql`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Write the failing documentation/menu expectations**

```md
- 质量工场只保留 `/quality-workbench` 总览、`/business-acceptance` 业务验收台、`/business-acceptance/results/:runId` 业务结果页、`/automation-governance` 自动化治理台。
- 自动化治理台内部统一承接 `资产编排 / 执行配置 / 结果证据`。
- `研发工场 / 执行中心 / 结果与基线中心 / 自动化资产中心 / 自动化工场` 不再作为独立入口存在。
```

```sql
(93003015, 1, 93000005, '自动化治理台', 'system:automation-governance', '/automation-governance', 'AutomationGovernanceWorkbenchView', 'operation', '{"caption":"资产编排、执行配置与结果证据统一入口"}', 52, 1, 1, '/automation-governance', 'system:automation-governance', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
```

- [ ] **Step 2: Apply the menu-seed and doc updates**

```sql
-- sql/init-data.sql
-- remove page rows 93003009, 93003012, 93003013, 93003014, 93003016, 93003017, 93003018, 93003019
-- keep action permissions, but re-parent them under 93003015 so all RD/execution/evidence buttons still live beneath the new governance page
(93003015, 1, 93000005, '自动化治理台', 'system:automation-governance', '/automation-governance', 'AutomationGovernanceWorkbenchView', 'operation', '{"caption":"资产编排、执行配置与结果证据统一入口"}', 52, 1, 1, '/automation-governance', 'system:automation-governance', 52, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003703, 1, 93003015, '刷新盘点', 'system:rd-automation-inventory:refresh', '', '', '', '{"caption":"刷新页面盘点结果"}', 5303, 2, 2, '', 'system:rd-automation-inventory:refresh', 5303, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003714, 1, 93003015, '复制执行命令', 'system:automation-execution:copy-command', '', '', '', '{"caption":"复制执行中心命令"}', 5714, 2, 2, '', 'system:automation-execution:copy-command', 5714, 1, 1, 1, NOW(), 1, NOW(), 0),
(93003716, 1, 93003015, '刷新结果台账', 'system:automation-results:refresh', '', '', '', '{"caption":"刷新自动化结果运行台账"}', 5816, 2, 2, '', 'system:automation-results:refresh', 5816, 1, 1, 1, NOW(), 1, NOW(), 0),
```

```md
<!-- docs/05-自动化测试与质量保障.md -->
- 质量工场当前收口为 `/quality-workbench` 总览 + `/business-acceptance` 业务验收台 + `/business-acceptance/results/:runId` + `/automation-governance` 自动化治理台。
- 自动化治理台内部统一按 `资产编排 / 执行配置 / 结果证据` 组织；`runId` 深链统一进入 `tab=evidence`。
```

- [ ] **Step 3: Run the full verification set**

Run from repo root:

```bash
cd spring-boot-iot-ui && ./node_modules/.bin/vitest --run src/__tests__/views/AutomationWorkbenchViews.test.ts src/__tests__/utils/automationGovernance.test.ts src/__tests__/utils/sectionHomes.test.ts src/__tests__/utils/permissionStoreRouteGuard.test.ts src/__tests__/utils/shellPanelContent.test.ts src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
npm run build
cd ..
node scripts/docs/check-topology.mjs
git diff --check
```

Expected:

- Vitest PASS
- `npm run build` exits `0`
- `Document topology check passed.`
- `git diff --check` prints no errors

- [ ] **Step 4: Commit docs and seed consolidation**

```bash
git add sql/init-data.sql README.md AGENTS.md docs/02-业务功能与流程说明.md docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: consolidate quality factory governance model"
```

- [ ] **Step 5: Final review against the approved spec**

Check the completed work against:

```text
docs/superpowers/specs/2026-04-25-quality-factory-governance-consolidation-design.md
```

Confirm all of the following before closing the task:

1. `/quality-workbench` only exposes `业务验收台` and `自动化治理台`
2. `/automation-governance` is the only engineering quality-factory route
3. governance tabs cover `assets / execution / evidence`
4. retired old routes are gone from router, docs, and menu seeds
5. `runId` links land on governance evidence
