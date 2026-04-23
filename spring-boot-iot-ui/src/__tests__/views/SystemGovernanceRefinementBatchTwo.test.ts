import { defineComponent } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ChannelView from '@/views/ChannelView.vue'
import HelpDocView from '@/views/HelpDocView.vue'
import MenuView from '@/views/MenuView.vue'
import OrganizationView from '@/views/OrganizationView.vue'
import RegionView from '@/views/RegionView.vue'

const {
  mockRouterPush,
  mockPageMenus,
  mockListMenus,
  mockPageRegions,
  mockListRegions,
  mockPageOrganizations,
  mockListOrganizations,
  mockPageChannels,
  mockPageHelpDocuments,
  mockListRoles,
  mockFetchChannelTypeOptions,
  mockFetchHelpDocCategoryOptions,
  mockHasPermission,
  mockFetchCurrentUser
} = vi.hoisted(() => ({
  mockRouterPush: vi.fn(),
  mockPageMenus: vi.fn(),
  mockListMenus: vi.fn(),
  mockPageRegions: vi.fn(),
  mockListRegions: vi.fn(),
  mockPageOrganizations: vi.fn(),
  mockListOrganizations: vi.fn(),
  mockPageChannels: vi.fn(),
  mockPageHelpDocuments: vi.fn(),
  mockListRoles: vi.fn(),
  mockFetchChannelTypeOptions: vi.fn(),
  mockFetchHelpDocCategoryOptions: vi.fn(),
  mockHasPermission: vi.fn(() => true),
  mockFetchCurrentUser: vi.fn().mockResolvedValue(undefined)
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: mockRouterPush
  })
}))

vi.mock('@/api/menu', () => ({
  pageMenus: mockPageMenus,
  listMenus: mockListMenus,
  getMenu: vi.fn(),
  addMenu: vi.fn(),
  updateMenu: vi.fn(),
  deleteMenu: vi.fn()
}))

vi.mock('@/api/region', () => ({
  pageRegions: mockPageRegions,
  listRegions: mockListRegions,
  getRegion: vi.fn(),
  addRegion: vi.fn(),
  updateRegion: vi.fn(),
  deleteRegion: vi.fn()
}))

vi.mock('@/api/organization', () => ({
  pageOrganizations: mockPageOrganizations,
  listOrganizations: mockListOrganizations,
  getOrganization: vi.fn(),
  addOrganization: vi.fn(),
  updateOrganization: vi.fn(),
  deleteOrganization: vi.fn()
}))

vi.mock('@/api/channel', () => ({
  CHANNEL_TYPES: [
    { label: '邮件', value: 'email' },
    { label: 'Webhook', value: 'webhook' }
  ],
  pageChannels: mockPageChannels,
  fetchChannelTypeOptions: mockFetchChannelTypeOptions,
  getChannelByCode: vi.fn(),
  addChannel: vi.fn(),
  updateChannel: vi.fn(),
  deleteChannel: vi.fn(),
  testChannel: vi.fn()
}))

vi.mock('@/api/helpDoc', () => ({
  HELP_DOC_CATEGORY_OPTIONS: [
    { label: '业务类', value: 'business' },
    { label: '技术类', value: 'technical' },
    { label: '常见问题', value: 'faq' }
  ],
  pageHelpDocuments: mockPageHelpDocuments,
  fetchHelpDocCategoryOptions: mockFetchHelpDocCategoryOptions,
  getHelpDocument: vi.fn(),
  addHelpDocument: vi.fn(),
  updateHelpDocument: vi.fn(),
  deleteHelpDocument: vi.fn()
}))

vi.mock('@/api/role', () => ({
  listRoles: mockListRoles
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => ({
    hasPermission: mockHasPermission,
    fetchCurrentUser: mockFetchCurrentUser
  })
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
    <section class="standard-workbench-panel-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <slot name="header-actions" />
      <slot name="filters" />
      <slot name="applied-filters" />
      <slot name="notices" />
      <slot name="toolbar" />
      <slot />
      <slot name="pagination" />
    </section>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle', 'size'],
  template: `
    <section class="standard-form-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p v-if="subtitle">{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDetailDrawerStub = defineComponent({
  name: 'StandardDetailDrawer',
  props: ['modelValue', 'eyebrow', 'title', 'subtitle'],
  template: `
    <section class="standard-detail-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p v-if="subtitle">{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  template: `
    <div class="el-table-stub">
      <slot />
    </div>
  `
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label', 'prop'],
  template: `
    <div class="el-table-column-stub">
      <span v-if="label">{{ label }}</span>
      <slot
        :row="{
          status: 1,
          type: 1,
          regionType: 'province',
          orgType: 'dept',
          channelType: 'email',
          docCategory: 'business',
          visibleRoleCodes: 'admin',
          relatedPaths: '/system/help-doc',
          targetType: 'all'
        }"
      />
    </div>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label', 'prop'],
  template: `
    <div class="standard-table-text-column-stub">
      <span v-if="label">{{ label }}</span>
      <slot
        :row="{
          status: 1,
          type: 1,
          regionType: 'province',
          orgType: 'dept',
          channelType: 'email',
          docCategory: 'business',
          visibleRoleCodes: 'admin',
          relatedPaths: '/system/help-doc'
        }"
      />
    </div>
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountView(component: object) {
  return shallowMount(component, {
    global: {
      renderStubDefaultSlot: true,
      directives: {
        permission: () => {}
      },
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardDetailDrawer: StandardDetailDrawerStub,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardListFilterHeader: true,
        StandardPagination: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        StandardTableToolbar: true,
        StandardButton: true,
        StandardWorkbenchRowActions: true,
        StandardRowActions: true,
        StandardActionLink: true,
        CsvColumnSettingDialog: true,
        EmptyState: true,
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub,
        'el-form': true,
        'el-form-item': true,
        'el-input': true,
        'el-select': true,
        'el-option': true,
        'el-tag': true,
        'el-radio-group': true,
        'el-radio': true,
        'el-input-number': true,
        'el-alert': true,
        'el-row': true,
        'el-col': true
      }
    }
  })
}

describe('system governance refinement batch two', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    mockHasPermission.mockReturnValue(true)
    mockFetchCurrentUser.mockResolvedValue(undefined)
    mockFetchChannelTypeOptions.mockResolvedValue([
      { label: '閭欢', value: 'email', sortNo: 1 },
      { label: 'Webhook', value: 'webhook', sortNo: 2 }
    ])
    mockFetchHelpDocCategoryOptions.mockResolvedValue([
      { label: 'Business', value: 'business' },
      { label: 'Technical', value: 'technical' },
      { label: 'FAQ', value: 'faq' }
    ])

    mockPageMenus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockListMenus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockPageRegions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockListRegions.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockPageOrganizations.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockListOrganizations.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockPageChannels.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockPageHelpDocuments.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockListRoles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          roleCode: 'admin',
          roleName: '管理员',
          status: 1
        }
      ]
    })
  })

  it('removes the legacy English drawer eyebrow tier from the menu workbench', async () => {
    const wrapper = mountView(MenuView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增菜单')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护菜单树')
    expect(wrapper.text()).toContain('导航编排')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('removes the legacy English drawer eyebrow tier from the region workbench', async () => {
    const wrapper = mountView(RegionView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增区域')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护区域层级')
    expect(wrapper.text()).toContain('区域版图')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('removes the legacy English drawer eyebrow tier from the organization workbench', async () => {
    const wrapper = mountView(OrganizationView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增组织机构')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护组织层级')
    expect(wrapper.text()).toContain('组织架构')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('removes the legacy English drawer eyebrow tier from the channel workbench', async () => {
    const wrapper = mountView(ChannelView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增通知编排')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护通知编排')
    expect(wrapper.text()).toContain('通知编排')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('removes the legacy English content and form eyebrow tiers from help docs', async () => {
    const wrapper = mountView(HelpDocView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const detailDrawer = wrapper.findComponent(StandardDetailDrawerStub)
    const formDrawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(detailDrawer.props('eyebrow')).toBeUndefined()
    expect(formDrawer.props('eyebrow')).toBeUndefined()
    expect(detailDrawer.props('title')).toBe('帮助文档详情')
    expect(formDrawer.props('title')).toBe('新增帮助文档')
    expect(wrapper.text()).toContain('帮助文档管理')
    expect(wrapper.text()).not.toContain('System Content')
    expect(wrapper.text()).not.toContain('System Form')
  })
})
