import { request } from './request';
import type { ApiEnvelope, PageResult } from '../types/api';

export interface RiskMonitoringListQuery {
  regionId?: number;
  riskPointId?: number;
  deviceCode?: string;
  riskLevel?: string;
  onlineStatus?: number;
  pageNum?: number;
  pageSize?: number;
}

export interface RiskMonitoringListItem {
  bindingId: number;
  regionId?: number | null;
  regionName?: string | null;
  riskPointId?: number | null;
  riskPointName?: string | null;
  riskLevel?: string | null;
  deviceId?: number | null;
  deviceCode?: string | null;
  deviceName?: string | null;
  productName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  currentValue?: string | null;
  unit?: string | null;
  monitorStatus?: string | null;
  onlineStatus?: number | null;
  latestReportTime?: string | null;
  alarmFlag?: boolean | null;
}

export interface RiskMonitoringTrendPoint {
  reportTime?: string | null;
  value?: string | null;
  numericValue?: number | null;
}

export interface RiskMonitoringAlarmSummary {
  id: number;
  alarmCode?: string | null;
  alarmTitle?: string | null;
  alarmLevel?: string | null;
  status?: number | null;
  currentValue?: string | null;
  thresholdValue?: string | null;
  triggerTime?: string | null;
}

export interface RiskMonitoringEventSummary {
  id: number;
  eventCode?: string | null;
  eventTitle?: string | null;
  riskLevel?: string | null;
  status?: number | null;
  currentValue?: string | null;
  triggerTime?: string | null;
}

export interface RiskMonitoringDetail {
  bindingId: number;
  regionId?: number | null;
  regionName?: string | null;
  riskPointId?: number | null;
  riskPointCode?: string | null;
  riskPointName?: string | null;
  riskLevel?: string | null;
  deviceId?: number | null;
  deviceCode?: string | null;
  deviceName?: string | null;
  productName?: string | null;
  metricIdentifier?: string | null;
  metricName?: string | null;
  currentValue?: string | null;
  unit?: string | null;
  valueType?: string | null;
  monitorStatus?: string | null;
  onlineStatus?: number | null;
  latestReportTime?: string | null;
  longitude?: number | null;
  latitude?: number | null;
  address?: string | null;
  activeAlarmCount?: number | null;
  recentEventCount?: number | null;
  trendPoints?: RiskMonitoringTrendPoint[] | null;
  recentAlarms?: RiskMonitoringAlarmSummary[] | null;
  recentEvents?: RiskMonitoringEventSummary[] | null;
}

export interface RiskMonitoringGisPoint {
  regionId?: number | null;
  regionName?: string | null;
  riskPointId: number;
  riskPointCode?: string | null;
  riskPointName?: string | null;
  riskLevel?: string | null;
  longitude?: number | null;
  latitude?: number | null;
  deviceCount?: number | null;
  onlineDeviceCount?: number | null;
  activeAlarmCount?: number | null;
}

export function getRiskMonitoringList(
  params: RiskMonitoringListQuery = {}
): Promise<ApiEnvelope<PageResult<RiskMonitoringListItem>>> {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value));
    }
  });
  const path = query.toString()
    ? `/api/risk-monitoring/realtime/list?${query.toString()}`
    : '/api/risk-monitoring/realtime/list';
  return request<PageResult<RiskMonitoringListItem>>(path, { method: 'GET' });
}

export function getRiskMonitoringDetail(bindingId: number): Promise<ApiEnvelope<RiskMonitoringDetail>> {
  return request<RiskMonitoringDetail>(`/api/risk-monitoring/realtime/${bindingId}`, { method: 'GET' });
}

export function getRiskMonitoringGisPoints(regionId?: number): Promise<ApiEnvelope<RiskMonitoringGisPoint[]>> {
  const path = regionId === undefined || regionId === null
    ? '/api/risk-monitoring/gis/points'
    : `/api/risk-monitoring/gis/points?regionId=${regionId}`;
  return request<RiskMonitoringGisPoint[]>(path, { method: 'GET' });
}
