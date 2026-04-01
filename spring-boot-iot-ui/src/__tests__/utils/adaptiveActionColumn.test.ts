import { describe, expect, it } from 'vitest'

import {
  resolveAdaptiveActionColumnWidth,
  resolveWorkbenchActionColumnWidth,
  resolveWorkbenchActionColumnWidthByRows,
  splitWorkbenchRowActions
} from '@/utils/adaptiveActionColumn'

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
    ).toBe(144)
  })

  it('shrinks two short wide-gap actions below the old fixed 180px column', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '观测'],
        gap: 'wide'
      })
    ).toBe(112)
  })

  it('expands for three direct actions in wide mode', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '追踪', '删除'],
        gap: 'wide'
      })
    ).toBe(152)
  })

  it('accounts for the more-actions trigger in adaptive mode', () => {
    expect(
      resolveAdaptiveActionColumnWidth({
        directLabels: ['详情', '编辑'],
        menuLabel: '更多',
        gap: 'wide'
      })
    ).toBe(152)
  })

  it('splits row actions into two direct entries plus a more-menu payload', () => {
    expect(
      splitWorkbenchRowActions({
        directItems: [
          { command: 'detail', label: '详情' },
          { command: 'edit', label: '编辑' },
          { command: 'delete', label: '删除' }
        ]
      })
    ).toEqual({
      directItems: [
        { command: 'detail', label: '详情' },
        { command: 'edit', label: '编辑' }
      ],
      menuItems: [{ command: 'delete', label: '删除', disabled: undefined, key: undefined }]
    })
  })

  it('uses the shared three-action desktop baseline when overflow direct actions are folded into more', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [
          { command: 'detail', label: '详情' },
          { command: 'trace', label: '追踪' },
          { command: 'observe', label: '观测' }
        ],
        gap: 'compact'
      })
    ).toBe(160)
  })

  it('uses the shared single-action desktop width tier for one visible table action', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [{ command: 'detail', label: '详情' }],
        gap: 'compact'
      })
    ).toBe(96)
  })

  it('keeps short two-action desktop columns close to content width instead of the old 144px floor', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [
          { command: 'detail', label: '详情' },
          { command: 'observe', label: '观测' }
        ],
        gap: 'compact'
      })
    ).toBe(112)
  })

  it('still allows longer two-action desktop columns to expand to the product-workbench width', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [
          { command: 'detail', label: '进入工作台' },
          { command: 'delete', label: '删除' }
        ],
        gap: 'compact'
      })
    ).toBe(152)
  })

  it('uses the shared detail-edit-more desktop width tier when overflow actions fold into more', () => {
    expect(
      resolveWorkbenchActionColumnWidth({
        directItems: [
          { command: 'detail', label: '详情' },
          { command: 'trace', label: '追踪' },
          { command: 'observe', label: '观测' }
        ],
        gap: 'compact'
      })
    ).toBe(160)
  })

  it('shrinks a current page of event-style two-action rows to the dual-action desktop width', () => {
    expect(
      resolveWorkbenchActionColumnWidthByRows({
        rows: [
          {
            directItems: [
              { command: 'detail', label: '详情' },
              { command: 'close', label: '关闭' }
            ]
          },
          {
            directItems: [
              { command: 'detail', label: '详情' },
              { command: 'close', label: '关闭' }
            ]
          }
        ]
      })
    ).toBe(112)
  })

  it('keeps the three-action desktop width when at least one current row still overflows into more', () => {
    expect(
      resolveWorkbenchActionColumnWidthByRows({
        rows: [
          {
            directItems: [
              { command: 'detail', label: '详情' },
              { command: 'dispatch', label: '派发' },
              { command: 'close', label: '关闭' }
            ]
          },
          {
            directItems: [
              { command: 'detail', label: '详情' },
              { command: 'close', label: '关闭' }
            ]
          }
        ]
      })
    ).toBe(160)
  })
})
