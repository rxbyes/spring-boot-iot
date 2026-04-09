import type { Device, PageResult } from '@/types/api'

export interface DeviceFilterSnapshot {
  deviceId: string
  productKey: string
  deviceCode: string
  deviceName: string
  onlineStatus?: number
  activateStatus?: number
  deviceStatus?: number
  registrationStatus?: number
}

export interface DevicePageQuerySnapshot extends DeviceFilterSnapshot {
  pageNum: number
  pageSize: number
}

export interface DevicePageCacheEntry {
  key: string
  pageNum: number
  pageSize: number
  total: number
  records: Device[]
  cachedAt: number
}

export interface DeviceDetailCacheEntry {
  key: string
  detail: Device
  cachedAt: number
}

export interface DevicePageLoadStrategy {
  useFreshCacheOnly: boolean
  silentRequest: boolean
}

interface DevicePageCachePayload {
  version?: number
  entries?: unknown
}

interface DeviceDetailCachePayload {
  version?: number
  entries?: unknown
}

function normalizeSearchText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  return String(value).trim().toLowerCase()
}

function normalizeComparableText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  return String(value)
}

function normalizePositiveInt(value: number, fallback: number) {
  const normalized = Number(value)
  if (!Number.isFinite(normalized) || normalized < 1) {
    return fallback
  }
  return Math.trunc(normalized)
}

export function buildDevicePageCacheKey(query: DevicePageQuerySnapshot) {
  return [
    normalizeSearchText(query.deviceId),
    normalizeSearchText(query.productKey),
    normalizeSearchText(query.deviceCode),
    normalizeSearchText(query.deviceName),
    query.onlineStatus ?? '',
    query.activateStatus ?? '',
    query.deviceStatus ?? '',
    query.registrationStatus ?? '',
    normalizePositiveInt(query.pageNum, 1),
    normalizePositiveInt(query.pageSize, 10)
  ].join('|')
}

export function createDevicePageCacheEntry(
  query: DevicePageQuerySnapshot,
  pageResult: PageResult<Device>,
  cachedAt = Date.now()
): DevicePageCacheEntry {
  const pageNum = normalizePositiveInt(Number(pageResult.pageNum || query.pageNum), 1)
  const pageSize = normalizePositiveInt(Number(pageResult.pageSize || query.pageSize), 10)
  const normalizedQuery = {
    ...query,
    deviceId: query.deviceId.trim(),
    productKey: query.productKey.trim(),
    deviceCode: query.deviceCode.trim(),
    deviceName: query.deviceName.trim(),
    pageNum,
    pageSize
  }

  return {
    key: buildDevicePageCacheKey(normalizedQuery),
    pageNum,
    pageSize,
    total: Number(pageResult.total || 0),
    records: (pageResult.records || []).map((item) => ({ ...item })),
    cachedAt
  }
}

export function cloneDevicePageCacheEntry(entry?: DevicePageCacheEntry | null) {
  if (!entry) {
    return null
  }
  return {
    ...entry,
    records: entry.records.map((item) => ({ ...item }))
  }
}

export function isDevicePageCacheFresh(entry: DevicePageCacheEntry | null | undefined, ttlMs: number, now = Date.now()) {
  if (!entry || ttlMs < 0) {
    return false
  }
  return now - entry.cachedAt <= ttlMs
}

function normalizeDevicePageCacheEntry(entry: unknown) {
  if (!entry || typeof entry !== 'object') {
    return null
  }

  const candidate = entry as Record<string, unknown>
  if (typeof candidate.key !== 'string' || !candidate.key.trim()) {
    return null
  }

  const pageNum = normalizePositiveInt(Number(candidate.pageNum), 1)
  const pageSize = normalizePositiveInt(Number(candidate.pageSize), 10)
  const total = Math.max(0, Number(candidate.total) || 0)
  const cachedAt = Number(candidate.cachedAt)
  if (!Number.isFinite(cachedAt) || cachedAt < 0) {
    return null
  }

  const rawRecords = Array.isArray(candidate.records) ? candidate.records : []
  const records = rawRecords
    .filter((item) => item && typeof item === 'object')
    .map((item) => ({ ...(item as Device) }))

  return {
    key: candidate.key,
    pageNum,
    pageSize,
    total,
    records,
    cachedAt
  } satisfies DevicePageCacheEntry
}

export function deserializeDevicePageCacheEntries(raw: string | null | undefined, ttlMs: number, maxEntries: number, now = Date.now()) {
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as DevicePageCachePayload | unknown[]
    const rawEntries = Array.isArray(parsed)
      ? parsed
      : Array.isArray((parsed as DevicePageCachePayload)?.entries)
        ? (parsed as DevicePageCachePayload).entries || []
        : []

    return rawEntries
      .map((entry) => normalizeDevicePageCacheEntry(entry))
      .filter((entry): entry is DevicePageCacheEntry => Boolean(entry))
      .filter((entry) => isDevicePageCacheFresh(entry, ttlMs, now))
      .sort((left, right) => right.cachedAt - left.cachedAt)
      .slice(0, Math.max(0, maxEntries))
      .map((entry) => cloneDevicePageCacheEntry(entry) as DevicePageCacheEntry)
  } catch {
    return []
  }
}

export function serializeDevicePageCacheEntries(entries: Iterable<DevicePageCacheEntry>, maxEntries: number) {
  const normalizedEntries = Array.from(entries)
    .map((entry) => cloneDevicePageCacheEntry(entry))
    .filter((entry): entry is DevicePageCacheEntry => Boolean(entry))
    .sort((left, right) => right.cachedAt - left.cachedAt)
    .slice(0, Math.max(0, maxEntries))

  return JSON.stringify({
    version: 1,
    entries: normalizedEntries
  })
}

export function createDeviceDetailCacheEntry(device: Device, cachedAt = Date.now()): DeviceDetailCacheEntry {
  return {
    key: getDeviceRowKey(device),
    detail: { ...device },
    cachedAt
  }
}

export function cloneDeviceDetailCacheEntry(entry?: DeviceDetailCacheEntry | null) {
  if (!entry) {
    return null
  }
  return {
    ...entry,
    detail: { ...entry.detail }
  }
}

export function isDeviceDetailCacheFresh(entry: DeviceDetailCacheEntry | null | undefined, ttlMs: number, now = Date.now()) {
  if (!entry || ttlMs < 0) {
    return false
  }
  return now - entry.cachedAt <= ttlMs
}

function normalizeDeviceDetailCacheEntry(entry: unknown) {
  if (!entry || typeof entry !== 'object') {
    return null
  }

  const candidate = entry as Record<string, unknown>
  if (typeof candidate.key !== 'string' || !candidate.key.trim()) {
    return null
  }

  const cachedAt = Number(candidate.cachedAt)
  if (!Number.isFinite(cachedAt) || cachedAt < 0) {
    return null
  }

  if (!candidate.detail || typeof candidate.detail !== 'object' || Array.isArray(candidate.detail)) {
    return null
  }

  return {
    key: candidate.key,
    detail: { ...(candidate.detail as Device) },
    cachedAt
  } satisfies DeviceDetailCacheEntry
}

export function deserializeDeviceDetailCacheEntries(raw: string | null | undefined, ttlMs: number, maxEntries: number, now = Date.now()) {
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as DeviceDetailCachePayload | unknown[]
    const rawEntries = Array.isArray(parsed)
      ? parsed
      : Array.isArray((parsed as DeviceDetailCachePayload)?.entries)
        ? (parsed as DeviceDetailCachePayload).entries || []
        : []

    return rawEntries
      .map((entry) => normalizeDeviceDetailCacheEntry(entry))
      .filter((entry): entry is DeviceDetailCacheEntry => Boolean(entry))
      .filter((entry) => isDeviceDetailCacheFresh(entry, ttlMs, now))
      .sort((left, right) => right.cachedAt - left.cachedAt)
      .slice(0, Math.max(0, maxEntries))
      .map((entry) => cloneDeviceDetailCacheEntry(entry) as DeviceDetailCacheEntry)
  } catch {
    return []
  }
}

export function serializeDeviceDetailCacheEntries(entries: Iterable<DeviceDetailCacheEntry>, maxEntries: number) {
  const normalizedEntries = Array.from(entries)
    .map((entry) => cloneDeviceDetailCacheEntry(entry))
    .filter((entry): entry is DeviceDetailCacheEntry => Boolean(entry))
    .sort((left, right) => right.cachedAt - left.cachedAt)
    .slice(0, Math.max(0, maxEntries))

  return JSON.stringify({
    version: 1,
    entries: normalizedEntries
  })
}

export function resolveDevicePageLoadStrategy(options: {
  hasCachedPage: boolean
  hasFreshCache: boolean
  force?: boolean
  silent?: boolean
}): DevicePageLoadStrategy {
  const force = options.force === true
  const useFreshCacheOnly = options.hasFreshCache && !force

  return {
    useFreshCacheOnly,
    silentRequest: !useFreshCacheOnly && (options.silent === true || (options.hasCachedPage && !force))
  }
}

export function getNextDevicePageQuery(query: DevicePageQuerySnapshot, total: number) {
  const pageNum = normalizePositiveInt(query.pageNum, 1)
  const pageSize = normalizePositiveInt(query.pageSize, 10)
  const normalizedTotal = Math.max(0, Number(total) || 0)
  if (pageNum * pageSize >= normalizedTotal) {
    return null
  }
  return {
    ...query,
    deviceId: query.deviceId.trim(),
    productKey: query.productKey.trim(),
    deviceCode: query.deviceCode.trim(),
    deviceName: query.deviceName.trim(),
    pageNum: pageNum + 1,
    pageSize
  }
}

export function getDeviceRowKey(row?: Partial<Device> | null) {
  if (!row) {
    return ''
  }
  if (row.id !== undefined && row.id !== null && row.id !== '') {
    return String(row.id)
  }
  return row.deviceCode ? String(row.deviceCode) : ''
}

export function matchesDeviceFilters(device: Device, filters: DeviceFilterSnapshot) {
  const deviceId = filters.deviceId.trim()
  if (deviceId && String(device.id || '') !== deviceId) {
    return false
  }

  const productKey = filters.productKey.trim().toLowerCase()
  if (productKey && !String(device.productKey || '').toLowerCase().includes(productKey)) {
    return false
  }

  const deviceCode = filters.deviceCode.trim().toLowerCase()
  if (deviceCode && !String(device.deviceCode || '').toLowerCase().includes(deviceCode)) {
    return false
  }

  const deviceName = filters.deviceName.trim().toLowerCase()
  if (deviceName && !String(device.deviceName || '').toLowerCase().includes(deviceName)) {
    return false
  }

  if (filters.onlineStatus !== undefined && device.onlineStatus !== filters.onlineStatus) {
    return false
  }
  if (filters.activateStatus !== undefined && device.activateStatus !== filters.activateStatus) {
    return false
  }
  if (filters.deviceStatus !== undefined && device.deviceStatus !== filters.deviceStatus) {
    return false
  }
  if (filters.registrationStatus !== undefined && device.registrationStatus !== filters.registrationStatus) {
    return false
  }

  return true
}

export function shouldRefreshDeviceDetail(row: Device, cachedDetail: Device | null) {
  if (!cachedDetail) {
    return true
  }

  const rowUpdateTime = normalizeComparableText(row.updateTime)
  const cachedUpdateTime = normalizeComparableText(cachedDetail.updateTime)
  if (rowUpdateTime && cachedUpdateTime) {
    return rowUpdateTime !== cachedUpdateTime
  }

  return !(
    cachedDetail.clientId ||
    cachedDetail.username ||
    cachedDetail.password ||
    cachedDetail.deviceSecret ||
    cachedDetail.metadataJson
  )
}

export function replaceSelectedDeviceSnapshot(selectedRows: Device[], device: Device) {
  const rowKey = getDeviceRowKey(device)
  if (!rowKey) {
    return selectedRows
  }
  return selectedRows.map((item) => (getDeviceRowKey(item) === rowKey ? { ...item, ...device } : item))
}

export function removeSelectedDeviceSnapshot(selectedRows: Device[], row?: Partial<Device> | null) {
  const rowKey = getDeviceRowKey(row)
  if (!rowKey) {
    return selectedRows
  }
  return selectedRows.filter((item) => getDeviceRowKey(item) !== rowKey)
}

export function mergeLocalDeviceRow(rows: Device[], device: Device) {
  const rowKey = getDeviceRowKey(device)
  const rowIndex = rows.findIndex((item) => getDeviceRowKey(item) === rowKey)
  if (rowIndex < 0) {
    return null
  }

  const nextRows = [...rows]
  nextRows[rowIndex] = {
    ...nextRows[rowIndex],
    ...device
  }
  return nextRows
}

export function prependLocalDeviceRow(rows: Device[], device: Device, pageSize: number) {
  const rowKey = getDeviceRowKey(device)
  return [device, ...rows.filter((item) => getDeviceRowKey(item) !== rowKey)].slice(0, pageSize)
}

export function removeLocalDeviceRow(rows: Device[], row?: Partial<Device> | null) {
  const rowKey = getDeviceRowKey(row)
  if (!rowKey) {
    return null
  }

  const nextRows = rows.filter((item) => getDeviceRowKey(item) !== rowKey)
  if (nextRows.length === rows.length) {
    return null
  }
  return nextRows
}
