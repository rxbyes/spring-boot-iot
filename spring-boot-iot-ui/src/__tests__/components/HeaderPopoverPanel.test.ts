import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';

import HeaderPopoverPanel from '@/components/HeaderPopoverPanel.vue';
import type { ShellPopoverContent, ShellPopoverItem } from '@/types/shell';

describe('HeaderPopoverPanel', () => {
  const content: ShellPopoverContent = {
    title: '消息通知',
    subtitle: '最近动态',
    summaryTitle: '站内消息摘要',
    summaryDescription: '按角色和权限聚合的系统提醒。',
    metrics: [
      { id: 'metric-1', label: '最近操作', value: '2', tone: 'neutral' }
    ],
    sections: [
      {
        id: 'section-1',
        title: '系统事件',
        description: '系统类提醒',
        items: [
          { id: '1', title: '消息 1', description: '描述 1', path: '/one', badge: '系统事件', tone: 'brand' },
          { id: '2', title: '消息 2', description: '描述 2', path: '/two', badge: '业务事件', tone: 'accent' }
        ]
      },
      {
        id: 'section-2',
        title: '常见问题',
        description: 'FAQ',
        items: [
          { id: '3', title: '问题 1', description: '回答 1', badge: 'FAQ', tone: 'warning' }
        ]
      }
    ],
    footerActions: [
      { id: 'view-more', label: '查看更多', tone: 'brand' }
    ]
  };

  it('renders title and list items', () => {
    const wrapper = mount(HeaderPopoverPanel, {
      props: {
        panelId: 'panel-id',
        ariaLabel: '测试面板',
        content
      }
    });

    expect(wrapper.text()).toContain('消息通知');
    expect(wrapper.text()).toContain('站内消息摘要');
    expect(wrapper.findAll('.header-popover__section')).toHaveLength(2);
    expect(wrapper.findAll('.header-popover__list li')).toHaveLength(3);
  });

  it('emits select with item payload when item is clicked', async () => {
    const wrapper = mount(HeaderPopoverPanel, {
      props: {
        panelId: 'panel-id',
        ariaLabel: '测试面板',
        content
      }
    });

    await wrapper.findAll('.header-popover__list li button')[1].trigger('click');
    expect(wrapper.emitted('select')?.[0]).toEqual([expect.objectContaining<ShellPopoverItem>({ id: '2', path: '/two' })]);
  });

  it('emits footer action when footer button is clicked', async () => {
    const wrapper = mount(HeaderPopoverPanel, {
      props: {
        panelId: 'panel-id',
        ariaLabel: '测试面板',
        content
      }
    });

    await wrapper.find('.header-popover__footer-action').trigger('click');
    expect(wrapper.emitted('action')?.[0]).toEqual(['view-more']);
  });
});
