import { defineComponent } from 'vue';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ShellWorkspaceTabs from '@/components/ShellWorkspaceTabs.vue';
import type { ShellWorkspaceTabsProps } from '@/types/shell';

const ElTooltipStub = defineComponent({
  name: 'ElTooltip',
  props: {
    content: {
      type: String,
      default: ''
    }
  },
  template: '<div class="tooltip-stub" :data-content="content"><slot /></div>'
});

describe('ShellWorkspaceTabs', () => {
  const baseProps: ShellWorkspaceTabsProps = {
    activeGroupKey: 'iot-access',
    groups: [
      {
        key: 'iot-access',
        label: 'Access',
        description: 'Device onboarding and diagnostics',
        menuTitle: 'Access',
        menuHint: 'Access menu',
        items: []
      },
      {
        key: 'risk-ops',
        label: 'Risk Ops',
        description: 'Monitoring and disposal',
        menuTitle: 'Risk Ops',
        menuHint: 'Risk menu',
        items: []
      }
    ]
  };

  it('renders the active group description and active tab state', () => {
    const wrapper = mount(ShellWorkspaceTabs, {
      props: baseProps,
      global: {
        stubs: {
          ElTooltip: ElTooltipStub
        }
      }
    });

    expect(wrapper.find('.shell-workspace-tabs__label small').text()).toBe('Device onboarding and diagnostics');

    const items = wrapper.findAll('.shell-workspace-tabs__item');
    expect(items).toHaveLength(2);
    expect(items[0].classes()).toContain('shell-workspace-tabs__item--active');
    expect(items[1].classes()).not.toContain('shell-workspace-tabs__item--active');
  });

  it('emits switch-group with the selected group key', async () => {
    const wrapper = mount(ShellWorkspaceTabs, {
      props: baseProps,
      global: {
        stubs: {
          ElTooltip: ElTooltipStub
        }
      }
    });

    await wrapper.findAll('.shell-workspace-tabs__item')[1].trigger('click');

    expect(wrapper.emitted('switch-group')?.[0]).toEqual(['risk-ops']);
  });
});
