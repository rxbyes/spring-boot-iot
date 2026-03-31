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
  it('renders a cleaner business header with one product title and a relaxed summary-card rail', () => {
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
    expect(wrapper.find('.product-business-workbench__headline').text()).toContain('演示产品')
    expect(wrapper.find('.product-business-workbench__identity-key').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__brief').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__header-summary').exists()).toBe(true)
    expect(wrapper.get('[data-testid="product-workbench-summary-deviceCount"]').text()).toContain('12')
    expect(wrapper.get('[data-testid="product-workbench-summary-onlineDeviceCount"]').text()).toContain('8')
    expect(wrapper.get('[data-testid="product-workbench-summary-thirtyDaysActiveCount"]').text()).toContain('10')
    expect(wrapper.find('.product-business-workbench__header-note').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__judgement').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__status-badge').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__meta-strip').text()).toContain('mqtt-json')
    expect(wrapper.text().match(/演示产品/g)?.length).toBe(1)
    expect(wrapper.text().match(/demo-product/g)?.length).toBe(1)
    expect(wrapper.text()).toContain('经营总览')
    expect(wrapper.text()).toContain('物模型治理')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('编辑治理')
    expect(wrapper.find('.product-business-workbench__tab-rail').exists()).toBe(true)
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
  })
})
