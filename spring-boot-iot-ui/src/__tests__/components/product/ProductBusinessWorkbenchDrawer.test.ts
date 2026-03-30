import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductBusinessWorkbenchDrawer from '@/components/product/ProductBusinessWorkbenchDrawer.vue'

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle', 'size'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="standard-detail-drawer-stub" :data-size="size">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <div class="standard-detail-drawer-stub__body"><slot /></div>
    </section>
  `
})

describe('ProductBusinessWorkbenchDrawer', () => {
  it('renders one business header and keeps all visited views inside the same drawer stage', () => {
    const wrapper = mount(ProductBusinessWorkbenchDrawer, {
      props: {
        modelValue: true,
        activeView: 'models',
        product: {
          id: 1001,
          productKey: 'demo-product',
          productName: '演示产品',
          protocolCode: 'mqtt-json',
          nodeType: 1,
          dataFormat: 'JSON',
          status: 1,
          deviceCount: 12,
          onlineDeviceCount: 8,
          thirtyDaysActiveCount: 10
        }
      },
      slots: {
        overview: '<div>overview-slot</div>',
        models: '<div>models-slot</div>',
        devices: '<div>devices-slot</div>',
        edit: '<div>edit-slot</div>'
      },
      global: {
        stubs: {
          StandardDetailDrawer: StandardDetailDrawerStub,
          ElTag: true
        }
      }
    })

    expect(wrapper.text()).toContain('演示产品')
    expect(wrapper.text()).toContain('demo-product')
    expect(wrapper.text()).toContain('经营总览')
    expect(wrapper.text()).toContain('物模型治理')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('编辑治理')
    expect(wrapper.get('[data-view="models"]').text()).toContain('models-slot')
    expect(wrapper.get('[data-view="overview"]').text()).toContain('overview-slot')
    expect(wrapper.get('[data-view="overview"]').attributes('style')).toContain('display: none;')
  })
})
