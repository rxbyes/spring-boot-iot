import type { ProductMetadata, ProductObjectInsightCustomMetricConfig } from '@/types/api'

const MAX_CUSTOM_METRICS = 20

export function createEmptyProductObjectInsightMetric(): ProductObjectInsightCustomMetricConfig {
  return {
    identifier: '',
    displayName: '',
    group: 'status',
    includeInTrend: true,
    includeInExtension: true,
    analysisTitle: '',
    analysisTag: '系统自定义参数',
    analysisTemplate: '',
    enabled: true,
    sortNo: 10
  }
}

export function parseProductObjectInsightMetrics(metadataJson?: string | null): ProductObjectInsightCustomMetricConfig[] {
  const metadata = parseProductMetadata(metadataJson)
  const customMetrics = Array.isArray(metadata?.objectInsight?.customMetrics) ? metadata.objectInsight?.customMetrics : []

  return customMetrics.flatMap((item) => {
    if (!item || typeof item !== 'object') {
      return []
    }

    const row = item as Record<string, unknown>
    return [
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: normalizeText(row.identifier),
        displayName: normalizeText(row.displayName),
        group: row.group === 'measure' ? 'measure' : 'status',
        includeInTrend: typeof row.includeInTrend === 'boolean' ? row.includeInTrend : true,
        includeInExtension: typeof row.includeInExtension === 'boolean' ? row.includeInExtension : true,
        analysisTitle: normalizeText(row.analysisTitle),
        analysisTag: normalizeText(row.analysisTag),
        analysisTemplate: normalizeText(row.analysisTemplate),
        enabled: typeof row.enabled === 'boolean' ? row.enabled : true,
        sortNo: Number.isFinite(Number(row.sortNo)) ? Number(row.sortNo) : 10
      }
    ]
  })
}

export function validateProductObjectInsightMetrics(rows: ProductObjectInsightCustomMetricConfig[]): string | null {
  if (rows.length > MAX_CUSTOM_METRICS) {
    return '对象洞察配置最多允许 20 个指标'
  }

  const identifiers = new Set<string>()
  for (const row of rows) {
    const identifier = normalizeText(row.identifier)
    if (!identifier) {
      return '对象洞察指标标识不能为空'
    }
    if (!normalizeText(row.displayName)) {
      return `对象洞察指标 ${identifier} 缺少中文名称`
    }
    if (identifiers.has(identifier)) {
      return `对象洞察配置中存在重复指标标识：${identifier}`
    }
    identifiers.add(identifier)
    if (normalizeText(row.analysisTemplate).length > 300) {
      return `对象洞察指标 ${identifier} 的分析描述模板不能超过300字`
    }
  }

  return null
}

export function buildProductMetadataJson(
  rows: ProductObjectInsightCustomMetricConfig[],
  baseMetadataJson?: string | null
): string | undefined {
  const normalizedRows = rows
    .map((item) => ({
      identifier: normalizeText(item.identifier),
      displayName: normalizeText(item.displayName),
      group: item.group,
      includeInTrend: item.includeInTrend ?? true,
      includeInExtension: item.includeInExtension ?? true,
      analysisTitle: normalizeText(item.analysisTitle) || undefined,
      analysisTag: normalizeText(item.analysisTag) || undefined,
      analysisTemplate: normalizeText(item.analysisTemplate) || undefined,
      enabled: item.enabled ?? true,
      sortNo: item.sortNo ?? 10
    }))
    .filter((item) => item.identifier && item.displayName)

  const baseMetadata = parseProductMetadata(baseMetadataJson) ?? {}
  if (!normalizedRows.length && !Object.keys(baseMetadata).length) {
    return undefined
  }

  const nextMetadata: ProductMetadata = {
    ...baseMetadata,
    objectInsight: {
      ...(baseMetadata.objectInsight ?? {}),
      customMetrics: normalizedRows
    }
  }
  return JSON.stringify(nextMetadata)
}

function parseProductMetadata(metadataJson?: string | null): ProductMetadata | null {
  const text = normalizeText(metadataJson)
  if (!text) {
    return null
  }

  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null
    }
    return parsed as ProductMetadata
  } catch {
    return null
  }
}

function normalizeText(value: unknown): string {
  return typeof value === 'string' ? value.trim() : ''
}
