import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button type="button" class="standard-action-link-stub" @click="$emit(\'click\')"><slot /></button>'
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="standard-row-actions-stub"><slot /></div>'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  template: '<section class="product-device-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'width', 'className'],
  template: `
    <section class="product-device-table-column-stub" :data-label="label" :data-width="width" :data-class-name="className">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

describe('ProductDeviceListWorkspace', () => {
  it('keeps the device registry as the primary stage without repeating device summary metrics', () => {
    const wrapper = mount(ProductDeviceListWorkspace, {
      props: {
        product: {
          id: 1001,
          productKey: 'demo-product',
          productName: '演示产品',
          protocolCode: 'mqtt-json',
          nodeType: 1
        },
        devices: [
          {
            id: 2001,
            deviceName: '一号终端',
            deviceCode: 'device-001',
            onlineStatus: 1,
            activateStatus: 1,
            firmwareVersion: 'v1.0.0',
            lastReportTime: '2026-03-30T10:00:00'
          }
        ],
        totalDevices: 1,
        onlineDevices: 1,
        offlineDevices: 0
      },
      global: {
        stubs: {
          ElTag: true,
          ElTable: ElTableStub,
          ElTableColumn: ElTableColumnStub,
          StandardActionLink: StandardActionLinkStub,
          StandardRowActions: StandardRowActionsStub
        }
      }
    })

    expect(wrapper.find('.device-workspace__chapter-header').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('设备总数')
    expect(wrapper.text()).not.toContain('在线设备')
    expect(wrapper.text()).not.toContain('离线设备')
    expect(wrapper.text()).not.toContain('在线比例')
    expect(wrapper.find('.device-workspace__summary-strip').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__brief-band').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__ledger-marquee').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__ledger-ruler').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__ruler-metrics').exists()).toBe(false)
    expect(wrapper.findAll('.device-workspace__ruler-item')).toHaveLength(0)
    expect(wrapper.find('.device-workspace__registry-sheet').exists()).toBe(true)
    expect(wrapper.find('.device-drawer__summary').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__registry-heading').text()).toContain('设备清册')
    expect(wrapper.text()).not.toContain('关联设备台账')
    expect(wrapper.find('.device-workspace__ledger-intro').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(String(actionColumn?.props('width'))).toBe('96')
  })
})
