import { describe, expect, it } from 'vitest';

import { getAlarmLevelTagType, getAlarmLevelText, normalizeAlarmLevel } from '@/utils/alarmLevel';

describe('alarmLevel utils', () => {
  it('normalizes legacy values into four-color alarm levels', () => {
    expect(normalizeAlarmLevel('critical')).toBe('red');
    expect(normalizeAlarmLevel('warning')).toBe('orange');
    expect(normalizeAlarmLevel('info')).toBe('blue');
    expect(normalizeAlarmLevel('red')).toBe('red');
    expect(normalizeAlarmLevel('yellow')).toBe('yellow');
  });

  it('renders four-color labels and compatible tag types', () => {
    expect(getAlarmLevelText('red')).toBe('红色');
    expect(getAlarmLevelText('orange')).toBe('橙色');
    expect(getAlarmLevelText('yellow')).toBe('黄色');
    expect(getAlarmLevelText('blue')).toBe('蓝色');
    expect(getAlarmLevelTagType('orange')).toBe('warning');
    expect(getAlarmLevelTagType('red')).toBe('danger');
    expect(getAlarmLevelTagType('blue')).toBe('info');
  });
});
