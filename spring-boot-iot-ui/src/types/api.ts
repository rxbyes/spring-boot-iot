export interface ApiEnvelope<T> {
  code: number;
  msg: string;
  data: T;
}

export interface Product {
  id: number;
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat?: string | null;
  manufacturer?: string | null;
  description?: string | null;
  status?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface Device {
  id: number;
  productId?: number | null;
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

export interface DeviceProperty {
  id: number;
  identifier: string;
  propertyName?: string | null;
  propertyValue?: string | null;
  valueType?: string | null;
  reportTime?: string | null;
  updateTime?: string | null;
}

export interface DeviceMessageLog {
  id: number;
  messageType?: string | null;
  topic?: string | null;
  payload?: string | null;
  reportTime?: string | null;
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
}

export interface DeviceAddPayload {
  productKey: string;
  deviceName: string;
  deviceCode: string;
  deviceSecret?: string;
  clientId?: string;
  username?: string;
  password?: string;
  firmwareVersion?: string;
  ipAddress?: string;
  address?: string;
  metadataJson?: string;
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

export interface ActivityEntry {
  id: string;
  module: string;
  action: string;
  request: unknown;
  response?: unknown;
  ok: boolean;
  createdAt: string;
  detail: string;
}
