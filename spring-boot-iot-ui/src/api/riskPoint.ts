import { request } from './request';
import { buildQueryString } from './query';
import type { ApiEnvelope, IdType } from '../types/api';

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
      responsibleUser: number;
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

// 获取风险点列表
export const getRiskPointList = (params?: {
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
export const bindDevice = (data: Partial<RiskPointDevice>): Promise<ApiEnvelope<void>> => {
      return request<void>('/api/risk-point/bind-device', { method: 'POST', body: data });
};

// 解绑风险点与设备
export const unbindDevice = (riskPointId: IdType, deviceId: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/risk-point/unbind-device?riskPointId=${riskPointId}&deviceId=${deviceId}`, { method: 'POST' });
};

// 获取风险点绑定的设备列表
export const getBoundDevices = (riskPointId: IdType): Promise<ApiEnvelope<RiskPointDevice[]>> => {
      return request<RiskPointDevice[]>(`/api/risk-point/bound-devices/${riskPointId}`, { method: 'GET' });
};

function normalizeRiskPointQuery(params?: {
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
      const { riskPointLevel, riskLevel, ...rest } = params;
      return {
            ...rest,
            riskPointLevel: riskPointLevel || riskLevel || undefined
      };
}
