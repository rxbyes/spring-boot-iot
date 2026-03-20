import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 应急预案 API
 */

// 应急预案接口定义
export interface EmergencyPlan {
      id: IdType;
      planName: string;
      riskLevel: string;
      description: string;
      responseSteps: string;
      contactList: string;
      status: number;
      tenantId: IdType;
      createTime: string;
      updateTime: string;
      createBy: number;
      updateBy: number;
      deleted: number;
}

export interface EmergencyPlanPageResult {
      total: number;
      pageNum: number;
      pageSize: number;
      records: EmergencyPlan[];
}

// 获取预案列表
export const getPlanList = (params?: {
      planName?: string;
      riskLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<EmergencyPlan[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/emergency-plan/list?${queryString}` : '/api/emergency-plan/list';
      return request<EmergencyPlan[]>(path, { method: 'GET' });
};

// 分页获取预案列表
export const pagePlanList = (params?: {
      planName?: string;
      riskLevel?: string;
      status?: number;
      pageNum?: number;
      pageSize?: number;
}): Promise<ApiEnvelope<EmergencyPlanPageResult>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/emergency-plan/page?${queryString}` : '/api/emergency-plan/page';
      return request<EmergencyPlanPageResult>(path, { method: 'GET' });
};

// 获取预案详情
export const getPlanById = (id: IdType): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>(`/api/emergency-plan/get/${id}`, { method: 'GET' });
};

// 新增预案
export const addPlan = (data: Partial<EmergencyPlan>): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>('/api/emergency-plan/add', { method: 'POST', body: data });
};

// 更新预案
export const updatePlan = (data: Partial<EmergencyPlan>): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>('/api/emergency-plan/update', { method: 'POST', body: data });
};

// 删除预案
export const deletePlan = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/emergency-plan/delete/${id}`, { method: 'POST' });
};
