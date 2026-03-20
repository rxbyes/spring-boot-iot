import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export const CHANNEL_TYPES = [
  { value: 'email', label: '邮件' },
  { value: 'sms', label: '短信' },
  { value: 'webhook', label: 'Webhook' },
  { value: 'wechat', label: '微信' },
  { value: 'feishu', label: '飞书' },
  { value: 'dingtalk', label: '钉钉' }
]

export interface ChannelRecord {
  id: IdType
  channelCode: string
  channelName: string
  channelType: string
  status: number
  sortNo?: number
  remark?: string
  isDefault?: number
  config?: string
  createTime?: string
  updateTime?: string
}

export interface ChannelQueryParams {
  channelName?: string
  channelCode?: string
  channelType?: string
}

export interface ChannelPageQueryParams extends ChannelQueryParams {
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

export function listChannels(params: ChannelQueryParams = {}): Promise<ApiEnvelope<ChannelRecord[]>> {
  const query = buildQuery(params)
  return request<ChannelRecord[]>(`/api/system/channel/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function pageChannels(params: ChannelPageQueryParams = {}): Promise<ApiEnvelope<PageResult<ChannelRecord>>> {
  const query = buildQuery(params)
  return request<PageResult<ChannelRecord>>(`/api/system/channel/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function getChannelByCode(channelCode: string): Promise<ApiEnvelope<ChannelRecord>> {
  return request<ChannelRecord>(`/api/system/channel/getByCode/${channelCode}`, { method: 'GET' })
}

export function addChannel(data: Partial<ChannelRecord>): Promise<ApiEnvelope<ChannelRecord>> {
  return request<ChannelRecord>('/api/system/channel/add', { method: 'POST', body: data })
}

export function updateChannel(data: Partial<ChannelRecord>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/system/channel/update', { method: 'PUT', body: data })
}

export function testChannel(channelCode: string): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/channel/test/${channelCode}`, { method: 'POST' })
}

export function deleteChannel(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/system/channel/delete/${id}`, { method: 'DELETE' })
}
