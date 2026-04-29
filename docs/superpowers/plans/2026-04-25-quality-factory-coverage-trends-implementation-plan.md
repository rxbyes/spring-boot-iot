# Quality Factory Coverage Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an offline acceptance coverage diff CLI that compares two coverage matrix JSON files and writes JSON/Markdown trend evidence.

**Architecture:** Keep the existing coverage matrix generator untouched as the producer. Add a focused diff library that validates two generated matrices, compares scenarios/packages/count buckets/gaps/policy summaries, and renders Markdown. Add a thin CLI wrapper that selects explicit paths or the latest two `logs/acceptance/acceptance-coverage-*.json` files, then writes `acceptance-coverage-diff-*.json/.md`.

**Tech Stack:** Node.js ESM, `node:test`, `node:fs/promises`, existing `scripts/auto` conventions, Markdown documentation.

---

## File Map

- Create: `scripts/auto/acceptance-coverage-diff-lib.mjs`
  - Owns matrix validation, diff calculation, status classification, latest-file selection, Markdown rendering, and artifact writing helpers.
- Create: `scripts/auto/diff-acceptance-coverage.mjs`
  - CLI entrypoint and exported `runCoverageDiffCli`.
- Create: `scripts/auto/acceptance-coverage-diff.test.mjs`
  - Node tests for diff behavior and CLI behavior.
- Modify: `README.md`
  - Adds the trend diff command next to coverage matrix commands.
- Modify: `AGENTS.md`
  - Adds current-state note for coverage trend diff readiness.
- Modify: `docs/05-自动化测试与质量保障.md`
  - Adds usage, output, and status interpretation.
- Modify: `docs/真实环境测试与验收手册.md`
  - Adds封板/周回归 diff evidence guidance.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Adds acceptance standard note for third-stage coverage trend diff.

## Task 1: Add Failing Tests For Coverage Diff Library

**Files:**
- Create: `scripts/auto/acceptance-coverage-diff.test.mjs`
- Create later: `scripts/auto/acceptance-coverage-diff-lib.mjs`

- [ ] **Step 1: Write the failing diff library tests**

Create `scripts/auto/acceptance-coverage-diff.test.mjs` with these imports and fixtures:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  buildCoverageDiff,
  findLatestCoverageMatrixFiles,
  renderCoverageDiffMarkdown
} from './acceptance-coverage-diff-lib.mjs';
import { runCoverageDiffCli } from './diff-acceptance-coverage.mjs';

function createMatrix({
  scenarioIds = [],
  packageCodes = [],
  priorityCounts = {},
  runnerCounts = {},
  ownerCounts = {},
  summary = {},
  policySummary
} = {}) {
  const scenarios = scenarioIds.map((scenarioId) => ({ scenarioId }));
  const packages = packageCodes.map((packageCode) => ({ packageCode }));
  const toBucket = (counts) =>
    Object.fromEntries(
      Object.entries(counts).map(([key, total]) => [
        key,
        { total, scenarioIds: Array.from({ length: total }, (_, index) => `${key}.${index + 1}`) }
      ])
    );
  const matrix = {
    generatedAt: '2026-04-25T10:00:00.000Z',
    summary: {
      totalScenarios: scenarioIds.length,
      totalPackages: packageCodes.length,
      missingScenarioRefs: 0,
      unreferencedScenarios: 0,
      metadataMissingScenarios: 0,
      ...summary
    },
    coverageByPriority: toBucket(priorityCounts),
    coverageByRunnerType: toBucket(runnerCounts),
    coverageByOwnerDomain: toBucket(ownerCounts),
    scenarios,
    packages,
    gaps: {
      missingScenarioRefs: [],
      unreferencedScenarios: [],
      missingMetadata: []
    }
  };
  if (policySummary) {
    matrix.policyEvaluation = {
      status: policySummary.status,
      summary: {
        errors: policySummary.errors,
        warnings: policySummary.warnings
      }
    };
  }
  return matrix;
}
```

Add tests:

```js
test('buildCoverageDiff reports added and removed scenarios and packages', () => {
  const baseline = createMatrix({
    scenarioIds: ['auth.browser-smoke', 'legacy.scenario'],
    packageCodes: ['platform-p0-full-flow'],
    priorityCounts: { P0: 1 },
    runnerCounts: { browserPlan: 1 },
    ownerCounts: { quality: 1 }
  });
  const current = createMatrix({
    scenarioIds: ['auth.browser-smoke', 'object-insight.p1.browser-smoke'],
    packageCodes: ['platform-p0-full-flow', 'object-insight-p1'],
    priorityCounts: { P0: 1, P1: 1 },
    runnerCounts: { browserPlan: 2 },
    ownerCounts: { quality: 1, 'object-insight': 1 }
  });

  const diff = buildCoverageDiff({
    baseline,
    current,
    baselinePath: 'baseline.json',
    currentPath: 'current.json',
    generatedAt: '2026-04-25T10:30:00.000Z'
  });

  assert.deepEqual(diff.changes.scenarios.added, ['object-insight.p1.browser-smoke']);
  assert.deepEqual(diff.changes.scenarios.removed, ['legacy.scenario']);
  assert.deepEqual(diff.changes.packages.added, ['object-insight-p1']);
  assert.deepEqual(diff.changes.packages.removed, []);
  assert.equal(diff.changes.coverageByPriority.P1.delta, 1);
  assert.equal(diff.summary.status, 'mixed');
});

test('buildCoverageDiff marks P0 coverage loss as regressed', () => {
  const baseline = createMatrix({
    scenarioIds: ['p0.a', 'p0.b'],
    packageCodes: ['platform-p0-full-flow'],
    priorityCounts: { P0: 2 },
    runnerCounts: { browserPlan: 2 },
    ownerCounts: { quality: 2 }
  });
  const current = createMatrix({
    scenarioIds: ['p0.a'],
    packageCodes: ['platform-p0-full-flow'],
    priorityCounts: { P0: 1 },
    runnerCounts: { browserPlan: 1 },
    ownerCounts: { quality: 1 }
  });

  const diff = buildCoverageDiff({ baseline, current });

  assert.equal(diff.summary.status, 'regressed');
  assert.equal(diff.changes.coverageByPriority.P0.delta, -1);
});

test('buildCoverageDiff marks warning reduction without regression as improved', () => {
  const baseline = createMatrix({
    scenarioIds: ['p0.a'],
    packageCodes: ['platform-p0-full-flow'],
    priorityCounts: { P0: 1 },
    runnerCounts: { browserPlan: 1 },
    ownerCounts: { quality: 1 },
    policySummary: { status: 'warning', errors: 0, warnings: 1 }
  });
  const current = createMatrix({
    scenarioIds: ['p0.a', 'p1.a'],
    packageCodes: ['platform-p0-full-flow'],
    priorityCounts: { P0: 1, P1: 1 },
    runnerCounts: { browserPlan: 2 },
    ownerCounts: { quality: 2 },
    policySummary: { status: 'passed', errors: 0, warnings: 0 }
  });

  const diff = buildCoverageDiff({ baseline, current });

  assert.equal(diff.summary.status, 'improved');
  assert.equal(diff.summary.policyWarningsDelta, -1);
});

test('renderCoverageDiffMarkdown includes status and next actions', () => {
  const diff = buildCoverageDiff({
    baseline: createMatrix({ scenarioIds: ['p0.a'], packageCodes: ['platform-p0-full-flow'], priorityCounts: { P0: 1 } }),
    current: createMatrix({ scenarioIds: ['p0.a'], packageCodes: ['platform-p0-full-flow'], priorityCounts: { P0: 1 } }),
    baselinePath: 'baseline.json',
    currentPath: 'current.json'
  });

  const markdown = renderCoverageDiffMarkdown(diff);

  assert.match(markdown, /# Acceptance Coverage Diff/);
  assert.match(markdown, /Status/);
  assert.match(markdown, /Next Actions/);
});
```

- [ ] **Step 2: Run the library tests and verify they fail**

Run:

```bash
node --test scripts/auto/acceptance-coverage-diff.test.mjs
```

Expected: FAIL with `Cannot find module ... acceptance-coverage-diff-lib.mjs` or missing exported functions.

- [ ] **Step 3: Commit the failing tests**

```bash
git add scripts/auto/acceptance-coverage-diff.test.mjs
git commit -m "test: cover acceptance coverage trend diff"
```

## Task 2: Implement Coverage Diff Library

**Files:**
- Create: `scripts/auto/acceptance-coverage-diff-lib.mjs`
- Test: `scripts/auto/acceptance-coverage-diff.test.mjs`

- [ ] **Step 1: Create the diff library**

Create `scripts/auto/acceptance-coverage-diff-lib.mjs` with:

```js
import fs from 'node:fs/promises';
import path from 'node:path';

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function cleanText(value) {
  return String(value || '').trim();
}

function requireCoverageMatrix(matrix, label) {
  if (!matrix || typeof matrix !== 'object' || Array.isArray(matrix)) {
    throw new Error(`${label} coverage matrix must be a JSON object.`);
  }
  if (!matrix.summary || typeof matrix.summary !== 'object') {
    throw new Error(`${label} coverage matrix is missing summary.`);
  }
  if (!Array.isArray(matrix.scenarios)) {
    throw new Error(`${label} coverage matrix is missing scenarios array.`);
  }
  if (!Array.isArray(matrix.packages)) {
    throw new Error(`${label} coverage matrix is missing packages array.`);
  }
}

function scenarioIdOf(row) {
  return cleanText(row.scenarioId || row.id);
}

function packageCodeOf(row) {
  return cleanText(row.packageCode);
}

function sortedSetDifference(left, right) {
  const rightSet = new Set(right);
  return left.filter((item) => !rightSet.has(item)).sort();
}

function extractScenarioIds(matrix) {
  return asArray(matrix.scenarios).map(scenarioIdOf).filter(Boolean).sort();
}

function extractPackageCodes(matrix) {
  return asArray(matrix.packages).map(packageCodeOf).filter(Boolean).sort();
}

function bucketTotal(index, key) {
  const value = index?.[key]?.total;
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function diffCountIndex(baselineIndex = {}, currentIndex = {}) {
  const keys = Array.from(
    new Set([...Object.keys(baselineIndex || {}), ...Object.keys(currentIndex || {})])
  ).sort();
  return Object.fromEntries(
    keys.map((key) => {
      const baseline = bucketTotal(baselineIndex, key);
      const current = bucketTotal(currentIndex, key);
      return [key, { baseline, current, delta: current - baseline }];
    })
  );
}

function summaryNumber(matrix, key) {
  const value = matrix.summary?.[key];
  return Number.isFinite(Number(value)) ? Number(value) : 0;
}

function policySnapshot(matrix) {
  if (!matrix.policyEvaluation?.summary) {
    return {
      status: 'not-provided',
      errors: null,
      warnings: null
    };
  }
  return {
    status: cleanText(matrix.policyEvaluation.status) || 'unknown',
    errors: Number(matrix.policyEvaluation.summary.errors || 0),
    warnings: Number(matrix.policyEvaluation.summary.warnings || 0)
  };
}

function nullableDelta(left, right) {
  return left === null || right === null ? null : right - left;
}

function classifyStatus(summary, changes) {
  const p0Delta = changes.coverageByPriority.P0?.delta || 0;
  const regressed =
    summary.policyErrorsDelta > 0 ||
    summary.missingScenarioRefsDelta > 0 ||
    summary.metadataMissingScenariosDelta > 0 ||
    p0Delta < 0;
  if (regressed) {
    return 'regressed';
  }
  const improved =
    summary.policyErrorsDelta < 0 ||
    summary.policyWarningsDelta < 0 ||
    summary.missingScenarioRefsDelta < 0 ||
    summary.metadataMissingScenariosDelta < 0 ||
    summary.scenarioDelta > 0 ||
    summary.packageDelta > 0;
  const nonBlockingRegression =
    summary.policyWarningsDelta > 0 ||
    summary.unreferencedScenariosDelta > 0 ||
    changes.scenarios.removed.length > 0 ||
    changes.packages.removed.length > 0;
  if (improved && nonBlockingRegression) {
    return 'mixed';
  }
  if (improved) {
    return 'improved';
  }
  return 'unchanged';
}

export function buildCoverageDiff({
  baseline,
  current,
  baselinePath = '',
  currentPath = '',
  generatedAt = new Date().toISOString()
} = {}) {
  requireCoverageMatrix(baseline, 'Baseline');
  requireCoverageMatrix(current, 'Current');
  const baselineScenarios = extractScenarioIds(baseline);
  const currentScenarios = extractScenarioIds(current);
  const baselinePackages = extractPackageCodes(baseline);
  const currentPackages = extractPackageCodes(current);
  const baselinePolicy = policySnapshot(baseline);
  const currentPolicy = policySnapshot(current);
  const changes = {
    scenarios: {
      added: sortedSetDifference(currentScenarios, baselineScenarios),
      removed: sortedSetDifference(baselineScenarios, currentScenarios)
    },
    packages: {
      added: sortedSetDifference(currentPackages, baselinePackages),
      removed: sortedSetDifference(baselinePackages, currentPackages)
    },
    coverageByPriority: diffCountIndex(
      baseline.coverageByPriority,
      current.coverageByPriority
    ),
    coverageByRunnerType: diffCountIndex(
      baseline.coverageByRunnerType,
      current.coverageByRunnerType
    ),
    coverageByOwnerDomain: diffCountIndex(
      baseline.coverageByOwnerDomain,
      current.coverageByOwnerDomain
    )
  };
  const summary = {
    scenarioDelta: currentScenarios.length - baselineScenarios.length,
    packageDelta: currentPackages.length - baselinePackages.length,
    missingScenarioRefsDelta:
      summaryNumber(current, 'missingScenarioRefs') -
      summaryNumber(baseline, 'missingScenarioRefs'),
    unreferencedScenariosDelta:
      summaryNumber(current, 'unreferencedScenarios') -
      summaryNumber(baseline, 'unreferencedScenarios'),
    metadataMissingScenariosDelta:
      summaryNumber(current, 'metadataMissingScenarios') -
      summaryNumber(baseline, 'metadataMissingScenarios'),
    policyErrorsDelta: nullableDelta(baselinePolicy.errors, currentPolicy.errors),
    policyWarningsDelta: nullableDelta(
      baselinePolicy.warnings,
      currentPolicy.warnings
    )
  };
  summary.status = classifyStatus(summary, changes);
  return {
    generatedAt,
    baselinePath,
    currentPath,
    summary,
    changes,
    policyEvaluation: {
      baseline: baselinePolicy,
      current: currentPolicy
    }
  };
}
```

Continue the same file with Markdown and filesystem helpers:

```js
function escapeTableText(value) {
  return cleanText(value).replace(/\|/g, '\\|');
}

function renderDelta(value) {
  if (value === null) {
    return 'n/a';
  }
  return value > 0 ? `+${value}` : String(value);
}

function renderList(items) {
  return items.length === 0
    ? ['- None']
    : items.map((item) => `- \`${escapeTableText(item)}\``);
}

function renderIndexDiff(title, index, label) {
  const lines = [
    `## ${title}`,
    '',
    `| ${label} | Baseline | Current | Delta |`,
    '|---|---:|---:|---:|'
  ];
  Object.entries(index).forEach(([key, value]) => {
    lines.push(
      `| ${escapeTableText(key)} | ${value.baseline} | ${value.current} | ${renderDelta(value.delta)} |`
    );
  });
  if (Object.keys(index).length === 0) {
    lines.push(`| None | 0 | 0 | 0 |`);
  }
  return lines;
}

function nextActionFor(status) {
  if (status === 'regressed') {
    return 'Coverage regressed. Resolve blocking deltas before release readiness.';
  }
  if (status === 'mixed') {
    return 'Coverage changed in both directions. Confirm non-blocking regressions with the owner.';
  }
  if (status === 'improved') {
    return 'Coverage improved. Keep this diff as release readiness evidence.';
  }
  return 'Coverage is unchanged. Keep this diff as a baseline comparison record.';
}

export function renderCoverageDiffMarkdown(diff) {
  const lines = [
    '# Acceptance Coverage Diff',
    '',
    `- Generated At: \`${diff.generatedAt}\``,
    `- Baseline: \`${diff.baselinePath || 'not provided'}\``,
    `- Current: \`${diff.currentPath || 'not provided'}\``,
    `- Status: \`${diff.summary.status}\``,
    '',
    '## Summary',
    '',
    '| Metric | Delta |',
    '|---|---:|',
    `| Scenario count | ${renderDelta(diff.summary.scenarioDelta)} |`,
    `| Package count | ${renderDelta(diff.summary.packageDelta)} |`,
    `| Missing scenario refs | ${renderDelta(diff.summary.missingScenarioRefsDelta)} |`,
    `| Unreferenced scenarios | ${renderDelta(diff.summary.unreferencedScenariosDelta)} |`,
    `| Metadata missing scenarios | ${renderDelta(diff.summary.metadataMissingScenariosDelta)} |`,
    `| Policy errors | ${renderDelta(diff.summary.policyErrorsDelta)} |`,
    `| Policy warnings | ${renderDelta(diff.summary.policyWarningsDelta)} |`,
    '',
    '## Added Scenarios',
    '',
    ...renderList(diff.changes.scenarios.added),
    '',
    '## Removed Scenarios',
    '',
    ...renderList(diff.changes.scenarios.removed),
    '',
    '## Added Packages',
    '',
    ...renderList(diff.changes.packages.added),
    '',
    '## Removed Packages',
    '',
    ...renderList(diff.changes.packages.removed),
    '',
    ...renderIndexDiff('Coverage By Priority', diff.changes.coverageByPriority, 'Priority'),
    '',
    ...renderIndexDiff('Coverage By Runner Type', diff.changes.coverageByRunnerType, 'Runner Type'),
    '',
    ...renderIndexDiff('Coverage By Owner Domain', diff.changes.coverageByOwnerDomain, 'Owner Domain'),
    '',
    '## Policy Evaluation',
    '',
    '| Side | Status | Errors | Warnings |',
    '|---|---|---:|---:|',
    `| Baseline | ${escapeTableText(diff.policyEvaluation.baseline.status)} | ${diff.policyEvaluation.baseline.errors ?? 'n/a'} | ${diff.policyEvaluation.baseline.warnings ?? 'n/a'} |`,
    `| Current | ${escapeTableText(diff.policyEvaluation.current.status)} | ${diff.policyEvaluation.current.errors ?? 'n/a'} | ${diff.policyEvaluation.current.warnings ?? 'n/a'} |`,
    '',
    '## Next Actions',
    '',
    `- ${nextActionFor(diff.summary.status)}`,
    ''
  ];
  return `${lines.join('\n')}\n`;
}

export async function readCoverageMatrix(filePath) {
  try {
    return JSON.parse(await fs.readFile(filePath, 'utf8'));
  } catch (error) {
    throw new Error(`Unable to read coverage matrix ${filePath}: ${error.message}`);
  }
}

export async function findLatestCoverageMatrixFiles({
  workspaceRoot = process.cwd(),
  logsDir = path.join(workspaceRoot, 'logs', 'acceptance')
} = {}) {
  const entries = await fs.readdir(logsDir);
  const files = entries
    .filter((entry) => /^acceptance-coverage-\d{14}\.json$/.test(entry))
    .sort()
    .map((entry) => path.join(logsDir, entry));
  if (files.length < 2) {
    throw new Error(
      'At least two acceptance coverage matrix JSON files are required. Run generate-acceptance-coverage.mjs twice or pass explicit paths.'
    );
  }
  return {
    baselinePath: files[files.length - 2],
    currentPath: files[files.length - 1]
  };
}

export async function writeCoverageDiffArtifacts({
  diff,
  jsonPath,
  markdownPath
}) {
  await fs.mkdir(path.dirname(jsonPath), { recursive: true });
  await fs.mkdir(path.dirname(markdownPath), { recursive: true });
  await fs.writeFile(jsonPath, JSON.stringify(diff, null, 2), 'utf8');
  await fs.writeFile(markdownPath, renderCoverageDiffMarkdown(diff), 'utf8');
}
```

- [ ] **Step 2: Run the library tests and verify they now pass**

Run:

```bash
node --test scripts/auto/acceptance-coverage-diff.test.mjs
```

Expected: PASS for the first four tests; later CLI tests are not present yet.

- [ ] **Step 3: Commit the diff library**

```bash
git add scripts/auto/acceptance-coverage-diff-lib.mjs scripts/auto/acceptance-coverage-diff.test.mjs
git commit -m "feat: add acceptance coverage trend diff library"
```

## Task 3: Add CLI Tests And CLI Entrypoint

**Files:**
- Modify: `scripts/auto/acceptance-coverage-diff.test.mjs`
- Create: `scripts/auto/diff-acceptance-coverage.mjs`

- [ ] **Step 1: Add failing CLI tests**

Append these tests to `scripts/auto/acceptance-coverage-diff.test.mjs`:

```js
async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(value, null, 2), 'utf8');
}

test('findLatestCoverageMatrixFiles selects the latest two timestamped matrices', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'coverage-diff-latest-'));
  const logsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  await writeJson(path.join(logsDir, 'acceptance-coverage-20260425100000.json'), createMatrix());
  await writeJson(path.join(logsDir, 'acceptance-coverage-20260425110000.json'), createMatrix());
  await writeJson(path.join(logsDir, 'acceptance-coverage-20260425120000.json'), createMatrix());
  await writeJson(path.join(logsDir, 'acceptance-coverage-diff-20260425130000.json'), {});

  const latest = await findLatestCoverageMatrixFiles({ workspaceRoot });

  assert.equal(path.basename(latest.baselinePath), 'acceptance-coverage-20260425110000.json');
  assert.equal(path.basename(latest.currentPath), 'acceptance-coverage-20260425120000.json');
});

test('runCoverageDiffCli writes explicit baseline and current diff artifacts', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'coverage-diff-cli-'));
  const baselinePath = path.join(workspaceRoot, 'baseline.json');
  const currentPath = path.join(workspaceRoot, 'current.json');
  await writeJson(
    baselinePath,
    createMatrix({
      scenarioIds: ['p0.a'],
      packageCodes: ['platform-p0-full-flow'],
      priorityCounts: { P0: 1 },
      policySummary: { status: 'warning', errors: 0, warnings: 1 }
    })
  );
  await writeJson(
    currentPath,
    createMatrix({
      scenarioIds: ['p0.a', 'p1.a'],
      packageCodes: ['platform-p0-full-flow'],
      priorityCounts: { P0: 1, P1: 1 },
      policySummary: { status: 'passed', errors: 0, warnings: 0 }
    })
  );

  const result = await runCoverageDiffCli({
    workspaceRoot,
    argv: [
      `--baseline-path=${baselinePath}`,
      `--current-path=${currentPath}`,
      '--json-out=diff.json',
      '--md-out=diff.md'
    ]
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.diff.summary.status, 'improved');
  assert.equal(
    JSON.parse(await fs.readFile(path.join(workspaceRoot, 'diff.json'), 'utf8')).summary.status,
    'improved'
  );
  assert.match(await fs.readFile(path.join(workspaceRoot, 'diff.md'), 'utf8'), /Acceptance Coverage Diff/);
});

test('runCoverageDiffCli fails when baseline and current resolve to the same file', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'coverage-diff-same-'));
  const matrixPath = path.join(workspaceRoot, 'matrix.json');
  await writeJson(matrixPath, createMatrix({ scenarioIds: ['p0.a'], packageCodes: ['platform-p0-full-flow'] }));

  await assert.rejects(
    () =>
      runCoverageDiffCli({
        workspaceRoot,
        argv: [`--baseline-path=${matrixPath}`, `--current-path=${matrixPath}`]
      }),
    /must be different/
  );
});
```

- [ ] **Step 2: Run the tests and verify CLI tests fail**

Run:

```bash
node --test scripts/auto/acceptance-coverage-diff.test.mjs
```

Expected: FAIL with `Cannot find module ... diff-acceptance-coverage.mjs` or missing `runCoverageDiffCli`.

- [ ] **Step 3: Create the CLI entrypoint**

Create `scripts/auto/diff-acceptance-coverage.mjs`:

```js
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

import {
  buildCoverageDiff,
  findLatestCoverageMatrixFiles,
  readCoverageMatrix,
  writeCoverageDiffArtifacts
} from './acceptance-coverage-diff-lib.mjs';

function parseDiffArgs(argv) {
  const options = {};
  argv.forEach((arg) => {
    if (arg.startsWith('--baseline-path=')) {
      options.baselinePath = arg.slice('--baseline-path='.length).trim();
      return;
    }
    if (arg.startsWith('--current-path=')) {
      options.currentPath = arg.slice('--current-path='.length).trim();
      return;
    }
    if (arg.startsWith('--json-out=')) {
      options.jsonOut = arg.slice('--json-out='.length).trim();
      return;
    }
    if (arg.startsWith('--md-out=')) {
      options.mdOut = arg.slice('--md-out='.length).trim();
      return;
    }
    throw new Error(`Unknown argument: ${arg}`);
  });
  return options;
}

function resolveWorkspacePath(workspaceRoot, value) {
  if (!value) {
    return '';
  }
  return path.isAbsolute(value) ? value : path.resolve(workspaceRoot, value);
}

function createTimestamp() {
  const now = new Date();
  return [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0')
  ].join('');
}

async function resolveInputPaths({ workspaceRoot, options }) {
  const baselinePath = resolveWorkspacePath(workspaceRoot, options.baselinePath);
  const currentPath = resolveWorkspacePath(workspaceRoot, options.currentPath);
  if (baselinePath || currentPath) {
    if (!baselinePath || !currentPath) {
      throw new Error('Both --baseline-path and --current-path are required when either is provided.');
    }
    if (path.resolve(baselinePath) === path.resolve(currentPath)) {
      throw new Error('Coverage diff baseline and current paths must be different.');
    }
    return { baselinePath, currentPath };
  }
  return findLatestCoverageMatrixFiles({ workspaceRoot });
}

export async function runCoverageDiffCli({
  argv = process.argv.slice(2),
  workspaceRoot = process.cwd()
} = {}) {
  const options = parseDiffArgs(argv);
  const { baselinePath, currentPath } = await resolveInputPaths({
    workspaceRoot,
    options
  });
  const baseline = await readCoverageMatrix(baselinePath);
  const current = await readCoverageMatrix(currentPath);
  const diff = buildCoverageDiff({
    baseline,
    current,
    baselinePath,
    currentPath
  });
  const timestamp = createTimestamp();
  const jsonPath =
    resolveWorkspacePath(workspaceRoot, options.jsonOut) ||
    path.join(workspaceRoot, 'logs', 'acceptance', `acceptance-coverage-diff-${timestamp}.json`);
  const markdownPath =
    resolveWorkspacePath(workspaceRoot, options.mdOut) ||
    path.join(workspaceRoot, 'logs', 'acceptance', `acceptance-coverage-diff-${timestamp}.md`);
  await writeCoverageDiffArtifacts({ diff, jsonPath, markdownPath });
  return {
    exitCode: 0,
    jsonPath,
    markdownPath,
    diff
  };
}

const currentFilePath = fileURLToPath(import.meta.url);

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === path.resolve(currentFilePath)
) {
  try {
    const result = await runCoverageDiffCli();
    console.log(
      JSON.stringify(
        {
          exitCode: result.exitCode,
          summary: result.diff.summary,
          jsonPath: result.jsonPath,
          markdownPath: result.markdownPath
        },
        null,
        2
      )
    );
    process.exitCode = result.exitCode;
  } catch (error) {
    console.error(error?.stack || error);
    process.exitCode = 1;
  }
}
```

- [ ] **Step 4: Run the CLI tests and verify they pass**

Run:

```bash
node --test scripts/auto/acceptance-coverage-diff.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit the CLI**

```bash
git add scripts/auto/acceptance-coverage-diff.test.mjs scripts/auto/diff-acceptance-coverage.mjs
git commit -m "feat: add acceptance coverage trend diff cli"
```

## Task 4: Update Documentation

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update README commands**

Add the trend diff command after the coverage matrix commands in `README.md`:

```markdown
node scripts/auto/diff-acceptance-coverage.mjs
node scripts/auto/diff-acceptance-coverage.mjs --baseline-path=logs/acceptance/acceptance-coverage-<old>.json --current-path=logs/acceptance/acceptance-coverage-<new>.json
```

Add bullets:

```markdown
  - 输出 `logs/acceptance/acceptance-coverage-diff-<timestamp>.json` 与 `.md`
  - 默认比较最近两份覆盖矩阵；封板或 CI readiness 复盘建议显式传入 baseline/current 路径
  - diff 只做覆盖治理趋势分析，不替代真实环境业务验收
```

- [ ] **Step 2: Update AGENTS current status**

Append one sentence to the quality factory coverage governance paragraph:

```markdown
同日覆盖治理继续补齐 `scripts/auto/diff-acceptance-coverage.mjs`，可对比最近两份或显式指定的 `acceptance-coverage-*.json`，输出 `acceptance-coverage-diff-*.json/.md`，用于封板和周回归判断覆盖趋势是否改善、持平、混合变化或回退。
```

- [ ] **Step 3: Update docs/05**

After the coverage matrix section, add a subsection:

```markdown
### 10.9 质量工场覆盖趋势对比

覆盖趋势对比用于比较两份 `acceptance-coverage-*.json`，回答新增/移除场景、验收包、优先级、执行器、责任域、缺口和 policy errors/warnings 是否变化。该命令只读本地覆盖矩阵证据，不访问真实环境。

```bash
node scripts/auto/diff-acceptance-coverage.mjs
node scripts/auto/diff-acceptance-coverage.mjs --baseline-path=logs/acceptance/acceptance-coverage-<old>.json --current-path=logs/acceptance/acceptance-coverage-<new>.json
```

输出 `logs/acceptance/acceptance-coverage-diff-<timestamp>.json` 与 `.md`。`summary.status=regressed` 表示出现 P0 覆盖减少、policy error 增加、缺失引用增加或元数据缺口增加；`mixed` 表示既有改善也有非阻断回退；`improved/unchanged` 可作为覆盖治理趋势证据留存。
```

- [ ] **Step 4: Update true environment guide**

After the coverage matrix section, add:

```markdown
封板或周回归建议保留一次覆盖趋势 diff：

```bash
node scripts/auto/diff-acceptance-coverage.mjs --baseline-path=logs/acceptance/acceptance-coverage-<old>.json --current-path=logs/acceptance/acceptance-coverage-<new>.json
```

若只做本地快速检查，可不传路径，脚本会默认选择最近两份覆盖矩阵。`regressed` 必须先说明回退原因；`mixed` 必须由对应 ownerDomain 负责人确认。
```

- [ ] **Step 5: Update docs/21**

Extend the existing coverage governance paragraph:

```markdown
覆盖历史对比进一步新增 `scripts/auto/diff-acceptance-coverage.mjs`，用于对比两份覆盖矩阵的场景、包、优先级、执行器、责任域、gap 与 policy 变化，输出 `acceptance-coverage-diff-<timestamp>.json / .md`。该结果只用于覆盖治理趋势评审，仍不表示真实业务链路已经执行通过。
```

- [ ] **Step 6: Commit docs**

```bash
git add README.md AGENTS.md docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: document acceptance coverage trend diff"
```

## Task 5: Final Verification

**Files:**
- Verify all changed files.

- [ ] **Step 1: Run focused diff tests**

Run:

```bash
node --test scripts/auto/acceptance-coverage-diff.test.mjs
```

Expected: all tests pass.

- [ ] **Step 2: Run existing automation tests**

Run:

```bash
node --test scripts/auto/acceptance-coverage.test.mjs scripts/auto/acceptance-coverage-diff.test.mjs scripts/run-acceptance-registry.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
```

Expected: all tests pass.

- [ ] **Step 3: Generate two coverage matrices and diff them**

Run:

```bash
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/auto/generate-acceptance-coverage.mjs --policy-path=config/automation/acceptance-coverage-policy.json
node scripts/auto/diff-acceptance-coverage.mjs
```

Expected: all commands exit `0`; the diff command prints JSON containing `summary.status`.

- [ ] **Step 4: Run docs and whitespace checks**

Run:

```bash
node scripts/docs/check-topology.mjs
git diff --check
```

Expected: topology check passes and `git diff --check` exits `0`.

- [ ] **Step 5: Review status**

Run:

```bash
git status --short --branch
git log --oneline --max-count=8
```

Expected: branch is `codex/quality-factory-coverage-trends`; only expected committed changes are present or the worktree is clean after final commits.
