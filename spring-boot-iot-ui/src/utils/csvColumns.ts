import type { CsvColumn } from '@/utils/csv'

export interface CsvColumnOption {
  key: string
  label: string
}

export interface CsvColumnPreset {
  label: string
  keys: string[]
  group?: string
  lastModifiedAt?: string
}

export interface CsvRecentPresetRecord {
  label: string
  usedAt: string
}

const STORAGE_PREFIX = 'iot.csv.columns.'
const PRESET_STORAGE_PREFIX = 'iot.csv.presets.'
const PRESET_RECENT_PREFIX = 'iot.csv.presets.recent.'
const PRESET_PINNED_PREFIX = 'iot.csv.presets.pinned.'

export const loadCsvColumnSelection = (storageKey: string, defaultKeys: string[]): string[] => {
  try {
    const raw = localStorage.getItem(`${STORAGE_PREFIX}${storageKey}`)
    if (!raw) {
      return defaultKeys
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return defaultKeys
    }
    return parsed.filter((item) => typeof item === 'string')
  } catch {
    return defaultKeys
  }
}

export const saveCsvColumnSelection = (storageKey: string, selectedKeys: string[]) => {
  try {
    localStorage.setItem(`${STORAGE_PREFIX}${storageKey}`, JSON.stringify(selectedKeys))
  } catch {
    // 忽略本地存储异常，避免阻断导出主流程
  }
}

export const loadCsvPresetTemplates = (storageKey: string): CsvColumnPreset[] => {
  try {
    const raw = localStorage.getItem(`${PRESET_STORAGE_PREFIX}${storageKey}`)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed
      .filter((item) => item && typeof item.label === 'string' && Array.isArray(item.keys))
      .map((item) => ({
        label: item.label.trim(),
        keys: item.keys.filter((key: unknown) => typeof key === 'string'),
        group: typeof item.group === 'string' ? item.group.trim() : undefined,
        lastModifiedAt: typeof item.lastModifiedAt === 'string' ? item.lastModifiedAt : undefined
      }))
      .filter((item) => item.label && item.keys.length)
  } catch {
    return []
  }
}

export const saveCsvPresetTemplates = (storageKey: string, presets: CsvColumnPreset[]) => {
  try {
    localStorage.setItem(`${PRESET_STORAGE_PREFIX}${storageKey}`, JSON.stringify(presets))
  } catch {
    // 忽略本地存储异常，避免阻断主流程
  }
}

export const loadCsvRecentPresetRecords = (storageKey: string): CsvRecentPresetRecord[] => {
  try {
    const raw = localStorage.getItem(`${PRESET_RECENT_PREFIX}${storageKey}`)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    if (parsed.length && typeof parsed[0] === 'string') {
      return parsed
        .filter((item) => typeof item === 'string')
        .map((label) => ({ label, usedAt: '' }))
    }
    return parsed
      .filter((item) => item && typeof item.label === 'string')
      .map((item) => ({
        label: item.label.trim(),
        usedAt: typeof item.usedAt === 'string' ? item.usedAt : ''
      }))
      .filter((item) => item.label)
  } catch {
    return []
  }
}

export const saveCsvRecentPresetRecords = (storageKey: string, records: CsvRecentPresetRecord[]) => {
  try {
    localStorage.setItem(
      `${PRESET_RECENT_PREFIX}${storageKey}`,
      JSON.stringify(records.slice(0, 10))
    )
  } catch {
    // 忽略本地存储异常，避免阻断主流程
  }
}

export const loadCsvPinnedPresetLabels = (storageKey: string): string[] => {
  try {
    const raw = localStorage.getItem(`${PRESET_PINNED_PREFIX}${storageKey}`)
    if (!raw) {
      return []
    }
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return []
    }
    return parsed.filter((item) => typeof item === 'string')
  } catch {
    return []
  }
}

export const saveCsvPinnedPresetLabels = (storageKey: string, labels: string[]) => {
  try {
    localStorage.setItem(`${PRESET_PINNED_PREFIX}${storageKey}`, JSON.stringify(labels.slice(0, 20)))
  } catch {
    // 忽略本地存储异常，避免阻断主流程
  }
}

export const toCsvColumnOptions = <T extends object>(columns: CsvColumn<T>[]): CsvColumnOption[] =>
  columns.map((column) => ({
    key: String(column.key),
    label: column.label
  }))

export const resolveCsvColumns = <T extends object>(
  columns: CsvColumn<T>[],
  selectedKeys: string[]
): CsvColumn<T>[] => {
  if (!selectedKeys.length) {
    return columns
  }
  const map = new Map(columns.map((column) => [String(column.key), column]))
  return selectedKeys.map((key) => map.get(key)).filter((column): column is CsvColumn<T> => Boolean(column))
}
