# Quality Factory Coverage Policy Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable readiness policy gate for acceptance coverage reports without changing the default coverage matrix behavior.

**Architecture:** Extend the existing Node coverage library with a pure `evaluateCoveragePolicy(matrix, policy)` function and policy rendering helpers. Extend the CLI to load an optional `--policy-path`, embed `policyEvaluation` into JSON/Markdown artifacts, and return non-zero only when policy error rules fail or when existing `--fail-on-gaps` fails. Add one default policy JSON under `config/automation` and document how it differs from real execution acceptance.

**Tech Stack:** Node.js ESM, Node test runner, JSON config, existing acceptance registry/packages, Markdown/JSON evidence artifacts.

---

## Scope

This plan implements the coverage readiness policy slice approved in:

```text
docs/superpowers/specs/2026-04-25-quality-factory-coverage-policy-design.md
```

Included:

- Default policy config: `config/automation/acceptance-coverage-policy.json`
- Policy evaluator with strict validation for known rules and severity values.
- CLI `--policy-path=...` support.
- JSON/Markdown report output with `policyEvaluation`.
- Tests for policy evaluation, CLI exit behavior, and canonical policy pass.
- Documentation updates in root and quality/acceptance docs.

Not included:

- CI configuration.
- Database archival.
- Frontend UI/heatmap.
- Changes to `run-acceptance-registry.mjs` execution behavior.

## File Structure

- Create: `config/automation/acceptance-coverage-policy.json`
  - Default quality-factory readiness policy.
- Modify: `scripts/auto/acceptance-coverage-lib.mjs`
  - Add policy validation, policy evaluation, report summary helpers, and Markdown rendering.
- Modify: `scripts/auto/generate-acceptance-coverage.mjs`
  - Add `--policy-path`, load policy file, attach evaluation, and compute exit code.
- Modify: `scripts/auto/acceptance-coverage.test.mjs`
  - Add policy evaluator and CLI contract tests.
- Modify: `README.md`
  - Document policy gate command.
- Modify: `AGENTS.md`
  - Record current quality factory coverage policy baseline.
- Modify: `docs/05-自动化测试与质量保障.md`
  - Add policy command and judging rules.
- Modify: `docs/真实环境测试与验收手册.md`
  - Add readiness gate checklist.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Add policy artifact to acceptance output suggestions.

---

### Task 1: Add Failing Policy Tests

**Files:**
- Modify: `scripts/auto/acceptance-coverage.test.mjs`

- [ ] **Step 1: Import policy evaluator**

Change the import block to include `evaluateCoveragePolicy`:

```js
import {
  buildCoverageMatrix,
  evaluateCoveragePolicy
} from './acceptance-coverage-lib.mjs';
```

- [ ] **Step 2: Add policy fixtures**

Append helpers after `createFixtureSources()`:

```js
function createPolicyFixture(overrides = {}) {
  return {
    version: '1.0.0',
    policyName: 'fixture-policy',
    rules: {
      missingScenarioRefs: {
        severity: 'error',
        allowScenarioRefs: []
      },
      unreferencedScenarios: {
        severity: 'warning',
        allowScenarioIds: [],
        allowScopes: ['baseline']
      },
      metadata: {
        severity: 'error',
        requiredPriorities: ['P1', 'P2']
      },
      minimumScenarioCountByPriority: {
        severity: 'error',
        minimums: {
          P0: 1,
          P1: 2
        }
      },
      requiredRunnerTypes: {
        severity: 'warning',
        runnerTypes: ['browserPlan', 'apiSmoke']
      },
      ...overrides.rules
    }
  };
}

function createPolicyCleanSources() {
  return {
    registry: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'auth.browser-smoke',
          module: 'device',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          runner: {}
        },
        {
          id: 'protocol.p1',
          module: 'protocol-governance',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'warning',
          ownerDomain: 'protocol-governance',
          priority: 'P1',
          failureCategory: 'business-assertion',
          dataSetup: { strategy: 'autotest-prefix' },
          cleanupPolicy: { strategy: 'retain-autotest-prefix' },
          runner: {}
        },
        {
          id: 'object.p1',
          module: 'object-insight',
          runnerType: 'apiSmoke',
          scope: 'delivery',
          blocking: 'warning',
          ownerDomain: 'object-insight',
          priority: 'P1',
          failureCategory: 'business-assertion',
          dataSetup: { strategy: 'reuse-seeded-devices' },
          cleanupPolicy: { strategy: 'read-only' },
          runner: {}
        },
        {
          id: 'baseline.not-packaged',
          module: 'system',
          runnerType: 'apiSmoke',
          scope: 'baseline',
          blocking: 'warning',
          runner: {}
        }
      ]
    },
    packages: {
      packages: [
        {
          packageCode: 'platform-p0-full-flow',
          packageName: 'P0',
          modules: [
            {
              moduleCode: 'login-auth',
              moduleName: '登录',
              scenarioRefs: ['auth.browser-smoke']
            }
          ]
        },
        {
          packageCode: 'protocol-governance-p1',
          packageName: '协议 P1',
          modules: [
            {
              moduleCode: 'protocol',
              moduleName: '协议治理',
              scenarioRefs: ['protocol.p1']
            }
          ]
        },
        {
          packageCode: 'object-insight-p1',
          packageName: '对象 P1',
          modules: [
            {
              moduleCode: 'object',
              moduleName: '对象洞察',
              scenarioRefs: ['object.p1']
            }
          ]
        }
      ]
    }
  };
}
```

- [ ] **Step 3: Add evaluator tests**

Append tests:

```js
test('evaluates coverage policy with errors and warnings', () => {
  const matrix = buildCoverageMatrix(createFixtureSources());
  const evaluation = evaluateCoveragePolicy(matrix, createPolicyFixture());

  assert.equal(evaluation.status, 'failed');
  assert.equal(evaluation.summary.errors, 2);
  assert.equal(evaluation.summary.warnings, 0);
  assert.equal(
    evaluation.results.find((item) => item.ruleId === 'missingScenarioRefs').status,
    'failed'
  );
  assert.equal(
    evaluation.results.find((item) => item.ruleId === 'metadata').status,
    'failed'
  );
  assert.equal(
    evaluation.results.find((item) => item.ruleId === 'unreferencedScenarios')
      .status,
    'passed'
  );
});

test('allows warning-only policy failures without failing overall status', () => {
  const matrix = buildCoverageMatrix(createPolicyCleanSources());
  const evaluation = evaluateCoveragePolicy(
    matrix,
    createPolicyFixture({
      rules: {
        requiredRunnerTypes: {
          severity: 'warning',
          runnerTypes: ['browserPlan', 'messageFlow']
        }
      }
    })
  );

  assert.equal(evaluation.status, 'warning');
  assert.equal(evaluation.summary.errors, 0);
  assert.equal(evaluation.summary.warnings, 1);
  assert.equal(
    evaluation.results.find((item) => item.ruleId === 'requiredRunnerTypes')
      .status,
    'failed'
  );
});
```

- [ ] **Step 4: Add CLI policy artifact tests**

Append a test:

```js
test('writes policy evaluation and exits on policy errors', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-coverage-policy-')
  );
  const registryPath = path.join(workspaceRoot, 'acceptance-registry.json');
  const packagesPath = path.join(
    workspaceRoot,
    'business-acceptance-packages.json'
  );
  const policyPath = path.join(workspaceRoot, 'acceptance-coverage-policy.json');
  const jsonOut = path.join(workspaceRoot, 'coverage.json');
  const mdOut = path.join(workspaceRoot, 'coverage.md');
  const { registry, packages } = createFixtureSources();

  await fs.writeFile(registryPath, JSON.stringify(registry, null, 2), 'utf8');
  await fs.writeFile(packagesPath, JSON.stringify(packages, null, 2), 'utf8');
  await fs.writeFile(
    policyPath,
    JSON.stringify(createPolicyFixture(), null, 2),
    'utf8'
  );

  const result = await runCoverageCli({
    workspaceRoot,
    argv: [
      `--registry-path=${registryPath}`,
      `--packages-path=${packagesPath}`,
      `--policy-path=${policyPath}`,
      `--json-out=${jsonOut}`,
      `--md-out=${mdOut}`
    ]
  });

  assert.equal(result.exitCode, 1);
  const json = JSON.parse(await fs.readFile(jsonOut, 'utf8'));
  const markdown = await fs.readFile(mdOut, 'utf8');

  assert.equal(json.policyEvaluation.status, 'failed');
  assert.match(markdown, /## Policy Evaluation/);
});
```

- [ ] **Step 5: Verify red**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs
```

Expected: FAIL because `evaluateCoveragePolicy` and `--policy-path` are not implemented.

- [ ] **Step 6: Commit red tests**

```bash
git add scripts/auto/acceptance-coverage.test.mjs
git commit -m "test: guard acceptance coverage policy gate"
```

### Task 2: Implement Policy Evaluator

**Files:**
- Modify: `scripts/auto/acceptance-coverage-lib.mjs`

- [ ] **Step 1: Add policy constants and helpers**

Add near the existing constants:

```js
const POLICY_RULE_IDS = new Set([
  'missingScenarioRefs',
  'unreferencedScenarios',
  'metadata',
  'minimumScenarioCountByPriority',
  'requiredRunnerTypes'
]);
const POLICY_SEVERITIES = new Set(['error', 'warning']);
```

Add helpers before `buildCoverageMatrix`:

```js
function requirePolicyObject(policy) {
  if (!policy || typeof policy !== 'object' || Array.isArray(policy)) {
    throw new Error('Coverage policy must be a JSON object.');
  }
  const rules = policy.rules;
  if (!rules || typeof rules !== 'object' || Array.isArray(rules)) {
    throw new Error('Coverage policy rules must be a JSON object.');
  }
  Object.entries(rules).forEach(([ruleId, rule]) => {
    if (!POLICY_RULE_IDS.has(ruleId)) {
      throw new Error(`Unknown coverage policy rule: ${ruleId}`);
    }
    if (!rule || typeof rule !== 'object' || Array.isArray(rule)) {
      throw new Error(`Coverage policy rule must be an object: ${ruleId}`);
    }
    if (!POLICY_SEVERITIES.has(rule.severity)) {
      throw new Error(`Unsupported coverage policy severity for ${ruleId}: ${rule.severity}`);
    }
  });
}

function policyArray(value) {
  return Array.isArray(value) ? value.map(cleanText).filter(Boolean) : [];
}

function createPolicyResult({ ruleId, severity, failedDetails, passMessage, failMessage }) {
  const failed = failedDetails.length > 0;
  return {
    ruleId,
    severity,
    status: failed ? 'failed' : 'passed',
    message: failed ? failMessage : passMessage,
    details: failedDetails
  };
}
```

- [ ] **Step 2: Implement rule evaluators**

Add rule helpers before exported policy function:

```js
function evaluateMissingScenarioRefs(matrix, rule) {
  const allowed = new Set(policyArray(rule.allowScenarioRefs));
  const failedDetails = matrix.gaps.missingScenarioRefs.filter(
    (item) => !allowed.has(item.scenarioRef)
  );
  return createPolicyResult({
    ruleId: 'missingScenarioRefs',
    severity: rule.severity,
    failedDetails,
    passMessage: 'No missing scenario references.',
    failMessage: `${failedDetails.length} missing scenario reference(s) are not allowed.`
  });
}

function evaluateUnreferencedScenarios(matrix, rule) {
  const allowedIds = new Set(policyArray(rule.allowScenarioIds));
  const allowedScopes = new Set(policyArray(rule.allowScopes));
  const failedDetails = matrix.gaps.unreferencedScenarios.filter(
    (item) => !allowedIds.has(item.scenarioId) && !allowedScopes.has(item.scope)
  );
  return createPolicyResult({
    ruleId: 'unreferencedScenarios',
    severity: rule.severity,
    failedDetails,
    passMessage: 'No disallowed unreferenced scenarios.',
    failMessage: `${failedDetails.length} unreferenced scenario(s) require packaging or explicit allowance.`
  });
}

function evaluateMetadata(matrix, rule) {
  const requiredPriorities = new Set(policyArray(rule.requiredPriorities));
  const failedDetails = matrix.gaps.missingMetadata.filter((item) =>
    requiredPriorities.has(item.resolvedPriority)
  );
  return createPolicyResult({
    ruleId: 'metadata',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Required priority metadata is complete.',
    failMessage: `${failedDetails.length} scenario(s) are missing required governance metadata.`
  });
}

function evaluateMinimumScenarioCountByPriority(matrix, rule) {
  const minimums =
    rule.minimums && typeof rule.minimums === 'object' && !Array.isArray(rule.minimums)
      ? rule.minimums
      : {};
  const failedDetails = Object.entries(minimums)
    .map(([priority, minimum]) => ({
      priority,
      minimum: Number(minimum),
      actual: matrix.coverageByPriority[priority]?.total || 0
    }))
    .filter((item) => Number.isFinite(item.minimum) && item.actual < item.minimum);
  return createPolicyResult({
    ruleId: 'minimumScenarioCountByPriority',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Priority scenario minimums are satisfied.',
    failMessage: `${failedDetails.length} priority scenario minimum(s) are not satisfied.`
  });
}

function evaluateRequiredRunnerTypes(matrix, rule) {
  const failedDetails = policyArray(rule.runnerTypes)
    .map((runnerType) => ({
      runnerType,
      actual: matrix.coverageByRunnerType[runnerType]?.total || 0
    }))
    .filter((item) => item.actual === 0);
  return createPolicyResult({
    ruleId: 'requiredRunnerTypes',
    severity: rule.severity,
    failedDetails,
    passMessage: 'Required runner types are represented.',
    failMessage: `${failedDetails.length} required runner type(s) are missing from coverage.`
  });
}
```

- [ ] **Step 3: Export evaluator**

Add after `buildCoverageMatrix`:

```js
export function evaluateCoveragePolicy(matrix, policy) {
  requirePolicyObject(policy);
  const rules = policy.rules;
  const results = Object.entries(rules).map(([ruleId, rule]) => {
    if (ruleId === 'missingScenarioRefs') {
      return evaluateMissingScenarioRefs(matrix, rule);
    }
    if (ruleId === 'unreferencedScenarios') {
      return evaluateUnreferencedScenarios(matrix, rule);
    }
    if (ruleId === 'metadata') {
      return evaluateMetadata(matrix, rule);
    }
    if (ruleId === 'minimumScenarioCountByPriority') {
      return evaluateMinimumScenarioCountByPriority(matrix, rule);
    }
    return evaluateRequiredRunnerTypes(matrix, rule);
  });
  const failed = results.filter((item) => item.status === 'failed');
  const errors = failed.filter((item) => item.severity === 'error').length;
  const warnings = failed.filter((item) => item.severity === 'warning').length;

  return {
    policyName: cleanText(policy.policyName) || 'unnamed-policy',
    policyVersion: cleanText(policy.version) || '1.0.0',
    status: errors > 0 ? 'failed' : warnings > 0 ? 'warning' : 'passed',
    summary: {
      totalRules: results.length,
      passed: results.filter((item) => item.status === 'passed').length,
      failed: failed.length,
      warnings,
      errors
    },
    results
  };
}
```

- [ ] **Step 4: Extend Markdown renderer**

In `renderCoverageMarkdown(matrix)`, after the gaps summary section or before final return, append policy lines only when `matrix.policyEvaluation` exists:

```js
  if (matrix.policyEvaluation) {
    lines.push(
      '',
      '## Policy Evaluation',
      '',
      `- Policy: \`${matrix.policyEvaluation.policyName}\``,
      `- Version: \`${matrix.policyEvaluation.policyVersion}\``,
      `- Status: \`${matrix.policyEvaluation.status}\``,
      '',
      '| Rule | Severity | Status | Message |',
      '|---|---|---|---|'
    );
    matrix.policyEvaluation.results.forEach((result) => {
      lines.push(
        `| ${escapeTableText(result.ruleId)} | ${escapeTableText(
          result.severity
        )} | ${escapeTableText(result.status)} | ${escapeTableText(
          result.message
        )} |`
      );
      if (result.details.length > 0) {
        lines.push(
          `| ${escapeTableText(`${result.ruleId} details`)} | ${escapeTableText(
            result.severity
          )} | ${escapeTableText(result.status)} | ${escapeTableText(
            JSON.stringify(result.details)
          )} |`
        );
      }
    });
  }
```

- [ ] **Step 5: Verify evaluator green**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs
```

Expected: policy evaluator tests pass, CLI policy test still fails until CLI supports `--policy-path`.

### Task 3: Implement CLI Policy Loading And Default Policy

**Files:**
- Create: `config/automation/acceptance-coverage-policy.json`
- Modify: `scripts/auto/generate-acceptance-coverage.mjs`
- Test: `scripts/auto/acceptance-coverage.test.mjs`

- [ ] **Step 1: Add default policy config**

Create `config/automation/acceptance-coverage-policy.json`:

```json
{
  "version": "1.0.0",
  "policyName": "quality-factory-default-readiness",
  "rules": {
    "missingScenarioRefs": {
      "severity": "error",
      "allowScenarioRefs": []
    },
    "unreferencedScenarios": {
      "severity": "warning",
      "allowScenarioIds": [],
      "allowScopes": ["baseline"]
    },
    "metadata": {
      "severity": "error",
      "requiredPriorities": ["P1", "P2"]
    },
    "minimumScenarioCountByPriority": {
      "severity": "error",
      "minimums": {
        "P0": 1,
        "P1": 1
      }
    },
    "requiredRunnerTypes": {
      "severity": "warning",
      "runnerTypes": ["browserPlan", "apiSmoke"]
    }
  }
}
```

- [ ] **Step 2: Import policy evaluator**

Change `generate-acceptance-coverage.mjs` import:

```js
import {
  buildCoverageMatrix,
  evaluateCoveragePolicy,
  renderCoverageMarkdown
} from './acceptance-coverage-lib.mjs';
```

- [ ] **Step 3: Parse policy path**

In `parseCoverageArgs`, add:

```js
    if (arg.startsWith('--policy-path=')) {
      options.policyPath = arg.slice('--policy-path='.length).trim();
      return;
    }
```

- [ ] **Step 4: Load policy and attach evaluation**

In `runCoverageCli`, after building `matrix`, add:

```js
  if (options.policyPath) {
    const policyPath = resolveWorkspacePath(workspaceRoot, options.policyPath);
    const policy = await readJsonFile(policyPath);
    matrix.policyEvaluation = evaluateCoveragePolicy(matrix, policy);
  }
```

Then change exit code:

```js
  const policyHasErrors = (matrix.policyEvaluation?.summary?.errors || 0) > 0;
  const exitCode =
    (options.failOnGaps && matrix.summary.hasGaps) || policyHasErrors ? 1 : 0;
```

- [ ] **Step 5: Include policy summary in CLI stdout**

Add `policyEvaluation: result.matrix.policyEvaluation?.summary` to the printed JSON object:

```js
          policyEvaluation: result.matrix.policyEvaluation?.summary,
```

- [ ] **Step 6: Verify CLI tests green**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs
```

Expected: all tests in the file pass.

- [ ] **Step 7: Verify canonical policy**

Run:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

Expected: exit `0`, summary still reports no gaps, stdout includes `policyEvaluation` with zero errors.

- [ ] **Step 8: Commit implementation**

```bash
git add config/automation/acceptance-coverage-policy.json scripts/auto/acceptance-coverage-lib.mjs scripts/auto/generate-acceptance-coverage.mjs scripts/auto/acceptance-coverage.test.mjs
git commit -m "feat: add acceptance coverage policy gate"
```

### Task 4: Document Policy Gate

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update root README**

In the quality factory coverage command block, add:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

Add one sentence:

```text
  - `--policy-path` 会把覆盖矩阵按默认 readiness policy 评估；policy error 会让命令退出 1，warning 只进入报告。
```

- [ ] **Step 2: Update AGENTS current status**

Extend the `2026-04-25` quality factory coverage paragraph with:

```text
同日默认策略文件 `config/automation/acceptance-coverage-policy.json` 已作为 readiness gate 基线：缺失场景引用、P1/P2 元数据缺口和 P0/P1 最小覆盖不足为 error；baseline 未纳包和 runner 分布缺口先按 warning 进入报告。
```

- [ ] **Step 3: Update docs/05**

In `10.8 质量工场覆盖治理矩阵`, add the policy command:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

Add judging rule:

```text
5. 传入 `--policy-path` 时会追加 `policyEvaluation`；`error` 规则失败时命令退出 `1`，`warning` 规则失败只进入 Markdown/JSON 报告。
```

- [ ] **Step 4: Update real environment manual**

In `12.4 质量工场覆盖治理矩阵检查`, add the policy command and checklist item:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

```text
5. 封板或 CI readiness gate 应优先使用 `--policy-path=config/automation/acceptance-coverage-policy.json`，并检查 `policyEvaluation.summary.errors=0`。
```

- [ ] **Step 5: Update docs/21**

Update the quality factory stage 3 paragraph to mention policy gate:

```text
`config/automation/acceptance-coverage-policy.json` 作为默认准入策略，负责把缺失引用、P1/P2 元数据和最低覆盖要求转为 error/warning 判定。
```

Update output suggestions:

```text
覆盖治理：封板或新增验收包前执行 `node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json`，保留 `acceptance-coverage-*.json/.md`。
```

- [ ] **Step 6: Run docs checks**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: both pass.

- [ ] **Step 7: Commit docs**

```bash
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document acceptance coverage policy gate"
```

### Task 5: Final Verification

**Files:**
- None

- [ ] **Step 1: Run full Node verification**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
```

Expected: all tests pass.

- [ ] **Step 2: Run canonical coverage with policy**

Run:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
```

Expected: exit `0`, `policyEvaluation.errors=0`, generated JSON/Markdown paths are printed.

- [ ] **Step 3: Run docs and whitespace checks**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
git status --short --branch
```

Expected: docs check and diff check pass; worktree is clean after commits.

- [ ] **Step 4: Summarize**

Report:

- New policy file path.
- CLI command.
- Policy behavior for `error` vs `warning`.
- Verification evidence and branch/worktree path.
