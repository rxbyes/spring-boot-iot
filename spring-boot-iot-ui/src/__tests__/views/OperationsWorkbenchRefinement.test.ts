import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { defineComponent, ref } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import RealTimeMonitoringView from '@/views/RealTimeMonitoringView.vue';
import RiskGisView from '@/views/RiskGisView.vue';
import AlarmCenterView from '@/views/AlarmCenterView.vue';
import EventDisposalView from '@/views/EventDisposalView.vue';

const sourceRoot = resolve(import.meta.dirname, '../../..');

function readViewSource(fileName: string) {
  return readFileSync(resolve(sourceRoot, `src/views/${fileName}`), 'utf8');
}

function readWorkbenchOpenTag(fileName: string) {
  const source = readViewSource(fileName);
  const start = source.indexOf('<StandardWorkbenchPanel');
  const end = source.indexOf('>', start);
  return source.slice(start, end);
}

vi.mock('@/composables/useAutomationPlanBuilder', () => ({
  useAutomationPlanBuilder: () => ({
    scopeOptions: [],
    locatorTypeOptions: [],
    stepTypeOptions: [],
    inventoryTemplateOptions: [],
    plan: {
      target: { project: 'demo-project' },
      scenarios: []
    },
    inventoryTableRef: ref(null),
    showImportDialog: false,
    showManualPageDialog: false,
    scenarioPreviews: [],
    suggestions: [],
    pageInventory: [],
    inventorySourceText: '静态路由种子 + 自定义页面',
    planMetrics: [],
    inventoryMetrics: [],
    commandPreview: 'node scripts/auto/run-browser-acceptance.mjs --plan=demo.json',
    buildTemplateLabel: () => '页面冒烟',
    buildInventorySourceLabel: () => '静态种子',
    isRouteCovered: () => false,
    handleInventorySelectionChange: vi.fn(),
    refreshPageInventory: vi.fn(),
    selectUncoveredPages: vi.fn(),
    generateSelectedInventoryScenarios: vi.fn(),
    generateUncoveredInventoryScenarios: vi.fn(),
    openManualPageDialog: vi.fn(),
    saveManualPage: vi.fn(),
    removeManualPage: vi.fn(),
    addScenario: vi.fn(),
    copyScenario: vi.fn(),
    removeScenario: vi.fn(),
    moveScenario: vi.fn(),
    addInitialApi: vi.fn(),
    addStep: vi.fn(),
    addCapture: vi.fn(),
    handleStepTypeChange: vi.fn(),
    handleScreenshotTargetChange: vi.fn(),
    moveStep: vi.fn(),
    copyCommand: vi.fn(),
    downloadPlan: vi.fn(),
    resetPlan: vi.fn(),
    applyImport: vi.fn()
  })
}));

vi.mock('@/api/riskPoint', () => ({
  pageRiskPointList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  }),
  addRiskPoint: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  updateRiskPoint: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  deleteRiskPoint: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  bindDevice: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  getRiskPointList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
}));

vi.mock('@/api/iot', () => ({
  listDeviceOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getDeviceMetricOptions: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] })
}));

vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  }),
  getRiskMonitoringGisPoints: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
}));

vi.mock('@/api/alarm', () => ({
  getAlarmList: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getAlarmDetail: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  confirmAlarm: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  suppressAlarm: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  closeAlarm: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  getEventList: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: [] }),
  getEventDetail: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  dispatchEvent: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  closeEvent: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
}));

vi.mock('@/api/ruleDefinition', () => ({
  pageRuleList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  }),
  addRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  updateRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  deleteRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
}));

vi.mock('@/api/linkageRule', () => ({
  pageRuleList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  }),
  addRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  updateRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  deleteRule: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
}));

vi.mock('@/api/emergencyPlan', () => ({
  pagePlanList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 0,
      pageNum: 1,
      pageSize: 10,
      records: []
    }
  }),
  addPlan: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  updatePlan: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null }),
  deletePlan: vi.fn().mockResolvedValue({ code: 200, msg: 'success', data: null })
}));

vi.mock('@/utils/riskLevel', () => ({
  fetchRiskLevelOptions: vi.fn().mockResolvedValue([]),
  getRiskLevelTagType: vi.fn(() => 'info'),
  getRiskLevelText: vi.fn((value?: string) => value || '未标注'),
  normalizeRiskLevel: vi.fn((value?: string) => (value || '').trim().toLowerCase())
}));

vi.mock('@/utils/confirm', () => ({
  confirmAction: vi.fn(),
  confirmDelete: vi.fn(),
  isConfirmCancelled: vi.fn(() => false)
}));

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>();
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn()
    }
  };
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['title'],
  template: `
    <section class="panel-card-stub">
      <h3 v-if="title">{{ title }}</h3>
      <slot />
    </section>
  `
});

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="standard-workbench-panel-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="standard-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="standard-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="standard-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="standard-workbench-panel-stub__body"><slot /></div>
      <div class="standard-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
});

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['eyebrow', 'title', 'description', 'showTitle', 'showBreadcrumbs', 'breadcrumbs'],
  template: `
    <section class="standard-page-shell-stub">
      <p v-if="eyebrow" class="standard-page-shell-stub__eyebrow">{{ eyebrow }}</p>
      <h1 v-if="showTitle !== false && title" class="standard-page-shell-stub__title">{{ title }}</h1>
      <p v-if="description" class="standard-page-shell-stub__description">{{ description }}</p>
      <div class="standard-page-shell-stub__body"><slot /></div>
    </section>
  `
});

const StandardFormDrawerStub = defineComponent({
  name: 'StandardFormDrawer',
  props: ['eyebrow', 'title', 'subtitle'],
  template: `
    <section class="standard-form-drawer-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
      <slot />
      <slot name="footer" />
    </section>
  `
});

const StandardActionMenuStub = defineComponent({
  name: 'StandardActionMenu',
  props: ['label', 'items'],
  template: `
    <section class="standard-action-menu-stub">
      <span class="standard-action-menu-stub__label">{{ label || '更多' }}</span>
      <span class="standard-action-menu-stub__count">{{ (items || []).length }}</span>
    </section>
  `
});

const StandardWorkbenchRowActionsStub = defineComponent({
  name: 'StandardWorkbenchRowActions',
  props: ['variant', 'gap', 'directItems', 'menuItems'],
  template: `
    <section
      class="standard-workbench-row-actions-stub"
      :data-variant="variant"
      :data-gap="gap"
      :data-direct-count="(directItems || []).length"
      :data-menu-count="(menuItems || []).length"
    />
  `
});

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  emits: ['click'],
  template: '<button class="standard-button-stub" type="button" @click="$emit(\'click\')"><slot /></button>'
});

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  template: `
    <section class="standard-table-toolbar-stub">
      <div class="standard-table-toolbar-stub__default"><slot /></div>
      <div class="standard-table-toolbar-stub__right"><slot name="right" /></div>
    </section>
  `
});

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: ['data'],
  template: '<section class="el-table-stub"><slot /></section>'
});

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['prop', 'label', 'width', 'fixed', 'type', 'align', 'className', 'showOverflowTooltip'],
  template: `
    <section
      class="el-table-column-stub"
      :data-prop="prop || type || 'column'"
      :data-label="label"
      :data-width="width"
      :data-class-name="$attrs['class-name'] || className"
    >
      <slot :row="{}" />
      <slot name="default" :row="{}" />
    </section>
  `
});

function mountView(component: object) {
  return shallowMount(component, {
    global: {
      renderStubDefaultSlot: true,
      stubs: {
        StandardPageShell: StandardPageShellStub,
        PanelCard: PanelCardStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        MetricCard: true,
        StandardTableToolbar: StandardTableToolbarStub,
        StandardTableTextColumn: true,
        StandardPagination: true,
        StandardButton: StandardButtonStub,
        StandardActionMenu: StandardActionMenuStub,
        StandardWorkbenchRowActions: StandardWorkbenchRowActionsStub,
        'standard-workbench-row-actions': StandardWorkbenchRowActionsStub,
        StandardRowActions: true,
        StandardActionLink: true,
        StandardListFilterHeader: true,
        StandardAppliedFiltersBar: true,
        StandardFormDrawer: StandardFormDrawerStub,
        StandardDrawerFooter: true,
        CsvColumnSettingDialog: true,
        AlarmDetailDrawer: true,
        EventDetailDrawer: true,
        RiskMonitoringDetailDrawer: true,
        'el-form': true,
        'el-form-item': true,
        'el-row': true,
        'el-col': true,
        'el-select': true,
        'el-option': true,
        'el-input': true,
        'el-input-number': true,
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub,
        'el-tag': true,
        'el-alert': true
      }
    }
  });
}

describe('operations workbench refinement', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders realtime monitoring inside a single unified workbench', () => {
    const wrapper = mountView(RealTimeMonitoringView);
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-page-shell-stub')).toHaveLength(1);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('实时监测台');
    expect(wrapper.text()).not.toContain('RISK MONITORING');
  });

  it('renders GIS monitoring inside a single unified workbench', () => {
    const wrapper = mountView(RiskGisView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-page-shell-stub')).toHaveLength(1);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('GIS态势图');
  });

  it('removes the standalone hero card from the alarm workbench', () => {
    const wrapper = mountView(AlarmCenterView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-page-shell-stub')).toHaveLength(1);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('告警列表');
  });

  it('keeps the alarm workbench toolbar focused by collapsing secondary actions into a more-actions menu', () => {
    const wrapper = mountView(AlarmCenterView);
    const actionMenu = wrapper.findComponent(StandardActionMenuStub);

    expect(wrapper.text()).toContain('刷新列表');
    expect(actionMenu.exists()).toBe(true);
    expect(actionMenu.props('label')).toBe('更多操作');
    expect((actionMenu.props('items') as Array<unknown>)?.length ?? 0).toBe(4);
    expect(wrapper.text()).not.toContain('导出列设置');
    expect(wrapper.text()).not.toContain('导出选中');
    expect(wrapper.text()).not.toContain('导出当前结果');
    expect(wrapper.text()).not.toContain('清空选中');
  });

  it('aligns the alarm table action column with the product-definition spacing baseline', () => {
    const source = readFileSync(resolve(sourceRoot, 'src/views/AlarmCenterView.vue'), 'utf8');

    expect(source).toContain('<StandardWorkbenchRowActions');
    expect(source).toContain('class-name="standard-row-actions-column"');
    expect(source).toContain(':width="alarmActionColumnWidth"');
    expect(source).toContain('const alarmActionColumnWidth = computed(() =>');
    expect(source).toContain('resolveWorkbenchActionColumnWidthByRows({');
    expect(source).toContain("label: '确认'");
    expect(source).not.toContain("gap: 'wide'");
  });

  it('removes the standalone hero card from the event workbench', () => {
    const wrapper = mountView(EventDisposalView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-page-shell-stub')).toHaveLength(1);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('事件列表');
  });

  it('keeps event disposal form drawers calm without the legacy English eyebrow tier', () => {
    const wrapper = mountView(EventDisposalView);
    const formDrawers = wrapper.findAllComponents(StandardFormDrawerStub);

    expect(formDrawers).toHaveLength(2);
    expect(formDrawers.every((item) => item.props('eyebrow') === undefined)).toBe(true);
    expect(wrapper.text()).toContain('工单派发');
    expect(wrapper.text()).toContain('事件关闭');
    expect(wrapper.text()).not.toContain('Event Workflow');
  });

  it('keeps the event disposal toolbar focused by collapsing secondary actions into a more-actions menu', () => {
    const wrapper = mountView(EventDisposalView);
    const actionMenu = wrapper.findComponent(StandardActionMenuStub);

    expect(wrapper.text()).toContain('刷新列表');
    expect(actionMenu.exists()).toBe(true);
    expect(actionMenu.props('label')).toBe('更多操作');
    expect((actionMenu.props('items') as Array<unknown>)?.length ?? 0).toBe(4);
    expect(wrapper.text()).not.toContain('导出列设置');
    expect(wrapper.text()).not.toContain('导出选中');
    expect(wrapper.text()).not.toContain('导出当前结果');
    expect(wrapper.text()).not.toContain('清空选中');
  });

  it('removes the standalone hero card from the risk point workbench', () => {
    const source = readViewSource('RiskPointView.vue');
    const workbenchOpenTag = readWorkbenchOpenTag('RiskPointView.vue');

    expect(source).toContain('<StandardPageShell');
    expect(source).toContain('<StandardWorkbenchPanel');
    expect(workbenchOpenTag).not.toContain('eyebrow');
    expect(source).toContain('title="风险对象中心"');
    expect(source).not.toContain('Risk Point Workspace');
  });

  it('removes the standalone hero card from the threshold rule workbench', () => {
    const source = readViewSource('RuleDefinitionView.vue');
    const workbenchOpenTag = readWorkbenchOpenTag('RuleDefinitionView.vue');

    expect(source).toContain('<StandardPageShell');
    expect(source).toContain('<StandardWorkbenchPanel');
    expect(workbenchOpenTag).not.toContain('eyebrow');
    expect(source).toContain('title="阈值策略"');
    expect(source).not.toContain('Threshold Rules');
  });

  it('removes the standalone hero card from the linkage workbench', () => {
    const source = readViewSource('LinkageRuleView.vue');
    const workbenchOpenTag = readWorkbenchOpenTag('LinkageRuleView.vue');

    expect(source).toContain('<StandardPageShell');
    expect(source).toContain('<StandardWorkbenchPanel');
    expect(workbenchOpenTag).not.toContain('eyebrow');
    expect(source).toContain('title="联动编排"');
    expect(source).not.toContain('Linkage Workflow');
  });

  it('removes the standalone hero card from the emergency plan workbench', () => {
    const source = readViewSource('EmergencyPlanView.vue');
    const workbenchOpenTag = readWorkbenchOpenTag('EmergencyPlanView.vue');

    expect(source).toContain('<StandardPageShell');
    expect(source).toContain('<StandardWorkbenchPanel');
    expect(workbenchOpenTag).not.toContain('eyebrow');
    expect(source).toContain('title="应急预案库"');
    expect(source).not.toContain('Emergency Plans');
  });

  it('removes the standalone hero panel from the automation workbench and aligns it with the shared governance shell', () => {
    const entrySource = readViewSource('AutomationTestCenterView.vue');
    const landingSource = readViewSource('RdWorkbenchLandingView.vue');

    expect(entrySource).toContain('<RdWorkbenchLandingView />');
    expect(landingSource).toContain('<StandardPageShell');
    expect(landingSource).toContain('<StandardWorkbenchPanel');
    expect(landingSource).toContain('title="研发工场总览"');
  });

  it('aligns audit-log action columns with adaptive shared row actions', () => {
    const source = readFileSync(resolve(sourceRoot, 'src/views/AuditLogView.vue'), 'utf8');

    expect(source).toContain('<StandardWorkbenchRowActions');
    expect(source).toContain('class-name="standard-row-actions-column"');
    expect(source).toContain('auditActionColumnWidth');
  });

  it('keeps governed list row actions on the shared workbench contract', () => {
    const governedSources = [
      'src/views/RuleDefinitionView.vue',
      'src/views/LinkageRuleView.vue',
      'src/views/EmergencyPlanView.vue',
      'src/views/RoleView.vue',
      'src/views/DictView.vue',
      'src/views/InAppMessageView.vue'
    ];

    governedSources.forEach((relativePath) => {
      const source = readFileSync(resolve(sourceRoot, relativePath), 'utf8');

      expect(source).not.toContain('<StandardRowActions variant="table"');
      expect(source).not.toMatch(/label="操作"[\s\S]{0,160}\swidth="/);
    });
  });
});
