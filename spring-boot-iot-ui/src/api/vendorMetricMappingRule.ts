import { request } from './request'
import type { RequestOptions } from './request'
import { buildQueryString } from './query'
import type {
  ApiEnvelope,
  GovernanceSubmissionResult,
  IdType,
  PageResult,
  VendorMetricMappingRule,
  VendorMetricMappingRuleCreatePayload,
  VendorMetricMappingRuleHitPreview,
  VendorMetricMappingRuleLedgerRow,
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

export async function listVendorMetricMappingRuleLedger(
  productId: IdType,
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRuleLedgerRow[]>> {
  const queryString = buildQueryString({
    pageNum: 1,
    pageSize: 100
  })
  const response = await request<PageResult<VendorMetricMappingRule>>(
    `/api/device/product/${productId}/vendor-mapping-rules${queryString ? `?${queryString}` : ''}`,
    {
      method: 'GET',
      ...options
    }
  )
  return {
    ...response,
    data: (response.data?.records ?? []).map((row) => ({
      ruleId: row.id,
      productId: row.productId,
      rawIdentifier: row.rawIdentifier,
      logicalChannelCode: row.logicalChannelCode,
      targetNormativeIdentifier: row.targetNormativeIdentifier,
      scopeType: row.scopeType,
      draftStatus: row.status,
      draftVersionNo: row.versionNo,
      publishedStatus: row.publishedStatus,
      publishedVersionNo: row.publishedVersionNo,
      latestApprovalOrderId: row.approvalOrderId,
      publishedSource: row.publishedStatus ? 'published_snapshot' : 'draft_table'
    }))
  }
}

export function submitVendorMetricMappingRulePublish(
  productId: IdType,
  ruleId: IdType,
  submitReason: string,
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(
    `/api/device/product/${productId}/vendor-mapping-rules/${ruleId}/submit-publish`,
    {
      method: 'POST',
      body: { submitReason },
      ...options
    }
  )
}

export function submitVendorMetricMappingRuleRollback(
  productId: IdType,
  ruleId: IdType,
  submitReason: string,
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(
    `/api/device/product/${productId}/vendor-mapping-rules/${ruleId}/submit-rollback`,
    {
      method: 'POST',
      body: { submitReason },
      ...options
    }
  )
}

export function previewVendorMetricMappingRuleHit(
  productId: IdType,
  payload: {
    rawIdentifier: string
    logicalChannelCode?: string
  },
  options: VendorMetricMappingRuleRequestOptions = {}
): Promise<ApiEnvelope<VendorMetricMappingRuleHitPreview>> {
  return request<VendorMetricMappingRuleHitPreview>(
    `/api/device/product/${productId}/vendor-mapping-rules/preview-hit`,
    {
      method: 'POST',
      body: payload,
      ...options
    }
  )
}
