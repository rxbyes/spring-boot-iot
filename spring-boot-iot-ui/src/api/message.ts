import { request } from './request';
import { buildQueryString } from './query';
import type {
  DeviceMessageLog,
  HttpReportPayload,
  MessageFlowOpsOverview,
  MessageFlowRecentSession,
  MessageFlowSession,
  MessageFlowSubmitResult,
  MessageTraceDetail,
  MessageFlowTimeline,
  MessageTraceStats,
  PageResult
} from '../types/api';

export interface MessageTraceQueryParams {
  keyword?: string;
  deviceCode?: string;
  productKey?: string;
  traceId?: string;
  messageType?: string;
  topic?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface MessageFlowRecentQueryParams {
  size?: number;
  deviceCode?: string;
  topic?: string;
  transportMode?: string;
  status?: string;
}

/**
 * 消息相关API
 */
export const messageApi = {
  /**
   * HTTP模拟上报
   */
  reportByHttp(payload: HttpReportPayload) {
    return request<MessageFlowSubmitResult>('/api/message/http/report', {
      method: 'POST',
      body: payload
    });
  },

  getMessageFlowSession(sessionId: string) {
    return request<MessageFlowSession>(`/api/device/message-flow/session/${sessionId}`);
  },

  getMessageFlowTrace(traceId: string) {
    return request<MessageFlowTimeline>(`/api/device/message-flow/trace/${traceId}`);
  },

  getMessageTraceDetail(id: string | number) {
    return request<MessageTraceDetail>(`/api/device/message-flow/detail/${id}`);
  },

  getMessageFlowOpsOverview() {
    return request<MessageFlowOpsOverview>('/api/device/message-flow/ops/overview');
  },

  getMessageFlowRecentSessions(params: MessageFlowRecentQueryParams = {}) {
    const query = buildQueryString(params);
    const path = `/api/device/message-flow/recent${query ? `?${query}` : ''}`;
    return request<MessageFlowRecentSession[]>(path);
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
   * 查询消息追踪统计概览
   */
  pageMessageTraceStats(params: MessageTraceQueryParams = {}) {
    const query = buildQueryString(params);
    const path = `/api/device/message-trace/stats${query ? `?${query}` : ''}`;
    return request<MessageTraceStats>(path);
  },

  /**
   * 查询消息日志详情
   */
  getMessageLogById(id: string | number) {
    return request<DeviceMessageLog>(`/api/message/log/${id}`);
  },

  /**
   * 删除消息日志
   */
  deleteMessageLog(id: string | number) {
    return request<null>(`/api/message/log/${id}`, {
      method: 'DELETE'
    });
  }
};
