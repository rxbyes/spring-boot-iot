import { computed, defineComponent, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  pushMock: vi.fn(),
  useRouteMock: vi.fn(),
  useRouterMock: vi.fn(),
  usePermissionStoreMock: vi.fn(),
  pageMyInAppMessagesMock: vi.fn(),
  getMyInAppMessageUnreadStatsMock: vi.fn(),
  getMyInAppMessageMock: vi.fn(),
  markMyInAppMessageReadMock: vi.fn(),
  markAllMyInAppMessagesReadMock: vi.fn(),
  listAccessibleHelpDocumentsMock: vi.fn(),
  pageAccessibleHelpDocumentsMock: vi.fn(),
  getAccessibleHelpDocumentMock: vi.fn()
}))

class BroadcastChannelMock {
  static instances: BroadcastChannelMock[] = []

  readonly name: string

  private listeners = new Set<(event: MessageEvent) => void>()

  constructor(name: string) {
    this.name = name
    BroadcastChannelMock.instances.push(this)
  }

  addEventListener(_type: string, listener: (event: MessageEvent) => void) {
    this.listeners.add(listener)
  }

  postMessage(_data: unknown) {
    return undefined
  }

  emit(data: unknown) {
    this.listeners.forEach((listener) => listener({ data } as MessageEvent))
  }

  close() {
    this.listeners.clear()
  }

  static reset() {
    BroadcastChannelMock.instances = []
  }
}

vi.mock('vue-router', () => ({
  useRoute: mocks.useRouteMock,
  useRouter: mocks.useRouterMock
}))

vi.mock('@/stores/permission', () => ({
  usePermissionStore: mocks.usePermissionStoreMock
}))

vi.mock('@/stores/activity', () => ({
  activityEntries: ref([])
}))

vi.mock('@/api/inAppMessage', () => ({
  pageMyInAppMessages: mocks.pageMyInAppMessagesMock,
  getMyInAppMessageUnreadStats: mocks.getMyInAppMessageUnreadStatsMock,
  getMyInAppMessage: mocks.getMyInAppMessageMock,
  markMyInAppMessageRead: mocks.markMyInAppMessageReadMock,
  markAllMyInAppMessagesRead: mocks.markAllMyInAppMessagesReadMock
}))

vi.mock('@/api/helpDoc', () => ({
  listAccessibleHelpDocuments: mocks.listAccessibleHelpDocumentsMock,
  pageAccessibleHelpDocuments: mocks.pageAccessibleHelpDocumentsMock,
  getAccessibleHelpDocument: mocks.getAccessibleHelpDocumentMock
}))

import { useShellHeaderInteractions } from '@/composables/useShellHeaderInteractions'

function flushPromises() {
  return new Promise((resolve) => setTimeout(resolve, 0))
}

function createDeferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function createShellOptions() {
  return {
    headerRef: ref(null),
    navigationGroups: computed(() => [{
      key: 'iot-access',
      label: 'Ops',
      description: 'desc',
      menuTitle: 'Ops',
      menuHint: 'hint',
      items: [
        { to: '/system-log', label: 'System Log', caption: 'caption', short: 'SL' }
      ]
    }]),
    flattenedItems: computed(() => [
      { to: '/system-log', label: 'System Log', caption: 'caption', short: 'SL' }
    ]),
    activeGroup: computed(() => ({
      key: 'iot-access',
      label: 'Ops',
      description: 'desc',
      menuTitle: 'Ops',
      menuHint: 'hint',
      items: [
        { to: '/system-log', label: 'System Log', caption: 'caption', short: 'SL' }
      ]
    }))
  }
}

function mountSubject() {
  let state!: ReturnType<typeof useShellHeaderInteractions>
  const wrapper = mount(defineComponent({
    setup() {
      state = useShellHeaderInteractions(createShellOptions())
      return () => null
    }
  }))
  return { state, wrapper }
}

describe('useShellHeaderInteractions', () => {
  beforeEach(() => {
    BroadcastChannelMock.reset()
    vi.stubGlobal('BroadcastChannel', BroadcastChannelMock)
    Object.defineProperty(document, 'visibilityState', {
      configurable: true,
      value: 'visible'
    })
    mocks.pushMock.mockReset()
    mocks.useRouteMock.mockReturnValue({
      path: '/system-log'
    })
    mocks.useRouterMock.mockReturnValue({
      push: mocks.pushMock
    })
    mocks.usePermissionStoreMock.mockReturnValue({
      authContext: {
        userId: '1'
      },
      roleProfile: {
        key: 'ops',
        label: 'Ops',
        roleCodes: ['OPS_STAFF'],
        roleNameKeywords: ['ops'],
        defaultPath: '/device-access',
        preferredWorkspaceKeys: ['iot-access', 'risk-ops'],
        featuredPaths: ['/devices', '/message-trace', '/system-log'],
        cockpitRole: 'ops',
        focusLabel: 'Ops Focus',
        focusDescription: 'Focus on message flow and diagnostics.'
      },
      homePath: '/device-access',
      allowedPaths: ['/devices', '/message-trace', '/system-log', '/alarm-center'],
      hasRoutePermission: vi.fn(() => true)
    })

    mocks.pageMyInAppMessagesMock.mockImplementation((params?: { pageSize?: number }) => {
      const pageSize = params?.pageSize || 10
      return Promise.resolve({
        data: {
          total: 1,
          pageNum: 1,
          pageSize,
          records: [
            {
              id: '1',
              messageType: 'system',
              priority: 'high',
              title: 'Maintenance Notice',
              summary: 'Scheduled maintenance tonight.',
              content: 'Full maintenance detail',
              targetType: 'all',
              relatedPath: '/system-log',
              sourceType: 'system',
              sourceId: 'maint-1',
              publishTime: '2026-03-21 23:00:00',
              expireTime: null,
              read: false,
              readTime: null
            }
          ]
        }
      })
    })
    mocks.getMyInAppMessageUnreadStatsMock.mockResolvedValue({
      data: {
        totalUnreadCount: 1,
        systemUnreadCount: 1,
        businessUnreadCount: 0,
        errorUnreadCount: 0
      }
    })
    mocks.getMyInAppMessageMock.mockResolvedValue({
      data: {
        id: '1',
        messageType: 'system',
        priority: 'high',
        title: 'Maintenance Notice',
        summary: 'Scheduled maintenance tonight.',
        content: 'Full maintenance detail',
        targetType: 'all',
        relatedPath: '/system-log',
        sourceType: 'system',
        sourceId: 'maint-1',
        publishTime: '2026-03-21 23:00:00',
        expireTime: null,
        read: false,
        readTime: null
      }
    })
    mocks.markMyInAppMessageReadMock.mockResolvedValue({ data: undefined })
    mocks.markAllMyInAppMessagesReadMock.mockResolvedValue({ data: undefined })
    mocks.listAccessibleHelpDocumentsMock.mockResolvedValue({ data: [] })
    mocks.pageAccessibleHelpDocumentsMock.mockResolvedValue({
      data: {
        total: 0,
        pageNum: 1,
        pageSize: 10,
        records: []
      }
    })
    mocks.getAccessibleHelpDocumentMock.mockResolvedValue({ data: null })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('keeps unread count until explicit read action and opens center/detail flows', async () => {
    const { state, wrapper } = mountSubject()

    await flushPromises()

    expect(state.unreadNoticeCount.value).toBe(1)
    state.toggleNoticePanel()
    await flushPromises()

    expect(state.showNoticePanel.value).toBe(true)
    expect(state.unreadNoticeCount.value).toBe(1)
    expect(mocks.markAllMyInAppMessagesReadMock).not.toHaveBeenCalled()

    state.handlePopoverAction('notice-view-more')
    await flushPromises()

    expect(state.showNoticeCenterDrawer.value).toBe(true)
    expect(state.noticeCenterItems.value).toHaveLength(1)

    await state.openNoticeDetail(state.noticeCenterItems.value[0])
    await flushPromises()

    expect(mocks.getMyInAppMessageMock).toHaveBeenCalledWith('1', expect.any(Object))
    expect(state.noticeDetailRecord.value?.content).toContain('Full maintenance detail')

    await state.markNoticeRead(state.noticeCenterItems.value[0])

    expect(mocks.markMyInAppMessageReadMock).toHaveBeenCalledWith('1')
    expect(state.unreadNoticeCount.value).toBe(0)
    expect(state.noticeCenterItems.value[0].read).toBe(true)

    wrapper.unmount()
  })

  it('loads unread stats on mount without preloading notice page records', async () => {
    const { state, wrapper } = mountSubject()

    await flushPromises()

    expect(mocks.getMyInAppMessageUnreadStatsMock).toHaveBeenCalledTimes(1)
    expect(mocks.getMyInAppMessageUnreadStatsMock).toHaveBeenCalledWith(
      expect.objectContaining({
        signal: expect.any(AbortSignal)
      })
    )
    expect(mocks.pageMyInAppMessagesMock).not.toHaveBeenCalled()
    expect(state.unreadNoticeCount.value).toBe(1)
    wrapper.unmount()
  })

  it('refreshes notice summary when tab becomes visible again after the auto refresh window expires', async () => {
    const now = vi.spyOn(Date, 'now')
    now.mockReturnValue(1_000)
    const { state, wrapper } = mountSubject()

    await flushPromises()
    const initialPageCalls = mocks.pageMyInAppMessagesMock.mock.calls.length
    const initialStatsCalls = mocks.getMyInAppMessageUnreadStatsMock.mock.calls.length

    now.mockReturnValue(62_000)
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()

    expect(mocks.getMyInAppMessageUnreadStatsMock.mock.calls.length).toBeGreaterThan(initialStatsCalls)
    expect(mocks.pageMyInAppMessagesMock.mock.calls.length).toBe(initialPageCalls)
    expect(state.unreadNoticeCount.value).toBe(1)
    wrapper.unmount()
    now.mockRestore()
  })

  it('skips repeated auto stats refreshes while unread stats are still fresh', async () => {
    const now = vi.spyOn(Date, 'now')
    now.mockReturnValue(1_000)
    const { wrapper } = mountSubject()

    await flushPromises()
    expect(mocks.getMyInAppMessageUnreadStatsMock).toHaveBeenCalledTimes(1)

    now.mockReturnValue(2_000)
    document.dispatchEvent(new Event('visibilitychange'))
    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(mocks.getMyInAppMessageUnreadStatsMock).toHaveBeenCalledTimes(1)
    wrapper.unmount()
    now.mockRestore()
  })

  it('reuses the pending auto stats request instead of issuing another one', async () => {
    const statsDeferred = createDeferred<{
      data: {
        totalUnreadCount: number
        systemUnreadCount: number
        businessUnreadCount: number
        errorUnreadCount: number
      }
    }>()

    mocks.getMyInAppMessageUnreadStatsMock.mockReset()
    mocks.getMyInAppMessageUnreadStatsMock.mockImplementation(() => statsDeferred.promise)

    const { wrapper } = mountSubject()
    await flushPromises()

    document.dispatchEvent(new Event('visibilitychange'))
    window.dispatchEvent(new Event('focus'))
    await flushPromises()

    expect(mocks.getMyInAppMessageUnreadStatsMock).toHaveBeenCalledTimes(1)

    statsDeferred.resolve({
      data: {
        totalUnreadCount: 1,
        systemUnreadCount: 1,
        businessUnreadCount: 0,
        errorUnreadCount: 0
      }
    })
    await flushPromises()
    wrapper.unmount()
  })

  it('syncs unread state from broadcast channel events across tabs', async () => {
    const { state, wrapper } = mountSubject()

    await flushPromises()
    const initialPageCalls = mocks.pageMyInAppMessagesMock.mock.calls.length
    const initialStatsCalls = mocks.getMyInAppMessageUnreadStatsMock.mock.calls.length

    BroadcastChannelMock.instances[0]?.emit({
      reason: 'read-all',
      timestamp: Date.now(),
      userId: '1'
    })
    await flushPromises()

    expect(mocks.getMyInAppMessageUnreadStatsMock.mock.calls.length).toBeGreaterThan(initialStatsCalls)
    expect(mocks.pageMyInAppMessagesMock.mock.calls.length).toBe(initialPageCalls)
    expect(state.unreadNoticeCount.value).toBe(1)
    wrapper.unmount()
  })
})
