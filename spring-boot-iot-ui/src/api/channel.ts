import { request } from './request'
import type { ApiEnvelope, IdType } from '../types/api'

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
  isDefault?: number
  config?: string
  createTime?: string
  updateTime?: string
}

export function listChannels(): Promise<ApiEnvelope<ChannelRecord[]>> {
  return request<ChannelRecord[]>('/api/system/channel/list', { method: 'GET' })
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
