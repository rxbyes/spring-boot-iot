import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 联动规则 API
 */

// 联动规则接口定义
export interface LinkageRule {
      id: number;
      ruleName: string;
      description: string;
      triggerCondition: string;
      actionList: string;
      status: number;
      tenantId: number;
      createTime: string;
      updateTime: string;
      createBy: number;
      updateBy: number;
      deleted: number;
}

// 获取规则列表
export const getRuleList = (params?: {
      ruleName?: string;
      status?: number;
}): Promise<ApiEnvelope<LinkageRule[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/linkage-rule/list?${queryString}` : '/linkage-rule/list';
      return request<LinkageRule[]>(path, { method: 'GET' });
};

// 获取规则详情
export const getRuleById = (id: number): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>(`/linkage-rule/get/${id}`, { method: 'GET' });
};

// 新增规则
export const addRule = (data: Partial<LinkageRule>): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>('/linkage-rule/add', { method: 'POST', body: data });
};

// 更新规则
export const updateRule = (data: Partial<LinkageRule>): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>('/linkage-rule/update', { method: 'POST', body: data });
};

// 删除规则
export const deleteRule = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/linkage-rule/delete/${id}`, { method: 'POST' });
};
