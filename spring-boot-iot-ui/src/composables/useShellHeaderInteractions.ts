import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  getMyInAppMessageUnreadStats,
  markAllMyInAppMessagesRead,
  pageMyInAppMessages,
  type InAppMessageAccessRecord,
  type InAppMessageUnreadStats
} from '../api/inAppMessage';
import {
  listAccessibleHelpDocuments,
  type HelpDocumentAccessRecord
} from '../api/helpDoc';
import { activityEntries } from '../stores/activity';
import { usePermissionStore } from '../stores/permission';
import type {
  ShellCommandPaletteGroup,
  ShellCommandPaletteItem,
  ShellHeaderInteractionsOptions,
  ShellHeaderInteractionsState
} from '../types/shell';
import { normalizeOptionalRoutePath } from '../utils/routePath';
import {
  buildShellHelpPopoverContentFromApi,
  buildShellHelpPopoverContent,
  buildShellNoticePopoverContentFromApi,
  buildShellNoticePopoverContent
} from '../utils/shellPanelContent';
import {
  getWorkspaceCommandEntryByPath,
  sortByPreferredPaths,
  type WorkspaceCommandEntry,
  type WorkspaceNavGroup
} from '../utils/sectionWorkspaces';

type CommandSearchEntry = WorkspaceCommandEntry & { searchText: string };

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
  const remoteNoticeMessages = ref<InAppMessageAccessRecord[]>([]);
  const remoteNoticeStats = ref<InAppMessageUnreadStats | null>(null);
  const remoteNoticeLoaded = ref(false);
  const remoteHelpDocuments = ref<HelpDocumentAccessRecord[]>([]);
  const remoteHelpLoaded = ref(false);
  const noticePanelId = 'header-notice-panel';
  const helpPanelId = 'header-help-panel';
  const allowedPathSignature = computed(() => [...(permissionStore.allowedPaths || [])].sort().join('|'));
  let remoteNoticeRequestId = 0;
  let remoteHelpRequestId = 0;

  const fallbackNoticePopoverContent = computed(() =>
    buildShellNoticePopoverContent({
      roleProfile: permissionStore.roleProfile,
      homePath: permissionStore.homePath || '/',
      currentPath: route.path,
      activeGroup: activeGroup.value,
      allowedPaths: permissionStore.allowedPaths || [],
      activities: activityEntries.value
    })
  );

  const noticePopoverContent = computed(() =>
    remoteNoticeLoaded.value
      ? buildShellNoticePopoverContentFromApi({
          roleProfile: permissionStore.roleProfile,
          homePath: permissionStore.homePath || '/',
          currentPath: route.path,
          activeGroup: activeGroup.value,
          allowedPaths: permissionStore.allowedPaths || [],
          activities: activityEntries.value
        }, remoteNoticeMessages.value, remoteNoticeStats.value)
      : fallbackNoticePopoverContent.value
  );

  const noticeItems = computed(() =>
    noticePopoverContent.value.sections.flatMap((section) => section.items)
  );

  const fallbackNoticeItems = computed(() =>
    fallbackNoticePopoverContent.value.sections.flatMap((section) => section.items)
  );

  const unreadNoticeCount = computed(() =>
    remoteNoticeLoaded.value
      ? remoteNoticeStats.value?.totalUnreadCount ?? remoteNoticeMessages.value.filter((item) => !item.read).length
      : fallbackNoticeItems.value.filter((item) => !readNoticeIds.value.includes(item.id)).length
  );

  const fallbackHelpPopoverContent = computed(() =>
    buildShellHelpPopoverContent({
      roleProfile: permissionStore.roleProfile,
      homePath: permissionStore.homePath || '/',
      currentPath: route.path,
      activeGroup: activeGroup.value,
      allowedPaths: permissionStore.allowedPaths || [],
      activities: activityEntries.value
    })
  );

  const helpPopoverContent = computed(() =>
    remoteHelpLoaded.value
      ? buildShellHelpPopoverContentFromApi({
          roleProfile: permissionStore.roleProfile,
          homePath: permissionStore.homePath || '/',
          currentPath: route.path,
          activeGroup: activeGroup.value,
          allowedPaths: permissionStore.allowedPaths || [],
          activities: activityEntries.value
        }, remoteHelpDocuments.value)
      : fallbackHelpPopoverContent.value
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
    fallbackNoticeItems,
    (items) => {
      const validIds = new Set(items.map((item) => item.id));
      readNoticeIds.value = readNoticeIds.value.filter((id) => validIds.has(id));
    },
    { immediate: true }
  );

  watch(
    () => `${permissionStore.authContext?.userId || ''}|${allowedPathSignature.value}`,
    () => {
      void refreshRemoteNoticeContent();
    },
    { immediate: true }
  );

  watch(
    () => `${permissionStore.authContext?.userId || ''}|${route.path}|${allowedPathSignature.value}`,
    () => {
      void refreshRemoteHelpContent();
    },
    { immediate: true }
  );

  async function refreshRemoteNoticeContent() {
    const userId = permissionStore.authContext?.userId;
    if (!userId) {
      remoteNoticeMessages.value = [];
      remoteNoticeStats.value = null;
      remoteNoticeLoaded.value = false;
      return;
    }

    const requestId = ++remoteNoticeRequestId;
    try {
      const [messageResponse, statsResponse] = await Promise.all([
        pageMyInAppMessages({ pageNum: 1, pageSize: 6 }),
        getMyInAppMessageUnreadStats()
      ]);
      if (requestId !== remoteNoticeRequestId) {
        return;
      }
      remoteNoticeMessages.value = messageResponse.data.records || [];
      remoteNoticeStats.value = statsResponse.data;
      remoteNoticeLoaded.value = true;
    } catch {
      if (requestId !== remoteNoticeRequestId) {
        return;
      }
      remoteNoticeMessages.value = [];
      remoteNoticeStats.value = null;
      remoteNoticeLoaded.value = false;
    }
  }

  async function refreshRemoteHelpContent() {
    const userId = permissionStore.authContext?.userId;
    if (!userId) {
      remoteHelpDocuments.value = [];
      remoteHelpLoaded.value = false;
      return;
    }

    const requestId = ++remoteHelpRequestId;
    try {
      const response = await listAccessibleHelpDocuments({
        currentPath: route.path,
        limit: 6
      });
      if (requestId !== remoteHelpRequestId) {
        return;
      }
      remoteHelpDocuments.value = response.data || [];
      remoteHelpLoaded.value = true;
    } catch {
      if (requestId !== remoteHelpRequestId) {
        return;
      }
      remoteHelpDocuments.value = [];
      remoteHelpLoaded.value = false;
    }
  }

  function markAllRemoteNoticeItemsRead() {
    if (!remoteNoticeLoaded.value || unreadNoticeCount.value <= 0) {
      return;
    }
    remoteNoticeMessages.value = remoteNoticeMessages.value.map((item) => ({
      ...item,
      read: true,
      readTime: item.readTime || new Date().toISOString()
    }));
    if (remoteNoticeStats.value) {
      remoteNoticeStats.value = {
        ...remoteNoticeStats.value,
        totalUnreadCount: 0,
        systemUnreadCount: 0,
        businessUnreadCount: 0,
        errorUnreadCount: 0
      };
    }
    void markAllMyInAppMessagesRead()
      .then(() => refreshRemoteNoticeContent())
      .catch(() => refreshRemoteNoticeContent());
  }

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
      if (remoteNoticeLoaded.value) {
        markAllRemoteNoticeItemsRead();
      } else {
        readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...fallbackNoticeItems.value.map((item) => item.id)]));
      }
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
    if (!remoteNoticeLoaded.value) {
      readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, ...fallbackNoticeItems.value.map((item) => item.id)]));
    }
    showNoticePanel.value = false;
    if (path !== route.path) {
      router.push(path);
    }
  }

  function openHelp(path: string) {
    showHelpPanel.value = false;
    if (path !== route.path) {
      router.push(path);
    }
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
    noticePopoverContent,
    unreadNoticeCount,
    helpPopoverContent,
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
