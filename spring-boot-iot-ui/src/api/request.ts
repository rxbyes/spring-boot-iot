import { runtimeState } from '../stores/runtime';
import type { ApiEnvelope, IdType } from '../types/api';

export interface RequestOptions extends Omit<RequestInit, 'body' | 'headers'> {
  body?: BodyInit | Record<string, unknown> | unknown | null;
  headers?: HeadersInit;
  errorHandler?: (error: Error) => void;
}

export interface RequestInterceptor {
  onRequest?: (options: RequestOptions) => RequestOptions | Promise<RequestOptions>;
  onerror?: (error: Error) => Error | Promise<Error>;
}

export interface ResponseInterceptor<T = unknown> {
  onsuccess?: (data: ApiEnvelope<T>) => ApiEnvelope<T> | Promise<ApiEnvelope<T>>;
  onerror?: (error: Error) => Error | Promise<Error>;
}

class InterceptorManager {
  private requestInterceptors: RequestInterceptor[] = [];
  private responseInterceptors: ResponseInterceptor[] = [];

  addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.requestInterceptors.push(interceptor);
  }

  addResponseInterceptor(interceptor: ResponseInterceptor): void {
    this.responseInterceptors.push(interceptor);
  }

  async applyRequestInterceptors(options: RequestOptions): Promise<RequestOptions> {
    let result = options;
    for (const interceptor of this.requestInterceptors) {
      if (interceptor.onRequest) {
        result = await interceptor.onRequest(result);
      }
    }
    return result;
  }

  async applyResponseInterceptors<T>(payload: ApiEnvelope<T>): Promise<ApiEnvelope<T>> {
    let result = payload;
    for (const interceptor of this.responseInterceptors) {
      if (interceptor.onsuccess) {
        result = await interceptor.onsuccess(result);
      }
    }
    return result;
  }

  async applyRequestErrorInterceptors(error: Error): Promise<void> {
    for (const interceptor of this.requestInterceptors) {
      if (interceptor.onerror) {
        await interceptor.onerror(error);
      }
    }
  }

  async applyResponseErrorInterceptors(error: Error): Promise<void> {
    for (const interceptor of this.responseInterceptors) {
      if (interceptor.onerror) {
        await interceptor.onerror(error);
      }
    }
  }
}

export const interceptorManager = new InterceptorManager();

function normalizeBody(body: RequestOptions['body'], headers: Headers): BodyInit | undefined {
  if (body === null || body === undefined) {
    return undefined;
  }
  if (body instanceof FormData || body instanceof URLSearchParams || typeof body === 'string') {
    return body;
  }

  headers.set('Content-Type', 'application/json');
  return JSON.stringify(body);
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
  const { body, headers, errorHandler, ...rest } = options;
  const requestHeaders = new Headers(headers);

  const url = `${runtimeState.apiBaseUrl}${path}`;
  const finalOptions = await interceptorManager.applyRequestInterceptors({
    ...rest,
    headers: requestHeaders,
    body: normalizeBody(body, requestHeaders)
  });

  try {
    const response = await fetch(url, finalOptions as RequestInit);
    const rawText = await response.text();
    const bodyText = rawText.trim();
    let payload: ApiEnvelope<T> | null = null;

    if (bodyText) {
      try {
        payload = JSON.parse(bodyText) as ApiEnvelope<T>;
      } catch {
        payload = null;
      }
    }

    if (!response.ok) {
      const statusMessage = response.statusText ? `${response.status} ${response.statusText}` : String(response.status);
      const message = payload?.msg || bodyText || `请求失败: ${statusMessage}`;
      throw new Error(message);
    }

    if (!payload) {
      throw new Error('服务端返回格式无效，请检查后端日志');
    }

    const processedPayload = await interceptorManager.applyResponseInterceptors(payload);
    return processedPayload;
  } catch (error) {
    if (error instanceof Error) {
      if (errorHandler) {
        errorHandler(error);
      }
      await interceptorManager.applyRequestErrorInterceptors(error);
      await interceptorManager.applyResponseErrorInterceptors(error);
    }
    throw error;
  }
}
