import { request } from './request';
import type { ApiEnvelope } from '../types/api';

/**
 * 用户管理 API
 */

// 用户接口定义
export interface User {
      id: number;
      tenantId: number;
      username: string;
      realName: string;
      phone: string;
      email: string;
      avatar: string;
      status: number;
      lastLoginTime: string;
      lastLoginIp: string;
      createTime: string;
      updateTime: string;
}

// 查询用户列表
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

// 根据 ID 查询用户
export const getUser = (id: number): Promise<ApiEnvelope<User>> => {
      return request<User>(`/api/user/${id}`, { method: 'GET' });
};

// 添加用户
export const addUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
      return request<User>('/api/user', { method: 'POST', body: data });
};

// 更新用户
export const updateUser = (data: Partial<User>): Promise<ApiEnvelope<User>> => {
      return request<User>('/api/user', { method: 'PUT', body: data });
};

// 删除用户
export const deleteUser = (id: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/user/${id}`, { method: 'DELETE' });
};

// 根据用户名查询用户
export const getUserByUsername = (username: string): Promise<ApiEnvelope<User>> => {
      return request<User>(`/api/user/username/${username}`, { method: 'GET' });
};

// 修改密码
export const changePassword = (data: { id: number; oldPassword: string; newPassword: string }): Promise<ApiEnvelope<void>> => {
      return request<void>('/api/user/change-password', { method: 'POST', body: data });
};

// 重置密码
export const resetPassword = (userId: number): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/user/reset-password/${userId}`, { method: 'POST' });
};
