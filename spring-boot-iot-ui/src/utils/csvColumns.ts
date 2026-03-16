import type { CsvColumn } from '@/utils/csv'

export interface CsvColumnOption {
  key: string
  label: string
}

const STORAGE_PREFIX = 'iot.csv.columns.'

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
