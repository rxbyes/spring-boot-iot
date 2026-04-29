# Global Workbench Foundation Rollout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the validated `/system-log` workbench visual language to the global foundation layer so shared list/workbench pages inherit the lighter orange-and-slate grammar without rewriting each business page.

**Architecture:** Roll the change out in three layers. First, establish reusable design tokens and Element Plus overrides for tables, tags, pagination, and quiet action links. Second, update shared workbench shell components so page shells, filter headers, tab rails, and row-action wrappers speak the same density and spacing language. Third, run a focused regression sweep on the high-frequency workbench pages that already consume these shared pieces, fixing test expectations where the new shared grammar changes rendered structure or copy emphasis.

**Tech Stack:** Vue 3 SFCs, TypeScript in `<script setup>`, scoped CSS, global CSS, Element Plus, Vitest, Vue Test Utils, Vite build, in-app browser walkthrough.

---

## File Structure

### Foundation styles

- Create: `spring-boot-iot-ui/src/__tests__/styles/globalWorkbenchFoundation.test.ts`
  - Contract-test the new token names and key global selectors so the foundation layer does not drift silently.
- Modify: `spring-boot-iot-ui/src/styles/tokens.css`
  - Add the shared workbench color/spacing aliases derived from `/system-log`.
- Modify: `spring-boot-iot-ui/src/styles/global.css`
  - Unify quiet row-action, list-surface, and pagination-adjacent grammar that is not owned by Element Plus.
- Modify: `spring-boot-iot-ui/src/styles/element-overrides.css`
  - Align Element Plus table, button, tag, and pagination defaults with the new foundation tokens.

### Shared workbench components

- Modify: `spring-boot-iot-ui/src/components/StandardPageShell.vue`
  - Promote the lighter workbench headline rhythm into the shared page shell.
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
  - Tighten header, filter, toolbar, and pagination spacing for workbench pages.
- Modify: `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
  - Make the filter header read as a work band instead of a second floating card.
- Modify: `spring-boot-iot-ui/src/components/StandardRowActions.vue`
  - Provide a stable global modifier class for quieter action rails.
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
  - Keep direct actions prominent enough for operators while reserving the menu for overflow/low-frequency actions.
- Modify: `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`
  - Apply the segmented workbench tab rail language globally.

### Tests and regressions

- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardPageShell.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`
  - Update assertions where the global workbench shell now emits refined wrapper classes, quieter tab/header surfaces, or different row-action emphasis.

### Execution note

- This plan stays in the current workspace because the active `/system-log` refinements already live here and are the visual source of truth for the rollout.

## Task 1: Establish global foundation tokens and style contracts

**Files:**
- Create: `spring-boot-iot-ui/src/__tests__/styles/globalWorkbenchFoundation.test.ts`
- Modify: `spring-boot-iot-ui/src/styles/tokens.css`
- Modify: `spring-boot-iot-ui/src/styles/global.css`
- Modify: `spring-boot-iot-ui/src/styles/element-overrides.css`

- [ ] **Step 1: Write the failing style contract test**

Create `spring-boot-iot-ui/src/__tests__/styles/globalWorkbenchFoundation.test.ts` so the foundation layer is verified from source text instead of by screenshots only.

```ts
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const readUiFile = (relativePath: string) =>
  readFileSync(resolve(process.cwd(), 'spring-boot-iot-ui', 'src', relativePath), 'utf8')

describe('global workbench foundation styles', () => {
  it('defines the shared workbench token aliases', () => {
    const tokensCss = readUiFile('styles/tokens.css')

    expect(tokensCss).toContain('--workbench-surface-bg:')
    expect(tokensCss).toContain('--workbench-surface-bg-strong:')
    expect(tokensCss).toContain('--workbench-table-header-bg:')
    expect(tokensCss).toContain('--workbench-table-row-hover-bg:')
    expect(tokensCss).toContain('--workbench-action-text:')
    expect(tokensCss).toContain('--workbench-status-danger-bg:')
  })

  it('applies the quieter workbench row-action grammar globally', () => {
    const globalCss = readUiFile('styles/global.css')

    expect(globalCss).toContain('.standard-row-actions--foundation')
    expect(globalCss).toContain('color: var(--workbench-action-text);')
    expect(globalCss).toContain('color: var(--workbench-action-hover-text);')
  })

  it('aligns Element Plus tables, tags, and pagination with the workbench foundation', () => {
    const overridesCss = readUiFile('styles/element-overrides.css')

    expect(overridesCss).toContain('--el-table-header-bg-color: var(--workbench-table-header-bg);')
    expect(overridesCss).toContain('.el-tag')
    expect(overridesCss).toContain('.standard-pagination .el-pager li.is-active')
  })
})
```

- [ ] **Step 2: Run the style contract test to verify it fails**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\styles\globalWorkbenchFoundation.test.ts
```

Expected: FAIL because the new `--workbench-*` aliases and `.standard-row-actions--foundation` selector are not defined yet.

- [ ] **Step 3: Add the global workbench token aliases and overrides**

Extend `tokens.css` with aliases that describe the `/system-log` language instead of hard-coding every consumer to raw brand values.

```css
--workbench-surface-bg: linear-gradient(
  180deg,
  color-mix(in srgb, white 94%, var(--brand) 6%) 0%,
  color-mix(in srgb, white 98%, var(--accent) 2%) 100%
);
--workbench-surface-bg-strong: linear-gradient(
  180deg,
  color-mix(in srgb, white 98%, var(--brand) 2%) 0%,
  color-mix(in srgb, white 96%, var(--accent) 4%) 100%
);
--workbench-surface-border: color-mix(in srgb, var(--panel-border) 84%, white 16%);
--workbench-table-header-bg: color-mix(in srgb, var(--surface-muted) 78%, white 22%);
--workbench-table-row-hover-bg: color-mix(in srgb, var(--brand) 5%, white 95%);
--workbench-pagination-active-bg: color-mix(in srgb, white 92%, var(--brand) 8%);
--workbench-action-text: color-mix(in srgb, var(--text-secondary) 78%, var(--brand) 22%);
--workbench-action-hover-text: var(--brand);
--workbench-status-danger-bg: color-mix(in srgb, var(--danger) 10%, white 90%);
--workbench-status-warning-bg: color-mix(in srgb, var(--warning) 12%, white 88%);
--workbench-status-success-bg: color-mix(in srgb, var(--success) 10%, white 90%);
--workbench-status-neutral-bg: color-mix(in srgb, var(--text-secondary) 8%, white 92%);
```

Wire those aliases into `global.css` and `element-overrides.css`.

```css
/* global.css */
.standard-row-actions--foundation > .standard-button,
.standard-row-actions--foundation > .standard-action-menu .standard-action-menu__trigger {
  color: var(--workbench-action-text);
  font-weight: 600;
}

.standard-row-actions--foundation > .standard-button:hover,
.standard-row-actions--foundation > .standard-action-menu .standard-action-menu__trigger:hover {
  color: var(--workbench-action-hover-text);
  background: color-mix(in srgb, var(--brand) 8%, white);
}

/* element-overrides.css */
.el-table {
  --el-table-header-bg-color: var(--workbench-table-header-bg);
}

.el-table__body tr:hover > td.el-table__cell {
  background: var(--workbench-table-row-hover-bg);
}

.el-tag {
  border-radius: var(--radius-pill);
  box-shadow: none;
}

.standard-pagination .el-pager li.is-active {
  color: var(--brand);
  background: var(--workbench-pagination-active-bg);
  box-shadow: none;
}
```

- [ ] **Step 4: Run the style contract test to verify it passes**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\styles\globalWorkbenchFoundation.test.ts
```

Expected: PASS with all three style contract assertions green.

- [ ] **Step 5: Commit the foundation-style slice**

```bash
git add spring-boot-iot-ui/src/__tests__/styles/globalWorkbenchFoundation.test.ts spring-boot-iot-ui/src/styles/tokens.css spring-boot-iot-ui/src/styles/global.css spring-boot-iot-ui/src/styles/element-overrides.css
git commit -m "feat: add global workbench foundation tokens"
```

## Task 2: Update the shared workbench shells, filter header, and tab rail

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardPageShell.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts`
- Modify: `spring-boot-iot-ui/src/components/StandardPageShell.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- Modify: `spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue`

- [ ] **Step 1: Write the failing shared-component tests**

Update the component tests so they lock the new foundation wrapper classes and the lighter segmented workbench structure.

```ts
// StandardPageShell.test.ts
expect(wrapper.classes()).toContain('standard-page-shell--workbench-foundation')
expect(wrapper.find('.standard-page-shell__headline').classes()).toContain('standard-page-shell__headline--balanced')

// StandardWorkbenchPanel.test.ts
expect(wrapper.classes()).toContain('standard-workbench-panel--workbench-foundation')
expect(wrapper.find('.standard-workbench-panel__filters').classes()).toContain('standard-workbench-panel__filters--compact')

// StandardListFilterHeader.test.ts
expect(wrapper.classes()).toContain('standard-list-filter-header--workbench-foundation')
expect(wrapper.find('.standard-list-filter-header__actions-row').classes()).toContain('standard-list-filter-header__actions-row--workbench')

// IotAccessTabWorkspace.test.ts
expect(wrapper.classes()).toContain('iot-access-tab-workspace--workbench')
expect(wrapper.find('.iot-access-tab-workspace__tabs').classes()).toContain('iot-access-tab-workspace__tabs--segmented')
```

- [ ] **Step 2: Run the shared-component tests to verify they fail**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\StandardPageShell.test.ts src\__tests__\components\StandardWorkbenchPanel.test.ts src\__tests__\components\StandardListFilterHeader.test.ts src\__tests__\components\iotAccess\IotAccessTabWorkspace.test.ts
```

Expected: FAIL because the new foundation modifier classes do not exist yet.

- [ ] **Step 3: Add the shared workbench foundation wrappers and spacing**

Update `StandardPageShell.vue` so every shared shell advertises the global foundation modifier and the headline can be styled consistently.

```vue
<section class="standard-page-shell standard-page-shell--workbench-foundation">
  <div
    v-if="showHeadline"
    class="standard-page-shell__headline"
    :class="{ 'standard-page-shell__headline--balanced': Boolean($slots.actions) }"
  >
```

```css
.standard-page-shell--workbench-foundation {
  gap: 0.66rem;
}

.standard-page-shell__headline--balanced {
  padding-bottom: 0.08rem;
  border-bottom: 1px solid color-mix(in srgb, var(--workbench-surface-border) 82%, transparent);
}
```

Update `StandardWorkbenchPanel.vue` and `StandardListFilterHeader.vue` so the filter band is tighter and reads as part of one work surface.

```vue
<div class="standard-workbench-panel ops-workbench standard-list-view standard-workbench-panel--workbench-foundation">
```

```css
.standard-workbench-panel--workbench-foundation {
  --ops-workbench-gap: 0.64rem;
}

.standard-workbench-panel__filters--compact,
.standard-list-filter-header--workbench-foundation {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}
```

Update `IotAccessTabWorkspace.vue` so the workbench variant uses a segmented rail instead of card-like tabs.

```vue
<nav
  class="iot-access-tab-workspace__tabs"
  :class="{ 'iot-access-tab-workspace__tabs--segmented': variant === 'workbench' }"
  aria-label="业务视图切换"
>
```

```css
.iot-access-tab-workspace__tabs--segmented {
  gap: 0.34rem;
  padding: 0.24rem;
  border: 1px solid var(--workbench-surface-border);
  background: var(--workbench-surface-bg);
  box-shadow: none;
}
```

- [ ] **Step 4: Run the shared-component tests to verify they pass**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\StandardPageShell.test.ts src\__tests__\components\StandardWorkbenchPanel.test.ts src\__tests__\components\StandardListFilterHeader.test.ts src\__tests__\components\iotAccess\IotAccessTabWorkspace.test.ts
```

Expected: PASS with the new wrapper-class assertions and unchanged functional behavior.

- [ ] **Step 5: Commit the shared-shell slice**

```bash
git add spring-boot-iot-ui/src/__tests__/components/StandardPageShell.test.ts spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts spring-boot-iot-ui/src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts spring-boot-iot-ui/src/components/StandardPageShell.vue spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue spring-boot-iot-ui/src/components/StandardListFilterHeader.vue spring-boot-iot-ui/src/components/iotAccess/IotAccessTabWorkspace.vue
git commit -m "feat: align shared workbench shells with foundation"
```

## Task 3: Globalize the quieter row-action grammar

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts`
- Modify: `spring-boot-iot-ui/src/components/StandardRowActions.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue`
- Modify: `spring-boot-iot-ui/src/styles/global.css`

- [ ] **Step 1: Write the failing row-action tests**

Update the two row-action tests so they lock the new quiet action rail class and keep direct actions separate from the overflow menu.

```ts
// StandardRowActions.test.ts
expect(wrapper.classes()).toContain('standard-row-actions--foundation')

// StandardWorkbenchRowActions.test.ts
expect(wrapper.classes()).toContain('standard-workbench-row-actions--quiet')
expect(wrapper.findAll('.standard-action-link').map((node) => node.text())).toEqual([
  '详情',
  '证据',
  '追踪',
  '删除'
])
expect(wrapper.find('.standard-action-menu').exists()).toBe(true)
```

- [ ] **Step 2: Run the row-action tests to verify they fail**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\StandardRowActions.test.ts src\__tests__\components\StandardWorkbenchRowActions.test.ts
```

Expected: FAIL because the foundation/quiet modifier classes are not present yet.

- [ ] **Step 3: Add the quiet row-action modifiers and styling**

Update `StandardRowActions.vue` so every consumer gets the foundation class automatically.

```ts
const classes = computed(() => [
  'standard-row-actions',
  'standard-row-actions--foundation',
  `standard-row-actions--variant-${props.variant}`,
  `standard-row-actions--${props.gap}`,
  `standard-row-actions--distribution-${props.distribution}`,
  {
    'standard-row-actions--wrap': shouldWrap.value
  }
])
```

Update `StandardWorkbenchRowActions.vue` so the global workbench-specific quiet class can be targeted from CSS without changing command behavior.

```vue
<StandardRowActions
  :variant="variant"
  :gap="resolvedGap"
  :distribution="resolvedDistribution"
  class="standard-workbench-row-actions standard-workbench-row-actions--quiet"
>
```

Finish the look in `global.css`.

```css
.standard-workbench-row-actions--quiet {
  gap: 0.42rem;
}

.standard-workbench-row-actions--quiet .standard-action-link {
  color: var(--workbench-action-text);
  font-weight: 600;
}

.standard-workbench-row-actions--quiet .standard-action-link:hover {
  color: var(--workbench-action-hover-text);
}

.standard-workbench-row-actions--quiet .standard-action-link + .standard-action-link::before,
.standard-workbench-row-actions--quiet .standard-action-menu::before {
  content: '';
  display: inline-block;
  width: 1px;
  height: 0.72rem;
  margin-right: 0.42rem;
  background: color-mix(in srgb, var(--line-panel) 74%, transparent);
}
```

- [ ] **Step 4: Run the row-action tests to verify they pass**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\components\StandardRowActions.test.ts src\__tests__\components\StandardWorkbenchRowActions.test.ts
```

Expected: PASS with the foundation and quiet action-rail assertions green.

- [ ] **Step 5: Commit the row-action slice**

```bash
git add spring-boot-iot-ui/src/__tests__/components/StandardRowActions.test.ts spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchRowActions.test.ts spring-boot-iot-ui/src/components/StandardRowActions.vue spring-boot-iot-ui/src/components/StandardWorkbenchRowActions.vue spring-boot-iot-ui/src/styles/global.css
git commit -m "feat: globalize quiet workbench row actions"
```

## Task 4: Regress the highest-traffic workbench pages against the new shared foundation

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts`

- [ ] **Step 1: Update the regression tests to look for the shared foundation wrappers**

Adjust the high-frequency page tests so they assert the refined workbench shell surfaces instead of older card-heavy assumptions.

```ts
expect(wrapper.find('.standard-page-shell--workbench-foundation').exists()).toBe(true)
expect(wrapper.find('.standard-workbench-panel--workbench-foundation').exists()).toBe(true)
expect(wrapper.find('.standard-list-filter-header--workbench-foundation').exists()).toBe(true)
```

Where a page uses the tab workspace, assert the segmented rail class:

```ts
expect(wrapper.find('.iot-access-tab-workspace__tabs--segmented').exists()).toBe(true)
```

Where a page uses workbench row actions, assert the quiet modifier:

```ts
expect(wrapper.find('.standard-workbench-row-actions--quiet').exists()).toBe(true)
```

- [ ] **Step 2: Run the targeted regression suite and verify failures are expected**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\views\AuditLogView.test.ts src\__tests__\views\DeviceWorkbenchView.test.ts src\__tests__\views\ProductWorkbenchView.test.ts src\__tests__\views\GovernanceControlPlaneViews.test.ts src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts src\__tests__\views\MessageTraceView.test.ts
```

Expected: Any failures should point to missing foundation classes or stale test assumptions, not business-logic regressions.

- [ ] **Step 3: Fix test fixtures and stubs to mirror the shared foundation changes**

Update the page-level test stubs so they render the new shared wrapper classes. For example, in view tests that stub `StandardPageShell`, `StandardWorkbenchPanel`, `StandardListFilterHeader`, or `IotAccessTabWorkspace`, make the stub output match the new shared contract:

```ts
const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: `
    <section class="standard-page-shell-stub standard-page-shell--workbench-foundation">
      <slot />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  template: `
    <section class="audit-log-workbench-stub standard-workbench-panel--workbench-foundation">
      <slot name="filters" />
      <slot />
      <slot name="pagination" />
    </section>
  `
})
```

Do not change production business-page SFCs in this task unless a shared-component contract break proves they need a tiny compatibility shim.

- [ ] **Step 4: Run the full targeted suite, build, and browser walkthrough**

Run:

```bash
node node_modules\vitest\vitest.mjs --run src\__tests__\styles\globalWorkbenchFoundation.test.ts src\__tests__\components\StandardPageShell.test.ts src\__tests__\components\StandardWorkbenchPanel.test.ts src\__tests__\components\StandardListFilterHeader.test.ts src\__tests__\components\StandardRowActions.test.ts src\__tests__\components\StandardWorkbenchRowActions.test.ts src\__tests__\components\iotAccess\IotAccessTabWorkspace.test.ts src\__tests__\views\AuditLogView.test.ts src\__tests__\views\DeviceWorkbenchView.test.ts src\__tests__\views\ProductWorkbenchView.test.ts src\__tests__\views\GovernanceControlPlaneViews.test.ts src\__tests__\views\DeviceOnboardingWorkbenchView.test.ts src\__tests__\views\MessageTraceView.test.ts
npm run build
```

Expected:

```text
PASS  src/__tests__/styles/globalWorkbenchFoundation.test.ts
PASS  src/__tests__/components/StandardPageShell.test.ts
PASS  src/__tests__/components/StandardWorkbenchPanel.test.ts
PASS  src/__tests__/components/StandardListFilterHeader.test.ts
PASS  src/__tests__/components/StandardRowActions.test.ts
PASS  src/__tests__/components/StandardWorkbenchRowActions.test.ts
PASS  src/__tests__/components/iotAccess/IotAccessTabWorkspace.test.ts
PASS  src/__tests__/views/AuditLogView.test.ts
PASS  src/__tests__/views/DeviceWorkbenchView.test.ts
PASS  src/__tests__/views/ProductWorkbenchView.test.ts
PASS  src/__tests__/views/GovernanceControlPlaneViews.test.ts
PASS  src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts
PASS  src/__tests__/views/MessageTraceView.test.ts
vite v... build complete
```

Then do an in-app browser walkthrough on these routes:

```text
/system-log
/audit-log
/devices
/products
/governance-task
/device-onboarding
/message-trace
```

Check:

```text
- top shell feels lighter and less card-stacked
- filter bands read as one working strip
- active tabs use the segmented rail instead of floating cards
- row actions are quieter until hover
- pagination uses a light active state instead of a heavy button block
```

- [ ] **Step 5: Commit the regression-alignment slice**

```bash
git add spring-boot-iot-ui/src/__tests__/views/AuditLogView.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/ProductWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/GovernanceControlPlaneViews.test.ts spring-boot-iot-ui/src/__tests__/views/DeviceOnboardingWorkbenchView.test.ts spring-boot-iot-ui/src/__tests__/views/MessageTraceView.test.ts
git commit -m "test: align high-traffic workbench pages with foundation"
```
