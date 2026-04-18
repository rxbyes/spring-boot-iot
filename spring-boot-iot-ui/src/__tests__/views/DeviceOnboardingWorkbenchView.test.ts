import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DeviceOnboardingWorkbenchView from '@/views/DeviceOnboardingWorkbenchView.vue'

const {
  mockPageDeviceOnboardingCases,
  mockCreateDeviceOnboardingCase,
  mockUpdateDeviceOnboardingCase,
  mockRefreshDeviceOnboardingCaseStatus,
  mockMessageSuccess,
  mockMessageError,
  mockRouter
} = vi.hoisted(() => ({
  mockPageDeviceOnboardingCases: vi.fn(),
  mockCreateDeviceOnboardingCase: vi.fn(),
  mockUpdateDeviceOnboardingCase: vi.fn(),
  mockRefreshDeviceOnboardingCaseStatus: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn(),
  mockRouter: {
    push: vi.fn()
  }
}))

vi.mock('vue-router', () => ({
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : \'#\'"><slot /></a>'
  }),
  useRouter: () => mockRouter
}))

vi.mock('@/api/deviceOnboarding', () => ({
  pageDeviceOnboardingCases: mockPageDeviceOnboardingCases,
  createDeviceOnboardingCase: mockCreateDeviceOnboardingCase,
  updateDeviceOnboardingCase: mockUpdateDeviceOnboardingCase,
  refreshDeviceOnboardingCaseStatus: mockRefreshDeviceOnboardingCaseStatus
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: mockMessageSuccess,
    error: mockMessageError
  }
}))

const IotAccessPageShellStub = defineComponent({
  name: 'IotAccessPageShell',
  props: ['title', 'description'],
  template: `
    <section class="device-onboarding-page-shell">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <slot name="actions" />
      <slot />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="device-onboarding-panel">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="filters" />
      <slot name="toolbar" />
      <slot />
      <slot name="pagination" />
    </section>
  `
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="device-onboarding-card">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountView() {
  return mount(DeviceOnboardingWorkbenchView, {
    global: {
      stubs: {
        IotAccessPageShell: IotAccessPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        PanelCard: PanelCardStub,
        StandardButton: StandardButtonStub,
        StandardListFilterHeader: true,
        StandardTableToolbar: true,
        StandardPagination: true
      }
    }
  })
}

describe('DeviceOnboardingWorkbenchView', () => {
  beforeEach(() => {
    mockPageDeviceOnboardingCases.mockReset()
    mockCreateDeviceOnboardingCase.mockReset()
    mockUpdateDeviceOnboardingCase.mockReset()
    mockRefreshDeviceOnboardingCaseStatus.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()
    mockRouter.push.mockReset()

    mockPageDeviceOnboardingCases.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 9101,
            tenantId: 1,
            caseCode: 'CASE-9101',
            caseName: '裂缝传感器接入',
            scenarioCode: 'phase1-crack',
            deviceFamily: 'crack_sensor',
            protocolFamilyCode: null,
            decryptProfileCode: null,
            protocolTemplateCode: null,
            productId: null,
            releaseBatchId: null,
            currentStep: 'PRODUCT_GOVERNANCE',
            status: 'BLOCKED',
            blockers: ['待绑定产品并完成契约治理']
          }
        ]
      }
    })
    mockCreateDeviceOnboardingCase.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9102,
        tenantId: 1,
        caseCode: 'CASE-9102',
        caseName: '雨量计接入',
        currentStep: 'PROTOCOL_GOVERNANCE',
        status: 'BLOCKED',
        blockers: ['待补齐协议族/解密档案/协议模板']
      }
    })
    mockRefreshDeviceOnboardingCaseStatus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9101,
        tenantId: 1,
        caseCode: 'CASE-9101',
        caseName: '裂缝传感器接入',
        currentStep: 'CONTRACT_RELEASE',
        status: 'IN_PROGRESS',
        blockers: ['待发布正式合同批次']
      }
    })
  })

  it('renders onboarding summary cards and next-step actions', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('无代码接入台')
    expect(wrapper.text()).toContain('裂缝传感器接入')
    expect(wrapper.text()).toContain('待绑定产品并完成契约治理')
    expect(wrapper.text()).toContain('前往产品治理')
    expect(wrapper.text()).toContain('阻塞案例')
  })

  it('creates a new onboarding case from the inline form', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-case-code"]').setValue('CASE-9102')
    await wrapper.get('[data-testid="onboarding-case-name"]').setValue('雨量计接入')
    await wrapper.get('[data-testid="onboarding-save"]').trigger('click')
    await flushPromises()

    expect(mockCreateDeviceOnboardingCase).toHaveBeenCalledWith(expect.objectContaining({
      caseCode: 'CASE-9102',
      caseName: '雨量计接入'
    }))
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('refreshes row status and routes to products for product governance rows', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-refresh-9101"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click')

    expect(mockRefreshDeviceOnboardingCaseStatus).toHaveBeenCalledWith(9101)
    expect(mockRouter.push).toHaveBeenCalledWith(expect.objectContaining({
      path: '/products'
    }))
  })
})
