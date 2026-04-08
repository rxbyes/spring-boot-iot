import { request } from './request'
import type {
  ApiEnvelope,
  GovernanceApprovalOrderDetail,
  GovernanceApprovalResubmitPayload,
  IdType
} from '../types/api'

export const governanceApprovalApi = {
  getOrderDetail(orderId: IdType): Promise<ApiEnvelope<GovernanceApprovalOrderDetail>> {
    return request<GovernanceApprovalOrderDetail>(`/api/system/governance-approval/${orderId}`, {
      method: 'GET'
    })
  },

  resubmitOrder(orderId: IdType, payload: GovernanceApprovalResubmitPayload): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/system/governance-approval/${orderId}/resubmit`, {
      method: 'POST',
      body: payload
    })
  }
}
