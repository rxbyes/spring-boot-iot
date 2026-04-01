import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RiskPointView from '@/views/RiskPointView.vue'

const {
  mockPageRiskPointList,
  mockAddRiskPoint,
  mockUpdateRiskPoint,
  mockDeleteRiskPoint,
  mockBindDevice,
  mockListOrganizationTree
} = vi.hoisted(() => ({
  mockPageRiskPointList: vi.fn(),
  mockAddRiskPoint: vi.fn(),
  mockUpdateRiskPoint: vi.fn(),
  mockDeleteRiskPoint: vi.fn(),
  mockBindDevice: vi.fn(),
  mockListOrganizationTree: vi.fn()
}))

vi.mock('@/api/riskPoint', () => ({
  pageRiskPointList: mockPageRiskPointList,
  addRiskPoint: mockAddRiskPoint,
  updateRiskPoint: mockUpdateRiskPoint,
  deleteRiskPoint: mockDeleteRiskPoint,
  bindDevice: mockBindDevice
}))

vi.mock('@/api/iot', () => ({
  listDeviceOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getDeviceMetricOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
}))

vi.mock('@/api/organization', () => ({
  listOrganizationTree: mockListOrganizationTree
}))

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
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
    <section class="risk-point-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="risk-point-workbench-panel-stub__header-actions"><slot name="header-actions" /></div>
      <div class="risk-point-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="risk-point-workbench-panel-stub__applied"><slot name="applied-filters" /></div>
      <div class="risk-point-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="risk-point-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="risk-point-workbench-panel-stub__inline"><slot name="inline-state" /></div>
      <div class="risk-point-workbench-panel-stub__body"><slot /></div>
      <div class="risk-point-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="risk-point-list-filter-header-stub">
      <div class="risk-point-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="risk-point-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="risk-point-table-toolbar-stub">
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
      class="risk-point-pagination-stub"
      :data-total="total"
      :data-layout="layout"
      :data-page-sizes="JSON.stringify(pageSizes || [])"
    />
  `
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'directItems', 'menuItems', 'maxDirectItems'],
  template: `
    <section
      class="risk-point-row-actions-stub"
      :data-variant="variant"
      :data-max-direct-items="maxDirectItems"
    >
      <span
        v-for="item in directItems || []"
        :key="item.key || item.command"
        class="risk-point-row-actions-stub__item"
      >
        {{ item.label }}
      </span>
      <span class="risk-point-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </section>
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

function createRiskPointRow() {
  return {
    id: 1,
    riskPointCode: 'RP-OPSCEN-NORTHS-CRIT-001',
    riskPointName: '一号风险对象',
    orgId: 7101,
    orgName: '平台运维中心',
    regionId: 1,
    regionName: '东区',
    responsibleUser: 1,
    responsiblePhone: '13800000000',
    riskLevel: 'critical',
    description: 'desc',
    status: 0,
    tenantId: 1,
    remark: '',
    createBy: 1,
    createTime: '2026-04-01 08:00:00',
    updateBy: 1,
    updateTime: '2026-04-01 09:00:00',
    deleted: 0
  }
}

function mountView() {
  return shallowMount(RiskPointView, {
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
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardFormDrawer: true,
        StandardTableTextColumn: true,
        StandardButton: true,
        StandardInlineState: true,
        EmptyState: EmptyStateStub,
        ElAlert: true,
        ElForm: true,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true,
        ElCheckbox: true,
        ElTag: true,
        ElRadioGroup: true,
        ElRadio: true,
        ElTreeSelect: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('RiskPointView', () => {
  beforeEach(() => {
    mockPageRiskPointList.mockReset()
    mockAddRiskPoint.mockReset()
    mockUpdateRiskPoint.mockReset()
    mockDeleteRiskPoint.mockReset()
    mockBindDevice.mockReset()
    mockListOrganizationTree.mockReset()
    mockListOrganizationTree.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
  })

  it('shows a list skeleton while the first page is still loading', async () => {
    const deferred = createDeferred<{ code: number; msg: string; data: { total: number; pageNum: number; pageSize: number; records: ReturnType<typeof createRiskPointRow>[] } }>()
    mockPageRiskPointList.mockReturnValueOnce(deferred.promise)

    const wrapper = mountView()
    await nextTick()

    expect(wrapper.find('.ops-list-loading-state').exists()).toBe(true)
  })

  it('uses the standard pagination sizes and full layout after loading data', async () => {
    mockPageRiskPointList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 24,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    const pagination = wrapper.findComponent(StandardPaginationStub)
    expect(pagination.props('pageSizes')).toEqual([10, 20, 50, 100])
    expect(pagination.props('layout')).toBe('total, sizes, prev, pager, next, jumper')
  })

  it('loads organization options on mount for risk-point ownership selection', async () => {
    mockPageRiskPointList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })

    const wrapper = mountView()
    await flushPromises()

    expect(mockListOrganizationTree).toHaveBeenCalledTimes(1)
  })

})
