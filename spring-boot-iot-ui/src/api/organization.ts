import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 组织机构 API
 */

// 组织机构接口定义
export interface Organization {
      id: IdType;
      tenantId: IdType;
      parentId: IdType;
      orgName: string;
      orgCode: string;
      orgType: string; // dept/position/team
      leaderUserId: IdType;
      leaderName: string;
      phone: string;
      email: string;
      status: number; // 1启用 0禁用
      sortNo: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
      children?: Organization[];
}

// 查询组织机构列表
export const listOrganizations = (parentId?: number): Promise<ApiEnvelope<Organization[]>> => {
      const queryString = parentId ? `?parentId=${parentId}` : '';
      const path = `/api/organization/list${queryString}`;
      return request<Organization[]>(path, { method: 'GET' });
};

// 查询组织机构树
export const listOrganizationTree = (): Promise<ApiEnvelope<Organization[]>> => {
      return request<Organization[]>('/api/organization/tree', { method: 'GET' });
};

// 根据ID查询组织机构
export const getOrganization = (id: IdType): Promise<ApiEnvelope<Organization>> => {
      return request<Organization>(`/api/organization/${id}`, { method: 'GET' });
};

// 添加组织机构
export const addOrganization = (data: Partial<Organization>): Promise<ApiEnvelope<Organization>> => {
      return request<Organization>('/api/organization', { method: 'POST', body: data });
};

// 更新组织机构
export const updateOrganization = (data: Partial<Organization>): Promise<ApiEnvelope<Organization>> => {
      return request<Organization>('/api/organization', { method: 'PUT', body: data });
};

// 删除组织机构
export const deleteOrganization = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/organization/${id}`, { method: 'DELETE' });
};
