import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 联动规则 API
 */

// 联动规则接口定义
export interface LinkageRule {
      id: IdType;
      ruleName: string;
      description: string;
      triggerCondition: string;
      actionList: string;
      status: number;
      tenantId: IdType;
      createTime: string;
      updateTime: string;
      createBy: number;
      updateBy: number;
      deleted: number;
}

export interface LinkageRulePageResult {
      total: number;
      pageNum: number;
      pageSize: number;
      records: LinkageRule[];
}

// 获取规则列表
export const getRuleList = (params?: {
      ruleName?: string;
      status?: number;
}): Promise<ApiEnvelope<LinkageRule[]>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/linkage-rule/list?${queryString}` : '/api/linkage-rule/list';
      return request<LinkageRule[]>(path, { method: 'GET' });
};

// 分页获取规则列表
export const pageRuleList = (params?: {
      ruleName?: string;
      status?: number;
      pageNum?: number;
      pageSize?: number;
}): Promise<ApiEnvelope<LinkageRulePageResult>> => {
      const queryString = buildQueryString(params);
      const path = queryString ? `/api/linkage-rule/page?${queryString}` : '/api/linkage-rule/page';
      return request<LinkageRulePageResult>(path, { method: 'GET' });
};

// 获取规则详情
export const getRuleById = (id: IdType): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>(`/api/linkage-rule/get/${id}`, { method: 'GET' });
};

// 新增规则
export const addRule = (data: Partial<LinkageRule>): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>('/api/linkage-rule/add', { method: 'POST', body: data });
};

// 更新规则
export const updateRule = (data: Partial<LinkageRule>): Promise<ApiEnvelope<LinkageRule>> => {
      return request<LinkageRule>('/api/linkage-rule/update', { method: 'POST', body: data });
};

// 删除规则
export const deleteRule = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/linkage-rule/delete/${id}`, { method: 'POST' });
};
