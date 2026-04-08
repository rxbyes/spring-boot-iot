import { describe, expect, it } from 'vitest';

import {
  DEFAULT_INSIGHT_RANGE,
  buildInsightHistoryRequest,
  getInsightCapabilityProfile
} from '@/utils/deviceInsightCapability';

describe('deviceInsightCapability', () => {
  it('matches muddy-water sample device with chinese metrics only', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'SK00EB0D1308313',
      productName: '宏观现象监测设备泥水位'
    });

    expect(profile.heroMetrics.map((item) => item.displayName)).toEqual([
      '泥水位高程',
      '传感器在线状态',
      '剩余电量'
    ]);
    expect(profile.trendGroups.map((item) => item.title)).toEqual(['监测数据', '状态数据']);
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('相对湿度');
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('4G 信号强度');
  });

  it('builds telemetry batch request with default weekly range and zero fill', () => {
    const profile = getInsightCapabilityProfile({ deviceCode: 'SK00EB0D1308313' });
    const request = buildInsightHistoryRequest(2001, profile, DEFAULT_INSIGHT_RANGE);

    expect(request.deviceId).toBe(2001);
    expect(request.rangeCode).toBe('7d');
    expect(request.fillPolicy).toBe('ZERO');
    expect(request.identifiers).toContain('L4_NW_1');
    expect(request.identifiers).toContain('S1_ZT_1.sensor_state.L4_NW_1');
    expect(request.identifiers).toContain('S1_ZT_1.battery_dump_energy');
  });

  it('builds collect-device profile from runtime properties instead of falling back to empty generic template', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-001',
      productName: '雨量采集终端',
      properties: [
        {
          id: 1,
          identifier: 'YL_1',
          propertyName: '雨量',
          propertyValue: '12.4',
          valueType: 'double'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.YL_1',
          propertyName: '采集通道在线状态',
          propertyValue: '1',
          valueType: 'int'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.humidity',
          propertyName: '相对湿度',
          propertyValue: '75',
          valueType: 'double'
        },
        {
          id: 4,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-82',
          valueType: 'int'
        }
      ]
    });

    expect(profile.objectType).toBe('collect');
    expect(profile.heroMetrics.map((item) => item.displayName)).toEqual([
      '雨量',
      '采集通道在线状态',
      '4G 信号强度'
    ]);
    expect(profile.trendGroups.find((item) => item.key === 'measure')?.identifiers).toContain('YL_1');
    expect(profile.trendGroups.find((item) => item.key === 'status')?.identifiers).toContain('S1_ZT_1.humidity');
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('相对湿度');
  });

  it('builds warning-device profile with warning-centric metrics and dynamic status extensions', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'WARN-001',
      productName: '边坡预警终端',
      metricName: '预警广播状态',
      properties: [
        {
          id: 11,
          identifier: 'warning_level',
          propertyName: '预警等级',
          propertyValue: '2',
          valueType: 'int'
        },
        {
          id: 12,
          identifier: 'S1_ZT_1.sensor_state.warning_level',
          propertyName: '预警通道在线状态',
          propertyValue: '1',
          valueType: 'int'
        },
        {
          id: 13,
          identifier: 'S1_ZT_1.battery_dump_energy',
          propertyName: '剩余电量',
          propertyValue: '63',
          valueType: 'int'
        },
        {
          id: 14,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-76',
          valueType: 'int'
        }
      ]
    });

    expect(profile.objectType).toBe('warning');
    expect(profile.heroMetrics.map((item) => item.displayName)).toEqual([
      '预警等级',
      '预警通道在线状态',
      '剩余电量'
    ]);
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('4G 信号强度');
    expect(profile.historyIdentifiers).toContain('warning_level');
    expect(profile.historyIdentifiers).toContain('S1_ZT_1.signal_4g');
  });

  it('merges metadataJson custom metrics and analysis templates into the capability profile', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-002',
      productName: '雨量采集终端',
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
              displayName: '传输信号',
              group: 'status',
              analysisTemplate: '{{label}}当前为{{value}}，用于判断设备回传链路稳定性。'
            }
          ]
        }
      }),
      properties: [
        {
          id: 1,
          identifier: 'YL_1',
          propertyName: '雨量',
          propertyValue: '9.2',
          valueType: 'double'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.YL_1',
          propertyName: '采集通道在线状态',
          propertyValue: '1',
          valueType: 'int'
        }
      ]
    });

    expect(profile.extensionParameters.map((item) => item.displayName)).toEqual(
      expect.arrayContaining(['相对湿度', '传输信号'])
    );
    expect(profile.historyIdentifiers).toEqual(
      expect.arrayContaining(['S1_ZT_1.humidity', 'S1_ZT_1.signal_4g'])
    );
    expect(
      profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.humidity')?.analysisTemplate
    ).toBe('{{label}}当前为{{value}}，可辅助判断现场环境湿润程度。');
    expect(
      profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.signal_4g')?.displayName
    ).toBe('传输信号');
  });

  it('prefers device metadata over product metadata while preserving product-level fallback metrics', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-003',
      productName: '雨量采集终端',
      productMetadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.signal_4g',
              displayName: '产品侧传输信号',
              group: 'status',
              analysisTemplate: '{{label}}来自产品正式配置。'
            },
            {
              identifier: 'S1_ZT_1.humidity',
              displayName: '产品侧湿度',
              group: 'status',
              analysisTemplate: '{{label}}来自产品正式配置。'
            }
          ]
        }
      }),
      deviceMetadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.signal_4g',
              displayName: '设备侧传输信号',
              group: 'status',
              analysisTemplate: '{{label}}来自设备独立配置。'
            }
          ]
        }
      }),
      properties: [
        {
          id: 1,
          identifier: 'YL_1',
          propertyName: '雨量',
          propertyValue: '5.6',
          valueType: 'double'
        },
        {
          id: 2,
          identifier: 'S1_ZT_1.sensor_state.YL_1',
          propertyName: '采集通道在线状态',
          propertyValue: '1',
          valueType: 'int'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-79',
          valueType: 'int'
        },
        {
          id: 4,
          identifier: 'S1_ZT_1.humidity',
          propertyName: '相对湿度',
          propertyValue: '72',
          valueType: 'double'
        }
      ]
    });

    expect(
      profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.signal_4g')?.displayName
    ).toBe('设备侧传输信号');
    expect(
      profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.signal_4g')?.analysisTemplate
    ).toBe('{{label}}来自设备独立配置。');
    expect(
      profile.customMetrics.find((item) => item.identifier === 'S1_ZT_1.humidity')?.displayName
    ).toBe('产品侧湿度');
    expect(profile.historyIdentifiers).toEqual(
      expect.arrayContaining(['S1_ZT_1.signal_4g', 'S1_ZT_1.humidity'])
    );
  });
});
