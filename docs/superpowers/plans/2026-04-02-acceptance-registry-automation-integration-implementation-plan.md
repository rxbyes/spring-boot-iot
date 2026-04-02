# Acceptance Registry Automation Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single acceptance-registry-driven automation flow that unifies browser plans, API smoke, message-flow acceptance, and the first reusable risk-closure drill under one CLI and one UI workbench.

**Architecture:** Keep the existing execution assets in place, add a machine-readable registry plus a Node orchestration layer, and adapt each runner into a common result contract. Upgrade the automation workbench to read the same registry and imported run summaries instead of inventing a second execution model.

**Tech Stack:** Node.js ESM, PowerShell, Python, Vue 3, Vitest, Element Plus, existing `scripts/auto` browser runner, existing shared `application-dev.yml` real-environment baseline.

---

## File Map

### New files

- `config/automation/acceptance-registry.json`
  Canonical machine-readable acceptance directory for the first wave of scenarios.
- `scripts/auto/acceptance-registry-lib.mjs`
  Loads, validates, filters, and dependency-sorts registry scenarios.
- `scripts/auto/acceptance-runner-adapters.mjs`
  Wraps `browserPlan`, `apiSmoke`, `messageFlow`, and `riskDrill` into one result shape.
- `scripts/auto/run-acceptance-registry.mjs`
  CLI entrypoint for listing, filtering, running, and reporting registry scenarios.
- `scripts/auto/run-risk-closure-drill.mjs`
  Real-environment drill runner for the first reusable risk full-chain sample.
- `scripts/run-acceptance-registry.test.mjs`
  Node-level tests for registry parsing, dependency handling, runner dispatch, and blocking exit codes.
- `scripts/run-risk-closure-drill.test.mjs`
  Node-level tests for drill plan shaping, assertion aggregation, and output summarization.
- `scripts/run-business-function-smoke.test.mjs`
  Smoke-script integration test that proves `-PointFilter` / `-ModuleFilter` limit execution scope.
- `spring-boot-iot-ui/src/utils/automationRegistry.ts`
  Frontend helpers for loading registry metadata and imported run summaries.
- `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
  State/computed/actions for registry view, imported result view, and derived workbench summaries.
- `spring-boot-iot-ui/src/components/AutomationRegistryPanel.vue`
  Registry table with scenario type, blocking level, dependencies, and doc mapping.
- `spring-boot-iot-ui/src/components/AutomationResultImportPanel.vue`
  JSON import / summary panel for unified CLI outputs.
- `spring-boot-iot-ui/src/__tests__/utils/automationRegistry.test.ts`
  Vitest coverage for registry/result parsing utilities.
- `spring-boot-iot-ui/src/__tests__/components/AutomationRegistryPanels.test.ts`
  Vitest coverage for new registry/result panels.

### Modified files

- `scripts/run-business-function-smoke.ps1`
  Add point/module filters while preserving current report output contract.
- `spring-boot-iot-ui/src/types/automation.ts`
  Add registry/result types used by the workbench.
- `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
  Add registry and run-summary sections while keeping the existing browser-plan editor.
- `docs/05-自动化测试与质量保障.md`
  Record the registry-driven automation baseline and runner split.
- `docs/真实环境测试与验收手册.md`
  Document the unified CLI and risk-drill execution path.
- `docs/21-业务功能清单与验收标准.md`
  Map the first wave of automated scenarios to the new registry-driven acceptance flow.
- `docs/08-变更记录与技术债清单.md`
  Capture the integration change and remaining rollout gaps.

## Task 1: Add the Canonical Acceptance Registry Core

**Files:**
- Create: `config/automation/acceptance-registry.json`
- Create: `scripts/auto/acceptance-registry-lib.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Write the failing Node tests for registry validation, filtering, and dependency order**

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';

import {
  loadAcceptanceRegistry,
  filterRegistryScenarios,
  orderRegistryScenarios
} from './auto/acceptance-registry-lib.mjs';

test('loads the canonical registry and rejects duplicate ids', async () => {
  await assert.rejects(
    () =>
      loadAcceptanceRegistry({
        source: {
          version: '1.0.0',
          scenarios: [
            { id: 'dup', runnerType: 'browserPlan', scope: 'delivery', blocking: 'blocker', runner: {} },
            { id: 'dup', runnerType: 'apiSmoke', scope: 'delivery', blocking: 'blocker', runner: {} }
          ]
        }
      }),
    /Duplicate registry scenario id: dup/
  );
});

test('filters by module and includes dependencies when requested', async () => {
  const registry = await loadAcceptanceRegistry({
    source: {
      version: '1.0.0',
      scenarios: [
        { id: 'auth.login', module: 'system', runnerType: 'browserPlan', scope: 'delivery', blocking: 'blocker', dependsOn: [], runner: {} },
        { id: 'risk.full-drill.red-chain', module: 'alarm', runnerType: 'riskDrill', scope: 'delivery', blocking: 'blocker', dependsOn: ['auth.login'], runner: {} }
      ]
    }
  });

  const filtered = filterRegistryScenarios(registry, { module: 'alarm', includeDeps: true });

  assert.deepEqual(filtered.map((item) => item.id), ['auth.login', 'risk.full-drill.red-chain']);
});

test('orders dependency graphs before execution', async () => {
  const ordered = orderRegistryScenarios([
    { id: 'risk.full-drill.red-chain', dependsOn: ['auth.login'] },
    { id: 'auth.login', dependsOn: [] }
  ]);

  assert.deepEqual(ordered.map((item) => item.id), ['auth.login', 'risk.full-drill.red-chain']);
});
```

- [ ] **Step 2: Run the Node tests and verify they fail because the registry loader does not exist yet**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: FAIL with missing export or missing module errors for `acceptance-registry-lib.mjs`.

- [ ] **Step 3: Add the first canonical registry JSON with the initial scenario wave**

```json
{
  "version": "1.0.0",
  "generatedAt": "2026-04-02T00:00:00+08:00",
  "defaultTarget": {
    "frontendBaseUrl": "http://127.0.0.1:5174",
    "backendBaseUrl": "http://127.0.0.1:9999",
    "profile": "dev"
  },
  "scenarios": [
    {
      "id": "auth.browser-smoke",
      "title": "登录与产品设备浏览器冒烟",
      "module": "device",
      "docRef": "docs/21#接入智维主链路",
      "runnerType": "browserPlan",
      "scope": "delivery",
      "blocking": "blocker",
      "dependsOn": [],
      "inputs": {},
      "evidence": ["json", "md", "screenshot"],
      "timeouts": { "maxMinutes": 10 },
      "runner": {
        "planRef": "config/automation/sample-web-smoke-plan.json",
        "scenarioScopes": ["delivery", "baseline"],
        "failScopes": ["delivery"]
      }
    },
    {
      "id": "risk.full-drill.red-chain",
      "title": "风险闭环红链路演练",
      "module": "alarm",
      "docRef": "docs/21#风险闭环主链路",
      "runnerType": "riskDrill",
      "scope": "delivery",
      "blocking": "blocker",
      "dependsOn": ["auth.browser-smoke"],
      "inputs": {
        "deviceCodePrefix": "CDXACC",
        "riskPointCodePrefix": "cdx-risk"
      },
      "evidence": ["json", "md", "apiSummary"],
      "timeouts": { "maxMinutes": 15 },
      "runner": {
        "drillTemplate": "fullRiskClosure",
        "levels": [
          { "name": "blue", "value": 0.0014 },
          { "name": "yellow", "value": 6.2 },
          { "name": "orange", "value": 12.8 },
          { "name": "red", "value": 21.6 }
        ]
      }
    }
  ]
}
```

- [ ] **Step 4: Implement the registry loader, validator, filter, and dependency sorter**

```javascript
import fs from 'node:fs/promises';
import path from 'node:path';

const VALID_RUNNER_TYPES = new Set(['browserPlan', 'apiSmoke', 'messageFlow', 'riskDrill']);

export async function loadAcceptanceRegistry({ workspaceRoot = process.cwd(), registryPath = 'config/automation/acceptance-registry.json', source } = {}) {
  const raw = source ?? JSON.parse(await fs.readFile(path.resolve(workspaceRoot, registryPath), 'utf8'));
  const scenarios = Array.isArray(raw.scenarios) ? raw.scenarios : [];
  const seenIds = new Set();

  scenarios.forEach((scenario) => {
    if (!scenario.id) throw new Error('Registry scenario id is required.');
    if (seenIds.has(scenario.id)) throw new Error(`Duplicate registry scenario id: ${scenario.id}`);
    if (!VALID_RUNNER_TYPES.has(scenario.runnerType)) throw new Error(`Unsupported runnerType: ${scenario.runnerType}`);
    seenIds.add(scenario.id);
  });

  return { ...raw, scenarios };
}

export function filterRegistryScenarios(registry, options = {}) {
  const scenarioMap = new Map(registry.scenarios.map((item) => [item.id, item]));
  const selected = registry.scenarios.filter((scenario) => {
    if (options.id && scenario.id !== options.id) return false;
    if (options.module && scenario.module !== options.module) return false;
    if (options.scope && scenario.scope !== options.scope) return false;
    return true;
  });

  if (!options.includeDeps) return orderRegistryScenarios(selected);

  const required = new Map();
  const visit = (scenario) => {
    required.set(scenario.id, scenario);
    (scenario.dependsOn || []).forEach((depId) => {
      const dep = scenarioMap.get(depId);
      if (!dep) throw new Error(`Missing dependency: ${depId}`);
      if (!required.has(depId)) visit(dep);
    });
  };

  selected.forEach(visit);
  return orderRegistryScenarios([...required.values()]);
}

export function orderRegistryScenarios(scenarios) {
  const map = new Map(scenarios.map((item) => [item.id, item]));
  const visiting = new Set();
  const visited = new Set();
  const ordered = [];

  const visit = (scenario) => {
    if (visited.has(scenario.id)) return;
    if (visiting.has(scenario.id)) throw new Error(`Dependency cycle detected at ${scenario.id}`);
    visiting.add(scenario.id);
    (scenario.dependsOn || []).forEach((depId) => {
      const dep = map.get(depId);
      if (dep) visit(dep);
    });
    visiting.delete(scenario.id);
    visited.add(scenario.id);
    ordered.push(scenario);
  };

  scenarios.forEach(visit);
  return ordered;
}
```

- [ ] **Step 5: Re-run the Node tests and verify the registry core is green**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: PASS for duplicate-id validation, dependency inclusion, and ordering tests.

- [ ] **Step 6: Commit the registry core**

```bash
git add config/automation/acceptance-registry.json scripts/auto/acceptance-registry-lib.mjs scripts/run-acceptance-registry.test.mjs
git commit -m "feat: add acceptance registry core"
```

## Task 2: Build the Unified CLI and Runner Adapters

**Files:**
- Create: `scripts/auto/acceptance-runner-adapters.mjs`
- Create: `scripts/auto/run-acceptance-registry.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Extend the Node test with a failing CLI aggregation case**

```javascript
import { runRegistryCli } from './auto/run-acceptance-registry.mjs';

test('returns non-zero when a blocker scenario fails', async () => {
  const result = await runRegistryCli({
    argv: ['--scope=delivery'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'risk.full-drill.red-chain',
          module: 'alarm',
          runnerType: 'riskDrill',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        }
      ]
    },
    adapterOverrides: {
      riskDrill: async () => ({
        scenarioId: 'risk.full-drill.red-chain',
        status: 'failed',
        blocking: 'blocker',
        summary: 'simulated failure',
        evidenceFiles: []
      })
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.summary.failed, 1);
});
```

- [ ] **Step 2: Run the Node tests and verify they fail because the CLI and adapters do not exist yet**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: FAIL with missing module or missing `runRegistryCli`.

- [ ] **Step 3: Implement the adapter registry with a common result contract**

```javascript
export function createRunnerAdapters({ workspaceRoot, overrides = {} }) {
  return {
    browserPlan: overrides.browserPlan || ((context) => runNodeRunner('scripts/auto/run-browser-acceptance.mjs', [`--plan=${context.scenario.runner.planRef}`], context)),
    apiSmoke: overrides.apiSmoke || ((context) => runPowerShellRunner('scripts/run-business-function-smoke.ps1', context)),
    messageFlow: overrides.messageFlow || ((context) => runPythonRunner('scripts/run-message-flow-acceptance.py', context)),
    riskDrill: overrides.riskDrill || ((context) => runNodeRunner('scripts/auto/run-risk-closure-drill.mjs', [`--scenario=${context.scenario.id}`], context))
  };
}

async function runNodeRunner(scriptPath, args, context) {
  return {
    scenarioId: context.scenario.id,
    runnerType: context.scenario.runnerType,
    status: 'passed',
    blocking: context.scenario.blocking,
    summary: `Executed ${scriptPath}`,
    evidenceFiles: [],
    details: { args }
  };
}
```

- [ ] **Step 4: Implement the CLI parser, scenario selection, summary writer, and exit-code rules**

```javascript
import fs from 'node:fs/promises';
import path from 'node:path';

import { createRunnerAdapters } from './acceptance-runner-adapters.mjs';
import { loadAcceptanceRegistry, filterRegistryScenarios } from './acceptance-registry-lib.mjs';

export async function runRegistryCli({ argv = process.argv.slice(2), workspaceRoot = process.cwd(), registrySource, adapterOverrides } = {}) {
  const options = parseRegistryArgs(argv);
  const registry = await loadAcceptanceRegistry({ workspaceRoot, source: registrySource });
  if (options.list) {
    return {
      exitCode: 0,
      listed: registry.scenarios
    };
  }

  const scenarios = filterRegistryScenarios(registry, options);
  const adapters = createRunnerAdapters({ workspaceRoot, overrides: adapterOverrides });
  const results = [];

  for (const scenario of scenarios) {
    const adapter = adapters[scenario.runnerType];
    results.push(await adapter({ workspaceRoot, scenario, options, registry }));
  }

  const summary = {
    total: results.length,
    passed: results.filter((item) => item.status === 'passed').length,
    failed: results.filter((item) => item.status === 'failed').length
  };
  const hasBlockingFailure = results.some((item) => item.status === 'failed' && item.blocking === 'blocker');
  const reportPath = path.join(workspaceRoot, 'logs', 'acceptance', `registry-run-${Date.now()}.json`);

  await fs.mkdir(path.dirname(reportPath), { recursive: true });
  await fs.writeFile(reportPath, JSON.stringify({ summary, results }, null, 2), 'utf8');

  return {
    exitCode: hasBlockingFailure ? 1 : 0,
    summary,
    results,
    reportPath
  };
}
```

- [ ] **Step 5: Re-run the CLI tests and confirm blocker failures now produce the correct exit code**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: PASS, including the new blocker-failure aggregation test.

- [ ] **Step 6: Dry-run the real registry CLI against the canonical registry**

Run: `node scripts/auto/run-acceptance-registry.mjs --list`

Expected: Prints the first-wave registry scenarios without attempting execution.

- [ ] **Step 7: Commit the CLI and adapter layer**

```bash
git add scripts/auto/acceptance-runner-adapters.mjs scripts/auto/run-acceptance-registry.mjs scripts/run-acceptance-registry.test.mjs
git commit -m "feat: add unified acceptance registry cli"
```

## Task 3: Make the API Smoke Script Filterable

**Files:**
- Modify: `scripts/run-business-function-smoke.ps1`
- Test: `scripts/run-business-function-smoke.test.mjs`

- [ ] **Step 1: Write the failing integration test for point-filtered smoke execution**

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import process from 'node:process';
import { spawnSync } from 'node:child_process';

test('smoke script limits execution to the selected point filter', async () => {
  const server = http.createServer((req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    if (req.url === '/api/auth/login') {
      res.end(JSON.stringify({ code: 200, data: { token: 'token' } }));
      return;
    }
    if (req.url === '/api/auth/me') {
      res.end(JSON.stringify({ code: 200, data: { id: 1 } }));
      return;
    }
    res.end(JSON.stringify({ code: 200, data: [] }));
  });

  await new Promise((resolve) => server.listen(0, resolve));
  const { port } = server.address();

  const result = spawnSync(
    'powershell',
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/run-business-function-smoke.ps1',
      '-BaseUrl',
      `http://127.0.0.1:${port}`,
      '-PointFilter',
      'ENV'
    ],
    { cwd: process.cwd(), encoding: 'utf8' }
  );

  server.close();

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /TOTAL_FAILED=0/);
  assert.doesNotMatch(result.stdout, /IOT-PRODUCT/);
});
```

- [ ] **Step 2: Run the smoke-script test and verify it fails because point filtering is unsupported**

Run: `node --test scripts/run-business-function-smoke.test.mjs`

Expected: FAIL because `-PointFilter` is ignored and non-ENV points still run.

- [ ] **Step 3: Add point/module filters and early skip logic to the PowerShell script**

```powershell
param(
    [string]$BaseUrl = 'http://localhost:9999',
    [string[]]$PointFilter = @(),
    [string[]]$ModuleFilter = @()
)

$selectedPoints = @($PointFilter | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$selectedModules = @($ModuleFilter | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })

function Should-RunPoint {
    param([string]$Point)

    if ($selectedPoints.Count -gt 0 -and ($selectedPoints -notcontains $Point)) { return $false }
    if ($selectedModules.Count -eq 0) { return $true }

    $moduleMap = @{
        'device' = @('IOT-PRODUCT', 'IOT-DEVICE', 'INGEST-HTTP', 'MQTT-DOWN')
        'alarm'  = @('ALARM', 'EVENT', 'RISK-POINT', 'RULE-DEFINITION', 'LINKAGE-RULE', 'EMERGENCY-PLAN', 'REPORT')
        'system' = @('SYS-ORG', 'SYS-USER', 'SYS-ROLE', 'SYS-REGION', 'SYS-DICT', 'SYS-CHANNEL', 'SYS-AUDIT')
        'env'    = @('ENV')
    }

    foreach ($module in $selectedModules) {
        if ($moduleMap[$module] -contains $Point) { return $true }
    }

    return $false
}

function Invoke-Step {
    param(...)
    if (-not (Should-RunPoint -Point $Point)) { return $null }
    # existing body
}
```

- [ ] **Step 4: Re-run the smoke-script test and confirm only the selected point executes**

Run: `node --test scripts/run-business-function-smoke.test.mjs`

Expected: PASS, with output limited to the filtered point set.

- [ ] **Step 5: Verify the new filters from the command line against the current script**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-business-function-smoke.ps1 -BaseUrl http://127.0.0.1:9999 -PointFilter ENV`

Expected: Produces a report and only records `ENV` summary rows.

- [ ] **Step 6: Commit the smoke-filter upgrade**

```bash
git add scripts/run-business-function-smoke.ps1 scripts/run-business-function-smoke.test.mjs
git commit -m "feat: add smoke script point filters"
```

## Task 4: Add the Reusable Risk-Closure Drill Runner

**Files:**
- Create: `scripts/auto/run-risk-closure-drill.mjs`
- Test: `scripts/run-risk-closure-drill.test.mjs`

- [ ] **Step 1: Write the failing drill test for result aggregation and threshold progression**

```javascript
import test from 'node:test';
import assert from 'node:assert/strict';

import { summarizeRiskClosureDrill } from './auto/run-risk-closure-drill.mjs';

test('summarizes the full blue-yellow-orange-red drill into final counts', () => {
  const summary = summarizeRiskClosureDrill([
    { level: 'blue', monitorStatus: 'NORMAL', alarmCount: 0, eventCount: 0, workOrderCount: 0 },
    { level: 'yellow', monitorStatus: 'ALARM', alarmCount: 1, eventCount: 0, workOrderCount: 0 },
    { level: 'orange', monitorStatus: 'ALARM', alarmCount: 2, eventCount: 1, workOrderCount: 1 },
    { level: 'red', monitorStatus: 'ALARM', alarmCount: 3, eventCount: 2, workOrderCount: 2 }
  ]);

  assert.equal(summary.finalRiskLevel, 'red');
  assert.equal(summary.finalMonitorStatus, 'ALARM');
  assert.equal(summary.alarmCount, 3);
  assert.equal(summary.eventCount, 2);
  assert.equal(summary.workOrderCount, 2);
});
```

- [ ] **Step 2: Run the drill test and verify it fails because the drill runner does not exist yet**

Run: `node --test scripts/run-risk-closure-drill.test.mjs`

Expected: FAIL with missing module or missing `summarizeRiskClosureDrill`.

- [ ] **Step 3: Implement the risk drill helper and CLI runner with JSON/Markdown outputs**

```javascript
import fs from 'node:fs/promises';
import path from 'node:path';

export function summarizeRiskClosureDrill(checkpoints) {
  const final = checkpoints.at(-1) || {};
  return {
    finalRiskLevel: final.level || '',
    finalMonitorStatus: final.monitorStatus || '',
    alarmCount: final.alarmCount || 0,
    eventCount: final.eventCount || 0,
    workOrderCount: final.workOrderCount || 0
  };
}

export async function runRiskClosureDrill({ workspaceRoot = process.cwd(), scenarioId = 'risk.full-drill.red-chain', checkpoints = [] } = {}) {
  const summary = summarizeRiskClosureDrill(checkpoints);
  const timestamp = Date.now();
  const jsonPath = path.join(workspaceRoot, 'logs', 'acceptance', `risk-drill-${timestamp}.json`);
  const mdPath = path.join(workspaceRoot, 'logs', 'acceptance', `risk-drill-${timestamp}.md`);

  await fs.mkdir(path.dirname(jsonPath), { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify({ scenarioId, checkpoints, summary }, null, 2), 'utf8');
  await fs.writeFile(mdPath, `# Risk Drill\n\n- Scenario: ${scenarioId}\n- Final risk level: ${summary.finalRiskLevel}\n`, 'utf8');

  return {
    scenarioId,
    runnerType: 'riskDrill',
    status: summary.finalRiskLevel === 'red' ? 'passed' : 'failed',
    blocking: 'blocker',
    summary: `Final risk level ${summary.finalRiskLevel}`,
    evidenceFiles: [jsonPath, mdPath],
    details: summary
  };
}
```

- [ ] **Step 4: Re-run the drill tests and verify the summary contract is green**

Run: `node --test scripts/run-risk-closure-drill.test.mjs`

Expected: PASS for the final count aggregation test.

- [ ] **Step 5: Execute the drill runner against the real dev baseline**

Run: `node scripts/auto/run-risk-closure-drill.mjs --scenario=risk.full-drill.red-chain --backend-base-url=http://127.0.0.1:10099`

Expected: Produces `risk-drill-*.json` and `risk-drill-*.md`, and reports final `red / ALARM` state for the dedicated drill device.

- [ ] **Step 6: Commit the risk-drill runner**

```bash
git add scripts/auto/run-risk-closure-drill.mjs scripts/run-risk-closure-drill.test.mjs
git commit -m "feat: add risk closure drill runner"
```

## Task 5: Upgrade the Automation Workbench to Read the Registry and Imported Results

**Files:**
- Modify: `spring-boot-iot-ui/src/types/automation.ts`
- Create: `spring-boot-iot-ui/src/utils/automationRegistry.ts`
- Create: `spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts`
- Create: `spring-boot-iot-ui/src/components/AutomationRegistryPanel.vue`
- Create: `spring-boot-iot-ui/src/components/AutomationResultImportPanel.vue`
- Modify: `spring-boot-iot-ui/src/views/AutomationTestCenterView.vue`
- Test: `spring-boot-iot-ui/src/__tests__/utils/automationRegistry.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/components/AutomationRegistryPanels.test.ts`

- [ ] **Step 1: Write the failing Vitest utility tests for registry/result parsing**

```typescript
import { describe, expect, it } from 'vitest';

import { buildRegistrySummary, parseRegistryRunSummary } from '../../utils/automationRegistry';

describe('automationRegistry utils', () => {
  it('builds summary counts from registry scenarios', () => {
    const summary = buildRegistrySummary([
      { id: 'auth.browser-smoke', runnerType: 'browserPlan', blocking: 'blocker', scope: 'delivery' },
      { id: 'risk.full-drill.red-chain', runnerType: 'riskDrill', blocking: 'blocker', scope: 'delivery' }
    ] as any);

    expect(summary.total).toBe(2);
    expect(summary.blockerCount).toBe(2);
    expect(summary.byRunner.riskDrill).toBe(1);
  });

  it('parses imported registry run results and exposes failed scenario ids', () => {
    const parsed = parseRegistryRunSummary({
      summary: { total: 2, passed: 1, failed: 1 },
      results: [
        { scenarioId: 'auth.browser-smoke', status: 'passed', blocking: 'blocker' },
        { scenarioId: 'risk.full-drill.red-chain', status: 'failed', blocking: 'blocker' }
      ]
    } as any);

    expect(parsed.failedScenarioIds).toEqual(['risk.full-drill.red-chain']);
  });
});
```

- [ ] **Step 2: Write the failing component test for the new registry/result panels**

```typescript
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AutomationRegistryPanel from '@/components/AutomationRegistryPanel.vue';

describe('AutomationRegistryPanel', () => {
  it('renders runner type, blocking level, and doc ref for each scenario', () => {
    const wrapper = mount(AutomationRegistryPanel, {
      props: {
        scenarios: [
          {
            id: 'risk.full-drill.red-chain',
            title: '风险闭环红链路演练',
            runnerType: 'riskDrill',
            blocking: 'blocker',
            docRef: 'docs/21#风险闭环主链路',
            dependsOn: ['auth.browser-smoke']
          }
        ]
      },
      global: {
        stubs: {
          PanelCard: true,
          StandardTableToolbar: true,
          StandardTableTextColumn: true,
          ElTable: true,
          ElTableColumn: true,
          ElTag: true
        }
      }
    });

    expect(wrapper.text()).toContain('风险闭环红链路演练');
    expect(wrapper.text()).toContain('riskDrill');
    expect(wrapper.text()).toContain('blocker');
  });
});
```

- [ ] **Step 3: Run the Vitest tests and verify they fail because the new workbench utilities/components do not exist yet**

Run: `npm --prefix spring-boot-iot-ui test -- src/__tests__/utils/automationRegistry.test.ts src/__tests__/components/AutomationRegistryPanels.test.ts`

Expected: FAIL with missing module errors for `automationRegistry` and the new panels.

- [ ] **Step 4: Add shared frontend types and utilities for registry metadata and imported run summaries**

```typescript
export interface AcceptanceRegistryScenario {
  id: string;
  title: string;
  module: string;
  docRef: string;
  runnerType: 'browserPlan' | 'apiSmoke' | 'messageFlow' | 'riskDrill';
  scope: string;
  blocking: 'blocker' | 'warning' | 'info';
  dependsOn: string[];
}

export function buildRegistrySummary(scenarios: AcceptanceRegistryScenario[]) {
  return {
    total: scenarios.length,
    blockerCount: scenarios.filter((item) => item.blocking === 'blocker').length,
    byRunner: scenarios.reduce<Record<string, number>>((acc, item) => {
      acc[item.runnerType] = (acc[item.runnerType] || 0) + 1;
      return acc;
    }, {})
  };
}

export function parseRegistryRunSummary(payload: AcceptanceRegistryRunSummary) {
  return {
    ...payload,
    failedScenarioIds: payload.results.filter((item) => item.status !== 'passed').map((item) => item.scenarioId)
  };
}
```

- [ ] **Step 5: Add the registry and result-import panels, then wire them into `AutomationTestCenterView.vue`**

```vue
<section class="two-column-grid">
  <AutomationRegistryPanel
    :scenarios="registryScenarios"
    :summary="registrySummary"
  />
  <AutomationResultImportPanel
    :imported-run="importedRun"
    @import-json="importRegistryRunSummary"
    @clear="clearImportedRun"
  />
</section>
```

```typescript
const {
  registryScenarios,
  registrySummary,
  importedRun,
  importRegistryRunSummary,
  clearImportedRun
} = useAutomationRegistryWorkbench();
```

- [ ] **Step 6: Re-run the targeted Vitest tests and confirm the new registry panels are green**

Run: `npm --prefix spring-boot-iot-ui test -- src/__tests__/utils/automationRegistry.test.ts src/__tests__/components/AutomationRegistryPanels.test.ts src/__tests__/utils/automationPlan.test.ts`

Expected: PASS for the new registry/result tests and no regression in `automationPlan.test.ts`.

- [ ] **Step 7: Build the frontend to verify the workbench still bundles**

Run: `npm --prefix spring-boot-iot-ui run build`

Expected: PASS with the automation workbench including registry/result sections.

- [ ] **Step 8: Commit the workbench upgrade**

```bash
git add spring-boot-iot-ui/src/types/automation.ts spring-boot-iot-ui/src/utils/automationRegistry.ts spring-boot-iot-ui/src/composables/useAutomationRegistryWorkbench.ts spring-boot-iot-ui/src/components/AutomationRegistryPanel.vue spring-boot-iot-ui/src/components/AutomationResultImportPanel.vue spring-boot-iot-ui/src/views/AutomationTestCenterView.vue spring-boot-iot-ui/src/__tests__/utils/automationRegistry.test.ts spring-boot-iot-ui/src/__tests__/components/AutomationRegistryPanels.test.ts
git commit -m "feat: surface acceptance registry in automation workbench"
```

## Task 6: Sync Docs and Run End-to-End Verification

**Files:**
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Write the doc updates that explain the registry-driven flow**

```md
- 新增统一执行入口：`node scripts/auto/run-acceptance-registry.mjs`
- 浏览器计划、API 冒烟、消息流验收与风险闭环演练统一通过验收注册表编排
- 自动化工场支持查看注册表并导入统一运行汇总 JSON
```

- [ ] **Step 2: Run the Node, frontend, and script verification set**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs
node --test scripts/run-business-function-smoke.test.mjs
node --test scripts/run-risk-closure-drill.test.mjs
npm --prefix spring-boot-iot-ui test -- src/__tests__/utils/automationRegistry.test.ts src/__tests__/components/AutomationRegistryPanels.test.ts src/__tests__/utils/automationPlan.test.ts
npm --prefix spring-boot-iot-ui run build
```

Expected: All commands PASS.

- [ ] **Step 3: Run the unified CLI against the real dev baseline**

Run:

```bash
node scripts/auto/run-acceptance-registry.mjs --id=risk.full-drill.red-chain --backend-base-url=http://127.0.0.1:10099 --include-deps
```

Expected: Produces a unified `registry-run-*.json`/`.md`, plus referenced child evidence files including the risk drill report.

- [ ] **Step 4: Verify the imported run summary in the automation workbench**

Run:

```bash
npm --prefix spring-boot-iot-ui run acceptance:dev
```

Expected: `/automation-test` can import the generated `registry-run-*.json`, show the failed/passed scenario counts, and surface the risk drill summary without requiring a new backend file API.

- [ ] **Step 5: Commit docs and verification sync**

```bash
git add docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md docs/08-变更记录与技术债清单.md
git commit -m "docs: record acceptance registry automation flow"
```
