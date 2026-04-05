# IoT Access Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a minimum-deliverable automation baseline for the six existing `接入智维` entries so R&D can execute, review, and maintain the module as a dedicated automation special topic.

**Architecture:** Keep the current `browserPlan / apiSmoke / messageFlow / acceptance-registry / registry-run` substrate. Add one dedicated browser smoke plan for the six IoT-access entries, register dedicated IoT-access scenarios in `config/automation/acceptance-registry.json`, and document the special-topic execution and acceptance rules in the existing quality docs without creating a second automation system.

**Tech Stack:** JSON config, Node.js test runner, existing `scripts/auto` browser runner, existing PowerShell business smoke script, existing acceptance registry runner, Markdown docs.

---

## Task Summary

Implement the confirmed `接入智维自动化测试专项` as a config-first rollout:

1. Add one dedicated browser smoke plan that covers the six confirmed routes:
   - `/products`
   - `/devices`
   - `/reporting`
   - `/system-log`
   - `/message-trace`
   - `/file-debug`
2. Add acceptance-registry scenarios so the special topic can be executed from the current execution center and archived into the current results center.
3. Update the quality and acceptance docs so the scope, commands, and completion definition are explicit.

## Affected Modules

- `config/automation`
- `scripts`
- `docs/05-自动化测试与质量保障.md`
- `docs/08-变更记录与技术债清单.md`
- `docs/21-业务功能清单与验收标准.md`

## Assumptions

- Phase 1 keeps the rollout inside the existing automation substrate. No new backend scheduler, no new database archive, and no second result model are introduced.
- The current `sample-web-smoke-plan.json` remains the generic demo baseline; the new IoT-access plan is a dedicated special-topic plan rather than a replacement.
- The browser runner already supports plan-level variable sharing through `runToken` and `captures`, so `products -> devices -> reporting -> trace/debug` can be chained inside one plan file.
- The current routes already expose enough stable anchors for a first implementation:
  - `/products` and `/devices` use `#quick-search`
  - `/reporting` uses `#report-device-code`
  - `/system-log` and `/message-trace` use `#quick-search`
  - `/file-debug` uses `#file-debug-device-code`
- API smoke for the IoT-access special topic can reuse the current PowerShell smoke script through `-PointFilter`, instead of inventing a new smoke script.

## File Map

### New files

- `E:\idea\ghatg\spring-boot-iot\config\automation\iot-access-web-smoke-plan.json`
  Dedicated config-driven browser plan for the six IoT-access entries and the three linked flows.

### Modified files

- `E:\idea\ghatg\spring-boot-iot\config\automation\acceptance-registry.json`
  Register dedicated IoT-access special-topic scenarios for browser smoke, API smoke, and message-flow regression.
- `E:\idea\ghatg\spring-boot-iot\scripts\run-browser-acceptance.test.mjs`
  Lock the new IoT-access browser plan contract and dry-run behavior.
- `E:\idea\ghatg\spring-boot-iot\scripts\run-acceptance-registry.test.mjs`
  Lock the dedicated IoT-access registry scenarios and runner references.
- `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
  Document the IoT-access special-topic commands, scope, and completion definition.
- `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
  Record the rollout and its Phase 1 scope boundary.
- `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`
  Add the IoT-access automation special-topic to the acceptance/output guidance.

## Task 1: Lock the IoT-access browser plan contract

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\config\automation\iot-access-web-smoke-plan.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\scripts\run-browser-acceptance.test.mjs`

- [ ] **Step 1: Add the failing browser-plan contract tests**

```javascript
test('iot-access browser smoke plan dry-run works and exposes six routes', () => {
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
  assert.match(result.stdout, /"route": "\/products"/);
  assert.match(result.stdout, /"route": "\/devices"/);
  assert.match(result.stdout, /"route": "\/reporting"/);
  assert.match(result.stdout, /"route": "\/system-log"/);
  assert.match(result.stdout, /"route": "\/message-trace"/);
  assert.match(result.stdout, /"route": "\/file-debug"/);
});

test('iot-access browser smoke plan matches the current page anchors and linked-flow captures', () => {
  const planPath = path.join(repoRoot, 'config', 'automation', 'iot-access-web-smoke-plan.json');
  const plan = JSON.parse(fs.readFileSync(planPath, 'utf8'));
  const scenarios = new Map(plan.scenarios.map((scenario) => [scenario.key, scenario]));

  assert.ok(scenarios.get('iot-access-login'));
  assert.ok(scenarios.get('iot-access-products'));
  assert.ok(scenarios.get('iot-access-devices'));
  assert.ok(scenarios.get('iot-access-reporting'));
  assert.ok(scenarios.get('iot-access-system-log'));
  assert.ok(scenarios.get('iot-access-message-trace'));
  assert.ok(scenarios.get('iot-access-file-debug'));

  assert.equal(scenarios.get('iot-access-products').readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-devices').readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-reporting').readySelector, '#report-device-code');
  assert.equal(scenarios.get('iot-access-system-log').readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-message-trace').readySelector, '#quick-search');
  assert.equal(scenarios.get('iot-access-file-debug').readySelector, '#file-debug-device-code');

  const reportingScenario = scenarios.get('iot-access-reporting');
  const captureTraceStep = reportingScenario.steps.find((step) => step.id === 'reporting-submit-and-capture-trace');
  assert.equal(captureTraceStep?.type, 'triggerApi');
  assert.equal(captureTraceStep?.matcher, '/api/message/http/report');
  assert.deepEqual(captureTraceStep?.captures, [
    { variable: 'traceId', path: 'payload.data.traceId' }
  ]);
});
```

- [ ] **Step 2: Run the browser-plan contract test before the new plan exists**

Run: `node --test scripts/run-browser-acceptance.test.mjs`

Expected: FAIL because `config/automation/iot-access-web-smoke-plan.json` does not exist and the new contract assertions cannot pass yet.

- [ ] **Step 3: Create the dedicated IoT-access browser smoke plan**

```json
{
  "version": "1.0.0",
  "createdAt": "2026-04-05T00:00:00+08:00",
  "target": {
    "planName": "接入智维自动化专项基线",
    "frontendBaseUrl": "http://127.0.0.1:5174",
    "backendBaseUrl": "http://127.0.0.1:9999",
    "loginRoute": "/login",
    "username": "admin",
    "password": "123456",
    "headless": true,
    "outputPrefix": "iot-access-browser",
    "baselineDir": "config/automation/baselines",
    "scenarioScopes": ["delivery", "baseline"],
    "failScopes": ["delivery"]
  },
  "scenarios": [
    {
      "key": "iot-access-login",
      "name": "接入智维登录初始化",
      "route": "/login",
      "scope": "delivery",
      "readySelector": "#login-submit",
      "requiresLogin": false,
      "steps": [
        {
          "id": "login-fill-username",
          "type": "fill",
          "locator": { "type": "css", "value": "#login-username" },
          "value": "${target.username}"
        },
        {
          "id": "login-fill-password",
          "type": "fill",
          "locator": { "type": "css", "value": "#login-password" },
          "value": "${target.password}"
        },
        {
          "id": "login-submit",
          "type": "triggerApi",
          "matcher": "/api/auth/login",
          "action": {
            "type": "click",
            "locator": { "type": "css", "value": "#login-submit" }
          }
        }
      ]
    },
    {
      "key": "iot-access-products",
      "name": "产品定义中心主链路",
      "route": "/products",
      "scope": "delivery",
      "readySelector": "#quick-search",
      "steps": [
        {
          "id": "product-open-create",
          "type": "click",
          "locator": { "type": "role", "role": "button", "name": "新增产品", "exact": true }
        },
        {
          "id": "product-fill-key",
          "type": "fill",
          "locator": { "type": "css", "value": "#product-key" },
          "value": "iot-access-product-${runToken}"
        },
        {
          "id": "product-fill-name",
          "type": "fill",
          "locator": { "type": "css", "value": "#product-name" },
          "value": "接入智维产品 ${runToken}"
        },
        {
          "id": "product-fill-protocol",
          "type": "fill",
          "locator": { "type": "css", "value": "#protocol-code" },
          "value": "mqtt-json"
        },
        {
          "id": "product-fill-format",
          "type": "fill",
          "locator": { "type": "css", "value": "#data-format" },
          "value": "JSON"
        },
        {
          "id": "product-submit",
          "type": "triggerApi",
          "matcher": "/api/device/product/add",
          "action": {
            "type": "click",
            "locator": { "type": "css", "value": "#product-submit-button" }
          }
        }
      ]
    },
    {
      "key": "iot-access-devices",
      "name": "设备资产中心主链路",
      "route": "/devices",
      "scope": "delivery",
      "readySelector": "#quick-search",
      "steps": [
        {
          "id": "device-open-create",
          "type": "click",
          "locator": { "type": "role", "role": "button", "name": "新增设备", "exact": true }
        },
        {
          "id": "device-select-product",
          "type": "selectOption",
          "locator": { "type": "css", "value": "#device-form-product-key" },
          "optionText": "iot-access-product-${runToken}"
        },
        {
          "id": "device-fill-code",
          "type": "fill",
          "locator": { "type": "css", "value": "#device-code" },
          "value": "iot-access-device-${runToken}"
        },
        {
          "id": "device-submit",
          "type": "triggerApi",
          "matcher": "/api/device/add",
          "action": {
            "type": "click",
            "locator": { "type": "role", "role": "button", "name": "提交设备建档", "exact": true }
          }
        }
      ]
    },
    {
      "key": "iot-access-reporting",
      "name": "链路验证中心主链路",
      "route": "/reporting",
      "scope": "delivery",
      "readySelector": "#report-device-code",
      "steps": [
        {
          "id": "reporting-fill-device",
          "type": "fill",
          "locator": { "type": "css", "value": "#report-device-code" },
          "value": "iot-access-device-${runToken}"
        },
        {
          "id": "reporting-fill-payload",
          "type": "fill",
          "locator": { "type": "css", "value": "#payload" },
          "value": "{\"messageType\":\"property\",\"properties\":{\"temperature\":25.6}}"
        },
        {
          "id": "reporting-submit-and-capture-trace",
          "type": "triggerApi",
          "matcher": "/api/message/http/report",
          "action": {
            "type": "click",
            "locator": { "type": "role", "role": "button", "name": "发送验证", "exact": true }
          },
          "captures": [
            { "variable": "traceId", "path": "payload.data.traceId" }
          ]
        }
      ]
    },
    {
      "key": "iot-access-system-log",
      "name": "异常观测台主链路",
      "route": "/system-log",
      "scope": "baseline",
      "readySelector": "#quick-search",
      "steps": [
        {
          "id": "system-log-fill-quick-search",
          "type": "fill",
          "locator": { "type": "css", "value": "#quick-search" },
          "value": "${variables.traceId}"
        },
        {
          "id": "system-log-query",
          "type": "click",
          "locator": { "type": "role", "role": "button", "name": "查询", "exact": true }
        }
      ]
    },
    {
      "key": "iot-access-message-trace",
      "name": "链路追踪台主链路",
      "route": "/message-trace",
      "scope": "delivery",
      "readySelector": "#quick-search",
      "steps": [
        {
          "id": "message-trace-fill-quick-search",
          "type": "fill",
          "locator": { "type": "css", "value": "#quick-search" },
          "value": "${variables.traceId}"
        },
        {
          "id": "message-trace-query",
          "type": "click",
          "locator": { "type": "role", "role": "button", "name": "查询", "exact": true }
        }
      ]
    },
    {
      "key": "iot-access-file-debug",
      "name": "数据校验台主链路",
      "route": "/file-debug",
      "scope": "baseline",
      "readySelector": "#file-debug-device-code",
      "steps": [
        {
          "id": "file-debug-fill-device-code",
          "type": "fill",
          "locator": { "type": "css", "value": "#file-debug-device-code" },
          "value": "iot-access-device-${runToken}"
        },
        {
          "id": "file-debug-refresh",
          "type": "click",
          "locator": { "type": "role", "role": "button", "name": "刷新数据", "exact": true }
        }
      ]
    }
  ]
}
```

- [ ] **Step 4: Re-run the browser-plan contract suite and a dry-run of the dedicated plan**

Run: `node --test scripts/run-browser-acceptance.test.mjs`
Expected: PASS

Run: `node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/iot-access-web-smoke-plan.json`
Expected: exit `0`, JSON output includes the six IoT-access routes and the linked scenario order.

- [ ] **Step 5: Commit the browser-plan baseline**

```bash
git add config/automation/iot-access-web-smoke-plan.json scripts/run-browser-acceptance.test.mjs
git commit -m "feat: add iot access browser smoke baseline"
```

## Task 2: Register the IoT-access special-topic scenarios in the acceptance registry

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\config\automation\acceptance-registry.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\scripts\run-acceptance-registry.test.mjs`

- [ ] **Step 1: Add the failing acceptance-registry contract tests**

```javascript
test('canonical registry exposes dedicated iot-access special-topic scenarios', async () => {
  const registryPath = path.join(process.cwd(), 'config', 'automation', 'acceptance-registry.json');
  const registry = await loadAcceptanceRegistry({
    source: JSON.parse(await fs.readFile(registryPath, 'utf8'))
  });

  const ids = registry.scenarios.map((item) => item.id);
  assert.ok(ids.includes('iot-access.browser-smoke'));
  assert.ok(ids.includes('iot-access.api-smoke'));
  assert.ok(ids.includes('iot-access.message-flow'));
});

test('iot-access browser smoke points to the dedicated plan and api smoke reuses point filters', async () => {
  const registryPath = path.join(process.cwd(), 'config', 'automation', 'acceptance-registry.json');
  const registry = await loadAcceptanceRegistry({
    source: JSON.parse(await fs.readFile(registryPath, 'utf8'))
  });

  const scenarioMap = new Map(registry.scenarios.map((item) => [item.id, item]));

  assert.equal(
    scenarioMap.get('iot-access.browser-smoke')?.runner?.planRef,
    'config/automation/iot-access-web-smoke-plan.json'
  );
  assert.deepEqual(
    scenarioMap.get('iot-access.api-smoke')?.runner?.pointFilters,
    ['IOT-PRODUCT', 'IOT-DEVICE', 'INGEST-HTTP', 'MQTT-DOWN', 'SYS-AUDIT']
  );
  assert.equal(
    scenarioMap.get('iot-access.message-flow')?.runnerType,
    'messageFlow'
  );
});
```

- [ ] **Step 2: Run the acceptance-registry test suite before the new scenarios exist**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: FAIL because the canonical registry does not yet expose the dedicated IoT-access scenarios.

- [ ] **Step 3: Add the IoT-access registry scenarios**

```json
{
  "id": "iot-access.browser-smoke",
  "title": "接入智维浏览器主链路专项",
  "module": "iot-access",
  "docRef": "docs/superpowers/specs/2026-04-05-iot-access-automation-design.md#6",
  "runnerType": "browserPlan",
  "scope": "delivery",
  "blocking": "blocker",
  "dependsOn": [],
  "inputs": {},
  "evidence": ["json", "md", "screenshot"],
  "timeouts": { "maxMinutes": 12 },
  "runner": {
    "planRef": "config/automation/iot-access-web-smoke-plan.json",
    "scenarioScopes": ["delivery", "baseline"],
    "failScopes": ["delivery"]
  }
},
{
  "id": "iot-access.api-smoke",
  "title": "接入智维接口主链路专项",
  "module": "iot-access",
  "docRef": "docs/superpowers/specs/2026-04-05-iot-access-automation-design.md#8",
  "runnerType": "apiSmoke",
  "scope": "delivery",
  "blocking": "warning",
  "dependsOn": [],
  "inputs": {},
  "evidence": ["json", "md"],
  "timeouts": { "maxMinutes": 10 },
  "runner": {
    "pointFilters": ["IOT-PRODUCT", "IOT-DEVICE", "INGEST-HTTP", "MQTT-DOWN", "SYS-AUDIT"]
  }
},
{
  "id": "iot-access.message-flow",
  "title": "接入智维链路追踪专项",
  "module": "iot-access",
  "docRef": "docs/superpowers/specs/2026-04-05-iot-access-automation-design.md#7",
  "runnerType": "messageFlow",
  "scope": "baseline",
  "blocking": "warning",
  "dependsOn": ["iot-access.browser-smoke"],
  "inputs": {},
  "evidence": ["json"],
  "timeouts": { "maxMinutes": 10 },
  "runner": {
    "entryScript": "scripts/run-message-flow-acceptance.py",
    "requiresExpiredTraceId": true
  }
}
```

- [ ] **Step 4: Re-run the acceptance-registry test suite**

Run: `node --test scripts/run-acceptance-registry.test.mjs`

Expected: PASS, and the canonical registry exposes the three IoT-access scenarios with the correct runner mapping.

- [ ] **Step 5: Commit the registry rollout**

```bash
git add config/automation/acceptance-registry.json scripts/run-acceptance-registry.test.mjs
git commit -m "feat: register iot access automation scenarios"
```

## Task 3: Document the IoT-access special-topic execution and acceptance rules

**Files:**
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Update `docs/05` with the new IoT-access special-topic commands**

````md
### 接入智维自动化专项（Phase 1）

- 专项范围：`/products`、`/devices`、`/reporting`、`/system-log`、`/message-trace`、`/file-debug`
- 浏览器专项 dry-run：

```bash
node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/iot-access-web-smoke-plan.json
```

- 统一注册表执行：

```bash
node scripts/auto/run-acceptance-registry.mjs --id=iot-access.browser-smoke --backend-base-url=http://127.0.0.1:10099 --frontend-base-url=http://127.0.0.1:5175
```
````

- [ ] **Step 2: Update `docs/21` with the IoT-access automation completion definition**

```md
- `接入智维` 当前已新增研发专项自动化基线：首轮只要求 6 个现有入口各具备 1 条稳定主用例，并补齐 `产品 -> 设备`、`上报 -> 链路追踪`、`异常观测 -> 链路追踪` 三条跨入口联动链路；不把全部按钮、全部异常和全部视觉回归并入 Phase 1 完成定义。
```

- [ ] **Step 3: Record the rollout in `docs/08`**

```md
- 2026-04-05：已补齐“接入智维自动化测试专项”实施计划与首轮配置落地口径。当前按 `products / devices / reporting / system-log / message-trace / file-debug` 六个现有入口建立专用浏览器计划与统一注册表场景，首轮目标是主链路回归、跨入口联动和结果复盘，不新增第二套自动化体系，也不宣称已完成全量覆盖。
```

- [ ] **Step 4: Verify documentation topology**

Run: `node scripts/docs/check-topology.mjs`

Expected: `Document topology check passed.`

- [ ] **Step 5: Commit the docs update**

```bash
git add docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "docs: add iot access automation rollout guidance"
```

## Task 4: Final verification and rollout handoff

**Files:**
- Create: `E:\idea\ghatg\spring-boot-iot\config\automation\iot-access-web-smoke-plan.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\config\automation\acceptance-registry.json`
- Modify: `E:\idea\ghatg\spring-boot-iot\scripts\run-browser-acceptance.test.mjs`
- Modify: `E:\idea\ghatg\spring-boot-iot\scripts\run-acceptance-registry.test.mjs`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\05-自动化测试与质量保障.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\08-变更记录与技术债清单.md`
- Modify: `E:\idea\ghatg\spring-boot-iot\docs\21-业务功能清单与验收标准.md`

- [ ] **Step 1: Run the focused verification commands end-to-end**

Run: `node --test scripts/run-browser-acceptance.test.mjs`
Expected: PASS

Run: `node --test scripts/run-acceptance-registry.test.mjs`
Expected: PASS

Run: `node scripts/auto/run-browser-acceptance.mjs --dry-run --no-append-issues --plan=config/automation/iot-access-web-smoke-plan.json`
Expected: exit `0`

Run: `node scripts/docs/check-topology.mjs`
Expected: `Document topology check passed.`

- [ ] **Step 2: Smoke the dedicated registry scenario list output**

Run: `node scripts/auto/run-acceptance-registry.mjs --list --scope=delivery`

Expected: the list output includes:

```text
iot-access.browser-smoke
iot-access.api-smoke
```

and `--list --scope=baseline` includes:

```text
iot-access.message-flow
```

- [ ] **Step 3: Inspect the diff and make sure the Phase 1 boundary stayed intact**

Run: `git diff --stat`

Expected:

1. New config file only for the dedicated IoT-access plan
2. No new backend service/module code
3. No unrelated UI restructuring
4. Docs only describe the six-entry Phase 1 scope

- [ ] **Step 4: Commit the final rollout state**

```bash
git add config/automation/iot-access-web-smoke-plan.json config/automation/acceptance-registry.json scripts/run-browser-acceptance.test.mjs scripts/run-acceptance-registry.test.mjs docs/05-自动化测试与质量保障.md docs/08-变更记录与技术债清单.md docs/21-业务功能清单与验收标准.md
git commit -m "feat: add iot access automation baseline"
```
