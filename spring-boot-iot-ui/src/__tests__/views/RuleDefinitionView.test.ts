import { defineComponent, nextTick } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import RuleDefinitionView from '@/views/RuleDefinitionView.vue';

const {
  mockPageRuleList,
  mockAddRule,
  mockUpdateRule,
  mockDeleteRule,
  mockListMissingPolicies,
  mockFetchAlarmLevelOptions,
  mockRoute
} = vi.hoisted(() => ({
  mockPageRuleList: vi.fn(),
  mockAddRule: vi.fn(),
  mockUpdateRule: vi.fn(),
  mockDeleteRule: vi.fn(),
  mockListMissingPolicies: vi.fn(),
  mockFetchAlarmLevelOptions: vi.fn(),
  mockRoute: {
    query: {}
  }
}));

vi.mock('@/api/ruleDefinition', () => ({
  pageRuleList: mockPageRuleList,
  addRule: mockAddRule,
  updateRule: mockUpdateRule,
  deleteRule: mockDeleteRule
}));

vi.mock('@/api/riskGovernance', () => ({
  listMissingPolicies: mockListMissingPolicies
}));

vi.mock('@/utils/alarmLevel', async () => {
  const actual = await vi.importActual<typeof import('@/utils/alarmLevel')>('@/utils/alarmLevel');
  return {
    ...actual,
    fetchAlarmLevelOptions: mockFetchAlarmLevelOptions
  };
});

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

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute
}));

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  template: '<section class="standard-page-shell-stub"><slot /></section>'
});

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['title', 'description'],
  template: `
    <section class="rule-definition-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="rule-definition-workbench-panel-stub__header-actions"><slot name="header-actions" /></div>
      <div class="rule-definition-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="rule-definition-workbench-panel-stub__applied"><slot name="applied-filters" /></div>
      <div class="rule-definition-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="rule-definition-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="rule-definition-workbench-panel-stub__body"><slot /></div>
      <div class="rule-definition-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
});

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="rule-definition-list-filter-header-stub">
      <div class="rule-definition-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="rule-definition-list-filter-header-stub__actions"><slot name="actions" /></div>
    </section>
  `
});

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="rule-definition-table-toolbar-stub">
      <div class="rule-definition-table-toolbar-stub__meta">{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
});

const StandardPaginationStub = defineComponent({
  name: 'StandardPagination',
  props: ['currentPage', 'pageSize', 'total', 'pageSizes', 'layout'],
  template: `
    <section
      class="rule-definition-pagination-stub"
      :data-total="total"
      :data-layout="layout"
      :data-page-sizes="JSON.stringify(pageSizes || [])"
    />
  `
});

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['modelValue', 'title', 'subtitle'],
  template: `
    <section class="standard-form-drawer-stub" :data-model-value="modelValue">
      <header class="standard-form-drawer-stub__header">
        <h3>{{ title }}</h3>
        <p>{{ subtitle }}</p>
      </header>
      <slot />
    </section>
  `
});

const ElAlertStub = defineComponent({
  name: 'ElAlert',
  props: ['title', 'type'],
  template: `
    <section class="el-alert-stub" :data-type="type">
      <strong>{{ title }}</strong>
      <slot />
    </section>
  `
});

const ElFormStub = defineComponent({
  name: 'ElForm',
  template: '<form class="el-form-stub"><slot /></form>',
  methods: {
    async validate() {
      return true;
    },
    clearValidate() {
      return undefined;
    }
  }
});

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function createRuleRow() {
  return {
    id: 1,
    riskMetricId: 6102,
    ruleName: '北坡位移红色阈值',
    metricIdentifier: 'displacementX',
    metricName: '位移 X',
    expression: 'value >= 12',
    duration: 120,
    alarmLevel: 'critical',
    notificationMethods: 'email,sms',
    convertToEvent: 1,
    status: 0,
    remark: 'desc'
  };
}

function mountView() {
  return shallowMount(RuleDefinitionView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardPagination: StandardPaginationStub,
        StandardAppliedFiltersBar: true,
        StandardDrawerFooter: true,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardTableTextColumn: true,
        StandardWorkbenchRowActions: true,
        StandardButton: true,
        EmptyState: true,
        ElAlert: ElAlertStub,
        ElForm: ElFormStub,
        ElFormItem: true,
        ElInput: true,
        ElInputNumber: true,
        ElSelect: true,
        ElOption: true,
        ElCheckboxGroup: true,
        ElCheckbox: true,
        ElRadioGroup: true,
        ElRadio: true,
        ElTag: true,
        ElTable: true,
        ElTableColumn: true
      }
    }
  });
}

describe('RuleDefinitionView', () => {
  beforeEach(() => {
    mockPageRuleList.mockReset();
    mockAddRule.mockReset();
    mockUpdateRule.mockReset();
    mockDeleteRule.mockReset();
    mockListMissingPolicies.mockReset();
    mockFetchAlarmLevelOptions.mockReset();
    mockRoute.query = {};
    mockFetchAlarmLevelOptions.mockResolvedValue([
      { label: '红色', value: 'red', sortNo: 1 },
      { label: '橙色', value: 'orange', sortNo: 2 },
      { label: '黄色', value: 'yellow', sortNo: 3 },
      { label: '蓝色', value: 'blue', sortNo: 4 }
    ]);
    mockListMissingPolicies.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 3,
        records: [
          {
            issueType: 'MISSING_POLICY',
            issueLabel: '待配置阈值策略',
            deviceCode: 'DEVICE-001',
            deviceName: '一号设备',
            riskPointName: '一号风险点',
            metricIdentifier: 'displacementX',
            metricName: '位移 X'
          }
        ]
      }
    });
  });

  it('shows policy governance backlog in notices after loading rules', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 24,
        pageNum: 1,
        pageSize: 10,
        records: [createRuleRow()]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain('待配置阈值策略');
    expect(wrapper.text()).toContain('位移 X');
    expect(wrapper.text()).toContain('一号风险点');
    expect(mockListMissingPolicies).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 3
    });
  });

  it('uses the standard pagination sizes and full layout after loading data', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 24,
        pageNum: 1,
        pageSize: 10,
        records: [createRuleRow()]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    const pagination = wrapper.findComponent(StandardPaginationStub);
    expect(pagination.props('pageSizes')).toEqual([10, 20, 50, 100]);
    expect(pagination.props('layout')).toBe('total, sizes, prev, pager, next, jumper');
  });

  it('counts mixed legacy and new values under the four-color alarm semantics', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 2,
        pageNum: 1,
        pageSize: 10,
        records: [
          createRuleRow(),
          {
            ...createRuleRow(),
            id: 2,
            ruleName: '桥梁红色新口径',
            alarmLevel: 'red'
          }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain('红色 2 项');
  });

  it('preserves riskMetricId when editing and submitting an existing rule', async () => {
    const pageResponse = {
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRuleRow()]
      }
    };
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockUpdateRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRuleRow()
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleEdit(createRuleRow());
    await nextTick();
    await (wrapper.vm as any).handleSubmit();

    expect(mockUpdateRule).toHaveBeenCalledWith(expect.objectContaining({
      id: 1,
      riskMetricId: 6102,
      metricIdentifier: 'displacementX'
    }));
  });

  it('hydrates route query filters before loading threshold strategies', async () => {
    mockRoute.query = {
      ruleName: '裂缝值红色阈值',
      metricIdentifier: 'value',
      alarmLevel: 'red',
      status: '0'
    };
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [createRuleRow()]
      }
    });

    mountView();
    await flushPromises();

    expect(mockPageRuleList).toHaveBeenCalledWith(expect.objectContaining({
      ruleName: '裂缝值红色阈值',
      metricIdentifier: 'value',
      alarmLevel: 'red',
      status: 0
    }));
  });
});
