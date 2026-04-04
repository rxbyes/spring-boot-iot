import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  loadAcceptanceRegistry,
  filterRegistryScenarios,
  orderRegistryScenarios
} from './auto/acceptance-registry-lib.mjs';
import { runRegistryCli } from './auto/run-acceptance-registry.mjs';

test('loads the canonical registry and rejects duplicate ids', async () => {
  await assert.rejects(
    () =>
      loadAcceptanceRegistry({
        source: {
          version: '1.0.0',
          scenarios: [
            {
              id: 'dup',
              runnerType: 'browserPlan',
              scope: 'delivery',
              blocking: 'blocker',
              runner: {}
            },
            {
              id: 'dup',
              runnerType: 'apiSmoke',
              scope: 'delivery',
              blocking: 'blocker',
              runner: {}
            }
          ]
        }
      }),
    /Duplicate registry scenario id: dup/
  );
});

test('filters by module and includes dependencies when requested', async () => {
  const registry = await loadAcceptanceRegistry({
    source: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'auth.login',
          module: 'system',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        },
        {
          id: 'risk.full-drill.red-chain',
          module: 'alarm',
          runnerType: 'riskDrill',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: ['auth.login'],
          runner: {}
        }
      ]
    }
  });

  const filtered = filterRegistryScenarios(registry, {
    module: 'alarm',
    includeDeps: true
  });

  assert.deepEqual(
    filtered.map((item) => item.id),
    ['auth.login', 'risk.full-drill.red-chain']
  );
});

test('orders dependency graphs before execution', async () => {
  const ordered = orderRegistryScenarios([
    { id: 'risk.full-drill.red-chain', dependsOn: ['auth.login'] },
    { id: 'auth.login', dependsOn: [] }
  ]);

  assert.deepEqual(
    ordered.map((item) => item.id),
    ['auth.login', 'risk.full-drill.red-chain']
  );
});

test('returns non-zero when a blocker scenario fails', async () => {
  const result = await runRegistryCli({
    argv: ['--scope=delivery'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'risk.full-drill.red-chain',
          module: 'alarm',
          runnerType: 'riskDrill',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        }
      ]
    },
    adapterOverrides: {
      riskDrill: async () => ({
        scenarioId: 'risk.full-drill.red-chain',
        status: 'failed',
        blocking: 'blocker',
        summary: 'simulated failure',
        evidenceFiles: []
      })
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.summary.failed, 1);
});

test('lists registry scenarios without executing runners', async () => {
  const result = await runRegistryCli({
    argv: ['--list'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'auth.browser-smoke',
          module: 'device',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        }
      ]
    }
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.listed.length, 1);
  assert.equal(result.listed[0].id, 'auth.browser-smoke');
});

test('runRegistryCli accepts a derived registry path and persists business metadata', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'registry-cli-'));
  const registryPath = path.join(workspaceRoot, 'business-acceptance-registry.json');

  await fs.writeFile(
    registryPath,
    JSON.stringify(
      {
        version: '1.0.0',
        scenarios: [
          {
            id: 'auth.browser-smoke',
            title: '登录与产品设备浏览器冒烟',
            module: 'device',
            docRef: 'docs/21#接入智维主链路',
            runnerType: 'browserPlan',
            scope: 'delivery',
            blocking: 'blocker',
            dependsOn: [],
            runner: {}
          }
        ]
      },
      null,
      2
    ),
    'utf8'
  );

  const result = await runRegistryCli({
    workspaceRoot,
    argv: [
      `--registry-path=${registryPath}`,
      '--package-code=product-device',
      '--environment-code=dev',
      '--account-template=acceptance-default',
      '--selected-modules=product-create,device-query'
    ],
    adapterOverrides: {
      browserPlan: async () => ({
        scenarioId: 'auth.browser-smoke',
        runnerType: 'browserPlan',
        status: 'passed',
        blocking: 'blocker',
        summary: 'ok',
        evidenceFiles: []
      })
    }
  });

  const report = JSON.parse(await fs.readFile(result.reportPath, 'utf8'));

  assert.equal(result.summary.total, 1);
  assert.equal(report.options.packageCode, 'product-device');
  assert.equal(report.options.environmentCode, 'dev');
  assert.equal(report.options.accountTemplate, 'acceptance-default');
  assert.equal(report.options.selectedModules, 'product-create,device-query');
});
