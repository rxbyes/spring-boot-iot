import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createRequestError } from '@/api/request'
import RiskPointBindingMaintenanceDrawer from '@/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue'
import { ElMessage } from '@/utils/message'

const {
  mockBindDevice,
  mockBindDeviceCapability,
  mockListBindableDevices,
  mockListBindingGroups,
  mockListFormalBindingMetricOptions,
  mockRemoveBinding,
  mockReplaceBinding,
  mockUnbindDevice,
  mockConfirmAction,
  mockIsConfirmCancelled
} = vi.hoisted(() => ({
  mockBindDevice: vi.fn(),
  mockBindDeviceCapability: vi.fn(),
  mockListBindableDevices: vi.fn(),
  mockListBindingGroups: vi.fn(),
  mockListFormalBindingMetricOptions: vi.fn(),
  mockRemoveBinding: vi.fn(),
  mockReplaceBinding: vi.fn(),
  mockUnbindDevice: vi.fn(),
  mockConfirmAction: vi.fn(),
  mockIsConfirmCancelled: vi.fn(() => false)
}))

vi.mock('@/api/riskPoint', () => ({
  bindDevice: mockBindDevice,
  bindDeviceCapability: mockBindDeviceCapability,
  listBindableDevices: mockListBindableDevices,
  listBindingGroups: mockListBindingGroups,
  listFormalBindingMetricOptions: mockListFormalBindingMetricOptions,
  removeBinding: mockRemoveBinding,
  replaceBinding: mockReplaceBinding,
  unbindDevice: mockUnbindDevice
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: mockConfirmAction,
  isConfirmCancelled: mockIsConfirmCancelled
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle', 'size'],
  emits: ['update:modelValue', 'close'],
  template: `
    <section v-if="modelValue" class="standard-form-drawer-stub" :data-size="size">
      <header class="standard-form-drawer-stub__header">
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </header>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      type="button"
      class="standard-button-stub"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  props: ['confirmDisabled', 'confirmLoading'],
  emits: ['cancel', 'confirm'],
  template: `
    <footer class="standard-drawer-footer-stub">
      <button type="button" data-testid="drawer-footer-cancel" @click="$emit('cancel')">取消</button>
      <button
        type="button"
        data-testid="drawer-footer-confirm"
        :disabled="Boolean(confirmDisabled) || Boolean(confirmLoading)"
        @click="$emit('confirm')"
      >
        确认
      </button>
    </footer>
  `
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  setup(_, { slots, expose }) {
    expose({
      validate: () => Promise.resolve(true),
      clearValidate: () => undefined
    })
    return () => h('form', { class: 'el-form-stub' }, slots.default?.())
  }
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: ['label'],
  template: `
    <label class="el-form-item-stub">
      <span v-if="label" class="el-form-item-stub__label">{{ label }}</span>
      <slot />
    </label>
  `
})

const ElSelectStub = defineComponent({
  name: 'ElSelect',
  props: ['modelValue', 'placeholder', 'disabled', 'filterable', 'multiple', 'loading'],
  emits: ['update:modelValue', 'change'],
  methods: {
    normalizeValue(value: string) {
      if (value === '') {
        return ''
      }
      if (/^-?\d+$/.test(value) && Math.abs(Number(value)) <= Number.MAX_SAFE_INTEGER) {
        return Number(value)
      }
      return value
    },
    collectMultipleValues(event: Event) {
      const target = event.target as HTMLSelectElement
      return Array.from(target.selectedOptions).map((option) => this.normalizeValue(option.value))
    }
  },
  template: `
    <select
      class="el-select-stub"
      :multiple="Boolean(multiple)"
      :value="modelValue ?? ''"
      :disabled="Boolean(disabled)"
      :data-filterable="String(filterable === '' || Boolean(filterable))"
      :data-multiple="String(Boolean(multiple))"
      :data-loading="String(Boolean(loading))"
      @change="
        $emit('update:modelValue', multiple ? collectMultipleValues($event) : normalizeValue($event.target.value));
        $emit('change', multiple ? collectMultipleValues($event) : normalizeValue($event.target.value));
      "
    >
      <option value="">{{ placeholder || '请选择' }}</option>
      <slot />
    </select>
  `
})

const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: ['label', 'value'],
  template: '<option :value="value">{{ label }}</option>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: '<section class="empty-state-stub">{{ title }}{{ description }}</section>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((innerResolve, innerReject) => {
    resolve = innerResolve
    reject = innerReject
  })

  return { promise, resolve, reject }
}

function createBindingGroups() {
  return [
    {
      deviceId: 2001,
      deviceCode: 'DEV-2001',
      deviceName: '北坡一体机',
      bindingMode: 'METRIC',
      deviceCapabilityType: 'MONITORING',
      aiEventExpandable: false,
      extensionStatus: null,
      metricCount: 2,
      metrics: [
        {
          bindingId: 9001,
          riskMetricId: 6101,
          metricIdentifier: 'tiltX',
          metricName: 'X轴倾角',
          bindingSource: 'MANUAL',
          createTime: '2026-04-04 09:00:00'
        },
        {
          bindingId: 9002,
          riskMetricId: 6102,
          metricIdentifier: 'crackWidth',
          metricName: '裂缝宽度',
          bindingSource: 'PENDING_PROMOTION',
          createTime: '2026-04-04 09:10:00'
        }
      ]
    },
    {
      deviceId: 2004,
      deviceCode: 'DEV-2004',
      deviceName: '北坡视频设备',
      bindingMode: 'DEVICE_ONLY',
      deviceCapabilityType: 'VIDEO',
      aiEventExpandable: true,
      extensionStatus: 'AI_EVENT_RESERVED',
      metricCount: 0,
      metrics: []
    }
  ]
}

function createBindableDevices() {
  return [
    {
      id: 2001,
      deviceCode: 'DEV-2001',
      deviceName: '北坡一体机',
      productId: 1101,
      orgId: 7101,
      orgName: '北坡监测站'
    },
    {
      id: 2002,
      deviceCode: 'DEV-2002',
      deviceName: '南坡一体机',
      productId: 1102,
      productKey: 'monitor-tilt-v2',
      productName: '监测型倾角仪',
      orgId: 7102,
      orgName: '南坡监测站',
      deviceCapabilityType: 'MONITORING',
      supportsMetricBinding: true,
      aiEventExpandable: false
    },
    {
      id: 2003,
      deviceCode: 'DEV-2003',
      deviceName: '北坡声光告警器',
      productId: 1103,
      productKey: 'warning-horn-v1',
      productName: '预警型声光告警器',
      orgId: 7101,
      orgName: '北坡监测站',
      deviceCapabilityType: 'WARNING',
      supportsMetricBinding: false,
      aiEventExpandable: false
    },
    {
      id: 2004,
      deviceCode: 'DEV-2004',
      deviceName: '北坡视频设备',
      productId: 1104,
      productKey: 'ipc-camera-v1',
      productName: '视频摄像机',
      orgId: 7101,
      orgName: '北坡监测站',
      deviceCapabilityType: 'VIDEO',
      supportsMetricBinding: false,
      aiEventExpandable: true
    }
  ]
}

function createMetricOptions() {
  return [
    { identifier: 'tiltX', name: 'X轴倾角', dataType: 'double', riskMetricId: 6101 },
    { identifier: 'tiltY', name: 'Y轴倾角', dataType: 'double', riskMetricId: 6103 },
    { identifier: 'tiltZ', name: 'Z轴倾角', dataType: 'double', riskMetricId: 6104 }
  ]
}

function mountDrawer(propOverrides: Record<string, unknown> = {}) {
  return mount(RiskPointBindingMaintenanceDrawer, {
    props: {
      modelValue: true,
      riskPointId: 1,
      riskPointName: '北坡风险点',
      riskPointCode: 'RP-NORTH-001',
      orgName: '北坡监测站',
      pendingBindingCount: 2,
      ...propOverrides
    },
    global: {
      stubs: {
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        EmptyState: EmptyStateStub,
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElSelect: ElSelectStub,
        ElOption: ElOptionStub,
        ElTag: ElTagStub
      }
    }
  })
}

describe('RiskPointBindingMaintenanceDrawer', () => {
  beforeEach(() => {
    mockBindDevice.mockReset()
    mockBindDeviceCapability.mockReset()
    mockListBindableDevices.mockReset()
    mockListBindingGroups.mockReset()
    mockListFormalBindingMetricOptions.mockReset()
    mockRemoveBinding.mockReset()
    mockReplaceBinding.mockReset()
    mockUnbindDevice.mockReset()
    mockConfirmAction.mockReset()
    mockIsConfirmCancelled.mockReset()
    mockIsConfirmCancelled.mockReturnValue(false)

    mockListBindingGroups.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createBindingGroups()
    })
    mockListBindableDevices.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createBindableDevices()
    })
    mockListFormalBindingMetricOptions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createMetricOptions()
    })
    mockBindDevice.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockBindDeviceCapability.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockRemoveBinding.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockReplaceBinding.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockUnbindDevice.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockConfirmAction.mockResolvedValue('confirm')
  })

  it('renders grouped bindings and shows source badges', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    expect(mockListBindingGroups).toHaveBeenCalledWith(1)
    expect(mockListBindableDevices).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('北坡风险点')
    expect(wrapper.text()).toContain('RP-NORTH-001')
    expect(wrapper.text()).toContain('北坡一体机')
    expect(wrapper.text()).toContain('X轴倾角')
    expect(wrapper.text()).toContain('裂缝宽度')
    expect(wrapper.text()).toContain('北坡视频设备')
    expect(wrapper.text()).toContain('设备级正式绑定')
    expect(wrapper.text()).toContain('AI 事件扩展预留')
    expect(wrapper.text()).toContain('人工维护')
    expect(wrapper.text()).toContain('待治理转正')
    expect(wrapper.text()).toContain('目录指标 #6101')
    expect(wrapper.text()).toContain('待治理 2 条')
  })

  it('renders the maintenance content without an outer drawer wrapper when embedded mode is enabled', async () => {
    const wrapper = mountDrawer({ embedded: true })
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.standard-form-drawer-stub').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('北坡风险点')
    expect(wrapper.text()).not.toContain('RP-NORTH-001')
    expect(wrapper.text()).not.toContain('所属组织 北坡管理站')
    expect(wrapper.text()).toContain('当前正式绑定')
  })

  it('adds a formal metric binding through the drawer add form', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()
    ;(wrapper.vm as any).addForm.metricIdentifiers = ['tiltY', 'tiltZ']
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith('2002')
    expect(mockBindDevice).toHaveBeenCalledWith({
      riskPointId: 1,
      deviceId: 2002,
      deviceCode: 'DEV-2002',
      deviceName: '南坡一体机',
      metrics: [
        {
          riskMetricId: 6103,
          metricIdentifier: 'tiltY',
          metricName: 'Y轴倾角'
        },
        {
          riskMetricId: 6104,
          metricIdentifier: 'tiltZ',
          metricName: 'Z轴倾角'
        }
      ]
    })
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('does not show a second error toast when add binding failure was already handled globally', async () => {
    mockBindDevice.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', true, 500))
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()
    ;(wrapper.vm as any).addForm.metricIdentifiers = ['tiltY']
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(mockBindDevice).toHaveBeenCalled()
    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalled()
  })

  it('switches warning devices to device-only binding and skips the metric picker', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2003')
    await flushPromises()
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="binding-add-metric"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('该设备无正式测点能力，将按设备级正式绑定收口，仅参与被动处置关联。')
    expect(wrapper.text()).toContain('新增设备级正式绑定')
    expect(mockListFormalBindingMetricOptions).not.toHaveBeenCalledWith('2003')
    expect(mockBindDeviceCapability).toHaveBeenCalledWith({
      riskPointId: 1,
      deviceId: 2003,
      deviceCapabilityType: 'WARNING'
    })
    expect(mockBindDevice).not.toHaveBeenCalled()
  })

  it('shows the AI-event reserved hint for video devices and submits device-only binding', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2004')
    await flushPromises()
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="binding-add-metric"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('该设备当前按设备级正式绑定收口，并预留 AI 事件分析扩展位。')
    expect(mockBindDeviceCapability).toHaveBeenCalledWith({
      riskPointId: 1,
      deviceId: 2004,
      deviceCapabilityType: 'VIDEO'
    })
  })

  it('loads formal metric options from the risk-point API and keeps only published metrics', async () => {
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        { identifier: 'value', name: '激光测距值', dataType: 'double', riskMetricId: 6101 },
        { identifier: 'dispsX', name: 'X轴位移', dataType: 'double', riskMetricId: 6102 }
      ]
    })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()

    expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith('2002')
    const optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('激光测距值')
    expect(optionTexts).toContain('X轴位移')
  })

  it('marks the add-device selector as filterable for device-code lookup', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    expect(wrapper.get('[data-testid="binding-add-device"]').attributes('data-filterable')).toBe('true')
  })

  it('shows an explicit empty hint when the selected device has no formal catalog metrics', async () => {
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()

    expect(wrapper.text()).toContain('当前设备所属产品暂无可用于风险绑定的正式目录字段')
  })

  it('shows a loading hint instead of the empty-state hint while formal metrics are still loading', async () => {
    const metricRequest = createDeferred<{ code: number; msg: string; data: ReturnType<typeof createMetricOptions> }>()

    mockListFormalBindingMetricOptions.mockReset()
    mockListFormalBindingMetricOptions.mockReturnValueOnce(metricRequest.promise)

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await nextTick()

    expect(wrapper.get('[data-testid="binding-add-metric"]').attributes('data-loading')).toBe('true')
    expect(wrapper.text()).toContain('正在加载当前设备可绑定的正式目录测点')
    expect(wrapper.text()).not.toContain('当前设备所属产品暂无可用于风险绑定的正式目录字段')

    metricRequest.resolve({
      code: 200,
      msg: 'success',
      data: createMetricOptions()
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="binding-add-metric"]').attributes('data-loading')).toBe('false')
    expect(wrapper.text()).not.toContain('正在加载当前设备可绑定的正式目录测点')
    const optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('Y轴倾角')
  })

  it('keeps unsafe long ids as strings when loading metrics and submitting bindings', async () => {
    mockListBindingGroups.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListBindableDevices.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: '2041364367361843202',
          deviceCode: 'DEV-LONG-01',
          deviceName: '长整型设备',
          productId: '2041364367361843203',
          orgId: '7101',
          orgName: '北坡监测站'
        }
      ]
    })
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          identifier: 'tiltY',
          name: 'Y轴倾角',
          dataType: 'double',
          riskMetricId: '2041364367361843204'
        }
      ]
    })

    const wrapper = mountDrawer({
      riskPointId: '2041364367361843201'
    })
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2041364367361843202')
    await flushPromises()
    ;(wrapper.vm as any).addForm.metricIdentifiers = ['tiltY']
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith('2041364367361843202')
    expect(mockBindDevice).toHaveBeenCalledWith({
      riskPointId: '2041364367361843201',
      deviceId: '2041364367361843202',
      deviceCode: 'DEV-LONG-01',
      deviceName: '长整型设备',
      metrics: [
        {
          riskMetricId: '2041364367361843204',
          metricIdentifier: 'tiltY',
          metricName: 'Y轴倾角'
        }
      ]
    })
  })

  it('filters already bound metrics out of the add-metric selector for the same device', async () => {
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        { identifier: 'tiltX', name: 'X轴倾角', dataType: 'double' },
        { identifier: 'tiltY', name: 'Y轴倾角', dataType: 'double' },
        { identifier: 'crackWidth', name: '裂缝宽度', dataType: 'double' }
      ]
    })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2001')
    await flushPromises()

    const optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('Y轴倾角')
    expect(optionTexts).not.toContain('X轴倾角')
    expect(optionTexts).not.toContain('裂缝宽度')
  })

  it('whole-device unbind stays on the dedicated unbind API', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-unbind-device-2001"]').trigger('click')
    await flushPromises()

    expect(mockUnbindDevice).toHaveBeenCalledWith(1, 2001)
    expect(mockRemoveBinding).not.toHaveBeenCalled()
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('removes a single binding without calling whole-device unbind', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-remove-9001"]').trigger('click')
    await flushPromises()

    expect(mockRemoveBinding).toHaveBeenCalledWith(9001)
    expect(mockUnbindDevice).not.toHaveBeenCalled()
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('replaces a metric binding with the selected target metric', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-replace-open-9001"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="binding-replace-metric-9001"]').setValue('tiltZ')
    await wrapper.get('[data-testid="binding-replace-submit-9001"]').trigger('click')
    await flushPromises()

    expect(mockListFormalBindingMetricOptions).toHaveBeenCalledWith('2001')
    expect(mockReplaceBinding).toHaveBeenCalledWith(9001, {
      riskMetricId: 6104,
      metricIdentifier: 'tiltZ',
      metricName: 'Z轴倾角'
    })
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('filters the current and already bound metrics out of replace choices', async () => {
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        { identifier: 'tiltX', name: 'X轴倾角', dataType: 'double' },
        { identifier: 'tiltY', name: 'Y轴倾角', dataType: 'double' },
        { identifier: 'crackWidth', name: '裂缝宽度', dataType: 'double' }
      ]
    })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-replace-open-9001"]').trigger('click')
    await flushPromises()

    const optionTexts = wrapper
      .get('[data-testid="binding-replace-metric-9001"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('Y轴倾角')
    expect(optionTexts).not.toContain('X轴倾角')
    expect(optionTexts).not.toContain('裂缝宽度')
  })

  it('ignores stale drawer loads when the risk point changes before the first request resolves', async () => {
    const firstGroupRequest = createDeferred<{ code: number; msg: string; data: ReturnType<typeof createBindingGroups> }>()
    const firstDeviceRequest = createDeferred<{ code: number; msg: string; data: ReturnType<typeof createBindableDevices> }>()

    mockListBindingGroups.mockReset()
    mockListBindableDevices.mockReset()
    mockListBindingGroups
      .mockReturnValueOnce(firstGroupRequest.promise)
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: [
          {
            deviceId: 3001,
            deviceCode: 'DEV-3001',
            deviceName: '南坡监测终端',
            metricCount: 1,
            metrics: [
              {
                bindingId: 9101,
                metricIdentifier: 'southTilt',
                metricName: '南坡倾角',
                bindingSource: 'MANUAL',
                createTime: '2026-04-04 10:00:00'
              }
            ]
          }
        ]
      })
    mockListBindableDevices
      .mockReturnValueOnce(firstDeviceRequest.promise)
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: [
          {
            id: 3001,
            deviceCode: 'DEV-3001',
            deviceName: '南坡监测终端',
            productId: 1201,
            orgId: 7201,
            orgName: '南坡监测站'
          }
        ]
      })

    const wrapper = mountDrawer()
    await nextTick()

    await wrapper.setProps({
      riskPointId: 2,
      riskPointName: '南坡风险点',
      riskPointCode: 'RP-SOUTH-002',
      orgName: '南坡监测站'
    })
    await flushPromises()

    expect(wrapper.text()).toContain('南坡监测终端')
    expect(wrapper.text()).not.toContain('北坡一体机')

    firstGroupRequest.resolve({
      code: 200,
      msg: 'success',
      data: createBindingGroups()
    })
    firstDeviceRequest.resolve({
      code: 200,
      msg: 'success',
      data: createBindableDevices()
    })
    await flushPromises()

    expect(wrapper.text()).toContain('南坡监测终端')
    expect(wrapper.text()).not.toContain('北坡一体机')
  })

  it('keeps the latest add-device metric options when device selection changes quickly', async () => {
    const firstMetricRequest = createDeferred<{ code: number; msg: string; data: ReturnType<typeof createMetricOptions> }>()

    mockListFormalBindingMetricOptions.mockReset()
    mockListFormalBindingMetricOptions
      .mockReturnValueOnce(firstMetricRequest.promise)
      .mockResolvedValueOnce({
        code: 200,
        msg: 'success',
        data: [{ identifier: 'southTilt', name: '南坡倾角', dataType: 'double' }]
      })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2001')
    await nextTick()
    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()

    let optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('南坡倾角')
    expect(optionTexts).not.toContain('X轴倾角')

    firstMetricRequest.resolve({
      code: 200,
      msg: 'success',
      data: createMetricOptions()
    })
    await flushPromises()

    optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('南坡倾角')
    expect(optionTexts).not.toContain('X轴倾角')
  })

  it('re-filters add-metric options after binding groups finish loading late', async () => {
    const delayedGroupRequest = createDeferred<{ code: number; msg: string; data: ReturnType<typeof createBindingGroups> }>()

    mockListBindingGroups.mockReset()
    mockListBindableDevices.mockReset()
    mockListFormalBindingMetricOptions.mockReset()
    mockListBindingGroups.mockReturnValueOnce(delayedGroupRequest.promise)
    mockListBindableDevices.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: createBindableDevices()
    })
    mockListFormalBindingMetricOptions.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        { identifier: 'tiltX', name: 'X轴倾角', dataType: 'double' },
        { identifier: 'tiltY', name: 'Y轴倾角', dataType: 'double' },
        { identifier: 'crackWidth', name: '裂缝宽度', dataType: 'double' }
      ]
    })

    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2001')
    await flushPromises()

    delayedGroupRequest.resolve({
      code: 200,
      msg: 'success',
      data: createBindingGroups()
    })
    await flushPromises()

    const optionTexts = wrapper
      .get('[data-testid="binding-add-metric"]')
      .findAll('option')
      .map((node) => node.text())

    expect(optionTexts).toContain('Y轴倾角')
    expect(optionTexts).not.toContain('X轴倾角')
    expect(optionTexts).not.toContain('裂缝宽度')
  })

  it('emits close only once when the drawer close sequence fires both events', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    ;(wrapper.vm as any).handleModelValueChange(false)
    ;(wrapper.vm as any).handleClose()

    expect(wrapper.emitted('close')).toHaveLength(1)
    expect(wrapper.emitted('update:modelValue')).toHaveLength(1)
  })
})
