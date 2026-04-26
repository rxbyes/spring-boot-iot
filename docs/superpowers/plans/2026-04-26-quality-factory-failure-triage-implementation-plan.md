# Quality Factory Failure Triage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add run-indexed failure triage so business acceptance results and automation governance evidence can explain failed modules and failed scenarios with category, reason, and evidence summary.

**Architecture:** Keep `logs/acceptance` and `registry-run-*.json` as the source of truth, extend the archive-index generation path with deterministic rule-based diagnosis, then let the report module and frontend consume those read-only diagnosis fields. Do not change `passed / failed / blocked`, readiness gates, or real-environment execution entrypoints.

**Tech Stack:** Node.js ESM scripts, Spring Boot report module, Vue 3, Vitest, JUnit 5, Maven

---

## File Structure

### Node archive-index layer

- Create: `scripts/auto/automation-result-diagnosis-lib.mjs`
  - Owns scenario-level category rules, module aggregation, run-level summary aggregation, and diagnosis text generation.
- Modify: `scripts/auto/automation-result-archive-index-lib.mjs`
  - Calls the diagnosis helper while normalizing run records, writes `failureSummary / failedModules / failedScenarios`.
- Modify: `scripts/auto/automation-result-archive-index.test.mjs`
  - Covers category matching, module aggregation, and index field emission.

### Java report/business-acceptance layer

- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationFailureDiagnosisVO.java`
  - Reusable diagnosis DTO: `category / reason / evidenceSummary`.
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailureSummaryVO.java`
  - Run-level triage summary: `primaryCategory / countsByCategory`.
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedModuleVO.java`
  - Module-level failed-module row for indexed detail.
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedScenarioVO.java`
  - Scenario-level failed-scenario row for indexed detail.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveIndexVO.java`
  - Uses the new triage DTOs in `RunRecord`.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunDetailVO.java`
  - Exposes run-level triage summary plus failed-module and failed-scenario slices.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunResultVO.java`
  - Exposes per-result diagnosis for raw detail consumers.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java`
  - Exposes module-level diagnosis to the business result page.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java`
  - Maps index triage fields into run detail and page/recent rows.
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java`
  - Reads matching index run by `runId`, then attaches module-level diagnosis during business result aggregation.
- Modify tests:
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java`

### Frontend business/governance layer

- Modify: `spring-boot-iot-ui/src/types/automation.ts`
  - Adds run triage, failed-module, failed-scenario, and diagnosis interfaces.
- Modify: `spring-boot-iot-ui/src/types/businessAcceptance.ts`
  - Adds module-level diagnosis type.
- Modify: `spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue`
  - Shows module-level category, reason, and evidence summary in the detail pane.
- Modify: `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
  - Projects run-level triage summary and scenario-level diagnosis into UI-friendly rows.
- Modify: `spring-boot-iot-ui/src/components/automationGovernance/AutomationEvidenceWorkspaceSection.vue`
  - Adds run-level failure distribution and scenario-level diagnosis columns/blocks.
- Modify tests:
  - `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
  - `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
  - `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

### Docs

- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/superpowers/README.md`

---

### Task 1: Extend archive-index generation with failure diagnosis

**Files:**
- Create: `scripts/auto/automation-result-diagnosis-lib.mjs`
- Modify: `scripts/auto/automation-result-archive-index-lib.mjs`
- Modify: `scripts/auto/automation-result-archive-index.test.mjs`

- [ ] **Step 1: Write the failing Node tests for category matching and aggregation**

Add test coverage like:

```js
test('buildAutomationResultArchiveIndex adds failure summary, module diagnosis, and scenario diagnosis', async () => {
  const index = await buildAutomationResultArchiveIndex({ workspaceRoot, resultsDir });

  assert.equal(index.runs[0].failureSummary.primaryCategory, '接口');
  assert.equal(index.runs[0].failedModules[0].diagnosis.category, '接口');
  assert.equal(index.runs[0].failedScenarios[0].diagnosis.category, '接口');
  assert.match(index.runs[0].failedScenarios[0].diagnosis.reason, /500/);
});

test('buildAutomationResultArchiveIndex falls back to 其他 when no rule matches', async () => {
  const index = await buildAutomationResultArchiveIndex({ workspaceRoot, resultsDir });

  assert.equal(index.runs[0].failedScenarios[0].diagnosis.category, '其他');
  assert.match(index.runs[0].failedScenarios[0].diagnosis.reason, /未命中已知规则/);
});
```

- [ ] **Step 2: Run the Node archive-index tests and verify RED**

Run:

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

Expected: FAIL because `failureSummary / failedModules / failedScenarios` do not exist yet.

- [ ] **Step 3: Implement the diagnosis helper and integrate it into run normalization**

Create `scripts/auto/automation-result-diagnosis-lib.mjs` with focused helpers:

```js
const CATEGORY_PRIORITY = ['权限', '环境', '接口', 'UI', '数据', '断言', '其他'];

export function diagnoseFailedScenario(result, evidenceTexts = []) {
  const haystack = [result?.summary, result?.stepLabel, result?.apiRef, result?.pageAction, ...evidenceTexts]
    .filter(Boolean)
    .join('\n')
    .toLowerCase();

  if (haystack.includes('401') || haystack.includes('403') || haystack.includes('forbidden')) {
    return {
      category: '权限',
      reason: '命中 401/403 或未授权信号',
      evidenceSummary: compactEvidenceSummary(result, evidenceTexts)
    };
  }
  // 环境 / 接口 / UI / 数据 / 断言 rules...
  return {
    category: '其他',
    reason: '未命中已知规则，建议查看原始证据',
    evidenceSummary: compactEvidenceSummary(result, evidenceTexts)
  };
}

export function buildRunFailureDiagnosis(results) {
  const failedScenarios = results
    .filter((item) => normalizeStatus(item?.status) !== 'passed')
    .map((item) => toFailedScenarioRecord(item));

  return {
    failureSummary: summarizeFailureCategories(failedScenarios),
    failedModules: aggregateFailedModules(failedScenarios),
    failedScenarios
  };
}
```

Then wire it into `automation-result-archive-index-lib.mjs`:

```js
const diagnosis = buildRunFailureDiagnosis(results);

return {
  runId: resolveRunId(payload, fileName),
  // existing fields...
  evidenceItems,
  failureSummary: diagnosis.failureSummary,
  failedModules: diagnosis.failedModules,
  failedScenarios: diagnosis.failedScenarios
};
```

- [ ] **Step 4: Re-run the Node archive-index tests and verify GREEN**

Run:

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

Expected: PASS with the new diagnosis assertions.

- [ ] **Step 5: Commit the Node diagnosis slice**

```bash
git add scripts/auto/automation-result-diagnosis-lib.mjs scripts/auto/automation-result-archive-index-lib.mjs scripts/auto/automation-result-archive-index.test.mjs
git commit -m "feat: add archive result failure diagnosis"
```

### Task 2: Expose diagnosis through the report and business-acceptance read models

**Files:**
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationFailureDiagnosisVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailureSummaryVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedModuleVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedScenarioVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveIndexVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunDetailVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunResultVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java`
- Modify tests:
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
  - `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java`

- [ ] **Step 1: Add failing Java tests for indexed diagnosis fields**

Extend the archive-index and business-acceptance tests with assertions like:

```java
assertThat(index.getRuns().get(0).getFailureSummary().getPrimaryCategory()).isEqualTo("接口");
assertThat(index.getRuns().get(0).getFailedModules().get(0).getDiagnosis().getCategory()).isEqualTo("接口");
assertThat(result.getModules().get(0).getDiagnosis().getReason()).contains("接口问题");
```

Also add a controller assertion:

```java
jsonPath("$.data.modules[0].diagnosis.category").value("接口")
```

- [ ] **Step 2: Run the report-module tests and verify RED**

Run:

```bash
mvn -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest,BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest test
```

Expected: FAIL because the new VO fields and service mappings do not exist yet.

- [ ] **Step 3: Implement reusable diagnosis DTOs and read-model mappings**

Create the reusable VO:

```java
@Data
public class AutomationFailureDiagnosisVO {
    private String category;
    private String reason;
    private String evidenceSummary;
}
```

Update indexed run and detail VO shapes:

```java
@Data
public class AutomationResultFailedScenarioVO {
    private String scenarioId;
    private String scenarioTitle;
    private String moduleCode;
    private String runnerType;
    private String stepLabel;
    private String apiRef;
    private String pageAction;
    private AutomationFailureDiagnosisVO diagnosis;
}
```

Map triage fields in `AutomationResultQueryServiceImpl`:

```java
detail.setFailureSummary(indexedRun.getFailureSummary());
detail.setFailedModules(indexedRun.getFailedModules());
detail.setFailedScenarios(indexedRun.getFailedScenarios());

resultVO.setDiagnosis(matchDiagnosisByScenarioId(indexedRun, rawResult.getScenarioId()));
```

Attach module diagnosis in `BusinessAcceptanceServiceImpl`:

```java
Map<String, AutomationResultFailedModuleVO> moduleDiagnosisMap = loadIndexedModuleDiagnosis(runId);
moduleResult.setDiagnosis(Optional.ofNullable(moduleDiagnosisMap.get(moduleCode))
        .map(AutomationResultFailedModuleVO::getDiagnosis)
        .orElse(null));
```

- [ ] **Step 4: Re-run the report-module tests and verify GREEN**

Run:

```bash
mvn -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest,BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest test
```

Expected: PASS with diagnosis fields serialized and mapped.

- [ ] **Step 5: Commit the backend read-model slice**

```bash
git add spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationFailureDiagnosisVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailureSummaryVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedModuleVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultFailedScenarioVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultArchiveIndexVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunDetailVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/AutomationResultRunResultVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImpl.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultArchiveIndexServiceImplTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java
git commit -m "feat: expose failure triage in report results"
```

### Task 3: Surface module/scenario diagnosis in the frontend

**Files:**
- Modify: `spring-boot-iot-ui/src/types/businessAcceptance.ts`
- Modify: `spring-boot-iot-ui/src/types/automation.ts`
- Modify: `spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/components/automationGovernance/AutomationEvidenceWorkspaceSection.vue`
- Modify tests:
  - `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
  - `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
  - `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Add failing Vitest coverage for diagnosis rendering**

Add tests like:

```ts
expect(workbench.result.value?.modules[0]?.diagnosis?.category).toBe('接口');
expect(workbench.failedScenarioDetails.value[0]?.diagnosis?.category).toBe('接口');
expect(source).toContain('主分类');
expect(source).toContain('证据摘要');
expect(source).toContain('失败分类分布');
```

- [ ] **Step 2: Run the focused frontend tests and verify RED**

Run:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Workdir:

```bash
/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/.worktrees/quality-factory-result-archive-index/spring-boot-iot-ui
```

Expected: FAIL because diagnosis types and UI rendering are missing.

- [ ] **Step 3: Implement diagnosis types and the two UI surfaces**

Add shared frontend types:

```ts
export interface AutomationFailureDiagnosis {
  category: string;
  reason: string;
  evidenceSummary: string;
}
```

Use them in business acceptance:

```ts
export interface BusinessAcceptanceModuleResult {
  moduleCode: string;
  moduleName: string;
  status: BusinessAcceptanceStatus;
  failedScenarioCount: number;
  failedScenarioTitles: string[];
  suggestedDirection?: string;
  failureDetails: BusinessAcceptanceFailureDetail[];
  diagnosis?: AutomationFailureDiagnosis | null;
}
```

Render module diagnosis in `BusinessAcceptanceModuleResultPanel.vue`:

```vue
<article v-if="activeModule.diagnosis">
  <h4>主分类</h4>
  <p>{{ activeModule.diagnosis.category }}</p>
</article>
<article v-if="activeModule.diagnosis">
  <h4>证据摘要</h4>
  <p>{{ activeModule.diagnosis.evidenceSummary }}</p>
</article>
```

Project governance diagnosis in `useAutomationResultsWorkbench.ts`:

```ts
const failureCategoryMetrics = computed(() =>
  Object.entries(currentRun.value?.failureSummary?.countsByCategory || {}).map(([category, count]) => ({
    category,
    count
  }))
);
```

Render the governance diagnosis section:

```vue
<PanelCard title="失败分类分布" description="先看本次运行主要更像什么问题。">
  <div v-if="failureCategoryMetrics.length === 0" class="empty-block">当前没有失败分类数据。</div>
</PanelCard>
```

- [ ] **Step 4: Re-run the focused frontend tests and verify GREEN**

Run:

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS with both module-level and scenario-level diagnosis surfaced.

- [ ] **Step 5: Commit the frontend triage slice**

```bash
git add spring-boot-iot-ui/src/types/businessAcceptance.ts spring-boot-iot-ui/src/types/automation.ts spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue spring-boot-iot-ui/src/composables/useAutomationResultsWorkbench.ts spring-boot-iot-ui/src/components/automationGovernance/AutomationEvidenceWorkspaceSection.vue spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: show failure triage in quality factory views"
```

### Task 4: Synchronize authoritative docs

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/superpowers/README.md`

- [ ] **Step 1: Update the authoritative docs with failure-triage semantics**

Document:

1. the fixed seven-category taxonomy;
2. the fact that diagnosis is read-only and does not change `passed / failed / blocked`;
3. business result page module diagnosis;
4. governance evidence run/scenario diagnosis;
5. the boundary that raw evidence remains the truth source.

Use wording like:

```md
- `2026-04-26` 起，质量工场结果证据继续补齐首版失败归因：索引生成阶段会按 `环境 / 数据 / 权限 / 接口 / UI / 断言 / 其他` 产出模块级与场景级诊断。该能力只提升结果解释与复盘效率，不改变当前 `passed / failed / blocked` 判定，也不替代原始 evidence 真相。
```

- [ ] **Step 2: Run docs verification**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: PASS

- [ ] **Step 3: Commit the documentation sync**

```bash
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md docs/superpowers/README.md
git commit -m "docs: document quality factory failure triage"
```

### Task 5: Run final verification for the whole slice

**Files:**
- All files from Tasks 1-4

- [ ] **Step 1: Run the Node archive-index tests**

```bash
node --test scripts/auto/automation-result-archive-index.test.mjs
```

Expected: PASS

- [ ] **Step 2: Run the report-module tests**

```bash
mvn -q -pl spring-boot-iot-report -Dtest=AutomationResultArchiveIndexServiceImplTest,AutomationResultQueryServiceImplTest,BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest test
```

Expected: PASS

- [ ] **Step 3: Run the focused frontend tests**

```bash
./node_modules/.bin/vitest --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS

- [ ] **Step 4: Run the frontend production build**

```bash
npm run build
```

Workdir:

```bash
/Users/rxbyes/Downloads/rxbyes/idea/spring-boot-iot/.worktrees/quality-factory-result-archive-index/spring-boot-iot-ui
```

Expected: PASS

- [ ] **Step 5: Re-run docs and diff guards**

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: PASS

- [ ] **Step 6: Commit the final verified slice**

```bash
git status --short
git add -A
git commit -m "feat: add quality factory failure triage"
```
