import { request } from './request'
import type { ApiEnvelope, IdType, PageResult } from '../types/api'

export interface User {
  id?: IdType
  tenantId?: IdType
  orgId?: IdType
  orgName?: string
  username: string
  nickname?: string
  realName: string
  phone?: string
  email?: string
  avatar?: string
  isAdmin?: number
  remark?: string
  password?: string
  status: number
  lastLoginTime?: string
  lastLoginIp?: string
  createTime?: string
  updateTime?: string
  roleIds?: IdType[]
  roleNames?: string[]
}

export interface UserQueryParams {
  username?: string
  phone?: string
  email?: string
  status?: number
}

export interface UserPageQueryParams extends UserQueryParams {
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

export function listUsers(params: UserQueryParams = {}): Promise<ApiEnvelope<User[]>> {
  const query = buildQuery(params)
  return request<User[]>(`/api/user/list${query ? `?${query}` : ''}`, { method: 'GET' })
}

export function pageUsers(params: UserPageQueryParams = {}): Promise<ApiEnvelope<PageResult<User>>> {
  const query = buildQuery(params)
  return request<PageResult<User>>(`/api/user/page${query ? `?${query}` : ''}`, { method: 'GET' })
}

export const getUser = (id: IdType): Promise<ApiEnvelope<User>> => {
  return request<User>(`/api/user/${id}`, { method: 'GET' })
}

export const addUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
  return request<User>('/api/user/add', { method: 'POST', body: data })
}

export const updateUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
  return request<User>('/api/user/update', { method: 'PUT', body: data })
}

export const updateCurrentUserProfile = (data: Pick<User, 'nickname' | 'realName' | 'phone' | 'email' | 'avatar'>): Promise<ApiEnvelope<void>> => {
  return request<void>('/api/user/profile', { method: 'PUT', body: data })
}

export const deleteUser = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/user/${id}`, { method: 'DELETE' })
}

export const getUserByUsername = (username: string): Promise<ApiEnvelope<User>> => {
  return request<User>(`/api/user/username/${username}`, { method: 'GET' })
}

export const changePassword = (data: { id: IdType; oldPassword: string; newPassword: string }): Promise<ApiEnvelope<void>> => {
  return request<void>('/api/user/change-password', { method: 'POST', body: data })
}

export const resetPassword = (userId: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/user/reset-password/${userId}`, { method: 'POST' })
}
