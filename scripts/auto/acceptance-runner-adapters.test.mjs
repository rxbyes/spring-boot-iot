import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  createRunnerAdapters,
  resolveApiSmokeCommandForTest,
  resolvePythonExecutableForTest,
  buildMessageFlowCommandForTest,
  buildPythonScriptCommandForTest
} from './acceptance-runner-adapters.mjs';

test('apiSmoke adapter maps first failing summary row to business failure details', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-adapter-')
  );
  const reportPath = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    'business-function-test.json'
  );
  const summaryPath = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    'business-function-summary-test.json'
  );
  await fs.mkdir(path.dirname(reportPath), { recursive: true });
  await fs.writeFile(
    reportPath,
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
  await fs.writeFile(
    summaryPath,
    JSON.stringify(
      [
        {
          point: 'TELEMETRY',
          total: 1,
          passed: 0,
          failed: 1,
          status: 'FAIL'
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
      `console.log('REPORT_JSON=${reportPath}');`,
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

test('apiSmoke adapter ignores metadata rows without explicit failure status', async () => {
  const workspaceRoot = await fs.mkdtemp(
    path.join(os.tmpdir(), 'acceptance-adapter-')
  );
  const reportPath = path.join(
    workspaceRoot,
    'logs',
    'acceptance',
    'business-function-test.json'
  );
  await fs.mkdir(path.dirname(reportPath), { recursive: true });
  await fs.writeFile(
    reportPath,
    JSON.stringify(
      {
        point: 'TELEMETRY',
        detail: 'metadata only'
      },
      null,
      2
    ),
    'utf8'
  );

  const stubPath = path.join(workspaceRoot, 'stub-api-smoke-runner.mjs');
  await fs.writeFile(
    stubPath,
    [
      "console.log('REPORT_JSON=logs/acceptance/business-function-test.json');",
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
  assert.equal(result.summary, 'telemetry failed');
  assert.equal(result.details.stepLabel, undefined);
  assert.equal(result.details.apiRef, undefined);
});

test('apiSmoke adapter falls back to the node smoke runner on non-Windows hosts without PowerShell', () => {
  const runner = resolveApiSmokeCommandForTest({
    workspaceRoot: '/repo',
    backendBaseUrl: 'http://127.0.0.1:10099',
    pointFilters: ['TELEMETRY', 'SYS-AUDIT'],
    platform: 'darwin',
    availableCommands: ['bash', 'sh']
  });

  assert.equal(runner.executable, process.execPath);
  assert.deepEqual(runner.args, [
    'scripts/run-business-function-smoke.mjs',
    '-BaseUrl',
    'http://127.0.0.1:10099',
    '-PointFilter',
    'TELEMETRY',
    '-PointFilter',
    'SYS-AUDIT'
  ]);
});

test('messageFlow prefers python3 on non-Windows hosts when python is unavailable', () => {
  assert.equal(
    resolvePythonExecutableForTest({
      platform: 'darwin',
      availableCommands: ['python3']
    }),
    'python3'
  );
});

test('messageFlow adapter forwards backend base url to the python acceptance script', () => {
  const command = buildMessageFlowCommandForTest({
    backendBaseUrl: 'http://127.0.0.1:10099',
    expiredTraceId: 'trace-1',
    platform: 'darwin',
    availableCommands: ['python3']
  });

  assert.equal(command.executable, 'python3');
  assert.deepEqual(command.args, [
    'scripts/run-message-flow-acceptance.py',
    '--base-url=http://127.0.0.1:10099',
    '--expired-trace-id',
    'trace-1'
  ]);
});

test('pythonScript adapter builds a generic python command with configured args', () => {
  const command = buildPythonScriptCommandForTest({
    entryScript: 'scripts/verify-threshold-policy-real-env.py',
    args: ['--fail-on-breaches'],
    platform: 'darwin',
    availableCommands: ['python3']
  });

  assert.equal(command.executable, 'python3');
  assert.deepEqual(command.args, [
    'scripts/verify-threshold-policy-real-env.py',
    '--fail-on-breaches'
  ]);
});
