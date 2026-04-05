import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import fs from 'node:fs';
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

test('iot access dry-run loads dedicated smoke plan and prints required routes', () => {
  const result = spawnSync(
    process.execPath,
    [
      browserAcceptanceScript,
      '--dry-run',
      '--no-append-issues',
      '--plan=config/automation/iot-access-web-smoke-plan.json'
    ],
    {
      cwd: repoRoot,
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /"dryRun": true/);
  for (const route of ['/products', '/devices', '/reporting', '/system-log', '/message-trace', '/file-debug']) {
    assert.match(result.stdout, new RegExp(`"route": "${route}"`));
  }
});

test('iot access smoke plan defines expected scenarios, ready selectors, and reporting trace capture', () => {
  const planPath = path.join(repoRoot, 'config', 'automation', 'iot-access-web-smoke-plan.json');
  const plan = JSON.parse(fs.readFileSync(planPath, 'utf8'));
  const scenarios = new Map(plan.scenarios.map((scenario) => [scenario.key, scenario]));

  assert.equal(plan.scenarios.length, 7);
  for (const key of [
    'iot-access-login',
    'iot-access-products',
    'iot-access-devices',
    'iot-access-reporting',
    'iot-access-system-log',
    'iot-access-message-trace',
    'iot-access-file-debug'
  ]) {
    assert.ok(scenarios.has(key), `${key} scenario should exist`);
  }

  assert.equal(scenarios.get('iot-access-products')?.readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-devices')?.readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-reporting')?.readySelector, '#report-device-code');
  assert.equal(scenarios.get('iot-access-system-log')?.readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-message-trace')?.readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-file-debug')?.readySelector, '#file-debug-device-code');

  const reportingScenario = scenarios.get('iot-access-reporting');
  const reportingStep = reportingScenario?.steps.find((step) => step.id === 'reporting-submit-and-capture-trace');
  assert.equal(reportingStep?.type, 'triggerApi');
  assert.equal(reportingStep?.matcher, '/api/message/http/report');
  assert.deepEqual(reportingStep?.captures, [
    {
      variable: 'traceId',
      path: 'payload.data.traceId'
    }
  ]);
});

test('sample web smoke plan matches current login and product/device workbench flow', () => {
  const samplePlanPath = path.join(repoRoot, 'config', 'automation', 'sample-web-smoke-plan.json');
  const samplePlan = JSON.parse(fs.readFileSync(samplePlanPath, 'utf8'));
  const scenarios = new Map(samplePlan.scenarios.map((scenario) => [scenario.key, scenario]));

  const loginScenario = scenarios.get('login');
  assert.ok(loginScenario, 'login scenario should exist');
  const loginAssertStep = loginScenario.steps.at(-1);
  assert.equal(loginAssertStep.locator.value, 'body');
  assert.match(loginAssertStep.value, /平台治理|页面入口/);

  const productScenario = scenarios.get('product-workbench');
  assert.ok(productScenario, 'product-workbench scenario should exist');
  assert.equal(productScenario.readySelector, '#quick-search');
  assert.ok(
    productScenario.steps.some((step) => step.id === 'product-open-create-drawer'),
    'product scenario should open create drawer before filling form'
  );
  assert.ok(
    productScenario.steps.some((step) => step.locator?.value === '#quick-search'),
    'product scenario should reuse current quick-search input for list verification'
  );
  assert.ok(
    productScenario.steps.every((step) => step.locator?.value !== '#query-product-key'),
    'product scenario should not rely on removed query-product-key selector'
  );

  const deviceScenario = scenarios.get('device-workbench');
  assert.ok(deviceScenario, 'device-workbench scenario should exist');
  assert.equal(deviceScenario.readySelector, '#quick-search');
  assert.ok(
    deviceScenario.steps.some((step) => step.id === 'device-open-create-drawer'),
    'device scenario should open create drawer before filling form'
  );
  assert.ok(
    deviceScenario.steps.some((step) => step.locator?.value === '#quick-search'),
    'device scenario should reuse current quick-search input for list verification'
  );
  assert.ok(
    deviceScenario.steps.every((step) => !['#query-device-id', '#query-device-code', '#device-product-key'].includes(step.locator?.value)),
    'device scenario should not rely on removed query selector set'
  );
  const deviceVisualStep = deviceScenario.steps.find((step) => step.id === 'device-assert-visual-page');
  assert.equal(deviceVisualStep?.optional, true, 'device visual baseline sample should be optional by default');

  const productGovernanceScenario = scenarios.get('product-governance-warning-fallback');
  assert.ok(productGovernanceScenario, 'product governance warning fallback scenario should exist');
  assert.equal(productGovernanceScenario.readySelector, '#quick-search');
  assert.ok(
    productGovernanceScenario.steps.some((step) => step.id === 'product-governance-open-workbench' && step.type === 'tableRowAction'),
    'product governance scenario should open the workbench from the current product list row'
  );
  const compareStep = productGovernanceScenario.steps.find((step) => step.id === 'product-governance-compare-fallback');
  assert.equal(compareStep?.type, 'triggerApi');
  assert.equal(compareStep?.matcher, '/model-governance/compare');
  assert.deepEqual(compareStep?.captures, [
    {
      variable: 'governanceMode',
      path: 'requestPayload.governanceMode'
    }
  ]);
  const assertModeStep = productGovernanceScenario.steps.find((step) => step.id === 'product-governance-assert-generic-mode');
  assert.equal(assertModeStep?.type, 'assertVariableEquals');
  assert.equal(assertModeStep?.variable, 'governanceMode');
  assert.equal(assertModeStep?.value, 'generic');
});

test('automation plan source no longer uses removed console page title selector defaults', () => {
  const automationPlanSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'utils', 'automationPlan.ts'),
    'utf8'
  );

  assert.doesNotMatch(automationPlanSource, /\[data-testid="console-page-title"\]/);
  assert.doesNotMatch(automationPlanSource, /#query-product-key/);
  assert.doesNotMatch(automationPlanSource, /#device-product-key/);
  assert.match(automationPlanSource, /readySelector:\s*'#quick-search'/);
});

test('device route preload in browser core uses current product ready selector', () => {
  const browserCoreSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-acceptance-core.mjs'),
    'utf8'
  );

  assert.match(
    browserCoreSource,
    /waitForPageReady\(page,\s*\{\s*expectedPath:\s*'\/products',\s*readySelector:\s*'#quick-search'/s
  );
});

test('selectOption handler supports Element Plus select triggers', () => {
  const configDrivenSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-config-driven.mjs'),
    'utf8'
  );

  assert.match(configDrivenSource, /locator\.locator\('\.el-select__wrapper, \.el-select__selection, \.el-input__wrapper'/);
});

test('config driven browser steps support request payload captures and variable assertions', () => {
  const browserCoreSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-acceptance-core.mjs'),
    'utf8'
  );
  const configDrivenSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-config-driven.mjs'),
    'utf8'
  );

  assert.match(browserCoreSource, /requestPayload\b/);
  assert.match(configDrivenSource, /registerPlanStepHandler\('assertVariableEquals'/);
});

test('product governance compare payload type exposes generic mode contract', () => {
  const apiSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'types', 'api.ts'),
    'utf8'
  );
  const apiDeclarationSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'types', 'api.d.ts'),
    'utf8'
  );

  assert.match(apiSource, /governanceMode\?: 'normative' \| 'generic' \| null;/);
  assert.match(apiDeclarationSource, /governanceMode\?: 'normative' \| 'generic' \| null;/);
});
