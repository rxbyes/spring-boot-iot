import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, DeviceMetricOption, DeviceOption, GovernanceSubmissionResult, IdType, PageResult } from '../types/api';

/**
 * 风险点管理 API
 */

// 风险点接口定义
export interface RiskPoint {
      id: IdType;
      riskPointCode: string;
      riskPointName: string;
      orgId: IdType;
      orgName: string;
      regionId: IdType;
      regionName: string;
      responsibleUser: IdType;
      responsibleUserName?: string;
      responsiblePhone: string;
      riskPointLevel?: string; // level_1-一级风险点, level_2-二级风险点, level_3-三级风险点
      currentRiskLevel?: string; // red-红色, orange-橙色, yellow-黄色, blue-蓝色
      riskLevel?: string; // 兼容字段，过渡期与 currentRiskLevel 保持一致
      description: string;
      status: number; // 0-启用，1-停用
      tenantId: IdType;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

export interface RiskPointPageResult {
      total: number;
      pageNum: number;
      pageSize: number;
      records: RiskPoint[];
}

// 风险点与设备绑定接口定义
export interface RiskPointDevice {
      id: IdType;
      riskPointId: IdType;
      deviceId: IdType;
      deviceCode: string;
      deviceName: string;
      riskMetricId?: IdType | null;
      metricIdentifier: string;
      metricName: string;
      defaultThreshold: string;
      thresholdUnit: string;
      tenantId: IdType;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

export interface RiskPointBindingSummary {
      riskPointId: IdType;
      boundDeviceCount: number;
      boundMetricCount: number;
      pendingBindingCount: number;
}

export interface RiskPointBindingMetric {
      bindingId: IdType;
      riskMetricId?: IdType | null;
      metricIdentifier: string;
      metricName?: string | null;
      bindingSource: 'MANUAL' | 'PENDING_PROMOTION' | 'UNKNOWN';
      createTime?: string | null;
}

export interface RiskPointBindingDeviceGroup {
      deviceId: IdType;
      deviceCode: string;
      deviceName: string;
      metricCount: number;
      metrics: RiskPointBindingMetric[];
}

export interface RiskPointPendingBindingItem {
      id: IdType;
      riskPointId: IdType;
      riskPointCode?: string | null;
      riskPointName?: string | null;
      deviceId?: IdType | null;
      deviceCode: string;
      deviceName?: string | null;
      resolutionStatus: string;
      resolutionNote?: string | null;
      batchNo?: string | null;
      sourceRowNo?: number | null;
      promotedBindingId?: IdType | null;
      promotedTime?: string | null;
}

export interface RiskPointPendingMetricCandidate {
      riskMetricId?: IdType | null;
      metricIdentifier: string;
      metricName: string;
      dataType?: string | null;
      evidenceSources: string[];
      lastSeenTime?: string | null;
      sampleValue?: string | null;
      seenCount?: number | null;
      recommendationScore?: number | null;
      recommendationLevel?: string | null;
      reasonSummary?: string | null;
}

export interface RiskPointPendingPromotionHistory {
      id?: IdType;
      pendingBindingId?: IdType;
      riskPointDeviceId?: IdType | null;
      metricIdentifier?: string | null;
      metricName?: string | null;
      promotionStatus?: string | null;
      recommendationLevel?: string | null;
      recommendationScore?: number | null;
      promotionNote?: string | null;
      operatorId?: IdType | null;
      operatorName?: string | null;
      createTime?: string | null;
}

export interface RiskPointPendingCandidateBundle {
      pendingId: IdType;
      batchNo?: string | null;
      riskPointId: IdType;
      riskPointCode?: string | null;
      riskPointName?: string | null;
      deviceId?: IdType | null;
      deviceCode?: string | null;
      deviceName?: string | null;
      resolutionStatus?: string | null;
      resolutionNote?: string | null;
      metricIdentifier?: string | null;
      metricName?: string | null;
      createTime?: string | null;
      candidates: RiskPointPendingMetricCandidate[];
      promotionHistory?: RiskPointPendingPromotionHistory[];
      history?: RiskPointPendingPromotionHistory[];
}

export interface RiskPointPendingPromotionMetric {
      riskMetricId?: IdType | null;
      metricIdentifier: string;
      metricName: string;
}

export interface RiskPointPendingPromotionRequest {
      metrics: RiskPointPendingPromotionMetric[];
      completePending?: boolean;
      promotionNote?: string;
}

export interface RiskPointBindingReplaceRequest {
      riskMetricId?: IdType | null;
      metricIdentifier: string;
      metricName: string;
}

export interface RiskPointPendingPromotionItem {
      metricIdentifier: string;
      metricName?: string | null;
      promotionStatus: string;
      bindingId?: IdType | null;
}

export interface RiskPointPendingPromotionResult {
      pendingId: IdType;
      pendingStatus: string;
      items: RiskPointPendingPromotionItem[];
}

// 获取风险点列表
export const getRiskPointList = (params?: {
      keyword?: string;
      riskPointCode?: string;
      riskPointLevel?: string;
      riskLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<RiskPoint[]>> => {
      const queryString = buildQueryString(normalizeRiskPointQuery(params));
      const path = queryString ? `/api/risk-point/list?${queryString}` : '/api/risk-point/list';
      return request<RiskPoint[]>(path, { method: 'GET' });
};

// 分页获取风险点列表
export const pageRiskPointList = (params?: {
      keyword?: string;
      riskPointCode?: string;
      riskPointLevel?: string;
      riskLevel?: string;
      status?: number;
      pageNum?: number;
      pageSize?: number;
}): Promise<ApiEnvelope<RiskPointPageResult>> => {
      const queryString = buildQueryString(normalizeRiskPointQuery(params));
      const path = queryString ? `/api/risk-point/page?${queryString}` : '/api/risk-point/page';
      return request<RiskPointPageResult>(path, { method: 'GET' });
};

// 获取风险点详情
export const getRiskPointById = (id: IdType): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>(`/api/risk-point/get/${id}`, { method: 'GET' });
};

// 新增风险点
export const addRiskPoint = (data: Partial<RiskPoint>): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>('/api/risk-point/add', { method: 'POST', body: data });
};

// 更新风险点
export const updateRiskPoint = (data: Partial<RiskPoint>): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>('/api/risk-point/update', { method: 'POST', body: data });
};

// 删除风险点
export const deleteRiskPoint = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/risk-point/delete/${id}`, { method: 'POST' });
};

// 绑定风险点与设备
export const bindDevice = (data: Partial<RiskPointDevice>): Promise<ApiEnvelope<GovernanceSubmissionResult>> => {
      return request<GovernanceSubmissionResult>('/api/risk-point/bind-device', { method: 'POST', body: data });
};

// 解绑风险点与设备
export const unbindDevice = (riskPointId: IdType, deviceId: IdType): Promise<ApiEnvelope<GovernanceSubmissionResult>> => {
      return request<GovernanceSubmissionResult>(`/api/risk-point/unbind-device?riskPointId=${riskPointId}&deviceId=${deviceId}`, { method: 'POST' });
};

// 获取风险点绑定的设备列表
export const getBoundDevices = (riskPointId: IdType): Promise<ApiEnvelope<RiskPointDevice[]>> => {
      return request<RiskPointDevice[]>(`/api/risk-point/bound-devices/${riskPointId}`, { method: 'GET' });
};

// 获取风险点可绑定设备列表
export const listBindableDevices = (riskPointId: IdType): Promise<ApiEnvelope<DeviceOption[]>> => {
      return request<DeviceOption[]>(`/api/risk-point/bindable-devices/${riskPointId}`, { method: 'GET' });
};

export const listBindingSummaries = (riskPointIds: IdType[]): Promise<ApiEnvelope<RiskPointBindingSummary[]>> => {
      if (!riskPointIds.length) {
            return Promise.resolve({ code: 200, msg: 'success', data: [] });
      }
      const queryString = buildQueryString({
            riskPointIds: riskPointIds.map((item) => String(item)).join(',')
      });
      return request<RiskPointBindingSummary[]>(`/api/risk-point/binding-summaries?${queryString}`, { method: 'GET' });
};

export const listBindingGroups = (riskPointId: IdType): Promise<ApiEnvelope<RiskPointBindingDeviceGroup[]>> => {
      return request<RiskPointBindingDeviceGroup[]>(`/api/risk-point/binding-groups/${riskPointId}`, { method: 'GET' });
};

export const listFormalBindingMetricOptions = (deviceId: IdType): Promise<ApiEnvelope<DeviceMetricOption[]>> => {
      return request<DeviceMetricOption[]>(`/api/risk-point/devices/${deviceId}/formal-metrics`, { method: 'GET' });
};

export const removeBinding = (bindingId: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/risk-point/bindings/${bindingId}/remove`, { method: 'POST' });
};

export const replaceBinding = (bindingId: IdType, body: RiskPointBindingReplaceRequest): Promise<ApiEnvelope<RiskPointBindingMetric>> => {
      return request<RiskPointBindingMetric>(`/api/risk-point/bindings/${bindingId}/replace`, { method: 'POST', body });
};

export const listPendingBindings = (params: {
      riskPointId: IdType;
      deviceCode?: string;
      resolutionStatus?: string;
      batchNo?: string;
      pageNum?: number;
      pageSize?: number;
}): Promise<ApiEnvelope<PageResult<RiskPointPendingBindingItem>>> => {
      const queryString = buildQueryString(params);
      return request<PageResult<RiskPointPendingBindingItem>>(`/api/risk-point/pending-bindings?${queryString}`, { method: 'GET' });
};

export const getPendingBindingCandidates = (pendingId: IdType): Promise<ApiEnvelope<RiskPointPendingCandidateBundle>> => {
      return request<RiskPointPendingCandidateBundle>(`/api/risk-point/pending-bindings/${pendingId}/candidates`, { method: 'GET' });
};

export const promotePendingBinding = (pendingId: IdType, body: RiskPointPendingPromotionRequest): Promise<ApiEnvelope<GovernanceSubmissionResult>> => {
      return request<GovernanceSubmissionResult>(`/api/risk-point/pending-bindings/${pendingId}/promote`, { method: 'POST', body });
};

export const ignorePendingBinding = (pendingId: IdType, body: { ignoreNote?: string }): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/risk-point/pending-bindings/${pendingId}/ignore`, { method: 'POST', body });
};

function normalizeRiskPointQuery(params?: {
      keyword?: string;
      riskPointCode?: string;
      riskPointLevel?: string;
      riskLevel?: string;
      status?: number;
      pageNum?: number;
      pageSize?: number;
}) {
      if (!params) {
            return params;
      }
      const { keyword, riskPointCode, riskPointLevel, riskLevel, ...rest } = params;
      return {
            ...rest,
            keyword: keyword || riskPointCode || undefined,
            riskPointLevel: riskPointLevel || riskLevel || undefined
      };
}
