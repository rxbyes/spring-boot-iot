import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 阈值规则配置 API
 */

// 阈值规则接口定义
export interface RuleDefinition {
      id: number;
      ruleName: string;
      metricIdentifier: string;
      metricName: string;
      expression: string;
      duration: number;
      alarmLevel: string;
      notificationMethods: string;
      convertToEvent: number;
      status: number;
      tenantId: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 获取规则列表
export const getRuleList = (params?: {
      ruleName?: string;
      metricIdentifier?: string;
      alarmLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<RuleDefinition[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/rule-definition/list?${queryString}` : '/rule-definition/list';
      return request<RuleDefinition[]>(path, { method: 'GET' });
};

// 获取规则详情
export const getRuleById = (id: number): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>(`/rule-definition/get/${id}`, { method: 'GET' });
};

// 新增规则
export const addRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/rule-definition/add', { method: 'POST', body: data });
};

// 更新规则
export const updateRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/rule-definition/update', { method: 'POST', body: data });
};

// 删除规则
export const deleteRule = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/rule-definition/delete/${id}`, { method: 'POST' });
};
