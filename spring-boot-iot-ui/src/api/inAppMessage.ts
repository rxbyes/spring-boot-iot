import { request, type RequestOptions } from './request'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export type InAppMessageType = 'system' | 'business' | 'error'
export type InAppMessagePriority = 'critical' | 'high' | 'medium' | 'low'
export type InAppMessageTargetType = 'all' | 'role' | 'user'
export type InAppMessageSourceType = 'manual' | 'system_error' | 'event_dispatch' | 'work_order' | 'governance'

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

export const IN_APP_MESSAGE_SOURCE_TYPE_OPTIONS = [
  { value: 'manual' as const, label: '手工广播' },
  { value: 'system_error' as const, label: '系统异常' },
  { value: 'event_dispatch' as const, label: '事件派工' },
  { value: 'work_order' as const, label: '工单状态' },
  { value: 'governance' as const, label: '治理任务' }
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
  dedupKey?: string | null
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
  sourceType?: string
  targetType?: InAppMessageTargetType
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface InAppMessageStatsBucket {
  key: string
  label: string
  deliveryCount: number
  readCount: number
  unreadCount: number
  readRate: number
}

export interface InAppMessageTrendBucket {
  date: string
  deliveryCount: number
  readCount: number
  unreadCount: number
}

export interface InAppMessageTopUnreadRecord {
  messageId: IdType
  title: string
  messageType?: string | null
  sourceType?: string | null
  publishTime?: string | null
  deliveryCount: number
  readCount: number
  unreadCount: number
  unreadRate: number
}

export interface InAppMessageStatsRecord {
  startTime?: string | null
  endTime?: string | null
  totalDeliveryCount: number
  totalReadCount: number
  totalUnreadCount: number
  readRate: number
  trend: InAppMessageTrendBucket[]
  messageTypeBuckets: InAppMessageStatsBucket[]
  sourceTypeBuckets: InAppMessageStatsBucket[]
  topUnreadMessages: InAppMessageTopUnreadRecord[]
}

export interface InAppMessageStatsQuery {
  startTime?: string
  endTime?: string
  messageType?: InAppMessageType
  sourceType?: string
}

export interface InAppMessageBridgeTrendBucket {
  date: string
  bridgeCount: number
  successCount: number
  pendingRetryCount: number
  totalAttemptCount: number
}

export interface InAppMessageBridgeChannelBucket {
  key: string
  label: string
  channelType?: string | null
  bridgeCount: number
  successCount: number
  pendingRetryCount: number
  successRate: number
}

export interface InAppMessageBridgeSourceTypeBucket {
  key: string
  label: string
  bridgeCount: number
  successCount: number
  pendingRetryCount: number
  successRate: number
}

export interface InAppMessageBridgeStatsRecord {
  startTime?: string | null
  endTime?: string | null
  totalBridgeCount: number
  successCount: number
  pendingRetryCount: number
  totalAttemptCount: number
  successRate: number
  trend: InAppMessageBridgeTrendBucket[]
  channelBuckets: InAppMessageBridgeChannelBucket[]
  sourceTypeBuckets: InAppMessageBridgeSourceTypeBucket[]
}

export interface InAppMessageBridgeLogRecord {
  id: IdType
  messageId: IdType
  title: string
  messageType?: InAppMessageType | null
  priority?: InAppMessagePriority | null
  sourceType?: string | null
  sourceId?: string | null
  relatedPath?: string | null
  publishTime?: string | null
  channelCode?: string | null
  channelName?: string | null
  channelType?: string | null
  bridgeScene?: string | null
  bridgeStatus?: number | null
  unreadCount?: number | null
  attemptCount?: number | null
  lastAttemptTime?: string | null
  successTime?: string | null
  responseStatusCode?: number | null
  responseBody?: string | null
}

export interface InAppMessageBridgeAttemptRecord {
  id: IdType
  bridgeLogId: IdType
  messageId: IdType
  channelCode?: string | null
  bridgeScene?: string | null
  attemptNo?: number | null
  bridgeStatus?: number | null
  unreadCount?: number | null
  recipientSnapshot?: string | null
  responseStatusCode?: number | null
  responseBody?: string | null
  attemptTime?: string | null
}

export interface InAppMessageBridgeQuery {
  startTime?: string
  endTime?: string
  messageType?: InAppMessageType
  sourceType?: string
  priority?: InAppMessagePriority
  channelCode?: string
  bridgeStatus?: number
}

export interface InAppMessageBridgePageQuery extends InAppMessageBridgeQuery {
  pageNum?: number
  pageSize?: number
}

type InAppMessageRequestOptions = Pick<RequestOptions, 'signal'>

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

export function getInAppMessageStats(params: InAppMessageStatsQuery = {}): Promise<ApiEnvelope<InAppMessageStatsRecord>> {
  const query = buildQuery(params)
  return request<InAppMessageStatsRecord>(`/api/system/in-app-message/stats${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function getInAppMessageBridgeStats(
  params: InAppMessageBridgeQuery = {},
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<InAppMessageBridgeStatsRecord>> {
  const query = buildQuery(params)
  return request<InAppMessageBridgeStatsRecord>(`/api/system/in-app-message/bridge/stats${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function pageInAppMessageBridgeLogs(
  params: InAppMessageBridgePageQuery = {},
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<PageResult<InAppMessageBridgeLogRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<InAppMessageBridgeLogRecord>>(`/api/system/in-app-message/bridge/page${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function listInAppMessageBridgeAttempts(
  id: IdType,
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<InAppMessageBridgeAttemptRecord[]>> {
  return request<InAppMessageBridgeAttemptRecord[]>(`/api/system/in-app-message/bridge/${id}/attempts`, {
    method: 'GET',
    ...options
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

export function pageMyInAppMessages(
  params: MyInAppMessagePageQuery = {},
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<PageResult<InAppMessageAccessRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<InAppMessageAccessRecord>>(`/api/system/in-app-message/my/page${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function getMyInAppMessageUnreadStats(
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<InAppMessageUnreadStats>> {
  return request<InAppMessageUnreadStats>('/api/system/in-app-message/my/unread-count', {
    method: 'GET',
    ...options
  })
}

export function getMyInAppMessage(
  id: IdType,
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<InAppMessageAccessRecord>> {
  return request<InAppMessageAccessRecord>(`/api/system/in-app-message/my/${id}`, {
    method: 'GET',
    ...options
  })
}

export function markMyInAppMessageRead(
  id: IdType,
  options: InAppMessageRequestOptions = {}
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/in-app-message/my/read/${id}`, {
    method: 'POST',
    ...options
  })
}

export function markAllMyInAppMessagesRead(options: InAppMessageRequestOptions = {}): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/in-app-message/my/read-all', {
    method: 'POST',
    ...options
  })
}
