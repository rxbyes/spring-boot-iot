import { computed, defineComponent, Fragment, h, inject, nextTick, provide, type ComputedRef, type VNode } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('@/api/inAppMessage', async () => {
  const actual = await vi.importActual<typeof import('@/api/inAppMessage')>('@/api/inAppMessage')
  return {
    ...actual,
    addInAppMessage: vi.fn(),
    deleteInAppMessage: vi.fn(),
    getInAppMessage: vi.fn(),
    getInAppMessageBridgeStats: vi.fn(),
    getInAppMessageStats: vi.fn(),
    listInAppMessageBridgeAttempts: vi.fn(),
    pageInAppMessageBridgeLogs: vi.fn(),
    pageInAppMessages: vi.fn(),
    updateInAppMessage: vi.fn()
  }
})

vi.mock('@/api/channel', async () => {
  const actual = await vi.importActual<typeof import('@/api/channel')>('@/api/channel')
  return {
    ...actual,
    listChannels: vi.fn()
  }
})

vi.mock('@/api/role', async () => {
  const actual = await vi.importActual<typeof import('@/api/role')>('@/api/role')
  return {
    ...actual,
    listRoles: vi.fn()
  }
})

vi.mock('@/api/user', async () => {
  const actual = await vi.importActual<typeof import('@/api/user')>('@/api/user')
  return {
    ...actual,
    listUsers: vi.fn()
  }
})

vi.mock('@/utils/sectionWorkspaces', () => ({
  listWorkspaceCommandEntries: vi.fn(() => [
    {
      type: 'page',
      path: '/system-log',
      workspaceLabel: '平台治理',
      title: '系统日志'
    }
  ])
}))

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      error: vi.fn(),
      info: vi.fn(),
      success: vi.fn()
    }
  }
})

import InAppMessageView from '@/views/InAppMessageView.vue'
import {
  getInAppMessage,
  getInAppMessageBridgeStats,
  getInAppMessageStats,
  listInAppMessageBridgeAttempts,
  pageInAppMessageBridgeLogs,
  pageInAppMessages
} from '@/api/inAppMessage'
import { createRequestError } from '@/api/request'
import { listChannels } from '@/api/channel'
import { listRoles } from '@/api/role'
import { listUsers } from '@/api/user'

const flushPromises = async () => {
  await Promise.resolve()
  await nextTick()
  await Promise.resolve()
  await nextTick()
}

const collapseStateKey = Symbol('collapse-state')

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: ['disabled', 'loading', 'type', 'link', 'icon'],
  emits: ['click'],
  template: `
    <button
      class="el-button-stub"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'type', 'placeholder', 'disabled'],
  emits: ['update:modelValue', 'keyup.enter'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      class="el-input-stub"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="Boolean(disabled)"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      class="el-input-stub"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="Boolean(disabled)"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('keyup.enter')"
    />
  `
})

const ElSelectStub = defineComponent({
  name: 'ElSelect',
  props: ['modelValue', 'multiple', 'placeholder', 'disabled'],
  emits: ['update:modelValue'],
  methods: {
    handleChange(event: Event) {
      const target = event.target as HTMLSelectElement
      if (this.multiple) {
        const values = Array.from(target.selectedOptions).map((option) => option.value)
        this.$emit('update:modelValue', values)
        return
      }
      const rawValue = target.value
      if (!rawValue) {
        this.$emit('update:modelValue', undefined)
        return
      }
      this.$emit('update:modelValue', /^-?\d+$/.test(rawValue) ? Number(rawValue) : rawValue)
    }
  },
  template: `
    <select
      class="el-select-stub"
      :multiple="Boolean(multiple)"
      :disabled="Boolean(disabled)"
      :value="modelValue ?? ''"
      :data-placeholder="placeholder"
      @change="handleChange"
    >
      <option v-if="!multiple" value=""></option>
      <slot />
    </select>
  `
})

const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: ['label', 'value'],
  template: '<option :value="value"><slot>{{ label }}</slot></option>'
})

const ElTagStub = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>'
})

const ElEmptyStub = defineComponent({
  name: 'ElEmpty',
  props: ['description'],
  template: '<div class="el-empty-stub">{{ description }}</div>'
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  template: '<form class="el-form-stub"><slot /></form>'
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: ['label'],
  template: `
    <div class="el-form-item-stub">
      <span v-if="label" class="el-form-item-stub__label">{{ label }}</span>
      <slot />
    </div>
  `
})

const ElRowStub = defineComponent({
  name: 'ElRow',
  template: '<div class="el-row-stub"><slot /></div>'
})

const ElColStub = defineComponent({
  name: 'ElCol',
  template: '<div class="el-col-stub"><slot /></div>'
})

const ElDatePickerStub = defineComponent({
  name: 'ElDatePicker',
  props: ['modelValue', 'placeholder', 'startPlaceholder', 'endPlaceholder'],
  template: `
    <div
      class="el-date-picker-stub"
      :data-placeholder="startPlaceholder || placeholder || ''"
      :data-end-placeholder="endPlaceholder || ''"
    >
      {{ Array.isArray(modelValue) ? modelValue.join(' ~ ') : modelValue }}
    </div>
  `
})

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  props: ['title'],
  template: '<div class="el-alert-stub">{{ title }}</div>'
})

const ElRadioGroupStub = defineComponent({
  name: 'ElRadioGroup',
  template: '<div class="el-radio-group-stub"><slot /></div>'
})

const ElRadioStub = defineComponent({
  name: 'ElRadio',
  template: '<label class="el-radio-stub"><slot /></label>'
})

const ElInputNumberStub = defineComponent({
  name: 'ElInputNumber',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: `
    <input
      class="el-input-number-stub"
      type="number"
      :value="modelValue"
      @input="$emit('update:modelValue', Number($event.target.value))"
    />
  `
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['prop', 'label', 'type'],
  setup() {
    return () => null
  }
})

function normalizeNodes(nodes: VNode[]): VNode[] {
  const result: VNode[] = []
  nodes.forEach((node) => {
    if (!node) {
      return
    }
    if (node.type === Fragment && Array.isArray(node.children)) {
      result.push(...normalizeNodes(node.children as VNode[]))
      return
    }
    result.push(node)
  })
  return result
}

function collectTableColumns(nodes: VNode[]): VNode[] {
  return normalizeNodes(nodes).filter((node) => node.type !== Comment && node.props?.type !== 'selection')
}

function renderColumnContent(node: VNode, row: Record<string, unknown>, rowIndex: number) {
  const children = node.children as Record<string, ((scope: Record<string, unknown>) => VNode[]) | undefined> | null
  const defaultSlot = children && typeof children === 'object' ? children.default : undefined
  if (defaultSlot) {
    return defaultSlot({ row, $index: rowIndex })
  }
  const prop = node.props?.prop as string | undefined
  return prop ? String(row[prop] ?? '') : ''
}

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    emptyText: {
      type: String,
      default: '暂无数据'
    }
  },
  setup(props, { slots, expose }) {
    expose({
      clearSelection: vi.fn()
    })
    return () => {
      const columns = collectTableColumns(slots.default?.() as VNode[] || [])
      const rows = props.data as Array<Record<string, unknown>>

      if (rows.length === 0) {
        return h('div', { class: 'el-table-stub__empty' }, props.emptyText)
      }

      return h('table', { class: 'el-table-stub' }, [
        h('tbody', rows.map((row, rowIndex) => h(
          'tr',
          { class: 'el-table-stub__row', 'data-row-index': rowIndex },
          columns.map((column, columnIndex) => h(
            'td',
            {
              class: 'el-table-stub__cell',
              'data-column-index': columnIndex,
              'data-column-label': String(column.props?.label || '')
            },
            renderColumnContent(column, row, rowIndex)
          ))
        )))
      ])
    }
  }
})

const ElCollapseStub = defineComponent({
  name: 'ElCollapse',
  props: {
    modelValue: {
      type: [Array, String],
      default: () => []
    }
  },
  setup(props, { slots }) {
    provide(collapseStateKey, computed(() => Array.isArray(props.modelValue) ? props.modelValue : [props.modelValue]))
    return () => h('div', { class: 'el-collapse-stub' }, slots.default?.())
  }
})

const ElCollapseItemStub = defineComponent({
  name: 'ElCollapseItem',
  props: ['name'],
  setup(props, { slots }) {
    const activeNames = inject<ComputedRef<Array<string | number>>>(collapseStateKey, computed(() => []))
    return () => {
      const visible = activeNames.value.includes(props.name)
      return h('section', { class: 'el-collapse-item-stub' }, [
        h('div', { class: 'el-collapse-item-stub__title' }, slots.title?.()),
        visible ? h('div', { class: 'el-collapse-item-stub__body' }, slots.default?.()) : null
      ])
    }
  }
})

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="panel-card-stub">
      <header class="panel-card-stub__header">
        <slot name="header" />
        <template v-if="!$slots.header">
          <p v-if="eyebrow">{{ eyebrow }}</p>
          <h2 v-if="title">{{ title }}</h2>
          <p v-if="description">{{ description }}</p>
        </template>
      </header>
      <div class="panel-card-stub__body">
        <slot />
      </div>
    </section>
  `
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  props: ['currentPage', 'pageSize', 'total'],
  template: '<div class="standard-pagination-stub">{{ currentPage }} / {{ pageSize }} / {{ total }}</div>'
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: {
    metaItems: {
      type: Array,
      default: () => []
    }
  },
  template: `
    <div class="standard-table-toolbar-stub">
      <div class="standard-table-toolbar-stub__left">
        <span v-for="item in metaItems" :key="String(item)">{{ item }}</span>
      </div>
      <div class="standard-table-toolbar-stub__right">
        <slot name="right" />
      </div>
    </div>
  `
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle', 'loading', 'errorMessage', 'empty'],
  emits: ['update:modelValue'],
  template: `
    <section v-if="modelValue" class="standard-detail-drawer-stub">
      <h2>{{ title }}</h2>
      <p>{{ subtitle }}</p>
      <div v-if="loading">正在加载详情...</div>
      <div v-else-if="errorMessage">{{ errorMessage }}</div>
      <div v-else-if="empty">暂无详情数据</div>
      <slot v-else />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'eyebrow', 'title'],
  emits: ['update:modelValue', 'close'],
  template: `
    <section v-if="modelValue" class="standard-form-drawer-stub">
      <h2>{{ title }}</h2>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  template: '<div class="standard-drawer-footer-stub" />'
})

function buildMessageStats() {
  return {
    startTime: '2026-03-15 00:00:00',
    endTime: '2026-03-22 23:59:59',
    totalDeliveryCount: 80,
    totalReadCount: 40,
    totalUnreadCount: 40,
    readRate: 0.5,
    trend: [],
    messageTypeBuckets: [],
    sourceTypeBuckets: [
      {
        key: 'system_error',
        label: '系统异常',
        deliveryCount: 48,
        readCount: 20,
        unreadCount: 28,
        readRate: 0.416
      }
    ],
    topUnreadMessages: [
      {
        messageId: '101',
        title: '系统异常桥接',
        unreadCount: 12,
        unreadRate: 0.75,
        deliveryCount: 16,
        readCount: 4,
        messageType: 'error',
        sourceType: 'system_error',
        publishTime: '2026-03-22 09:00:00'
      }
    ]
  }
}

function buildBridgeStats() {
  return {
    startTime: '2026-03-15 00:00:00',
    endTime: '2026-03-22 23:59:59',
    totalBridgeCount: 1,
    successCount: 0,
    pendingRetryCount: 1,
    totalAttemptCount: 3,
    successRate: 0,
    trend: [
      {
        date: '2026-03-22',
        bridgeCount: 1,
        successCount: 0,
        pendingRetryCount: 1,
        totalAttemptCount: 3
      }
    ],
    channelBuckets: [
      {
        key: 'wechat-alert',
        label: '微信告警',
        channelType: 'wechat',
        bridgeCount: 1,
        successCount: 0,
        pendingRetryCount: 1,
        successRate: 0
      }
    ],
    sourceTypeBuckets: [
      {
        key: 'system_error',
        label: '系统异常',
        bridgeCount: 1,
        successCount: 0,
        pendingRetryCount: 1,
        successRate: 0
      }
    ]
  }
}

function buildBridgeLog() {
  return {
    id: '201',
    messageId: '101',
    title: '系统异常桥接',
    messageType: 'error',
    priority: 'high',
    sourceType: 'system_error',
    sourceId: 'trace-1001',
    relatedPath: '/system-log',
    publishTime: '2026-03-22 09:00:00',
    channelCode: 'wechat-alert',
    channelName: '微信告警',
    channelType: 'wechat',
    bridgeScene: 'in_app_unread_bridge',
    bridgeStatus: 0,
    unreadCount: 12,
    attemptCount: 3,
    lastAttemptTime: '2026-03-22 09:30:00',
    successTime: '',
    responseStatusCode: 500,
    responseBody: '{"message":"gateway timeout"}'
  }
}

function buildMessageDetail() {
  return {
    id: '101',
    title: '系统异常桥接',
    summary: '系统异常需要继续桥接',
    content: 'TraceId 1001 仍未解除，请立即检查通知渠道。',
    messageType: 'error',
    priority: 'high',
    targetType: 'role',
    targetRoleCodes: 'OPS_STAFF,DEVELOPER_STAFF',
    targetUserIds: '',
    relatedPath: '/system-log',
    sourceType: 'system_error',
    sourceId: 'trace-1001',
    publishTime: '2026-03-22 09:00:00',
    expireTime: '2026-03-23 09:00:00',
    status: 1,
    sortNo: 10
  }
}

function buildAttemptRecords() {
  return [
    {
      id: 'a-2',
      bridgeLogId: '201',
      messageId: '101',
      channelCode: 'wechat-alert',
      bridgeScene: 'in_app_unread_bridge',
      attemptNo: 2,
      bridgeStatus: 0,
      unreadCount: 12,
      recipientSnapshot: 'OPS_STAFF,DEVELOPER_STAFF',
      responseStatusCode: 500,
      responseBody: 'gateway timeout',
      attemptTime: '2026-03-22 09:30:00'
    },
    {
      id: 'a-1',
      bridgeLogId: '201',
      messageId: '101',
      channelCode: 'wechat-alert',
      bridgeScene: 'in_app_unread_bridge',
      attemptNo: 1,
      bridgeStatus: 0,
      unreadCount: 12,
      recipientSnapshot: 'OPS_STAFF,DEVELOPER_STAFF',
      responseStatusCode: 500,
      responseBody: 'first timeout',
      attemptTime: '2026-03-22 09:20:00'
    }
  ]
}

function seedSuccessMocks() {
  vi.mocked(pageInAppMessages).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [buildMessageDetail()]
    }
  })
  vi.mocked(getInAppMessageStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: buildMessageStats()
  })
  vi.mocked(getInAppMessageBridgeStats).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: buildBridgeStats()
  })
  vi.mocked(pageInAppMessageBridgeLogs).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [buildBridgeLog()]
    }
  })
  vi.mocked(getInAppMessage).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: buildMessageDetail()
  })
  vi.mocked(listInAppMessageBridgeAttempts).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: buildAttemptRecords()
  })
  vi.mocked(listChannels).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        id: '1',
        channelCode: 'wechat-alert',
        channelName: '微信告警',
        channelType: 'wechat',
        status: 1
      },
      {
        id: '2',
        channelCode: 'email-alert',
        channelName: '邮件告警',
        channelType: 'email',
        status: 1
      }
    ]
  })
  vi.mocked(listRoles).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
  vi.mocked(listUsers).mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
}

function mountView() {
  return mount(InAppMessageView, {
    global: {
      directives: {
        permission: () => undefined
      },
      stubs: {
        ElAlert: ElAlertStub,
        ElButton: ElButtonStub,
        ElCol: ElColStub,
        ElCollapse: ElCollapseStub,
        ElCollapseItem: ElCollapseItemStub,
        ElDatePicker: ElDatePickerStub,
        ElEmpty: ElEmptyStub,
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElInputNumber: ElInputNumberStub,
        ElOption: ElOptionStub,
        ElRadio: ElRadioStub,
        ElRadioGroup: ElRadioGroupStub,
        ElRow: ElRowStub,
        ElSelect: ElSelectStub,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElTag: ElTagStub,
        PanelCard: PanelCardStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardPagination: StandardPaginationStub,
        StandardTableToolbar: StandardTableToolbarStub,
        teleport: true,
        transition: false
      }
    }
  })
}

function findBridgeSearchButton(wrapper: ReturnType<typeof mountView>) {
  return wrapper.findAll('button').filter((button) => button.text() === '查询').at(-1)
}

describe('InAppMessageView bridge operations', () => {
  beforeEach(() => {
    vi.mocked(pageInAppMessages).mockReset()
    vi.mocked(getInAppMessageStats).mockReset()
    vi.mocked(getInAppMessageBridgeStats).mockReset()
    vi.mocked(pageInAppMessageBridgeLogs).mockReset()
    vi.mocked(getInAppMessage).mockReset()
    vi.mocked(listInAppMessageBridgeAttempts).mockReset()
    vi.mocked(listChannels).mockReset()
    vi.mocked(listRoles).mockReset()
    vi.mocked(listUsers).mockReset()
    seedSuccessMocks()
  })

  it('keeps the bridge operations section expanded by default and renders pending retry state', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('桥接效果运营')
    expect(wrapper.text()).toContain('桥接记录数')
    expect(wrapper.text()).toContain('待重试')
    expect(wrapper.text()).toContain('微信告警')
  })

  it('removes the legacy English eyebrow tier from the message governance workbench and drawers', async () => {
    const wrapper = mountView()
    await flushPromises()

    const panelCard = wrapper.findComponent(PanelCardStub)
    const detailDrawers = wrapper.findAllComponents(StandardDetailDrawerStub)
    const formDrawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(panelCard.props('eyebrow')).toBeUndefined()
    expect(detailDrawers.length).toBe(2)
    expect(detailDrawers.every((item) => item.props('eyebrow') === undefined)).toBe(true)
    expect(formDrawer.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).not.toContain('System Content')
    expect(wrapper.text()).not.toContain('Bridge Insight')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('refreshes bridge stats and page together after changing filters', async () => {
    const wrapper = mountView()
    await flushPromises()

    const bridgeStatusSelect = wrapper.find('select[data-placeholder="桥接状态"]')
    await bridgeStatusSelect.setValue('0')
    await findBridgeSearchButton(wrapper)!.trigger('click')
    await flushPromises()

    expect(vi.mocked(getInAppMessageBridgeStats).mock.calls.at(-1)?.[0]).toMatchObject({
      bridgeStatus: 0
    })
    expect(vi.mocked(pageInAppMessageBridgeLogs).mock.calls.at(-1)?.[0]).toMatchObject({
      bridgeStatus: 0,
      pageNum: 1,
      pageSize: 10
    })
  })

  it('loads message original content and attempt details when opening bridge detail', async () => {
    const wrapper = mountView()
    await flushPromises()

    const bridgeDetailButton = wrapper.findAll('button').find((button) => button.text() === '桥接详情')
    expect(bridgeDetailButton).toBeTruthy()

    await bridgeDetailButton!.trigger('click')
    await flushPromises()

    expect(getInAppMessage).toHaveBeenCalledWith('101')
    expect(listInAppMessageBridgeAttempts).toHaveBeenCalledWith('201')
    expect(wrapper.text()).toContain('消息原文')
    expect(wrapper.text()).toContain('尝试明细')
    expect(wrapper.text()).toContain('gateway timeout')
  })

  it('shows bridge failure state without affecting the governance section', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    vi.mocked(pageInAppMessageBridgeLogs).mockRejectedValueOnce(createRequestError('桥接日志加载失败', true))

    try {
      const wrapper = mountView()
      await flushPromises()

      expect(wrapper.text()).toContain('桥接日志加载失败')
      expect(wrapper.text()).toContain('站内消息管理')
      expect(wrapper.text()).toContain('投放总量')
      expect(errorSpy).not.toHaveBeenCalled()
    } finally {
      errorSpy.mockRestore()
    }
  })
})
