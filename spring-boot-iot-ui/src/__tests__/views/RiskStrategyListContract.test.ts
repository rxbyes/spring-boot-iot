import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

const governedRiskStrategyViews = [
  'RiskPointView.vue',
  'RuleDefinitionView.vue',
  'LinkageRuleView.vue',
  'EmergencyPlanView.vue'
]

describe('risk strategy list contract', () => {
  it.each(governedRiskStrategyViews)('%s adopts the standard list loading and pagination contract', (fileName) => {
    const source = readViewSource(fileName)

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('EmptyState')
    expect(source).toContain('page-sizes="[10, 20, 50, 100]"')
    expect(source).toContain('layout="total, sizes, prev, pager, next, jumper"')
    expect(source).toContain('v-loading="loading && hasRecords"')
  })

  it('keeps risk point row actions fully direct so delete does not fall back into more', () => {
    const source = readViewSource('RiskPointView.vue')

    expect(source).toContain(':max-direct-items="3"')
  })

  it('moves risk point maintenance to organization selection and backend auto-code generation', () => {
    const source = readViewSource('RiskPointView.vue')

    expect(source).toContain('listOrganizationTree')
    expect(source).toContain('label="所属组织"')
    expect(source).toContain('form.orgId')
    expect(source).toContain('保存后自动生成')
    expect(source).not.toContain('label="区域" prop="regionName"')
  })
})
