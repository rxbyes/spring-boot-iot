import { request } from './request'
import type { RequestOptions } from './request'
import type {
  ApiEnvelope,
  IdType,
  VendorMetricMappingRule,
  VendorMetricMappingRuleCreatePayload,
  VendorMetricMappingRuleSuggestion,
  VendorMetricMappingRuleSuggestionQuery
} from '@/types/api'

type VendorMetricMappingRuleRequestOptions = Pick<RequestOptions, 'signal' | 'suppressErrorToast'>

function buildQuery(params: VendorMetricMappingRuleSuggestionQuery) {
  const query = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.append(key, String(value))
    }
  })

  return query.toString()
}

export function listVendorMetricMappingRuleSuggestions(
  productId: IdType,
  query: VendorMetricMappingRuleSuggestionQuery = {},
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRuleSuggestion[]>> {
  const queryString = buildQuery(query)
  return request<VendorMetricMappingRuleSuggestion[]>(
    `/api/device/product/${productId}/vendor-mapping-rule-suggestions${queryString ? `?${queryString}` : ''}`,
    {
      method: 'GET',
      ...options
    }
  )
}

export function createVendorMetricMappingRule(
  productId: IdType,
  payload: VendorMetricMappingRuleCreatePayload,
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRule>> {
  return request<VendorMetricMappingRule>(`/api/device/product/${productId}/vendor-mapping-rules`, {
    method: 'POST',
    body: payload,
    ...options
  })
}
