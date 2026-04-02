import test from 'node:test';
import assert from 'node:assert/strict';

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
