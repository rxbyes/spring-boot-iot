import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import HeaderPopoverPanel from '@/components/HeaderPopoverPanel.vue';

describe('HeaderPopoverPanel', () => {
  const items = [
    { id: '1', title: '消息 1', description: '描述 1', path: '/one' },
    { id: '2', title: '消息 2', description: '描述 2', path: '/two' }
  ];

  it('renders title and list items', () => {
    const wrapper = mount(HeaderPopoverPanel, {
      props: {
        panelId: 'panel-id',
        ariaLabel: '测试面板',
        title: '消息通知',
        subtitle: '最近动态',
        items
      }
    });

    expect(wrapper.text()).toContain('消息通知');
    expect(wrapper.findAll('.header-popover__list li')).toHaveLength(2);
  });

  it('emits select with target path when item is clicked', async () => {
    const wrapper = mount(HeaderPopoverPanel, {
      props: {
        panelId: 'panel-id',
        ariaLabel: '测试面板',
        title: '消息通知',
        subtitle: '最近动态',
        items
      }
    });

    await wrapper.findAll('.header-popover__list li button')[1].trigger('click');
    expect(wrapper.emitted('select')?.[0]).toEqual(['/two']);
  });
});
