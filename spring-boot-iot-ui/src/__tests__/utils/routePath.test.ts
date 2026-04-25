import { describe, expect, it } from 'vitest';

import { normalizeOptionalRoutePath, normalizeRoutePath, routePathMatchesPattern } from '@/utils/routePath';

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

  it('matches parameterized route patterns against concrete paths', () => {
    expect(routePathMatchesPattern('/products/:productId/contracts', '/products/202603192100560252/contracts')).toBe(true);
    expect(routePathMatchesPattern('/business-acceptance/results/:runId', '/business-acceptance/results/RUN-001')).toBe(true);
    expect(routePathMatchesPattern('/products/:productId/contracts', '/products/202603192100560252/releases')).toBe(false);
  });
});
