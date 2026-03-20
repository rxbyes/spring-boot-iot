import type { ShellOrchestratorState } from '../types/shell';

import { useShellAccountCenter } from './useShellAccountCenter';
import { useShellHeaderInteractions } from './useShellHeaderInteractions';
import { useShellNavigation } from './useShellNavigation';
import { useShellRouteChangeEffects } from './useShellRouteChangeEffects';
import { useShellViewport } from './useShellViewport';

export function useShellOrchestrator(): ShellOrchestratorState {
  const viewport = useShellViewport();
  const accountCenter = useShellAccountCenter();
  const navigation = useShellNavigation();
  const headerInteractions = useShellHeaderInteractions({
    headerRef: viewport.headerRef,
    navigationGroups: navigation.navigationGroups,
    flattenedItems: navigation.flattenedItems,
    activeGroup: navigation.activeGroup
  });

  useShellRouteChangeEffects({
    currentRoutePath: navigation.currentRoutePath,
    isMobile: viewport.isMobile,
    mobileMenuOpen: viewport.mobileMenuOpen,
    resetHeaderOverlays: headerInteractions.resetHeaderOverlays,
    closeAccountOverlays: accountCenter.closeAccountOverlays
  });

  return {
    ...viewport,
    ...accountCenter,
    ...navigation,
    ...headerInteractions
  };
}
