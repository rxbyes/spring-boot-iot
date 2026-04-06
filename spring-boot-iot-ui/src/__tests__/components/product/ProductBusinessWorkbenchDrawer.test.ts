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
  it('renders the compact two-card workbench header and itemized identity row', () => {
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
    expect(wrapper.find('[data-testid="product-overview-card"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="related-device-card"]').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench__journal-title').text()).toContain('演示产品')
    expect(wrapper.find('.product-business-workbench__journal-kicker').text()).toContain('产品经营页')
    expect(wrapper.text()).not.toContain('契约状态')
    expect(wrapper.text()).not.toContain('下一步建议')
    expect(wrapper.text()).not.toContain('当前已有运行设备，可继续补齐并核对契约字段')
    expect(wrapper.text()).toContain('关联设备')
    expect(wrapper.text()).toContain('12 台')
    expect(wrapper.find('.product-business-workbench__identity-row').exists()).toBe(true)
    const identityItems = wrapper.findAll('.product-business-workbench__identity-item')
    expect(identityItems).toHaveLength(4)
    const identityLabels = wrapper.findAll('.product-business-workbench__identity-label')
    const identityValues = wrapper.findAll('.product-business-workbench__identity-value')
    expect(identityLabels).toHaveLength(4)
    expect(identityValues).toHaveLength(4)
    expect(identityItems[0]?.classes()).toContain('product-business-workbench__identity-item--key')
    identityLabels.forEach((node) => {
      expect(node.classes()).toContain('product-business-workbench__copy-label')
    })
    identityValues.forEach((node, index) => {
      expect(node.classes()).toContain('product-business-workbench__copy-value')
      if (index === 0) {
        expect(node.classes()).toContain('product-business-workbench__identity-value--key')
      } else {
        expect(node.classes()).toContain('product-business-workbench__identity-value--detail')
      }
    })
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('产品Key')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('demo-product')
    expect(wrapper.get('[data-testid="product-key-hero"]').text()).toContain('demo-product')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('接入协议')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('mqtt-json')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('节点类型')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('直连设备')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('数据格式')
    expect(wrapper.find('.product-business-workbench__identity-row').text()).toContain('JSON')
    expect(wrapper.text().match(/演示产品/g)?.length).toBe(1)
    expect(wrapper.text().match(/demo-product/g)?.length).toBe(1)
    expect(wrapper.find('.standard-detail-drawer-stub__header-actions').text()).toContain('编辑档案')
    const tabLabels = wrapper
      .findAll('.product-business-workbench__tab-label')
      .map((node) => node.text())
      .filter((label) => label.trim().length > 0)
    expect(tabLabels).toEqual(['产品总览', '关联设备', '契约字段'])
    expect(wrapper.get('[data-view="models"]').text()).toContain('models-slot')
    expect(wrapper.get('[data-view="overview"]').text()).toContain('overview-slot')
    expect(wrapper.get('[data-view="overview"]').attributes('style')).toContain('display: none;')
  })

  it('renders 0 台 in relation metric card when product has no devices', () => {
    const wrapper = mount(ProductBusinessWorkbenchDrawer, {
      props: {
        modelValue: true,
        activeView: 'devices',
        product: {
          id: 1003,
          productKey: 'zero-device-product',
          productName: '零设备产品',
          protocolCode: 'mqtt-json',
          nodeType: 1,
          dataFormat: 'JSON',
          status: 1,
          deviceCount: 0,
          onlineDeviceCount: 0,
          thirtyDaysActiveCount: 0
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

    expect(wrapper.find('.product-business-workbench__metric-card').text()).toContain('关联设备')
    expect(wrapper.find('.product-business-workbench__metric-card').text()).toContain('0 台')
    expect(wrapper.find('.product-business-workbench__metric-card').text()).not.toContain('--')
  })
})
