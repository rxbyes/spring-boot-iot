import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export type InAppMessageType = 'system' | 'business' | 'error'
export type InAppMessagePriority = 'critical' | 'high' | 'medium' | 'low'
export type InAppMessageTargetType = 'all' | 'role' | 'user'

export const IN_APP_MESSAGE_TYPE_OPTIONS = [
  { value: 'system' as const, label: '系统事件' },
  { value: 'business' as const, label: '业务事件' },
  { value: 'error' as const, label: '错误事件' }
]

export const IN_APP_MESSAGE_PRIORITY_OPTIONS = [
  { value: 'critical' as const, label: '紧急' },
  { value: 'high' as const, label: '高' },
  { value: 'medium' as const, label: '中' },
  { value: 'low' as const, label: '低' }
]

export const IN_APP_MESSAGE_TARGET_TYPE_OPTIONS = [
  { value: 'all' as const, label: '全员可见' },
  { value: 'role' as const, label: '按角色推送' },
  { value: 'user' as const, label: '按用户定向' }
]

export interface InAppMessageRecord {
  id?: IdType
  tenantId?: IdType
  messageType: InAppMessageType
  priority: InAppMessagePriority
  title: string
  summary?: string | null
  content?: string | null
  targetType: InAppMessageTargetType
  targetRoleCodes?: string | null
  targetUserIds?: string | null
  relatedPath?: string | null
  sourceType?: string | null
  sourceId?: string | null
  publishTime?: string | null
  expireTime?: string | null
  status: number
  sortNo?: number | null
  createBy?: IdType
  createTime?: string | null
  updateBy?: IdType
  updateTime?: string | null
}

export interface InAppMessageAccessRecord {
  id: IdType
  messageType: InAppMessageType
  priority: InAppMessagePriority
  title: string
  summary?: string | null
  content?: string | null
  targetType?: string | null
  relatedPath?: string | null
  sourceType?: string | null
  sourceId?: string | null
  publishTime?: string | null
  expireTime?: string | null
  read?: boolean | null
  readTime?: string | null
}

export interface InAppMessageUnreadStats {
  totalUnreadCount: number
  systemUnreadCount: number
  businessUnreadCount: number
  errorUnreadCount: number
}

export interface MyInAppMessagePageQuery {
  messageType?: InAppMessageType
  unreadOnly?: boolean
  pageNum?: number
  pageSize?: number
}

export interface InAppMessagePageQuery {
  title?: string
  messageType?: InAppMessageType
  priority?: InAppMessagePriority
  targetType?: InAppMessageTargetType
  status?: number
  pageNum?: number
  pageSize?: number
}

function buildQuery(params: Record<string, unknown>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })
  return query.toString()
}

export function pageInAppMessages(params: InAppMessagePageQuery = {}): Promise<ApiEnvelope<PageResult<InAppMessageRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<InAppMessageRecord>>(`/api/system/in-app-message/page${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function getInAppMessage(id: IdType): Promise<ApiEnvelope<InAppMessageRecord>> {
  return request<InAppMessageRecord>(`/api/system/in-app-message/${id}`, {
    method: 'GET'
  })
}

export function addInAppMessage(data: Partial<InAppMessageRecord>): Promise<ApiEnvelope<InAppMessageRecord>> {
  return request<InAppMessageRecord>('/api/system/in-app-message/add', {
    method: 'POST',
    body: data
  })
}

export function updateInAppMessage(data: Partial<InAppMessageRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/in-app-message/update', {
    method: 'PUT',
    body: data
  })
}

export function deleteInAppMessage(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/in-app-message/delete/${id}`, {
    method: 'DELETE'
  })
}

export function pageMyInAppMessages(params: MyInAppMessagePageQuery = {}): Promise<ApiEnvelope<PageResult<InAppMessageAccessRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<InAppMessageAccessRecord>>(`/api/system/in-app-message/my/page${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function getMyInAppMessageUnreadStats(): Promise<ApiEnvelope<InAppMessageUnreadStats>> {
  return request<InAppMessageUnreadStats>('/api/system/in-app-message/my/unread-count', {
    method: 'GET'
  })
}

export function markMyInAppMessageRead(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/in-app-message/my/read/${id}`, {
    method: 'POST'
  })
}

export function markAllMyInAppMessagesRead(): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/in-app-message/my/read-all', {
    method: 'POST'
  })
}
