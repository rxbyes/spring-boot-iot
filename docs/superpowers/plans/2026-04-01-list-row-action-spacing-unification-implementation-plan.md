# List Row Action Spacing Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify governed list-page row action spacing on desktop tables while preserving mobile adaptive action layouts.

**Architecture:** Move the desktop distribution rule and width tiers into shared row-action primitives so governed views inherit one default behavior, then remove page-level overrides and lock the rule with tests, guards, and docs.

**Tech Stack:** Vue 3, TypeScript, Vitest, Node.js guard scripts, Markdown docs

---

### Task 1: Lock the new shared row-action contract with TDD

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/utils/adaptiveActionColumn.test.ts`

- [ ] **Step 1: Write the failing component test**

```ts
it('keeps table rows with two visible actions on start distribution', () => {
  const wrapper = mountComponent({
    variant: 'table',
    directItems: [
      { key: 'detail', command: 'detail', label: '详情' },
      { key: 'edit', command: 'edit', label: '编辑' }
    ],
    menuItems: []
  })

expect(wrapper.get('.standard-row-actions-stub').attributes('data-distribution')).toBe('start')
})

it('keeps single-action table rows on start distribution', () => {
  const wrapper = mountComponent({
    variant: 'table',
    directItems: [{ key: 'detail', command: 'detail', label: '详情' }],
    menuItems: []
  })

  expect(wrapper.get('.standard-row-actions-stub').attributes('data-distribution')).toBe('start')
})
```

- [ ] **Step 2: Write the failing width-tier tests**

```ts
it('uses the shared dual-action desktop width tier for two visible table actions', () => {
  expect(
    resolveWorkbenchActionColumnWidth({
      directItems: [
        { command: 'detail', label: '详情' },
        { command: 'observe', label: '观测' }
      ],
      gap: 'compact'
    })
  ).toBe(144)
})

it('uses the shared detail-edit-more desktop width tier when overflow actions fold into more', () => {
  expect(
    resolveWorkbenchActionColumnWidth({
      directItems: [
        { command: 'detail', label: '详情' },
        { command: 'trace', label: '追踪' },
        { command: 'observe', label: '观测' }
      ],
      gap: 'compact'
    })
  ).toBe(160)
})
```

- [ ] **Step 3: Run the focused tests to verify they fail**

Run: `npm test -- src/__tests__/components/StandardWorkbenchRowActions.test.ts src/__tests__/utils/adaptiveActionColumn.test.ts`
Expected: FAIL because table rows still default to `start`, two-action widths stay below the new desktop tier, and folded `更多` rows still resolve to the old compact width.

### Task 2: Implement the shared row-action default behavior

**Files:**
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- Modify: `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`

- [ ] **Step 1: Add the shared default distribution logic**

```ts
const visibleActionCount = computed(
  () => resolvedDirectItems.value.length + (hasMenuItems.value ? 1 : 0)
)

const resolvedDistribution = computed(() => {
  if (props.distribution) {
    return props.distribution
  }
if (props.variant === 'table' && visibleActionCount.value >= 3) {
    return 'between'
  }
  return 'start'
})
```

```vue
<StandardRowActions
  :variant="variant"
  :gap="resolvedGap"
  :distribution="resolvedDistribution"
  class="standard-workbench-row-actions"
>
```

- [ ] **Step 2: Add the shared desktop width tiers**

```ts
const WORKBENCH_TABLE_MIN_WIDTH_BY_VISIBLE_COUNT: Record<number, number> = {
  1: ACTION_MIN_WIDTH_PX,
  2: 144,
  3: 160
}

function resolveWorkbenchTableMinWidth(visibleActionCount: number, fallbackMinWidth: number) {
  return WORKBENCH_TABLE_MIN_WIDTH_BY_VISIBLE_COUNT[visibleActionCount] ?? fallbackMinWidth
}
```

```ts
const visibleActionCount = resolvedActions.directItems.length + (resolvedActions.menuItems.length > 0 ? 1 : 0)

return resolveAdaptiveActionColumnWidth({
  directLabels: resolvedActions.directItems.map((item) => item.label),
  menuLabel: resolvedActions.menuItems.length > 0 ? menuLabel : undefined,
  gap,
  minWidth: resolveWorkbenchTableMinWidth(visibleActionCount, minWidth)
})
```

- [ ] **Step 3: Run the focused tests to verify they pass**

Run: `npm test -- src/__tests__/components/StandardWorkbenchRowActions.test.ts src/__tests__/utils/adaptiveActionColumn.test.ts`
Expected: PASS with the new default desktop distribution and width tiers.

### Task 3: Remove page-level action-column overrides

**Files:**
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`

- [ ] **Step 1: Remove the device-page private distribution and width hack**

```vue
<StandardWorkbenchRowActions
  variant="table"
  gap="compact"
  :direct-items="getDeviceDirectActions(row)"
  :menu-items="getDeviceRowActions(row)"
  @command="(command) => handleRowAction(command, row)"
/>
```

```ts
const deviceActionColumnWidth = computed(() => {
  const visibleRowWidths = tableData.value.map((row) =>
    resolveWorkbenchActionColumnWidth({
      directItems: getDeviceDirectActions(row),
      menuItems: getDeviceRowActions(row),
      gap: 'compact'
    })
  )

  return visibleRowWidths.length > 0
    ? Math.max(...visibleRowWidths)
    : resolveWorkbenchActionColumnWidth({
        directItems: [
          { command: 'detail', label: '详情' },
          ...(permissionStore.hasPermission('iot:devices:update') ? [{ command: 'edit', label: '编辑' }] : [])
        ],
        menuItems: [{ command: 'more', label: '更多' }],
        gap: 'compact'
      })
})
```

- [ ] **Step 2: Update view tests to the new shared expectations**

```ts
expect(tableRowActions?.props('distribution')).toBeUndefined()
expect(String(actionColumn?.props('width'))).toBe('160')
```

```ts
expect(actionColumn?.attributes('data-width')).toBe('144')
```

```ts
expect(actionColumn?.attributes('data-width')).toBe('160')
```

- [ ] **Step 3: Run the governed view tests**

Run: `npm test -- src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts`
Expected: PASS with desktop widths now resolved by the shared tiers and device view no longer passing a private distribution prop.

### Task 4: Lock the rule in list guards and docs

**Files:**
- Modify: `spring-boot-iot-ui/scripts/list-page-guard.mjs`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Ban governed view overrides of desktop row-action distribution**

```js
{
  pattern: /<StandardWorkbenchRowActions\b[\s\S]{0,200}\bdistribution\s*=/g,
  message: '纳管页桌面表格操作列禁止再显式传 distribution，必须使用共享默认分布策略。'
}
```

- [ ] **Step 2: Update the frontend spec**

```md
- 列表页“操作”列当前统一以设备资产中心桌面端为基线：`StandardWorkbenchRowActions` 的 `table` 变体只在可见动作达到 3 个时默认启用共享等距分布，`1~2` 个可见动作继续保持共享紧凑分布，页面不得再显式传 `distribution` 覆写。
- `resolveWorkbenchActionColumnWidth` 当前继续负责桌面操作列宽度收口，并统一采用共享宽度分层：双动作桌面列使用同一基线宽度，出现 `更多` 时自动提升到三段式基线；移动端 `card` 变体继续保持自适应触控布局。
```

```md
- 纳管列表页的桌面 `操作` 列继续统一收口到共享规则：页面不得再通过 `distribution="between"`、`Math.max(160, ...)` 或 scoped CSS 自行放大间距；桌面等距与宽度分层由 `StandardWorkbenchRowActions + resolveWorkbenchActionColumnWidth` 统一负责，移动端卡片操作区仍复用 `card` 变体自适应。
```

### Task 5: Verify the full frontend slice

**Files:**
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- Modify: `spring-boot-iot-ui/src/utils/adaptiveActionColumn.ts`
- Modify: `spring-boot-iot-ui/src/views/DeviceWorkbenchView.vue`
- Modify: `spring-boot-iot-ui/scripts/list-page-guard.mjs`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Run the focused row-action tests**

Run: `npm test -- src/__tests__/components/StandardWorkbenchRowActions.test.ts src/__tests__/utils/adaptiveActionColumn.test.ts src/__tests__/views/DeviceWorkbenchView.test.ts src/__tests__/views/MessageTraceView.test.ts src/__tests__/views/AuditLogView.test.ts`
Expected: PASS

- [ ] **Step 2: Run the frontend guards**

Run: `npm run component:guard && npm run list:guard && npm run style:guard`
Expected: all three scripts exit with code `0`
