import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readComponentSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../components/${fileName}`), 'utf8')
}

describe('quality workshop row action spacing governance', () => {
  it('keeps automation discovery actions on shared table defaults without page-private gap overrides', () => {
    const source = readComponentSource('AutomationPageDiscoveryPanel.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).not.toContain('gap: "compact"')
  })
})
