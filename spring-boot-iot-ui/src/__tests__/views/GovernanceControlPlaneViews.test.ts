import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GovernanceTaskView from '@/views/GovernanceTaskView.vue'
import GovernanceOpsWorkbenchView from '@/views/GovernanceOpsWorkbenchView.vue'

const {
  mockPageWorkItems,
  mockGetWorkItemDecisionContext,
  mockAckWorkItem,
  mockBlockWorkItem,
  mockCloseWorkItem,
  mockGetProductById,
  mockPageOpsAlerts,
  mockGetRiskGovernanceReplay,
  mockSubmitGovernanceReplayFeedback,
  mockAckOpsAlert,
  mockSuppressOpsAlert,
  mockCloseOpsAlert,
  mockConfirmAction,
  mockMessageSuccess,
  mockMessageError,
  mockRoute,
  mockRouter
} = vi.hoisted(() => ({
  mockPageWorkItems: vi.fn(),
  mockGetWorkItemDecisionContext: vi.fn(),
  mockAckWorkItem: vi.fn(),
  mockBlockWorkItem: vi.fn(),
  mockCloseWorkItem: vi.fn(),
  mockGetProductById: vi.fn(),
  mockPageOpsAlerts: vi.fn(),
  mockGetRiskGovernanceReplay: vi.fn(),
  mockSubmitGovernanceReplayFeedback: vi.fn(),
  mockAckOpsAlert: vi.fn(),
  mockSuppressOpsAlert: vi.fn(),
  mockCloseOpsAlert: vi.fn(),
  mockConfirmAction: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn(),
  mockRoute: {
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn(),
    replace: vi.fn()
  }
}))

vi.mock('@/api/governanceWorkItem', () => ({
  pageGovernanceWorkItems: mockPageWorkItems,
  getGovernanceWorkItemDecisionContext: mockGetWorkItemDecisionContext,
  ackGovernanceWorkItem: mockAckWorkItem,
  blockGovernanceWorkItem: mockBlockWorkItem,
  closeGovernanceWorkItem: mockCloseWorkItem
}))

vi.mock('@/api/product', () => ({
  productApi: {
    getProductById: mockGetProductById
  }
}))

vi.mock('@/api/governanceOpsAlert', () => ({
  pageGovernanceOpsAlerts: mockPageOpsAlerts,
  ackGovernanceOpsAlert: mockAckOpsAlert,
  suppressGovernanceOpsAlert: mockSuppressOpsAlert,
  closeGovernanceOpsAlert: mockCloseOpsAlert
}))

vi.mock('@/api/riskGovernance', () => ({
  getRiskGovernanceReplay: mockGetRiskGovernanceReplay,
  submitGovernanceReplayFeedback: mockSubmitGovernanceReplayFeedback
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: mockConfirmAction,
  isConfirmCancelled: (error: unknown) => error === 'cancel' || error === 'close'
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: mockMessageSuccess,
    error: mockMessageError
  }
}))

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="governance-control-plane-workbench-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="filters" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="governance-control-plane-toolbar-stub">
      <div>{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="governance-control-plane-pagination-stub" />'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(`click`)"><slot /></button>'
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="governance-control-plane-filter-header-stub">
      <slot name="primary" />
      <slot name="actions" />
    </section>
  `
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description'],
  template: `
    <section class="governance-control-plane-panel-card-stub">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle'],
  template: `
    <section v-if="modelValue" class="governance-control-plane-detail-drawer-stub">
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
})

function mountWithStubs(component: Parameters<typeof mount>[0]) {
  return mount(component, {
    global: {
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardButton: StandardButtonStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        PanelCard: PanelCardStub,
        StandardDetailDrawer: StandardDetailDrawerStub
      }
    }
  })
}

describe('governance control plane views', () => {
  beforeEach(() => {
    mockPageWorkItems.mockReset()
    mockGetWorkItemDecisionContext.mockReset()
    mockAckWorkItem.mockReset()
    mockBlockWorkItem.mockReset()
    mockCloseWorkItem.mockReset()
    mockGetProductById.mockReset()
    mockPageOpsAlerts.mockReset()
    mockGetRiskGovernanceReplay.mockReset()
    mockSubmitGovernanceReplayFeedback.mockReset()
    mockAckOpsAlert.mockReset()
    mockSuppressOpsAlert.mockReset()
    mockCloseOpsAlert.mockReset()
    mockConfirmAction.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()
    mockRoute.query = {}
    mockRouter.push.mockReset()
    mockRouter.replace.mockReset()
    mockConfirmAction.mockResolvedValue(undefined)
    mockAckWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockBlockWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockCloseWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockAckOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockSuppressOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockCloseOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockSubmitGovernanceReplayFeedback.mockResolvedValue({ code: 200, msg: 'success', data: null })
  })

  it('renders governance task rows from backend work items', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            workItemCode: 'PENDING_PRODUCT_GOVERNANCE',
            workStatus: 'OPEN',
            blockingReason: '产品尚未进入治理主链路',
            snapshotJson: '{"productKey":"phase2-gnss"}'
          },
          {
            id: 2,
            workItemCode: 'PENDING_THRESHOLD_POLICY',
            workStatus: 'OPEN',
            blockingReason: '待补阈值策略'
          },
          {
            id: 3,
            workItemCode: 'PENDING_LINKAGE_PLAN',
            workStatus: 'OPEN',
            blockingReason: '待补联动预案'
          },
          {
            id: 4,
            workItemCode: 'PENDING_REPLAY',
            workStatus: 'OPEN',
            blockingReason: '待运营复盘'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('待治理产品')
    expect(wrapper.text()).toContain('待补阈值')
    expect(wrapper.text()).toContain('待补联动预案')
    expect(wrapper.text()).toContain('待运营复盘')
    expect(wrapper.text()).toContain('产品尚未进入治理主链路')
  })

  it('keeps long governance route ids as strings when loading filtered work items', async () => {
    mockRoute.query = {
      productId: '9223372036854775807',
      workItemCode: 'PENDING_CONTRACT_RELEASE',
      workStatus: 'OPEN'
    }
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })

    mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(mockPageWorkItems).toHaveBeenCalledWith(
      expect.objectContaining({
        productId: '9223372036854775807',
        workItemCode: 'PENDING_CONTRACT_RELEASE',
        workStatus: 'OPEN'
      })
    )
  })

  it('loads governance task work items with keyword and executionStatus from route query', async () => {
    mockRoute.query = {
      keyword: '2043187508765708289',
      executionStatus: 'PENDING_APPROVAL',
      workStatus: 'OPEN'
    }
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })

    mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(mockPageWorkItems).toHaveBeenCalledWith(
      expect.objectContaining({
        keyword: '2043187508765708289',
        executionStatus: 'PENDING_APPROVAL',
        workStatus: 'OPEN'
      })
    )
  })

  it('writes quick-search keywords back into the route query', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const input = wrapper.get('input[placeholder*="审批单号"]')
    await input.setValue('2043187508765708289')
    await input.trigger('keyup.enter')

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: expect.objectContaining({
        keyword: '2043187508765708289',
        workStatus: 'OPEN'
      })
    })
  })

  it('maps the pending approval preset to executionStatus=PENDING_APPROVAL', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const pendingApprovalButton = wrapper.findAll('button').find((button) => button.text() === '待审批')
    expect(pendingApprovalButton).toBeTruthy()
    await pendingApprovalButton!.trigger('click')

    expect(mockRouter.replace).toHaveBeenCalledWith({
      query: expect.objectContaining({
        executionStatus: 'PENDING_APPROVAL',
        workStatus: 'OPEN'
      })
    })
  })

  it('renders approvalOrderId ahead of productKey in the task anchor and shows recommended counts', async () => {
    mockRoute.query = { view: 'recommended', workStatus: 'OPEN' }
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            workItemCode: 'PENDING_CONTRACT_RELEASE',
            workStatus: 'OPEN',
            priorityLevel: 'P1',
            approvalOrderId: '2043187508765708289',
            productKey: 'phase2-gnss',
            recommendation: {
              suggestedAction: '去审批'
            }
          },
          {
            id: 2,
            workItemCode: 'PENDING_RISK_BINDING',
            workStatus: 'OPEN',
            priorityLevel: 'P3',
            productKey: 'phase1-crack'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('2043187508765708289')
    expect(wrapper.text()).toContain('推荐先处理 1 项')
  })

  it('dispatches pending contract release work items into the product contract workspace', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 11,
            workItemCode: 'PENDING_CONTRACT_RELEASE',
            workStatus: 'OPEN',
            productId: 1001,
            productKey: 'phase2-gnss',
            blockingReason: '合同尚未发布'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/products',
      query: {
        openProductId: '1001',
        workbenchView: 'models',
        governanceSource: 'task',
        workItemCode: 'PENDING_CONTRACT_RELEASE'
      }
    })
  })

  it('dispatches pending product governance work items into the product workspace with collector-child context', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 10,
            workItemCode: 'PENDING_PRODUCT_GOVERNANCE',
            workStatus: 'OPEN',
            productId: 6006,
            blockingReason: '采集器产品尚未进入治理主链路',
            snapshotJson: JSON.stringify({
              governanceBoundary: 'collector-child',
              subjectOwnership: 'collector',
              governanceFocus: 'collector-runtime',
              dispatchPath: '/products'
            })
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/products',
      query: {
        openProductId: '6006',
        workbenchView: 'models',
        governanceSource: 'task',
        workItemCode: 'PENDING_PRODUCT_GOVERNANCE',
        governanceBoundary: 'collector-child',
        subjectOwnership: 'collector',
        governanceFocus: 'collector-runtime'
      }
    })
  })

  it('dispatches pending risk binding work items into the risk-point pending promotion workspace', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 12,
            workItemCode: 'PENDING_RISK_BINDING',
            workStatus: 'OPEN',
            subjectId: 3001,
            deviceCode: 'DEVICE-3001',
            blockingReason: '风险点绑定缺口待收口',
            snapshotJson: JSON.stringify({
              riskPointId: 3001,
              riskPointName: '北坡GNSS-01',
              governanceBoundary: 'collector-child',
              subjectOwnership: 'child'
            })
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/risk-point',
      query: {
        openRiskPointId: '3001',
        bindingAction: 'pending-promotion',
        keyword: '北坡GNSS-01',
        governanceSource: 'task',
        workItemCode: 'PENDING_RISK_BINDING',
        governanceBoundary: 'collector-child',
        subjectOwnership: 'child'
      }
    })
  })

  it('dispatches pending threshold policy work items into the rule-definition create workspace', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 13,
            workItemCode: 'PENDING_THRESHOLD_POLICY',
            workStatus: 'OPEN',
            riskMetricId: 6102,
            blockingReason: '风险点已绑定，待补阈值策略',
            snapshotJson: JSON.stringify({
              riskPointDeviceId: 8801,
              riskPointId: 3001,
              deviceId: 5001,
              deviceCode: 'DEVICE-5001',
              metricIdentifier: 'displacementX',
              metricName: '位移 X',
              governanceBoundary: 'collector-child',
              subjectOwnership: 'child'
            })
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/rule-definition',
      query: {
        governanceAction: 'create',
        governanceSource: 'task',
        workItemCode: 'PENDING_THRESHOLD_POLICY',
        riskMetricId: '6102',
        metricIdentifier: 'displacementX',
        metricName: '位移 X',
        governanceBoundary: 'collector-child',
        subjectOwnership: 'child'
      }
    })
  })

  it('dispatches pending linkage-plan work items into the linkage-rule create workspace', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 14,
            workItemCode: 'PENDING_LINKAGE_PLAN',
            workStatus: 'OPEN',
            riskMetricId: 6102,
            blockingReason: '已纳管指标待补联动规则',
            snapshotJson: JSON.stringify({
              coverageType: 'LINKAGE',
              dimensionKey: 'LINKAGE:6102',
              metricIdentifier: 'displacementX',
              metricName: '位移 X'
            })
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/linkage-rule',
      query: {
        governanceAction: 'create',
        governanceSource: 'task',
        workItemCode: 'PENDING_LINKAGE_PLAN',
        coverageType: 'LINKAGE',
        dimensionKey: 'LINKAGE:6102',
        riskMetricId: '6102',
        metricIdentifier: 'displacementX',
        metricName: '位移 X'
      }
    })
  })

  it('dispatches pending emergency-plan work items into the emergency-plan create workspace', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 15,
            workItemCode: 'PENDING_LINKAGE_PLAN',
            workStatus: 'OPEN',
            riskMetricId: 6203,
            blockingReason: '已纳管指标待补应急预案',
            snapshotJson: JSON.stringify({
              coverageType: 'EMERGENCY_PLAN',
              dimensionKey: 'EMERGENCY:6203',
              metricIdentifier: 'value',
              metricName: '裂缝值'
            })
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const dispatchButton = wrapper.findAll('button').find((button) => button.text() === '去处理')
    expect(dispatchButton).toBeTruthy()

    await dispatchButton!.trigger('click')

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/emergency-plan',
      query: {
        governanceAction: 'create',
        governanceSource: 'task',
        workItemCode: 'PENDING_LINKAGE_PLAN',
        coverageType: 'EMERGENCY_PLAN',
        dimensionKey: 'EMERGENCY:6203',
        riskMetricId: '6203',
        metricIdentifier: 'value',
        metricName: '裂缝值'
      }
    })
  })

  it('loads governance replay from pending replay work items by resolving product key', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 5,
            workItemCode: 'PENDING_REPLAY',
            workStatus: 'OPEN',
            productId: 3001,
            blockingReason: '风险指标缺阈值策略，待运营复盘',
            snapshotJson: JSON.stringify({
              metricIdentifier: 'gpsTotalX',
              dimensionLabel: 'GNSS 累计位移 X'
            })
          }
        ]
      }
    })
    mockGetProductById.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 3001,
        productKey: 'phase2-gnss'
      }
    })
    mockGetRiskGovernanceReplay.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        productKey: 'phase2-gnss',
        matchedMessageCount: 2,
        matchedAccessErrorCount: 0,
        gapSummary: {
          missingBindingCount: 0,
          missingPolicyCount: 1,
          missingRiskMetricCount: 0
        },
        replayChainSteps: [
          {
            stepCode: 'GOVERNANCE_GAPS',
            stepName: '治理缺口',
            status: 'ACTION_REQUIRED',
            summary: '仍有 1 条待补策略',
            nextAction: '继续补齐阈值策略'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const replayButton = wrapper.findAll('button').find((button) => button.text() === '复盘')
    expect(replayButton).toBeTruthy()

    await replayButton!.trigger('click')
    await flushPromises()

    expect(mockGetProductById).toHaveBeenCalledWith(3001)
    expect(mockGetRiskGovernanceReplay).toHaveBeenCalledWith({
      productKey: 'phase2-gnss'
    })
    expect(wrapper.text()).toContain('治理链路复盘')
    expect(wrapper.text()).toContain('phase2-gnss')
    expect(wrapper.text()).toContain('待补策略 1')
    expect(wrapper.text()).toContain('GOVERNANCE_GAPS')
    expect(wrapper.text()).toContain('继续补齐阈值策略')
  })

  it('submits replay closeout explicitly from governance task replay drawer', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 19,
            workItemCode: 'PENDING_REPLAY',
            workStatus: 'OPEN',
            approvalOrderId: 8201,
            releaseBatchId: 7001,
            productKey: 'phase2-gnss',
            blockingReason: '待运营复盘',
            recommendation: {
              recommendationType: 'PROMOTE',
              suggestedAction: 'Promote pending binding'
            }
          }
        ]
      }
    })
    mockGetRiskGovernanceReplay.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        productKey: 'phase2-gnss',
        releaseBatchId: 7001,
        matchedMessageCount: 2,
        gapSummary: {
          missingBindingCount: 0,
          missingPolicyCount: 1,
          missingRiskMetricCount: 0
        }
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const replayButton = wrapper.findAll('button').find((button) => button.text() === '复盘')
    expect(replayButton).toBeTruthy()

    await replayButton!.trigger('click')
    await flushPromises()

    expect(mockSubmitGovernanceReplayFeedback).not.toHaveBeenCalled()

    await wrapper.get('[data-testid=\"task-replay-adopted-decision\"]').setValue('PROMOTE')
    await wrapper.get('[data-testid=\"task-replay-execution-outcome\"]').setValue('SUCCESS')
    await wrapper.get('[data-testid=\"task-replay-root-cause\"]').setValue('MISSING_POLICY')
    await wrapper.get('[data-testid=\"task-replay-operator-summary\"]').setValue('复盘确认缺少阈值策略')

    const submitButton = wrapper.findAll('button').find((button) => button.text() === '提交复盘结论')
    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mockSubmitGovernanceReplayFeedback).toHaveBeenCalledWith({
      workItemId: 19,
      approvalOrderId: 8201,
      releaseBatchId: 7001,
      productKey: 'phase2-gnss',
      recommendedDecision: 'PROMOTE',
      adoptedDecision: 'PROMOTE',
      executionOutcome: 'SUCCESS',
      rootCauseCode: 'MISSING_POLICY',
      operatorSummary: '复盘确认缺少阈值策略'
    })
  })

  it('renders unified recommendation evidence and impact on governance task cards', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 16,
            workItemCode: 'PENDING_RISK_BINDING',
            workStatus: 'OPEN',
            blockingReason: 'Pending promotion evidence ready',
            recommendation: {
              recommendationType: 'PROMOTE',
              confidence: 0.92,
              reasonCodes: ['LOW_BINDING_COVERAGE'],
              suggestedAction: 'Promote pending binding',
              evidenceItems: [
                { evidenceType: 'RUNTIME_PAYLOAD', title: 'Payload 1', summary: 'gpsTotalX pending' },
                { evidenceType: 'CATALOG_DIFF', title: 'Catalog 2', summary: 'metric missing' },
                { evidenceType: 'APPROVAL_TRACE', title: 'Trace 3', summary: 'approval linked' },
                { evidenceType: 'SHOULD_NOT_RENDER', title: 'Hidden 4', summary: 'should stay hidden' }
              ]
            },
            impact: {
              affectedCount: 3,
              affectedTypes: ['RISK_POINT', 'DEVICE'],
              rollbackable: true,
              rollbackPlanSummary: 'Can revert pending promotion'
            }
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('0.92')
    expect(wrapper.text()).toContain('RUNTIME_PAYLOAD')
    expect(wrapper.text()).toContain('CATALOG_DIFF')
    expect(wrapper.text()).toContain('APPROVAL_TRACE')
    expect(wrapper.text()).not.toContain('SHOULD_NOT_RENDER')
    expect(wrapper.text()).toContain('Can revert pending promotion')
  })

  it('loads governance decision context and renders deterministic priority explanations', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 18,
            workItemCode: 'PENDING_CONTRACT_RELEASE',
            workStatus: 'OPEN',
            priorityLevel: 'P1',
            blockingReason: '待发布合同影响多个下游模块'
          }
        ]
      }
    })
    mockGetWorkItemDecisionContext.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        workItemId: 18,
        priorityLevel: 'P1',
        problemSummary: '待发布合同影响多个下游模块',
        reasonCodes: ['LOW_BINDING_COVERAGE', 'HIGH_IMPACT_RELEASE'],
        affectedModules: ['PRODUCT', 'RISK_POINT', 'RULE'],
        recommendedAction: 'Publish contract release',
        affectedCount: 5,
        rollbackable: true,
        rollbackPlanSummary: 'Can rollback contract release'
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('P1')

    const decisionButton = wrapper.findAll('button').find((button) => button.text() === '决策说明')
    expect(decisionButton).toBeTruthy()

    await decisionButton!.trigger('click')
    await flushPromises()

    expect(mockGetWorkItemDecisionContext).toHaveBeenCalledWith(18)
    expect(wrapper.text()).toContain('LOW_BINDING_COVERAGE')
    expect(wrapper.text()).toContain('HIGH_IMPACT_RELEASE')
    expect(wrapper.text()).toContain('PRODUCT')
    expect(wrapper.text()).toContain('RISK_POINT')
    expect(wrapper.text()).toContain('RULE')
    expect(wrapper.text()).toContain('Publish contract release')
  })

  it('renders governance ops rows from backend alerts', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 11,
            alertType: 'FIELD_DRIFT',
            alertTitle: '字段漂移告警',
            alertMessage: 'value 已偏离正式合同'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    expect(wrapper.text()).toContain('字段漂移告警')
    expect(wrapper.text()).toContain('value 已偏离正式合同')
  })

  it('renders unified recommendation evidence and impact on governance ops cards', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 17,
            alertType: 'FIELD_DRIFT',
            alertTitle: 'Field drift alert',
            alertMessage: 'value drift detected',
            recommendation: {
              recommendationType: 'PUBLISH',
              confidence: 0.92,
              reasonCodes: ['FIELD_DRIFT'],
              suggestedAction: 'Publish contract update',
              evidenceItems: [
                { evidenceType: 'RUNTIME_PAYLOAD', title: 'Payload 1', summary: 'value drift detected' },
                { evidenceType: 'CONTRACT_DIFF', title: 'Contract 2', summary: 'formal contract mismatch' },
                { evidenceType: 'TRACE_LINK', title: 'Trace 3', summary: 'trace evidence ready' },
                { evidenceType: 'SHOULD_NOT_RENDER', title: 'Hidden 4', summary: 'should stay hidden' }
              ]
            },
            impact: {
              affectedCount: 2,
              affectedTypes: ['PRODUCT', 'RISK_POINT'],
              rollbackable: true,
              rollbackPlanSummary: 'Can re-publish previous contract'
            }
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    expect(wrapper.text()).toContain('0.92')
    expect(wrapper.text()).toContain('RUNTIME_PAYLOAD')
    expect(wrapper.text()).toContain('CONTRACT_DIFF')
    expect(wrapper.text()).toContain('TRACE_LINK')
    expect(wrapper.text()).not.toContain('SHOULD_NOT_RENDER')
    expect(wrapper.text()).toContain('Can re-publish previous contract')
  })

  it('loads governance replay from ops alerts with replay context', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 12,
            alertType: 'CONTRACT_DIFF',
            alertTitle: '合同差异告警',
            alertMessage: 'gpsTotalX 与正式合同存在差异',
            releaseBatchId: 7001,
            traceId: 'trace-ops-1',
            deviceCode: 'device-ops-1',
            productKey: 'phase2-gnss'
          }
        ]
      }
    })
    mockGetRiskGovernanceReplay.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-ops-1',
        deviceCode: 'device-ops-1',
        productKey: 'phase2-gnss',
        releaseBatchId: 7001,
        matchedMessageCount: 4,
        matchedAccessErrorCount: 1,
        gapSummary: {
          missingBindingCount: 2,
          missingPolicyCount: 1,
          missingRiskMetricCount: 0
        },
        batchReconciliation: {
          consistent: false,
          missingCurrentFieldCount: 1,
          extraCurrentFieldCount: 0
        },
        replayChainSteps: [
          {
            stepCode: 'MESSAGE_TRACE',
            stepName: '消息追踪',
            status: 'READY',
            summary: '已命中 4 条消息',
            nextAction: '继续核对最近消息'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    const replayButton = wrapper.findAll('button').find((button) => button.text() === '复盘')
    expect(replayButton).toBeTruthy()

    await replayButton!.trigger('click')
    await flushPromises()

    expect(mockGetRiskGovernanceReplay).toHaveBeenCalledWith({
      traceId: 'trace-ops-1',
      deviceCode: 'device-ops-1',
      productKey: 'phase2-gnss',
      releaseBatchId: 7001
    })
    expect(wrapper.text()).toContain('治理链路复盘')
    expect(wrapper.text()).toContain('发布批次 7001')
    expect(wrapper.text()).toContain('待补绑定 2')
    expect(wrapper.text()).toContain('MESSAGE_TRACE')
    expect(wrapper.text()).toContain('继续核对最近消息')
  })

  it('submits replay closeout explicitly from governance ops replay drawer', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 22,
            alertType: 'CONTRACT_DIFF',
            alertTitle: '合同差异告警',
            alertMessage: 'gpsTotalX 与正式合同存在差异',
            releaseBatchId: 7002,
            traceId: 'trace-ops-2',
            deviceCode: 'device-ops-2',
            productKey: 'phase1-crack',
            recommendation: {
              recommendationType: 'IGNORE',
              suggestedAction: 'Ignore current recommendation'
            }
          }
        ]
      }
    })
    mockGetRiskGovernanceReplay.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        traceId: 'trace-ops-2',
        deviceCode: 'device-ops-2',
        productKey: 'phase1-crack',
        releaseBatchId: 7002,
        matchedMessageCount: 1,
        gapSummary: {
          missingBindingCount: 0,
          missingPolicyCount: 1,
          missingRiskMetricCount: 0
        }
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    const replayButton = wrapper.findAll('button').find((button) => button.text() === '复盘')
    expect(replayButton).toBeTruthy()

    await replayButton!.trigger('click')
    await flushPromises()

    expect(mockSubmitGovernanceReplayFeedback).not.toHaveBeenCalled()

    await wrapper.get('[data-testid=\"ops-replay-adopted-decision\"]').setValue('CREATE_POLICY')
    await wrapper.get('[data-testid=\"ops-replay-execution-outcome\"]').setValue('SUCCESS')
    await wrapper.get('[data-testid=\"ops-replay-root-cause\"]').setValue('MISSING_POLICY')
    await wrapper.get('[data-testid=\"ops-replay-operator-summary\"]').setValue('运维复盘确认需要补齐阈值策略')

    const submitButton = wrapper.findAll('button').find((button) => button.text() === '提交复盘结论')
    expect(submitButton).toBeTruthy()

    await submitButton!.trigger('click')
    await flushPromises()

    expect(mockSubmitGovernanceReplayFeedback).toHaveBeenCalledWith({
      releaseBatchId: 7002,
      traceId: 'trace-ops-2',
      deviceCode: 'device-ops-2',
      productKey: 'phase1-crack',
      recommendedDecision: 'IGNORE',
      adoptedDecision: 'CREATE_POLICY',
      executionOutcome: 'SUCCESS',
      rootCauseCode: 'MISSING_POLICY',
      operatorSummary: '运维复盘确认需要补齐阈值策略'
    })
  })

  it('executes governance task actions from card buttons', async () => {
    mockPageWorkItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 101,
            workItemCode: 'PENDING_CONTRACT_RELEASE',
            workStatus: 'OPEN',
            blockingReason: '合同尚未发布'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceTaskView)
    await flushPromises()

    const ackButton = wrapper.findAll('button').find((button) => button.text() === '确认')
    const blockButton = wrapper.findAll('button').find((button) => button.text() === '阻塞')
    const closeButton = wrapper.findAll('button').find((button) => button.text() === '关闭')

    expect(ackButton).toBeTruthy()
    expect(blockButton).toBeTruthy()
    expect(closeButton).toBeTruthy()

    await ackButton!.trigger('click')
    await flushPromises()
    await blockButton!.trigger('click')
    await flushPromises()
    await closeButton!.trigger('click')
    await flushPromises()

    expect(mockConfirmAction).toHaveBeenCalledTimes(3)
    expect(mockAckWorkItem).toHaveBeenCalledWith(101, { comment: '治理任务已确认，进入跟进状态。' })
    expect(mockBlockWorkItem).toHaveBeenCalledWith(101, { comment: '治理任务存在阻塞，待补外部条件。' })
    expect(mockCloseWorkItem).toHaveBeenCalledWith(101, { comment: '治理任务已人工关闭。' })
    expect(mockMessageSuccess).toHaveBeenCalled()
  })

  it('executes governance ops actions from card buttons', async () => {
    mockPageOpsAlerts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 201,
            alertType: 'MISSING_RISK_METRIC',
            alertTitle: '风险指标缺失告警',
            alertMessage: 'gpsTotalX 缺少治理指标'
          }
        ]
      }
    })

    const wrapper = mountWithStubs(GovernanceOpsWorkbenchView)
    await flushPromises()

    const ackButton = wrapper.findAll('button').find((button) => button.text() === '确认')
    const suppressButton = wrapper.findAll('button').find((button) => button.text() === '抑制')
    const closeButton = wrapper.findAll('button').find((button) => button.text() === '关闭')

    expect(ackButton).toBeTruthy()
    expect(suppressButton).toBeTruthy()
    expect(closeButton).toBeTruthy()

    await ackButton!.trigger('click')
    await flushPromises()
    await suppressButton!.trigger('click')
    await flushPromises()
    await closeButton!.trigger('click')
    await flushPromises()

    expect(mockConfirmAction).toHaveBeenCalledTimes(3)
    expect(mockAckOpsAlert).toHaveBeenCalledWith(201, { comment: '治理告警已确认，进入持续跟进。' })
    expect(mockSuppressOpsAlert).toHaveBeenCalledWith(201, { comment: '治理告警暂时抑制，等待下一轮检测。' })
    expect(mockCloseOpsAlert).toHaveBeenCalledWith(201, { comment: '治理告警已人工关闭。' })
    expect(mockMessageSuccess).toHaveBeenCalled()
  })
})
