import { describe, expect, it } from 'vitest';

import { compareIdDesc, normalizeOptionalId, sameId } from '@/utils/id';

describe('id utilities', () => {
  it('preserves long string ids without numeric coercion', () => {
    const longId = '202603192100560260';

    expect(normalizeOptionalId(longId)).toBe(longId);
    expect(normalizeOptionalId(` ${longId} `)).toBe(longId);
  });

  it('treats ids with the same text value as equal', () => {
    expect(sameId('202603192100560260', '202603192100560260')).toBe(true);
    expect(sameId('202603192100560260', '202603192100560258')).toBe(false);
  });

  it('drops empty optional ids', () => {
    expect(normalizeOptionalId('')).toBeUndefined();
    expect(normalizeOptionalId(null)).toBeUndefined();
    expect(normalizeOptionalId(undefined)).toBeUndefined();
  });

  it('compares long numeric ids without converting them to numbers', () => {
    expect(compareIdDesc('202604280000000201', '202604280000000202')).toBeGreaterThan(0);
    expect(compareIdDesc('202604280000000202', '202604280000000201')).toBeLessThan(0);
  });
});
