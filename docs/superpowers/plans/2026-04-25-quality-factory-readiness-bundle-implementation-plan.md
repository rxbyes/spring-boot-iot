# Quality Factory Readiness Bundle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the acceptance readiness CLI and simplify the quality workbench / business acceptance layouts so the quality factory feels balanced, minimal, and release-ready.

**Architecture:** Reuse the existing coverage matrix and coverage diff libraries instead of adding a second reporting pipeline, then layer a thin readiness aggregator on top. On the frontend, keep the existing quality factory routes and shared workbench components, but rebalance the page composition into more symmetric bands and calmer card layouts.

**Tech Stack:** Node.js ESM scripts, Vue 3 SFCs, Vitest, Node test runner

---

### Task 1: Add readiness CLI tests first

**Files:**
- Create: `scripts/auto/acceptance-readiness-lib.mjs`
- Create: `scripts/auto/run-acceptance-readiness.mjs`
- Create: `scripts/auto/acceptance-readiness.test.mjs`
- Modify: `scripts/auto/acceptance-coverage.test.mjs`

- [ ] **Step 1: Write the failing readiness library tests**

```js
test('buildAcceptanceReadinessReport returns failed when policy errors exist', () => {
  const report = buildAcceptanceReadinessReport({
    coverage: {
      summary: { totalScenarios: 3, totalPackages: 2, missingScenarioRefs: 0, metadataMissingScenarios: 0, hasGaps: false },
      policyEvaluation: { status: 'failed', summary: { errors: 1, warnings: 0 } }
    },
    diff: { summary: { status: 'unchanged', scenarioDelta: 0, packageDelta: 0, policyErrorsDelta: 0, policyWarningsDelta: 0 } }
  });

  assert.equal(report.status, 'failed');
  assert.equal(report.exitCode, 1);
});

test('buildAcceptanceReadinessReport returns warning for mixed diff', () => {
  const report = buildAcceptanceReadinessReport({
    coverage: {
      summary: { totalScenarios: 3, totalPackages: 2, missingScenarioRefs: 0, metadataMissingScenarios: 0, hasGaps: false },
      policyEvaluation: { status: 'passed', summary: { errors: 0, warnings: 0 } }
    },
    diff: { summary: { status: 'mixed', scenarioDelta: 1, packageDelta: 0, policyErrorsDelta: 0, policyWarningsDelta: 1 } }
  });

  assert.equal(report.status, 'warning');
  assert.equal(report.exitCode, 0);
});
```

- [ ] **Step 2: Run the new test file and verify RED**

Run: `node --test scripts/auto/acceptance-readiness.test.mjs`

Expected: FAIL because `acceptance-readiness-lib.mjs` and its exports do not exist yet.

- [ ] **Step 3: Add CLI coverage tests before implementation**

```js
test('runAcceptanceReadinessCli writes readiness artifacts and skips diff without history', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'acceptance-readiness-'));
  // write minimal registry, packages, policy fixtures
  const result = await runAcceptanceReadinessCli({
    workspaceRoot,
    argv: ['--skip-diff', `--registry-path=${registryPath}`, `--packages-path=${packagesPath}`, `--coverage-policy-path=${policyPath}`]
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.report.diff.skipped, true);
  assert.match(await fs.readFile(result.markdownPath, 'utf8'), /# Acceptance Readiness/);
});
```

- [ ] **Step 4: Run the targeted test set and verify RED**

Run: `node --test scripts/auto/acceptance-readiness.test.mjs scripts/auto/acceptance-coverage.test.mjs`

Expected: FAIL with missing import / missing export errors for readiness helpers.

- [ ] **Step 5: Commit the red test skeleton**

```bash
git add scripts/auto/acceptance-readiness.test.mjs scripts/auto/acceptance-coverage.test.mjs
git commit -m "test: cover acceptance readiness bundle"
```

### Task 2: Implement the readiness aggregator

**Files:**
- Create: `scripts/auto/acceptance-readiness-lib.mjs`
- Create: `scripts/auto/run-acceptance-readiness.mjs`
- Modify: `scripts/auto/generate-acceptance-coverage.mjs`
- Modify: `scripts/auto/acceptance-coverage-lib.mjs`
- Test: `scripts/auto/acceptance-readiness.test.mjs`

- [ ] **Step 1: Implement the minimal readiness report builder**

```js
export function buildAcceptanceReadinessReport({ generatedAt = new Date().toISOString(), inputs = {}, coverage, diff, skipReason = '' } = {}) {
  const policyErrors = Number(coverage?.policyEvaluation?.summary?.errors || 0);
  const policyWarnings = Number(coverage?.policyEvaluation?.summary?.warnings || 0);
  const diffStatus = diff?.summary?.status || '';

  let status = 'passed';
  if (policyErrors > 0 || diffStatus === 'regressed') {
    status = 'failed';
  } else if (policyWarnings > 0 || diffStatus === 'mixed') {
    status = 'warning';
  }

  return {
    generatedAt,
    status,
    exitCode: status === 'failed' ? 1 : 0,
    inputs,
    coverage,
    diff: diff || { skipped: true, skippedReason: skipReason || 'Diff was skipped.' },
    nextActions: buildNextActions({ status, policyErrors, policyWarnings, diffStatus, skipReason })
  };
}
```

- [ ] **Step 2: Implement markdown rendering and artifact writing**

```js
export function renderAcceptanceReadinessMarkdown(report) {
  return [
    '# Acceptance Readiness',
    '',
    `- Status: \`${report.status}\``,
    `- Generated At: \`${report.generatedAt}\``,
    `- Coverage JSON: \`${report.coverage.jsonPath}\``,
    `- Coverage Markdown: \`${report.coverage.markdownPath}\``,
    report.diff?.skipped ? `- Coverage Diff: skipped (${report.diff.skippedReason})` : `- Coverage Diff JSON: \`${report.diff.jsonPath}\``,
    '',
    '## Next Actions',
    '',
    ...report.nextActions.map((line) => `- ${line}`)
  ].join('\n');
}
```

- [ ] **Step 3: Implement the CLI wrapper by composing existing coverage and diff CLIs**

```js
export async function runAcceptanceReadinessCli({ argv = process.argv.slice(2), workspaceRoot = process.cwd() } = {}) {
  const options = parseReadinessArgs(argv);
  const coverageResult = await runCoverageCli({
    workspaceRoot,
    argv: buildCoverageArgs(options)
  });

  const diffResult = options.skipDiff
    ? null
    : await maybeRunCoverageDiff({ workspaceRoot, options, coverageResult });

  const report = buildAcceptanceReadinessReport({
    inputs: resolvedInputs,
    coverage: {
      jsonPath: coverageResult.jsonPath,
      markdownPath: coverageResult.markdownPath,
      summary: coverageResult.matrix.summary,
      policyEvaluation: coverageResult.matrix.policyEvaluation
    },
    diff: diffResult?.diff,
    skipReason: diffResult?.skipReason || ''
  });

  return writeAcceptanceReadinessArtifacts({ workspaceRoot, options, report });
}
```

- [ ] **Step 4: Run the readiness-focused tests and verify GREEN**

Run: `node --test scripts/auto/acceptance-readiness.test.mjs scripts/auto/acceptance-coverage.test.mjs`

Expected: PASS

- [ ] **Step 5: Commit the readiness implementation**

```bash
git add scripts/auto/acceptance-readiness-lib.mjs scripts/auto/run-acceptance-readiness.mjs scripts/auto/acceptance-readiness.test.mjs scripts/auto/generate-acceptance-coverage.mjs scripts/auto/acceptance-coverage-lib.mjs
git commit -m "feat: add acceptance readiness bundle"
```

### Task 3: Lock the new quality factory layout with tests first

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
- Modify: `spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue`
- Modify: `spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/components/BusinessAcceptancePackagePanel.vue`
- Modify: `spring-boot-iot-ui/src/components/BusinessAcceptanceRunConfigPanel.vue`

- [ ] **Step 1: Add failing view assertions for the simplified layout**

```ts
it('keeps the quality workbench landing page in a symmetric overview plus entry grid layout', () => {
  const source = readView('QualityWorkbenchLandingView.vue');

  expect(source).toContain('quality-workbench-landing__hero');
  expect(source).toContain('quality-workbench-landing__summary-grid');
  expect(source).toContain('quality-workbench-landing__entry-grid');
});

it('keeps the business acceptance view in a balanced summary and action layout', () => {
  const source = readView('BusinessAcceptanceWorkbenchView.vue');

  expect(source).toContain('business-acceptance-workbench__hero');
  expect(source).toContain('business-acceptance-workbench__balanced-grid');
  expect(source).toContain('business-acceptance-workbench__aside');
});
```

- [ ] **Step 2: Run the frontend view test and verify RED**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: FAIL because the new layout class names and structure are not present yet.

- [ ] **Step 3: Redesign the quality workbench landing page with a simpler symmetric composition**

```vue
<section class="quality-workbench-landing__hero">
  <div class="quality-workbench-landing__hero-copy">
    <h2>质量工场</h2>
    <p>把业务验收、执行准备和结果复盘收成三类稳定入口。</p>
  </div>
  <div class="quality-workbench-landing__summary-grid">
    <MetricCard ... />
  </div>
</section>
```

- [ ] **Step 4: Redesign the business acceptance page and its child panels to be calmer and more balanced**

```vue
<section class="business-acceptance-workbench__hero">
  <div class="business-acceptance-workbench__hero-copy">...</div>
  <div class="business-acceptance-workbench__hero-metrics">...</div>
</section>

<section class="business-acceptance-workbench__balanced-grid">
  <div class="business-acceptance-workbench__main">...</div>
  <aside class="business-acceptance-workbench__aside">...</aside>
</section>
```

- [ ] **Step 5: Run the view test and verify GREEN**

Run: `pnpm vitest run spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

Expected: PASS

- [ ] **Step 6: Commit the layout redesign**

```bash
git add spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts spring-boot-iot-ui/src/views/QualityWorkbenchLandingView.vue spring-boot-iot-ui/src/views/BusinessAcceptanceWorkbenchView.vue spring-boot-iot-ui/src/components/BusinessAcceptancePackagePanel.vue spring-boot-iot-ui/src/components/BusinessAcceptanceRunConfigPanel.vue
git commit -m "feat: simplify quality factory layouts"
```

### Task 4: Update docs and run verification

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Document the readiness command and its boundary**

```md
- `node scripts/auto/run-acceptance-readiness.mjs` 会聚合覆盖矩阵、策略门禁和覆盖趋势，输出 `acceptance-readiness-*.json/.md`。
- readiness 只回答“自动化资产是否具备验收准备状态”，不替代真实 `dev` 环境业务验收。
```

- [ ] **Step 2: Run the focused verification commands**

Run:

```bash
node --test scripts/auto/acceptance-readiness.test.mjs scripts/auto/acceptance-coverage.test.mjs scripts/auto/acceptance-coverage-diff.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
pnpm vitest run spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
node scripts/auto/run-acceptance-readiness.mjs --coverage-policy-path=config/automation/acceptance-coverage-policy.json
node scripts/docs/check-topology.mjs
git diff --check
```

Expected:

```text
All targeted tests pass, readiness command exits 0 or 1 according to fixture state, topology check passes, diff check is clean.
```

- [ ] **Step 3: Commit docs and final polish**

```bash
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document acceptance readiness bundle"
```
