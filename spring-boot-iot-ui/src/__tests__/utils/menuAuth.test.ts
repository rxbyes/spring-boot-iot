import { describe, expect, it } from 'vitest';

import type { MenuTreeNode } from '@/types/auth';
import {
  buildMenuNodeMap,
  buildRolePageTree,
  composeRoleGrantedMenuIds,
  resolveRoleCheckedMenuIds,
  resolveRoleMenuSummary,
  resolveRoleSelectedButtonIdsByPage,
  resolveRoleSelectedPageIds
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

describe('menuAuth utils', () => {
  it('builds a flattened node map from tree data', () => {
    const nodeMap = buildMenuNodeMap(menuTree);
    expect(nodeMap.get(2)?.menuName).toBe('角色权限');
    expect(nodeMap.get(3)?.menuCode).toBe('system:role:add');
  });

  it('filters out directory nodes from role checked ids', () => {
    expect(resolveRoleCheckedMenuIds(menuTree, [1, 2, 3, 4])).toEqual([2, 3, 4]);
  });

  it('keeps menu summary in tree order and limits size', () => {
    expect(resolveRoleMenuSummary(menuTree, [1, 2, 3, 4], 2)).toEqual(['角色权限', '新增角色']);
  });

  it('builds a page-only tree and groups granted buttons by parent page', () => {
    const pageTree = buildRolePageTree(menuTree);

    expect(pageTree[0].children.map((item) => item.id)).toEqual([2, 4]);
    expect(pageTree[0].children[0].children).toEqual([]);
    expect(pageTree[0].children[1].children).toEqual([]);
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
});
