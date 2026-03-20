import { watch } from 'vue';

import type { ShellRouteChangeEffectsOptions } from '../types/shell';

export function useShellRouteChangeEffects({
  currentRoutePath,
  isMobile,
  mobileMenuOpen,
  resetHeaderOverlays,
  closeAccountOverlays
}: ShellRouteChangeEffectsOptions) {
  watch(currentRoutePath, () => {
    if (isMobile.value) {
      mobileMenuOpen.value = false;
    }
    resetHeaderOverlays();
    closeAccountOverlays();
  });
}
