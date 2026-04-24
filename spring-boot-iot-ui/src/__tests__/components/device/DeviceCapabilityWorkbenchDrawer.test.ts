import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DeviceCapabilityWorkbenchDrawer from '@/components/device/DeviceCapabilityWorkbenchDrawer.vue'
import type { CommandRecordPageItem, Device, DeviceCapabilityOverview } from '@/types/api'

const device: Device = {
  deviceCode: '6260370286',
  deviceName: '中海达声光报警器-1',
  productKey: 'zhd-warning-sound-light-alarm-v1',
  productName: '中海达 预警型 声光报警器',
  onlineStatus: 1,
  activateStatus: 1,
  deviceStatus: 1
}

const overview: DeviceCapabilityOverview = {
  deviceCode: device.deviceCode,
  productId: '202603192100560271',
  productKey: device.productKey,
  productCapabilityType: 'WARNING',
  subType: 'BROADCAST',
  onlineExecutable: true,
  capabilities: [
    {
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      enabled: true,
      requiresOnline: true,
      paramsSchema: {}
    }
  ]
}

const commands: CommandRecordPageItem[] = [
  {
    id: 1,
    commandId: 'CMD-001',
    serviceIdentifier: 'broadcast_play',
    status: 'SENT',
    sendTime: '2026-04-24T10:50:00',
    topic: '/iot/broadcast/6260370286'
  }
]

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle', 'empty'],
  emits: ['update:modelValue'],
  template: `
    <section class="standard-detail-drawer-stub" :data-visible="String(modelValue)">
      <h2 class="standard-detail-drawer-stub__title">{{ title }}</h2>
      <p class="standard-detail-drawer-stub__subtitle">{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const DeviceCapabilityPanelStub = defineComponent({
  name: 'DeviceCapabilityPanel',
  props: ['overview', 'commands', 'loading', 'commandLoading'],
  emits: ['execute', 'refreshCommands'],
  template: `
    <section class="device-capability-panel-stub">
      <button class="device-capability-panel-stub__execute" type="button" @click="$emit('execute', { code: 'broadcast_play' })">
        execute
      </button>
      <button class="device-capability-panel-stub__refresh" type="button" @click="$emit('refreshCommands')">
        refresh
      </button>
      <span class="device-capability-panel-stub__overview">{{ overview?.productCapabilityType }}</span>
      <span class="device-capability-panel-stub__commands">{{ (commands || []).length }}</span>
    </section>
  `
})

function mountDrawer(props: Record<string, unknown> = {}) {
  return mount(DeviceCapabilityWorkbenchDrawer, {
    props: {
      modelValue: true,
      device,
      overview,
      commands,
      capabilityLoading: false,
      commandLoading: false,
      ...props
    },
    global: {
      stubs: {
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardInlineState: StandardInlineStateStub,
        StandardButton: StandardButtonStub,
        DeviceCapabilityPanel: DeviceCapabilityPanelStub
      }
    }
  })
}

describe('DeviceCapabilityWorkbenchDrawer', () => {
  it('renders a compact device-operation drawer with summary and capability panel', () => {
    const wrapper = mountDrawer()

    expect(wrapper.text()).toContain('设备操作')
    expect(wrapper.text()).toContain('中海达声光报警器-1')
    expect(wrapper.text()).toContain('6260370286')
    expect(wrapper.text()).toContain('中海达 预警型 声光报警器')
    expect(wrapper.text()).toContain('在线 / 已激活 / 启用')
    expect(wrapper.text()).toContain('WARNING')
    expect(wrapper.text()).toContain('1')
  })

  it('emits execute and refresh commands from the inner capability panel and closes from the footer', async () => {
    const wrapper = mountDrawer()

    await wrapper.get('.device-capability-panel-stub__execute').trigger('click')
    await wrapper.get('.device-capability-panel-stub__refresh').trigger('click')
    await wrapper.get('.device-capability-workbench-drawer__close').trigger('click')

    expect(wrapper.emitted('executeCapability')?.[0]?.[0]).toMatchObject({
      code: 'broadcast_play'
    })
    expect(wrapper.emitted('refreshCommands')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toBe(false)
  })

  it('shows a loading hint before the capability overview resolves', () => {
    const wrapper = mountDrawer({
      overview: null,
      capabilityLoading: true
    })

    expect(wrapper.text()).toContain('正在加载设备能力')
    expect(wrapper.find('.device-capability-panel-stub').exists()).toBe(false)
  })
})
