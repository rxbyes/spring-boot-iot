import { describe, expect, it } from 'vitest'

import { resolveAdaptiveActionColumnWidth } from '@/utils/adaptiveActionColumn'

describe('resolveAdaptiveActionColumnWidth', () => {
  it('keeps one lightweight action at the minimum compact width baseline', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['查看'],
        gap: 'compact'
      })
    ).toBe(96)
  })

  it('shrinks a two-action compact workbench column to the dual-action baseline', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['进入工作台', '删除'],
        gap: 'compact'
      })
    ).toBe(176)
  })

  it('shrinks two short wide-gap actions below the old fixed 180px column', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '观测'],
        gap: 'wide'
      })
    ).toBe(144)
  })

  it('expands for three direct actions in wide mode', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '追踪', '删除'],
        gap: 'wide'
      })
    ).toBe(200)
  })

  it('accounts for the more-actions trigger in adaptive mode', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '编辑'],
        menuLabel: '更多',
        gap: 'wide'
      })
    ).toBe(200)
  })
})
