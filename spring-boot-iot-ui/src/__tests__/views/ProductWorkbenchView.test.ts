import { defineComponent, nextTick } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ProductWorkbenchView from '@/views/ProductWorkbenchView.vue'

const { mockRoute, mockRouter, mockPageProducts } = vi.hoisted(() => ({
  mockRoute: {
    path: '/products',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    replace: vi.fn(),
    push: vi.fn()
  },
  mockPageProducts: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/api/product', () => ({
  productApi: {
    pageProducts: mockPageProducts,
    getProductById: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    addProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    updateProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
    deleteProduct: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
  }
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
  props: ['title', 'description'],
  template: `
    <section class="product-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="product-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="product-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="product-workbench-panel-stub__body"><slot /></div>
      <div class="product-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="product-list-filter-header-stub">
      <div class="product-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="product-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
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

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  template: `
    <section class="product-detail-drawer-stub">
      <div class="product-detail-drawer-stub__header-actions"><slot name="header-actions" /></div>
      <div class="product-detail-drawer-stub__body"><slot /></div>
      <div class="product-detail-drawer-stub__footer"><slot name="footer" /></div>
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  template: '<section class="product-form-drawer-stub"><slot /><slot name="footer" /></section>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
})

const ProductModelDesignerDrawerStub = defineComponent({
  name: 'ProductModelDesignerDrawer',
  props: ['modelValue', 'product'],
  template: `
    <section v-if="modelValue" class="product-model-designer-drawer-stub">
      <h3>属性模型</h3>
      <h3>事件模型</h3>
      <h3>服务模型</h3>
      <p>暂无物模型</p>
    </section>
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function installSessionStorageMock() {
  const store = new Map<string, string>()
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
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardRowActions: StandardRowActionsStub,
        StandardActionLink: StandardActionLinkStub,
        StandardActionMenu: true,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardButton: StandardButtonStub,
        ProductModelDesignerDrawer: ProductModelDesignerDrawerStub,
        StandardDrawerFooter: true,
        StandardAppliedFiltersBar: true,
        StandardTableToolbar: true,
        StandardInlineState: true,
        StandardPagination: true,
        StandardTableTextColumn: true,
        CsvColumnSettingDialog: true,
        DeviceListDrawer: true,
        EmptyState: true
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
    mockPageProducts.mockReset()
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
    installSessionStorageMock()
  })

  it('renders a product-model designer entry and opens the empty designer state', async () => {
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
    ;(wrapper.vm as any).viewType = 'card'
    await nextTick()

    const designerEntry = wrapper.find('[data-testid="open-product-model-designer"]')
    expect(designerEntry.exists()).toBe(true)

    await designerEntry.trigger('click')
    await flushPromises()
    await nextTick()

    expect(wrapper.text()).toContain('属性模型')
    expect(wrapper.text()).toContain('事件模型')
    expect(wrapper.text()).toContain('服务模型')
    expect(wrapper.text()).toContain('暂无物模型')
  })
})
