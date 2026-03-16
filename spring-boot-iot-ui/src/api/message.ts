import { request } from './request';
import type {
  DeviceMessageLog,
  HttpReportPayload
} from '../types/api';

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
