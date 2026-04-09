import { beforeEach, describe, expect, it, vi } from 'vitest'

import { pageGovernanceWorkItems } from '@/api/governanceWorkItem'
import { pageGovernanceOpsAlerts } from '@/api/governanceOpsAlert'
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
})
