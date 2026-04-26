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

  it('does not register the retired quality workbench legacy routes', () => {
    const registeredPaths = new Set(router.getRoutes().map((route) => route.path))

    expect(registeredPaths.has('/rd-workbench')).toBe(false)
    expect(registeredPaths.has('/rd-automation-inventory')).toBe(false)
    expect(registeredPaths.has('/rd-automation-templates')).toBe(false)
    expect(registeredPaths.has('/rd-automation-plans')).toBe(false)
    expect(registeredPaths.has('/rd-automation-handoff')).toBe(false)
    expect(registeredPaths.has('/automation-assets')).toBe(false)
    expect(registeredPaths.has('/automation-test')).toBe(false)
    expect(registeredPaths.has('/automation-execution')).toBe(false)
    expect(registeredPaths.has('/automation-results')).toBe(false)
  })
})
