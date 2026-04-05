import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductModelDesignerDrawer from '@/components/product/ProductModelDesignerDrawer.vue'

const {
  mockListProductModels,
  mockCompareProductModelGovernance,
  mockApplyProductModelGovernance,
  mockWarningMessage
} = vi.hoisted(() => ({
  mockListProductModels: vi.fn(),
  mockCompareProductModelGovernance: vi.fn(),
  mockApplyProductModelGovernance: vi.fn(),
  mockWarningMessage: vi.fn()
}))

vi.mock('@/api/product', () => ({
  productApi: {
    listProductModels: mockListProductModels,
    listProductModelCandidates: vi.fn(),
    manualExtractProductModelCandidates: vi.fn(),
    confirmProductModelCandidates: vi.fn(),
    compareProductModelGovernance: mockCompareProductModelGovernance,
    applyProductModelGovernance: mockApplyProductModelGovernance,
    addProductModel: vi.fn(),
    updateProductModel: vi.fn(),
    deleteProductModel: vi.fn()
  }
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: mockWarningMessage
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
  props: ['confirmDisabled', 'confirmLoading'],
  emits: ['confirm', 'cancel'],
  template: `
    <footer class="standard-drawer-footer-stub">
      <button type="button" data-testid="governance-apply-cancel" @click="$emit('cancel')">关闭</button>
      <button
        type="button"
        data-testid="governance-apply-submit"
        :disabled="Boolean(confirmDisabled) || Boolean(confirmLoading)"
        @click="$emit('confirm')"
      >
        确认应用
      </button>
    </footer>
  `
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

function compareResult() {
  return {
    productId: 1001,
    summary: {
      manualCount: 1,
      runtimeCount: 1,
      formalCount: 0,
      propertyCount: 1,
      eventCount: 0,
      serviceCount: 0,
      doubleAlignedCount: 1,
      manualOnlyCount: 0,
      runtimeOnlyCount: 0,
      formalExistsCount: 0,
      suspectedConflictCount: 0,
      evidenceInsufficientCount: 1,
      lastComparedAt: '2026-03-31T12:00:00'
    },
    manualSummary: {
      extractionMode: 'manual',
      sampleType: 'business',
      sampleDeviceCode: 'SK11E80D1307426AZ',
      propertyEvidenceCount: 1,
      propertyCandidateCount: 1
    },
    runtimeSummary: {
      extractionMode: 'runtime',
      propertyEvidenceCount: 1,
      propertyCandidateCount: 1
    },
    formalSummary: {
      formalCount: 0,
      propertyCount: 0,
      eventCount: 0,
      serviceCount: 0
    },
    compareRows: [
      {
        modelType: 'property',
        identifier: 'L1_QJ_1.X',
        compareStatus: 'double_aligned',
        suggestedAction: '纳入新增',
        riskFlags: [],
        suspectedMatches: [],
        manualCandidate: {
          modelType: 'property',
          identifier: 'L1_QJ_1.X',
          modelName: '1号倾角测点X轴位移',
          dataType: 'double',
          sortNo: 10,
          requiredFlag: 0,
          description: '来源于手动录入样本，归属测点属性。',
          sourceTables: ['manual_sample']
        },
        runtimeCandidate: {
          modelType: 'property',
          identifier: 'L1_QJ_1.X',
          modelName: '1号倾角测点X轴位移',
          dataType: 'double',
          sortNo: 10,
          requiredFlag: 0,
          description: '来源于真实上报证据。',
          sourceTables: ['iot_device_property']
        }
      }
    ]
  }
}

function mountDrawer(productOverrides?: Partial<{
  id: number
  productKey: string
  productName: string
  protocolCode: string
  nodeType: number
}>) {
  return mount(ProductModelDesignerDrawer, {
    props: {
      modelValue: true,
      product: {
        id: 1001,
        productKey: 'south-survey-multi-detector-v1',
        productName: '南方测绘多维检测仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        ...productOverrides
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
    mockCompareProductModelGovernance.mockReset()
    mockApplyProductModelGovernance.mockReset()
    mockWarningMessage.mockReset()
    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockCompareProductModelGovernance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: compareResult()
    })
    mockApplyProductModelGovernance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        createdCount: 1,
        updatedCount: 0,
        skippedCount: 0,
        conflictCount: 0,
        lastAppliedAt: '2026-03-31T12:05:00'
      }
    })
  })

  it('defaults the drawer to normative governance for the integrated preset and applies the selected decisions', async () => {
    const wrapper = mountDrawer()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('规范证据优先')
    expect(wrapper.text()).toContain('倾角 / 加速度 / 裂缝一体机')

    const sampleInput = wrapper.find('[data-testid="manual-sample-input"] textarea')
    await sampleInput.setValue('{"SK11E80D1307426AZ":{"L1_QJ_1":{"2026-03-31T04:05:55.000Z":{"X":-0.0376,"AZI":-8.6772}}}}')
    await wrapper.find('[data-testid="governance-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockListProductModels).toHaveBeenCalledWith(1001)
    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      governanceMode: 'normative',
      normativePresetCode: 'landslide-integrated-tilt-accel-crack-v1',
      selectedNormativeIdentifiers: ['L1_QJ_1.X', 'L1_QJ_1.Y', 'L1_QJ_1.Z', 'L1_QJ_1.angle', 'L1_QJ_1.AZI'],
      manualExtract: undefined,
      manualDraftItems: [],
      includeRuntimeCandidates: true
    })
    expect(wrapper.text()).toContain('SK11E80D1307426AZ')
    expect(wrapper.text()).toContain('双证据一致')
    expect(wrapper.text()).toContain('L1_QJ_1.X')
    expect(wrapper.text()).toContain('纳入新增')
    expect(wrapper.get('[data-testid="governance-summary-evidence-insufficient"]').text()).toContain('1')
    expect(wrapper.get('[data-testid="governance-apply-stage"]').text()).toContain('正式模型确认区')
    expect(wrapper.get('[data-testid="governance-apply-item-property:L1_QJ_1.X"]').text()).toContain('纳入新增')

    await wrapper.get('[data-testid="governance-apply-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockApplyProductModelGovernance).toHaveBeenCalledWith(1001, {
      items: [
        {
          decision: 'create',
          targetModelId: undefined,
          modelType: 'property',
          identifier: 'L1_QJ_1.X',
          modelName: '1号倾角测点X轴位移',
          dataType: 'double',
          specsJson: undefined,
          eventType: undefined,
          serviceInputJson: undefined,
          serviceOutputJson: undefined,
          sortNo: 10,
          requiredFlag: 0,
          description: '来源于手动录入样本，归属测点属性。',
          compareStatus: 'double_aligned'
        }
      ]
    })
  })

  it('shows an empty normative preset state for warning products and can switch to generic governance', async () => {
    const wrapper = mountDrawer({
      productKey: 'zhd-warning-sound-light-alarm-v1',
      productName: '中海达 预警型 声光报警器'
    })
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('当前产品暂无适用规范预设')
    expect(wrapper.text()).not.toContain('倾角 X 轴')

    await wrapper.find('[data-testid="governance-switch-generic"]').trigger('click')

    expect(wrapper.text()).toContain('通用双证据')
  })

  it('falls back to generic compare when the current product has no applicable normative preset', async () => {
    const wrapper = mountDrawer({
      productKey: 'zhd-warning-sound-light-alarm-v1',
      productName: '中海达 预警型 声光报警器'
    })
    await flushPromises()
    await nextTick()

    await wrapper.find('[data-testid="governance-compare-submit"]').trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockWarningMessage).toHaveBeenCalledWith('当前产品暂无适用规范预设，已改按通用双证据生成对比结果')
    expect(mockCompareProductModelGovernance).toHaveBeenCalledWith(1001, {
      governanceMode: 'generic',
      normativePresetCode: undefined,
      selectedNormativeIdentifiers: undefined,
      manualExtract: undefined,
      manualDraftItems: [],
      includeRuntimeCandidates: true
    })
  })
})
