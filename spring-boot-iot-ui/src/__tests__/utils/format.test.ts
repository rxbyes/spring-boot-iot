import { afterEach, describe, expect, it, vi } from 'vitest';

import { formatDateTime, formatMessageTraceReportTime } from '@/utils/format';

describe('formatDateTime', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('uses Asia/Shanghai when formatting explicit UTC timestamps', () => {
    const formatterSpy = vi.spyOn(Intl, 'DateTimeFormat');

    const result = formatDateTime('2026-04-05T10:50:35Z');

    expect(result).toBe('2026/04/05 18:50:35');
    expect(formatterSpy).toHaveBeenCalledWith('zh-CN', expect.objectContaining({
      timeZone: 'Asia/Shanghai',
      hour12: false
    }));
  });

  it('keeps naive timestamps as local display values', () => {
    expect(formatDateTime('2026-04-05 10:50:35')).toBe('2026/04/05 10:50:35');
  });

  it('normalizes naive ISO timestamps without reinterpreting their clock time', () => {
    expect(formatDateTime('2026-04-05T10:50:35')).toBe('2026/04/05 10:50:35');
  });

  it('realigns message trace report time when it is shifted eight hours ahead of create time', () => {
    expect(formatMessageTraceReportTime('2026-04-05 19:29:12', '2026-04-05 11:29:03')).toBe('2026/04/05 11:29:12');
  });

  it('realigns message trace report time when explicit UTC parsing drifts eight hours ahead of create time', () => {
    expect(formatMessageTraceReportTime('2026-04-05T11:29:12Z', '2026-04-05 11:29:03')).toBe('2026/04/05 11:29:12');
  });
});
