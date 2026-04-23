# Protocol Governance Browser Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 扩大 `/protocol-governance` 浏览器专项的真实环境覆盖，补齐详情加载、模板发布快照和批量异常约束，同时保持共享 `dev` 环境低污染。

**Architecture:** 先从自动化执行骨架补最小 `assertValue / assertDisabled` 步骤，让浏览器计划能够直接校验编辑区输入值和按钮禁用态；随后扩充协议治理专项计划，复用现有页面 `data-testid` 和 deterministic `runToken` 数据，不新增后端接口。最后以定向 node test、真实浏览器验收和文档回写收口。

**Tech Stack:** Node.js test runner, Playwright config-driven executor, Vue 3, TypeScript, JSON automation plans, Markdown docs

---

## File Structure

### Existing files to modify

- `scripts/auto/browser-config-driven.mjs`
- `scripts/run-browser-acceptance.test.mjs`
- `config/automation/protocol-governance-web-smoke-plan.json`
- `docs/真实环境测试与验收手册.md`
- `docs/08-变更记录与技术债清单.md`

### New files to create

- `scripts/browser-config-driven-step-handlers.test.mjs`

### Review-only sync targets

- `spring-boot-iot-ui/src/views/ProtocolGovernanceWorkbenchView.vue`
- `spring-boot-iot-ui/src/__tests__/views/ProtocolGovernanceWorkbenchView.test.ts`
- `README.md`
- `AGENTS.md`

说明：

- 页面已有的 `data-testid`、详情加载、模板发布和批量回滚前端约束是本轮前提；若实现中发现现有标记不足，再做最小补点。
- 协议族/解密档案审批写接口本轮不纳入浏览器专项，不为它们新增真实审批台账写入步骤。
- `README.md` 与 `AGENTS.md` 只在最终核对时确认是否需要更新；按当前范围预计无需变更。

## Implementation Notes

- `assertValue / assertDisabled` 只做本轮最小能力：分别用于读取 locator 当前 value 和断言按钮禁用态，不扩展成通用属性断言框架。
- 协议治理浏览器计划继续复用 `runToken` 生成唯一编码，避免共享环境历史数据碰撞。
- 详情步骤优先通过“先手动改表单值，再点详情重载，再断言输入值恢复”为双证据，不把 API 命中当唯一成功依据。
- 模板发布快照仍使用现有 `POST /api/governance/protocol/templates/{templateId}/publish`，不新增审批步骤。
- 若 Element Plus 瞬时消息在真实浏览器中不稳定，异常路径仍以“点击后立刻断言 body 包含错误文案”为主，不引入新的页面状态组件。

### Task 1: 为配置驱动执行器补齐 `assertValue` 步骤

**Files:**
- Create: `scripts/browser-config-driven-step-handlers.test.mjs`
- Modify: `scripts/auto/browser-config-driven.mjs`

- [ ] **Step 1: 先写 `assertValue` 步骤的失败测试**

在 `scripts/browser-config-driven-step-handlers.test.mjs` 新增：

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createConfigDrivenScenarios } from './auto/browser-config-driven.mjs';

class FakeLocator {
  constructor(value) {
    this.value = value;
  }

  first() {
    return this;
  }

  async inputValue() {
    return this.value;
  }

  async getAttribute(name) {
    if (name === 'value') {
      return this.value;
    }
    return null;
  }
}

class FakePage {
  constructor(values) {
    this.values = values;
  }

  getByTestId(testId) {
    return new FakeLocator(this.values[testId]);
  }

  locator() {
    return new FakeLocator('');
  }
}

function buildPlan(value) {
  return {
    target: {
      planName: 'assert value',
      frontendBaseUrl: 'http://127.0.0.1:5175',
      backendBaseUrl: 'http://127.0.0.1:10099'
    },
    scenarios: [
      {
        key: 'assert-value',
        name: 'assert value',
        route: '/protocol-governance',
        readySelector: '',
        requiresLogin: false,
        steps: [
          {
            id: 'assert-input-value',
            label: '断言输入值',
            type: 'assertValue',
            locator: {
              type: 'testId',
              value: 'protocol-family-display-name'
            },
            value
          }
        ]
      }
    ]
  };
}

test('assertValue step passes when input value matches expected text', async () => {
  const [scenario] = createConfigDrivenScenarios(buildPlan('自动化协议族'))({ runToken: 'token-1' });
  const detail = await scenario.run({
    page: new FakePage({
      'protocol-family-display-name': '自动化协议族'
    }),
    runtime: {},
    helpers: {
      openRoute: async () => []
    },
    options: {}
  });

  assert.equal(detail.stepResults[0].status, 'passed');
  assert.equal(detail.stepResults[0].expected, '自动化协议族');
});

test('assertValue step fails when input value does not match expected text', async () => {
  const [scenario] = createConfigDrivenScenarios(buildPlan('自动化协议族'))({ runToken: 'token-2' });

  await assert.rejects(
    () =>
      scenario.run({
        page: new FakePage({
          'protocol-family-display-name': '临时值'
        }),
        runtime: {},
        helpers: {
          openRoute: async () => []
        },
        options: {}
      }),
    /Expected value "自动化协议族", got "临时值"/
  );
});
```

- [ ] **Step 2: 运行 node 测试，确认它先红**

Run:

```bash
node --test scripts/browser-config-driven-step-handlers.test.mjs
```

Expected: FAIL，错误包含 `Unsupported step type: assertValue`。

- [ ] **Step 3: 在执行器里实现最小 `assertValue` 步骤**

在 `scripts/auto/browser-config-driven.mjs` 的内置 step handler 注册区新增：

```js
  registerPlanStepHandler('assertValue', async ({ step, page, utils }) => {
    const locator = utils.resolveLocator(page, step.locator);
    const expected = utils.interpolateTemplate(step.value || '');

    let actual = '';
    try {
      actual = await locator.inputValue();
    } catch {
      actual = (await locator.getAttribute('value')) || '';
    }

    if (actual !== expected) {
      throw new Error(`Expected value "${expected}", got "${actual}".`);
    }

    return {
      expected,
      actual
    };
  });
```

- [ ] **Step 4: 重新运行 node 测试，确认转绿**

Run:

```bash
node --test scripts/browser-config-driven-step-handlers.test.mjs
```

Expected: PASS，`2` 个测试全部通过。

- [ ] **Step 5: 小步提交**

```bash
git add scripts/auto/browser-config-driven.mjs scripts/browser-config-driven-step-handlers.test.mjs
git commit -m "test: add assert value step for browser plans"
```

### Task 2: 扩充协议治理专项计划到详情、模板发布和异常约束

**Files:**
- Modify: `config/automation/protocol-governance-web-smoke-plan.json`
- Modify: `scripts/run-browser-acceptance.test.mjs`

- [ ] **Step 1: 先写协议治理专项计划结构的失败测试**

在 `scripts/run-browser-acceptance.test.mjs` 追加：

```js
test('protocol governance smoke plan expands detail, publish and guard scenarios', () => {
  const planPath = path.join(repoRoot, 'config', 'automation', 'protocol-governance-web-smoke-plan.json');
  const plan = JSON.parse(fs.readFileSync(planPath, 'utf8'));
  const scenarios = new Map(plan.scenarios.map((scenario) => [scenario.key, scenario]));

  assert.equal(plan.scenarios.length, 4);
  for (const key of [
    'protocol-governance-login',
    'protocol-governance-workbench',
    'protocol-governance-detail-and-publish',
    'protocol-governance-batch-guards'
  ]) {
    assert.ok(scenarios.has(key), `${key} scenario should exist`);
  }

  const detailScenario = scenarios.get('protocol-governance-detail-and-publish');
  assert.equal(detailScenario?.readySelector, "[data-testid='protocol-family-save']");
  assert.ok(
    detailScenario.steps.some((step) => step.id === 'protocol-governance-family-detail-assert-display-name')
  );
  assert.ok(
    detailScenario.steps.some((step) => step.id === 'protocol-governance-template-publish')
  );

  const guardScenario = scenarios.get('protocol-governance-batch-guards');
  assert.ok(
    guardScenario?.steps.some((step) => step.id === 'protocol-governance-family-batch-rollback-guard')
  );
  assert.ok(
    guardScenario?.steps.some((step) => step.id === 'protocol-governance-profile-batch-rollback-guard')
  );
});
```

- [ ] **Step 2: 运行 node 测试，确认计划结构先红**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs
```

Expected: FAIL，错误包含协议治理计划场景数仍为 `2` 或缺少新场景 key。

- [ ] **Step 3: 扩充协议治理专项 JSON 计划**

在 `config/automation/protocol-governance-web-smoke-plan.json` 中：

1. 保留现有 `protocol-governance-login` 与 `protocol-governance-workbench`
2. 新增 `protocol-governance-detail-and-publish`
3. 新增 `protocol-governance-batch-guards`

新增场景至少包含以下步骤片段：

```json
{
  "key": "protocol-governance-detail-and-publish",
  "name": "协议治理详情与模板发布",
  "route": "/protocol-governance",
  "scope": "delivery",
  "readySelector": "[data-testid='protocol-family-save']",
  "requiresLogin": true,
  "steps": [
    {
      "id": "protocol-governance-family-detail-overwrite-display-name",
      "type": "fill",
      "locator": {
        "type": "testId",
        "value": "protocol-family-display-name"
      },
      "value": "临时协议族名称"
    },
    {
      "id": "protocol-governance-family-detail-open",
      "type": "triggerApi",
      "matcher": "/api/governance/protocol/families/",
      "action": {
        "type": "click",
        "locator": {
          "type": "css",
          "value": ".protocol-governance-workbench__item:has-text(\"autotest-family-${runToken}\") [data-testid^='protocol-family-detail-']"
        }
      }
    },
    {
      "id": "protocol-governance-family-detail-assert-display-name",
      "type": "assertValue",
      "locator": {
        "type": "testId",
        "value": "protocol-family-display-name"
      },
      "value": "自动化协议族 ${runToken}"
    }
  ]
}
```

同一场景继续补齐：

- 解密档案详情回填断言 `protocol-profile-merchant-key`
- 模板详情回填断言 `protocol-template-display-name`
- 模板发布步骤 `matcher` 使用 `/publish`

批量异常场景至少包含以下步骤片段：

```json
{
  "id": "protocol-governance-family-batch-rollback-guard",
  "type": "click",
  "locator": {
    "type": "testId",
    "value": "protocol-family-batch-submit-rollback"
  }
},
{
  "id": "protocol-governance-family-batch-rollback-guard-assert-error",
  "type": "assertText",
  "locator": {
    "type": "css",
    "value": "body"
  },
  "value": "仅已发布协议族定义支持回滚，请先选择已发布记录"
}
```

解密档案批量回滚场景以真实页面行为为准：若未发布记录被选中后按钮仍保持禁用，则改用 `assertDisabled` 收口，而不是强行模拟点击并等待错误文案。

- [ ] **Step 4: 重新运行 node 测试，确认计划与干跑转绿**

Run:

```bash
node --test scripts/run-browser-acceptance.test.mjs
node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/protocol-governance-web-smoke-plan.json --dry-run
```

Expected:

- `scripts/run-browser-acceptance.test.mjs` PASS
- dry-run 输出中出现 `protocol-governance-detail-and-publish`
- dry-run 输出中出现 `protocol-governance-batch-guards`

- [ ] **Step 5: 小步提交**

```bash
git add config/automation/protocol-governance-web-smoke-plan.json scripts/run-browser-acceptance.test.mjs
git commit -m "test: expand protocol governance browser smoke plan"
```

### Task 3: 跑真实环境验收并回写文档

**Files:**
- Modify: `docs/真实环境测试与验收手册.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: 启动真实 `dev` 前后端**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/start-backend-acceptance.ps1 -Port 10099
```

另开前端：

```powershell
cd spring-boot-iot-ui
$env:VITE_PROXY_TARGET='http://127.0.0.1:10099'
npm run acceptance:dev -- --port 5175
```

Expected:

- `http://127.0.0.1:10099/actuator/health` 返回 `UP`
- `http://127.0.0.1:5175/login` 可达

- [ ] **Step 2: 运行协议治理专项浏览器计划**

Run:

```bash
node scripts/auto/run-browser-acceptance.mjs --plan=config/automation/protocol-governance-web-smoke-plan.json --no-append-issues
```

Expected:

- exit code `0`
- 生成新的 `logs/acceptance/protocol-governance-browser-summary-<timestamp>.json`
- 新增详情、模板发布和批量异常场景全部通过

- [ ] **Step 3: 回写真实环境文档**

在 `docs/真实环境测试与验收手册.md` 补充：

```md
协议治理专项最新通过基线（<timestamp>，前端 `5175` / 后端 `10099`）：
1. `logs/acceptance/protocol-governance-browser-summary-<timestamp>.json`
2. `logs/acceptance/protocol-governance-browser-results-<timestamp>.json`
3. `logs/acceptance/protocol-governance-browser-report-<timestamp>.md`
4. 结果：覆盖详情加载、模板发布快照、协议族/解密档案批量回滚前端约束。
```

在 `docs/08-变更记录与技术债清单.md` 追加：

```md
- 2026-04-18：协议治理浏览器专项已继续扩面到详情加载、模板发布快照与批量回滚前端约束。共享 `dev` 最新计划结果为 `logs/acceptance/protocol-governance-browser-*.json/.md`，当前仍明确未把协议族/解密档案审批写链路纳入浏览器专项。
```

- [ ] **Step 4: 做最终校验**

Run:

```bash
git diff --check
git status --short
```

Expected:

- `git diff --check` 无输出
- 仅剩本任务相关文件变更

- [ ] **Step 5: 完成收口提交**

```bash
git add scripts/browser-config-driven-step-handlers.test.mjs scripts/auto/browser-config-driven.mjs scripts/run-browser-acceptance.test.mjs config/automation/protocol-governance-web-smoke-plan.json docs/真实环境测试与验收手册.md docs/08-变更记录与技术债清单.md
git commit -m "test: expand protocol governance browser coverage"
```
