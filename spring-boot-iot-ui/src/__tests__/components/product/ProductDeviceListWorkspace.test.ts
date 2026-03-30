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

describe('ProductDeviceListWorkspace', () => {
  it('renders a compact metric band and keeps the ledger table as the primary stage', () => {
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
          ElTable: true,
          ElTableColumn: true,
          StandardActionLink: StandardActionLinkStub,
          StandardRowActions: StandardRowActionsStub
        }
      }
    })

    expect(wrapper.text()).toContain('设备运行概览')
    expect(wrapper.text()).toContain('设备总数')
    expect(wrapper.text()).toContain('在线设备')
    expect(wrapper.text()).toContain('离线设备')
    expect(wrapper.text()).toContain('在线比例')
    expect(wrapper.text()).toContain('关联设备台账')
    expect(wrapper.find('.device-workspace__summary-band').exists()).toBe(true)
    expect(wrapper.find('.device-workspace__table-stage').exists()).toBe(true)
    expect(wrapper.find('.device-drawer__summary').exists()).toBe(false)
    expect(wrapper.find('.device-workspace__section-copy').text()).toContain('关联设备台账')
    expect(wrapper.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)
  })
})
