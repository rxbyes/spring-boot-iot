import { getDictByCode } from '@/api/dict'

export type AlarmLevelTagType = 'danger' | 'warning' | 'success' | 'info'

export interface AlarmLevelOptionLike {
  itemName?: string
  itemValue?: string
  sortNo?: number
  status?: number
}

export interface AlarmLevelOption {
  label: string
  value: string
  sortNo: number
}

export const DEFAULT_ALARM_LEVEL_OPTIONS: AlarmLevelOption[] = [
  { label: '红色', value: 'red', sortNo: 1 },
  { label: '橙色', value: 'orange', sortNo: 2 },
  { label: '黄色', value: 'yellow', sortNo: 3 },
  { label: '蓝色', value: 'blue', sortNo: 4 }
]

export function normalizeAlarmLevel(value?: string | null) {
  switch ((value || '').trim().toLowerCase()) {
    case 'red':
    case 'critical':
      return 'red'
    case 'orange':
    case 'high':
    case 'warning':
      return 'orange'
    case 'yellow':
    case 'medium':
      return 'yellow'
    case 'blue':
    case 'low':
    case 'info':
      return 'blue'
    default:
      return (value || '').trim().toLowerCase()
  }
}

export function buildAlarmLevelOptions(items: AlarmLevelOptionLike[] = []) {
  const normalizedOptions = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || getDefaultAlarmLevelText(item.itemValue),
      value: normalizeAlarmLevel(item.itemValue),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const uniqueOptions = new Map<string, AlarmLevelOption>()
  normalizedOptions
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!uniqueOptions.has(item.value)) {
        uniqueOptions.set(item.value, item)
      }
    })

  return uniqueOptions.size > 0 ? Array.from(uniqueOptions.values()) : cloneDefaultAlarmLevelOptions()
}

export function getAlarmLevelText(value?: string | null, options: AlarmLevelOption[] = []) {
  const normalized = normalizeAlarmLevel(value)
  if (!normalized) {
    return '未标注'
  }
  return options.find((item) => item.value === normalized)?.label || getDefaultAlarmLevelText(normalized)
}

export function getAlarmLevelTagType(value?: string | null): AlarmLevelTagType {
  switch (normalizeAlarmLevel(value)) {
    case 'red':
      return 'danger'
    case 'orange':
      return 'warning'
    case 'yellow':
      return 'success'
    default:
      return 'info'
  }
}

export async function fetchAlarmLevelOptions() {
  try {
    const response = await getDictByCode('alarm_level')
    return response.code === 200 ? buildAlarmLevelOptions(response.data?.items || []) : cloneDefaultAlarmLevelOptions()
  } catch {
    return cloneDefaultAlarmLevelOptions()
  }
}

function getDefaultAlarmLevelText(value?: string | null) {
  switch (normalizeAlarmLevel(value)) {
    case 'red':
      return '红色'
    case 'orange':
      return '橙色'
    case 'yellow':
      return '黄色'
    case 'blue':
      return '蓝色'
    default:
      return value || '未标注'
  }
}

function cloneDefaultAlarmLevelOptions() {
  return DEFAULT_ALARM_LEVEL_OPTIONS.map((item) => ({ ...item }))
}
