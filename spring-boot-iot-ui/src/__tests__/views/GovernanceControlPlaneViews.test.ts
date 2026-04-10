import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GovernanceTaskView from '@/views/GovernanceTaskView.vue'
import GovernanceOpsWorkbenchView from '@/views/GovernanceOpsWorkbenchView.vue'

const {
  mockPageWorkItems,
  mockAckWorkItem,
  mockBlockWorkItem,
  mockCloseWorkItem,
  mockGetProductById,
  mockPageOpsAlerts,
  mockGetRiskGovernanceReplay,
  mockAckOpsAlert,
  mockSuppressOpsAlert,
  mockCloseOpsAlert,
  mockConfirmAction,
  mockMessageSuccess,
  mockMessageError
} = vi.hoisted(() => ({
  mockPageWorkItems: vi.fn(),
  mockAckWorkItem: vi.fn(),
  mockBlockWorkItem: vi.fn(),
  mockCloseWorkItem: vi.fn(),
  mockGetProductById: vi.fn(),
  mockPageOpsAlerts: vi.fn(),
  mockGetRiskGovernanceReplay: vi.fn(),
  mockAckOpsAlert: vi.fn(),
  mockSuppressOpsAlert: vi.fn(),
  mockCloseOpsAlert: vi.fn(),
  mockConfirmAction: vi.fn(),
  mockMessageSuccess: vi.fn(),
  mockMessageError: vi.fn()
}))

vi.mock('@/api/governanceWorkItem', () => ({
  pageGovernanceWorkItems: mockPageWorkItems,
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
  getRiskGovernanceReplay: mockGetRiskGovernanceReplay
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {}
  }),
  useRouter: () => ({
    push: vi.fn()
  })
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
        PanelCard: PanelCardStub,
        StandardDetailDrawer: StandardDetailDrawerStub
      }
    }
  })
}

describe('governance control plane views', () => {
  beforeEach(() => {
    mockPageWorkItems.mockReset()
    mockAckWorkItem.mockReset()
    mockBlockWorkItem.mockReset()
    mockCloseWorkItem.mockReset()
    mockGetProductById.mockReset()
    mockPageOpsAlerts.mockReset()
    mockGetRiskGovernanceReplay.mockReset()
    mockAckOpsAlert.mockReset()
    mockSuppressOpsAlert.mockReset()
    mockCloseOpsAlert.mockReset()
    mockConfirmAction.mockReset()
    mockMessageSuccess.mockReset()
    mockMessageError.mockReset()
    mockConfirmAction.mockResolvedValue(undefined)
    mockAckWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockBlockWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockCloseWorkItem.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockAckOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockSuppressOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockCloseOpsAlert.mockResolvedValue({ code: 200, msg: 'success', data: null })
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
