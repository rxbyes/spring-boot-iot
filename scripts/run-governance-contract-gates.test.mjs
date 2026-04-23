import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import process from 'node:process';

import {
  buildGovernanceContractGatePlan,
  runGovernanceContractGates
} from './run-governance-contract-gates.mjs';

test('builds backend and frontend governance contract steps with Maven settings', () => {
  const repoRoot = path.resolve(process.cwd());
  const plan = buildGovernanceContractGatePlan({
    repoRoot,
    platform: 'win32',
    hasMavenSettings: true
  });

  assert.equal(plan.logFile, path.join(repoRoot, 'logs', 'governance-contract-gates.log'));
  assert.equal(plan.steps.length, 2);
  assert.equal(plan.steps[0].step, 'backend governance contract tests');
  assert.equal(plan.steps[0].command[0], 'mvn.cmd');
  assert.deepEqual(plan.steps[0].command.slice(1, 4), [
    '-s',
    path.join(repoRoot, '.mvn', 'settings.xml'),
    '-pl'
  ]);
  assert.match(
    plan.steps[0].command.join(' '),
    /GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductContractGovernanceApprovalPayloadsTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest/
  );
  assert.deepEqual(plan.steps[1].command, [
    'npm.cmd',
    '--prefix',
    path.join(repoRoot, 'spring-boot-iot-ui'),
    'test',
    '--',
    '--run',
    'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts',
    'src/__tests__/components/product/ProductModelGovernanceCompareTable.test.ts'
  ]);
});

test('falls back to plain Maven args when settings.xml is absent', () => {
  const repoRoot = path.resolve(process.cwd());
  const plan = buildGovernanceContractGatePlan({
    repoRoot,
    platform: 'linux',
    hasMavenSettings: false
  });

  assert.equal(plan.steps[0].command[0], 'mvn');
  assert.equal(plan.steps[0].command.includes('-s'), false);
});

test('runGovernanceContractGates executes cmd-based steps on Windows', { skip: process.platform !== 'win32' }, async (t) => {
  const tempRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'governance-gates-'));
  const repoRoot = path.join(tempRoot, 'repo');
  const fakeBin = path.join(tempRoot, 'bin');
  const uiRoot = path.join(repoRoot, 'spring-boot-iot-ui');
  const settingsDir = path.join(repoRoot, '.mvn');
  const previousPath = process.env.PATH ?? '';

  fs.mkdirSync(fakeBin, { recursive: true });
  fs.mkdirSync(uiRoot, { recursive: true });
  fs.mkdirSync(settingsDir, { recursive: true });
  fs.writeFileSync(path.join(settingsDir, 'settings.xml'), '<settings />', 'utf8');
  fs.writeFileSync(
    path.join(fakeBin, 'mvn.cmd'),
    '@echo off\r\necho backend-ok\r\nexit /b 0\r\n',
    'utf8'
  );
  fs.writeFileSync(
    path.join(fakeBin, 'npm.cmd'),
    '@echo off\r\necho frontend-ok\r\nexit /b 0\r\n',
    'utf8'
  );
  process.env.PATH = `${fakeBin}${path.delimiter}${previousPath}`;

  t.after(() => {
    process.env.PATH = previousPath;
    fs.rmSync(tempRoot, { recursive: true, force: true });
  });

  await runGovernanceContractGates({
    repoRoot,
    platform: 'win32',
    hasMavenSettings: true
  });

  const log = fs.readFileSync(path.join(repoRoot, 'logs', 'governance-contract-gates.log'), 'utf8');
  assert.match(log, /START backend governance contract tests/);
  assert.match(log, /backend-ok/);
  assert.match(log, /START frontend product governance tests/);
  assert.match(log, /frontend-ok/);
  assert.match(log, /All governance contract quality gates passed/);
});
