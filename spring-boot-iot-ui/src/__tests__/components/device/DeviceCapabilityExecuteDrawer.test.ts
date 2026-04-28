import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'

import DeviceCapabilityExecuteDrawer from '@/components/device/DeviceCapabilityExecuteDrawer.vue'
import type { DeviceCapability } from '@/types/api'

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle', 'size'],
  emits: ['update:modelValue', 'close'],
  template: `
    <section class="standard-form-drawer-stub" :data-visible="String(modelValue)">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  props: ['confirmText', 'confirmLoading'],
  emits: ['cancel', 'confirm'],
  template: `
    <div class="standard-drawer-footer-stub">
      <button class="standard-drawer-footer-stub__cancel" type="button" @click="$emit('cancel')">取消</button>
      <button class="standard-drawer-footer-stub__confirm" type="button" @click="$emit('confirm')">
        {{ confirmText || '确认' }}
      </button>
    </div>
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

const ElFormStub = defineComponent({
  name: 'ElForm',
  methods: {
    validate() {
      return Promise.resolve(true)
    },
    clearValidate() {
      return undefined
    }
  },
  template: '<form class="el-form-stub"><slot /></form>'
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: ['label', 'prop'],
  template: `
    <div class="el-form-item-stub" :data-label="label" :data-prop="prop">
      <slot />
    </div>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue', 'clear'],
  template: `
    <input
      class="el-input-stub"
      :placeholder="placeholder"
      :value="modelValue ?? ''"
      @input="$emit('update:modelValue', $event.target?.value ?? '')"
    />
  `
})

const ElInputNumberStub = defineComponent({
  name: 'ElInputNumber',
  props: ['modelValue', 'min', 'max'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-input-number-stub"
      type="number"
      :value="modelValue ?? ''"
      @input="$emit('update:modelValue', $event.target?.value === '' ? undefined : Number($event.target?.value))"
    />
  `
})

function mountDrawer(capability: DeviceCapability | null = null) {
  return mount(DeviceCapabilityExecuteDrawer, {
    props: {
      modelValue: true,
      deviceCode: '6260370286',
      capability
    },
    global: {
      stubs: {
        StandardFormDrawer: StandardFormDrawerStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardInlineState: StandardInlineStateStub,
        StandardButton: StandardButtonStub,
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElInputNumber: ElInputNumberStub
      }
    }
  })
}

function buildStorageKey(capabilityCode: string) {
  return `iot:device-capability-execute:last-params:6260370286:${capabilityCode}`
}

function buildFavoriteStorageKey(capabilityCode: string) {
  return `iot:device-capability-execute:favorites:6260370286:${capabilityCode}`
}

describe('DeviceCapabilityExecuteDrawer', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('renders an identity-first summary for schema-driven capability execution', () => {
    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true },
        note: { type: 'string', label: '备注', required: false }
      }
    })

    expect(wrapper.text()).toContain('参数模板')
    expect(wrapper.text()).toContain('空白模板')
    expect(wrapper.text()).toContain('保守模板')
    expect(wrapper.text()).toContain('演示模板')
    expect(wrapper.text()).toContain('设备编码')
    expect(wrapper.text()).toContain('6260370286')
    expect(wrapper.html()).toContain('device-capability-execute-drawer__summary-card--identity')
    expect(wrapper.html()).toContain('device-capability-execute-drawer__summary-meta')
    expect(wrapper.findAll('input.el-input-stub')).toHaveLength(2)
    expect(wrapper.find('input.el-input-number-stub').exists()).toBe(true)
  })

  it('restores the latest draft and exposes the recent template entry', () => {
    window.localStorage.setItem(
      buildStorageKey('broadcast_play'),
      JSON.stringify({
        volume: 22,
        content: '夜间广播',
        note: '保持静音'
      })
    )

    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true },
        note: { type: 'string', label: '备注', required: false }
      }
    })

    expect(wrapper.text()).toContain('最近一次参数')
    expect(wrapper.text()).toContain('已自动回填最近一次参数草稿')
    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '22')
    expect(wrapper.findAll('input.el-input-stub')[0].element).toHaveProperty('value', '夜间广播')
    expect(wrapper.findAll('input.el-input-stub')[1].element).toHaveProperty('value', '保持静音')
  })

  it('restores favorite commands and applies them back into the form', async () => {
    window.localStorage.setItem(
      buildFavoriteStorageKey('broadcast_play'),
      JSON.stringify([
        {
          signature: '{"volume":18,"content":"夜间广播","note":""}',
          label: '音量 18 · 播报内容 夜间广播',
          alias: '夜间播报',
          pinned: true,
          params: {
            volume: 18,
            content: '夜间广播',
            note: ''
          },
          updatedAt: '2026-04-24T10:00:00.000Z'
        }
      ])
    )

    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true },
        note: { type: 'string', label: '备注', required: false }
      }
    })

    expect(wrapper.text()).toContain('常用命令')
    expect(wrapper.text()).toContain('夜间播报')
    expect(wrapper.text()).toContain('已置顶')

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '回填')!.trigger('click')
    await nextTick()

    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '18')
    expect(wrapper.findAll('input.el-input-stub')[0].element).toHaveProperty('value', '夜间广播')
  })

  it('renames, pins and deletes favorite commands', async () => {
    window.localStorage.setItem(
      buildFavoriteStorageKey('broadcast_play'),
      JSON.stringify([
        {
          signature: '{"volume":18,"content":"夜间广播","note":""}',
          label: '音量 18 · 播报内容 夜间广播',
          params: {
            volume: 18,
            content: '夜间广播',
            note: ''
          },
          updatedAt: '2026-04-24T10:00:00.000Z'
        }
      ])
    )

    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true },
        note: { type: 'string', label: '备注', required: false }
      }
    })

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '重命名')!.trigger('click')
    await nextTick()

    const renameInput = wrapper.get('input.el-input-stub')
    await renameInput.setValue('夜间巡检')
    await nextTick()

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '保存名称')!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('夜间巡检')
    expect(JSON.parse(window.localStorage.getItem(buildFavoriteStorageKey('broadcast_play')) || '[]')[0]).toMatchObject({
      alias: '夜间巡检'
    })

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '置顶')!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('已置顶')
    expect(JSON.parse(window.localStorage.getItem(buildFavoriteStorageKey('broadcast_play')) || '[]')[0]).toMatchObject({
      pinned: true
    })

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '删除')!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('收藏一次常用参数后，可在这里直接回填。')
    expect(JSON.parse(window.localStorage.getItem(buildFavoriteStorageKey('broadcast_play')) || '[]')).toHaveLength(0)
  })

  it('shows capability specific presets for broadcast commands', async () => {
    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true }
      }
    })

    expect(wrapper.text()).toContain('能力专用预设')
    expect(wrapper.text()).toContain('应急播报')
    expect(wrapper.text()).toContain('巡检播报')

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '应急播报')!.trigger('click')
    await nextTick()

    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '80')
    expect(String(wrapper.findAll('input.el-input-stub')[0].element.value)).toContain('设备异常')
  })

  it('shows capability specific presets for ptz commands', async () => {
    const wrapper = mountDrawer({
      code: 'video_ptz_move',
      name: '云台控制',
      group: '视频控制',
      paramsSchema: {
        direction: { type: 'string', label: '方向', required: true },
        speed: { type: 'integer', label: '速度', required: true, min: 1, max: 5 }
      }
    })

    expect(wrapper.text()).toContain('左转预设')
    expect(wrapper.text()).toContain('右转预设')
    expect(wrapper.text()).toContain('停止预设')

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '左转预设')!.trigger('click')
    await nextTick()

    expect(wrapper.findAll('input.el-input-stub')[0].element).toHaveProperty('value', 'left')
    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '3')
  })

  it('fills conservative and demo template values into the form', async () => {
    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true },
        note: { type: 'string', label: '备注', required: false }
      }
    })

    const buttons = wrapper.findAll('button.standard-button-stub')
    await buttons.find((button) => button.text() === '保守模板')!.trigger('click')
    await nextTick()

    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '10')
    expect(wrapper.findAll('input.el-input-stub')[0].element).toHaveProperty('value', '播报内容')
    expect(wrapper.findAll('input.el-input-stub')[1].element).toHaveProperty('value', '')

    await buttons.find((button) => button.text() === '演示模板')!.trigger('click')
    await nextTick()

    expect(wrapper.get('input.el-input-number-stub').element).toHaveProperty('value', '45')
    expect(String(wrapper.findAll('input.el-input-stub')[0].element.value)).toContain('播放内容-播报内容-示例')
  })

  it('persists the last submitted params before emitting submit', async () => {
    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true }
      }
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('33')
    await inputs[1].setValue('现场广播')
    await nextTick()

    const submitButton = wrapper.get('button.standard-drawer-footer-stub__confirm')
    expect(submitButton).toBeTruthy()
    await submitButton.trigger('click')

    expect(wrapper.emitted('submit')?.[0]?.[0]).toMatchObject({
      params: {
        volume: 33,
        content: '现场广播'
      }
    })
    expect(JSON.parse(window.localStorage.getItem(buildStorageKey('broadcast_play')) || '{}')).toMatchObject({
      volume: 33,
      content: '现场广播'
    })
  })

  it('saves the current params as a favorite command', async () => {
    const wrapper = mountDrawer({
      code: 'broadcast_play',
      name: '播放内容',
      group: '广播预警',
      paramsSchema: {
        volume: { type: 'integer', label: '音量', required: true, min: 10, max: 80 },
        content: { type: 'string', label: '播报内容', required: true }
      }
    })

    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('24')
    await inputs[1].setValue('山体预警')
    await nextTick()

    await wrapper.findAll('button.standard-button-stub').find((button) => button.text() === '收藏当前参数')!.trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('音量 24 · 播报内容 山体预警')
    expect(JSON.parse(window.localStorage.getItem(buildFavoriteStorageKey('broadcast_play')) || '[]')).toMatchObject([
      {
        label: '音量 24 · 播报内容 山体预警',
        params: {
          volume: 24,
          content: '山体预警'
        }
      }
    ])
  })

  it('shows the no-parameter hint when the capability schema is empty', () => {
    const wrapper = mountDrawer({
      code: 'reboot',
      name: '重启',
      group: '基础维护',
      paramsSchema: {}
    })

    expect(wrapper.text()).toContain('当前能力无需额外参数，直接下发即可。')
    expect(wrapper.text()).not.toContain('参数模板')
  })
})
