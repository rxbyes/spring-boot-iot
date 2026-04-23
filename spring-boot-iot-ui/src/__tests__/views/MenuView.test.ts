import { defineComponent, h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'

import MenuView from '@/views/MenuView.vue'

const {
  mockAddMenu,
  mockDeleteMenu,
  mockGetMenu,
  mockListMenus,
  mockPageMenus,
  mockUpdateMenu,
  mockPermissionStore,
  mockRouter
} = vi.hoisted(() => ({
  mockAddMenu: vi.fn(),
  mockDeleteMenu: vi.fn(),
  mockGetMenu: vi.fn(),
  mockListMenus: vi.fn(),
  mockPageMenus: vi.fn(),
  mockUpdateMenu: vi.fn(),
  mockPermissionStore: {
    hasPermission: vi.fn(() => true),
    fetchCurrentUser: vi.fn()
  },
  mockRouter: {
    push: vi.fn()
  }
}))

vi.mock('@/api/menu', () => ({
  addMenu: mockAddMenu,
  deleteMenu: mockDeleteMenu,
  getMenu: mockGetMenu,
  listMenus: mockListMenus,
  pageMenus: mockPageMenus,
  updateMenu: mockUpdateMenu
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: () => mockPermissionStore
}))

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter
}))

vi.mock('@/utils/confirm', () => ({
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

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0))

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  template: `
    <section class="menu-view-workbench-stub">
      <div><slot name="header-actions" /></div>
      <div><slot name="filters" /></div>
      <div><slot name="applied-filters" /></div>
      <div><slot name="notices" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: '<div><slot name="primary" /><slot name="actions" /></div>'
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: '<div><slot /><slot name="right" /></div>'
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
})

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue'],
  template: `
    <section v-if="modelValue" class="menu-view-form-drawer-stub">
      <slot />
      <slot name="footer" />
    </section>
  `
})

const StandardDrawerFooterStub = defineComponent({
  name: 'StandardDrawerFooter',
  emits: ['cancel', 'confirm'],
  template: `
    <div class="menu-view-drawer-footer-stub">
      <slot />
    </div>
  `
})

const ElFormStub = defineComponent({
  name: 'ElForm',
  setup(_, { slots, expose }) {
    expose({
      validate: vi.fn().mockResolvedValue(true),
      clearValidate: vi.fn()
    })
    return () => h('form', { class: 'el-form-stub' }, slots.default?.())
  }
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItem',
  template: '<label><slot /></label>'
})

function mountMenuView() {
  return mount(MenuView, {
    global: {
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      stubs: {
        EmptyState: true,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: StandardDrawerFooterStub,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardPagination: true,
        StandardTableTextColumn: true,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardWorkbenchRowActions: true,
        ElAlert: true,
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: true,
        ElInputNumber: true,
        ElOption: true,
        ElRadio: true,
        ElRadioGroup: true,
        ElSelect: true,
        ElTable: true,
        ElTableColumn: true,
        ElTag: true,
        StandardButton: StandardButtonStub
      }
    }
  })
}

describe('MenuView', () => {
  beforeEach(() => {
    mockAddMenu.mockReset()
    mockDeleteMenu.mockReset()
    mockGetMenu.mockReset()
    mockListMenus.mockReset()
    mockPageMenus.mockReset()
    mockUpdateMenu.mockReset()
    mockPermissionStore.hasPermission.mockReset()
    mockPermissionStore.hasPermission.mockReturnValue(true)
    mockPermissionStore.fetchCurrentUser.mockReset()
    mockPermissionStore.fetchCurrentUser.mockResolvedValue(null)
    mockRouter.push.mockReset()
    mockPageMenus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { total: 0, pageNum: 1, pageSize: 10, records: [] }
    })
    mockListMenus.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockAddMenu.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: { id: 1 }
    })
    mockUpdateMenu.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  })

  it('refreshes the current auth context after saving a menu successfully', async () => {
    const wrapper = mountMenuView()

    await flushPromises()
    await nextTick()

    ;(wrapper.vm as any).openAddRoot()
    ;(wrapper.vm as any).formRef = {
      validate: vi.fn().mockResolvedValue(true),
      clearValidate: vi.fn()
    }
    ;(wrapper.vm as any).form.menuName = '秘钥治理'
    ;(wrapper.vm as any).form.menuCode = 'system:governance-security'
    ;(wrapper.vm as any).form.path = '/governance-security'

    await (wrapper.vm as any).submitForm()
    await flushPromises()
    await nextTick()

    expect(mockAddMenu).toHaveBeenCalledTimes(1)
    expect(mockPermissionStore.fetchCurrentUser).toHaveBeenCalledTimes(1)
    expect(ElMessage.success).toHaveBeenCalledWith('新增成功')
  })
})
