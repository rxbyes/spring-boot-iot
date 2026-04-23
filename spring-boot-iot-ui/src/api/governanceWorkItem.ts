import { buildQueryString } from './query'
import { request } from './request'
import type {
  ApiEnvelope,
  GovernanceDecisionContext,
  GovernanceWorkItem,
  GovernanceWorkItemPageQuery,
  GovernanceWorkItemTransitionPayload,
  IdType,
  PageResult
} from '@/types/api'

export type GovernanceWorkItemPageResult = PageResult<GovernanceWorkItem>

export function pageGovernanceWorkItems(
  params: GovernanceWorkItemPageQuery = {}
): Promise<ApiEnvelope<GovernanceWorkItemPageResult>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/governance/work-items?${queryString}`
    : '/api/governance/work-items'
  return request<GovernanceWorkItemPageResult>(path, {
    method: 'GET'
  })
}

export function ackGovernanceWorkItem(
  id: IdType,
  payload?: GovernanceWorkItemTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/work-items/${id}/ack`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export function blockGovernanceWorkItem(
  id: IdType,
  payload?: GovernanceWorkItemTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/work-items/${id}/block`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export function closeGovernanceWorkItem(
  id: IdType,
  payload?: GovernanceWorkItemTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/work-items/${id}/close`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export function getGovernanceWorkItemDecisionContext(
  id: IdType
): Promise<ApiEnvelope<GovernanceDecisionContext>> {
  return request<GovernanceDecisionContext>(`/api/governance/work-items/${id}/decision-context`, {
    method: 'GET'
  })
}

export const governanceWorkItemApi = {
  pageWorkItems: pageGovernanceWorkItems,
  getDecisionContext: getGovernanceWorkItemDecisionContext,
  ackWorkItem: ackGovernanceWorkItem,
  blockWorkItem: blockGovernanceWorkItem,
  closeWorkItem: closeGovernanceWorkItem
}
