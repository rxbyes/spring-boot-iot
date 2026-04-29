# Quality Factory P0 Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first-stage P0 quality factory automation loop so business users can run a full-flow acceptance package and engineers can trace every result to registry scenarios and evidence.

**Architecture:** Keep the existing quality factory architecture: `business-acceptance-packages.json` maps business modules to `acceptance-registry.json` scenarios, `run-acceptance-registry.mjs` dispatches existing runner adapters, `/business-acceptance` launches runs, and `/automation-results` reads `registry-run-*.json` evidence. This plan expands P0 coverage, adds a quality-factory browser smoke plan, adds telemetry API smoke coverage, and improves P0 failure detail extraction without adding a second automation engine.

**Tech Stack:** Spring Boot 4, Java 17, JUnit 5, Node test runner, PowerShell smoke script, Vue 3, Vitest, Playwright-based browser acceptance runner, existing `application-dev.yml` real-environment baseline.

---

## Scope

This plan implements **Stage 1: P0 executable loop** from the approved spec:

- `platform-p0-full-flow`
- `iot-access-p0`
- `risk-p0`
- `governance-p0`
- quality factory self-check through browser smoke and API package loading
- telemetry latest/history P0 smoke point
- result detail extraction for API smoke failures
- documentation for P0 execution and evidence

P1 and P2 capabilities remain separate future plans.

## File Structure

- Modify: `config/automation/acceptance-registry.json`
  - Add P0 registry scenarios for telemetry and quality factory.
  - Keep existing scenario IDs stable.
- Modify: `config/automation/business-acceptance-packages.json`
  - Add P0 business packages and map their modules to registry scenarios.
- Create: `config/automation/quality-factory-web-smoke-plan.json`
  - Browser plan for `/business-acceptance`, `/business-acceptance/results/:runId` route compatibility, and `/automation-results`.
- Modify: `scripts/run-business-function-smoke.ps1`
  - Add `TELEMETRY` point and module mapping.
  - Probe `/api/telemetry/latest` and `/api/telemetry/history/batch` after the device/reporting setup has a `deviceId`.
- Modify: `scripts/run-business-function-smoke.test.mjs`
  - Guard `TELEMETRY` point filtering and endpoint calls.
- Modify: `scripts/auto/acceptance-runner-adapters.mjs`
  - Extract first failing API smoke step from `REPORT_SUMMARY` into `details.stepLabel`, `details.apiRef`, and `details.pageAction`.
- Create: `scripts/auto/acceptance-runner-adapters.test.mjs`
  - Unit-test API smoke detail extraction using a temporary command script and a synthetic `REPORT_SUMMARY`.
- Modify: `scripts/run-acceptance-registry.test.mjs`
  - Guard canonical P0 registry scenarios, package metadata passthrough, and P0 package references.
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
  - Guard `platform-p0-full-flow` package aggregation, dependency inclusion, and blocked classification.
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
  - Guard P0 package defaults and `blocked` result deep link behavior.
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
  - Guard quality factory views remain focused and expose P0 language.
- Modify: `docs/05-自动化测试与质量保障.md`
  - Add P0 execution commands and judging rules.
- Modify: `docs/真实环境测试与验收手册.md`
  - Add P0 real-environment runbook and evidence checklist.
- Modify: `docs/21-业务功能清单与验收标准.md`
  - Map P0 packages to business capability acceptance.
- Modify: `README.md`
  - Only update if the P0 entrypoint wording changes.
- Review: `AGENTS.md`
  - Update only if the real-environment acceptance rule changes; this plan does not require such a change.

---

### Task 1: Add Canonical P0 Registry And Package Guards

**Files:**
- Modify: `scripts/run-acceptance-registry.test.mjs`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add failing canonical P0 registry coverage test**

Append this test to `scripts/run-acceptance-registry.test.mjs`:

```js
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
```

- [ ] **Step 2: Add failing business acceptance P0 package coverage test**

Append this test to `scripts/run-acceptance-registry.test.mjs`:

```js
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
```

- [ ] **Step 3: Run the registry tests and verify they fail**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs
```

Expected: FAIL with missing `telemetry.api-smoke`, `quality-factory.business-acceptance.browser-smoke`, or P0 package assertions.

- [ ] **Step 4: Commit the failing tests**

```bash
git add scripts/run-acceptance-registry.test.mjs
git commit -m "test: guard quality factory p0 automation catalog"
```

---

### Task 2: Add P0 Registry Scenarios And Business Packages

**Files:**
- Modify: `config/automation/acceptance-registry.json`
- Modify: `config/automation/business-acceptance-packages.json`
- Test: `scripts/run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add `telemetry.api-smoke` to the canonical registry**

Insert this scenario object into `config/automation/acceptance-registry.json` under `scenarios`:

```json
{
  "id": "telemetry.api-smoke",
  "title": "遥测 latest/history 读侧冒烟",
  "module": "telemetry",
  "docRef": "docs/21#TDengine 时序点落库与 latest 查询",
  "runnerType": "apiSmoke",
  "scope": "delivery",
  "blocking": "warning",
  "dependsOn": ["iot-access.api-smoke"],
  "inputs": {},
  "evidence": ["json", "md"],
  "timeouts": {
    "maxMinutes": 8
  },
  "runner": {
    "pointFilters": ["TELEMETRY"]
  }
}
```

- [ ] **Step 2: Add quality factory browser smoke scenario to the canonical registry**

Insert this scenario object into `config/automation/acceptance-registry.json` under `scenarios`:

```json
{
  "id": "quality-factory.business-acceptance.browser-smoke",
  "title": "质量工场业务验收台与结果中心冒烟",
  "module": "quality-factory",
  "docRef": "docs/05#质量工场当前组织",
  "runnerType": "browserPlan",
  "scope": "delivery",
  "blocking": "blocker",
  "dependsOn": ["auth.browser-smoke"],
  "inputs": {},
  "evidence": ["json", "md", "screenshot"],
  "timeouts": {
    "maxMinutes": 10
  },
  "runner": {
    "planRef": "config/automation/quality-factory-web-smoke-plan.json",
    "scenarioScopes": ["delivery"],
    "failScopes": ["delivery"]
  }
}
```

- [ ] **Step 3: Add `platform-p0-full-flow` package**

Insert this package into `config/automation/business-acceptance-packages.json` under `packages`:

```json
{
  "packageCode": "platform-p0-full-flow",
  "packageName": "P0 全流程业务验收",
  "description": "覆盖登录权限、接入智维、遥测读侧、风险闭环、平台治理和质量工场自验的交付前阻断验收。",
  "targetRoles": ["acceptance", "product", "manager"],
  "supportedEnvironments": ["dev", "test"],
  "defaultAccountTemplate": "manager-default",
  "modules": [
    {
      "moduleCode": "login-auth",
      "moduleName": "登录与权限上下文",
      "scenarioRefs": ["auth.browser-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "登录并读取受保护工作台",
        "apiRef": "POST /api/auth/login；GET /api/auth/me",
        "pageAction": "进入登录页，提交账号并检查平台壳层",
        "summary": "登录或权限上下文未通过，请先检查账号、菜单授权和后端鉴权。"
      }
    },
    {
      "moduleCode": "iot-access-main",
      "moduleName": "接入智维主链路",
      "scenarioRefs": ["iot-access.browser-smoke", "iot-access.api-smoke", "iot-access.message-flow"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行产品设备、上报和链路追踪验收",
        "apiRef": "POST /api/device/product/add；POST /api/device/add；POST /api/message/http/report；GET /api/device/message-flow/trace/{traceId}",
        "pageAction": "进入产品、设备、链路验证和链路追踪页面完成冒烟",
        "summary": "接入智维主链路未通过，请按 runId 查看浏览器、接口和 message-flow 证据。"
      }
    },
    {
      "moduleCode": "telemetry-read",
      "moduleName": "遥测 latest/history 读侧",
      "scenarioRefs": ["telemetry.api-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "查询遥测 latest 和 history batch",
        "apiRef": "GET /api/telemetry/latest；POST /api/telemetry/history/batch",
        "pageAction": "通过业务烟测脚本读取最新遥测和历史趋势窗口",
        "summary": "遥测读侧未通过，请检查 TDengine、latest 路由、history 窗口和测试设备数据。"
      }
    },
    {
      "moduleCode": "risk-closure",
      "moduleName": "风险运营闭环",
      "scenarioRefs": ["risk.full-drill.red-chain"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行风险红链路演练",
        "apiRef": "POST /api/risk-point/add；POST /api/rule-definition/add；GET /api/alarm/list；GET /api/event/list",
        "pageAction": "生成风险点、策略、告警、事件和闭环证据",
        "summary": "风险闭环未通过，请优先查看 risk-drill JSON 和告警事件证据。"
      }
    },
    {
      "moduleCode": "platform-governance",
      "moduleName": "平台治理基础",
      "scenarioRefs": ["system.api-smoke", "governance.control-plane.browser-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "校验组织用户角色和治理控制面",
        "apiRef": "GET /api/organization/list；GET /api/user/list；GET /api/governance/work-items；GET /api/governance/ops-alerts",
        "pageAction": "进入治理任务台和治理运维台检查列表与上下文",
        "summary": "平台治理基础未通过，请检查系统接口、菜单授权和治理控制面页面证据。"
      }
    },
    {
      "moduleCode": "quality-factory-self-check",
      "moduleName": "质量工场自验",
      "scenarioRefs": ["quality-factory.business-acceptance.browser-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "打开业务验收台和结果中心",
        "apiRef": "GET /api/report/business-acceptance/packages；GET /api/report/automation-results/page",
        "pageAction": "进入业务验收台、查看 P0 验收包、进入结果与基线中心",
        "summary": "质量工场自验未通过，请检查业务验收包接口、页面授权和结果中心证据读取。"
      }
    }
  ]
}
```

- [ ] **Step 4: Add focused P0 packages**

Insert these three package objects into `config/automation/business-acceptance-packages.json` under `packages`:

```json
{
  "packageCode": "iot-access-p0",
  "packageName": "接入智维 P0",
  "description": "覆盖产品、设备、HTTP/MQTT 上报、链路追踪和遥测读侧。",
  "targetRoles": ["acceptance", "product", "manager"],
  "supportedEnvironments": ["dev", "test"],
  "defaultAccountTemplate": "acceptance-default",
  "modules": [
    {
      "moduleCode": "product-device-browser",
      "moduleName": "产品设备页面冒烟",
      "scenarioRefs": ["iot-access.browser-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "打开接入智维产品和设备页面",
        "apiRef": "GET /api/device/product/page；GET /api/device/page",
        "pageAction": "进入产品定义中心、设备资产中心并检查主列表",
        "summary": "产品设备页面冒烟未通过，请查看浏览器报告。"
      }
    },
    {
      "moduleCode": "product-device-api",
      "moduleName": "产品设备接口冒烟",
      "scenarioRefs": ["iot-access.api-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行产品设备接口烟测",
        "apiRef": "POST /api/device/product/add；POST /api/device/add",
        "pageAction": "通过接口烟测准备 accept 前缀产品和设备",
        "summary": "产品设备接口冒烟未通过，请查看 apiSmoke 明细。"
      }
    },
    {
      "moduleCode": "message-flow",
      "moduleName": "消息链路追踪",
      "scenarioRefs": ["iot-access.message-flow"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行 message-flow 验收",
        "apiRef": "POST /api/message/http/report；GET /api/device/message-flow/trace/{traceId}",
        "pageAction": "提交上报后查看 trace 链路",
        "summary": "消息链路追踪未通过，请查看 message-flow JSON。"
      }
    },
    {
      "moduleCode": "telemetry-read",
      "moduleName": "遥测读侧",
      "scenarioRefs": ["telemetry.api-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "查询遥测 latest/history",
        "apiRef": "GET /api/telemetry/latest；POST /api/telemetry/history/batch",
        "pageAction": "读取最新遥测和历史趋势窗口",
        "summary": "遥测读侧未通过，请检查 telemetry 存储和读路由。"
      }
    }
  ]
}
```

```json
{
  "packageCode": "risk-p0",
  "packageName": "风险闭环 P0",
  "description": "覆盖风险点、阈值策略、告警、事件、联动和预案的红链路演练。",
  "targetRoles": ["acceptance", "product", "manager"],
  "supportedEnvironments": ["dev", "test"],
  "defaultAccountTemplate": "manager-default",
  "modules": [
    {
      "moduleCode": "risk-red-chain",
      "moduleName": "风险红链路",
      "scenarioRefs": ["risk.full-drill.red-chain"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行风险红链路演练",
        "apiRef": "POST /api/risk-point/add；POST /api/rule-definition/add；GET /api/alarm/list；GET /api/event/list",
        "pageAction": "生成风险策略、告警、事件和处置证据",
        "summary": "风险红链路未通过，请查看 risk-drill 证据。"
      }
    }
  ]
}
```

```json
{
  "packageCode": "governance-p0",
  "packageName": "平台治理 P0",
  "description": "覆盖组织、用户、角色、治理任务台和治理运维台。",
  "targetRoles": ["acceptance", "product", "manager"],
  "supportedEnvironments": ["dev", "test"],
  "defaultAccountTemplate": "manager-default",
  "modules": [
    {
      "moduleCode": "system-api",
      "moduleName": "系统治理接口",
      "scenarioRefs": ["system.api-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "执行系统治理接口烟测",
        "apiRef": "GET /api/organization/list；GET /api/user/list；GET /api/role/list",
        "pageAction": "通过接口烟测校验组织、账号和角色",
        "summary": "系统治理接口未通过，请查看 apiSmoke 明细。"
      }
    },
    {
      "moduleCode": "control-plane-browser",
      "moduleName": "治理控制面页面",
      "scenarioRefs": ["governance.control-plane.browser-smoke"],
      "suggestedDirection": "needsReview",
      "fallbackFailure": {
        "stepLabel": "打开治理任务台和治理运维台",
        "apiRef": "GET /api/governance/work-items；GET /api/governance/ops-alerts",
        "pageAction": "进入管理驾驶舱、治理任务台和治理运维台",
        "summary": "治理控制面页面未通过，请查看浏览器证据。"
      }
    }
  ]
}
```

- [ ] **Step 5: Run registry tests and verify they pass**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs
```

Expected: PASS.

- [ ] **Step 6: Commit registry and package changes**

```bash
git add config/automation/acceptance-registry.json config/automation/business-acceptance-packages.json scripts/run-acceptance-registry.test.mjs
git commit -m "feat: add quality factory p0 automation catalog"
```

---

### Task 3: Add Telemetry P0 API Smoke Point

**Files:**
- Modify: `scripts/run-business-function-smoke.ps1`
- Modify: `scripts/run-business-function-smoke.test.mjs`
- Test: `scripts/run-business-function-smoke.test.mjs`

- [ ] **Step 1: Add failing telemetry point filter test**

Append this test to `scripts/run-business-function-smoke.test.mjs`:

```js
test('smoke script probes telemetry latest and history when TELEMETRY point is selected', async () => {
  const requests = [];
  let nextId = 2000;

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

      if (url.pathname === '/api/device/product/add') {
        writeJson(res, { code: 200, data: { id: nextId++ } });
        return;
      }

      if (url.pathname === '/api/device/add') {
        writeJson(res, { code: 200, data: { id: 3001 } });
        return;
      }

      if (url.pathname === '/api/message/http/report') {
        writeJson(res, { code: 200, data: { traceId: 'trace-001' } });
        return;
      }

      if (url.pathname === '/api/telemetry/latest') {
        writeJson(res, { code: 200, data: { temperature: 26.8 } });
        return;
      }

      if (url.pathname === '/api/telemetry/history/batch') {
        writeJson(res, {
          code: 200,
          data: {
            deviceId: 3001,
            series: [
              {
                identifier: 'temperature',
                buckets: [{ ts: '2026-04-25 10:00:00', value: 26.8 }]
              }
            ]
          }
        });
        return;
      }

      writeJson(res, { code: 200, data: [] });
    });
  });

  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  const port = typeof address === 'object' && address ? address.port : 0;

  const result = await runPowerShellScript([
    '-NoProfile',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    'scripts/run-business-function-smoke.ps1',
    '-BaseUrl',
    `http://127.0.0.1:${port}`,
    '-PointFilter',
    'TELEMETRY'
  ]);

  await new Promise((resolve) => server.close(resolve));

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.ok(
    requests.some(
      (item) =>
        item.method === 'GET' &&
        item.path === '/api/telemetry/latest' &&
        item.query.get('deviceId') === '3001'
    ),
    'expected telemetry latest request'
  );
  assert.ok(
    requests.some(
      (item) =>
        item.method === 'POST' &&
        item.path === '/api/telemetry/history/batch' &&
        item.body?.deviceId === 3001 &&
        item.body?.rangeCode === '1h' &&
        Array.isArray(item.body?.identifiers) &&
        item.body.identifiers.includes('temperature')
    ),
    'expected telemetry history batch request'
  );
});
```

- [ ] **Step 2: Run the telemetry smoke test and verify it fails**

Run:

```bash
node --test scripts/run-business-function-smoke.test.mjs
```

Expected: FAIL with `expected telemetry latest request` or `expected telemetry history batch request`.

- [ ] **Step 3: Add TELEMETRY to module mapping**

In `scripts/run-business-function-smoke.ps1`, update `$script:modulePointMap`:

```powershell
$script:modulePointMap = @{
    'env'       = @('ENV')
    'device'    = @('IOT-PRODUCT', 'IOT-DEVICE', 'INGEST-HTTP', 'MQTT-DOWN', 'TELEMETRY')
    'telemetry' = @('TELEMETRY')
    'alarm'     = @('ALARM', 'EVENT', 'RISK-POINT', 'RULE-DEFINITION', 'LINKAGE-RULE', 'EMERGENCY-PLAN', 'REPORT')
    'system'    = @('SYS-ORG', 'SYS-USER', 'SYS-ROLE', 'SYS-REGION', 'SYS-DICT', 'SYS-CHANNEL', 'SYS-AUDIT')
}
```

- [ ] **Step 4: Add telemetry latest/history steps after HTTP report and property/message-log checks**

In `scripts/run-business-function-smoke.ps1`, after the existing `INGEST-HTTP` message log step, add:

```powershell
if ($deviceId) {
    Invoke-Step -Point 'TELEMETRY' -Case 'latest' -Method 'GET' -Path "/api/telemetry/latest?deviceId=$deviceId" | Out-Null
    Invoke-Step -Point 'TELEMETRY' -Case 'history-batch' -Method 'POST' -Path '/api/telemetry/history/batch' -Body @{
        deviceId    = $deviceId
        identifiers = @('temperature')
        rangeCode   = '1h'
        fillPolicy  = 'zero'
    } | Out-Null
} else {
    Skip-Step -Point 'TELEMETRY' -Case 'latest' -Method 'GET' -Path '/api/telemetry/latest?deviceId={id}' -Reason 'deviceId missing'
    Skip-Step -Point 'TELEMETRY' -Case 'history-batch' -Method 'POST' -Path '/api/telemetry/history/batch' -Reason 'deviceId missing'
}
```

- [ ] **Step 5: Run the smoke script tests and verify they pass**

Run:

```bash
node --test scripts/run-business-function-smoke.test.mjs
```

Expected: PASS.

- [ ] **Step 6: Commit telemetry smoke coverage**

```bash
git add scripts/run-business-function-smoke.ps1 scripts/run-business-function-smoke.test.mjs
git commit -m "feat: add telemetry p0 smoke coverage"
```

---

### Task 4: Add Quality Factory Browser Smoke Plan

**Files:**
- Create: `config/automation/quality-factory-web-smoke-plan.json`
- Modify: `scripts/run-browser-acceptance.test.mjs`
- Test: `scripts/run-browser-acceptance.test.mjs`

- [ ] **Step 1: Add failing browser plan dry-run test**

Append this test to `scripts/run-browser-acceptance.test.mjs`:

```js
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
```

- [ ] **Step 2: Run the browser acceptance tests and verify the new test fails**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs
```

Expected: FAIL because `config/automation/quality-factory-web-smoke-plan.json` does not exist.

- [ ] **Step 3: Create the quality factory browser plan**

Create `config/automation/quality-factory-web-smoke-plan.json` with:

```json
{
  "version": "1.0.0",
  "createdAt": "2026-04-25T00:00:00+08:00",
  "target": {
    "planName": "质量工场 P0 业务验收台冒烟基线",
    "frontendBaseUrl": "http://127.0.0.1:5174",
    "backendBaseUrl": "http://127.0.0.1:9999",
    "loginRoute": "/login",
    "username": "admin",
    "password": "123456",
    "browserPath": "",
    "headless": true,
    "outputPrefix": "quality-factory-browser",
    "baselineDir": "config/automation/baselines",
    "scenarioScopes": ["delivery"],
    "failScopes": ["delivery"]
  },
  "tags": ["playwright", "config-driven", "quality-factory", "business-acceptance"],
  "plugins": ["builtin-report", "builtin-suggestion", "capture-response"],
  "scenarios": [
    {
      "key": "quality-factory-login",
      "name": "质量工场登录初始化",
      "route": "/login",
      "scope": "delivery",
      "readySelector": "#login-submit",
      "requiresLogin": false,
      "description": "初始化质量工场浏览器会话。",
      "businessFlow": "登录鉴权与业务验收台会话建立",
      "featurePoints": ["登录页可达", "登录接口返回 200", "进入受保护工作台"],
      "steps": [
        {
          "id": "quality-login-fill-username",
          "label": "填写账号",
          "type": "fill",
          "locator": { "type": "css", "value": "#login-username" },
          "value": "${target.username}"
        },
        {
          "id": "quality-login-fill-password",
          "label": "填写密码",
          "type": "fill",
          "locator": { "type": "css", "value": "#login-password" },
          "value": "${target.password}"
        },
        {
          "id": "quality-login-submit",
          "label": "提交登录",
          "type": "triggerApi",
          "matcher": "/api/auth/login",
          "action": {
            "type": "click",
            "locator": { "type": "css", "value": "#login-submit" }
          }
        },
        {
          "id": "quality-login-assert-shell",
          "label": "断言进入平台壳层",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "质量工场"
        }
      ]
    },
    {
      "key": "business-acceptance-workbench",
      "name": "业务验收台 P0 验收包可见",
      "route": "/business-acceptance",
      "scope": "delivery",
      "readySelector": ".business-acceptance-workbench",
      "requiresLogin": true,
      "description": "验证业务验收台可达，并能加载 P0 全流程业务验收包。",
      "businessFlow": "业务验收台加载业务验收包与账号模板",
      "featurePoints": ["业务验收包接口返回 200", "P0 全流程验收包可见", "运行配置区可见"],
      "initialApis": [
        { "label": "业务验收包", "matcher": "/api/report/business-acceptance/packages" },
        { "label": "账号模板", "matcher": "/api/report/business-acceptance/account-templates" }
      ],
      "steps": [
        {
          "id": "business-acceptance-assert-title",
          "label": "断言业务验收台标题可见",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "业务验收台"
        },
        {
          "id": "business-acceptance-assert-p0-package",
          "label": "断言 P0 验收包可见",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "P0 全流程业务验收"
        },
        {
          "id": "business-acceptance-assert-config",
          "label": "断言运行配置区可见",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "模块范围"
        }
      ]
    },
    {
      "key": "automation-results-workbench",
      "name": "结果与基线中心可达",
      "route": "/automation-results",
      "scope": "delivery",
      "readySelector": ".automation-results-view",
      "requiresLogin": true,
      "description": "验证结果与基线中心可达并加载历史台账区域。",
      "businessFlow": "质量工场结果中心历史运行和证据复盘",
      "featurePoints": ["历史台账可见", "证据区域可见", "结果建议区域可见"],
      "initialApis": [
        { "label": "自动化运行台账", "matcher": "/api/report/automation-results/page" }
      ],
      "steps": [
        {
          "id": "automation-results-assert-title",
          "label": "断言结果与基线中心标题可见",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "结果与基线中心"
        },
        {
          "id": "automation-results-assert-evidence",
          "label": "断言证据区域可见",
          "type": "assertText",
          "locator": { "type": "css", "value": "body" },
          "value": "证据"
        }
      ]
    }
  ]
}
```

- [ ] **Step 4: Run browser acceptance tests and verify they pass**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Dry-run the new plan directly**

Run:

```bash
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/quality-factory-web-smoke-plan.json
```

Expected: PASS and list the three scenarios.

- [ ] **Step 6: Commit the quality factory browser plan**

```bash
git add config/automation/quality-factory-web-smoke-plan.json scripts/run-browser-acceptance.test.mjs
git commit -m "feat: add quality factory browser smoke plan"
```

---

### Task 5: Extract API Smoke Failure Details Into Registry Results

**Files:**
- Modify: `scripts/auto/acceptance-runner-adapters.mjs`
- Create: `scripts/auto/acceptance-runner-adapters.test.mjs`
- Test: `scripts/auto/acceptance-runner-adapters.test.mjs`

- [ ] **Step 1: Add failing adapter detail extraction test**

Create `scripts/auto/acceptance-runner-adapters.test.mjs`:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import { createRunnerAdapters } from './acceptance-runner-adapters.mjs';

test('apiSmoke adapter maps first failing summary row to business failure details', async () => {
  const workspaceRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'api-smoke-detail-'));
  const logsDir = path.join(workspaceRoot, 'logs', 'acceptance');
  await fs.mkdir(logsDir, { recursive: true });
  const summaryPath = path.join(logsDir, 'business-function-summary-test.json');
  await fs.writeFile(
    summaryPath,
    JSON.stringify(
      [
        {
          point: 'TELEMETRY',
          case: 'latest',
          method: 'GET',
          path: '/api/telemetry/latest?deviceId=3001',
          critical: true,
          status: 'FAIL',
          detail: 'code=500; msg=tdengine unavailable'
        }
      ],
      null,
      2
    ),
    'utf8'
  );

  const runnerScript = path.join(workspaceRoot, 'api-smoke-stub.mjs');
  await fs.writeFile(
    runnerScript,
    [
      `process.stdout.write('REPORT_SUMMARY=${summaryPath.replace(/\\\\/g, '\\\\\\\\')}\\n');`,
      "process.stdout.write('SUMMARY=telemetry failed\\n');",
      'process.exitCode = 1;'
    ].join('\n'),
    'utf8'
  );

  const adapters = createRunnerAdapters({
    workspaceRoot,
    overrides: {
      apiSmoke: undefined
    }
  });

  const result = await adapters.__runCommandForTest({
    executable: process.execPath,
    args: [runnerScript],
    context: {
      workspaceRoot,
      scenario: {
        id: 'telemetry.api-smoke',
        runnerType: 'apiSmoke',
        blocking: 'warning',
        runner: {}
      },
      options: {},
      registry: { defaultTarget: {} }
    }
  });

  assert.equal(result.status, 'failed');
  assert.equal(result.details.stepLabel, 'TELEMETRY/latest');
  assert.equal(result.details.apiRef, 'GET /api/telemetry/latest?deviceId=3001');
  assert.equal(result.details.pageAction, '执行业务烟测点 TELEMETRY/latest');
  assert.match(result.summary, /tdengine unavailable|telemetry failed/);
});
```

- [ ] **Step 2: Run the adapter test and verify it fails**

Run:

```bash
node --test scripts/auto/acceptance-runner-adapters.test.mjs
```

Expected: FAIL because `__runCommandForTest` is not exported.

- [ ] **Step 3: Export a test hook and parse first failed report summary row**

In `scripts/auto/acceptance-runner-adapters.mjs`, add `fs/promises` and `path` imports if they are not present:

```js
import fs from 'node:fs/promises';
import path from 'node:path';
```

Add these helpers above `runCommandRunner`:

```js
async function readJsonFileIfPresent(filePath) {
  if (!filePath) {
    return null;
  }
  try {
    return JSON.parse(await fs.readFile(filePath, 'utf8'));
  } catch {
    return null;
  }
}

function firstFailedSummaryRow(summaryPayload) {
  const rows = Array.isArray(summaryPayload)
    ? summaryPayload
    : Array.isArray(summaryPayload?.records)
      ? summaryPayload.records
      : [];
  return rows.find((item) => String(item?.status || '').toUpperCase() !== 'PASS') || null;
}

function buildApiSmokeFailureDetails(meta, workspaceRoot) {
  const summaryPath = meta.REPORT_SUMMARY || meta.SUMMARY_JSON || '';
  const absoluteSummaryPath = summaryPath && path.isAbsolute(summaryPath)
    ? summaryPath
    : summaryPath
      ? path.join(workspaceRoot, summaryPath)
      : '';
  return { absoluteSummaryPath };
}

function applyApiSmokeFailureDetails(result, failedRow) {
  if (!failedRow) {
    return result;
  }
  const point = String(failedRow.point || '').trim();
  const caseName = String(failedRow.case || '').trim();
  const method = String(failedRow.method || '').trim();
  const apiPath = String(failedRow.path || '').trim();
  const detail = String(failedRow.detail || '').trim();
  return {
    ...result,
    summary: detail || result.summary,
    details: {
      ...result.details,
      stepLabel: [point, caseName].filter(Boolean).join('/'),
      apiRef: [method, apiPath].filter(Boolean).join(' '),
      pageAction: `执行业务烟测点 ${[point, caseName].filter(Boolean).join('/')}`,
      smokePoint: point,
      smokeCase: caseName,
      smokeDetail: detail
    }
  };
}
```

Update `runCommandRunner` after `const evidenceFiles = ...` and before `return`:

```js
  const baseResult = {
    scenarioId: context.scenario.id,
    runnerType: context.scenario.runnerType,
    status: completed.code === 0 ? 'passed' : 'failed',
    blocking: context.scenario.blocking,
    summary:
      meta.SUMMARY ||
      meta.STATUS ||
      completed.stderr.trim() ||
      completed.stdout.trim().split(/\r?\n/).at(-1) ||
      `${context.scenario.id} ${completed.code === 0 ? 'passed' : 'failed'}`,
    evidenceFiles,
    details: {
      executable,
      args,
      exitCode: completed.code,
      stdout: completed.stdout,
      stderr: completed.stderr
    }
  };

  if (context.scenario.runnerType !== 'apiSmoke' || completed.code === 0) {
    return baseResult;
  }

  const { absoluteSummaryPath } = buildApiSmokeFailureDetails(meta, context.workspaceRoot);
  const summaryPayload = await readJsonFileIfPresent(absoluteSummaryPath);
  return applyApiSmokeFailureDetails(baseResult, firstFailedSummaryRow(summaryPayload));
```

Replace the old direct `return { ... }` block in `runCommandRunner` with the code above.

Export the test hook at the end of the file:

```js
export const __runCommandForTest = runCommandRunner;
```

Also expose it from `createRunnerAdapters`:

```js
    __runCommandForTest: runCommandRunner
```

- [ ] **Step 4: Run the adapter test and verify it passes**

Run:

```bash
node --test scripts/auto/acceptance-runner-adapters.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Run registry tests to protect existing behavior**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs
```

Expected: PASS.

- [ ] **Step 6: Commit API smoke detail extraction**

```bash
git add scripts/auto/acceptance-runner-adapters.mjs scripts/auto/acceptance-runner-adapters.test.mjs
git commit -m "feat: expose api smoke failure details"
```

---

### Task 6: Guard Backend P0 Business Aggregation

**Files:**
- Modify: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`
- Test: `spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java`

- [ ] **Step 1: Add backend aggregation test for platform P0 package**

Append this test to `BusinessAcceptanceServiceImplTest`:

```java
@Test
void shouldAggregatePlatformP0FullFlowPackageWithBlockedQualityFactoryModule() throws Exception {
    Path automationDir = Files.createDirectories(tempDir.resolve("config").resolve("automation"));
    Path logsDir = Files.createDirectories(tempDir.resolve("logs").resolve("acceptance"));
    Files.writeString(
            automationDir.resolve("acceptance-registry.json"),
            """
                    {
                      "version": "1.0.0",
                      "scenarios": [
                        {
                          "id": "auth.browser-smoke",
                          "title": "登录与产品设备浏览器冒烟",
                          "module": "device",
                          "runnerType": "browserPlan",
                          "scope": "delivery",
                          "blocking": "blocker",
                          "dependsOn": [],
                          "runner": {}
                        },
                        {
                          "id": "quality-factory.business-acceptance.browser-smoke",
                          "title": "质量工场业务验收台与结果中心冒烟",
                          "module": "quality-factory",
                          "runnerType": "browserPlan",
                          "scope": "delivery",
                          "blocking": "blocker",
                          "dependsOn": ["auth.browser-smoke"],
                          "runner": {}
                        }
                      ]
                    }
                    """,
            StandardCharsets.UTF_8
    );
    Files.writeString(
            automationDir.resolve("business-acceptance-packages.json"),
            """
                    {
                      "version": "1.0.0",
                      "packages": [
                        {
                          "packageCode": "platform-p0-full-flow",
                          "packageName": "P0 全流程业务验收",
                          "description": "覆盖 P0 主业务链路。",
                          "targetRoles": ["acceptance", "product", "manager"],
                          "supportedEnvironments": ["dev", "test"],
                          "defaultAccountTemplate": "manager-default",
                          "modules": [
                            {
                              "moduleCode": "login-auth",
                              "moduleName": "登录与权限上下文",
                              "scenarioRefs": ["auth.browser-smoke"],
                              "suggestedDirection": "needsReview",
                              "fallbackFailure": {
                                "stepLabel": "登录并读取受保护工作台",
                                "apiRef": "POST /api/auth/login",
                                "pageAction": "进入登录页并提交账号",
                                "summary": "登录上下文未通过。"
                              }
                            },
                            {
                              "moduleCode": "quality-factory-self-check",
                              "moduleName": "质量工场自验",
                              "scenarioRefs": ["quality-factory.business-acceptance.browser-smoke"],
                              "suggestedDirection": "needsReview",
                              "fallbackFailure": {
                                "stepLabel": "打开业务验收台和结果中心",
                                "apiRef": "GET /api/report/business-acceptance/packages",
                                "pageAction": "进入业务验收台和结果与基线中心",
                                "summary": "质量工场自验未通过。"
                              }
                            }
                          ]
                        }
                      ],
                      "accountTemplates": [
                        {
                          "templateCode": "manager-default",
                          "templateName": "项目经理账号模板",
                          "username": "manager_demo",
                          "roleHint": "项目经理",
                          "supportedEnvironments": ["dev", "test"]
                        }
                      ]
                    }
                    """,
            StandardCharsets.UTF_8
    );
    Files.writeString(
            logsDir.resolve("registry-run-20260425101010.json"),
            """
                    {
                      "runId": "20260425101010",
                      "options": {
                        "packageCode": "platform-p0-full-flow",
                        "environmentCode": "dev",
                        "accountTemplate": "manager-default",
                        "selectedModules": "login-auth,quality-factory-self-check"
                      },
                      "summary": {
                        "total": 2,
                        "passed": 1,
                        "failed": 1
                      },
                      "results": [
                        {
                          "scenarioId": "auth.browser-smoke",
                          "runnerType": "browserPlan",
                          "status": "passed",
                          "blocking": "blocker",
                          "summary": "login passed",
                          "evidenceFiles": []
                        },
                        {
                          "scenarioId": "quality-factory.business-acceptance.browser-smoke",
                          "runnerType": "browserPlan",
                          "status": "failed",
                          "blocking": "blocker",
                          "summary": "Frontend API preflight failed: 500",
                          "evidenceFiles": [],
                          "details": {
                            "stepLabel": "业务验收包接口预检",
                            "apiRef": "GET /api/report/business-acceptance/packages",
                            "pageAction": "进入业务验收台",
                            "stdout": "health unavailable"
                          }
                        }
                      ]
                    }
                    """,
            StandardCharsets.UTF_8
    );

    BusinessAcceptanceServiceImpl service = new BusinessAcceptanceServiceImpl(
            tempDir,
            automationDir.resolve("business-acceptance-packages.json"),
            automationDir.resolve("acceptance-registry.json"),
            logsDir,
            JsonMapper.builder().findAndAddModules().build()
    );

    BusinessAcceptanceResultVO result = service.getRunResult("platform-p0-full-flow", "20260425101010");

    assertThat(result.getStatus()).isEqualTo("blocked");
    assertThat(result.getFailedModuleNames()).containsExactly("质量工场自验");
    var failedModule = result.getModules().stream()
            .filter(item -> "quality-factory-self-check".equals(item.getModuleCode()))
            .findFirst()
            .orElseThrow();
    assertThat(failedModule.getStatus()).isEqualTo("blocked");
    assertThat(failedModule.getSuggestedDirection()).isEqualTo("environment");
    assertThat(failedModule.getFailureDetails().get(0).getStepLabel()).isEqualTo("业务验收包接口预检");
    assertThat(failedModule.getFailureDetails().get(0).getApiRef()).isEqualTo("GET /api/report/business-acceptance/packages");
    assertThat(failedModule.getFailureDetails().get(0).getPageAction()).isEqualTo("进入业务验收台");
}
```

- [ ] **Step 2: Run backend business acceptance tests**

Run:

```bash
mvn --% -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest test
```

Expected: PASS.

For zsh/bash on macOS or Linux, run:

```bash
mvn -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest test
```

- [ ] **Step 3: Commit backend aggregation guard**

```bash
git add spring-boot-iot-report/src/test/java/com/ghlzm/iot/report/service/impl/BusinessAcceptanceServiceImplTest.java
git commit -m "test: guard platform p0 business acceptance aggregation"
```

---

### Task 7: Guard Frontend P0 Business Acceptance Behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts`
- Test: `spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts`

- [ ] **Step 1: Add P0 package default selection test**

Append this test to `useBusinessAcceptanceWorkbench.test.ts`:

```ts
it('selects the platform P0 package defaults and preserves blocked result deep links', async () => {
  listBusinessAcceptancePackagesMock.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        ...createPackage(),
        packageCode: 'platform-p0-full-flow',
        packageName: 'P0 全流程业务验收',
        defaultAccountTemplate: 'manager-default',
        supportedEnvironments: ['dev', 'test'],
        latestResult: {
          runId: '20260425101010',
          status: 'blocked',
          updatedAt: '2026-04-25T10:10:10+08:00',
          passedModuleCount: 5,
          failedModuleCount: 1,
          failedModuleNames: ['质量工场自验']
        },
        modules: [
          {
            moduleCode: 'login-auth',
            moduleName: '登录与权限上下文',
            suggestedDirection: 'needsReview',
            scenarioRefs: ['auth.browser-smoke']
          },
          {
            moduleCode: 'quality-factory-self-check',
            moduleName: '质量工场自验',
            suggestedDirection: 'environment',
            scenarioRefs: ['quality-factory.business-acceptance.browser-smoke']
          }
        ]
      }
    ]
  });
  listBusinessAcceptanceAccountTemplatesMock.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: createAccountTemplates()
  });

  const workbench = useBusinessAcceptanceWorkbench();

  await workbench.loadInitialData();

  expect(workbench.selectedPackageCode.value).toBe('platform-p0-full-flow');
  expect(workbench.selectedEnvironment.value).toBe('dev');
  expect(workbench.selectedAccountTemplate.value).toBe('manager-default');
  expect(workbench.selectedModuleCodes.value).toEqual([
    'login-auth',
    'quality-factory-self-check'
  ]);

  await workbench.goToAutomationResults('20260425101010');

  expect(mockRouter.push).toHaveBeenCalledWith({
    path: '/automation-results',
    query: {
      runId: '20260425101010'
    }
  });
});
```

- [ ] **Step 2: Add view contract assertion for P0 language**

In `AutomationWorkbenchViews.test.ts`, update the business acceptance view test:

```ts
it('keeps the business acceptance view free of rd authoring widgets', () => {
  const source = readView('BusinessAcceptanceWorkbenchView.vue');

  expect(source).toContain('<BusinessAcceptancePackagePanel');
  expect(source).toContain('<BusinessAcceptanceRunConfigPanel');
  expect(source).toContain('launchSelectedPackage');
  expect(source).toContain('useBusinessAcceptanceWorkbench');
  expect(source).toContain('哪些模块没过');
  expect(source).not.toContain('<AutomationScenarioEditor');
  expect(source).not.toContain('<AutomationExecutionConfigPanel');
});
```

- [ ] **Step 3: Run frontend tests**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts
```

Expected: PASS.

- [ ] **Step 4: Commit frontend guards**

```bash
git add spring-boot-iot-ui/src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts spring-boot-iot-ui/src/__tests__/views/AutomationWorkbenchViews.test.ts
git commit -m "test: guard quality factory p0 frontend behavior"
```

---

### Task 8: Update P0 Documentation

**Files:**
- Modify: `docs/05-自动化测试与质量保障.md`
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/21-业务功能清单与验收标准.md`
- Modify: `README.md`

- [ ] **Step 1: Update `docs/05` with P0 quality factory execution commands**

Add this section near the quality factory guidance in `docs/05-自动化测试与质量保障.md`:

```markdown
### 10.10 质量工场 P0 全流程验收

质量工场 P0 全流程验收固定使用现有统一注册表和业务验收台，不新增第二套自动化执行系统。

推荐入口：

1. 业务、产品、项目经理进入 `/business-acceptance`，选择 `P0 全流程业务验收`。
2. 研发和测试进入 `/automation-execution`，核对 `platform-p0-full-flow` 对应 registry 场景。
3. 运行结束后进入 `/automation-results?runId=<runId>` 查看同一次证据。

CLI 执行：

```bash
node scripts/auto/run-acceptance-registry.mjs --registry-path=config/automation/acceptance-registry.json --package-code=platform-p0-full-flow --environment-code=dev --account-template=manager-default --selected-modules=login-auth,iot-access-main,telemetry-read,risk-closure,platform-governance,quality-factory-self-check --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175 --include-deps
```

阻断判断：

1. `auth.browser-smoke`、`iot-access.browser-smoke`、`risk.full-drill.red-chain`、`governance.control-plane.browser-smoke`、`quality-factory.business-acceptance.browser-smoke` 任一失败时，P0 不通过。
2. `telemetry.api-smoke` 和 `system.api-smoke` 当前按 warning 进入模块失败或复核，不单独替代真实环境综合结论。
3. 后端、前端、MQTT leader、TDengine、MySQL、Redis 或 schema 不可达时，结果应判为 `blocked`，不得写成业务失败。
```

- [ ] **Step 2: Update real environment runbook with evidence checklist**

Add this section to `docs/真实环境测试与验收手册.md` near the automation registry section:

```markdown
### 13.3 质量工场 P0 全流程真实环境验收

前置条件：

1. 后端使用 `spring-boot-iot-admin/src/main/resources/application-dev.yml` 或等价环境变量启动。
2. 前端验收服务的 `VITE_PROXY_TARGET` 指向真实后端。
3. 若使用 MQTT 场景，目标后端应为 `consumerActive=true` 的 leader 实例。
4. 历史库已经按最新 schema registry 和 seed 对齐。

执行方式：

```bash
node scripts/auto/run-acceptance-registry.mjs --id=quality-factory.business-acceptance.browser-smoke --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175 --include-deps
node scripts/auto/run-acceptance-registry.mjs --registry-path=config/automation/acceptance-registry.json --package-code=platform-p0-full-flow --environment-code=dev --account-template=manager-default --selected-modules=login-auth,iot-access-main,telemetry-read,risk-closure,platform-governance,quality-factory-self-check --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175 --include-deps
```

留痕要求：

1. `logs/acceptance/registry-run-<runId>.json`
2. `logs/acceptance/registry-run-<runId>.md`
3. `quality-factory-browser-*` 浏览器证据
4. `message-flow-*` 链路证据
5. `risk-drill-*` 风险演练证据
6. 业务验收台结果页截图或结论摘录
7. `/automation-results?runId=<runId>` 证据中心预选结果
```

- [ ] **Step 3: Update `docs/21` quality factory acceptance matrix**

In the quality factory section of `docs/21-业务功能清单与验收标准.md`, add:

```markdown
质量工场 P0 全流程业务验收当前以 `platform-p0-full-flow` 为统一业务验收包，覆盖 `登录与权限上下文 / 接入智维主链路 / 遥测 latest-history 读侧 / 风险运营闭环 / 平台治理基础 / 质量工场自验` 六类模块。验收通过必须满足业务验收台可发起、结果页可回答是否通过、失败模块可下钻、结果中心可按同一 `runId` 预选证据。
```

- [ ] **Step 4: Update `README.md` top-level quality summary**

Update the quality-factory summary bullet in `README.md` so it includes this sentence:

```markdown
质量工场支持 `P0 全流程业务验收` 预置包，按同一 `runId` 贯通业务验收台结果页与结果与基线中心证据。
```

- [ ] **Step 5: Run documentation topology check**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected in a clean worktree: PASS. In the current shared worktree, an unrelated external untracked file can still block topology; record the exact path in the task notes and do not modify that file without ownership.

- [ ] **Step 6: Commit documentation**

```bash
git add docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md README.md
git commit -m "docs: document quality factory p0 acceptance flow"
```

---

### Task 9: Run P0 Verification Bundle

**Files:**
- Verify only unless a previous task exposed a defect.

- [ ] **Step 1: Run Node automation tests**

Run:

```bash
node --test scripts/run-acceptance-registry.test.mjs scripts/run-business-function-smoke.test.mjs scripts/run-browser-acceptance.test.mjs scripts/auto/acceptance-runner-adapters.test.mjs
```

Expected: PASS.

- [ ] **Step 2: Run backend business acceptance tests**

Run:

```bash
mvn -pl spring-boot-iot-report -DskipTests=false -Dmaven.test.skip=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=BusinessAcceptanceServiceImplTest,BusinessAcceptanceControllerTest,AutomationResultQueryServiceImplTest test
```

Expected: PASS.

- [ ] **Step 3: Run frontend targeted tests**

Run:

```bash
npm --prefix spring-boot-iot-ui test -- --run src/__tests__/composables/useBusinessAcceptanceWorkbench.test.ts src/__tests__/views/AutomationWorkbenchViews.test.ts src/__tests__/composables/useAutomationRegistryWorkbench.test.ts
```

Expected: PASS.

- [ ] **Step 4: Run browser plan dry-run**

Run:

```bash
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/quality-factory-web-smoke-plan.json
```

Expected: PASS and list `quality-factory-login`, `business-acceptance-workbench`, and `automation-results-workbench`.

- [ ] **Step 5: List P0 package execution through registry CLI**

Run:

```bash
node scripts/auto/run-acceptance-registry.mjs --list --registry-path=config/automation/acceptance-registry.json --scope=delivery
```

Expected: output includes `telemetry.api-smoke` and `quality-factory.business-acceptance.browser-smoke`.

- [ ] **Step 6: Run docs topology check**

Run:

```bash
node scripts/docs/check-topology.mjs
```

Expected: PASS, or a clearly recorded unrelated external-file blocker.

- [ ] **Step 7: Commit verification notes when documentation changed**

Run:

```bash
git diff -- docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md README.md
```

When the diff is non-empty because verification findings required doc updates, make the doc update and commit:

```bash
git add docs/05-自动化测试与质量保障.md docs/真实环境测试与验收手册.md docs/21-业务功能清单与验收标准.md README.md
git commit -m "docs: record quality factory p0 verification notes"
```

When the diff is empty, record "no verification doc changes required" in the task notes and do not create an empty commit.

---

## Final Handoff Checklist

- [ ] `platform-p0-full-flow` exists and lists six business modules.
- [ ] `iot-access-p0`, `risk-p0`, and `governance-p0` exist.
- [ ] `telemetry.api-smoke` exists and dispatches `TELEMETRY`.
- [ ] `quality-factory.business-acceptance.browser-smoke` exists and points to `quality-factory-web-smoke-plan.json`.
- [ ] The smoke script probes `/api/telemetry/latest` and `/api/telemetry/history/batch`.
- [ ] API smoke failures can populate `stepLabel`, `apiRef`, and `pageAction`.
- [ ] Business acceptance aggregation classifies environment failures as `blocked`.
- [ ] Frontend business acceptance tests cover P0 defaults and blocked deep links.
- [ ] Docs describe P0 execution and evidence.
- [ ] All targeted tests in Task 9 have been run or their environment blockers are documented.
