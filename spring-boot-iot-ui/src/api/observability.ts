import { buildQueryString } from './query'
import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export interface ObservabilityBusinessEvent {
  id?: IdType | null
  tenantId?: IdType | null
  traceId?: string | null
  eventCode?: string | null
  eventName?: string | null
  domainCode?: string | null
  actionCode?: string | null
  objectType?: string | null
  objectId?: string | null
  objectName?: string | null
  actorUserId?: IdType | null
  actorName?: string | null
  resultStatus?: string | null
  sourceType?: string | null
  evidenceType?: string | null
  evidenceId?: string | null
  requestMethod?: string | null
  requestUri?: string | null
  durationMs?: number | null
  errorCode?: string | null
  errorMessage?: string | null
  metadataJson?: string | null
  occurredAt?: string | null
  createTime?: string | null
}

export interface ObservabilitySpan {
  id?: IdType | null
  tenantId?: IdType | null
  traceId?: string | null
  parentSpanId?: IdType | null
  spanType?: string | null
  spanName?: string | null
  domainCode?: string | null
  eventCode?: string | null
  objectType?: string | null
  objectId?: string | null
  transportType?: string | null
  status?: string | null
  durationMs?: number | null
  startedAt?: string | null
  finishedAt?: string | null
  errorClass?: string | null
  errorMessage?: string | null
  tagsJson?: string | null
  createTime?: string | null
}

export interface ObservabilityTraceEvidenceItem {
  itemType?: 'BUSINESS_EVENT' | 'SPAN' | string | null
  itemId?: IdType | null
  traceId?: string | null
  code?: string | null
  name?: string | null
  domainCode?: string | null
  objectType?: string | null
  objectId?: string | null
  status?: string | null
  durationMs?: number | null
  occurredAt?: string | null
}

export interface ObservabilityTraceEvidence {
  traceId?: string | null
  businessEvents?: ObservabilityBusinessEvent[]
  spans?: ObservabilitySpan[]
  timeline?: ObservabilityTraceEvidenceItem[]
}

export interface ObservabilitySlowSpanSummary {
  spanType?: string | null
  domainCode?: string | null
  eventCode?: string | null
  objectType?: string | null
  objectId?: string | null
  totalCount?: number | null
  avgDurationMs?: number | null
  maxDurationMs?: number | null
  latestTraceId?: string | null
  latestStartedAt?: string | null
}

export interface ObservabilitySlowSpanTrend {
  bucket?: string | null
  bucketStart?: string | null
  bucketEnd?: string | null
  totalCount?: number | null
  successCount?: number | null
  errorCount?: number | null
  errorRate?: number | null
  avgDurationMs?: number | null
  maxDurationMs?: number | null
  p95DurationMs?: number | null
  p99DurationMs?: number | null
}

export interface ObservabilityBusinessEventPageQuery {
  traceId?: string
  eventCode?: string
  domainCode?: string
  actionCode?: string
  objectType?: string
  objectId?: string
  resultStatus?: string
  dateFrom?: string
  dateTo?: string
  pageNum?: number
  pageSize?: number
}

export interface ObservabilitySpanPageQuery {
  traceId?: string
  spanType?: string
  eventCode?: string
  domainCode?: string
  objectType?: string
  objectId?: string
  status?: string
  minDurationMs?: number
  dateFrom?: string
  dateTo?: string
  pageNum?: number
  pageSize?: number
}

export interface ObservabilitySlowSpanSummaryQuery {
  spanType?: string
  eventCode?: string
  domainCode?: string
  objectType?: string
  objectId?: string
  status?: string
  minDurationMs?: number
  dateFrom?: string
  dateTo?: string
  limit?: number
}

export interface ObservabilitySlowSpanTrendQuery {
  spanType?: string
  eventCode?: string
  domainCode?: string
  objectType?: string
  objectId?: string
  status?: string
  minDurationMs?: number
  dateFrom?: string
  dateTo?: string
  bucket?: 'HOUR' | 'DAY' | string
}

export function pageObservabilityBusinessEvents(
  params: ObservabilityBusinessEventPageQuery = {}
): Promise<ApiEnvelope<PageResult<ObservabilityBusinessEvent>>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/business-events/page${query ? `?${query}` : ''}`
  return request<PageResult<ObservabilityBusinessEvent>>(path, { method: 'GET' })
}

export function pageObservabilitySpans(
  params: ObservabilitySpanPageQuery = {}
): Promise<ApiEnvelope<PageResult<ObservabilitySpan>>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/spans/page${query ? `?${query}` : ''}`
  return request<PageResult<ObservabilitySpan>>(path, { method: 'GET' })
}

export function listObservabilitySlowSpanSummaries(
  params: ObservabilitySlowSpanSummaryQuery = {}
): Promise<ApiEnvelope<ObservabilitySlowSpanSummary[]>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/spans/slow-summary${query ? `?${query}` : ''}`
  return request<ObservabilitySlowSpanSummary[]>(path, { method: 'GET' })
}

export function listObservabilitySlowSpanTrends(
  params: ObservabilitySlowSpanTrendQuery = {}
): Promise<ApiEnvelope<ObservabilitySlowSpanTrend[]>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/spans/slow-trends${query ? `?${query}` : ''}`
  return request<ObservabilitySlowSpanTrend[]>(path, { method: 'GET' })
}

export function getTraceEvidence(traceId: string): Promise<ApiEnvelope<ObservabilityTraceEvidence>> {
  return request<ObservabilityTraceEvidence>(
    `/api/system/observability/trace/${encodeURIComponent(traceId)}`,
    { method: 'GET' }
  )
}
