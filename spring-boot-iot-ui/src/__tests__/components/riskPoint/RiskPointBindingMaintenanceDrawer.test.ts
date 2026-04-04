import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RiskPointBindingMaintenanceDrawer from '@/components/riskPoint/RiskPointBindingMaintenanceDrawer.vue'

const {
  mockBindDevice,
  mockListBindableDevices,
  mockListBindingGroups,
  mockRemoveBinding,
  mockReplaceBinding,
  mockUnbindDevice,
  mockGetDeviceMetricOptions,
  mockConfirmAction,
  mockIsConfirmCancelled
} = vi.hoisted(() => ({
  mockBindDevice: vi.fn(),
  mockListBindableDevices: vi.fn(),
  mockListBindingGroups: vi.fn(),
  mockRemoveBinding: vi.fn(),
  mockReplaceBinding: vi.fn(),
  mockUnbindDevice: vi.fn(),
  mockGetDeviceMetricOptions: vi.fn(),
  mockConfirmAction: vi.fn(),
  mockIsConfirmCancelled: vi.fn(() => false)
}))

vi.mock('@/api/riskPoint', () => ({
  bindDevice: mockBindDevice,
  listBindableDevices: mockListBindableDevices,
  listBindingGroups: mockListBindingGroups,
  removeBinding: mockRemoveBinding,
  replaceBinding: mockReplaceBinding,
  unbindDevice: mockUnbindDevice
}))

vi.mock('@/api/iot', () => ({
  getDeviceMetricOptions: mockGetDeviceMetricOptions
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
  props: ['modelValue', 'placeholder', 'disabled'],
  emits: ['update:modelValue', 'change'],
  methods: {
    normalizeValue(value: string) {
      if (value === '') {
        return ''
      }
      return /^-?\d+$/.test(value) ? Number(value) : value
    }
  },
  template: `
    <select
      class="el-select-stub"
      :value="modelValue ?? ''"
      :disabled="Boolean(disabled)"
      @change="
        $emit('update:modelValue', normalizeValue($event.target.value));
        $emit('change', normalizeValue($event.target.value));
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

function createBindingGroups() {
  return [
    {
      deviceId: 2001,
      deviceCode: 'DEV-2001',
      deviceName: '北坡一体机',
      metricCount: 2,
      metrics: [
        {
          bindingId: 9001,
          metricIdentifier: 'tiltX',
          metricName: 'X向倾角',
          bindingSource: 'MANUAL',
          createTime: '2026-04-04 09:00:00'
        },
        {
          bindingId: 9002,
          metricIdentifier: 'crackWidth',
          metricName: '裂缝宽度',
          bindingSource: 'PENDING_PROMOTION',
          createTime: '2026-04-04 09:10:00'
        }
      ]
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
      orgId: 7102,
      orgName: '南坡监测站'
    }
  ]
}

function createMetricOptions() {
  return [
    { identifier: 'tiltX', name: 'X向倾角', type: 'property' },
    { identifier: 'tiltY', name: 'Y向倾角', type: 'property' },
    { identifier: 'tiltZ', name: 'Z向倾角', type: 'property' }
  ]
}

function mountDrawer() {
  return mount(RiskPointBindingMaintenanceDrawer, {
    props: {
      modelValue: true,
      riskPointId: 1,
      riskPointName: '北坡风险点',
      riskPointCode: 'RP-NORTH-001',
      orgName: '北坡监测站',
      pendingBindingCount: 2
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
    mockListBindableDevices.mockReset()
    mockListBindingGroups.mockReset()
    mockRemoveBinding.mockReset()
    mockReplaceBinding.mockReset()
    mockUnbindDevice.mockReset()
    mockGetDeviceMetricOptions.mockReset()
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
    mockGetDeviceMetricOptions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createMetricOptions()
    })
    mockBindDevice.mockResolvedValue({ code: 200, msg: 'success', data: null })
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
    expect(wrapper.text()).toContain('X向倾角')
    expect(wrapper.text()).toContain('裂缝宽度')
    expect(wrapper.text()).toContain('人工维护')
    expect(wrapper.text()).toContain('待治理转正')
    expect(wrapper.text()).toContain('待治理 2 条')
  })

  it('adds a formal metric binding through the drawer add form', async () => {
    const wrapper = mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="binding-add-device"]').setValue('2002')
    await flushPromises()
    await wrapper.get('[data-testid="binding-add-metric"]').setValue('tiltY')
    await wrapper.get('[data-testid="binding-add-submit"]').trigger('click')
    await flushPromises()

    expect(mockGetDeviceMetricOptions).toHaveBeenCalledWith(2002)
    expect(mockBindDevice).toHaveBeenCalledWith({
      riskPointId: 1,
      deviceId: 2002,
      metricIdentifier: 'tiltY',
      metricName: 'Y向倾角'
    })
    expect(wrapper.emitted('updated')).toHaveLength(1)
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

    expect(mockGetDeviceMetricOptions).toHaveBeenCalledWith(2001)
    expect(mockReplaceBinding).toHaveBeenCalledWith(9001, {
      metricIdentifier: 'tiltZ',
      metricName: 'Z向倾角'
    })
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })
})
