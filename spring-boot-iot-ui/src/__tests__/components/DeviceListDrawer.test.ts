import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DeviceListDrawer from '@/components/DeviceListDrawer.vue'

const DrawerStub = defineComponent({
  name: 'ElDrawer',
  props: ['modelValue'],
  template: `
    <div class="device-list-drawer-el-stub">
      <div><slot name="header" /></div>
      <div><slot /></div>
      <div><slot name="footer" /></div>
    </div>
  `
})

describe('DeviceListDrawer', () => {
  it('keeps the device list drawer in two-tier Chinese hierarchy without eyebrow text', () => {
    const wrapper = mount(DeviceListDrawer, {
      props: {
        modelValue: true,
        title: '设备列表',
        subtitle: '查看产品关联设备',
        eyebrow: 'DEVICE LEDGER',
        devices: [],
        totalDevices: 0,
        onlineDevices: 0,
        offlineDevices: 0
      },
      global: {
        stubs: {
          'el-drawer': DrawerStub,
          ElTag: true,
          ElTable: true,
          ElTableColumn: true,
          StandardRowActions: true,
          StandardActionLink: true
        }
      }
    })

    expect(wrapper.text()).toContain('设备列表')
    expect(wrapper.text()).toContain('查看产品关联设备')
    expect(wrapper.text()).not.toContain('DEVICE LEDGER')
  })
})
