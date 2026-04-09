import { request } from './request'
import { getDictByCode, type DictItem } from './dict'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export interface ChannelTypeOption {
  label: string
  value: string
  sortNo: number
}

export const CHANNEL_TYPES: ChannelTypeOption[] = [
  { value: 'email', label: '邮件', sortNo: 1 },
  { value: 'sms', label: '短信', sortNo: 2 },
  { value: 'webhook', label: 'Webhook', sortNo: 3 },
  { value: 'wechat', label: '微信', sortNo: 4 },
  { value: 'feishu', label: '飞书', sortNo: 5 },
  { value: 'dingtalk', label: '钉钉', sortNo: 6 }
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

function cloneChannelTypeOptions(): ChannelTypeOption[] {
  return CHANNEL_TYPES.map((item) => ({ ...item }))
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

export function buildChannelTypeOptions(items: Partial<DictItem>[] = []): ChannelTypeOption[] {
  const normalized = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || String(item.itemValue || ''),
      value: String(item.itemValue || '').trim().toLowerCase(),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const unique = new Map<string, ChannelTypeOption>()
  normalized
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!unique.has(item.value)) {
        unique.set(item.value, item)
      }
    })

  return unique.size > 0 ? Array.from(unique.values()) : cloneChannelTypeOptions()
}

export async function fetchChannelTypeOptions(): Promise<ChannelTypeOption[]> {
  try {
    const response = await getDictByCode('notification_channel_type')
    return response.code === 200
      ? buildChannelTypeOptions(response.data?.items || [])
      : cloneChannelTypeOptions()
  } catch {
    return cloneChannelTypeOptions()
  }
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
