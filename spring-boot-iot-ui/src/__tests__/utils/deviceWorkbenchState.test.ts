import { describe, expect, it } from 'vitest'

import type { Device } from '@/types/api'
import {
  buildDevicePageCacheKey,
  cloneDeviceDetailCacheEntry,
  cloneDevicePageCacheEntry,
  createDeviceDetailCacheEntry,
  createDevicePageCacheEntry,
  deserializeDeviceDetailCacheEntries,
  deserializeDevicePageCacheEntries,
  getNextDevicePageQuery,
  getDeviceRowKey,
  isDeviceDetailCacheFresh,
  isDevicePageCacheFresh,
  matchesDeviceFilters,
  mergeLocalDeviceRow,
  prependLocalDeviceRow,
  removeLocalDeviceRow,
  removeSelectedDeviceSnapshot,
  resolveDevicePageLoadStrategy,
  replaceSelectedDeviceSnapshot,
  serializeDeviceDetailCacheEntries,
  serializeDevicePageCacheEntries
} from '@/views/deviceWorkbenchState'

function createDevice(overrides: Partial<Device> = {}): Device {
  return {
    id: '2001',
    productId: 101,
    productKey: 'pressure-pump',
    productName: '压力泵产品',
    deviceName: '一号泵站设备',
    deviceCode: 'device-001',
    deviceSecret: 'secret-001',
    clientId: 'client-001',
    username: 'user-001',
    password: 'pwd-001',
    protocolCode: 'mqtt-json',
    nodeType: 1,
    onlineStatus: 1,
    activateStatus: 1,
    deviceStatus: 1,
    firmwareVersion: 'v1.0.0',
    ipAddress: '10.0.0.1',
    address: '一号泵房',
    metadataJson: '{"site":"A01"}',
    lastOnlineTime: '2026-03-20 10:00:00',
    lastOfflineTime: '2026-03-19 09:00:00',
    lastReportTime: '2026-03-20 10:05:00',
    createTime: '2026-03-20 09:00:00',
    updateTime: '2026-03-20 10:10:00',
    ...overrides
  }
}

describe('deviceWorkbenchState', () => {
  it('prefers id for row key and falls back to deviceCode', () => {
    expect(getDeviceRowKey(createDevice())).toBe('2001')
    expect(getDeviceRowKey(createDevice({ id: '', deviceCode: 'fallback-code' }))).toBe('fallback-code')
    expect(getDeviceRowKey(null)).toBe('')
  })

  it('matches device filters by id, keywords and statuses', () => {
    const device = createDevice({
      id: '9001',
      productKey: 'pump-core',
      deviceCode: 'pump-9001',
      deviceName: '九号泵设备',
      onlineStatus: 0,
      activateStatus: 1,
      deviceStatus: 0
    })

    expect(
      matchesDeviceFilters(device, {
        deviceId: '9001',
        productKey: 'pump',
        deviceCode: '9001',
        deviceName: '九号',
        onlineStatus: 0,
        activateStatus: 1,
        deviceStatus: 0
      })
    ).toBe(true)

    expect(
      matchesDeviceFilters(device, {
        deviceId: '',
        productKey: 'meter',
        deviceCode: '',
        deviceName: '',
        onlineStatus: undefined,
        activateStatus: undefined,
        deviceStatus: undefined
      })
    ).toBe(false)
  })

  it('builds, clones and validates device page cache entries', () => {
    const query = {
      deviceId: '  2001  ',
      productKey: '  pressure-pump  ',
      deviceCode: '  device-001  ',
      deviceName: '  一号泵站设备  ',
      onlineStatus: 1,
      activateStatus: 1,
      deviceStatus: 1,
      pageNum: 1,
      pageSize: 10
    }
    const pageResult = {
      total: 26,
      pageNum: 1,
      pageSize: 10,
      records: [createDevice()]
    }

    expect(buildDevicePageCacheKey(query)).toBe('2001|pressure-pump|device-001|一号泵站设备|1|1|1|1|10')

    const cacheEntry = createDevicePageCacheEntry(query, pageResult, 1_000)
    expect(cacheEntry.key).toBe('2001|pressure-pump|device-001|一号泵站设备|1|1|1|1|10')
    expect(cacheEntry.records[0].deviceName).toBe('一号泵站设备')

    const cloned = cloneDevicePageCacheEntry(cacheEntry)
    cloned?.records[0] && (cloned.records[0].deviceName = '已变更名称')
    expect(cacheEntry.records[0].deviceName).toBe('一号泵站设备')

    expect(isDevicePageCacheFresh(cacheEntry, 5_000, 5_999)).toBe(true)
    expect(isDevicePageCacheFresh(cacheEntry, 5_000, 6_001)).toBe(false)
  })

  it('serializes and restores recent device page cache entries for session reuse', () => {
    const firstEntry = createDevicePageCacheEntry(
      {
        deviceId: '',
        productKey: 'pressure-pump',
        deviceCode: '',
        deviceName: '',
        onlineStatus: undefined,
        activateStatus: undefined,
        deviceStatus: undefined,
        pageNum: 1,
        pageSize: 10
      },
      {
        total: 26,
        pageNum: 1,
        pageSize: 10,
        records: [createDevice({ id: '2001', deviceName: '设备A' })]
      },
      2_000
    )
    const secondEntry = createDevicePageCacheEntry(
      {
        deviceId: '',
        productKey: 'pressure-pump',
        deviceCode: '',
        deviceName: '',
        onlineStatus: undefined,
        activateStatus: undefined,
        deviceStatus: undefined,
        pageNum: 2,
        pageSize: 10
      },
      {
        total: 26,
        pageNum: 2,
        pageSize: 10,
        records: [createDevice({ id: '2002', deviceName: '设备B' })]
      },
      3_000
    )

    const serialized = serializeDevicePageCacheEntries([firstEntry, secondEntry], 8)
    const restored = deserializeDevicePageCacheEntries(serialized, 10_000, 8, 5_000)

    expect(restored.map((item) => item.key)).toEqual([secondEntry.key, firstEntry.key])
    expect(restored[0]?.records[0]?.deviceName).toBe('设备B')
  })

  it('builds, clones and restores fresh device detail cache entries', () => {
    const firstEntry = createDeviceDetailCacheEntry(createDevice({ id: '2001', deviceName: 'device-a' }), 2_000)
    const secondEntry = createDeviceDetailCacheEntry(createDevice({ id: '2002', deviceName: 'device-b' }), 3_000)

    const cloned = cloneDeviceDetailCacheEntry(secondEntry)
    if (cloned) {
      cloned.detail.deviceName = 'device-changed'
    }
    expect(secondEntry.detail.deviceName).toBe('device-b')

    expect(isDeviceDetailCacheFresh(firstEntry, 2_500, 4_000)).toBe(true)
    expect(isDeviceDetailCacheFresh(firstEntry, 1_000, 4_001)).toBe(false)

    const serialized = serializeDeviceDetailCacheEntries([firstEntry, secondEntry], 8)
    const restored = deserializeDeviceDetailCacheEntries(serialized, 10_000, 8, 5_000)

    expect(restored.map((item) => item.key)).toEqual(['2002', '2001'])
    expect(restored[0]?.detail.deviceName).toBe('device-b')
  })

  it('drops stale or invalid device page cache entries when restoring session cache', () => {
    const freshEntry = createDevicePageCacheEntry(
      {
        deviceId: '',
        productKey: 'pressure-pump',
        deviceCode: '',
        deviceName: '',
        onlineStatus: undefined,
        activateStatus: undefined,
        deviceStatus: undefined,
        pageNum: 1,
        pageSize: 10
      },
      {
        total: 26,
        pageNum: 1,
        pageSize: 10,
        records: [createDevice()]
      },
      5_000
    )

    const payload = JSON.stringify({
      version: 1,
      entries: [
        { ...freshEntry, cachedAt: 1_000 },
        freshEntry,
        { key: '', pageNum: 'bad', pageSize: 10, total: 0, records: [], cachedAt: 5_000 }
      ]
    })

    const restored = deserializeDevicePageCacheEntries(payload, 1_500, 8, 6_000)
    expect(restored).toHaveLength(1)
    expect(restored[0]?.key).toBe(freshEntry.key)
  })

  it('builds next page query only when there are more records to prefetch', () => {
    const nextPage = getNextDevicePageQuery(
      {
        deviceId: '',
        productKey: 'pressure-pump',
        deviceCode: 'device',
        deviceName: '',
        onlineStatus: 1,
        activateStatus: 1,
        deviceStatus: 1,
        pageNum: 1,
        pageSize: 10
      },
      26
    )
    expect(nextPage).toEqual({
      deviceId: '',
      productKey: 'pressure-pump',
      deviceCode: 'device',
      deviceName: '',
      onlineStatus: 1,
      activateStatus: 1,
      deviceStatus: 1,
      pageNum: 2,
      pageSize: 10
    })

    expect(
      getNextDevicePageQuery(
        {
          deviceId: '',
          productKey: 'pressure-pump',
          deviceCode: 'device',
          deviceName: '',
          onlineStatus: 1,
          activateStatus: 1,
          deviceStatus: 1,
          pageNum: 3,
          pageSize: 10
        },
        26
      )
    ).toBeNull()
  })

  it('uses fresh page cache only when request is not forced', () => {
    expect(
      resolveDevicePageLoadStrategy({
        hasCachedPage: true,
        hasFreshCache: true
      })
    ).toEqual({
      useFreshCacheOnly: true,
      silentRequest: false
    })

    expect(
      resolveDevicePageLoadStrategy({
        hasCachedPage: true,
        hasFreshCache: true,
        silent: true,
        force: true
      })
    ).toEqual({
      useFreshCacheOnly: false,
      silentRequest: true
    })
  })

  it('keeps cache-backed requests silent without blocking forced revalidation', () => {
    expect(
      resolveDevicePageLoadStrategy({
        hasCachedPage: true,
        hasFreshCache: false
      })
    ).toEqual({
      useFreshCacheOnly: false,
      silentRequest: true
    })

    expect(
      resolveDevicePageLoadStrategy({
        hasCachedPage: false,
        hasFreshCache: false,
        silent: true,
        force: true
      })
    ).toEqual({
      useFreshCacheOnly: false,
      silentRequest: true
    })
  })

  it('replaces and removes selected device snapshots by row key', () => {
    const source = [
      createDevice({ id: '2001', deviceName: '旧设备A' }),
      createDevice({ id: '2002', deviceCode: 'device-b', deviceName: '旧设备B' })
    ]

    const replaced = replaceSelectedDeviceSnapshot(source, createDevice({ id: '2002', deviceName: '新设备B' }))
    expect(replaced[1].deviceName).toBe('新设备B')

    const removed = removeSelectedDeviceSnapshot(replaced, createDevice({ id: '2001' }))
    expect(removed.map((item) => item.id)).toEqual(['2002'])
  })

  it('merges, prepends and removes local table rows', () => {
    const source = [
      createDevice({ id: '2001', deviceName: '设备A' }),
      createDevice({ id: '2002', deviceCode: 'device-b', deviceName: '设备B' }),
      createDevice({ id: '2003', deviceCode: 'device-c', deviceName: '设备C' })
    ]

    const merged = mergeLocalDeviceRow(source, createDevice({ id: '2002', deviceName: '设备B-已更新' }))
    expect(merged?.[1].deviceName).toBe('设备B-已更新')

    const prepended = prependLocalDeviceRow(source, createDevice({ id: '2004', deviceCode: 'device-d', deviceName: '设备D' }), 3)
    expect(prepended.map((item) => item.id)).toEqual(['2004', '2001', '2002'])

    const removed = removeLocalDeviceRow(source, createDevice({ id: '2002' }))
    expect(removed?.map((item) => item.id)).toEqual(['2001', '2003'])
  })
})
