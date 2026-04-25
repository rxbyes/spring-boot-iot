import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import http from 'node:http';
import process from 'node:process';
import { spawn } from 'node:child_process';

function extractOutputValue(stdout, key) {
  const line = stdout
    .split(/\r?\n/)
    .map((item) => item.trim())
    .find((item) => item.startsWith(`${key}=`));
  return line ? line.slice(`${key}=`.length).trim() : '';
}

async function runPowerShellScript(args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn('powershell', args, {
      cwd: process.cwd(),
      stdio: ['ignore', 'pipe', 'pipe'],
      ...options
    });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });
    child.on('error', reject);
    child.on('close', (code) => {
      resolve({
        status: code ?? 1,
        stdout,
        stderr
      });
    });
  });
}

test('smoke script limits execution to the selected point filter', async () => {
  const server = http.createServer((req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');

    if (req.url === '/api/auth/login') {
      res.end(JSON.stringify({ code: 200, data: { token: 'token', username: 'admin' } }));
      return;
    }

    if (req.url === '/api/auth/me') {
      res.end(JSON.stringify({ code: 200, data: { id: 1, username: 'admin' } }));
      return;
    }

    res.end(JSON.stringify({ code: 200, data: [] }));
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  const result = await runPowerShellScript(
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/run-business-function-smoke.ps1',
      '-BaseUrl',
      `http://127.0.0.1:${port}`,
      '-PointFilter',
      'ENV'
    ]
  );

  await new Promise((resolve) => server.close(resolve));

  assert.equal(result.status, 0, result.stderr || result.stdout);

  const summaryPath = extractOutputValue(result.stdout, 'REPORT_SUMMARY');
  assert.ok(summaryPath, 'expected REPORT_SUMMARY path in stdout');

  const summaryText = fs.readFileSync(summaryPath, 'utf8').replace(/^\uFEFF/, '');
  const summaryPayload = JSON.parse(summaryText);
  const summary = Array.isArray(summaryPayload) ? summaryPayload : [summaryPayload];
  assert.equal(summary.length, 1);
  assert.equal(summary[0].point, 'ENV');
});

test('smoke script uses current governance headers and field contracts', async () => {
  const requests = [];
  let nextId = 1000;

  function writeJson(res, payload) {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.end(JSON.stringify(payload));
  }

  const server = http.createServer((req, res) => {
    let rawBody = '';
    req.on('data', (chunk) => {
      rawBody += chunk.toString();
    });
    req.on('end', () => {
      const url = new URL(req.url || '/', 'http://127.0.0.1');
      const body = rawBody ? JSON.parse(rawBody) : null;
      requests.push({
        method: req.method || 'GET',
        path: url.pathname,
        query: url.searchParams,
        headers: req.headers,
        body
      });

      if (url.pathname === '/api/auth/login') {
        writeJson(res, { code: 200, data: { token: 'token', username: 'admin' } });
        return;
      }

      if (url.pathname === '/api/auth/me') {
        writeJson(res, { code: 200, data: { id: 1, username: 'admin' } });
        return;
      }

      if (url.pathname === '/api/organization/tree') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 11,
              status: 1,
              orgName: 'Auto Org',
              phone: '13800000000',
              children: []
            }
          ]
        });
        return;
      }

      if (url.pathname === '/api/region/list') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 21,
              status: 1,
              regionName: 'Auto Region'
            }
          ]
        });
        return;
      }

      if (url.pathname === '/api/event/work-orders') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 31,
              eventCode: 'stub-event'
            }
          ]
        });
        return;
      }

      if (url.pathname === '/api/role/list') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 41,
              roleCode: 'AUTO_ROLE'
            }
          ]
        });
        return;
      }

      if (url.pathname === '/api/system/audit-log/list') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 51
            }
          ]
        });
        return;
      }

      if (url.pathname === '/api/user/username/governance_reviewer') {
        writeJson(res, {
          code: 200,
          data: {
            id: 99000001,
            username: 'governance_reviewer'
          }
        });
        return;
      }

      if (url.pathname.startsWith('/api/user/username/')) {
        writeJson(res, {
          code: 200,
          data: {
            id: 61,
            username: url.pathname.split('/').at(-1)
          }
        });
        return;
      }

      if (url.pathname === '/api/role/user/61') {
        writeJson(res, {
          code: 200,
          data: [
            {
              id: 41,
              roleCode: 'AUTO_ROLE'
            }
          ]
        });
        return;
      }

      if (
        req.method === 'POST' &&
        /\/(add|report|publish|bind-device)$/.test(url.pathname)
      ) {
        writeJson(res, { code: 200, data: { id: nextId++ } });
        return;
      }

      if (req.method === 'POST' || req.method === 'PUT' || req.method === 'DELETE') {
        writeJson(res, { code: 200, data: {} });
        return;
      }

      writeJson(res, { code: 200, data: [] });
    });
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  const result = await runPowerShellScript(
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/run-business-function-smoke.ps1',
      '-BaseUrl',
      `http://127.0.0.1:${port}`
    ]
  );

  await new Promise((resolve) => server.close(resolve));

  assert.equal(result.status, 0, result.stderr || result.stdout);

  assert.ok(
    requests.some(
      (item) =>
        item.method === 'GET' &&
        item.path === '/api/user/username/governance_reviewer'
    ),
    'expected smoke script to resolve governance reviewer user'
  );

  const governanceWrites = requests.filter((item) =>
    ['/api/rule-definition/add', '/api/rule-definition/update',
      '/api/linkage-rule/add', '/api/linkage-rule/update',
      '/api/emergency-plan/add', '/api/emergency-plan/update'].includes(item.path)
  );
  assert.ok(
    governanceWrites.length >= 6,
    `expected governance write requests to be recorded, got ${requests
      .filter(
        (item) =>
          item.path.includes('/api/rule-definition') ||
          item.path.includes('/api/linkage-rule') ||
          item.path.includes('/api/emergency-plan') ||
          item.path.includes('/api/risk-point') ||
          item.path.includes('/api/user/') ||
          item.path.includes('/api/role/')
      )
      .map((item) => `${item.method} ${item.path}`)
      .join(', ')}`
  );
  governanceWrites.forEach((item) => {
    assert.equal(
      item.headers['x-governance-approver-id'],
      '99000001',
      `expected governance approver header for ${item.path}`
    );
  });

  const riskPointAdd = requests.find((item) => item.path === '/api/risk-point/add');
  const riskPointUpdate = requests.find((item) => item.path === '/api/risk-point/update');
  assert.equal(riskPointAdd?.body?.riskPointLevel, 'level_1');
  assert.equal(riskPointUpdate?.body?.riskPointLevel, 'level_2');
  assert.equal(Object.hasOwn(riskPointAdd?.body || {}, 'riskLevel'), false);
  assert.equal(Object.hasOwn(riskPointUpdate?.body || {}, 'riskLevel'), false);

  const emergencyAdd = requests.find((item) => item.path === '/api/emergency-plan/add');
  const emergencyUpdate = requests.find((item) => item.path === '/api/emergency-plan/update');
  assert.equal(emergencyAdd?.body?.alarmLevel, 'yellow');
  assert.equal(emergencyUpdate?.body?.alarmLevel, 'red');
  assert.equal(Object.hasOwn(emergencyAdd?.body || {}, 'riskLevel'), false);
  assert.equal(Object.hasOwn(emergencyUpdate?.body || {}, 'riskLevel'), false);

  const deleteUserIndex = requests.findIndex(
    (item) => item.method === 'DELETE' && item.path === '/api/user/61'
  );
  const listUserRolesIndex = requests.findIndex(
    (item) => item.method === 'GET' && item.path === '/api/role/user/61'
  );
  assert.ok(deleteUserIndex >= 0, 'expected delete-user request');
  assert.ok(listUserRolesIndex >= 0, 'expected list-user-roles request');
  assert.ok(
    listUserRolesIndex < deleteUserIndex,
    'expected list-user-roles to run before delete-user'
  );
});

test('smoke script probes telemetry latest and history when TELEMETRY point is selected', async () => {
  const requests = [];

  function writeJson(res, payload) {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.end(JSON.stringify(payload));
  }

  const server = http.createServer((req, res) => {
    let rawBody = '';
    req.on('data', (chunk) => {
      rawBody += chunk.toString();
    });
    req.on('end', () => {
      const url = new URL(req.url || '/', 'http://127.0.0.1');
      const body = rawBody ? JSON.parse(rawBody) : null;
      requests.push({
        method: req.method || 'GET',
        path: url.pathname,
        query: url.searchParams,
        body
      });

      if (url.pathname === '/api/auth/login') {
        writeJson(res, { code: 200, data: { token: 'token', username: 'admin' } });
        return;
      }

      if (url.pathname === '/api/auth/me') {
        writeJson(res, { code: 200, data: { id: 1, username: 'admin' } });
        return;
      }

      if (url.pathname === '/api/user/username/governance_reviewer') {
        writeJson(res, {
          code: 200,
          data: {
            id: 99000001,
            username: 'governance_reviewer'
          }
        });
        return;
      }

      if (url.pathname === '/api/device/product/add') {
        writeJson(res, { code: 200, data: { id: 2001 } });
        return;
      }

      if (url.pathname === '/api/device/add') {
        writeJson(res, { code: 200, data: { id: 3001 } });
        return;
      }

      writeJson(res, { code: 200, data: [] });
    });
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  const result = await runPowerShellScript(
    [
      '-NoProfile',
      '-ExecutionPolicy',
      'Bypass',
      '-File',
      'scripts/run-business-function-smoke.ps1',
      '-BaseUrl',
      `http://127.0.0.1:${port}`,
      '-PointFilter',
      'TELEMETRY'
    ]
  );

  await new Promise((resolve) => server.close(resolve));

  assert.equal(result.status, 0, result.stderr || result.stdout);

  const latestRequest = requests.find(
    (item) =>
      item.method === 'GET' &&
      item.path === '/api/telemetry/latest' &&
      item.query.get('deviceId') === '3001'
  );
  assert.ok(
    latestRequest,
    `expected telemetry latest request, got ${requests
      .map((item) => `${item.method} ${item.path}`)
      .join(', ')}`
  );

  const historyRequest = requests.find(
    (item) =>
      item.method === 'POST' &&
      item.path === '/api/telemetry/history/batch'
  );
  assert.ok(historyRequest, 'expected telemetry history batch request');
  assert.equal(historyRequest.body?.deviceId, 3001);
  assert.equal(historyRequest.body?.rangeCode, '1h');
  assert.equal(historyRequest.body?.fillPolicy, 'zero');
  assert.ok(
    [historyRequest.body?.identifiers || []].flat().includes('temperature'),
    'expected telemetry history identifiers to include temperature'
  );
});
