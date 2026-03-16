import { request } from './request';
import type { ApiEnvelope } from '../types/api';
import type { MenuTreeNode } from '../types/auth';

export function listMenuTree(): Promise<ApiEnvelope<MenuTreeNode[]>> {
  return request<MenuTreeNode[]>('/api/menu/tree', { method: 'GET' });
}
