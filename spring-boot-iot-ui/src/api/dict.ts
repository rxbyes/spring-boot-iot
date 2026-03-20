import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface DictItem {
  id: IdType
  tenantId: IdType
  dictId: IdType
  itemName: string
  itemValue: string
  itemType: string
  status: number
  sortNo: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  deleted: number
}

export interface Dict {
  id: IdType
  tenantId: IdType
  dictName: string
  dictCode: string
  dictType: string
  status: number
  sortNo: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  deleted: number
  items?: DictItem[]
}

export interface DictQueryParams {
  dictName?: string
  dictCode?: string
  dictType?: string
}

export interface DictPageQueryParams extends DictQueryParams {
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

export const listDicts = (params: DictQueryParams = {}): Promise<ApiEnvelope<Dict[]>> => {
  const query = buildQuery(params)
  return request<Dict[]>(`/api/dict/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const pageDicts = (params: DictPageQueryParams = {}): Promise<ApiEnvelope<PageResult<Dict>>> => {
  const query = buildQuery(params)
  return request<PageResult<Dict>>(`/api/dict/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const listDictTree = (): Promise<ApiEnvelope<Dict[]>> => {
  return request<Dict[]>('/api/dict/tree', { method: 'GET' })
}

export const getDict = (id: IdType): Promise<ApiEnvelope<Dict>> => {
  return request<Dict>(`/api/dict/${id}`, { method: 'GET' })
}

export const getDictByCode = (dictCode: string): Promise<ApiEnvelope<Dict>> => {
  return request<Dict>(`/api/dict/code/${dictCode}`, { method: 'GET' })
}

export const addDict = (data: Partial<Dict>): Promise<ApiEnvelope<Dict>> => {
  return request<Dict>('/api/dict', { method: 'POST', body: data })
}

export const updateDict = (data: Partial<Dict>): Promise<ApiEnvelope<Dict>> => {
  return request<Dict>('/api/dict', { method: 'PUT', body: data })
}

export const deleteDict = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/dict/${id}`, { method: 'DELETE' })
}

export const listDictItems = (dictId: IdType): Promise<ApiEnvelope<DictItem[]>> => {
  return request<DictItem[]>(`/api/dict/${dictId}/items`, { method: 'GET' })
}

export const addDictItem = (data: Partial<DictItem>): Promise<ApiEnvelope<DictItem>> => {
  return request<DictItem>(`/api/dict/${data.dictId}/items`, { method: 'POST', body: data })
}

export const updateDictItem = (data: Partial<DictItem>): Promise<ApiEnvelope<DictItem>> => {
  return request<DictItem>(`/api/dict/${data.dictId}/items`, { method: 'PUT', body: data })
}

export const deleteDictItem = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/dict/items/${id}`, { method: 'DELETE' })
}
