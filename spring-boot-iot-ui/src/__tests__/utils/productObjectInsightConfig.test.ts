import { describe, expect, it } from 'vitest'

import {
  buildProductMetadataJson,
  createEmptyProductObjectInsightMetric,
  parseProductObjectInsightMetrics,
  validateProductObjectInsightMetrics
} from '@/utils/productObjectInsightConfig'

describe('productObjectInsightConfig', () => {
  it('parses custom metrics from metadataJson into editable rows', () => {
    const rows = parseProductObjectInsightMetrics(
      JSON.stringify({
        objectInsight: {
          customMetrics: [
            {
              identifier: 'S1_ZT_1.humidity',
              displayName: '相对湿度',
              group: 'status',
              includeInTrend: true,
              includeInExtension: true,
              enabled: true,
              sortNo: 10
            }
          ]
        }
      })
    )

    expect(rows).toHaveLength(1)
    expect(rows[0].displayName).toBe('相对湿度')
    expect(rows[0].group).toBe('status')
  })

  it('serializes editable rows back into metadataJson.objectInsight.customMetrics', () => {
    const metadataJson = buildProductMetadataJson([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.signal_4g',
        displayName: '4G 信号强度',
        group: 'status',
        analysisTemplate: '{{label}}当前为{{value}}',
        sortNo: 20
      }
    ])

    expect(metadataJson).toContain('objectInsight')
    expect(metadataJson).toContain('S1_ZT_1.signal_4g')
  })

  it('rejects duplicate identifiers before submit', () => {
    const message = validateProductObjectInsightMetrics([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '相对湿度',
        group: 'status'
      },
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '重复湿度',
        group: 'status'
      }
    ])

    expect(message).toBe('对象洞察配置中存在重复指标标识：S1_ZT_1.humidity')
  })

  it('rejects more than twenty metrics before submit', () => {
    const rows = Array.from({ length: 21 }, (_, index) => ({
      ...createEmptyProductObjectInsightMetric(),
      identifier: `S1_ZT_1.metric_${index + 1}`,
      displayName: `指标${index + 1}`,
      group: 'status' as const
    }))

    const message = validateProductObjectInsightMetrics(rows)

    expect(message).toBe('对象洞察配置最多允许 20 个指标')
  })
})
