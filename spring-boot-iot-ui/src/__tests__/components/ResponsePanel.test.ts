import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ResponsePanel from '@/components/ResponsePanel.vue'

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="response-panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
      <slot />
      <slot name="actions" />
    </section>
  `
})

describe('ResponsePanel', () => {
  it('keeps the JSON response panel on title/description only without eyebrow passthrough', () => {
    const wrapper = mount(ResponsePanel, {
      props: {
        title: '原始响应',
        description: '用于查看接口回包',
        eyebrow: 'JSON PREVIEW',
        body: {
          ok: true
        }
      },
      global: {
        stubs: {
          PanelCard: PanelCardStub,
          StandardButton: true
        }
      }
    })

    const panel = wrapper.findComponent(PanelCardStub)

    expect(panel.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('原始响应')
    expect(wrapper.text()).not.toContain('JSON PREVIEW')
  })
})
