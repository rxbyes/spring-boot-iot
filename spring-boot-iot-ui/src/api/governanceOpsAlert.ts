import { buildQueryString } from './query'
import { request } from './request'
import type {
  ApiEnvelope,
  GovernanceOpsAlert,
  GovernanceOpsAlertPageQuery,
  GovernanceOpsAlertTransitionPayload,
  IdType,
  PageResult
} from '@/types/api'

export function pageGovernanceOpsAlerts(
  params: GovernanceOpsAlertPageQuery = {}
): Promise<ApiEnvelope<PageResult<GovernanceOpsAlert>>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/governance/ops-alerts?${queryString}`
    : '/api/governance/ops-alerts'
  return request<PageResult<GovernanceOpsAlert>>(path, {
    method: 'GET'
  })
}

export function ackGovernanceOpsAlert(
  id: IdType,
  payload?: GovernanceOpsAlertTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/ops-alerts/${id}/ack`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export function suppressGovernanceOpsAlert(
  id: IdType,
  payload?: GovernanceOpsAlertTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/ops-alerts/${id}/suppress`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export function closeGovernanceOpsAlert(
  id: IdType,
  payload?: GovernanceOpsAlertTransitionPayload
): Promise<ApiEnvelope<void>> {
  return request<void>(`/api/governance/ops-alerts/${id}/close`, {
    method: 'POST',
    body: payload ?? {}
  })
}

export const governanceOpsAlertApi = {
  pageAlerts: pageGovernanceOpsAlerts,
  ackAlert: ackGovernanceOpsAlert,
  suppressAlert: suppressGovernanceOpsAlert,
  closeAlert: closeGovernanceOpsAlert
}
