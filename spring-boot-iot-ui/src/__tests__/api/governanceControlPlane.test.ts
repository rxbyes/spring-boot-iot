import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  ackGovernanceWorkItem,
  blockGovernanceWorkItem,
  closeGovernanceWorkItem,
  pageGovernanceWorkItems
} from '@/api/governanceWorkItem'
import {
  ackGovernanceOpsAlert,
  closeGovernanceOpsAlert,
  pageGovernanceOpsAlerts,
  suppressGovernanceOpsAlert
} from '@/api/governanceOpsAlert'
import { request } from '@/api/request'

vi.mock('@/api/request', () => ({
  request: vi.fn()
}))

describe('governance control plane api', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
    vi.mocked(request).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  })

  it('builds governance work item page query parameters correctly', async () => {
    await pageGovernanceWorkItems({
      workStatus: 'OPEN',
      workItemCode: 'PENDING_CONTRACT_RELEASE',
      pageNum: 1,
      pageSize: 20
    })

    expect(request).toHaveBeenCalledWith(
      '/api/governance/work-items?workStatus=OPEN&workItemCode=PENDING_CONTRACT_RELEASE&pageNum=1&pageSize=20',
      {
        method: 'GET'
      }
    )
  })

  it('builds governance ops alert page query parameters correctly', async () => {
    await pageGovernanceOpsAlerts({
      alertStatus: 'OPEN',
      alertType: 'FIELD_DRIFT',
      pageNum: 1,
      pageSize: 20
    })

    expect(request).toHaveBeenCalledWith(
      '/api/governance/ops-alerts?alertStatus=OPEN&alertType=FIELD_DRIFT&pageNum=1&pageSize=20',
      {
        method: 'GET'
      }
    )
  })

  it('posts governance work item transitions to dedicated endpoints', async () => {
    await ackGovernanceWorkItem(11, { comment: '已确认处理' })
    await blockGovernanceWorkItem(12, { comment: '等待外部条件' })
    await closeGovernanceWorkItem(13, { comment: '已关闭' })

    expect(request).toHaveBeenNthCalledWith(1, '/api/governance/work-items/11/ack', {
      method: 'POST',
      body: {
        comment: '已确认处理'
      }
    })
    expect(request).toHaveBeenNthCalledWith(2, '/api/governance/work-items/12/block', {
      method: 'POST',
      body: {
        comment: '等待外部条件'
      }
    })
    expect(request).toHaveBeenNthCalledWith(3, '/api/governance/work-items/13/close', {
      method: 'POST',
      body: {
        comment: '已关闭'
      }
    })
  })

  it('posts governance ops transitions to dedicated endpoints', async () => {
    await ackGovernanceOpsAlert(21, { comment: '已确认' })
    await suppressGovernanceOpsAlert(22, { comment: '暂时抑制' })
    await closeGovernanceOpsAlert(23, { comment: '已关闭' })

    expect(request).toHaveBeenNthCalledWith(1, '/api/governance/ops-alerts/21/ack', {
      method: 'POST',
      body: {
        comment: '已确认'
      }
    })
    expect(request).toHaveBeenNthCalledWith(2, '/api/governance/ops-alerts/22/suppress', {
      method: 'POST',
      body: {
        comment: '暂时抑制'
      }
    })
    expect(request).toHaveBeenNthCalledWith(3, '/api/governance/ops-alerts/23/close', {
      method: 'POST',
      body: {
        comment: '已关闭'
      }
    })
  })
})
