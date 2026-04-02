import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..');
const browserAcceptanceScript = path.join(repoRoot, 'scripts', 'auto', 'run-browser-acceptance.mjs');

test('config-driven dry-run works without loading default scenario module', () => {
  const result = spawnSync(
    process.execPath,
    [
      browserAcceptanceScript,
      '--dry-run',
      '--no-append-issues',
      '--plan=config/automation/sample-web-smoke-plan.json'
    ],
    {
      cwd: repoRoot,
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /"dryRun": true/);
  assert.match(result.stdout, /"key": "login"/);
});
