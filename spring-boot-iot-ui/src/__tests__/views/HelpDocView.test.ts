import { computed, defineComponent, inject, nextTick, provide, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

import HelpDocView from '@/views/HelpDocView.vue'

const { mockPageHelpDocuments, mockListRoles, mockFetchHelpDocCategoryOptions } = vi.hoisted(() => ({
  mockPageHelpDocuments: vi.fn(),
  mockListRoles: vi.fn(),
  mockFetchHelpDocCategoryOptions: vi.fn()
}))

vi.mock('@/api/helpDoc', () => ({
  HELP_DOC_CATEGORY_OPTIONS: [
    { label: '业务类', value: 'business' },
    { label: '技术类', value: 'technical' },
    { label: '常见问题', value: 'faq' }
  ],
  fetchHelpDocCategoryOptions: mockFetchHelpDocCategoryOptions,
  pageHelpDocuments: mockPageHelpDocuments,
  getHelpDocument: vi.fn(),
  addHelpDocument: vi.fn(),
  updateHelpDocument: vi.fn(),
  deleteHelpDocument: vi.fn()
}))

vi.mock('@/api/role', () => ({
  listRoles: mockListRoles
}))

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
}))

vi.mock('@/utils/sectionWorkspaces', () => ({
  listWorkspaceCommandEntries: () => [
    {
      type: 'page',
      path: '/help-doc',
      workspaceLabel: '平台治理',
      title: '帮助文档管理'
    }
  ]
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
    <section class="help-doc-workbench-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="header-actions" /></div>
      <div><slot name="filters" /></div>
      <div><slot name="applied-filters" /></div>
      <div class="help-doc-toolbar-stub"><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
})

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="help-doc-filter-stub">
      <div><slot name="primary" /></div>
      <div><slot name="actions" /></div>
    </section>
  `
})

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="help-doc-table-toolbar-stub">
      <slot />
      <slot name="right" />
    </section>
  `
})

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled'],
  emits: ['click'],
  template: `
    <button type="button" :disabled="Boolean(disabled)" @click="$emit('click')">
      <slot />
    </button>
  `
})

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label', 'items', 'disabled'],
  emits: ['command'],
  template: `
    <div
      class="help-doc-action-menu-stub"
      :data-label="label"
      :data-disabled="Boolean(disabled)"
      :data-items="JSON.stringify(items || [])"
    >
      <button type="button">{{ label }}</button>
    </div>
  `
})

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  setup(props) {
    provide('tableRows', computed(() => props.data ?? []))
    return {}
  },
  template: '<section class="help-doc-table-stub"><slot /></section>'
})

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  setup() {
    const rows = inject('tableRows', ref([]))
    return { rows }
  },
  template: `
    <div class="help-doc-column-stub">
      <div v-for="(row, index) in rows" :key="index">
        <slot :row="row" />
      </div>
    </div>
  `
})

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  setup() {
    const rows = inject('tableRows', ref([]))
    return { rows }
  },
  template: `
    <div class="help-doc-text-column-stub">
      <div v-if="$slots.default">
        <div v-for="(row, index) in rows" :key="index">
          <slot :row="row" />
        </div>
      </div>
      <span v-else>{{ label }}</span>
    </div>
  `
})

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return mount(HelpDocView, {
    global: {
      plugins: [pinia],
      directives: {
        loading: () => undefined,
        permission: () => undefined
      },
      renderStubDefaultSlot: true,
      stubs: {
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardButton: StandardButtonStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardAppliedFiltersBar: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        StandardWorkbenchRowActions: true,
        StandardActionLink: true,
        StandardPagination: true,
        StandardDetailDrawer: true,
        StandardFormDrawer: true,
        StandardDrawerFooter: true,
        ElTable: ElTableStub,
        ElTableColumn: ElTableColumnStub,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true,
        ElTag: true,
        ElAlert: true,
        ElForm: true,
        ElRow: true,
        ElCol: true,
        ElRadioGroup: true,
        ElRadio: true,
        ElInputNumber: true
      }
    }
  })
}

describe('HelpDocView', () => {
  beforeEach(() => {
    mockPageHelpDocuments.mockReset()
    mockListRoles.mockReset()
    mockFetchHelpDocCategoryOptions.mockReset()
    mockPageHelpDocuments.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            id: 1,
            title: '告警闭环 FAQ',
            docCategory: 'faq',
            summary: '说明常见闭环问题',
            status: 1,
            sortNo: 10,
            updateTime: '2026-03-29 10:00:00'
          }
        ]
      }
    })
    mockListRoles.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: []
    })
    mockFetchHelpDocCategoryOptions.mockResolvedValue([
      { label: '业务类', value: 'business', sortNo: 1 },
      { label: '技术类', value: 'technical', sortNo: 2 },
      { label: '常见问题', value: 'faq', sortNo: 3 }
    ])
  })

  it('loads dict-backed help doc category options on mount', async () => {
    mountView()
    await flushPromises()
    await nextTick()

    expect(mockFetchHelpDocCategoryOptions).toHaveBeenCalledTimes(1)
  })

  it('keeps refresh as the direct toolbar action and moves clear-selection into more actions', async () => {
    const wrapper = mountView()
    await flushPromises()
    await nextTick()

    const toolbarText = wrapper.find('.help-doc-toolbar-stub').text()

    expect(toolbarText).toContain('刷新列表')
    expect(toolbarText).toContain('更多操作')
    expect(toolbarText).not.toContain('清空选中')

    const actionMenu = wrapper.findComponent(StandardActionMenuStub)
    expect(actionMenu.exists()).toBe(true)
    expect(actionMenu.props('label')).toBe('更多操作')
    expect(actionMenu.props('items')).toEqual([
      expect.objectContaining({
        command: 'clear-selection',
        label: '清空选中',
        disabled: true
      })
    ])
  })
})
