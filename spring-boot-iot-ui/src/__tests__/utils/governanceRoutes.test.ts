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

  it('keeps legacy quality workbench routes as compatibility redirects', () => {
    const registeredPaths = new Set(router.getRoutes().map((route) => route.path))

    expect(registeredPaths.has('/rd-workbench')).toBe(true)
    expect(registeredPaths.has('/rd-automation-inventory')).toBe(true)
    expect(registeredPaths.has('/rd-automation-templates')).toBe(true)
    expect(registeredPaths.has('/rd-automation-plans')).toBe(true)
    expect(registeredPaths.has('/rd-automation-handoff')).toBe(true)
    expect(registeredPaths.has('/automation-assets')).toBe(true)
    expect(registeredPaths.has('/automation-test')).toBe(true)
    expect(registeredPaths.has('/automation-execution')).toBe(true)
    expect(registeredPaths.has('/automation-results')).toBe(true)
  })
})
