# Risk Operations Row Action Spacing Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align `实时监测台`、`GIS态势图`、`告警运营台`、`事件协同台` to the same shared row-action spacing and width rules already used by `产品定义中心`, while keeping mobile/card behavior adaptive.

**Architecture:** Keep the existing global row-action primitives as the only desktop authority, then remove risk-operations page-level `gap` hints that imply a local desktop spacing rule. Lock the rollout with a small risk-operations regression test, a targeted list-page guard, shared contract tests, and in-place doc updates.

**Tech Stack:** Vue 3, TypeScript, Vitest, Node.js guard scripts, Markdown docs

---

## File Map

- `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
  - Risk monitoring table view. Remove the page-level `gap="compact"` table override so the table row actions read directly from the shared desktop baseline.
- `spring-boot-iot-ui/src/views/RiskGisView.vue`
  - GIS view with both a table action slot and a card action slot. Remove the view-local `gap` props so table rows use shared desktop spacing and cards fall back to shared card spacing.
- `spring-boot-iot-ui/src/views/EventDisposalView.vue`
  - Event disposal table view. Remove the page-level table `gap="compact"` override.
- `spring-boot-iot-ui/src/views/AlarmCenterView.vue`
  - Already aligned in code, but included in the regression test/guard scope as the risk-operations baseline reference.
- `spring-boot-iot-ui/src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`
  - New lightweight regression file that reads the four risk-operation views as source and locks “shared component + no page-private row-action gap override”.
- `spring-boot-iot-ui/scripts/list-page-guard.mjs`
  - Add a risk-operations-specific guard that forbids `gap=` on `StandardWorkbenchRowActions` when `variant="table"` in the four governed risk-operation pages.
- `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
  - Add a regression test proving table rows ignore an explicit page-level `gap` override and still render the shared desktop gap.
- `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`
  - Add a regression test for the single-visible-action desktop width tier (`96px`) so the risk-operation single-action pages are covered by shared tests.
- `docs/06-前端开发与CSS规范.md`
  - Update the hard rule to include risk-operations pages and the explicit `96 / 112 / 160px` desktop width tiers.
- `docs/15-前端优化与治理计划.md`
  - Record the risk-operations rollout as a governance action and forbid reintroducing page-level table `gap` overrides in this module.
- `docs/08-变更记录与技术债清单.md`
  - Record the root cause and the completed risk-operations alignment outcome.

### Task 1: Add a failing risk-operations regression test

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`

- [ ] **Step 1: Write the failing source-level regression test**

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

describe('risk operations row action spacing governance', () => {
  it('keeps real-time monitoring on shared row-action grammar without a page-private gap override', () => {
    const source = readViewSource('RealTimeMonitoringView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).not.toContain('gap="compact"')
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps GIS table and card actions on shared defaults without local gap overrides', () => {
    const source = readViewSource('RiskGisView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).toContain('variant="card"')
    expect(source).not.toContain('gap="compact"')
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps alarm center on shared row actions without page-private gap props', () => {
    const source = readViewSource('AlarmCenterView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).not.toContain('gap="compact"')
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps event disposal on shared row actions without a table gap override', () => {
    const source = readViewSource('EventDisposalView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).not.toContain('gap="compact"')
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })
})
```

- [ ] **Step 2: Run the new view regression test to verify it fails**

Run: `npx vitest run src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`

Expected: FAIL because `RealTimeMonitoringView.vue`、`RiskGisView.vue` and `EventDisposalView.vue` still contain page-level `gap="compact"` row-action props.

### Task 2: Tighten the risk-operations list guard first

**Files:**
- Modify: `spring-boot-iot-ui/scripts/list-page-guard.mjs`

- [ ] **Step 1: Add a risk-operations-specific forbidden pattern for table gap overrides**

```js
const riskOperationsViews = new Set([
  "RealTimeMonitoringView.vue",
  "RiskGisView.vue",
  "AlarmCenterView.vue",
  "EventDisposalView.vue",
]);
```

```js
function scanRiskOperationsForbiddenPatterns(filePath, content, errors) {
  if (!riskOperationsViews.has(path.basename(filePath))) {
    return;
  }

  const forbiddenPatterns = [
    {
      pattern: /<StandardWorkbenchRowActions\b[\s\S]{0,240}\bvariant\s*=\s*["']table["'][\s\S]{0,240}\bgap\s*=/g,
      message: "风险运营纳管页的 table 操作列禁止显式传 gap，必须直接使用共享桌面间距基线。",
    },
  ];

  forbiddenPatterns.forEach(({ pattern, message }) => {
    let match = pattern.exec(content);
    while (match) {
      pushError(errors, filePath, getLineNumber(content, match.index), message);
      match = pattern.exec(content);
    }
  });
}
```

```js
    scanRequiredUsage(filePath, content, errors);
    scanForbiddenPatterns(filePath, content, errors);
    scanRiskOperationsForbiddenPatterns(filePath, content, errors);
```

- [ ] **Step 2: Run the guard to verify it fails before the page cleanup**

Run: `npm run list:guard`

Expected: FAIL and report `RealTimeMonitoringView.vue`、`RiskGisView.vue` and `EventDisposalView.vue` for explicit table `gap` overrides.

### Task 3: Remove page-private row-action gap props from the risk-operation views

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskGisView.vue`
- Modify: `spring-boot-iot-ui/src/views/EventDisposalView.vue`

- [ ] **Step 1: Remove the table `gap` prop from real-time monitoring**

```vue
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="monitoringRowActions"
                @command="() => openDetail(row.bindingId)"
              />
```

- [ ] **Step 2: Remove both GIS row-action `gap` props so the page falls back to shared defaults**

```vue
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="gisRowActions"
                @command="() => openDetailByRiskPoint(row.riskPointId)"
              />
```

```vue
              <StandardWorkbenchRowActions
                variant="card"
                :direct-items="gisRowActions"
                @command="() => openDetailByRiskPoint(point.riskPointId)"
              />
```

- [ ] **Step 3: Remove the table `gap` prop from event disposal**

```vue
              <StandardWorkbenchRowActions
                variant="table"
                :direct-items="getEventRowActions(row)"
                @command="(command) => handleEventRowAction(command, row)"
              />
```

- [ ] **Step 4: Re-run the new regression test and the list guard**

Run: `npx vitest run src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`

Expected: PASS

Run: `npm run list:guard`

Expected: PASS

### Task 4: Lock the shared contract and update docs

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Add the shared component regression proving table rows ignore page-level gap props**

```ts
  it('ignores an explicit table gap override and keeps the shared desktop baseline', () => {
    const wrapper = mountComponent({
      variant: 'table',
      gap: 'compact',
      directItems: [
        { key: 'detail', command: 'detail', label: '详情' },
        { key: 'observe', command: 'observe', label: '观测' }
      ],
      menuItems: []
    })

    expect(wrapper.get('.standard-row-actions-stub').attributes('data-gap')).toBe('wide')
    expect(wrapper.get('.standard-row-actions-stub').attributes('data-distribution')).toBe('start')
  })
```

- [ ] **Step 2: Add the single-action desktop width-tier regression**

```ts
  it('uses the shared single-action desktop width tier for one visible table action', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [{ command: 'detail', label: '详情' }],
        gap: 'compact'
      })
    ).toBe(96)
  })
```

- [ ] **Step 3: Update the frontend spec and governance docs with the concrete risk-operations rule**

```md
- `实时监测台`、`GIS态势图`、`告警运营台`、`事件协同台` 当前与 `产品定义中心` 共用同一套列表操作列口径：桌面表格必须复用 `StandardWorkbenchRowActions` 的 `table` 变体，页面不得再显式传 `gap` 或 `distribution`；共享桌面档位固定为 `1=96px`、`2=112px`、`2+更多=160px`。
```

```md
- `2026-04-01` 已完成风险运营四页操作列收口：`实时监测台 / GIS态势图 / 告警运营台 / 事件协同台` 不再保留页面私有桌面 gap 语义，统一改为共享桌面 `12px` 间距和共享宽度分档；`GIS` 卡片操作区同步回到共享 `card` 默认节奏。
```

```md
- `2026-04-01`：风险运营模块的 `实时监测台`、`GIS态势图`、`告警运营台`、`事件协同台` 已进一步对齐到和 `产品定义中心` 相同的列表操作列基线。根因确认不是“没用共享组件”，而是页面仍保留私有 `gap` 语义、且历史上双动作短文案曾受过宽下限影响；当前已清理风险运营页的私有 gap 表达，并通过测试、list guard 与文档锁定共享 `96 / 112 / 160px` 档位。`README.md` 与 `AGENTS.md` 本轮无需变更。
```

- [ ] **Step 4: Run the shared regression tests**

Run: `npx vitest run src/__tests__/components/StandardWorkbenchRowActions.test.ts src/__tests__/utils/adaptiveActionColumn.test.ts src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`

Expected: PASS

### Task 5: Run the full frontend verification slice

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue`
- Modify: `spring-boot-iot-ui/src/views/RiskGisView.vue`
- Modify: `spring-boot-iot-ui/src/views/EventDisposalView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`
- Modify: `spring-boot-iot-ui/scripts/list-page-guard.mjs`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`
- Modify: `docs/08-变更记录与技术债清单.md`

- [ ] **Step 1: Run the focused Vitest suite**

Run: `npx vitest run src/__tests__/components/StandardWorkbenchRowActions.test.ts src/__tests__/utils/adaptiveActionColumn.test.ts src/__tests__/views/RiskOperationsRowActionSpacing.test.ts`

Expected: PASS

- [ ] **Step 2: Run the frontend guards**

Run: `npm run list:guard`

Expected: PASS

Run: `npm run component:guard`

Expected: PASS

Run: `npm run style:guard`

Expected: PASS

- [ ] **Step 3: Inspect the worktree before handoff**

Run: `git status --short`

Expected: only the planned view, test, guard, and doc files are modified or committed; unrelated existing work remains untouched.

- [ ] **Step 4: Create the final implementation commit**

```bash
git add -- "spring-boot-iot-ui/src/views/RealTimeMonitoringView.vue" "spring-boot-iot-ui/src/views/RiskGisView.vue" "spring-boot-iot-ui/src/views/EventDisposalView.vue" "spring-boot-iot-ui/src/__tests__/views/RiskOperationsRowActionSpacing.test.ts" "spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts" "spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts" "spring-boot-iot-ui/scripts/list-page-guard.mjs" "docs/06-前端开发与CSS规范.md" "docs/15-前端优化与治理计划.md" "docs/08-变更记录与技术债清单.md"
git commit -m "feat: unify risk operations row action spacing"
```
