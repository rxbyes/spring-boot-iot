import { computed, defineComponent, inject, provide } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import GovernanceApprovalView from '@/views/GovernanceApprovalView.vue'

const {
  mockPageOrders,
  mockGetOrderDetail,
  mockApproveOrder,
  mockRejectOrder,
  mockCancelOrder,
  mockResubmitOrder,
  mockConfirmAction,
  mockPermissionStore
} = vi.hoisted(() => ({
  mockPageOrders: vi.fn(),
  mockGetOrderDetail: vi.fn(),
  mockApproveOrder: vi.fn(),
  mockRejectOrder: vi.fn(),
  mockCancelOrder: vi.fn(),
  mockResubmitOrder: vi.fn(),
  mockConfirmAction: vi.fn(),
  mockPermissionStore: {
    userInfo: {
      id: 2002
    }
  }
}))

vi.mock('@/api/governanceApproval', () => ({
  governanceApprovalApi: {
    pageOrders: mockPageOrders,
    getOrderDetail: mockGetOrderDetail,
    approveOrder: mockApproveOrder,
    rejectOrder: mockRejectOrder,
    cancelOrder: mockCancelOrder,
    resubmitOrder: mockResubmitOrder
  }
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => mockPermissionStore
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: mockConfirmAction,
  isConfirmCancelled: vi.fn(() => false)
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  }
})

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="governance-approval-workbench-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="filters" /></div>
      <div><slot name="applied-filters" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="governance-approval-filter-stub">
      <div><slot name="primary" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="governance-approval-toolbar-stub">
      <div class="governance-approval-toolbar-stub__meta">{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
})

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

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['directItems', 'menuItems'],
  emits: ['command'],
  template: `
    <div class="governance-approval-row-actions-stub">
      <button
        v-for="item in directItems || []"
        :key="item.key || item.command"
        type="button"
        :disabled="Boolean(item.disabled)"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <span class="governance-approval-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </div>
  `
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  template: '<div class="governance-approval-pagination-stub" />'
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'title', 'subtitle'],
  template: `
    <section v-if="modelValue" class="governance-approval-detail-drawer-stub">
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle'],
  template: `
    <section v-if="modelValue" class="governance-approval-form-drawer-stub">
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  props: ['confirmText', 'cancelText', 'confirmLoading'],
  emits: ['cancel', 'confirm'],
  template: `
    <div class="governance-approval-drawer-footer-stub">
      <button type="button" @click="$emit('cancel')">{{ cancelText || '取消' }}</button>
      <button type="button" :disabled="Boolean(confirmLoading)" @click="$emit('confirm')">
        {{ confirmText || '确定' }}
      </button>
    </div>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type', 'placeholder'],
  emits: ['update:modelValue'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      class="el-input-stub"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      class="el-input-stub"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  template: `
    <div class="el-form-item-stub">
      <slot />
    </div>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('governanceApprovalTableRows', computed(() => props.data ?? []))
    return {}
  },
  template: '<section class="governance-approval-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  setup() {
    const rows = inject<any[]>('governanceApprovalTableRows', [])
    return { rows }
  },
  template: `
    <div class="governance-approval-column-stub" :data-label="label">
      <div v-for="(row, index) in rows" :key="index">
        <slot :row="row" />
      </div>
    </div>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  template: '<div class="governance-approval-text-column-stub">{{ label }}</div>'
})

function mountView() {
  return mount(GovernanceApprovalView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardButton: StandardButtonStub,
        StandardPagination: StandardPaginationStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardAppliedFiltersBar: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        EmptyState: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElInput: ElInputStub,
        ElFormItem: ElFormItemStub,
        ElTag: true,
        ElSelect: true,
        ElOption: true
      }
    }
  })
}

function createPendingOrder() {
  return {
    id: 88001,
    actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
    actionName: '合同发布',
    subjectType: 'PRODUCT',
    subjectId: 1001,
    status: 'PENDING',
    operatorUserId: 1001,
    approverUserId: 2002,
    createTime: '2026-04-08 09:00:00'
  }
}

function createRejectedOrder() {
  return {
    id: 88002,
    actionCode: 'PRODUCT_CONTRACT_RELEASE_APPLY',
    actionName: '合同发布',
    subjectType: 'PRODUCT',
    subjectId: 1002,
    status: 'REJECTED',
    operatorUserId: 1001,
    approverUserId: 2002,
    approvalComment: '字段描述不完整',
    createTime: '2026-04-08 10:00:00'
  }
}

describe('GovernanceApprovalView', () => {
  beforeEach(() => {
    mockPageOrders.mockReset()
    mockGetOrderDetail.mockReset()
    mockApproveOrder.mockReset()
    mockRejectOrder.mockReset()
    mockCancelOrder.mockReset()
    mockResubmitOrder.mockReset()
    mockConfirmAction.mockReset()
    mockConfirmAction.mockResolvedValue(undefined)
    mockPermissionStore.userInfo.id = 2002

    mockPageOrders.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createPendingOrder()]
      }
    })
    mockGetOrderDetail.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        order: {
          ...createPendingOrder(),
          payloadJson: JSON.stringify({
            version: 1,
            request: {
              productId: 1001
            },
            execution: {
              executedAt: '2026-04-08T10:00:00Z',
              result: {
                releaseBatchId: 99001,
                createdCount: 1
              }
            }
          })
        },
        transitions: [
          {
            id: 1,
            fromStatus: null,
            toStatus: 'PENDING',
            actorUserId: 1001,
            transitionComment: 'submit',
            createTime: '2026-04-08 09:00:00'
          }
        ]
      }
    })
  })

  it('loads approval orders and renders pending status', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(mockPageOrders).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 10
    })
    expect(wrapper.text()).toContain('治理审批台')
    expect(wrapper.text()).toContain('合同发布')
    expect(wrapper.text()).toContain('待审批')
  })

  it('opens detail drawer and renders execution result from payloadJson', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text().includes('详情'))?.trigger('click')
    await flushPromises()

    expect(mockGetOrderDetail).toHaveBeenCalledWith(88001)
    expect(wrapper.text()).toContain('审批概览')
    expect(wrapper.text()).toContain('99001')
    expect(wrapper.text()).toContain('submit')
  })

  it('submits approve action from the action drawer', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text().includes('通过'))?.trigger('click')
    await flushPromises()

    const commentInput = wrapper.find('textarea.el-input-stub')
    await commentInput.setValue('审批通过')
    await wrapper.findAll('button').find((button) => button.text().includes('确认通过'))?.trigger('click')
    await flushPromises()

    expect(mockConfirmAction).toHaveBeenCalled()
    expect(mockApproveOrder).toHaveBeenCalledWith(88001, {
      comment: '审批通过'
    })
  })

  it('supports resubmitting a rejected order for the original operator', async () => {
    mockPermissionStore.userInfo.id = 1001
    mockPageOrders.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRejectedOrder()]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    await wrapper.findAll('button').find((button) => button.text().includes('原单重提'))?.trigger('click')
    await flushPromises()

    const actionDrawer = wrapper.find('.governance-approval-form-drawer-stub')
    const approverInput = actionDrawer.find('input.el-input-stub')
    await approverInput.setValue('3003')
    const commentInput = actionDrawer.find('textarea.el-input-stub')
    await commentInput.setValue('重新指定复核人后提交')
    await wrapper.findAll('button').find((button) => button.text().includes('确认重提'))?.trigger('click')
    await flushPromises()

    expect(mockResubmitOrder).toHaveBeenCalledWith(88002, {
      approverUserId: 3003,
      comment: '重新指定复核人后提交'
    })
  })
})
