import { describe, expect, it } from 'vitest';

import {
  DEFAULT_INSIGHT_RANGE,
  INSIGHT_RANGE_OPTIONS,
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
    expect(profile.trendGroups.map((item) => item.title)).toEqual(['监测数据', '状态事件', '运行参数']);
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('相对湿度');
    expect(profile.extensionParameters.map((item) => item.displayName)).toContain('4G 信号强度');
  });

  it('builds telemetry batch request with default one-day range and zero fill', () => {
    const profile = getInsightCapabilityProfile({ deviceCode: 'SK00EB0D1308313' });
    const request = buildInsightHistoryRequest(2001, profile, DEFAULT_INSIGHT_RANGE);

    expect(request.deviceId).toBe(2001);
    expect(request.rangeCode).toBe('1d');
    expect(request.fillPolicy).toBe('ZERO');
    expect(request.identifiers).toEqual([]);
  });

  it('preserves snowflake device ids instead of coercing them to unsafe numbers', () => {
    const profile = getInsightCapabilityProfile({ deviceCode: 'SJ11E6148716807A' });
    const request = buildInsightHistoryRequest('1987747920257933314', profile, DEFAULT_INSIGHT_RANGE);

    expect(request.deviceId).toBe('1987747920257933314');
  });

  it('keeps object insight ranges focused on day week month and year', () => {
    expect(DEFAULT_INSIGHT_RANGE).toBe('1d');
    expect(
      INSIGHT_RANGE_OPTIONS.map((item) => item.value)
    ).toEqual(['1d', '7d', '30d', '365d']);
  });

  it('prioritizes multidimensional device snapshot metrics with fixed chinese names', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'CXH15522832',
      productName: '中海达 监测型 多维位移监测仪',
      properties: [
        {
          id: 1,
          identifier: 'L1_LF_1.value',
          propertyName: 'value',
          propertyValue: '1224.37',
          valueType: 'double'
        },
        {
          id: 2,
          identifier: 'L1_QJ_1.angle',
          propertyName: '1号倾角测点angle',
          propertyValue: '-6.03',
          valueType: 'double'
        },
        {
          id: 3,
          identifier: 'L1_JS_1.gX',
          propertyName: '1号加速度测点gX',
          propertyValue: '0.48',
          valueType: 'double'
        },
        {
          id: 4,
          identifier: 'L1_QJ_1.X',
          propertyName: '1号倾角测点X',
          propertyValue: '3.19',
          valueType: 'double'
        }
      ]
    });

    expect(profile.heroMetrics.map((item) => item.displayName)).toEqual([
      '裂缝量',
      '水平面夹角',
      'X轴加速度'
    ]);
    expect(profile.trendGroups.find((item) => item.key === 'measure')?.identifiers).toEqual([]);
  });

  it('builds collect-device snapshot metrics without auto-filling trend groups', () => {
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
    expect(profile.trendGroups.find((item) => item.key === 'measure')?.identifiers).toEqual([]);
    expect(profile.trendGroups.find((item) => item.key === 'statusEvent')?.identifiers).toEqual([]);
    expect(profile.trendGroups.find((item) => item.key === 'runtime')?.identifiers).toEqual([]);
    expect(profile.historyIdentifiers).toEqual([]);
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
    expect(profile.historyIdentifiers).toEqual([]);
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

  it('does not rewrite short configured identifiers to full-path runtime identifiers', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-ALIAS-001',
      productName: '雨量采集终端',
      metadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'signal_4g',
              displayName: '传输信号',
              group: 'status',
              includeInTrend: true,
              includeInExtension: false
            }
          ]
        }
      }),
      properties: [
        {
          id: 1,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-82',
          valueType: 'int'
        }
      ]
    });

    expect(profile.historyIdentifiers).toEqual(['signal_4g']);
    expect(profile.customMetrics.find((item) => item.displayName === '传输信号')?.identifier).toBe('signal_4g');
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

  it('prioritizes enabled product-level focus metrics and excludes disabled ones from trend preview', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-FOCUS-001',
      productName: '雨量采集终端',
      productMetadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.signal_4g',
              displayName: '传输信号',
              group: 'status',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true,
              sortNo: 1
            },
            {
              identifier: 'YL_1',
              displayName: '重点雨量',
              group: 'measure',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true,
              sortNo: 2
            },
            {
              identifier: 'S1_ZT_1.humidity',
              displayName: '相对湿度',
              group: 'status',
              includeInTrend: true,
              includeInExtension: false,
              enabled: false,
              sortNo: 3
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

    expect(profile.trendGroups.find((item) => item.key === 'measure')?.identifiers[0]).toBe('YL_1');
    expect(profile.trendGroups.find((item) => item.key === 'runtime')?.identifiers[0]).toBe('S1_ZT_1.signal_4g');
    expect(profile.trendGroups.find((item) => item.key === 'runtime')?.identifiers).not.toContain('S1_ZT_1.humidity');
    expect(profile.historyIdentifiers).not.toContain('S1_ZT_1.humidity');
  });

  it('aligns metadata custom metric identifiers to runtime property casing before building history identifiers', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'SK11EB0D1308100AZ',
      productName: '南方测绘 监测型 多维位移监测仪',
      productMetadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'l1_js_1.gx',
              displayName: '1号加速度测点gX',
              group: 'measure',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true
            },
            {
              identifier: 'l1_lf_1.value',
              displayName: '裂缝值',
              group: 'measure',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true
            },
            {
              identifier: 's1_zt_1.signal_4g',
              displayName: '4G 信号',
              group: 'runtime',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true
            }
          ]
        }
      }),
      properties: [
        {
          id: 1,
          identifier: 'L1_JS_1.gX',
          propertyName: 'X轴加速度',
          propertyValue: '0.1667',
          valueType: 'double'
        },
        {
          id: 2,
          identifier: 'L1_LF_1.value',
          propertyName: '裂缝值',
          propertyValue: '0.2136',
          valueType: 'double'
        },
        {
          id: 3,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G信号',
          propertyValue: '-55',
          valueType: 'int'
        }
      ]
    });

    expect(profile.historyIdentifiers).toEqual(
      expect.arrayContaining(['L1_JS_1.gX', 'L1_LF_1.value', 'S1_ZT_1.signal_4g'])
    );
    expect(profile.historyIdentifiers).not.toEqual(
      expect.arrayContaining(['l1_js_1.gx', 'l1_lf_1.value', 's1_zt_1.signal_4g'])
    );
  });

  it('keeps short configured runtime custom metrics unchanged when the latest property uses a full-path identifier', () => {
    const profile = getInsightCapabilityProfile({
      deviceCode: 'COLLECT-CANONICAL-001',
      productName: '雨量采集终端',
      productMetadataJson: JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'signal_4g',
              displayName: '传输信号',
              group: 'runtime',
              includeInTrend: true,
              includeInExtension: false,
              enabled: true
            }
          ]
        }
      }),
      properties: [
        {
          id: 1,
          identifier: 'S1_ZT_1.signal_4g',
          propertyName: '4G 信号强度',
          propertyValue: '-81',
          valueType: 'int'
        }
      ]
    });

    expect(profile.historyIdentifiers).toEqual(['signal_4g']);
    expect(profile.customMetrics.find((item) => item.displayName === '传输信号')?.identifier).toBe('signal_4g');
  });
});
