import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProductVendorMappingSuggestionPanel from '@/components/product/ProductVendorMappingSuggestionPanel.vue'

const {
  listVendorMetricMappingRuleSuggestions,
  createVendorMetricMappingRule,
  confirmAction,
  isConfirmCancelled,
  messageSuccess,
  messageError,
  messageWarning
} = vi.hoisted(() => ({
  listVendorMetricMappingRuleSuggestions: vi.fn(),
  createVendorMetricMappingRule: vi.fn(),
  confirmAction: vi.fn(),
  isConfirmCancelled: vi.fn(),
  messageSuccess: vi.fn(),
  messageError: vi.fn(),
  messageWarning: vi.fn()
}))

vi.mock(
  '@/api/vendorMetricMappingRule',
  () => ({
    listVendorMetricMappingRuleSuggestions,
    createVendorMetricMappingRule
  })
)

vi.mock('@/utils/confirm', () => ({ confirmAction, isConfirmCancelled }))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')

  return {
    ...actual,
    ElMessage: {
      success: messageSuccess,
      error: messageError,
      warning: messageWarning
    }
  }
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  template: '<section class="panel-card-stub"><slot /></section>'
})

function buildSuggestion(overrides: Record<string, unknown> = {}) {
  return {
    id: 'suggestion-ready',
    rawIdentifier: 'vendor.value',
    logicalChannelCode: 'L1_LF_1',
    targetNormativeIdentifier: 'value',
    status: 'READY_TO_CREATE',
    evidenceCount: 3,
    confidence: '0.96',
    sampleValue: '0.2136',
    reason: '运行态证据与规范字段稳定命中',
    ...overrides
  }
}

function mockSuggestionResponse(items: Array<Record<string, unknown>>) {
  return {
    code: 200,
    data: items
  }
}

async function mountPanel(props: Record<string, unknown> = {}) {
  const wrapper = mount(ProductVendorMappingSuggestionPanel, {
    props: {
      productId: 1001,
      refreshToken: 0,
      ...props
    },
    global: {
      stubs: {
        PanelCard: PanelCardStub,
        ElButton: false,
        ElCheckbox: false,
        ElEmpty: true,
        ElInputNumber: false,
        ElScrollbar: true,
        ElSkeleton: true,
        ElTag: false
      }
    }
  })

  await flushPromises()
  return wrapper
}

describe('ProductVendorMappingSuggestionPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listVendorMetricMappingRuleSuggestions.mockResolvedValue(mockSuggestionResponse([]))
    createVendorMetricMappingRule.mockResolvedValue({
      code: 200,
      data: {
        id: 9001
      }
    })
    confirmAction.mockResolvedValue(undefined)
    isConfirmCancelled.mockReturnValue(false)
  })

  it('loads with the default filters and re-requests when covered, refreshToken, and productId change', async () => {
    const wrapper = await mountPanel()
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(1)
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledWith(1001, {
      includeCovered: false,
      includeIgnored: false,
      minEvidenceCount: 1
    }, expect.objectContaining({
      suppressErrorToast: true,
      signal: expect.any(Object)
    }))

    const callCountAfterInitialLoad = listVendorMetricMappingRuleSuggestions.mock.calls.length

    await wrapper.get('[data-testid="vendor-mapping-suggestion-filter-covered"]').trigger('click')
    await flushPromises()

    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(callCountAfterInitialLoad + 1)
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(1001, {
      includeCovered: true,
      includeIgnored: false,
      minEvidenceCount: 1
    }, expect.objectContaining({
      suppressErrorToast: true,
      signal: expect.any(Object)
    }))
    const callCountAfterCoveredToggle = listVendorMetricMappingRuleSuggestions.mock.calls.length

    await wrapper.setProps({ refreshToken: 1 })
    await flushPromises()

    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(
      callCountAfterCoveredToggle + 1
    )
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(1001, {
      includeCovered: true,
      includeIgnored: false,
      minEvidenceCount: 1
    }, expect.objectContaining({
      suppressErrorToast: true,
      signal: expect.any(Object)
    }))
    const callCountAfterRefresh = listVendorMetricMappingRuleSuggestions.mock.calls.length

    await wrapper.setProps({ productId: 2002 })
    await flushPromises()

    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(callCountAfterRefresh + 1)
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(2002, {
      includeCovered: true,
      includeIgnored: false,
      minEvidenceCount: 1
    }, expect.objectContaining({
      suppressErrorToast: true,
      signal: expect.any(Object)
    }))
  })

  it('creates a PRODUCT plus DRAFT rule from READY_TO_CREATE and emits accepted', async () => {
    const suggestion = buildSuggestion()
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(mockSuggestionResponse([suggestion]))

    const wrapper = await mountPanel()

    await wrapper.get('[data-testid="vendor-mapping-suggestion-accept-suggestion-ready"]').trigger('click')
    await flushPromises()

    expect(createVendorMetricMappingRule).toHaveBeenCalledTimes(1)
    expect(createVendorMetricMappingRule).toHaveBeenLastCalledWith(
      1001,
      expect.objectContaining({
        rawIdentifier: 'vendor.value',
        targetNormativeIdentifier: 'value',
        scopeType: 'PRODUCT',
        status: 'DRAFT'
      })
    )
    expect(messageSuccess).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('accepted')).toEqual([[suggestion]])
  })

  it('requires confirmation before creating a LOW_CONFIDENCE suggestion', async () => {
    let resolveConfirmation: (() => void) | undefined
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(
      mockSuggestionResponse([
        buildSuggestion({
          id: 'suggestion-low-confidence',
          status: 'LOW_CONFIDENCE',
          confidence: '0.42',
          reason: '证据次数较少，需要人工确认'
        })
      ])
    )
    confirmAction.mockImplementationOnce(
      () =>
        new Promise<void>((resolve) => {
          resolveConfirmation = resolve
        })
    )

    const wrapper = await mountPanel()

    const acceptPromise = wrapper
      .get('[data-testid="vendor-mapping-suggestion-accept-suggestion-low-confidence"]')
      .trigger('click')
    await flushPromises()

    expect(confirmAction).toHaveBeenCalledTimes(1)
    expect(confirmAction).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '采纳低置信度建议',
        confirmButtonText: '继续创建草稿'
      })
    )
    expect(createVendorMetricMappingRule).not.toHaveBeenCalled()

    resolveConfirmation?.()
    await acceptPromise
    await flushPromises()

    expect(createVendorMetricMappingRule).toHaveBeenCalledTimes(1)
    expect(createVendorMetricMappingRule).toHaveBeenLastCalledWith(
      1001,
      expect.objectContaining({
        rawIdentifier: 'vendor.value',
        targetNormativeIdentifier: 'value',
        scopeType: 'PRODUCT',
        status: 'DRAFT'
      })
    )
    expect(wrapper.emitted('accepted')?.[0]?.[0]).toEqual(
      expect.objectContaining({
        id: 'suggestion-low-confidence',
        status: 'LOW_CONFIDENCE'
      })
    )
  })

  it('does not render an accept action for CONFLICTS_WITH_EXISTING suggestions', async () => {
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(
      mockSuggestionResponse([
        buildSuggestion({
          id: 'suggestion-conflict',
          rawIdentifier: 'L1_QJ_1.angle',
          logicalChannelCode: 'L1_QJ_1',
          status: 'CONFLICTS_WITH_EXISTING',
          sampleValue: '82.2744',
          reason: '当前产品已有命中规则',
          existingRuleId: 91,
          existingTargetNormativeIdentifier: 'sensor_state'
        })
      ])
    )

    const wrapper = await mountPanel()

    expect(wrapper.text()).toContain('L1_QJ_1')
    expect(wrapper.text()).toContain('82.2744')
    expect(wrapper.text()).toContain('当前产品已有命中规则')
    expect(wrapper.text()).toContain('existingRuleId=91')
    expect(
      wrapper.find('[data-testid="vendor-mapping-suggestion-accept-suggestion-conflict"]').exists()
    ).toBe(false)
  })

  it('does not show a duplicate error toast for handled request errors', async () => {
    const handledError = Object.assign(new Error('request already handled'), { handled: true })

    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(
      mockSuggestionResponse([buildSuggestion()])
    )
    createVendorMetricMappingRule.mockRejectedValueOnce(handledError)

    const wrapper = await mountPanel()

    await wrapper.get('[data-testid="vendor-mapping-suggestion-accept-suggestion-ready"]').trigger('click')
    await flushPromises()

    expect(messageError).not.toHaveBeenCalled()
  })

  it('renders inline load errors and retries without stale-row collisions for duplicate raw identifiers', async () => {
    listVendorMetricMappingRuleSuggestions
      .mockRejectedValueOnce(new Error('加载失败'))
      .mockResolvedValueOnce(
        mockSuggestionResponse([
          buildSuggestion({
            id: null,
            rawIdentifier: 'vendor.value',
            logicalChannelCode: 'L1_A',
            targetNormativeIdentifier: 'value'
          }),
          buildSuggestion({
            id: null,
            rawIdentifier: 'vendor.value',
            logicalChannelCode: 'L1_B',
            targetNormativeIdentifier: 'sensor_state'
          })
        ])
      )

    const wrapper = await mountPanel()

    expect(wrapper.text()).toContain('映射规则建议加载失败')
    expect(wrapper.text()).toContain('加载失败')

    await wrapper.get('[data-testid="vendor-mapping-suggestion-retry"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('L1_A')
    expect(wrapper.text()).toContain('L1_B')

    await wrapper
      .get('[data-testid="vendor-mapping-suggestion-accept-vendor.value::L1_B::sensor_state"]')
      .trigger('click')
    await flushPromises()

    expect(createVendorMetricMappingRule).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        rawIdentifier: 'vendor.value',
        logicalChannelCode: 'L1_B',
        targetNormativeIdentifier: 'sensor_state'
      })
    )
  })
})
