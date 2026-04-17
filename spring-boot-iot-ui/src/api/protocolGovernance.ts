import { request } from './request'
import { buildQueryString } from './query'
import type {
  ApiEnvelope,
  GovernanceSubmissionResult,
  IdType,
  PageResult,
  ProtocolGovernanceBatchSubmitPayload,
  ProtocolGovernanceBatchSubmitResult,
  ProtocolGovernanceReplay,
  ProtocolGovernanceReplayPayload,
  ProtocolDecryptPreview,
  ProtocolDecryptPreviewPayload,
  ProtocolDecryptProfile,
  ProtocolDecryptProfileUpsertPayload,
  ProtocolFamilyDefinition,
  ProtocolFamilyDefinitionUpsertPayload,
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

export function getProtocolFamilyDetail(
  familyId: IdType
): Promise<ApiEnvelope<ProtocolFamilyDefinition>> {
  return request<ProtocolFamilyDefinition>(`/api/governance/protocol/families/${familyId}`, {
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

export function getProtocolDecryptProfileDetail(
  profileId: IdType
): Promise<ApiEnvelope<ProtocolDecryptProfile>> {
  return request<ProtocolDecryptProfile>(`/api/governance/protocol/decrypt-profiles/${profileId}`, {
    method: 'GET'
  })
}

export function saveProtocolFamily(
  payload: ProtocolFamilyDefinitionUpsertPayload
): Promise<ApiEnvelope<ProtocolFamilyDefinition>> {
  return request<ProtocolFamilyDefinition>('/api/governance/protocol/families', {
    method: 'POST',
    body: payload
  })
}

export function saveProtocolDecryptProfile(
  payload: ProtocolDecryptProfileUpsertPayload
): Promise<ApiEnvelope<ProtocolDecryptProfile>> {
  return request<ProtocolDecryptProfile>('/api/governance/protocol/decrypt-profiles', {
    method: 'POST',
    body: payload
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

export function submitProtocolFamilyBatchPublish(
  payload: ProtocolGovernanceBatchSubmitPayload
): Promise<ApiEnvelope<ProtocolGovernanceBatchSubmitResult>> {
  return request<ProtocolGovernanceBatchSubmitResult>('/api/governance/protocol/families/batch-submit-publish', {
    method: 'POST',
    body: payload
  })
}

export function submitProtocolFamilyBatchRollback(
  payload: ProtocolGovernanceBatchSubmitPayload
): Promise<ApiEnvelope<ProtocolGovernanceBatchSubmitResult>> {
  return request<ProtocolGovernanceBatchSubmitResult>('/api/governance/protocol/families/batch-submit-rollback', {
    method: 'POST',
    body: payload
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

export function submitProtocolDecryptProfileBatchPublish(
  payload: ProtocolGovernanceBatchSubmitPayload
): Promise<ApiEnvelope<ProtocolGovernanceBatchSubmitResult>> {
  return request<ProtocolGovernanceBatchSubmitResult>(
    '/api/governance/protocol/decrypt-profiles/batch-submit-publish',
    {
      method: 'POST',
      body: payload
    }
  )
}

export function submitProtocolDecryptProfileBatchRollback(
  payload: ProtocolGovernanceBatchSubmitPayload
): Promise<ApiEnvelope<ProtocolGovernanceBatchSubmitResult>> {
  return request<ProtocolGovernanceBatchSubmitResult>(
    '/api/governance/protocol/decrypt-profiles/batch-submit-rollback',
    {
      method: 'POST',
      body: payload
    }
  )
}

export function previewProtocolDecrypt(
  payload: ProtocolDecryptPreviewPayload
): Promise<ApiEnvelope<ProtocolDecryptPreview>> {
  return request<ProtocolDecryptPreview>('/api/governance/protocol/decrypt-profiles/preview', {
    method: 'POST',
    body: payload
  })
}

export function replayProtocolDecrypt(
  payload: ProtocolGovernanceReplayPayload
): Promise<ApiEnvelope<ProtocolGovernanceReplay>> {
  return request<ProtocolGovernanceReplay>('/api/governance/protocol/decrypt-profiles/replay', {
    method: 'POST',
    body: payload
  })
}
