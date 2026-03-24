import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import ShellNoticeDetailDrawer from '@/components/ShellNoticeDetailDrawer.vue'
import type { ShellNoticeDetailDrawerProps } from '@/types/shell'

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

function createWrapper(props: ShellNoticeDetailDrawerProps) {
  return mount(ShellNoticeDetailDrawer, {
    props,
    global: {
      stubs: {
        'el-drawer': DrawerStub
      }
    }
  })
}

function findButtonByText(wrapper: ReturnType<typeof createWrapper>, text: string) {
  const button = wrapper.findAll('button').find((current) => current.text().includes(text))
  if (!button) {
    throw new Error(`Button not found: ${text}`)
  }
  return button
}

describe('ShellNoticeDetailDrawer', () => {
  const baseProps: ShellNoticeDetailDrawerProps = {
    modelValue: true,
    loading: false,
    errorMessage: '',
    record: {
      id: 'notice-center-1',
      resourceId: '1',
      fallback: false,
      messageType: 'system',
      priority: 'high',
      title: '系统维护窗口提醒',
      summary: '今晚 23:00 执行维护',
      content: '维护详情与排障指引',
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
  }

  it('renders detail content and emits navigate and mark-read actions', async () => {
    const wrapper = createWrapper(baseProps)

    expect(wrapper.text()).toContain('系统维护窗口提醒')
    expect(wrapper.text()).toContain('未读')
    expect(wrapper.text()).toContain('维护详情与排障指引')

    await findButtonByText(wrapper, '进入页面').trigger('click')
    await findButtonByText(wrapper, '标记已读').trigger('click')

    expect(wrapper.emitted('navigate')?.[0]).toEqual(['/system-log'])
    expect(wrapper.emitted('markRead')).toHaveLength(1)
  })

  it('renders the shared error state when detail loading fails', () => {
    const wrapper = createWrapper({
      ...baseProps,
      record: null,
      errorMessage: '消息详情加载失败'
    })

    expect(wrapper.text()).toContain('消息详情加载失败')
  })
})
