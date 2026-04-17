import { request } from './request'
import { buildQueryString } from './query'
import type {
  ApiEnvelope,
  GovernanceSubmissionResult,
  IdType,
  PageResult,
  ProtocolDecryptPreview,
  ProtocolDecryptProfile,
  ProtocolFamilyDefinition,
  ProtocolGovernancePageQuery
} from '@/types/api'

export function pageProtocolFamilies(
  params: ProtocolGovernancePageQuery = {}
): Promise<ApiEnvelope<PageResult<ProtocolFamilyDefinition>>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/governance/protocol/families?${queryString}`
    : '/api/governance/protocol/families'
  return request<PageResult<ProtocolFamilyDefinition>>(path, {
    method: 'GET'
  })
}

export function pageProtocolDecryptProfiles(
  params: ProtocolGovernancePageQuery = {}
): Promise<ApiEnvelope<PageResult<ProtocolDecryptProfile>>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/governance/protocol/decrypt-profiles?${queryString}`
    : '/api/governance/protocol/decrypt-profiles'
  return request<PageResult<ProtocolDecryptProfile>>(path, {
    method: 'GET'
  })
}

export function submitProtocolFamilyPublish(
  familyId: IdType,
  submitReason: string
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(`/api/governance/protocol/families/${familyId}/submit-publish`, {
    method: 'POST',
    body: { submitReason }
  })
}

export function submitProtocolFamilyRollback(
  familyId: IdType,
  submitReason: string
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(`/api/governance/protocol/families/${familyId}/submit-rollback`, {
    method: 'POST',
    body: { submitReason }
  })
}

export function submitProtocolDecryptProfilePublish(
  profileId: IdType,
  submitReason: string
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(
    `/api/governance/protocol/decrypt-profiles/${profileId}/submit-publish`,
    {
      method: 'POST',
      body: { submitReason }
    }
  )
}

export function submitProtocolDecryptProfileRollback(
  profileId: IdType,
  submitReason: string
): Promise<ApiEnvelope<GovernanceSubmissionResult>> {
  return request<GovernanceSubmissionResult>(
    `/api/governance/protocol/decrypt-profiles/${profileId}/submit-rollback`,
    {
      method: 'POST',
      body: { submitReason }
    }
  )
}

export function previewProtocolDecrypt(payload: {
  familyCode?: string
  appId?: string
  encryptedPayload?: string
}): Promise<ApiEnvelope<ProtocolDecryptPreview>> {
  return request<ProtocolDecryptPreview>('/api/governance/protocol/decrypt-profiles/preview', {
    method: 'POST',
    body: payload
  })
}
