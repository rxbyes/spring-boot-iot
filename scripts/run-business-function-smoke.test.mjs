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
