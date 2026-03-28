import { computed, defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import SectionLandingView from '@/views/SectionLandingView.vue'

const { mockRoute, mockRouter, permissionState } = vi.hoisted(() => ({
  mockRoute: {
    path: '/device-access',
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn()
  },
  permissionState: {
    hasRoutePermission: true
  }
}))

vi.mock('vue-router', () => ({
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : \'#\'"><slot /></a>'
  }),
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    primaryRoleName: '开发人员',
    roleProfile: {
      focusLabel: '接入智维',
      featuredPaths: ['/products', '/devices']
    },
    userInfo: {
      accountType: '正式账号',
      authStatus: '已实名'
    },
    hasRoutePermission: () => permissionState.hasRoutePermission
  })
}))

vi.mock('@/stores/activity', () => ({
  activityEntries: computed(() => [
    {
      id: 'activity-1',
      title: '链路验证中心 · 发送模拟上报',
      detail: '刚刚完成一次 HTTP 模拟上报',
      ok: true,
      createdAt: '2026-03-27T09:00:00.000Z',
      path: '/reporting'
    }
  ])
}))

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title', 'description', 'eyebrow'],
  template: `
    <section class="panel-card-stub">
      <p>{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const EmptyStateStub = defineComponent({
  name: 'EmptyState',
  props: ['title', 'description', 'action'],
  template: `
    <section class="empty-state-stub">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <button v-if="action" type="button" class="empty-state-action">{{ action.label }}</button>
    </section>
  `
})

function mountView() {
  return mount(SectionLandingView, {
    global: {
      stubs: {
        EmptyState: EmptyStateStub,
        PanelCard: PanelCardStub
      }
    }
  })
}

describe('SectionLandingView', () => {
  beforeEach(() => {
    mockRoute.path = '/device-access'
    mockRoute.query = {}
    mockRouter.push.mockReset()
    permissionState.hasRoutePermission = true
  })

  it('renders a compact iot access entry strip and keeps the hub sections', () => {
    const wrapper = mountView()

    expect(wrapper.find('.iot-access-page-shell').exists()).toBe(true)
    expect(wrapper.find('.iot-access-tab-workspace').exists()).toBe(true)
    expect(wrapper.text()).toContain('接入智维')
    expect(wrapper.text()).toContain('先处理资产底座，再进入链路诊断。')
    expect(wrapper.text()).toContain('推荐处理')
    expect(wrapper.text()).toContain('最近使用')
    expect(wrapper.text()).toContain('全部能力')
    expect(wrapper.text()).toContain('产品定义中心')
    expect(wrapper.text()).toContain('最近使用')
    expect(wrapper.text()).toContain('推荐处理顺序')
    expect(wrapper.text()).toContain('全部能力')
    expect(wrapper.text()).toContain('链路验证中心 · 发送模拟上报')
  })

  it('hides hub chrome and self-link CTA when the user has no accessible pages', () => {
    permissionState.hasRoutePermission = false

    const wrapper = mountView()

    expect(wrapper.text()).toContain('当前账号暂无可用入口')
    expect(wrapper.text()).toContain('暂无分组入口')
    expect(wrapper.text()).not.toContain('接入智维')
    expect(wrapper.text()).not.toContain('最近使用')
    expect(wrapper.text()).not.toContain('推荐处理顺序')
    expect(wrapper.text()).not.toContain('全部能力')
    expect(wrapper.find('.empty-state-action').exists()).toBe(false)
  })
})
