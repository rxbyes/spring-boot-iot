export interface ApiEnvelope<T> {
  code: number;
  msg: string;
  data: T;
}

export type IdType = string | number;

export interface PageResult<T> {
  total: number;
  pageNum: number;
  pageSize: number;
  records: T[];
}

export interface StatsBucket {
  label: string;
  value: string;
  count: number;
}

export interface SystemErrorStats {
  total: number;
  todayCount: number;
  mqttCount: number;
  systemCount: number;
  distinctTraceCount: number;
  distinctDeviceCount: number;
  topModules: StatsBucket[];
  topExceptionClasses: StatsBucket[];
  topErrorCodes: StatsBucket[];
}

export interface BusinessAuditStats {
  total: number;
  todayCount: number;
  successCount: number;
  failureCount: number;
  distinctUserCount: number;
  topModules: StatsBucket[];
  topUsers: StatsBucket[];
  topOperationTypes: StatsBucket[];
}

export interface Product {
  id: IdType;
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat?: string | null;
  manufacturer?: string | null;
  description?: string | null;
  status?: number | null;
  deviceCount?: number | null;
  onlineDeviceCount?: number | null;
  lastReportTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface Device {
  id: IdType;
  productId?: number | null;
  gatewayId?: IdType | null;
  parentDeviceId?: IdType | null;
  productKey?: string | null;
  productName?: string | null;
  gatewayDeviceCode?: string | null;
  gatewayDeviceName?: string | null;
  parentDeviceCode?: string | null;
  parentDeviceName?: string | null;
  deviceName: string;
  deviceCode: string;
  deviceSecret?: string | null;
  clientId?: string | null;
  username?: string | null;
  password?: string | null;
  protocolCode?: string | null;
  nodeType?: number | null;
  onlineStatus?: number | null;
  activateStatus?: number | null;
  deviceStatus?: number | null;
  firmwareVersion?: string | null;
  ipAddress?: string | null;
  address?: string | null;
  metadataJson?: string | null;
  lastOnlineTime?: string | null;
  lastOfflineTime?: string | null;
  lastReportTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface DeviceOption {
  id: IdType;
  productId?: number | null;
  gatewayId?: IdType | null;
  parentDeviceId?: IdType | null;
  productKey?: string | null;
  productName?: string | null;
  deviceCode: string;
  deviceName: string;
  nodeType?: number | null;
  onlineStatus?: number | null;
  deviceStatus?: number | null;
}

export interface DeviceMetricOption {
  identifier: string;
  name: string;
  dataType?: string | null;
}

export interface DeviceProperty {
  id: IdType;
  identifier: string;
  propertyName?: string | null;
  propertyValue?: string | null;
  valueType?: string | null;
  reportTime?: string | null;
  updateTime?: string | null;
}

export interface DeviceMessageLog {
  id: IdType;
  deviceId?: number | null;
  productId?: number | null;
  traceId?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  messageType?: string | null;
  topic?: string | null;
  payload?: string | null;
  reportTime?: string | null;
  createTime?: string | null;
}

export interface DeviceAccessErrorLog {
  id: IdType;
  tenantId?: number | null;
  traceId?: string | null;
  protocolCode?: string | null;
  requestMethod?: string | null;
  failureStage?: string | null;
  deviceCode?: string | null;
  productKey?: string | null;
  gatewayDeviceCode?: string | null;
  subDeviceCode?: string | null;
  topicRouteType?: string | null;
  messageType?: string | null;
  topic?: string | null;
  clientId?: string | null;
  payloadSize?: number | null;
  payloadEncoding?: string | null;
  payloadTruncated?: number | null;
  rawPayload?: string | null;
  errorCode?: string | null;
  exceptionClass?: string | null;
  errorMessage?: string | null;
  createTime?: string | null;
}

export interface DeviceFileSnapshot {
  transferId: string;
  deviceCode: string;
  productId?: number | null;
  messageType?: string | null;
  dataSetId?: string | null;
  fileType?: string | null;
  description?: string | null;
  timestamp?: string | null;
  binaryLength?: number | null;
  binaryBase64?: string | null;
  descriptor?: Record<string, unknown> | null;
  completed?: boolean | null;
  updatedTime?: string | null;
}

export interface DeviceFirmwareAggregate {
  transferId: string;
  deviceCode: string;
  productId?: number | null;
  messageType?: string | null;
  dataSetId?: string | null;
  fileType?: string | null;
  description?: string | null;
  timestamp?: string | null;
  binaryLength?: number | null;
  totalPackets?: number | null;
  receivedPacketCount?: number | null;
  receivedPacketIndexes?: number[] | null;
  firmwareMd5?: string | null;
  calculatedMd5?: string | null;
  md5Matched?: boolean | null;
  completed?: boolean | null;
  assembledBase64?: string | null;
  assembledLength?: number | null;
  descriptor?: Record<string, unknown> | null;
  updatedTime?: string | null;
}

export interface ProductAddPayload {
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat?: string;
  manufacturer?: string;
  description?: string;
  status?: number;
}

export interface DeviceAddPayload {
  productKey: string;
  deviceName: string;
  deviceCode: string;
  parentDeviceId?: IdType | null;
  parentDeviceCode?: string | null;
  deviceSecret?: string;
  clientId?: string;
  username?: string;
  password?: string;
  activateStatus?: number;
  deviceStatus?: number;
  firmwareVersion?: string;
  ipAddress?: string;
  address?: string;
  metadataJson?: string;
}

export interface DeviceBatchAddPayload {
  items: DeviceAddPayload[];
}

export interface DeviceBatchAddError {
  rowNo: number;
  deviceCode?: string | null;
  message: string;
}

export interface DeviceBatchAddResult {
  totalCount: number;
  successCount: number;
  failureCount: number;
  createdDeviceCodes?: string[] | null;
  errors: DeviceBatchAddError[];
}

export interface DeviceReplacePayload {
  productKey?: string;
  deviceName: string;
  deviceCode: string;
  parentDeviceId?: IdType | null;
  parentDeviceCode?: string | null;
  deviceSecret?: string;
  clientId?: string;
  username?: string;
  password?: string;
  activateStatus?: number;
  deviceStatus?: number;
  firmwareVersion?: string;
  ipAddress?: string;
  address?: string;
  metadataJson?: string;
}

export interface DeviceReplaceResult {
  sourceDeviceId: IdType;
  sourceDeviceCode: string;
  sourceDeviceName: string;
  targetDeviceId: IdType;
  targetDeviceCode: string;
  targetDeviceName: string;
}

export interface HttpReportPayload {
  protocolCode: string;
  productKey: string;
  deviceCode: string;
  payload: string;
  topic?: string;
  clientId?: string;
  tenantId?: string;
}

export interface ActivityDraft {
  title?: string;
  detail: string;
  tag?: string;
  module?: string;
  action?: string;
  request?: unknown;
  response?: unknown;
  ok?: boolean;
  path?: string;
}

export interface ActivityEntry {
  id: string;
  title: string;
  detail: string;
  createdAt: string;
  tag?: string;
  module?: string;
  action?: string;
  request?: unknown;
  response?: unknown;
  ok: boolean;
  path?: string;
}
