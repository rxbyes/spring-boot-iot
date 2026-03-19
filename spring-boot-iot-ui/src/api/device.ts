import { request } from './request'
import type {
  ApiEnvelope,
  Device,
  DeviceAddPayload,
  DeviceFileSnapshot,
  DeviceFirmwareAggregate,
  DeviceMessageLog,
  DeviceProperty,
  HttpReportPayload,
  IdType,
  PageResult
} from '../types/api'

export interface DevicePageQueryParams {
  deviceId?: IdType
  productKey?: string
  deviceCode?: string
  deviceName?: string
  onlineStatus?: number
  activateStatus?: number
  deviceStatus?: number
  pageNum?: number
  pageSize?: number
}

function buildQuery(params: Record<string, unknown>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })
  return query.toString()
}

export function addDevice(payload: DeviceAddPayload): Promise<ApiEnvelope<Device>> {
  return request<Device>('/api/device/add', {
    method: 'POST',
    body: payload
  })
}

export function getDeviceById(id: IdType): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/${id}`)
}

export function getDeviceByCode(deviceCode: string): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/code/${deviceCode}`)
}

export function pageDevices(params: DevicePageQueryParams = {}): Promise<ApiEnvelope<PageResult<Device>>> {
  const query = buildQuery(params)
  return request<PageResult<Device>>(`/api/device/page${query ? `?${query}` : ''}`, {
    method: 'GET'
  })
}

export function updateDevice(id: IdType, payload: DeviceAddPayload): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/${id}`, {
    method: 'PUT',
    body: payload
  })
}

export function deleteDevice(id: IdType): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/device/${id}`, {
    method: 'DELETE'
  })
}

export function batchDeleteDevices(ids: IdType[]): Promise<ApiEnvelope<void>> {
  return request<void>('/api/device/batch-delete', {
    method: 'POST',
    body: { ids }
  })
}

export function getDeviceProperties(deviceCode: string): Promise<ApiEnvelope<DeviceProperty[]>> {
  return request<DeviceProperty[]>(`/api/device/${deviceCode}/properties`)
}

export function getDeviceMessageLogs(deviceCode: string): Promise<ApiEnvelope<DeviceMessageLog[]>> {
  return request<DeviceMessageLog[]>(`/api/device/${deviceCode}/message-logs`)
}

export function reportByHttp(payload: HttpReportPayload): Promise<ApiEnvelope<null>> {
  return request<null>('/api/message/http/report', {
    method: 'POST',
    body: payload
  })
}

export function getDeviceFileSnapshots(deviceCode: string): Promise<ApiEnvelope<DeviceFileSnapshot[]>> {
  return request<DeviceFileSnapshot[]>(`/api/device/${deviceCode}/file-snapshots`)
}

export function getDeviceFirmwareAggregates(deviceCode: string): Promise<ApiEnvelope<DeviceFirmwareAggregate[]>> {
  return request<DeviceFirmwareAggregate[]>(`/api/device/${deviceCode}/firmware-aggregates`)
}

export const deviceApi = {
  addDevice,
  getDeviceById,
  getDeviceByCode,
  pageDevices,
  updateDevice,
  deleteDevice,
  batchDeleteDevices,
  getDeviceProperties,
  getDeviceMessageLogs,
  reportByHttp,
  getDeviceFileSnapshots,
  getDeviceFirmwareAggregates
}
