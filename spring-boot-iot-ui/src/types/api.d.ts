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
  id: number;
  productId: number;
  identifier: string;
  name: string;
  description: string;
  type: 'property' | 'event' | 'service';
  required: boolean;
  readOnly: boolean;
  writeOnly: boolean;
  valueType: string;
  min: number | null;
  max: number | null;
  unit: string | null;
  enumList: string | null;
  createdAt: string;
  updatedAt: string;
}

/**
 * 设备相关类型
 */
export interface Device {
  id: number;
  productKey: string;
  deviceName: string;
  deviceCode: string;
  deviceSecret: string;
  clientId: string;
  username: string;
  password: string;
  firmwareVersion: string;
  ipAddress: string;
  address: string;
  onlineStatus: number;
  lastOnlineTime: string | null;
  lastReportTime: string | null;
  createdAt: string;
  updatedAt: string;
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