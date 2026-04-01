import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

const governedRiskOperationsViews = [
  'RealTimeMonitoringView.vue',
  'AlarmCenterView.vue',
  'EventDisposalView.vue'
]

describe('risk operations list contract', () => {
  it.each(governedRiskOperationsViews)('%s adopts the standard list loading and pagination contract', (fileName) => {
    const source = readViewSource(fileName)

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('EmptyState')
    expect(source).toContain('page-sizes="[10, 20, 50, 100]"')
    expect(source).toContain('layout="total, sizes, prev, pager, next, jumper"')
    expect(source).toContain('v-loading="loading && hasRecords"')
  })

  it('keeps GIS monitoring on the same standard result-surface and pagination contract', () => {
    const source = readViewSource('RiskGisView.vue')

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('EmptyState')
    expect(source).toContain('ops-list-loading-state')
    expect(source).toContain('page-sizes="[10, 20, 50, 100]"')
    expect(source).toContain('layout="total, sizes, prev, pager, next, jumper"')
  })
})
