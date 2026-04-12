import { computed, defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import SectionLandingView from '@/views/SectionLandingView.vue'

const { mockRoute, mockRouter, permissionState } = vi.hoisted(() => ({
  mockRoute: {
    path: '/device-access'
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
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot />
    </section>
  `
})

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['breadcrumbs', 'title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
})

const IotAccessTabWorkspaceStub = defineComponent({
  name: 'IotAccessTabWorkspace',
  props: ['items'],
  template: `
    <section class="iot-access-tab-workspace-stub">
      <span v-for="item in items || []" :key="item.key">{{ item.label }}</span>
      <slot :active-key="items?.[0]?.key" />
    </section>
  `
})

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="standard-workbench-panel-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="filters" />
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
        StandardPageShell: StandardPageShellStub,
        IotAccessTabWorkspace: IotAccessTabWorkspaceStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        EmptyState: EmptyStateStub,
        PanelCard: PanelCardStub
      }
    }
  })
}

describe('SectionLandingView', () => {
  beforeEach(() => {
    mockRoute.path = '/device-access'
    mockRouter.push.mockReset()
    permissionState.hasRoutePermission = true
  })

  it('renders the iot access hub as two real business tabs with a single entry list', () => {
    const wrapper = mountView()
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true)
    expect(wrapper.find('.iot-access-tab-workspace-stub').exists()).toBe(true)
    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).toContain('资产底座')
    expect(wrapper.text()).toContain('诊断排障')
    expect(wrapper.text()).toContain('标准排障路径')
    expect(wrapper.text()).toContain('总览负责回答先去哪、再去哪、最后去哪修')
    expect(wrapper.text()).toContain('链路验证中心')
    expect(wrapper.text()).toContain('链路追踪台')
    expect(wrapper.text()).toContain('异常观测台')
    expect(wrapper.text()).toContain('数据校验台')
    expect(wrapper.text()).toContain('产品定义中心')
    expect(wrapper.text()).toContain('设备资产中心')
    expect(wrapper.text()).not.toContain('QUIET CONSOLE')
    expect(wrapper.text()).not.toContain('最近使用')
    expect(wrapper.text()).not.toContain('推荐处理顺序')
    expect(wrapper.text()).not.toContain('全部能力')
  })

  it('hides hub chrome and self-link CTA when the user has no accessible pages', () => {
    permissionState.hasRoutePermission = false

    const wrapper = mountView()
    const emptyPanel = wrapper.findComponent(PanelCardStub)

    expect(wrapper.text()).toContain('当前账号暂无可用入口')
    expect(wrapper.text()).toContain('暂无分组入口')
    expect(emptyPanel.props('eyebrow')).toBeUndefined()
    expect(wrapper.text()).not.toContain('接入智维')
    expect(wrapper.text()).not.toContain('ACCESS STATUS')
    expect(wrapper.text()).not.toContain('最近使用')
    expect(wrapper.text()).not.toContain('推荐处理顺序')
    expect(wrapper.text()).not.toContain('全部能力')
    expect(wrapper.find('.empty-state-action').exists()).toBe(false)
  })
})
