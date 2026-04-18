import { buildQueryString } from './query'
import { request } from './request'
import type {
  ApiEnvelope,
  DeviceOnboardingCase,
  DeviceOnboardingCaseCreatePayload,
  DeviceOnboardingCasePageQuery,
  DeviceOnboardingCaseUpdatePayload,
  IdType,
  PageResult
} from '@/types/api'

export type DeviceOnboardingCasePageResult = PageResult<DeviceOnboardingCase>

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

export function createDeviceOnboardingCase(
  payload: DeviceOnboardingCaseCreatePayload
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>('/api/device/onboarding/cases', {
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

export function refreshDeviceOnboardingCaseStatus(
  id: IdType
): Promise<ApiEnvelope<DeviceOnboardingCase>> {
  return request<DeviceOnboardingCase>(`/api/device/onboarding/cases/${id}/refresh-status`, {
    method: 'POST'
  })
}

export const deviceOnboardingApi = {
  pageCases: pageDeviceOnboardingCases,
  createCase: createDeviceOnboardingCase,
  getCase: getDeviceOnboardingCase,
  updateCase: updateDeviceOnboardingCase,
  refreshStatus: refreshDeviceOnboardingCaseStatus
}
