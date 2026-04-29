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
  mockMessageError,
  mockRoute
} = vi.hoisted(() => ({
  mockListRuntimeMetricDisplayRules: vi.fn(),
  mockCreateRuntimeMetricDisplayRule: vi.fn(),
  mockUpdateRuntimeMetricDisplayRule: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn(),
  mockRoute: {
    query: {} as Record<string, unknown>
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute
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

function mountPanel(props?: Record<string, unknown>) {
  return mount(ProductRuntimeMetricDisplayRulePanel, {
    props: {
      productId: 1001,
      formalPropertyIdentifiers: [],
      ...props
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
    mockRoute.query = {}

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

  it('adopts a route-query candidate into the form and shows static preview warnings', async () => {
    mockRoute.query = {
      rawIdentifier: 'S1_ZT_1.humidity',
      displayName: '相对湿度',
      unit: '%RH',
      deviceCode: 'DEVICE-001',
      runtimeGovernanceDraft: '1',
      source: 'insight'
    }
    const wrapper = mountPanel({
      formalPropertyIdentifiers: ['value']
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('待治理候选')
    expect(wrapper.text()).toContain('DEVICE-001')

    await wrapper.get('[data-testid="runtime-display-rule-candidate-adopt"]').trigger('click')

    expect((wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').element as HTMLInputElement).value).toBe(
      'S1_ZT_1.humidity'
    )
    expect((wrapper.get('[data-testid="runtime-display-rule-display-name"]').element as HTMLInputElement).value).toBe(
      '相对湿度'
    )
    expect((wrapper.get('[data-testid="runtime-display-rule-unit"]').element as HTMLInputElement).value).toBe('%RH')

    const preview = wrapper.get('[data-testid="runtime-display-rule-preview"]')
    expect(preview.text()).toContain('设备属性快照')
    expect(preview.text()).toContain('历史趋势')
    expect(preview.text()).toContain('对象洞察')
    expect(preview.text()).toContain('产品级')
    expect(preview.text()).toContain('已存在同范围治理规则')
  })

  it('marks rules covered by formal property identifiers and quick-disables them', async () => {
    mockListRuntimeMetricDisplayRules.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 100,
        records: [
          {
            id: 9001,
            productId: 1001,
            scopeType: 'PRODUCT',
            rawIdentifier: 'value',
            displayName: '裂缝值',
            unit: 'mm',
            status: 'ACTIVE',
            versionNo: 3
          }
        ]
      }
    })

    const wrapper = mountPanel({
      formalPropertyIdentifiers: ['value']
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('已被正式字段覆盖')

    await wrapper.get('[data-testid="runtime-display-rule-disable-9001"]').trigger('click')
    await flushPromises()

    expect(mockUpdateRuntimeMetricDisplayRule).toHaveBeenCalledWith(1001, 9001, {
      scopeType: 'PRODUCT',
      rawIdentifier: 'value',
      displayName: '裂缝值',
      unit: 'mm',
      status: 'DISABLED',
      scenarioCode: null,
      deviceFamily: null,
      protocolCode: null
    })
  })

  it('prefills the raw identifier when contract compare focuses runtime display governance', async () => {
    const wrapper = mountPanel({
      focusRawIdentifier: 'L4_NW_1',
      focusToken: 1
    })

    await flushPromises()
    await flushPromises()

    expect((wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').element as HTMLInputElement).value).toBe(
      'L4_NW_1'
    )
    expect(wrapper.get('[data-testid="runtime-display-rule-form-message"]').text()).toContain('已带入原始字段')

    await wrapper.get('[data-testid="runtime-display-rule-display-name"]').setValue('泥水位')
    await wrapper.setProps({
      focusRawIdentifier: 'L4_LD_1.speed',
      focusToken: 2
    })
    await flushPromises()

    expect((wrapper.get('[data-testid="runtime-display-rule-raw-identifier"]').element as HTMLInputElement).value).toBe(
      'L4_LD_1.speed'
    )
    expect((wrapper.get('[data-testid="runtime-display-rule-display-name"]').element as HTMLInputElement).value).toBe('')
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
