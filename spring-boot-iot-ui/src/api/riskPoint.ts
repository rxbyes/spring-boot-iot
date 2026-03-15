import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 风险点管理 API
 */

// 风险点接口定义
export interface RiskPoint {
      id: number;
      riskPointCode: string;
      riskPointName: string;
      regionId: number;
      regionName: string;
      responsibleUser: number;
      responsiblePhone: string;
      riskLevel: string; // critical-严重, warning-警告, info-提醒
      description: string;
      status: number; // 0-启用，1-停用
      tenantId: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 风险点与设备绑定接口定义
export interface RiskPointDevice {
      id: number;
      riskPointId: number;
      deviceId: number;
      deviceCode: string;
      deviceName: string;
      metricIdentifier: string;
      metricName: string;
      defaultThreshold: string;
      thresholdUnit: string;
      tenantId: number;
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
      riskLevel?: string;
      status?: number;
}): Promise<ApiEnvelope<RiskPoint[]>> => {
      const queryString = params ? new URLSearchParams(params as any).toString() : '';
      const path = queryString ? `/risk-point/list?${queryString}` : '/risk-point/list';
      return request<RiskPoint[]>(path, { method: 'GET' });
};

// 获取风险点详情
export const getRiskPointById = (id: number): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>(`/risk-point/get/${id}`, { method: 'GET' });
};

// 新增风险点
export const addRiskPoint = (data: Partial<RiskPoint>): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>('/risk-point/add', { method: 'POST', body: data });
};

// 更新风险点
export const updateRiskPoint = (data: Partial<RiskPoint>): Promise<ApiEnvelope<RiskPoint>> => {
      return request<RiskPoint>('/risk-point/update', { method: 'POST', body: data });
};

// 删除风险点
export const deleteRiskPoint = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/risk-point/delete/${id}`, { method: 'POST' });
};

// 绑定风险点与设备
export const bindDevice = (data: Partial<RiskPointDevice>): Promise<ApiEnvelope<void>> => {
      return request<void>('/risk-point/bind-device', { method: 'POST', body: data });
};

// 解绑风险点与设备
export const unbindDevice = (riskPointId: number, deviceId: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/risk-point/unbind-device?riskPointId=${riskPointId}&deviceId=${deviceId}`, { method: 'POST' });
};

// 获取风险点绑定的设备列表
export const getBoundDevices = (riskPointId: number): Promise<ApiEnvelope<RiskPointDevice[]>> => {
      return request<RiskPointDevice[]>(`/risk-point/bound-devices/${riskPointId}`, { method: 'GET' });
};
