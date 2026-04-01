import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getDeviceByCode, getDeviceMessageLogs, getDeviceProperties } from '@/api/iot';
import { getRiskMonitoringList } from '@/api/riskMonitoring';
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
        payload: '{"properties":{"temperature":23.5}}',
        reportTime: '2026-03-28 10:05:00'
      }
    ]
  })
}));

vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 2,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          bindingId: 11,
          deviceCode: 'demo-device-01',
          deviceName: '演示设备',
          riskPointName: '北坡位移监测点',
          riskLevel: 'INFO',
          metricIdentifier: 'displacementX',
          metricName: '位移 X',
          onlineStatus: 1,
          latestReportTime: '2026-04-01 08:00:00'
        },
        {
          bindingId: 22,
          deviceCode: 'demo-device-01',
          deviceName: '演示设备',
          riskPointName: '北坡预警点',
          riskLevel: 'WARNING',
          metricIdentifier: 'warningLightState',
          metricName: '预警灯状态',
          onlineStatus: 1,
          latestReportTime: '2026-04-01 09:00:00'
        }
      ]
    }
  }),
  getRiskMonitoringDetail: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      bindingId: 22,
      riskPointId: 2,
      riskPointCode: 'RP-022',
      riskPointName: '北坡预警点',
      riskLevel: 'WARNING',
      deviceId: 1,
      deviceCode: 'demo-device-01',
      deviceName: '演示设备',
      productName: '边坡预警终端',
      metricIdentifier: 'warningLightState',
      metricName: '预警灯状态',
      currentValue: '1',
      unit: '',
      valueType: 'int',
      monitorStatus: 'ALARM',
      onlineStatus: 1,
      latestReportTime: '2026-04-01 09:00:00',
      regionName: '北坡区',
      address: '北坡 1 号点',
      activeAlarmCount: 2,
      recentEventCount: 1,
      trendPoints: [
        { reportTime: '2026-04-01 08:00:00', value: '0', numericValue: 0 },
        { reportTime: '2026-04-01 09:00:00', value: '1', numericValue: 1 }
      ]
    }
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
          identifier: 'temperature',
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

const TrendPanelStub = defineComponent({
  name: 'TrendPanelStub',
  template: '<section class="trend-panel-stub">属性趋势预览</section>'
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
        MetricCard: true,
        PanelCard: PanelCardStub,
        PropertyTrendPanel: TrendPanelStub,
        RiskInsightTrendPanel: TrendPanelStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        'el-form-item': true,
        'el-input': true,
        'el-tag': true,
        'el-segmented': true,
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

  it('renders risk-monitoring-first insight content and removes legacy action cards', async () => {
    const wrapper = mountView();

    await flushPromises();

    expect(wrapper.find('.standard-page-shell-stub').exists()).toBe(true);
    expect(wrapper.findAll('.device-insight-workbench-stub')).toHaveLength(1);
    expect(wrapper.text()).toContain('对象洞察台');
    expect(wrapper.text()).toContain('基础档案');
    expect(wrapper.text()).toContain('研判依据');
    expect(wrapper.text()).toContain('属性趋势预览');
    expect(wrapper.text()).toContain('关键监测指标');
    expect(wrapper.text()).toContain('风险分析草稿');
    expect(wrapper.text()).toContain('设备属性快照');
    expect(wrapper.text()).toContain('北坡预警点');
    expect(wrapper.text()).not.toContain('当前建议动作');
    expect(wrapper.text()).not.toContain('一线建议');
    expect(wrapper.text()).not.toContain('运维建议');
    expect(wrapper.text()).not.toContain('研发建议');
    expect(wrapper.text()).not.toContain('消息日志与审计回看');
  });

  it('shows binding switcher when one device hits multiple monitoring bindings', async () => {
    const wrapper = mountView();

    await flushPromises();

    expect(wrapper.text()).toContain('监测对象切换');
  });

  it('falls back to device report analysis when the device is not bound to risk monitoring', async () => {
    vi.mocked(getRiskMonitoringList).mockResolvedValueOnce({
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

    expect(getDeviceByCode).toHaveBeenCalledWith('demo-device-01');
    expect(getDeviceProperties).toHaveBeenCalledWith('demo-device-01');
    expect(getDeviceMessageLogs).toHaveBeenCalledWith('demo-device-01');
    expect(wrapper.text()).toContain('演示设备');
    expect(wrapper.text()).toContain('温度');
    expect(wrapper.text()).toContain('当前设备未纳入风险监测绑定');
  });
});
