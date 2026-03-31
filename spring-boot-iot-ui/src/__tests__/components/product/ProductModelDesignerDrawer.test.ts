import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerDrawer from '@/components/product/ProductModelDesignerDrawer.vue'

const {
  mockListProductModels,
  mockManualExtractProductModelCandidates,
  mockConfirmProductModelCandidates
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockManualExtractProductModelCandidates: vi.fn(),
  mockConfirmProductModelCandidates: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    listProductModelCandidates: vi.fn(),
    manualExtractProductModelCandidates: mockManualExtractProductModelCandidates,
    confirmProductModelCandidates: mockConfirmProductModelCandidates,
    addProductModel: vi.fn(),
    updateProductModel: vi.fn(),
    deleteProductModel: vi.fn()
  }
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
}))

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle', 'loading', 'errorMessage', 'empty'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="standard-detail-drawer-stub">
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue'],
  emits: ['update:modelValue', 'close'],
  template: '<section v-if="modelValue" class="standard-form-drawer-stub"><slot /><slot name="footer" /></section>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      class="standard-button-stub"
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  template: '<footer class="standard-drawer-footer-stub"><slot /></footer>'
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button type="button" class="standard-action-link-stub" @click="$emit(\'click\')"><slot /></button>'
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="standard-row-actions-stub"><slot /></div>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElCheckboxStub = defineComponent({
  name: 'ElCheckbox',
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-checkbox-stub"
      type="checkbox"
      :checked="Boolean(modelValue)"
      :disabled="Boolean(disabled)"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type'],
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      class="el-input-stub el-input-stub--textarea"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      class="el-input-stub"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
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

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function manualResult() {
  return {
    productId: 1001,
    summary: {
      extractionMode: 'manual',
      sampleType: 'business',
      sampleDeviceCode: 'SK11E80D1307426AZ',
      propertyEvidenceCount: 2,
      propertyCandidateCount: 2,
      eventEvidenceCount: 0,
      eventCandidateCount: 0,
      serviceEvidenceCount: 0,
      serviceCandidateCount: 0,
      needsReviewCount: 0,
      existingModelCount: 0,
      createdCount: 0,
      skippedCount: 0,
      conflictCount: 0,
      eventHint: '手动提炼当前仅生成属性候选，事件请在正式模型中人工补充。',
      serviceHint: '手动提炼当前仅生成属性候选，服务请在正式模型中人工补充。',
      ignoredFieldCount: 0,
      lastExtractedAt: '2026-03-31T12:00:00'
    },
    propertyCandidates: [
      {
        modelType: 'property',
        identifier: 'L1_QJ_1.X',
        modelName: '1号倾角测点X轴位移',
        dataType: 'double',
        sortNo: 10,
        requiredFlag: 0,
        description: '来源于手动录入样本，归属测点属性。',
        groupKey: 'telemetry',
        confidence: 0.96,
        needsReview: false,
        candidateStatus: 'ready',
        evidenceCount: 1,
        messageEvidenceCount: 1,
        lastReportTime: '2026-03-31T12:00:00',
        sourceTables: ['manual_sample']
      },
      {
        modelType: 'property',
        identifier: 'L1_QJ_1.AZI',
        modelName: '1号倾角测点方位角',
        dataType: 'double',
        sortNo: 10,
        requiredFlag: 0,
        description: '来源于手动录入样本，归属测点属性。',
        groupKey: 'telemetry',
        confidence: 0.96,
        needsReview: false,
        candidateStatus: 'ready',
        evidenceCount: 1,
        messageEvidenceCount: 1,
        lastReportTime: '2026-03-31T12:00:00',
        sourceTables: ['manual_sample']
      }
    ],
    eventCandidates: [],
    serviceCandidates: []
  }
}

function mountDrawer() {
  return mount(ProductModelDesignerDrawer, {
    props: {
      modelValue: true,
      product: {
        id: 1001,
        productKey: 'south-monitor',
        productName: '南方监测终端',
        protocolCode: 'mqtt-json',
        nodeType: 1
      }
    },
    global: {
      stubs: {
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardActionLink: StandardActionLinkStub,
        StandardRowActions: StandardRowActionsStub,
        ElTag: ElTagStub,
        ElCheckbox: ElCheckboxStub,
        ElInput: ElInputStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElSelect: true,
        ElOption: true,
        ElSwitch: true,
        ElInputNumber: true
      }
    }
  })
}

describe('ProductModelDesignerDrawer', () => {
  beforeEach(() => {
    mockListProductModels.mockReset()
    mockManualExtractProductModelCandidates.mockReset()
    mockConfirmProductModelCandidates.mockReset()
    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockManualExtractProductModelCandidates.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: manualResult()
    })
    mockConfirmProductModelCandidates.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        createdCount: 2,
        skippedCount: 0,
        conflictCount: 0
      }
    })
  })

  it('extracts candidates from a manual sample payload', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    const sampleInput = wrapper.find('[data-testid="manual-sample-input"] textarea')
    await sampleInput.setValue('{"SK11E80D1307426AZ":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":-0.0376,"AZI":-8.6772}}}}')
    await wrapper.find('[data-testid="manual-extract-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockListProductModels).toHaveBeenCalledWith(1001)
    expect(mockManualExtractProductModelCandidates).toHaveBeenCalledWith(1001, {
      sampleType: 'business',
      samplePayload: '{"SK11E80D1307426AZ":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":-0.0376,"AZI":-8.6772}}}}'
    })
    expect(wrapper.text()).toContain('SK11E80D1307426AZ')
    expect(wrapper.text()).toContain('L1_QJ_1.X')
    expect(wrapper.text()).toContain('1号倾角测点方位角')
  })
})
