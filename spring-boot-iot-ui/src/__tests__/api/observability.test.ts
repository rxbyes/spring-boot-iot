import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getObservabilityMessageArchiveBatchCompare,
  getObservabilityMessageArchiveBatchReportPreview,
  getTraceEvidence,
  listObservabilitySlowSpanSummaries,
  listObservabilitySlowSpanTrends,
  pageObservabilityMessageArchiveBatches,
  pageObservabilityScheduledTasks,
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

  it('builds slow span summary query parameters correctly', async () => {
    await listObservabilitySlowSpanSummaries({
      spanType: 'SLOW_SQL',
      domainCode: 'system',
      status: 'SUCCESS',
      minDurationMs: 1000,
      dateFrom: '2026-04-25',
      dateTo: '2026-04-25',
      limit: 10
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/spans/slow-summary?spanType=SLOW_SQL&domainCode=system&status=SUCCESS&minDurationMs=1000&dateFrom=2026-04-25&dateTo=2026-04-25&limit=10',
      {
        method: 'GET'
      }
    )
  })

  it('builds scheduled task page query parameters correctly', async () => {
    await pageObservabilityScheduledTasks({
      traceId: 'trace-scheduled-1',
      taskCode: 'DeviceSessionTimeoutScheduler#closeTimedOutSessions',
      triggerType: 'FIXED_DELAY',
      status: 'SUCCESS',
      minDurationMs: 200,
      pageNum: 1,
      pageSize: 5
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/scheduled-tasks/page?traceId=trace-scheduled-1&taskCode=DeviceSessionTimeoutScheduler%23closeTimedOutSessions&triggerType=FIXED_DELAY&status=SUCCESS&minDurationMs=200&pageNum=1&pageSize=5',
      {
        method: 'GET'
      }
    )
  })

  it('builds message archive batch page query parameters correctly', async () => {
    await pageObservabilityMessageArchiveBatches({
      batchNo: 'iot_message_log-20260426000119',
      sourceTable: 'iot_message_log',
      status: 'SUCCEEDED',
      dateFrom: '2026-04-26T00:00:00',
      dateTo: '2026-04-26T23:59:59',
      pageNum: 1,
      pageSize: 10
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/message-archive-batches/page?batchNo=iot_message_log-20260426000119&sourceTable=iot_message_log&status=SUCCEEDED&dateFrom=2026-04-26T00%3A00%3A00&dateTo=2026-04-26T23%3A59%3A59&pageNum=1&pageSize=10',
      {
        method: 'GET'
      }
    )
  })

  it('loads message archive batch report preview by batch number', async () => {
    await getObservabilityMessageArchiveBatchReportPreview('iot_message_log-20260426000119')

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/message-archive-batches/report-preview?batchNo=iot_message_log-20260426000119',
      {
        method: 'GET'
      }
    )
  })

  it('loads message archive batch compare by batch number', async () => {
    await getObservabilityMessageArchiveBatchCompare('iot_message_log-20260426000119')

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/message-archive-batches/compare?batchNo=iot_message_log-20260426000119',
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

  it('builds slow span trend query parameters correctly', async () => {
    await listObservabilitySlowSpanTrends({
      spanType: 'SLOW_SQL',
      domainCode: 'system',
      eventCode: 'system.error.archive',
      objectType: 'sql',
      objectId: 'iot_message_log',
      minDurationMs: 1000,
      bucket: 'HOUR',
      dateFrom: '2026-04-25T09:00:00',
      dateTo: '2026-04-25T11:59:59'
    })

    expect(request).toHaveBeenCalledWith(
      '/api/system/observability/spans/slow-trends?spanType=SLOW_SQL&domainCode=system&eventCode=system.error.archive&objectType=sql&objectId=iot_message_log&minDurationMs=1000&bucket=HOUR&dateFrom=2026-04-25T09%3A00%3A00&dateTo=2026-04-25T11%3A59%3A59',
      {
        method: 'GET'
      }
    )
  })
})
