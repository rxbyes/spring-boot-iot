import { defineComponent, nextTick } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import RuleDefinitionView from '@/views/RuleDefinitionView.vue';

const {
  mockPageRuleList,
  mockAddRule,
  mockAddRuleBatch,
  mockUpdateRule,
  mockDeleteRule,
  mockPreviewEffectiveRule,
  mockListMissingPolicies,
  mockPageMissingPolicyProductMetricSummaries,
  mockGetAllProducts,
  mockFetchAlarmLevelOptions,
  mockRoute
} = vi.hoisted(() => ({
  mockPageRuleList: vi.fn(),
  mockAddRule: vi.fn(),
  mockAddRuleBatch: vi.fn(),
  mockUpdateRule: vi.fn(),
  mockDeleteRule: vi.fn(),
  mockPreviewEffectiveRule: vi.fn(),
  mockListMissingPolicies: vi.fn(),
  mockPageMissingPolicyProductMetricSummaries: vi.fn(),
  mockGetAllProducts: vi.fn(),
  mockFetchAlarmLevelOptions: vi.fn(),
  mockRoute: {
    query: {}
  }
}));

vi.mock('@/api/ruleDefinition', () => ({
  pageRuleList: mockPageRuleList,
  addRule: mockAddRule,
  addRuleBatch: mockAddRuleBatch,
  updateRule: mockUpdateRule,
  deleteRule: mockDeleteRule,
  previewEffectiveRule: mockPreviewEffectiveRule
}));

vi.mock('@/api/riskGovernance', () => ({
  listMissingPolicies: mockListMissingPolicies,
  pageMissingPolicyProductMetricSummaries: mockPageMissingPolicyProductMetricSummaries
}));

vi.mock('@/api/product', () => ({
  productApi: {
    getAllProducts: mockGetAllProducts
  }
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
    ruleScope: 'METRIC',
    productType: null,
    productId: null,
    deviceId: null,
    riskPointDeviceId: null,
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

function createProductMetricSummary(overrides: Record<string, unknown> = {}) {
  return {
    productId: 1001,
    productKey: 'nf-monitor-crack-v1',
    productName: 'Crack Product',
    riskMetricId: 6102,
    metricIdentifier: 'displacementX',
    metricName: 'Displacement X',
    bindingCount: 12,
    riskPointCount: 6,
    deviceCount: 8,
    ...overrides
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
    mockAddRuleBatch.mockReset();
    mockUpdateRule.mockReset();
    mockDeleteRule.mockReset();
    mockPreviewEffectiveRule.mockReset();
    mockListMissingPolicies.mockReset();
    mockPageMissingPolicyProductMetricSummaries.mockReset();
    mockGetAllProducts.mockReset();
    mockFetchAlarmLevelOptions.mockReset();
    mockRoute.query = {};
    mockFetchAlarmLevelOptions.mockResolvedValue([
      { label: '红色', value: 'red', sortNo: 1 },
      { label: '橙色', value: 'orange', sortNo: 2 },
      { label: '黄色', value: 'yellow', sortNo: 3 },
      { label: '蓝色', value: 'blue', sortNo: 4 }
    ]);
    mockPreviewEffectiveRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        hasMatchedRule: true,
        metricIdentifier: 'displacementX',
        productId: 1001,
        matchedScope: 'PRODUCT',
        matchedScopeText: '产品默认',
        decision: '最终生效策略：产品默认阈值',
        matchedRule: createRuleRow(),
        candidates: []
      }
    });
    mockGetAllProducts.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1001,
          productKey: 'nf-monitor-crack-v1',
          productName: '裂缝监测仪',
          protocolCode: 'mqtt-json',
          nodeType: 1
        }
      ]
    });
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
            productId: 1001,
            productKey: 'nf-monitor-crack-v1',
            productName: '裂缝监测仪',
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
    mockPageMissingPolicyProductMetricSummaries.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 5,
        records: [createProductMetricSummary()]
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

  it('preserves long product ids when editing a product default threshold strategy', async () => {
    const longProductId = '202603192100560260';
    const productDefaultRule = {
      ...createRuleRow(),
      id: 8250,
      ruleScope: 'PRODUCT',
      productType: 'MONITORING',
      productId: longProductId,
      metricIdentifier: 'value',
      metricName: '当前雨量',
      expression: 'value >= 1'
    };
    const pageResponse = {
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [productDefaultRule]
      }
    };
    mockGetAllProducts.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: longProductId,
          productKey: 'nf-monitor-tipping-bucket-rain-gauge-v1',
          productName: '翻斗式雨量计',
          protocolCode: 'mqtt-json',
          nodeType: 1
        }
      ]
    });
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockUpdateRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: productDefaultRule
    });

    const wrapper = mountView();
    await flushPromises();

    expect((wrapper.vm as any).getRuleScopeTargetText(productDefaultRule)).toBe('翻斗式雨量计');

    (wrapper.vm as any).handleEdit(productDefaultRule);
    await nextTick();
    (wrapper.vm as any).form.expression = 'value >= 2';
    await (wrapper.vm as any).handleSubmit();

    expect(mockUpdateRule).toHaveBeenCalledWith(expect.objectContaining({
      id: 8250,
      ruleScope: 'PRODUCT',
      productId: longProductId,
      metricIdentifier: 'value',
      expression: 'value >= 2'
    }));
  });

  it('preserves long risk metric ids when editing a threshold strategy', async () => {
    const longRiskMetricId = '202604280000000001';
    const rule = {
      ...createRuleRow(),
      id: '202604280000000101',
      riskMetricId: longRiskMetricId,
      ruleScope: 'PRODUCT',
      productId: '202603192100560260',
      metricIdentifier: 'value',
      metricName: '当前雨量',
      expression: 'value >= 1'
    };
    const pageResponse = {
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [rule]
      }
    };
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockPageRuleList.mockResolvedValueOnce(pageResponse);
    mockUpdateRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: rule
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleEdit(rule);
    await nextTick();
    await (wrapper.vm as any).handleSubmit();

    expect(mockUpdateRule).toHaveBeenCalledWith(expect.objectContaining({
      id: '202604280000000101',
      riskMetricId: longRiskMetricId,
      productId: '202603192100560260'
    }));
  });

  it('previews the effective threshold strategy with preserved scope identities', async () => {
    const rule = {
      ...createRuleRow(),
      id: '202604280000000101',
      tenantId: '1',
      riskMetricId: '202604280000000001',
      ruleScope: 'BINDING',
      productId: '202603192100560260',
      deviceId: '202603192100560261',
      riskPointDeviceId: '202603192100560262',
      metricIdentifier: 'value',
      metricName: '当前雨量'
    };
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [rule]
      }
    });
    mockPreviewEffectiveRule.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        hasMatchedRule: true,
        metricIdentifier: 'value',
        productId: '202603192100560260',
        deviceId: '202603192100560261',
        riskPointDeviceId: '202603192100560262',
        decision: '最终生效策略：绑定个性阈值',
        matchedRule: rule,
        candidates: [
          {
            ruleId: '202604280000000101',
            ruleName: '绑定个性阈值',
            ruleScope: 'BINDING',
            ruleScopeText: '绑定个性',
            scopeTarget: '绑定 202603192100560262',
            expression: 'value >= 1',
            matchedContext: true,
            selected: true
          }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    await (wrapper.vm as any).handlePreviewEffectiveRule(rule);
    await flushPromises();

    expect(mockPreviewEffectiveRule).toHaveBeenCalledWith({
      tenantId: '1',
      riskMetricId: '202604280000000001',
      metricIdentifier: 'value',
      productId: '202603192100560260',
      productType: undefined,
      deviceId: '202603192100560261',
      riskPointDeviceId: '202603192100560262'
    });
    expect((wrapper.vm as any).effectivePreviewVisible).toBe(true);
    expect(wrapper.text()).toContain('最终生效策略：绑定个性阈值');
    expect(wrapper.text()).toContain('绑定个性阈值');
  });

  it('hydrates route query filters before loading threshold strategies', async () => {
    mockRoute.query = {
      ruleName: '裂缝值红色阈值',
      metricIdentifier: 'value',
      scopeView: 'BUSINESS',
      ruleScope: 'PRODUCT',
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
    mockPageMissingPolicyProductMetricSummaries.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 5,
        records: [
          {
            productId: 1001,
            productKey: 'nf-monitor-crack-v1',
            productName: '裂缝监测仪',
            riskMetricId: 6102,
            metricIdentifier: 'displacementX',
            metricName: '位移 X',
            bindingCount: 12,
            riskPointCount: 6,
            deviceCount: 8
          }
        ]
      }
    });

    mountView();
    await flushPromises();

    expect(mockPageRuleList).toHaveBeenCalledWith(expect.objectContaining({
      ruleName: '裂缝值红色阈值',
      metricIdentifier: 'value',
      scopeView: 'BUSINESS',
      ruleScope: 'PRODUCT',
      alarmLevel: 'red',
      status: 0
    }));
  });

  it('uses business threshold scopes by default and opens product default create form', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect(mockPageRuleList).toHaveBeenCalledWith(expect.objectContaining({
      scopeView: 'BUSINESS',
      ruleScope: undefined
    }));
    expect((wrapper.vm as any).activeFilterTags.some((tag: any) => tag.key === 'scopeView')).toBe(false);

    (wrapper.vm as any).handleAdd();
    await nextTick();

    expect((wrapper.vm as any).form.ruleScope).toBe('PRODUCT');
    expect((wrapper.vm as any).formRuleScopeOptions.map((option: any) => option.value)).toEqual([
      'PRODUCT',
      'DEVICE',
      'BINDING'
    ]);
  });

  it('clears incompatible scope when switching to system template view', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).filters.ruleScope = 'PRODUCT';
    (wrapper.vm as any).filters.scopeView = 'SYSTEM';
    (wrapper.vm as any).handleScopeViewChange();

    expect((wrapper.vm as any).filters.ruleScope).toBe('');
    expect((wrapper.vm as any).currentRuleScopeFilterOptions.map((option: any) => option.value)).toEqual([
      'METRIC',
      'PRODUCT_TYPE'
    ]);
  });

  it('removes system strategy view from applied filters back to the default business view', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).filters.scopeView = 'SYSTEM';
    (wrapper.vm as any).handleSearch();
    await flushPromises();

    expect((wrapper.vm as any).activeFilterTags.some((tag: any) => tag.key === 'scopeView')).toBe(true);

    (wrapper.vm as any).handleRemoveAppliedFilter('scopeView');
    await flushPromises();

    expect((wrapper.vm as any).filters.scopeView).toBe('BUSINESS');
    expect((wrapper.vm as any).activeFilterTags.some((tag: any) => tag.key === 'scopeView')).toBe(false);
    expect(mockPageRuleList).toHaveBeenLastCalledWith(expect.objectContaining({
      scopeView: 'BUSINESS'
    }));
  });

  it('filters product default threshold strategies from the governance shortcut', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleProductDefaultFilter();
    await flushPromises();

    expect(mockPageRuleList).toHaveBeenLastCalledWith(expect.objectContaining({
      ruleScope: 'PRODUCT'
    }));
  });

  it('opens product default create drawer from missing policy backlog', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleProductDefaultAdd();
    await nextTick();

    expect((wrapper.vm as any).formVisible).toBe(true);
    expect((wrapper.vm as any).form.ruleScope).toBe('PRODUCT');
    expect((wrapper.vm as any).form.productId).toBe(1001);
    expect((wrapper.vm as any).form.metricIdentifier).toBe('displacementX');
    expect((wrapper.vm as any).form.metricName).toBe('Displacement X');
  });

  it('generates product default threshold drafts from selected product metric summaries', async () => {
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleSelectVisibleMissingPolicySummaries();
    expect((wrapper.vm as any).selectedMissingPolicySummaryKeys).toHaveLength(1);

    (wrapper.vm as any).handleGenerateSelectedDefaultDrafts();
    await nextTick();

    expect((wrapper.vm as any).productDefaultDrafts).toHaveLength(1);
    expect((wrapper.vm as any).selectedMissingPolicySummaryKeys).toHaveLength(0);
    expect((wrapper.vm as any).productDefaultDrafts[0].metricIdentifier).toBe('displacementX');
  });

  it('removes an active product default draft after the strategy is submitted', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });
    mockAddRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRuleRow()
    });

    const wrapper = mountView();
    await flushPromises();

    const summary = createProductMetricSummary();
    (wrapper.vm as any).handleAddSingleProductDefaultDraft(summary);
    (wrapper.vm as any).handleProductDefaultAdd(summary);
    (wrapper.vm as any).form.expression = 'value >= 10';
    await (wrapper.vm as any).handleSubmit();

    expect(mockAddRule).toHaveBeenCalledWith(expect.objectContaining({
      ruleScope: 'PRODUCT',
      productId: 1001,
      metricIdentifier: 'displacementX'
    }));
    expect((wrapper.vm as any).productDefaultDrafts).toHaveLength(0);
  });

  it('batch submits completed product default drafts and keeps incomplete drafts', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });
    mockAddRuleBatch.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 1,
        successCount: 1,
        failedCount: 0,
        items: [
          {
            index: 0,
            success: true,
            ruleId: 1,
            ruleName: 'Displacement X 产品默认阈值',
            metricIdentifier: 'displacementX',
            message: 'OK'
          }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).addProductDefaultDrafts([
      createProductMetricSummary(),
      createProductMetricSummary({
        riskMetricId: 6103,
        metricIdentifier: 'displacementY',
        metricName: 'Displacement Y'
      })
    ]);
    (wrapper.vm as any).productDefaultDrafts[0].expression = 'value >= 10';

    await (wrapper.vm as any).handleSubmitProductDefaultDrafts();

    expect(mockAddRuleBatch).toHaveBeenCalledTimes(1);
    expect(mockAddRuleBatch).toHaveBeenCalledWith([
      expect.objectContaining({
        ruleScope: 'PRODUCT',
        productId: 1001,
        metricIdentifier: 'displacementX',
        expression: 'value >= 10'
      })
    ]);
    expect((wrapper.vm as any).productDefaultDrafts).toHaveLength(1);
    expect((wrapper.vm as any).productDefaultDrafts[0].metricIdentifier).toBe('displacementY');
  });

  it('marks failed product default drafts with backend messages after batch submit', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });
    mockAddRuleBatch.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        totalCount: 2,
        successCount: 1,
        failedCount: 1,
        items: [
          {
            index: 0,
            success: true,
            ruleId: 1,
            ruleName: 'Displacement X 产品默认阈值',
            metricIdentifier: 'displacementX',
            message: 'OK'
          },
          {
            index: 1,
            success: false,
            ruleName: 'Displacement Y 产品默认阈值',
            metricIdentifier: 'displacementY',
            message: '表达式格式无效'
          }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).addProductDefaultDrafts([
      createProductMetricSummary(),
      createProductMetricSummary({
        riskMetricId: 6103,
        metricIdentifier: 'displacementY',
        metricName: 'Displacement Y'
      })
    ]);
    (wrapper.vm as any).productDefaultDrafts[0].expression = 'value >= 10';
    (wrapper.vm as any).productDefaultDrafts[1].expression = 'value >< 20';

    await (wrapper.vm as any).handleSubmitProductDefaultDrafts();

    expect((wrapper.vm as any).productDefaultDrafts).toHaveLength(1);
    expect((wrapper.vm as any).productDefaultDrafts[0].metricIdentifier).toBe('displacementY');
    expect((wrapper.vm as any).productDefaultDrafts[0].submitStatus).toBe('FAILED');
    expect((wrapper.vm as any).productDefaultDrafts[0].submitMessage).toBe('表达式格式无效');

    (wrapper.vm as any).clearProductDefaultDraftSubmitState((wrapper.vm as any).productDefaultDrafts[0]);

    expect((wrapper.vm as any).productDefaultDrafts[0].submitStatus).toBe('IDLE');
    expect((wrapper.vm as any).productDefaultDrafts[0].submitMessage).toBe('');
  });

  it('applies draft template to empty expressions and can overwrite all drafts', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).addProductDefaultDrafts([
      createProductMetricSummary(),
      createProductMetricSummary({
        riskMetricId: 6103,
        metricIdentifier: 'displacementY',
        metricName: 'Displacement Y'
      })
    ]);
    (wrapper.vm as any).productDefaultDrafts[0].expression = 'value >= 8';
    (wrapper.vm as any).productDefaultDraftTemplate.expression = 'value >= 12';
    (wrapper.vm as any).productDefaultDraftTemplate.duration = 60;
    (wrapper.vm as any).productDefaultDraftTemplate.alarmLevel = 'red';
    (wrapper.vm as any).productDefaultDraftTemplate.convertToEvent = 1;

    (wrapper.vm as any).handleApplyProductDefaultDraftTemplate();

    expect((wrapper.vm as any).productDefaultDrafts[0].expression).toBe('value >= 8');
    expect((wrapper.vm as any).productDefaultDrafts[1].expression).toBe('value >= 12');
    expect((wrapper.vm as any).productDefaultDrafts[1].duration).toBe(60);
    expect((wrapper.vm as any).productDefaultDrafts[1].alarmLevel).toBe('red');
    expect((wrapper.vm as any).productDefaultDrafts[1].convertToEvent).toBe(1);

    (wrapper.vm as any).productDefaultDraftTemplate.applyMode = 'ALL';
    (wrapper.vm as any).productDefaultDraftTemplate.expression = 'value >= 20';
    (wrapper.vm as any).handleApplyProductDefaultDraftTemplate();

    expect((wrapper.vm as any).productDefaultDrafts[0].expression).toBe('value >= 20');
    expect((wrapper.vm as any).productDefaultDrafts[1].expression).toBe('value >= 20');
  });

  it('submits product default threshold strategy with product scope identity', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });
    mockAddRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRuleRow()
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleAdd();
    (wrapper.vm as any).form.ruleScope = 'PRODUCT';
    (wrapper.vm as any).form.productId = 1001;
    (wrapper.vm as any).form.ruleName = '裂缝产品默认阈值';
    (wrapper.vm as any).form.metricIdentifier = 'value';
    (wrapper.vm as any).form.metricName = '裂缝值';
    (wrapper.vm as any).form.expression = 'value >= 10';
    await (wrapper.vm as any).handleSubmit();

    expect(mockAddRule).toHaveBeenCalledWith(expect.objectContaining({
      ruleScope: 'PRODUCT',
      productType: undefined,
      productId: 1001,
      metricIdentifier: 'value'
    }));
  });

  it('submits monitoring product type threshold template without product identity', async () => {
    mockPageRuleList.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });
    mockAddRule.mockResolvedValue({
      code: 200,
      msg: 'success',
      data: createRuleRow()
    });

    const wrapper = mountView();
    await flushPromises();

    (wrapper.vm as any).handleProductTypeTemplateAdd('MONITORING');
    (wrapper.vm as any).form.metricIdentifier = 'value';
    (wrapper.vm as any).form.metricName = 'Crack Value';
    (wrapper.vm as any).form.expression = 'value >= 10';
    await (wrapper.vm as any).handleSubmit();

    expect(mockAddRule).toHaveBeenCalledWith(expect.objectContaining({
      ruleScope: 'PRODUCT_TYPE',
      productType: 'MONITORING',
      productId: undefined,
      metricIdentifier: 'value'
    }));
  });

  it('auto-opens create drawer from governance-task dispatch context and prefills metric fields', async () => {
    mockRoute.query = {
      governanceAction: 'create',
      governanceSource: 'task',
      workItemCode: 'PENDING_THRESHOLD_POLICY',
      riskMetricId: '6102',
      metricIdentifier: 'displacementX',
      metricName: '位移 X'
    };
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect((wrapper.vm as any).formVisible).toBe(true);
    expect((wrapper.vm as any).form.riskMetricId).toBe('6102');
    expect((wrapper.vm as any).form.metricIdentifier).toBe('displacementX');
    expect((wrapper.vm as any).form.metricName).toBe('位移 X');
    expect(wrapper.find('.standard-form-drawer-stub').attributes('data-model-value')).toBe('true');
  });

  it('shows collector-child governance note when threshold drawer is opened for child-owned metric context', async () => {
    mockRoute.query = {
      governanceAction: 'create',
      governanceSource: 'task',
      workItemCode: 'PENDING_THRESHOLD_POLICY',
      riskMetricId: '6102',
      metricIdentifier: 'dispsX',
      metricName: 'X轴位移',
      governanceBoundary: 'collector-child',
      subjectOwnership: 'child'
    };
    mockPageRuleList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain('当前规则针对子设备正式测点');
    expect(wrapper.text()).toContain('采集器仅承担状态采集');
  });
});
