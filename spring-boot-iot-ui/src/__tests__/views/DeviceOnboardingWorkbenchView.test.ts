import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DeviceOnboardingWorkbenchView from '@/views/DeviceOnboardingWorkbenchView.vue'

const {
  mockPageDeviceOnboardingCases,
  mockPageOnboardingTemplatePacks,
  mockCreateOnboardingTemplatePack,
  mockUpdateOnboardingTemplatePack,
  mockBatchCreateDeviceOnboardingCases,
  mockBatchApplyDeviceOnboardingCaseTemplate,
  mockBatchStartDeviceOnboardingCasesAcceptance,
  mockCreateDeviceOnboardingCase,
  mockUpdateDeviceOnboardingCase,
  mockRefreshDeviceOnboardingCaseStatus,
  mockStartDeviceOnboardingCaseAcceptance,
  mockMessageSuccess,
  mockMessageError,
  mockRouter
} = vi.hoisted(() => ({
  mockPageDeviceOnboardingCases: vi.fn(),
  mockPageOnboardingTemplatePacks: vi.fn(),
  mockCreateOnboardingTemplatePack: vi.fn(),
  mockUpdateOnboardingTemplatePack: vi.fn(),
  mockBatchCreateDeviceOnboardingCases: vi.fn(),
  mockBatchApplyDeviceOnboardingCaseTemplate: vi.fn(),
  mockBatchStartDeviceOnboardingCasesAcceptance: vi.fn(),
  mockCreateDeviceOnboardingCase: vi.fn(),
  mockUpdateDeviceOnboardingCase: vi.fn(),
  mockRefreshDeviceOnboardingCaseStatus: vi.fn(),
  mockStartDeviceOnboardingCaseAcceptance: vi.fn(),
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
  pageOnboardingTemplatePacks: mockPageOnboardingTemplatePacks,
  createOnboardingTemplatePack: mockCreateOnboardingTemplatePack,
  updateOnboardingTemplatePack: mockUpdateOnboardingTemplatePack,
  batchCreateDeviceOnboardingCases: mockBatchCreateDeviceOnboardingCases,
  batchApplyDeviceOnboardingCaseTemplate: mockBatchApplyDeviceOnboardingCaseTemplate,
  batchStartDeviceOnboardingCasesAcceptance: mockBatchStartDeviceOnboardingCasesAcceptance,
  createDeviceOnboardingCase: mockCreateDeviceOnboardingCase,
  updateDeviceOnboardingCase: mockUpdateDeviceOnboardingCase,
  refreshDeviceOnboardingCaseStatus: mockRefreshDeviceOnboardingCaseStatus,
  startDeviceOnboardingCaseAcceptance: mockStartDeviceOnboardingCaseAcceptance
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

function buildCaseRecords() {
  return [
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
    },
    {
      id: 9201,
      tenantId: 1,
      caseCode: 'CASE-9201',
      caseName: '裂缝传感器验收通过',
      scenarioCode: 'phase1-crack',
      deviceFamily: 'crack_sensor',
      protocolFamilyCode: 'legacy-dp-crack',
      decryptProfileCode: 'aes-62000002',
      protocolTemplateCode: 'nf-crack-v1',
      productId: 1001,
      releaseBatchId: 88001,
      deviceCode: 'DEV-9201',
      currentStep: 'ACCEPTANCE',
      status: 'READY',
      blockers: [],
      acceptance: {
        jobId: 'doa-job-9201',
        runId: '20260418193000',
        status: 'PASSED',
        summary: '8/8 检查项通过',
        failedLayers: [],
        jumpPath: '/automation-results?runId=20260418193000'
      }
    },
    {
      id: 9301,
      tenantId: 1,
      caseCode: 'CASE-9301',
      caseName: '雨量计接入待验收',
      scenarioCode: 'phase4-rain-gauge',
      deviceFamily: 'rain_gauge',
      protocolFamilyCode: 'legacy-dp-rain',
      decryptProfileCode: 'aes-62000003',
      protocolTemplateCode: 'nf-rain-v1',
      productId: 1002,
      releaseBatchId: 88002,
      deviceCode: 'DEV-9301',
      currentStep: 'ACCEPTANCE',
      status: 'READY',
      blockers: []
    }
  ]
}

function buildCasePageResponse(records = buildCaseRecords()) {
  return {
    code: 200,
    msg: 'success',
    data: {
      total: records.length,
      pageNum: 1,
      pageSize: 10,
      records
    }
  }
}

describe('DeviceOnboardingWorkbenchView', () => {
  beforeEach(() => {
    mockPageDeviceOnboardingCases.mockReset()
    mockPageOnboardingTemplatePacks.mockReset()
    mockCreateOnboardingTemplatePack.mockReset()
    mockUpdateOnboardingTemplatePack.mockReset()
    mockBatchCreateDeviceOnboardingCases.mockReset()
    mockBatchApplyDeviceOnboardingCaseTemplate.mockReset()
    mockBatchStartDeviceOnboardingCasesAcceptance.mockReset()
    mockCreateDeviceOnboardingCase.mockReset()
    mockUpdateDeviceOnboardingCase.mockReset()
    mockRefreshDeviceOnboardingCaseStatus.mockReset()
    mockStartDeviceOnboardingCaseAcceptance.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()
    mockRouter.push.mockReset()

    mockPageDeviceOnboardingCases.mockResolvedValue(buildCasePageResponse())
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
    mockPageOnboardingTemplatePacks.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 20,
        records: [
          {
            id: 7001,
            tenantId: 1,
            packCode: 'PACK-CRACK-V1',
            packName: '裂缝模板包',
            scenarioCode: 'phase1-crack',
            deviceFamily: 'crack_sensor',
            status: 'ACTIVE',
            versionNo: 1,
            protocolFamilyCode: 'legacy-dp-crack',
            decryptProfileCode: 'aes-62000002',
            protocolTemplateCode: 'nf-crack-v1',
            description: '首版裂缝模板包'
          }
        ]
      }
    })
    mockCreateOnboardingTemplatePack.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 7002,
        tenantId: 1,
        packCode: 'PACK-GNSS-V1',
        packName: 'GNSS 模板包',
        scenarioCode: 'phase2-gnss',
        deviceFamily: 'gnss_sensor',
        status: 'ACTIVE',
        versionNo: 1,
        protocolFamilyCode: 'legacy-dp-gnss',
        decryptProfileCode: 'aes-62000004',
        protocolTemplateCode: 'nf-gnss-v1'
      }
    })
    mockBatchCreateDeviceOnboardingCases.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        action: 'BATCH_CREATE',
        requestedCount: 2,
        successCount: 2,
        failedCount: 0,
        successItems: [
          {
            caseId: 9401,
            caseCode: 'CASE-9401',
            caseName: '批量裂缝 01',
            currentStep: 'PROTOCOL_GOVERNANCE',
            status: 'BLOCKED',
            deviceCode: 'DEV-9401'
          },
          {
            caseId: 9402,
            caseCode: 'CASE-9402',
            caseName: '批量裂缝 02',
            currentStep: 'PROTOCOL_GOVERNANCE',
            status: 'BLOCKED',
            deviceCode: 'DEV-9402'
          }
        ],
        failureItems: [],
        failureGroups: []
      }
    })
    mockBatchApplyDeviceOnboardingCaseTemplate.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        action: 'BATCH_APPLY_TEMPLATE',
        requestedCount: 2,
        successCount: 2,
        failedCount: 0,
        successItems: [
          {
            caseId: 9101,
            caseCode: 'CASE-9101',
            caseName: '裂缝传感器接入',
            currentStep: 'PRODUCT_GOVERNANCE',
            status: 'BLOCKED'
          },
          {
            caseId: 9301,
            caseCode: 'CASE-9301',
            caseName: '雨量计接入待验收',
            currentStep: 'PRODUCT_GOVERNANCE',
            status: 'BLOCKED'
          }
        ],
        failureItems: [],
        failureGroups: []
      }
    })
    mockBatchStartDeviceOnboardingCasesAcceptance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        action: 'BATCH_START_ACCEPTANCE',
        requestedCount: 2,
        successCount: 1,
        failedCount: 1,
        successItems: [
          {
            caseId: 9301,
            caseCode: 'CASE-9301',
            caseName: '雨量计接入待验收',
            currentStep: 'ACCEPTANCE',
            status: 'IN_PROGRESS',
            acceptanceStatus: 'RUNNING'
          }
        ],
        failureItems: [
          {
            caseId: 9101,
            caseCode: 'CASE-9101',
            caseName: '裂缝传感器接入',
            failureKey: '待补齐验收设备编码',
            message: '待补齐验收设备编码'
          }
        ],
        failureGroups: [
          {
            failureKey: '待补齐验收设备编码',
            summary: '待补齐验收设备编码',
            count: 1,
            caseCodes: ['CASE-9101']
          }
        ]
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
    mockStartDeviceOnboardingCaseAcceptance.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 9301,
        tenantId: 1,
        caseCode: 'CASE-9301',
        caseName: '雨量计接入待验收',
        currentStep: 'ACCEPTANCE',
        status: 'IN_PROGRESS',
        blockers: ['标准接入验收执行中'],
        acceptance: {
          jobId: 'doa-job-9301',
          status: 'RUNNING',
          summary: '标准接入验收执行中',
          failedLayers: []
        }
      }
    })
  })

  it('renders onboarding summary cards and next-step actions', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('无代码接入台')
    expect(wrapper.text()).toContain('产品相关执行统一跳到产品工作台处理')
    expect(wrapper.text()).toContain('裂缝传感器接入')
    expect(wrapper.text()).toContain('待绑定产品并完成契约治理')
    expect(wrapper.text()).toContain('前往产品列表')
    expect(wrapper.text()).toContain('阻塞案例')
  })

  it('creates a new onboarding case from the inline form', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-case-code"]').setValue('CASE-9102')
    await wrapper.get('[data-testid="onboarding-case-name"]').setValue('雨量计接入')
    await wrapper.get('[data-testid="onboarding-device-code"]').setValue('DEV-9102')
    await wrapper.get('[data-testid="onboarding-save"]').trigger('click')
    await flushPromises()

    expect(mockCreateDeviceOnboardingCase).toHaveBeenCalledWith(expect.objectContaining({
      caseCode: 'CASE-9102',
      caseName: '雨量计接入',
      deviceCode: 'DEV-9102'
    }))
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('preserves 18-digit product ids when creating onboarding cases', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-case-code"]').setValue('CASE-9103')
    await wrapper.get('[data-testid="onboarding-case-name"]').setValue('GNSS 接入')
    await wrapper.get('input[placeholder="可选，填正式产品 ID"]').setValue('202603192100560252')
    await wrapper.get('[data-testid="onboarding-save"]').trigger('click')
    await flushPromises()

    expect(mockCreateDeviceOnboardingCase).toHaveBeenCalledWith(expect.objectContaining({
      caseCode: 'CASE-9103',
      caseName: 'GNSS 接入',
      productId: '202603192100560252'
    }))
  })

  it('applies template pack defaults into the onboarding case form', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="template-pack-select"]').setValue('7001')
    await wrapper.get('[data-testid="apply-template-pack"]').trigger('click')

    expect((wrapper.get('[data-testid="onboarding-scenario-code"]').element as HTMLInputElement).value).toBe('phase1-crack')
    expect((wrapper.get('[data-testid="onboarding-device-family"]').element as HTMLInputElement).value).toBe('crack_sensor')
    expect((wrapper.get('[data-testid="onboarding-protocol-family-code"]').element as HTMLInputElement).value).toBe('legacy-dp-crack')
    expect((wrapper.get('[data-testid="onboarding-decrypt-profile-code"]').element as HTMLInputElement).value).toBe('aes-62000002')
    expect((wrapper.get('[data-testid="onboarding-protocol-template-code"]').element as HTMLInputElement).value).toBe('nf-crack-v1')
  })

  it('creates template packs from the inline template form', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="template-pack-code"]').setValue('PACK-GNSS-V1')
    await wrapper.get('[data-testid="template-pack-name"]').setValue('GNSS 模板包')
    await wrapper.get('[data-testid="template-pack-scenario-code"]').setValue('phase2-gnss')
    await wrapper.get('[data-testid="template-pack-device-family"]').setValue('gnss_sensor')
    await wrapper.get('[data-testid="template-pack-protocol-family-code"]').setValue('legacy-dp-gnss')
    await wrapper.get('[data-testid="template-pack-decrypt-profile-code"]').setValue('aes-62000004')
    await wrapper.get('[data-testid="template-pack-protocol-template-code"]').setValue('nf-gnss-v1')
    await wrapper.get('[data-testid="template-pack-save"]').trigger('click')
    await flushPromises()

    expect(mockCreateOnboardingTemplatePack).toHaveBeenCalledWith(expect.objectContaining({
      packCode: 'PACK-GNSS-V1',
      packName: 'GNSS 模板包',
      scenarioCode: 'phase2-gnss',
      deviceFamily: 'gnss_sensor',
      protocolFamilyCode: 'legacy-dp-gnss',
      decryptProfileCode: 'aes-62000004',
      protocolTemplateCode: 'nf-gnss-v1'
    }))
  })

  it('shows a direct product workbench action only for bound cases', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="onboarding-open-product-9101"]').exists()).toBe(false)
    expect(wrapper.get('[data-testid="onboarding-open-product-9201"]').text()).toContain('进入产品工作台')

    await wrapper.get('[data-testid="onboarding-open-product-9201"]').trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/overview')
  })

  it('routes product governance rows without productId to the products list', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="onboarding-next-9101"]').text()).toContain('前往产品列表')

    await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith('/products')
  })

  it('routes product governance rows with productId to contract fields', async () => {
    const governanceRecords = buildCaseRecords().map((row) => {
      if (row.id !== 9101) {
        return row
      }
      return {
        ...row,
        productId: 1001,
        protocolFamilyCode: 'legacy-dp-crack',
        decryptProfileCode: 'aes-62000002',
        protocolTemplateCode: 'nf-crack-v1',
        currentStep: 'PRODUCT_GOVERNANCE',
        status: 'BLOCKED',
        blockers: ['待完成产品契约治理']
      }
    })

    mockPageDeviceOnboardingCases.mockReset()
    mockPageDeviceOnboardingCases.mockResolvedValueOnce(buildCasePageResponse(governanceRecords))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[data-testid="onboarding-next-9101"]').text()).toContain('前往契约字段')

    await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/contracts')
  })

  it('routes refreshed contract release rows with productId to contract fields', async () => {
    const refreshedRecords = buildCaseRecords().map((row) => {
      if (row.id !== 9101) {
        return row
      }
      return {
        ...row,
        productId: 1001,
        releaseBatchId: 88001,
        currentStep: 'CONTRACT_RELEASE',
        status: 'IN_PROGRESS',
        blockers: ['待发布正式合同批次']
      }
    })

    mockPageDeviceOnboardingCases.mockReset()
    mockPageDeviceOnboardingCases
      .mockResolvedValueOnce(buildCasePageResponse())
      .mockResolvedValueOnce(buildCasePageResponse(refreshedRecords))
    mockRefreshDeviceOnboardingCaseStatus.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: refreshedRecords[0]
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-refresh-9101"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="onboarding-next-9101"]').text()).toContain('前往契约字段')

    await wrapper.get('[data-testid="onboarding-next-9101"]').trigger('click')

    expect(mockRefreshDeviceOnboardingCaseStatus).toHaveBeenCalledWith(9101)
    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/contracts')
  })

  it('shows acceptance summary and normalizes legacy acceptance result links into governance evidence', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('8/8 检查项通过')

    await wrapper.get('[data-testid="onboarding-result-9201"]').trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith('/automation-governance?tab=evidence&runId=20260418193000')
  })

  it('starts acceptance for ready acceptance rows', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-accept-9301"]').trigger('click')
    await flushPromises()

    expect(mockStartDeviceOnboardingCaseAcceptance).toHaveBeenCalledWith(9301)
    expect(mockMessageSuccess).toHaveBeenCalledWith('标准接入验收已触发')
  })

  it('creates onboarding cases in batch from multiline input', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-batch-input"]').setValue('CASE-9401,批量裂缝 01,DEV-9401\nCASE-9402,批量裂缝 02,DEV-9402')
    await wrapper.get('[data-testid="onboarding-batch-create"]').trigger('click')
    await flushPromises()

    expect(mockBatchCreateDeviceOnboardingCases).toHaveBeenCalledWith({
      items: [
        expect.objectContaining({
          caseCode: 'CASE-9401',
          caseName: '批量裂缝 01',
          deviceCode: 'DEV-9401'
        }),
        expect.objectContaining({
          caseCode: 'CASE-9402',
          caseName: '批量裂缝 02',
          deviceCode: 'DEV-9402'
        })
      ]
    })
    expect(mockMessageSuccess).toHaveBeenCalledWith('批量创建已完成：成功 2 条，失败 0 条')
  })

  it('applies template pack in batch for selected cases', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-select-9101"]').setValue(true)
    await wrapper.get('[data-testid="onboarding-select-9301"]').setValue(true)
    await wrapper.get('[data-testid="onboarding-batch-template-pack-select"]').setValue('7001')
    await wrapper.get('[data-testid="onboarding-batch-apply-template"]').trigger('click')
    await flushPromises()

    expect(mockBatchApplyDeviceOnboardingCaseTemplate).toHaveBeenCalledWith({
      caseIds: [9101, 9301],
      templatePackId: 7001
    })
    expect(mockMessageSuccess).toHaveBeenCalledWith('批量套用模板已完成：成功 2 条，失败 0 条')
  })

  it('starts acceptance in batch and renders grouped failure summary', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[data-testid="onboarding-select-9101"]').setValue(true)
    await wrapper.get('[data-testid="onboarding-select-9301"]').setValue(true)
    await wrapper.get('[data-testid="onboarding-batch-start-acceptance"]').trigger('click')
    await flushPromises()

    expect(mockBatchStartDeviceOnboardingCasesAcceptance).toHaveBeenCalledWith({
      caseIds: [9101, 9301]
    })
    expect(wrapper.text()).toContain('失败分组')
    expect(wrapper.text()).toContain('待补齐验收设备编码')
    expect(wrapper.text()).toContain('CASE-9101')
  })
})
