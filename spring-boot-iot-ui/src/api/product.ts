import { request } from './request';
import type {
  Product,
  ProductAddPayload
} from '../types/api';

/**
 * 产品相关API
 */
export const productApi = {
  /**
   * 新增产品
   */
  addProduct(payload: ProductAddPayload) {
    return request<Product>('/device/product/add', {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 根据ID查询产品
   */
  getProductById(id: string | number) {
    return request<Product>(`/device/product/${id}`);
  },

  /**
   * 查询所有产品
   */
  getAllProducts() {
    return request<Product[]>('/device/product/list');
  },

  /**
   * 更新产品
   */
  updateProduct(id: string | number, payload: Partial<ProductAddPayload>) {
    return request<Product>(`/device/product/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  /**
   * 删除产品
   */
  deleteProduct(id: string | number) {
    return request<null>(`/device/product/${id}`, {
      method: 'DELETE'
    });
  }
};