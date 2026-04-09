export interface RiskLevelOptionLike {
  itemName?: string
  itemValue?: string
  sortNo?: number
  status?: number
}

export interface RiskLevelOption {
  label: string
  value: string
  sortNo: number
}

export function normalizeRiskLevel(value?: string | null) {
  switch ((value || '').trim().toLowerCase()) {
    case 'critical':
      return 'red'
    case 'warning':
      return 'orange'
    case 'info':
      return 'blue'
    default:
      return (value || '').trim().toLowerCase()
  }
}

export function buildRiskLevelOptions(items: RiskLevelOptionLike[] = []) {
  const normalizedOptions = items
    .filter((item) => item && item.status !== 0)
    .map((item, index) => ({
      label: item.itemName || getDefaultRiskLevelText(item.itemValue),
      value: normalizeRiskLevel(item.itemValue),
      sortNo: Number(item.sortNo ?? index)
    }))
    .filter((item) => Boolean(item.value))

  const uniqueOptions = new Map<string, RiskLevelOption>()
  normalizedOptions
    .sort((left, right) => left.sortNo - right.sortNo)
    .forEach((item) => {
      if (!uniqueOptions.has(item.value)) {
        uniqueOptions.set(item.value, item)
      }
    })

  return Array.from(uniqueOptions.values())
}

export function getRiskLevelText(value?: string | null, options: RiskLevelOption[] = []) {
  const normalized = normalizeRiskLevel(value)
  if (!normalized) {
    return '未标注'
  }
  return options.find((item) => item.value === normalized)?.label || getDefaultRiskLevelText(normalized)
}

export function getRiskLevelTagType(value?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  switch (normalizeRiskLevel(value)) {
    case 'red':
      return 'danger'
    case 'orange':
      return 'warning'
    case 'yellow':
      return 'success'
    case 'blue':
      return 'info'
    default:
      return 'info'
  }
}

export function getRiskLevelWeight(value?: string | null) {
  switch (normalizeRiskLevel(value)) {
    case 'red':
      return 4
    case 'orange':
      return 3
    case 'yellow':
      return 2
    case 'blue':
      return 1
    default:
      return 0
  }
}

export async function fetchRiskLevelOptions() {
  const response = await getDictByCode('risk_level')
  return response.code === 200 ? buildRiskLevelOptions(response.data?.items || []) : []
}

function getDefaultRiskLevelText(value?: string | null) {
  switch (normalizeRiskLevel(value)) {
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
import { getDictByCode } from '@/api/dict'
