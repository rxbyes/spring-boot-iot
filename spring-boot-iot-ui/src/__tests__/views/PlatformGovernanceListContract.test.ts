import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function readViewSource(fileName: string) {
  return readFileSync(resolve(import.meta.dirname, `../../views/${fileName}`), 'utf8')
}

const governedPlatformViews = [
  'OrganizationView.vue',
  'UserView.vue',
  'RoleView.vue',
  'MenuView.vue',
  'RegionView.vue',
  'DictView.vue',
  'ChannelView.vue',
  'HelpDocView.vue'
]

describe('platform governance list contract', () => {
  it.each(governedPlatformViews)('%s adopts the standard list loading and pagination contract', (fileName) => {
    const source = readViewSource(fileName)

    expect(source).toContain('standard-list-surface')
    expect(source).toContain('EmptyState')
    expect(source).toContain('page-sizes="[10, 20, 50, 100]"')
    expect(source).toContain('layout="total, sizes, prev, pager, next, jumper"')
    expect(source).toContain('v-loading="loading && hasRecords"')
  })

  it('keeps region and organization first columns on the shared stacked identity grammar', () => {
    const regionSource = readViewSource('RegionView.vue')
    const organizationSource = readViewSource('OrganizationView.vue')

    expect(regionSource).toContain('secondary-prop="regionCode"')
    expect(regionSource).not.toContain('prop="regionCode"\r\n              label="区域编码"')
    expect(organizationSource).toContain('secondary-prop="orgCode"')
    expect(organizationSource).not.toContain('<StandardTableTextColumn prop="orgCode" label="组织编码"')
  })

  it('keeps user, role, menu, dict, and channel first columns on the shared stacked identity grammar', () => {
    const userSource = readViewSource('UserView.vue')
    const roleSource = readViewSource('RoleView.vue')
    const menuSource = readViewSource('MenuView.vue')
    const dictSource = readViewSource('DictView.vue')
    const channelSource = readViewSource('ChannelView.vue')

    expect(userSource).toContain('secondary-prop="nickname"')
    expect(userSource).not.toContain('<StandardTableTextColumn prop="nickname" label="昵称"')
    expect(roleSource).toContain('secondary-prop="roleCode"')
    expect(roleSource).not.toContain('prop="roleCode"\r\n              label="角色编码"')
    expect(menuSource).toContain('secondary-prop="menuCode"')
    expect(menuSource).not.toContain('<StandardTableTextColumn prop="menuCode" label="菜单编码"')
    expect(dictSource).toContain('secondary-prop="dictCode"')
    expect(dictSource).toContain('secondary-prop="itemValue"')
    expect(dictSource).not.toContain('<StandardTableTextColumn prop="dictCode" label="字典编码"')
    expect(dictSource).not.toContain('<StandardTableTextColumn prop="itemValue" label="项值"')
    expect(channelSource).toContain('secondary-prop="channelCode"')
    expect(channelSource).not.toContain('<StandardTableTextColumn prop="channelCode" label="渠道编码"')
  })

  it('keeps help docs and in-app messages on the shared stacked identity grammar', () => {
    const helpDocSource = readViewSource('HelpDocView.vue')
    const inAppMessageSource = readViewSource('InAppMessageView.vue')

    expect(helpDocSource).toContain('<template #secondary="{ row }">')
    expect(helpDocSource).toContain('getRelatedPathSummary(row.relatedPaths)')
    expect(helpDocSource).not.toContain('<StandardTableTextColumn label="鍏宠仈椤甸潰"')

    expect(inAppMessageSource).toContain('getPathLabel(row.relatedPath)')
    expect(inAppMessageSource).toContain('secondary-prop="channelCode"')
    expect(inAppMessageSource).not.toContain('<StandardTableTextColumn prop="relatedPath" label="鍏宠仈椤甸潰"')
    expect(inAppMessageSource).not.toContain('<StandardTableTextColumn prop="channelCode" label="娓犻亾缂栫爜"')
  })
})
