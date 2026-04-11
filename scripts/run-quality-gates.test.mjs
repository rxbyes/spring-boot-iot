import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import { resolveQualityGateRunner } from './run-quality-gates.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));

function getPythonUnittestArgsForBasename(basename) {
  const lowered = basename.toLowerCase();
  if (lowered === 'py' || lowered === 'py.exe') {
    return ['-3', '-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v'];
  }
  return ['-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v'];
}

test('prefers pwsh on Windows when available', () => {
  const runner = resolveQualityGateRunner({
    platform: 'win32',
    availableCommands: ['pwsh', 'powershell']
  });

  assert.equal(runner.executable, 'pwsh');
  assert.match(runner.scriptPath, /run-quality-gates\.ps1$/);
  assert.deepEqual(runner.args.slice(0, 3), ['-ExecutionPolicy', 'Bypass', '-File']);
});

test('falls back to powershell on Windows when pwsh is unavailable', () => {
  const runner = resolveQualityGateRunner({
    platform: 'win32',
    availableCommands: ['powershell']
  });

  assert.equal(runner.executable, 'powershell');
  assert.match(runner.scriptPath, /run-quality-gates\.ps1$/);
});

test('uses bash with sh runner on macOS', () => {
  const runner = resolveQualityGateRunner({
    platform: 'darwin',
    availableCommands: ['bash', 'sh']
  });

  assert.equal(runner.executable, 'bash');
  assert.match(runner.scriptPath, /run-quality-gates\.sh$/);
  assert.deepEqual(runner.args, [runner.scriptPath]);
});

test('falls back to sh on Linux when bash is unavailable', () => {
  const runner = resolveQualityGateRunner({
    platform: 'linux',
    availableCommands: ['sh']
  });

  assert.equal(runner.executable, 'sh');
  assert.match(runner.scriptPath, /run-quality-gates\.sh$/);
});

test('python launcher args add -3 only for py launcher basenames', () => {
  assert.deepEqual(
    getPythonUnittestArgsForBasename('py'),
    ['-3', '-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
  assert.deepEqual(
    getPythonUnittestArgsForBasename('py.exe'),
    ['-3', '-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
  assert.deepEqual(
    getPythonUnittestArgsForBasename('python'),
    ['-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
  assert.deepEqual(
    getPythonUnittestArgsForBasename('python.exe'),
    ['-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
  assert.deepEqual(
    getPythonUnittestArgsForBasename('python3'),
    ['-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
  assert.deepEqual(
    getPythonUnittestArgsForBasename('python3.exe'),
    ['-m', 'unittest', 'scripts/test_risk_point_pending_promotion_schema.py', '-v']
  );
});

test('powershell quality gate uses exact py/py.exe detection', () => {
  const psScript = fs.readFileSync(path.join(scriptDir, 'run-quality-gates.ps1'), 'utf8');

  assert.match(psScript, /function Get-PythonUnittestArgs/);
  assert.match(psScript, /\$pythonBasename -eq 'py'/);
  assert.match(psScript, /\$pythonBasename -eq 'py\.exe'/);
  assert.doesNotMatch(psScript, /StartsWith\('py'\)/);
});

test('quality gate scripts invoke governance contract gates before docs topology check', () => {
  const psScript = fs.readFileSync(path.join(scriptDir, 'run-quality-gates.ps1'), 'utf8');
  const shScript = fs.readFileSync(path.join(scriptDir, 'run-quality-gates.sh'), 'utf8');

  assert.match(psScript, /governance contract gates/);
  assert.match(shScript, /governance contract gates/);
  assert.ok(
    psScript.indexOf('governance contract gates') < psScript.indexOf('docs topology check'),
    'powershell runner should execute governance contract gates before docs topology check'
  );
  assert.ok(
    shScript.indexOf('governance contract gates') < shScript.indexOf('docs topology check'),
    'shell runner should execute governance contract gates before docs topology check'
  );
});

test('powershell runner tolerates stderr output from successful native commands', { skip: process.platform !== 'win32' }, () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'quality-gates-powershell-'));
  const fakeBin = path.join(tempRoot, 'bin');
  const psScript = path.join(scriptDir, 'run-quality-gates.ps1');

  fs.mkdirSync(fakeBin, { recursive: true });
  fs.writeFileSync(
    path.join(fakeBin, 'mvn.cmd'),
    '@echo off\r\necho mvn ok\r\nexit /b 0\r\n'
  );
  fs.writeFileSync(
    path.join(fakeBin, 'npm.cmd'),
    '@echo off\r\necho npm %*\r\nexit /b 0\r\n'
  );
  fs.writeFileSync(
    path.join(fakeBin, 'py.cmd'),
    '@echo off\r\n(echo schema ok) 1>&2\r\nexit /b 0\r\n'
  );
  fs.writeFileSync(
    path.join(fakeBin, 'node.cmd'),
    [
      '@echo off',
      'echo node %*',
      'exit /b 0',
      ''
    ].join('\r\n')
  );

  const result = spawnSync('powershell', ['-ExecutionPolicy', 'Bypass', '-File', psScript], {
    cwd: path.resolve(scriptDir, '..'),
    env: {
      ...process.env,
      PATH: `${fakeBin}${path.delimiter}${process.env.PATH ?? ''}`
    },
    encoding: 'utf8'
  });

  try {
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
    assert.match(`${result.stdout}\n${result.stderr}`, /PASS schema baseline guard/);
    assert.match(`${result.stdout}\n${result.stderr}`, /PASS docs topology check/);
  } finally {
    fs.rmSync(tempRoot, { recursive: true, force: true });
  }
});

test('shell runner exits non-zero and stops before docs check when style guard fails', { skip: process.platform === 'win32' }, () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'quality-gates-'));
  const fakeBin = path.join(tempRoot, 'bin');
  const docsMarker = path.join(tempRoot, 'docs-check.marker');
  const shellScript = path.join(scriptDir, 'run-quality-gates.sh');

  fs.mkdirSync(fakeBin, { recursive: true });

  fs.writeFileSync(
    path.join(fakeBin, 'mvn'),
    '#!/usr/bin/env sh\nexit 0\n',
    { mode: 0o755 }
  );
  fs.writeFileSync(
    path.join(fakeBin, 'npm'),
    [
      '#!/usr/bin/env sh',
      'if [ "$1" = "run" ] && [ "$2" = "style:guard" ]; then',
      '  echo "style guard failed" >&2',
      '  exit 23',
      'fi',
      'exit 0',
      ''
    ].join('\n'),
    { mode: 0o755 }
  );
  fs.writeFileSync(
    path.join(fakeBin, 'node'),
    [
      '#!/usr/bin/env sh',
      `echo docs-check >> "${docsMarker}"`,
      'exit 0',
      ''
    ].join('\n'),
    { mode: 0o755 }
  );

  const result = spawnSync('sh', [shellScript], {
    cwd: path.resolve(scriptDir, '..'),
    env: {
      ...process.env,
      PATH: `${fakeBin}${path.delimiter}${process.env.PATH ?? ''}`
    },
    encoding: 'utf8'
  });

  fs.rmSync(tempRoot, { recursive: true, force: true });

  assert.equal(result.status, 23);
  assert.ok(!fs.existsSync(docsMarker), 'docs topology check should not run after a failed gate');
});

test('shell runner exits non-zero and stops before docs check when governance contract gates fail', { skip: process.platform === 'win32' }, () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'quality-gates-governance-'));
  const fakeBin = path.join(tempRoot, 'bin');
  const docsMarker = path.join(tempRoot, 'docs-check.marker');
  const shellScript = path.join(scriptDir, 'run-quality-gates.sh');

  fs.mkdirSync(fakeBin, { recursive: true });
  fs.writeFileSync(
    path.join(fakeBin, 'mvn'),
    '#!/usr/bin/env sh\nexit 0\n',
    { mode: 0o755 }
  );
  fs.writeFileSync(
    path.join(fakeBin, 'npm'),
    '#!/usr/bin/env sh\nexit 0\n',
    { mode: 0o755 }
  );
  fs.writeFileSync(
    path.join(fakeBin, 'python3'),
    '#!/usr/bin/env sh\nexit 0\n',
    { mode: 0o755 }
  );
  fs.writeFileSync(
    path.join(fakeBin, 'node'),
    [
      '#!/usr/bin/env sh',
      'case "$1" in',
      '  *run-governance-contract-gates.mjs) exit 19 ;;',
      `  *check-topology.mjs) echo docs-check >> "${docsMarker}"; exit 0 ;;`,
      '  *) exit 0 ;;',
      'esac',
      ''
    ].join('\n'),
    { mode: 0o755 }
  );

  const result = spawnSync('sh', [shellScript], {
    cwd: path.resolve(scriptDir, '..'),
    env: {
      ...process.env,
      PATH: `${fakeBin}${path.delimiter}${process.env.PATH ?? ''}`
    },
    encoding: 'utf8'
  });

  fs.rmSync(tempRoot, { recursive: true, force: true });

  assert.equal(result.status, 19);
  assert.ok(!fs.existsSync(docsMarker), 'docs topology check should not run after governance gate failure');
});
