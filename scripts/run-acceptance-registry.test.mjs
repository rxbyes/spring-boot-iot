import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  loadAcceptanceRegistry,
  filterRegistryScenarios,
  orderRegistryScenarios
} from './auto/acceptance-registry-lib.mjs';
import { runRegistryCli } from './auto/run-acceptance-registry.mjs';

test('canonical registry includes iot-access specialized scenarios', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));
  const registry = await loadAcceptanceRegistry({ source });

  const browserScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.browser-smoke'
  );
  const apiScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.api-smoke'
  );
  const messageFlowScenario = registry.scenarios.find(
    (item) => item.id === 'iot-access.message-flow'
  );

  assert.ok(browserScenario, 'missing scenario: iot-access.browser-smoke');
  assert.ok(apiScenario, 'missing scenario: iot-access.api-smoke');
  assert.ok(messageFlowScenario, 'missing scenario: iot-access.message-flow');

  const iotAccessScenarios = registry.scenarios.filter((item) =>
    item.id.startsWith('iot-access.')
  );
  assert.deepEqual(
    iotAccessScenarios.map((item) => item.id).sort(),
    [
      'iot-access.api-smoke',
      'iot-access.browser-smoke',
      'iot-access.message-flow'
    ]
  );

  assert.equal(browserScenario.module, 'iot-access');
  assert.equal(browserScenario.runnerType, 'browserPlan');
  assert.equal(browserScenario.scope, 'delivery');
  assert.equal(browserScenario.blocking, 'blocker');
  assert.deepEqual(browserScenario.evidence, ['json', 'md', 'screenshot']);
  assert.equal(browserScenario.timeouts.maxMinutes, 12);
  assert.equal(
    browserScenario.runner.planRef,
    'config/automation/iot-access-web-smoke-plan.json'
  );
  assert.deepEqual(browserScenario.runner.scenarioScopes, ['delivery', 'baseline']);
  assert.deepEqual(browserScenario.runner.failScopes, ['delivery']);

  assert.equal(apiScenario.module, 'iot-access');
  assert.equal(apiScenario.runnerType, 'apiSmoke');
  assert.equal(apiScenario.scope, 'delivery');
  assert.equal(apiScenario.blocking, 'warning');
  assert.deepEqual(apiScenario.evidence, ['json', 'md']);
  assert.equal(apiScenario.timeouts.maxMinutes, 10);
  assert.deepEqual(apiScenario.runner.pointFilters, [
    'IOT-PRODUCT',
    'IOT-DEVICE',
    'INGEST-HTTP',
    'MQTT-DOWN',
    'SYS-AUDIT'
  ]);
  assert.equal('moduleFilters' in apiScenario.runner, false);

  assert.equal(messageFlowScenario.module, 'iot-access');
  assert.equal(messageFlowScenario.runnerType, 'messageFlow');
  assert.equal(messageFlowScenario.scope, 'baseline');
  assert.equal(messageFlowScenario.blocking, 'warning');
  assert.deepEqual(messageFlowScenario.dependsOn, ['iot-access.browser-smoke']);
  assert.deepEqual(messageFlowScenario.evidence, ['json']);
  assert.equal(messageFlowScenario.timeouts.maxMinutes, 10);
  assert.equal(
    messageFlowScenario.runner.entryScript,
    'scripts/run-message-flow-acceptance.py'
  );
  assert.equal(messageFlowScenario.runner.requiresExpiredTraceId, true);
});

test('canonical registry includes threshold policy real-env readiness scenario', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));
  const registry = await loadAcceptanceRegistry({ source });

  const scenario = registry.scenarios.find(
    (item) => item.id === 'threshold-policy.real-env-readiness'
  );

  assert.ok(scenario, 'missing scenario: threshold-policy.real-env-readiness');
  assert.equal(scenario.module, 'alarm');
  assert.equal(scenario.runnerType, 'pythonScript');
  assert.equal(scenario.scope, 'baseline');
  assert.equal(scenario.blocking, 'warning');
  assert.equal(scenario.priority, 'P1');
  assert.equal(scenario.runner.entryScript, 'scripts/verify-threshold-policy-real-env.py');
  assert.deepEqual(scenario.runner.args, ['--fail-on-breaches']);
});

test('loads the canonical registry and rejects duplicate ids', async () => {
  await assert.rejects(
    () =>
      loadAcceptanceRegistry({
        source: {
          version: '1.0.0',
          scenarios: [
            {
              id: 'dup',
              runnerType: 'browserPlan',
              scope: 'delivery',
              blocking: 'blocker',
              runner: {}
            },
            {
              id: 'dup',
              runnerType: 'apiSmoke',
              scope: 'delivery',
              blocking: 'blocker',
              runner: {}
            }
          ]
        }
      }),
    /Duplicate registry scenario id: dup/
  );
});

test('filters by module and includes dependencies when requested', async () => {
  const registry = await loadAcceptanceRegistry({
    source: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'auth.login',
          module: 'system',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        },
        {
          id: 'risk.full-drill.red-chain',
          module: 'alarm',
          runnerType: 'riskDrill',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: ['auth.login'],
          runner: {}
        }
      ]
    }
  });

  const filtered = filterRegistryScenarios(registry, {
    module: 'alarm',
    includeDeps: true
  });

  assert.deepEqual(
    filtered.map((item) => item.id),
    ['auth.login', 'risk.full-drill.red-chain']
  );
});

test('orders dependency graphs before execution', async () => {
  const ordered = orderRegistryScenarios([
    { id: 'risk.full-drill.red-chain', dependsOn: ['auth.login'] },
    { id: 'auth.login', dependsOn: [] }
  ]);

  assert.deepEqual(
    ordered.map((item) => item.id),
    ['auth.login', 'risk.full-drill.red-chain']
  );
});

test('returns non-zero when a blocker scenario fails', async () => {
  const result = await runRegistryCli({
    argv: ['--scope=delivery'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'risk.full-drill.red-chain',
          module: 'alarm',
          runnerType: 'riskDrill',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        }
      ]
    },
    adapterOverrides: {
      riskDrill: async () => ({
        scenarioId: 'risk.full-drill.red-chain',
        status: 'failed',
        blocking: 'blocker',
        summary: 'simulated failure',
        evidenceFiles: []
      })
    }
  });

  assert.equal(result.exitCode, 1);
  assert.equal(result.summary.failed, 1);
});

test('lists registry scenarios without executing runners', async () => {
  const result = await runRegistryCli({
    argv: ['--list'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'auth.browser-smoke',
          module: 'device',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        }
      ]
    }
  });

  assert.equal(result.exitCode, 0);
  assert.equal(result.listed.length, 1);
  assert.equal(result.listed[0].id, 'auth.browser-smoke');
});

test('list mode filters delivery scenarios by scope', async () => {
  const result = await runRegistryCli({
    argv: ['--list', '--scope=delivery'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'iot-access.browser-smoke',
          module: 'iot-access',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        },
        {
          id: 'iot-access.message-flow',
          module: 'iot-access',
          runnerType: 'messageFlow',
          scope: 'baseline',
          blocking: 'warning',
          dependsOn: [],
          runner: {}
        }
      ]
    }
  });

  assert.equal(result.exitCode, 0);
  assert.deepEqual(
    result.listed.map((item) => item.id),
    ['iot-access.browser-smoke']
  );
});

test('list mode filters baseline scenarios by scope', async () => {
  const result = await runRegistryCli({
    argv: ['--list', '--scope=baseline'],
    registrySource: {
      version: '1.0.0',
      scenarios: [
        {
          id: 'iot-access.browser-smoke',
          module: 'iot-access',
          runnerType: 'browserPlan',
          scope: 'delivery',
          blocking: 'blocker',
          dependsOn: [],
          runner: {}
        },
        {
          id: 'iot-access.message-flow',
          module: 'iot-access',
          runnerType: 'messageFlow',
          scope: 'baseline',
          blocking: 'warning',
          dependsOn: [],
          runner: {}
        }
      ]
    }
  });

  assert.equal(result.exitCode, 0);
  assert.deepEqual(
    result.listed.map((item) => item.id),
    ['iot-access.message-flow']
  );
});

test('canonical registry list mode respects delivery and baseline scopes', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));

  const deliveryResult = await runRegistryCli({
    argv: ['--list', '--scope=delivery'],
    registrySource: source
  });
  const baselineResult = await runRegistryCli({
    argv: ['--list', '--scope=baseline'],
    registrySource: source
  });

  const deliveryIds = new Set(deliveryResult.listed.map((item) => item.id));
  const baselineIds = new Set(baselineResult.listed.map((item) => item.id));

  assert.equal(deliveryResult.exitCode, 0);
  assert.equal(baselineResult.exitCode, 0);
  assert.equal(deliveryIds.has('iot-access.browser-smoke'), true);
  assert.equal(deliveryIds.has('iot-access.api-smoke'), true);
  assert.equal(baselineIds.has('iot-access.message-flow'), true);
  assert.equal(baselineIds.has('iot-access.browser-smoke'), false);
  assert.equal(baselineIds.has('iot-access.api-smoke'), false);
});

test('runRegistryCli accepts a derived registry path and persists business metadata', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'registry-cli-'));
  const registryPath = path.join(workspaceRoot, 'business-acceptance-registry.json');

  await fs.writeFile(
    registryPath,
    JSON.stringify(
      {
        version: '1.0.0',
        scenarios: [
          {
            id: 'auth.browser-smoke',
            title: '登录与产品设备浏览器冒烟',
            module: 'device',
            docRef: 'docs/21#接入智维主链路',
            runnerType: 'browserPlan',
            scope: 'delivery',
            blocking: 'blocker',
            dependsOn: [],
            runner: {}
          }
        ]
      },
      null,
      2
    ),
    'utf8'
  );

  const result = await runRegistryCli({
    workspaceRoot,
    argv: [
      `--registry-path=${registryPath}`,
      '--package-code=product-device',
      '--environment-code=dev',
      '--account-template=acceptance-default',
      '--selected-modules=product-create,device-query'
    ],
    adapterOverrides: {
      browserPlan: async () => ({
        scenarioId: 'auth.browser-smoke',
        runnerType: 'browserPlan',
        status: 'passed',
        blocking: 'blocker',
        summary: 'ok',
        evidenceFiles: []
      })
    }
  });

  const report = JSON.parse(await fs.readFile(result.reportPath, 'utf8'));

  assert.equal(result.summary.total, 1);
  assert.equal(report.options.packageCode, 'product-device');
  assert.equal(report.options.environmentCode, 'dev');
  assert.equal(report.options.accountTemplate, 'acceptance-default');
  assert.equal(report.options.selectedModules, 'product-create,device-query');
});

test('runRegistryCli filters package scenarios by selected business modules and includes dependencies', async () => {
  const registrySource = {
    version: '1.0.0',
    scenarios: [
      {
        id: 'auth.browser-smoke',
        module: 'device',
        runnerType: 'browserPlan',
        scope: 'delivery',
        blocking: 'blocker',
        dependsOn: [],
        runner: {}
      },
      {
        id: 'telemetry.api-smoke',
        module: 'telemetry',
        runnerType: 'apiSmoke',
        scope: 'delivery',
        blocking: 'warning',
        dependsOn: ['auth.browser-smoke'],
        runner: {}
      },
      {
        id: 'risk.full-drill.red-chain',
        module: 'alarm',
        runnerType: 'riskDrill',
        scope: 'delivery',
        blocking: 'blocker',
        dependsOn: ['auth.browser-smoke'],
        runner: {}
      }
    ]
  };

  const packagesSource = {
    version: '1.0.0',
    packages: [
      {
        packageCode: 'platform-p0-full-flow',
        packageName: 'P0',
        defaultAccountTemplate: 'manager-default',
        supportedEnvironments: ['dev'],
        targetRoles: ['manager'],
        modules: [
          {
            moduleCode: 'login-auth',
            moduleName: '登录',
            scenarioRefs: ['auth.browser-smoke']
          },
          {
            moduleCode: 'telemetry-read',
            moduleName: '遥测',
            scenarioRefs: ['telemetry.api-smoke']
          },
          {
            moduleCode: 'risk-closure',
            moduleName: '风险',
            scenarioRefs: ['risk.full-drill.red-chain']
          }
        ]
      }
    ]
  };

  const result = await runRegistryCli({
    argv: [
      '--package-code=platform-p0-full-flow',
      '--selected-modules=telemetry-read',
      '--include-deps'
    ],
    registrySource,
    packagesSource,
    adapterOverrides: {
      browserPlan: async (context) => ({
        scenarioId: context.scenario.id,
        runnerType: 'browserPlan',
        status: 'passed',
        blocking: context.scenario.blocking,
        summary: 'ok',
        evidenceFiles: []
      }),
      apiSmoke: async (context) => ({
        scenarioId: context.scenario.id,
        runnerType: 'apiSmoke',
        status: 'passed',
        blocking: context.scenario.blocking,
        summary: 'ok',
        evidenceFiles: []
      }),
      riskDrill: async (context) => ({
        scenarioId: context.scenario.id,
        runnerType: 'riskDrill',
        status: 'passed',
        blocking: context.scenario.blocking,
        summary: 'ok',
        evidenceFiles: []
      })
    }
  });

  assert.deepEqual(
    result.results.map((item) => item.scenarioId),
    ['auth.browser-smoke', 'telemetry.api-smoke']
  );
});

test('canonical registry includes P0 full-flow quality factory scenarios', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));
  const registry = await loadAcceptanceRegistry({ source });

  const ids = new Set(registry.scenarios.map((item) => item.id));

  [
    'auth.browser-smoke',
    'iot-access.browser-smoke',
    'iot-access.api-smoke',
    'iot-access.message-flow',
    'telemetry.api-smoke',
    'risk.full-drill.red-chain',
    'system.api-smoke',
    'governance.control-plane.browser-smoke',
    'quality-factory.business-acceptance.browser-smoke'
  ].forEach((scenarioId) => {
    assert.equal(ids.has(scenarioId), true, `missing P0 scenario: ${scenarioId}`);
  });

  const telemetryScenario = registry.scenarios.find((item) => item.id === 'telemetry.api-smoke');
  assert.equal(telemetryScenario.module, 'telemetry');
  assert.equal(telemetryScenario.runnerType, 'apiSmoke');
  assert.equal(telemetryScenario.scope, 'delivery');
  assert.equal(telemetryScenario.blocking, 'warning');
  assert.deepEqual(telemetryScenario.runner.pointFilters, ['TELEMETRY']);

  const qualityScenario = registry.scenarios.find(
    (item) => item.id === 'quality-factory.business-acceptance.browser-smoke'
  );
  assert.equal(qualityScenario.module, 'quality-factory');
  assert.equal(qualityScenario.runnerType, 'browserPlan');
  assert.equal(qualityScenario.scope, 'delivery');
  assert.equal(qualityScenario.blocking, 'blocker');
  assert.equal(
    qualityScenario.runner.planRef,
    'config/automation/quality-factory-web-smoke-plan.json'
  );
});

test('business acceptance packages expose P0 full-flow packages', async () => {
  const packagePath = path.resolve(
    process.cwd(),
    'config/automation/business-acceptance-packages.json'
  );
  const source = JSON.parse(await fs.readFile(packagePath, 'utf8'));
  const packages = Array.isArray(source.packages) ? source.packages : [];
  const byCode = new Map(packages.map((item) => [item.packageCode, item]));

  [
    'platform-p0-full-flow',
    'iot-access-p0',
    'risk-p0',
    'governance-p0'
  ].forEach((packageCode) => {
    assert.equal(byCode.has(packageCode), true, `missing P0 package: ${packageCode}`);
  });

  const platform = byCode.get('platform-p0-full-flow');
  assert.equal(platform.defaultAccountTemplate, 'manager-default');
  assert.deepEqual(platform.supportedEnvironments, ['dev', 'test']);

  const platformRefs = new Set(
    platform.modules.flatMap((module) => module.scenarioRefs || [])
  );
  [
    'auth.browser-smoke',
    'iot-access.browser-smoke',
    'iot-access.api-smoke',
    'iot-access.message-flow',
    'telemetry.api-smoke',
    'risk.full-drill.red-chain',
    'system.api-smoke',
    'governance.control-plane.browser-smoke',
    'quality-factory.business-acceptance.browser-smoke'
  ].forEach((scenarioId) => {
    assert.equal(
      platformRefs.has(scenarioId),
      true,
      `platform-p0-full-flow missing scenario ref: ${scenarioId}`
    );
  });
});

test('canonical registry includes P1 quality factory coverage metadata', async () => {
  const canonicalRegistryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const source = JSON.parse(await fs.readFile(canonicalRegistryPath, 'utf8'));
  const registry = await loadAcceptanceRegistry({ source });
  const byId = new Map(registry.scenarios.map((item) => [item.id, item]));

  const requiredP1ScenarioIds = [
    'product-governance.contracts.browser-smoke',
    'product-governance.mapping-rules.browser-smoke',
    'protocol-governance.p1.browser-smoke',
    'device-onboarding.p1.browser-smoke',
    'object-insight.p1.browser-smoke',
    'automation-results.p1.browser-smoke'
  ];

  for (const scenarioId of requiredP1ScenarioIds) {
    const scenario = byId.get(scenarioId);
    assert.ok(scenario, `missing P1 scenario: ${scenarioId}`);
    assert.equal(scenario.priority, 'P1');
    assert.ok(scenario.ownerDomain, `${scenarioId} ownerDomain is required`);
    assert.ok(scenario.failureCategory, `${scenarioId} failureCategory is required`);
    assert.ok(scenario.dataSetup?.strategy, `${scenarioId} dataSetup.strategy is required`);
    assert.ok(scenario.cleanupPolicy?.strategy, `${scenarioId} cleanupPolicy.strategy is required`);
  }

  assert.deepEqual(
    byId.get('product-governance.contracts.browser-smoke')?.runner.scenarioKeys,
    [
      'login',
      'product-workbench',
      'product-governance-warning-fallback',
      'product-governance-unknown-capability'
    ]
  );
  assert.equal(
    byId.get('device-onboarding.p1.browser-smoke')?.runner.planRef,
    'config/automation/device-onboarding-web-smoke-plan.json'
  );
  assert.equal(
    byId.get('object-insight.p1.browser-smoke')?.runner.planRef,
    'config/automation/object-insight-web-smoke-plan.json'
  );
  assert.deepEqual(
    byId.get('automation-results.p1.browser-smoke')?.runner.scenarioKeys,
    ['quality-factory-login', 'automation-results-workbench']
  );
});

test('business acceptance packages expose P1 coverage packages', async () => {
  const registryPath = path.resolve(
    process.cwd(),
    'config/automation/acceptance-registry.json'
  );
  const packagePath = path.resolve(
    process.cwd(),
    'config/automation/business-acceptance-packages.json'
  );
  const registrySource = JSON.parse(await fs.readFile(registryPath, 'utf8'));
  const packageSource = JSON.parse(await fs.readFile(packagePath, 'utf8'));
  const registryIds = new Set((registrySource.scenarios || []).map((item) => item.id));
  const packages = Array.isArray(packageSource.packages) ? packageSource.packages : [];
  const byCode = new Map(packages.map((item) => [item.packageCode, item]));

  for (const packageCode of [
    'product-governance-p1',
    'protocol-governance-p1',
    'device-onboarding-p1',
    'object-insight-p1',
    'automation-results-p1'
  ]) {
    const acceptancePackage = byCode.get(packageCode);
    assert.ok(acceptancePackage, `missing P1 package: ${packageCode}`);
    assert.deepEqual(acceptancePackage.supportedEnvironments, ['dev', 'test']);
    assert.ok(acceptancePackage.modules?.length, `${packageCode} modules are required`);

    for (const module of acceptancePackage.modules) {
      assert.ok(module.fallbackFailure?.stepLabel, `${packageCode}/${module.moduleCode} stepLabel is required`);
      assert.ok(module.fallbackFailure?.apiRef, `${packageCode}/${module.moduleCode} apiRef is required`);
      assert.ok(module.fallbackFailure?.pageAction, `${packageCode}/${module.moduleCode} pageAction is required`);
      assert.ok(module.fallbackFailure?.summary, `${packageCode}/${module.moduleCode} summary is required`);
      for (const scenarioRef of module.scenarioRefs || []) {
        assert.equal(
          registryIds.has(scenarioRef),
          true,
          `${packageCode}/${module.moduleCode} references missing scenario ${scenarioRef}`
        );
      }
    }
  }
});
