import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ShellCommandPalette from '@/components/ShellCommandPalette.vue';
import type { ShellCommandPaletteProps } from '@/types/shell';

describe('ShellCommandPalette', () => {
  const recentItems: ShellCommandPaletteProps['recentItems'] = [
    {
      path: '/products',
      title: 'Products',
      description: 'Recently visited product list',
      workspaceLabel: 'Access',
      short: 'P'
    }
  ];

  const commandGroups: ShellCommandPaletteProps['groups'] = [
    {
      key: 'risk-ops',
      label: 'Risk Ops',
      items: [
        {
          path: '/risk-monitoring',
          title: 'Monitoring',
          description: 'Live monitoring desk',
          workspaceLabel: 'Risk Ops',
          short: 'M'
        }
      ]
    }
  ];

  function mountPalette(props: Partial<ShellCommandPaletteProps> = {}) {
    return mount(ShellCommandPalette, {
      props: {
        modelValue: true,
        query: '',
        groups: commandGroups,
        recentItems,
        ...props
      }
    });
  }

  it('emits close events from mask and close button', async () => {
    const wrapper = mountPalette();

    await wrapper.find('.command-palette__mask').trigger('click');
    await wrapper.find('.command-palette__close').trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[false], [false]]);
  });

  it('emits query updates and closes after selecting a recent item', async () => {
    const wrapper = mountPalette();

    await wrapper.find('input').setValue('prod');
    await wrapper.find('.command-palette__section .command-palette__item').trigger('click');

    expect(wrapper.emitted('update:query')?.[0]).toEqual(['prod']);
    expect(wrapper.emitted('select')?.[0]).toEqual(['/products']);
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false]);
  });

  it('renders group results, shows empty state, and clears query when the palette is closed externally', async () => {
    const groupWrapper = mountPalette({
      query: 'risk',
      recentItems: []
    });

    expect(groupWrapper.text()).toContain('Monitoring');
    await groupWrapper.find('.command-palette__item').trigger('click');
    expect(groupWrapper.emitted('select')?.[0]).toEqual(['/risk-monitoring']);

    const emptyWrapper = mountPalette({
      query: 'missing',
      groups: [],
      recentItems: []
    });

    expect(emptyWrapper.find('.command-palette__empty').exists()).toBe(true);

    await emptyWrapper.setProps({
      modelValue: false
    });

    expect(emptyWrapper.emitted('update:query')?.[0]).toEqual(['']);
  });
});
