import { computed, ref } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  ShellAccountCenterState,
  ShellHelpCenterEntry,
  ShellHeaderInteractionsState,
  ShellNavigationState,
  ShellNoticeCenterEntry,
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
      showChangePasswordDialog: ref(false),
      passwordSubmitting: ref(false),
      profileSubmitting: ref(false),
      headerIdentity: computed(() => '当前角色：运维人员'),
      accountSummary: computed(() => ({
        name: '测试账号',
        roleName: '运维人员',
        code: 'ops_demo',
        type: 'sub-account',
        tenantName: '默认租户',
        orgName: '平台治理中心',
        nickname: '测试昵称',
        authStatus: 'verified',
        dataScopeSummary: '租户内全部',
        lastLoginTime: '2026-04-02 09:00:00',
        lastLoginIp: '10.10.10.8',
        primaryContact: '13800000000',
        loginMethods: 'account',
        initial: '测',
        realName: '测试账号',
        displayName: '测试账号',
        phone: '13800000000',
        email: 'ops@example.com'
      })),
      openAccountCenter: vi.fn(),
      openChangePasswordDialog: vi.fn(),
      closeAccountOverlays,
      closeChangePasswordDialog: vi.fn(),
      submitProfileUpdate: vi.fn(async () => undefined),
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
      showNoticeCenterDrawer: ref(false),
      showHelpCenterDrawer: ref(false),
      showNoticeDetailDrawer: ref(false),
      showHelpDetailDrawer: ref(false),
      noticePanelId: 'notice-panel',
      helpPanelId: 'help-panel',
      noticePopoverContent: computed(() => ({
        title: '通知中心',
        subtitle: 'subtitle',
        summaryTitle: 'summary',
        summaryDescription: 'description',
        metrics: [],
        sections: [],
        footerActions: []
      })),
      unreadNoticeCount: computed(() => 0),
      helpPopoverContent: computed(() => ({
        title: '帮助中心',
        subtitle: 'subtitle',
        summaryTitle: 'summary',
        summaryDescription: 'description',
        metrics: [],
        sections: [],
        footerActions: []
      })),
      noticeCenterLoading: ref(false),
      noticeCenterErrorMessage: ref(''),
      noticeCenterItems: ref<ShellNoticeCenterEntry[]>([]),
      noticeCenterPagination: { pageNum: 1, pageSize: 10, total: 0 },
      activeNoticeFilter: ref('all'),
      unreadOnlyNotice: ref(false),
      helpCenterLoading: ref(false),
      helpCenterErrorMessage: ref(''),
      helpCenterItems: ref<ShellHelpCenterEntry[]>([]),
      helpCenterPagination: { pageNum: 1, pageSize: 10, total: 0 },
      activeHelpFilter: ref('all'),
      helpKeyword: ref(''),
      noticeDetailLoading: ref(false),
      noticeDetailErrorMessage: ref(''),
      noticeDetailRecord: ref(null),
      helpDetailLoading: ref(false),
      helpDetailErrorMessage: ref(''),
      helpDetailRecord: ref(null),
      helpDetailKeyword: ref(''),
      commandGroups: computed(() => []),
      recentCommandItems: computed(() => []),
      openCommandPalette: vi.fn(),
      selectCommandPath: vi.fn(),
      toggleNoticePanel: vi.fn(),
      toggleHelpPanel: vi.fn(),
      openNotice: vi.fn(),
      openHelp: vi.fn(),
      handlePopoverAction: vi.fn(),
      openNoticeCenter: vi.fn(),
      openHelpCenter: vi.fn(),
      markNoticeRead: vi.fn(async () => undefined),
      markAllNoticeRead: vi.fn(async () => undefined),
      openNoticeDetail: vi.fn(async () => undefined),
      openHelpDetail: vi.fn(async () => undefined),
      navigateToPath: vi.fn(),
      handleNoticePageChange: vi.fn(),
      handleNoticePageSizeChange: vi.fn(),
      handleHelpPageChange: vi.fn(),
      handleHelpPageSizeChange: vi.fn(),
      handleNoticeFilterChange: vi.fn(),
      handleNoticeUnreadOnlyChange: vi.fn(),
      handleHelpFilterChange: vi.fn(),
      handleHelpKeywordChange: vi.fn(),
      handleHelpSearch: vi.fn(),
      refreshNoticeCenter: vi.fn(async () => undefined),
      refreshHelpCenter: vi.fn(async () => undefined),
      closeHeaderPanels: vi.fn(),
      closeContentDrawers: vi.fn(),
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
