import { request } from './request';
import { buildQueryString } from './query';
import type {
  DeviceAccessErrorLog,
  DeviceAccessErrorStats,
  PageResult
} from '../types/api';

export interface DeviceAccessErrorQueryParams {
  traceId?: string;
  protocolCode?: string;
  failureStage?: string;
  deviceCode?: string;
  productKey?: string;
  topicRouteType?: string;
  messageType?: string;
  topic?: string;
  clientId?: string;
  errorCode?: string;
  exceptionClass?: string;
  pageNum?: number;
  pageSize?: number;
}

export const accessErrorApi = {
  /**
   * 分页查询接入失败归档
   */
  pageAccessErrors(params: DeviceAccessErrorQueryParams = {}) {
    const query = buildQueryString(params);
    const path = `/api/device/access-error/page${query ? `?${query}` : ''}`;
    return request<PageResult<DeviceAccessErrorLog>>(path);
  },

  /**
   * 查询接入失败归档统计概览
   */
  getAccessErrorStats(params: DeviceAccessErrorQueryParams = {}) {
    const query = buildQueryString(params);
    const path = `/api/device/access-error/stats${query ? `?${query}` : ''}`;
    return request<DeviceAccessErrorStats>(path);
  },

  /**
   * 查询接入失败归档详情
   */
  getAccessErrorById(id: string | number) {
    return request<DeviceAccessErrorLog>(`/api/device/access-error/${id}`);
  }
};
