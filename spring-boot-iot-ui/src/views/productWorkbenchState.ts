import type { PageResult, Product } from '@/types/api'

export interface ProductFilterSnapshot {
  productName: string
  nodeType?: number
  status?: number
}

export interface ProductPageQuerySnapshot extends ProductFilterSnapshot {
  pageNum: number
  pageSize: number
}

export interface ProductPageCacheEntry {
  key: string
  pageNum: number
  pageSize: number
  total: number
  records: Product[]
  cachedAt: number
}

export interface ProductDetailCacheEntry {
  key: string
  detail: Product
  cachedAt: number
}

export interface ProductPageLoadStrategy {
  useFreshCacheOnly: boolean
  silentRequest: boolean
}

interface ProductPageCachePayload {
  version?: number
  entries?: unknown
}

interface ProductDetailCachePayload {
  version?: number
  entries?: unknown
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

export function buildProductPageCacheKey(query: ProductPageQuerySnapshot) {
  return [
    query.productName.trim().toLowerCase(),
    query.nodeType ?? '',
    query.status ?? '',
    normalizePositiveInt(query.pageNum, 1),
    normalizePositiveInt(query.pageSize, 10)
  ].join('|')
}

export function createProductPageCacheEntry(
  query: ProductPageQuerySnapshot,
  pageResult: PageResult<Product>,
  cachedAt = Date.now()
): ProductPageCacheEntry {
  const pageNum = normalizePositiveInt(Number(pageResult.pageNum || query.pageNum), 1)
  const pageSize = normalizePositiveInt(Number(pageResult.pageSize || query.pageSize), 10)
  const normalizedQuery = {
    ...query,
    productName: query.productName.trim(),
    pageNum,
    pageSize
  }

  return {
    key: buildProductPageCacheKey(normalizedQuery),
    pageNum,
    pageSize,
    total: Number(pageResult.total || 0),
    records: (pageResult.records || []).map((item) => ({ ...item })),
    cachedAt
  }
}

export function cloneProductPageCacheEntry(entry?: ProductPageCacheEntry | null) {
  if (!entry) {
    return null
  }
  return {
    ...entry,
    records: entry.records.map((item) => ({ ...item }))
  }
}

export function isProductPageCacheFresh(entry: ProductPageCacheEntry | null | undefined, ttlMs: number, now = Date.now()) {
  if (!entry || ttlMs < 0) {
    return false
  }
  return now - entry.cachedAt <= ttlMs
}

function normalizeProductPageCacheEntry(entry: unknown) {
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
    .map((item) => ({ ...(item as Product) }))

  return {
    key: candidate.key,
    pageNum,
    pageSize,
    total,
    records,
    cachedAt
  } satisfies ProductPageCacheEntry
}

export function createProductDetailCacheEntry(product: Product, cachedAt = Date.now()): ProductDetailCacheEntry {
  return {
    key: getProductRowKey(product),
    detail: { ...product },
    cachedAt
  }
}

export function cloneProductDetailCacheEntry(entry?: ProductDetailCacheEntry | null) {
  if (!entry) {
    return null
  }
  return {
    ...entry,
    detail: { ...entry.detail }
  }
}

export function isProductDetailCacheFresh(entry: ProductDetailCacheEntry | null | undefined, ttlMs: number, now = Date.now()) {
  if (!entry || ttlMs < 0) {
    return false
  }
  return now - entry.cachedAt <= ttlMs
}

function normalizeProductDetailCacheEntry(entry: unknown) {
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
    detail: { ...(candidate.detail as Product) },
    cachedAt
  } satisfies ProductDetailCacheEntry
}

export function deserializeProductPageCacheEntries(raw: string | null | undefined, ttlMs: number, maxEntries: number, now = Date.now()) {
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as ProductPageCachePayload | unknown[] | Record<string, unknown>
    const rawEntries = Array.isArray(parsed)
      ? parsed
      : Array.isArray((parsed as ProductPageCachePayload)?.entries)
        ? (parsed as ProductPageCachePayload).entries || []
      : Array.isArray((parsed as Record<string, unknown>)?.entries)
        ? (parsed as Record<string, unknown>).entries || []
        : []

    return rawEntries
      .map((entry: unknown) => normalizeProductPageCacheEntry(entry))
      .filter((entry): entry is ProductPageCacheEntry => Boolean(entry))
      .filter((entry) => isProductPageCacheFresh(entry, ttlMs, now))
      .sort((left: ProductPageCacheEntry, right: ProductPageCacheEntry) => right.cachedAt - left.cachedAt)
      .slice(0, Math.max(0, maxEntries))
      .map((entry: ProductPageCacheEntry) => cloneProductPageCacheEntry(entry) as ProductPageCacheEntry)
  } catch {
    return []
  }
}

export function serializeProductPageCacheEntries(entries: Iterable<ProductPageCacheEntry>, maxEntries: number) {
  const normalizedEntries = Array.from(entries)
    .map((entry: ProductPageCacheEntry) => cloneProductPageCacheEntry(entry))
    .filter((entry): entry is ProductPageCacheEntry => Boolean(entry))
    .sort((left: ProductPageCacheEntry, right: ProductPageCacheEntry) => right.cachedAt - left.cachedAt)
    .slice(0, Math.max(0, maxEntries))

  return JSON.stringify({
    version: 1,
    entries: normalizedEntries
  })
}

export function deserializeProductDetailCacheEntries(raw: string | null | undefined, ttlMs: number, maxEntries: number, now = Date.now()) {
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw) as ProductDetailCachePayload | unknown[] | Record<string, unknown>
    const rawEntries = Array.isArray(parsed)
      ? parsed
      : Array.isArray((parsed as ProductDetailCachePayload)?.entries)
        ? (parsed as ProductDetailCachePayload).entries || []
      : Array.isArray((parsed as Record<string, unknown>)?.entries)
        ? (parsed as Record<string, unknown>).entries || []
        : []

    return rawEntries
      .map((entry: unknown) => normalizeProductDetailCacheEntry(entry))
      .filter((entry): entry is ProductDetailCacheEntry => Boolean(entry))
      .filter((entry) => isProductDetailCacheFresh(entry, ttlMs, now))
      .sort((left: ProductDetailCacheEntry, right: ProductDetailCacheEntry) => right.cachedAt - left.cachedAt)
      .slice(0, Math.max(0, maxEntries))
      .map((entry: ProductDetailCacheEntry) => cloneProductDetailCacheEntry(entry) as ProductDetailCacheEntry)
  } catch {
    return []
  }
}

export function serializeProductDetailCacheEntries(entries: Iterable<ProductDetailCacheEntry>, maxEntries: number) {
  const normalizedEntries = Array.from(entries)
    .map((entry: ProductDetailCacheEntry) => cloneProductDetailCacheEntry(entry))
    .filter((entry): entry is ProductDetailCacheEntry => Boolean(entry))
    .sort((left: ProductDetailCacheEntry, right: ProductDetailCacheEntry) => right.cachedAt - left.cachedAt)
    .slice(0, Math.max(0, maxEntries))

  return JSON.stringify({
    version: 1,
    entries: normalizedEntries
  })
}

export function resolveProductPageLoadStrategy(options: {
  hasCachedPage: boolean
  hasFreshCache: boolean
  force?: boolean
  silent?: boolean
}): ProductPageLoadStrategy {
  const force = options.force === true
  const useFreshCacheOnly = options.hasFreshCache && !force

  return {
    useFreshCacheOnly,
    silentRequest: !useFreshCacheOnly && (options.silent === true || (options.hasCachedPage && !force))
  }
}

export function getNextProductPageQuery(query: ProductPageQuerySnapshot, total: number) {
  const pageNum = normalizePositiveInt(query.pageNum, 1)
  const pageSize = normalizePositiveInt(query.pageSize, 10)
  const normalizedTotal = Math.max(0, Number(total) || 0)
  if (pageNum * pageSize >= normalizedTotal) {
    return null
  }
  return {
    ...query,
    productName: query.productName.trim(),
    pageNum: pageNum + 1,
    pageSize
  }
}

export function getProductRowKey(row?: Partial<Product> | null) {
  if (!row) {
    return ''
  }
  if (row.id !== undefined && row.id !== null && row.id !== '') {
    return String(row.id)
  }
  return row.productKey ? String(row.productKey) : ''
}

export function matchesProductFilters(product: Product, filters: ProductFilterSnapshot) {
  const keyword = filters.productName.trim().toLowerCase()
  if (keyword && !String(product.productName || '').toLowerCase().includes(keyword)) {
    return false
  }
  if (filters.nodeType !== undefined && product.nodeType !== filters.nodeType) {
    return false
  }
  if (filters.status !== undefined && (product.status ?? 1) !== filters.status) {
    return false
  }
  return true
}

export function shouldRefreshProductDetail(row: Product, cachedDetail: Product | null) {
  if (!cachedDetail) {
    return true
  }

  const rowUpdateTime = normalizeComparableText(row.updateTime)
  const cachedUpdateTime = normalizeComparableText(cachedDetail.updateTime)
  if (rowUpdateTime && cachedUpdateTime) {
    return rowUpdateTime !== cachedUpdateTime
  }

  return !cachedDetail.description
}

export function replaceSelectedProductSnapshot(selectedRows: Product[], product: Product) {
  const rowKey = getProductRowKey(product)
  if (!rowKey) {
    return selectedRows
  }
  return selectedRows.map((item) => (getProductRowKey(item) === rowKey ? { ...item, ...product } : item))
}

export function removeSelectedProductSnapshot(selectedRows: Product[], row?: Partial<Product> | null) {
  const rowKey = getProductRowKey(row)
  if (!rowKey) {
    return selectedRows
  }
  return selectedRows.filter((item) => getProductRowKey(item) !== rowKey)
}

export function mergeLocalProductRow(rows: Product[], product: Product) {
  const rowKey = getProductRowKey(product)
  const rowIndex = rows.findIndex((item) => getProductRowKey(item) === rowKey)
  if (rowIndex < 0) {
    return null
  }

  const nextRows = [...rows]
  nextRows[rowIndex] = {
    ...nextRows[rowIndex],
    ...product
  }
  return nextRows
}

export function prependLocalProductRow(rows: Product[], product: Product, pageSize: number) {
  const rowKey = getProductRowKey(product)
  return [product, ...rows.filter((item) => getProductRowKey(item) !== rowKey)].slice(0, pageSize)
}

export function removeLocalProductRow(rows: Product[], row?: Partial<Product> | null) {
  const rowKey = getProductRowKey(row)
  if (!rowKey) {
    return null
  }

  const nextRows = rows.filter((item) => getProductRowKey(item) !== rowKey)
  if (nextRows.length === rows.length) {
    return null
  }
  return nextRows
}
