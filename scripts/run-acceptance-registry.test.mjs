import test from 'node:test';
import assert from 'node:assert/strict';

import {
  loadAcceptanceRegistry,
  filterRegistryScenarios,
  orderRegistryScenarios
} from './auto/acceptance-registry-lib.mjs';

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
