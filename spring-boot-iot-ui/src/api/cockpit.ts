import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 驾驶舱 API
 */

// 驾驶舱数据接口定义
export interface CockpitMetric {
      label: string;
      value: string;
      hint: string;
      badge: {
            label: string;
            tone: 'danger' | 'warning' | 'brand' | 'success' | 'info';
      };
}

export interface CockpitTrendData {
      name: string;
      data: number[];
}

export interface CockpitDistributionData {
      value: number;
      name: string;
      color: string;
}

export interface CockpitWarningStatus {
      type: string;
      label: string;
      count: number;
      percentage: number;
      color: string;
      icon: string;
}

export interface CockpitActivity {
      id: IdType;
      time: string;
      desc: string;
}

export interface CockpitData {
      metrics: CockpitMetric[];
      trendData: CockpitTrendData[];
      distributionData: CockpitDistributionData[];
      warningStatuses: CockpitWarningStatus[];
      capabilities: string[];
      recentActivities: CockpitActivity[];
}

// 获取完整的驾驶舱数据
export const getCockpitData = (): Promise<ApiEnvelope<CockpitData>> => {
      return request<CockpitData>('/api/cockpit/data', { method: 'GET' });
};

// 获取风险趋势数据
export const getRiskTrendData = (): Promise<ApiEnvelope<CockpitTrendData[]>> => {
      return request<CockpitTrendData[]>('/api/cockpit/trend', { method: 'GET' });
};

// 获取风险分布数据
export const getRiskDistributionData = (): Promise<ApiEnvelope<CockpitDistributionData[]>> => {
      return request<CockpitDistributionData[]>('/api/cockpit/distribution', { method: 'GET' });
};

// 获取预警状态数据
export const getWarningStatusData = (): Promise<ApiEnvelope<CockpitWarningStatus[]>> => {
      return request<CockpitWarningStatus[]>('/api/cockpit/warnings', { method: 'GET' });
};

// 获取最近活动数据
export const getRecentActivities = (): Promise<ApiEnvelope<CockpitActivity[]>> => {
      return request<CockpitActivity[]>('/api/cockpit/activities', { method: 'GET' });
};
