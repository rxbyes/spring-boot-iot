import { runtimeState } from '../stores/runtime';
import type { ApiEnvelope } from '../types/api';

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

export interface RequestError extends Error {
  handled?: boolean;
  status?: number;
  rawMessage?: string;
}

export const SYSTEM_BUSY_MESSAGE = '系统繁忙，请稍后重试！';

const UNSAFE_ID_JSON_FIELD_PATTERN =
  /(^|[{\[,])(\s*)"([A-Za-z_][A-Za-z0-9_]*(?:Id|ID|_id)|id)"\s*:\s*(-?\d{16,})(?=\s*[,}\]])/gm;

export function createRequestError(message: string, handled = false, status?: number, rawMessage?: string): RequestError {
  const error = new Error(message) as RequestError;
  error.handled = handled;
  error.status = status;
  if (rawMessage && rawMessage !== message) {
    error.rawMessage = rawMessage;
  }
  return error;
}

export function isHandledRequestError(error: unknown): error is RequestError {
  return Boolean(error && typeof error === 'object' && (error as RequestError).handled);
}

export function resolveRequestErrorMessage(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error) {
    const requestError = error as RequestError;
    if (requestError.status === 500) {
      return SYSTEM_BUSY_MESSAGE;
    }
    const message = error.message?.trim();
    if (message) {
      return message;
    }
  }
  return fallbackMessage;
}

export function normalizeUnsafeIdJson(bodyText: string): string {
  // 真实环境历史响应仍可能把雪花 Long 主键直接返回为 number，这里在 JSON.parse 前兜底转成字符串。
  return bodyText.replace(
    UNSAFE_ID_JSON_FIELD_PATTERN,
    (_match, prefix: string, whitespace: string, fieldName: string, value: string) => {
      return `${prefix}${whitespace}"${fieldName}":"${value}"`;
    }
  );
}

export function parseApiEnvelope<T>(bodyText: string): ApiEnvelope<T> | null {
  if (!bodyText) {
    return null;
  }

  try {
    return JSON.parse(normalizeUnsafeIdJson(bodyText)) as ApiEnvelope<T>;
  } catch {
    return null;
  }
}

function resolveHttpErrorMessage(status: number, bodyText: string, fallbackMessage: string): string {
  if (status === 500) {
    return SYSTEM_BUSY_MESSAGE;
  }
  if (status > 500) {
    return bodyText || SYSTEM_BUSY_MESSAGE;
  }
  return bodyText || fallbackMessage;
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
    const payload = parseApiEnvelope<T>(bodyText);

    if (payload) {
      const processedPayload = await interceptorManager.applyResponseInterceptors(payload);
      if (!response.ok) {
        const statusMessage = response.statusText ? `${response.status} ${response.statusText}` : String(response.status);
        const message = processedPayload.msg || resolveHttpErrorMessage(response.status, bodyText, `请求失败: ${statusMessage}`);
        throw createRequestError(message, false, response.status);
      }
      return processedPayload;
    }

    if (!response.ok) {
      const statusMessage = response.statusText ? `${response.status} ${response.statusText}` : String(response.status);
      const message = resolveHttpErrorMessage(response.status, bodyText, `请求失败: ${statusMessage}`);
      throw createRequestError(message, false, response.status);
    }

    if (!payload) {
      throw createRequestError('服务端返回格式无效，请检查后端日志');
    }
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
