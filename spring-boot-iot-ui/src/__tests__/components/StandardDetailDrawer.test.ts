import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import StandardDetailDrawer from '@/components/StandardDetailDrawer.vue'

const DrawerStub = {
  props: ['modelValue'],
  template: `
    <div class="el-drawer-stub">
      <div class="el-drawer-stub__header"><slot name="header" /></div>
      <div class="el-drawer-stub__body"><slot /></div>
      <div class="el-drawer-stub__footer"><slot name="footer" /></div>
    </div>
  `
}

describe('StandardDetailDrawer', () => {
  it('keeps the header on title/subtitle only even if legacy eyebrow input is provided', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '产品详情',
        subtitle: '用于查看产品主档',
        eyebrow: 'LEGACY DETAIL'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.text()).toContain('产品详情')
    expect(wrapper.text()).toContain('用于查看产品主档')
    expect(wrapper.text()).not.toContain('LEGACY DETAIL')
  })

  it('renders footer content only when a footer slot is provided', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '产品详情'
      },
      slots: {
        default: '<div class="detail-body">body</div>',
        footer: '<button class="detail-submit">提交</button>'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.find('.detail-submit').exists()).toBe(true)
  })

  it('keeps the drawer footer empty when no footer slot exists', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '产品详情'
      },
      slots: {
        default: '<div class="detail-body">body</div>'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.find('.detail-drawer__footer').exists()).toBe(false)
  })
})
