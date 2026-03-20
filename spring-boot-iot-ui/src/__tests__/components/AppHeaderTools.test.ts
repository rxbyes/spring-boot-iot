import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';

import AppHeaderTools from '@/components/AppHeaderTools.vue';
import type { ShellHeaderToolsProps } from '@/types/shell';

describe('AppHeaderTools', () => {
  const baseProps: ShellHeaderToolsProps = {
    showNoticePanel: false,
    showHelpPanel: false,
    noticePanelId: 'notice-panel',
    helpPanelId: 'help-panel',
    headerIdentity: '系统管理员 · 超级管理员',
    headerAccountName: 'rxbyes',
    headerRoleName: '超级管理员',
    headerAccountCode: 'rxbyes',
    headerAccountType: '主账号',
    headerAuthStatus: '实名认证待接入',
    headerPrimaryContact: '手机号：138****1234',
    headerLoginMethods: '账号登录 / 手机号登录',
    accountInitial: 'R',
    unreadNoticeCount: 0
  };

  it('renders account and role info', () => {
    const wrapper = mount(AppHeaderTools, { props: baseProps });
    expect(wrapper.text()).toContain('rxbyes');
    expect(wrapper.find('.account-chip').attributes('title')).toContain('系统管理员');
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

  it('opens account panel and emits account actions', async () => {
    const wrapper = mount(AppHeaderTools, { props: baseProps });

    await wrapper.find('.account-entry').trigger('mouseenter');
    expect(wrapper.text()).toContain('账号中心');
    expect(wrapper.text()).toContain('登录方式');
    expect(wrapper.text()).toContain('主账号');
    expect(wrapper.text()).toContain('手机号：138****1234');

    await wrapper.find('[data-action="change-password"]').trigger('click');
    await wrapper.find('.account-entry').trigger('mouseenter');
    await wrapper.find('[data-action="logout"]').trigger('click');

    expect(wrapper.emitted('open-account-menu')).toHaveLength(2);
    expect(wrapper.emitted('open-change-password')).toHaveLength(1);
    expect(wrapper.emitted('logout')).toHaveLength(1);
  });
});
