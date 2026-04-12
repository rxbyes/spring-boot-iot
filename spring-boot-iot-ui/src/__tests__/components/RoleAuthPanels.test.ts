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
              {
                id: 2,
                parentId: 1,
                menuName: '角色权限',
                path: '/role',
                type: 1,
                children: []
              }
            ]
          }
        ],
        currentNodeId: 1,
        expandedKeys: [1],
        selectionStateMap: new Map([
          [1, { checked: false, indeterminate: true, selfSelected: true, selectedChildCount: 0, totalChildCount: 1 }],
          [2, { checked: false, indeterminate: false, selfSelected: false, selectedChildCount: 0, totalChildCount: 0 }]
        ]),
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
        currentNode: { id: 2, menuName: '角色权限', type: 1, path: '/role', children: [] },
        currentNodeState: {
          checked: false,
          indeterminate: true,
          selfSelected: true,
          selectedChildCount: 1,
          totalChildCount: 1
        },
        items: [
          {
            id: 3,
            menuName: '新增角色',
            menuCode: 'system:role:add',
            type: 2,
            checked: true,
            indeterminate: false,
            selfSelected: true,
            childCount: 0
          }
        ],
        keyword: '',
        loading: false
      }
    });

    expect(wrapper.text()).toContain('新增角色');
    expect(wrapper.text()).toContain('system:role:add');
    expect(wrapper.text()).not.toContain('步骤 2：已选页面');
  });
});
