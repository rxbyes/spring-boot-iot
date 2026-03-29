import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import RealTimeMonitoringView from '@/views/RealTimeMonitoringView.vue';
import RiskGisView from '@/views/RiskGisView.vue';
import AlarmCenterView from '@/views/AlarmCenterView.vue';
import EventDisposalView from '@/views/EventDisposalView.vue';
import RiskPointView from '@/views/RiskPointView.vue';
import RuleDefinitionView from '@/views/RuleDefinitionView.vue';
import LinkageRuleView from '@/views/LinkageRuleView.vue';
import EmergencyPlanView from '@/views/EmergencyPlanView.vue';

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

function mountView(component: object) {
  return shallowMount(component, {
    global: {
      renderStubDefaultSlot: true,
      stubs: {
        PanelCard: PanelCardStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        MetricCard: true,
        StandardTableToolbar: true,
        StandardTableTextColumn: true,
        StandardPagination: true,
        StandardButton: true,
        StandardRowActions: true,
        StandardActionLink: true,
        StandardListFilterHeader: true,
        StandardAppliedFiltersBar: true,
        StandardFormDrawer: true,
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
        'el-table': true,
        'el-table-column': true,
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
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('实时监测台');
    expect(wrapper.text()).not.toContain('RISK MONITORING');
  });

  it('renders GIS monitoring inside a single unified workbench', () => {
    const wrapper = mountView(RiskGisView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('GIS态势图');
  });

  it('removes the standalone hero card from the alarm workbench', () => {
    const wrapper = mountView(AlarmCenterView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('告警列表');
  });

  it('removes the standalone hero card from the event workbench', () => {
    const wrapper = mountView(EventDisposalView);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('事件列表');
  });

  it('removes the standalone hero card from the risk point workbench', () => {
    const wrapper = mountView(RiskPointView);
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('风险对象中心');
    expect(wrapper.text()).not.toContain('Risk Point Workspace');
  });

  it('removes the standalone hero card from the threshold rule workbench', () => {
    const wrapper = mountView(RuleDefinitionView);
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('阈值策略');
    expect(wrapper.text()).not.toContain('Threshold Rules');
  });

  it('removes the standalone hero card from the linkage workbench', () => {
    const wrapper = mountView(LinkageRuleView);
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('联动编排');
    expect(wrapper.text()).not.toContain('Linkage Workflow');
  });

  it('removes the standalone hero card from the emergency plan workbench', () => {
    const wrapper = mountView(EmergencyPlanView);
    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.findAll('.panel-card-stub')).toHaveLength(0);
    expect(wrapper.findAll('.standard-workbench-panel-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(wrapper.text()).toContain('应急预案库');
    expect(wrapper.text()).not.toContain('Emergency Plans');
  });
});
