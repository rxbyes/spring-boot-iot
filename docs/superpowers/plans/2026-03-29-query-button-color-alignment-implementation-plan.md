# Query Button Color Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align global list-page `query` buttons to the same primary color branch used by the device insight refresh button.

**Architecture:** Keep the change inside the shared `StandardButton` component by removing the query-only palette override, then lock the new behavior with a focused component test and update the frontend governance docs to describe the new rule.

**Tech Stack:** Vue 3, TypeScript, Vitest, Element Plus, Markdown docs

---

### Task 1: Lock the new button contract with TDD

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardButton.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
it('maps query actions to the shared primary branch used by confirm buttons', () => {
  const wrapper = mount(StandardButton, {
    props: { action: 'query' },
    slots: { default: '查询' },
    global: {
      stubs: {
        ElButton: ElButtonStub
      }
    }
  })

  const button = wrapper.get('button')
  expect(button.attributes('data-type')).toBe('primary')
  expect(button.attributes('data-link')).toBe('false')
  expect(button.attributes('class')).toContain('standard-button--query')
  expect(button.attributes('class')).toContain('standard-button--palette-default')
  expect(button.attributes('class')).not.toContain('standard-button--palette-query')
  expect(button.attributes('class')).toContain('standard-button--tone-solid')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `npm test -- src/__tests__/components/StandardButton.test.ts`
Expected: FAIL because `query` still renders `standard-button--palette-query`.

### Task 2: Implement the minimal shared-button change

**Files:**
- Modify: `spring-boot-iot-ui/src/components/StandardButton.vue`

- [ ] **Step 1: Remove the query-only palette override**

```ts
const resolvedPalette = computed<ButtonPalette>(() => 'default')
```

```css
.standard-button--add,
.standard-button--batch,
.standard-button--confirm,
.standard-button--delete {
  --standard-button-shadow: var(--shadow-brand);
}
```

- [ ] **Step 2: Run the focused test to verify it passes**

Run: `npm test -- src/__tests__/components/StandardButton.test.ts`
Expected: PASS with all `StandardButton` assertions green.

### Task 3: Update the documented button rule

**Files:**
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Rewrite the query-button rule**

```md
- 当前常用主操作按钮统一采用共享主按钮配色：查询、新增、提交、删除、批量等主操作与对象洞察台“刷新对象洞察”保持同一主按钮色；重置、取消、刷新、返回等次操作使用橙色描边与浅橙悬浮态。
```

- [ ] **Step 2: Rewrite the governance rule**

```md
- 按钮继续按品牌 / 辅助层级表达：查询、确认、新增、删除、批量等主操作统一使用共享主按钮白字方案，次操作使用白底橙描边和橙字，`link / text` 中的主语义操作保持品牌橙。
```

### Task 4: Verify the full frontend slice

**Files:**
- Modify: `spring-boot-iot-ui/src/components/StandardButton.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/StandardButton.test.ts`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Run the focused component test**

Run: `npm test -- src/__tests__/components/StandardButton.test.ts`
Expected: PASS

- [ ] **Step 2: Run the frontend build**

Run: `npm run build`
Expected: build succeeds with exit code `0`
