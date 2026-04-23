import { describe, expect, it } from 'vitest';

import type { MenuTreeNode } from '@/types/auth';
import {
  buildMenuNodeMap,
  buildMenuSelectionStateMap,
  filterPermissionTreeByKeyword,
  resolveGrantedMenuIds,
  resolveNodeDetailItems,
  toggleMenuGrant
} from '@/utils/menuAuth';

const menuTree: MenuTreeNode[] = [
  {
    id: 1,
    menuName: '平台治理',
    menuCode: 'system-governance',
    type: 0,
    children: [
      {
        id: 2,
        parentId: 1,
        menuName: '角色权限',
        menuCode: 'system:role',
        path: '/role',
        type: 1,
        children: [
          {
            id: 3,
            parentId: 2,
            menuName: '新增角色',
            menuCode: 'system:role:add',
            type: 2,
            children: []
          }
        ]
      },
      {
        id: 4,
        parentId: 1,
        menuName: '导航编排',
        menuCode: 'system:menu',
        path: '/menu',
        type: 1,
        children: [
          {
            id: 5,
            parentId: 4,
            menuName: '刷新菜单',
            menuCode: 'system:menu:refresh',
            type: 2,
            children: []
          }
        ]
      }
    ]
  }
];

const stringIdMenuTree = [
  {
    id: '1922974195260456961',
    menuName: '平台治理',
    menuCode: 'system-governance',
    type: 0,
    children: [
      {
        id: '1922974195260456962',
        parentId: '1922974195260456961',
        menuName: '角色权限',
        menuCode: 'system:role',
        path: '/role',
        type: 1,
        children: [
          {
            id: '1922974195260456963',
            parentId: '1922974195260456962',
            menuName: '新增角色',
            menuCode: 'system:role:add',
            type: 2,
            children: []
          }
        ]
      }
    ]
  }
] as MenuTreeNode[];

describe('menuAuth utils', () => {
  it('builds a flattened node map from tree data', () => {
    const nodeMap = buildMenuNodeMap(menuTree);
    expect(nodeMap.get(2)?.menuName).toBe('角色权限');
    expect(nodeMap.get(5)?.menuCode).toBe('system:menu:refresh');
  });

  it('filters invalid granted ids and keeps tree order', () => {
    expect(resolveGrantedMenuIds(menuTree, [4, undefined, 1, 999, 4, 5])).toEqual([1, 4, 5]);
  });

  it('normalizes granted ids from leaf menus by auto-including ancestors', () => {
    expect(resolveGrantedMenuIds(menuTree, [5])).toEqual([1, 4, 5]);
  });

  it('keeps granted menu ids when real-environment menu ids are serialized as strings', () => {
    expect(resolveGrantedMenuIds(stringIdMenuTree, ['1922974195260456963'])).toEqual([
      '1922974195260456961',
      '1922974195260456962',
      '1922974195260456963'
    ]);

    const stateMap = buildMenuSelectionStateMap(stringIdMenuTree, ['1922974195260456962']);
    expect(stateMap.get('1922974195260456962')).toMatchObject({
      selfSelected: true,
      indeterminate: true
    });
  });

  it('marks a selected page without all buttons as half-selected', () => {
    const stateMap = buildMenuSelectionStateMap(menuTree, [1, 2]);

    expect(stateMap.get(1)).toMatchObject({
      checked: false,
      indeterminate: true,
      selfSelected: true
    });
    expect(stateMap.get(2)).toMatchObject({
      checked: false,
      indeterminate: true,
      selfSelected: true
    });
    expect(stateMap.get(3)).toMatchObject({
      checked: false,
      indeterminate: false,
      selfSelected: false
    });
  });

  it('checks a parent with descendants and keeps it half-selected after deselecting a child', () => {
    const granted = toggleMenuGrant(menuTree, [], 1, true);
    const nextGranted = toggleMenuGrant(menuTree, granted, 5, false);
    const stateMap = buildMenuSelectionStateMap(menuTree, nextGranted);

    expect(granted).toEqual([1, 2, 3, 4, 5]);
    expect(nextGranted).toEqual([1, 2, 3, 4]);
    expect(stateMap.get(1)).toMatchObject({
      checked: false,
      indeterminate: true,
      selfSelected: true
    });
  });

  it('allows selecting a child from an unselected parent by auto-including ancestors', () => {
    expect(toggleMenuGrant(menuTree, [], 5, true)).toEqual([1, 4, 5]);
  });

  it('filters the display tree without mutating granted ids', () => {
    const granted = resolveGrantedMenuIds(menuTree, [1, 4, 5]);
    const filteredTree = filterPermissionTreeByKeyword(menuTree, '角色');

    expect(filteredTree).toHaveLength(1);
    expect(filteredTree[0].id).toBe(1);
    expect(filteredTree[0].children.map((item) => item.id)).toEqual([2]);
    expect(granted).toEqual([1, 4, 5]);
  });

  it('returns current node direct children for the flat detail panel', () => {
    expect(resolveNodeDetailItems(menuTree, 1).map((item) => item.id)).toEqual([2, 4]);
    expect(resolveNodeDetailItems(menuTree, 2).map((item) => item.id)).toEqual([3]);
  });
});
