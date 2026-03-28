import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import StandardWorkbenchPanel from '@/components/StandardWorkbenchPanel.vue'

const PanelCardStub = {
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot name="header" /><slot /></section>'
}

describe('StandardWorkbenchPanel', () => {
  it('applies the refined minimal-shell classes by default', () => {
    const wrapper = mount(StandardWorkbenchPanel, {
      props: {
        title: '产品定义中心',
        titleVariant: 'section',
        showFilters: true,
        showToolbar: true
      },
      slots: {
        filters: '<div class="filters-slot">filters</div>',
        toolbar: '<div class="toolbar-slot">toolbar</div>',
        default: '<div class="body-slot">body</div>'
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(wrapper.classes()).toContain('standard-workbench-panel--minimal')
    expect(wrapper.find('.standard-workbench-panel__header').classes()).toContain('standard-workbench-panel__header--minimal')
    expect(wrapper.find('.standard-workbench-panel__body').classes()).toContain('standard-workbench-panel__body--minimal')
  })
})
