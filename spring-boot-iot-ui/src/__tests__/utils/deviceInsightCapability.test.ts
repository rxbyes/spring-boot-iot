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
});
