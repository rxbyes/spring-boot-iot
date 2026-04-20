import { describe, expect, it } from 'vitest'

import * as requestModule from '@/api/request'
import { createRequestError } from '@/api/request'

describe('request error message normalization', () => {
  it('retains same-text raw business messages for 500 request errors', () => {
    const error = createRequestError(
      '当前动作未配置固定复核策略',
      false,
      500,
      '当前动作未配置固定复核策略'
    )

    expect(error.rawMessage).toBe('当前动作未配置固定复核策略')
    expect((requestModule as any).resolveRequestErrorMessage(error, '获取列表失败')).toBe(
      '当前动作未配置固定复核策略'
    )
  })

  it('prefers raw business messages for 500 request errors when provided', () => {
    const error = createRequestError(
      '系统繁忙，请稍后重试！',
      false,
      500,
      '对象洞察指标必须使用已发布合同标识符: value'
    )

    expect((requestModule as any).resolveRequestErrorMessage(error, '获取列表失败')).toBe(
      '对象洞察指标必须使用已发布合同标识符: value'
    )
  })

  it('falls back to the page-specific message when the error is not an Error instance', () => {
    expect((requestModule as any).resolveRequestErrorMessage(null, '获取列表失败')).toBe('获取列表失败')
  })

  it('keeps the shared system busy copy for generic 500 request errors', () => {
    const error = createRequestError('系统繁忙，请稍后重试！', false, 500, '系统繁忙，请稍后再试')

    expect((requestModule as any).resolveRequestErrorMessage(error, '获取列表失败')).toBe(
      '系统繁忙，请稍后重试！'
    )
  })
})
