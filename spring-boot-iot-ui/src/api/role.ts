import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

export interface Role {
  id?: IdType;
  tenantId?: IdType;
  roleName: string;
  roleCode: string;
  description?: string;
  status: number;
  createTime?: string;
  updateTime?: string;
  menuIds?: number[];
}

export function listRoles(params: {
  roleName?: string;
  roleCode?: string;
  status?: number;
} = {}): Promise<ApiEnvelope<Role[]>> {
  const query = new URLSearchParams();
  if (params.roleName) query.append('roleName', params.roleName);
  if (params.roleCode) query.append('roleCode', params.roleCode);
  if (params.status !== undefined) query.append('status', String(params.status));
  const path = `/api/role/list${query.toString() ? `?${query.toString()}` : ''}`;
  return request<Role[]>(path, { method: 'GET' });
}

export function getRole(id: IdType): Promise<ApiEnvelope<Role>> {
  return request<Role>(`/api/role/${id}`, { method: 'GET' });
}

export function addRole(data: Partial<Role>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/role/add', { method: 'POST', body: data });
}

export function updateRole(data: Partial<Role>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/role/update', { method: 'PUT', body: data });
}

export function deleteRole(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/role/${id}`, { method: 'DELETE' });
}

export function listUserRoles(userId: IdType): Promise<ApiEnvelope<Role[]>> {
  return request<Role[]>(`/api/role/user/${userId}`, { method: 'GET' });
}
