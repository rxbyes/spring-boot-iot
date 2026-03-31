import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardWorkbenchRowActions from '@/components/StandardWorkbenchRowActions.vue'

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  props: ['variant', 'gap', 'distribution'],
  template: `
    <div
      class="standard-row-actions-stub"
      :data-variant="variant"
      :data-gap="gap"
      :data-distribution="distribution"
    >
      <slot />
    </div>
  `
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: `
    <button
      class="standard-action-link-stub"
      type="button"
      :data-testid="$attrs['data-testid']"
      :title="$attrs.title"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['items', 'label'],
  emits: ['command'],
  template: `
    <button class="standard-action-menu-stub" type="button" @click="$emit('command', items?.[0]?.command)">
      {{ label }}
    </button>
  `
})

function mountComponent(props: Record<string, unknown>) {
  return mount(StandardWorkbenchRowActions, {
    props,
    global: {
      stubs: {
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: StandardActionMenuStub
      }
    }
  })
}

describe('StandardWorkbenchRowActions', () => {
  it('uses the compact product-table spacing baseline for table rows and forwards commands', async () => {
    const wrapper = mountComponent({
      variant: 'table',
      directItems: [
        { key: 'detail', command: 'detail', label: '详情' },
        { key: 'edit', command: 'edit', label: '编辑', title: '打开编辑' }
      ],
      menuItems: [{ key: 'delete', command: 'delete', label: '删除' }]
    })

    const rowActions = wrapper.get('.standard-row-actions-stub')
    const directButtons = wrapper.findAll('.standard-action-link-stub')

    expect(rowActions.attributes('data-variant')).toBe('table')
    expect(rowActions.attributes('data-gap')).toBe('compact')
    expect(directButtons.map((button) => button.text())).toEqual(['详情', '编辑'])
    expect(directButtons[1].attributes('title')).toBe('打开编辑')

    await directButtons[1].trigger('click')
    await wrapper.get('.standard-action-menu-stub').trigger('click')

    expect(wrapper.emitted('command')).toEqual([[ 'edit' ], [ 'delete' ]])
  })

  it('uses comfortable spacing for card rows by default', () => {
    const wrapper = mountComponent({
      variant: 'card',
      directItems: [{ key: 'detail', command: 'detail', label: '详情' }],
      menuItems: []
    })

    const rowActions = wrapper.get('.standard-row-actions-stub')

    expect(rowActions.attributes('data-variant')).toBe('card')
    expect(rowActions.attributes('data-gap')).toBe('comfortable')
    expect(wrapper.find('.standard-action-menu-stub').exists()).toBe(false)
  })

  it('respects an explicit gap override', () => {
    const wrapper = mountComponent({
      variant: 'table',
      gap: 'compact',
      directItems: [{ key: 'detail', command: 'detail', label: '详情' }],
      menuItems: []
    })

    expect(wrapper.get('.standard-row-actions-stub').attributes('data-gap')).toBe('compact')
  })

  it('forwards the distribution mode to the shared row-actions layout', () => {
    const wrapper = mountComponent({
      variant: 'table',
      distribution: 'between',
      directItems: [
        { key: 'detail', command: 'detail', label: '详情' },
        { key: 'edit', command: 'edit', label: '编辑' }
      ],
      menuItems: [{ key: 'delete', command: 'delete', label: '删除' }]
    })

    expect(wrapper.get('.standard-row-actions-stub').attributes('data-distribution')).toBe('between')
  })

  it('hides the menu trigger when there are no menu items', () => {
    const wrapper = mountComponent({
      variant: 'table',
      directItems: [
        { key: 'detail', command: 'detail', label: '进入工作台' },
        { key: 'delete', command: 'delete', label: '删除' }
      ],
      menuItems: []
    })

    expect(wrapper.findAll('.standard-action-link-stub').map((button) => button.text())).toEqual(['进入工作台', '删除'])
    expect(wrapper.find('.standard-action-menu-stub').exists()).toBe(false)
  })

  it('keeps at most two direct actions and folds overflow actions into the more menu', async () => {
    const wrapper = mountComponent({
      variant: 'table',
      directItems: [
        { key: 'detail', command: 'detail', label: '详情' },
        { key: 'edit', command: 'edit', label: '编辑' },
        { key: 'delete', command: 'delete', label: '删除' }
      ],
      menuItems: []
    })

    expect(wrapper.findAll('.standard-action-link-stub').map((button) => button.text())).toEqual(['详情', '编辑'])
    expect(wrapper.find('.standard-action-menu-stub').exists()).toBe(true)

    await wrapper.get('.standard-action-menu-stub').trigger('click')

    expect(wrapper.emitted('command')).toEqual([['delete']])
  })
})
