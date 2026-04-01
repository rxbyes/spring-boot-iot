import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

describe('risk operations row action spacing governance', () => {
  it('keeps real-time monitoring on shared row-action grammar without a page-private gap override', () => {
    const source = readViewSource('RealTimeMonitoringView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps GIS table and card actions on shared defaults without local gap overrides', () => {
    const source = readViewSource('RiskGisView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).toContain('variant="card"')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps alarm center on shared row actions without page-private gap props', () => {
    const source = readViewSource('AlarmCenterView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })

  it('keeps event disposal on shared row actions without a table gap override', () => {
    const source = readViewSource('EventDisposalView.vue')

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).toContain('variant="table"')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).toContain('resolveWorkbenchActionColumnWidth')
  })
})
