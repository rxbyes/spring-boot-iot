# Products Semantics And IoT Access Decision Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep `/products` as the product ledger-first `产品定义中心` while making its governance scope explicit, and harden `接入智维` into a visible decision tree from validation to diagnosis to governance correction.

**Architecture:** Reuse the existing shared `sectionWorkspaces` schema and `iotAccessDiagnostics` context as the single copy source. First align the `接入智维` overview and shared metadata, then tighten `/products` wording and governance notices, then update each diagnosis page so it only states “current node + next step”, and finally sync the existing docs in place.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, existing `Standard*` shared components, markdown docs under `docs/`.

---

## File Structure

- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
  - Keep `接入智维` overview metadata authoritative for the new decision-tree wording and updated `/products` card description.
- `spring-boot-iot-ui/src/views/SectionLandingView.vue`
  - Render the decision-tree summary from shared metadata instead of the current generic “页面入口” shell.
- `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
  - Lock the shared workspace metadata wording.
- `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`
  - Lock the rendered `接入智维` overview copy and decision tree.
- `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`
  - Update `/products` description, governance notice summary, and restored-diagnostic inline guidance.
- `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
  - Lock `/products` as “ledger first, governance explicit”.
- `spring-boot-iot-ui/src/views/reportingDiagnosis.ts`
  - Make chain-validation verdict copy describe the diagnosis start-node and next-step split.
- `spring-boot-iot-ui/src/views/reportingRecentDiagnosis.ts`
  - Keep recent-session action labels aligned with the same decision tree.
- `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
  - Update the `ReportingStatusHeader` copy to show the start-node semantics and next-step actions.
- `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`
  - Lock the updated validation-start wording and cross-page action labels.
- `spring-boot-iot-ui/src/views/MessageTraceView.vue`
  - Reword the page description and inline-state guidance as “main pipeline replay + next step”.
- `spring-boot-iot-ui/src/views/AuditLogView.vue`
  - Reword the system-error mode description and inline-state guidance as “background exception check + next step”.
- `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
  - Reword the page description and inline-state guidance as “data/raw-response validation + next step”.
- `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
  - Align failure-archive advice and jump labels with the same tree end-state semantics.
- `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
  - Lock trace-page source-context and wording changes.
- `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
  - Lock system-log source-context wording changes.
- `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
  - Lock file-debug source-context wording changes.
- `spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts`
  - Lock failure-archive advice wording and governance jump labels.
- `spring-boot-iot-ui/src/utils/shellPanelContent.ts`
  - Update shell help/FAQ summaries that still describe the old generic access wording.
- `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts`
  - Keep shell panel summaries aligned with the new semantics.
- `README.md`
  - Update the top-level product and access-workbench wording.
- `docs/02-业务功能与流程说明.md`
  - Update authoritative business semantics for `/products` and `接入智维`.
- `docs/11-可观测性、日志追踪与消息通知治理.md`
  - Update the troubleshooting flow so the decision tree is explicit.
- `docs/15-前端优化与治理计划.md`
  - Record the page-structure rule: complete tree only on `/device-access`, diagnosis pages only show current node + next step.
- `docs/08-变更记录与技术债清单.md`
  - Log the behavior change and verification evidence.

## Task 1: Align Shared IoT Access Overview Metadata And Landing UI

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/views/SectionLandingView.vue`

- [ ] **Step 1: Extend the overview tests to lock the target copy**

Update `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts` so the `iot-access` config locks the new descriptions:

```ts
expect(config?.description).toBe('接入智维总览负责回答先去哪、再去哪、最后去哪修。');
expect(config?.hubJudgement).toBe('先做链路验证，再按证据分流到诊断页，最后回产品或设备治理修正。');
expect(config?.hubLeadTitle).toBe('标准排障路径');
expect(config?.hubLeadDescription).toBe('标准排障路径固定为：链路验证 -> 链路追踪 / 异常观测 / 数据校验 -> 产品定义中心 / 设备资产中心。');
expect(config?.cards.find((item) => item.path === '/products')?.description).toBe('维护产品定义，并承接契约治理、版本治理与风险目录入口。');
expect(config?.cards.find((item) => item.path === '/reporting')?.description).toBe('排障起点：先发起模拟验证，再决定进入哪一条诊断分支。');
```

Update `spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts` to lock the rendered decision tree instead of the generic `页面入口` wording:

```ts
expect(wrapper.text()).toContain('接入智维总览负责回答先去哪、再去哪、最后去哪修');
expect(wrapper.text()).toContain('标准排障路径');
expect(wrapper.text()).toContain('链路验证中心');
expect(wrapper.text()).toContain('链路追踪台');
expect(wrapper.text()).toContain('异常观测台');
expect(wrapper.text()).toContain('数据校验台');
expect(wrapper.text()).toContain('产品定义中心');
expect(wrapper.text()).toContain('设备资产中心');
expect(wrapper.text()).not.toContain('页面入口');
```

- [ ] **Step 2: Run the overview tests and verify the current UI fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/SectionLandingView.test.ts
```

Expected: FAIL because `SectionLandingView.vue` still renders the generic `页面入口 / 按职责筛选入口，再进入对应业务页。` shell and does not surface the decision tree from `sectionWorkspaces.ts`.

- [ ] **Step 3: Implement the metadata-driven landing panel**

Update `spring-boot-iot-ui/src/views/SectionLandingView.vue` so the workbench title/description come from the config, and render the decision tree block above the filtered entry list:

```vue
<StandardWorkbenchPanel
  :title="config?.title || '接入智维'"
  :description="config?.description || '接入智维总览负责回答先去哪、再去哪、最后去哪修。'"
  show-filters
>
  <template #filters>
    <StandardListFilterHeader :model="{ keyword: landingKeyword }">
      <template #primary>
        <el-form-item>
          <el-input
            v-model="landingKeyword"
            placeholder="搜索页面名称或职责关键词"
            clearable
            prefix-icon="Search"
          />
        </el-form-item>
      </template>
    </StandardListFilterHeader>
  </template>

  <section v-if="config?.hubJudgement" class="section-landing__decision-tree">
    <p class="section-landing__decision-eyebrow">{{ config.hubLeadTitle }}</p>
    <h3>{{ config.hubJudgement }}</h3>
    <p>{{ config.hubLeadDescription }}</p>
    <ol class="section-landing__decision-steps">
      <li v-for="step in config.steps" :key="step">{{ step }}</li>
    </ol>
  </section>

  <div v-if="groupedCards[activeKey]?.length" class="section-landing__entry-list">
```

Keep `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts` authoritative for the `iot-access` text:

```ts
description: '接入智维总览负责回答先去哪、再去哪、最后去哪修。',
intro: '先在总览判断当前位于排障树哪一段，再进入对应诊断页或治理页。',
hubJudgement: '先做链路验证，再按证据分流到诊断页，最后回产品或设备治理修正。',
hubLeadTitle: '标准排障路径',
hubLeadDescription: '标准排障路径固定为：链路验证 -> 链路追踪 / 异常观测 / 数据校验 -> 产品定义中心 / 设备资产中心。',
```

- [ ] **Step 4: Re-run the overview tests and verify they pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/SectionLandingView.test.ts
```

Expected: PASS with the decision-tree wording rendered from the shared config.

- [ ] **Step 5: Commit the overview alignment**

Run:

```bash
git add spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/views/SectionLandingView.vue spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/views/SectionLandingView.test.ts
git commit -m "refactor: harden iot access overview decision tree"
```

## Task 2: Make `/products` Ledger-First But Governance-Explicit

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`

- [ ] **Step 1: Add failing `/products` semantics assertions**

Extend `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts` so the page now locks the upgraded description and inline guidance:

```ts
expect(wrapper.text()).toContain('产品定义中心');
expect(wrapper.text()).toContain('统一维护产品定义，并承接契约治理、版本治理与风险目录入口。');
expect(wrapper.text()).toContain('当前页同时承接产品定义、契约治理、版本治理与风险目录入口。');
```

For the restored diagnostic context test, update the expectation from the old generic product-contract hint to the new governance-baseline guidance:

```ts
expect(wrapper.text()).toContain('来自异常观测台');
expect(wrapper.text()).toContain('Trace trace-001');
expect(wrapper.text()).toContain('优先核对产品定义与契约基线');
```

- [ ] **Step 2: Run the product page test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: FAIL because the page still says `统一维护产品台账、协议绑定与接入契约。` and the inline hint still says `优先核对产品契约、协议编码与物模型完整性。`

- [ ] **Step 3: Implement the `/products` wording changes**

Update the workbench shell in `spring-boot-iot-ui/src/views/ProductWorkbenchView.vue`:

```vue
<StandardWorkbenchPanel
  title="产品定义中心"
  description="统一维护产品定义，并承接契约治理、版本治理与风险目录入口。"
  show-filters
  :show-applied-filters="hasAppliedFilters"
  show-notices
  show-toolbar
  :show-inline-state="showListInlineState"
  show-pagination
>
```

Tighten the governance summary and restored-diagnostic copy:

```ts
const diagnosticEntryMessage = computed(() => {
  if (!diagnosticContext.value) {
    return '';
  }
  const sourceLabel = describeDiagnosticSource(diagnosticContext.value.sourcePage);
  const traceLabel = diagnosticContext.value.traceId ? `Trace ${diagnosticContext.value.traceId}` : '';
  return [
    sourceLabel ? `来自${sourceLabel}` : '',
    traceLabel,
    '优先核对产品定义与契约基线，必要时进入契约字段继续完成治理修正。'
  ].filter(Boolean).join(' · ');
});

const governanceSummaryTitle = computed(() => {
  const focusProduct = governanceFocusProduct.value;
  if (!focusProduct) {
    return '当前页同时承接产品定义、契约治理、版本治理与风险目录入口。请选择产品后查看当前聚焦产品的治理进度。';
  }
  return `当前聚焦产品 ${focusProduct.productName || focusProduct.productKey}；列表仍用于选定产品，治理入口继续收口在契约字段工作区。`;
});
```

- [ ] **Step 4: Re-run the product page test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/ProductWorkbenchView.test.ts
```

Expected: PASS with the updated description, notice summary, and restored-context guidance.

- [ ] **Step 5: Commit the `/products` semantics change**

Run:

```bash
git add spring-boot-iot-ui/src/views/ProductWorkbenchView.vue spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts
git commit -m "refactor: clarify products governance semantics"
```

## Task 3: Reword The Validation Start Node In `/reporting`

**Files:**
- Modify: `spring-boot-iot-ui/src/views/reportingDiagnosis.ts`
- Modify: `spring-boot-iot-ui/src/views/reportingRecentDiagnosis.ts`
- Modify: `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts`

- [ ] **Step 1: Add failing validation-start assertions**

Extend `spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts` so the status header now locks the start-node semantics:

```ts
expect(wrapper.text()).toContain('排障起点');
expect(wrapper.text()).toContain('先发起一次模拟验证，再决定进入哪一条诊断分支。');
expect(wrapper.text()).toContain('继续链路追踪');
expect(wrapper.text()).toContain('查看异常观测');
expect(wrapper.text()).toContain('打开数据校验');
```

For the success path, keep the trace handoff explicit:

```ts
expect(wrapper.text()).toContain('当前节点：链路验证');
expect(wrapper.text()).toContain('下一步进入链路追踪台复盘固定 Pipeline。');
```

- [ ] **Step 2: Run the reporting test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/ReportWorkbenchView.test.ts
```

Expected: FAIL because the current header copy still uses the generic `尚未验证 / 验证成功 / 验证失败` wording without the explicit “start node + next step” framing.

- [ ] **Step 3: Implement the validation-start wording**

Update `spring-boot-iot-ui/src/views/reportingDiagnosis.ts`:

```ts
if (!hasReplayContext) {
  return buildDiagnosis({
    verdict: 'idle',
    title: '排障起点',
    summary: '先发起一次模拟验证，再决定进入哪一条诊断分支。',
    blockerLabel: '当前节点',
    blocker: '链路验证',
    actions: [{ target: 'simulate', label: '前往模拟验证' }]
  });
}

if (isFailed(session, timeline, failedStep)) {
  return buildDiagnosis({
    verdict: 'failed',
    title: '当前节点：链路验证',
    summary: normalizeText(failedStep?.errorMessage) || '当前验证失败，下一步进入链路追踪或异常观测定位证据。',
    blockerLabel: '下一步',
    blocker: normalizeText(failedStep?.stage) || '链路追踪 / 异常观测',
    actions: [
      { target: 'message-trace', label: '继续链路追踪' },
      { target: 'system-log', label: '查看异常观测' }
    ]
  });
}
```

Update the success hint in `spring-boot-iot-ui/src/views/ReportWorkbenchView.vue`:

```ts
return {
  kind: 'timeline',
  level: 'success',
  title: '当前节点：链路验证',
  summary: '下一步进入链路追踪台复盘固定 Pipeline。',
  reason: '当前 session 已绑定 traceId，可跨页联查。',
  nextActionLabel: '继续链路追踪',
  nextActionTarget: '/message-trace'
};
```

Keep `spring-boot-iot-ui/src/views/reportingRecentDiagnosis.ts` aligned by preserving the same action labels:

```ts
if (verdict === 'failed') {
  return '打开链路追踪';
}
if (verdict === 'validated') {
  return '打开数据校验';
}
```

- [ ] **Step 4: Re-run the reporting test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/ReportWorkbenchView.test.ts
```

Expected: PASS with the new start-node wording and next-step actions.

- [ ] **Step 5: Commit the reporting copy change**

Run:

```bash
git add spring-boot-iot-ui/src/views/reportingDiagnosis.ts spring-boot-iot-ui/src/views/reportingRecentDiagnosis.ts spring-boot-iot-ui/src/views/ReportWorkbenchView.vue spring-boot-iot-ui/src/__tests__/components/ReportWorkbenchView.test.ts
git commit -m "refactor: clarify reporting start node guidance"
```

## Task 4: Reword Diagnosis Pages And Failure Archive End-State Guidance

**Files:**
- Modify: `spring-boot-iot-ui/src/views/MessageTraceView.vue`
- Modify: `spring-boot-iot-ui/src/views/AuditLogView.vue`
- Modify: `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`
- Modify: `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`
- Modify: `spring-boot-iot-ui/src/utils/shellPanelContent.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts`

- [ ] **Step 1: Add failing diagnosis-page assertions**

Update the trace, system-log, file-debug, and failure-archive tests to lock the new semantics. Examples:

```ts
// MessageTraceView.test.ts
expect(wrapper.text()).toContain('链路追踪台');
expect(wrapper.text()).toContain('主链路复盘');
expect(wrapper.text()).toContain('来自链路验证中心');

// AuditLogView.test.ts
expect(wrapper.text()).toContain('异常观测台');
expect(wrapper.text()).toContain('后台异常核对');
expect(wrapper.text()).toContain('来自链路追踪台');

// FilePayloadDebugView.test.ts
expect(wrapper.text()).toContain('数据校验台');
expect(wrapper.text()).toContain('数据与原始响应校验');
expect(wrapper.text()).toContain('来自链路追踪台');

// AccessErrorArchivePanel.test.ts
expect(wrapper.text()).toContain('建议先到链路追踪台核对失败阶段');
expect(wrapper.text()).toContain('产品定义中心');
expect(wrapper.text()).toContain('设备资产中心');
```

Update `spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts` so the shell help copy keeps the same flow:

```ts
expect(content.sections.find((section) => section.id === 'help-tech')?.items.map((item) => item.title)).toContain('HTTP / MQTT 联调指引');
expect(content.sections.find((section) => section.id === 'help-tech')?.items.map((item) => item.description).join(' ')).toContain('排障起点');
```

- [ ] **Step 2: Run the diagnosis-page test suite and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts src/__tests__/utils/shellPanelContent.test.ts
```

Expected: FAIL because the affected views still use the older generic descriptions and failure-archive advice.

- [ ] **Step 3: Implement the diagnosis-page wording changes**

Update `spring-boot-iot-ui/src/views/MessageTraceView.vue`:

```vue
title="链路追踪台"
description="主链路复盘：按 TraceId、设备编码、产品标识与 Topic 串联同一条接入链路，并判断下一步去异常观测、数据校验还是治理修正。"
```

```ts
const traceInlineMessage = computed(() => {
  const contextSource = restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`
    : '';
  return [contextSource, '当前节点：主链路复盘', '下一步按证据进入异常观测、数据校验或治理页。']
    .filter(Boolean)
    .join(' · ');
});
```

Update `spring-boot-iot-ui/src/views/AuditLogView.vue`:

```ts
const pageDescription = computed(() =>
  isSystemMode.value
    ? '后台异常核对：集中查看 system_error 与异步异常，并决定是否回链路追踪或进入治理修正。'
    : '查看平台审计日志与操作留痕。'
);

const systemInlineMessage = computed(() =>
  restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)} · 当前节点：后台异常核对 · 下一步回链路追踪或进入治理修正。`
    : ''
);
```

Update `spring-boot-iot-ui/src/views/FilePayloadDebugView.vue`:

```vue
title="数据校验台"
description="数据与原始响应校验：按快照、聚合和原始响应判断是否回治理页修正。"
```

```ts
const inlineStateMessage = computed(() => {
  const sourceLabel = restoredDiagnosticContext.value
    ? `来自${describeDiagnosticSource(restoredDiagnosticContext.value.sourcePage)}`
    : '';
  return [sourceLabel, '当前节点：数据与原始响应校验', errorMessage.value || '下一步回链路追踪或治理页完成修正。']
    .filter(Boolean)
    .join(' · ');
});
```

Update `spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue`:

```ts
const detailRouteAdvice = computed(() => {
  if (detailData.value.traceId) {
    return `建议先到链路追踪台核对失败阶段（Trace ${detailData.value.traceId}），再按证据进入异常观测台；若确认是产品或设备基线问题，再回治理页修正。`;
  }
  if (detailData.value.deviceCode) {
    return `建议先围绕设备编码 ${detailData.value.deviceCode} 核对失败阶段，再视证据进入异常观测台、产品定义中心或设备资产中心。`;
  }
  return '建议先核对失败阶段，再按证据进入异常观测台或治理页修正。';
});
```

Update `spring-boot-iot-ui/src/utils/shellPanelContent.ts` so the access-workbench help copy matches the same tree:

```ts
description: '通过链路验证中心作为排障起点，先发起模拟验证，再按证据进入链路追踪、异常观测或数据校验。',
```

- [ ] **Step 4: Re-run the diagnosis-page suite and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts src/__tests__/utils/shellPanelContent.test.ts
```

Expected: PASS with the new “current node + next step” copy across all diagnosis pages.

- [ ] **Step 5: Commit the diagnosis-page wording changes**

Run:

```bash
git add spring-boot-iot-ui/src/views/MessageTraceView.vue spring-boot-iot-ui/src/views/AuditLogView.vue spring-boot-iot-ui/src/views/FilePayloadDebugView.vue spring-boot-iot-ui/src/components/AccessErrorArchivePanel.vue spring-boot-iot-ui/src/utils/shellPanelContent.ts spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts spring-boot-iot-ui/src/__tests__/views/FilePayloadDebugView.test.ts spring-boot-iot-ui/src/__tests__/components/AccessErrorArchivePanel.test.ts spring-boot-iot-ui/src/__tests__/utils/shellPanelContent.test.ts
git commit -m "refactor: align iot diagnosis page guidance"
```

## Task 5: Update Documentation And Run Verification

**Files:**
- Modify: `README.md`
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/11-可观测性、日志追踪与消息通知治理.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Update the authoritative docs in place**

Apply the wording changes directly in the existing docs:

```md
README.md
- `/products` 继续以“产品定义中心”作为产品定义主入口，但当前已显式承接契约治理、版本治理与风险目录入口。
- `接入智维` 当前标准排障路径固定为：链路验证 -> 链路追踪 / 异常观测 / 数据校验 -> 产品定义中心 / 设备资产中心。
```

```md
docs/02-业务功能与流程说明.md
- `/products` 首屏继续保持产品列表主视图，页级 notices 明确承接契约治理、版本台账与风险目录入口。
- `/device-access` 总览负责完整排障决策树；`/reporting`、`/message-trace`、`/system-log`、`/file-debug` 只表达当前节点与下一步。
```

```md
docs/11-可观测性、日志追踪与消息通知治理.md
- 排障主流程先到链路验证中心，再按证据进入链路追踪台、异常观测台或数据校验台，最后回产品定义中心或设备资产中心修正基线。
```

```md
docs/15-前端优化与治理计划.md
- `接入智维` 完整决策树只允许在 `/device-access` 总览显式展示；诊断页只保留“当前节点 + 下一步”。
- `/products` 必须保持列表主视图，不得回流治理驾驶舱式首屏。
```

```md
docs/08-变更记录与技术债清单.md
- 记录本轮“产品定义中心语义减负 + 接入智维排障决策树固化”的行为变化和验证命令。
```

- [ ] **Step 2: Run the targeted front-end verification suite**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/SectionLandingView.test.ts src/__tests__/views/ProductWorkbenchView.test.ts src/__tests__/components/ReportWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts src/__tests__/views/FilePayloadDebugView.test.ts src/__tests__/components/AccessErrorArchivePanel.test.ts src/__tests__/utils/shellPanelContent.test.ts
```

Expected: PASS across all updated `接入智维` / `/products` semantics tests.

- [ ] **Step 3: Run build and shared front-end guards**

Run:

```bash
npm --prefix spring-boot-iot-ui run build
npm --prefix spring-boot-iot-ui run component:guard
npm --prefix spring-boot-iot-ui run list:guard
npm --prefix spring-boot-iot-ui run style:guard
```

Expected: PASS with no new guard regressions.

- [ ] **Step 4: Commit the docs and final verification result**

Run:

```bash
git add README.md docs/02-业务功能与流程说明.md docs/11-可观测性、日志追踪与消息通知治理.md docs/15-前端优化与治理计划.md docs/08-变更记录与技术债清单.md
git commit -m "docs: sync products semantics and access troubleshooting tree"
```
