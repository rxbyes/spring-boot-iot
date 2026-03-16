import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

export interface User {
  id?: IdType;
  tenantId?: IdType;
  username: string;
  realName: string;
  phone?: string;
  email?: string;
  avatar?: string;
  password?: string;
  status: number;
  lastLoginTime?: string;
  lastLoginIp?: string;
  createTime?: string;
  updateTime?: string;
  roleIds?: number[];
  roleNames?: string[];
}

export const listUsers = (params: {
  username?: string;
  phone?: string;
  email?: string;
  status?: number;
}): Promise<ApiEnvelope<User[]>> => {
  const queryString = new URLSearchParams();
  if (params.username) queryString.append('username', params.username);
  if (params.phone) queryString.append('phone', params.phone);
  if (params.email) queryString.append('email', params.email);
  if (params.status !== undefined) queryString.append('status', String(params.status));
  const path = `/api/user/list${queryString.toString() ? `?${queryString.toString()}` : ''}`;
  return request<User[]>(path, { method: 'GET' });
};

export const getUser = (id: IdType): Promise<ApiEnvelope<User>> => {
  return request<User>(`/api/user/${id}`, { method: 'GET' });
};

export const addUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
  return request<User>('/api/user/add', { method: 'POST', body: data });
};

export const updateUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
  return request<User>('/api/user/update', { method: 'PUT', body: data });
};

export const deleteUser = (id: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/user/${id}`, { method: 'DELETE' });
};

export const getUserByUsername = (username: string): Promise<ApiEnvelope<User>> => {
  return request<User>(`/api/user/username/${username}`, { method: 'GET' });
};

export const changePassword = (data: { id: IdType; oldPassword: string; newPassword: string }): Promise<ApiEnvelope<void>> => {
  return request<void>('/api/user/change-password', { method: 'POST', body: data });
};

export const resetPassword = (userId: IdType): Promise<ApiEnvelope<void>> => {
  return request<void>(`/api/user/reset-password/${userId}`, { method: 'POST' });
};
