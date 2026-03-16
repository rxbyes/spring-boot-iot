const sanitizeCell = (value: unknown): string => {
  if (value === null || value === undefined) {
    return ''
  }
  if (Array.isArray(value) || typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const escapeCsvCell = (value: string): string => {
  const escaped = value.replace(/"/g, '""')
  return `"${escaped}"`
}

export interface CsvColumn<T extends object> {
  key: keyof T | string
  label: string
  formatter?: (value: unknown, row: T) => unknown
}

export const downloadRowsAsCsv = <T extends object>(
  filename: string,
  rows: T[],
  columns?: CsvColumn<T>[]
) => {
  if (!rows.length) {
    return
  }
  const defaultKeys = Array.from(
    rows.reduce((set, row) => {
      Object.keys(row as Record<string, unknown>).forEach((key) => {
        if (key !== 'children') {
          set.add(key)
        }
      })
      return set
    }, new Set<string>())
  )
  const resolvedColumns = columns?.length
    ? columns
    : defaultKeys.map((key) => ({ key, label: key }))
  if (!resolvedColumns.length) {
    return
  }

  const lines = [
    resolvedColumns.map((column) => escapeCsvCell(column.label)).join(','),
    ...rows.map((row) =>
      resolvedColumns
        .map((column) => {
          const rawValue = (row as Record<string, unknown>)[String(column.key)]
          const value = column.formatter ? column.formatter(rawValue, row) : rawValue
          return escapeCsvCell(sanitizeCell(value))
        })
        .join(',')
    )
  ]
  const csv = `\ufeff${lines.join('\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}
