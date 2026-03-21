import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import ShellNoticeCenterDrawer from '@/components/ShellNoticeCenterDrawer.vue'
import type { ShellNoticeCenterDrawerProps } from '@/types/shell'

const DrawerStub = {
  props: ['modelValue'],
  emits: ['close'],
  template: `
    <div class="el-drawer-stub">
      <div class="el-drawer-stub__header"><slot name="header" /></div>
      <div class="el-drawer-stub__body"><slot /></div>
      <div class="el-drawer-stub__footer"><slot name="footer" /></div>
      <button class="el-drawer-stub__close" @click="$emit('close')">close</button>
    </div>
  `
}

function findButtonByText(wrapper: ReturnType<typeof mount>, text: string) {
  const button = wrapper.findAll('button').find((current) => current.text().includes(text))
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }
  return button
}

describe('ShellNoticeCenterDrawer', () => {
  const baseProps: ShellNoticeCenterDrawerProps = {
    modelValue: true,
    loading: false,
    errorMessage: '',
    activeFilter: 'all',
    unreadOnly: false,
    pagination: {
      pageNum: 1,
      pageSize: 10,
      total: 1
    },
    items: [
      {
        id: 'notice-center-1',
        resourceId: '1',
        fallback: false,
        messageType: 'system',
        priority: 'high',
        title: '系统维护窗口提醒',
        summary: '今晚 23:00 执行维护',
        content: '维护详情',
        targetType: 'all',
        relatedPath: '/system-log',
        relatedPathLabel: '接入智维 / 异常观测台',
        workspaceLabel: '接入智维',
        sourceType: 'system',
        sourceId: 'maint-1',
        publishTime: '2026-03-21 23:00:00',
        expireTime: null,
        read: false,
        readTime: null
      }
    ]
  }

  it('renders list items and emits center actions', async () => {
    const wrapper = mount(ShellNoticeCenterDrawer, {
      props: baseProps,
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.text()).toContain('系统维护窗口提醒')
    expect(wrapper.text()).toContain('未读')

    await findButtonByText(wrapper, '查看详情').trigger('click')
    await findButtonByText(wrapper, '标记已读').trigger('click')
    await findButtonByText(wrapper, '全部已读').trigger('click')

    expect(wrapper.emitted('select')?.[0]?.[0]).toEqual(expect.objectContaining({ resourceId: '1' }))
    expect(wrapper.emitted('read')?.[0]?.[0]).toEqual(expect.objectContaining({ resourceId: '1' }))
    expect(wrapper.emitted('readAll')).toHaveLength(1)
  })
})
