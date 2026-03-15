import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 区域管理 API
 */

// 区域管理接口定义
export interface Region {
      id: number;
      tenantId: number;
      regionName: string;
      regionCode: string;
      parentId: number;
      regionType: string; // province/city/district/street
      longitude: number;
      latitude: number;
      status: number; // 1启用 0禁用
      sortNo: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
      children?: Region[];
}

// 查询区域列表
export const listRegions = (parentId?: number): Promise<ApiEnvelope<Region[]>> => {
      const queryString = parentId ? `?parentId=${parentId}` : '';
      const path = `/api/region/list${queryString}`;
      return request<Region[]>(path, { method: 'GET' });
};

// 查询区域树
export const listRegionTree = (): Promise<ApiEnvelope<Region[]>> => {
      return request<Region[]>('/api/region/tree', { method: 'GET' });
};

// 根据ID查询区域
export const getRegion = (id: number): Promise<ApiEnvelope<Region>> => {
      return request<Region>(`/api/region/${id}`, { method: 'GET' });
};

// 添加区域
export const addRegion = (data: Partial<Region>): Promise<ApiEnvelope<Region>> => {
      return request<Region>('/api/region', { method: 'POST', body: data });
};

// 更新区域
export const updateRegion = (data: Partial<Region>): Promise<ApiEnvelope<Region>> => {
      return request<Region>('/api/region', { method: 'PUT', body: data });
};

// 删除区域
export const deleteRegion = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/region/${id}`, { method: 'DELETE' });
};
