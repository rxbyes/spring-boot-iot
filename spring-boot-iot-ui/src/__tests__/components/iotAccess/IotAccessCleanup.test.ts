import { existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

const root = resolve(import.meta.dirname, '../../..');

describe('iot access cleanup', () => {
  it('removes discarded result/filter helper components', () => {
    expect(existsSync(resolve(root, 'components/iotAccess/IotAccessResultSection.vue'))).toBe(false);
    expect(existsSync(resolve(root, 'components/iotAccess/IotAccessFilterBar.vue'))).toBe(false);
  });
});
