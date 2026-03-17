import { request } from './request';
import { buildQueryString } from './query';
import type {
  DeviceMessageLog,
  HttpReportPayload,
  PageResult
} from '../types/api';

export interface MessageTraceQueryParams {
  deviceCode?: string;
  productKey?: string;
  traceId?: string;
  messageType?: string;
  topic?: string;
  pageNum?: number;
  pageSize?: number;
}

/**
 * 消息相关API
 */
export const messageApi = {
  /**
   * HTTP模拟上报
   */
  reportByHttp(payload: HttpReportPayload) {
    return request<null>('/message/http/report', {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 查询设备消息日志
   */
  getDeviceMessageLogs(deviceCode: string) {
    return request<DeviceMessageLog[]>(`/api/device/${deviceCode}/message-logs`);
  },

  /**
   * 分页查询消息追踪日志
   */
  pageMessageTraceLogs(params: MessageTraceQueryParams = {}) {
    const query = buildQueryString(params);
    const path = `/api/device/message-trace/page${query ? `?${query}` : ''}`;
    return request<PageResult<DeviceMessageLog>>(path);
  },

  /**
   * 查询消息日志详情
   */
  getMessageLogById(id: string | number) {
    return request<DeviceMessageLog>(`/message/log/${id}`);
  },

  /**
   * 删除消息日志
   */
  deleteMessageLog(id: string | number) {
    return request<null>(`/message/log/${id}`, {
      method: 'DELETE'
    });
  }
};
