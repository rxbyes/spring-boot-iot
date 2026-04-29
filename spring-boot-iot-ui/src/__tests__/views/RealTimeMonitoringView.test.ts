import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RealTimeMonitoringView from '@/views/RealTimeMonitoringView.vue'

const { mockGetRiskMonitoringList, mockGetRiskPointList, mockGetDictByCode } = vi.hoisted(() => ({
  mockGetRiskMonitoringList: vi.fn(),
  mockGetRiskPointList: vi.fn(),
  mockGetDictByCode: vi.fn()
}))

vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringList: mockGetRiskMonitoringList
}))

vi.mock('@/api/riskPoint', () => ({
  getRiskPointList: mockGetRiskPointList
}))

vi.mock('@/api/dict', () => ({
  getDictByCode: mockGetDictByCode
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="realtime-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="realtime-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="realtime-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="realtime-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="realtime-workbench-panel-stub__body"><slot /></div>
      <div class="realtime-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="realtime-list-filter-header-stub">
      <div class="realtime-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="realtime-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="realtime-table-toolbar-stub">
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  template: `
    <section
      class="realtime-pagination-stub"
      :data-total="total"
      :data-layout="layout"
      :data-page-sizes="JSON.stringify(pageSizes || [])"
    />
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  template: '<section class="el-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'width', 'type'],
  template: '<section class="el-table-column-stub"><slot name="default" :row="{}" /></section>'
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: '<section class="empty-state-stub">{{ title }}{{ description }}</section>'
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((innerResolve, innerReject) => {
    resolve = innerResolve
    reject = innerReject
  })

  return { promise, resolve, reject }
}

function createMonitoringRow() {
  return {
    bindingId: 1,
    deviceCode: 'DEVICE-001',
    deviceName: '一号监测设备',
    productName: '边坡监测终端',
    riskPointName: '一号风险点',
    metricName: '位移',
    metricIdentifier: 'displacement',
    currentValue: '12.5',
    unit: 'mm',
    monitorStatus: 'ALARM',
    latestReportTime: '2026-04-01 10:00:00',
    riskLevel: 'CRITICAL',
    alarmFlag: true,
    onlineStatus: 1
  }
}

function mountView() {
  return shallowMount(RealTimeMonitoringView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardTableTextColumn: true,
        StandardWorkbenchRowActions: true,
        StandardButton: true,
        RiskMonitoringDetailDrawer: true,
        EmptyState: EmptyStateStub,
        ElAlert: true,
        ElForm: true,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true,
        ElTag: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('RealTimeMonitoringView', () => {
  beforeEach(() => {
    mockGetRiskMonitoringList.mockReset()
    mockGetRiskPointList.mockReset()
    mockGetDictByCode.mockReset()
    mockGetRiskPointList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockGetDictByCode.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        items: [
          { itemName: '红色', itemValue: 'red', status: 1, sortNo: 1 },
          { itemName: '橙色', itemValue: 'orange', status: 1, sortNo: 2 },
          { itemName: '黄色', itemValue: 'yellow', status: 1, sortNo: 3 },
          { itemName: '蓝色', itemValue: 'blue', status: 1, sortNo: 4 }
        ]
      }
    })
  })

  it('shows a list skeleton while the first page is still loading', async () => {
    const deferred = createDeferred<{
      code: number
      msg: string
      data: {
        total: number
        pageNum: number
        pageSize: number
        records: ReturnType<typeof createMonitoringRow>[]
      }
    }>()
    mockGetRiskMonitoringList.mockReturnValueOnce(deferred.promise)

    const wrapper = mountView()
    await nextTick()

    expect(wrapper.find('.ops-list-loading-state').exists()).toBe(true)
  })

  it('uses the standard pagination sizes and full layout after loading data', async () => {
    mockGetRiskMonitoringList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 24,
        pageNum: 1,
        pageSize: 10,
        records: [createMonitoringRow()]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    const pagination = wrapper.findComponent(StandardPaginationStub)
    expect(pagination.props('pageSizes')).toEqual([10, 20, 50, 100])
    expect(pagination.props('layout')).toBe('total, sizes, prev, pager, next, jumper')
  })

  it('opens detail with a long binding id without numeric coercion', async () => {
    const longBindingId = '202604280000000201'
    mockGetRiskMonitoringList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [{ ...createMonitoringRow(), bindingId: longBindingId }]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    ;(wrapper.vm as any).openDetail(longBindingId)

    expect((wrapper.vm as any).activeBindingId).toBe(longBindingId)
    expect((wrapper.vm as any).detailVisible).toBe(true)
  })
})
