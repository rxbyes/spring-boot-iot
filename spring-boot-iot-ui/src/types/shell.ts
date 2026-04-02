import type { ComputedRef, Ref } from 'vue';

import type { HelpDocCategory, HelpDocumentAccessRecord } from '../api/helpDoc';
import type { InAppMessageAccessRecord, InAppMessagePriority, InAppMessageType } from '../api/inAppMessage';
import type { WorkspaceNavGroup, WorkspaceNavItem } from '../utils/sectionWorkspaces';

export interface ShellAccountSummary {
  initial: string;
  name: string;
  code: string;
  type: string;
  roleName: string;
  tenantName: string;
  orgName: string;
  nickname: string;
  realName: string;
  displayName: string;
  phone: string;
  email: string;
  authStatus: string;
  loginMethods: string;
  dataScopeSummary: string;
  lastLoginTime: string;
  lastLoginIp: string;
  primaryContact: string;
}

export interface ShellPasswordPayload {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ShellProfilePayload {
  nickname: string;
  realName: string;
  phone: string;
  email: string;
  avatar?: string;
}

export type ShellPopoverTone = 'brand' | 'accent' | 'success' | 'warning' | 'danger' | 'neutral';

export interface ShellPopoverMetric {
  id: string;
  label: string;
  value: string;
  tone?: ShellPopoverTone;
}

export interface ShellPopoverFooterAction {
  id: string;
  label: string;
  tone?: ShellPopoverTone;
  disabled?: boolean;
}

export interface ShellPopoverItem {
  id: string;
  title: string;
  description: string;
  meta?: string;
  badge?: string;
  path?: string;
  tone?: ShellPopoverTone;
  actionLabel?: string;
  categoryKey?: string;
  resourceId?: string;
  read?: boolean;
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
  footerActions?: ShellPopoverFooterAction[];
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
  showChangePasswordDialog: boolean;
  passwordSubmitting: boolean;
  profileSubmitting: boolean;
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

export type ShellNoticeFilter = 'all' | InAppMessageType;
export type ShellHelpFilter = 'all' | HelpDocCategory;

export interface ShellContentPagination {
  pageNum: number;
  pageSize: number;
  total: number;
}

export interface ShellNoticeCenterEntry extends InAppMessageAccessRecord {
  resourceId?: string;
  fallback: boolean;
  relatedPathLabel: string;
  workspaceLabel: string;
}

export interface ShellHelpCenterEntry extends HelpDocumentAccessRecord {
  resourceId?: string;
  fallback: boolean;
  relatedPathLabel: string;
  workspaceLabel: string;
  primaryPath?: string;
}

export interface ShellNoticeCenterDrawerProps {
  modelValue: boolean;
  loading: boolean;
  errorMessage?: string;
  items: ShellNoticeCenterEntry[];
  pagination: ShellContentPagination;
  activeFilter: ShellNoticeFilter;
  unreadOnly: boolean;
}

export interface ShellHelpCenterDrawerProps {
  modelValue: boolean;
  loading: boolean;
  errorMessage?: string;
  items: ShellHelpCenterEntry[];
  pagination: ShellContentPagination;
  activeFilter: ShellHelpFilter;
  keyword: string;
}

export interface ShellNoticeDetailDrawerProps {
  modelValue: boolean;
  loading: boolean;
  errorMessage?: string;
  record: ShellNoticeCenterEntry | null;
}

export interface ShellHelpDetailDrawerProps {
  modelValue: boolean;
  loading: boolean;
  errorMessage?: string;
  record: ShellHelpCenterEntry | null;
  highlightKeyword?: string;
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
  showChangePasswordDialog: Ref<boolean>;
  passwordSubmitting: Ref<boolean>;
  profileSubmitting: Ref<boolean>;
  headerIdentity: ComputedRef<string>;
  accountSummary: ComputedRef<ShellAccountSummary>;
  openAccountCenter: () => void;
  openChangePasswordDialog: () => void;
  closeAccountOverlays: () => void;
  closeChangePasswordDialog: () => void;
  submitProfileUpdate: (payload: ShellProfilePayload) => Promise<void>;
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
  showNoticeCenterDrawer: Ref<boolean>;
  showHelpCenterDrawer: Ref<boolean>;
  showNoticeDetailDrawer: Ref<boolean>;
  showHelpDetailDrawer: Ref<boolean>;
  noticePanelId: string;
  helpPanelId: string;
  noticePopoverContent: ComputedRef<ShellPopoverContent>;
  unreadNoticeCount: ComputedRef<number>;
  helpPopoverContent: ComputedRef<ShellPopoverContent>;
  noticeCenterLoading: Ref<boolean>;
  noticeCenterErrorMessage: Ref<string>;
  noticeCenterItems: Ref<ShellNoticeCenterEntry[]>;
  noticeCenterPagination: ShellContentPagination;
  activeNoticeFilter: Ref<ShellNoticeFilter>;
  unreadOnlyNotice: Ref<boolean>;
  helpCenterLoading: Ref<boolean>;
  helpCenterErrorMessage: Ref<string>;
  helpCenterItems: Ref<ShellHelpCenterEntry[]>;
  helpCenterPagination: ShellContentPagination;
  activeHelpFilter: Ref<ShellHelpFilter>;
  helpKeyword: Ref<string>;
  noticeDetailLoading: Ref<boolean>;
  noticeDetailErrorMessage: Ref<string>;
  noticeDetailRecord: Ref<ShellNoticeCenterEntry | null>;
  helpDetailLoading: Ref<boolean>;
  helpDetailErrorMessage: Ref<string>;
  helpDetailRecord: Ref<ShellHelpCenterEntry | null>;
  helpDetailKeyword: Ref<string>;
  commandGroups: ComputedRef<ShellCommandPaletteGroup[]>;
  recentCommandItems: ComputedRef<ShellCommandPaletteItem[]>;
  openCommandPalette: () => void;
  selectCommandPath: (path: string) => void;
  toggleNoticePanel: () => void;
  toggleHelpPanel: () => void;
  openNotice: (item: ShellPopoverItem) => void;
  openHelp: (item: ShellPopoverItem) => void;
  handlePopoverAction: (actionId: string) => void;
  openNoticeCenter: () => void;
  openHelpCenter: () => void;
  markNoticeRead: (entry: ShellNoticeCenterEntry) => Promise<void>;
  markAllNoticeRead: () => Promise<void>;
  openNoticeDetail: (entry: ShellNoticeCenterEntry) => Promise<void>;
  openHelpDetail: (entry: ShellHelpCenterEntry, keyword?: string) => Promise<void>;
  navigateToPath: (path?: string | null) => void;
  handleNoticePageChange: (page: number) => void;
  handleNoticePageSizeChange: (size: number) => void;
  handleHelpPageChange: (page: number) => void;
  handleHelpPageSizeChange: (size: number) => void;
  handleNoticeFilterChange: (value: ShellNoticeFilter) => void;
  handleNoticeUnreadOnlyChange: (value: boolean) => void;
  handleHelpFilterChange: (value: ShellHelpFilter) => void;
  handleHelpKeywordChange: (value: string) => void;
  handleHelpSearch: () => void;
  refreshNoticeCenter: () => Promise<void>;
  refreshHelpCenter: () => Promise<void>;
  closeHeaderPanels: () => void;
  closeContentDrawers: () => void;
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
