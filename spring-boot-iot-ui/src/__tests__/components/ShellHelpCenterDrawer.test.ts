import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import ShellHelpCenterDrawer from '@/components/ShellHelpCenterDrawer.vue'
import type { ShellHelpCenterDrawerProps } from '@/types/shell'

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

function createWrapper(props: ShellHelpCenterDrawerProps) {
  return mount(ShellHelpCenterDrawer, {
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

describe('ShellHelpCenterDrawer', () => {
  const baseProps: ShellHelpCenterDrawerProps = {
    modelValue: true,
    loading: false,
    errorMessage: '',
    activeFilter: 'all',
    keyword: '告警',
    pagination: {
      pageNum: 1,
      pageSize: 10,
      total: 1
    },
    items: [
      {
        id: 'help-center-10',
        resourceId: '10',
        fallback: false,
        docCategory: 'business',
        sortNo: 1,
        title: '告警运营与事件协同业务手册',
        summary: '面向业务角色的告警闭环指引',
        content: '告警详情与处理步骤',
        keywords: '告警,事件',
        relatedPaths: '/alarm-center,/event-disposal',
        currentPathMatched: true,
        keywordList: ['告警', '事件'],
        relatedPathList: ['/alarm-center', '/event-disposal'],
        relatedPathLabel: '风险运营 / 告警运营台、风险运营 / 事件协同台',
        workspaceLabel: '风险运营',
        primaryPath: '/alarm-center'
      }
    ]
  }

  it('renders highlighted records and emits search, detail and navigate actions', async () => {
    const wrapper = createWrapper(baseProps)

    expect(wrapper.text()).toContain('告警运营与事件协同业务手册')
    expect(wrapper.text()).toContain('当前页相关')
    expect(wrapper.findAll('mark').length).toBeGreaterThan(0)
    expect(wrapper.text()).not.toContain('Shell Help')

    const categorySelect = wrapper.findComponent({ name: 'ElSelect' })
    categorySelect.vm.$emit('update:modelValue', 'faq')
    const keywordInput = wrapper.findComponent({ name: 'ElInput' })
    keywordInput.vm.$emit('update:modelValue', '事件')

    await findButtonByText(wrapper, '搜索').trigger('click')
    await findButtonByText(wrapper, '查看详情').trigger('click')
    await findButtonByText(wrapper, '进入页面').trigger('click')

    expect(wrapper.emitted('update:activeFilter')?.[0]).toEqual(['faq'])
    expect(wrapper.emitted('update:keyword')?.[0]).toEqual(['事件'])
    expect(wrapper.emitted('search')).toHaveLength(1)
    expect(wrapper.emitted('select')?.[0]?.[0]).toEqual(expect.objectContaining({ resourceId: '10' }))
    expect(wrapper.emitted('navigate')?.[0]).toEqual(['/alarm-center'])
  })

  it('renders error and empty states from the shared detail drawer contract', () => {
    const errorWrapper = createWrapper({
      ...baseProps,
      items: [],
      errorMessage: '帮助资料加载失败'
    })
    expect(errorWrapper.text()).toContain('帮助资料加载失败')

    const emptyWrapper = createWrapper({
      ...baseProps,
      items: [],
      errorMessage: ''
    })
    expect(emptyWrapper.text()).toContain('当前暂无可查看的帮助资料')
  })
})
