import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createRequestError } from '@/api/request'
import ProductRuntimeMetricDisplayRulePanel from '@/components/product/ProductRuntimeMetricDisplayRulePanel.vue'

const {
  mockListRuntimeMetricDisplayRules,
  mockCreateRuntimeMetricDisplayRule,
  mockUpdateRuntimeMetricDisplayRule,
  mockMessageSuccess,
  mockMessageError
} = vi.hoisted(() => ({
  mockListRuntimeMetricDisplayRules: vi.fn(),
  mockCreateRuntimeMetricDisplayRule: vi.fn(),
  mockUpdateRuntimeMetricDisplayRule: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn()
}))

vi.mock('@/api/runtimeMetricDisplayRule', () => ({
  listRuntimeMetricDisplayRules: mockListRuntimeMetricDisplayRules,
  createRuntimeMetricDisplayRule: mockCreateRuntimeMetricDisplayRule,
  updateRuntimeMetricDisplayRule: mockUpdateRuntimeMetricDisplayRule
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: mockMessageSuccess,
    error: mockMessageError
  }
}))

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountPanel() {
  return mount(ProductRuntimeMetricDisplayRulePanel, {
    props: {
      productId: 1001
    },
    global: {
      stubs: {
        StandardButton: StandardButtonStub
      }
    }
  })
}

describe('ProductRuntimeMetricDisplayRulePanel', () => {
  beforeEach(() => {
    mockListRuntimeMetricDisplayRules.mockReset()
    mockCreateRuntimeMetricDisplayRule.mockReset()
    mockUpdateRuntimeMetricDisplayRule.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()

    mockListRuntimeMetricDisplayRules.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 100,
        records: [
          {
            id: 8101,
            productId: 1001,
            scopeType: 'PRODUCT',
            rawIdentifier: 'S1_ZT_1.humidity',
            displayName: '相对湿度',
            unit: '%RH',
            status: 'ACTIVE',
            versionNo: 3
          },
          {
            id: 8102,
            productId: 1001,
            scopeType: 'DEVICE_FAMILY',
            scenarioCode: 'phase4-rain-gauge',
            deviceFamily: 'rain_gauge',
            rawIdentifier: 'L3_YL_1.value',
            displayName: '当前雨量',
            unit: 'mm',
            status: 'DISABLED',
            versionNo: 2
          }
        ]
      }
    })
    mockCreateRuntimeMetricDisplayRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 8103
      }
    })
    mockUpdateRuntimeMetricDisplayRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 8102
      }
    })
  })

  it('renders runtime display rules and scope signature details', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    expect(mockListRuntimeMetricDisplayRules).toHaveBeenCalledWith(1001, {
      pageNum: 1,
      pageSize: 100
    })
    expect(wrapper.text()).toContain('相对湿度')
    expect(wrapper.text()).toContain('%RH')
    expect(wrapper.text()).toContain('设备族级')
    expect(wrapper.text()).toContain('phase4-rain-gauge / rain_gauge')
  })

  it('creates a product-scoped runtime display rule with trimmed payload', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').setValue('  S1_ZT_1.temperature  ')
    await wrapper.get('[data-testid="runtime-display-rule-display-name"]').setValue(' 环境温度 ')
    await wrapper.get('[data-testid="runtime-display-rule-unit"]').setValue(' ℃ ')
    await wrapper.get('[data-testid="runtime-display-rule-submit"]').trigger('click')
    await flushPromises()

    expect(mockCreateRuntimeMetricDisplayRule).toHaveBeenCalledWith(1001, {
      scopeType: 'PRODUCT',
      rawIdentifier: 'S1_ZT_1.temperature',
      displayName: '环境温度',
      unit: '℃',
      status: 'ACTIVE',
      scenarioCode: null,
      deviceFamily: null,
      protocolCode: null
    })
    expect(mockMessageSuccess).toHaveBeenCalledWith('运行态名称/单位治理规则已新增')
  })

  it('loads row data into the form and updates a device-family rule', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="runtime-display-rule-edit-8102"]').trigger('click')
    await flushPromises()

    expect((wrapper.get('[data-testid="runtime-display-rule-scope-type"]').element as HTMLSelectElement).value).toBe('DEVICE_FAMILY')
    expect((wrapper.get('[data-testid="runtime-display-rule-scenario-code"]').element as HTMLInputElement).value).toBe('phase4-rain-gauge')
    expect((wrapper.get('[data-testid="runtime-display-rule-device-family"]').element as HTMLInputElement).value).toBe('rain_gauge')

    await wrapper.get('[data-testid="runtime-display-rule-display-name"]').setValue('小时雨量')
    await wrapper.get('[data-testid="runtime-display-rule-unit"]').setValue('mm/h')
    await wrapper.get('[data-testid="runtime-display-rule-status"]').setValue('ACTIVE')
    await wrapper.get('[data-testid="runtime-display-rule-submit"]').trigger('click')
    await flushPromises()

    expect(mockUpdateRuntimeMetricDisplayRule).toHaveBeenCalledWith(1001, 8102, {
      scopeType: 'DEVICE_FAMILY',
      rawIdentifier: 'L3_YL_1.value',
      displayName: '小时雨量',
      unit: 'mm/h',
      status: 'ACTIVE',
      scenarioCode: 'phase4-rain-gauge',
      deviceFamily: 'rain_gauge',
      protocolCode: null
    })
    expect(mockMessageSuccess).toHaveBeenCalledWith('运行态名称/单位治理规则已更新')
  })

  it('does not show a second toast for handled creation errors', async () => {
    mockCreateRuntimeMetricDisplayRule.mockRejectedValueOnce(
      createRequestError('运行态规则冲突', true, 400, '运行态规则冲突')
    )
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').setValue('S1_ZT_1.signal_4g')
    await wrapper.get('[data-testid="runtime-display-rule-display-name"]').setValue('4G 信号强度')
    await wrapper.get('[data-testid="runtime-display-rule-submit"]').trigger('click')
    await flushPromises()

    expect(mockMessageError).not.toHaveBeenCalled()
  })
})
