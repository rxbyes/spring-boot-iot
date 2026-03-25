import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import { resolveQualityGateRunner } from './run-quality-gates.mjs';

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

test('shell runner exits non-zero and stops before docs check when style guard fails', { skip: process.platform === 'win32' }, () => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'quality-gates-'));
  const fakeBin = path.join(tempRoot, 'bin');
  const docsMarker = path.join(tempRoot, 'docs-check.marker');
  const scriptDir = path.dirname(fileURLToPath(import.meta.url));
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
