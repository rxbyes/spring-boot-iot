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

test('canonical registry includes iot-access specialized scenarios', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));
  const registry = await loadAcceptanceRegistry({ source });

  const browserScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.browser-smoke'
  );
  const apiScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.api-smoke'
  );
  const messageFlowScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.message-flow'
  );

  assert.ok(browserScenario, 'missing scenario: iot-access.browser-smoke');
  assert.ok(apiScenario, 'missing scenario: iot-access.api-smoke');
  assert.ok(messageFlowScenario, 'missing scenario: iot-access.message-flow');

  const iotAccessScenarios = registry.scenarios.filter((item) =>
    item.id.startsWith('iot-access.')
  );
  assert.deepEqual(
    iotAccessScenarios.map((item) => item.id).sort(),
    [
      'iot-access.api-smoke',
      'iot-access.browser-smoke',
      'iot-access.message-flow'
    ]
  );

  assert.equal(browserScenario.module, 'iot-access');
  assert.equal(browserScenario.runnerType, 'browserPlan');
  assert.equal(browserScenario.scope, 'delivery');
  assert.equal(browserScenario.blocking, 'blocker');
  assert.deepEqual(browserScenario.evidence, ['json', 'md', 'screenshot']);
  assert.equal(browserScenario.timeouts.maxMinutes, 12);
  assert.equal(
    browserScenario.runner.planRef,
    'config/automation/iot-access-web-smoke-plan.json'
  );
  assert.deepEqual(browserScenario.runner.scenarioScopes, ['delivery', 'baseline']);
  assert.deepEqual(browserScenario.runner.failScopes, ['delivery']);

  assert.equal(apiScenario.module, 'iot-access');
  assert.equal(apiScenario.runnerType, 'apiSmoke');
  assert.equal(apiScenario.scope, 'delivery');
  assert.equal(apiScenario.blocking, 'warning');
  assert.deepEqual(apiScenario.evidence, ['json', 'md']);
  assert.equal(apiScenario.timeouts.maxMinutes, 10);
  assert.deepEqual(apiScenario.runner.pointFilters, [
    'IOT-PRODUCT',
    'IOT-DEVICE',
    'INGEST-HTTP',
    'MQTT-DOWN',
    'SYS-AUDIT'
  ]);
  assert.equal('moduleFilters' in apiScenario.runner, false);

  assert.equal(messageFlowScenario.module, 'iot-access');
  assert.equal(messageFlowScenario.runnerType, 'messageFlow');
  assert.equal(messageFlowScenario.scope, 'baseline');
  assert.equal(messageFlowScenario.blocking, 'warning');
  assert.deepEqual(messageFlowScenario.dependsOn, ['iot-access.browser-smoke']);
  assert.deepEqual(messageFlowScenario.evidence, ['json']);
  assert.equal(messageFlowScenario.timeouts.maxMinutes, 10);
  assert.equal(
    messageFlowScenario.runner.entryScript,
    'scripts/run-message-flow-acceptance.py'
  );
  assert.equal(messageFlowScenario.runner.requiresExpiredTraceId, true);
});

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
