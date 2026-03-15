import { ElMessage } from 'element-plus';
import { interceptorManager } from './request';
import type { RequestInterceptor, ResponseInterceptor } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 错误码映射
 */
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

/**
 * 请求拦截器：添加认证头
 */
export const authRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    // 可以在这里添加认证token
    // const token = localStorage.getItem('token');
    // if (token && options.headers) {
    //   options.headers.set('Authorization', `Bearer ${token}`);
    // }
    return options;
  }
};

/**
 * 请求拦截器：添加加载状态
 */
export const loadingRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    // 可以在这里添加全局加载状态
    // showLoading();
    return options;
  }
};

/**
 * 响应拦截器：统一错误处理
 */
export const errorResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    // 检查响应码
    if (data.code !== 200) {
      const message = ERROR_CODE_MAP[data.code] || data.msg || '请求失败';
      ElMessage.error(message);
      throw new Error(message);
    }
    return data;
  }
};

/**
 * 响应拦截器：处理空数据
 */
export const emptyDataResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    // 处理空数据情况
    if (data.data === null || data.data === undefined) {
      data.data = [] as any;
    }
    return data;
  }
};

/**
 * 请求拦截器：日志记录
 */
export const logRequestInterceptor: RequestInterceptor = {
  async onRequest(options) {
    // 可以在这里添加请求日志
    // console.log('Request:', options);
    return options;
  }
};

/**
 * 响应拦截器：日志记录
 */
export const logResponseInterceptor: ResponseInterceptor = {
  async onsuccess(data) {
    // 可以在这里添加响应日志
    // console.log('Response:', data);
    return data;
  }
};

/**
 * 默认拦截器配置
 */
export const defaultInterceptors = {
  request: [
    authRequestInterceptor,
    loadingRequestInterceptor,
    logRequestInterceptor
  ],
  response: [
    errorResponseInterceptor,
    emptyDataResponseInterceptor,
    logResponseInterceptor
  ]
};

/**
 * 注册默认拦截器
 */
export function registerDefaultInterceptors() {
  const { request: requestInterceptors, response: responseInterceptors } = defaultInterceptors;

  requestInterceptors.forEach(interceptor => {
    interceptorManager.addRequestInterceptor(interceptor);
  });

  responseInterceptors.forEach(interceptor => {
    interceptorManager.addResponseInterceptor(interceptor);
  });
}