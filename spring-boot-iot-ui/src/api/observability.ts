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

export interface ObservabilityScheduledTask {
  id?: IdType | null
  tenantId?: IdType | null
  traceId?: string | null
  domainCode?: string | null
  taskCode?: string | null
  taskName?: string | null
  taskClassName?: string | null
  taskMethodName?: string | null
  triggerType?: string | null
  triggerExpression?: string | null
  initialDelayExpression?: string | null
  initialDelayMs?: number | null
  retryCount?: number | null
  threadName?: string | null
  status?: string | null
  durationMs?: number | null
  startedAt?: string | null
  finishedAt?: string | null
  errorClass?: string | null
  errorMessage?: string | null
  tagsJson?: string | null
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

export interface ObservabilityMessageArchiveBatch {
  id?: IdType | null
  batchNo?: string | null
  sourceTable?: string | null
  governanceMode?: string | null
  status?: string | null
  retentionDays?: number | null
  cutoffAt?: string | null
  confirmReportPath?: string | null
  confirmReportGeneratedAt?: string | null
  confirmedExpiredRows?: number | null
  candidateRows?: number | null
  archivedRows?: number | null
  deletedRows?: number | null
  failedReason?: string | null
  artifactsJson?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface ObservabilityMessageArchiveBatchReportTableSummary {
  tableName?: string | null
  label?: string | null
  retentionDays?: number | null
  timeField?: string | null
  cutoffAt?: string | null
  totalRows?: number | null
  expiredRows?: number | null
  deletedRows?: number | null
  remainingExpiredRows?: number | null
  earliestRecordAt?: string | null
  latestRecordAt?: string | null
}

export interface ObservabilityMessageArchiveBatchReportPreview {
  batchNo?: string | null
  sourceTable?: string | null
  status?: string | null
  confirmReportPath?: string | null
  confirmReportGeneratedAt?: string | null
  available?: boolean | null
  reasonCode?: string | null
  reasonMessage?: string | null
  resolvedJsonPath?: string | null
  resolvedMarkdownPath?: string | null
  markdownAvailable?: boolean | null
  markdownTruncated?: boolean | null
  markdownPreview?: string | null
  fileLastModifiedAt?: string | null
  summary?: Record<string, unknown> | null
  tableSummaries?: ObservabilityMessageArchiveBatchReportTableSummary[]
}

export interface ObservabilityMessageArchiveBatchCompareSource {
  confirmReportPath?: string | null
  resolvedDryRunJsonPath?: string | null
  resolvedApplyJsonPath?: string | null
  dryRunAvailable?: boolean | null
  applyAvailable?: boolean | null
}

export interface ObservabilityMessageArchiveBatchCompareSummary {
  confirmedExpiredRows?: number | null
  dryRunExpiredRows?: number | null
  applyArchivedRows?: number | null
  applyDeletedRows?: number | null
  remainingExpiredRows?: number | null
  deltaConfirmedVsDeleted?: number | null
  deltaDryRunVsDeleted?: number | null
  matched?: boolean | null
}

export interface ObservabilityMessageArchiveBatchCompareTable {
  tableName?: string | null
  label?: string | null
  dryRunExpiredRows?: number | null
  applyArchivedRows?: number | null
  applyDeletedRows?: number | null
  applyRemainingExpiredRows?: number | null
  deltaDryRunVsDeleted?: number | null
  matched?: boolean | null
  reason?: string | null
}

export interface ObservabilityMessageArchiveBatchCompare {
  batchNo?: string | null
  sourceTable?: string | null
  status?: string | null
  compareStatus?: 'MATCHED' | 'DRIFTED' | 'PARTIAL' | 'UNAVAILABLE' | string | null
  compareMessage?: string | null
  sources?: ObservabilityMessageArchiveBatchCompareSource | null
  summaryCompare?: ObservabilityMessageArchiveBatchCompareSummary | null
  tableComparisons?: ObservabilityMessageArchiveBatchCompareTable[]
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

export interface ObservabilityScheduledTaskPageQuery {
  traceId?: string
  domainCode?: string
  taskCode?: string
  triggerType?: string
  status?: string
  minDurationMs?: number
  dateFrom?: string
  dateTo?: string
  pageNum?: number
  pageSize?: number
}

export interface ObservabilityMessageArchiveBatchPageQuery {
  batchNo?: string
  sourceTable?: string
  status?: string
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

export function pageObservabilityScheduledTasks(
  params: ObservabilityScheduledTaskPageQuery = {}
): Promise<ApiEnvelope<PageResult<ObservabilityScheduledTask>>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/scheduled-tasks/page${query ? `?${query}` : ''}`
  return request<PageResult<ObservabilityScheduledTask>>(path, { method: 'GET' })
}

export function pageObservabilityMessageArchiveBatches(
  params: ObservabilityMessageArchiveBatchPageQuery = {}
): Promise<ApiEnvelope<PageResult<ObservabilityMessageArchiveBatch>>> {
  const query = buildQueryString(params)
  const path = `/api/system/observability/message-archive-batches/page${query ? `?${query}` : ''}`
  return request<PageResult<ObservabilityMessageArchiveBatch>>(path, { method: 'GET' })
}

export function getObservabilityMessageArchiveBatchReportPreview(
  batchNo: string
): Promise<ApiEnvelope<ObservabilityMessageArchiveBatchReportPreview>> {
  const query = buildQueryString({ batchNo })
  const path = `/api/system/observability/message-archive-batches/report-preview${query ? `?${query}` : ''}`
  return request<ObservabilityMessageArchiveBatchReportPreview>(path, { method: 'GET' })
}

export function getObservabilityMessageArchiveBatchCompare(
  batchNo: string
): Promise<ApiEnvelope<ObservabilityMessageArchiveBatchCompare>> {
  const query = buildQueryString({ batchNo })
  const path = `/api/system/observability/message-archive-batches/compare${query ? `?${query}` : ''}`
  return request<ObservabilityMessageArchiveBatchCompare>(path, { method: 'GET' })
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
