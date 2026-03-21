import type { ComputedRef, Ref } from 'vue';

import type { WorkspaceNavGroup, WorkspaceNavItem } from '../utils/sectionWorkspaces';

export interface ShellAccountSummary {
  initial: string;
  name: string;
  code: string;
  type: string;
  roleName: string;
  realName: string;
  displayName: string;
  phone: string;
  email: string;
  authStatus: string;
  loginMethods: string;
  primaryContact: string;
}

export interface ShellPasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export type ShellPopoverTone = 'brand' | 'accent' | 'success' | 'warning' | 'danger' | 'neutral';

export interface ShellPopoverMetric {
  id: string;
  label: string;
  value: string;
  tone?: ShellPopoverTone;
}

export interface ShellPopoverItem {
  id: string;
  title: string;
  description: string;
  meta?: string;
  badge?: string;
  path?: string;
  tone?: ShellPopoverTone;
}

export interface ShellPopoverSection {
  id: string;
  title: string;
  description: string;
  items: ShellPopoverItem[];
}

export interface ShellPopoverContent {
  title: string;
  subtitle: string;
  summaryTitle: string;
  summaryDescription: string;
  metrics: ShellPopoverMetric[];
  sections: ShellPopoverSection[];
}

export interface ShellCommandPaletteItem {
  path: string;
  title: string;
  description: string;
  workspaceLabel: string;
  short?: string;
}

export interface ShellCommandPaletteGroup {
  key: string;
  label: string;
  items: ShellCommandPaletteItem[];
}

export interface ShellViewportStyle {
  '--shell-header-height': string;
}

export interface ShellWorkspaceTabsProps {
  groups: WorkspaceNavGroup[];
  activeGroupKey: string;
}

export interface ShellSidebarNavProps {
  group: WorkspaceNavGroup;
  currentRoutePath: string;
  sidebarCollapsed: boolean;
  isMobile: boolean;
  mobileMenuOpen: boolean;
}

export interface ShellBreadcrumbProps {
  groupLabel: string;
  activeTitle: string;
}

export interface ShellAccountDrawersProps {
  summary: ShellAccountSummary;
  showAccountDialog: boolean;
  showRealNameAuthDialog: boolean;
  showLoginMethodsDialog: boolean;
  showChangePasswordDialog: boolean;
  passwordSubmitting: boolean;
}

export interface ShellHeaderToolsProps {
  showNoticePanel: boolean;
  showHelpPanel: boolean;
  noticePanelId: string;
  helpPanelId: string;
  headerIdentity: string;
  headerAccountName: string;
  headerRoleName: string;
  headerAccountCode: string;
  headerAccountType: string;
  headerAuthStatus: string;
  headerPrimaryContact: string;
  headerLoginMethods: string;
  accountInitial: string;
  unreadNoticeCount: number;
}

export interface HeaderPopoverPanelProps {
  panelId: string;
  panelClass?: string;
  ariaLabel: string;
  content: ShellPopoverContent;
}

export interface ShellCommandPaletteProps {
  modelValue: boolean;
  query: string;
  groups: ShellCommandPaletteGroup[];
  recentItems: ShellCommandPaletteItem[];
}

export interface ShellViewportState {
  headerRef: Ref<HTMLElement | null>;
  shellViewportStyle: ComputedRef<ShellViewportStyle>;
  isMobile: Ref<boolean>;
  mobileMenuOpen: Ref<boolean>;
  sidebarCollapsed: Ref<boolean>;
  toggleSidebar: () => void;
}

export interface ShellAccountCenterState {
  showAccountDialog: Ref<boolean>;
  showRealNameAuthDialog: Ref<boolean>;
  showLoginMethodsDialog: Ref<boolean>;
  showChangePasswordDialog: Ref<boolean>;
  passwordSubmitting: Ref<boolean>;
  headerIdentity: ComputedRef<string>;
  accountSummary: ComputedRef<ShellAccountSummary>;
  openAccountCenter: () => void;
  openRealNameAuth: () => void;
  openLoginMethods: () => void;
  openChangePasswordDialog: () => void;
  closeAccountOverlays: () => void;
  closeChangePasswordDialog: () => void;
  submitChangePassword: (payload: ShellPasswordPayload) => Promise<void>;
  handleLogout: () => void;
}

export type ShellRoutePathRef = Ref<string> | ComputedRef<string>;

export interface ShellNavigationState {
  navigationGroups: ComputedRef<WorkspaceNavGroup[]>;
  flattenedItems: ComputedRef<WorkspaceNavItem[]>;
  currentRoutePath: ShellRoutePathRef;
  activeGroup: ComputedRef<WorkspaceNavGroup>;
  activeMenuItem: ComputedRef<WorkspaceNavItem | null>;
  activeGroupHomePath: ComputedRef<string>;
  showSidebarContext: ComputedRef<boolean>;
  activeTitle: ComputedRef<string>;
  switchGroup: (groupKey: string) => void;
}

export interface ShellHeaderInteractionsOptions {
  headerRef: Ref<HTMLElement | null>;
  navigationGroups: ComputedRef<WorkspaceNavGroup[]>;
  flattenedItems: ComputedRef<WorkspaceNavItem[]>;
  activeGroup: ComputedRef<WorkspaceNavGroup>;
}

export interface ShellHeaderInteractionsState {
  showCommandPalette: Ref<boolean>;
  commandKeyword: Ref<string>;
  showNoticePanel: Ref<boolean>;
  showHelpPanel: Ref<boolean>;
  noticePanelId: string;
  helpPanelId: string;
  noticePopoverContent: ComputedRef<ShellPopoverContent>;
  unreadNoticeCount: ComputedRef<number>;
  helpPopoverContent: ComputedRef<ShellPopoverContent>;
  commandGroups: ComputedRef<ShellCommandPaletteGroup[]>;
  recentCommandItems: ComputedRef<ShellCommandPaletteItem[]>;
  openCommandPalette: () => void;
  selectCommandPath: (path: string) => void;
  toggleNoticePanel: () => void;
  toggleHelpPanel: () => void;
  openNotice: (path: string) => void;
  openHelp: (path: string) => void;
  closeHeaderPanels: () => void;
  resetHeaderOverlays: () => void;
}

export interface ShellRouteChangeEffectsOptions {
  currentRoutePath: ShellRoutePathRef;
  isMobile: Ref<boolean>;
  mobileMenuOpen: Ref<boolean>;
  resetHeaderOverlays: () => void;
  closeAccountOverlays: () => void;
}

export type ShellOrchestratorState =
  & ShellViewportState
  & ShellAccountCenterState
  & ShellNavigationState
  & ShellHeaderInteractionsState;
