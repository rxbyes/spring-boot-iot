import { request } from './request';
import type {
  Device,
  DeviceAddPayload,
  DeviceMetricOption,
  DeviceOption,
  DeviceFileSnapshot,
  DeviceFirmwareAggregate,
  DeviceMessageLog,
  DeviceProperty,
  HttpReportPayload,
  Product,
  ProductAddPayload
} from '../types/api';

export function addProduct(payload: ProductAddPayload) {
  return request<Product>('/api/device/product/add', {
    method: 'POST',
    body: payload
  });
}

export function getProductById(id: string | number) {
  return request<Product>(`/api/device/product/${id}`);
}

export function addDevice(payload: DeviceAddPayload) {
  return request<Device>('/api/device/add', {
    method: 'POST',
    body: payload
  });
}

export function getDeviceById(id: string | number) {
  return request<Device>(`/api/device/${id}`);
}

export function getDeviceByCode(deviceCode: string) {
  return request<Device>(`/api/device/code/${deviceCode}`);
}

export function listDeviceOptions() {
  return request<DeviceOption[]>('/api/device/list');
}

export function getDeviceMetricOptions(deviceId: string | number) {
  return request<DeviceMetricOption[]>(`/api/device/${deviceId}/metrics`);
}

export function reportByHttp(payload: HttpReportPayload) {
  return request<null>('/api/message/http/report', {
    method: 'POST',
    body: payload
  });
}

export function getDeviceProperties(deviceCode: string) {
  return request<DeviceProperty[]>(`/api/device/${deviceCode}/properties`);
}

export function getDeviceMessageLogs(deviceCode: string) {
  return request<DeviceMessageLog[]>(`/api/device/${deviceCode}/message-logs`);
}

export function getDeviceFileSnapshots(deviceCode: string) {
  return request<DeviceFileSnapshot[]>(`/api/device/${deviceCode}/file-snapshots`);
}

export function getDeviceFirmwareAggregates(deviceCode: string) {
  return request<DeviceFirmwareAggregate[]>(`/api/device/${deviceCode}/firmware-aggregates`);
}
