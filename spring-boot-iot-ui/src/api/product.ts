import { request } from './request';
import type { RequestOptions } from './request';
import type {
  ApiEnvelope,
  IdType,
  PageResult,
  Product,
  ProductAddPayload,
  ProductModelCandidateConfirmPayload,
  ProductModelManualExtractPayload,
  ProductModelCandidateResult,
  ProductModelCandidateSummary,
  ProductModel,
  ProductModelUpsertPayload
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

type ProductRequestOptions = Pick<RequestOptions, 'signal'>

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
  pageProducts(
    params: ProductPageQueryParams = {},
    options: ProductRequestOptions = {}
  ): Promise<ApiEnvelope<PageResult<Product>>> {
    const query = buildQuery(params)
    return request<PageResult<Product>>(`/api/device/product/page${query ? `?${query}` : ''}`, {
      method: 'GET',
      ...options
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
  getProductById(id: IdType, options: ProductRequestOptions = {}) {
    return request<Product>(`/api/device/product/${id}`, options);
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
  },

  /**
   * 查询指定产品的物模型列表
   */
  listProductModels(productId: IdType) {
    return request<ProductModel[]>(`/api/device/product/${productId}/models`);
  },

  /**
   * 提炼指定产品的物模型候选
   */
  listProductModelCandidates(productId: IdType): Promise<ApiEnvelope<ProductModelCandidateResult>> {
    return request<ProductModelCandidateResult>(`/api/device/product/${productId}/model-candidates`);
  },

  /**
   * 基于手动输入的单设备样本提炼物模型候选
   */
  manualExtractProductModelCandidates(
    productId: IdType,
    payload: ProductModelManualExtractPayload
  ): Promise<ApiEnvelope<ProductModelCandidateResult>> {
    return request<ProductModelCandidateResult>(`/api/device/product/${productId}/model-candidates/manual-extract`, {
      method: 'POST',
      body: payload
    })
  },

  /**
   * 确认候选并写入正式物模型
   */
  confirmProductModelCandidates(
    productId: IdType,
    payload: ProductModelCandidateConfirmPayload
  ): Promise<ApiEnvelope<ProductModelCandidateSummary>> {
    return request<ProductModelCandidateSummary>(`/api/device/product/${productId}/model-candidates/confirm`, {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 新增产品物模型
   */
  addProductModel(productId: IdType, payload: ProductModelUpsertPayload): Promise<ApiEnvelope<ProductModel>> {
    return request<ProductModel>(`/api/device/product/${productId}/models`, {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 更新产品物模型
   */
  updateProductModel(productId: IdType, modelId: IdType, payload: ProductModelUpsertPayload): Promise<ApiEnvelope<ProductModel>> {
    return request<ProductModel>(`/api/device/product/${productId}/models/${modelId}`, {
      method: 'PUT',
      body: payload
    });
  },

  /**
   * 删除产品物模型
   */
  deleteProductModel(productId: IdType, modelId: IdType) {
    return request<void>(`/api/device/product/${productId}/models/${modelId}`, {
      method: 'DELETE'
    });
  }
};
