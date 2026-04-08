import type { InsightRangeCode, TelemetryHistoryBatchRequest } from '@/api/telemetry';

export interface InsightMetricDefinition {
  identifier: string;
  displayName: string;
  group: 'measure' | 'status';
}

export interface InsightTrendGroupDefinition {
  key: 'measure' | 'status';
  title: string;
  identifiers: string[];
}

export interface InsightExtensionParameterDefinition {
  parameterKey: string;
  identifier: string;
  displayName: string;
}

export interface InsightCapabilityProfile {
  key: string;
  objectType: 'detect' | 'warning' | 'collect' | 'generic';
  heroMetrics: InsightMetricDefinition[];
  trendGroups: InsightTrendGroupDefinition[];
  extensionParameters: InsightExtensionParameterDefinition[];
  historyIdentifiers: string[];
}

export const INSIGHT_RANGE_OPTIONS = [
  { label: '近一天', value: '1d' },
  { label: '近一周', value: '7d' },
  { label: '近一月', value: '30d' },
  { label: '近一季度', value: '90d' },
  { label: '近一年', value: '365d' }
] as const;

export const DEFAULT_INSIGHT_RANGE: InsightRangeCode = '7d';

const MUDDY_WATER_PROFILE: InsightCapabilityProfile = {
  key: 'muddy-water-level',
  objectType: 'detect',
  heroMetrics: [
    { identifier: 'L4_NW_1', displayName: '泥水位高程', group: 'measure' },
    { identifier: 'S1_ZT_1.sensor_state.L4_NW_1', displayName: '传感器在线状态', group: 'status' },
    { identifier: 'S1_ZT_1.battery_dump_energy', displayName: '剩余电量', group: 'status' }
  ],
  trendGroups: [
    { key: 'measure', title: '监测数据', identifiers: ['L4_NW_1'] },
    {
      key: 'status',
      title: '状态数据',
      identifiers: ['S1_ZT_1.sensor_state.L4_NW_1', 'S1_ZT_1.battery_dump_energy']
    }
  ],
  extensionParameters: [
    { parameterKey: 'humidity', identifier: 'S1_ZT_1.humidity', displayName: '相对湿度' },
    { parameterKey: 'signal_4g', identifier: 'S1_ZT_1.signal_4g', displayName: '4G 信号强度' }
  ],
  historyIdentifiers: [
    'L4_NW_1',
    'S1_ZT_1.sensor_state.L4_NW_1',
    'S1_ZT_1.battery_dump_energy',
    'S1_ZT_1.humidity',
    'S1_ZT_1.signal_4g'
  ]
};

const GENERIC_MONITORING_PROFILE: InsightCapabilityProfile = {
  key: 'generic-monitoring',
  objectType: 'detect',
  heroMetrics: [],
  trendGroups: [
    { key: 'measure', title: '监测数据', identifiers: [] },
    { key: 'status', title: '状态数据', identifiers: [] }
  ],
  extensionParameters: [],
  historyIdentifiers: []
};

export function getInsightCapabilityProfile(source: {
  deviceCode?: string | null;
  productName?: string | null;
}): InsightCapabilityProfile {
  const deviceCode = source.deviceCode?.trim();
  const productName = source.productName?.trim() ?? '';
  if (deviceCode === 'SK00EB0D1308313' || /泥水位/.test(productName)) {
    return MUDDY_WATER_PROFILE;
  }
  return GENERIC_MONITORING_PROFILE;
}

export function buildInsightHistoryRequest(
  deviceId: number | string,
  profile: InsightCapabilityProfile,
  rangeCode: InsightRangeCode = DEFAULT_INSIGHT_RANGE
): TelemetryHistoryBatchRequest {
  return {
    deviceId: Number(deviceId),
    identifiers: profile.historyIdentifiers,
    rangeCode,
    fillPolicy: 'ZERO'
  };
}
