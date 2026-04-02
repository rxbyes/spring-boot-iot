import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readView(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

function readRouter() {
  return readFileSync(resolve(import.meta.dirname, '../../router/index.ts'), 'utf8')
}

describe('automation workbench route splits', () => {
  it('keeps the asset page focused on inventory and scenario editing', () => {
    const source = readView('AutomationAssetsView.vue')

    expect(source).toContain('<AutomationPageDiscoveryPanel')
    expect(source).toContain('<AutomationScenarioEditor')
    expect(source).not.toContain('<AutomationRegistryPanel')
    expect(source).not.toContain('<AutomationResultImportPanel')
  })

  it('keeps the legacy automation-test view as a compatibility wrapper', () => {
    const source = readView('AutomationTestCenterView.vue')

    expect(source).toContain('<AutomationAssetsView')
    expect(source).not.toContain('<AutomationRegistryPanel')
    expect(source).not.toContain('<AutomationResultImportPanel')
  })

  it('registers the new asset route while keeping the legacy automation-test path', () => {
    const source = readRouter()

    expect(source).toContain("path: '/automation-assets'")
    expect(source).toContain("name: 'automation-assets'")
    expect(source).toContain("path: '/automation-test'")
    expect(source).toContain("name: 'automation-test'")
  })
})
