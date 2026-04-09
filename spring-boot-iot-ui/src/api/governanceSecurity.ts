import { request } from './request'
import { buildQueryString } from './query'
import type { ApiEnvelope, IdType, PageResult } from '@/types/api'

export interface GovernancePermissionMatrixItem {
  domainCode?: string | null
  domainName?: string | null
  actionCode?: string | null
  actionName?: string | null
  operatorPermissionCode?: string | null
  approverPermissionCode?: string | null
  defaultRoleCodes?: string[] | null
  defaultApproverRoleCodes?: string[] | null
  dualControlRequired?: boolean | null
  auditModule?: string | null
}

export interface DeviceSecretRotationLogPageItem {
  id?: IdType | null
  deviceId?: IdType | null
  deviceCode?: string | null
  productKey?: string | null
  rotationBatchId?: string | null
  reason?: string | null
  previousSecretDigest?: string | null
  currentSecretDigest?: string | null
  rotatedBy?: IdType | null
  approvedBy?: IdType | null
  rotateTime?: string | null
}

export interface DeviceSecretRotationLogPageQuery {
  deviceCode?: string
  productKey?: string
  rotationBatchId?: string
  rotatedBy?: IdType
  approvedBy?: IdType
  beginTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export function getGovernancePermissionMatrix(): Promise<ApiEnvelope<GovernancePermissionMatrixItem[]>> {
  return request<GovernancePermissionMatrixItem[]>('/api/system/governance/permission-matrix', {
    method: 'GET'
  })
}

export function pageDeviceSecretRotationLogs(
  params: DeviceSecretRotationLogPageQuery = {}
): Promise<ApiEnvelope<PageResult<DeviceSecretRotationLogPageItem>>> {
  const queryString = buildQueryString(params)
  const path = queryString
    ? `/api/device/secret-rotation-logs?${queryString}`
    : '/api/device/secret-rotation-logs'
  return request<PageResult<DeviceSecretRotationLogPageItem>>(path, {
    method: 'GET'
  })
}
