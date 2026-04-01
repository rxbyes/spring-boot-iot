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
  it('keeps the device ledger as the primary stage and uses one formal brief band instead of stacked metric cards', () => {
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
    expect(wrapper.text()).toContain('设备总数')
    expect(wrapper.text()).toContain('在线设备')
    expect(wrapper.text()).toContain('离线设备')
    expect(wrapper.text()).toContain('在线比例')
    expect(wrapper.find('.device-workspace__summary-strip').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__brief-band').exists()).toBe(true)
    expect(wrapper.findAll('.device-workspace__brief-item')).toHaveLength(4)
    expect(wrapper.find('.device-workspace__ledger-stage').exists()).toBe(true)
    expect(wrapper.find('.device-drawer__summary').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__ledger-heading').text()).toContain('关联设备台账')
    expect((wrapper.text().match(/关联设备台账/g) || []).length).toBe(1)
    expect(wrapper.find('.device-workspace__ledger-intro').exists()).toBe(true)
    expect(wrapper.find('.device-workspace__ledger-intro').text()).toContain('核对设备身份、在线状态和最近上报')
    expect(wrapper.find('.device-workspace__brief-item small').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__brief-item[data-tone="brand"]').exists()).toBe(true)
    expect(wrapper.find('.device-workspace__brief-item[data-tone="success"]').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__brief-item[data-tone="danger"]').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__brief-item[data-tone="accent"]').exists()).toBe(false)
    expect(wrapper.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(String(actionColumn?.props('width'))).toBe('96')
  })
})
