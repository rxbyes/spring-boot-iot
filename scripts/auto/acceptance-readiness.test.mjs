import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  buildAcceptanceReadinessReport,
  renderAcceptanceReadinessMarkdown
} from './acceptance-readiness-lib.mjs';
import { runAcceptanceReadinessCli } from './run-acceptance-readiness.mjs';

function createCoverageResult({
  summary = {},
  policySummary = { status: 'passed', errors: 0, warnings: 0 }
} = {}) {
  return {
    jsonPath: 'logs/acceptance/acceptance-coverage-20260425200000.json',
    markdownPath: 'logs/acceptance/acceptance-coverage-20260425200000.md',
    summary: {
      totalScenarios: 3,
      totalPackages: 2,
      missingScenarioRefs: 0,
      metadataMissingScenarios: 0,
      hasGaps: false,
      ...summary
    },
    policyEvaluation: {
      status: policySummary.status,
      summary: {
        errors: policySummary.errors,
        warnings: policySummary.warnings
      }
    }
  };
}

function createDiffResult({
  status = 'unchanged',
  summary = {}
} = {}) {
  return {
    jsonPath: 'logs/acceptance/acceptance-coverage-diff-20260425200000.json',
    markdownPath: 'logs/acceptance/acceptance-coverage-diff-20260425200000.md',
    summary: {
      status,
      scenarioDelta: 0,
      packageDelta: 0,
      policyErrorsDelta: 0,
      policyWarningsDelta: 0,
      ...summary
    }
  };
}

function createFixtureSources() {
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
    },
    policy: {
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
        }
      }
    }
  };
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true });
  await fs.writeFile(filePath, JSON.stringify(value, null, 2), 'utf8');
}

test('buildAcceptanceReadinessReport returns failed when policy errors exist', () => {
  const report = buildAcceptanceReadinessReport({
    coverage: createCoverageResult({
      policySummary: { status: 'failed', errors: 1, warnings: 0 }
    }),
    diff: createDiffResult({ status: 'unchanged' })
  });

  assert.equal(report.status, 'failed');
  assert.equal(report.exitCode, 1);
});

test('buildAcceptanceReadinessReport returns warning for mixed coverage changes', () => {
  const report = buildAcceptanceReadinessReport({
    coverage: createCoverageResult(),
    diff: createDiffResult({
      status: 'mixed',
      summary: { scenarioDelta: 1, policyWarningsDelta: 1 }
    })
  });

  assert.equal(report.status, 'warning');
  assert.equal(report.exitCode, 0);
});

test('renderAcceptanceReadinessMarkdown includes readiness status and next actions', () => {
  const markdown = renderAcceptanceReadinessMarkdown(
    buildAcceptanceReadinessReport({
      coverage: createCoverageResult(),
      diff: createDiffResult()
    })
  );

  assert.match(markdown, /# Acceptance Readiness/);
  assert.match(markdown, /Next Actions/);
  assert.match(markdown, /passed/i);
});

test('runAcceptanceReadinessCli writes readiness artifacts and skips diff when requested', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-readiness-skip-')
  );
  const { registry, packages, policy } = createFixtureSources();
  const registryPath = path.join(workspaceRoot, 'acceptance-registry.json');
  const packagesPath = path.join(workspaceRoot, 'business-acceptance-packages.json');
  const policyPath = path.join(workspaceRoot, 'acceptance-coverage-policy.json');
  await writeJson(registryPath, registry);
  await writeJson(packagesPath, packages);
  await writeJson(policyPath, policy);

  const result = await runAcceptanceReadinessCli({
    workspaceRoot,
    argv: [
      '--skip-diff',
      `--registry-path=${registryPath}`,
      `--packages-path=${packagesPath}`,
      `--coverage-policy-path=${policyPath}`
    ]
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.report.diff.skipped, true);
  assert.match(await fs.readFile(result.markdownPath, 'utf8'), /Acceptance Readiness/);
});

test('runAcceptanceReadinessCli compares explicit baseline and current coverage matrices', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-readiness-diff-')
  );
  const { registry, packages, policy } = createFixtureSources();
  const registryPath = path.join(workspaceRoot, 'acceptance-registry.json');
  const packagesPath = path.join(workspaceRoot, 'business-acceptance-packages.json');
  const policyPath = path.join(workspaceRoot, 'acceptance-coverage-policy.json');
  const baselinePath = path.join(workspaceRoot, 'baseline.json');
  const currentPath = path.join(workspaceRoot, 'current.json');
  await writeJson(registryPath, registry);
  await writeJson(packagesPath, packages);
  await writeJson(policyPath, policy);
  await writeJson(baselinePath, {
    generatedAt: '2026-04-25T10:00:00.000Z',
    summary: {
      totalScenarios: 2,
      totalPackages: 2,
      missingScenarioRefs: 0,
      unreferencedScenarios: 0,
      metadataMissingScenarios: 0
    },
    coverageByPriority: { P0: { total: 1, scenarioIds: ['auth.browser-smoke'] } },
    coverageByRunnerType: { browserPlan: { total: 1, scenarioIds: ['auth.browser-smoke'] } },
    coverageByOwnerDomain: { quality: { total: 1, scenarioIds: ['auth.browser-smoke'] } },
    scenarios: [{ scenarioId: 'auth.browser-smoke' }, { scenarioId: 'protocol.p1' }],
    packages: [{ packageCode: 'platform-p0-full-flow' }, { packageCode: 'protocol-governance-p1' }],
    gaps: {
      missingScenarioRefs: [],
      unreferencedScenarios: [],
      missingMetadata: []
    },
    policyEvaluation: {
      status: 'warning',
      summary: {
        errors: 0,
        warnings: 1
      }
    }
  });
  await writeJson(currentPath, {
    generatedAt: '2026-04-25T11:00:00.000Z',
    summary: {
      totalScenarios: 3,
      totalPackages: 3,
      missingScenarioRefs: 0,
      unreferencedScenarios: 0,
      metadataMissingScenarios: 0
    },
    coverageByPriority: {
      P0: { total: 1, scenarioIds: ['auth.browser-smoke'] },
      P1: { total: 2, scenarioIds: ['protocol.p1', 'object.p1'] }
    },
    coverageByRunnerType: {
      browserPlan: { total: 2, scenarioIds: ['auth.browser-smoke', 'protocol.p1'] },
      apiSmoke: { total: 1, scenarioIds: ['object.p1'] }
    },
    coverageByOwnerDomain: {
      'protocol-governance': { total: 1, scenarioIds: ['protocol.p1'] },
      'object-insight': { total: 1, scenarioIds: ['object.p1'] }
    },
    scenarios: [
      { scenarioId: 'auth.browser-smoke' },
      { scenarioId: 'protocol.p1' },
      { scenarioId: 'object.p1' }
    ],
    packages: [
      { packageCode: 'platform-p0-full-flow' },
      { packageCode: 'protocol-governance-p1' },
      { packageCode: 'object-insight-p1' }
    ],
    gaps: {
      missingScenarioRefs: [],
      unreferencedScenarios: [],
      missingMetadata: []
    },
    policyEvaluation: {
      status: 'passed',
      summary: {
        errors: 0,
        warnings: 0
      }
    }
  });

  const result = await runAcceptanceReadinessCli({
    workspaceRoot,
    argv: [
      `--registry-path=${registryPath}`,
      `--packages-path=${packagesPath}`,
      `--coverage-policy-path=${policyPath}`,
      `--baseline-coverage-path=${baselinePath}`,
      `--current-coverage-path=${currentPath}`
    ]
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.report.diff.summary.status, 'improved');
  assert.match(result.report.diff.jsonPath, /acceptance-coverage-diff-/);
});
