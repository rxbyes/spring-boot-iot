import { request } from './request';
import type {
  Device,
  DeviceAddPayload,
  DeviceFileSnapshot,
  DeviceFirmwareAggregate,
  DeviceMessageLog,
  DeviceProperty,
  HttpReportPayload
} from '../types/api';

/**
 * 设备相关API
 */
export const deviceApi = {
  /**
   * 新增设备
   */
  addDevice(payload: DeviceAddPayload) {
    return request<Device>('/device/add', {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 根据ID查询设备
   */
  getDeviceById(id: string | number) {
    return request<Device>(`/device/${id}`);
  },

  /**
   * 根据设备编码查询设备
   */
  getDeviceByCode(deviceCode: string) {
    return request<Device>(`/device/code/${deviceCode}`);
  },

  /**
   * 查询设备属性
   */
  getDeviceProperties(deviceCode: string) {
    return request<DeviceProperty[]>(`/device/${deviceCode}/properties`);
  },

  /**
   * 查询设备消息日志
   */
  getDeviceMessageLogs(deviceCode: string) {
    return request<DeviceMessageLog[]>(`/device/${deviceCode}/message-logs`);
  },

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
   * 查询设备文件快照
   */
  getDeviceFileSnapshots(deviceCode: string) {
    return request<DeviceFileSnapshot[]>(`/device/${deviceCode}/file-snapshots`);
  },

  /**
   * 查询设备固件聚合结果
   */
  getDeviceFirmwareAggregates(deviceCode: string) {
    return request<DeviceFirmwareAggregate[]>(`/device/${deviceCode}/firmware-aggregates`);
  },

  /**
   * 更新设备
   */
  updateDevice(id: string | number, payload: Partial<DeviceAddPayload>) {
    return request<Device>(`/device/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  /**
   * 删除设备
   */
  deleteDevice(id: string | number) {
    return request<null>(`/device/${id}`, {
      method: 'DELETE'
    });
  }
};