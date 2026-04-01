import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

const governedViews = [
  'RiskPointView.vue',
  'RuleDefinitionView.vue',
  'LinkageRuleView.vue',
  'EmergencyPlanView.vue',
  'OrganizationView.vue',
  'UserView.vue',
  'RoleView.vue',
  'RegionView.vue',
  'MenuView.vue',
  'DictView.vue',
  'ChannelView.vue',
  'InAppMessageView.vue',
  'HelpDocView.vue'
]

describe('governed row action spacing governance', () => {
  it.each(governedViews)('%s keeps table row actions on shared defaults without page-private gap overrides', (fileName) => {
    const source = readViewSource(fileName)

    expect(source).toContain('<StandardWorkbenchRowActions')
    expect(source).not.toContain('gap="compact"')
    expect(source).not.toContain("gap: 'compact'")
    expect(source).not.toContain('gap: "compact"')
  })
})
