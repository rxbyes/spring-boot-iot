import { describe, expect, it } from 'vitest';

import { getRiskPointLevelText, normalizeRiskPointLevel } from '@/utils/riskPointLevel';

describe('riskPointLevel utils', () => {
  it('normalizes archive grades', () => {
    expect(normalizeRiskPointLevel('level_1')).toBe('level_1');
    expect(normalizeRiskPointLevel('LEVEL_2')).toBe('level_2');
    expect(normalizeRiskPointLevel(' level_3 ')).toBe('level_3');
  });

  it('renders archive-grade labels', () => {
    expect(getRiskPointLevelText('level_1')).toBe('一级风险点');
    expect(getRiskPointLevelText('level_2')).toBe('二级风险点');
    expect(getRiskPointLevelText('level_3')).toBe('三级风险点');
  });
});
