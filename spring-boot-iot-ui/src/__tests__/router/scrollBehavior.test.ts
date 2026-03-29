import { describe, expect, it } from 'vitest'

import { resolveAppScrollBehavior } from '@/router/scrollBehavior'

describe('resolveAppScrollBehavior', () => {
  it('keeps current scroll position when only the same-route query changes', () => {
    expect(
      resolveAppScrollBehavior(
        { path: '/products', hash: '', fullPath: '/products?pageNum=2' } as any,
        { path: '/products', hash: '', fullPath: '/products?pageNum=1' } as any
      )
    ).toBe(false)
  })

  it('keeps browser savedPosition when available', () => {
    expect(
      resolveAppScrollBehavior(
        { path: '/products', hash: '', fullPath: '/products?pageNum=2' } as any,
        { path: '/products', hash: '', fullPath: '/products?pageNum=1' } as any,
        { left: 0, top: 420 }
      )
    ).toEqual({ left: 0, top: 420 })
  })

  it('scrolls to the top for real page navigation', () => {
    expect(
      resolveAppScrollBehavior(
        { path: '/devices', hash: '', fullPath: '/devices' } as any,
        { path: '/products', hash: '', fullPath: '/products' } as any
      )
    ).toEqual({ top: 0 })
  })
})
