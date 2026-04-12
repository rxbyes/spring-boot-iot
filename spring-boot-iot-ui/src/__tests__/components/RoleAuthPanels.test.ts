import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RoleAuthButtonPanel from '@/components/role/RoleAuthButtonPanel.vue';
import RoleAuthPageTreePanel from '@/components/role/RoleAuthPageTreePanel.vue';
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

  it('keeps the page tree focused on page names instead of rendering route text inside tree rows', () => {
    const wrapper = mount(RoleAuthPageTreePanel, {
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
        checkedPageIds: [],
        keyword: '',
        loading: false
      }
    });

    expect(wrapper.text()).toContain('角色权限');
    expect(wrapper.text()).toContain('目录节点自动补齐');
    expect(wrapper.text()).not.toContain('/role');
  });
});
