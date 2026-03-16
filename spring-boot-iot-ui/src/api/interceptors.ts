import { ElMessage } from 'element-plus';
import { interceptorManager } from './request';
import type { RequestInterceptor, ResponseInterceptor } from './request';
import { clearStoredAuth, getStoredAccessToken } from '../stores/permission';

const ERROR_CODE_MAP: Record<number, string> = {
  400: '请求参数错误',
  401: '未授权，请重新登录',
  403: '拒绝访问',
  404: '请求资源不存在',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务不可用',
  504: '网关超时'
};

export const authRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    const token = getStoredAccessToken();
    if (token && options.headers) {
      options.headers.set('Authorization', `Bearer ${token}`);
    }
    return options;
  }
};

export const loadingRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    return options;
  }
};

export const errorResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    if (data.code !== 200) {
      if (data.code === 401) {
        // 会话失效时清理本地登录状态，避免继续带着过期 token 请求。
        clearStoredAuth();
      }
      const message = ERROR_CODE_MAP[data.code] || data.msg || '请求失败';
      ElMessage.error(message);
      throw new Error(message);
    }
    return data;
  }
};

export const emptyDataResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    if (data.data === null || data.data === undefined) {
      (data as any).data = [];
    }
    return data;
  }
};

export const logRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    return options;
  }
};

export const logResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    return data;
  }
};

export const defaultInterceptors = {
  request: [authRequestInterceptor, loadingRequestInterceptor, logRequestInterceptor],
  response: [errorResponseInterceptor, emptyDataResponseInterceptor, logResponseInterceptor]
};

export function registerDefaultInterceptors() {
  const { request: requestInterceptors, response: responseInterceptors } = defaultInterceptors;
  requestInterceptors.forEach((interceptor) => {
    interceptorManager.addRequestInterceptor(interceptor);
  });
  responseInterceptors.forEach((interceptor) => {
    interceptorManager.addResponseInterceptor(interceptor);
  });
}
