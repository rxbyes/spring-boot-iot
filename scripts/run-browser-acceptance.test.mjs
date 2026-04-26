import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import fs from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

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
  for (const route of ['/products', '/devices', '/reporting\\?tab=simulate', '/system-log', '/message-trace', '/file-debug']) {
    assert.match(result.stdout, new RegExp(`"route": "${route}"`));
  }
});

test('quality factory dry-run loads business acceptance and results routes', () => {
  const result = spawnSync(
    process.execPath,
    [
      browserAcceptanceScript,
      '--dry-run',
      '--no-append-issues',
      '--plan=config/automation/quality-factory-web-smoke-plan.json'
    ],
    {
      cwd: repoRoot,
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /"dryRun": true/);
  assert.match(result.stdout, /"key": "business-acceptance-workbench"/);
  assert.match(result.stdout, /"key": "automation-results-workbench"/);
});

test('config-driven dry-run filters exact scenario keys', () => {
  const result = spawnSync(
    process.execPath,
    [
      browserAcceptanceScript,
      '--dry-run',
      '--no-append-issues',
      '--plan=config/automation/sample-web-smoke-plan.json',
      '--scenario-keys=login,product-governance-warning-fallback'
    ],
    {
      cwd: repoRoot,
      encoding: 'utf8'
    }
  );

  assert.equal(result.status, 0, result.stderr || result.stdout);
  const payload = JSON.parse(result.stdout);
  assert.deepEqual(
    payload.scenarios.map((scenario) => scenario.key),
    ['login', 'product-governance-warning-fallback']
  );
});

test('p1 onboarding and object insight dry-runs load dedicated plans', () => {
  for (const [planPath, expectedKey] of [
    ['config/automation/device-onboarding-web-smoke-plan.json', 'device-onboarding-workbench'],
    ['config/automation/object-insight-web-smoke-plan.json', 'object-insight-workbench']
  ]) {
    const result = spawnSync(
      process.execPath,
      [
        browserAcceptanceScript,
        '--dry-run',
        '--no-append-issues',
        `--plan=${planPath}`
      ],
      {
        cwd: repoRoot,
        encoding: 'utf8'
      }
    );

    assert.equal(result.status, 0, result.stderr || result.stdout);
    assert.match(result.stdout, /"dryRun": true/);
    assert.match(result.stdout, new RegExp(`"key": "${expectedKey}"`));
  }
});

test('config-driven dry-run prefers acceptance env urls over plan defaults', async (t) => {
  const { runCli } = await import(pathToFileURL(browserAcceptanceScript).href);
  const previousFrontendUrl = process.env.IOT_ACCEPTANCE_FRONTEND_URL;
  const previousBackendUrl = process.env.IOT_ACCEPTANCE_BACKEND_URL;

  t.after(() => {
    if (previousFrontendUrl === undefined) {
      delete process.env.IOT_ACCEPTANCE_FRONTEND_URL;
    } else {
      process.env.IOT_ACCEPTANCE_FRONTEND_URL = previousFrontendUrl;
    }
    if (previousBackendUrl === undefined) {
      delete process.env.IOT_ACCEPTANCE_BACKEND_URL;
    } else {
      process.env.IOT_ACCEPTANCE_BACKEND_URL = previousBackendUrl;
    }
  });

  process.env.IOT_ACCEPTANCE_FRONTEND_URL = 'http://127.0.0.1:5175';
  process.env.IOT_ACCEPTANCE_BACKEND_URL = 'http://127.0.0.1:10099';

  const result = await runCli([
    '--dry-run',
    '--no-append-issues',
    '--plan=config/automation/iot-access-web-smoke-plan.json'
  ]);

  assert.equal(result.options.frontendBaseUrl, 'http://127.0.0.1:5175/');
  assert.equal(result.options.backendBaseUrl, 'http://127.0.0.1:10099/');
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
  assert.equal(reportingScenario?.route, '/reporting?tab=simulate');
  assert.equal(reportingScenario?.expectedPath, '/reporting');
  const reportingPayloadStep = reportingScenario?.steps.find((step) => step.id === 'reporting-fill-payload');
  assert.equal(reportingPayloadStep?.locator?.value, '#payload');
  const reportingQueryDeviceStep = reportingScenario?.steps.find((step) => step.id === 'reporting-query-device');
  assert.equal(reportingQueryDeviceStep?.type, 'triggerApi');
  assert.equal(reportingQueryDeviceStep?.matcher, '/api/device/code/');
  assert.equal(reportingQueryDeviceStep?.action?.locator?.type, 'role');
  assert.equal(reportingQueryDeviceStep?.action?.locator?.name, '查询设备');
  const reportingStep = reportingScenario?.steps.find((step) => step.id === 'reporting-submit-and-capture-trace');
  assert.equal(reportingStep?.type, 'triggerApi');
  assert.equal(reportingStep?.matcher, '/api/message/http/report');
  assert.notEqual(reportingStep?.action?.locator?.value, '#report-submit');
  assert.equal(reportingStep?.action?.locator?.type, 'role');
  assert.equal(reportingStep?.action?.locator?.name, '发送 HTTP 模拟');
  assert.deepEqual(reportingStep?.captures, [
    {
      variable: 'traceId',
      path: 'payload.data.traceId'
    }
  ]);

  const systemLogScenario = scenarios.get('iot-access-system-log');
  assert.equal(systemLogScenario?.scope, 'baseline');
  const systemLogQueryStep = systemLogScenario?.steps.find((step) => step.id === 'system-log-query');
  assert.equal(systemLogQueryStep?.matcher, '/api/system/audit-log/page');

  const messageTraceScenario = scenarios.get('iot-access-message-trace');
  const messageTraceQueryStep = messageTraceScenario?.steps.find((step) => step.id === 'message-trace-query');
  assert.equal(messageTraceQueryStep?.matcher, '/api/device/message-trace/page');

  const fileDebugScenario = scenarios.get('iot-access-file-debug');
  assert.equal(fileDebugScenario?.scope, 'baseline');
  const fileDebugStepIds = (fileDebugScenario?.steps || []).map((step) => step.id);
  assert.deepEqual(fileDebugStepIds, [
    'file-debug-fill-device-code',
    'file-debug-refresh-file-snapshots',
    'file-debug-refresh-firmware-aggregates'
  ]);
  const fileSnapshotsStep = fileDebugScenario?.steps.find((step) => step.id === 'file-debug-refresh-file-snapshots');
  assert.equal(fileSnapshotsStep?.type, 'triggerApi');
  assert.equal(fileSnapshotsStep?.matcher, '/api/device/iot-access-device-${runToken}/file-snapshots');
  assert.equal(fileSnapshotsStep?.action?.locator?.type, 'role');
  assert.equal(fileSnapshotsStep?.action?.locator?.name, '\u5237\u65b0\u6570\u636e');
  const firmwareAggregatesStep = fileDebugScenario?.steps.find(
    (step) => step.id === 'file-debug-refresh-firmware-aggregates'
  );
  assert.equal(firmwareAggregatesStep?.type, 'triggerApi');
  assert.equal(
    firmwareAggregatesStep?.matcher,
    '/api/device/iot-access-device-${runToken}/firmware-aggregates'
  );
  assert.equal(firmwareAggregatesStep?.action?.locator?.type, 'role');
  assert.equal(firmwareAggregatesStep?.action?.locator?.name, '\u5237\u65b0\u6570\u636e');
});

test('reporting and file debug contracts are aligned with current source defaults', () => {
  const reportWorkbenchSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'views', 'ReportWorkbenchView.vue'),
    'utf8'
  );
  const iotApiSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'api', 'iot.ts'),
    'utf8'
  );

  assert.match(reportWorkbenchSource, /default-key="replay"/);
  assert.match(reportWorkbenchSource, /id="report-device-code"/);
  assert.match(reportWorkbenchSource, /id="payload"/);
  assert.match(reportWorkbenchSource, /查询设备/);
  assert.match(reportWorkbenchSource, /发送 HTTP 模拟/);
  assert.match(iotApiSource, /\/api\/device\/\$\{deviceCode\}\/file-snapshots/);
  assert.match(iotApiSource, /\/api\/device\/\$\{deviceCode\}\/firmware-aggregates/);
});

test('iot access tab workspace gives query key precedence when resolving active key', () => {
  const tabWorkspaceSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'components', 'iotAccess', 'IotAccessTabWorkspace.vue'),
    'utf8'
  );

  assert.match(tabWorkspaceSource, /route\.query\?\.\[props\.queryKey\]/);
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
  const workspaceStep = productGovernanceScenario.steps.find((step) => step.id === 'product-governance-wait-workspace');
  assert.equal(workspaceStep?.type, 'waitVisible');
  assert.equal(workspaceStep?.locator?.value, 'contract-field-sample-stage');
  const compareEntryStep = productGovernanceScenario.steps.find((step) => step.id === 'product-governance-wait-compare-action');
  assert.equal(compareEntryStep?.type, 'waitVisible');
  assert.equal(compareEntryStep?.locator?.value, 'contract-field-compare-submit');
  const workspaceTitleStep = productGovernanceScenario.steps.find((step) => step.id === 'product-governance-assert-workspace-title');
  assert.equal(workspaceTitleStep?.type, 'assertText');
  assert.equal(workspaceTitleStep?.value, '基于现有上报手动提炼契约字段');
  assert.ok(
    productGovernanceScenario.steps.every((step) => step.matcher !== '/model-governance/compare'),
    'current product governance smoke should validate workspace reachability instead of old generic compare fallback'
  );

  const unknownCapabilityScenario = scenarios.get('product-governance-unknown-capability');
  assert.ok(unknownCapabilityScenario, 'unknown capability governance scenario should exist');
  const unknownCapabilityQueryStep = unknownCapabilityScenario.steps.find(
    (step) => step.id === 'product-governance-unknown-query'
  );
  assert.equal(
    unknownCapabilityQueryStep?.type,
    'triggerApi',
    'unknown capability governance scenario should wait for the product page query response'
  );
  assert.equal(unknownCapabilityQueryStep?.matcher, '/api/device/product/page');
});

test('protocol governance smoke plan expands detail, publish and guard scenarios', () => {
  const planPath = path.join(repoRoot, 'config', 'automation', 'protocol-governance-web-smoke-plan.json');
  const plan = JSON.parse(fs.readFileSync(planPath, 'utf8'));
  const scenarios = new Map(plan.scenarios.map((scenario) => [scenario.key, scenario]));

  assert.equal(plan.scenarios.length, 5);
  for (const key of [
    'protocol-governance-login',
    'protocol-governance-workbench',
    'protocol-governance-detail-and-publish',
    'protocol-governance-approval-submit',
    'protocol-governance-batch-guards'
  ]) {
    assert.ok(scenarios.has(key), `${key} scenario should exist`);
  }

  const detailScenario = scenarios.get('protocol-governance-detail-and-publish');
  assert.equal(detailScenario?.readySelector, "[data-testid='protocol-family-save']");
  assert.ok(
    detailScenario?.steps.some((step) => step.id === 'protocol-governance-family-detail-assert-display-name')
  );
  assert.ok(detailScenario?.steps.some((step) => step.id === 'protocol-governance-template-publish'));

  const approvalScenario = scenarios.get('protocol-governance-approval-submit');
  assert.equal(approvalScenario?.readySelector, "[data-testid='protocol-family-save']");
  const familyApprovalStep = approvalScenario?.steps.find(
    (step) => step.id === 'protocol-governance-family-submit-publish'
  );
  assert.equal(familyApprovalStep?.type, 'triggerApi');
  assert.equal(familyApprovalStep?.matcher, '/submit-publish');
  assert.deepEqual(familyApprovalStep?.captures, [
    {
      variable: 'familyApprovalOrderId',
      path: 'payload.data.approvalOrderId'
    }
  ]);
  const familyApprovalAssertStep = approvalScenario?.steps.find(
    (step) => step.id === 'protocol-governance-family-submit-publish-assert'
  );
  assert.equal(familyApprovalAssertStep?.type, 'assertText');
  assert.equal(familyApprovalAssertStep?.value, '协议族定义发布审批已提交');

  const profileApprovalStep = approvalScenario?.steps.find(
    (step) => step.id === 'protocol-governance-profile-submit-publish'
  );
  assert.equal(profileApprovalStep?.type, 'triggerApi');
  assert.equal(profileApprovalStep?.matcher, '/submit-publish');
  assert.deepEqual(profileApprovalStep?.captures, [
    {
      variable: 'profileApprovalOrderId',
      path: 'payload.data.approvalOrderId'
    }
  ]);
  const profileApprovalAssertStep = approvalScenario?.steps.find(
    (step) => step.id === 'protocol-governance-profile-submit-publish-assert'
  );
  assert.equal(profileApprovalAssertStep?.type, 'assertText');
  assert.equal(profileApprovalAssertStep?.value, '解密档案发布审批已提交');

  const guardScenario = scenarios.get('protocol-governance-batch-guards');
  const familyGuardStep = guardScenario?.steps.find(
    (step) => step.id === 'protocol-governance-family-batch-rollback-guard'
  );
  assert.equal(familyGuardStep?.type, 'click');

  const profileGuardStep = guardScenario?.steps.find(
    (step) => step.id === 'protocol-governance-profile-batch-rollback-guard'
  );
  assert.equal(profileGuardStep?.type, 'assertDisabled');
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

test('browser core waits for login redirect before marking login helper complete', () => {
  const browserCoreSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-acceptance-core.mjs'),
    'utf8'
  );

  assert.match(
    browserCoreSource,
    /await page\.waitForURL\(\(url\) => !isLoginPath\(url\.toString\(\)\),\s*\{\s*timeout:\s*runtimeOptions\.pageReadyTimeout\s*\}\)/s
  );
  assert.match(browserCoreSource, /Login succeeded but page did not leave \/login\./);
});

test('scenario login recovery waits for pending redirect before attempting another login', () => {
  const browserCoreSource = fs.readFileSync(
    path.join(repoRoot, 'scripts', 'auto', 'browser-acceptance-core.mjs'),
    'utf8'
  );

  assert.match(
    browserCoreSource,
    /const ensureScenarioLogin = async \(page, scenarioKey\) => \{\s*if \(!isLoginPath\(page\.url\(\)\)\) \{\s*return;\s*\}\s*try \{\s*await page\.waitForURL\(\(url\) => !isLoginPath\(url\.toString\(\)\),\s*\{\s*timeout:\s*Math\.min\(runtimeOptions\.pageReadyTimeout, 3000\)\s*\}\);\s*return;/s
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

test('product governance compare payload type exposes manual extract contract', () => {
  const apiSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'types', 'api.ts'),
    'utf8'
  );
  const apiDeclarationSource = fs.readFileSync(
    path.join(repoRoot, 'spring-boot-iot-ui', 'src', 'types', 'api.d.ts'),
    'utf8'
  );

  assert.match(apiSource, /manualExtract\?: ProductModelGovernanceManualExtractPayload;/);
  assert.match(apiDeclarationSource, /manualExtract\?: \{/);
  assert.match(apiDeclarationSource, /sampleType: 'business' \| 'status';/);
  assert.match(apiDeclarationSource, /deviceStructure: 'single' \| 'composite';/);
  assert.doesNotMatch(apiSource, /governanceMode\?: 'normative' \| 'generic' \| null;/);
  assert.doesNotMatch(apiDeclarationSource, /governanceMode\?: 'normative' \| 'generic' \| null;/);
});
