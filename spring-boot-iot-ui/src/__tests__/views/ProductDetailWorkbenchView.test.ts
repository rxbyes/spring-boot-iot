import { defineComponent, nextTick } from 'vue'
import { shallowMount, flushPromises } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import ProductDetailWorkbenchView from '@/views/ProductDetailWorkbenchView.vue'

const mockRoute = vi.hoisted(() => ({
  path: '/products/42/overview',
  name: 'product-overview',
  params: { productId: '42' },
  query: {}
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
    pageDevices: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
    })
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
        ProductDeviceListWorkspace: true,
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
