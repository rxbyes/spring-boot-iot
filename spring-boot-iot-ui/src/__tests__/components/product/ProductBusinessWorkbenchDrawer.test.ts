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
      <div class="standard-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="standard-detail-drawer-stub__body"><slot /></div>
    </section>
  `
})

describe('ProductBusinessWorkbenchDrawer', () => {
  it('renders the workbench header with a single contract sentence and without repeated running-period judgement copy', () => {
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
        'header-actions': '<button type="button">编辑档案</button>',
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
    expect(wrapper.find('.product-business-workbench__journal-head').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__journal-masthead').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__exhibit-head').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__title-column').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__journal-title').text()).toContain('演示产品')
    expect(wrapper.find('.product-business-workbench__journal-kicker').text()).toContain('产品经营页')
    expect(wrapper.find('.product-business-workbench__journal-summary').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__journal-summary').text()).not.toContain('已进入运行期')
    expect(wrapper.find('.product-business-workbench__journal-summary').text()).not.toContain('围绕规模、契约与档案校准')
    expect(wrapper.find('.product-business-workbench__scale-column').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__scale-panel').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__scale-panel').text()).toContain('12')
    expect(wrapper.find('.product-business-workbench__scale-panel').text()).toContain('关联设备总量')
    expect(wrapper.find('.product-business-workbench__meta-line').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__meta-line').text()).toContain(
      '产品key：demo-product｜接入协议：mqtt-json｜节点类型：直连设备｜数据格式：JSON'
    )
    expect(wrapper.text().match(/演示产品/g)?.length).toBe(1)
    expect(wrapper.text().match(/demo-product/g)?.length).toBe(1)
    expect(wrapper.find('.standard-detail-drawer-stub__header-actions').text()).toContain('编辑档案')
    expect(wrapper.text()).toContain('产品总览')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('契约字段')
    expect(wrapper.text()).not.toContain('经营总览')
    expect(wrapper.text()).not.toContain('物模型治理')
    expect(wrapper.text()).not.toContain('编辑治理')
    expect(wrapper.find('.product-business-workbench__tab-index').exists()).toBe(false)
    expect(wrapper.find('.product-business-workbench__tab-strip').exists()).toBe(true)
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
        'header-actions': '<button type="button">编辑档案</button>',
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

    expect(wrapper.find('.product-business-workbench__scale-panel').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__journal-head').exists()).toBe(true)
    expect(wrapper.find('.standard-detail-drawer-stub__header-actions').text()).toContain('编辑档案')
    expect(wrapper.find('.product-business-workbench__meta-line').text()).toContain(
      '产品key：zhd-warning-sound-light-alarm-v1｜接入协议：mqtt-json｜节点类型：直连设备｜数据格式：JSON'
    )
  })
})
