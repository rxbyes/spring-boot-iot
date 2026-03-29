import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot name="header" /><slot /></section>'
})

describe('StandardWorkbenchPanel', () => {
  it('renders the console eyebrow above the workbench title', () => {
    const wrapper = mount(StandardWorkbenchPanel, {
      props: {
        eyebrow: 'PRODUCT CENTER',
        title: '产品定义中心',
        description: '统一维护产品台账、协议绑定与接入契约。'
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(wrapper.find('.standard-workbench-panel__eyebrow').text()).toBe('PRODUCT CENTER')
    expect(wrapper.find('.standard-workbench-panel__title').text()).toBe('产品定义中心')
    expect(wrapper.find('.standard-workbench-panel__caption').text()).toContain('统一维护产品台账')
  })
})
