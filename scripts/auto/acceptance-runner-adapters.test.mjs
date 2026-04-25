import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import { createRunnerAdapters } from './acceptance-runner-adapters.mjs';

test('apiSmoke adapter maps first failing summary row to business failure details', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-adapter-')
  );
  const summaryPath = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    'business-function-summary-test.json'
  );
  await fs.mkdir(path.dirname(summaryPath), { recursive: true });
  await fs.writeFile(
    summaryPath,
    JSON.stringify(
      [
        {
          point: 'TELEMETRY',
          case: 'latest',
          method: 'GET',
          path: '/api/telemetry/latest?deviceId=3001',
          status: 'FAIL',
          detail: 'code=500; msg=tdengine unavailable'
        }
      ],
      null,
      2
    ),
    'utf8'
  );

  const stubPath = path.join(workspaceRoot, 'stub-api-smoke-runner.mjs');
  await fs.writeFile(
    stubPath,
    [
      `console.log('REPORT_SUMMARY=${summaryPath}');`,
      "console.log('SUMMARY=telemetry failed');",
      'process.exit(1);'
    ].join('\n'),
    'utf8'
  );

  const adapters = createRunnerAdapters({ workspaceRoot });
  const result = await adapters.__runCommandForTest({
    executable: process.execPath,
    args: [stubPath],
    context: {
      workspaceRoot,
      scenario: {
        id: 'iot-access.api-smoke',
        runnerType: 'apiSmoke',
        blocking: 'warning'
      },
      registry: {},
      options: {}
    }
  });

  assert.equal(result.status, 'failed');
  assert.equal(result.details.stepLabel, 'TELEMETRY/latest');
  assert.equal(
    result.details.apiRef,
    'GET /api/telemetry/latest?deviceId=3001'
  );
  assert.equal(
    result.details.pageAction,
    '执行业务烟测点 TELEMETRY/latest'
  );
  assert.equal(result.summary, 'code=500; msg=tdengine unavailable');
});
