import { ElMessage } from '@/utils/message';

import router from '../router';
import { clearStoredAuth, getStoredAccessToken, usePermissionStore } from '../stores/permission';
import { interceptorManager } from './request';
import type { RequestInterceptor, ResponseInterceptor } from './request';

const ERROR_CODE_MAP: Record<number, string> = {
  400: '请求参数错误',
  401: '未授权，请重新登�?,
  403: '拒绝访问',
  404: '请求资源不存�?,
  500: '服务器内部错�?,
  502: '网关错误',
  503: '服务不可�?,
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
        // 会话失效后同时清理响应式状态并回到登录页，避免停留在受保护页面�?        const permissionStore = usePermissionStore();
        permissionStore.logout();
        clearStoredAuth();
        if (router.currentRoute.value.path !== '/login') {
          await router.push({
            path: '/login',
            query: {
              redirect: router.currentRoute.value.fullPath
            }
          });
        }
      }
      const message = data.msg || ERROR_CODE_MAP[data.code] || '请求失败';
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

