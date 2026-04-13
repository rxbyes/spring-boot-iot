import { describe, expect, it } from 'vitest'

import {
  buildProductMetadataJson,
  createEmptyProductObjectInsightMetric,
  createProductObjectInsightMetricFromModel,
  findProductObjectInsightMetric,
  removeProductObjectInsightMetric,
  parseProductObjectInsightMetrics,
  upsertProductObjectInsightMetric,
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
    expect(rows[0].group).toBe('runtime')
  })

  it('serializes editable rows back into metadataJson.objectInsight.customMetrics', () => {
    const metadataJson = buildProductMetadataJson([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.signal_4g',
        displayName: '4G 信号强度',
        group: 'runtime',
        analysisTemplate: '{{label}}当前为{{value}}',
        sortNo: 20
      }
    ])

    expect(metadataJson).toContain('objectInsight')
    expect(metadataJson).toContain('S1_ZT_1.signal_4g')
  })

  it('serializes status-event rows back into the backend status group contract', () => {
    const metadataJson = buildProductMetadataJson([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.sensor_state',
        displayName: '设备状态',
        group: 'statusEvent',
        sortNo: 12
      }
    ])

    expect(metadataJson).toContain('"group":"status"')
    expect(metadataJson).not.toContain('"group":"statusEvent"')
  })

  it('rejects duplicate identifiers before submit', () => {
    const message = validateProductObjectInsightMetrics([
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '相对湿度',
        group: 'runtime'
      },
      {
        ...createEmptyProductObjectInsightMetric(),
        identifier: 'S1_ZT_1.humidity',
        displayName: '重复湿度',
        group: 'runtime'
      }
    ])

    expect(message).toBe('对象洞察配置中存在重复指标标识：S1_ZT_1.humidity')
  })

  it('rejects more than twenty metrics before submit', () => {
    const rows = Array.from({ length: 21 }, (_, index) => ({
      ...createEmptyProductObjectInsightMetric(),
      identifier: `S1_ZT_1.metric_${index + 1}`,
      displayName: `指标${index + 1}`,
      group: 'runtime' as const
    }))

    const message = validateProductObjectInsightMetrics(rows)

    expect(message).toBe('对象洞察配置最多允许 20 个指标')
  })

  it('creates and upserts trend focus metrics from formal product models', () => {
    const created = createProductObjectInsightMetricFromModel(
      {
        identifier: 'L1_LF_1.value',
        modelName: '裂缝量',
        sortNo: 6
      },
      'measure'
    )

    expect(created).toEqual(
      expect.objectContaining({
        identifier: 'value',
        displayName: '裂缝量',
        group: 'measure',
        includeInTrend: true,
        includeInExtension: false,
        enabled: true,
        sortNo: 6
      })
    )

    const updated = upsertProductObjectInsightMetric(
      [
        {
          ...created,
          group: 'runtime',
          analysisTemplate: '{{label}}来自旧配置'
        }
      ],
      createProductObjectInsightMetricFromModel(
        {
          identifier: 'L1_LF_1.value',
          modelName: '裂缝量',
          sortNo: 2
        },
        'measure'
      )
    )

    expect(updated).toHaveLength(1)
    expect(findProductObjectInsightMetric(updated, 'L1_LF_1.value')?.identifier).toBe('value')
    expect(findProductObjectInsightMetric(updated, 'L1_LF_1.value')?.group).toBe('measure')
    expect(findProductObjectInsightMetric(updated, 'L1_LF_1.value')?.analysisTemplate).toBe('{{label}}来自旧配置')
    expect(removeProductObjectInsightMetric(updated, 'L1_LF_1.value')).toEqual([])
  })
})
