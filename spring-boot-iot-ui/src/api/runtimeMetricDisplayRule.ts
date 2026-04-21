import { buildQueryString } from './query'
import { request } from './request'
import type { RequestOptions } from './request'
import type {
  ApiEnvelope,
  IdType,
  PageResult,
  RuntimeMetricDisplayRule,
  RuntimeMetricDisplayRuleUpsertPayload
} from '@/types/api'

type RuntimeMetricDisplayRuleRequestOptions = Pick<RequestOptions, 'signal' | 'suppressErrorToast'>

export function listRuntimeMetricDisplayRules(
  productId: IdType,
  query: {
    status?: string
    pageNum?: number
    pageSize?: number
  } = {},
  options: RuntimeMetricDisplayRuleRequestOptions = {}
): Promise<ApiEnvelope<PageResult<RuntimeMetricDisplayRule>>> {
  const queryString = buildQueryString(query)
  return request<PageResult<RuntimeMetricDisplayRule>>(
    `/api/device/product/${productId}/runtime-display-rules${queryString ? `?${queryString}` : ''}`,
    {
      method: 'GET',
      ...options
    }
  )
}

export function createRuntimeMetricDisplayRule(
  productId: IdType,
  payload: RuntimeMetricDisplayRuleUpsertPayload,
  options: RuntimeMetricDisplayRuleRequestOptions = {}
): Promise<ApiEnvelope<RuntimeMetricDisplayRule>> {
  return request<RuntimeMetricDisplayRule>(`/api/device/product/${productId}/runtime-display-rules`, {
    method: 'POST',
    body: payload,
    ...options
  })
}

export function updateRuntimeMetricDisplayRule(
  productId: IdType,
  ruleId: IdType,
  payload: RuntimeMetricDisplayRuleUpsertPayload,
  options: RuntimeMetricDisplayRuleRequestOptions = {}
): Promise<ApiEnvelope<RuntimeMetricDisplayRule>> {
  return request<RuntimeMetricDisplayRule>(
    `/api/device/product/${productId}/runtime-display-rules/${ruleId}`,
    {
      method: 'PUT',
      body: payload,
      ...options
    }
  )
}
