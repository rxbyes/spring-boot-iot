import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  findBindingForDeviceMetric,
  normalizeEntityId,
  resolveMetricOptionWithWarmup,
  summarizeRiskClosureDrill,
  resolveRiskClosureFixture
} from './auto/run-risk-closure-drill.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));

test('summarizes the full blue-yellow-orange-red drill into final counts', () => {
  const summary = summarizeRiskClosureDrill([
    {
      level: 'blue',
      monitorStatus: 'NORMAL',
      riskLevel: 'blue',
      alarmCount: 0,
      eventCount: 0,
      workOrderCount: 0
    },
    {
      level: 'yellow',
      monitorStatus: 'ALARM',
      riskLevel: 'yellow',
      alarmCount: 1,
      eventCount: 0,
      workOrderCount: 0
    },
    {
      level: 'orange',
      monitorStatus: 'ALARM',
      riskLevel: 'orange',
      alarmCount: 2,
      eventCount: 1,
      workOrderCount: 1
    },
    {
      level: 'red',
      monitorStatus: 'ALARM',
      riskLevel: 'red',
      alarmCount: 3,
      eventCount: 2,
      workOrderCount: 2
    }
  ]);

  assert.equal(summary.finalRiskLevel, 'red');
  assert.equal(summary.finalMonitorStatus, 'ALARM');
  assert.equal(summary.alarmCount, 3);
  assert.equal(summary.eventCount, 2);
  assert.equal(summary.workOrderCount, 2);
});

test('resolves the shared 10099 fixture for the first risk drill scenario', () => {
  const fixture = resolveRiskClosureFixture({
    backendBaseUrl: 'http://127.0.0.1:10099',
    scenarioId: 'risk.full-drill.red-chain'
  });

  assert.equal(fixture.deviceCode, 'CDXDD10099A1');
  assert.equal(fixture.bindingId, '8176');
  assert.equal(fixture.metricIdentifier, 'dispsY');
});

test('preserves long entity ids as strings for follow-up API requests', () => {
  assert.equal(
    normalizeEntityId('2039603140650594306'),
    '2039603140650594306'
  );
  assert.equal(
    normalizeEntityId(8176),
    '8176'
  );
  assert.equal(
    normalizeEntityId(null),
    ''
  );
});

test('matches bound device rows by exact id text without numeric precision loss', () => {
  const binding = findBindingForDeviceMetric(
    [
      {
        id: '8176',
        deviceId: '2039603140650594306',
        metricIdentifier: 'dispsY'
      }
    ],
    {
      id: '2039603140650594306'
    },
    {
      identifier: 'dispsY'
    }
  );

  assert.equal(binding.id, '8176');
});

test('warms up metric options once when a fresh device has no runtime metrics yet', async () => {
  let loadCount = 0;
  let warmupCount = 0;

  const metric = await resolveMetricOptionWithWarmup({
    metricIdentifier: 'dispsY',
    maxAttempts: 2,
    waitMs: 0,
    loadMetrics: async () => {
      loadCount += 1;
      if (loadCount === 1) {
        return [];
      }
      return [
        {
          identifier: 'dispsY',
          name: 'dispsY',
          dataType: 'double'
        }
      ];
    },
    warmupMetric: async () => {
      warmupCount += 1;
    }
  });

  assert.equal(loadCount, 2);
  assert.equal(warmupCount, 1);
  assert.equal(metric.identifier, 'dispsY');
});

test('fresh risk drill provisioning uses riskPointLevel instead of legacy riskLevel field', () => {
  const source = fs.readFileSync(path.join(scriptDir, 'auto', 'run-risk-closure-drill.mjs'), 'utf8');

  assert.match(source, /riskPointLevel:\s*'level_1'/);
  assert.doesNotMatch(source, /riskLevel:\s*'warning'/);
});
