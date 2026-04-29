import { request } from './request'
import type { ApiEnvelope, IdType, PageResult, SystemErrorStats, BusinessAuditStats } from '../types/api'

export interface AuditLogRecord {
  id: IdType
  tenantId?: number
  userId?: number
  userName?: string
  traceId?: string
  deviceCode?: string
  productKey?: string
  operationModule?: string
  operationType?: string
  operationMethod?: string
  requestUrl?: string
  requestMethod?: string
  requestParams?: string
  responseResult?: string
  ipAddress?: string
  operationResult?: number
  resultMessage?: string
  errorCode?: string
  exceptionClass?: string
  operationTime?: string
  createTime?: string
}

export interface SystemErrorClusterRow {
  clusterKey: string
  operationModule?: string
  exceptionClass?: string
  errorCode?: string
  count: number
  distinctTraceCount: number
  distinctDeviceCount: number
  latestOperationTime?: string
  latestRequestUrl?: string
  latestRequestMethod?: string
  latestResultMessage?: string
}

export interface AuditLogQueryParams {
  userName?: string
  operationType?: string
  operationModule?: string
  requestMethod?: string
  requestUrl?: string
  traceId?: string
  deviceCode?: string
  productKey?: string
  resultMessage?: string
  errorCode?: string
  exceptionClass?: string
  operationResult?: number
  pageNum?: number
  pageSize?: number
  excludeSystemError?: boolean
}

function toQueryString(params: Record<string, unknown>) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, String(value))
    }
  })
  return search.toString()
}

export function listLogs(params: AuditLogQueryParams = {}): Promise<ApiEnvelope<AuditLogRecord[]>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/list${query ? `?${query}` : ''}`
  return request<AuditLogRecord[]>(path, { method: 'GET' })
}

export function pageLogs(params: AuditLogQueryParams = {}): Promise<ApiEnvelope<PageResult<AuditLogRecord>>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/page${query ? `?${query}` : ''}`
  return request<PageResult<AuditLogRecord>>(path, { method: 'GET' })
}

export function getSystemErrorStats(params: AuditLogQueryParams = {}): Promise<ApiEnvelope<SystemErrorStats>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/system-error/stats${query ? `?${query}` : ''}`
  return request<SystemErrorStats>(path, { method: 'GET' })
}

export function pageSystemErrorClusters(
  params: AuditLogQueryParams = {}
): Promise<ApiEnvelope<PageResult<SystemErrorClusterRow>>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/system-error/clusters/page${query ? `?${query}` : ''}`
  return request<PageResult<SystemErrorClusterRow>>(path, { method: 'GET' })
}

export function getBusinessAuditStats(params: AuditLogQueryParams = {}): Promise<ApiEnvelope<BusinessAuditStats>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/business/stats${query ? `?${query}` : ''}`
  return request<BusinessAuditStats>(path, { method: 'GET' })
}

export function getAuditLogById(id: IdType): Promise<ApiEnvelope<AuditLogRecord>> {
  return request<AuditLogRecord>(`/api/system/audit-log/get/${id}`, { method: 'GET' })
}

export function addAuditLog(data: Partial<AuditLogRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/audit-log/add', { method: 'POST', body: data })
}

export function deleteAuditLog(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/audit-log/delete/${id}`, { method: 'DELETE' })
}
