import { defineComponent, inject, nextTick, provide } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import RiskPointView from '@/views/RiskPointView.vue'

const {
  mockPageRiskPointList,
  mockAddRiskPoint,
  mockUpdateRiskPoint,
  mockDeleteRiskPoint,
  mockBindDevice,
  mockListBindingSummaries,
  mockListBindingGroups,
  mockListPendingBindings,
  mockGetPendingCandidates,
  mockPromotePendingBinding,
  mockIgnorePendingBinding,
  mockListBindableDevices,
  mockListOrganizationTree,
  mockListRegions,
  mockListRegionTree,
  mockGetRegion,
  mockListUsers,
  mockGetUser,
  mockGetDictByCode,
  mockListMissingBindings
} = vi.hoisted(() => ({
  mockPageRiskPointList: vi.fn(),
  mockAddRiskPoint: vi.fn(),
  mockUpdateRiskPoint: vi.fn(),
  mockDeleteRiskPoint: vi.fn(),
  mockBindDevice: vi.fn(),
  mockListBindingSummaries: vi.fn(),
  mockListBindingGroups: vi.fn(),
  mockListPendingBindings: vi.fn(),
  mockGetPendingCandidates: vi.fn(),
  mockPromotePendingBinding: vi.fn(),
  mockIgnorePendingBinding: vi.fn(),
  mockListBindableDevices: vi.fn(),
  mockListOrganizationTree: vi.fn(),
  mockListRegions: vi.fn(),
  mockListRegionTree: vi.fn(),
  mockGetRegion: vi.fn(),
  mockListUsers: vi.fn(),
  mockGetUser: vi.fn(),
  mockGetDictByCode: vi.fn(),
  mockListMissingBindings: vi.fn()
}))

vi.mock('@/api/riskPoint', () => ({
  pageRiskPointList: mockPageRiskPointList,
  addRiskPoint: mockAddRiskPoint,
  updateRiskPoint: mockUpdateRiskPoint,
  deleteRiskPoint: mockDeleteRiskPoint,
  bindDevice: mockBindDevice,
  listBindingSummaries: mockListBindingSummaries,
  listBindingGroups: mockListBindingGroups,
  listPendingBindings: mockListPendingBindings,
  getPendingBindingCandidates: mockGetPendingCandidates,
  promotePendingBinding: mockPromotePendingBinding,
  ignorePendingBinding: mockIgnorePendingBinding,
  listBindableDevices: mockListBindableDevices
}))

vi.mock('@/api/iot', () => ({
  getDeviceMetricOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
}))

vi.mock('@/api/organization', () => ({
  listOrganizationTree: mockListOrganizationTree
}))

vi.mock('@/api/region', () => ({
  listRegions: mockListRegions,
  listRegionTree: mockListRegionTree,
  getRegion: mockGetRegion
}))

vi.mock('@/api/user', () => ({
  listUsers: mockListUsers,
  getUser: mockGetUser
}))

vi.mock('@/api/dict', () => ({
  getDictByCode: mockGetDictByCode
}))

vi.mock('@/api/riskGovernance', () => ({
  listMissingBindings: mockListMissingBindings
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

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label', 'prop', 'width', 'minWidth'],
  setup() {
    const rows = inject<Array<Record<string, unknown>>>('el-table-rows', [])
    return { rows }
  },
  template: `
    <section class="standard-table-text-column-stub">
      <header v-if="label" class="standard-table-text-column-stub__label">{{ label }}</header>
      <template v-if="$slots.default">
        <template v-for="(row, index) in rows" :key="index">
          <slot name="default" :row="row" />
        </template>
      </template>
      <template v-else>
        <div v-for="(row, index) in rows" :key="index">{{ prop ? row[prop] : '' }}</div>
      </template>
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle'],
  template: `
    <section class="standard-form-drawer-stub" :data-model-value="modelValue">
      <header class="standard-form-drawer-stub__header">
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </header>
      <slot />
    </section>
  `
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  emits: ['command'],
  props: ['variant', 'directItems', 'menuItems', 'maxDirectItems'],
  template: `
    <section
      class="risk-point-row-actions-stub"
      :data-variant="variant"
      :data-max-direct-items="maxDirectItems"
    >
      <button
        v-for="item in directItems || []"
        :key="item.key || item.command"
        type="button"
        class="risk-point-row-actions-stub__item"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <span class="risk-point-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </section>
  `
})

const RiskPointBindingMaintenanceDrawerStub = defineComponent({
  name: 'RiskPointBindingMaintenanceDrawer',
  props: ['modelValue', 'riskPointId', 'riskPointName', 'riskPointCode', 'orgName', 'pendingBindingCount'],
  emits: ['updated'],
  template: `
    <section class="risk-point-binding-maintenance-drawer-stub" :data-model-value="modelValue">
      <h3>维护绑定</h3>
      <p>查看正式绑定摘要，并继续维护设备与测点关系。</p>
      <div v-if="riskPointName">{{ riskPointName }}</div>
      <div v-if="riskPointCode">{{ riskPointCode }}</div>
      <div v-if="orgName">{{ orgName }}</div>
      <div>待治理 {{ pendingBindingCount ?? 0 }} 条</div>
      <button type="button" data-testid="binding-maintenance-updated" @click="$emit('updated')">刷新摘要</button>
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('el-table-rows', props.data ?? [])
    return {}
  },
  template: '<section class="el-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'width', 'type'],
  setup() {
    const rows = inject<Array<Record<string, unknown>>>('el-table-rows', [])
    return { rows }
  },
  template: `
    <section class="el-table-column-stub">
      <header v-if="label" class="el-table-column-stub__label">{{ label }}</header>
      <template v-for="(row, index) in rows" :key="index">
        <slot name="default" :row="row" />
      </template>
    </section>
  `
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: '<section class="empty-state-stub">{{ title }}{{ description }}</section>'
})

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  props: ['title', 'type'],
  template: `
    <section class="el-alert-stub" :data-type="type">
      <strong>{{ title }}</strong>
      <slot />
    </section>
  `
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  template: '<form class="el-form-stub"><slot /></form>'
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  props: ['label', 'prop'],
  template: `
    <label class="el-form-item-stub" :data-prop="prop">
      <span v-if="label" class="el-form-item-stub__label">{{ label }}</span>
      <slot />
    </label>
  `
})

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: ['modelValue', 'placeholder', 'readonly'],
  template: `
    <div class="el-input-stub" :data-readonly="readonly">
      <span class="el-input-stub__value">{{ modelValue }}</span>
      <span v-if="placeholder" class="el-input-stub__placeholder">{{ placeholder }}</span>
    </div>
  `
})

const ElSelectStub = defineComponent({
  name: 'ElSelect',
  template: '<div class="el-select-stub"><slot /></div>'
})

const ElOptionStub = defineComponent({
  name: 'ElOption',
  props: ['label', 'value'],
  template: '<div class="el-option-stub" :data-value="value">{{ label }}</div>'
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
    riskPointName: '示例风险点',
    orgId: 7101,
    orgName: '平台运维中心',
    regionId: 1,
    regionName: '东区',
    responsibleUser: 1,
    responsibleUserName: '张三',
    responsiblePhone: '13800000000',
    riskPointLevel: 'level_1',
    currentRiskLevel: 'red',
    riskLevel: 'red',
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
        RiskPointBindingMaintenanceDrawer: RiskPointBindingMaintenanceDrawerStub,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        StandardButton: true,
        StandardInlineState: true,
        EmptyState: EmptyStateStub,
        ElAlert: ElAlertStub,
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElSelect: ElSelectStub,
        ElOption: ElOptionStub,
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
    mockListBindingSummaries.mockReset()
    mockListBindingGroups.mockReset()
    mockListBindableDevices.mockReset()
    mockListPendingBindings.mockReset()
    mockGetPendingCandidates.mockReset()
    mockPromotePendingBinding.mockReset()
    mockIgnorePendingBinding.mockReset()
    mockListOrganizationTree.mockReset()
    mockListRegions.mockReset()
    mockListRegionTree.mockReset()
    mockGetRegion.mockReset()
    mockListUsers.mockReset()
    mockGetUser.mockReset()
    mockGetDictByCode.mockReset()
    mockListMissingBindings.mockReset()
    mockListOrganizationTree.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListRegions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListRegionTree.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockGetRegion.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
    mockListUsers.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockGetUser.mockImplementation(async (id: number | string) => ({
      code: 200,
      msg: 'success',
      data: {
        id: Number(id),
        username: `user-${id}`,
        realName: `用户${id}`,
        phone: `1380000${String(id).slice(-4).padStart(4, '0')}`,
        status: 1
      }
    }))
    mockGetDictByCode.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 1,
        tenantId: 1,
        dictName: '风险点等级',
        dictCode: 'risk_point_level',
        dictType: 'text',
        status: 1,
        sortNo: 1,
        remark: '',
        createBy: 1,
        createTime: '2026-04-01 08:00:00',
        updateBy: 1,
        updateTime: '2026-04-01 08:00:00',
        deleted: 0,
        items: [
          { id: 11, tenantId: 1, dictId: 1, itemName: '一级风险点', itemValue: 'level_1', itemType: 'string', status: 1, sortNo: 1, remark: '', createBy: 1, createTime: '', updateBy: 1, updateTime: '', deleted: 0 },
          { id: 12, tenantId: 1, dictId: 1, itemName: '二级风险点', itemValue: 'level_2', itemType: 'string', status: 1, sortNo: 2, remark: '', createBy: 1, createTime: '', updateBy: 1, updateTime: '', deleted: 0 },
          { id: 13, tenantId: 1, dictId: 1, itemName: '三级风险点', itemValue: 'level_3', itemType: 'string', status: 1, sortNo: 3, remark: '', createBy: 1, createTime: '', updateBy: 1, updateTime: '', deleted: 0 }
        ]
      }
    })
    mockListMissingBindings.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 3,
        records: [
          {
            issueType: 'MISSING_BINDING',
            issueLabel: '待纳入风险对象',
            deviceId: 81,
            deviceCode: 'DEVICE-081',
            deviceName: '81 号设备',
            lastReportTime: '2026-04-01 09:10:00'
          }
        ]
      }
    })
    mockListBindableDevices.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListBindingSummaries.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockListBindingGroups.mockResolvedValue({
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

  it('renders archive-grade and runtime-risk columns from the new risk point semantics', async () => {
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

    expect(wrapper.text()).toContain('所属组织')
    expect(wrapper.text()).toContain('所属区域')
    expect(wrapper.text()).toContain('负责人')
    expect(wrapper.text()).toContain('风险点等级')
    expect(wrapper.text()).toContain('当前风险态势')
    expect(wrapper.text()).toContain('一级风险点')
    expect(mockGetDictByCode).toHaveBeenCalledWith('risk_point_level')
  })

  it('loads organization and risk-point-level options on mount while deferring region roots until the form opens', async () => {
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
    expect(mockListRegionTree).not.toHaveBeenCalled()
    expect(mockListRegions).not.toHaveBeenCalled()
    expect(mockListUsers).not.toHaveBeenCalled()
    expect(mockGetDictByCode).toHaveBeenCalledWith('risk_point_level')

    ;(wrapper.vm as unknown as { handleAdd: () => void }).handleAdd()
    await flushPromises()

    expect(mockListRegions).toHaveBeenCalledTimes(1)
    expect(mockListRegions).toHaveBeenCalledWith()
  })

  it('defaults the responsible user and phone from the selected organization leader in create mode', async () => {
    mockListOrganizationTree.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 7101,
          tenantId: 1,
          parentId: 0,
          orgName: '平台运维中心',
          orgCode: 'OPS-CENTER',
          orgType: 'dept',
          leaderUserId: 101,
          leaderName: '机构管理员',
          phone: '021-66889900',
          email: 'ops@example.com',
          status: 1,
          sortNo: 1,
          remark: '',
          createBy: 1,
          createTime: '2026-04-01 08:00:00',
          updateBy: 1,
          updateTime: '2026-04-01 08:00:00',
          deleted: 0,
          children: []
        }
      ]
    })
    mockGetUser.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 101,
        username: 'ops_admin',
        realName: '机构管理员',
        phone: '13812345678',
        status: 1
      }
    })
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

    const vm = wrapper.vm as unknown as {
      handleAdd: () => void
      form: { orgId: number | ''; responsibleUser: number | ''; responsiblePhone: string }
    }
    vm.handleAdd()
    await nextTick()
    vm.form.orgId = 7101
    await flushPromises()

    expect(mockGetUser).toHaveBeenCalledWith(101)
    expect(vm.form.responsibleUser).toBe(101)
    expect(vm.form.responsiblePhone).toBe('13812345678')

    vm.form.responsiblePhone = '13900001111'
    await nextTick()
    expect(vm.form.responsiblePhone).toBe('13900001111')
  })

  it('resolves responsible text from row data without relying on a preloaded global user list', async () => {
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

    const vm = wrapper.vm as unknown as {
      getResponsibleUserText: (row: ReturnType<typeof createRiskPointRow>) => string
    }
    expect(vm.getResponsibleUserText(createRiskPointRow())).toBe('张三')
  })

  it('shows missing-binding backlog in the notice area after loading the page', async () => {
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

    expect(wrapper.text()).toContain('待纳入风险对象')
    expect(wrapper.text()).toContain('DEVICE-081')
    expect(mockListMissingBindings).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 3
    })
  })

  it('hides the code field in create mode and shows audit fields in edit mode', async () => {
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

    ;(wrapper.vm as unknown as { handleAdd: () => void }).handleAdd()
    await nextTick()

    const drawerBeforeEdit = wrapper.findAllComponents(StandardFormDrawerStub)[0]
    expect(drawerBeforeEdit.text()).not.toContain('风险点编号')

    ;(wrapper.vm as unknown as { handleEdit: (row: ReturnType<typeof createRiskPointRow>) => void }).handleEdit(createRiskPointRow())
    await nextTick()

    const drawerAfterEdit = wrapper.findAllComponents(StandardFormDrawerStub)[0]
    expect(drawerAfterEdit.text()).toContain('风险点编号')
    expect(drawerAfterEdit.text()).toContain('创建人编号')
    expect(drawerAfterEdit.text()).toContain('更新人编号')
  })

  it('loads pending bindings for the selected risk point and renders candidate evidence', async () => {
    mockPageRiskPointList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })
    mockListPendingBindings.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [{ id: 77, riskPointId: 1, deviceCode: 'DEVICE-2001', deviceName: '一号设备', resolutionStatus: 'PENDING_METRIC_GOVERNANCE' }]
      }
    })
    mockGetPendingCandidates.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        pendingId: 77,
        riskPointId: 1,
        deviceCode: 'DEVICE-2001',
        deviceName: '一号设备',
        resolutionStatus: 'PENDING_METRIC_GOVERNANCE',
        candidates: [
          {
            metricIdentifier: 'dispsX',
            metricName: 'X向位移',
            recommendationLevel: 'HIGH',
            evidenceSources: ['PRODUCT_MODEL', 'LATEST_PROPERTY', 'MESSAGE_LOG']
          }
        ],
        promotionHistory: []
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await (wrapper.vm as any).handleOpenPendingPromotion(createRiskPointRow())
    await flushPromises()

    expect(mockListPendingBindings).toHaveBeenCalledWith({ riskPointId: 1, pageNum: 1, pageSize: 10 })
    expect(wrapper.text()).toContain('待治理转正')
    expect(wrapper.text()).toContain('X向位移')
    expect(wrapper.text()).toContain('PRODUCT_MODEL')
  })

  it('loads binding summaries for the current page and renders the summary column', async () => {
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
    mockListBindingSummaries.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          riskPointId: 1,
          boundDeviceCount: 2,
          boundMetricCount: 5,
          pendingBindingCount: 1
        }
      ]
    })

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      getRiskPointRowActions: () => Array<{ label: string }>
    }

    expect(mockListBindingSummaries).toHaveBeenCalledWith([1])
    expect(wrapper.text()).toContain('绑定概览')
    expect(wrapper.text()).toContain('2 台设备')
    expect(wrapper.text()).toContain('5 个测点')
    expect(wrapper.text()).toContain('待治理 1 条')
    expect(vm.getRiskPointRowActions().map((item) => item.label)).toContain('维护绑定')
  })

  it('shows an unbound summary state instead of three zero lines when no formal bindings exist', async () => {
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
    mockListBindingSummaries.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          riskPointId: 1,
          boundDeviceCount: 0,
          boundMetricCount: 0,
          pendingBindingCount: 2
        }
      ]
    })

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('未绑定')
    expect(wrapper.text()).toContain('待治理 2 条')
    expect(wrapper.text()).not.toContain('0 台设备')
    expect(wrapper.text()).not.toContain('0 个测点')
  })

  it('skips completed pending rows and auto-loads the first promotable row', async () => {
    mockPageRiskPointList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })
    mockListPendingBindings.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          { id: 77, riskPointId: 1, deviceCode: 'DEVICE-DONE', deviceName: '已完成设备', resolutionStatus: 'PROMOTED' },
          { id: 78, riskPointId: 1, deviceCode: 'DEVICE-PENDING', deviceName: '待治理设备', resolutionStatus: 'PENDING_METRIC_GOVERNANCE' }
        ]
      }
    })
    mockGetPendingCandidates.mockImplementationOnce(async (pendingId: number) => {
      if (pendingId === 77) {
        throw new Error('系统繁忙，请稍后重试！')
      }
      return {
        code: 200,
        msg: 'success',
        data: {
          pendingId: 78,
          riskPointId: 1,
          deviceCode: 'DEVICE-PENDING',
          deviceName: '待治理设备',
          resolutionStatus: 'PENDING_METRIC_GOVERNANCE',
          candidates: [
            {
              metricIdentifier: 'dispsY',
              metricName: 'Y向位移',
              recommendationLevel: 'HIGH',
              evidenceSources: ['LATEST_PROPERTY']
            }
          ],
          promotionHistory: []
        }
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await (wrapper.vm as any).handleOpenPendingPromotion(createRiskPointRow())
    await flushPromises()

    expect(mockGetPendingCandidates).toHaveBeenCalledTimes(1)
    expect(mockGetPendingCandidates).toHaveBeenCalledWith(78)
    expect(wrapper.text()).toContain('DEVICE-PENDING')
    expect(wrapper.text()).toContain('Y向位移')
  })

  it('opens the binding maintenance drawer when the row action emits maintain-binding', async () => {
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
    mockListBindingSummaries.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          riskPointId: 1,
          boundDeviceCount: 2,
          boundMetricCount: 5,
          pendingBindingCount: 1
        }
      ]
    })

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      bindingMaintenanceVisible: boolean
      bindingMaintenanceRiskPoint: ReturnType<typeof createRiskPointRow> | null
    }

    const actionButton = wrapper
      .findAll('.risk-point-row-actions-stub__item')
      .find((node) => node.text() === '维护绑定')

    expect(actionButton).toBeTruthy()
    await actionButton!.trigger('click')
    await nextTick()

    const drawer = wrapper.findComponent(RiskPointBindingMaintenanceDrawerStub)

    expect(wrapper.text()).toContain('维护绑定')
    expect(vm.bindingMaintenanceVisible).toBe(true)
    expect(vm.bindingMaintenanceRiskPoint?.riskPointName).toBe('示例风险点')
    expect(drawer.props('riskPointId')).toBe(1)
    expect(drawer.props('riskPointName')).toBe('示例风险点')
    expect(drawer.props('riskPointCode')).toBe('RP-OPSCEN-NORTHS-CRIT-001')
    expect(drawer.props('orgName')).toBe('平台运维中心')
    expect(drawer.props('pendingBindingCount')).toBe(1)
  })

  it('refreshes the list and binding summaries after the maintenance drawer emits updated', async () => {
    mockPageRiskPointList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })
    mockListBindingSummaries.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          riskPointId: 1,
          boundDeviceCount: 2,
          boundMetricCount: 5,
          pendingBindingCount: 1
        }
      ]
    })

    const wrapper = mountView()
    await flushPromises()
    await flushPromises()

    const actionButton = wrapper
      .findAll('.risk-point-row-actions-stub__item')
      .find((node) => node.text() === '维护绑定')

    await actionButton!.trigger('click')
    await nextTick()
    await wrapper.get('[data-testid="binding-maintenance-updated"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(mockPageRiskPointList).toHaveBeenCalledTimes(2)
    expect(mockListBindingSummaries).toHaveBeenCalledTimes(2)
  })

  it('submits the selected pending metrics through the promote API', async () => {
    mockPageRiskPointList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRiskPointRow()]
      }
    })
    mockListPendingBindings.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockPromotePendingBinding.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        pendingId: 77,
        pendingStatus: 'PROMOTED',
        items: [{ metricIdentifier: 'dispsX', promotionStatus: 'SUCCESS', bindingId: 9001 }]
      }
    })

    const wrapper = mountView()
    await flushPromises()
    ;(wrapper.vm as any).pendingPromotionForm.pendingId = 77
    ;(wrapper.vm as any).pendingPromotionForm.riskPointId = 1
    ;(wrapper.vm as any).pendingPromotionForm.selectedMetrics = [{ metricIdentifier: 'dispsX', metricName: 'X向位移' }]
    ;(wrapper.vm as any).pendingPromotionForm.completePending = true

    await (wrapper.vm as any).handlePendingPromotionSubmit()

    expect(mockPromotePendingBinding).toHaveBeenCalledWith(77, {
      metrics: [{ metricIdentifier: 'dispsX', metricName: 'X向位移' }],
      completePending: true,
      promotionNote: ''
    })
  })

  it('loads bindable devices from the dedicated risk-point endpoint before opening the bind drawer', async () => {
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
    mockListBindableDevices.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 2001,
          productId: 1001,
          deviceCode: 'DEV-2001',
          deviceName: '北侧监测终端',
          orgId: 7101,
          orgName: '平台运维中心'
        }
      ]
    })

    const wrapper = mountView()
    await flushPromises()

    const vm = wrapper.vm as unknown as {
      handleBindDevice: (row: ReturnType<typeof createRiskPointRow>) => Promise<void>
      deviceList: Array<{ id: number; deviceName: string; orgName?: string }>
      bindDeviceVisible: boolean
    }
    await vm.handleBindDevice(createRiskPointRow())
    await flushPromises()

    expect(mockListBindableDevices).toHaveBeenCalledWith(1)
    expect(vm.deviceList).toEqual([
      expect.objectContaining({
        id: 2001,
        deviceName: '北侧监测终端',
        orgName: '平台运维中心'
      })
    ])
    expect(vm.bindDeviceVisible).toBe(true)
  })

})
