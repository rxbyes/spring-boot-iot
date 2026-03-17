import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface Region {
  id: IdType
  tenantId: IdType
  regionName: string
  regionCode: string
  parentId: IdType
  regionType: string
  longitude?: number
  latitude?: number
  status: number
  sortNo: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  deleted: number
  children?: Region[]
  hasChildren?: boolean
}

export interface RegionQueryParams {
  regionName?: string
  regionCode?: string
  regionType?: string
}

export interface RegionPageQueryParams extends RegionQueryParams {
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

export const listRegions = (parentId?: IdType): Promise<ApiEnvelope<Region[]>> => {
  const query = buildQuery({ parentId })
  return request<Region[]>(`/api/region/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const pageRegions = (params: RegionPageQueryParams = {}): Promise<ApiEnvelope<PageResult<Region>>> => {
  const query = buildQuery(params)
  return request<PageResult<Region>>(`/api/region/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const listRegionTree = (): Promise<ApiEnvelope<Region[]>> => {
  return request<Region[]>('/api/region/tree', { method: 'GET' })
}

export const getRegion = (id: IdType): Promise<ApiEnvelope<Region>> => {
  return request<Region>(`/api/region/${id}`, { method: 'GET' })
}

export const addRegion = (data: Partial<Region>): Promise<ApiEnvelope<Region>> => {
  return request<Region>('/api/region', { method: 'POST', body: data })
}

export const updateRegion = (data: Partial<Region>): Promise<ApiEnvelope<Region>> => {
  return request<Region>('/api/region', { method: 'PUT', body: data })
}

export const deleteRegion = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/region/${id}`, { method: 'DELETE' })
}
