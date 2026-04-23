import type { ProductObjectInsightMetricGroup } from '@/types/api'

const STATUS_EVENT_PATTERN =
  /(sensor_state|alarm|warn|warning|fault|switch|enable|relay|valve|pump|door|light|horn|status|state|online|offline|normal|异常|状态|在线|离线|正常|告警|预警|报警|开关|启停|开启|关闭|阀|泵|门|声光)/i
const RUNTIME_PATTERN =
  /(battery|signal|humidity|temperature|temp|voltage|current|network|energy|power|4g|rssi|snr|dbm|strength|soc|dump_energy|remaining|电量|电池|湿度|温度|电压|电流|信号|网络|能量|功率|剩余)/i

export const OBJECT_INSIGHT_METRIC_GROUP_OPTIONS: Array<{
  label: string
  value: ProductObjectInsightMetricGroup
}> = [
  { label: '监测数据', value: 'measure' },
  { label: '状态事件', value: 'statusEvent' },
  { label: '运行参数', value: 'runtime' }
]

export function getObjectInsightMetricGroupLabel(group: ProductObjectInsightMetricGroup) {
  return OBJECT_INSIGHT_METRIC_GROUP_OPTIONS.find((item) => item.value === group)?.label || '运行参数'
}

export function inferObjectInsightStatusGroup(identifier?: string | null, displayName?: string | null): ProductObjectInsightMetricGroup {
  const semanticSource = `${identifier ?? ''} ${displayName ?? ''}`.trim()
  if (!semanticSource) {
    return 'runtime'
  }
  if (RUNTIME_PATTERN.test(semanticSource)) {
    return 'runtime'
  }
  if (STATUS_EVENT_PATTERN.test(semanticSource)) {
    return 'statusEvent'
  }
  return 'runtime'
}

export function normalizeObjectInsightMetricGroup(
  group: unknown,
  identifier?: string | null,
  displayName?: string | null
): ProductObjectInsightMetricGroup {
  if (group === 'measure' || group === 'statusEvent' || group === 'runtime') {
    return group
  }
  if (group === 'status') {
    return inferObjectInsightStatusGroup(identifier, displayName)
  }
  return inferObjectInsightStatusGroup(identifier, displayName)
}
