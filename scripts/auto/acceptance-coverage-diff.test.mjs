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
        {
          total,
          scenarioIds: Array.from(
            { length: total },
            (_, index) => `${key}.${index + 1}`
          )
        }
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

  assert.deepEqual(diff.changes.scenarios.added, [
    'object-insight.p1.browser-smoke'
  ]);
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
    baseline: createMatrix({
      scenarioIds: ['p0.a'],
      packageCodes: ['platform-p0-full-flow'],
      priorityCounts: { P0: 1 }
    }),
    current: createMatrix({
      scenarioIds: ['p0.a'],
      packageCodes: ['platform-p0-full-flow'],
      priorityCounts: { P0: 1 }
    }),
    baselinePath: 'baseline.json',
    currentPath: 'current.json'
  });

  const markdown = renderCoverageDiffMarkdown(diff);

  assert.match(markdown, /# Acceptance Coverage Diff/);
  assert.match(markdown, /Status/);
  assert.match(markdown, /Next Actions/);
});

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(value, null, 2), 'utf8');
}

test('findLatestCoverageMatrixFiles selects the latest two timestamped matrices', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'coverage-diff-latest-')
  );
  const logsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  await writeJson(
    path.join(logsDir, 'acceptance-coverage-20260425100000.json'),
    createMatrix()
  );
  await writeJson(
    path.join(logsDir, 'acceptance-coverage-20260425110000.json'),
    createMatrix()
  );
  await writeJson(
    path.join(logsDir, 'acceptance-coverage-20260425120000.json'),
    createMatrix()
  );
  await writeJson(
    path.join(logsDir, 'acceptance-coverage-diff-20260425130000.json'),
    {}
  );

  const latest = await findLatestCoverageMatrixFiles({ workspaceRoot });

  assert.equal(
    path.basename(latest.baselinePath),
    'acceptance-coverage-20260425110000.json'
  );
  assert.equal(
    path.basename(latest.currentPath),
    'acceptance-coverage-20260425120000.json'
  );
});

test('runCoverageDiffCli writes explicit baseline and current diff artifacts', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'coverage-diff-cli-')
  );
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
    JSON.parse(await fs.readFile(path.join(workspaceRoot, 'diff.json'), 'utf8'))
      .summary.status,
    'improved'
  );
  assert.match(
    await fs.readFile(path.join(workspaceRoot, 'diff.md'), 'utf8'),
    /Acceptance Coverage Diff/
  );
});

test('runCoverageDiffCli fails when baseline and current resolve to the same file', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'coverage-diff-same-')
  );
  const matrixPath = path.join(workspaceRoot, 'matrix.json');
  await writeJson(
    matrixPath,
    createMatrix({
      scenarioIds: ['p0.a'],
      packageCodes: ['platform-p0-full-flow']
    })
  );

  await assert.rejects(
    () =>
      runCoverageDiffCli({
        workspaceRoot,
        argv: [`--baseline-path=${matrixPath}`, `--current-path=${matrixPath}`]
      }),
    /must be different/
  );
});
