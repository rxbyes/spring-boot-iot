import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import ShellHelpDetailDrawer from '@/components/ShellHelpDetailDrawer.vue'
import type { ShellHelpDetailDrawerProps } from '@/types/shell'

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

describe('ShellHelpDetailDrawer', () => {
  const baseProps: ShellHelpDetailDrawerProps = {
    modelValue: true,
    loading: false,
    errorMessage: '',
    highlightKeyword: '告警',
    record: {
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
  }

  it('highlights keyword and emits navigate action', async () => {
    const wrapper = mount(ShellHelpDetailDrawer, {
      props: baseProps,
      global: {
        stubs: {
          'el-drawer': DrawerStub
        }
      }
    })

    expect(wrapper.findAll('mark').length).toBeGreaterThan(0)
    await wrapper.find('button.el-button--primary').trigger('click')
    expect(wrapper.emitted('navigate')?.[0]).toEqual(['/alarm-center'])
  })
})
