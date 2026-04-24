import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import DeviceCapabilityPanel from '@/components/device/DeviceCapabilityPanel.vue'
import type { CommandRecordPageItem, DeviceCapabilityOverview } from '@/types/api'

let currentCommands: CommandRecordPageItem[] = []

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: '<div class="standard-table-toolbar-stub"><slot /><slot name="right" /></div>'
})

const ElEmptyStub = defineComponent({
  name: 'ElEmpty',
  props: ['description'],
  template: '<div class="el-empty-stub">{{ description }}</div>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  props: ['type', 'round'],
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  setup(_, { slots }) {
    return () => h('section', { class: 'el-table-stub' }, slots.default?.())
  }
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'prop'],
  setup(props, { slots }) {
    return () =>
      h('section', { class: 'el-table-column-stub', 'data-label': props.label }, [
        slots.default?.({ row: currentCommands[0] || {} }) ||
          h('span', { class: 'el-table-column-stub__value' }, String((currentCommands[0] || {})[props.prop as string] ?? ''))
      ])
  }
})

function mountPanel(overview: DeviceCapabilityOverview, commands: CommandRecordPageItem[] = []) {
  currentCommands = commands
  return mount(DeviceCapabilityPanel, {
    props: {
      overview,
      commands
    },
    global: {
      stubs: {
        StandardButton: StandardButtonStub,
        StandardInlineState: StandardInlineStateStub,
        StandardTableToolbar: StandardTableToolbarStub,
        ElEmpty: ElEmptyStub,
        ElTag: ElTagStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('DeviceCapabilityPanel', () => {
  it('groups capabilities by product capability type and emits execute from enabled actions', async () => {
    const overview: DeviceCapabilityOverview = {
      deviceCode: 'DEV-001',
      productId: 1001,
      productKey: 'warning-broadcast-v1',
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
          paramsSchema: {
            content: { type: 'string', label: '播报内容', required: true }
          }
        },
        {
          code: 'reboot',
          name: '重启',
          group: '基础维护',
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
        status: 'SUCCESS',
        sendTime: '2026-04-24T10:50:00',
        ackTime: '2026-04-24T10:50:05',
        topic: '/iot/broadcast/DEV-001',
        errorMessage: ''
      }
    ]

    const wrapper = mountPanel(overview, commands)

    expect(wrapper.text()).toContain('设备能力与命令')
    expect(wrapper.text()).toContain('预警型')
    expect(wrapper.text()).toContain('广播喇叭')
    expect(wrapper.text()).toContain('广播预警')
    expect(wrapper.text()).toContain('基础维护')
    expect(wrapper.text()).toContain('播放内容')
    expect(wrapper.text()).toContain('CMD-001')
    expect(wrapper.text()).toContain('执行成功')

    const playButton = wrapper
      .findAll('button.standard-button-stub')
      .find((button) => button.text() === '播放内容')
    expect(playButton).toBeTruthy()

    await playButton!.trigger('click')

    expect(wrapper.emitted('execute')?.[0]?.[0]).toMatchObject({
      code: 'broadcast_play',
      name: '播放内容'
    })
  })

  it('shows the disabled reason when the overview is not executable', () => {
    const overview: DeviceCapabilityOverview = {
      deviceCode: 'DEV-002',
      productCapabilityType: 'VIDEO',
      subType: 'PTZ_CAMERA',
      onlineExecutable: false,
      disabledReason: '设备离线，当前能力需要在线执行',
      capabilities: [
        {
          code: 'video_play',
          name: '播放视频',
          group: '视频控制',
          enabled: false,
          disabledReason: '设备离线，当前能力需要在线执行',
          requiresOnline: true,
          paramsSchema: {}
        }
      ]
    }

    const wrapper = mountPanel(overview)

    expect(wrapper.text()).toContain('设备离线，当前能力需要在线执行')
    expect(wrapper.text()).toContain('视频云台')
    expect(wrapper.get('button.standard-button-stub').attributes('title')).toBe('设备离线，当前能力需要在线执行')
  })
})
