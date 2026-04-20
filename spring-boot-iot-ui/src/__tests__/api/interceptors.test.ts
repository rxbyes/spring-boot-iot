import { beforeEach, describe, expect, it, vi } from 'vitest'

const { messageErrorSpy } = vi.hoisted(() => ({
  messageErrorSpy: vi.fn()
}))

vi.mock('@/utils/message', () => ({
  ElMessage: {
    error: messageErrorSpy
  }
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: {
      value: {
        path: '/products',
        fullPath: '/products'
      }
    },
    replace: vi.fn().mockResolvedValue(undefined)
  }
}))

vi.mock('@/stores/permission', () => ({
  getStoredAccessToken: vi.fn(() => null),
  usePermissionStore: vi.fn(() => ({
    logout: vi.fn()
  }))
}))

import { errorResponseInterceptor } from '@/api/interceptors'
import type { RequestError } from '@/api/request'

describe('errorResponseInterceptor', () => {
  beforeEach(() => {
    messageErrorSpy.mockReset()
  })

  it('surfaces raw 500 business messages instead of collapsing them to the generic busy copy', async () => {
    try {
      await errorResponseInterceptor.onsuccess?.(
        {
          code: 500,
          msg: '对象洞察指标必须使用已发布合同标识符: value',
          data: null
        },
        {}
      )
      throw new Error('expected interceptor to reject')
    } catch (error) {
      const requestError = error as RequestError
      expect(requestError.handled).toBe(true)
      expect(requestError.status).toBe(500)
      expect(requestError.message).toBe('对象洞察指标必须使用已发布合同标识符: value')
      expect(requestError.rawMessage).toBe('对象洞察指标必须使用已发布合同标识符: value')
    }
  })

  it('keeps the generic busy copy for true system-level 500 errors', async () => {
    await expect(
      errorResponseInterceptor.onsuccess?.(
        {
          code: 500,
          msg: '系统繁忙，请稍后再试',
          data: null
        },
        {}
      )
    ).rejects.toMatchObject({
      handled: true,
      status: 500,
      message: '系统繁忙，请稍后重试！',
      rawMessage: '系统繁忙，请稍后再试'
    })

  })
})
