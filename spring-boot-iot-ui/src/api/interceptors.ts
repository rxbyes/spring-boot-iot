import { ElMessage } from '@/utils/message';

import router from '../router';
import { getStoredAccessToken, usePermissionStore } from '@/stores/permission';
import { createRequestError } from './request';
import { interceptorManager } from './request';
import type { RequestError, RequestInterceptor, ResponseInterceptor } from './request';

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

let authRedirectPromise: Promise<void> | null = null;

function buildLoginRedirectQuery() {
  const currentRoute = router.currentRoute.value;
  if (!currentRoute?.path || currentRoute.path === '/login') {
    return undefined;
  }
  return {
    redirect: currentRoute.fullPath
  };
}

async function redirectToLogin() {
  if (router.currentRoute.value.path === '/login') {
    return;
  }
  if (!authRedirectPromise) {
    authRedirectPromise = router
      .replace({
        path: '/login',
        query: buildLoginRedirectQuery()
      })
      .finally(() => {
        authRedirectPromise = null;
      });
  }
  await authRedirectPromise;
}

async function handleUnauthorized() {
  const permissionStore = usePermissionStore();
  permissionStore.logout();
  await redirectToLogin();
}

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
      const message = data.msg || ERROR_CODE_MAP[data.code] || '请求失败';
      if (data.code === 401) {
        // 会话失效后统一清理鉴权状态并跳回登录页，避免页面停留在受保护路由或展示原始 401 JSON。
        await handleUnauthorized();
        throw createRequestError(message, true, 401);
      }
      ElMessage.error(message);
      throw createRequestError(message, true, data.code);
    }
    return data;
  },
  async onerror(error) {
    const requestError = error as RequestError;
    if (requestError.status === 401 && !requestError.handled) {
      await handleUnauthorized();
      requestError.handled = true;
    }
    return requestError;
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
