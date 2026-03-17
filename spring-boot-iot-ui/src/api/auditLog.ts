import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface AuditLogRecord {
  id: IdType
  tenantId?: number
  userId?: number
  userName?: string
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
  operationTime?: string
  createTime?: string
}

export interface AuditLogQueryParams {
  userName?: string
  operationType?: string
  operationModule?: string
  requestMethod?: string
  requestUrl?: string
  resultMessage?: string
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

export function getAuditLogById(id: IdType): Promise<ApiEnvelope<AuditLogRecord>> {
  return request<AuditLogRecord>(`/api/system/audit-log/get/${id}`, { method: 'GET' })
}

export function addAuditLog(data: Partial<AuditLogRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/audit-log/add', { method: 'POST', body: data })
}

export function deleteAuditLog(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/audit-log/delete/${id}`, { method: 'DELETE' })
}
