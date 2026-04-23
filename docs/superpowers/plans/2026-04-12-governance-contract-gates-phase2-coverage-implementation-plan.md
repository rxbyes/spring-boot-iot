# Governance Contract Gates Phase 2 Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the local governance contract gate so it covers the product governance phase-2 workflow semantics without expanding into hosted CI or the global quality gate runner.

**Architecture:** Keep `scripts/run-governance-contract-gates.mjs` as the only local governance contract gate entrypoint. Tighten its scripted coverage by adding the missing frontend compare-table test and the missing backend approval-payload test, then update the script test and quality doc so the gate contract is explicit and regression-safe.

**Tech Stack:** Node.js scripts, Node test runner, Maven, Vitest, Markdown docs.

---

### Task 1: Expand the governance contract gate coverage and lock it with script tests

**Files:**
- Modify: `scripts/run-governance-contract-gates.test.mjs`
- Modify: `scripts/run-governance-contract-gates.mjs`

- [ ] **Step 1: Write the failing script test expectations**

```js
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
    /GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalPayloadsTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest/
  );
  assert.deepEqual(plan.steps[1].command, [
    'npm.cmd',
    '--prefix',
    path.join(repoRoot, 'spring-boot-iot-ui'),
    'test',
    '--',
    '--run',
    'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts',
    'src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts'
  ]);
});
```

- [ ] **Step 2: Run the script test to verify it fails on current gate coverage**

Run:

```powershell
node --test scripts/run-governance-contract-gates.test.mjs
```

Expected: FAIL because the current script still omits `ProductContractGovernanceApprovalPayloadsTest` and `ProductModelGovernanceCompareTable.test.ts`.

- [ ] **Step 3: Implement the minimal gate coverage change**

```js
  const backendCommand = [
    mvnExecutable,
    ...(hasMavenSettings ? ['-s', path.join(repoRoot, '.mvn', 'settings.xml')] : []),
    '-pl',
    'spring-boot-iot-system,spring-boot-iot-device,spring-boot-iot-alarm',
    '-am',
    '-DskipTests=false',
    '-Dsurefire.failIfNoSpecifiedTests=false',
    '-Dtest=GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalPayloadsTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest',
    'test'
  ];
```

```js
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
          'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts',
          'src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts'
        ]
      }
```

- [ ] **Step 4: Run the script test to verify it passes**

Run:

```powershell
node --test scripts/run-governance-contract-gates.test.mjs
```

Expected: PASS with all script tests green.

- [ ] **Step 5: Commit the gate script change**

```bash
git add scripts/run-governance-contract-gates.test.mjs scripts/run-governance-contract-gates.mjs
git commit -m "test: extend governance contract gate phase2 coverage"
```

### Task 2: Update quality docs and verify the real governance gate command

**Files:**
- Modify: `docs/05-自动化测试与质量保障.md`

- [ ] **Step 1: Update the doc wording to match the new gate coverage**

```md
- `scripts/run-governance-contract-gates.mjs` 当前固定覆盖产品物模型治理主线回归：`GovernanceApprovalPolicyResolverImplTest`、`GovernanceApprovalServiceImplTest`、`ProductModelServiceImplTest`、`VendorMetricMappingRuntimeServiceImplTest`、`ProductModelControllerTest`、`ProductContractReleaseControllerTest`、`ProductContractGovernanceApprovalPayloadsTest`、`ProductGovernanceApprovalControllerTest`、`DefaultRiskMetricCatalogPublishRuleTest`，以及前端 `ProductModelDesignerWorkspace.test.ts`、`ProductModelGovernanceCompareTable.test.ts`。`2026-04-12` 起，该专项门禁已显式覆盖二期治理工作流新增语义：`治理阶段总览`、`治理候选快照`、`审批提交回执 / 正式发布结果` 与 `submittedItemCount`。
```

- [ ] **Step 2: Run the real governance contract gate command**

Run:

```powershell
node scripts/run-governance-contract-gates.mjs
```

Expected: PASS, producing `logs/governance-contract-gates.log`.

- [ ] **Step 3: Run the docs topology check**

Run:

```powershell
node scripts/docs/check-topology.mjs
```

Expected: PASS with `Document topology check passed.`

- [ ] **Step 4: Commit the docs update**

```bash
git add docs/05-自动化测试与质量保障.md
git commit -m "docs: update governance contract gate coverage"
```

## 完成标准

1. `scripts/run-governance-contract-gates.test.mjs` 明确锁定后端 `ProductContractGovernanceApprovalPayloadsTest` 与前端 `ProductModelGovernanceCompareTable.test.ts`。
2. `node --test scripts/run-governance-contract-gates.test.mjs` 通过。
3. `node scripts/run-governance-contract-gates.mjs` 通过。
4. `docs/05-自动化测试与质量保障.md` 已把专项门禁覆盖说明同步到二期治理工作流语义。
5. 本轮不改 `run-quality-gates` 行为，不引入托管 CI。
