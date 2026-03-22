import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  getInAppMessageBridgeStats,
  listInAppMessageBridgeAttempts,
  pageInAppMessageBridgeLogs
} from '@/api/inAppMessage'
import { request } from '@/api/request'

vi.mock('@/api/request', () => ({
  request: vi.fn()
}))

describe('inAppMessage bridge api', () => {
  beforeEach(() => {
    vi.mocked(request).mockReset()
    vi.mocked(request).mockResolvedValue({
      code: 200,
      msg: 'success',
      data: null
    })
  })

  it('builds bridge stats query parameters correctly', async () => {
    const controller = new AbortController()

    await getInAppMessageBridgeStats({
      startTime: '2026-03-15 00:00:00',
      endTime: '2026-03-22 23:59:59',
      messageType: 'error',
      sourceType: 'system_error',
      priority: 'high',
      channelCode: 'wechat-alert',
      bridgeStatus: 0
    }, {
      signal: controller.signal
    })

    const [url, options] = vi.mocked(request).mock.calls[0]
    const params = new URLSearchParams(String(url).split('?')[1] || '')

    expect(String(url)).toContain('/api/system/in-app-message/bridge/stats?')
    expect(params.get('startTime')).toBe('2026-03-15 00:00:00')
    expect(params.get('endTime')).toBe('2026-03-22 23:59:59')
    expect(params.get('messageType')).toBe('error')
    expect(params.get('sourceType')).toBe('system_error')
    expect(params.get('priority')).toBe('high')
    expect(params.get('channelCode')).toBe('wechat-alert')
    expect(params.get('bridgeStatus')).toBe('0')
    expect(options).toMatchObject({
      method: 'GET',
      signal: controller.signal
    })
  })

  it('builds bridge page query parameters correctly', async () => {
    await pageInAppMessageBridgeLogs({
      sourceType: 'work_order',
      priority: 'medium',
      pageNum: 3,
      pageSize: 20
    })

    const [url, options] = vi.mocked(request).mock.calls[0]
    const params = new URLSearchParams(String(url).split('?')[1] || '')

    expect(String(url)).toContain('/api/system/in-app-message/bridge/page?')
    expect(params.get('sourceType')).toBe('work_order')
    expect(params.get('priority')).toBe('medium')
    expect(params.get('pageNum')).toBe('3')
    expect(params.get('pageSize')).toBe('20')
    expect(options).toMatchObject({
      method: 'GET'
    })
  })

  it('requests attempt details by bridge log id', async () => {
    const controller = new AbortController()

    await listInAppMessageBridgeAttempts('3001', {
      signal: controller.signal
    })

    expect(request).toHaveBeenCalledWith('/api/system/in-app-message/bridge/3001/attempts', {
      method: 'GET',
      signal: controller.signal
    })
  })
})
