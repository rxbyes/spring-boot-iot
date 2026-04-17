import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductVendorMappingRuleLedgerPanel from '@/components/product/ProductVendorMappingRuleLedgerPanel.vue'

const {
  mockListVendorMetricMappingRuleLedger,
  mockSubmitVendorMetricMappingRulePublish,
  mockSubmitVendorMetricMappingRuleRollback,
  mockPreviewVendorMetricMappingRuleHit,
  mockBatchUpdateVendorMetricMappingRuleStatus,
  mockReplayVendorMetricMappingRule,
  mockMessageSuccess,
  mockMessageError
} = vi.hoisted(() => ({
  mockListVendorMetricMappingRuleLedger: vi.fn(),
  mockSubmitVendorMetricMappingRulePublish: vi.fn(),
  mockSubmitVendorMetricMappingRuleRollback: vi.fn(),
  mockPreviewVendorMetricMappingRuleHit: vi.fn(),
  mockBatchUpdateVendorMetricMappingRuleStatus: vi.fn(),
  mockReplayVendorMetricMappingRule: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn()
}))

vi.mock('@/api/vendorMetricMappingRule', () => ({
  listVendorMetricMappingRuleLedger: mockListVendorMetricMappingRuleLedger,
  submitVendorMetricMappingRulePublish: mockSubmitVendorMetricMappingRulePublish,
  submitVendorMetricMappingRuleRollback: mockSubmitVendorMetricMappingRuleRollback,
  previewVendorMetricMappingRuleHit: mockPreviewVendorMetricMappingRuleHit,
  batchUpdateVendorMetricMappingRuleStatus: mockBatchUpdateVendorMetricMappingRuleStatus,
  replayVendorMetricMappingRule: mockReplayVendorMetricMappingRule
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
  return mount(ProductVendorMappingRuleLedgerPanel, {
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

describe('ProductVendorMappingRuleLedgerPanel', () => {
  beforeEach(() => {
    mockListVendorMetricMappingRuleLedger.mockReset()
    mockSubmitVendorMetricMappingRulePublish.mockReset()
    mockSubmitVendorMetricMappingRuleRollback.mockReset()
    mockPreviewVendorMetricMappingRuleHit.mockReset()
    mockBatchUpdateVendorMetricMappingRuleStatus.mockReset()
    mockReplayVendorMetricMappingRule.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()

    mockListVendorMetricMappingRuleLedger.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          ruleId: 7101,
          productId: 1001,
          rawIdentifier: 'disp',
          targetNormativeIdentifier: 'value',
          scopeType: 'PRODUCT',
          draftStatus: 'DRAFT',
          draftVersionNo: 4,
          publishedStatus: 'PUBLISHED',
          publishedVersionNo: 3,
          latestApprovalOrderId: 99001,
          publishedSource: 'published_snapshot'
        },
        {
          ruleId: 7102,
          productId: 1001,
          rawIdentifier: 'disp_x',
          targetNormativeIdentifier: 'dispsX',
          scopeType: 'PRODUCT',
          draftStatus: 'ACTIVE',
          draftVersionNo: 2,
          publishedStatus: null,
          publishedVersionNo: null,
          latestApprovalOrderId: null,
          publishedSource: 'draft_table',
          logicalChannelCode: 'L1_LF_1'
        }
      ]
    })
    mockSubmitVendorMetricMappingRulePublish.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        approvalOrderId: 99002,
        approvalStatus: 'PENDING'
      }
    })
    mockSubmitVendorMetricMappingRuleRollback.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        approvalOrderId: 99003,
        approvalStatus: 'PENDING'
      }
    })
    mockPreviewVendorMetricMappingRuleHit.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        matched: true,
        hitSource: 'published_snapshot',
        ruleId: 7101,
        rawIdentifier: 'disp',
        targetNormativeIdentifier: 'value',
        publishedVersionNo: 3,
        approvalOrderId: 99001
      }
    })
    mockBatchUpdateVendorMetricMappingRuleStatus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        requestedCount: 2,
        matchedCount: 2,
        changedCount: 2,
        targetStatus: 'DISABLED'
      }
    })
    mockReplayVendorMetricMappingRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        matched: true,
        hitSource: 'PUBLISHED_SNAPSHOT',
        matchedScopeType: 'PRODUCT',
        ruleId: 7101,
        rawIdentifier: 'disp',
        logicalChannelCode: 'L1_LF_1',
        targetNormativeIdentifier: 'value',
        canonicalIdentifier: 'value',
        sampleValue: '0.2136'
      }
    })
  })

  it('renders published snapshot state and publish action for a draft mapping rule', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('disp')
    expect(wrapper.text()).toContain('DRAFT')
    expect(wrapper.text()).toContain('PUBLISHED')
    expect(wrapper.text()).toContain('v4 / v3')
    expect(wrapper.find('[data-testid="rule-ledger-submit-publish-7101"]').exists()).toBe(true)
  })

  it('previews hit result and submits publish approval from the ledger row', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="rule-ledger-preview-hit-7101"]').trigger('click')
    await flushPromises()
    expect(mockPreviewVendorMetricMappingRuleHit).toHaveBeenCalledWith(1001, {
      rawIdentifier: 'disp',
      logicalChannelCode: undefined
    })
    expect(wrapper.text()).toContain('published_snapshot')
    expect(wrapper.text()).toContain('value')

    await wrapper.get('[data-testid="rule-ledger-submit-publish-7101"]').trigger('click')
    await flushPromises()

    expect(mockSubmitVendorMetricMappingRulePublish).toHaveBeenCalledWith(1001, 7101, expect.stringContaining('disp'))
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('supports selecting multiple rows and submitting batch status update with summary', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="rule-ledger-select-7101"]').setValue(true)
    await wrapper.get('[data-testid="rule-ledger-select-7102"]').setValue(true)
    await wrapper.get('[data-testid="rule-ledger-batch-status-disabled"]').trigger('click')
    await flushPromises()

    expect(mockBatchUpdateVendorMetricMappingRuleStatus).toHaveBeenCalledWith(1001, {
      ruleIds: [7101, 7102],
      targetStatus: 'DISABLED'
    })
    expect(wrapper.text()).toContain('请求 2 · 命中 2 · 变更 2 · 目标 DISABLED')
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('clears batch status summary when switching to another valid product', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="rule-ledger-select-7101"]').setValue(true)
    await wrapper.get('[data-testid="rule-ledger-select-7102"]').setValue(true)
    await wrapper.get('[data-testid="rule-ledger-batch-status-disabled"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="rule-ledger-batch-status-summary"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('请求 2 · 命中 2 · 变更 2 · 目标 DISABLED')

    await wrapper.setProps({ productId: 2002 })
    await flushPromises()
    await flushPromises()

    expect(mockListVendorMetricMappingRuleLedger).toHaveBeenLastCalledWith(2002)
    expect(wrapper.find('[data-testid="rule-ledger-batch-status-summary"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('请求 2 · 命中 2 · 变更 2 · 目标 DISABLED')
  })

  it('runs replay and renders hit source, scope, canonical identifier and sample value', async () => {
    const wrapper = mountPanel()

    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="rule-ledger-replay-sample-7101"]').setValue('0.2136')
    await wrapper.get('[data-testid="rule-ledger-replay-submit-7101"]').trigger('click')
    await flushPromises()

    expect(mockReplayVendorMetricMappingRule).toHaveBeenCalledWith(1001, {
      rawIdentifier: 'disp',
      logicalChannelCode: undefined,
      sampleValue: '0.2136'
    })
    expect(wrapper.text()).toContain('PUBLISHED_SNAPSHOT')
    expect(wrapper.text()).toContain('PRODUCT')
    expect(wrapper.text()).toContain('value')
    expect(wrapper.text()).toContain('0.2136')
  })
})
