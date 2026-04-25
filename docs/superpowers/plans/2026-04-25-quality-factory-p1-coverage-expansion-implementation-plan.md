# Quality Factory P1 Coverage Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first P1 quality factory coverage slice so governance, onboarding, object insight, and result-center flows are visible as executable business acceptance packages with precise browser scenario filtering and standard metadata.

**Architecture:** Continue using the existing `business-acceptance-packages.json -> acceptance-registry.json -> run-acceptance-registry.mjs` chain. Add P1 registry metadata (`ownerDomain`, `priority`, `failureCategory`, `dataSetup`, `cleanupPolicy`) and let browser-plan scenarios select exact config-driven scenario keys, avoiding broad plan execution when a P1 module only needs one slice. Keep all results in the existing `registry-run-*.json / .md` evidence model.

**Tech Stack:** Node.js test runner, config-driven browser acceptance runner, JSON automation registry, Vue route selectors, existing Spring Boot report services, real `application-dev.yml` acceptance baseline.

---

## Scope

This plan implements the first **Stage 2: P1 coverage expansion** slice from the approved quality factory test-plan design:

- `product-governance-p1`
- `protocol-governance-p1`
- `device-onboarding-p1`
- `object-insight-p1`
- `automation-results-p1`
- P1 registry metadata normalization and guard tests
- exact browser scenario key filtering for reused browser plans
- dry-run browser plan coverage for new P1 plans
- documentation updates for P1 execution and evidence rules

This plan does not add database tables, CI scheduling, flaky analysis, centralized result indexing, or new production APIs.

## File Structure

- Modify: `scripts/auto/acceptance-registry-lib.mjs`
  - Preserve P1 metadata fields when loading registry scenarios.
- Modify: `scripts/auto/browser-acceptance-core.mjs`
  - Add `scenarioKeys` runtime filtering after scope filtering.
- Modify: `scripts/auto/run-browser-acceptance.mjs`
  - Add `--scenario-keys=a,b,c` CLI option and pass it into the core runner.
- Modify: `scripts/auto/acceptance-runner-adapters.mjs`
  - Pass `scenarioScopes`, `failScopes`, and `scenarioKeys` from registry browser runners to `run-browser-acceptance.mjs`.
- Modify: `scripts/run-browser-acceptance.test.mjs`
  - Guard `--scenario-keys` dry-run filtering and new P1 plans.
- Modify: `scripts/run-acceptance-registry.test.mjs`
  - Guard P1 scenario catalog, metadata, and business packages.
- Modify: `config/automation/acceptance-registry.json`
  - Add P1 scenarios and metadata.
- Modify: `config/automation/business-acceptance-packages.json`
  - Add P1 business acceptance packages.
- Create: `config/automation/device-onboarding-web-smoke-plan.json`
  - Read-side browser smoke plan for `/device-onboarding`.
- Create: `config/automation/object-insight-web-smoke-plan.json`
  - Read-side browser smoke plan for `/insight`.
- Modify: `docs/05-自动化测试与质量保障.md`
  - Add P1 coverage expansion commands and judging rules.
- Modify: `docs/真实环境测试与验收手册.md`
  - Add P1 real-environment dry-run / execution checklist.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Map P1 packages to business acceptance coverage.
- Review: `README.md`
  - Update only if quality factory headline wording needs the P1 slice.

---

### Task 1: Add P1 Registry Metadata And Browser Key Filtering Tests

**Files:**
- Modify: `scripts/run-acceptance-registry.test.mjs`
- Modify: `scripts/run-browser-acceptance.test.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`
- Test: `scripts/run-browser-acceptance.test.mjs`

- [ ] **Step 1: Add failing registry metadata and P1 package tests**

Append tests that assert:

```js
const requiredP1ScenarioIds = [
  'product-governance.contracts.browser-smoke',
  'product-governance.mapping-rules.browser-smoke',
  'protocol-governance.p1.browser-smoke',
  'device-onboarding.p1.browser-smoke',
  'object-insight.p1.browser-smoke',
  'automation-results.p1.browser-smoke'
];
```

Each P1 scenario must expose `priority: 'P1'`, non-empty `ownerDomain`, `failureCategory`, `dataSetup.strategy`, and `cleanupPolicy.strategy`. The package file must expose `product-governance-p1`, `protocol-governance-p1`, `device-onboarding-p1`, `object-insight-p1`, and `automation-results-p1`, and every module reference must resolve to a registry scenario.

- [ ] **Step 2: Add failing browser scenario-key dry-run test**

Append a test that runs:

```js
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/sample-web-smoke-plan.json --scenario-keys=login,product-governance-warning-fallback
```

Expected dry-run output contains only `login` and `product-governance-warning-fallback`.

- [ ] **Step 3: Add failing P1 plan dry-run test**

Append a test that dry-runs:

```js
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/device-onboarding-web-smoke-plan.json
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/object-insight-web-smoke-plan.json
```

Expected output includes `device-onboarding-workbench` and `object-insight-workbench`.

- [ ] **Step 4: Run tests and verify red**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs
```

Expected: FAIL because metadata fields, P1 packages, CLI filtering, and new plans are not implemented.

- [ ] **Step 5: Commit failing tests**

```bash
git add scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs
git commit -m "test: guard quality factory p1 coverage catalog"
```

### Task 2: Preserve P1 Metadata And Add Browser Scenario Key Filtering

**Files:**
- Modify: `scripts/auto/acceptance-registry-lib.mjs`
- Modify: `scripts/auto/browser-acceptance-core.mjs`
- Modify: `scripts/auto/run-browser-acceptance.mjs`
- Modify: `scripts/auto/acceptance-runner-adapters.mjs`
- Test: `scripts/run-browser-acceptance.test.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Preserve registry metadata**

Add these fields to `normalizeScenario()`:

```js
ownerDomain: String(source.ownerDomain || '').trim(),
priority: String(source.priority || '').trim(),
failureCategory: String(source.failureCategory || '').trim(),
dataSetup: source.dataSetup && typeof source.dataSetup === 'object' ? { ...source.dataSetup } : {},
cleanupPolicy: source.cleanupPolicy && typeof source.cleanupPolicy === 'object' ? { ...source.cleanupPolicy } : {}
```

- [ ] **Step 2: Add browser core scenario-key filtering**

Add `scenarioKeys` to runtime options and filter executable scenarios by key when the list is non-empty:

```js
scenarioKeys: rawOptions.scenarioKeys?.length ? [...rawOptions.scenarioKeys] : [],
```

```js
const executableScenarios = createScenarios({ runToken: artifacts.runToken }).filter((scenario) => {
  const scopeMatches = runtimeOptions.scenarioScopes.includes(scenario.scope);
  const keyMatches =
    runtimeOptions.scenarioKeys.length === 0 ||
    runtimeOptions.scenarioKeys.includes(scenario.key);
  return scopeMatches && keyMatches;
});
```

- [ ] **Step 3: Add CLI parsing**

Add `scenarioKeys` to `run-browser-acceptance.mjs` parse args with `--scenario-keys=` and include it in `options`.

- [ ] **Step 4: Pass registry browser runner filters**

In `acceptance-runner-adapters.mjs`, include:

```js
...(context.scenario.runner.scenarioScopes || []).length
  ? [`--scopes=${context.scenario.runner.scenarioScopes.join(',')}`]
  : [],
...(context.scenario.runner.failScopes || []).length
  ? [`--fail-scopes=${context.scenario.runner.failScopes.join(',')}`]
  : [],
...(context.scenario.runner.scenarioKeys || []).length
  ? [`--scenario-keys=${context.scenario.runner.scenarioKeys.join(',')}`]
  : []
```

- [ ] **Step 5: Run targeted tests**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs scripts/run-acceptance-registry.test.mjs
```

Expected: browser scenario-key test passes; P1 catalog tests still fail until registry and plans are added.

- [ ] **Step 6: Commit**

```bash
git add scripts/auto/acceptance-registry-lib.mjs scripts/auto/browser-acceptance-core.mjs scripts/auto/run-browser-acceptance.mjs scripts/auto/acceptance-runner-adapters.mjs
git commit -m "feat: support p1 browser scenario filters"
```

### Task 3: Add P1 Browser Plans And Registry Scenarios

**Files:**
- Create: `config/automation/device-onboarding-web-smoke-plan.json`
- Create: `config/automation/object-insight-web-smoke-plan.json`
- Modify: `config/automation/acceptance-registry.json`
- Test: `scripts/run-browser-acceptance.test.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add device onboarding browser plan**

Create a config-driven plan with scenarios:

```json
[
  {
    "key": "device-onboarding-login",
    "route": "/login",
    "scope": "delivery",
    "readySelector": "#login-submit"
  },
  {
    "key": "device-onboarding-workbench",
    "route": "/device-onboarding",
    "scope": "delivery",
    "readySelector": ".device-onboarding-workbench__summary"
  }
]
```

The workbench scenario asserts `无代码接入台`, `接入案例`, `模板包`, and `批量触发验收`.

- [ ] **Step 2: Add object insight browser plan**

Create a config-driven plan with scenarios:

```json
[
  {
    "key": "object-insight-login",
    "route": "/login",
    "scope": "delivery",
    "readySelector": "#login-submit"
  },
  {
    "key": "object-insight-workbench",
    "route": "/insight",
    "scope": "delivery",
    "readySelector": ".device-insight-view"
  }
]
```

The workbench scenario asserts `对象洞察台`, `请输入设备编码后开始综合分析`, and `开始分析`.

- [ ] **Step 3: Add P1 registry scenarios**

Add these six scenarios to `acceptance-registry.json`:

```json
[
  "product-governance.contracts.browser-smoke",
  "product-governance.mapping-rules.browser-smoke",
  "protocol-governance.p1.browser-smoke",
  "device-onboarding.p1.browser-smoke",
  "object-insight.p1.browser-smoke",
  "automation-results.p1.browser-smoke"
]
```

Every scenario uses `scope: "delivery"`, `priority: "P1"`, `blocking: "warning"`, and a metadata block describing setup/cleanup. Product governance scenarios reuse `sample-web-smoke-plan.json` with `scenarioKeys`; protocol governance reuses `protocol-governance-web-smoke-plan.json`; device onboarding and object insight use the new plans; automation results reuses `quality-factory-web-smoke-plan.json` with `automation-results-workbench`.

- [ ] **Step 4: Run tests**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs scripts/run-acceptance-registry.test.mjs
```

Expected: browser plan tests pass; P1 package test still fails until packages are added.

- [ ] **Step 5: Commit**

```bash
git add config/automation/acceptance-registry.json config/automation/device-onboarding-web-smoke-plan.json config/automation/object-insight-web-smoke-plan.json scripts/run-browser-acceptance.test.mjs
git commit -m "feat: add quality factory p1 browser scenarios"
```

### Task 4: Add P1 Business Acceptance Packages

**Files:**
- Modify: `config/automation/business-acceptance-packages.json`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add P1 packages**

Add:

```json
[
  "product-governance-p1",
  "protocol-governance-p1",
  "device-onboarding-p1",
  "object-insight-p1",
  "automation-results-p1"
]
```

Each package uses `supportedEnvironments: ["dev", "test"]`, `defaultAccountTemplate: "manager-default"` except object insight, which may use `acceptance-default`. Each module must include `fallbackFailure.stepLabel`, `apiRef`, `pageAction`, and `summary`.

- [ ] **Step 2: Run registry tests**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add config/automation/business-acceptance-packages.json scripts/run-acceptance-registry.test.mjs
git commit -m "feat: add quality factory p1 acceptance packages"
```

### Task 5: Document P1 Execution And Verify

**Files:**
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Review: `README.md`

- [ ] **Step 1: Update quality documentation**

Document that P1 packages are warning-grade expansion checks, with these CLI examples:

```bash
node scripts/auto/run-acceptance-registry.mjs --registry-path=config/automation/acceptance-registry.json --package-code=product-governance-p1 --environment-code=dev --account-template=manager-default --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175 --include-deps
node scripts/auto/run-acceptance-registry.mjs --registry-path=config/automation/acceptance-registry.json --package-code=protocol-governance-p1 --environment-code=dev --account-template=manager-default --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175 --include-deps
```

- [ ] **Step 2: Update real environment manual**

Add P1 dry-run and execution evidence checklist:

```bash
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/device-onboarding-web-smoke-plan.json
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/object-insight-web-smoke-plan.json
```

- [ ] **Step 3: Update business function acceptance standard**

Record that P1 packages are not daily blockers but should run before version sealing or when related domains change.

- [ ] **Step 4: Run final verification**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/device-onboarding-web-smoke-plan.json
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/object-insight-web-smoke-plan.json
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: all commands pass. If local frontend or browser dependencies are missing, record the exact missing executable as an environment blocker.

- [ ] **Step 5: Commit**

```bash
git add docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md README.md
git commit -m "docs: document quality factory p1 coverage expansion"
```

### Task 6: Final Status

**Files:**
- None

- [ ] **Step 1: Check worktree status**

Run:

```bash
git status --short --branch
```

Expected: only intentional changes are present, or clean after commits.

- [ ] **Step 2: Summarize evidence**

Report:

1. P1 packages added.
2. P1 registry metadata added.
3. Browser scenario key filtering added.
4. Tests and dry-runs passed or exact environment blockers.
