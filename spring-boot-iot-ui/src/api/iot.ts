import { request } from './request';
import type {
  CollectorChildInsightOverview,
  Device,
  DeviceAddPayload,
  DeviceMetricOption,
  DeviceOption,
  DeviceFileSnapshot,
  DeviceFirmwareAggregate,
  DeviceMessageLog,
  MessageFlowSubmitResult,
  DeviceProperty,
  HttpReportPayload,
  MqttReportPublishPayload,
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

export function listDeviceOptions(params: { includeDisabled?: boolean } = {}) {
  const query = new URLSearchParams();
  if (params.includeDisabled !== undefined) {
    query.set('includeDisabled', String(params.includeDisabled));
  }
  const suffix = query.toString();
  return request<DeviceOption[]>(`/api/device/list${suffix ? `?${suffix}` : ''}`);
}

export function getDeviceMetricOptions(deviceId: string | number) {
  return request<DeviceMetricOption[]>(`/api/device/${deviceId}/metrics`);
}

export function reportByHttp(payload: HttpReportPayload) {
  return request<MessageFlowSubmitResult>('/api/message/http/report', {
    method: 'POST',
    body: payload
  });
}

export function reportByMqtt(payload: MqttReportPublishPayload) {
  return request<MessageFlowSubmitResult>('/api/message/mqtt/report/publish', {
    method: 'POST',
    body: payload
  });
}

export function getDeviceProperties(deviceCode: string) {
  return request<DeviceProperty[]>(`/api/device/${deviceCode}/properties`);
}

export function getCollectorChildInsightOverview(deviceCode: string) {
  return request<CollectorChildInsightOverview | null>(`/api/device/${deviceCode}/collector-children/overview`);
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
