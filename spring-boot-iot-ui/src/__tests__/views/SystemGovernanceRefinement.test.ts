import { defineComponent } from 'vue'
import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DictView from '@/views/DictView.vue'
import RoleView from '@/views/RoleView.vue'
import UserView from '@/views/UserView.vue'

const {
  mockPageUsers,
  mockPageRoles,
  mockPageDicts,
  mockListMenuTree,
  mockListDictItems
} = vi.hoisted(() => ({
  mockPageUsers: vi.fn(),
  mockPageRoles: vi.fn(),
  mockPageDicts: vi.fn(),
  mockListMenuTree: vi.fn(),
  mockListDictItems: vi.fn()
}))

vi.mock('@/api/user', () => ({
  pageUsers: mockPageUsers,
  getUser: vi.fn(),
  addUser: vi.fn(),
  updateUser: vi.fn(),
  deleteUser: vi.fn(),
  resetPassword: vi.fn()
}))

vi.mock('@/api/role', () => ({
  pageRoles: mockPageRoles,
  getRole: vi.fn(),
  addRole: vi.fn(),
  updateRole: vi.fn(),
  deleteRole: vi.fn()
}))

vi.mock('@/api/menu', () => ({
  listMenuTree: mockListMenuTree
}))

vi.mock('@/api/dict', () => ({
  pageDicts: mockPageDicts,
  getDict: vi.fn(),
  addDict: vi.fn(),
  updateDict: vi.fn(),
  deleteDict: vi.fn(),
  listDictItems: mockListDictItems,
  addDictItem: vi.fn(),
  updateDictItem: vi.fn(),
  deleteDictItem: vi.fn()
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
          dictType: 'text',
          itemType: 'string',
          dictName: '测试字典',
          itemName: '测试项'
        }"
      />
    </div>
  `
})

const ElTreeStub = defineComponent({
  name: 'ElTree',
  template: `
    <div class="el-tree-stub">
      <slot
        :data="{
          menuName: '系统管理',
          menuCode: 'system:root',
          path: '/system',
          type: 0,
          disabled: false
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
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardListFilterHeader: true,
        StandardPagination: true,
        StandardTableTextColumn: true,
        StandardTableToolbar: true,
        StandardButton: true,
        StandardRowActions: true,
        StandardActionLink: true,
        CsvColumnSettingDialog: true,
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
        'el-alert': true,
        'el-tree': ElTreeStub,
        'el-input-number': true
      }
    }
  })
}

describe('system governance refinement', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()

    mockPageUsers.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockPageRoles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockPageDicts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mockListMenuTree.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1001,
          menuName: '系统管理',
          menuCode: 'system:root',
          path: '/system',
          type: 0,
          children: []
        }
      ]
    })
    mockListDictItems.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
  })

  it('keeps the user workbench calm and removes the legacy English drawer eyebrow tier', async () => {
    const wrapper = mountView(UserView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增用户')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护账号基础信息')
    expect(wrapper.text()).toContain('账号中心')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('keeps the role workbench calm and removes the legacy English drawer eyebrow tier', async () => {
    const wrapper = mountView(RoleView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawer = wrapper.findComponent(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawer.props('eyebrow')).toBeUndefined()
    expect(drawer.props('title')).toBe('新增角色')
    expect(drawer.props('subtitle')).toContain('右侧抽屉维护角色基础信息')
    expect(wrapper.text()).toContain('角色权限')
    expect(wrapper.text()).toContain('菜单与按钮授权')
    expect(wrapper.text()).not.toContain('System Form')
  })

  it('keeps dict management drawers in Chinese and removes every legacy English eyebrow tier', async () => {
    const wrapper = mountView(DictView)

    await flushPromises()

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub)
    const drawers = wrapper.findAllComponents(StandardFormDrawerStub)

    expect(workbench.props('eyebrow')).toBeUndefined()
    expect(drawers).toHaveLength(3)
    expect(drawers.map((drawer) => drawer.props('eyebrow'))).toEqual([undefined, undefined, undefined])
    expect(drawers.map((drawer) => drawer.props('title'))).toEqual(['新增字典', '字典项管理', '新增字典项'])
    expect(drawers[0].props('subtitle')).toContain('右侧抽屉维护字典分类')
    expect(drawers[1].props('subtitle')).toContain('右侧抽屉查看和维护字典项明细')
    expect(drawers[2].props('subtitle')).toContain('右侧抽屉维护字典项')
    expect(wrapper.text()).toContain('数据字典')
    expect(wrapper.text()).not.toContain('System Form')
    expect(wrapper.text()).not.toContain('Dictionary Items')
    expect(wrapper.text()).not.toContain('Dictionary Item')
  })
})
