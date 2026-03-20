import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

import type { ShellViewportState } from '../types/shell';

export function useShellViewport(): ShellViewportState {
  const headerRef = ref<HTMLElement | null>(null);
  const headerHeight = ref(122);
  const isMobile = ref(false);
  const mobileMenuOpen = ref(false);
  const sidebarCollapsed = ref(false);
  let headerResizeObserver: ResizeObserver | null = null;

  const shellViewportStyle = computed(() => ({
    '--shell-header-height': `${Math.max(headerHeight.value, 96)}px`
  }));

  function syncHeaderHeight() {
    if (!headerRef.value) {
      return;
    }
    headerHeight.value = Math.max(96, Math.ceil(headerRef.value.getBoundingClientRect().height));
  }

  function updateViewportState() {
    isMobile.value = window.matchMedia('(max-width: 1200px)').matches;
    if (isMobile.value) {
      sidebarCollapsed.value = false;
    } else {
      mobileMenuOpen.value = false;
    }

    syncHeaderHeight();
  }

  function toggleSidebar() {
    if (isMobile.value) {
      mobileMenuOpen.value = !mobileMenuOpen.value;
      return;
    }

    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  onMounted(() => {
    updateViewportState();
    syncHeaderHeight();
    if (typeof ResizeObserver !== 'undefined' && headerRef.value) {
      headerResizeObserver = new ResizeObserver(() => syncHeaderHeight());
      headerResizeObserver.observe(headerRef.value);
    }
    window.addEventListener('resize', updateViewportState);
  });

  onBeforeUnmount(() => {
    headerResizeObserver?.disconnect();
    headerResizeObserver = null;
    window.removeEventListener('resize', updateViewportState);
  });

  return {
    headerRef,
    shellViewportStyle,
    isMobile,
    mobileMenuOpen,
    sidebarCollapsed,
    toggleSidebar
  };
}
