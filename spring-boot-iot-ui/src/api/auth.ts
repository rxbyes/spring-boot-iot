import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';
import type { LoginResult, UserAuthContext } from '../types/auth';

export interface LoginPayload {
  loginType?: 'account' | 'phone';
  username?: string;
  phone?: string;
  password: string;
}

export function login(payload: LoginPayload): Promise<ApiEnvelope<LoginResult>> {
  return request<LoginResult>('/api/auth/login', {
    method: 'POST',
    body: payload
  });
}

export function getCurrentUser(): Promise<ApiEnvelope<UserAuthContext>> {
  return request<UserAuthContext>('/api/auth/me', {
    method: 'GET'
  });
}
