import { getDictByCode } from '@/api/dict'

export interface RiskPointLevelOptionLike {
  itemName?: string
  itemValue?: string
  sortNo?: number
  status?: number
}

export interface RiskPointLevelOption {
  label: string
  value: string
  sortNo: number
}

export const DEFAULT_RISK_POINT_LEVEL_OPTIONS: RiskPointLevelOption[] = [
  { label: '一级风险点', value: 'level_1', sortNo: 1 },
  { label: '二级风险点', value: 'level_2', sortNo: 2 },
  { label: '三级风险点', value: 'level_3', sortNo: 3 }
]

export function normalizeRiskPointLevel(value?: string | null) {
  return (value || '').trim().toLowerCase()
}

export function buildRiskPointLevelOptions(items: RiskPointLevelOptionLike[] = []) {
  const normalizedOptions = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || getDefaultRiskPointLevelText(item.itemValue),
      value: normalizeRiskPointLevel(item.itemValue),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const uniqueOptions = new Map<string, RiskPointLevelOption>()
  normalizedOptions
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!uniqueOptions.has(item.value)) {
        uniqueOptions.set(item.value, item)
      }
    })

  return uniqueOptions.size > 0 ? Array.from(uniqueOptions.values()) : cloneDefaultRiskPointLevelOptions()
}

export function getRiskPointLevelText(value?: string | null, options: RiskPointLevelOption[] = []) {
  const normalized = normalizeRiskPointLevel(value)
  if (!normalized) {
    return '未标注'
  }
  return options.find((item) => item.value === normalized)?.label || getDefaultRiskPointLevelText(normalized)
}

export async function fetchRiskPointLevelOptions() {
  try {
    const response = await getDictByCode('risk_point_level')
    return response.code === 200 ? buildRiskPointLevelOptions(response.data?.items || []) : cloneDefaultRiskPointLevelOptions()
  } catch {
    return cloneDefaultRiskPointLevelOptions()
  }
}

function getDefaultRiskPointLevelText(value?: string | null) {
  switch (normalizeRiskPointLevel(value)) {
    case 'level_1':
      return '一级风险点'
    case 'level_2':
      return '二级风险点'
    case 'level_3':
      return '三级风险点'
    default:
      return value || '未标注'
  }
}

function cloneDefaultRiskPointLevelOptions() {
  return DEFAULT_RISK_POINT_LEVEL_OPTIONS.map((item) => ({ ...item }))
}
