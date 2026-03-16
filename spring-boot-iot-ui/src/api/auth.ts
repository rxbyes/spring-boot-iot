import { request } from './request';
import type { ApiEnvelope } from '../types/api';

export interface LoginPayload {
  loginType?: 'account' | 'phone';
  username?: string;
  phone?: string;
  password: string;
}

export interface LoginResult {
  token: string;
  tokenType?: string;
  expiresIn?: number;
  tokenHeader?: string;
  userId?: number;
  username?: string;
  realName?: string;
}

export function login(payload: LoginPayload): Promise<ApiEnvelope<LoginResult>> {
  return request<LoginResult>('/api/auth/login', {
    method: 'POST',
    body: payload
  });
}
