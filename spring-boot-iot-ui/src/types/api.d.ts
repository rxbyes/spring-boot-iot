/**
 * API 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

/**
 * 分页响应格式
 */
export interface PageResponse<T = any> {
  list: T[];
  total: number;
  pageSize: number;
  currentPage: number;
}

/**
 * 产品相关类型
 */
export interface Product {
  id: number;
  productKey: string;
  productName: string;
  protocolCode: string;
  nodeType: number;
  dataFormat: string;
  manufacturer: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductModel {
  id: string | number;
  productId: string | number;
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  dataType?: string | null;
  specsJson?: string | null;
  eventType?: string | null;
  serviceInputJson?: string | null;
  serviceOutputJson?: string | null;
  sortNo?: number | null;
  requiredFlag?: number | null;
  description?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

export interface ProductModelUpsertPayload {
  modelType: 'property' | 'event' | 'service';
  identifier: string;
  modelName: string;
  dataType?: string;
  specsJson?: string;
  eventType?: string;
  serviceInputJson?: string;
  serviceOutputJson?: string;
  sortNo?: number;
  requiredFlag?: number;
  description?: string;
}

/**
 * 设备相关类型
 */
export interface Device {
  id?: number | string | null;
  productId?: number | string | null;
  gatewayId?: number | string | null;
  parentDeviceId?: number | string | null;
  sourceRecordId?: number | string | null;
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
  registrationStatus?: number | null;
  assetSourceType?: string | null;
  firmwareVersion?: string | null;
  ipAddress?: string | null;
  address?: string | null;
  metadataJson?: string | null;
  lastFailureStage?: string | null;
  lastErrorMessage?: string | null;
  lastReportTopic?: string | null;
  lastTraceId?: string | null;
  lastPayload?: string | null;
  lastOnlineTime?: string | null;
  lastOfflineTime?: string | null;
  lastReportTime?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/**
 * 属性相关类型
 */
export interface DeviceProperty {
  identifier: string;
  propertyName: string;
  propertyValue: string;
  valueType: string;
  unit: string | null;
  timestamp: string | null;
}

/**
 * 消息日志相关类型
 */
export interface MessageLog {
  id: number;
  deviceCode: string;
  topic: string;
  messageType: string;
  payload: string;
  timestamp: string;
  createdAt: string;
}

/**
 * HTTP 请求配置
 */
export interface RequestConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  params?: Record<string, any>;
  data?: Record<string, any>;
  headers?: Record<string, string>;
}

/**
 * HTTP 响应
 */
export interface HttpResponse<T = any> {
  data: T;
  status: number;
  statusText: string;
  headers: Record<string, string>;
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  currentPage?: number;
  pageSize?: number;
  keyword?: string;
}

/**
 * 设备上报消息类型
 */
export interface DeviceUpMessage {
  messageType: 'property' | 'event' | 'status';
  properties?: Record<string, any>;
  events?: Record<string, any>;
  status?: {
    timestamp: string;
    value: string;
  };
}

/**
 * 设备状态
 */
export interface DeviceStatus {
  deviceCode: string;
  onlineStatus: number;
  lastOnlineTime: string | null;
  lastReportTime: string | null;
  lastSeenTime: string | null;
}
