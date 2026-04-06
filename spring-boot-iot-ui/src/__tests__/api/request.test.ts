import { describe, expect, it } from 'vitest'

import * as requestModule from '@/api/request'
import { createRequestError } from '@/api/request'

describe('request error message normalization', () => {
  it('prefers the shared system busy copy for 500 request errors', () => {
    const error = createRequestError('系统繁忙，请稍后重试！', false, 500)

    expect((requestModule as any).resolveRequestErrorMessage(error, '获取列表失败')).toBe(
      '系统繁忙，请稍后重试！'
    )
  })

  it('falls back to the page-specific message when the error is not an Error instance', () => {
    expect((requestModule as any).resolveRequestErrorMessage(null, '获取列表失败')).toBe('获取列表失败')
  })
})
