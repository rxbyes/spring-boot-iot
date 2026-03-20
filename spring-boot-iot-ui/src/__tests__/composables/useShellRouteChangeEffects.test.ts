import { computed, nextTick, ref } from 'vue';
import { describe, expect, it, vi } from 'vitest';

import { useShellRouteChangeEffects } from '@/composables/useShellRouteChangeEffects';

describe('useShellRouteChangeEffects', () => {
  it('closes the mobile menu and resets overlays on route change', async () => {
    const currentRoutePath = ref('/products');
    const isMobile = ref(true);
    const mobileMenuOpen = ref(true);
    const resetHeaderOverlays = vi.fn();
    const closeAccountOverlays = vi.fn();

    useShellRouteChangeEffects({
      currentRoutePath,
      isMobile,
      mobileMenuOpen,
      resetHeaderOverlays,
      closeAccountOverlays
    });

    currentRoutePath.value = '/devices';
    await nextTick();

    expect(mobileMenuOpen.value).toBe(false);
    expect(resetHeaderOverlays).toHaveBeenCalledTimes(1);
    expect(closeAccountOverlays).toHaveBeenCalledTimes(1);
  });

  it('keeps the desktop menu state but still resets overlays on route change', async () => {
    const routeRef = ref('/risk-disposal');
    const isMobile = ref(false);
    const mobileMenuOpen = ref(true);
    const resetHeaderOverlays = vi.fn();
    const closeAccountOverlays = vi.fn();

    useShellRouteChangeEffects({
      currentRoutePath: computed(() => routeRef.value),
      isMobile,
      mobileMenuOpen,
      resetHeaderOverlays,
      closeAccountOverlays
    });

    routeRef.value = '/report-analysis';
    await nextTick();

    expect(mobileMenuOpen.value).toBe(true);
    expect(resetHeaderOverlays).toHaveBeenCalledTimes(1);
    expect(closeAccountOverlays).toHaveBeenCalledTimes(1);
  });
});
