import { request } from './request';
import type { RequestOptions } from './request';
import type {
  ApiEnvelope,
  IdType,
  PageResult,
  Product,
  ProductAddPayload,
  ProductModelGovernanceApplyPayload,
  ProductModelGovernanceApplyResult,
  ProductModelGovernanceComparePayload,
  ProductModelGovernanceCompareResult,
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

export interface ProductContractReleaseBatch {
  id?: IdType | null
  productId?: IdType | null
  scenarioCode?: string | null
  releaseSource?: string | null
  releasedFieldCount?: number | null
  createBy?: IdType | null
  createTime?: string | null
  approvalOrderId?: IdType | null
  releaseReason?: string | null
  releaseStatus?: string | null
}

export interface ProductContractReleaseRollbackResult {
  targetBatchId?: IdType | null
  rolledBackBatchId?: IdType | null
  productId?: IdType | null
  scenarioCode?: string | null
  releaseSource?: string | null
  releasedFieldCount?: number | null
  restoredFieldCount?: number | null
  rollbackMode?: string | null
  rollbackLimitations?: string | null
  rollbackTime?: string | null
  approvalOrderId?: IdType | null
  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | null
  executionPending?: boolean | null
}

export interface ProductContractReleaseImpactItem {
  changeType?: string | null
  modelType?: string | null
  identifier?: string | null
  changedFields?: string[] | null
}

export interface ProductContractReleaseDependencySummary {
  affectedRiskMetricCount?: number | null
  affectedRiskPointBindingCount?: number | null
  affectedRuleCount?: number | null
  affectedLinkageBindingCount?: number | null
  affectedEmergencyPlanBindingCount?: number | null
  affectedRiskMetrics?: ProductContractReleaseRiskMetricDetail[] | null
  affectedRiskPointBindings?: ProductContractReleaseRiskPointBindingDetail[] | null
  affectedRules?: ProductContractReleaseRuleDetail[] | null
  affectedLinkageBindings?: ProductContractReleaseLinkageBindingDetail[] | null
  affectedEmergencyPlanBindings?: ProductContractReleaseEmergencyPlanBindingDetail[] | null
}

export interface ProductContractReleaseRiskMetricDetail {
  riskMetricId?: IdType | null
  contractIdentifier?: string | null
  normativeIdentifier?: string | null
  riskMetricCode?: string | null
  riskMetricName?: string | null
  metricRole?: string | null
  lifecycleStatus?: string | null
}

export interface ProductContractReleaseRiskPointBindingDetail {
  bindingId?: IdType | null
  riskPointId?: IdType | null
  riskPointName?: string | null
  deviceId?: IdType | null
  deviceCode?: string | null
  deviceName?: string | null
  riskMetricId?: IdType | null
  metricIdentifier?: string | null
  metricName?: string | null
}

export interface ProductContractReleaseRuleDetail {
  ruleId?: IdType | null
  ruleName?: string | null
  riskMetricId?: IdType | null
  metricIdentifier?: string | null
  metricName?: string | null
  alarmLevel?: string | null
}

export interface ProductContractReleaseLinkageBindingDetail {
  bindingId?: IdType | null
  linkageRuleId?: IdType | null
  linkageRuleName?: string | null
  riskMetricId?: IdType | null
  bindingStatus?: string | null
}

export interface ProductContractReleaseEmergencyPlanBindingDetail {
  bindingId?: IdType | null
  emergencyPlanId?: IdType | null
  emergencyPlanName?: string | null
  riskMetricId?: IdType | null
  bindingStatus?: string | null
  alarmLevel?: string | null
}

export interface ProductContractReleaseImpact {
  batchId?: IdType | null
  productId?: IdType | null
  scenarioCode?: string | null
  releaseSource?: string | null
  releasedFieldCount?: number | null
  totalBeforeCount?: number | null
  totalAfterCount?: number | null
  addedCount?: number | null
  removedCount?: number | null
  changedCount?: number | null
  unchangedCount?: number | null
  comparedAt?: string | null
  impactItems?: ProductContractReleaseImpactItem[] | null
  dependencySummary?: ProductContractReleaseDependencySummary | null
}

type ProductRequestOptions = Pick<RequestOptions, 'signal' | 'suppressErrorToast'>

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
   * 构建产品物模型双证据对比结果
   */
  compareProductModelGovernance(
    productId: IdType,
    payload: ProductModelGovernanceComparePayload
  ): Promise<ApiEnvelope<ProductModelGovernanceCompareResult>> {
    return request<ProductModelGovernanceCompareResult>(`/api/device/product/${productId}/model-governance/compare`, {
      method: 'POST',
      body: payload
    });
  },

  /**
   * 应用产品物模型双证据治理决策
   */
  applyProductModelGovernance(
    productId: IdType,
    payload: ProductModelGovernanceApplyPayload,
    options: ProductRequestOptions = {}
  ): Promise<ApiEnvelope<ProductModelGovernanceApplyResult>> {
    return request<ProductModelGovernanceApplyResult>(`/api/device/product/${productId}/model-governance/apply`, {
      method: 'POST',
      body: payload,
      ...options
    });
  },

  pageProductContractReleaseBatches(
    productId: IdType,
    params: { pageNum?: number; pageSize?: number } = {}
  ): Promise<ApiEnvelope<PageResult<ProductContractReleaseBatch>>> {
    const query = buildQuery(params)
    return request<PageResult<ProductContractReleaseBatch>>(
      `/api/device/product/${productId}/contract-release-batches${query ? `?${query}` : ''}`,
      { method: 'GET' }
    )
  },

  getProductContractReleaseBatch(batchId: IdType): Promise<ApiEnvelope<ProductContractReleaseBatch>> {
    return request<ProductContractReleaseBatch>(`/api/device/product/contract-release-batches/${batchId}`, { method: 'GET' })
  },

  getProductContractReleaseBatchImpact(
    batchId: IdType,
    options: ProductRequestOptions = {}
  ): Promise<ApiEnvelope<ProductContractReleaseImpact>> {
    return request<ProductContractReleaseImpact>(`/api/device/product/contract-release-batches/${batchId}/impact`, {
      method: 'GET',
      ...options
    })
  },

  rollbackProductContractReleaseBatch(batchId: IdType): Promise<ApiEnvelope<ProductContractReleaseRollbackResult>> {
    return request<ProductContractReleaseRollbackResult>(
      `/api/device/product/contract-release-batches/${batchId}/rollback`,
      {
        method: 'POST'
      }
    )
  },

  resubmitProductGovernanceApproval(orderId: IdType): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/device/product/governance-approval/${orderId}/resubmit`, {
      method: 'POST'
    })
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
