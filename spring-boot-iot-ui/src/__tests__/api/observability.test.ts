import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getTraceEvidence,
  pageObservabilityBusinessEvents,
  pageObservabilitySpans
} from '@/api/observability'
import { request } from '@/api/request'

vi.mock('@/api/request', () => ({
  request: vi.fn()
}))

describe('observability api', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
    vi.mocked(request).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  })

  it('builds business event page query parameters correctly', async () => {
    await pageObservabilityBusinessEvents({
      traceId: 'trace-001',
      eventCode: 'product.contract.apply',
      domainCode: 'product',
      resultStatus: 'SUCCESS',
      pageNum: 1,
      pageSize: 20
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/business-events/page?traceId=trace-001&eventCode=product.contract.apply&domainCode=product&resultStatus=SUCCESS&pageNum=1&pageSize=20',
      {
        method: 'GET'
      }
    )
  })

  it('builds span page query parameters correctly', async () => {
    await pageObservabilitySpans({
      traceId: 'trace-001',
      spanType: 'SLOW_SQL',
      status: 'SUCCESS',
      minDurationMs: 1000,
      pageNum: 1,
      pageSize: 20
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/spans/page?traceId=trace-001&spanType=SLOW_SQL&status=SUCCESS&minDurationMs=1000&pageNum=1&pageSize=20',
      {
        method: 'GET'
      }
    )
  })

  it('loads a combined trace evidence package', async () => {
    await getTraceEvidence('trace-001')

    expect(request).toHaveBeenCalledWith('/api/system/observability/trace/trace-001', {
      method: 'GET'
    })
  })
})
