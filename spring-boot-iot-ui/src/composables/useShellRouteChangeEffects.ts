import { watch } from 'vue';

import type { ShellRouteChangeEffectsOptions } from '../types/shell';

function blurActiveElementInMobileSidebar() {
  if (typeof document === 'undefined') {
    return;
  }

  const activeElement = document.activeElement;
  if (!(activeElement instanceof HTMLElement)) {
    return;
  }

  if (activeElement.closest('.shell-sidebar-nav--mobile')) {
    activeElement.blur();
  }
}

export function useShellRouteChangeEffects({
  currentRoutePath,
  isMobile,
  mobileMenuOpen,
  resetHeaderOverlays,
  closeAccountOverlays
}: ShellRouteChangeEffectsOptions) {
  watch(currentRoutePath, () => {
    if (isMobile.value) {
      blurActiveElementInMobileSidebar();
      mobileMenuOpen.value = false;
    }
    resetHeaderOverlays();
    closeAccountOverlays();
  });
}
