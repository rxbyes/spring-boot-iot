import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
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

    expect(wrapper.classes()).toContain('standard-workbench-panel--workbench-foundation')
    expect(wrapper.find('.standard-workbench-panel__eyebrow').text()).toBe('PRODUCT CENTER')
    expect(wrapper.find('.standard-workbench-panel__title').text()).toBe('产品定义中心')
    expect(wrapper.find('.standard-workbench-panel__caption').text()).toContain('统一维护产品台账')
  })

  it('uses shared workbench spacing tokens instead of page-private gaps', () => {
    const source = readFileSync(
      resolve(import.meta.dirname, '../../components/StandardWorkbenchPanel.vue'),
      'utf8'
    )

    expect(source).toContain('--ops-workbench-gap')
    expect(source).toContain('var(--ops-workbench-gap')
    expect(source).toContain('standard-workbench-panel__body')
  })

  it('binds heading typography to the shared song-style hierarchy tokens', () => {
    const source = readFileSync(
      resolve(import.meta.dirname, '../../components/StandardWorkbenchPanel.vue'),
      'utf8'
    )

    expect(source).toContain('var(--type-overline-size)')
    expect(source).toContain('var(--type-title-2-size)')
    expect(source).toContain('var(--type-caption-size)')
    expect(source).toContain('var(--font-letter-spacing-wide)')
    expect(source).toContain('var(--font-letter-spacing-tight)')
  })

  it('marks the filter band as compact when filters are rendered', () => {
    const wrapper = mount(StandardWorkbenchPanel, {
      props: {
        title: 'Workbench',
        showFilters: true
      },
      slots: {
        filters: '<div class="filter-stub">Filter</div>'
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub
        }
      }
    })

    expect(wrapper.find('.standard-workbench-panel__filters').classes()).toContain(
      'standard-workbench-panel__filters--compact'
    )
  })
})
