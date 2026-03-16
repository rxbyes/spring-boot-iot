import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import AppHeaderTools from '@/components/AppHeaderTools.vue';

describe('AppHeaderTools', () => {
  const baseProps = {
    showNoticePanel: false,
    showHelpPanel: false,
    noticePanelId: 'notice-panel',
    helpPanelId: 'help-panel',
    headerIdentity: '系统管理员 · 超级管理员',
    headerAccountName: 'rxbyes',
    headerRoleName: '超级管理员',
    accountInitial: 'R',
    unreadNoticeCount: 0
  };

  it('renders account and role info', () => {
    const wrapper = mount(AppHeaderTools, { props: baseProps });
    expect(wrapper.text()).toContain('rxbyes');
    expect(wrapper.text()).toContain('超级管理员');
  });

  it('shows unread badge when unreadNoticeCount > 0', () => {
    const wrapper = mount(AppHeaderTools, {
      props: {
        ...baseProps,
        unreadNoticeCount: 3
      }
    });
    expect(wrapper.find('.tool-text__badge').exists()).toBe(true);
    expect(wrapper.find('.tool-text__badge').text()).toBe('3');
  });

  it('emits toggle events on button click', async () => {
    const wrapper = mount(AppHeaderTools, { props: baseProps });
    const buttons = wrapper.findAll('button.tool-text');
    await buttons[0].trigger('click');
    await buttons[1].trigger('click');

    expect(wrapper.emitted('toggle-notice')).toHaveLength(1);
    expect(wrapper.emitted('toggle-help')).toHaveLength(1);
  });
});
