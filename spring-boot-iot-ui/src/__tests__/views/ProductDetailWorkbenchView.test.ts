import { defineComponent, nextTick } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { beforeEach, expect, it, vi } from 'vitest'

import ProductDetailWorkbenchView from '@/views/ProductDetailWorkbenchView.vue'

const { mockRoute, mockPageDevices } = vi.hoisted(() => ({
  mockRoute: {
    path: '/products/42/overview',
    name: 'product-overview',
    params: { productId: '42' },
    query: {}
  },
  mockPageDevices: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => ({
    push: vi.fn()
  })
}))

vi.mock('@/api/product', () => ({
  productApi: {
    getProductById: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 42,
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        description: '采集型遥测终端设备，协议 mqtt-json，直接接入'
      }
    }),
    getProductOverviewSummary: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  }
}))

vi.mock('@/api/device', () => ({
  deviceApi: {
    pageDevices: mockPageDevices
  }
}))

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['title', 'description', 'eyebrow', 'breadcrumbs', 'showTitle'],
  template: `
    <section>
      <p class="shell-title">{{ title }}</p>
      <p v-if="eyebrow" class="shell-eyebrow">{{ eyebrow }}</p>
      <pre v-if="breadcrumbs" class="shell-breadcrumbs">{{ JSON.stringify(breadcrumbs) }}</pre>
      <slot name="actions" />
      <slot />
    </section>
  `
})

const ProductDeviceListWorkspaceStub = defineComponent({
  name: 'ProductDeviceListWorkspace',
  props: ['pagination'],
  emits: ['viewDevice', 'page-change', 'page-size-change'],
  template: `
    <section class="product-device-list-workspace-stub">
      <span class="device-page-num">{{ pagination?.pageNum }}</span>
      <span class="device-page-size">{{ pagination?.pageSize }}</span>
      <button class="emit-page-2" type="button" @click="$emit('page-change', 2)">page2</button>
      <button class="emit-size-20" type="button" @click="$emit('page-size-change', 20)">size20</button>
    </section>
  `
})

beforeEach(() => {
  mockRoute.path = '/products/42/overview'
  mockRoute.name = 'product-overview'
  mockRoute.params = { productId: '42' }
  mockRoute.query = {}
  mockPageDevices.mockReset()
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
  })
})

it.each([
  ['product-overview', '产品总览'],
  ['product-contracts', '契约字段'],
  ['product-mapping-rules', '映射规则']
])('titles the page by active section for %s', async (routeName, expectedTitle) => {
  mockRoute.name = routeName

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: true,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(wrapper.find('.shell-title').text()).toBe(expectedTitle)
  expect(wrapper.find('.shell-eyebrow').exists()).toBe(false)
  expect(wrapper.find('.shell-breadcrumbs').exists()).toBe(false)
})

it('requests related devices with server pagination and reacts to pagination events', async () => {
  mockRoute.path = '/products/42/devices'
  mockRoute.name = 'product-devices'
  mockPageDevices.mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 25, pageNum: 1, pageSize: 10, records: [] }
  })

  const wrapper = shallowMount(ProductDetailWorkbenchView, {
    global: {
      stubs: {
        RouterLink: true,
        StandardButton: true,
        ProductDetailWorkbench: true,
        ProductDeviceListWorkspace: ProductDeviceListWorkspaceStub,
        ProductModelDesignerWorkspace: true,
        StandardPageShell: StandardPageShellStub
      }
    }
  })

  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 10
  })

  await wrapper.get('.emit-page-2').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 2,
    pageSize: 10
  })

  await wrapper.get('.emit-size-20').trigger('click')
  await flushPromises()
  await nextTick()

  expect(mockPageDevices).toHaveBeenLastCalledWith({
    productKey: 'nf-collect-rtu-v1',
    pageNum: 1,
    pageSize: 20
  })
})
