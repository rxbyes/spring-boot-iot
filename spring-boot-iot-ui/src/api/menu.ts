import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'
import type { MenuTreeNode } from '../types/auth'

export interface Menu {
  id: IdType
  parentId?: IdType | null
  menuName: string
  menuCode?: string
  path?: string
  component?: string
  icon?: string
  metaJson?: string
  sort?: number
  type?: number
  status?: number
  createBy?: number
  createTime?: string
  updateBy?: number
  updateTime?: string
  children?: Menu[]
  hasChildren?: boolean
}

export interface MenuListParams {
  parentId?: IdType
  menuName?: string
  menuCode?: string
  type?: number
  status?: number
}

export interface MenuPageQueryParams {
  menuName?: string
  menuCode?: string
  type?: number
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

export function listMenuTree(): Promise<ApiEnvelope<MenuTreeNode[]>> {
  return request<MenuTreeNode[]>('/api/menu/tree', { method: 'GET' })
}

export function listMenus(params: MenuListParams = {}): Promise<ApiEnvelope<Menu[]>> {
  const query = buildQuery(params)
  return request<Menu[]>(`/api/menu/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function pageMenus(params: MenuPageQueryParams = {}): Promise<ApiEnvelope<PageResult<Menu>>> {
  const query = buildQuery(params)
  return request<PageResult<Menu>>(`/api/menu/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function getMenu(id: IdType): Promise<ApiEnvelope<Menu>> {
  return request<Menu>(`/api/menu/${id}`, { method: 'GET' })
}

export function addMenu(payload: Partial<Menu>): Promise<ApiEnvelope<Menu>> {
  return request<Menu>('/api/menu/add', {
    method: 'POST',
    body: payload
  })
}

export function updateMenu(payload: Partial<Menu>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/menu/update', {
    method: 'PUT',
    body: payload
  })
}

export function deleteMenu(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/menu/${id}`, { method: 'DELETE' })
}
