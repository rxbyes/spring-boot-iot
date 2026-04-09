import { describe, expect, it } from 'vitest'

import router from '@/router'

describe('governance routes', () => {
  it('registers governance control-plane and security routes', () => {
    const registeredPaths = new Set(router.getRoutes().map((route) => route.path))

    expect(registeredPaths.has('/governance-task')).toBe(true)
    expect(registeredPaths.has('/governance-ops')).toBe(true)
    expect(registeredPaths.has('/governance-approval')).toBe(true)
    expect(registeredPaths.has('/governance-security')).toBe(true)
  })
})
