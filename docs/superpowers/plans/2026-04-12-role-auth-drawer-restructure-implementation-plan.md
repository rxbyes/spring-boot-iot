# Role Auth Drawer Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the `RoleView` authorization drawer so the tree only handles directory/page access while button permissions are edited per selected page, without changing the existing backend `menuIds` contract.

**Architecture:** Keep `/api/menu/tree` and role detail `menuIds` as the only truth, add pure `menuAuth` helpers that derive a page-only tree plus per-page button groups, and refactor `RoleView.vue` into a left-form + right three-section authorization workspace. The implementation stays fully front-end: tests first, then minimal Vue changes, then document the new interaction contract in the existing docs.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, existing `Standard*` shared components and guards.

---

## File Structure

- `spring-boot-iot-ui/src/utils/menuAuth.ts`
  - Add pure helpers for page-only trees, page/button selection splitting, per-page button grouping, and final `menuIds` recomposition.
- `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts`
  - Lock the new page/button split semantics before touching `RoleView.vue`.
- `spring-boot-iot-ui/src/components/role/RoleAuthPageTreePanel.vue`
  - Render the page-only tree, search box, and page-level bulk actions.
- `spring-boot-iot-ui/src/components/role/RoleAuthSelectedPagesPanel.vue`
  - Render the selected-page cards and current-page selection state.
- `spring-boot-iot-ui/src/components/role/RoleAuthButtonPanel.vue`
  - Render current-page button empty states, search box, and page-local button bulk actions.
- `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts`
  - Verify the new authorization panels render the intended empty states and current-page button rows.
- `spring-boot-iot-ui/src/views/RoleView.vue`
  - Replace the mixed menu/button tree with the three-section authorization workspace and wire new local state.
- `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`
  - Keep the governance contract test aligned with the new structure and merged-submit semantics.
- `docs/02-业务功能与流程说明.md`
  - Update the role-permission page behavior from “same tree picks menu + button” to “page tree + per-page button refinement”.
- `docs/06-前端开发与CSS规范.md`
  - Add the new role-permission drawer layout rule so button nodes never return to the tree.
- `docs/08-变更记录与技术债清单.md`
  - Log the behavior change and verification evidence.
- `docs/15-前端优化与治理计划.md`
  - Record the prevention rule for future governance-page regressions.

## Task 1: Lock Page/Button Split Semantics in `menuAuth`

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts`
- Modify: `spring-boot-iot-ui/src/utils/menuAuth.ts`

- [ ] **Step 1: Write the failing utility tests**

Add the new imports and tests to `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts`:

```ts
import {
  buildMenuNodeMap,
  buildRolePageTree,
  composeRoleGrantedMenuIds,
  resolveRoleCheckedMenuIds,
  resolveRoleMenuSummary,
  resolveRoleSelectedButtonIdsByPage,
  resolveRoleSelectedPageIds
} from '@/utils/menuAuth';

it('builds a page-only tree and groups granted buttons by parent page', () => {
  const pageTree = buildRolePageTree(menuTree);

  expect(pageTree[0].children.map((item) => item.id)).toEqual([2, 4]);
  expect(pageTree[0].children[0].children).toEqual([]);
  expect(resolveRoleSelectedPageIds(menuTree, [1, 2, 3, 4, 5])).toEqual([2, 4]);
  expect(resolveRoleSelectedButtonIdsByPage(menuTree, [1, 2, 3, 4, 5])).toEqual({
    2: [3],
    4: [5]
  });
});

it('drops orphan button ids when recomposing submit menu ids', () => {
  expect(
    composeRoleGrantedMenuIds(menuTree, [4], {
      2: [3],
      4: [5]
    })
  ).toEqual([4, 5]);
});
```

Also extend the test fixture with a second page button so the orphan-filter case is real:

```ts
{
  id: 5,
  parentId: 4,
  menuName: '刷新菜单',
  menuCode: 'system:menu:refresh',
  type: 2,
  children: []
}
```

- [ ] **Step 2: Run the utility test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts
```

Expected: FAIL with TypeScript import errors such as `No exported member 'buildRolePageTree'` / `No exported member 'composeRoleGrantedMenuIds'`.

- [ ] **Step 3: Implement the pure helpers in `menuAuth.ts`**

Add the new helpers to `spring-boot-iot-ui/src/utils/menuAuth.ts`:

```ts
export function buildRolePageTree(nodes: MenuTreeNode[]): MenuTreeNode[] {
  return nodes
    .filter((node) => node.type !== 2)
    .map((node) => ({
      ...node,
      children: buildRolePageTree(node.children || [])
    }));
}

export function resolveRoleSelectedPageIds(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): number[] {
  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is number => typeof menuId === 'number')
  );

  const pageIds: number[] = [];
  visitMenus(nodes, (node) => {
    if (node.type === 1 && grantedSet.has(node.id)) {
      pageIds.push(node.id);
    }
  });
  return pageIds;
}

export function resolveRoleSelectedButtonIdsByPage(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): Record<number, number[]> {
  const grantedSet = new Set(
    grantedIds.filter((menuId): menuId is number => typeof menuId === 'number')
  );
  const buttonIdsByPage: Record<number, number[]> = {};

  visitMenus(nodes, (node) => {
    if (node.type !== 2 || typeof node.parentId !== 'number' || !grantedSet.has(node.id)) {
      return;
    }
    if (!buttonIdsByPage[node.parentId]) {
      buttonIdsByPage[node.parentId] = [];
    }
    buttonIdsByPage[node.parentId].push(node.id);
  });

  return buttonIdsByPage;
}

export function composeRoleGrantedMenuIds(
  nodes: MenuTreeNode[],
  selectedPageIds: number[],
  selectedButtonIdsByPage: Record<number, number[]>
): number[] {
  const pageSet = new Set(selectedPageIds);
  const result = new Set<number>(selectedPageIds);
  const nodeMap = buildMenuNodeMap(nodes);

  Object.entries(selectedButtonIdsByPage).forEach(([pageIdText, buttonIds]) => {
    const pageId = Number(pageIdText);
    if (!pageSet.has(pageId)) {
      return;
    }
    buttonIds.forEach((buttonId) => {
      const node = nodeMap.get(buttonId);
      if (node?.type === 2 && node.parentId === pageId) {
        result.add(buttonId);
      }
    });
  });

  return Array.from(result);
}
```

- [ ] **Step 4: Re-run the utility test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts
```

Expected: PASS with all `menuAuth utils` tests green.

- [ ] **Step 5: Commit the utility layer**

Run:

```bash
git add spring-boot-iot-ui/src/utils/menuAuth.ts spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts
git commit -m "test: lock role auth menu split helpers"
```

## Task 2: Add Presentational Role Authorization Panels

**Files:**
- Create: `spring-boot-iot-ui/src/components/role/RoleAuthPageTreePanel.vue`
- Create: `spring-boot-iot-ui/src/components/role/RoleAuthSelectedPagesPanel.vue`
- Create: `spring-boot-iot-ui/src/components/role/RoleAuthButtonPanel.vue`
- Create: `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts`

- [ ] **Step 1: Write the failing panel tests**

Create `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts`:

```ts
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RoleAuthButtonPanel from '@/components/role/RoleAuthButtonPanel.vue';
import RoleAuthSelectedPagesPanel from '@/components/role/RoleAuthSelectedPagesPanel.vue';

describe('Role auth panels', () => {
  it('shows a guidance empty state before a page is selected', () => {
    const wrapper = mount(RoleAuthButtonPanel, {
      props: {
        activePage: null,
        buttonRows: [],
        keyword: '',
        loading: false
      }
    });

    expect(wrapper.text()).toContain('请先勾选页面，或从已选页面列表选择一个页面');
  });

  it('shows page status cards without expanding every button name', () => {
    const wrapper = mount(RoleAuthSelectedPagesPanel, {
      props: {
        items: [
          { id: 2, menuName: '角色权限', path: '/role', buttonSummary: '已选 2 个按钮', active: true },
          { id: 4, menuName: '导航编排', path: '/menu', buttonSummary: '无独立按钮', active: false }
        ]
      }
    });

    expect(wrapper.text()).toContain('已选 2 个按钮');
    expect(wrapper.text()).toContain('无独立按钮');
    expect(wrapper.text()).not.toContain('system:role:add');
  });
});
```

- [ ] **Step 2: Run the panel test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RoleAuthPanels.test.ts
```

Expected: FAIL with module resolution errors because the new role authorization components do not exist yet.

- [ ] **Step 3: Create the three panel components with minimal props/emits**

Create `spring-boot-iot-ui/src/components/role/RoleAuthPageTreePanel.vue`:

```vue
<template>
  <section class="role-auth-panel">
    <div class="role-auth-panel__header">
      <div>
        <h3>步骤 1：页面授权</h3>
        <p>树里只显示目录和页面；按钮权限请在页面选定后单独配置。</p>
      </div>
      <slot name="actions" />
    </div>
    <slot />
  </section>
</template>
```

Create `spring-boot-iot-ui/src/components/role/RoleAuthSelectedPagesPanel.vue`:

```vue
<script setup lang="ts">
defineProps<{
  items: Array<{ id: number; menuName: string; path?: string; buttonSummary: string; active: boolean }>;
}>();
const emit = defineEmits<{ select: [pageId: number] }>();
</script>

<template>
  <section class="role-auth-panel">
    <div class="role-auth-panel__header">
      <div>
        <h3>步骤 2：已选页面</h3>
        <p>从已选页面里挑一个页面，再精修该页面按钮。</p>
      </div>
    </div>
    <button
      v-for="item in items"
      :key="item.id"
      class="role-selected-page-card"
      :class="{ 'role-selected-page-card--active': item.active }"
      type="button"
      @click="emit('select', item.id)"
    >
      <strong>{{ item.menuName }}</strong>
      <span>{{ item.path || '--' }}</span>
      <span>{{ item.buttonSummary }}</span>
    </button>
  </section>
</template>
```

Create `spring-boot-iot-ui/src/components/role/RoleAuthButtonPanel.vue`:

```vue
<script setup lang="ts">
defineProps<{
  activePage: { id: number; menuName: string } | null;
  buttonRows: Array<{ id: number; menuName: string; menuCode?: string; description?: string; checked: boolean }>;
  keyword: string;
  loading: boolean;
}>();
</script>

<template>
  <section class="role-auth-panel">
    <div class="role-auth-panel__header">
      <div>
        <h3>当前页面按钮权限</h3>
        <p v-if="activePage">当前页：{{ activePage.menuName }}</p>
        <p v-else>请先勾选页面，或从已选页面列表选择一个页面</p>
      </div>
    </div>
    <div v-if="!activePage" class="role-auth-panel__empty">
      请先勾选页面，或从已选页面列表选择一个页面
    </div>
    <div v-else-if="buttonRows.length === 0" class="role-auth-panel__empty">
      当前页面暂无独立按钮权限
    </div>
    <slot v-else />
  </section>
</template>
```

- [ ] **Step 4: Re-run the panel test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RoleAuthPanels.test.ts
```

Expected: PASS with the two new panel tests green.

- [ ] **Step 5: Commit the panel layer**

Run:

```bash
git add spring-boot-iot-ui/src/components/role spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts
git commit -m "feat: add role auth drawer panels"
```

## Task 3: Wire `RoleView` to the Split Authorization Workspace

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`

- [ ] **Step 1: Write the failing `RoleView` contract tests**

Update `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`:

```ts
it('splits the drawer auth area into page authorization, selected pages and current page buttons', () => {
  const source = readSource();

  expect(source).toContain('步骤 1：页面授权');
  expect(source).toContain('步骤 2：已选页面');
  expect(source).toContain('当前页面按钮权限');
  expect(source).not.toContain('h3>菜单与按钮授权');
});

it('keeps page ids and button ids in separate local state before recomposing menuIds on submit', () => {
  const source = readSource();

  expect(source).toContain('selectedPageIds');
  expect(source).toContain('selectedButtonIdsByPage');
  expect(source).toContain('composeRoleGrantedMenuIds');
  expect(source).not.toContain('function collectCheckedMenuIds()');
});
```

- [ ] **Step 2: Run the `RoleView` test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RoleView.test.ts
```

Expected: FAIL because the current source still contains the old `菜单与按钮授权` section and still uses the mixed tree submission path.

- [ ] **Step 3: Refactor `RoleView.vue` to use page/button split state**

In `spring-boot-iot-ui/src/views/RoleView.vue`, replace the mixed-tree state with separate page/button state:

```ts
const selectedPageIds = ref<number[]>([]);
const selectedButtonIdsByPage = ref<Record<number, number[]>>({});
const activePageId = ref<number | null>(null);

const pageTreeData = computed(() => buildRolePageTree(rawMenuTree.value));
const selectedPages = computed(() =>
  resolveRoleSelectedPageIds(rawMenuTree.value, composeRoleGrantedMenuIds(rawMenuTree.value, selectedPageIds.value, selectedButtonIdsByPage.value))
);
const currentPageButtons = computed(() => {
  if (!activePageId.value) {
    return [];
  }
  return (pageButtonMap.value[activePageId.value] || []).filter((item) =>
    [item.menuName, item.menuCode].filter(Boolean).some((text) =>
      String(text).toLowerCase().includes(buttonKeyword.value.trim().toLowerCase())
    )
  );
});

function applyGrantedMenuIds(menuIds: number[]) {
  selectedPageIds.value = resolveRoleSelectedPageIds(rawMenuTree.value, menuIds);
  selectedButtonIdsByPage.value = resolveRoleSelectedButtonIdsByPage(rawMenuTree.value, menuIds);
  activePageId.value = selectedPageIds.value[0] ?? null;
}

function buildSubmitMenuIds() {
  return composeRoleGrantedMenuIds(
    rawMenuTree.value,
    selectedPageIds.value,
    selectedButtonIdsByPage.value
  );
}
```

Replace the old right-side tree block with the three panels:

```vue
<RoleAuthPageTreePanel>
  <!-- page search + page-only el-tree -->
</RoleAuthPageTreePanel>

<div class="role-auth-workspace">
  <RoleAuthSelectedPagesPanel
    :items="selectedPageCards"
    @select="handleSelectAuthorizedPage"
  />
  <RoleAuthButtonPanel
    :active-page="activeButtonPage"
    :button-rows="currentPageButtonRows"
    :keyword="buttonKeyword"
    :loading="menuTreeLoading"
  >
    <!-- button search + per-page checkboxes -->
  </RoleAuthButtonPanel>
</div>
```

Update the left summary to numeric counters instead of a tag cloud:

```vue
<div class="role-auth-summary role-auth-summary--compact">
  <span class="role-auth-summary__metric">已选页面 {{ selectedPageIds.length }}</span>
  <span class="role-auth-summary__metric">已配置按钮页面 {{ configuredButtonPageCount }}</span>
  <span class="role-auth-summary__metric">当前页已选按钮 {{ activePageCheckedCount }}</span>
</div>
```

Finally, submit the merged ids instead of the old mixed tree collection:

```ts
const payload = {
  id: formData.value.id,
  roleName: formData.value.roleName,
  roleCode: formData.value.roleCode,
  description: formData.value.description,
  dataScopeType: formData.value.dataScopeType,
  status: formData.value.status,
  menuIds: buildSubmitMenuIds()
};
```

- [ ] **Step 4: Re-run the targeted drawer tests and verify they pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts
```

Expected: PASS with utility, panel, and `RoleView` contract tests green.

- [ ] **Step 5: Commit the `RoleView` refactor**

Run:

```bash
git add spring-boot-iot-ui/src/views/RoleView.vue spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts
git commit -m "feat: split role auth drawer by page and button"
```

## Task 4: Update Docs and Run Front-End Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the behavior docs**

Apply the following Markdown snippets in the listed files.

In `docs/02-业务功能与流程说明.md`, add the role-permission page behavior update:

```md
- `角色权限` 抽屉当前已按“页面授权 -> 已选页面 -> 当前页面按钮权限”收口：页面树只展示目录与页面，按钮权限改为从已选页面中逐页精修，仍沿用原有 `menuIds` 与按钮权限码真相。
```

In `docs/06-前端开发与CSS规范.md`, add the UI rule:

```md
- `RoleView` 权限抽屉当前固定采用“页面树 + 已选页面列表 + 当前页面按钮区”的三段式结构；按钮节点不得再回流到授权树展示中，按钮精修必须以下半区当前页面面板承接。
```

In `docs/15-前端优化与治理计划.md`, add the prevention rule:

```md
- 若治理类表单抽屉同时涉及“访问范围”和“细粒度操作权限”，优先先分离主层级与细粒度层级，不要再用同一棵树同时堆页面和按钮；`角色权限` 已将该类回归收口为页面树 + 按页按钮精修基线。
```

In `docs/08-变更记录与技术债清单.md`, add the change log entry:

```md
- 2026-04-12：`/role` 抽屉已完成角色权限分层重构。前端当前把混合菜单/按钮树拆为“页面授权 / 已选页面 / 当前页面按钮权限”三段式工作区，树中不再展示按钮节点；按钮权限继续保留在原 `menuIds` 提交语义中，并按当前页面单独精修。定向验证：`npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts`、`npm --prefix spring-boot-iot-ui run build`、`npm --prefix spring-boot-iot-ui run component:guard`、`npm --prefix spring-boot-iot-ui run list:guard`、`npm --prefix spring-boot-iot-ui run style:guard`。
```

- [ ] **Step 2: Run the verification commands**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts
npm --prefix spring-boot-iot-ui run build
npm --prefix spring-boot-iot-ui run component:guard
npm --prefix spring-boot-iot-ui run list:guard
npm --prefix spring-boot-iot-ui run style:guard
git diff --check
```

Expected:

- targeted Vitest suite: PASS
- `vite build`: PASS
- `component:guard`: PASS
- `list:guard`: PASS
- `style:guard`: PASS
- `git diff --check`: no output

- [ ] **Step 3: Commit the docs and verification pass**

Run:

```bash
git add docs/02-业务功能与流程说明.md docs/06-前端开发与CSS规范.md docs/08-变更记录与技术债清单.md docs/15-前端优化与治理计划.md
git commit -m "docs: record role auth drawer restructure"
```

## Self-Review

### Spec coverage

- Page tree no longer mixes buttons: Task 1 + Task 3
- Selected-page list as button refinement entry: Task 2 + Task 3
- Current-page button panel with empty states and page-local bulk actions: Task 2 + Task 3
- Existing `menuIds` contract preserved: Task 1 + Task 3
- Docs updated in-place: Task 4

### Placeholder scan

- No unresolved placeholders or deferred-implementation markers remain.
- Every code-changing step includes the code shape to add.
- Every verification step includes exact commands and expected outcomes.

### Type consistency

- Utility helper names are consistent across tests and integration:
  - `buildRolePageTree`
  - `resolveRoleSelectedPageIds`
  - `resolveRoleSelectedButtonIdsByPage`
  - `composeRoleGrantedMenuIds`
- `RoleView` state names are consistent across the plan:
  - `selectedPageIds`
  - `selectedButtonIdsByPage`
  - `activePageId`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-04-12-role-auth-drawer-restructure-implementation-plan.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
