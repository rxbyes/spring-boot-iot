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

