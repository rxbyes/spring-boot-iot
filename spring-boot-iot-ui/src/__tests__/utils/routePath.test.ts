import { describe, expect, it } from 'vitest';

import { normalizeOptionalRoutePath, normalizeRoutePath } from '@/utils/routePath';

describe('routePath utils', () => {
  it('normalizes route path with leading slash and trims tail slash', () => {
    expect(normalizeRoutePath('user')).toBe('/user');
    expect(normalizeRoutePath('/user/')).toBe('/user');
    expect(normalizeRoutePath('///user///')).toBe('/user');
    expect(normalizeRoutePath('')).toBe('/');
  });

  it('returns null for empty optional route path', () => {
    expect(normalizeOptionalRoutePath('')).toBeNull();
    expect(normalizeOptionalRoutePath('   ')).toBeNull();
    expect(normalizeOptionalRoutePath('/role/')).toBe('/role');
  });
});
