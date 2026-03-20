import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { activityEntries } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import type {
  ShellCommandPaletteGroup,
  ShellCommandPaletteItem,
  ShellHeaderInteractionsOptions,
  ShellHeaderInteractionsState,
  ShellPopoverItem
} from '../types/shell';
import { formatDateTime } from '../utils/format';
import { normalizeOptionalRoutePath } from '../utils/routePath';
import {
  getWorkspaceCommandEntryByPath,
  sortByPreferredPaths,
  type WorkspaceCommandEntry,
  type WorkspaceNavGroup
} from '../utils/sectionWorkspaces';

type CommandSearchEntry = WorkspaceCommandEntry & { searchText: string };

const helpItems = [
  { label: '平台首页', caption: '查看系统总览和业务入口', path: '/' },
  { label: '链路验证中心', caption: '验证 HTTP 上报与链路解析', path: '/reporting' },
  { label: '风险策略', caption: '进入对象、阈值与联动配置总览', path: '/risk-config' },
  { label: '角色权限', caption: '维护角色与权限关系', path: '/role' }
];

export function useShellHeaderInteractions({
  headerRef,
  navigationGroups,
  flattenedItems,
  activeGroup
}: ShellHeaderInteractionsOptions): ShellHeaderInteractionsState {
  const route = useRoute();
  const router = useRouter();
  const permissionStore = usePermissionStore();
  const showCommandPalette = ref(false);
  const commandKeyword = ref('');
  const showNoticePanel = ref(false);
  const showHelpPanel = ref(false);
  const readNoticeIds = ref<string[]>([]);
  const noticePanelId = 'header-notice-panel';
  const helpPanelId = 'header-help-panel';

  const noticeItems = computed(() => {
    const fromActivity = activityEntries.value.slice(0, 4).map((item) => ({
      id: item.id,
      title: item.title || [item.module, item.action].filter(Boolean).join(' · ') || '最近操作',
      time: formatDateTime(item.createdAt),
      path: item.path || route.path
    }));

    if (fromActivity.length > 0) {
      return fromActivity;
    }

    return [
      { id: 'notice-1', title: '系统导航已升级为统一控制台样式', time: '刚刚', path: '/' },
      { id: 'notice-2', title: '按钮权限仍按数据库角色授权控制', time: '刚刚', path: '/role' },
      { id: 'notice-3', title: '右上角头像已收口账号信息与安全操作', time: '刚刚', path: route.path }
    ];
  });

  const noticePopoverItems = computed<ShellPopoverItem[]>(() =>
    noticeItems.value.map((item) => ({
      id: item.id,
      title: item.title,
      description: item.time,
      path: item.path
    }))
  );

  const unreadNoticeCount = computed(() => noticeItems.value.filter((item) => !readNoticeIds.value.includes(item.id)).length);

  const helpPopoverItems = computed<ShellPopoverItem[]>(() =>
    helpItems.map((item, index) => ({
      id: `help-${index}`,
      title: item.label,
      description: item.caption,
      path: item.path
    }))
  );

  const commandEntries = computed(() => {
    const groupByPath = new Map<string, WorkspaceNavGroup>();
    navigationGroups.value.forEach((group) => {
      group.items.forEach((item) => {
        groupByPath.set(item.to, group);
      });
    });

    const uniqueEntries = new Map<string, CommandSearchEntry>();
    flattenedItems.value.forEach((item) => {
      const preset = getWorkspaceCommandEntryByPath(item.to);
      const group = groupByPath.get(item.to);
      const entry = {
        id: preset?.id || item.to,
        path: item.to,
        title: item.label,
        description: item.caption,
        workspaceKey: preset?.workspaceKey || group?.key || activeGroup.value.key,
        workspaceLabel: preset?.workspaceLabel || group?.label || activeGroup.value.label,
        short: preset?.short || item.short,
        type: preset?.type || 'page',
        keywords: preset?.keywords || [],
        searchText: [
          item.label,
          item.caption,
          item.to,
          preset?.workspaceLabel,
          ...(preset?.keywords || [])
        ].join(' ').toLowerCase()
      };
      uniqueEntries.set(item.to, entry);
    });

    return sortByPreferredPaths(Array.from(uniqueEntries.values()), permissionStore.roleProfile.featuredPaths);
  });

  const commandGroups = computed<ShellCommandPaletteGroup[]>(() => {
    const keyword = commandKeyword.value.trim().toLowerCase();
    const filteredEntries = keyword
      ? commandEntries.value.filter((entry) => entry.searchText.includes(keyword))
      : commandEntries.value;

    const groupedEntries = new Map<string, { key: string; label: string; items: CommandSearchEntry[] }>();
    filteredEntries.forEach((entry) => {
      if (!groupedEntries.has(entry.workspaceKey)) {
        groupedEntries.set(entry.workspaceKey, {
          key: entry.workspaceKey,
          label: entry.workspaceLabel,
          items: []
        });
      }
      groupedEntries.get(entry.workspaceKey)!.items.push(entry);
    });

    const preferredWorkspaceKeys = [
      activeGroup.value.key,
      ...permissionStore.roleProfile.preferredWorkspaceKeys
    ];
    return sortByPreferredPaths(
      Array.from(groupedEntries.values()).map((group) => ({
        ...group,
        path: `/${group.key}`
      })),
      preferredWorkspaceKeys.map((key) => `/${key}`)
    ).map(({ path: _path, ...group }) => ({
      ...group,
      items: group.items.slice(0, keyword ? 6 : 4).map((item) => ({
        path: item.path,
        title: item.title,
        description: item.description,
        workspaceLabel: item.workspaceLabel,
        short: item.short
      }))
    }));
  });

  const recentCommandItems = computed<ShellCommandPaletteItem[]>(() => {
    const recentItems = activityEntries.value
      .map((item) => {
        const path = normalizeOptionalRoutePath(item.path);
        if (!path || !permissionStore.hasRoutePermission(path)) {
          return null;
        }
        const preset = getWorkspaceCommandEntryByPath(path);
        if (!preset) {
          return null;
        }
        return {
          path,
          title: preset.title,
          description: item.title || preset.description,
          workspaceLabel: preset.workspaceLabel,
          short: preset.short
        };
      })
      .filter((item): item is NonNullable<typeof item> => Boolean(item));

    const deduped = Array.from(new Map(recentItems.map((item) => [item.path, item])).values());
    if (deduped.length > 0) {
      return deduped.slice(0, 4);
    }

    return commandEntries.value
      .filter((entry) => permissionStore.roleProfile.featuredPaths.includes(entry.path))
      .slice(0, 4)
      .map((entry) => ({
        path: entry.path,
        title: entry.title,
        description: entry.description,
        workspaceLabel: entry.workspaceLabel,
        short: entry.short
      }));
  });

  watch(
    noticeItems,
    (items) => {
      const validIds = new Set(items.map((item) => item.id));
      readNoticeIds.value = readNoticeIds.value.filter((id) => validIds.has(id));
    },
    { immediate: true }
  );

  function closeHeaderPanels() {
    showNoticePanel.value = false;
    showHelpPanel.value = false;
  }

  function resetHeaderOverlays() {
    showCommandPalette.value = false;
    closeHeaderPanels();
  }

  function openCommandPalette() {
    closeHeaderPanels();
    showCommandPalette.value = true;
  }

  function selectCommandPath(path: string) {
    showCommandPalette.value = false;
    if (path !== route.path) {
      router.push(path);
    }
  }

  function toggleNoticePanel() {
    const willOpen = !showNoticePanel.value;
    showNoticePanel.value = willOpen;
    if (showNoticePanel.value) {
      readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...noticeItems.value.map((item) => item.id)]));
      showHelpPanel.value = false;
    }
  }

  function toggleHelpPanel() {
    showHelpPanel.value = !showHelpPanel.value;
    if (showHelpPanel.value) {
      showNoticePanel.value = false;
    }
  }

  function openNotice(path: string) {
    readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...noticeItems.value.map((item) => item.id)]));
    showNoticePanel.value = false;
    router.push(path);
  }

  function openHelp(path: string) {
    showHelpPanel.value = false;
    router.push(path);
  }

  function handleDocumentPointerDown(event: PointerEvent) {
    if (!showNoticePanel.value && !showHelpPanel.value) {
      return;
    }
    const target = event.target as Node | null;
    if (!target) {
      return;
    }
    if (headerRef.value?.contains(target)) {
      return;
    }
    closeHeaderPanels();
  }

  function handleDocumentKeydown(event: KeyboardEvent) {
    const pressedKey = event.key.toLowerCase();
    if ((event.ctrlKey || event.metaKey) && pressedKey === 'k') {
      event.preventDefault();
      closeHeaderPanels();
      showCommandPalette.value = !showCommandPalette.value;
      return;
    }

    if (event.key !== 'Escape') {
      return;
    }
    if (showCommandPalette.value) {
      showCommandPalette.value = false;
      return;
    }
    if (!showNoticePanel.value && !showHelpPanel.value) {
      return;
    }
    closeHeaderPanels();
  }

  onMounted(() => {
    document.addEventListener('pointerdown', handleDocumentPointerDown);
    document.addEventListener('keydown', handleDocumentKeydown);
  });

  onBeforeUnmount(() => {
    document.removeEventListener('pointerdown', handleDocumentPointerDown);
    document.removeEventListener('keydown', handleDocumentKeydown);
  });

  return {
    showCommandPalette,
    commandKeyword,
    showNoticePanel,
    showHelpPanel,
    noticePanelId,
    helpPanelId,
    noticePopoverItems,
    unreadNoticeCount,
    helpPopoverItems,
    commandGroups,
    recentCommandItems,
    openCommandPalette,
    selectCommandPath,
    toggleNoticePanel,
    toggleHelpPanel,
    openNotice,
    openHelp,
    closeHeaderPanels,
    resetHeaderOverlays
  };
}
