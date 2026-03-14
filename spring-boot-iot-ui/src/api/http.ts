import { runtimeState } from '../stores/runtime';
import type { ApiEnvelope } from '../types/api';

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: BodyInit | Record<string, unknown> | null;
};

export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
  const { body, headers, ...rest } = options;
  const requestHeaders = new Headers(headers);
  let resolvedBody: BodyInit | undefined;

  if (body && !(body instanceof FormData) && typeof body !== 'string' && !(body instanceof URLSearchParams)) {
    requestHeaders.set('Content-Type', 'application/json');
    resolvedBody = JSON.stringify(body);
  } else if (typeof body === 'string' || body instanceof FormData || body instanceof URLSearchParams) {
    resolvedBody = body;
  }

  const url = `${runtimeState.apiBaseUrl}${path}`;
  const response = await fetch(url, {
    ...rest,
    headers: requestHeaders,
    body: resolvedBody
  });

  const rawText = await response.text();
  const payload = rawText ? (JSON.parse(rawText) as ApiEnvelope<T>) : null;

  if (!response.ok) {
    throw new Error(payload?.msg || `请求失败: ${response.status}`);
  }

  if (!payload) {
    throw new Error('服务端没有返回有效内容');
  }

  if (payload.code !== 200) {
    throw new Error(payload.msg || '接口调用失败');
  }

  return payload;
}
