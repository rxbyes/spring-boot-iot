import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ProductDeviceListWorkspace from '@/components/product/ProductDeviceListWorkspace.vue'

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  template: '<div class="device-row-actions-stub">操作</div>'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  template: '<section class="product-device-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  template: `
    <section class="product-device-table-column-stub" :data-label="label">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

describe('ProductDeviceListWorkspace', () => {
  it('shows the device list workspace with visible heading and table actions', () => {
    const wrapper = mount(ProductDeviceListWorkspace, {
      props: {
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
        ]
      },
      global: {
        stubs: {
          ElTag: true,
          ElTable: ElTableStub,
          ElTableColumn: ElTableColumnStub,
          StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub
        }
      }
    })

    expect(wrapper.find('.device-workspace__registry-heading').text()).toContain('设备清单')
    expect(wrapper.text()).not.toContain('设备总数')
    expect(wrapper.text()).not.toContain('在线设备')
    expect(wrapper.text()).not.toContain('离线设备')
    expect(wrapper.text()).not.toContain('在线比例')
    expect(wrapper.find('.device-workspace__registry-heading').text()).not.toContain('设备清册')
    expect(wrapper.find('.device-workspace__table-shell').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)
    expect(wrapper.find('.device-row-actions-stub').exists()).toBe(true)
  })
})
