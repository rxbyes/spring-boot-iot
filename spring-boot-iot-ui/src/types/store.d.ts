/**
 * Pinia 状态管理类型定义
 */

// Runtime Store
export interface RuntimeState {
  loading: boolean;
  loadingText: string;
  error: string | null;
  lastErrorTime: string | null;
}

// Product Store
export interface ProductState {
  products: Product[];
  currentProduct: Product | null;
  loading: boolean;
  error: string | null;
}

// Theme Store
export interface ThemeState {
  mode: 'light' | 'dark';
  primaryColor: string;
  secondaryColor: string;
  backgroundColor: string;
  textColor: string;
}

// Tabs Store
export interface TabInfo {
  path: string;
  title: string;
  name: string;
  closable: boolean;
}

export interface TabsState {
  visitedTabs: TabInfo[];
  cachedTabs: string[];
}

// Device Store
export interface DeviceState {
  devices: Device[];
  currentDevice: Device | null;
  loading: boolean;
  error: string | null;
}

// Message Store
export interface MessageState {
  messageLogs: MessageLog[];
  loading: boolean;
  error: string | null;
}