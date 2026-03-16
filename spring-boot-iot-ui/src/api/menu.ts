import { request } from './request';
import type { ApiEnvelope } from '../types/api';
import type { MenuTreeNode } from '../types/auth';

export interface Menu {
  id: number;
  parentId?: number | null;
  menuName: string;
  menuCode?: string;
  path?: string;
  component?: string;
  icon?: string;
  metaJson?: string;
  sort?: number;
  type?: number;
  status?: number;
  createBy?: number;
  createTime?: string;
  updateBy?: number;
  updateTime?: string;
}

export function listMenuTree(): Promise<ApiEnvelope<MenuTreeNode[]>> {
  return request<MenuTreeNode[]>('/api/menu/tree', { method: 'GET' });
}

export function listMenus(params?: {
  menuName?: string;
  menuCode?: string;
  type?: number;
  status?: number;
}): Promise<ApiEnvelope<Menu[]>> {
  const query = new URLSearchParams();
  if (params?.menuName) query.set('menuName', params.menuName);
  if (params?.menuCode) query.set('menuCode', params.menuCode);
  if (typeof params?.type === 'number') query.set('type', String(params.type));
  if (typeof params?.status === 'number') query.set('status', String(params.status));
  const suffix = query.toString() ? `?${query.toString()}` : '';
  return request<Menu[]>(`/api/menu/list${suffix}`, { method: 'GET' });
}

export function getMenu(id: number): Promise<ApiEnvelope<Menu>> {
  return request<Menu>(`/api/menu/${id}`, { method: 'GET' });
}

export function addMenu(payload: Partial<Menu>): Promise<ApiEnvelope<Menu>> {
  return request<Menu>('/api/menu/add', {
    method: 'POST',
    body: payload
  });
}

export function updateMenu(payload: Partial<Menu>): Promise<ApiEnvelope<void>> {
  return request<void>('/api/menu/update', {
    method: 'PUT',
    body: payload
  });
}

export function deleteMenu(id: number): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/menu/${id}`, { method: 'DELETE' });
}
