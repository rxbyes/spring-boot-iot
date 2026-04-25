import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductNormativeMetricImportPanel from '@/components/product/ProductNormativeMetricImportPanel.vue'
import { createRequestError } from '@/api/request'

const {
  mockPreviewNormativeMetricImport,
  mockApplyNormativeMetricImport,
  mockMessageSuccess,
  mockMessageError
} = vi.hoisted(() => ({
  mockPreviewNormativeMetricImport: vi.fn(),
  mockApplyNormativeMetricImport: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn()
}))

vi.mock('@/api/normativeMetricDefinition', () => ({
  previewNormativeMetricImport: mockPreviewNormativeMetricImport,
  applyNormativeMetricImport: mockApplyNormativeMetricImport
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
  return mount(ProductNormativeMetricImportPanel, {
    global: {
      stubs: {
        StandardButton: StandardButtonStub
      }
    }
  })
}

async function pasteValidImportJson(wrapper: ReturnType<typeof mountPanel>) {
  await wrapper.get('[data-testid="normative-import-json"]').setValue(JSON.stringify([
    {
      scenarioCode: 'phase4-surface-flow-speed',
      deviceFamily: 'SURFACE_FLOW_SPEED',
      identifier: 'value',
      displayName: '表面流速',
      monitorContentCode: 'L4',
      monitorTypeCode: 'BMLS'
    }
  ]))
}

describe('ProductNormativeMetricImportPanel', () => {
  beforeEach(() => {
    mockPreviewNormativeMetricImport.mockReset()
    mockApplyNormativeMetricImport.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()

    mockPreviewNormativeMetricImport.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        readyCount: 1,
        conflictCount: 0,
        rows: [
          {
            rowIndex: 1,
            scenarioCode: 'phase4-surface-flow-speed',
            deviceFamily: 'SURFACE_FLOW_SPEED',
            identifier: 'value',
            displayName: '表面流速',
            fallbackKey: 'L4/BMLS/value',
            action: 'CREATE',
            status: 'READY',
            message: '可导入'
          }
        ]
      }
    })
    mockApplyNormativeMetricImport.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        readyCount: 0,
        conflictCount: 0,
        appliedCount: 1,
        rows: [
          {
            rowIndex: 1,
            scenarioCode: 'phase4-surface-flow-speed',
            deviceFamily: 'SURFACE_FLOW_SPEED',
            identifier: 'value',
            displayName: '表面流速',
            fallbackKey: 'L4/BMLS/value',
            action: 'CREATE',
            status: 'APPLIED_CREATE',
            message: '已创建规范字段'
          }
        ]
      }
    })
  })

  it('previews a pasted JSON array and renders ready rows', async () => {
    const wrapper = mountPanel()

    await pasteValidImportJson(wrapper)
    await wrapper.get('[data-testid="normative-import-preview"]').trigger('click')
    await flushPromises()

    expect(mockPreviewNormativeMetricImport).toHaveBeenCalledWith({
      items: [
        expect.objectContaining({
          scenarioCode: 'phase4-surface-flow-speed',
          identifier: 'value'
        })
      ]
    })
    expect(wrapper.text()).toContain('可导入')
    expect(wrapper.text()).toContain('L4/BMLS/value')
  })

  it('applies only after a clean preview result', async () => {
    const wrapper = mountPanel()

    await pasteValidImportJson(wrapper)
    await wrapper.get('[data-testid="normative-import-preview"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="normative-import-apply"]').trigger('click')
    await flushPromises()

    expect(mockApplyNormativeMetricImport).toHaveBeenCalledTimes(1)
    expect(mockMessageSuccess).toHaveBeenCalledWith('规范字段已导入 1 条')
    expect(wrapper.text()).toContain('已创建规范字段')
  })

  it('shows handled preview permission errors inline without a duplicate toast', async () => {
    mockPreviewNormativeMetricImport.mockRejectedValueOnce(
      createRequestError('缺少产品契约治理权限', true, 500, '缺少产品契约治理权限')
    )
    const wrapper = mountPanel()

    await pasteValidImportJson(wrapper)
    await wrapper.get('[data-testid="normative-import-preview"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="normative-import-message"]').text()).toBe('缺少产品契约治理权限')
    expect(mockMessageError).not.toHaveBeenCalled()
    expect(mockApplyNormativeMetricImport).not.toHaveBeenCalled()
  })

  it('shows unhandled preview failures inline and emits one toast', async () => {
    mockPreviewNormativeMetricImport.mockRejectedValueOnce(new Error('网络连接失败'))
    const wrapper = mountPanel()

    await pasteValidImportJson(wrapper)
    await wrapper.get('[data-testid="normative-import-preview"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="normative-import-message"]').text()).toBe('网络连接失败')
    expect(mockMessageError).toHaveBeenCalledTimes(1)
    expect(mockMessageError).toHaveBeenCalledWith('网络连接失败')
  })

  it('keeps preview evidence and shows handled apply failures inline', async () => {
    mockApplyNormativeMetricImport.mockRejectedValueOnce(
      createRequestError(
        '规范字段导入存在冲突，请先预检并修正',
        true,
        500,
        '规范字段导入存在冲突，请先预检并修正'
      )
    )
    const wrapper = mountPanel()

    await pasteValidImportJson(wrapper)
    await wrapper.get('[data-testid="normative-import-preview"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="normative-import-apply"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('L4/BMLS/value')
    expect(wrapper.get('[data-testid="normative-import-message"]').text())
      .toBe('规范字段导入存在冲突，请先预检并修正')
    expect(mockMessageError).not.toHaveBeenCalled()
  })
})
