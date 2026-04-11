import { request } from './request'
import { buildQueryString } from './query'
import type {
  ApiEnvelope,
  GovernanceApprovalDecisionPayload,
  GovernanceApprovalOrder,
  GovernanceApprovalOrderDetail,
  GovernanceApprovalPageQuery,
  GovernanceApprovalResubmitPayload,
  GovernanceSimulationResult,
  IdType,
  PageResult
} from '../types/api'

export const governanceApprovalApi = {
  pageOrders(params: GovernanceApprovalPageQuery = {}): Promise<ApiEnvelope<PageResult<GovernanceApprovalOrder>>> {
    const query = buildQueryString(params)
    const path = `/api/system/governance-approval/page${query ? `?${query}` : ''}`
    return request<PageResult<GovernanceApprovalOrder>>(path, {
      method: 'GET'
    })
  },

  getOrderDetail(orderId: IdType): Promise<ApiEnvelope<GovernanceApprovalOrderDetail>> {
    return request<GovernanceApprovalOrderDetail>(`/api/system/governance-approval/${orderId}`, {
      method: 'GET'
    })
  },

  simulateOrder(orderId: IdType): Promise<ApiEnvelope<GovernanceSimulationResult>> {
    return request<GovernanceSimulationResult>(`/api/system/governance-simulation/approval/${orderId}`, {
      method: 'POST'
    })
  },

  approveOrder(orderId: IdType, payload?: GovernanceApprovalDecisionPayload): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/system/governance-approval/${orderId}/approve`, {
      method: 'POST',
      body: payload ?? {}
    })
  },

  rejectOrder(orderId: IdType, payload?: GovernanceApprovalDecisionPayload): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/system/governance-approval/${orderId}/reject`, {
      method: 'POST',
      body: payload ?? {}
    })
  },

  cancelOrder(orderId: IdType, payload?: GovernanceApprovalDecisionPayload): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/system/governance-approval/${orderId}/cancel`, {
      method: 'POST',
      body: payload ?? {}
    })
  },

  resubmitOrder(orderId: IdType, payload: GovernanceApprovalResubmitPayload): Promise<ApiEnvelope<void>> {
    return request<void>(`/api/system/governance-approval/${orderId}/resubmit`, {
      method: 'POST',
      body: payload
    })
  }
}
