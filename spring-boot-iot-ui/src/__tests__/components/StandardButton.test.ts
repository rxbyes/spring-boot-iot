import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'

import StandardActionLink from '@/components/StandardActionLink.vue'
import StandardActionMenu from '@/components/StandardActionMenu.vue'
import StandardButton from '@/components/StandardButton.vue'

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: {
    type: { type: String, default: '' },
    plain: { type: Boolean, default: false },
    link: { type: Boolean, default: false },
    text: { type: Boolean, default: false },
    size: { type: String, default: '' },
    nativeType: { type: String, default: 'button' }
  },
  template: `
    <button
      class="el-button-stub"
      :data-type="type"
      :data-plain="String(plain)"
      :data-link="String(link)"
      :data-text="String(text)"
      :data-size="size"
      :type="nativeType"
    >
      <slot />
    </button>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: {
    action: { type: String, default: 'default' },
    type: { type: String, default: '' },
    link: { type: Boolean, default: false }
  },
  template: `
    <button
      class="standard-button-stub"
      :data-action="action"
      :data-type="type"
      :data-link="String(link)"
    >
      <slot />
    </button>
  `
})

const ElDropdownStub = defineComponent({
  name: 'ElDropdown',
  template: `
    <div class="el-dropdown-stub">
      <slot />
      <slot name="dropdown" />
    </div>
  `
})

const ElDropdownMenuStub = defineComponent({
  name: 'ElDropdownMenu',
  template: '<div class="el-dropdown-menu-stub"><slot /></div>'
})

const ElDropdownItemStub = defineComponent({
  name: 'ElDropdownItem',
  props: {
    command: { type: [String, Number, Object], default: '' },
    disabled: { type: Boolean, default: false },
    divided: { type: Boolean, default: false }
  },
  template: `
    <div
      class="el-dropdown-item-stub"
      :data-command="typeof command === 'string' ? command : ''"
      :data-disabled="String(disabled)"
      :data-divided="String(divided)"
    >
      <slot />
    </div>
  `
})

describe('StandardButton', () => {
  it('maps query actions to the orange primary button semantics', () => {
    const wrapper = mount(StandardButton, {
      props: { action: 'query' },
      slots: { default: '查询' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    const button = wrapper.get('button')
    expect(button.attributes('data-type')).toBe('primary')
    expect(button.attributes('data-link')).toBe('false')
    expect(button.attributes('class')).toContain('standard-button--query')
    expect(button.attributes('class')).toContain('standard-button--tone-solid')
  })

  it('keeps reset and cancel actions in the shared secondary semantics', () => {
    const resetWrapper = mount(StandardButton, {
      props: { action: 'reset' },
      slots: { default: '重置' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    const cancelWrapper = mount(StandardButton, {
      props: { action: 'cancel' },
      slots: { default: '取消' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    expect(resetWrapper.get('button').attributes('data-type')).toBe('')
    expect(resetWrapper.get('button').attributes('class')).toContain('standard-button--tone-secondary')
    expect(cancelWrapper.get('button').attributes('data-type')).toBe('')
    expect(cancelWrapper.get('button').attributes('class')).toContain('standard-button--tone-secondary')
  })

  it('maps delete and confirm actions to the orange primary branch', () => {
    const deleteWrapper = mount(StandardButton, {
      props: { action: 'delete' },
      slots: { default: '删除' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    const confirmWrapper = mount(StandardButton, {
      props: { action: 'confirm' },
      slots: { default: '确定' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    expect(deleteWrapper.get('button').attributes('data-type')).toBe('primary')
    expect(deleteWrapper.get('button').attributes('class')).toContain('standard-button--delete')
    expect(confirmWrapper.get('button').attributes('data-type')).toBe('primary')
    expect(confirmWrapper.get('button').attributes('class')).toContain('standard-button--confirm')
  })

  it('keeps link buttons in the shared orange text action branch', () => {
    const wrapper = mount(StandardButton, {
      props: { action: 'refresh', link: true },
      slots: { default: '刷新列表' },
      global: {
        stubs: {
          ElButton: ElButtonStub
        }
      }
    })

    const button = wrapper.get('button')
    expect(button.attributes('data-type')).toBe('')
    expect(button.attributes('data-link')).toBe('true')
    expect(button.attributes('class')).toContain('standard-button--tone-link')
  })
})

describe('StandardActionLink', () => {
  it('keeps action links on the shared brand branch without forwarding type or tone attrs', () => {
    const wrapper = mount(StandardActionLink, {
      props: { action: 'delete' },
      attrs: {
        type: 'danger',
        tone: 'danger'
      },
      slots: { default: '删除' },
      global: {
        stubs: {
          StandardButton: StandardButtonStub
        }
      }
    })

    const button = wrapper.get('button')
    expect(button.attributes('data-action')).toBe('delete')
    expect(button.attributes('data-type')).toBe('')
    expect(button.attributes('data-link')).toBe('true')
    expect(button.attributes('tone')).toBeUndefined()
  })
})

describe('StandardActionMenu', () => {
  it('renders shared menu items without a tone branch', () => {
    const wrapper = mount(StandardActionMenu, {
      props: {
        items: [
          { command: 'devices', label: '查看设备' },
          { command: 'delete', label: '删除' }
        ]
      },
      global: {
        stubs: {
          StandardActionLink: StandardButtonStub,
          ElDropdown: ElDropdownStub,
          ElDropdownMenu: ElDropdownMenuStub,
          ElDropdownItem: ElDropdownItemStub
        }
      }
    })

    const items = wrapper.findAll('.el-dropdown-item-stub')
    expect(items).toHaveLength(2)
    expect(items[0].text()).toContain('查看设备')
    expect(items[1].text()).toContain('删除')
  })
})
