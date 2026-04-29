import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

export type RuleDefinitionScope = 'METRIC' | 'PRODUCT_TYPE' | 'PRODUCT' | 'DEVICE' | 'BINDING';
export type RuleDefinitionScopeView = 'BUSINESS' | 'SYSTEM' | 'ALL';

/**
 * 阈值规则配置 API
 */

// 阈值规则接口定义
export interface RuleDefinition {
      id: IdType;
      riskMetricId?: IdType | null;
      ruleScope?: RuleDefinitionScope | string | null;
      productType?: string | null;
      productId?: IdType | null;
      deviceId?: IdType | null;
      riskPointDeviceId?: IdType | null;
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

export interface RuleDefinitionBatchAddItem {
      index: number;
      ruleId?: IdType | null;
      ruleName?: string | null;
      metricIdentifier?: string | null;
      success?: boolean | null;
      message?: string | null;
}

export interface RuleDefinitionBatchAddResult {
      totalCount: number;
      successCount: number;
      failedCount: number;
      items: RuleDefinitionBatchAddItem[];
}

export interface RuleDefinitionEffectivePreviewCandidate {
      ruleId?: IdType | null;
      ruleName?: string | null;
      ruleScope?: RuleDefinitionScope | string | null;
      ruleScopeText?: string | null;
      scopeTarget?: string | null;
      metricIdentifier?: string | null;
      metricName?: string | null;
      expression?: string | null;
      alarmLevel?: string | null;
      status?: number | null;
      priority?: number | null;
      matchedContext?: boolean | null;
      selected?: boolean | null;
      reason?: string | null;
}

export interface RuleDefinitionEffectivePreview {
      tenantId?: IdType | null;
      riskMetricId?: IdType | null;
      metricIdentifier?: string | null;
      productId?: IdType | null;
      productType?: string | null;
      deviceId?: IdType | null;
      riskPointDeviceId?: IdType | null;
      hasMatchedRule?: boolean | null;
      matchedScope?: RuleDefinitionScope | string | null;
      matchedScopeText?: string | null;
      decision?: string | null;
      matchedRule?: RuleDefinition | null;
      candidates?: RuleDefinitionEffectivePreviewCandidate[];
}

// 获取规则列表
export const getRuleList = (params?: {
      riskMetricId?: IdType;
      ruleName?: string;
      metricIdentifier?: string;
      alarmLevel?: string;
      status?: number;
      ruleScope?: RuleDefinitionScope | string;
      scopeView?: RuleDefinitionScopeView | string;
      productType?: string;
      productId?: IdType;
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
      ruleScope?: RuleDefinitionScope | string;
      scopeView?: RuleDefinitionScopeView | string;
      productType?: string;
      productId?: IdType;
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

export const previewEffectiveRule = (params: {
      tenantId?: IdType;
      riskMetricId?: IdType;
      metricIdentifier?: string;
      productId?: IdType;
      productType?: string;
      deviceId?: IdType;
      riskPointDeviceId?: IdType;
}): Promise<ApiEnvelope<RuleDefinitionEffectivePreview>> => {
      const queryString = buildQueryString(params);
      const path = queryString
            ? `/api/rule-definition/effective-preview?${queryString}`
            : '/api/rule-definition/effective-preview';
      return request<RuleDefinitionEffectivePreview>(path, { method: 'GET' });
};

// 新增规则
export const addRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/api/rule-definition/add', { method: 'POST', body: data });
};

// 更新规则
export const addRuleBatch = (data: Array<Partial<RuleDefinition>>): Promise<ApiEnvelope<RuleDefinitionBatchAddResult>> => {
      return request<RuleDefinitionBatchAddResult>('/api/rule-definition/batch-add', { method: 'POST', body: data });
};

export const updateRule = (data: Partial<RuleDefinition>): Promise<ApiEnvelope<RuleDefinition>> => {
      return request<RuleDefinition>('/api/rule-definition/update', { method: 'POST', body: data });
};

// 删除规则
export const deleteRule = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/rule-definition/delete/${id}`, { method: 'POST' });
};
