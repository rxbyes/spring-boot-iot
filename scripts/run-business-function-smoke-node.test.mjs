import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import process from 'node:process';
import { spawn } from 'node:child_process';

async function runNodeSmokeScript(args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, ['scripts/run-business-function-smoke.mjs', ...args], {
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

test('node smoke runner probes telemetry latest and history when TELEMETRY point is selected', async () => {
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

  const result = await runNodeSmokeScript([
    '-BaseUrl',
    `http://127.0.0.1:${port}`,
    '-PointFilter',
    'TELEMETRY'
  ]);

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
  assert.equal(historyRequest.body?.rangeCode, '1d');
  assert.equal(historyRequest.body?.fillPolicy, 'zero');
  assert.ok(
    [historyRequest.body?.identifiers || []].flat().includes('temperature'),
    'expected telemetry history identifiers to include temperature'
  );
});

test('node smoke runner accepts string ids from add-product and add-device responses', async () => {
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
        writeJson(res, { code: 200, data: { id: '2048269776688885762' } });
        return;
      }

      if (url.pathname === '/api/device/add') {
        writeJson(res, { code: 200, data: { id: '2048269781373923329' } });
        return;
      }

      writeJson(res, { code: 200, data: [] });
    });
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  const result = await runNodeSmokeScript([
    '-BaseUrl',
    `http://127.0.0.1:${port}`,
    '-PointFilter',
    'TELEMETRY'
  ]);

  await new Promise((resolve) => server.close(resolve));

  assert.equal(result.status, 0, result.stderr || result.stdout);

  const latestRequest = requests.find(
    (item) =>
      item.method === 'GET' &&
      item.path === '/api/telemetry/latest' &&
      item.query.get('deviceId') === '2048269781373923329'
  );
  assert.ok(latestRequest, 'expected telemetry latest request to use string device id from add-device response');
});
