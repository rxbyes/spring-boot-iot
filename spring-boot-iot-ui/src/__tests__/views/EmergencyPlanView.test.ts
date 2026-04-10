import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import EmergencyPlanView from '@/views/EmergencyPlanView.vue';

const {
  mockPagePlanList,
  mockAddPlan,
  mockUpdatePlan,
  mockDeletePlan,
  mockFetchRiskLevelOptions,
  mockFetchAlarmLevelOptions,
  mockRoute
} = vi.hoisted(() => ({
  mockPagePlanList: vi.fn(),
  mockAddPlan: vi.fn(),
  mockUpdatePlan: vi.fn(),
  mockDeletePlan: vi.fn(),
  mockFetchRiskLevelOptions: vi.fn(),
  mockFetchAlarmLevelOptions: vi.fn(),
  mockRoute: {
    query: {}
  }
}));

vi.mock('@/api/emergencyPlan', () => ({
  pagePlanList: mockPagePlanList,
  addPlan: mockAddPlan,
  updatePlan: mockUpdatePlan,
  deletePlan: mockDeletePlan
}));

vi.mock('@/utils/riskLevel', async () => {
  const actual = await vi.importActual<typeof import('@/utils/riskLevel')>('@/utils/riskLevel');
  return {
    ...actual,
    fetchRiskLevelOptions: mockFetchRiskLevelOptions
  };
});

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
    <section class="emergency-plan-workbench-panel-stub">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div class="emergency-plan-workbench-panel-stub__header-actions"><slot name="header-actions" /></div>
      <div class="emergency-plan-workbench-panel-stub__filters"><slot name="filters" /></div>
      <div class="emergency-plan-workbench-panel-stub__applied"><slot name="applied-filters" /></div>
      <div class="emergency-plan-workbench-panel-stub__notices"><slot name="notices" /></div>
      <div class="emergency-plan-workbench-panel-stub__toolbar"><slot name="toolbar" /></div>
      <div class="emergency-plan-workbench-panel-stub__body"><slot /></div>
      <div class="emergency-plan-workbench-panel-stub__pagination"><slot name="pagination" /></div>
    </section>
  `
});

const StandardTableToolbarStub = defineComponent({
  name: 'StandardTableToolbar',
  props: ['metaItems'],
  template: `
    <section class="emergency-plan-table-toolbar-stub">
      <div class="emergency-plan-table-toolbar-stub__meta">{{ (metaItems || []).join(' | ') }}</div>
      <slot />
      <slot name="right" />
    </section>
  `
});

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function mountView() {
  return shallowMount(EmergencyPlanView, {
    global: {
      directives: {
        loading: () => undefined
      },
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        StandardTableToolbar: StandardTableToolbarStub,
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

describe('EmergencyPlanView', () => {
  beforeEach(() => {
    mockPagePlanList.mockReset();
    mockAddPlan.mockReset();
    mockUpdatePlan.mockReset();
    mockDeletePlan.mockReset();
    mockFetchRiskLevelOptions.mockReset();
    mockFetchAlarmLevelOptions.mockReset();
    mockRoute.query = {};
    mockFetchRiskLevelOptions.mockResolvedValue([
      { label: '红色', value: 'red', sortNo: 1 },
      { label: '橙色', value: 'orange', sortNo: 2 },
      { label: '蓝色', value: 'blue', sortNo: 4 }
    ]);
    mockFetchAlarmLevelOptions.mockResolvedValue([
      { label: '红色', value: 'red', sortNo: 1 },
      { label: '橙色', value: 'orange', sortNo: 2 },
      { label: '蓝色', value: 'blue', sortNo: 4 }
    ]);
  });

  it('counts emergency plans by alarm level instead of the old risk level field', async () => {
    mockPagePlanList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 3,
        pageNum: 1,
        pageSize: 10,
        records: [
          { id: 1, planName: '红色旧预案', alarmLevel: 'critical', status: 0 },
          { id: 2, planName: '橙色旧预案', alarmLevel: 'warning', status: 0 },
          { id: 3, planName: '蓝色新预案', alarmLevel: 'blue', status: 1 }
        ]
      }
    });

    const wrapper = mountView();
    await flushPromises();

    expect(wrapper.text()).toContain('红色 1 项');
    expect(wrapper.text()).toContain('橙色 1 项');
  });

  it('hydrates route query filters before loading emergency plans', async () => {
    mockRoute.query = {
      planName: '裂缝值应急预案',
      alarmLevel: 'red',
      status: '0'
    };
    mockPagePlanList.mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [{ id: 1, planName: '裂缝值应急预案', alarmLevel: 'red', status: 0 }]
      }
    });

    mountView();
    await flushPromises();

    expect(mockPagePlanList).toHaveBeenCalledWith(expect.objectContaining({
      planName: '裂缝值应急预案',
      alarmLevel: 'red',
      status: 0
    }));
  });
});
