import { describe, expect, it } from 'vitest';

import { pickPrimaryBinding, resolveInsightObjectType } from '@/utils/deviceInsight';

describe('deviceInsight utils', () => {
  it('prefers latest report time when selecting a primary binding', () => {
    const selected = pickPrimaryBinding([
      {
        bindingId: 1,
        latestReportTime: '2026-04-01 09:00:00',
        onlineStatus: 0,
        riskLevel: 'INFO'
      },
      {
        bindingId: 2,
        latestReportTime: '2026-04-01 10:00:00',
        onlineStatus: 1,
        riskLevel: 'WARNING'
      }
    ]);

    expect(selected?.bindingId).toBe(2);
  });

  it('prefers online bindings when report time is tied', () => {
    const selected = pickPrimaryBinding([
      {
        bindingId: 1,
        latestReportTime: '2026-04-01 10:00:00',
        onlineStatus: 0,
        riskLevel: 'WARNING'
      },
      {
        bindingId: 2,
        latestReportTime: '2026-04-01 10:00:00',
        onlineStatus: 1,
        riskLevel: 'INFO'
      }
    ]);

    expect(selected?.bindingId).toBe(2);
  });

  it('classifies warning devices by metric or product keywords', () => {
    expect(
      resolveInsightObjectType({
        metricIdentifier: 'warningLightState',
        metricName: '预警灯状态',
        productName: '边坡预警终端'
      })
    ).toBe('warning');
  });

  it('classifies collection devices by rainfall and water level keywords', () => {
    expect(
      resolveInsightObjectType({
        metricIdentifier: 'rainfall',
        metricName: '雨量',
        productName: '雨量采集终端'
      })
    ).toBe('collect');
  });
});
