import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getMyInAppMessage,
  getMyInAppMessageUnreadStats,
  markAllMyInAppMessagesRead,
  markMyInAppMessageRead,
  pageMyInAppMessages,
  type InAppMessageAccessRecord,
  type InAppMessageUnreadStats
} from '../api/inAppMessage'
import {
  getAccessibleHelpDocument,
  listAccessibleHelpDocuments,
  pageAccessibleHelpDocuments,
  type HelpDocumentAccessRecord
} from '../api/helpDoc'
import { activityEntries } from '../stores/activity'
import { usePermissionStore } from '../stores/permission'
import type {
  ShellCommandPaletteGroup,
  ShellCommandPaletteItem,
  ShellContentPagination,
  ShellHeaderInteractionsOptions,
  ShellHeaderInteractionsState,
  ShellHelpCenterEntry,
  ShellHelpFilter,
  ShellNoticeCenterEntry,
  ShellNoticeFilter,
  ShellPopoverItem
} from '../types/shell'
import { normalizeOptionalRoutePath, normalizeRoutePath } from '../utils/routePath'
import {
  buildShellHelpPopoverContentFromApi,
  buildShellHelpPopoverContent,
  buildShellNoticePopoverContentFromApi,
  buildShellNoticePopoverContent
} from '../utils/shellPanelContent'
import {
  getWorkspaceCommandEntryByPath,
  sortByPreferredPaths,
  type WorkspaceCommandEntry,
  type WorkspaceNavGroup
} from '../utils/sectionWorkspaces'

type CommandSearchEntry = WorkspaceCommandEntry & { searchText: string }

const NOTICE_SYNC_CHANNEL_NAME = 'spring-boot-iot:shell-notice-sync'
const NOTICE_SYNC_STORAGE_KEY = 'spring-boot-iot:shell-notice-sync-event'
const NOTICE_STATS_AUTO_REFRESH_INTERVAL_MS = 60_000
const NOTICE_STATS_ERROR_RETRY_INTERVAL_MS = 15_000

interface NoticeRefreshOptions {
  force?: boolean
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException
    ? error.name === 'AbortError'
    : error instanceof Error && error.name === 'AbortError'
}

export function useShellHeaderInteractions({
  headerRef,
  navigationGroups,
  flattenedItems,
  activeGroup
}: ShellHeaderInteractionsOptions): ShellHeaderInteractionsState {
  const route = useRoute()
  const router = useRouter()
  const permissionStore = usePermissionStore()

  const showCommandPalette = ref(false)
  const commandKeyword = ref('')
  const showNoticePanel = ref(false)
  const showHelpPanel = ref(false)
  const showNoticeCenterDrawer = ref(false)
  const showHelpCenterDrawer = ref(false)
  const showNoticeDetailDrawer = ref(false)
  const showHelpDetailDrawer = ref(false)

  const readNoticeIds = ref<string[]>([])
  const remoteNoticeMessages = ref<InAppMessageAccessRecord[]>([])
  const remoteNoticeStats = ref<InAppMessageUnreadStats | null>(null)
  const remoteNoticeLoaded = ref(false)
  const remoteHelpDocuments = ref<HelpDocumentAccessRecord[]>([])
  const remoteHelpLoaded = ref(false)

  const noticeCenterLoading = ref(false)
  const noticeCenterErrorMessage = ref('')
  const noticeCenterItems = ref<ShellNoticeCenterEntry[]>([])
  const noticeCenterPagination = reactive<ShellContentPagination>({
    pageNum: 1,
    pageSize: 10,
    total: 0
  })
  const activeNoticeFilter = ref<ShellNoticeFilter>('all')
  const unreadOnlyNotice = ref(false)

  const helpCenterLoading = ref(false)
  const helpCenterErrorMessage = ref('')
  const helpCenterItems = ref<ShellHelpCenterEntry[]>([])
  const helpCenterPagination = reactive<ShellContentPagination>({
    pageNum: 1,
    pageSize: 10,
    total: 0
  })
  const activeHelpFilter = ref<ShellHelpFilter>('all')
  const helpKeyword = ref('')

  const noticeDetailLoading = ref(false)
  const noticeDetailErrorMessage = ref('')
  const noticeDetailRecord = ref<ShellNoticeCenterEntry | null>(null)
  const helpDetailLoading = ref(false)
  const helpDetailErrorMessage = ref('')
  const helpDetailRecord = ref<ShellHelpCenterEntry | null>(null)
  const helpDetailKeyword = ref('')

  const noticePanelId = 'header-notice-panel'
  const helpPanelId = 'header-help-panel'
  const allowedPathSignature = computed(() => [...(permissionStore.allowedPaths || [])].sort().join('|'))

  let remoteNoticeRequestId = 0
  let remoteHelpRequestId = 0
  let remoteNoticeController: AbortController | null = null
  let remoteNoticeStatsRequestId = 0
  let remoteNoticeStatsController: AbortController | null = null
  let remoteNoticeStatsRefreshTask: Promise<void> | null = null
  let remoteNoticeStatsLastRequestedAt = 0
  let remoteHelpController: AbortController | null = null
  let noticeCenterController: AbortController | null = null
  let helpCenterController: AbortController | null = null
  let noticeDetailController: AbortController | null = null
  let helpDetailController: AbortController | null = null
  let noticeSyncChannel: BroadcastChannel | null = null

  const fallbackNoticePopoverContent = computed(() =>
    buildShellNoticePopoverContent({
      roleProfile: permissionStore.roleProfile,
      homePath: permissionStore.homePath || '/',
      currentPath: route.path,
      activeGroup: activeGroup.value,
      allowedPaths: permissionStore.allowedPaths || [],
      activities: activityEntries.value
    })
  )

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
  )

  const fallbackNoticeItems = computed(() =>
    fallbackNoticePopoverContent.value.sections.flatMap((section) => section.items)
  )

  const fallbackHelpPopoverContent = computed(() =>
    buildShellHelpPopoverContent({
      roleProfile: permissionStore.roleProfile,
      homePath: permissionStore.homePath || '/',
      currentPath: route.path,
      activeGroup: activeGroup.value,
      allowedPaths: permissionStore.allowedPaths || [],
      activities: activityEntries.value
    })
  )

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
  )

  const unreadNoticeCount = computed(() =>
    remoteNoticeStats.value
      ? Number(remoteNoticeStats.value.totalUnreadCount ?? 0)
      : remoteNoticeLoaded.value
        ? remoteNoticeMessages.value.filter((item) => !item.read).length
      : fallbackNoticeItems.value.filter((item) => !readNoticeIds.value.includes(item.id)).length
  )

  const commandEntries = computed(() => {
    const groupByPath = new Map<string, WorkspaceNavGroup>()
    navigationGroups.value.forEach((group) => {
      group.items.forEach((item) => {
        groupByPath.set(item.to, group)
      })
    })

    const uniqueEntries = new Map<string, CommandSearchEntry>()
    flattenedItems.value.forEach((item) => {
      const preset = getWorkspaceCommandEntryByPath(item.to)
      const group = groupByPath.get(item.to)
      uniqueEntries.set(item.to, {
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
      })
    })

    return sortByPreferredPaths(Array.from(uniqueEntries.values()), permissionStore.roleProfile.featuredPaths)
  })

  const commandGroups = computed<ShellCommandPaletteGroup[]>(() => {
    const keyword = commandKeyword.value.trim().toLowerCase()
    const filteredEntries = keyword
      ? commandEntries.value.filter((entry) => entry.searchText.includes(keyword))
      : commandEntries.value

    const groupedEntries = new Map<string, { key: string; label: string; items: CommandSearchEntry[] }>()
    filteredEntries.forEach((entry) => {
      if (!groupedEntries.has(entry.workspaceKey)) {
        groupedEntries.set(entry.workspaceKey, {
          key: entry.workspaceKey,
          label: entry.workspaceLabel,
          items: []
        })
      }
      groupedEntries.get(entry.workspaceKey)!.items.push(entry)
    })

    const preferredWorkspaceKeys = [
      activeGroup.value.key,
      ...permissionStore.roleProfile.preferredWorkspaceKeys
    ]
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
    }))
  })

  const recentCommandItems = computed<ShellCommandPaletteItem[]>(() => {
    const recentItems = activityEntries.value
      .map((item) => {
        const path = normalizeOptionalRoutePath(item.path)
        if (!path || !permissionStore.hasRoutePermission(path)) {
          return null
        }
        const preset = getWorkspaceCommandEntryByPath(path)
        if (!preset) {
          return null
        }
        return {
          path,
          title: preset.title,
          description: item.title || preset.description,
          workspaceLabel: preset.workspaceLabel,
          short: preset.short
        }
      })
      .filter((item): item is NonNullable<typeof item> => Boolean(item))

    const deduped = Array.from(new Map(recentItems.map((item) => [item.path, item])).values())
    if (deduped.length > 0) {
      return deduped.slice(0, 4)
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
      }))
  })

  function resetPagination(pagination: ShellContentPagination) {
    pagination.pageNum = 1
  }

  function resolvePathLabel(path?: string | null, emptyLabel = '仅站内展示') {
    const normalizedPath = normalizeOptionalRoutePath(path)
    if (!normalizedPath) {
      return emptyLabel
    }
    const entry = getWorkspaceCommandEntryByPath(normalizedPath)
    return entry ? `${entry.workspaceLabel} / ${entry.title}` : normalizedPath
  }

  function resolveWorkspaceLabel(path?: string | null, fallbackLabel = '站内资料') {
    const normalizedPath = normalizeOptionalRoutePath(path)
    const entry = normalizedPath ? getWorkspaceCommandEntryByPath(normalizedPath) : undefined
    return entry?.workspaceLabel || fallbackLabel
  }

  function mapNoticeRecord(record: InAppMessageAccessRecord): ShellNoticeCenterEntry {
    const relatedPath = normalizeOptionalRoutePath(record.relatedPath)
    return {
      ...record,
      id: `notice-center-${record.id}`,
      resourceId: String(record.id),
      fallback: false,
      relatedPath,
      relatedPathLabel: resolvePathLabel(relatedPath),
      workspaceLabel: resolveWorkspaceLabel(relatedPath, record.sourceType || '站内消息'),
      read: Boolean(record.read)
    }
  }

  function mapHelpRecord(record: HelpDocumentAccessRecord): ShellHelpCenterEntry {
    const relatedPaths = (record.relatedPathList || []).map((path) => normalizeRoutePath(path))
    const preferredPath = relatedPaths.find((path) => path === normalizeRoutePath(route.path))
      || relatedPaths[0]
    return {
      ...record,
      id: `help-center-${record.id}`,
      resourceId: String(record.id),
      fallback: false,
      relatedPathList: relatedPaths,
      primaryPath: preferredPath,
      relatedPathLabel: relatedPaths.length > 0
        ? relatedPaths.slice(0, 2).map((path) => resolvePathLabel(path, path)).join('、')
        : '未绑定页面',
      workspaceLabel: resolveWorkspaceLabel(preferredPath, '帮助资料')
    }
  }

  function mapFallbackNoticeItem(item: ShellPopoverItem): ShellNoticeCenterEntry {
    const relatedPath = normalizeOptionalRoutePath(item.path)
    return {
      id: item.id,
      messageType: (item.categoryKey as ShellNoticeFilter) === 'system'
        ? 'system'
        : (item.categoryKey as ShellNoticeFilter) === 'error'
          ? 'error'
          : 'business',
      priority: item.tone === 'danger' ? 'high' : 'medium',
      title: item.title,
      summary: item.description,
      content: item.description,
      targetType: null,
      relatedPath,
      sourceType: item.badge || '壳层兜底',
      sourceId: null,
      publishTime: null,
      expireTime: null,
      read: readNoticeIds.value.includes(item.id),
      readTime: null,
      resourceId: item.resourceId,
      fallback: true,
      relatedPathLabel: resolvePathLabel(relatedPath),
      workspaceLabel: item.meta?.split(' · ')[0] || resolveWorkspaceLabel(relatedPath, '站内消息')
    }
  }

  function mapFallbackHelpItem(item: ShellPopoverItem): ShellHelpCenterEntry {
    const primaryPath = normalizeOptionalRoutePath(item.path)
    const relatedPathList = primaryPath ? [primaryPath] : []
    return {
      id: item.id,
      docCategory: (item.categoryKey as ShellHelpFilter) === 'technical'
        ? 'technical'
        : (item.categoryKey as ShellHelpFilter) === 'faq'
          ? 'faq'
          : 'business',
      sortNo: 0,
      title: item.title,
      summary: item.description,
      content: item.description,
      keywords: '',
      relatedPaths: primaryPath || '',
      currentPathMatched: Boolean(primaryPath && normalizeRoutePath(primaryPath) === normalizeRoutePath(route.path)),
      keywordList: [],
      relatedPathList,
      resourceId: item.resourceId,
      fallback: true,
      primaryPath,
      relatedPathLabel: resolvePathLabel(primaryPath, '未绑定页面'),
      workspaceLabel: item.meta?.split(' · ')[0] || resolveWorkspaceLabel(primaryPath, '帮助资料')
    }
  }

  function buildFallbackNoticeEntries() {
    const entries = fallbackNoticePopoverContent.value.sections
      .flatMap((section) => section.items)
      .map((item) => mapFallbackNoticeItem(item))
      .filter((entry) => activeNoticeFilter.value === 'all' || entry.messageType === activeNoticeFilter.value)
      .filter((entry) => !unreadOnlyNotice.value || !entry.read)

    noticeCenterPagination.total = entries.length
    const fromIndex = Math.min((noticeCenterPagination.pageNum - 1) * noticeCenterPagination.pageSize, entries.length)
    const toIndex = Math.min(fromIndex + noticeCenterPagination.pageSize, entries.length)
    noticeCenterItems.value = entries.slice(fromIndex, toIndex)
    noticeCenterErrorMessage.value = ''
  }

  function buildFallbackHelpEntries() {
    const normalizedKeyword = helpKeyword.value.trim().toLowerCase()
    const entries = fallbackHelpPopoverContent.value.sections
      .flatMap((section) => section.items)
      .map((item) => mapFallbackHelpItem(item))
      .filter((entry) => activeHelpFilter.value === 'all' || entry.docCategory === activeHelpFilter.value)
      .filter((entry) => {
        if (!normalizedKeyword) {
          return true
        }
        return [
          entry.title,
          entry.summary,
          entry.content,
          ...(entry.keywordList || [])
        ].some((text) => String(text || '').toLowerCase().includes(normalizedKeyword))
      })

    helpCenterPagination.total = entries.length
    const fromIndex = Math.min((helpCenterPagination.pageNum - 1) * helpCenterPagination.pageSize, entries.length)
    const toIndex = Math.min(fromIndex + helpCenterPagination.pageSize, entries.length)
    helpCenterItems.value = entries.slice(fromIndex, toIndex)
    helpCenterErrorMessage.value = ''
  }

  function applyNoticeReadLocally(resourceId?: string | null) {
    if (!resourceId) {
      return
    }
    const readTime = new Date().toISOString()
    const matchedMessage = remoteNoticeMessages.value.find((item) => String(item.id) === String(resourceId))
    const updateNoticeRecord = (record: InAppMessageAccessRecord) =>
      String(record.id) === String(resourceId)
        ? { ...record, read: true, readTime: record.readTime || readTime }
        : record

    remoteNoticeMessages.value = remoteNoticeMessages.value.map(updateNoticeRecord)
    noticeCenterItems.value = noticeCenterItems.value.map((item) =>
      String(item.resourceId || item.id) === String(resourceId)
        ? { ...item, read: true, readTime: item.readTime || readTime }
        : item
    )
    if (noticeDetailRecord.value && String(noticeDetailRecord.value.resourceId || noticeDetailRecord.value.id) === String(resourceId)) {
      noticeDetailRecord.value = {
        ...noticeDetailRecord.value,
        read: true,
        readTime: noticeDetailRecord.value.readTime || readTime
      }
    }

    if (!matchedMessage || matchedMessage.read || !remoteNoticeStats.value) {
      return
    }
    remoteNoticeStats.value = {
      ...remoteNoticeStats.value,
      totalUnreadCount: Math.max(0, Number(remoteNoticeStats.value.totalUnreadCount || 0) - 1),
      systemUnreadCount: matchedMessage.messageType === 'system'
        ? Math.max(0, Number(remoteNoticeStats.value.systemUnreadCount || 0) - 1)
        : Number(remoteNoticeStats.value.systemUnreadCount || 0),
      businessUnreadCount: matchedMessage.messageType === 'business'
        ? Math.max(0, Number(remoteNoticeStats.value.businessUnreadCount || 0) - 1)
        : Number(remoteNoticeStats.value.businessUnreadCount || 0),
      errorUnreadCount: matchedMessage.messageType === 'error'
        ? Math.max(0, Number(remoteNoticeStats.value.errorUnreadCount || 0) - 1)
        : Number(remoteNoticeStats.value.errorUnreadCount || 0)
    }
  }

  function markFallbackNoticeRead(entry: ShellNoticeCenterEntry) {
    readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, entry.id]))
    noticeCenterItems.value = noticeCenterItems.value.map((item) =>
      item.id === entry.id
        ? { ...item, read: true, readTime: item.readTime || new Date().toISOString() }
        : item
    )
    if (noticeDetailRecord.value?.id === entry.id) {
      noticeDetailRecord.value = {
        ...noticeDetailRecord.value,
        read: true,
        readTime: noticeDetailRecord.value.readTime || new Date().toISOString()
      }
    }
  }

  function closeHeaderPanels() {
    showNoticePanel.value = false
    showHelpPanel.value = false
  }

  function closeContentDrawers() {
    showNoticeCenterDrawer.value = false
    showHelpCenterDrawer.value = false
    showNoticeDetailDrawer.value = false
    showHelpDetailDrawer.value = false
  }

  function resetHeaderOverlays() {
    showCommandPalette.value = false
    closeHeaderPanels()
    closeContentDrawers()
  }

  function abortControllers() {
    remoteNoticeController?.abort()
    remoteNoticeController = null
    remoteNoticeStatsController?.abort()
    remoteNoticeStatsController = null
    remoteHelpController?.abort()
    remoteHelpController = null
    noticeCenterController?.abort()
    noticeCenterController = null
    helpCenterController?.abort()
    helpCenterController = null
    noticeDetailController?.abort()
    noticeDetailController = null
    helpDetailController?.abort()
    helpDetailController = null
  }

  function refreshNoticeSurfaces(options: NoticeRefreshOptions = {}) {
    if (showNoticePanel.value) {
      void refreshRemoteNoticeContent(options)
    } else {
      void refreshRemoteNoticeStats(options)
    }
    if (showNoticeCenterDrawer.value) {
      void refreshNoticeCenter()
    }
  }

  function emitNoticeSync(reason: string) {
    const payload = {
      reason,
      timestamp: Date.now(),
      userId: permissionStore.authContext?.userId || ''
    }
    try {
      noticeSyncChannel?.postMessage(payload)
    } catch {
      // 忽略同步通道异常，避免影响当前标签页交互。
    }
    try {
      window.localStorage.setItem(NOTICE_SYNC_STORAGE_KEY, JSON.stringify(payload))
      window.localStorage.removeItem(NOTICE_SYNC_STORAGE_KEY)
    } catch {
      // 某些隐私模式可能禁用 localStorage，同样不影响当前页。
    }
  }

  function handleNoticeSyncPayload(payload: unknown) {
    if (!permissionStore.authContext?.userId || !payload || typeof payload !== 'object') {
      return
    }
    const userId = String((payload as { userId?: unknown }).userId || '')
    if (userId && userId !== String(permissionStore.authContext?.userId || '')) {
      return
    }
    refreshNoticeSurfaces({ force: true })
  }

  function handleStorageSync(event: StorageEvent) {
    if (event.key !== NOTICE_SYNC_STORAGE_KEY || !event.newValue) {
      return
    }
    try {
      handleNoticeSyncPayload(JSON.parse(event.newValue))
    } catch {
      // 忽略非法同步载荷。
    }
  }

  function handleVisibilityChange() {
    if (document.visibilityState !== 'visible') {
      return
    }
    refreshNoticeSurfaces()
  }

  function handleWindowFocus() {
    refreshNoticeSurfaces()
  }

  function navigateToPath(path?: string | null) {
    const normalizedPath = normalizeOptionalRoutePath(path)
    closeHeaderPanels()
    closeContentDrawers()
    if (normalizedPath && normalizedPath !== route.path) {
      router.push(normalizedPath)
    }
  }

  async function refreshRemoteNoticeContent(options: NoticeRefreshOptions = {}) {
    const userId = permissionStore.authContext?.userId
    if (!userId) {
      remoteNoticeController?.abort()
      remoteNoticeStatsController?.abort()
      remoteNoticeStatsRefreshTask = null
      remoteNoticeStatsLastRequestedAt = 0
      remoteNoticeMessages.value = []
      remoteNoticeStats.value = null
      remoteNoticeLoaded.value = false
      return
    }

    if (!options.force && remoteNoticeController) {
      return
    }

    const requestId = ++remoteNoticeRequestId
    remoteNoticeController?.abort()
    remoteNoticeStatsController?.abort()
    const controller = new AbortController()
    remoteNoticeController = controller
    remoteNoticeStatsLastRequestedAt = Date.now()

    try {
      const [messageResponse, statsResponse] = await Promise.all([
        pageMyInAppMessages({ pageNum: 1, pageSize: 6 }, { signal: controller.signal }),
        getMyInAppMessageUnreadStats({ signal: controller.signal })
      ])
      if (requestId !== remoteNoticeRequestId || controller.signal.aborted) {
        return
      }
      remoteNoticeMessages.value = messageResponse.data.records || []
      remoteNoticeStats.value = statsResponse.data
      remoteNoticeLoaded.value = true
    } catch (error) {
      if (requestId !== remoteNoticeRequestId || isAbortError(error)) {
        return
      }
      remoteNoticeMessages.value = []
      remoteNoticeStats.value = null
      remoteNoticeLoaded.value = false
    } finally {
      if (remoteNoticeController === controller) {
        remoteNoticeController = null
      }
    }
  }

  async function refreshRemoteNoticeStats(options: NoticeRefreshOptions = {}) {
    const userId = permissionStore.authContext?.userId
    if (!userId) {
      remoteNoticeStatsController?.abort()
      remoteNoticeStatsRefreshTask = null
      remoteNoticeStatsLastRequestedAt = 0
      remoteNoticeStats.value = null
      remoteNoticeLoaded.value = false
      return
    }

    const forceRefresh = options.force === true
    if (!forceRefresh) {
      // 壳层自动刷新只做轻量保活：复用进行中的请求，并对成功/失败场景分别限流，避免焦点恢复类事件持续放大后台压力。
      if (remoteNoticeController) {
        return
      }
      if (remoteNoticeStatsRefreshTask) {
        return remoteNoticeStatsRefreshTask
      }
      const minIntervalMs = remoteNoticeStats.value
        ? NOTICE_STATS_AUTO_REFRESH_INTERVAL_MS
        : NOTICE_STATS_ERROR_RETRY_INTERVAL_MS
      if (remoteNoticeStatsLastRequestedAt > 0 && Date.now() - remoteNoticeStatsLastRequestedAt < minIntervalMs) {
        return
      }
    }

    const requestId = ++remoteNoticeStatsRequestId
    remoteNoticeStatsController?.abort()
    const controller = new AbortController()
    remoteNoticeStatsController = controller
    remoteNoticeStatsLastRequestedAt = Date.now()
    let refreshTask: Promise<void> | null = null

    refreshTask = (async () => {
      try {
        const statsResponse = await getMyInAppMessageUnreadStats({
          signal: controller.signal
        })
        if (requestId !== remoteNoticeStatsRequestId || controller.signal.aborted) {
          return
        }
        remoteNoticeStats.value = statsResponse.data
      } catch (error) {
        if (requestId !== remoteNoticeStatsRequestId || isAbortError(error)) {
          return
        }
        remoteNoticeStats.value = null
        remoteNoticeLoaded.value = false
      } finally {
        if (remoteNoticeStatsController === controller) {
          remoteNoticeStatsController = null
        }
        if (remoteNoticeStatsRefreshTask === refreshTask) {
          remoteNoticeStatsRefreshTask = null
        }
      }
    })()
    remoteNoticeStatsRefreshTask = refreshTask
    return refreshTask
  }

  async function refreshRemoteHelpContent() {
    const userId = permissionStore.authContext?.userId
    if (!userId) {
      remoteHelpController?.abort()
      remoteHelpDocuments.value = []
      remoteHelpLoaded.value = false
      return
    }

    const requestId = ++remoteHelpRequestId
    remoteHelpController?.abort()
    const controller = new AbortController()
    remoteHelpController = controller

    try {
      const response = await listAccessibleHelpDocuments({
        currentPath: route.path,
        limit: 6
      }, { signal: controller.signal })
      if (requestId !== remoteHelpRequestId || controller.signal.aborted) {
        return
      }
      remoteHelpDocuments.value = response.data || []
      remoteHelpLoaded.value = true
    } catch (error) {
      if (requestId !== remoteHelpRequestId || isAbortError(error)) {
        return
      }
      remoteHelpDocuments.value = []
      remoteHelpLoaded.value = false
    } finally {
      if (remoteHelpController === controller) {
        remoteHelpController = null
      }
    }
  }

  async function refreshNoticeCenter() {
    const userId = permissionStore.authContext?.userId
    if (!userId) {
      buildFallbackNoticeEntries()
      return
    }

    noticeCenterLoading.value = true
    noticeCenterErrorMessage.value = ''
    noticeCenterController?.abort()
    const controller = new AbortController()
    noticeCenterController = controller

    try {
      const response = await pageMyInAppMessages({
        messageType: activeNoticeFilter.value === 'all' ? undefined : activeNoticeFilter.value,
        unreadOnly: unreadOnlyNotice.value || undefined,
        pageNum: noticeCenterPagination.pageNum,
        pageSize: noticeCenterPagination.pageSize
      }, { signal: controller.signal })
      if (controller.signal.aborted) {
        return
      }
      const pageData = response.data
      noticeCenterPagination.total = Number(pageData.total || 0)
      noticeCenterPagination.pageNum = Number(pageData.pageNum || noticeCenterPagination.pageNum)
      noticeCenterPagination.pageSize = Number(pageData.pageSize || noticeCenterPagination.pageSize)
      noticeCenterItems.value = (pageData.records || []).map((item) => mapNoticeRecord(item))
    } catch (error) {
      if (isAbortError(error)) {
        return
      }
      buildFallbackNoticeEntries()
    } finally {
      if (noticeCenterController === controller) {
        noticeCenterController = null
      }
      noticeCenterLoading.value = false
    }
  }

  async function refreshHelpCenter() {
    const userId = permissionStore.authContext?.userId
    if (!userId) {
      buildFallbackHelpEntries()
      return
    }

    helpCenterLoading.value = true
    helpCenterErrorMessage.value = ''
    helpCenterController?.abort()
    const controller = new AbortController()
    helpCenterController = controller

    try {
      const response = await pageAccessibleHelpDocuments({
        docCategory: activeHelpFilter.value === 'all' ? undefined : activeHelpFilter.value,
        keyword: helpKeyword.value.trim() || undefined,
        currentPath: route.path,
        pageNum: helpCenterPagination.pageNum,
        pageSize: helpCenterPagination.pageSize
      }, { signal: controller.signal })
      if (controller.signal.aborted) {
        return
      }
      const pageData = response.data
      helpCenterPagination.total = Number(pageData.total || 0)
      helpCenterPagination.pageNum = Number(pageData.pageNum || helpCenterPagination.pageNum)
      helpCenterPagination.pageSize = Number(pageData.pageSize || helpCenterPagination.pageSize)
      helpCenterItems.value = (pageData.records || []).map((item) => mapHelpRecord(item))
    } catch (error) {
      if (isAbortError(error)) {
        return
      }
      buildFallbackHelpEntries()
    } finally {
      if (helpCenterController === controller) {
        helpCenterController = null
      }
      helpCenterLoading.value = false
    }
  }

  function openCommandPalette() {
    closeHeaderPanels()
    closeContentDrawers()
    showCommandPalette.value = true
  }

  function selectCommandPath(path: string) {
    showCommandPalette.value = false
    if (path !== route.path) {
      router.push(path)
    }
  }

  function toggleNoticePanel() {
    const willOpen = !showNoticePanel.value
    closeContentDrawers()
    showNoticePanel.value = willOpen
    if (willOpen) {
      showHelpPanel.value = false
      void refreshRemoteNoticeContent()
    }
  }

  function toggleHelpPanel() {
    const willOpen = !showHelpPanel.value
    closeContentDrawers()
    showHelpPanel.value = willOpen
    if (willOpen) {
      showNoticePanel.value = false
    }
  }

  function openNoticeCenter() {
    closeHeaderPanels()
    showHelpCenterDrawer.value = false
    showNoticeDetailDrawer.value = false
    showHelpDetailDrawer.value = false
    showNoticeCenterDrawer.value = true
    void refreshRemoteNoticeStats({ force: true })
    void refreshNoticeCenter()
  }

  function openHelpCenter() {
    closeHeaderPanels()
    showNoticeCenterDrawer.value = false
    showNoticeDetailDrawer.value = false
    showHelpDetailDrawer.value = false
    showHelpCenterDrawer.value = true
    void refreshHelpCenter()
  }

  function resolveNoticeEntryFromPopover(item: ShellPopoverItem): ShellNoticeCenterEntry {
    if (item.resourceId) {
      const summaryRecord = remoteNoticeMessages.value.find((message) => String(message.id) === String(item.resourceId))
      if (summaryRecord) {
        return mapNoticeRecord(summaryRecord)
      }
    }
    return mapFallbackNoticeItem(item)
  }

  function resolveHelpEntryFromPopover(item: ShellPopoverItem): ShellHelpCenterEntry {
    if (item.resourceId) {
      const summaryRecord = remoteHelpDocuments.value.find((document) => String(document.id) === String(item.resourceId))
      if (summaryRecord) {
        return mapHelpRecord(summaryRecord)
      }
    }
    return mapFallbackHelpItem(item)
  }

  async function openNoticeDetail(entry: ShellNoticeCenterEntry) {
    closeHeaderPanels()
    showNoticeCenterDrawer.value = false
    showHelpCenterDrawer.value = false
    showHelpDetailDrawer.value = false
    noticeDetailErrorMessage.value = ''
    noticeDetailRecord.value = entry
    showNoticeDetailDrawer.value = true

    if (entry.fallback || !entry.resourceId) {
      noticeDetailLoading.value = false
      return
    }

    noticeDetailLoading.value = true
    noticeDetailController?.abort()
    const controller = new AbortController()
    noticeDetailController = controller
    try {
      const response = await getMyInAppMessage(entry.resourceId, { signal: controller.signal })
      if (controller.signal.aborted) {
        return
      }
      noticeDetailRecord.value = mapNoticeRecord(response.data)
    } catch (error) {
      if (!isAbortError(error)) {
        noticeDetailErrorMessage.value = ''
      }
    } finally {
      if (noticeDetailController === controller) {
        noticeDetailController = null
      }
      noticeDetailLoading.value = false
    }
  }

  async function openHelpDetail(entry: ShellHelpCenterEntry, keyword = helpKeyword.value) {
    closeHeaderPanels()
    showNoticeCenterDrawer.value = false
    showHelpCenterDrawer.value = false
    showNoticeDetailDrawer.value = false
    helpDetailErrorMessage.value = ''
    helpDetailKeyword.value = keyword
    helpDetailRecord.value = entry
    showHelpDetailDrawer.value = true

    if (entry.fallback || !entry.resourceId) {
      helpDetailLoading.value = false
      return
    }

    helpDetailLoading.value = true
    helpDetailController?.abort()
    const controller = new AbortController()
    helpDetailController = controller
    try {
      const response = await getAccessibleHelpDocument(entry.resourceId, route.path, { signal: controller.signal })
      if (controller.signal.aborted) {
        return
      }
      helpDetailRecord.value = mapHelpRecord(response.data)
    } catch (error) {
      if (!isAbortError(error)) {
        helpDetailErrorMessage.value = ''
      }
    } finally {
      if (helpDetailController === controller) {
        helpDetailController = null
      }
      helpDetailLoading.value = false
    }
  }

  async function markNoticeRead(entry: ShellNoticeCenterEntry) {
    if (entry.read) {
      return
    }
    if (entry.fallback || !entry.resourceId) {
      markFallbackNoticeRead(entry)
      emitNoticeSync('fallback-read')
      return
    }

    applyNoticeReadLocally(entry.resourceId)
    try {
      await markMyInAppMessageRead(entry.resourceId)
      emitNoticeSync('single-read')
    } catch {
      await refreshRemoteNoticeContent()
    }
    if (showNoticeCenterDrawer.value) {
      await refreshNoticeCenter()
    }
  }

  async function markAllNoticeRead() {
    if (!permissionStore.authContext?.userId || !remoteNoticeLoaded.value) {
      fallbackNoticeItems.value.forEach((item) => {
        readNoticeIds.value = Array.from(new Set([...readNoticeIds.value, item.id]))
      })
      if (showNoticeCenterDrawer.value) {
        buildFallbackNoticeEntries()
      }
      if (noticeDetailRecord.value && !noticeDetailRecord.value.read) {
        markFallbackNoticeRead(noticeDetailRecord.value)
      }
      emitNoticeSync('fallback-read-all')
      return
    }

    remoteNoticeMessages.value = remoteNoticeMessages.value.map((item) => ({
      ...item,
      read: true,
      readTime: item.readTime || new Date().toISOString()
    }))
    if (remoteNoticeStats.value) {
      remoteNoticeStats.value = {
        ...remoteNoticeStats.value,
        totalUnreadCount: 0,
        systemUnreadCount: 0,
        businessUnreadCount: 0,
        errorUnreadCount: 0
      }
    }
    noticeCenterItems.value = noticeCenterItems.value.map((item) => ({
      ...item,
      read: true,
      readTime: item.readTime || new Date().toISOString()
    }))
    if (noticeDetailRecord.value) {
      noticeDetailRecord.value = {
        ...noticeDetailRecord.value,
        read: true,
        readTime: noticeDetailRecord.value.readTime || new Date().toISOString()
      }
    }

    try {
      await markAllMyInAppMessagesRead()
      emitNoticeSync('read-all')
    } finally {
      await refreshRemoteNoticeContent()
      if (showNoticeCenterDrawer.value) {
        await refreshNoticeCenter()
      }
    }
  }

  function openNotice(item: ShellPopoverItem) {
    void openNoticeDetail(resolveNoticeEntryFromPopover(item))
  }

  function openHelp(item: ShellPopoverItem) {
    void openHelpDetail(resolveHelpEntryFromPopover(item), '')
  }

  function handlePopoverAction(actionId: string) {
    switch (actionId) {
      case 'notice-view-more':
        openNoticeCenter()
        return
      case 'notice-mark-all-read':
        void markAllNoticeRead()
        return
      case 'help-view-more':
        openHelpCenter()
        return
      default:
        return
    }
  }

  function handleNoticePageChange(page: number) {
    noticeCenterPagination.pageNum = page
    void refreshNoticeCenter()
  }

  function handleNoticePageSizeChange(size: number) {
    noticeCenterPagination.pageSize = size
    noticeCenterPagination.pageNum = 1
    void refreshNoticeCenter()
  }

  function handleHelpPageChange(page: number) {
    helpCenterPagination.pageNum = page
    void refreshHelpCenter()
  }

  function handleHelpPageSizeChange(size: number) {
    helpCenterPagination.pageSize = size
    helpCenterPagination.pageNum = 1
    void refreshHelpCenter()
  }

  function handleDocumentPointerDown(event: PointerEvent) {
    if (!showNoticePanel.value && !showHelpPanel.value) {
      return
    }
    const target = event.target as Node | null
    if (!target || headerRef.value?.contains(target)) {
      return
    }
    closeHeaderPanels()
  }

  function handleDocumentKeydown(event: KeyboardEvent) {
    const pressedKey = event.key.toLowerCase()
    if ((event.ctrlKey || event.metaKey) && pressedKey === 'k') {
      event.preventDefault()
      closeHeaderPanels()
      closeContentDrawers()
      showCommandPalette.value = !showCommandPalette.value
      return
    }

    if (event.key !== 'Escape') {
      return
    }
    if (showCommandPalette.value) {
      showCommandPalette.value = false
      return
    }
    if (!showNoticePanel.value && !showHelpPanel.value) {
      return
    }
    closeHeaderPanels()
  }

  watch(
    fallbackNoticeItems,
    (items) => {
      const validIds = new Set(items.map((item) => item.id))
      readNoticeIds.value = readNoticeIds.value.filter((id) => validIds.has(id))
    },
    { immediate: true }
  )

  watch(
    () => `${permissionStore.authContext?.userId || ''}|${allowedPathSignature.value}`,
    () => {
      void refreshRemoteNoticeStats()
    },
    { immediate: true }
  )

  watch(
    () => `${permissionStore.authContext?.userId || ''}|${route.path}|${allowedPathSignature.value}`,
    () => {
      void refreshRemoteHelpContent()
    },
    { immediate: true }
  )

  watch(showNoticeCenterDrawer, (visible) => {
    if (visible) {
      void refreshNoticeCenter()
    }
  })

  watch(showHelpCenterDrawer, (visible) => {
    if (visible) {
      void refreshHelpCenter()
    }
  })

  onMounted(() => {
    document.addEventListener('pointerdown', handleDocumentPointerDown)
    document.addEventListener('keydown', handleDocumentKeydown)
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('focus', handleWindowFocus)
    window.addEventListener('storage', handleStorageSync)
    if (typeof BroadcastChannel !== 'undefined') {
      noticeSyncChannel = new BroadcastChannel(NOTICE_SYNC_CHANNEL_NAME)
      noticeSyncChannel.addEventListener('message', (event) => {
        handleNoticeSyncPayload(event.data)
      })
    }
  })

  onBeforeUnmount(() => {
    document.removeEventListener('pointerdown', handleDocumentPointerDown)
    document.removeEventListener('keydown', handleDocumentKeydown)
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('focus', handleWindowFocus)
    window.removeEventListener('storage', handleStorageSync)
    noticeSyncChannel?.close()
    noticeSyncChannel = null
    abortControllers()
  })

  function updateNoticeFilter(value: ShellNoticeFilter) {
    activeNoticeFilter.value = value
    resetPagination(noticeCenterPagination)
    if (showNoticeCenterDrawer.value) {
      void refreshNoticeCenter()
    }
  }

  function updateNoticeUnreadOnly(value: boolean) {
    unreadOnlyNotice.value = value
    resetPagination(noticeCenterPagination)
    if (showNoticeCenterDrawer.value) {
      void refreshNoticeCenter()
    }
  }

  function updateHelpFilter(value: ShellHelpFilter) {
    activeHelpFilter.value = value
    resetPagination(helpCenterPagination)
    if (showHelpCenterDrawer.value) {
      void refreshHelpCenter()
    }
  }

  function updateHelpKeyword(value: string) {
    helpKeyword.value = value
  }

  function searchHelpCenter() {
    resetPagination(helpCenterPagination)
    if (showHelpCenterDrawer.value) {
      void refreshHelpCenter()
    }
  }

  return {
    showCommandPalette,
    commandKeyword,
    showNoticePanel,
    showHelpPanel,
    showNoticeCenterDrawer,
    showHelpCenterDrawer,
    showNoticeDetailDrawer,
    showHelpDetailDrawer,
    noticePanelId,
    helpPanelId,
    noticePopoverContent,
    unreadNoticeCount,
    helpPopoverContent,
    noticeCenterLoading,
    noticeCenterErrorMessage,
    noticeCenterItems,
    noticeCenterPagination,
    activeNoticeFilter,
    unreadOnlyNotice,
    helpCenterLoading,
    helpCenterErrorMessage,
    helpCenterItems,
    helpCenterPagination,
    activeHelpFilter,
    helpKeyword,
    noticeDetailLoading,
    noticeDetailErrorMessage,
    noticeDetailRecord,
    helpDetailLoading,
    helpDetailErrorMessage,
    helpDetailRecord,
    helpDetailKeyword,
    commandGroups,
    recentCommandItems,
    openCommandPalette,
    selectCommandPath,
    toggleNoticePanel,
    toggleHelpPanel,
    openNotice,
    openHelp,
    handlePopoverAction,
    openNoticeCenter,
    openHelpCenter,
    markNoticeRead,
    markAllNoticeRead,
    openNoticeDetail,
    openHelpDetail,
    navigateToPath,
    handleNoticePageChange,
    handleNoticePageSizeChange,
    handleHelpPageChange,
    handleHelpPageSizeChange,
    refreshNoticeCenter,
    refreshHelpCenter: async () => {
      searchHelpCenter()
    },
    closeHeaderPanels,
    closeContentDrawers,
    resetHeaderOverlays,
    handleNoticeFilterChange: updateNoticeFilter,
    handleNoticeUnreadOnlyChange: updateNoticeUnreadOnly,
    handleHelpFilterChange: updateHelpFilter,
    handleHelpKeywordChange: updateHelpKeyword,
    handleHelpSearch: searchHelpCenter
  } as ShellHeaderInteractionsState & {
    handleNoticeFilterChange: (value: ShellNoticeFilter) => void
    handleNoticeUnreadOnlyChange: (value: boolean) => void
    handleHelpFilterChange: (value: ShellHelpFilter) => void
    handleHelpKeywordChange: (value: string) => void
    handleHelpSearch: () => void
  }
}
