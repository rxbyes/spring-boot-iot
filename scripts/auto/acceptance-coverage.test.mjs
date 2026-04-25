import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  buildCoverageMatrix,
  evaluateCoveragePolicy
} from './acceptance-coverage-lib.mjs';
import { runCoverageCli } from './generate-acceptance-coverage.mjs';

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
          id: 'object.p1.missing-meta',
          module: 'object-insight',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'warning',
          priority: 'P1',
          runner: {}
        },
        {
          id: 'orphan.baseline',
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
              scenarioRefs: ['protocol.p1', 'missing.scenario']
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
              scenarioRefs: ['object.p1.missing-meta']
            }
          ]
        }
      ]
    }
  };
}

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

test('builds package and scenario coverage matrix with actionable gaps', () => {
  const matrix = buildCoverageMatrix(createFixtureSources());

  assert.equal(matrix.summary.totalScenarios, 4);
  assert.equal(matrix.summary.totalPackages, 3);
  assert.equal(matrix.coverageByPriority.P0.total, 1);
  assert.equal(matrix.coverageByPriority.P1.total, 2);
  assert.equal(matrix.gaps.missingScenarioRefs[0].scenarioRef, 'missing.scenario');
  assert.ok(
    matrix.gaps.missingMetadata.some(
      (item) => item.scenarioId === 'object.p1.missing-meta'
    )
  );
  assert.ok(
    matrix.gaps.unreferencedScenarios.some(
      (item) => item.scenarioId === 'orphan.baseline'
    )
  );
});

test('writes coverage artifacts and can fail when gaps exist', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-coverage-')
  );
  const registryPath = path.join(workspaceRoot, 'acceptance-registry.json');
  const packagesPath = path.join(
    workspaceRoot,
    'business-acceptance-packages.json'
  );
  const jsonOut = path.join(workspaceRoot, 'coverage.json');
  const mdOut = path.join(workspaceRoot, 'coverage.md');
  const { registry, packages } = createFixtureSources();

  await fs.writeFile(registryPath, JSON.stringify(registry, null, 2), 'utf8');
  await fs.writeFile(packagesPath, JSON.stringify(packages, null, 2), 'utf8');

  const result = await runCoverageCli({
    workspaceRoot,
    argv: [
      `--registry-path=${registryPath}`,
      `--packages-path=${packagesPath}`,
      `--json-out=${jsonOut}`,
      `--md-out=${mdOut}`,
      '--fail-on-gaps'
    ]
  });

  assert.equal(result.exitCode, 1);
  const json = JSON.parse(await fs.readFile(jsonOut, 'utf8'));
  const markdown = await fs.readFile(mdOut, 'utf8');

  assert.ok(json.summary);
  assert.match(markdown, /# Acceptance Coverage Matrix/);
});

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
