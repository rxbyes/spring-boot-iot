import { buildQueryString } from './query'
import { request } from './request'
import type {
  ApiEnvelope,
  DeviceOnboardingCaseBatchCreatePayload,
  DeviceOnboardingCaseBatchResult,
  DeviceOnboardingCaseBatchStartAcceptancePayload,
  DeviceOnboardingCaseBatchTemplateApplyPayload,
  DeviceOnboardingCase,
  DeviceOnboardingCaseCreatePayload,
  DeviceOnboardingCasePageQuery,
  DeviceOnboardingCaseUpdatePayload,
  IdType,
  OnboardingTemplatePack,
  OnboardingTemplatePackCreatePayload,
  OnboardingTemplatePackPageQuery,
  OnboardingTemplatePackUpdatePayload,
  PageResult
} from '@/types/api'

export type DeviceOnboardingCasePageResult = PageResult<DeviceOnboardingCase>
export type OnboardingTemplatePackPageResult = PageResult<OnboardingTemplatePack>

export function pageDeviceOnboardingCases(
  params: DeviceOnboardingCasePageQuery = {}
): Promise<ApiEnvelope<DeviceOnboardingCasePageResult>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/device/onboarding/cases?${queryString}`
    : '/api/device/onboarding/cases'
  return request<DeviceOnboardingCasePageResult>(path, {
    method: 'GET'
  })
}

export function pageOnboardingTemplatePacks(
  params: OnboardingTemplatePackPageQuery = {}
): Promise<ApiEnvelope<OnboardingTemplatePackPageResult>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/device/onboarding/template-packs?${queryString}`
    : '/api/device/onboarding/template-packs'
  return request<OnboardingTemplatePackPageResult>(path, {
    method: 'GET'
  })
}

export function createDeviceOnboardingCase(
  payload: DeviceOnboardingCaseCreatePayload
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>('/api/device/onboarding/cases', {
    method: 'POST',
    body: payload
  })
}

export function batchCreateDeviceOnboardingCases(
  payload: DeviceOnboardingCaseBatchCreatePayload
): Promise<ApiEnvelope<DeviceOnboardingCaseBatchResult>> {
  return request<DeviceOnboardingCaseBatchResult>('/api/device/onboarding/cases/batch-create', {
    method: 'POST',
    body: payload
  })
}

export function createOnboardingTemplatePack(
  payload: OnboardingTemplatePackCreatePayload
): Promise<ApiEnvelope<OnboardingTemplatePack>> {
  return request<OnboardingTemplatePack>('/api/device/onboarding/template-packs', {
    method: 'POST',
    body: payload
  })
}

export function getDeviceOnboardingCase(id: IdType): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>(`/api/device/onboarding/cases/${id}`, {
    method: 'GET'
  })
}

export function updateDeviceOnboardingCase(
  id: IdType,
  payload: DeviceOnboardingCaseUpdatePayload
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>(`/api/device/onboarding/cases/${id}`, {
    method: 'PUT',
    body: payload
  })
}

export function updateOnboardingTemplatePack(
  id: IdType,
  payload: OnboardingTemplatePackUpdatePayload
): Promise<ApiEnvelope<OnboardingTemplatePack>> {
  return request<OnboardingTemplatePack>(`/api/device/onboarding/template-packs/${id}`, {
    method: 'PUT',
    body: payload
  })
}

export function batchApplyDeviceOnboardingCaseTemplate(
  payload: DeviceOnboardingCaseBatchTemplateApplyPayload
): Promise<ApiEnvelope<DeviceOnboardingCaseBatchResult>> {
  return request<DeviceOnboardingCaseBatchResult>('/api/device/onboarding/cases/batch-apply-template', {
    method: 'POST',
    body: payload
  })
}

export function refreshDeviceOnboardingCaseStatus(
  id: IdType
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>(`/api/device/onboarding/cases/${id}/refresh-status`, {
    method: 'POST'
  })
}

export function startDeviceOnboardingCaseAcceptance(
  id: IdType
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>(`/api/device/onboarding/cases/${id}/start-acceptance`, {
    method: 'POST'
  })
}

export function batchStartDeviceOnboardingCasesAcceptance(
  payload: DeviceOnboardingCaseBatchStartAcceptancePayload
): Promise<ApiEnvelope<DeviceOnboardingCaseBatchResult>> {
  return request<DeviceOnboardingCaseBatchResult>('/api/device/onboarding/cases/batch-start-acceptance', {
    method: 'POST',
    body: payload
  })
}

export const deviceOnboardingApi = {
  pageCases: pageDeviceOnboardingCases,
  pageTemplatePacks: pageOnboardingTemplatePacks,
  createCase: createDeviceOnboardingCase,
  batchCreateCases: batchCreateDeviceOnboardingCases,
  createTemplatePack: createOnboardingTemplatePack,
  getCase: getDeviceOnboardingCase,
  updateCase: updateDeviceOnboardingCase,
  updateTemplatePack: updateOnboardingTemplatePack,
  batchApplyTemplate: batchApplyDeviceOnboardingCaseTemplate,
  refreshStatus: refreshDeviceOnboardingCaseStatus,
  startAcceptance: startDeviceOnboardingCaseAcceptance,
  batchStartAcceptance: batchStartDeviceOnboardingCasesAcceptance
}
