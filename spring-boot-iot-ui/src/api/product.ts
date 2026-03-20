import { request } from './request';
import type {
  ApiEnvelope,
  IdType,
  PageResult,
  Product,
  ProductAddPayload
} from '../types/api';

export interface ProductPageQueryParams {
  productKey?: string
  productName?: string
  protocolCode?: string
  nodeType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

function buildQuery(params: Record<string, unknown>) {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })
  return query.toString()
}

/**
 * 产品相关API
 */
export const productApi = {
  /**
   * 分页查询产品台账
   */
  pageProducts(params: ProductPageQueryParams = {}): Promise<ApiEnvelope<PageResult<Product>>> {
    const query = buildQuery(params)
    return request<PageResult<Product>>(`/api/device/product/page${query ? `?${query}` : ''}`, {
      method: 'GET'
    })
  },

  /**
   * 新增产品
   */
  addProduct(payload: ProductAddPayload) {
    return request<Product>('/api/device/product/add', {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 根据ID查询产品
   */
  getProductById(id: IdType) {
    return request<Product>(`/api/device/product/${id}`);
  },

  /**
   * 查询所有产品
   */
  getAllProducts() {
    return request<Product[]>('/api/device/product/list');
  },

  /**
   * 更新产品
   */
  updateProduct(id: IdType, payload: ProductAddPayload) {
    return request<Product>(`/api/device/product/${id}`, {
      method: 'PUT',
      body: payload
    });
  },

  /**
   * 删除产品
   */
  deleteProduct(id: IdType) {
    return request<void>(`/api/device/product/${id}`, {
      method: 'DELETE'
    });
  }
};
