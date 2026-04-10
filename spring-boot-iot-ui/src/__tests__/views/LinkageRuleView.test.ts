import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import LinkageRuleView from '@/views/LinkageRuleView.vue';

const {
  mockPageRuleList,
  mockAddRule,
  mockUpdateRule,
  mockDeleteRule,
  mockRoute
} = vi.hoisted(() => ({
  mockPageRuleList: vi.fn(),
  mockAddRule: vi.fn(),
  mockUpdateRule: vi.fn(),
  mockDeleteRule: vi.fn(),
  mockRoute: {
    query: {}
  }
}));

vi.mock('@/api/linkageRule', () => ({
  pageRuleList: mockPageRuleList,
  addRule: mockAddRule,
  updateRule: mockUpdateRule,
  deleteRule: mockDeleteRule
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute
}));

vi.mock('@/utils/confirm', () => ({
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
}));

vi.mock('@/utils/message', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn()
  }
}));

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
});

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="linkage-rule-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="filters" /></div>
      <div><slot name="applied-filters" /></div>
      <div><slot name="notices" /></div>
      <div><slot name="toolbar" /></div>
      <div><slot /></div>
      <div><slot name="pagination" /></div>
    </section>
  `
});

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function mountView() {
  return shallowMount(LinkageRuleView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        StandardTableToolbar: true,
        StandardPagination: true,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardFormDrawer: true,
        StandardTableTextColumn: true,
        StandardWorkbenchRowActions: true,
        StandardButton: true,
        EmptyState: true,
        ElAlert: true,
        ElForm: true,
        ElFormItem: true,
        ElInput: true,
        ElSelect: true,
        ElOption: true,
        ElRadioGroup: true,
        ElRadio: true,
        ElTag: true,
        ElTable: true,
        ElTableColumn: true
      }
    }
  });
}

describe('LinkageRuleView', () => {
  beforeEach(() => {
    mockPageRuleList.mockReset();
    mockAddRule.mockReset();
    mockUpdateRule.mockReset();
    mockDeleteRule.mockReset();
    mockRoute.query = {};
  });

  it('hydrates route query filters before loading linkage rules', async () => {
    mockRoute.query = {
      ruleName: '裂缝值联动',
      status: '0'
    };
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [{ id: 1, ruleName: '裂缝值联动', status: 0 }]
      }
    });

    mountView();
    await flushPromises();

    expect(mockPageRuleList).toHaveBeenCalledWith(expect.objectContaining({
      ruleName: '裂缝值联动',
      status: 0
    }));
  });
});
