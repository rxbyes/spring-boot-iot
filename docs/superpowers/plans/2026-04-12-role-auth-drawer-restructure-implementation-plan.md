# Role Auth Drawer Full-Permission Tree Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild `/role` authorization so the drawer uses a single full-permission tree for directory/page/button assignment plus a flat current-node detail panel, while keeping the existing backend `menuIds` contract.

**Architecture:** Replace the old page-only tree split with a single selected-node set that contains all permission types, derive custom checked/half-checked states from tree relations, and render tree rows with custom checkboxes instead of the old built-in page/button split. Keep the role form, role APIs, and backend persistence contract unchanged; only refactor the front-end state model, components, tests, and in-place docs.

**Tech Stack:** Vue 3, TypeScript, Element Plus, Vitest, existing shared `Standard*` and `PanelCard` components.

---

## File Structure

- `spring-boot-iot-ui/src/utils/menuAuth.ts`
  - Own tree traversal, ancestor/descendant helpers, checked-state derivation, display filtering, and child-detail derivation for the new permission model.
- `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts`
  - Lock the new all-level permission behavior before touching the view.
- `spring-boot-iot-ui/src/components/role/RoleAuthPermissionTreePanel.vue`
  - Render the full permission tree with custom checkbox state and lightweight tree rows.
- `spring-boot-iot-ui/src/components/role/RoleAuthNodeDetailPanel.vue`
  - Render the flat current-node child list or button info panel.
- `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts`
  - Verify the new tree/detail panels render all-level authorization states.
- `spring-boot-iot-ui/src/views/RoleView.vue`
  - Replace the old three-panel page/button workspace with summary + tree + current-node detail.
- `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`
  - Keep the governance contract aligned with the new structure and selected-id model.
- `docs/02-业务功能与流程说明.md`
  - Update the role page behavior description to the new all-level authorization structure.
- `docs/06-前端开发与CSS规范.md`
  - Replace the old three-panel rule with the new full-permission-tree rule.
- `docs/08-变更记录与技术债清单.md`
  - Record the redesign and verification evidence.
- `docs/15-前端优化与治理计划.md`
  - Add a prevention rule so tree rows stay lightweight and node detail stays flat.

## Task 1: Lock Full-Permission Utility Semantics

**Files:**
- Modify: `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts`
- Modify: `spring-boot-iot-ui/src/utils/menuAuth.ts`

- [ ] **Step 1: Write the failing utility tests**

Update `spring-boot-iot-ui/src/__tests__/utils/menuAuth.test.ts` so it covers the new all-level helpers. Replace the old page-only expectations with tests like:

```ts
import {
  buildMenuNodeMap,
  buildMenuSelectionStateMap,
  filterPermissionTreeByKeyword,
  resolveGrantedMenuIds,
  resolveNodeDetailItems,
  toggleMenuGrant
} from '@/utils/menuAuth';

it('marks a selected page without all buttons as half-checked', () => {
  const granted = resolveGrantedMenuIds(menuTree, [1, 2]);
  const stateMap = buildMenuSelectionStateMap(menuTree, granted);

  expect(stateMap.get(1)).toMatchObject({ checked: false, indeterminate: true, selfSelected: true });
  expect(stateMap.get(2)).toMatchObject({ checked: false, indeterminate: true, selfSelected: true });
});

it('checking a directory adds descendants and keeping a sibling unchecked leaves the parent half-checked', () => {
  const granted = toggleMenuGrant(menuTree, [], 1, true);
  const nextGranted = toggleMenuGrant(menuTree, granted, 5, false);
  const stateMap = buildMenuSelectionStateMap(menuTree, nextGranted);

  expect(nextGranted).toEqual([1, 2, 3, 4]);
  expect(stateMap.get(1)).toMatchObject({ checked: false, indeterminate: true });
});

it('allows selecting a child from an unselected parent by auto-including ancestors', () => {
  expect(toggleMenuGrant(menuTree, [], 5, true)).toEqual([1, 4, 5]);
});

it('filters the display tree without mutating granted ids', () => {
  const granted = resolveGrantedMenuIds(menuTree, [1, 4, 5]);

  expect(filterPermissionTreeByKeyword(menuTree, '角色').map((item) => item.id)).toEqual([1]);
  expect(granted).toEqual([1, 4, 5]);
});

it('returns the current node direct children for the detail panel', () => {
  expect(resolveNodeDetailItems(menuTree, 1).map((item) => item.id)).toEqual([2, 4]);
  expect(resolveNodeDetailItems(menuTree, 2).map((item) => item.id)).toEqual([3]);
});
```

- [ ] **Step 2: Run the utility test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts
```

Expected: FAIL with missing-export or assertion failures because the old utility layer only knows the page/button split model.

- [ ] **Step 3: Implement the new tree helpers**

In `spring-boot-iot-ui/src/utils/menuAuth.ts`, add the all-level helpers and remove page-split-only logic. The implementation should include:

```ts
export function resolveGrantedMenuIds(
  nodes: MenuTreeNode[],
  grantedIds: Array<number | undefined | null>
): number[] {
  const nodeMap = buildMenuNodeMap(nodes);
  const seen = new Set<number>();
  return grantedIds.filter((menuId): menuId is number => {
    if (typeof menuId !== 'number' || seen.has(menuId) || !nodeMap.has(menuId)) {
      return false;
    }
    seen.add(menuId);
    return true;
  });
}

export function toggleMenuGrant(
  nodes: MenuTreeNode[],
  grantedIds: number[],
  targetId: number,
  checked: boolean
): number[] {
  const nodeMap = buildMenuNodeMap(nodes);
  const relationMap = buildMenuRelationMap(nodes);
  const nextGranted = new Set(resolveGrantedMenuIds(nodes, grantedIds));
  const descendants = relationMap.get(targetId)?.descendantIds ?? [];
  const ancestors = relationMap.get(targetId)?.ancestorIds ?? [];

  if (checked) {
    [targetId, ...descendants, ...ancestors].forEach((id) => nextGranted.add(id));
  } else {
    [targetId, ...descendants].forEach((id) => nextGranted.delete(id));
  }

  return Array.from(nextGranted);
}

export function buildMenuSelectionStateMap(nodes: MenuTreeNode[], grantedIds: number[]) {
  const grantedSet = new Set(resolveGrantedMenuIds(nodes, grantedIds));
  const relationMap = buildMenuRelationMap(nodes);
  const stateMap = new Map<number, { checked: boolean; indeterminate: boolean; selfSelected: boolean }>();

  relationMap.forEach((relation, nodeId) => {
    const subtreeIds = [nodeId, ...relation.descendantIds];
    const selectedCount = subtreeIds.filter((id) => grantedSet.has(id)).length;
    const total = subtreeIds.length;
    stateMap.set(nodeId, {
      checked: total > 0 && selectedCount === total,
      indeterminate: selectedCount > 0 && selectedCount < total,
      selfSelected: grantedSet.has(nodeId)
    });
  });

  return stateMap;
}
```

Also add helpers to:

- derive parent/child relations
- filter the display tree by keyword while preserving matching ancestors
- derive current-node direct children for the right panel

- [ ] **Step 4: Re-run the utility test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts
```

Expected: PASS with all new utility tests green.

## Task 2: Build the New Permission Tree and Detail Panels

**Files:**
- Create: `spring-boot-iot-ui/src/components/role/RoleAuthPermissionTreePanel.vue`
- Create: `spring-boot-iot-ui/src/components/role/RoleAuthNodeDetailPanel.vue`
- Delete: `spring-boot-iot-ui/src/components/role/RoleAuthPageTreePanel.vue`
- Delete: `spring-boot-iot-ui/src/components/role/RoleAuthSelectedPagesPanel.vue`
- Delete: `spring-boot-iot-ui/src/components/role/RoleAuthButtonPanel.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts`

- [ ] **Step 1: Write the failing panel tests**

Update `spring-boot-iot-ui/src/__tests__/components/RoleAuthPanels.test.ts` to exercise the new components:

```ts
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RoleAuthNodeDetailPanel from '@/components/role/RoleAuthNodeDetailPanel.vue';
import RoleAuthPermissionTreePanel from '@/components/role/RoleAuthPermissionTreePanel.vue';

describe('Role auth panels', () => {
  it('renders tree rows with type tag and child count but without stacked route text', () => {
    const wrapper = mount(RoleAuthPermissionTreePanel, {
      props: {
        treeData: [
          {
            id: 1,
            menuName: '平台治理',
            type: 0,
            children: [
              { id: 2, parentId: 1, menuName: '角色权限', path: '/role', type: 1, children: [] }
            ]
          }
        ],
        currentNodeId: 1,
        expandedKeys: [1],
        selectionState: { 1: { checked: false, indeterminate: true, selfSelected: true }, 2: { checked: false, indeterminate: false, selfSelected: false } },
        keyword: '',
        loading: false
      }
    });

    expect(wrapper.text()).toContain('平台治理');
    expect(wrapper.text()).toContain('目录');
    expect(wrapper.text()).toContain('1 项');
    expect(wrapper.text()).not.toContain('/role');
  });

  it('renders current node direct children as a flat list and shows button metadata on page nodes', () => {
    const wrapper = mount(RoleAuthNodeDetailPanel, {
      props: {
        currentNode: { id: 2, menuName: '角色权限', type: 1, path: '/role' },
        items: [{ id: 3, menuName: '新增角色', menuCode: 'system:role:add', type: 2, checked: true, indeterminate: false, childCount: 0 }],
        keyword: '',
        loading: false
      }
    });

    expect(wrapper.text()).toContain('新增角色');
    expect(wrapper.text()).toContain('system:role:add');
    expect(wrapper.text()).not.toContain('步骤 2：已选页面');
  });
});
```

- [ ] **Step 2: Run the panel test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RoleAuthPanels.test.ts
```

Expected: FAIL because the new components do not exist yet and the old assertions no longer match.

- [ ] **Step 3: Implement the two new panel components**

Create `spring-boot-iot-ui/src/components/role/RoleAuthPermissionTreePanel.vue` with:

- `PanelCard` shell
- search input + refresh action
- `el-tree` with custom checkbox nodes
- node row content containing only name, type tag, and child count
- emits for:
  - `update:keyword`
  - `toggle`
  - `select-node`
  - `expand`
  - `collapse`
  - `refresh`

Create `spring-boot-iot-ui/src/components/role/RoleAuthNodeDetailPanel.vue` with:

- `PanelCard` shell
- current-node header and node-status summary
- child search input
- flat child list when current node has children
- button info card when current node is a button
- emits for:
  - `update:keyword`
  - `toggle`
  - `focus-child`

Delete the old page-split components after `RoleView.vue` no longer imports them.

- [ ] **Step 4: Re-run the panel test and verify it passes**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/components/RoleAuthPanels.test.ts
```

Expected: PASS with the updated panel tests green.

## Task 3: Rewire `RoleView` to the Unified Permission Model

**Files:**
- Modify: `spring-boot-iot-ui/src/views/RoleView.vue`
- Modify: `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts`

- [ ] **Step 1: Write the failing `RoleView` contract tests**

Update `spring-boot-iot-ui/src/__tests__/views/RoleView.test.ts` so it expects the new structure:

```ts
it('renders the role authorization drawer as summary plus full permission tree and current-node detail', () => {
  const source = readSource();

  expect(source).toContain('RoleAuthPermissionTreePanel');
  expect(source).toContain('RoleAuthNodeDetailPanel');
  expect(source).toContain('role-auth-summary-grid');
  expect(source).not.toContain('RoleAuthSelectedPagesPanel');
  expect(source).not.toContain('步骤 1：页面授权');
});

it('keeps all-level granted ids in a single local state and submits menuIds from that state', () => {
  const source = readSource();

  expect(source).toContain('const grantedMenuIds = ref<number[]>([])');
  expect(source).toContain('toggleMenuGrant(');
  expect(source).toContain('resolveGrantedMenuIds(');
  expect(source).toContain('menuIds: [...grantedMenuIds.value]');
});
```

- [ ] **Step 2: Run the `RoleView` test and verify it fails**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/views/RoleView.test.ts
```

Expected: FAIL because `RoleView.vue` still imports and renders the old page/button split workspace.

- [ ] **Step 3: Refactor `RoleView.vue`**

In `spring-boot-iot-ui/src/views/RoleView.vue`:

- replace `selectedPageIds`, `selectedButtonIdsByPage`, `activePageId`, `pageKeyword`, `buttonKeyword`
  with:

```ts
const grantedMenuIds = ref<number[]>([]);
const currentNodeId = ref<number | null>(null);
const treeKeyword = ref('');
const detailKeyword = ref('');
const expandedNodeKeys = ref<number[]>([]);
```

- derive:

```ts
const grantedMenuIdSet = computed(() => new Set(grantedMenuIds.value));
const selectionStateMap = computed(() =>
  buildMenuSelectionStateMap(rawMenuTree.value, grantedMenuIds.value)
);
const displayTreeData = computed(() =>
  filterPermissionTreeByKeyword(rawMenuTree.value, treeKeyword.value)
);
const currentNode = computed(() =>
  currentNodeId.value === null ? null : menuNodeMap.value.get(currentNodeId.value) ?? null
);
const currentNodeDetailItems = computed(() =>
  resolveNodeDetailItems(rawMenuTree.value, currentNodeId.value, detailKeyword.value, selectionStateMap.value)
);
```

- load/edit/reset flows should use:

```ts
grantedMenuIds.value = resolveGrantedMenuIds(rawMenuTree.value, role.menuIds);
currentNodeId.value = grantedMenuIds.value[0] ?? rawMenuTree.value[0]?.id ?? null;
expandedNodeKeys.value = currentNodeId.value ? resolveNodeAncestorIds(rawMenuTree.value, currentNodeId.value) : [];
```

- tree/detail toggle handlers should use the same helper:

```ts
function handleToggleMenu(menuId: number, checked: boolean) {
  grantedMenuIds.value = toggleMenuGrant(rawMenuTree.value, grantedMenuIds.value, menuId, checked);
}
```

- submit should stay on the old backend contract:

```ts
const payload = {
  id: formData.value.id,
  roleName: formData.value.roleName,
  roleCode: formData.value.roleCode,
  description: formData.value.description,
  dataScopeType: formData.value.dataScopeType,
  status: formData.value.status,
  menuIds: [...grantedMenuIds.value]
};
```

- replace the old authorization workspace markup with:
  - a compact summary grid
  - `RoleAuthPermissionTreePanel`
  - `RoleAuthNodeDetailPanel`

- update drawer copy so it describes “统一权限树 + 当前节点详情”, not the old step-by-step page flow

- adjust layout CSS to a flatter two-column auth area and remove the old shared-height three-panel grid

- [ ] **Step 4: Re-run the targeted tests and verify they pass**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts
```

Expected: PASS with utility, panel, and `RoleView` tests green.

## Task 4: Update Docs and Run Front-End Verification

**Files:**
- Modify: `docs/02-业务功能与流程说明.md`
- Modify: `docs/06-前端开发与CSS规范.md`
- Modify: `docs/08-变更记录与技术债清单.md`
- Modify: `docs/15-前端优化与治理计划.md`

- [ ] **Step 1: Update the in-place docs**

Apply the new behavior wording:

In `docs/02-业务功能与流程说明.md`:

```md
- `角色权限` 抽屉当前已收口为“统一权限树 + 当前节点详情”结构：目录、页面、按钮都可直接授权；左侧树只保留节点名称、类型与子级数量，右侧只平铺当前节点直属子级或按钮详情，保存仍沿用既有 `menuIds` 合同。
```

In `docs/06-前端开发与CSS规范.md`:

```md
- `/role` 的授权抽屉必须保持“顶部摘要 + 左侧全权限树 + 右侧当前节点详情”的结构；目录、页面、按钮均可授权，但树节点只保留单行信息，不得再把路由、权限码、说明堆回树行，也不得回流旧的“已选页面 + 当前页面按钮”三段式工作区。
```

In `docs/15-前端优化与治理计划.md`:

```md
- 治理类权限抽屉若同时覆盖目录、页面与按钮授权，优先采用“统一权限树承载层级关系，当前节点详情承载平铺明细”的模式；`/role` 已以该模式替代旧的页面/按钮拆分工作区，后续不得通过默认全展开或树内多行说明来换取所谓“信息完整”。
```

In `docs/08-变更记录与技术债清单.md`, replace the 2026-04-12 role-auth entry with the new final version:

```md
- 2026-04-12：`/role` 的角色授权抽屉已按最终方案重构为“顶部摘要 + 左侧全权限树 + 右侧当前节点详情”。目录、页面、按钮当前都属于可授权节点；勾选父节点会默认覆盖全部后代，管理员仍可继续取消部分子节点，父节点会显示半选态。树行只保留名称、类型和子级数量，不再把路由/权限码/说明堆进树里；按钮颗粒度继续保留，并通过当前页面详情面板平铺维护。定向验证：`npx vitest run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts`、`npm run build`、`npm run component:guard`、`npm run list:guard`、`npm run style:guard`、`node scripts/docs/check-topology.mjs`（工作目录：`spring-boot-iot-ui`）。
```

- [ ] **Step 2: Run verification**

Run:

```bash
npm --prefix spring-boot-iot-ui run test -- --run src/__tests__/utils/menuAuth.test.ts src/__tests__/components/RoleAuthPanels.test.ts src/__tests__/views/RoleView.test.ts
npm --prefix spring-boot-iot-ui run build
npm --prefix spring-boot-iot-ui run component:guard
npm --prefix spring-boot-iot-ui run list:guard
npm --prefix spring-boot-iot-ui run style:guard
node scripts/docs/check-topology.mjs
git diff --check
```

Expected:

- targeted Vitest suite: PASS
- build: PASS
- component guard: PASS
- list guard: PASS
- style guard: PASS
- docs topology check: PASS
- `git diff --check`: no output

## Self-Review

### Spec coverage

- Unified tree across directory/page/button: Task 1 + Task 3
- Current-node flat detail panel: Task 2 + Task 3
- Parent-selects-descendants with partial child deselect: Task 1 + Task 3
- Existing `menuIds` contract preserved: Task 1 + Task 3
- Docs updated in place: Task 4

### Placeholder scan

- No deferred placeholders remain.
- Every verification step includes exact commands.
- Each task references exact file paths.

### Type consistency

- Utility names used consistently:
  - `resolveGrantedMenuIds`
  - `toggleMenuGrant`
  - `buildMenuSelectionStateMap`
  - `filterPermissionTreeByKeyword`
  - `resolveNodeDetailItems`
- `RoleView` state names used consistently:
  - `grantedMenuIds`
  - `currentNodeId`
  - `treeKeyword`
  - `detailKeyword`
  - `expandedNodeKeys`
