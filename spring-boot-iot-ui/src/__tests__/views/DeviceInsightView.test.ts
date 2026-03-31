import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import DeviceInsightView from '@/views/DeviceInsightView.vue';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {
      deviceCode: 'demo-device-01'
    } as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn(),
    replace: vi.fn()
  }
}));

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}));

vi.mock('@/api/iot', () => ({
  getDeviceByCode: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      deviceCode: 'demo-device-01',
      deviceName: '演示设备',
      onlineStatus: 1,
      protocolCode: 'mqtt-json',
      lastOnlineTime: '2026-03-28 10:00:00',
      lastOfflineTime: null,
      lastReportTime: '2026-03-28 10:05:00',
      firmwareVersion: '1.0.0',
      address: '测试区域'
    }
  }),
  getDeviceProperties: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        identifier: 'temperature',
        propertyName: '温度',
        propertyValue: '23.5',
        valueType: 'double',
        updateTime: '2026-03-28 10:05:00'
      }
    ]
  }),
  getDeviceMessageLogs: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        id: 1,
        messageType: 'PROPERTY_REPORT',
        topic: '/demo/topic',
        traceId: 'trace-001',
        payload: '{"temperature":23.5}',
        reportTime: '2026-03-28 10:05:00'
      }
    ]
  })
}));

vi.mock('@/stores/activity', () => ({
  recordActivity: vi.fn()
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

const StandardWorkbenchPanelStub = defineComponent({
  name: 'StandardWorkbenchPanel',
  props: {
    eyebrow: String,
    title: String,
    description: String,
    showFilters: Boolean,
    showInlineState: Boolean
  },
  template: `
    <section class="device-insight-workbench-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <div><slot name="filters" /></div>
      <div><slot name="inline-state" /></div>
      <div><slot /></div>
    </section>
  `
});

const StandardPageShellStub = defineComponent({
  name: 'StandardPageShell',
  props: ['title', 'showTitle'],
  template: `
    <section class="standard-page-shell-stub">
      <h1 v-if="showTitle !== false">{{ title }}</h1>
      <slot />
    </section>
  `
});

const PanelCardStub = defineComponent({
  name: 'PanelCard',
  props: ['eyebrow', 'title', 'description'],
  template: `
    <section class="panel-card-stub">
      <p v-if="eyebrow">{{ eyebrow }}</p>
      <h3 v-if="title">{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot />
    </section>
  `
});

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label'],
  template: `
    <div class="standard-table-text-column-stub">
      <span>{{ label }}</span>
      <slot
        :row="{
          propertyName: '温度',
          propertyValue: '23.5',
          valueType: 'double',
          updateTime: '2026-03-28 10:05:00',
          reportTime: '2026-03-28 10:05:00',
          messageType: 'PROPERTY_REPORT',
          topic: '/demo/topic',
          traceId: 'trace-001',
          payload: '{&quot;temperature&quot;:23.5}'
        }"
      />
    </div>
  `
});

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

function mountView() {
  return shallowMount(DeviceInsightView, {
    global: {
      renderStubDefaultSlot: true,
      stubs: {
        StandardPageShell: StandardPageShellStub,
        StandardWorkbenchPanel: StandardWorkbenchPanelStub,
        StandardListFilterHeader: true,
        StandardInlineState: true,
        StandardButton: true,
        StandardActionGroup: true,
        MetricCard: true,
        PanelCard: PanelCardStub,
        PropertyTrendPanel: true,
        StandardTableTextColumn: StandardTableTextColumnStub,
        'el-form-item': true,
        'el-input': true,
        'el-tag': true,
        'el-descriptions': true,
        'el-descriptions-item': true,
        'el-empty': true,
        'el-table': true,
        'el-table-column': true
      }
    }
  });
}

describe('DeviceInsightView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockRoute.query = {
      deviceCode: 'demo-device-01'
    };
  });

  it('renders object insight inside a single calm workbench without legacy English eyebrow tiers', async () => {
    const wrapper = mountView();

    await flushPromises();

    const workbench = wrapper.findComponent(StandardWorkbenchPanelStub);

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true);
    expect(wrapper.findAll('.device-insight-workbench-stub')).toHaveLength(1);
    expect(workbench.props('eyebrow')).toBeUndefined();
    expect(workbench.props('showFilters')).toBe(true);
    expect(workbench.props('showInlineState')).toBe(true);
    expect(wrapper.text()).toContain('对象洞察台');
    expect(wrapper.text()).toContain('当前建议动作');
    expect(wrapper.text()).toContain('关键监测指标');
    expect(wrapper.text()).toContain('消息日志与审计回看');
    expect(wrapper.text()).not.toContain('Risk Workbench');
    expect(wrapper.text()).not.toContain('Instant Focus');
    expect(wrapper.text()).not.toContain('Point Profile');
    expect(wrapper.text()).not.toContain('Risk Reasons');
    expect(wrapper.text()).not.toContain('Field Action');
    expect(wrapper.text()).not.toContain('O&M Action');
    expect(wrapper.text()).not.toContain('Dev Action');
    expect(wrapper.text()).not.toContain('Key Properties');
    expect(wrapper.text()).not.toContain('Report Draft');
    expect(wrapper.text()).not.toContain('Latest Properties');
    expect(wrapper.text()).not.toContain('Message Logs');
  });
});
