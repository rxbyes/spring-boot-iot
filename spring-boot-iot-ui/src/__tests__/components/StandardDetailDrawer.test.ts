import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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

  it('supports hiding the whole masthead when a detail drawer should start directly from body content', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '设备详情',
        subtitle: '这一整块刊头应被移除',
        tags: [{ label: '在线', type: 'success' }],
        hideHeader: true
      },
      slots: {
        default: '<div class="detail-body">body</div>'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub,
          'el-tag': {
            props: ['type', 'effect'],
            template: '<span class="el-tag-stub"><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.find('.detail-drawer__header').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('设备详情')
    expect(wrapper.text()).not.toContain('在线')
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

  it('renders header actions when a header-actions slot is provided', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '产品详情',
        subtitle: '统一查看产品主档'
      },
      slots: {
        'header-actions': '<button class="detail-header-action">编辑档案</button>',
        default: '<div class="detail-body">body</div>'
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.find('.detail-header-action').exists()).toBe(true)
    expect(wrapper.find('.detail-header-action').text()).toBe('编辑档案')
  })

  it('supports an inline title-row tag layout without wrapping status pills onto a second line', () => {
    const wrapper = mount(StandardDetailDrawer, {
      props: {
        modelValue: true,
        title: '设备资产',
        subtitle: '统一查看资产详情',
        tagLayout: 'title-inline',
        tags: [
          { label: '已登记', type: 'success' },
          { label: '在线', type: 'success' },
          { label: '已激活', type: 'success' },
          { label: '启用', type: 'success' }
        ]
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub,
          'el-tag': {
            props: ['type', 'effect'],
            template: '<span class="el-tag-stub"><slot /></span>'
          }
        }
      }
    })

    expect(wrapper.find('.detail-drawer__title-row').exists()).toBe(true)
    expect(wrapper.find('.detail-drawer__title-row .detail-drawer__tags').exists()).toBe(true)
    expect(wrapper.findAll('.detail-drawer__title-row .el-tag-stub')).toHaveLength(4)

    const source = readFileSync(resolve(import.meta.dirname, '../../components/StandardDetailDrawer.vue'), 'utf8')

    expect(source).toContain("tagLayout === 'title-inline'")
    expect(source).toContain('.detail-drawer__title-row')
    expect(source).toContain('flex-wrap: nowrap;')
    expect(source).toContain('overflow-x: auto;')
  })
})
