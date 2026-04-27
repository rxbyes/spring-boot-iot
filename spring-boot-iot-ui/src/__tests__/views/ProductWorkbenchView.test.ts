import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { defineComponent, h, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { createRequestError } from '@/api/request'
import { createEmptyProductObjectInsightMetric } from '@/utils/productObjectInsightConfig'
import ProductWorkbenchView from '@/views/ProductWorkbenchView.vue'

const {
  mockRoute,
  mockRouter,
  mockRecordActivity,
  mockPageProducts,
  mockPageProductContractReleaseBatches,
  mockListProductModels,
  mockGetProductById,
  mockAddProduct,
  mockUpdateProduct,
  mockDeleteProduct,
  mockGetRiskGovernanceCoverageOverview
} = vi.hoisted(() => ({
  mockRoute: {
    path: '/products',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockRecordActivity: vi.fn(),
  mockPageProducts: vi.fn(),
  mockPageProductContractReleaseBatches: vi.fn(),
  mockListProductModels: vi.fn(),
  mockGetProductById: vi.fn(),
  mockAddProduct: vi.fn(),
  mockUpdateProduct: vi.fn(),
  mockDeleteProduct: vi.fn(),
  mockGetRiskGovernanceCoverageOverview: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/product', () => ({
  productApi: {
    pageProducts: mockPageProducts,
    pageProductContractReleaseBatches: mockPageProductContractReleaseBatches,
    listProductModels: mockListProductModels,
    getProductById: mockGetProductById,
    addProduct: mockAddProduct,
    updateProduct: mockUpdateProduct,
    deleteProduct: mockDeleteProduct
  }
}))

vi.mock('@/api/riskGovernance', () => ({
  getRiskGovernanceCoverageOverview: mockGetRiskGovernanceCoverageOverview
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
  }
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    hasPermission: () => true
  })
}))

vi.mock('@/stores/activity', () => ({
  recordActivity: mockRecordActivity
}))

vi.mock('@/utils/confirm', () => ({
  confirmAction: vi.fn(),
  confirmDelete: vi.fn(),
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

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="product-workbench-panel-stub standard-workbench-panel--workbench-foundation">
      <p class="product-workbench-panel-stub__eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="product-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="product-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="product-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="product-workbench-panel-stub__inline"><slot name="inline-state" /></div>
      <div class="product-workbench-panel-stub__body"><slot /></div>
      <div class="product-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub standard-page-shell--workbench-foundation">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="product-list-filter-header-stub standard-list-filter-header--workbench-foundation">
      <div class="product-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="product-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="product-table-toolbar-stub">
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'gap', 'directItems', 'menuItems'],
  emits: ['command'],
  template: `
    <div class="product-workbench-row-actions-stub standard-workbench-row-actions--quiet" :data-variant="variant">
      <button
        v-for="item in directItems || []"
        :key="item.key || item.command"
        type="button"
        class="product-workbench-row-actions-stub__direct"
        :data-testid="item.dataTestid"
        @click="$emit('command', item.command)"
      >
        {{ item.label }}
      </button>
      <span class="product-workbench-row-actions-stub__menu-count">{{ (menuItems || []).length }}</span>
    </div>
  `
})

const StandardRowActionsStub = defineComponent({
  name: 'StandardRowActions',
  template: '<div class="product-row-actions-stub"><slot /></div>'
})

const StandardActionLinkStub = defineComponent({
  name: 'StandardActionLink',
  emits: ['click'],
  template: '<button class="product-action-link-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label'],
  template: '<button class="product-action-menu-stub" type="button">{{ label || \'更多\' }}</button>'
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['size', 'eyebrow', 'title', 'subtitle'],
  template: `
    <section class="product-detail-drawer-stub" :data-size="size">
      <p v-if="eyebrow" class="product-detail-drawer-stub__eyebrow">{{ eyebrow }}</p>
      <h2 class="product-detail-drawer-stub__title">{{ title }}</h2>
      <p class="product-detail-drawer-stub__subtitle">{{ subtitle }}</p>
      <div class="product-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="product-detail-drawer-stub__body"><slot /></div>
      <div class="product-detail-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const ProductDetailWorkbenchStub = defineComponent({
  name: 'ProductDetailWorkbench',
  props: ['product'],
  template: '<section class="product-detail-workbench-stub">{{ product?.productName }}</section>'
})

const ProductModelDesignerWorkspaceStub = defineComponent({
  name: 'ProductModelDesignerWorkspace',
  props: ['product'],
  template: '<section class="product-model-designer-workspace-stub">{{ product?.productKey }}</section>'
})

const ProductDeviceListWorkspaceStub = defineComponent({
  name: 'ProductDeviceListWorkspace',
  props: ['devices', 'loading', 'errorMessage', 'empty', 'devicesLoading'],
  template: `
    <section class="product-device-list-workspace-stub">
      <span class="product-device-list-workspace-stub__count">{{ devices?.length ?? 0 }}</span>
      <span class="product-device-list-workspace-stub__loading">{{ loading ? 'loading' : 'ready' }}</span>
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title'],
  template: `
    <section v-if="modelValue" class="product-form-drawer-stub">
      <h2 data-testid="product-form-drawer-title">{{ title }}</h2>
      <div class="product-form-drawer-stub__body"><slot /></div>
      <div class="product-form-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const ProductObjectInsightConfigEditorStub = defineComponent({
  name: 'ProductObjectInsightConfigEditor',
  props: ['modelValue', 'availableModels'],
  emits: ['update:modelValue'],
  template: `
    <section class="product-object-insight-config-editor-stub">
      <span class="product-object-insight-config-editor-stub__available-models">
        available-models:{{ availableModels?.length ?? 0 }}
      </span>
    </section>
  `
})

const DeviceListDrawerStub = defineComponent({
  name: 'DeviceListDrawer',
  props: ['eyebrow', 'title'],
  template: '<section class="device-list-drawer-stub">{{ title }}</section>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const StandardInlineStateStub = defineComponent({
  name: 'StandardInlineState',
  props: ['message', 'tone'],
  template: '<div class="standard-inline-state-stub">{{ message }}</div>'
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description'],
  template: `
    <section class="empty-state-stub">
      <strong class="empty-state-stub__title">{{ title }}</strong>
      <p class="empty-state-stub__description">{{ description }}</p>
    </section>
  `
})

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  props: ['title', 'type'],
  template: `
    <section class="el-alert-stub" :data-type="type">
      <strong v-if="title" class="el-alert-stub__title">{{ title }}</strong>
      <div class="el-alert-stub__content"><slot /></div>
    </section>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['prop', 'label', 'minWidth', 'width'],
  template: `
    <section class="standard-table-text-column-stub" :data-prop="prop">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  emits: ['selection-change'],
  setup(_props, { expose, slots }) {
    expose({
      clearSelection: () => undefined,
      toggleRowSelection: () => undefined
    })
    return () => h('section', { class: 'el-table-stub' }, slots.default ? slots.default() : [])
  }
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['prop', 'label', 'width', 'fixed', 'type', 'align', 'className', 'showOverflowTooltip'],
  template: `
    <section class="el-table-column-stub" :data-prop="prop || type || 'column'">
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  setup(_, { expose, slots }) {
    expose({
      validate: () => Promise.resolve(true),
      clearValidate: () => undefined
    })
    return () => h('form', { class: 'el-form-stub' }, slots.default ? slots.default() : [])
  }
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function installSessionStorageMock(value?: Record<string, string>) {
  const store = new Map<string, string>(Object.entries(value || {}))
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem: vi.fn((key: string) => store.get(key) ?? null),
      setItem: vi.fn((key: string, value: string) => {
        store.set(key, value)
      }),
      removeItem: vi.fn((key: string) => {
        store.delete(key)
      })
    }
  })
}

function mountView() {
  return shallowMount(ProductWorkbenchView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      renderStubDefaultSlot: true,
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        StandardTableToolbar: StandardTableToolbarStub,
        ProductDetailWorkbench: ProductDetailWorkbenchStub,
        ProductModelDesignerWorkspace: ProductModelDesignerWorkspaceStub,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductObjectInsightConfigEditor: ProductObjectInsightConfigEditorStub,
        StandardDrawerFooter: true,
        StandardAppliedFiltersBar: true,
        StandardInlineState: StandardInlineStateStub,
        StandardPagination: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        CsvColumnSettingDialog: true,
        DeviceListDrawer: DeviceListDrawerStub,
        EmptyState: EmptyStateStub,
        ElAlert: ElAlertStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub
      }
    }
  })
}

describe('ProductWorkbenchView', () => {
  beforeEach(() => {
    mockRoute.path = '/products'
    mockRoute.query = {}
    mockRouter.replace.mockReset()
    mockRouter.push.mockReset()
    mockRouter.replace.mockResolvedValue(undefined)
    mockRouter.push.mockResolvedValue(undefined)
    mockRecordActivity.mockReset()
    mockPageProducts.mockReset()
    mockPageProductContractReleaseBatches.mockReset()
    mockListProductModels.mockReset()
    mockGetProductById.mockReset()
    mockAddProduct.mockReset()
    mockUpdateProduct.mockReset()
    mockDeleteProduct.mockReset()
    mockGetRiskGovernanceCoverageOverview.mockReset()
    mockPageProducts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 1,
        records: []
      }
    })
    mockListProductModels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 7001,
          productId: 1001,
          modelType: 'property',
          identifier: 'value',
          modelName: '裂缝值',
          dataType: 'double',
          sortNo: 10
        }
      ]
    })
    mockGetProductById.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockAddProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockUpdateProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockDeleteProduct.mockResolvedValue({ code: 200, msg: 'success', data: null })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 5,
        publishedRiskMetricCount: 4,
        boundRiskMetricCount: 2,
        ruleCoveredRiskMetricCount: 1,
        contractMetricCoverageRate: 80,
        bindingCoverageRate: 50,
        ruleCoverageRate: 50
      }
    })
    installSessionStorageMock()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(ElMessage.success).mockReset()
    vi.mocked(ElMessage.warning).mockReset()
  })

  it('renders the product page inside the shared governance shell without the legacy eyebrow tier', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.find('.standard-page-shell--workbench-foundation').exists()).toBe(true)
    expect(wrapper.find('.standard-workbench-panel--workbench-foundation').exists()).toBe(true)
    expect(wrapper.find('.standard-list-filter-header--workbench-foundation').exists()).toBe(true)
    expect(wrapper.text()).toContain('产品定义中心')
    expect(wrapper.text()).toContain('新增产品')
    expect(wrapper.text()).toContain('统一维护产品定义，并作为进入产品工作台的统一入口承接契约、映射与版本治理。')
    expect(wrapper.text()).toContain('当前页同时承接产品定义、契约治理、版本治理与风险目录入口。')
    expect(wrapper.text()).toContain('当前还没有产品定义，先新增产品，再从这里进入产品工作台继续契约、映射和版本治理。')
    expect(wrapper.text()).not.toContain('PRODUCT CENTER')
  })

  it('shows governance task notices for the focused product when contract and coverage are incomplete', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'demo-monitoring-product',
            productName: '演示 监测型 产品',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 1,
        records: []
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('待发布合同')
    expect(wrapper.text()).toContain('待发布风险指标目录')
    expect(wrapper.text()).toContain('待绑定风险点')
    expect(wrapper.text()).toContain('待补阈值策略')
  })

  it('does not show a metric publish notice when all publishable fields are already in the catalog', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'nf-monitor-laser-rangefinder-v1',
            productName: '南方测绘 监测型 激光测距仪',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 1,
        records: [
          {
            id: 2604101739466672301,
            productId: 1001,
            scenarioCode: 'phase1-crack',
            releasedFieldCount: 2,
            createTime: '2026-04-10T12:00:00'
          }
        ]
      }
    })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 2,
        publishableContractPropertyCount: 1,
        publishedRiskMetricCount: 1,
        boundRiskMetricCount: 0,
        ruleCoveredRiskMetricCount: 0,
        contractMetricCoverageRate: 50,
        bindingCoverageRate: 0,
        ruleCoverageRate: 0
      } as any
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).not.toContain('待发布风险指标目录')
    expect(wrapper.text()).toContain('待绑定风险点')
    expect(wrapper.text()).not.toContain('待补阈值策略')
  })

  it('treats downstream metric governance as not applicable when the product has no publishable catalog fields', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'nf-monitor-empty-v1',
            productName: '南方测绘 监测型 空白产品',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 1,
        records: [
          {
            id: 5001
          }
        ]
      }
    })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 9,
        publishableContractPropertyCount: 0,
        publishedRiskMetricCount: 0,
        boundRiskMetricCount: 0,
        ruleCoveredRiskMetricCount: 0,
        contractMetricCoverageRate: 0,
        bindingCoverageRate: 0,
        ruleCoverageRate: 0
      } as any
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('当前暂无可入目录字段，目录发布、风险点绑定与策略覆盖暂不适用。')
    expect(wrapper.text()).not.toContain('待发布风险指标目录')
    expect(wrapper.text()).not.toContain('待绑定风险点')
    expect(wrapper.text()).not.toContain('待补阈值策略')
  })

  it('does not force a pending contract release when zero-publishable products already have formal fields but no batch', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'zhd-warning-siren-v1',
            productName: '中海达 预警型 声光报警器',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 1,
        records: []
      }
    })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 9,
        publishableContractPropertyCount: 0,
        publishedRiskMetricCount: 0,
        boundRiskMetricCount: 0,
        ruleCoveredRiskMetricCount: 0,
        contractMetricCoverageRate: 0,
        bindingCoverageRate: 0,
        ruleCoverageRate: 0
      } as any
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('当前已存在正式字段，但尚未查到正式发布批次')
    expect(wrapper.text()).toContain('当前已生效字段已是正式真相')
    expect(wrapper.text()).toContain('当前产品为预警型，不进入风险指标目录与阈值策略治理；支持设备级风险点绑定。')
    expect(wrapper.text()).not.toContain('待发布合同')
    expect(wrapper.text()).not.toContain('待发布风险指标目录')
    expect(wrapper.text()).not.toContain('待绑定风险点')
    expect(wrapper.text()).not.toContain('待补阈值策略')
  })

  it('shows device-only risk binding guidance for collecting products instead of metric-governance pending counts', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'nf-collect-telemetry-terminal-v1',
            productName: '南方测绘 采集型 遥测终端',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 1,
        records: [
          {
            id: 5001
          }
        ]
      }
    })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 17,
        publishableContractPropertyCount: 0,
        publishedRiskMetricCount: 0,
        boundRiskMetricCount: 0,
        ruleCoveredRiskMetricCount: 0,
        contractMetricCoverageRate: 0,
        bindingCoverageRate: 0,
        ruleCoverageRate: 0
      } as any
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('当前产品为采集型，不进入风险指标目录与阈值策略治理；支持设备级风险点绑定。')
    expect(wrapper.text()).toContain('去风险点绑定')
    expect(wrapper.text()).not.toContain('待发布风险指标目录')
    expect(wrapper.text()).not.toContain('待绑定风险点')
    expect(wrapper.text()).not.toContain('待补阈值策略')
  })

  it('surfaces capability confirmation instead of not-applicable guidance for unknown products', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'generic-device',
            productName: '通用设备',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 1,
        records: [
          {
            id: 5001
          }
        ]
      }
    })
    mockGetRiskGovernanceCoverageOverview.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        productId: 1001,
        contractPropertyCount: 3,
        publishableContractPropertyCount: 0,
        publishedRiskMetricCount: 0,
        boundRiskMetricCount: 0,
        ruleCoveredRiskMetricCount: 0,
        contractMetricCoverageRate: 0,
        bindingCoverageRate: 0,
        ruleCoverageRate: 0
      } as any
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('产品能力待确认')
    expect(wrapper.text()).toContain('去完善产品能力')
    expect(wrapper.text()).not.toContain('目录发布、风险点绑定与策略覆盖暂不适用')
  })

  it('routes supported governance todo items from /products into the correct domain workbench', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'demo-monitoring-product',
            productName: '演示 监测型 产品',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 1,
        records: []
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect((wrapper.vm as any).governanceTaskItems).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          key: 'pending-contract-release',
          path: '/products/1001/contracts'
        }),
        expect.objectContaining({
          key: 'pending-metric-publish',
          path: '/products/1001/contracts'
        }),
        expect.objectContaining({
          key: 'pending-risk-binding',
          path: '/governance-task?productId=1001&workStatus=OPEN&workItemCode=PENDING_RISK_BINDING'
        })
      ])
    )
  })

  it('records governance todo navigation before routing from the product notice panel', async () => {
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1001,
            productKey: 'demo-monitoring-product',
            productName: '演示 监测型 产品',
            protocolCode: 'mqtt-json',
            nodeType: 1,
            dataFormat: 'JSON',
            status: 1
          }
        ]
      }
    })
    mockPageProductContractReleaseBatches.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 1,
        records: []
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    const task = ((wrapper.vm as any).governanceTaskItems as Array<{ key: string; path: string }>).find(
      (item) => item.key === 'pending-contract-release'
    )

    expect(task).toBeTruthy()

    ;(wrapper.vm as any).openGovernanceTask(task!.path)
    await flushPromises()

    expect(mockRecordActivity).toHaveBeenCalledWith(
      expect.objectContaining({
        title: `产品治理待办跳转 · ${task!.path}`,
        detail: expect.stringContaining('demo-monitoring-product'),
        tag: 'product-governance-task'
      })
    )
    expect(mockRouter.push).toHaveBeenCalledWith(task!.path)
  })

  it('keeps the product toolbar focused by collapsing low-frequency actions into a more-actions menu', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('批量操作')
    expect(wrapper.text()).toContain('刷新列表')
    expect(wrapper.text()).toContain('更多操作')
    expect(wrapper.text()).not.toContain('表格视图')
    expect(wrapper.text()).not.toContain('卡片视图')
    expect(wrapper.text()).not.toContain('导出列设置')
    expect(wrapper.text()).not.toContain('导出选中')
    expect(wrapper.text()).not.toContain('导出当前结果')
    expect(wrapper.text()).not.toContain('清空选中')
  })

  it('keeps /products as a single main list with desktop table and responsive mobile cards', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 1001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        status: 1,
        deviceCount: 0,
        onlineDeviceCount: 0,
        createTime: '2026-03-24T09:00:00',
        updateTime: '2026-03-24T09:00:00'
      }
    ]
    await nextTick()

    expect(wrapper.find('.product-mobile-list').exists()).toBe(true)
    expect(wrapper.find('.product-desktop-table').exists()).toBe(true)

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)

    expect(rowActions.some((component) => component.props('variant') === 'card')).toBe(true)
    expect(rowActions.some((component) => component.props('variant') === 'table')).toBe(true)
  })

  it('shows the shared system busy copy when product page loading returns 500', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageProducts.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', false, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).toHaveBeenCalledWith('系统繁忙，请稍后重试！')
    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalledWith('获取产品分页失败')

    errorSpy.mockRestore()
  })

  it('does not show a second toast when the product page request error is already handled', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockPageProducts.mockRejectedValueOnce(createRequestError('系统繁忙，请稍后重试！', true, 500))

    mountView()
    await flushPromises()
    await nextTick()

    expect(vi.mocked(ElMessage.error)).not.toHaveBeenCalled()

    errorSpy.mockRestore()
  })

  it('routes the single direct product entry to the overview detail page', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 1001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        status: 1,
        deviceCount: 0,
        onlineDeviceCount: 0,
        createTime: '2026-03-24T09:00:00',
        updateTime: '2026-03-24T09:00:00'
      }
    ]
    await nextTick()

    const workbenchEntry = wrapper.find('[data-testid="open-product-business-workbench"]')
    expect(workbenchEntry.exists()).toBe(true)

    await workbenchEntry.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/overview')
    expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(false)
  })

  it('shows direct edit on product rows and keeps edit in the in-place drawer flow', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 1001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        status: 1,
        deviceCount: 0,
        onlineDeviceCount: 0,
        createTime: '2026-03-24T09:00:00',
        updateTime: '2026-03-24T09:00:00'
      }
    ]
    await nextTick()

    const directButtons = wrapper.findAll('.product-workbench-row-actions-stub__direct')
    expect(directButtons.some((node) => node.text() === '编辑')).toBe(true)

    await directButtons.find((node) => node.text() === '编辑')!.trigger('click')
    await flushPromises()
    await nextTick()

    expect(mockRouter.push).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="product-form-drawer-title"]').text()).toBe('编辑产品')
    expect(wrapper.find('.product-form-drawer-stub').exists()).toBe(true)
    expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(false)
  })

  it('redirects legacy governance-task workbench context to the routed contracts page', async () => {
    mockRoute.query = {
      openProductId: '1001',
      workbenchView: 'models',
      governanceSource: 'task',
      workItemCode: 'PENDING_CONTRACT_RELEASE'
    }
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockGetProductById.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 1001,
        productKey: 'phase2-gnss',
        productName: 'GNSS产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        status: 1
      }
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(mockGetProductById).toHaveBeenCalledWith('1001', expect.any(Object))
    expect(mockRouter.replace).toHaveBeenCalledWith('/products/1001/contracts')
    expect(wrapper.find('.product-form-drawer-stub').exists()).toBe(false)
  })

  it('redirects legacy governance workbench query context even when extra governance params are present', async () => {
    mockRoute.query = {
      openProductId: '1001',
      workbenchView: 'models',
      governanceSource: 'task',
      workItemCode: 'PENDING_CONTRACT_RELEASE',
      governanceBoundary: 'collector-child',
      subjectOwnership: 'child',
      governanceFocus: 'laser-rangefinder',
      pageNum: '2'
    }
    mockPageProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 2,
        pageSize: 10,
        records: []
      }
    })
    mockGetProductById.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 1001,
        productKey: 'phase2-gnss',
        productName: 'GNSS产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        status: 1
      }
    })

    mountView()
    await flushPromises()
    await nextTick()

    expect(mockRouter.replace).toHaveBeenCalledWith('/products/1001/contracts')
    expect(mockRouter.replace).not.toHaveBeenCalledWith({
      path: '/products',
      query: {
        pageNum: '2'
      }
    })
  })

  it('reuses the shared workbench row-actions component for both table and mobile product rows', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).tableData = [
      {
        id: 1001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        dataFormat: 'JSON',
        manufacturer: 'GHLZM',
        status: 1,
        deviceCount: 0,
        onlineDeviceCount: 0,
        createTime: '2026-03-24T09:00:00',
        updateTime: '2026-03-24T09:00:00'
      }
    ]
    await nextTick()

    const rowActions = wrapper.findAllComponents(StandardWorkbenchRowActionsStub)
    const cardRowActions = rowActions.find((component) => component.props('variant') === 'card')
    const tableRowActions = rowActions.find((component) => component.props('variant') === 'table')

    expect(cardRowActions?.exists()).toBe(true)
    expect(tableRowActions?.exists()).toBe(true)
    expect(cardRowActions?.classes()).toContain('standard-workbench-row-actions--quiet')
    expect(tableRowActions?.classes()).toContain('standard-workbench-row-actions--quiet')
    expect(cardRowActions?.props('gap')).toBeUndefined()
    expect(tableRowActions?.props('gap')).toBeUndefined()
    expect(((cardRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '进入工作台',
      '编辑',
      '删除'
    ])
    expect(((tableRowActions?.props('directItems') as Array<{ label: string }>) || []).map((item) => item.label)).toEqual([
      '进入工作台',
      '编辑',
      '删除'
    ])
    expect(((cardRowActions?.props('menuItems') as Array<unknown>) || []).length).toBe(0)
    expect(((tableRowActions?.props('menuItems') as Array<unknown>) || []).length).toBe(0)

    const actionColumn = wrapper
      .findAllComponents(ElTableColumnStub)
      .find((component) => component.props('label') === '操作')

    expect(String(actionColumn?.props('width'))).toBe('200')
  })

  it('shows a compact diagnostic intake hint when opened from system-log', async () => {
    mockRoute.query = {
      productKey: 'demo-product',
      traceId: 'trace-001'
    }
    installSessionStorageMock({
      'iot-access:diagnostic-context': JSON.stringify({
        storedAt: Date.now(),
        context: {
          sourcePage: 'system-log',
          productKey: 'demo-product',
          traceId: 'trace-001',
          capturedAt: new Date().toISOString()
        }
      })
    })

    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('来自异常观测台')
    expect(wrapper.text()).toContain('Trace trace-001')
    expect(wrapper.text()).toContain('优先核对产品定义与契约基线')
  })

  it('routes detail row actions to the overview detail page', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    ;(wrapper.vm as any).handleRowAction('detail', product)
    await nextTick()

    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/overview')
    expect(wrapper.find('.product-form-drawer-stub').exists()).toBe(false)
  })

  it('routes devices to the devices detail page and still supports in-place edit when explicitly requested', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    ;(wrapper.vm as any).handleRowAction('devices', product)
    await nextTick()
    expect(mockRouter.push).toHaveBeenCalledWith('/products/1001/devices')
    expect(wrapper.find('.product-business-workbench-drawer-stub').exists()).toBe(false)

    mockRouter.push.mockClear()
    ;(wrapper.vm as any).handleRowAction('edit', product)
    await flushPromises()
    await nextTick()

    expect(mockRouter.push).not.toHaveBeenCalled()
    expect(wrapper.get('[data-testid="product-form-drawer-title"]').text()).toBe('编辑产品')
    expect(wrapper.find('.product-form-drawer-stub').exists()).toBe(true)
  })

  it('loads formal property candidates into the edit workspace object-insight editor', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      status: 1
    }

    ;(wrapper.vm as any).handleRowAction('edit', product)
    await flushPromises()
    await nextTick()

    expect(mockListProductModels).toHaveBeenCalledWith(1001)
    const objectInsightEditor = wrapper.findComponent(ProductObjectInsightConfigEditorStub)
    expect(objectInsightEditor.exists()).toBe(true)
    expect((objectInsightEditor.props('availableModels') as Array<{ identifier: string }> | undefined)?.[0]?.identifier).toBe('value')
    expect(objectInsightEditor.text()).toContain('available-models:1')
  })

  it('keeps the unified workbench context in sync after saving product edits', async () => {
    const wrapper = mountView()

    const product = {
      id: 1001,
      productKey: 'demo-product',
      productName: '演示产品',
      protocolCode: 'mqtt-json',
      nodeType: 1,
      dataFormat: 'JSON',
      manufacturer: 'GHLZM',
      description: '原始说明',
      status: 1,
      deviceCount: 12,
      onlineDeviceCount: 8
    }

    const updatedProduct = {
      ...product,
      productName: '演示产品（已更新）',
      manufacturer: '更新厂商'
    }

    mockUpdateProduct.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: updatedProduct
    })

    ;(wrapper.vm as any).handleRowAction('edit', product)
    await nextTick()

    ;(wrapper.vm as any).formData.productName = '演示产品（已更新）'
    ;(wrapper.vm as any).formData.manufacturer = '更新厂商'

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()
    await nextTick()

    expect(mockUpdateProduct).toHaveBeenCalledWith(
      1001,
      expect.objectContaining({
        productName: '演示产品（已更新）',
        manufacturer: '更新厂商'
      })
    )
    expect((wrapper.vm as any).formVisible).toBe(true)
    expect((wrapper.vm as any).currentProduct.productName).toBe('演示产品（已更新）')
    expect((wrapper.vm as any).detailData.productName).toBe('演示产品（已更新）')
  })

  it('serializes product-level object insight metrics into metadataJson before submit', async () => {
    const wrapper = mountView()

    ;(wrapper.vm as any).handleAdd()
    await nextTick()

    ;(wrapper.vm as any).formData.productKey = 'demo-product'
    ;(wrapper.vm as any).formData.productName = '演示产品'
    ;(wrapper.vm as any).formData.protocolCode = 'mqtt-json'
    ;(wrapper.vm as any).formData.nodeType = 1
    ;(wrapper.vm as any).formData.metadataJson = JSON.stringify({
      site: '北坡监测点'
    })
    ;(wrapper.vm as any).productCapabilityType = 'COLLECTING'
    ;(wrapper.vm as any).objectInsightMetricRows = [
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '相对湿度',
        group: 'status',
        analysisTitle: '现场环境补充',
        analysisTag: '系统自定义参数',
        analysisTemplate: '{{label}}当前为{{value}}'
      }
    ]

    mockAddProduct.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 2001,
        productKey: 'demo-product',
        productName: '演示产品',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          site: '北坡监测点',
          objectInsight: {
            customMetrics: [
              {
                identifier: 'S1_ZT_1.humidity',
                displayName: '相对湿度',
                group: 'status'
              }
            ]
          }
        })
      }
    })

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()
    await nextTick()

    expect(mockAddProduct).toHaveBeenCalledWith(
      expect.objectContaining({
        metadataJson: expect.stringContaining('"site":"北坡监测点"')
      })
    )
    expect(mockAddProduct).toHaveBeenCalledWith(
      expect.objectContaining({
        metadataJson: expect.stringContaining('"identifier":"S1_ZT_1.humidity"')
      })
    )
    expect(mockAddProduct).toHaveBeenCalledWith(
      expect.objectContaining({
        metadataJson: expect.stringContaining('"productCapabilityType":"COLLECTING"')
      })
    )
  })

  it('blocks submit when object insight metrics contain duplicate identifiers', async () => {
    const wrapper = mountView()

    ;(wrapper.vm as any).handleAdd()
    await nextTick()

    ;(wrapper.vm as any).formData.productKey = 'demo-product'
    ;(wrapper.vm as any).formData.productName = '演示产品'
    ;(wrapper.vm as any).formData.protocolCode = 'mqtt-json'
    ;(wrapper.vm as any).formData.nodeType = 1
    ;(wrapper.vm as any).objectInsightMetricRows = [
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '相对湿度',
        group: 'status'
      },
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '重复湿度',
        group: 'status'
      }
    ]

    await (wrapper.vm as any).handleSubmit()
    await flushPromises()

    expect(mockAddProduct).not.toHaveBeenCalled()
    expect(vi.mocked(ElMessage.error)).toHaveBeenCalledWith('对象洞察配置中存在重复指标标识：S1_ZT_1.humidity')
  })

  it('removes the standalone device and model drawers from the /products page composition', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    expect(wrapper.findComponent(DeviceListDrawerStub).exists()).toBe(false)
    expect(wrapper.findComponent(StandardDetailDrawerStub).exists()).toBe(false)
  })

  it('labels the applied quick-search chip as quick search instead of product name', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).appliedFilters.productName = 'demo-product'
    await nextTick()

    expect((wrapper.vm as any).activeFilterTags[0].label).toBe('快速搜索：demo-product')
  })

  it('advertises product key support in the quick-search placeholder copy', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/ProductWorkbenchView.vue'), 'utf8')

    expect(source).toContain('快速搜索（产品名称、产品 Key、厂商）')
  })

  it('uses the shared list surface and mobile-card grammar', () => {
    const source = readFileSync(resolve(import.meta.dirname, '../../views/ProductWorkbenchView.vue'), 'utf8')

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('standard-mobile-record-grid')
    expect(source).toContain('standard-mobile-record-card')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
  })
})
