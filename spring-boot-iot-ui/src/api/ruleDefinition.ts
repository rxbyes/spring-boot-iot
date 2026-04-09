import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 阈值规则配置 API
 */

// 阈值规则接口定义
export interface RuleDefinition {
      id: IdType;
      riskMetricId?: IdType | null;
      ruleName: string;
      metricIdentifier: string;
      metricName: string;
      expression: string;
      duration: number;
      alarmLevel: string;
      notificationMethods: string;
      convertToEvent: number;
      status: number;
      tenantId: IdType;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

export interface RuleDefinitionPageResult {
      total: number;
      pageNum: number;
      pageSize: number;
      records: RuleDefinition[];
}

// 获取规则列表
export const getRuleList = (params?: {
      riskMetricId?: IdType;
      ruleName?: string;
      metricIdentifier?: string;
      alarmLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<RuleDefinition[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/rule-definition/list?${queryString}` : '/api/rule-definition/list';
      return request<RuleDefinition[]>(path, { method: 'GET' });
};

// 分页获取规则列表
export const pageRuleList = (params?: {
      riskMetricId?: IdType;
      ruleName?: string;
      metricIdentifier?: string;
      alarmLevel?: string;
      status?: number;
      pageNum?: number;
      pageSize?: number;
}): Promise<ApiEnvelope<RuleDefinitionPageResult>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/rule-definition/page?${queryString}` : '/api/rule-definition/page';
      return request<RuleDefinitionPageResult>(path, { method: 'GET' });
};

// 获取规则详情
export const getRuleById = (id: IdType): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>(`/api/rule-definition/get/${id}`, { method: 'GET' });
};

// 新增规则
export const addRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/api/rule-definition/add', { method: 'POST', body: data });
};

// 更新规则
export const updateRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/api/rule-definition/update', { method: 'POST', body: data });
};

// 删除规则
export const deleteRule = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/rule-definition/delete/${id}`, { method: 'POST' });
};
