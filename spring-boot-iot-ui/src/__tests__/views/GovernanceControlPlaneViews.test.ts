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
  mockPageOpsAlerts,
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
  mockPageOpsAlerts: vi.fn(),
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

vi.mock('@/api/governanceOpsAlert', () => ({
  pageGovernanceOpsAlerts: mockPageOpsAlerts,
  ackGovernanceOpsAlert: mockAckOpsAlert,
  suppressGovernanceOpsAlert: mockSuppressOpsAlert,
  closeGovernanceOpsAlert: mockCloseOpsAlert
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

function mountWithStubs(component: Parameters<typeof mount>[0]) {
  return mount(component, {
    global: {
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardButton: StandardButtonStub,
        PanelCard: PanelCardStub
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
    mockPageOpsAlerts.mockReset()
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
