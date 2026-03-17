import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface Organization {
  id: IdType
  tenantId: IdType
  parentId: IdType
  orgName: string
  orgCode: string
  orgType: string
  leaderUserId?: IdType
  leaderName: string
  phone: string
  email: string
  status: number
  sortNo: number
  remark: string
  createBy: number
  createTime: string
  updateBy: number
  updateTime: string
  deleted: number
  children?: Organization[]
  hasChildren?: boolean
}

export interface OrganizationQueryParams {
  orgName?: string
  orgCode?: string
  status?: number
}

export interface OrganizationPageQueryParams extends OrganizationQueryParams {
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

export const listOrganizations = (parentId?: IdType): Promise<ApiEnvelope<Organization[]>> => {
  const query = buildQuery({ parentId })
  return request<Organization[]>(`/api/organization/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const pageOrganizations = (params: OrganizationPageQueryParams = {}): Promise<ApiEnvelope<PageResult<Organization>>> => {
  const query = buildQuery(params)
  return request<PageResult<Organization>>(`/api/organization/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const listOrganizationTree = (): Promise<ApiEnvelope<Organization[]>> => {
  return request<Organization[]>('/api/organization/tree', { method: 'GET' })
}

export const getOrganization = (id: IdType): Promise<ApiEnvelope<Organization>> => {
  return request<Organization>(`/api/organization/${id}`, { method: 'GET' })
}

export const addOrganization = (data: Partial<Organization>): Promise<ApiEnvelope<Organization>> => {
  return request<Organization>('/api/organization', { method: 'POST', body: data })
}

export const updateOrganization = (data: Partial<Organization>): Promise<ApiEnvelope<Organization>> => {
  return request<Organization>('/api/organization', { method: 'PUT', body: data })
}

export const deleteOrganization = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/organization/${id}`, { method: 'DELETE' })
}
