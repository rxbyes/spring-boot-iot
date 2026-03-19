import { request as coreRequest } from './request';
import type { RequestOptions } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

const API_PREFIX_CANDIDATES = [
  '/device',
  '/message',
  '/auth',
  '/menu',
  '/user',
  '/role',
  '/organization',
  '/region',
  '/dict',
  '/alarm',
  '/event',
  '/risk-point',
  '/risk-monitoring',
  '/report',
  '/system'
];

function normalizeLegacyApiPath(path: string): string {
  if (!path) {
    return path;
  }

  if (path.startsWith('/api/') || path === '/api') {
    return path;
  }

  if (API_PREFIX_CANDIDATES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`))) {
    // 兼容历史调用（/device/**、/message/**），统一归一到当前鉴权基线（/api/**）
    return `/api${path}`;
  }

  return path;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<ApiEnvelope<T>> {
  return coreRequest<T>(normalizeLegacyApiPath(path), options);
}
