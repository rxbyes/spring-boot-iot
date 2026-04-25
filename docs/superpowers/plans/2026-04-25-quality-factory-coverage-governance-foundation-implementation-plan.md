# Quality Factory Coverage Governance Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first platform-maturity slice for quality factory coverage governance so registry scenarios and business packages can produce a coverage matrix with gaps, tiers, runner distribution, and metadata readiness.

**Architecture:** Keep the existing registry/package truth sources and add a read-only Node coverage generator. The generator loads `acceptance-registry.json` and `business-acceptance-packages.json`, derives scenario/package coverage, reports missing references and missing P1/P2 metadata, and writes JSON/Markdown artifacts under `logs/acceptance` unless explicit output paths are passed. No database schema, UI API, or second execution engine is added in this slice.

**Tech Stack:** Node.js ESM, Node test runner, existing automation registry JSON, existing business acceptance packages JSON, Markdown/JSON evidence artifacts.

---

## Scope

This plan implements the first **Stage 3: all-purpose automation platform** foundation:

- Coverage matrix generator for registry scenarios and business packages.
- P0/P1 tier inference from package codes and explicit `scenario.priority`.
- Runner, owner-domain, blocking, and metadata-readiness summaries.
- Gap detection for missing scenario references, unreferenced scenarios, and missing P1/P2 metadata.
- CLI output to JSON/Markdown and optional `--fail-on-gaps` quality gate.
- Documentation for using the matrix before adding CI, database indexing, heatmaps, or trend analytics.

This plan does not add CI schedules, database result archival, frontend heatmap UI, flaky scoring, or notification delivery.

## File Structure

- Create: `scripts/auto/acceptance-coverage-lib.mjs`
  - Pure functions to build coverage matrix and render Markdown.
- Create: `scripts/auto/generate-acceptance-coverage.mjs`
  - CLI wrapper that loads canonical files and writes coverage artifacts.
- Create: `scripts/auto/acceptance-coverage.test.mjs`
  - TDD tests for matrix derivation and CLI artifact writing.
- Modify: `docs/05-自动化测试与质量保障.md`
  - Add coverage governance command and judging rules.
- Modify: `docs/真实环境测试与验收手册.md`
  - Add coverage artifact checklist.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Record coverage matrix as Stage 3 platform foundation.

---

### Task 1: Add Failing Coverage Matrix Tests

**Files:**
- Create: `scripts/auto/acceptance-coverage.test.mjs`

- [ ] **Step 1: Write matrix derivation test**

Create a test that imports `buildCoverageMatrix` from `acceptance-coverage-lib.mjs` and asserts:

```js
const matrix = buildCoverageMatrix({
  registry: {
    version: '1.0.0',
    scenarios: [
      { id: 'auth.browser-smoke', module: 'device', runnerType: 'browserPlan', scope: 'delivery', blocking: 'blocker', runner: {} },
      { id: 'protocol.p1', module: 'protocol-governance', runnerType: 'browserPlan', scope: 'delivery', blocking: 'warning', ownerDomain: 'protocol-governance', priority: 'P1', failureCategory: 'business-assertion', dataSetup: { strategy: 'autotest-prefix' }, cleanupPolicy: { strategy: 'retain-autotest-prefix' }, runner: {} },
      { id: 'object.p1.missing-meta', module: 'object-insight', runnerType: 'browserPlan', scope: 'delivery', blocking: 'warning', priority: 'P1', runner: {} },
      { id: 'orphan.baseline', module: 'system', runnerType: 'apiSmoke', scope: 'baseline', blocking: 'warning', runner: {} }
    ]
  },
  packages: {
    packages: [
      { packageCode: 'platform-p0-full-flow', packageName: 'P0', modules: [{ moduleCode: 'login-auth', moduleName: '登录', scenarioRefs: ['auth.browser-smoke'] }] },
      { packageCode: 'protocol-governance-p1', packageName: '协议 P1', modules: [{ moduleCode: 'protocol', moduleName: '协议治理', scenarioRefs: ['protocol.p1', 'missing.scenario'] }] },
      { packageCode: 'object-insight-p1', packageName: '对象 P1', modules: [{ moduleCode: 'object', moduleName: '对象洞察', scenarioRefs: ['object.p1.missing-meta'] }] }
    ]
  }
});
```

Expected:

- `matrix.summary.totalScenarios === 4`
- `matrix.summary.totalPackages === 3`
- `matrix.coverageByPriority.P0.total === 1`
- `matrix.coverageByPriority.P1.total === 2`
- `matrix.gaps.missingScenarioRefs[0].scenarioRef === 'missing.scenario'`
- `matrix.gaps.missingMetadata` contains `object.p1.missing-meta`
- `matrix.gaps.unreferencedScenarios` contains `orphan.baseline`

- [ ] **Step 2: Write CLI artifact test**

Create a test that imports `runCoverageCli` from `generate-acceptance-coverage.mjs`, writes temp registry/package files, runs:

```js
await runCoverageCli({
  workspaceRoot,
  argv: [
    `--registry-path=${registryPath}`,
    `--packages-path=${packagesPath}`,
    `--json-out=${jsonOut}`,
    `--md-out=${mdOut}`,
    '--fail-on-gaps'
  ]
});
```

Expected:

- exit code is `1` when gaps exist
- JSON output includes `summary`
- Markdown output includes `# Acceptance Coverage Matrix`

- [ ] **Step 3: Verify red**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs
```

Expected: FAIL with module not found.

- [ ] **Step 4: Commit red tests**

```bash
git add scripts/auto/acceptance-coverage.test.mjs
git commit -m "test: guard acceptance coverage matrix"
```

### Task 2: Implement Coverage Matrix Library And CLI

**Files:**
- Create: `scripts/auto/acceptance-coverage-lib.mjs`
- Create: `scripts/auto/generate-acceptance-coverage.mjs`
- Test: `scripts/auto/acceptance-coverage.test.mjs`

- [ ] **Step 1: Implement pure matrix builder**

Implement:

```js
export function buildCoverageMatrix({ registry, packages } = {}) {}
export function renderCoverageMarkdown(matrix) {}
```

The matrix must include `summary`, `coverageByPriority`, `coverageByRunnerType`, `coverageByOwnerDomain`, `scenarios`, `packages`, and `gaps`.

- [ ] **Step 2: Implement CLI**

Implement:

```js
export async function runCoverageCli({ argv = process.argv.slice(2), workspaceRoot = process.cwd() } = {}) {}
```

Supported args:

- `--registry-path=...`
- `--packages-path=...`
- `--json-out=...`
- `--md-out=...`
- `--fail-on-gaps`

Default outputs must land in `logs/acceptance/acceptance-coverage-<timestamp>.json` and `.md`.

- [ ] **Step 3: Verify green**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add scripts/auto/acceptance-coverage-lib.mjs scripts/auto/generate-acceptance-coverage.mjs scripts/auto/acceptance-coverage.test.mjs
git commit -m "feat: add acceptance coverage matrix generator"
```

### Task 3: Run Canonical Coverage Generator And Wire Documentation

**Files:**
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Run canonical generator**

Run:

```bash
node scripts/auto/generate-acceptance-coverage.mjs
```

Expected: exit `0` and output JSON with generated artifact paths. The canonical registry may report unreferenced baseline scenarios, but the command without `--fail-on-gaps` should not fail.

- [ ] **Step 2: Document the command**

Add the command and interpretation rules:

```bash
node scripts/auto/generate-acceptance-coverage.mjs
node scripts/auto/generate-acceptance-coverage.mjs --fail-on-gaps
```

Explain that `--fail-on-gaps` is for CI/readiness gates after teams agree that all gaps are actionable.

- [ ] **Step 3: Run docs checks**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document acceptance coverage governance"
```

### Task 4: Final Verification

**Files:**
- None

- [ ] **Step 1: Run final checks**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
node scripts/auto/generate-acceptance-coverage.mjs
node scripts/docs/check-topology.mjs
git diff --check
git status --short --branch
```

Expected: tests and docs checks pass; worktree is clean after commits.

- [ ] **Step 2: Summarize**

Report generated capabilities, verification evidence, and whether canonical coverage still reports non-blocking gaps.
