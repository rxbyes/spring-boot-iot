import { defineComponent, inject } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { getTelemetryHistoryBatch } from '@/api/telemetry';
import { getCollectorChildInsightOverview, getDeviceByCode, getDeviceProperties } from '@/api/iot';
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
  getCollectorChildInsightOverview: vi.fn().mockResolvedValue({
    code: 200,
    msg: 'success',
    data: null
  }),
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
        unit: 'm',
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
    }),
    listProductModels: vi.fn().mockResolvedValue({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          productId: 501,
          modelType: 'property',
          identifier: 'L4_NW_1',
          modelName: '泥水位高程',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: 'm'
          })
        },
        {
          id: 2,
          productId: 501,
          modelType: 'property',
          identifier: 'S1_ZT_1.battery_dump_energy',
          modelName: '剩余电量',
          dataType: 'int',
          specsJson: JSON.stringify({
            unit: '%'
          })
        }
      ]
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

const StandardListFilterHeaderStub = defineComponent({
  name: 'StandardListFilterHeader',
  template: `
    <section class="standard-list-filter-header-stub">
      <div class="standard-list-filter-header-stub__primary"><slot name="primary" /></div>
      <div class="standard-list-filter-header-stub__actions"><slot name="actions" /></div>
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

const StandardButtonStub = defineComponent({
  name: 'StandardButton',
  props: ['disabled', 'loading'],
  emits: ['click'],
  template: `
    <button
      class="standard-button-stub"
      type="button"
      :disabled="Boolean(disabled) || Boolean(loading)"
      @click="$emit('click')"
    >
      <slot />
    </button>
  `
});

const MetricCardStub = defineComponent({
  name: 'MetricCard',
  props: ['label', 'value'],
  template: `
    <article class="metric-card-stub">
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
    </article>
  `
});

const TrendPanelStub = defineComponent({
  name: 'TrendPanelStub',
  props: ['groups', 'rangeCode', 'emptyMessage'],
  emits: ['change-range'],
  template: `
    <section class="trend-panel-stub">
      <div>属性趋势预览</div>
      <div>{{ rangeCode }}</div>
      <button data-testid="trend-panel-range-1d" type="button" @click="$emit('change-range', '1d')">近一天</button>
      <button data-testid="trend-panel-range-365d" type="button" @click="$emit('change-range', '365d')">近一年</button>
      <div v-if="!groups?.length" class="trend-panel-stub__empty">{{ emptyMessage }}</div>
      <div v-for="group in groups" :key="group.title">
        <div>{{ group.title }}</div>
        <div v-for="series in group.series" :key="series.identifier">{{ series.displayName }}</div>
      </div>
    </section>
  `
});

const ElSegmentedStub = defineComponent({
  name: 'ElSegmented',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    options: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue', 'change'],
  template: `
    <div class="el-segmented-stub">
      <button
        v-for="option in options"
        :key="option.value"
        :data-testid="'insight-range-' + option.value"
        type="button"
        @click="$emit('update:modelValue', option.value); $emit('change', option.value)"
      >
        {{ option.label }}
      </button>
    </div>
  `
});

const CollectorChildInsightPanelStub = defineComponent({
  name: 'CollectorChildInsightPanel',
  props: ['overview'],
  methods: {
    linkStateLabel(value?: string) {
      if (value === 'reachable') {
        return '链路可达';
      }
      if (value === 'unreachable') {
        return '链路不可达';
      }
      return '链路待确认';
    }
  },
  template: `
    <section v-if="overview?.children?.length" class="collector-child-insight-panel-stub">
      <div>子设备总览</div>
      <div>{{ overview.childCount }}</div>
      <article v-for="child in overview.children" :key="child.logicalChannelCode">
        <div>{{ child.logicalChannelCode }}</div>
        <div>{{ child.childDeviceCode }}</div>
        <div>{{ linkStateLabel(child.collectorLinkState) }}</div>
        <div>{{ child.sensorStateValue }}</div>
        <div v-for="metric in child.metrics" :key="metric.identifier">
          {{ metric.displayName || metric.identifier }}
        </div>
      </article>
    </section>
  `
});

const ElTableStub = defineComponent({
  name: 'ElTable',
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  provide() {
    return {
      deviceInsightTableRows: this.data
    };
  },
  template: `
    <section class="el-table-stub">
      <slot />
    </section>
  `
});

const StandardTableTextColumnStub = defineComponent({
  name: 'StandardTableTextColumn',
  props: ['label', 'prop'],
  setup() {
    const rows = inject<any[]>('deviceInsightTableRows', []);
    return { rows };
  },
  template: `
    <div class="standard-table-text-column-stub" :data-label="label">
      <span class="standard-table-text-column-stub__label">{{ label }}</span>
      <div
        v-for="(row, index) in rows"
        :key="index"
        class="standard-table-text-column-stub__value"
      >
        {{ row?.[prop] }}
      </div>
    </div>
  `
});

const ElTableColumnStub = defineComponent({
  name: 'ElTableColumn',
  props: ['label'],
  setup() {
    const rows = inject<any[]>('deviceInsightTableRows', []);
    return { rows };
  },
  template: `
    <div class="el-table-column-stub" :data-label="label">
      <div
        v-for="(row, index) in rows"
        :key="index"
        class="el-table-column-stub__cell"
      >
        <slot :row="row" />
      </div>
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
        StandardListFilterHeader: StandardListFilterHeaderStub,
        StandardInlineState: true,
        StandardButton: StandardButtonStub,
        MetricCard: MetricCardStub,
        PanelCard: PanelCardStub,
        CollectorChildInsightPanel: CollectorChildInsightPanelStub,
        RiskInsightTrendPanel: TrendPanelStub,
        StandardTableTextColumn: StandardTableTextColumnStub,
        'el-form-item': true,
        'el-input': true,
        'el-tag': true,
        'el-segmented': ElSegmentedStub,
        'el-descriptions': true,
        'el-descriptions-item': true,
        'el-empty': true,
        'el-table': ElTableStub,
        'el-table-column': ElTableColumnStub
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
    expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('对象洞察台');
    expect(wrapper.text()).toContain('基础档案信息');
    expect(wrapper.text()).toContain('设备基础档案');
    expect(wrapper.text()).toContain('风险上下文档案');
    expect(wrapper.text()).not.toContain('核心指标');
    expect(wrapper.text()).toContain('泥水位高程');
    expect(wrapper.text()).toContain('传感器在线状态');
    expect(wrapper.text()).toContain('剩余电量');
    expect(wrapper.text()).toContain('属性趋势预览');
    expect(
      wrapper
        .findAll('.standard-table-text-column-stub[data-label="属性名称"] .standard-table-text-column-stub__value')
        .map((node) => node.text().trim())
        .filter(Boolean)
    ).toEqual(['泥水位高程', '传感器在线状态', '剩余电量']);
    expect(wrapper.findAll('[data-testid^="insight-range-"]')).toHaveLength(0);
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(0);
  });

  it('renders collector child aggregate panel without merging child metrics into collector snapshot', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 6201,
        productId: 801,
        deviceCode: 'SK00EA0D1307988',
        deviceName: '激光采集器',
        productName: '南方测绘 监测型 采集器',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 2,
        lastOnlineTime: '2026-04-09 21:47:28',
        lastReportTime: '2026-04-09 21:47:28',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-71',
          valueType: 'int',
          updateTime: '2026-04-09 21:47:28'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 801,
        productKey: 'nf-monitor-collector-v1',
        productName: '南方测绘 监测型 采集器',
        protocolCode: 'mqtt-json',
        nodeType: 2,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    vi.mocked(getCollectorChildInsightOverview).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        parentDeviceCode: 'SK00EA0D1307988',
        parentOnlineStatus: 1,
        childCount: 1,
        reachableChildCount: 1,
        sensorStateReportedCount: 1,
        children: [
          {
            logicalChannelCode: 'L1_LF_1',
            childDeviceCode: '202018108',
            childDeviceName: '1# 激光测点',
            childProductKey: 'nf-monitor-laser-rangefinder-v1',
            collectorLinkState: 'reachable',
            sensorStateValue: '0',
            lastReportTime: '2026-04-09 21:47:28',
            metrics: [
              {
                identifier: 'value',
                displayName: '激光测距值',
                propertyValue: '10.86',
                unit: 'mm',
                reportTime: '2026-04-09 21:47:28'
              }
            ]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'SK00EA0D1307988'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getCollectorChildInsightOverview).toHaveBeenCalledWith('SK00EA0D1307988');
    expect(wrapper.text()).toContain('子设备总览');
    expect(wrapper.text()).toContain('L1_LF_1');
    expect(wrapper.text()).toContain('激光测距值');
    expect(wrapper.text()).toContain('链路可达');
  });

  it('loads collector child overview for nf-collect-rtu-v1 even when the device nodeType remains 1', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 6203,
        productId: 803,
        deviceCode: 'SK00EA0D1307967',
        deviceName: 'NF-COLLECTOR-SK00EA0D1307967',
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 1,
        lastOnlineTime: '2026-04-19 12:14:08',
        lastReportTime: '2026-04-19 12:14:08',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: 'S1_ZT_1.signal_4g',
          propertyValue: '18',
          valueType: 'int',
          updateTime: '2026-04-19 12:14:08'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 803,
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          governance: {
            productCapabilityType: 'COLLECTING'
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    vi.mocked(getCollectorChildInsightOverview).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        parentDeviceCode: 'SK00EA0D1307967',
        parentOnlineStatus: 1,
        childCount: 1,
        reachableChildCount: 1,
        sensorStateReportedCount: 1,
        children: [
          {
            logicalChannelCode: 'L1_LF_1',
            childDeviceCode: '202018134',
            childDeviceName: 'NF-LASER-202018134',
            childProductKey: 'nf-monitor-laser-rangefinder-v1',
            collectorLinkState: 'reachable',
            sensorStateValue: '0',
            lastReportTime: '2026-04-19 12:14:08',
            metrics: [
              {
                identifier: 'value',
                displayName: '激光测距值',
                propertyValue: '2473.72',
                reportTime: '2026-04-19 12:14:08'
              }
            ]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'SK00EA0D1307967'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getCollectorChildInsightOverview).toHaveBeenCalledWith('SK00EA0D1307967');
    expect(wrapper.text()).toContain('子设备总览');
    expect(wrapper.text()).toContain('激光测距值');
  });

  it('surfaces suggestion-first collector metrics for downstream object insight governance', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 6201,
        productId: 801,
        deviceCode: 'SK00EA0D1307988',
        deviceName: '激光采集器',
        productName: '南方测绘 监测型 采集器',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 2,
        lastOnlineTime: '2026-04-09 21:47:28',
        lastReportTime: '2026-04-09 21:47:28',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 801,
        productKey: 'nf-monitor-collector-v1',
        productName: '南方测绘 监测型 采集器',
        protocolCode: 'mqtt-json',
        nodeType: 2,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    vi.mocked(getCollectorChildInsightOverview).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        parentDeviceCode: 'SK00EA0D1307988',
        parentOnlineStatus: 1,
        childCount: 1,
        reachableChildCount: 1,
        sensorStateReportedCount: 1,
        recommendedMetricCount: 1,
        children: [
          {
            logicalChannelCode: 'L1_LF_1',
            childDeviceCode: '202018108',
            childDeviceName: '1# 激光测点',
            childProductKey: 'nf-monitor-laser-rangefinder-v1',
            collectorLinkState: 'reachable',
            sensorStateValue: '0',
            lastReportTime: '2026-04-09 21:47:28',
            recommendedMetricIdentifiers: ['value'],
            metrics: [
              {
                identifier: 'value',
                displayName: '激光测距值',
                propertyValue: '10.86',
                unit: 'mm',
                reportTime: '2026-04-09 21:47:28',
                recommended: true
              }
            ]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'SK00EA0D1307988'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(wrapper.text()).toContain('建议优先纳入对象洞察');
    expect(wrapper.text()).toContain('1# 激光测点');
    expect(wrapper.text()).toContain('激光测距值');
  });

  it('shows property snapshot units from snapshot first and product model fallback', async () => {
    mockRoute.query = {
      deviceCode: 'SK00EB0D1308313'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const unitColumn = wrapper.find('[data-label="单位"]');
    expect(unitColumn.exists()).toBe(true);
    expect(unitColumn.text()).toContain('单位');
    expect(unitColumn.text()).toContain('m');
    expect(unitColumn.text()).toContain('%');
    expect(productApi.listProductModels).toHaveBeenCalledWith(501);
  });

  it('prefers renamed formal field names over stale trend labels and fixed fallback labels', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4101,
        productId: 701,
        deviceCode: 'RENAMED-001',
        deviceName: '多维检测仪',
        productName: '多维检测仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-10 09:00:00',
        lastReportTime: '2026-04-10 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_JS_1.gX',
          propertyName: '甲方X轴指标',
          propertyValue: '0.12',
          valueType: 'double',
          updateTime: '2026-04-10 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 701,
        productKey: 'renamed-device-product',
        productName: '多维检测仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L1_JS_1.gX',
                displayName: '甲方X轴指标',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 11,
          productId: 701,
          modelType: 'property',
          identifier: 'L1_JS_1.gX',
          modelName: '甲方X轴指标',
          dataType: 'double',
          specsJson: null
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 4101,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'L1_JS_1.gX',
            displayName: '旧X轴加速度',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-10 09:00:00', value: 0.12, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'RENAMED-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(wrapper.text()).toContain('甲方X轴指标');
    expect(wrapper.text()).not.toContain('旧X轴加速度');
  });

  it('uses full formal display names with units for trend series labels', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4201,
        productId: 702,
        deviceCode: 'RENAMED-UNIT-001',
        deviceName: '多维检测仪',
        productName: '多维检测仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-10 10:00:00',
        lastReportTime: '2026-04-10 10:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_JS_1.gX',
          propertyName: '1号加速度测点gX',
          propertyValue: '0.12',
          valueType: 'double',
          updateTime: '2026-04-10 10:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 702,
        productKey: 'renamed-device-product-unit',
        productName: '多维检测仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L1_JS_1.gX',
                displayName: 'X轴加速度',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 12,
          productId: 702,
          modelType: 'property',
          identifier: 'L1_JS_1.gX',
          modelName: 'X轴加速度',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: 'm/s²'
          })
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 4201,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'L1_JS_1.gX',
            displayName: '轴加速度',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-10 10:00:00', value: 0.12, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'RENAMED-UNIT-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const trendPanelTexts = wrapper.find('.trend-panel-stub').findAll('div').map((node) => node.text());

    expect(wrapper.text()).toContain('X轴加速度（m/s²）');
    expect(trendPanelTexts).toContain('X轴加速度（m/s²）');
    expect(trendPanelTexts).not.toContain('轴加速度');
  });

  it('prefers canonical formal names and units when trend metric still keeps a raw alias identifier', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4301,
        productId: 703,
        deviceCode: 'RAIN-ALIAS-001',
        deviceName: '翻斗式雨量计',
        productName: '翻斗式雨量计',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-20 08:00:00',
        lastReportTime: '2026-04-20 08:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L3_YL_1.totalValue',
          propertyName: 'L3_YL_1.totalValue',
          propertyValue: '0',
          valueType: 'double',
          updateTime: '2026-04-20 08:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 703,
        productKey: 'nf-monitor-tipping-bucket-rain-gauge-v1',
        productName: '翻斗式雨量计',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L3_YL_1.totalValue',
                displayName: 'L3_YL_1.totalValue',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 13,
          productId: 703,
          modelType: 'property',
          identifier: 'totalValue',
          modelName: '累计雨量',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: 'mm'
          })
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 4301,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'L3_YL_1.totalValue',
            displayName: 'L3_YL_1.totalValue',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-20 08:00:00', value: 0, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'RAIN-ALIAS-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const trendPanelTexts = wrapper.find('.trend-panel-stub').findAll('div').map((node) => node.text());

    expect(wrapper.text()).toContain('累计雨量（mm）');
    expect(trendPanelTexts).toContain('累计雨量（mm）');
    expect(trendPanelTexts).not.toContain('L3_YL_1.totalValue');
  });

  it('uses object-insight configured unit when product model does not provide one', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4501,
        productId: 705,
        deviceCode: 'CONFIG-UNIT-001',
        deviceName: '深部位移监测仪',
        productName: '深部位移监测仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-21 09:00:00',
        lastReportTime: '2026-04-21 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_SW_1.dispsX',
          propertyName: '水平位移',
          propertyValue: '1.5',
          valueType: 'double',
          updateTime: '2026-04-21 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 705,
        productKey: 'config-unit-product',
        productName: '深部位移监测仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L1_SW_1.dispsX',
                displayName: '水平位移',
                group: 'measure',
                unit: 'mm',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 5
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 15,
          productId: 705,
          modelType: 'property',
          identifier: 'L1_SW_1.dispsX',
          modelName: '水平位移',
          dataType: 'double',
          specsJson: null
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 4501,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'L1_SW_1.dispsX',
            displayName: '位移值',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-21 09:00:00', value: 1.5, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'CONFIG-UNIT-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const trendPanelTexts = wrapper.find('.trend-panel-stub').findAll('div').map((node) => node.text());

    expect(wrapper.text()).toContain('水平位移（mm）');
    expect(trendPanelTexts).toContain('水平位移（mm）');
    expect(wrapper.text()).toContain('单位');
    expect(wrapper.text()).toContain('mm');
  });

  it('only queries explicitly configured trend metrics for collect devices', async () => {
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

    expect(getTelemetryHistoryBatch).toHaveBeenCalledTimes(1);
    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 3001,
      identifiers: expect.arrayContaining(['S1_ZT_1.humidity', 'S1_ZT_1.signal_4g'])
    }));
    const trendRequest = vi.mocked(getTelemetryHistoryBatch).mock.calls[0]?.[0];
    expect(trendRequest?.identifiers).not.toContain('YL_1');
    expect(trendRequest?.identifiers).not.toContain('S1_ZT_1.sensor_state.YL_1');
    expect(wrapper.text()).toContain('雨量');
    expect(wrapper.text()).toContain('采集通道在线状态');
    expect(wrapper.text()).toContain('4G 信号强度');
    expect(wrapper.text()).toContain('相对湿度');
    expect(wrapper.text()).not.toContain('系统自定义参数');
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(0);
  });

  it('upgrades legacy short collector identifiers to full-path runtime identifiers before querying history', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3002,
        productId: 804,
        deviceCode: 'SK00EA0D1307967',
        deviceName: 'NF-COLLECTOR-SK00EA0D1307967',
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 1,
        lastOnlineTime: '2026-04-19 12:14:08',
        lastReportTime: '2026-04-19 12:14:08',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: 'S1_ZT_1.signal_4g',
          propertyValue: '18',
          valueType: 'int',
          updateTime: '2026-04-19 12:14:08'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.battery_dump_energy',
          propertyName: 'S1_ZT_1.battery_dump_energy',
          propertyValue: '0',
          valueType: 'int',
          updateTime: '2026-04-19 12:14:08'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 804,
        productKey: 'nf-collect-rtu-v1',
        productName: '南方测绘 采集型 遥测终端',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'signal_4g',
                displayName: '4G信号',
                group: 'runtime',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true
              },
              {
                identifier: 'battery_dump_energy',
                displayName: '电池剩余电量',
                group: 'runtime',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 3002,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'S1_ZT_1.signal_4g',
            displayName: '4G信号',
            seriesType: 'status',
            buckets: [{ time: '2026-04-19 12:00:00', value: 18, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.battery_dump_energy',
            displayName: '电池剩余电量',
            seriesType: 'status',
            buckets: [{ time: '2026-04-19 12:00:00', value: 0, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'SK00EA0D1307967'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).toHaveBeenCalledTimes(1);
    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 3002,
      identifiers: expect.arrayContaining(['S1_ZT_1.signal_4g', 'S1_ZT_1.battery_dump_energy'])
    }));
    expect(wrapper.text()).toContain('4G信号');
    expect(wrapper.text()).toContain('电池剩余电量');
  });

  it('splits configured status trends into status events and runtime parameters', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3301,
        productId: 701,
        deviceCode: 'CXH15522832',
        deviceName: '多维检测仪',
        productName: '多维检测仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-08 13:00:00',
        lastReportTime: '2026-04-08 13:05:00',
        firmwareVersion: '1.0.2',
        address: '边坡点位 A',
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L1_LF_1.value',
                displayName: '裂缝量',
                group: 'measure'
              },
              {
                identifier: 'S1_ZT_1.sensor_state',
                displayName: '设备状态',
                group: 'status'
              },
              {
                identifier: 'S1_ZT_1.battery_dump_energy',
                displayName: '剩余电量',
                group: 'status'
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
          identifier: 'L1_LF_1.value',
          propertyName: 'value',
          propertyValue: '12.4',
          valueType: 'double',
          updateTime: '2026-04-08 13:05:00'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state',
          propertyName: 'sensor_state',
          propertyValue: '0',
          valueType: 'int',
          updateTime: '2026-04-08 13:05:00'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.battery_dump_energy',
          propertyName: 'battery_dump_energy',
          propertyValue: '86',
          valueType: 'int',
          updateTime: '2026-04-08 13:05:00'
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
            bindingId: 51,
            deviceCode: 'CXH15522832',
            deviceName: '多维检测仪',
            riskPointName: '边坡监测点',
            riskLevel: 'WARNING',
            metricIdentifier: 'L1_LF_1.value',
            metricName: '裂缝量',
            onlineStatus: 1,
            latestReportTime: '2026-04-08 13:05:00'
          }
        ]
      }
    });
    vi.mocked(getRiskMonitoringDetail).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        bindingId: 51,
        riskPointId: 29,
        riskPointCode: 'RP-051',
        riskPointName: '边坡监测点',
        riskLevel: 'WARNING',
        deviceId: 3301,
        deviceCode: 'CXH15522832',
        deviceName: '多维检测仪',
        productName: '多维检测仪',
        metricIdentifier: 'L1_LF_1.value',
        metricName: '裂缝量',
        currentValue: '12.4',
        monitorStatus: 'NORMAL',
        onlineStatus: 1,
        latestReportTime: '2026-04-08 13:05:00'
      }
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 3301,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-08 12:00:00', value: 12.4, filled: false }]
          },
          {
            identifier: 'S1_ZT_1.sensor_state',
            displayName: '设备状态',
            seriesType: 'status',
            buckets: [
              { time: '2026-04-08 12:00:00', value: 0, filled: false },
              { time: '2026-04-08 13:00:00', value: -1, filled: false }
            ]
          },
          {
            identifier: 'S1_ZT_1.battery_dump_energy',
            displayName: '剩余电量',
            seriesType: 'status',
            buckets: [{ time: '2026-04-08 12:00:00', value: 86, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'CXH15522832',
      rangeCode: '1d'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: 3301,
      identifiers: expect.arrayContaining(['L1_LF_1.value', 'S1_ZT_1.sensor_state', 'S1_ZT_1.battery_dump_energy']),
      rangeCode: '1d'
    }));
    expect(wrapper.text()).toContain('监测数据');
    expect(wrapper.text()).toContain('状态事件');
    expect(wrapper.text()).toContain('运行参数');
    expect(wrapper.text()).not.toContain('状态数据');
  });

  it('keeps configured laser sensor status in the property snapshot even when latest property only has value', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: '1927706037675085825',
        productId: '202603192100560258',
        deviceCode: '202018190',
        deviceName: '激光测距传感器12',
        productName: '南方测绘 监测型 激光测距仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-10 21:07:36',
        lastReportTime: '2026-04-10 21:07:36',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'value',
          propertyName: '激光测距值',
          propertyValue: '2473.72',
          valueType: 'double',
          updateTime: '2026-04-11 00:16:33'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: '202603192100560258',
        productKey: 'nf-monitor-laser-rangefinder-v1',
        productName: '南方测绘 监测型 激光测距仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'value',
                displayName: '激光测距值',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              },
              {
                identifier: 'sensor_state',
                displayName: '激光测距状态',
                group: 'status',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 110
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          productId: '202603192100560258',
          modelType: 'property',
          identifier: 'value',
          modelName: '激光测距值',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: 'mm'
          })
        },
        {
          id: 2,
          productId: '202603192100560258',
          modelType: 'property',
          identifier: 'sensor_state',
          modelName: '激光测距状态',
          dataType: 'integer',
          specsJson: null
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: '1927706037675085825',
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'value',
            displayName: '激光测距值',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-10 21:00:00', value: 2473.72, filled: false }]
          },
          {
            identifier: 'sensor_state',
            displayName: '激光测距状态',
            seriesType: 'status',
            buckets: [{ time: '2026-04-10 21:00:00', value: 0, filled: true }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: '202018190'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const displayNameColumn = wrapper.find('[data-label="属性名称"]');

    expect(displayNameColumn.exists()).toBe(true);
    expect(displayNameColumn.text()).toContain('激光测距值');
    expect(displayNameColumn.text()).toContain('激光测距状态');
  });

  it('does not use short formal aliases to override full-path runtime property names in the property snapshot', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3501,
        productId: 801,
        deviceCode: 'COLLECT-SIGNAL-001',
        deviceName: '采集终端 1 号',
        productName: '雨量采集终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-13 09:00:00',
        lastReportTime: '2026-04-13 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '现场信号读数',
          propertyValue: '-82',
          valueType: 'int',
          updateTime: '2026-04-13 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 801,
        productKey: 'collect-product-signal',
        productName: '雨量采集终端',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'signal_4g',
                displayName: '传输信号',
                group: 'status',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 10
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 31,
          productId: 801,
          modelType: 'property',
          identifier: 'signal_4g',
          modelName: '蜂窝信号强度',
          dataType: 'int',
          specsJson: JSON.stringify({
            unit: 'dBm'
          })
        }
      ]
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: 3501,
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'signal_4g',
            displayName: '传输信号',
            seriesType: 'status',
            buckets: [{ time: '2026-04-13 09:00:00', value: -82, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'COLLECT-SIGNAL-001'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const displayNameColumn = wrapper.find('[data-label="属性名称"]');

    expect(displayNameColumn.exists()).toBe(true);
    expect(displayNameColumn.text()).toContain('现场信号读数');
  });

  it('keeps trend preview empty when no manual trend metric is configured', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 3101,
        deviceCode: 'COLLECT-NO-TREND',
        deviceName: '未配置趋势的采集设备',
        productName: '雨量采集终端',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-08 11:00:00',
        lastReportTime: '2026-04-08 11:05:00',
        metadataJson: null
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
            deviceCode: 'COLLECT-NO-TREND',
            deviceName: '未配置趋势的采集设备',
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
        deviceId: 3101,
        deviceCode: 'COLLECT-NO-TREND',
        deviceName: '未配置趋势的采集设备',
        productName: '雨量采集终端',
        metricIdentifier: 'YL_1',
        metricName: '雨量',
        currentValue: '12.4',
        monitorStatus: 'NORMAL',
        onlineStatus: 1,
        latestReportTime: '2026-04-08 11:05:00'
      }
    });
    mockRoute.query = {
      deviceCode: 'COLLECT-NO-TREND'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('未配置趋势的采集设备');
    expect(wrapper.text()).toContain('当前产品未配置对象洞察重点趋势指标，请到 /products 先将正式字段加入对象洞察后再查看趋势。');
    expect(wrapper.text()).not.toContain('监测数据');
    expect(wrapper.text()).not.toContain('状态数据');
  });

  it('explains latest snapshot gaps for a single device when runtime properties are absent', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4101,
        productId: 901,
        deviceCode: 'SNAPSHOT-NO-LATEST',
        deviceName: '无快照监测点',
        productName: '普通监测设备',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 1,
        lastOnlineTime: '2026-04-18 10:00:00',
        lastReportTime: '2026-04-18 10:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 901,
        productKey: 'generic-monitor-device',
        productName: '普通监测设备',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    mockRoute.query = {
      deviceCode: 'SNAPSHOT-NO-LATEST'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(wrapper.text()).toContain('当前设备暂无最新属性快照，请检查设备上报与 latest 属性写入链路。');
  });

  it('explains collector parent snapshot and trend boundaries when only child metrics are available', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 6202,
        productId: 802,
        deviceCode: 'COLLECTOR-PARENT-EMPTY',
        deviceName: '裂缝采集器',
        productName: '南方测绘 监测型 采集器',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        nodeType: 2,
        lastOnlineTime: '2026-04-19 08:00:00',
        lastReportTime: '2026-04-19 08:00:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 802,
        productKey: 'nf-monitor-collector-v1',
        productName: '南方测绘 监测型 采集器',
        protocolCode: 'mqtt-json',
        nodeType: 2,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: []
    });
    vi.mocked(getCollectorChildInsightOverview).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        parentDeviceCode: 'COLLECTOR-PARENT-EMPTY',
        parentOnlineStatus: 1,
        childCount: 1,
        reachableChildCount: 1,
        sensorStateReportedCount: 1,
        children: [
          {
            logicalChannelCode: 'L1_LF_1',
            childDeviceCode: 'CHILD-1001',
            childDeviceName: '1# 裂缝计',
            childProductKey: 'nf-monitor-laser-rangefinder-v1',
            collectorLinkState: 'reachable',
            sensorStateValue: '0',
            lastReportTime: '2026-04-19 08:00:00',
            metrics: [
              {
                identifier: 'value',
                displayName: '裂缝量',
                propertyValue: '1.28',
                unit: 'mm',
                reportTime: '2026-04-19 08:00:00'
              }
            ]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'COLLECTOR-PARENT-EMPTY'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('当前采集器父设备未配置可展示的父设备趋势指标；子设备指标请查看子设备总览，并到 /products 为父设备或对应子产品单独配置对象洞察。');
    expect(wrapper.text()).toContain('当前采集器父设备暂无自身运行态属性快照；子设备监测值与 sensor_state 请查看子设备总览或进入子设备对象洞察。');
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
      identifiers: ['S1_ZT_1.signal_4g']
    }));
    expect(wrapper.text()).toContain('传输信号');
    expect(wrapper.text()).not.toContain('系统自定义参数');
  });

  it('matches bare telemetry history identifiers back to full-path object insight metrics', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: '1917408115385364482',
        productId: '202603192100560271',
        deviceCode: '6260370286',
        deviceName: '中海达声光报警器-1',
        productKey: 'zhd-warning-sound-light-alarm-v1',
        productName: '中海达 预警型 声光报警器',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-20 09:00:00',
        lastReportTime: '2026-04-20 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: '202603192100560271',
        productKey: 'zhd-warning-sound-light-alarm-v1',
        productName: '中海达 预警型 声光报警器',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'S1_ZT_1.sound_state',
                displayName: '声光状态',
                group: 'status',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 1
              },
              {
                identifier: 'S1_ZT_1.battery_dump_energy',
                displayName: '剩余电量',
                group: 'runtime',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 2
              }
            ]
          }
        })
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 51,
          productId: '202603192100560271',
          modelType: 'property',
          identifier: 'S1_ZT_1.sound_state',
          modelName: '声光状态',
          dataType: 'int',
          specsJson: null
        },
        {
          id: 52,
          productId: '202603192100560271',
          modelType: 'property',
          identifier: 'S1_ZT_1.battery_dump_energy',
          modelName: '剩余电量',
          dataType: 'int',
          specsJson: JSON.stringify({
            unit: '%'
          })
        }
      ]
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'S1_ZT_1.sound_state',
          propertyName: '声光状态',
          propertyValue: '1',
          valueType: 'int',
          updateTime: '2026-04-20 09:05:00'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.battery_dump_energy',
          propertyName: '剩余电量',
          propertyValue: '86',
          valueType: 'int',
          unit: '%',
          updateTime: '2026-04-20 09:05:00'
        }
      ]
    });
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
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: '1917408115385364482',
        rangeCode: '1d',
        bucket: 'hour',
        points: [
          {
            identifier: 'sound_state',
            displayName: '声光状态',
            seriesType: 'status',
            buckets: [{ time: '2026-04-20 09:00:00', value: 1, filled: false }]
          },
          {
            identifier: 'battery_dump_energy',
            displayName: '剩余电量',
            seriesType: 'status',
            buckets: [{ time: '2026-04-20 09:00:00', value: 86, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: '6260370286'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).toHaveBeenCalledWith(expect.objectContaining({
      deviceId: '1917408115385364482',
      identifiers: ['S1_ZT_1.sound_state', 'S1_ZT_1.battery_dump_energy']
    }));
    expect(wrapper.text()).toContain('状态事件');
    expect(wrapper.text()).toContain('运行参数');
    expect(wrapper.text()).toContain('声光状态');
    expect(wrapper.text()).toContain('剩余电量');
    expect(wrapper.text()).not.toContain('当前范围暂无可展示的 TDengine 趋势数据');
  });

  it('uses snapshot-first metrics and removes secondary metric panels for multidimensional devices', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: '1983438980179222530',
        productId: '202603192100560252',
        deviceCode: 'CXH15522832',
        deviceName: '多维检测仪',
        productName: '中海达 监测型 多维位移监测仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-09 10:48:03',
        lastReportTime: '2026-04-09 10:48:03',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_LF_1.value',
          propertyName: 'value',
          propertyValue: '1224.37',
          valueType: 'double',
          updateTime: '2026-04-09 10:48:28'
        },
        {
          id: 2,
          identifier: 'L1_QJ_1.angle',
          propertyName: '1号倾角测点angle',
          propertyValue: '-6.03',
          valueType: 'double',
          updateTime: '2026-04-09 10:46:34'
        },
        {
          id: 3,
          identifier: 'L1_JS_1.gX',
          propertyName: '1号加速度测点gX',
          propertyValue: '0.48',
          valueType: 'double',
          updateTime: '2026-04-09 10:46:32'
        }
      ]
    });
    vi.mocked(getRiskMonitoringList).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        total: 3,
        pageNum: 1,
        pageSize: 50,
        records: [
          {
            bindingId: 71,
            deviceCode: 'CXH15522832',
            deviceName: '多维检测仪',
            riskPointName: 'G6京藏高速K1623+400滑坡',
            riskLevel: 'blue',
            metricIdentifier: 'gX',
            metricName: 'X轴加速度',
            onlineStatus: 1,
            latestReportTime: '2026-04-09 10:48:03'
          }
        ]
      }
    });
    vi.mocked(getRiskMonitoringDetail).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        bindingId: 71,
        riskPointId: 71,
        riskPointCode: 'RP-HW-SLOPE-045',
        riskPointName: 'G6京藏高速K1623+400滑坡',
        riskLevel: 'blue',
        deviceId: '1983438980179222530',
        deviceCode: 'CXH15522832',
        deviceName: '多维检测仪',
        productName: '中海达 监测型 多维位移监测仪',
        metricIdentifier: 'gX',
        metricName: 'X轴加速度',
        currentValue: '0.48',
        monitorStatus: 'NO_DATA',
        onlineStatus: 1,
        latestReportTime: '2026-04-09 10:48:03'
      }
    });
    vi.mocked(getTelemetryHistoryBatch).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        deviceId: '1983438980179222530',
        rangeCode: '7d',
        bucket: 'day',
        points: [
          {
            identifier: 'L1_LF_1.value',
            displayName: '裂缝量',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-09 00:00:00', value: 1224.37, filled: false }]
          },
          {
            identifier: 'L1_QJ_1.angle',
            displayName: '水平面夹角',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-09 00:00:00', value: -6.03, filled: false }]
          },
          {
            identifier: 'L1_JS_1.gX',
            displayName: 'X轴加速度',
            seriesType: 'measure',
            buckets: [{ time: '2026-04-09 00:00:00', value: 0.48, filled: false }]
          }
        ]
      }
    });
    mockRoute.query = {
      deviceCode: 'CXH15522832'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('裂缝量');
    expect(wrapper.text()).toContain('水平面夹角');
    expect(wrapper.text()).toContain('X轴加速度');
    expect(wrapper.text()).not.toContain('核心指标');
    expect(wrapper.text()).not.toContain('仅使用中文业务名称展示当前最关心的监测值、状态值和关键状态项。');
    expect(wrapper.text()).not.toContain('为后续湿度、4G 信号等扩展项预留统一展示位。');
    expect(wrapper.text()).not.toContain('当前设备暂无已接入的系统自定义参数。');
    expect(wrapper.findAll('.metric-card-stub')).toHaveLength(0);
  });

  it('keeps the range selector inside the trend panel and requeries telemetry when range changes', async () => {
    vi.mocked(productApi.getProductById).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: {
        id: 501,
        productKey: 'muddy-water-product',
        productName: '宏观现象监测设备泥水位',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: JSON.stringify({
          objectInsight: {
            customMetrics: [
              {
                identifier: 'L4_NW_1',
                displayName: '泥水位高程',
                group: 'measure',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 1
              },
              {
                identifier: 'S1_ZT_1.sensor_state.L4_NW_1',
                displayName: '传感器在线状态',
                group: 'status',
                includeInTrend: true,
                includeInExtension: false,
                enabled: true,
                sortNo: 1
              }
            ]
          }
        })
      }
    });
    mockRoute.query = {
      deviceCode: 'SK00EB0D1308313'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    expect(wrapper.findAll('[data-testid^="insight-range-"]')).toHaveLength(0);
    expect(wrapper.get('[data-testid="trend-panel-range-1d"]').text()).toBe('近一天');
    expect(wrapper.get('[data-testid="trend-panel-range-365d"]').text()).toBe('近一年');

    await wrapper.get('[data-testid="trend-panel-range-365d"]').trigger('click');
    await flushPromises();
    await flushPromises();

    expect(getTelemetryHistoryBatch).toHaveBeenCalledTimes(2);
    expect(getTelemetryHistoryBatch).toHaveBeenLastCalledWith(expect.objectContaining({
      deviceId: 2001,
      rangeCode: '365d',
      fillPolicy: 'ZERO'
    }));
    expect(mockRouter.replace).toHaveBeenLastCalledWith(expect.objectContaining({
      query: expect.objectContaining({
        deviceCode: 'SK00EB0D1308313',
        rangeCode: '365d'
      })
    }));
  });

  it('deduplicates short aliases when a unique full-path snapshot field exists', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4101,
        productId: 901,
        deviceCode: '15522761',
        deviceName: '倾角仪 15522761',
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-21 09:00:00',
        lastReportTime: '2026-04-21 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'AZI',
          propertyName: 'AZI',
          propertyValue: '12.8',
          valueType: 'double',
          unit: '°',
          updateTime: '2026-04-21 09:05:00'
        },
        {
          id: 2,
          identifier: 'L1_QJ_1.AZI',
          propertyName: '1号倾角测点方位角',
          propertyValue: '12.8',
          valueType: 'double',
          unit: '°',
          updateTime: '2026-04-21 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 901,
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 101,
          productId: 901,
          modelType: 'property',
          identifier: 'L1_QJ_1.AZI',
          modelName: '方位角',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: '°'
          })
        }
      ]
    });
    mockRoute.query = {
      deviceCode: '15522761'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const identifierValues = wrapper
      .findAll('.standard-table-text-column-stub[data-label="标识符"] .standard-table-text-column-stub__value')
      .map((node) => node.text().trim())
      .filter(Boolean);
    const displayNameValues = wrapper
      .findAll('.standard-table-text-column-stub[data-label="属性名称"] .standard-table-text-column-stub__value')
      .map((node) => node.text().trim())
      .filter(Boolean);

    expect(identifierValues).toEqual(['L1_QJ_1.AZI']);
    expect(displayNameValues).toEqual(['1号倾角测点方位角']);
  });

  it('hides a bare logical-channel status alias when the formal sensor_state field exists', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4102,
        productId: 901,
        deviceCode: '15522761',
        deviceName: '倾角仪 15522761',
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-21 09:00:00',
        lastReportTime: '2026-04-21 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_QJ_1',
          propertyName: '1号倾角测点传感器状态',
          propertyValue: '0',
          valueType: 'int',
          updateTime: '2026-04-21 09:05:00'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.L1_QJ_1',
          propertyName: '1号倾角测点传感器状态',
          propertyValue: '0',
          valueType: 'int',
          updateTime: '2026-04-21 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 901,
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 101,
          productId: 901,
          modelType: 'property',
          identifier: 'S1_ZT_1.sensor_state.L1_QJ_1',
          modelName: '1号倾角测点传感器状态',
          dataType: 'int',
          specsJson: null
        }
      ]
    });
    mockRoute.query = {
      deviceCode: '15522761'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const identifierValues = wrapper
      .findAll('.standard-table-text-column-stub[data-label="标识符"] .standard-table-text-column-stub__value')
      .map((node) => node.text().trim())
      .filter(Boolean);

    expect(identifierValues).toEqual(['S1_ZT_1.sensor_state.L1_QJ_1']);
  });

  it('opens the product contract formal-field editor from the snapshot row', async () => {
    vi.mocked(getDeviceByCode).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 4102,
        productId: 901,
        deviceCode: '15522761',
        deviceName: '倾角仪 15522761',
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        onlineStatus: 1,
        protocolCode: 'mqtt-json',
        lastOnlineTime: '2026-04-21 09:00:00',
        lastReportTime: '2026-04-21 09:05:00',
        metadataJson: null
      }
    });
    vi.mocked(getDeviceProperties).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 1,
          identifier: 'L1_QJ_1.AZI',
          propertyName: '1号倾角测点方位角',
          propertyValue: '12.8',
          valueType: 'double',
          unit: '°',
          updateTime: '2026-04-21 09:05:00'
        }
      ]
    });
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
    vi.mocked(productApi.getProductById).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: {
        id: 901,
        productKey: 'zhd-monitor-tiltmeter-v1',
        productName: '中海达 监测型 倾角仪',
        protocolCode: 'mqtt-json',
        nodeType: 1,
        metadataJson: null
      }
    });
    vi.mocked(productApi.listProductModels).mockResolvedValueOnce({
      code: 200,
      msg: 'success',
      data: [
        {
          id: 101,
          productId: 901,
          modelType: 'property',
          identifier: 'L1_QJ_1.AZI',
          modelName: '方位角',
          dataType: 'double',
          specsJson: JSON.stringify({
            unit: '°'
          })
        }
      ]
    });
    mockRoute.query = {
      deviceCode: '15522761'
    };

    const wrapper = mountView();

    await flushPromises();
    await flushPromises();

    const editButton = wrapper.find('[data-testid="property-snapshot-edit-L1_QJ_1_AZI"]');
    expect(editButton.exists()).toBe(true);

    await editButton.trigger('click');

    expect(mockRouter.push).toHaveBeenCalledWith({
      path: '/products/901/contracts',
      query: {
        modelIdentifier: 'L1_QJ_1.AZI',
        renameModel: '1',
        source: 'insight'
      }
    });
  });
});
