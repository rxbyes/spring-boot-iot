import { describe, expect, it } from 'vitest';

import type { MenuTreeNode } from '@/types/auth';
import { buildMenuNodeMap, resolveRoleCheckedMenuIds, resolveRoleMenuSummary } from '@/utils/menuAuth';

const menuTree: MenuTreeNode[] = [
  {
    id: 1,
    menuName: '系统治理',
    menuCode: 'system-governance',
    type: 0,
    children: [
      {
        id: 2,
        parentId: 1,
        menuName: '角色管理',
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
        menuName: '菜单管理',
        menuCode: 'system:menu',
        path: '/menu',
        type: 1,
        children: []
      }
    ]
  }
];

describe('menuAuth utils', () => {
  it('builds a flattened node map from tree data', () => {
    const nodeMap = buildMenuNodeMap(menuTree);
    expect(nodeMap.get(2)?.menuName).toBe('角色管理');
    expect(nodeMap.get(3)?.menuCode).toBe('system:role:add');
  });

  it('filters out directory nodes from role checked ids', () => {
    expect(resolveRoleCheckedMenuIds(menuTree, [1, 2, 3, 4])).toEqual([2, 3, 4]);
  });

  it('keeps menu summary in tree order and limits size', () => {
    expect(resolveRoleMenuSummary(menuTree, [1, 2, 3, 4], 2)).toEqual(['角色管理', '新增角色']);
  });
});
