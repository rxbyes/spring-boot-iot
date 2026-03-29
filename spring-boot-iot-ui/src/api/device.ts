import { request, type RequestOptions } from './request'
import type {
  ApiEnvelope,
  Device,
  DeviceAddPayload,
  DeviceBatchAddPayload,
  DeviceBatchAddResult,
  DeviceFileSnapshot,
  DeviceFirmwareAggregate,
  DeviceMessageLog,
  MessageFlowSubmitResult,
  DeviceOption,
  DeviceProperty,
  DeviceReplacePayload,
  DeviceReplaceResult,
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
  registrationStatus?: number
  pageNum?: number
  pageSize?: number
}

export interface DeviceOptionQueryParams {
  includeDisabled?: boolean
}

type DeviceRequestOptions = Pick<RequestOptions, 'signal'>

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

export function getDeviceById(id: IdType, options: DeviceRequestOptions = {}): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/${id}`, {
    ...options
  })
}

export function getDeviceByCode(deviceCode: string): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/code/${deviceCode}`)
}

export function pageDevices(
  params: DevicePageQueryParams = {},
  options: DeviceRequestOptions = {}
): Promise<ApiEnvelope<PageResult<Device>>> {
  const query = buildQuery(params)
  return request<PageResult<Device>>(`/api/device/page${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function updateDevice(id: IdType, payload: DeviceAddPayload): Promise<ApiEnvelope<Device>> {
  return request<Device>(`/api/device/${id}`, {
    method: 'PUT',
    body: payload
  })
}

export function batchAddDevices(payload: DeviceBatchAddPayload): Promise<ApiEnvelope<DeviceBatchAddResult>> {
  return request<DeviceBatchAddResult>('/api/device/batch-add', {
    method: 'POST',
    body: payload
  })
}

export function replaceDevice(id: IdType, payload: DeviceReplacePayload): Promise<ApiEnvelope<DeviceReplaceResult>> {
  return request<DeviceReplaceResult>(`/api/device/${id}/replace`, {
    method: 'POST',
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

export function listDeviceOptions(
  params: DeviceOptionQueryParams = {},
  options: DeviceRequestOptions = {}
): Promise<ApiEnvelope<DeviceOption[]>> {
  const query = buildQuery(params)
  return request<DeviceOption[]>(`/api/device/list${query ? `?${query}` : ''}`, {
    method: 'GET',
    ...options
  })
}

export function getDeviceProperties(deviceCode: string): Promise<ApiEnvelope<DeviceProperty[]>> {
  return request<DeviceProperty[]>(`/api/device/${deviceCode}/properties`)
}

export function getDeviceMessageLogs(deviceCode: string): Promise<ApiEnvelope<DeviceMessageLog[]>> {
  return request<DeviceMessageLog[]>(`/api/device/${deviceCode}/message-logs`)
}

export function reportByHttp(payload: HttpReportPayload): Promise<ApiEnvelope<MessageFlowSubmitResult>> {
  return request<MessageFlowSubmitResult>('/api/message/http/report', {
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
  batchAddDevices,
  replaceDevice,
  deleteDevice,
  batchDeleteDevices,
  listDeviceOptions,
  getDeviceProperties,
  getDeviceMessageLogs,
  reportByHttp,
  getDeviceFileSnapshots,
  getDeviceFirmwareAggregates
}
