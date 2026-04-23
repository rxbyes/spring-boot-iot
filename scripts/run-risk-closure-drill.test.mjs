import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  findBindingForDeviceMetric,
  normalizeEntityId,
  resolveMetricOptionWithWarmup,
  selectExistingDeviceLiveCandidate,
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

test('resolves the shared leader live-device fixture for the real deep displacement scenario', () => {
  const fixture = resolveRiskClosureFixture({
    backendBaseUrl: 'http://127.0.0.1:9999',
    scenarioId: 'risk.live-deep-displacement.real-device-red-chain'
  });

  assert.equal(fixture.mode, 'existing_device_live');
  assert.equal(fixture.metricIdentifier, 'dispsY');
  assert.equal(Array.isArray(fixture.candidates), true);
  assert.ok(fixture.candidates.length >= 2);
  assert.equal(fixture.cleanupAfterRun, true);
  assert.equal(fixture.expectedAlarmDelta, 1);
  assert.equal(fixture.expectedEventDelta, 1);
  assert.equal(fixture.expectedWorkOrderDelta, 1);
});

test('selects the first fresh live-device candidate with formal metrics and non-zero latest value', () => {
  const selected = selectExistingDeviceLiveCandidate(
    [
      {
        deviceCode: 'STALE01',
        reportTime: '2026-04-18T00:00:00',
        metricIdentifier: 'dispsY',
        hasFormalMetric: true,
        latestValue: -0.045
      },
      {
        deviceCode: 'ZERO01',
        reportTime: '2026-04-19T10:00:00',
        metricIdentifier: 'dispsY',
        hasFormalMetric: true,
        latestValue: 0
      },
      {
        deviceCode: 'READY01',
        reportTime: '2026-04-19T10:03:00',
        metricIdentifier: 'dispsY',
        hasFormalMetric: true,
        latestValue: -0.0368
      }
    ],
    {
      nowText: '2026-04-19T10:05:00',
      maxReportAgeMinutes: 30
    }
  );

  assert.equal(selected?.deviceCode, 'READY01');
});

test('skips live-device candidates that already have a recent alarm in the duplicate cooldown window', () => {
  const selected = selectExistingDeviceLiveCandidate(
    [
      {
        deviceCode: 'COOLDOWN01',
        reportTime: '2026-04-19T10:03:00',
        metricIdentifier: 'dispsY',
        hasFormalMetric: true,
        latestValue: -0.0368,
        recentAlarmLevels: ['red']
      },
      {
        deviceCode: 'READY02',
        reportTime: '2026-04-19T10:04:00',
        metricIdentifier: 'dispsY',
        hasFormalMetric: true,
        latestValue: -0.0368,
        recentAlarmLevels: []
      }
    ],
    {
      nowText: '2026-04-19T10:05:00',
      maxReportAgeMinutes: 30,
      requiredAlarmLevel: 'red'
    }
  );

  assert.equal(selected?.deviceCode, 'READY02');
});
