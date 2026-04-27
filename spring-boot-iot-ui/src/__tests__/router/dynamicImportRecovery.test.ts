import { describe, expect, it, vi } from 'vitest';

import {
  DYNAMIC_IMPORT_RELOAD_FLAG,
  handleDynamicImportRouteError,
  isDynamicImportLoadError
} from '../../router/dynamicImportRecovery';

describe('dynamic import route recovery', () => {
  it('detects browser dynamic import load failures', () => {
    expect(isDynamicImportLoadError(new TypeError('Failed to fetch dynamically imported module: /src/views/DeviceWorkbenchView.vue'))).toBe(true);
    expect(isDynamicImportLoadError(new Error('Loading chunk DeviceWorkbenchView failed.'))).toBe(true);
    expect(isDynamicImportLoadError(new Error('ordinary navigation failure'))).toBe(false);
  });

  it('reloads once for a dynamic import load failure', () => {
    const replace = vi.fn();
    const reload = vi.fn();
    const sessionStorage = new Map<string, string>();

    const recovered = handleDynamicImportRouteError(
      new TypeError('Failed to fetch dynamically imported module: /src/views/DeviceWorkbenchView.vue'),
      {
        pathname: '/devices',
        search: '',
        hash: '',
        replace,
        reload
      },
      {
        getItem: (key: string) => sessionStorage.get(key) ?? null,
        setItem: (key: string, value: string) => sessionStorage.set(key, value),
        removeItem: (key: string) => sessionStorage.delete(key)
      }
    );

    expect(recovered).toBe(true);
    expect(replace).toHaveBeenCalledWith('/devices');
    expect(reload).not.toHaveBeenCalled();
    expect(sessionStorage.get(DYNAMIC_IMPORT_RELOAD_FLAG)).toBe('/devices');

    const secondAttempt = handleDynamicImportRouteError(
      new TypeError('Failed to fetch dynamically imported module: /src/views/DeviceWorkbenchView.vue'),
      {
        pathname: '/devices',
        search: '',
        hash: '',
        replace,
        reload
      },
      {
        getItem: (key: string) => sessionStorage.get(key) ?? null,
        setItem: (key: string, value: string) => sessionStorage.set(key, value),
        removeItem: (key: string) => sessionStorage.delete(key)
      }
    );

    expect(secondAttempt).toBe(false);
    expect(reload).toHaveBeenCalledTimes(1);
  });
});
