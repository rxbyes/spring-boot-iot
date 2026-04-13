import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const listVendorMetricMappingRuleSuggestions = vi.fn()
const createVendorMetricMappingRule = vi.fn()

const confirmAction = vi.fn()
const isConfirmCancelled = vi.fn()

const messageSuccess = vi.fn()
const messageError = vi.fn()
const messageWarning = vi.fn()

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
    targetNormativeIdentifier: 'value',
    targetNormativeName: '当前雨量',
    suggestionStatus: 'READY_TO_CREATE',
    evidenceCount: 3,
    confidence: 0.96,
    latestSource: 'runtime',
    ignored: false,
    covered: false,
    ...overrides
  }
}

function mockSuggestionResponse(items: Array<Record<string, unknown>>) {
  return {
    code: 200,
    data: {
      list: items,
      total: items.length
    }
  }
}

async function loadPanel() {
  return (await import('@/components/product/ProductVendorMappingSuggestionPanel.vue')).default
}

async function mountPanel(props: Record<string, unknown> = {}) {
  const ProductVendorMappingSuggestionPanel = await loadPanel()

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
    })

    const callCountAfterInitialLoad = listVendorMetricMappingRuleSuggestions.mock.calls.length

    await wrapper.get('[data-testid="vendor-mapping-suggestion-filter-covered"]').trigger('click')
    await flushPromises()

    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(callCountAfterInitialLoad + 1)
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(1001, {
      includeCovered: true,
      includeIgnored: false,
      minEvidenceCount: 1
    })
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
    })
    const callCountAfterRefresh = listVendorMetricMappingRuleSuggestions.mock.calls.length

    await wrapper.setProps({ productId: 2002 })
    await flushPromises()

    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenCalledTimes(callCountAfterRefresh + 1)
    expect(listVendorMetricMappingRuleSuggestions).toHaveBeenLastCalledWith(2002, {
      includeCovered: true,
      includeIgnored: false,
      minEvidenceCount: 1
    })
  })

  it('creates a PRODUCT plus DRAFT rule from READY_TO_CREATE and emits accepted', async () => {
    const suggestion = buildSuggestion()
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(mockSuggestionResponse([suggestion]))

    const wrapper = await mountPanel()

    await wrapper.get('[data-testid="vendor-mapping-suggestion-accept-suggestion-ready"]').trigger('click')
    await flushPromises()

    expect(createVendorMetricMappingRule).toHaveBeenCalledWith(1001, expect.objectContaining({
      rawIdentifier: 'vendor.value',
      targetNormativeIdentifier: 'value',
      scopeType: 'PRODUCT',
      lifecycleStatus: 'DRAFT'
    }))
    expect(messageSuccess).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('accepted')).toEqual([[suggestion]])
  })

  it('requires confirmation before creating a LOW_CONFIDENCE suggestion', async () => {
    let resolveConfirmation: (() => void) | undefined
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(
      mockSuggestionResponse([
        buildSuggestion({
          id: 'suggestion-low-confidence',
          suggestionStatus: 'LOW_CONFIDENCE',
          confidence: 0.42
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
    expect(createVendorMetricMappingRule).not.toHaveBeenCalled()

    resolveConfirmation?.()
    await acceptPromise
    await flushPromises()

    expect(createVendorMetricMappingRule).toHaveBeenCalledWith(1001, expect.objectContaining({
      rawIdentifier: 'vendor.value',
      targetNormativeIdentifier: 'value',
      scopeType: 'PRODUCT',
      lifecycleStatus: 'DRAFT'
    }))
    expect(wrapper.emitted('accepted')?.[0]?.[0]).toEqual(
      expect.objectContaining({
        id: 'suggestion-low-confidence',
        suggestionStatus: 'LOW_CONFIDENCE'
      })
    )
  })

  it('does not render an accept action for CONFLICTS_WITH_EXISTING suggestions', async () => {
    listVendorMetricMappingRuleSuggestions.mockResolvedValueOnce(
      mockSuggestionResponse([
        buildSuggestion({
          id: 'suggestion-conflict',
          suggestionStatus: 'CONFLICTS_WITH_EXISTING'
        })
      ])
    )

    const wrapper = await mountPanel()

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
})
