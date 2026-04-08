import { defineComponent } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getTelemetryHistoryBatch } from '@/api/telemetry';
import { getDeviceByCode, getDeviceProperties } from '@/api/iot';
import { productApi } from '@/api/product';
import { getRiskMonitoringDetail, getRiskMonitoringList } from '@/api/riskMonitoring';
import DeviceInsightView from '@/views/DeviceInsightView.vue';

const { mockRoute, mockRouter } = vi.hoisted(() => ({
  mockRoute: {
    query: {} as Record<string, unknown>
  },
  mockRouter: {
    push: vi.fn(),
    replace: vi.fn().mockResolvedValue(undefined)
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
      id: 2001,
      productId: 501,
      deviceCode: 'SK00EB0D1308313',
      deviceName: '泥水位监测设备',
      productName: '宏观现象监测设备泥水位',
      onlineStatus: 1,
      protocolCode: 'mqtt-json',
      lastOnlineTime: '2026-04-08 10:00:00',
      lastReportTime: '2026-04-08 10:05:00',
      firmwareVersion: '1.0.0',
      address: '测试沟道'
    }
  }),
  getDeviceProperties: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: [
      {
        identifier: 'L4_NW_1',
        propertyName: '泥水位高程',
        propertyValue: '2.60',
        valueType: 'double',
        updateTime: '2026-04-08 10:05:00'
      },
      {
        identifier: 'S1_ZT_1.sensor_state.L4_NW_1',
        propertyName: '传感器在线状态',
        propertyValue: '1',
        valueType: 'int',
        updateTime: '2026-04-08 10:05:00'
      },
      {
        identifier: 'S1_ZT_1.battery_dump_energy',
        propertyName: '剩余电量',
        propertyValue: '86',
        valueType: 'int',
        updateTime: '2026-04-08 10:05:00'
      }
    ]
  }),
  getDeviceMessageLogs: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: []
  })
}));

vi.mock('@/api/product', () => ({
  productApi: {
    getProductById: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 501,
        productKey: 'muddy-water-product',
        productName: '宏观现象监测设备泥水位',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: null
      }
    })
  }
}));

vi.mock('@/api/telemetry', () => ({
  getTelemetryHistoryBatch: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      deviceId: 2001,
      rangeCode: '7d',
      bucket: 'day',
      points: [
        {
          identifier: 'L4_NW_1',
          displayName: '泥水位高程',
          seriesType: 'measure',
          buckets: [{ time: '2026-04-07 00:00:00', value: 2.6, filled: false }]
        },
        {
          identifier: 'S1_ZT_1.sensor_state.L4_NW_1',
          displayName: '传感器在线状态',
          seriesType: 'status',
          buckets: [{ time: '2026-04-07 00:00:00', value: 1, filled: false }]
        },
        {
          identifier: 'S1_ZT_1.battery_dump_energy',
          displayName: '剩余电量',
          seriesType: 'status',
          buckets: [{ time: '2026-04-07 00:00:00', value: 86, filled: false }]
        }
      ]
    }
  })
}));

vi.mock('@/api/riskMonitoring', () => ({
  getRiskMonitoringList: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: {
      total: 1,
      pageNum: 1,
      pageSize: 10,
      records: [
        {
          bindingId: 22,
          deviceCode: 'SK00EB0D1308313',
          deviceName: '泥水位监测设备',
          riskPointName: '泥石流沟道风险点',
          riskLevel: 'WARNING',
          metricIdentifier: 'L4_NW_1',
          metricName: '泥水位高程',
          onlineStatus: 1,
          latestReportTime: '2026-04-08 10:05:00'
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
      riskPointName: '泥石流沟道风险点',
      riskLevel: 'WARNING',
      deviceId: 2001,
      deviceCode: 'SK00EB0D1308313',
      deviceName: '泥水位监测设备',
      productName: '宏观现象监测设备泥水位',
      metricIdentifier: 'L4_NW_1',
      metricName: '泥水位高程',
      currentValue: '2.60',
      monitorStatus: 'ALARM',
      onlineStatus: 1,
      latestReportTime: '2026-04-08 10:05:00',
      regionName: '北坡区',
      address: '测试沟道'
    }
  })
}));

vi.mock('@/api/riskGovernance', () => ({
  listMissingBindings: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 5, records: [] }
  }),
  listMissingPolicies: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: { total: 0, pageNum: 1, pageSize: 5, records: [] }
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
    title: String,
    description: String,
    showFilters: Boolean,
    showInlineState: Boolean
  },
  template: `
    <section class="device-insight-workbench-stub">
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
  props: ['title', 'description'],
  template: `
    <section class="panel-card-stub">
      <h3 v-if="title">{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot />
    </section>
  `
});

const TrendPanelStub = defineComponent({
  name: 'TrendPanelStub',
  props: ['groups', 'rangeCode'],
  template: `
    <section class="trend-panel-stub">
      <div>属性趋势预览</div>
      <div>{{ rangeCode }}</div>
      <div v-for="group in groups" :key="group.title">{{ group.title }}</div>
    </section>
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
        MetricCard: true,
        PanelCard: PanelCardStub,
        RiskInsightTrendPanel: TrendPanelStub,
        StandardTableTextColumn: true,
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
    mockRoute.query = {};
  });

  it('keeps direct-open insight idle until user inputs device code', async () => {
    const wrapper = mountView();

    await flushPromises();

    expect(getDeviceByCode).not.toHaveBeenCalled();
    expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('请输入设备编码后开始综合分析');
  });

  it('auto-loads single-device insight when device workbench passes deviceCode', async () => {
    mockRoute.query = {
      deviceCode: 'SK00EB0D1308313'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getDeviceByCode).toHaveBeenCalledWith('SK00EB0D1308313');
    expect(getRiskMonitoringList).toHaveBeenCalledWith(expect.objectContaining({
      deviceCode: 'SK00EB0D1308313'
    }));
    expect(getRiskMonitoringDetail).toHaveBeenCalledWith(22);
    expect(getDeviceProperties).toHaveBeenCalledWith('SK00EB0D1308313');
    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 2001,
      rangeCode: '7d',
      fillPolicy: 'ZERO'
    }));
    expect(wrapper.text()).toContain('对象洞察台');
    expect(wrapper.text()).toContain('基础档案信息');
    expect(wrapper.text()).toContain('设备基础档案');
    expect(wrapper.text()).toContain('风险上下文档案');
    expect(wrapper.text()).toContain('核心指标');
    expect(wrapper.text()).toContain('泥水位高程');
    expect(wrapper.text()).toContain('传感器在线状态');
    expect(wrapper.text()).toContain('剩余电量');
    expect(wrapper.text()).toContain('属性趋势预览');
    expect(wrapper.text()).not.toContain('L4_NW_1');
  });

  it('derives dynamic metrics for collect devices and includes runtime status extensions in trend query', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3001,
        deviceCode: 'COLLECT-001',
        deviceName: '雨量采集设备',
        productName: '雨量采集终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-08 11:00:00',
        lastReportTime: '2026-04-08 11:05:00',
        firmwareVersion: '1.1.0',
        address: '采集点 A',
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'S1_ZT_1.humidity',
                displayName: '相对湿度',
                group: 'status',
                analysisTitle: '现场环境补充',
                analysisTag: '系统自定义参数',
                analysisTemplate: '{{label}}当前为{{value}}，可辅助判断现场环境湿润程度。'
              },
              {
                identifier: 'S1_ZT_1.signal_4g',
                displayName: '4G 信号强度',
                group: 'status',
                analysisTitle: '通信状态补充',
                analysisTag: '系统自定义参数',
                analysisTemplate: '{{label}}当前为{{value}}，可辅助判断设备回传链路稳定性。'
              }
            ]
          }
        })
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'YL_1',
          propertyName: '雨量',
          propertyValue: '12.4',
          valueType: 'double',
          updateTime: '2026-04-08 11:05:00'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.YL_1',
          propertyName: '采集通道在线状态',
          propertyValue: '1',
          valueType: 'int',
          updateTime: '2026-04-08 11:05:00'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-82',
          valueType: 'int',
          updateTime: '2026-04-08 11:05:00'
        },
        {
          id: 4,
          identifier: 'S1_ZT_1.humidity',
          propertyName: '相对湿度',
          propertyValue: '75',
          valueType: 'double',
          updateTime: '2026-04-08 11:05:00'
        }
      ]
    });
    vi.mocked(getRiskMonitoringList).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            bindingId: 31,
            deviceCode: 'COLLECT-001',
            deviceName: '雨量采集设备',
            riskPointName: '沟道采集点',
            riskLevel: 'NOTICE',
            metricIdentifier: 'YL_1',
            metricName: '雨量',
            onlineStatus: 1,
            latestReportTime: '2026-04-08 11:05:00'
          }
        ]
      }
    });
    vi.mocked(getRiskMonitoringDetail).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        bindingId: 31,
        riskPointId: 9,
        riskPointCode: 'RP-031',
        riskPointName: '沟道采集点',
        riskLevel: 'NOTICE',
        deviceId: 3001,
        deviceCode: 'COLLECT-001',
        deviceName: '雨量采集设备',
        productName: '雨量采集终端',
        metricIdentifier: 'YL_1',
        metricName: '雨量',
        currentValue: '12.4',
        monitorStatus: 'NORMAL',
        onlineStatus: 1,
        latestReportTime: '2026-04-08 11:05:00'
      }
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 3001,
        rangeCode: '7d',
        bucket: 'day',
        points: [
          {
            identifier: 'YL_1',
            displayName: '雨量',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-07 00:00:00', value: 12.4, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.sensor_state.YL_1',
            displayName: '采集通道在线状态',
            seriesType: 'status',
            buckets: [{ time: '2026-04-07 00:00:00', value: 1, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.signal_4g',
            displayName: '4G 信号强度',
            seriesType: 'status',
            buckets: [{ time: '2026-04-07 00:00:00', value: -82, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.humidity',
            displayName: '相对湿度',
            seriesType: 'status',
            buckets: [{ time: '2026-04-07 00:00:00', value: 75, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'COLLECT-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 3001,
      identifiers: expect.arrayContaining(['YL_1', 'S1_ZT_1.sensor_state.YL_1', 'S1_ZT_1.signal_4g', 'S1_ZT_1.humidity'])
    }));
    expect(wrapper.text()).toContain('雨量');
    expect(wrapper.text()).toContain('采集通道在线状态');
    expect(wrapper.text()).toContain('4G 信号强度');
    expect(wrapper.text()).toContain('相对湿度');
    expect(wrapper.text()).toContain('相对湿度当前为75，可辅助判断现场环境湿润程度。');
    expect(wrapper.text()).toContain('4G 信号强度当前为-82，可辅助判断设备回传链路稳定性。');
  });

  it('loads product-level insight config by productId when device metadata is absent', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3201,
        productId: 601,
        deviceCode: 'COLLECT-009',
        deviceName: '雨量采集设备',
        productName: '雨量采集终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-08 12:00:00',
        lastReportTime: '2026-04-08 12:05:00',
        firmwareVersion: '1.2.0',
        address: '采集点 B',
        metadataJson: null
      }
    });
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 601,
        productKey: 'collect-product',
        productName: '雨量采集终端',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'S1_ZT_1.signal_4g',
                displayName: '传输信号',
                group: 'status',
                analysisTitle: '通信状态补充',
                analysisTag: '产品正式配置',
                analysisTemplate: '{{label}}当前为{{value}}，用于判断设备回传链路稳定性。'
              }
            ]
          }
        })
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'YL_1',
          propertyName: '雨量',
          propertyValue: '10.8',
          valueType: 'double',
          updateTime: '2026-04-08 12:05:00'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.YL_1',
          propertyName: '采集通道在线状态',
          propertyValue: '1',
          valueType: 'int',
          updateTime: '2026-04-08 12:05:00'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-80',
          valueType: 'int',
          updateTime: '2026-04-08 12:05:00'
        }
      ]
    });
    vi.mocked(getRiskMonitoringList).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 1,
        pageNum: 1,
        pageSize: 10,
        records: [
          {
            bindingId: 41,
            deviceCode: 'COLLECT-009',
            deviceName: '雨量采集设备',
            riskPointName: '沟道采集点',
            riskLevel: 'NOTICE',
            metricIdentifier: 'YL_1',
            metricName: '雨量',
            onlineStatus: 1,
            latestReportTime: '2026-04-08 12:05:00'
          }
        ]
      }
    });
    vi.mocked(getRiskMonitoringDetail).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        bindingId: 41,
        riskPointId: 19,
        riskPointCode: 'RP-041',
        riskPointName: '沟道采集点',
        riskLevel: 'NOTICE',
        deviceId: 3201,
        deviceCode: 'COLLECT-009',
        deviceName: '雨量采集设备',
        productName: '雨量采集终端',
        metricIdentifier: 'YL_1',
        metricName: '雨量',
        currentValue: '10.8',
        monitorStatus: 'NORMAL',
        onlineStatus: 1,
        latestReportTime: '2026-04-08 12:05:00'
      }
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 3201,
        rangeCode: '7d',
        bucket: 'day',
        points: [
          {
            identifier: 'YL_1',
            displayName: '雨量',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-07 00:00:00', value: 10.8, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.sensor_state.YL_1',
            displayName: '采集通道在线状态',
            seriesType: 'status',
            buckets: [{ time: '2026-04-07 00:00:00', value: 1, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.signal_4g',
            displayName: '传输信号',
            seriesType: 'status',
            buckets: [{ time: '2026-04-07 00:00:00', value: -80, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'COLLECT-009'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(productApi.getProductById).toHaveBeenCalledWith(601);
    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 3201,
      identifiers: expect.arrayContaining(['S1_ZT_1.signal_4g'])
    }));
    expect(wrapper.text()).toContain('传输信号');
    expect(wrapper.text()).toContain('传输信号当前为-80，用于判断设备回传链路稳定性。');
  });
});
