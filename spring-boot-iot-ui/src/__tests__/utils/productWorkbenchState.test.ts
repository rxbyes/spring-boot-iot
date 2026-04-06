import { describe, expect, it } from 'vitest'

import type { Product } from '@/types/api'
import {
  buildProductPageCacheKey,
  cloneProductDetailCacheEntry,
  cloneProductPageCacheEntry,
  createProductDetailCacheEntry,
  createProductPageCacheEntry,
  deserializeProductDetailCacheEntries,
  deserializeProductPageCacheEntries,
  getNextProductPageQuery,
  getProductRowKey,
  isProductDetailCacheFresh,
  isProductPageCacheFresh,
  matchesProductFilters,
  mergeLocalProductRow,
  prependLocalProductRow,
  removeLocalProductRow,
  removeSelectedProductSnapshot,
  resolveProductPageLoadStrategy,
  replaceSelectedProductSnapshot,
  serializeProductDetailCacheEntries,
  serializeProductPageCacheEntries,
  shouldRefreshProductDetail
} from '@/views/productWorkbenchState'

function createProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: '1001',
    productKey: 'accept-http-product-01',
    productName: '压力泵监测产品',
    protocolCode: 'mqtt-json',
    nodeType: 1,
    dataFormat: 'JSON',
    manufacturer: 'GHLZM',
    description: '默认描述',
    status: 1,
    deviceCount: 3,
    onlineDeviceCount: 2,
    lastReportTime: '2026-03-20 10:00:00',
    createTime: '2026-03-20 09:00:00',
    updateTime: '2026-03-20 11:00:00',
    ...overrides
  }
}

describe('productWorkbenchState', () => {
  it('prefers id for row key and falls back to productKey', () => {
    expect(getProductRowKey(createProduct())).toBe('1001')
    expect(getProductRowKey(createProduct({ id: '', productKey: 'fallback-key' }))).toBe('fallback-key')
    expect(getProductRowKey(null)).toBe('')
  })

  it('matches quick-search filters across product name, product key, manufacturer, nodeType and status', () => {
    const product = createProduct({ productName: '压力泵监测产品', nodeType: 2, status: 0 })

    expect(
      matchesProductFilters(product, {
        productName: '监测',
        nodeType: 2,
        status: 0
      })
    ).toBe(true)

    expect(
      matchesProductFilters(product, {
        productName: 'accept-http-product',
        nodeType: 2,
        status: 0
      })
    ).toBe(true)

    expect(
      matchesProductFilters(product, {
        productName: 'ghlzm',
        nodeType: 2,
        status: 0
      })
    ).toBe(true)

    expect(
      matchesProductFilters(product, {
        productName: '',
        nodeType: 1,
        status: 0
      })
    ).toBe(false)
  })

  it('refreshes detail when cache is missing, stale, or missing description', () => {
    const current = createProduct({ updateTime: '2026-03-20 11:00:00', description: '完整描述' })

    expect(shouldRefreshProductDetail(current, null)).toBe(true)
    expect(
      shouldRefreshProductDetail(current, createProduct({ updateTime: '2026-03-20 10:00:00', description: '完整描述' }))
    ).toBe(true)
    expect(
      shouldRefreshProductDetail(
        createProduct({ updateTime: '', description: '完整描述' }),
        createProduct({ updateTime: '', description: '' })
      )
    ).toBe(true)
    expect(
      shouldRefreshProductDetail(current, createProduct({ updateTime: '2026-03-20 11:00:00', description: '完整描述' }))
    ).toBe(false)
  })

  it('builds, clones and validates product detail cache entries', () => {
    const detailEntry = createProductDetailCacheEntry(createProduct(), 2_000)

    expect(detailEntry.key).toBe('1001')
    expect(detailEntry.detail.productName).toBe('压力泵监测产品')

    const cloned = cloneProductDetailCacheEntry(detailEntry)
    if (cloned) {
      cloned.detail.productName = '已变更名称'
    }
    expect(detailEntry.detail.productName).toBe('压力泵监测产品')

    expect(isProductDetailCacheFresh(detailEntry, 5_000, 6_999)).toBe(true)
    expect(isProductDetailCacheFresh(detailEntry, 5_000, 7_001)).toBe(false)
  })

  it('builds, clones and validates product page cache entries', () => {
    const query = {
      productName: '  监测产品  ',
      nodeType: 1,
      status: 1,
      pageNum: 1,
      pageSize: 10
    }
    const pageResult = {
      total: 21,
      pageNum: 1,
      pageSize: 10,
      records: [createProduct()]
    }

    expect(buildProductPageCacheKey(query)).toBe('监测产品|1|1|1|10')

    const cacheEntry = createProductPageCacheEntry(query, pageResult, 1_000)
    expect(cacheEntry.key).toBe('监测产品|1|1|1|10')
    expect(cacheEntry.records[0].productName).toBe('压力泵监测产品')

    const cloned = cloneProductPageCacheEntry(cacheEntry)
    cloned?.records[0] && (cloned.records[0].productName = '已变更名称')
    expect(cacheEntry.records[0].productName).toBe('压力泵监测产品')

    expect(isProductPageCacheFresh(cacheEntry, 5_000, 5_999)).toBe(true)
    expect(isProductPageCacheFresh(cacheEntry, 5_000, 6_001)).toBe(false)
  })

  it('serializes and restores recent page cache entries for session reuse', () => {
    const firstEntry = createProductPageCacheEntry(
      {
        productName: '监测产品',
        nodeType: 1,
        status: 1,
        pageNum: 1,
        pageSize: 10
      },
      {
        total: 21,
        pageNum: 1,
        pageSize: 10,
        records: [createProduct({ id: '1001', productName: '产品A' })]
      },
      2_000
    )
    const secondEntry = createProductPageCacheEntry(
      {
        productName: '监测产品',
        nodeType: 1,
        status: 1,
        pageNum: 2,
        pageSize: 10
      },
      {
        total: 21,
        pageNum: 2,
        pageSize: 10,
        records: [createProduct({ id: '1002', productName: '产品B' })]
      },
      3_000
    )

    const serialized = serializeProductPageCacheEntries([firstEntry, secondEntry], 8)
    const restored = deserializeProductPageCacheEntries(serialized, 10_000, 8, 5_000)

    expect(restored.map((item) => item.key)).toEqual([secondEntry.key, firstEntry.key])
    expect(restored[0]?.records[0]?.productName).toBe('产品B')
  })

  it('drops stale or invalid page cache entries when restoring session cache', () => {
    const freshEntry = createProductPageCacheEntry(
      {
        productName: '监测产品',
        nodeType: 1,
        status: 1,
        pageNum: 1,
        pageSize: 10
      },
      {
        total: 21,
        pageNum: 1,
        pageSize: 10,
        records: [createProduct()]
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

    const restored = deserializeProductPageCacheEntries(payload, 1_500, 8, 6_000)
    expect(restored).toHaveLength(1)
    expect(restored[0]?.key).toBe(freshEntry.key)
  })

  it('serializes and restores recent detail cache entries for session reuse', () => {
    const firstEntry = createProductDetailCacheEntry(createProduct({ id: '1001', productName: '产品A' }), 2_000)
    const secondEntry = createProductDetailCacheEntry(createProduct({ id: '1002', productName: '产品B' }), 3_000)

    const serialized = serializeProductDetailCacheEntries([firstEntry, secondEntry], 12)
    const restored = deserializeProductDetailCacheEntries(serialized, 10_000, 12, 5_000)

    expect(restored.map((item) => item.key)).toEqual([secondEntry.key, firstEntry.key])
    expect(restored[0]?.detail.productName).toBe('产品B')
  })

  it('drops stale or invalid detail cache entries when restoring session cache', () => {
    const freshEntry = createProductDetailCacheEntry(createProduct(), 5_000)

    const payload = JSON.stringify({
      version: 1,
      entries: [
        { ...freshEntry, cachedAt: 1_000 },
        freshEntry,
        { key: '', detail: null, cachedAt: 5_000 }
      ]
    })

    const restored = deserializeProductDetailCacheEntries(payload, 1_500, 12, 6_000)
    expect(restored).toHaveLength(1)
    expect(restored[0]?.key).toBe(freshEntry.key)
  })

  it('builds next page query only when there are more records to prefetch', () => {
    const nextPage = getNextProductPageQuery(
      {
        productName: '  监测产品  ',
        nodeType: 1,
        status: 1,
        pageNum: 1,
        pageSize: 10
      },
      21
    )
    expect(nextPage).toEqual({
      productName: '监测产品',
      nodeType: 1,
      status: 1,
      pageNum: 2,
      pageSize: 10
    })

    expect(
      getNextProductPageQuery(
        {
          productName: '监测产品',
          nodeType: 1,
          status: 1,
          pageNum: 3,
          pageSize: 10
        },
        21
      )
    ).toBeNull()
  })

  it('uses fresh page cache only when request is not forced', () => {
    expect(
      resolveProductPageLoadStrategy({
        hasCachedPage: true,
        hasFreshCache: true
      })
    ).toEqual({
      useFreshCacheOnly: true,
      silentRequest: false
    })

    expect(
      resolveProductPageLoadStrategy({
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
      resolveProductPageLoadStrategy({
        hasCachedPage: true,
        hasFreshCache: false
      })
    ).toEqual({
      useFreshCacheOnly: false,
      silentRequest: true
    })

    expect(
      resolveProductPageLoadStrategy({
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

  it('replaces and removes selected product snapshots by row key', () => {
    const source = [
      createProduct({ id: '1001', productName: '旧产品A' }),
      createProduct({ id: '1002', productKey: 'product-b', productName: '旧产品B' })
    ]

    const replaced = replaceSelectedProductSnapshot(source, createProduct({ id: '1002', productName: '新产品B' }))
    expect(replaced[1].productName).toBe('新产品B')

    const removed = removeSelectedProductSnapshot(replaced, createProduct({ id: '1001' }))
    expect(removed.map((item) => item.id)).toEqual(['1002'])
  })

  it('merges, prepends and removes local table rows', () => {
    const source = [
      createProduct({ id: '1001', productName: '产品A' }),
      createProduct({ id: '1002', productKey: 'product-b', productName: '产品B' }),
      createProduct({ id: '1003', productKey: 'product-c', productName: '产品C' })
    ]

    const merged = mergeLocalProductRow(source, createProduct({ id: '1002', productName: '产品B-已更新' }))
    expect(merged?.[1].productName).toBe('产品B-已更新')

    const prepended = prependLocalProductRow(source, createProduct({ id: '1004', productKey: 'product-d', productName: '产品D' }), 3)
    expect(prepended.map((item) => item.id)).toEqual(['1004', '1001', '1002'])

    const removed = removeLocalProductRow(source, createProduct({ id: '1002' }))
    expect(removed?.map((item) => item.id)).toEqual(['1001', '1003'])
  })
})
