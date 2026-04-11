import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';

import {
  buildGovernanceContractGatePlan
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
    /GovernanceApprovalPolicyResolverImplTest,GovernanceApprovalServiceImplTest,ProductModelServiceImplTest,VendorMetricMappingRuntimeServiceImplTest,ProductModelControllerTest,ProductContractReleaseControllerTest,ProductGovernanceApprovalControllerTest,DefaultRiskMetricCatalogPublishRuleTest/
  );
  assert.deepEqual(plan.steps[1].command, [
    'npm.cmd',
    '--prefix',
    path.join(repoRoot, 'spring-boot-iot-ui'),
    'test',
    '--',
    '--run',
    'src/__tests__/components/product/ProductModelDesignerWorkspace.test.ts'
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
