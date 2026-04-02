import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface Role {
  id?: IdType
  tenantId?: IdType
  roleName: string
  roleCode: string
  description?: string
  dataScopeType?: string
  dataScopeSummary?: string
  status: number
  createTime?: string
  updateTime?: string
  menuIds?: number[]
}

export interface RoleQueryParams {
  roleName?: string
  roleCode?: string
  status?: number
}

export interface RolePageQueryParams extends RoleQueryParams {
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

export function listRoles(params: RoleQueryParams = {}): Promise<ApiEnvelope<Role[]>> {
  const query = buildQuery(params)
  return request<Role[]>(`/api/role/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function pageRoles(params: RolePageQueryParams = {}): Promise<ApiEnvelope<PageResult<Role>>> {
  const query = buildQuery(params)
  return request<PageResult<Role>>(`/api/role/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function getRole(id: IdType): Promise<ApiEnvelope<Role>> {
  return request<Role>(`/api/role/${id}`, { method: 'GET' })
}

export function addRole(data: Partial<Role>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/role/add', { method: 'POST', body: data })
}

export function updateRole(data: Partial<Role>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/role/update', { method: 'PUT', body: data })
}

export function deleteRole(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/role/${id}`, { method: 'DELETE' })
}

export function listUserRoles(userId: IdType): Promise<ApiEnvelope<Role[]>> {
  return request<Role[]>(`/api/role/user/${userId}`, { method: 'GET' })
}
