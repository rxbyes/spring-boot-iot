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
  it('renders the workbench as a 2026 exhibition-flat shell with a title plinth and indexed tabs', () => {
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

    expect(wrapper.find('h2').text()).toBe('产品经营工作台')
    expect(wrapper.find('.product-business-workbench__header').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__exhibit-head').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__exhibit-copy').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__header-brief').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__identity-frame').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__headline').text()).toContain('演示产品')
    expect(wrapper.find('.product-business-workbench__eyebrow').text()).toContain('产品展陈')
    expect(wrapper.find('.product-business-workbench__summary-band').exists()).toBe(false)
    expect(wrapper.findAll('.product-business-workbench__summary-metric')).toHaveLength(0)
    expect(wrapper.findAll('.product-business-workbench__summary-divider')).toHaveLength(0)
    expect(wrapper.find('.product-business-workbench__status-statement').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__status-statement').text()).not.toContain('当前状态：')
    expect(wrapper.find('.product-business-workbench__status-statement').text()).not.toContain('治理')
    expect(wrapper.find('.product-business-workbench__meta-inline').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__meta-inline').text()).toContain('demo-product')
    expect(wrapper.find('.product-business-workbench__meta-inline').text()).toContain('mqtt-json')
    expect(wrapper.find('.product-business-workbench__meta-inline').text()).toContain('直连设备')
    expect(wrapper.find('.product-business-workbench__meta-inline').text()).toContain('JSON')
    expect(wrapper.text().match(/演示产品/g)?.length).toBe(1)
    expect(wrapper.text().match(/demo-product/g)?.length).toBe(1)
    expect(wrapper.text()).toContain('经营总览')
    expect(wrapper.text()).toContain('物模型治理')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('编辑治理')
    expect(wrapper.find('.product-business-workbench__tab-index').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__tab-rail').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__tabs').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__tab--active').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__view-shell').exists()).toBe(true)
    expect(wrapper.get('[data-view="models"]').text()).toContain('models-slot')
    expect(wrapper.get('[data-view="overview"]').text()).toContain('overview-slot')
    expect(wrapper.get('[data-view="overview"]').attributes('style')).toContain('display: none;')
  })

  it('avoids repeating the same identity text when product name and product key are identical', () => {
    const wrapper = mount(ProductBusinessWorkbenchDrawer, {
      props: {
        modelValue: true,
        activeView: 'overview',
        product: {
          id: 1002,
          productKey: 'zhd-warning-sound-light-alarm-v1',
          productName: 'zhd-warning-sound-light-alarm-v1',
          protocolCode: 'mqtt-json',
          nodeType: 1,
          dataFormat: 'JSON',
          status: 1,
          deviceCount: 24,
          onlineDeviceCount: 13,
          thirtyDaysActiveCount: 18
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

    expect(wrapper.text().match(/zhd-warning-sound-light-alarm-v1/g)?.length).toBe(1)
    expect(wrapper.find('.product-business-workbench__summary-band').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__exhibit-head').exists()).toBe(true)
  })
})
