import { request } from './request'
import type { ApiEnvelope } from '../types/api'

export interface AuditLogRecord {
  id: number
  operationModule?: string
  operationType?: string
  operationUri?: string
  status?: number
  userId?: number
  username?: string
  createTime?: string
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

export function listLogs(params: Record<string, unknown> = {}): Promise<ApiEnvelope<AuditLogRecord[]>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/list${query ? `?${query}` : ''}`
  return request<AuditLogRecord[]>(path, { method: 'GET' })
}

export function pageLogs(params: Record<string, unknown> = {}): Promise<ApiEnvelope<AuditLogRecord[]>> {
  const query = toQueryString(params)
  const path = `/api/system/audit-log/page${query ? `?${query}` : ''}`
  return request<AuditLogRecord[]>(path, { method: 'GET' })
}

export function getAuditLogById(id: number): Promise<ApiEnvelope<AuditLogRecord>> {
  return request<AuditLogRecord>(`/api/system/audit-log/get/${id}`, { method: 'GET' })
}

export function addAuditLog(data: Partial<AuditLogRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/audit-log/add', { method: 'POST', body: data })
}

export function deleteAuditLog(id: number): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/audit-log/delete/${id}`, { method: 'DELETE' })
}
