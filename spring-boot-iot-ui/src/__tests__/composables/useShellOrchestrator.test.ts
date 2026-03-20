import { computed, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  ShellAccountCenterState,
  ShellHeaderInteractionsState,
  ShellNavigationState,
  ShellViewportState
} from '@/types/shell';

const {
  useShellViewportMock,
  useShellAccountCenterMock,
  useShellNavigationMock,
  useShellHeaderInteractionsMock,
  useShellRouteChangeEffectsMock
} = vi.hoisted(() => ({
  useShellViewportMock: vi.fn(),
  useShellAccountCenterMock: vi.fn(),
  useShellNavigationMock: vi.fn(),
  useShellHeaderInteractionsMock: vi.fn(),
  useShellRouteChangeEffectsMock: vi.fn()
}));

vi.mock('@/composables/useShellViewport', () => ({ useShellViewport: useShellViewportMock }));
vi.mock('@/composables/useShellAccountCenter', () => ({ useShellAccountCenter: useShellAccountCenterMock }));
vi.mock('@/composables/useShellNavigation', () => ({ useShellNavigation: useShellNavigationMock }));
vi.mock('@/composables/useShellHeaderInteractions', () => ({ useShellHeaderInteractions: useShellHeaderInteractionsMock }));
vi.mock('@/composables/useShellRouteChangeEffects', () => ({ useShellRouteChangeEffects: useShellRouteChangeEffectsMock }));

import { useShellOrchestrator } from '@/composables/useShellOrchestrator';

describe('useShellOrchestrator', () => {
  beforeEach(() => {
    useShellViewportMock.mockReset();
    useShellAccountCenterMock.mockReset();
    useShellNavigationMock.mockReset();
    useShellHeaderInteractionsMock.mockReset();
    useShellRouteChangeEffectsMock.mockReset();
  });

  it('wires shell composables together through a single orchestration contract', () => {
    const headerRef = ref<HTMLElement | null>(null);
    const navigationGroups = computed(() => [{ key: 'iot-access', label: '接入智维', items: [] }]);
    const flattenedItems = computed(() => [{ label: '产品定义中心', to: '/products' }]);
    const activeGroup = computed(() => ({ key: 'iot-access', label: '接入智维', items: [] }));
    const currentRoutePath = ref('/products');
    const isMobile = ref(false);
    const mobileMenuOpen = ref(true);
    const closeAccountOverlays = vi.fn();
    const resetHeaderOverlays = vi.fn();

    const viewportState: ShellViewportState = {
      headerRef,
      shellViewportStyle: computed(() => ({ '--shell-header-height': '122px' })),
      isMobile,
      mobileMenuOpen,
      sidebarCollapsed: ref(false),
      toggleSidebar: vi.fn()
    };
    const accountCenterState: ShellAccountCenterState = {
      showAccountDialog: ref(false),
      showRealNameAuthDialog: ref(false),
      showLoginMethodsDialog: ref(false),
      showChangePasswordDialog: ref(false),
      passwordSubmitting: ref(false),
      headerIdentity: computed(() => '当前角色：运维人员'),
      accountSummary: computed(() => ({
        name: '测试账号',
        roleName: '运维人员',
        code: 'ops_demo',
        type: 'sub-account',
        authStatus: 'verified',
        primaryContact: '13800000000',
        loginMethods: 'account',
        initial: '测',
        realName: '测试账号',
        displayName: '测试账号',
        phone: '13800000000',
        email: 'ops@example.com'
      })),
      openAccountCenter: vi.fn(),
      openRealNameAuth: vi.fn(),
      openLoginMethods: vi.fn(),
      openChangePasswordDialog: vi.fn(),
      closeAccountOverlays,
      closeChangePasswordDialog: vi.fn(),
      submitChangePassword: vi.fn(async () => undefined),
      handleLogout: vi.fn()
    };
    const navigationState: ShellNavigationState = {
      navigationGroups,
      flattenedItems,
      currentRoutePath,
      activeGroup,
      activeMenuItem: computed(() => null),
      activeGroupHomePath: computed(() => '/device-access'),
      showSidebarContext: computed(() => false),
      activeTitle: computed(() => '产品定义中心'),
      switchGroup: vi.fn()
    };
    const headerInteractionsState: ShellHeaderInteractionsState = {
      showCommandPalette: ref(false),
      commandKeyword: ref(''),
      showNoticePanel: ref(false),
      showHelpPanel: ref(false),
      noticePanelId: 'notice-panel',
      helpPanelId: 'help-panel',
      noticePopoverItems: computed(() => []),
      unreadNoticeCount: computed(() => 0),
      helpPopoverItems: computed(() => []),
      commandGroups: computed(() => []),
      recentCommandItems: computed(() => []),
      openCommandPalette: vi.fn(),
      selectCommandPath: vi.fn(),
      toggleNoticePanel: vi.fn(),
      toggleHelpPanel: vi.fn(),
      openNotice: vi.fn(),
      openHelp: vi.fn(),
      closeHeaderPanels: vi.fn(),
      resetHeaderOverlays
    };

    useShellViewportMock.mockReturnValue(viewportState);
    useShellAccountCenterMock.mockReturnValue(accountCenterState);
    useShellNavigationMock.mockReturnValue(navigationState);
    useShellHeaderInteractionsMock.mockReturnValue(headerInteractionsState);

    const result = useShellOrchestrator();

    expect(useShellViewportMock).toHaveBeenCalledTimes(1);
    expect(useShellAccountCenterMock).toHaveBeenCalledTimes(1);
    expect(useShellNavigationMock).toHaveBeenCalledTimes(1);
    expect(useShellHeaderInteractionsMock).toHaveBeenCalledWith({
      headerRef,
      navigationGroups,
      flattenedItems,
      activeGroup
    });
    expect(useShellRouteChangeEffectsMock).toHaveBeenCalledWith({
      currentRoutePath,
      isMobile,
      mobileMenuOpen,
      resetHeaderOverlays,
      closeAccountOverlays
    });

    expect(result.headerRef).toBe(headerRef);
    expect(result.accountSummary).toBe(accountCenterState.accountSummary);
    expect(result.navigationGroups).toBe(navigationGroups);
    expect(result.commandGroups).toBe(headerInteractionsState.commandGroups);
  });
});
