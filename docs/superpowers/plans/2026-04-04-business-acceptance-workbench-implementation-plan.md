# Business Acceptance Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a business-facing acceptance workbench that lets验收人员、产品、项目经理按预置交付包选择环境、账号模板和模块范围后，一键发起真实环境验收，并在第一屏看到“是否通过”和“哪些模块没过”，同时保留跳转到底层结果与证据中心的能力。

**Architecture:** Add a config-backed business-acceptance layer on top of the existing acceptance registry and result ledger. `spring-boot-iot-report` reads `config/automation/business-acceptance-packages.json`, validates module-to-scenario mappings against `config/automation/acceptance-registry.json`, launches the existing Node registry runner through a derived registry file, then aggregates `registry-run-*.json` artifacts into package-level and module-level business results. `spring-boot-iot-ui` gets two new business-facing pages and a dedicated composable/API client, while `质量工场`、`结果与基线中心`、菜单种子和权限分配 are updated in place.

**Tech Stack:** Spring Boot 4, Java 17, Vue 3, Vue Router, Vitest, Node-based acceptance registry scripts, PowerShell/Node/Python existing runners, SQL seed data, Markdown docs.

---

## Task Summary

Implement a new `业务验收台` entry under `质量工场`, backed by config-defined business packages, account templates, and module drill-down metadata. The implementation must preserve the current `研发工场 -> 执行中心 -> 结果与基线中心` structure for研发/测试, and add a lightweight consumer-facing path for业务用户 without introducing H2 fallback, a new database table, or a second automation substrate.

## Affected Modules

- `spring-boot-iot-ui`
- `spring-boot-iot-report`
- `config/automation`
- `scripts/auto`
- `scripts/run-acceptance-registry.test.mjs`
- `sql/init-data.sql`
- `docs/02-业务功能与流程说明.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`

## Assumptions

- Phase 1 keeps `业务验收包`、`账号模板`、`模块失败回填信息` in repo config, not in a new database table.
- Real execution still targets the shared dev baseline from `spring-boot-iot-admin/src/main/resources/application-dev.yml`; no H2 or mock fallback is introduced.
- The backend may use `ProcessBuilder` to call local `node` and existing `scripts/auto/run-acceptance-registry.mjs`; if `node` is unavailable, the API must surface a clear execution-unavailable or environment-blocked result instead of silently succeeding.
- Existing demo accounts remain the first usable template source. If no dedicated `产品` or `验收` user is seeded in this round, Phase 1 may map multiple template labels to the current real-environment demo users and document the limitation.
- `registry-run-*.json` remains the single persisted run ledger so that the new business result pages and the existing `/automation-results` page stay aligned.

## File Map

### New files

- `config/automation/business-acceptance-packages.json`
  Config-backed definition of packages, modules, account templates, supported environments, and fallback failure drill-down metadata.
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/BusinessAcceptanceController.java`
  Business acceptance discovery, launch, polling, and result-aggregation endpoints.
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/BusinessAcceptanceService.java`
  Service contract for package listing, run launching, job status lookup, and business result aggregation.
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java`
  Config loading, registry mapping, process launching, in-memory job tracking, and module-level result aggregation.
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageModuleVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceAccountTemplateVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceLatestResultVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunRequest.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunLaunchVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunStatusVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceResultVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java`
- `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceFailureDetailVO.java`
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java`
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
- `spring-boot-iot-ui/src/types/businessAcceptance.ts`
  Frontend package, account template, launch, job status, and business result types.
- `spring-boot-iot-ui/src/api/businessAcceptance.ts`
  API client for discovery, run launch, job polling, and business result lookup.
- `spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts`
  Package selection, lightweight config, launch/poll orchestration, and result-page data loading.
- `spring-boot-iot-ui/src/components/BusinessAcceptancePackagePanel.vue`
- `spring-boot-iot-ui/src/components/BusinessAcceptanceRunConfigPanel.vue`
- `spring-boot-iot-ui/src/components/BusinessAcceptanceResultSummaryPanel.vue`
- `spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue`
- `spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue`
- `spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue`
- `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`

### Modified files

- `spring-boot-iot-ui/src/router/index.ts`
  Register `/business-acceptance` and `/business-acceptance/results/:runId`.
- `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
  Expose `业务验收台` under `质量工场`, add route meta, and keep shared nav/search metadata consistent.
- `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
  Promote `业务验收台` as the first consumer-facing quality entry without reintroducing execution/registry editing copy.
- `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
  Accept external `runId` context when the business result page deep-links into `/automation-results`.
- `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
  Consume route query context for direct跳转到已有结果与基线中心.
- `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
- `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/AutomationResultControllerTest.java`
  Keep the existing result-center contract intact while adding deep-link compatibility expectations if needed.
- `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`
  Add option-field coverage if the business launcher relies on persisted `options.packageCode` / `options.moduleCodes`.
- `scripts/auto/acceptance-registry-lib.mjs`
  Allow loading a derived registry path for business-pack launches.
- `scripts/auto/run-acceptance-registry.mjs`
  Accept derived registry path plus business metadata passthrough options and persist them into the run summary `options`.
- `scripts/run-acceptance-registry.test.mjs`
  Cover the new CLI contract without breaking current default-registry behavior.
- `sql/init-data.sql`
  Seed `业务验收台` menu entry and role-menu grants for target业务角色,管理角色,研发角色,超级管理员.
- `docs/02-业务功能与流程说明.md`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`

## Task 1: Lock the business-acceptance route, menu, and config contract

**Files:**
- Create: `config/automation/business-acceptance-packages.json`
- Modify: `spring-boot-iot-ui/src/utils/sectionWorkspaces.ts`
- Modify: `spring-boot-iot-ui/src/router/index.ts`
- Modify: `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
- Modify: `sql/init-data.sql`

- [ ] **Step 1: Extend the failing frontend contract tests for the new quality-workbench entry**

```typescript
it('maps quality workbench to business acceptance plus rd and shared centers', () => {
  const config = getSectionHomeConfigByPath('/quality-workbench');

  expect(config?.cards.map((item) => item.path)).toEqual([
    '/business-acceptance',
    '/rd-workbench',
    '/automation-execution',
    '/automation-results'
  ]);
  expect(getRouteMetaPreset('/business-acceptance')).toMatchObject({
    title: '业务验收台',
    description: '按交付清单选择预置验收包并一键运行业务验收。'
  });
});

it('registers the business acceptance routes', () => {
  const source = readRouter();

  expect(source).toContain("path: '/business-acceptance'");
  expect(source).toContain("path: '/business-acceptance/results/:runId'");
});

it('keeps the business acceptance view free of rd authoring widgets', () => {
  const source = readView('BusinessAcceptanceWorkbenchView.vue');

  expect(source).toContain('<BusinessAcceptancePackagePanel');
  expect(source).toContain('<BusinessAcceptanceRunConfigPanel');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationExecutionConfigPanel');
});
```

- [ ] **Step 2: Run the focused frontend contract suite and verify it fails before route/schema changes**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because `/business-acceptance` route, view, and quality-workbench card do not exist yet.

- [ ] **Step 3: Add the config seed, route metadata, and menu seed for the four approved packages**

```json
{
  "packages": [
    {
      "packageCode": "product-device",
      "packageName": "产品与设备",
      "description": "覆盖产品新增、产品查询、设备新增、设备查询。",
      "targetRoles": ["acceptance", "product", "manager"],
      "supportedEnvironments": ["dev", "test"],
      "defaultAccountTemplate": "acceptance-default",
      "modules": [
        {
          "moduleCode": "product-create",
          "moduleName": "产品新增",
          "scenarioRefs": ["auth.browser-smoke"],
          "suggestedDirection": "needsReview",
          "fallbackFailure": {
            "stepLabel": "提交产品新增表单",
            "apiRef": "POST /device/product/add",
            "pageAction": "点击新增产品并提交",
            "summary": "产品新增链路需要研发/测试复核"
          }
        }
      ]
    }
  ],
  "accountTemplates": [
    {
      "templateCode": "acceptance-default",
      "templateName": "验收账号模板",
      "username": "biz_demo",
      "roleHint": "业务验收",
      "supportedEnvironments": ["dev", "test"]
    }
  ]
}
```

```typescript
// spring-boot-iot-ui/src/utils/sectionWorkspaces.ts
{
  key: 'quality-workbench',
  cards: [
    { path: '/business-acceptance', label: '业务验收台', description: '按交付清单选择预置验收包并直接查看模块级结论。', short: '验' },
    { path: '/rd-workbench', label: '研发工场', description: '面向研发的自动化资产编排主入口。', short: '研' },
    { path: '/automation-execution', label: '执行中心', description: '统一查看执行配置、命令预览和验收注册表依赖关系。', short: '执' },
    { path: '/automation-results', label: '结果与基线中心', description: '统一导入运行结果、查看失败场景并维护质量建议与基线证据。', short: '果' }
  ]
}
```

```sql
-- sql/init-data.sql
(93003020, 1, 93000005, '业务验收台', 'system:business-acceptance', '/business-acceptance', 'BusinessAcceptanceWorkbenchView', 'finished', '{"caption":"按交付清单运行预置业务验收包"}', 51, 1, 1, '/business-acceptance', 'system:business-acceptance', 51, 1, 1, 1, NOW(), 1, NOW(), 0)
```

Grant `93000005` and `93003020` to `@role_business_id`, `@role_management_id`, `@role_developer_id`, and `@role_super_admin_id` so the target users can actually see the new entry.

- [ ] **Step 4: Re-run the focused frontend contract suite**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS for the new route/meta/card contract.

- [ ] **Step 5: Commit the route/config/menu contract**

```bash
git add config/automation/business-acceptance-packages.json spring-boot-iot-ui/src/utils/sectionWorkspaces.ts spring-boot-iot-ui/src/router/index.ts spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue spring-boot-iot-ui/src/__tests__/utils/sectionHomes.test.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts sql/init-data.sql
git commit -m "feat: add business acceptance route and config contract"
```

## Task 2: Enable the derived-registry runner contract for business-pack launches

**Files:**
- Modify: `scripts/auto/acceptance-registry-lib.mjs`
- Modify: `scripts/auto/run-acceptance-registry.mjs`
- Modify: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add failing Node tests for `--registry-path` and business metadata passthrough**

```javascript
test('runRegistryCli accepts a derived registry path and persists business metadata', async () => {
  const registryPath = path.join(tempDir, 'business-acceptance-registry.json');
  await fs.writeFile(registryPath, JSON.stringify({
    version: '1.0.0',
    scenarios: [
      {
        id: 'auth.browser-smoke',
        title: '登录与产品设备浏览器冒烟',
        module: 'device',
        docRef: 'docs/21#接入智维主链路',
        runnerType: 'browserPlan',
        scope: 'delivery',
        blocking: 'blocker'
      }
    ]
  }), 'utf8');

  const result = await runRegistryCli({
    workspaceRoot: tempDir,
    argv: [
      `--registry-path=${registryPath}`,
      '--package-code=product-device',
      '--environment-code=dev',
      '--account-template=acceptance-default',
      '--selected-modules=product-create,device-query'
    ],
    adapterOverrides: {
      browserPlan: async () => ({
        scenarioId: 'auth.browser-smoke',
        runnerType: 'browserPlan',
        status: 'passed',
        blocking: 'blocker',
        summary: 'ok',
        evidenceFiles: []
      })
    }
  });

  assert.equal(result.summary.total, 1);
});
```

- [ ] **Step 2: Run the Node script tests and verify they fail before CLI changes**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: FAIL because `--registry-path` and the new business metadata options are not parsed yet.

- [ ] **Step 3: Implement the CLI extension while preserving current defaults**

```javascript
// scripts/auto/run-acceptance-registry.mjs
if (arg.startsWith('--registry-path=')) {
  options.registryPath = arg.slice('--registry-path='.length).trim();
  return;
}
if (arg.startsWith('--package-code=')) {
  options.packageCode = arg.slice('--package-code='.length).trim();
  return;
}
if (arg.startsWith('--environment-code=')) {
  options.environmentCode = arg.slice('--environment-code='.length).trim();
  return;
}
if (arg.startsWith('--account-template=')) {
  options.accountTemplate = arg.slice('--account-template='.length).trim();
  return;
}
if (arg.startsWith('--selected-modules=')) {
  options.selectedModules = arg.slice('--selected-modules='.length).trim();
  return;
}
```

```javascript
const registry = await loadAcceptanceRegistry({
  workspaceRoot,
  registryPath: options.registryPath || DEFAULT_ACCEPTANCE_REGISTRY_PATH,
  source: registrySource
});
```

The persisted run JSON `options` payload must keep `packageCode`, `environmentCode`, `accountTemplate`, and `selectedModules` so that latest-package summaries and result aggregation can be rebuilt from the ledger alone.

- [ ] **Step 4: Re-run the Node script tests**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: PASS, and existing default-registry test coverage remains green.

- [ ] **Step 5: Commit the runner contract**

```bash
git add scripts/auto/acceptance-registry-lib.mjs scripts/auto/run-acceptance-registry.mjs scripts/run-acceptance-registry.test.mjs
git commit -m "feat: add business acceptance registry launcher options"
```

## Task 3: Implement backend package discovery and latest-result summaries

**Files:**
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/BusinessAcceptanceController.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/BusinessAcceptanceService.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageModuleVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceAccountTemplateVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceLatestResultVO.java`
- Create: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java`
- Create: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`

- [ ] **Step 1: Write failing report-module tests for package discovery and latest package status**

```java
@Test
void shouldExposeBusinessAcceptancePackagesRoute() throws Exception {
    when(businessAcceptanceService.listPackages()).thenReturn(List.of(new BusinessAcceptancePackageVO()));

    mockMvc.perform(get("/api/report/business-acceptance/packages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

    verify(businessAcceptanceService).listPackages();
}

@Test
void shouldLoadLatestPackageSummaryFromRunLedger() {
    BusinessAcceptancePackageVO pkg = service.listPackages().get(0);

    assertThat(pkg.getPackageCode()).isEqualTo("product-device");
    assertThat(pkg.getLatestResult().getStatus()).isIn("passed", "failed", "neverRun");
}
```

- [ ] **Step 2: Run the focused report tests and verify they fail before the new controller/service exists**

Run: `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest test`

Expected: FAIL because the business acceptance service and endpoints do not exist yet.

- [ ] **Step 3: Implement config loading, registry validation, and latest-summary projection**

```java
public interface BusinessAcceptanceService {
    List<BusinessAcceptancePackageVO> listPackages();
    List<BusinessAcceptanceAccountTemplateVO> listAccountTemplates();
}
```

```java
@GetMapping("/packages")
public R<List<BusinessAcceptancePackageVO>> listPackages() {
    return R.ok(businessAcceptanceService.listPackages());
}

@GetMapping("/account-templates")
public R<List<BusinessAcceptanceAccountTemplateVO>> listAccountTemplates() {
    return R.ok(businessAcceptanceService.listAccountTemplates());
}
```

Implementation rules:

- Read `config/automation/business-acceptance-packages.json` with Jackson and fail fast on duplicate `packageCode`, duplicate `moduleCode`, or unknown `scenarioRefs`.
- Validate every `scenarioRef` against `config/automation/acceptance-registry.json`.
- When projecting `latestResult`, scan `registry-run-*.json`, prefer runs whose `options.packageCode` matches, and summarize only the selected package modules.
- Return `neverRun` when a package exists but no matching run is found.

- [ ] **Step 4: Re-run the focused report tests**

Run: `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest test`

Expected: PASS for package and account-template discovery.

- [ ] **Step 5: Commit the discovery layer**

```bash
git add spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/BusinessAcceptanceController.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/BusinessAcceptanceService.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptancePackageModuleVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceAccountTemplateVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceLatestResultVO.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java
git commit -m "feat: add business acceptance package discovery"
```

## Task 4: Implement backend launch, polling, and business result aggregation

**Files:**
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/BusinessAcceptanceController.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/BusinessAcceptanceService.java`
- Modify: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunRequest.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunLaunchVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunStatusVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceResultVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java`
- Create: `spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceFailureDetailVO.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/AutomationResultControllerTest.java`
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java`

- [ ] **Step 1: Add failing tests for launch, job polling, and module-level aggregation**

```java
@Test
void shouldLaunchBusinessAcceptanceRun() throws Exception {
    mockMvc.perform(post("/api/report/business-acceptance/runs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "packageCode":"product-device",
                          "environmentCode":"dev",
                          "accountTemplateCode":"acceptance-default",
                          "moduleCodes":["product-create","device-query"]
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.jobId").isNotEmpty())
            .andExpect(jsonPath("$.data.status").value("running"));
}

@Test
void shouldAggregateRunResultToBusinessModules() {
    BusinessAcceptanceResultVO result = service.getRunResult("product-device", "20260404153000");

    assertThat(result.getPassedModuleCount()).isNotNull();
    assertThat(result.getFailedModuleNames()).isNotEmpty();
    assertThat(result.getModules().get(0).getFailureDetails().get(0).getStepLabel()).isNotBlank();
}
```

- [ ] **Step 2: Run the focused report tests and verify they fail before launch/aggregation exists**

Run: `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest,AutomationResultControllerTest,AutomationResultQueryServiceImplTest test`

Expected: FAIL because launch/poll/result APIs and option-field aggregation are not implemented yet.

- [ ] **Step 3: Implement async launch using a derived registry file and in-memory job tracking**

```java
public interface BusinessAcceptanceService {
    BusinessAcceptanceRunLaunchVO launchRun(BusinessAcceptanceRunRequest request);
    BusinessAcceptanceRunStatusVO getRunStatus(String jobId);
    BusinessAcceptanceResultVO getRunResult(String packageCode, String runId);
}
```

```java
@PostMapping("/runs")
public R<BusinessAcceptanceRunLaunchVO> launchRun(@RequestBody BusinessAcceptanceRunRequest request) {
    return R.ok(businessAcceptanceService.launchRun(request));
}

@GetMapping("/runs/{jobId}")
public R<BusinessAcceptanceRunStatusVO> getRunStatus(@PathVariable String jobId) {
    return R.ok(businessAcceptanceService.getRunStatus(jobId));
}

@GetMapping("/results/{runId}")
public R<BusinessAcceptanceResultVO> getRunResult(
        @PathVariable String runId,
        @RequestParam String packageCode
) {
    return R.ok(businessAcceptanceService.getRunResult(packageCode, runId));
}
```

Implementation rules:

- Validate `packageCode`, `environmentCode`, `accountTemplateCode`, and `moduleCodes` against the config file before starting a run.
- Build a derived registry JSON containing only the selected scenarios for the selected modules, preserving dependency order.
- Launch `node scripts/auto/run-acceptance-registry.mjs --registry-path=... --package-code=... --environment-code=... --account-template=... --selected-modules=...`.
- Track job state in a `ConcurrentHashMap<String, JobState>`; keep `status`, `startedAt`, `finishedAt`, `runId`, and `errorMessage`.
- Persist no second ledger; always rebuild the final business result from the generated `registry-run-*.json`.

- [ ] **Step 4: Implement module result aggregation, environment-block classification, and drill-down fallback mapping**

```java
if (runDetail == null) {
    status = "blocked";
    summary = "未读取到有效运行结果";
} else if (environmentFailureDetected(runDetail)) {
    status = "blocked";
    suggestedDirection = "environment";
} else if (moduleResults.stream().anyMatch(item -> "failed".equals(item.getStatus()))) {
    status = "failed";
} else {
    status = "passed";
}
```

Aggregation rules:

- `BusinessAcceptanceResultVO` must include `status`, `passedModuleCount`, `failedModuleCount`, `failedModuleNames`, `durationText`, `runId`, and `jumpToAutomationResultsPath`.
- `BusinessAcceptanceModuleResultVO` must include `moduleCode`, `moduleName`, `status`, `failedScenarioCount`, `failedScenarioTitles`, `suggestedDirection`, and `failureDetails`.
- `BusinessAcceptanceFailureDetailVO` must expose `scenarioTitle`, `stepLabel`, `apiRef`, `pageAction`, and `summary`.
- If the underlying run detail lacks step/API/page-action fields, fill them from `business-acceptance-packages.json`.
- If the launcher fails before a valid `runId` is created or health/login reachability keywords are detected, mark the overall result and affected modules as `blocked` rather than plain `failed`.

- [ ] **Step 5: Re-run the focused report-module suite**

Run: `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest,AutomationResultControllerTest,AutomationResultQueryServiceImplTest test`

Expected: PASS, including compatibility coverage for the existing automation results endpoints.

- [ ] **Step 6: Commit the launch and aggregation layer**

```bash
git add spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/controller/BusinessAcceptanceController.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/BusinessAcceptanceService.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImpl.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunRequest.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunLaunchVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceRunStatusVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceResultVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceModuleResultVO.java spring-boot-iot-report/src/main/java/com/ghlzm/iot/report/vo/BusinessAcceptanceFailureDetailVO.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/BusinessAcceptanceControllerTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/controller/AutomationResultControllerTest.java spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/AutomationResultQueryServiceImplTest.java
git commit -m "feat: add business acceptance execution and aggregation"
```

## Task 5: Implement the frontend API layer, launch orchestration, and deep-link handoff

**Files:**
- Create: `spring-boot-iot-ui/src/types/businessAcceptance.ts`
- Create: `spring-boot-iot-ui/src/api/businessAcceptance.ts`
- Create: `spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts`
- Create: `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
- Modify: `spring-boot-iot-ui/src/views/AutomationResultsView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`

- [ ] **Step 1: Write failing Vitest coverage for package loading, launch polling, and result-center deep links**

```typescript
it('loads package cards and account templates, then launches a run', async () => {
  const workbench = useBusinessAcceptanceWorkbench();

  await workbench.loadInitialData();
  expect(workbench.packages.value[0]?.packageCode).toBe('product-device');

  await workbench.launchSelectedPackage();
  expect(workbench.runStatus.value?.status).toBe('running');
});

it('preselects a run in automation results when runId query exists', async () => {
  mockRouteQuery({ runId: '20260404153000' });
  const workbench = useAutomationRegistryWorkbench();

  await flushPromises();
  expect(workbench.selectedLedgerRunId.value).toBe('20260404153000');
});
```

- [ ] **Step 2: Run the focused composable tests and verify they fail before the new API/composable exists**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`

Expected: FAIL because the business acceptance composable and results-center query handoff do not exist yet.

- [ ] **Step 3: Add business acceptance types and API methods**

```typescript
export interface BusinessAcceptanceRunRequest {
  packageCode: string;
  environmentCode: string;
  accountTemplateCode: string;
  moduleCodes: string[];
}

export function launchBusinessAcceptanceRun(body: BusinessAcceptanceRunRequest) {
  return request<BusinessAcceptanceRunLaunch>('/api/report/business-acceptance/runs', {
    method: 'POST',
    body
  });
}
```

```typescript
export function getBusinessAcceptanceResult(runId: string, packageCode: string) {
  return request<BusinessAcceptanceResult>(
    `/api/report/business-acceptance/results/${encodeURIComponent(runId)}?packageCode=${encodeURIComponent(packageCode)}`,
    { method: 'GET' }
  );
}
```

- [ ] **Step 4: Implement `useBusinessAcceptanceWorkbench` and results-center query preselection**

```typescript
watch(
  () => route.query.runId,
  (runId) => {
    const normalized = typeof runId === 'string' ? runId.trim() : '';
    if (normalized) {
      void selectLedgerRun(normalized, { silent: true });
    }
  },
  { immediate: true }
);
```

`useBusinessAcceptanceWorkbench` should:

- load packages and account templates on mount,
- keep `selectedPackage`, `selectedEnvironment`, `selectedAccountTemplate`, and `selectedModuleCodes`,
- POST the launch request,
- poll `/runs/{jobId}` until `completed`, `failed`, or `blocked`,
- redirect to `/business-acceptance/results/:runId?packageCode=...` when the job finishes with a `runId`,
- expose a `goToAutomationResults(runId)` action that routes to `/automation-results?runId=...`.

- [ ] **Step 5: Re-run the focused composable suite**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`

Expected: PASS for package loading, launch polling, and runId handoff.

- [ ] **Step 6: Commit the frontend orchestration layer**

```bash
git add spring-boot-iot-ui/src/types/businessAcceptance.ts spring-boot-iot-ui/src/api/businessAcceptance.ts spring-boot-iot-ui/src/composables/useBusinessAcceptanceWorkbench.ts spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts spring-boot-iot-ui/src/views/AutomationResultsView.vue spring-boot-iot-ui/src/__tests__/composables/useAutomationRegistryWorkbench.test.ts
git commit -m "feat: add business acceptance frontend orchestration"
```

## Task 6: Build the business-facing launch page and result page

**Files:**
- Create: `spring-boot-iot-ui/src/components/BusinessAcceptancePackagePanel.vue`
- Create: `spring-boot-iot-ui/src/components/BusinessAcceptanceRunConfigPanel.vue`
- Create: `spring-boot-iot-ui/src/components/BusinessAcceptanceResultSummaryPanel.vue`
- Create: `spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue`
- Create: `spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue`
- Create: `spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Add failing view-contract checks for the launch page and result page**

```typescript
it('keeps the business acceptance launch page limited to package cards, light config, and latest summary', () => {
  const source = readView('BusinessAcceptanceWorkbenchView.vue');

  expect(source).toContain('<BusinessAcceptancePackagePanel');
  expect(source).toContain('<BusinessAcceptanceRunConfigPanel');
  expect(source).toContain('一键执行验收');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationRegistryPanel');
});

it('keeps the business acceptance result page focused on pass/fail conclusion and module drill-down', () => {
  const source = readView('BusinessAcceptanceResultView.vue');

  expect(source).toContain('<BusinessAcceptanceResultSummaryPanel');
  expect(source).toContain('<BusinessAcceptanceModuleResultPanel');
  expect(source).toContain('进入结果与基线中心');
  expect(source).not.toContain('<AutomationExecutionConfigPanel');
});
```

- [ ] **Step 2: Run the focused view-contract suite and verify it fails**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because the new business views do not exist yet.

- [ ] **Step 3: Implement the launch page with only the approved business controls**

```vue
<BusinessAcceptanceRunConfigPanel
  :environment-options="environmentOptions"
  :account-templates="accountTemplates"
  :module-options="selectedPackageModules"
  :selected-environment="selectedEnvironment"
  :selected-account-template="selectedAccountTemplate"
  :selected-module-codes="selectedModuleCodes"
  @update:environment="selectedEnvironment = $event"
  @update:account-template="selectedAccountTemplate = $event"
  @update:module-codes="selectedModuleCodes = $event"
  @launch="launchSelectedPackage"
/>
```

Launch-page rules:

- Use `StandardPageShell`, `StandardWorkbenchPanel`, `PanelCard`, `MetricCard`, `StandardInlineState`, and `StandardButton`.
- The first screen must show package cards, environment selector, account-template selector, module multi-select, and the latest result summary for the selected package.
- Do not expose executor type, CLI command, blocker level, or registry raw JSON.

- [ ] **Step 4: Implement the result page with three layers: overall conclusion, module conclusions, and failure detail drill-down**

```vue
<BusinessAcceptanceResultSummaryPanel
  :status="result.status"
  :passed-module-count="result.passedModuleCount"
  :failed-module-count="result.failedModuleCount"
  :failed-module-names="result.failedModuleNames"
  :duration-text="result.durationText"
  @open-automation-results="goToAutomationResults(result.runId)"
/>

<BusinessAcceptanceModuleResultPanel
  :modules="result.modules"
  :active-module-code="activeModuleCode"
  @select-module="activeModuleCode = $event"
/>
```

Result-page rules:

- First layer: `是否通过`, `哪些模块没过`, `总耗时`.
- Second layer: one card or list item per module, including `suggestedDirection`.
- Third layer: expandable failure details with `stepLabel`, `apiRef`, `pageAction`, `summary`.
- Preserve a secondary action that jumps to `/automation-results?runId=...`.

- [ ] **Step 5: Re-run the focused view-contract suite**

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS for both new business views and the updated quality-workbench landing contract.

- [ ] **Step 6: Commit the business-facing UI**

```bash
git add spring-boot-iot-ui/src/components/BusinessAcceptancePackagePanel.vue spring-boot-iot-ui/src/components/BusinessAcceptanceRunConfigPanel.vue spring-boot-iot-ui/src/components/BusinessAcceptanceResultSummaryPanel.vue spring-boot-iot-ui/src/components/BusinessAcceptanceModuleResultPanel.vue spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue spring-boot-iot-ui/src/views/BusinessAcceptanceResultView.vue spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "feat: add business acceptance workbench pages"
```

## Task 7: Sync docs, build, and run real-environment verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Document the new business-acceptance workflow, API, permissions, and acceptance contract**

Document these exact points:

- `质量工场` now contains `业务验收台 /business-acceptance`.
- Business users choose only `环境 / 账号模板 / 模块范围`.
- One-click launch reuses `config/automation/acceptance-registry.json` and existing runners.
- Business result page shows `是否通过`, `哪些模块没过`, and failure drill-down.
- `/automation-results` remains the deep-link target for raw run ledger and evidence.

- [ ] **Step 2: Check whether `README.md` and `AGENTS.md` need sync, and record the decision in the delivery summary**

If implementation changes the top-level onboarding narrative or branch/routing expectations, update in place; otherwise explicitly note “checked, no change needed” in the final handoff.

- [ ] **Step 3: Run the automated verification suite**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Run: `mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest,AutomationResultControllerTest,AutomationResultQueryServiceImplTest test`

Run: `npm --prefix spring-boot-iot-ui test -- --run src/__tests__/utils/sectionHomes.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts`

Run: `mvn -pl spring-boot-iot-admin -am clean package -DskipTests`

Run: `node scripts/run-quality-gates.mjs`

Expected: PASS across Node script tests, report-module tests, frontend Vitest contracts, admin package build, and repo quality gates.

- [ ] **Step 4: Run the real-environment acceptance flow against the dev baseline**

Run: `powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1`

Run: `powershell -ExecutionPolicy Bypass -File scripts/start-frontend-acceptance.ps1`

Manual verification checklist:

- Login with a role that now sees `质量工场 -> 业务验收台`.
- Select `产品与设备` package.
- Switch environment, account template, and module subset at least once.
- Click `一键执行验收`.
- Wait for the business result page to show `是否通过` and `哪些模块没过`.
- Expand one failed module and confirm `步骤 / 接口 / 页面动作` are visible.
- Click `进入结果与基线中心` and confirm `/automation-results` lands on the same `runId`.

If the environment is blocked, record the blocker as an environment issue and stop; do not substitute H2 or mock acceptance.

- [ ] **Step 5: Commit docs and final verification fixes**

```bash
git add docs/02-业务功能与流程说明.md docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document business acceptance workbench"
```

## Self-Review

### Spec coverage

This plan covers:

1. `业务验收台` 独立入口、质量工场导航与权限开放: Task 1.
2. 四个预置验收包、账号模板、模块范围配置模型: Task 1 and Task 3.
3. 一键执行与现有执行器复用: Task 2 and Task 4.
4. 第一屏显示 `是否通过` / `哪些模块没过`: Task 6.
5. 失败明细展开到 `步骤 / 接口 / 页面动作`: Task 4 and Task 6.
6. 跳转 `结果与基线中心` 并保留运行上下文: Task 5 and Task 6.
7. 环境阻塞、结果聚合失败、真实环境验收与文档同步: Task 4 and Task 7.

### Placeholder scan

No `TODO`, `TBD`, “implement later”, or unnamed API/route placeholders remain. All package names, route paths, endpoint paths, and verification commands are concrete.

### Type consistency

The plan uses one stable naming set end-to-end:

- `packageCode`
- `environmentCode`
- `accountTemplateCode`
- `moduleCode`
- `jobId`
- `runId`

The backend VO names and the frontend type names mirror the same business entities so launch, polling, and result rendering do not drift.
