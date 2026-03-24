import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ShellSidebarNav from '@/components/ShellSidebarNav.vue';
import type { ShellSidebarNavProps } from '@/types/shell';

const ElTooltipStub = defineComponent({
  name: 'ElTooltip',
  props: {
    content: {
      type: String,
      default: ''
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  template: '<div class="tooltip-stub" :data-content="content" :data-disabled="String(disabled)"><slot /></div>'
});

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  props: {
    to: {
      type: [String, Object],
      required: true
    }
  },
  template: '<a class="router-link-stub" :data-to="typeof to === \'string\' ? to : to.path" v-bind="$attrs"><slot /></a>'
});

describe('ShellSidebarNav', () => {
  const baseProps: ShellSidebarNavProps = {
    group: {
      key: 'risk-ops',
      label: 'Risk Ops',
      description: 'Operations workspace',
      menuTitle: 'Risk menu',
      menuHint: 'Risk navigation',
      items: [
        {
          to: '/risk-monitoring',
          label: 'Monitoring',
          caption: 'Live monitoring',
          short: 'M'
        },
        {
          to: '/alarm-center',
          label: 'Alarm Center',
          caption: 'Alarm operations',
          short: 'A'
        }
      ]
    },
    currentRoutePath: '/risk-monitoring',
    sidebarCollapsed: false,
    isMobile: false,
    mobileMenuOpen: false
  };

  function mountSidebar(props: Partial<ShellSidebarNavProps> = {}) {
    return mount(ShellSidebarNav, {
      props: {
        ...baseProps,
        ...props
      },
      global: {
        stubs: {
          ElTooltip: ElTooltipStub,
          RouterLink: RouterLinkStub
        }
      }
    });
  }

  it('marks the current route as active', () => {
    const wrapper = mountSidebar();
    const items = wrapper.findAll('.shell-sidebar-nav__item');

    expect(items).toHaveLength(2);
    expect(items[0].classes()).toContain('shell-sidebar-nav__item--active');
    expect(items[1].classes()).not.toContain('shell-sidebar-nav__item--active');
    expect(wrapper.findAll('.tooltip-stub')).toHaveLength(0);
  });

  it('shows collapsed markers and enables tooltip text when the sidebar is collapsed', () => {
    const wrapper = mountSidebar({
      sidebarCollapsed: true
    });

    expect(wrapper.find('.shell-sidebar-nav').classes()).toContain('shell-sidebar-nav--collapsed');
    expect(wrapper.findAll('.tooltip-stub')[0].attributes('data-disabled')).toBe('false');
    expect(wrapper.findAll('.shell-sidebar-nav__marker')[0].text()).toBe('M');
  });

  it('sets aria-hidden when the mobile drawer is closed', () => {
    const wrapper = mountSidebar({
      isMobile: true,
      mobileMenuOpen: false
    });

    expect(wrapper.find('.shell-sidebar-nav').attributes('aria-hidden')).toBe('true');
    expect(wrapper.find('.shell-sidebar-nav').attributes('inert')).toBe('');
    expect(wrapper.findAll('.tooltip-stub')).toHaveLength(0);
  });
});
