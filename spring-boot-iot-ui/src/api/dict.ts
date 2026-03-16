import { request } from './request';
import type { ApiEnvelope, IdType } from '../types/api';

/**
 * 字典配置 API
 */

// 字典项接口定义
export interface DictItem {
      id: IdType;
      tenantId: IdType;
      dictId: IdType;
      itemName: string;
      itemValue: string;
      itemType: string; // string/number/boolean
      status: number; // 1启用 0禁用
      sortNo: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
}

// 字典配置接口定义
export interface Dict {
      id: IdType;
      tenantId: IdType;
      dictName: string;
      dictCode: string;
      dictType: string; // text/number/boolean/date
      status: number; // 1启用 0禁用
      sortNo: number;
      remark: string;
      createBy: number;
      createTime: string;
      updateBy: number;
      updateTime: string;
      deleted: number;
      items?: DictItem[];
}

// 查询字典列表
export const listDicts = (): Promise<ApiEnvelope<Dict[]>> => {
      return request<Dict[]>('/api/dict/list', { method: 'GET' });
};

// 查询字典树
export const listDictTree = (): Promise<ApiEnvelope<Dict[]>> => {
      return request<Dict[]>('/api/dict/tree', { method: 'GET' });
};

// 根据ID查询字典
export const getDict = (id: IdType): Promise<ApiEnvelope<Dict>> => {
      return request<Dict>(`/api/dict/${id}`, { method: 'GET' });
};

// 根据编码查询字典
export const getDictByCode = (dictCode: string): Promise<ApiEnvelope<Dict>> => {
      return request<Dict>(`/api/dict/code/${dictCode}`, { method: 'GET' });
};

// 添加字典
export const addDict = (data: Partial<Dict>): Promise<ApiEnvelope<Dict>> => {
      return request<Dict>('/api/dict', { method: 'POST', body: data });
};

// 更新字典
export const updateDict = (data: Partial<Dict>): Promise<ApiEnvelope<Dict>> => {
      return request<Dict>('/api/dict', { method: 'PUT', body: data });
};

// 删除字典
export const deleteDict = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/dict/${id}`, { method: 'DELETE' });
};

// 查询字典项列表
export const listDictItems = (dictId: IdType): Promise<ApiEnvelope<DictItem[]>> => {
      return request<DictItem[]>(`/api/dict/${dictId}/items`, { method: 'GET' });
};

// 添加字典项
export const addDictItem = (data: Partial<DictItem>): Promise<ApiEnvelope<DictItem>> => {
      return request<DictItem>(`/api/dict/${data.dictId}/items`, { method: 'POST', body: data });
};

// 更新字典项
export const updateDictItem = (data: Partial<DictItem>): Promise<ApiEnvelope<DictItem>> => {
      return request<DictItem>(`/api/dict/${data.dictId}/items`, { method: 'PUT', body: data });
};

// 删除字典项
export const deleteDictItem = (id: IdType): Promise<ApiEnvelope<void>> => {
      return request<void>(`/api/dict/items/${id}`, { method: 'DELETE' });
};
