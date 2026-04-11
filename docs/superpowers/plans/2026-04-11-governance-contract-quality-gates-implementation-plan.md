# Governance Contract Quality Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a repository-native governance contract quality gate that protects product model governance regressions and wire it into the existing `run-quality-gates` entrypoint.

**Architecture:** Keep `scripts/run-quality-gates.mjs` as the single top-level entrypoint and add a dedicated `scripts/run-governance-contract-gates.mjs` executor for governance-specific regression commands. Wire the new executor into the PowerShell and shell runners, then update repository docs so local quality gates, governance contract tests, and real-environment acceptance remain clearly separated.

**Tech Stack:** Node.js scripts, PowerShell, POSIX shell, Maven, Vitest, Spring Boot 4, Vue 3, Markdown docs.

---

### Task 1: Add the governance contract gate executor and its script tests

**Files:**
- Create: `scripts/run-governance-contract-gates.mjs`
- Create: `scripts/run-governance-contract-gates.test.mjs`

- [ ] **Step 1: Write the failing script tests**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';

import {
  buildGovernanceContractGatePlan
} from './run-governance-contract-gates.mjs';

test('builds backend and frontend governance contract steps with Maven settings', () => {
  const repoRoot = path.resolve(process.cwd());
  const plan = buildGovernanceContractGatePlan({
    repoRoot,
    platform: 'win32',
    hasMavenSettings: true
  });

  assert.equal(plan.logFile, path.join(repoRoot, 'logs', 'governance-contract-gates.log'));
  assert.equal(plan.steps.length, 2);
  assert.equal(plan.steps[0].step, 'backend governance contract tests');
  assert.equal(plan.steps[0].command[0], 'mvn.cmd');
  assert.deepEqual(plan.steps[0].command.slice(1, 4), [
    '-s',
    path.join(repoRoot, '.mvn', 'settings.xml'),
    '-pl'
  ]);
  assert.match(
    plan.steps[0].command.join(' '),
    /GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest/
  );
  assert.deepEqual(plan.steps[1].command, [
    'npm.cmd',
    '--prefix',
    path.join(repoRoot, 'spring-boot-iot-ui'),
    'test',
    '--',
    '--run',
    'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts'
  ]);
});

test('falls back to plain Maven args when settings.xml is absent', () => {
  const repoRoot = path.resolve(process.cwd());
  const plan = buildGovernanceContractGatePlan({
    repoRoot,
    platform: 'linux',
    hasMavenSettings: false
  });

  assert.equal(plan.steps[0].command[0], 'mvn');
  assert.equal(plan.steps[0].command.includes('-s'), false);
});
```

- [ ] **Step 2: Run the script tests to verify they fail**

Run:

```powershell
node --test scripts/run-governance-contract-gates.test.mjs
```

Expected: FAIL because `scripts/run-governance-contract-gates.mjs` and `buildGovernanceContractGatePlan(...)` do not exist yet.

- [ ] **Step 3: Implement the governance gate executor**

```js
#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawn } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const defaultRepoRoot = path.resolve(scriptDir, '..');

export function buildGovernanceContractGatePlan({
  repoRoot = defaultRepoRoot,
  platform = process.platform,
  hasMavenSettings = fs.existsSync(path.join(repoRoot, '.mvn', 'settings.xml'))
} = {}) {
  const uiRoot = path.join(repoRoot, 'spring-boot-iot-ui');
  const logFile = path.join(repoRoot, 'logs', 'governance-contract-gates.log');
  const mvnExecutable = platform === 'win32' ? 'mvn.cmd' : 'mvn';
  const npmExecutable = platform === 'win32' ? 'npm.cmd' : 'npm';
  const mavenCommand = [
    mvnExecutable,
    ...(hasMavenSettings ? ['-s', path.join(repoRoot, '.mvn', 'settings.xml')] : []),
    '-pl',
    'spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm',
    '-am',
    '-DskipTests=false',
    '-Dsurefire.failIfNoSpecifiedTests=false',
    '-Dtest=GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest',
    'test'
  ];

  return {
    repoRoot,
    uiRoot,
    logFile,
    steps: [
      {
        step: 'backend governance contract tests',
        cwd: repoRoot,
        command: mavenCommand
      },
      {
        step: 'frontend product governance tests',
        cwd: repoRoot,
        command: [
          npmExecutable,
          '--prefix',
          uiRoot,
          'test',
          '--',
          '--run',
          'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts'
        ]
      }
    ]
  };
}
```

```js
export async function runGovernanceContractGates(options = {}) {
  const plan = buildGovernanceContractGatePlan(options);
  fs.mkdirSync(path.dirname(plan.logFile), { recursive: true });
  fs.writeFileSync(plan.logFile, '', 'utf8');

  for (const current of plan.steps) {
    await runStep(current, plan.logFile);
  }
}
```

- [ ] **Step 4: Run the script tests to verify they pass**

Run:

```powershell
node --test scripts/run-governance-contract-gates.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/run-governance-contract-gates.mjs scripts/run-governance-contract-gates.test.mjs
git commit -m "test: add governance contract gate executor"
```

### Task 2: Wire the governance gate into the existing quality gate entrypoints

**Files:**
- Modify: `scripts/run-quality-gates.ps1`
- Modify: `scripts/run-quality-gates.sh`
- Modify: `scripts/run-quality-gates.test.mjs`

- [ ] **Step 1: Extend the quality gate runner tests first**

```js
test('quality gate scripts invoke governance contract gates before docs topology check', () => {
  const psScript = fs.readFileSync(path.join(scriptDir, 'run-quality-gates.ps1'), 'utf8');
  const shScript = fs.readFileSync(path.join(scriptDir, 'run-quality-gates.sh'), 'utf8');

  assert.match(psScript, /governance contract gates/);
  assert.match(shScript, /governance contract gates/);
  assert.ok(
    psScript.indexOf('governance contract gates') < psScript.indexOf('docs topology check'),
    'powershell runner should execute governance contract gates before docs topology check'
  );
  assert.ok(
    shScript.indexOf('governance contract gates') < shScript.indexOf('docs topology check'),
    'shell runner should execute governance contract gates before docs topology check'
  );
});
```

```js
test('shell runner exits non-zero and stops before docs check when governance contract gates fail', { skip: process.platform === 'win32' }, () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'quality-gates-governance-'));
  const fakeBin = path.join(tempRoot, 'bin');
  const docsMarker = path.join(tempRoot, 'docs-check.marker');
  const shellScript = path.join(scriptDir, 'run-quality-gates.sh');

  fs.mkdirSync(fakeBin, { recursive: true });
  fs.writeFileSync(path.join(fakeBin, 'mvn'), '#!/usr/bin/env sh\nexit 0\n', { mode: 0o755 });
  fs.writeFileSync(path.join(fakeBin, 'npm'), '#!/usr/bin/env sh\nexit 0\n', { mode: 0o755 });
  fs.writeFileSync(path.join(fakeBin, 'python3'), '#!/usr/bin/env sh\nexit 0\n', { mode: 0o755 });
  fs.writeFileSync(
    path.join(fakeBin, 'node'),
    [
      '#!/usr/bin/env sh',
      'case "$1" in',
      '  *run-governance-contract-gates.mjs) exit 19 ;;',
      `  *check-topology.mjs) echo docs-check >> "${docsMarker}"; exit 0 ;;`,
      '  *) exit 0 ;;',
      'esac',
      ''
    ].join('\n'),
    { mode: 0o755 }
  );

  const result = spawnSync('sh', [shellScript], {
    cwd: path.resolve(scriptDir, '..'),
    env: {
      ...process.env,
      PATH: `${fakeBin}${path.delimiter}${process.env.PATH ?? ''}`
    },
    encoding: 'utf8'
  });

  fs.rmSync(tempRoot, { recursive: true, force: true });
  assert.equal(result.status, 19);
  assert.ok(!fs.existsSync(docsMarker));
});
```

- [ ] **Step 2: Run the quality gate runner tests to verify they fail**

Run:

```powershell
node --test scripts/run-quality-gates.test.mjs
```

Expected: FAIL because neither the PowerShell script nor the shell script invokes `run-governance-contract-gates.mjs` yet.

- [ ] **Step 3: Wire the new executor into the PowerShell and shell runners**

```powershell
Invoke-LoggedCommand -Step 'frontend style guard' -WorkingDirectory $uiRoot -Executable $npmCmd -Arguments @('run', 'style:guard')
$schemaArgs = Get-PythonUnittestArgs -PythonExecutablePath $pythonCmd
Invoke-LoggedCommand -Step 'schema baseline guard' -WorkingDirectory $repoRoot -Executable $pythonCmd -Arguments $schemaArgs
Invoke-LoggedCommand -Step 'governance contract gates' -WorkingDirectory $repoRoot -Executable $nodeCmd -Arguments @('scripts/run-governance-contract-gates.mjs')
Invoke-LoggedCommand -Step 'docs topology check' -WorkingDirectory $repoRoot -Executable $nodeCmd -Arguments @('scripts/docs/check-topology.mjs')
```

```sh
run_step "frontend style guard" "$ui_root" "$npm_cmd" run style:guard
run_step "schema baseline guard" "$repo_root" "$python_cmd" -m unittest scripts/test_risk_point_pending_promotion_schema.py -v
run_step "governance contract gates" "$repo_root" "$node_cmd" scripts/run-governance-contract-gates.mjs
run_step "docs topology check" "$repo_root" "$node_cmd" scripts/docs/check-topology.mjs
```

- [ ] **Step 4: Run the quality gate runner tests to verify they pass**

Run:

```powershell
node --test scripts/run-quality-gates.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add scripts/run-quality-gates.ps1 scripts/run-quality-gates.sh scripts/run-quality-gates.test.mjs
git commit -m "build: wire governance contract gates into quality gates"
```

### Task 3: Update repository docs to formalize the new governance contract gate

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Add the documented local gate entrypoints**

````md
- 本地最小质量门禁脚本：

```bash
node scripts/run-quality-gates.mjs
```

  - 当前已串联 Maven 打包、前端 build、前端静态 guard、治理契约门禁、schema baseline guard 与 docs topology check
  - 如只需验证产品物模型治理主链路，可单独执行：

```bash
node scripts/run-governance-contract-gates.mjs
```
````

- [ ] **Step 2: Update the formal quality-gate and acceptance docs**

````md
统一本地质量门禁入口：

```bash
node scripts/run-quality-gates.mjs
```

治理契约专项门禁入口：

```bash
node scripts/run-governance-contract-gates.mjs
```

说明：
- `run-quality-gates` 当前会在前端静态 guard 与 docs topology check 之间追加治理契约专项门禁。
- 治理契约专项门禁固定覆盖产品物模型 compare/apply、mapping runtime、风险指标目录发布规则、固定复核人链路与 `/products` 工作台关键治理回归。
- 该门禁只服务仓库级最小回归，不替代共享 `dev` 环境真实验收。
````

```md
- 2026-04-11：统一 CI / 契约测试门禁已完成第一轮仓库级收口。新增 `scripts/run-governance-contract-gates.mjs`，并将其接入 `run-quality-gates`；当前专项门禁固定覆盖 `GovernanceApprovalPolicyResolverImplTest / GovernanceApprovalServiceImplTest / ProductModelServiceImplTest / VendorMetricMappingRuntimeServiceImplTest / ProductModelControllerTest / ProductContractReleaseControllerTest / ProductGovernanceApprovalControllerTest / DefaultRiskMetricCatalogPublishRuleTest` 以及 `ProductModelDesignerWorkspace.test.ts`，用于保护深部位移、激光测距、翻斗式雨量计、固定复核人与 mapping runtime 等已收口治理成果。真实 `dev` 环境验收继续通过 acceptance 脚本执行，不并入本地最小门禁。
```

- [ ] **Step 3: Run the docs checks to verify the updates are clean**

Run:

```powershell
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: both commands pass without topology errors or whitespace errors.

- [ ] **Step 4: Commit**

```powershell
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document governance contract quality gates"
```

### Task 4: Verify the new gate end to end and finish the branch state

**Files:**
- Verify only: `scripts/run-governance-contract-gates.mjs`
- Verify only: `scripts/run-quality-gates.mjs`
- Verify only: `logs/governance-contract-gates.log`
- Verify only: `logs/quality-gates.log`

- [ ] **Step 1: Run both node-based script test suites together**

Run:

```powershell
node --test scripts/run-governance-contract-gates.test.mjs scripts/run-quality-gates.test.mjs
```

Expected: PASS, including the new governance gate coverage.

- [ ] **Step 2: Run the governance contract gate directly**

Run:

```powershell
node scripts/run-governance-contract-gates.mjs
```

Expected: PASS, and `logs/governance-contract-gates.log` contains both backend and frontend governance gate steps.

- [ ] **Step 3: Run the full local quality gate entrypoint**

Run:

```powershell
node scripts/run-quality-gates.mjs
```

Expected: PASS, and `logs/quality-gates.log` shows `governance contract gates` before `docs topology check`.

- [ ] **Step 4: Record the final workspace state**

Run:

```powershell
git status --short
git log -4 --oneline --decorate
```

Expected: no unexpected modified tracked files remain, and the last commits show the governance contract gate implementation sequence.
