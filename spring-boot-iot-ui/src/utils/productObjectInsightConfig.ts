import type {
  ProductMetadata,
  ProductModel,
  ProductObjectInsightCustomMetricConfig,
  ProductObjectInsightMetricGroup
} from '@/types/api'
import {
  inferObjectInsightStatusGroup,
  normalizeObjectInsightMetricGroup
} from '@/utils/objectInsightMetricGroup'

export const MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS = 20

export function createEmptyProductObjectInsightMetric(): ProductObjectInsightCustomMetricConfig {
  return {
    identifier: '',
    displayName: '',
    group: 'runtime',
    unit: '',
    includeInTrend: true,
    includeInExtension: true,
    analysisTitle: '',
    analysisTag: '系统自定义参数',
    analysisTemplate: '',
    enabled: true,
    sortNo: 10
  }
}

export function createProductObjectInsightMetricFromModel(
  model: Pick<ProductModel, 'identifier' | 'modelName' | 'sortNo' | 'specsJson'>,
  group: ProductObjectInsightMetricGroup
): ProductObjectInsightCustomMetricConfig {
  const identifier = normalizeMetricIdentifier(model.identifier)
  const displayName = normalizeText(model.modelName) || identifier
  return {
    ...createEmptyProductObjectInsightMetric(),
    identifier,
    displayName,
    group,
    unit: parseProductModelUnit(model.specsJson),
    includeInTrend: true,
    includeInExtension: false,
    analysisTitle: '',
    analysisTag: '',
    analysisTemplate: '',
    enabled: true,
    sortNo: normalizeSortNo(model.sortNo)
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
        group: normalizeObjectInsightMetricGroup(
          row.group,
          normalizeText(row.identifier),
          normalizeText(row.displayName)
        ),
        unit: normalizeText(row.unit),
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
  if (rows.length > MAX_PRODUCT_OBJECT_INSIGHT_CUSTOM_METRICS) {
    return '对象洞察配置最多允许 20 个指标'
  }

  const identifiers = new Set<string>()
  for (const row of rows) {
    const identifier = normalizeText(row.identifier)
    const identifierKey = identifier.toLowerCase()
    if (!identifier) {
      return '对象洞察指标标识不能为空'
    }
    if (!normalizeText(row.displayName)) {
      return `对象洞察指标 ${identifier} 缺少中文名称`
    }
    if (identifiers.has(identifierKey)) {
      return `对象洞察配置中存在重复指标标识：${identifier}`
    }
    identifiers.add(identifierKey)
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
        group: serializeProductObjectInsightMetricGroup(item.group),
        unit: normalizeText(item.unit) || undefined,
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

export function findProductObjectInsightMetric(
  rows: ProductObjectInsightCustomMetricConfig[],
  identifier: string
) {
  const normalizedIdentifier = normalizeMetricIdentifier(identifier)
  return rows.find((item) => compareMetricIdentifier(item.identifier, normalizedIdentifier))
}

export function upsertProductObjectInsightMetric(
  rows: ProductObjectInsightCustomMetricConfig[],
  metric: ProductObjectInsightCustomMetricConfig
) {
  const normalizedMetric = normalizeMetric(metric)
  const targetIndex = rows.findIndex(
    (item) => compareMetricIdentifier(item.identifier, normalizedMetric.identifier)
  )

  if (targetIndex < 0) {
    return [...rows, normalizedMetric]
  }

  const existingMetric = normalizeMetric(rows[targetIndex])
    const nextMetric: ProductObjectInsightCustomMetricConfig = {
      ...existingMetric,
      identifier: normalizedMetric.identifier || existingMetric.identifier,
      displayName: normalizedMetric.displayName || existingMetric.displayName,
      group: normalizedMetric.group,
      unit: normalizedMetric.unit || existingMetric.unit,
      includeInTrend: normalizedMetric.includeInTrend ?? existingMetric.includeInTrend,
      includeInExtension: normalizedMetric.includeInExtension ?? existingMetric.includeInExtension,
      analysisTitle: normalizedMetric.analysisTitle || existingMetric.analysisTitle,
    analysisTag: normalizedMetric.analysisTag || existingMetric.analysisTag,
    analysisTemplate: normalizedMetric.analysisTemplate || existingMetric.analysisTemplate,
    enabled: normalizedMetric.enabled ?? existingMetric.enabled,
    sortNo: normalizeSortNo(normalizedMetric.sortNo ?? existingMetric.sortNo)
  }

  return rows.map((item, index) => (index === targetIndex ? nextMetric : item))
}

export function removeProductObjectInsightMetric(
  rows: ProductObjectInsightCustomMetricConfig[],
  identifier: string
) {
  const normalizedIdentifier = normalizeMetricIdentifier(identifier)
  return rows.filter((item) => !compareMetricIdentifier(item.identifier, normalizedIdentifier))
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

function normalizeMetricIdentifier(value: unknown): string {
  return normalizeText(value)
}

function compareMetricIdentifier(a: unknown, b: unknown): boolean {
  return normalizeMetricIdentifier(a).toLowerCase() === normalizeMetricIdentifier(b).toLowerCase()
}

function serializeProductObjectInsightMetricGroup(group: ProductObjectInsightMetricGroup) {
  return group === 'statusEvent' ? 'status' : group
}

function normalizeMetric(metric: ProductObjectInsightCustomMetricConfig): ProductObjectInsightCustomMetricConfig {
  return {
    ...createEmptyProductObjectInsightMetric(),
    ...metric,
    identifier: normalizeMetricIdentifier(metric.identifier),
    displayName: normalizeText(metric.displayName),
    unit: normalizeText(metric.unit),
    analysisTitle: normalizeText(metric.analysisTitle),
    analysisTag: normalizeText(metric.analysisTag),
    analysisTemplate: normalizeText(metric.analysisTemplate),
    group: normalizeObjectInsightMetricGroup(metric.group, metric.identifier, metric.displayName),
    includeInTrend: metric.includeInTrend ?? true,
    includeInExtension: metric.includeInExtension ?? true,
    enabled: metric.enabled ?? true,
    sortNo: normalizeSortNo(metric.sortNo)
  }
}

function normalizeSortNo(value: unknown) {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : 10
}

function parseProductModelUnit(specsJson?: string | null) {
  const specs = parseJsonObject(specsJson)
  return normalizeText(specs?.unit)
}

function parseJsonObject(value?: string | null): Record<string, unknown> | null {
  const text = normalizeText(value)
  if (!text) {
    return null
  }

  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null
    }
    return parsed as Record<string, unknown>
  } catch {
    return null
  }
}

export function inferProductObjectInsightMetricGroup(identifier?: string | null, displayName?: string | null) {
  return inferObjectInsightStatusGroup(identifier, displayName)
}
