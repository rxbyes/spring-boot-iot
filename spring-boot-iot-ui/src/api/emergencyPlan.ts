import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 应急预案 API
 */

// 应急预案接口定义
export interface EmergencyPlan {
      id: number;
      planName: string;
      riskLevel: string;
      description: string;
      responseSteps: string;
      contactList: string;
      status: number;
      tenantId: number;
      createTime: string;
      updateTime: string;
      createBy: number;
      updateBy: number;
      deleted: number;
}

// 获取预案列表
export const getPlanList = (params?: {
      planName?: string;
      riskLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<EmergencyPlan[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/emergency-plan/list?${queryString}` : '/emergency-plan/list';
      return request<EmergencyPlan[]>(path, { method: 'GET' });
};

// 获取预案详情
export const getPlanById = (id: number): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>(`/emergency-plan/get/${id}`, { method: 'GET' });
};

// 新增预案
export const addPlan = (data: Partial<EmergencyPlan>): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>('/emergency-plan/add', { method: 'POST', body: data });
};

// 更新预案
export const updatePlan = (data: Partial<EmergencyPlan>): Promise<ApiEnvelope<EmergencyPlan>> => {
      return request<EmergencyPlan>('/emergency-plan/update', { method: 'POST', body: data });
};

// 删除预案
export const deletePlan = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/emergency-plan/delete/${id}`, { method: 'POST' });
};
