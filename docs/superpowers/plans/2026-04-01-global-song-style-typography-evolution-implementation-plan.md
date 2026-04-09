# Global Song-Style Typography Evolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Evolve the approved full-site Song-style typography baseline into a higher-contrast hierarchy where headings feel ceremonial and operational copy stays restrained.

**Architecture:** Keep the existing color system and shared page architecture, then tighten the typography contract through CSS tokens, global rhythm rules, and shared workbench components so all governed pages inherit the same hierarchy without page-private overrides.

**Tech Stack:** Vue 3, TypeScript, CSS, Vitest, Markdown docs

---

### Task 1: Lock the shared typography hierarchy contract with TDD

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/fontTokens.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardWorkbenchPanel.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardListFilterHeader.test.ts`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardTableToolbar.test.ts`
- Create: `spring-boot-iot-ui/src/__tests__/components/StandardPageShell.test.ts`

- [ ] **Step 1: Extend the font-token contract test**

```ts
it('separates ceremonial headings from restrained dense content rhythm', () => {
  const tokensCss = read('styles/tokens.css')
  const globalCss = read('styles/global.css')
  const elementOverridesCss = read('styles/element-overrides.css')

  expect(tokensCss).toContain('--font-letter-spacing-tight:')
  expect(tokensCss).toContain('--font-letter-spacing-wide:')
  expect(tokensCss).toContain('--type-title-1-size:')
  expect(tokensCss).toContain('--type-body-size:')
  expect(globalCss).toContain('body {\n  margin: 0;\n  min-width: 320px;\n  font-family: var(--font-body);')
  expect(globalCss).toContain('line-height: var(--type-body-line-height);')
  expect(globalCss).toContain('font-size: var(--type-title-1-size);')
  expect(elementOverridesCss).toContain('--el-font-size-base: var(--type-body-size);')
})
```

- [ ] **Step 2: Add shared component tests for title and meta hierarchy**

```ts
expect(wrapper.find('.standard-page-shell__title').classes()).toContain('standard-page-shell__title')
expect(source).toContain('var(--type-title-2-size)')
expect(source).toContain('var(--type-overline-size)')
expect(source).toContain('var(--type-caption-size)')
```

```ts
expect(source).toContain('var(--type-label-size)')
expect(source).toContain('var(--font-letter-spacing-wide)')
expect(source).toContain('var(--type-toolbar-meta-size)')
```

- [ ] **Step 3: Run the focused tests to verify they fail**

Run: `npm test -- src/__tests__/utils/fontTokens.test.ts src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/StandardTableToolbar.test.ts src/__tests__/components/StandardPageShell.test.ts --run`
Expected: FAIL because the new shared typography tokens and title/meta hierarchy rules do not exist yet.

### Task 2: Implement the global Song-style typography scale

**Files:**
- Modify: `spring-boot-iot-ui/src/styles/tokens.css`
- Modify: `spring-boot-iot-ui/src/styles/global.css`
- Modify: `spring-boot-iot-ui/src/styles/element-overrides.css`

- [ ] **Step 1: Add typography scale tokens**

```css
  --font-letter-spacing-tight: -0.018em;
  --font-letter-spacing-base: 0;
  --font-letter-spacing-wide: 0.12em;
  --type-title-1-size: clamp(1.5rem, 1.08rem + 0.8vw, 1.92rem);
  --type-title-2-size: clamp(1.2rem, 1.02rem + 0.35vw, 1.38rem);
  --type-title-3-size: 1.04rem;
  --type-body-size: 0.92rem;
  --type-body-line-height: 1.72;
  --type-caption-size: 0.78rem;
  --type-label-size: 0.72rem;
  --type-toolbar-meta-size: 0.72rem;
```

- [ ] **Step 2: Rebuild global text rhythm**

```css
body {
  font-size: var(--type-body-size);
  line-height: var(--type-body-line-height);
  letter-spacing: var(--font-letter-spacing-base);
}

h1,
h2,
h3,
h4 {
  color: var(--text-heading);
  font-weight: 600;
  line-height: 1.28;
  letter-spacing: var(--font-letter-spacing-tight);
}
```

```css
p,
li,
label,
button,
input,
textarea,
select,
th,
td {
  line-height: var(--type-body-line-height);
}
```

- [ ] **Step 3: Align Element Plus typography to the new scale**

```css
:root {
  --el-font-size-base: var(--type-body-size);
}

.el-table {
  --el-table-header-text-color: var(--text-caption);
}

.el-button {
  font-size: 0.88rem;
  letter-spacing: 0.01em;
}
```

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `npm test -- src/__tests__/utils/fontTokens.test.ts --run`
Expected: PASS with the new typography scale and Element Plus bindings locked in source.

### Task 3: Apply the hierarchy to shared workbench surfaces

**Files:**
- Modify: `spring-boot-iot-ui/src/components/StandardPageShell.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardTableToolbar.vue`

- [ ] **Step 1: Strengthen page and panel titles without inflating helper copy**

```css
.standard-page-shell__title {
  font-size: var(--type-title-1-size);
  line-height: 1.24;
  letter-spacing: var(--font-letter-spacing-tight);
}

.standard-workbench-panel__title {
  font-size: var(--type-title-2-size);
  line-height: 1.28;
  letter-spacing: var(--font-letter-spacing-tight);
}
```

```css
.standard-page-shell__eyebrow,
.standard-workbench-panel__eyebrow {
  font-size: var(--type-overline-size);
  letter-spacing: var(--font-letter-spacing-wide);
}
```

- [ ] **Step 2: Restrain filter hints and toolbar meta text**

```css
.standard-list-filter-header__hint {
  font-size: var(--type-caption-size);
  line-height: 1.6;
}

.table-action-bar__meta {
  font-size: var(--type-toolbar-meta-size);
  letter-spacing: 0.04em;
}
```

- [ ] **Step 3: Run the shared component tests**

Run: `npm test -- src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/StandardTableToolbar.test.ts src/__tests__/components/StandardPageShell.test.ts --run`
Expected: PASS with all shared title and meta hierarchy contracts satisfied.

### Task 4: Update the frontend design baseline docs

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Record the new Song-style hierarchy rule**

```md
- 全站中文字体当前统一收口到宋感 serif 字体栈，但必须执行“强标题、弱正文、技术文本保留等宽”的分层规则；不得把列表、表单、表格正文再次加重到与页标题同级。
- 标题、眉题、抽屉标题、工作台标题的字号、行高、字距必须优先复用 `tokens.css` 中的共享排版令牌，不得在单页内重新写一套宋感标题比例。
```

- [ ] **Step 2: Record this round of shared-layer work**

```md
- 2026-04-01：全站宋感排版进入第二层共享治理，新增标题/正文/眉题/工具栏元信息的统一字号与字距令牌，并把 `StandardPageShell`、`StandardWorkbenchPanel`、`StandardListFilterHeader`、`StandardTableToolbar` 收口到同一刊物式层级。
```

### Task 5: Verify the frontend slice end to end

**Files:**
- Modify: `spring-boot-iot-ui/src/styles/tokens.css`
- Modify: `spring-boot-iot-ui/src/styles/global.css`
- Modify: `spring-boot-iot-ui/src/styles/element-overrides.css`
- Modify: `spring-boot-iot-ui/src/components/StandardPageShell.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardWorkbenchPanel.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardListFilterHeader.vue`
- Modify: `spring-boot-iot-ui/src/components/StandardTableToolbar.vue`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Run the focused typography tests**

Run: `npm test -- src/__tests__/utils/fontTokens.test.ts src/__tests__/components/StandardWorkbenchPanel.test.ts src/__tests__/components/StandardListFilterHeader.test.ts src/__tests__/components/StandardTableToolbar.test.ts src/__tests__/components/StandardPageShell.test.ts --run`
Expected: PASS

- [ ] **Step 2: Run the frontend guards**

Run: `npm run component:guard`
Expected: exit code `0`

Run: `npm run list:guard`
Expected: exit code `0`

Run: `npm run style:guard`
Expected: exit code `0`

- [ ] **Step 3: Run the production build**

Run: `npm run build`
Expected: exit code `0`
