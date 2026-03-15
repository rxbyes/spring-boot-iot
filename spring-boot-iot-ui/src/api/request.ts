import { runtimeState } from '../stores/runtime';
import type { ApiEnvelope } from '../types/api';

/**
 * 请求配置选项
 */
export interface RequestOptions extends Omit<RequestInit, 'body'> {
  /** 请求体，支持多种类型 */
  body?: BodyInit | Record<string, unknown> | null;
  /** 是否需要响应数据 */
  needData?: boolean;
  /** 自定义错误处理 */
  errorHandler?: (error: Error) => void;
}

/**
 * 请求拦截器回调类型
 */
export interface RequestInterceptor {
  /** 请求前拦截 */
  onRequest?: (options: RequestOptions) => RequestOptions | Promise<RequestOptions>;
  /** 请求成功拦截 */
  onsuccess?: (response: Response) => Response | Promise<Response>;
  /** 请求失败拦截 */
  onerror?: (error: Error) => Error | Promise<Error>;
}

/**
 * 响应拦截器回调类型
 */
export interface ResponseInterceptor<T = unknown> {
  /** 响应成功拦截 */
  onsuccess?: (data: ApiEnvelope<T>) => ApiEnvelope<T> | Promise<ApiEnvelope<T>>;
  /** 响应失败拦截 */
  onerror?: (error: Error) => Error | Promise<Error>;
}

/**
 * 拦截器管理器
 */
class InterceptorManager {
  private requestInterceptors: RequestInterceptor[] = [];
  private responseInterceptors: ResponseInterceptor[] = [];

  /** 添加请求拦截器 */
  public addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.requestInterceptors.push(interceptor);
  }

  /** 添加响应拦截器 */
  public addResponseInterceptor(interceptor: ResponseInterceptor): void {
    this.responseInterceptors.push(interceptor);
  }

  /** 应用请求拦截器 */
  public async applyRequestInterceptors(options: RequestOptions): Promise<RequestOptions> {
    let result = options;
    for (const interceptor of this.requestInterceptors) {
      if (interceptor.onRequest) {
        result = await interceptor.onRequest(result);
      }
    }
    return result;
  }

  /** 应用响应拦截器 */
  public async applyResponseInterceptors<T>(data: ApiEnvelope<T>): Promise<ApiEnvelope<T>> {
    let result = data;
    for (const interceptor of this.responseInterceptors) {
      if (interceptor.onsuccess) {
        result = await interceptor.onsuccess(result);
      }
    }
    return result;
  }
}

/**
 * 全局拦截器管理器
 */
export const interceptorManager = new InterceptorManager();

/**
 * 统一请求处理函数
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
  const { body, headers, needData = true, errorHandler, ...rest } = options;
  const requestHeaders = new Headers(headers);
  let resolvedBody: BodyInit | undefined;

  // 处理请求体
  if (body && !(body instanceof FormData) && typeof body !== 'string' && !(body instanceof URLSearchParams)) {
    requestHeaders.set('Content-Type', 'application/json');
    resolvedBody = JSON.stringify(body);
  } else if (typeof body === 'string' || body instanceof FormData || body instanceof URLSearchParams) {
    resolvedBody = body;
  }

  // 构建URL
  const url = `${runtimeState.apiBaseUrl}${path}`;

  try {
    // 应用请求拦截器
    const processedOptions = await interceptorManager.applyRequestInterceptors({
      ...rest,
      headers: requestHeaders,
      body: resolvedBody
    });

    // 发起请求
    const response = await fetch(url, processedOptions);

    // 应用响应拦截器
    const processedResponse = await interceptorManager.applyResponseInterceptors(response as any);

    // 处理响应
    const rawText = await processedResponse.text();
    const payload = rawText ? (JSON.parse(rawText) as ApiEnvelope<T>) : null;

    // 错误处理
    if (!processedResponse.ok) {
      const error = new Error(payload?.msg || `请求失败: ${processedResponse.status}`);
      if (errorHandler) {
        errorHandler(error);
      }
      throw error;
    }

    if (!payload) {
      const error = new Error('服务端没有返回有效内容');
      if (errorHandler) {
        errorHandler(error);
      }
      throw error;
    }

    if (payload.code !== 200) {
      const error = new Error(payload.msg || '接口调用失败');
      if (errorHandler) {
        errorHandler(error);
      }
      throw error;
    }

    return payload;
  } catch (error) {
    // 应用错误拦截器
    if (error instanceof Error) {
      for (const interceptor of interceptorManager.requestInterceptors) {
        if (interceptor.onerror) {
          await interceptor.onerror(error);
        }
      }
    }
    throw error;
  }
}